package xiaozhi.modules.agent.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.utils.JsonUtils;
import xiaozhi.modules.agent.dao.AgentDao;
import xiaozhi.modules.agent.dao.ChatDirectionAnalysisDao;
import xiaozhi.modules.agent.dao.ChatDirectionQueryDao;
import xiaozhi.modules.agent.dto.ChatDirectionQueryDTO;
import xiaozhi.modules.agent.dto.ChatDirectionRequestDTO;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.agent.entity.ChatDirectionAnalysisEntity;
import xiaozhi.modules.agent.service.ChatDirectionService;
import xiaozhi.modules.agent.service.ChatTextTokenizer;
import xiaozhi.modules.agent.service.ChatWakeupWordFilter;
import xiaozhi.modules.agent.vo.ChatDirectionAnalysisVO;
import xiaozhi.modules.agent.vo.ChatDirectionPromptPreviewVO;
import xiaozhi.modules.agent.vo.ChatDirectionVO;
import xiaozhi.modules.device.dao.DeviceDao;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.llm.service.LLMService;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.sys.service.SysParamsService;
import xiaozhi.modules.sys.service.UserDataScopeService;

/**
 * 会话内容分析丨高频方向。
 *
 * <p>时间口径与会话记录一致：库中 created_at 是 UTC 墙上值，前端传 epoch 毫秒，
 * 展示统一按 Asia/Shanghai 格式化。数据可见范围统一走 {@link UserDataScopeService}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatDirectionServiceImpl implements ChatDirectionService {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneOffset DB_ZONE = ZoneOffset.UTC;
    private static final DateTimeFormatter CN_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 最多读取的提问条数（最新优先），分词是 CPU 密集型，防止大表拖垮服务 */
    private static final int MAX_ROWS = 20000;

    /** 单条提问参与分词的最大字符数 */
    private static final int MAX_TEXT_LENGTH = 2000;

    /** 每个分组保留的高频问题 / 高频词数量范围 */
    private static final int MIN_TOP_N = 5;
    private static final int MAX_TOP_N = 100;

    /** 单条问题展示与参与统计的最大字符数，超长问题截断，避免提示词超长 */
    static final int MAX_QUESTION_LENGTH = 100;

    /** 发给大模型的内容上限（字符），超过时按分组从少到多截断，避免超长提示词 */
    private static final int MAX_LLM_INPUT_CHARS = 8000;

    /** 参与大模型分析的地区上限（按提问条数从高到低） */
    private static final int MAX_LLM_REGIONS = 20;

    /** 设备未解析出地域时的归类名称 */
    static final String UNKNOWN_REGION = "未知地区";

    /** 参数管理中没有配置提示词时的兜底（正常情况下由 Liquibase 写入 sys_params） */
    private static final String FALLBACK_PROMPT = "请分析下面按地区汇总的用户高频提问，先把意思相近的问题归为一类，"
            + "再归纳它们主要属于哪些方向，并按地区说明差异，最后给出建议。筛选条件：{filter}\n高频提问：\n{conversation}";

    /** 问题首尾需要去掉的标点、语气符号（中英文） */
    private static final String EDGE_PUNCT = "[\\s\\p{Punct}，。！？、；：“”‘’（）【】《》…～·]+";

    private final ChatDirectionQueryDao chatDirectionQueryDao;
    private final ChatDirectionAnalysisDao chatDirectionAnalysisDao;
    private final AgentDao agentDao;
    private final DeviceDao deviceDao;
    private final ChatTextTokenizer chatTextTokenizer;
    private final UserDataScopeService userDataScopeService;
    private final SysParamsService sysParamsService;
    private final LLMService llmService;
    private final ModelConfigService modelConfigService;
    private final ChatWakeupWordFilter chatWakeupWordFilter;

    // ---------------- 统计 ----------------

    @Override
    public ChatDirectionVO getChatDirection(ChatDirectionRequestDTO request, Long userId, int topN) {
        ChatDirectionVO result = new ChatDirectionVO();
        int size = Math.min(MAX_TOP_N, Math.max(MIN_TOP_N, topN));
        ChatDirectionRequestDTO req = request == null ? new ChatDirectionRequestDTO() : request;

        List<String> visibleAgentIds = resolveVisibleAgentIds(userId);
        if (visibleAgentIds != null && visibleAgentIds.isEmpty()) {
            return result;
        }
        // 指定智能体时仍要校验归属，防止越权查看他人数据
        if (visibleAgentIds != null && StringUtils.isNotBlank(req.getAgentId())
                && !visibleAgentIds.contains(req.getAgentId())) {
            return result;
        }

        ChatDirectionQueryDTO q = buildQuery(req, visibleAgentIds);
        List<Map<String, Object>> rows = chatDirectionQueryDao.selectUserQuestionRows(q);
        result.setTruncated(rows.size() >= MAX_ROWS);
        if (rows.isEmpty()) {
            return result;
        }

        Map<String, Counter> regionCounter = new LinkedHashMap<>();
        Map<String, Counter> agentCounter = new LinkedHashMap<>();
        Map<String, Counter> deviceCounter = new LinkedHashMap<>();
        long messageCount = 0;
        // 参数「会话分析是否过滤唤醒词」开启时，统计前去掉 wakeup_words 中的唤醒词
        List<String> wakeupWords = chatWakeupWordFilter.activeWords();
        for (Map<String, Object> row : rows) {
            String text = ChatWakeupWordFilter.strip(extractContent(asString(row.get("content"))), wakeupWords);
            if (StringUtils.isBlank(text)) {
                continue;
            }
            if (text.length() > MAX_TEXT_LENGTH) {
                text = text.substring(0, MAX_TEXT_LENGTH);
            }
            String question = normalizeQuestion(text);
            if (question == null) {
                continue;
            }
            List<String> tokens = chatTextTokenizer.tokenize(text);
            messageCount++;
            String region = StringUtils.defaultIfBlank(asString(row.get("province")), UNKNOWN_REGION);
            regionCounter.computeIfAbsent(region, k -> new Counter()).add(question, tokens);
            String agentId = asString(row.get("agent_id"));
            if (StringUtils.isNotBlank(agentId)) {
                agentCounter.computeIfAbsent(agentId, k -> new Counter()).add(question, tokens);
            }
            String mac = asString(row.get("mac_address"));
            if (StringUtils.isNotBlank(mac)) {
                deviceCounter.computeIfAbsent(mac, k -> new Counter()).add(question, tokens);
            }
        }
        result.setMessageCount(messageCount);
        result.setRegions(toGroups(regionCounter, size, key -> key));
        result.setAgents(toGroups(agentCounter, size, loadAgentNameMap(agentCounter.keySet())::get));
        result.setDevices(toGroups(deviceCounter, size, loadDeviceNames(deviceCounter.keySet())::get));
        return result;
    }

    // ---------------- 大模型分析 ----------------

    @Override
    public ChatDirectionAnalysisVO analyzeByLlm(ChatDirectionRequestDTO request, Long userId, int topN) {
        ChatDirectionRequestDTO req = request == null ? new ChatDirectionRequestDTO() : request;
        ChatDirectionVO direction = getChatDirection(req, userId, topN);
        if (direction.getRegions().isEmpty()) {
            throw new RenException("当前筛选条件下没有可分析的用户提问数据");
        }
        if (!llmService.isAvailable()) {
            throw new RenException("未配置可用的默认大语言模型，请先在模型配置中设置");
        }

        String input = buildLlmInput(direction);
        // LLMService 只替换 {conversation}，{filter} 在这里先填好
        String prompt = resolvePromptTemplate().replace("{filter}", describeFilter(req));

        String answer = llmService.generateSummary(input, prompt);
        if (isLlmFailure(answer)) {
            throw new RenException(StringUtils.defaultIfBlank(answer, "大模型分析失败，请稍后重试"));
        }

        ChatDirectionAnalysisEntity entity = new ChatDirectionAnalysisEntity();
        entity.setUserId(userId);
        entity.setProvince(trimToNull(req.getProvince()));
        entity.setAgentId(trimToNull(req.getAgentId()));
        entity.setMacAddress(trimToNull(req.getMacAddress()));
        entity.setStartTime(req.getStartTime() == null ? null : toDbDate(req.getStartTime()));
        entity.setEndTime(req.getEndTime() == null ? null : toDbDate(req.getEndTime()));
        entity.setTopN(Math.min(MAX_TOP_N, Math.max(MIN_TOP_N, topN)));
        entity.setInputContent(input);
        entity.setResult(answer);
        entity.setModelName(resolveDefaultModelName());
        entity.setCreatedAt(new Date());
        chatDirectionAnalysisDao.insert(entity);
        return toAnalysisVO(entity, loadAgentNameMap(Collections.singleton(entity.getAgentId())),
                Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    public ChatDirectionPromptPreviewVO previewPrompt(ChatDirectionRequestDTO request, Long userId, int topN) {
        ChatDirectionRequestDTO req = request == null ? new ChatDirectionRequestDTO() : request;
        ChatDirectionVO direction = getChatDirection(req, userId, topN);
        if (direction.getRegions().isEmpty()) {
            throw new RenException("当前筛选条件下没有可分析的用户提问数据");
        }
        String configured = sysParamsService.getValue(Constant.SERVER_CHAT_DIRECTION_PROMPT, true);
        String template = StringUtils.defaultIfBlank(configured, FALLBACK_PROMPT);
        String filterText = describeFilter(req);
        String input = buildLlmInput(direction);

        ChatDirectionPromptPreviewVO vo = new ChatDirectionPromptPreviewVO();
        vo.setParamCode(Constant.SERVER_CHAT_DIRECTION_PROMPT);
        vo.setTemplate(template);
        vo.setFallback(StringUtils.isBlank(configured));
        vo.setFilterText(filterText);
        vo.setInput(input);
        // 与 analyzeByLlm 的替换顺序一致，保证预览即实际发送的内容
        vo.setFullPrompt(template.replace("{filter}", filterText).replace("{conversation}", input));
        vo.setModelName(resolveDefaultModelName());
        vo.setLlmAvailable(llmService.isAvailable());
        return vo;
    }

    /** 参数管理中的提示词模板，未配置时使用内置兜底 */
    private String resolvePromptTemplate() {
        return StringUtils.defaultIfBlank(
                sysParamsService.getValue(Constant.SERVER_CHAT_DIRECTION_PROMPT, true), FALLBACK_PROMPT);
    }

    @Override
    public List<String> listProvinces(Long userId) {
        List<String> visibleAgentIds = resolveVisibleAgentIds(userId);
        if (visibleAgentIds != null && visibleAgentIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<DeviceEntity> wrapper = new QueryWrapper<DeviceEntity>()
                .select("province").isNotNull("province").ne("province", "");
        if (visibleAgentIds != null) {
            wrapper.in("agent_id", visibleAgentIds);
        }
        return deviceDao.selectList(wrapper).stream()
                .map(DeviceEntity::getProvince)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public ChatDirectionAnalysisVO getLatestAnalysis(Long userId) {
        List<ChatDirectionAnalysisVO> list = listAnalyses(userId, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<ChatDirectionAnalysisVO> listAnalyses(Long userId, int limit) {
        int size = Math.min(50, Math.max(1, limit));
        QueryWrapper<ChatDirectionAnalysisEntity> wrapper = new QueryWrapper<ChatDirectionAnalysisEntity>()
                .orderByDesc("created_at").last("LIMIT " + size);
        // 与会话记录同一套隔离规则：默认只看自己的，超管 + 参数开启才能看全部
        if (!userDataScopeService.canViewAllUserData()) {
            if (userId == null) {
                return Collections.emptyList();
            }
            wrapper.eq("user_id", userId);
        }
        List<ChatDirectionAnalysisEntity> entities = chatDirectionAnalysisDao.selectList(wrapper);
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> agentIds = entities.stream().map(ChatDirectionAnalysisEntity::getAgentId)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        Set<Long> ownerIds = entities.stream().map(ChatDirectionAnalysisEntity::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, String> agentNames = loadAgentNameMap(agentIds);
        Map<Long, String> usernames = userDataScopeService.getUsernameMap(ownerIds);
        Map<Long, String> nicknames = userDataScopeService.getNicknameMap(ownerIds);
        return entities.stream()
                .map(e -> toAnalysisVO(e, agentNames, usernames, nicknames))
                .collect(Collectors.toList());
    }

    // ---------------- 内部方法 ----------------

    private ChatDirectionQueryDTO buildQuery(ChatDirectionRequestDTO req, List<String> visibleAgentIds) {
        ChatDirectionQueryDTO q = new ChatDirectionQueryDTO();
        q.setVisibleAgentIds(visibleAgentIds);
        q.setProvince(trimToNull(req.getProvince()));
        q.setAgentId(trimToNull(req.getAgentId()));
        q.setMacAddress(trimToNull(req.getMacAddress()));
        if (req.getStartTime() != null) {
            q.setStartTime(toDbLiteral(req.getStartTime()));
        }
        if (req.getEndTime() != null) {
            q.setEndTime(toDbLiteral(req.getEndTime()));
        }
        q.setLimit(MAX_ROWS);
        return q;
    }

    /**
     * 当前登录用户可见的智能体ID列表；返回 null 表示不限制（超管 + 参数开启）。
     * ai_agent_chat_history 没有 user_id，必须经 ai_agent.user_id 做归属过滤。
     */
    private List<String> resolveVisibleAgentIds(Long userId) {
        if (userDataScopeService.canViewAllUserData()) {
            return null;
        }
        if (userId == null) {
            return Collections.emptyList();
        }
        List<AgentEntity> agents = agentDao.selectList(new QueryWrapper<AgentEntity>()
                .select("id").eq("user_id", userId));
        return agents.stream().map(AgentEntity::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    /** 把分组计数转成 VO，按提问条数倒序；相同时按 key 排序保证结果稳定 */
    private List<ChatDirectionVO.Group> toGroups(Map<String, Counter> counters, int topN,
            java.util.function.Function<String, String> nameResolver) {
        return counters.entrySet().stream()
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getValue().messages, a.getValue().messages);
                    return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                })
                .map(e -> {
                    ChatDirectionVO.Group group = new ChatDirectionVO.Group();
                    group.setKey(e.getKey());
                    String name = nameResolver.apply(e.getKey());
                    group.setName(StringUtils.defaultIfBlank(name, e.getKey()));
                    group.setMessageCount(e.getValue().messages);
                    group.setQuestions(e.getValue().topQuestions(topN));
                    group.setWords(e.getValue().top(topN));
                    return group;
                })
                .collect(Collectors.toList());
    }

    /**
     * 把地区统计组装成大模型输入：每个地区一段，下面逐行列出「- 问题（N 次）」。
     * 地区按提问条数从高到低取前 MAX_LLM_REGIONS 个，总长度超限即停止追加，避免超长提示词。
     */
    String buildLlmInput(ChatDirectionVO direction) {
        StringBuilder sb = new StringBuilder();
        int regions = 0;
        for (ChatDirectionVO.Group group : direction.getRegions()) {
            if (regions >= MAX_LLM_REGIONS) {
                break;
            }
            StringBuilder block = new StringBuilder();
            block.append(group.getName()).append("（").append(group.getMessageCount()).append(" 条提问）：\n");
            for (ChatDirectionVO.Question q : group.getQuestions()) {
                block.append("- ").append(q.getText()).append("（").append(q.getCount()).append(" 次）\n");
            }
            if (sb.length() > 0 && sb.length() + block.length() > MAX_LLM_INPUT_CHARS) {
                break;
            }
            sb.append(block).append("\n");
            regions++;
        }
        return sb.toString();
    }

    /**
     * 问题归一化：去掉首尾空白与标点、压缩中间空白，超长截断。
     * 归一化后为空（例如只有「嗯」「。」）或只有一个字的提问没有统计意义，返回 null。
     */
    static String normalizeQuestion(String text) {
        if (text == null) {
            return null;
        }
        String q = text.replaceAll("\\s+", " ").replaceAll("^" + EDGE_PUNCT, "").replaceAll(EDGE_PUNCT + "$", "");
        if (q.length() < 2) {
            return null;
        }
        return q.length() > MAX_QUESTION_LENGTH ? q.substring(0, MAX_QUESTION_LENGTH) : q;
    }

    /** 把筛选条件描述成一句话，填入提示词的 {filter} */
    private String describeFilter(ChatDirectionRequestDTO req) {
        List<String> parts = new ArrayList<>();
        parts.add("地区=" + StringUtils.defaultIfBlank(req.getProvince(), "全部"));
        parts.add("智能体=" + (StringUtils.isBlank(req.getAgentId()) ? "全部"
                : StringUtils.defaultIfBlank(loadAgentNameMap(Collections.singleton(req.getAgentId()))
                        .get(req.getAgentId()), req.getAgentId())));
        parts.add("设备=" + StringUtils.defaultIfBlank(req.getMacAddress(), "全部"));
        if (req.getStartTime() != null || req.getEndTime() != null) {
            parts.add("时间=" + (req.getStartTime() == null ? "不限" : formatCn(req.getStartTime()))
                    + " ~ " + (req.getEndTime() == null ? "不限" : formatCn(req.getEndTime())));
        } else {
            parts.add("时间=不限");
        }
        return String.join("；", parts);
    }

    /** OpenAIStyleLLMServiceImpl 失败时不抛异常，而是返回固定文案，这里识别出来当失败处理 */
    private boolean isLlmFailure(String answer) {
        if (StringUtils.isBlank(answer)) {
            return true;
        }
        return answer.startsWith("LLM服务不可用") || answer.startsWith("未找到可用的LLM模型配置")
                || answer.startsWith("LLM配置不完整") || answer.startsWith("生成总结失败");
    }

    /** 与 OpenAIStyleLLMServiceImpl 同一选择规则：默认模型优先，没有则取第一个启用的 */
    private String resolveDefaultModelName() {
        try {
            List<ModelConfigEntity> configs = modelConfigService.getEnabledModelsByType("LLM");
            if (configs == null || configs.isEmpty()) {
                return null;
            }
            for (ModelConfigEntity config : configs) {
                if (config.getIsDefault() != null && config.getIsDefault() == 1) {
                    return config.getModelName();
                }
            }
            return configs.get(0).getModelName();
        } catch (Exception e) {
            log.warn("读取默认大模型名称失败: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, String> loadAgentNameMap(Set<String> agentIds) {
        Set<String> ids = agentIds == null ? Collections.emptySet()
                : agentIds.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AgentEntity> agents = agentDao.selectList(new QueryWrapper<AgentEntity>()
                .select("id", "agent_name").in("id", ids));
        Map<String, String> map = new HashMap<>();
        for (AgentEntity agent : agents) {
            map.putIfAbsent(agent.getId(), agent.getAgentName());
        }
        return map;
    }

    private Map<String, String> loadDeviceNames(Set<String> macs) {
        if (macs == null || macs.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DeviceEntity> devices = deviceDao.selectList(new QueryWrapper<DeviceEntity>()
                .select("mac_address", "alias").in("mac_address", macs));
        Map<String, String> map = new HashMap<>();
        for (DeviceEntity device : devices) {
            if (StringUtils.isNotBlank(device.getAlias())) {
                map.putIfAbsent(device.getMacAddress(), device.getAlias() + " · " + device.getMacAddress());
            }
        }
        return map;
    }

    private ChatDirectionAnalysisVO toAnalysisVO(ChatDirectionAnalysisEntity e, Map<String, String> agentNames,
            Map<Long, String> usernames, Map<Long, String> nicknames) {
        ChatDirectionAnalysisVO vo = new ChatDirectionAnalysisVO();
        vo.setId(e.getId());
        vo.setProvince(e.getProvince());
        vo.setAgentId(e.getAgentId());
        vo.setAgentName(StringUtils.isBlank(e.getAgentId()) ? null
                : StringUtils.defaultIfBlank(agentNames.get(e.getAgentId()), e.getAgentId()));
        vo.setMacAddress(e.getMacAddress());
        vo.setStartTime(e.getStartTime() == null ? null : formatDbDate(e.getStartTime()));
        vo.setEndTime(e.getEndTime() == null ? null : formatDbDate(e.getEndTime()));
        vo.setResult(e.getResult());
        vo.setModelName(e.getModelName());
        vo.setCreatedAt(e.getCreatedAt() == null ? null : formatDbDate(e.getCreatedAt()));
        vo.setIsOwn(userDataScopeService.isOwn(e.getUserId()));
        vo.setOwnerUsername(usernames.get(e.getUserId()));
        vo.setOwnerNickname(nicknames.get(e.getUserId()));
        return vo;
    }

    /** epoch 毫秒 → 与库中 created_at 同口径的 UTC 字面值 */
    private LocalDateTime toDbLiteral(long epochMs) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), DB_ZONE);
    }

    /**
     * epoch 毫秒 → 写入 DATETIME 列的 Date。JDBC serverTimezone=UTC，Date#getTime 与 UTC 墙上值一致，
     * 所以直接用 epoch 毫秒构造即可，读回后再按 Asia/Shanghai 格式化。
     */
    private Date toDbDate(long epochMs) {
        return new Date(epochMs);
    }

    private String formatCn(long epochMs) {
        return CN_TIME_FORMAT.format(Instant.ofEpochMilli(epochMs).atZone(CHINA_ZONE));
    }

    private String formatDbDate(Date date) {
        return CN_TIME_FORMAT.format(date.toInstant().atZone(CHINA_ZONE));
    }

    /** 兼容 {"content": "..."} 与普通文本 */
    private String extractContent(String content) {
        if (StringUtils.isBlank(content)) {
            return content;
        }
        try {
            Map<String, Object> jsonMap = JsonUtils.parseMap(content);
            if (jsonMap != null && jsonMap.containsKey("content")) {
                Object obj = jsonMap.get("content");
                return obj != null ? obj.toString() : content;
            }
        } catch (Exception e) {
            // 不是 JSON，直接用原文
        }
        return content;
    }

    private String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    /** 分组计数器：提问条数 + 高频问题 + 词频 */
    private static class Counter {
        private long messages;
        /** 归一化 key（小写）→ 问题统计；展示文本取最先遇到的写法，明细按时间倒序读取，即最近一次的写法 */
        private final Map<String, QuestionStat> questions = new HashMap<>();
        private final Map<String, Long> words = new HashMap<>();

        void add(String question, List<String> tokens) {
            messages++;
            questions.computeIfAbsent(question.toLowerCase(java.util.Locale.ROOT), k -> new QuestionStat(question))
                    .count++;
            // 同一条提问里重复的词只计一次，避免一句话重复说法把某个词拉高
            Set<String> unique = new HashSet<>(tokens);
            for (String token : unique) {
                words.merge(token, 1L, Long::sum);
            }
        }

        List<ChatDirectionVO.Word> top(int topN) {
            return words.entrySet().stream()
                    .sorted((a, b) -> {
                        int byCount = Long.compare(b.getValue(), a.getValue());
                        return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                    })
                    .limit(topN)
                    .map(e -> new ChatDirectionVO.Word(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }

        /** 按出现次数倒序取前 N 个问题；次数相同时按问题文本排序，保证结果稳定 */
        List<ChatDirectionVO.Question> topQuestions(int topN) {
            return questions.values().stream()
                    .sorted((a, b) -> {
                        int byCount = Long.compare(b.count, a.count);
                        return byCount != 0 ? byCount : a.text.compareTo(b.text);
                    })
                    .limit(topN)
                    .map(s -> new ChatDirectionVO.Question(s.text, s.count))
                    .collect(Collectors.toList());
        }
    }

    private static class QuestionStat {
        private final String text;
        private long count;

        QuestionStat(String text) {
            this.text = text;
        }
    }
}
