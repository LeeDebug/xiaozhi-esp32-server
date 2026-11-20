package xiaozhi.modules.agent.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;

import xiaozhi.common.constant.Constant;
import xiaozhi.common.page.PageData;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.common.utils.JsonUtils;
import xiaozhi.modules.agent.Enums.AgentChatHistoryType;
import xiaozhi.modules.agent.dao.AgentChatTitleDao;
import xiaozhi.modules.agent.dao.AgentDao;
import xiaozhi.modules.agent.dao.AiAgentChatHistoryDao;
import xiaozhi.modules.agent.dto.AgentChatHistoryDTO;
import xiaozhi.modules.agent.dto.AgentChatSessionDTO;
import xiaozhi.modules.agent.dto.ChatRecordQueryDTO;
import xiaozhi.modules.agent.dto.ChatSessionQueryDTO;
import xiaozhi.modules.agent.entity.AgentChatHistoryEntity;
import xiaozhi.modules.agent.entity.AgentChatTitleEntity;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.agent.service.AgentChatHistoryService;
import xiaozhi.modules.agent.service.AgentChatTitleService;
import xiaozhi.modules.agent.service.ChatTextTokenizer;
import xiaozhi.modules.agent.vo.AgentChatHistoryUserVO;
import xiaozhi.modules.agent.vo.ChatAnalyticsVO;
import xiaozhi.modules.agent.vo.ChatRecordFilterVO;
import xiaozhi.modules.agent.vo.ChatRecordVO;
import xiaozhi.modules.agent.vo.ChatSessionVO;
import xiaozhi.modules.agent.vo.ChatWordCloudVO;
import xiaozhi.modules.device.dao.DeviceDao;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.sys.service.UserDataScopeService;

/**
 * 智能体会话记录表处理service {@link AgentChatHistoryService} impl
 *
 * @author Goody
 * @version 1.0, 2025/4/30
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AgentChatHistoryServiceImpl extends CrudRepository<AiAgentChatHistoryDao, AgentChatHistoryEntity>
        implements AgentChatHistoryService {

    private final AgentChatTitleService agentChatTitleService;
    private final AgentDao agentDao;
    private final AgentChatTitleDao agentChatTitleDao;
    private final DeviceDao deviceDao;
    private final UserDataScopeService userDataScopeService;
    private final ChatTextTokenizer chatTextTokenizer;

    /** 页面展示统一使用中国标准时间，避免依赖浏览器或 JVM 默认时区。 */
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    /** JDBC url 固定 serverTimezone=UTC，created_at(DATETIME) 保存的是 UTC 墙上时间。 */
    private static final ZoneOffset DB_ZONE = ZoneOffset.UTC;

    private static final DateTimeFormatter CN_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 列表页不返回超长正文，完整内容通过会话详情查看。 */
    private static final int MAX_CONTENT_LENGTH = 1000;

    @Override
    public PageData<AgentChatSessionDTO> getSessionListByAgentId(Map<String, Object> params) {
        String agentId = (String) params.get("agentId");
        int page = Integer.parseInt(params.get(Constant.PAGE).toString());
        int limit = Integer.parseInt(params.get(Constant.LIMIT).toString());

        // 构建查询条件
        QueryWrapper<AgentChatHistoryEntity> wrapper = new QueryWrapper<>();
        wrapper.select("session_id", "MAX(created_at) as created_at", "COUNT(*) as chat_count")
                .eq("agent_id", agentId)
                .groupBy("session_id")
                .orderByDesc("created_at");

        // 执行分页查询
        Page<Map<String, Object>> pageParam = new Page<>(page, limit);
        IPage<Map<String, Object>> result = this.baseMapper.selectMapsPage(pageParam, wrapper);

        List<AgentChatSessionDTO> records = result.getRecords().stream().map(map -> {
            AgentChatSessionDTO dto = new AgentChatSessionDTO();
            dto.setSessionId((String) map.get("session_id"));
            dto.setCreatedAt((LocalDateTime) map.get("created_at"));
            dto.setChatCount(((Number) map.get("chat_count")).intValue());
            dto.setTitle(agentChatTitleService.getTitleBySessionId(dto.getSessionId()));
            return dto;
        }).collect(Collectors.toList());

        return new PageData<>(records, result.getTotal());
    }

    @Override
    public List<AgentChatHistoryDTO> getChatHistoryBySessionId(String agentId, String sessionId) {
        // 构建查询条件
        QueryWrapper<AgentChatHistoryEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("agent_id", agentId)
                .eq("session_id", sessionId)
                .orderByAsc("created_at");

        // 查询会话记录
        List<AgentChatHistoryEntity> historyList = list(wrapper);

        // 转换为DTO
        List<AgentChatHistoryDTO> result = ConvertUtils.sourceToTarget(historyList, AgentChatHistoryDTO.class);
        // metrics_json 是 TEXT，字段名与 DTO 的 metrics 不同，无法由 ConvertUtils 自动映射，
        // 这里单独反序列化成对象供前端直接渲染；历史记录该列为 NULL，跳过即可。
        for (int i = 0; i < result.size(); i++) {
            String metricsJson = historyList.get(i).getMetricsJson();
            if (metricsJson != null && !metricsJson.isBlank()) {
                result.get(i).setMetrics(JsonUtils.parseMap(metricsJson));
            }
        }
        return result;
    }

    @Override
    public String getAgentIdBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        AgentChatHistoryEntity entity = baseMapper.selectOne(
                new LambdaQueryWrapper<AgentChatHistoryEntity>()
                        .select(AgentChatHistoryEntity::getAgentId)
                        .eq(AgentChatHistoryEntity::getSessionId, sessionId)
                        .last("LIMIT 1"));
        return entity == null ? null : entity.getAgentId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByAgentId(String agentId, Boolean deleteAudio, Boolean deleteText) {
        if (deleteAudio) {
            // 分批删除音频,避免超时
            List<String> audioIds = baseMapper.getAudioIdsByAgentId(agentId);
            if (CollUtil.isNotEmpty(audioIds)) {
                // 每批删除1000条
                List<List<String>> batch = ListUtil.split(audioIds, 1000);
                batch.forEach(dataList -> {
                    baseMapper.deleteAudioByIds(dataList);
                });
            }
        }
        if (deleteAudio && !deleteText) {
            baseMapper.deleteAudioIdByAgentId(agentId);
        }
        if (deleteText) {
            baseMapper.deleteHistoryByAgentId(agentId);
        }

    }

    @Override
    public List<AgentChatHistoryUserVO> getRecentlyFiftyByAgentId(String agentId) {
        // 构建查询条件(不添加按照创建时间排序，数据本来就是主键越大创建时间越大
        // 不添加这样可以减少排序全部数据在分页的全盘扫描消耗)
        LambdaQueryWrapper<AgentChatHistoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AgentChatHistoryEntity::getContent, AgentChatHistoryEntity::getAudioId)
                .eq(AgentChatHistoryEntity::getAgentId, agentId)
                .eq(AgentChatHistoryEntity::getChatType, AgentChatHistoryType.USER.getValue())
                .isNotNull(AgentChatHistoryEntity::getAudioId)
                // 添加此行，确保查询结果按照创建时间降序排列
                // 使用id的原因：数据形式，id越大的创建时间就越晚，所以使用id的结果和创建时间降序排列结果一样
                // id作为降序排列的优势，性能高，有主键索引，不用在排序的时候重新进行排除扫描比较
                .orderByDesc(AgentChatHistoryEntity::getId);

        // 构建分页查询，查询前50页数据
        Page<AgentChatHistoryEntity> pageParam = new Page<>(0, 50);
        IPage<AgentChatHistoryEntity> result = this.baseMapper.selectPage(pageParam, wrapper);
        return result.getRecords().stream().map(item -> {
            AgentChatHistoryUserVO vo = ConvertUtils.sourceToTarget(item, AgentChatHistoryUserVO.class);
            // 处理 content 字段，确保只返回聊天内容
            if (vo != null && vo.getContent() != null) {
                vo.setContent(extractContentFromString(vo.getContent()));
            }
            return vo;
        }).toList();
    }

    /**
     * 从 content 字段中提取聊天内容
     * 如果 content 是 JSON 格式（如 {"speaker": "未知说话人", "content": "现在几点了。"}），则提取 content
     * 字段
     * 如果 content 是普通字符串，则直接返回
     * 
     * @param content 原始内容
     * @return 提取的聊天内容
     */
    private String extractContentFromString(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        // 尝试解析为 JSON
        try {
            Map<String, Object> jsonMap = JsonUtils.parseMap(content);
            if (jsonMap != null && jsonMap.containsKey("content")) {
                Object contentObj = jsonMap.get("content");
                return contentObj != null ? contentObj.toString() : content;
            }
        } catch (Exception e) {
            // 如果不是有效的 JSON，直接返回原内容
        }

        // 如果不是 JSON 格式或没有 content 字段，直接返回原内容
        return content;
    }

    @Override
    public String getContentByAudioId(String audioId) {
        AgentChatHistoryEntity agentChatHistoryEntity = baseMapper
                .selectOne(new LambdaQueryWrapper<AgentChatHistoryEntity>()
                        .select(AgentChatHistoryEntity::getContent)
                        .eq(AgentChatHistoryEntity::getAudioId, audioId));
        return agentChatHistoryEntity == null ? null : agentChatHistoryEntity.getContent();
    }

    @Override
    public String getAgentIdByAudioId(String audioId) {
        if (audioId == null || audioId.isBlank()) {
            return null;
        }
        AgentChatHistoryEntity entity = baseMapper.selectOne(
                new LambdaQueryWrapper<AgentChatHistoryEntity>()
                        .select(AgentChatHistoryEntity::getAgentId)
                        .eq(AgentChatHistoryEntity::getAudioId, audioId)
                        .last("LIMIT 1"));
        return entity == null ? null : entity.getAgentId();
    }

    @Override
    public boolean isAudioOwnedByAgent(String audioId, String agentId) {
        // 查询是否有指定音频id和智能体id的数据，如果有且只有一条说明此数据属性此智能体
        Long row = baseMapper.selectCount(new LambdaQueryWrapper<AgentChatHistoryEntity>()
                .eq(AgentChatHistoryEntity::getAudioId, audioId)
                .eq(AgentChatHistoryEntity::getAgentId, agentId));
        return row == 1;
    }

    @Override
    public PageData<ChatRecordVO> getChatRecordPage(ChatRecordQueryDTO query, Long userId) {
        ChatRecordQueryDTO q = query == null ? new ChatRecordQueryDTO() : query;
        List<String> visibleAgentIds = resolveVisibleAgentIds(userId);
        if (visibleAgentIds != null && visibleAgentIds.isEmpty()) {
            return new PageData<>(Collections.emptyList(), 0);
        }

        int page = q.getPage() == null ? 1 : Math.max(1, q.getPage());
        int limit = q.getLimit() == null ? 10 : Math.min(100, Math.max(1, q.getLimit()));

        QueryWrapper<AgentChatHistoryEntity> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(q.getAgentId())) {
            // 指定智能体时仍需校验归属，防止越权查看他人记录（超管开启全库可见时跳过）
            if (visibleAgentIds != null && !visibleAgentIds.contains(q.getAgentId())) {
                return new PageData<>(Collections.emptyList(), 0);
            }
            wrapper.eq("agent_id", q.getAgentId());
        } else if (visibleAgentIds != null) {
            wrapper.in("agent_id", visibleAgentIds);
        }

        if (StringUtils.isNotBlank(q.getMacAddress())) {
            wrapper.eq("mac_address", q.getMacAddress().trim());
        }
        if (StringUtils.isNotBlank(q.getSessionId())) {
            wrapper.eq("session_id", q.getSessionId().trim());
        }
        if (q.getChatType() != null) {
            wrapper.eq("chat_type", q.getChatType());
        }
        if (StringUtils.isNotBlank(q.getKeyword())) {
            wrapper.like("content", q.getKeyword().trim());
        }
        if (q.getStartTime() != null) {
            wrapper.ge("created_at", toDbLiteral(q.getStartTime()));
        }
        if (q.getEndTime() != null) {
            wrapper.le("created_at", toDbLiteral(q.getEndTime()));
        }
        // UTC 字面值与北京时间只差固定 8 小时偏移，倒序即「中国时间最新在前」
        wrapper.orderByDesc("created_at").orderByDesc("id");

        Page<AgentChatHistoryEntity> pageParam = new Page<>(page, limit);
        IPage<AgentChatHistoryEntity> result = this.baseMapper.selectPage(pageParam, wrapper);
        List<AgentChatHistoryEntity> rows = result.getRecords();
        if (rows.isEmpty()) {
            return new PageData<>(Collections.emptyList(), result.getTotal());
        }

        Map<String, String> aliasByMac = loadDeviceAliases(rows);
        Map<String, String> agentNameById = loadAgentNames(rows);
        Map<String, String> titleBySession = loadSessionTitles(rows);
        Map<String, OwnerInfo> ownerByAgent = loadOwnerInfo(rows.stream()
                .map(AgentChatHistoryEntity::getAgentId)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet()));

        List<ChatRecordVO> list = rows.stream().map(entity -> {
            ChatRecordVO vo = new ChatRecordVO();
            vo.setId(entity.getId());
            long timeMs = entity.getCreatedAt() == null ? 0L : entity.getCreatedAt().getTime();
            vo.setCreatedAtMs(timeMs);
            vo.setCreatedAtText(timeMs == 0L ? ""
                    : Instant.ofEpochMilli(timeMs).atZone(CHINA_ZONE).format(CN_TIME_FORMAT));
            vo.setMacAddress(entity.getMacAddress());
            vo.setDeviceAlias(aliasByMac.get(entity.getMacAddress()));
            vo.setAgentId(entity.getAgentId());
            vo.setAgentName(agentNameById.get(entity.getAgentId()));
            OwnerInfo owner = ownerByAgent.get(entity.getAgentId());
            if (owner != null) {
                vo.setOwnerUsername(owner.getUsername());
                vo.setOwnerNickname(owner.getNickname());
                vo.setIsOwn(owner.getIsOwn());
            }
            vo.setSessionId(entity.getSessionId());
            vo.setSessionTitle(titleBySession.get(entity.getSessionId()));
            vo.setChatType(entity.getChatType());

            String content = entity.getContent();
            boolean truncated = content != null && content.length() > MAX_CONTENT_LENGTH;
            vo.setContent(truncated ? content.substring(0, MAX_CONTENT_LENGTH) : content);
            vo.setContentTruncated(truncated);
            vo.setHasAudio(StringUtils.isNotBlank(entity.getAudioId()));
            vo.setRoundId(entity.getRoundId());
            if (StringUtils.isNotBlank(entity.getMetricsJson())) {
                vo.setMetrics(JsonUtils.parseMap(entity.getMetricsJson()));
            }
            return vo;
        }).collect(Collectors.toList());

        return new PageData<>(list, result.getTotal());
    }

    @Override
    public PageData<ChatSessionVO> getChatSessionPage(ChatRecordQueryDTO query, Long userId) {
        ChatRecordQueryDTO httpQuery = query == null ? new ChatRecordQueryDTO() : query;
        List<String> visibleAgentIds = resolveVisibleAgentIds(userId);
        if (visibleAgentIds != null && visibleAgentIds.isEmpty()) {
            return new PageData<>(Collections.emptyList(), 0);
        }
        // 指定智能体时仍要校验归属，防止越权查看他人会话（超管开启全库可见时跳过）
        if (visibleAgentIds != null && StringUtils.isNotBlank(httpQuery.getAgentId())
                && !visibleAgentIds.contains(httpQuery.getAgentId())) {
            return new PageData<>(Collections.emptyList(), 0);
        }

        int page = httpQuery.getPage() == null ? 1 : Math.max(1, httpQuery.getPage());
        int limit = httpQuery.getLimit() == null ? 10 : Math.min(100, Math.max(1, httpQuery.getLimit()));

        ChatSessionQueryDTO q = new ChatSessionQueryDTO();
        // 会话列表与下拉选项统一走同一可见范围；null 表示超管已开启全库可见
        q.setVisibleAgentIds(visibleAgentIds);
        q.setMacAddress(trimToNull(httpQuery.getMacAddress()));
        q.setAgentId(trimToNull(httpQuery.getAgentId()));
        q.setKeyword(trimToNull(httpQuery.getKeyword()));
        q.setChatType(httpQuery.getChatType());
        if (httpQuery.getStartTime() != null) {
            q.setStartTime(toDbLiteral(httpQuery.getStartTime()));
        }
        if (httpQuery.getEndTime() != null) {
            q.setEndTime(toDbLiteral(httpQuery.getEndTime()));
        }
        q.setOffset((long) (page - 1) * limit);
        q.setLimit(limit);

        long total = baseMapper.countChatSessions(q);
        if (total == 0L) {
            return new PageData<>(Collections.emptyList(), 0L);
        }
        List<Map<String, Object>> rows = baseMapper.selectChatSessions(q);
        if (rows.isEmpty()) {
            return new PageData<>(Collections.emptyList(), total);
        }

        List<String> sessionIds = rows.stream()
                .map(row -> asString(row.get("session_id")))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        Set<String> macs = rows.stream()
                .map(row -> asString(row.get("mac_address")))
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        Set<String> agentIds = rows.stream()
                .map(row -> asString(row.get("agent_id")))
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());

        Map<String, String> titleBySession = findSessionTitles(sessionIds);
        Map<String, String> summaryBySession = findFirstContents(sessionIds);
        Map<String, String> aliasByMac = findDeviceAliases(macs);
        Map<String, String> nameByAgent = findAgentNames(agentIds);
        Map<String, OwnerInfo> ownerByAgent = loadOwnerInfo(agentIds);

        List<ChatSessionVO> list = rows.stream().map(row -> {
            ChatSessionVO vo = new ChatSessionVO();
            String sessionId = asString(row.get("session_id"));
            String mac = asString(row.get("mac_address"));
            String agentId = asString(row.get("agent_id"));
            vo.setSessionId(sessionId);
            vo.setSessionTitle(titleBySession.get(sessionId));
            vo.setMacAddress(mac);
            vo.setDeviceAlias(aliasByMac.get(mac));
            vo.setAgentId(agentId);
            vo.setAgentName(nameByAgent.get(agentId));
            OwnerInfo owner = ownerByAgent.get(agentId);
            if (owner != null) {
                vo.setOwnerUsername(owner.getUsername());
                vo.setOwnerNickname(owner.getNickname());
                vo.setIsOwn(owner.getIsOwn());
            }
            vo.setChatCount(asInt(row.get("chat_count")));

            Long startedMs = toEpochMs(row.get("started_at"));
            vo.setStartedAtMs(startedMs);
            vo.setStartedAtText(formatChinaTime(startedMs));
            Long lastMs = toEpochMs(row.get("last_at"));
            vo.setCreatedAtMs(lastMs);
            vo.setCreatedAtText(formatChinaTime(lastMs));
            vo.setSummary(summaryBySession.get(sessionId));
            vo.setHasAudio(asInt(row.get("has_audio")) > 0);
            return vo;
        }).collect(Collectors.toList());

        return new PageData<>(list, total);
    }

    @Override
    public ChatRecordFilterVO getChatRecordFilters(Long userId) {
        ChatRecordFilterVO result = new ChatRecordFilterVO();
        List<String> visibleAgentIds = resolveVisibleAgentIds(userId);
        boolean viewAll = visibleAgentIds == null;
        if (!viewAll && visibleAgentIds.isEmpty()) {
            return result;
        }

        // 智能体选项：与智能体管理页 /agent/list 使用同一可见范围
        QueryWrapper<AgentEntity> agentWrapper = new QueryWrapper<AgentEntity>()
                .select("id", "agent_name", "user_id")
                .orderByDesc("created_at");
        if (!viewAll) {
            agentWrapper.in("id", visibleAgentIds);
        }
        List<AgentEntity> agents = agentDao.selectList(agentWrapper);
        Map<Long, String> agentUsernameMap = userDataScopeService.getUsernameMap(
                agents.stream().map(AgentEntity::getUserId).collect(Collectors.toSet()));
        Map<Long, String> agentNicknameMap = userDataScopeService.getNicknameMap(
                agents.stream().map(AgentEntity::getUserId).collect(Collectors.toSet()));
        List<ChatRecordFilterVO.AgentOption> agentOptions = new ArrayList<>();
        for (AgentEntity agent : agents) {
            ChatRecordFilterVO.AgentOption option = new ChatRecordFilterVO.AgentOption();
            option.setAgentId(agent.getId());
            option.setAgentName(agent.getAgentName());
            option.setOwnerUsername(agentUsernameMap.get(agent.getUserId()));
            option.setOwnerNickname(agentNicknameMap.get(agent.getUserId()));
            option.setIsOwn(userDataScopeService.isOwn(agent.getUserId()));
            agentOptions.add(option);
        }
        result.setAgents(agentOptions);

        // 设备选项：按可见智能体过滤；超管开启全库可见时列出全部设备
        QueryWrapper<DeviceEntity> deviceWrapper = new QueryWrapper<DeviceEntity>()
                .select("mac_address", "alias", "agent_id", "user_id");
        if (!viewAll) {
            deviceWrapper.in("agent_id", visibleAgentIds);
        }
        List<DeviceEntity> devices = deviceDao.selectList(deviceWrapper);
        Map<Long, String> deviceUsernameMap = userDataScopeService.getUsernameMap(
                devices.stream().map(DeviceEntity::getUserId).collect(Collectors.toSet()));
        Map<Long, String> deviceNicknameMap = userDataScopeService.getNicknameMap(
                devices.stream().map(DeviceEntity::getUserId).collect(Collectors.toSet()));
        // 同一 MAC 理论上只应有一条设备记录，这里按 MAC 去重，避免下拉出现重复项
        Map<String, ChatRecordFilterVO.DeviceOption> uniqueDevices = new LinkedHashMap<>();
        for (DeviceEntity device : devices) {
            if (StringUtils.isBlank(device.getMacAddress())) {
                continue;
            }
            ChatRecordFilterVO.DeviceOption option = new ChatRecordFilterVO.DeviceOption();
            option.setMacAddress(device.getMacAddress());
            option.setAlias(device.getAlias());
            option.setAgentId(device.getAgentId());
            option.setOwnerUsername(deviceUsernameMap.get(device.getUserId()));
            option.setOwnerNickname(deviceNicknameMap.get(device.getUserId()));
            option.setIsOwn(userDataScopeService.isOwn(device.getUserId()));
            uniqueDevices.putIfAbsent(device.getMacAddress(), option);
        }
        result.setDevices(new ArrayList<>(uniqueDevices.values()));
        return result;
    }

    /**
     * 当前登录用户可见的智能体ID列表；返回 null 表示不限制（超管 + 参数开启）。
     * ai_agent_chat_history 本身没有 user_id，必须经 ai_agent.user_id 做归属过滤。
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

    /** 按智能体批量取归属信息，用于在会话列表/记录上标注「我的 / 其他用户 · 用户名」 */
    private Map<String, OwnerInfo> loadOwnerInfo(Set<String> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AgentEntity> agents = agentDao.selectList(new QueryWrapper<AgentEntity>()
                .select("id", "user_id").in("id", agentIds));
        Map<Long, String> usernameMap = userDataScopeService.getUsernameMap(
                agents.stream().map(AgentEntity::getUserId).collect(Collectors.toSet()));
        Map<Long, String> nicknameMap = userDataScopeService.getNicknameMap(
                agents.stream().map(AgentEntity::getUserId).collect(Collectors.toSet()));
        Map<String, OwnerInfo> map = new HashMap<>();
        for (AgentEntity agent : agents) {
            map.putIfAbsent(agent.getId(), new OwnerInfo(
                    usernameMap.get(agent.getUserId()),
                    nicknameMap.get(agent.getUserId()),
                    userDataScopeService.isOwn(agent.getUserId())));
        }
        return map;
    }

    private static class OwnerInfo {
        private final String username;
        private final String nickname;
        private final Boolean isOwn;

        OwnerInfo(String username, String nickname, Boolean isOwn) {
            this.username = username;
            this.nickname = nickname;
            this.isOwn = isOwn;
        }

        String getUsername() {
            return username;
        }

        String getNickname() {
            return nickname;
        }

        Boolean getIsOwn() {
            return isOwn;
        }
    }

    /**
     * epoch 毫秒 → 与库中 created_at 同口径的 UTC 字面值。
     * 前端按中国时间选择的起止时刻是绝对时刻，转成 UTC 字面值后即可直接比较 DATETIME 列。
     */
    private LocalDateTime toDbLiteral(long epochMs) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), DB_ZONE);
    }

    private Map<String, String> loadDeviceAliases(List<AgentChatHistoryEntity> rows) {
        Set<String> macs = rows.stream().map(AgentChatHistoryEntity::getMacAddress)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (macs.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DeviceEntity> devices = deviceDao.selectList(new QueryWrapper<DeviceEntity>()
                .select("mac_address", "alias").in("mac_address", macs));
        Map<String, String> map = new HashMap<>();
        for (DeviceEntity device : devices) {
            if (StringUtils.isNotBlank(device.getAlias())) {
                map.putIfAbsent(device.getMacAddress(), device.getAlias());
            }
        }
        return map;
    }

    private Map<String, String> loadAgentNames(List<AgentChatHistoryEntity> rows) {
        Set<String> ids = rows.stream().map(AgentChatHistoryEntity::getAgentId)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
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

    private Map<String, String> loadSessionTitles(List<AgentChatHistoryEntity> rows) {
        Set<String> sessionIds = rows.stream().map(AgentChatHistoryEntity::getSessionId)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AgentChatTitleEntity> titles = agentChatTitleDao.selectList(
                new QueryWrapper<AgentChatTitleEntity>()
                        .select("session_id", "title").in("session_id", sessionIds));
        Map<String, String> map = new HashMap<>();
        for (AgentChatTitleEntity title : titles) {
            if (StringUtils.isNotBlank(title.getTitle())) {
                map.putIfAbsent(title.getSessionId(), title.getTitle());
            }
        }
        return map;
    }

    // ---------------- 会话维度查询的辅助方法 ----------------

    private String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private int asInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    /**
     * 聚合查询返回的时间列，在不同 MyBatis/驱动组合下可能是 Timestamp、Date 或 LocalDateTime，
     * 统一转成 epoch 毫秒。JDBC 使用 serverTimezone=UTC，Timestamp#getTime 与实体里的
     * Date#getTime 口径一致，可直接交给 Asia/Shanghai 格式化。
     */
    private Long toEpochMs(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).getTime();
        }
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).atZone(DB_ZONE).toInstant().toEpochMilli();
        }
        return null;
    }

    private String formatChinaTime(Long epochMs) {
        return epochMs == null ? ""
                : Instant.ofEpochMilli(epochMs).atZone(CHINA_ZONE).format(CN_TIME_FORMAT);
    }

    private Map<String, String> findSessionTitles(List<String> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AgentChatTitleEntity> titles = agentChatTitleDao.selectList(
                new QueryWrapper<AgentChatTitleEntity>()
                        .select("session_id", "title").in("session_id", sessionIds));
        Map<String, String> map = new HashMap<>();
        for (AgentChatTitleEntity title : titles) {
            if (StringUtils.isNotBlank(title.getTitle())) {
                map.putIfAbsent(title.getSessionId(), title.getTitle());
            }
        }
        return map;
    }

    private Map<String, String> findDeviceAliases(Set<String> macs) {
        if (macs.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DeviceEntity> devices = deviceDao.selectList(new QueryWrapper<DeviceEntity>()
                .select("mac_address", "alias").in("mac_address", macs));
        Map<String, String> map = new HashMap<>();
        for (DeviceEntity device : devices) {
            if (StringUtils.isNotBlank(device.getAlias())) {
                map.putIfAbsent(device.getMacAddress(), device.getAlias());
            }
        }
        return map;
    }

    private Map<String, String> findAgentNames(Set<String> ids) {
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

    /** 批量取每个会话首条消息正文，作为列表摘要（一次查询，避免 N+1） */
    private Map<String, String> findFirstContents(List<String> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        ChatSessionQueryDTO q = new ChatSessionQueryDTO();
        q.setSessionIds(sessionIds);
        Map<String, String> map = new HashMap<>();
        for (Map<String, Object> row : baseMapper.selectFirstContents(q)) {
            String sessionId = asString(row.get("session_id"));
            if (StringUtils.isBlank(sessionId)) {
                continue;
            }
            map.putIfAbsent(sessionId, toSummary(asString(row.get("content"))));
        }
        return map;
    }

    // ---------------- 会话分析看板 ----------------

    /** 分析明细的最大读取行数，防止超大表担爆内存 */
    private static final int ANALYTICS_MAX_ROWS = 200000;

    /** 有效/等待时长堆叠图展示的会话条数（按总跨度倒序取前 N） */
    private static final int TOP_SESSION_LIMIT = 15;

    private static final int SCATTER_LIMIT = 300;

    private static final DateTimeFormatter CN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public ChatAnalyticsVO getChatAnalytics(ChatRecordQueryDTO query, Long userId) {
        ChatAnalyticsVO result = new ChatAnalyticsVO();
        // 24 小时固定补齐，前端不用处理空洞
        List<Integer> hourly = new ArrayList<>(Collections.nCopies(24, 0));
        result.setHourly(hourly);

        ChatRecordQueryDTO httpQuery = query == null ? new ChatRecordQueryDTO() : query;
        List<String> visibleAgentIds = resolveVisibleAgentIds(userId);
        if (visibleAgentIds != null && visibleAgentIds.isEmpty()) {
            return result;
        }
        // 指定智能体时仍要校验归属，防止越权查看他人数据（与会话列表一致）
        if (visibleAgentIds != null && StringUtils.isNotBlank(httpQuery.getAgentId())
                && !visibleAgentIds.contains(httpQuery.getAgentId())) {
            return result;
        }

        ChatSessionQueryDTO q = new ChatSessionQueryDTO();
        q.setVisibleAgentIds(visibleAgentIds);
        q.setMacAddress(trimToNull(httpQuery.getMacAddress()));
        q.setAgentId(trimToNull(httpQuery.getAgentId()));
        if (httpQuery.getStartTime() != null) {
            q.setStartTime(toDbLiteral(httpQuery.getStartTime()));
        }
        if (httpQuery.getEndTime() != null) {
            q.setEndTime(toDbLiteral(httpQuery.getEndTime()));
        }
        q.setLimit(ANALYTICS_MAX_ROWS);

        List<Map<String, Object>> rows = baseMapper.selectAnalyticsRows(q);
        if (rows.isEmpty()) {
            return result;
        }

        // 一次遍历按会话归组（rows 已按 created_at 升序）
        Map<String, SessionAgg> sessionMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String sessionId = asString(row.get("session_id"));
            Long ms = toEpochMs(row.get("created_at"));
            if (StringUtils.isBlank(sessionId) || ms == null) {
                continue;
            }
            SessionAgg agg = sessionMap.computeIfAbsent(sessionId, SessionAgg::new);
            agg.macAddress = StringUtils.defaultIfBlank(agg.macAddress, asString(row.get("mac_address")));
            agg.agentId = StringUtils.defaultIfBlank(agg.agentId, asString(row.get("agent_id")));
            agg.firstMs = agg.firstMs == null ? ms : Math.min(agg.firstMs, ms);
            agg.lastMs = agg.lastMs == null ? ms : Math.max(agg.lastMs, ms);
            agg.messages++;
            if (asInt(row.get("chat_type")) == AgentChatHistoryType.USER.getValue()) {
                agg.userMessages++;
            }
            // 耗时快照只在每轮末条回复上，累加各轮 e2eTotalMs 即「有效时长」
            String metricsJson = asString(row.get("metrics_json"));
            if (StringUtils.isNotBlank(metricsJson)) {
                Map<String, Object> metrics = JsonUtils.parseMap(metricsJson);
                if (metrics != null) {
                    long e2e = asLong(metrics.get("e2eTotalMs"));
                    if (e2e > 0) {
                        agg.activeMs += e2e;
                        agg.metricRounds++;
                    }
                    long firstAudio = asLong(metrics.get("e2eFirstAudioMs"));
                    if (firstAudio > 0) {
                        agg.firstAudioSumMs += firstAudio;
                        agg.firstAudioCount++;
                    }
                }
            }
        }
        if (sessionMap.isEmpty()) {
            return result;
        }

        Set<String> macs = sessionMap.values().stream().map(a -> a.macAddress)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        Set<String> agentIds = sessionMap.values().stream().map(a -> a.agentId)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        Map<String, String> aliasByMac = findDeviceAliases(macs);
        Map<String, String> nameByAgent = findAgentNames(agentIds);
        Map<String, String> titleBySession = findSessionTitles(new ArrayList<>(sessionMap.keySet()));

        ChatAnalyticsVO.Summary summary = result.getSummary();
        Map<String, long[]> dailyMap = new java.util.TreeMap<>();
        Map<String, ChatAnalyticsVO.DeviceItem> deviceMap = new LinkedHashMap<>();
        Map<String, ChatAnalyticsVO.AgentItem> agentMap = new LinkedHashMap<>();
        int[][] weekHour = new int[7][24];
        long[] durationBuckets = new long[DURATION_LABELS.length];
        long[] roundBuckets = new long[ROUND_LABELS.length];
        List<ChatAnalyticsVO.SessionDuration> allDurations = new ArrayList<>();
        List<ChatAnalyticsVO.ScatterItem> scatter = new ArrayList<>();
        long firstAudioSum = 0;
        long firstAudioCount = 0;
        long spanSecondsTotal = 0;
        Long minMs = null;
        Long maxMs = null;

        for (SessionAgg agg : sessionMap.values()) {
            long spanSeconds = Math.max(0L, (agg.lastMs - agg.firstMs) / 1000);
            long activeSeconds = Math.min(agg.activeMs / 1000, spanSeconds);
            long waitSeconds = Math.max(0L, spanSeconds - activeSeconds);
            long rounds = agg.userMessages;
            String deviceName = StringUtils.defaultIfBlank(aliasByMac.get(agg.macAddress),
                    StringUtils.defaultString(agg.macAddress));

            summary.setSessionCount(summary.getSessionCount() + 1);
            summary.setMessageCount(summary.getMessageCount() + agg.messages);
            summary.setUserMessageCount(summary.getUserMessageCount() + agg.userMessages);
            spanSecondsTotal += spanSeconds;
            summary.setActiveSeconds(summary.getActiveSeconds() + activeSeconds);
            summary.setWaitSeconds(summary.getWaitSeconds() + waitSeconds);
            if (agg.metricRounds > 0) {
                summary.setMetricSessionCount(summary.getMetricSessionCount() + 1);
            }
            firstAudioSum += agg.firstAudioSumMs;
            firstAudioCount += agg.firstAudioCount;
            minMs = minMs == null ? agg.firstMs : Math.min(minMs, agg.firstMs);
            maxMs = maxMs == null ? agg.lastMs : Math.max(maxMs, agg.lastMs);

            // 会话开始时刻（中国时间）→ 按天 / 小时 / 星期 x 小时
            ZonedDateTime start = Instant.ofEpochMilli(agg.firstMs).atZone(CHINA_ZONE);
            long[] day = dailyMap.computeIfAbsent(start.format(CN_DATE_FORMAT), k -> new long[2]);
            day[0]++;
            day[1] += agg.messages;
            hourly.set(start.getHour(), hourly.get(start.getHour()) + 1);
            weekHour[start.getDayOfWeek().getValue() - 1][start.getHour()]++;

            if (StringUtils.isNotBlank(agg.macAddress)) {
                ChatAnalyticsVO.DeviceItem device = deviceMap.computeIfAbsent(agg.macAddress, k -> {
                    ChatAnalyticsVO.DeviceItem item = new ChatAnalyticsVO.DeviceItem();
                    item.setMacAddress(k);
                    item.setName(deviceName);
                    return item;
                });
                device.setSessions(device.getSessions() + 1);
                device.setMessages(device.getMessages() + agg.messages);
                device.setActiveSeconds(device.getActiveSeconds() + activeSeconds);
                device.setWaitSeconds(device.getWaitSeconds() + waitSeconds);
            }
            if (StringUtils.isNotBlank(agg.agentId)) {
                ChatAnalyticsVO.AgentItem agent = agentMap.computeIfAbsent(agg.agentId, k -> {
                    ChatAnalyticsVO.AgentItem item = new ChatAnalyticsVO.AgentItem();
                    item.setAgentId(k);
                    item.setName(StringUtils.defaultIfBlank(nameByAgent.get(k), k));
                    return item;
                });
                agent.setSessions(agent.getSessions() + 1);
                agent.setMessages(agent.getMessages() + agg.messages);
            }

            durationBuckets[bucketIndex(spanSeconds, DURATION_UPPER)]++;
            roundBuckets[bucketIndex(rounds, ROUND_UPPER)]++;

            ChatAnalyticsVO.SessionDuration duration = new ChatAnalyticsVO.SessionDuration();
            duration.setSessionId(agg.sessionId);
            duration.setTitle(StringUtils.defaultIfBlank(titleBySession.get(agg.sessionId),
                    agg.sessionId.length() > 8 ? agg.sessionId.substring(0, 8) : agg.sessionId));
            duration.setDeviceName(deviceName);
            duration.setActiveSeconds(activeSeconds);
            duration.setWaitSeconds(waitSeconds);
            duration.setRounds(rounds);
            allDurations.add(duration);

            if (scatter.size() < SCATTER_LIMIT) {
                ChatAnalyticsVO.ScatterItem point = new ChatAnalyticsVO.ScatterItem();
                point.setRounds(rounds);
                point.setSpanSeconds(spanSeconds);
                point.setTitle(duration.getTitle());
                scatter.add(point);
            }
        }

        long sessions = summary.getSessionCount();
        summary.setDeviceCount(deviceMap.size());
        summary.setAgentCount(agentMap.size());
        summary.setAvgMessages(round1(summary.getMessageCount() * 1.0 / sessions));
        summary.setAvgRounds(round1(summary.getUserMessageCount() * 1.0 / sessions));
        summary.setAvgSpanSeconds(round1(spanSecondsTotal * 1.0 / sessions));
        summary.setAvgFirstAudioMs(firstAudioCount == 0 ? 0 : round1(firstAudioSum * 1.0 / firstAudioCount));
        summary.setFirstTime(formatChinaTime(minMs));
        summary.setLastTime(formatChinaTime(maxMs));

        dailyMap.forEach((date, v) -> {
            ChatAnalyticsVO.DailyItem item = new ChatAnalyticsVO.DailyItem();
            item.setDate(date);
            item.setSessions(v[0]);
            item.setMessages(v[1]);
            result.getDaily().add(item);
        });
        deviceMap.values().stream()
                .sorted((a, b) -> Long.compare(b.getSessions(), a.getSessions()))
                .forEach(result.getDevices()::add);
        agentMap.values().stream()
                .sorted((a, b) -> Long.compare(b.getSessions(), a.getSessions()))
                .forEach(result.getAgents()::add);

        for (int d = 0; d < 7; d++) {
            for (int h = 0; h < 24; h++) {
                if (weekHour[d][h] > 0) {
                    result.getWeekHour().add(java.util.Arrays.asList(h, d, weekHour[d][h]));
                }
            }
        }
        for (int i = 0; i < DURATION_LABELS.length; i++) {
            ChatAnalyticsVO.BucketItem item = new ChatAnalyticsVO.BucketItem();
            item.setLabel(DURATION_LABELS[i]);
            item.setCount(durationBuckets[i]);
            result.getDurationBuckets().add(item);
        }
        for (int i = 0; i < ROUND_LABELS.length; i++) {
            ChatAnalyticsVO.BucketItem item = new ChatAnalyticsVO.BucketItem();
            item.setLabel(ROUND_LABELS[i]);
            item.setCount(roundBuckets[i]);
            result.getRoundBuckets().add(item);
        }
        allDurations.stream()
                .sorted((a, b) -> Long.compare(b.getActiveSeconds() + b.getWaitSeconds(),
                        a.getActiveSeconds() + a.getWaitSeconds()))
                .limit(TOP_SESSION_LIMIT)
                .forEach(result.getTopSessions()::add);
        result.setScatter(scatter);
        return result;
    }

    /** 会话总跨度分桶（秒）上界，与 DURATION_LABELS 一一对应，最后一桶为开区间 */
    private static final long[] DURATION_UPPER = { 10, 30, 60, 180, 600, 1800 };
    private static final String[] DURATION_LABELS = { "<10s", "10-30s", "30s-1m", "1-3m", "3-10m", "10-30m", ">30m" };

    /** 会话轮数分桶上界（轮） */
    private static final long[] ROUND_UPPER = { 1, 2, 3, 5, 10, 20 };
    private static final String[] ROUND_LABELS = { "1", "2", "3", "4-5", "6-10", "11-20", ">20" };

    /** 返回 value 落入的桶下标：value <= upper[i] 即落入第 i 桶，超过全部上界落入最后一桶 */
    private int bucketIndex(long value, long[] upper) {
        for (int i = 0; i < upper.length; i++) {
            if (value <= upper[i]) {
                return i;
            }
        }
        return upper.length;
    }

    private long asLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // ---------------- 会话内容分析（词云） ----------------

    /** 词云最多读取的消息条数（最新优先），分词是 CPU 密集型，防止大表拖垮服务 */
    private static final int WORD_CLOUD_MAX_ROWS = 20000;

    /** 单条消息参与分词的最大字符数，避免异常超长正文占用过多时间 */
    private static final int WORD_CLOUD_MAX_TEXT_LENGTH = 2000;

    @Override
    public ChatWordCloudVO getChatWordCloud(ChatRecordQueryDTO query, Long userId, int limit) {
        ChatWordCloudVO result = new ChatWordCloudVO();
        int topN = Math.min(200, Math.max(10, limit));

        ChatRecordQueryDTO httpQuery = query == null ? new ChatRecordQueryDTO() : query;
        List<String> visibleAgentIds = resolveVisibleAgentIds(userId);
        if (visibleAgentIds != null && visibleAgentIds.isEmpty()) {
            return result;
        }
        // 指定智能体时仍要校验归属，防止越权查看他人数据（与会话列表一致）
        if (visibleAgentIds != null && StringUtils.isNotBlank(httpQuery.getAgentId())
                && !visibleAgentIds.contains(httpQuery.getAgentId())) {
            return result;
        }

        ChatSessionQueryDTO q = new ChatSessionQueryDTO();
        q.setVisibleAgentIds(visibleAgentIds);
        q.setMacAddress(trimToNull(httpQuery.getMacAddress()));
        q.setAgentId(trimToNull(httpQuery.getAgentId()));
        // 角色：只允许指定用户(1)/智能体(2)；其他取值（含工具 3）一律按「用户+智能体」处理，避免工具 JSON 混入词云
        Integer chatType = httpQuery.getChatType();
        if (chatType != null && (chatType == 1 || chatType == 2)) {
            q.setChatTypes(Collections.singletonList(chatType));
        } else {
            q.setChatTypes(java.util.Arrays.asList(1, 2));
        }
        if (httpQuery.getStartTime() != null) {
            q.setStartTime(toDbLiteral(httpQuery.getStartTime()));
        }
        if (httpQuery.getEndTime() != null) {
            q.setEndTime(toDbLiteral(httpQuery.getEndTime()));
        }
        q.setLimit(WORD_CLOUD_MAX_ROWS);

        List<Map<String, Object>> rows = baseMapper.selectContentRows(q);
        ChatWordCloudVO.Summary summary = result.getSummary();
        summary.setTruncated(rows.size() >= WORD_CLOUD_MAX_ROWS);
        if (rows.isEmpty()) {
            return result;
        }

        Map<String, long[]> all = new HashMap<>();
        Map<String, long[]> user = new HashMap<>();
        Map<String, long[]> agent = new HashMap<>();
        long totalWords = 0;
        for (Map<String, Object> row : rows) {
            String text = extractContentFromString(asString(row.get("content")));
            if (StringUtils.isBlank(text)) {
                continue;
            }
            if (text.length() > WORD_CLOUD_MAX_TEXT_LENGTH) {
                text = text.substring(0, WORD_CLOUD_MAX_TEXT_LENGTH);
            }
            boolean isUser = asInt(row.get("chat_type")) == AgentChatHistoryType.USER.getValue();
            List<String> tokens = chatTextTokenizer.tokenize(text);
            summary.setMessageCount(summary.getMessageCount() + 1);
            if (isUser) {
                summary.setUserMessageCount(summary.getUserMessageCount() + 1);
            } else {
                summary.setAgentMessageCount(summary.getAgentMessageCount() + 1);
            }
            for (String token : tokens) {
                all.computeIfAbsent(token, k -> new long[1])[0]++;
                (isUser ? user : agent).computeIfAbsent(token, k -> new long[1])[0]++;
                totalWords++;
            }
        }
        summary.setTotalWords(totalWords);
        summary.setDistinctWords(all.size());
        result.setWords(topWords(all, topN));
        result.setUserWords(topWords(user, topN));
        result.setAgentWords(topWords(agent, topN));
        return result;
    }

    /** 按词频倒序取前 N；词频相同时按词本身排序，保证结果稳定、可测试 */
    private List<ChatWordCloudVO.WordItem> topWords(Map<String, long[]> counter, int topN) {
        return counter.entrySet().stream()
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getValue()[0], a.getValue()[0]);
                    return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                })
                .limit(topN)
                .map(e -> new ChatWordCloudVO.WordItem(e.getKey(), e.getValue()[0]))
                .collect(Collectors.toList());
    }

    /** 单个会话的聚合中间态 */
    private static class SessionAgg {
        private final String sessionId;
        private String macAddress;
        private String agentId;
        private Long firstMs;
        private Long lastMs;
        private long messages;
        private long userMessages;
        private long activeMs;
        private long metricRounds;
        private long firstAudioSumMs;
        private long firstAudioCount;

        SessionAgg(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    /** 兼容 {"content": "..."} 与普通文本；压缩空白后截断，避免工具调用 JSON 撑爆列表 */
    private String toSummary(String content) {
        if (StringUtils.isBlank(content)) {
            return "";
        }
        String text = extractContentFromString(content);
        if (text == null) {
            text = content;
        }
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > 120 ? text.substring(0, 120) + "..." : text;
    }
}
