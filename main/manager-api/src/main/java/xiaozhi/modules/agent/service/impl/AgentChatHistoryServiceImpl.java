package xiaozhi.modules.agent.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
import xiaozhi.modules.agent.entity.AgentChatHistoryEntity;
import xiaozhi.modules.agent.entity.AgentChatTitleEntity;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.agent.service.AgentChatHistoryService;
import xiaozhi.modules.agent.service.AgentChatTitleService;
import xiaozhi.modules.agent.vo.AgentChatHistoryUserVO;
import xiaozhi.modules.agent.vo.ChatRecordFilterVO;
import xiaozhi.modules.agent.vo.ChatRecordVO;
import xiaozhi.modules.device.dao.DeviceDao;
import xiaozhi.modules.device.entity.DeviceEntity;

/**
 * 智能体聊天记录表处理service {@link AgentChatHistoryService} impl
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

        // 查询聊天记录
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
    public PageData<ChatRecordVO> getChatRecordPage(ChatRecordQueryDTO query, Long userId, boolean superAdmin) {
        ChatRecordQueryDTO q = query == null ? new ChatRecordQueryDTO() : query;
        List<String> visibleAgentIds = resolveVisibleAgentIds(userId, superAdmin);
        if (!superAdmin && visibleAgentIds.isEmpty()) {
            return new PageData<>(Collections.emptyList(), 0);
        }

        int page = q.getPage() == null ? 1 : Math.max(1, q.getPage());
        int limit = q.getLimit() == null ? 10 : Math.min(100, Math.max(1, q.getLimit()));

        QueryWrapper<AgentChatHistoryEntity> wrapper = new QueryWrapper<>();
        if (!superAdmin) {
            if (StringUtils.isNotBlank(q.getAgentId())) {
                // 指定智能体时仍需校验归属，防止越权查看他人记录
                if (!visibleAgentIds.contains(q.getAgentId())) {
                    return new PageData<>(Collections.emptyList(), 0);
                }
                wrapper.eq("agent_id", q.getAgentId());
            } else {
                wrapper.in("agent_id", visibleAgentIds);
            }
        } else if (StringUtils.isNotBlank(q.getAgentId())) {
            wrapper.eq("agent_id", q.getAgentId());
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
    public ChatRecordFilterVO getChatRecordFilters(Long userId, boolean superAdmin) {
        ChatRecordFilterVO result = new ChatRecordFilterVO();
        List<String> visibleAgentIds = resolveVisibleAgentIds(userId, superAdmin);
        if (!superAdmin && visibleAgentIds.isEmpty()) {
            return result;
        }

        QueryWrapper<AgentEntity> agentWrapper = new QueryWrapper<AgentEntity>()
                .select("id", "agent_name").orderByDesc("created_at");
        if (!superAdmin) {
            agentWrapper.in("id", visibleAgentIds);
        }
        List<AgentEntity> agents = agentDao.selectList(agentWrapper);
        result.setAgents(agents.stream()
                .map(item -> new ChatRecordFilterVO.AgentOption(item.getId(), item.getAgentName()))
                .collect(Collectors.toList()));

        QueryWrapper<DeviceEntity> deviceWrapper = new QueryWrapper<DeviceEntity>()
                .select("mac_address", "alias", "agent_id");
        if (!superAdmin) {
            deviceWrapper.in("agent_id", visibleAgentIds);
        }
        // 同一 MAC 理论上只应有一条设备记录，这里按 MAC 去重，避免下拉出现重复项
        Map<String, ChatRecordFilterVO.DeviceOption> uniqueDevices = new LinkedHashMap<>();
        for (DeviceEntity device : deviceDao.selectList(deviceWrapper)) {
            if (StringUtils.isBlank(device.getMacAddress())) {
                continue;
            }
            uniqueDevices.putIfAbsent(device.getMacAddress(),
                    new ChatRecordFilterVO.DeviceOption(
                            device.getMacAddress(), device.getAlias(), device.getAgentId()));
        }
        result.setDevices(new ArrayList<>(uniqueDevices.values()));
        return result;
    }

    /**
     * 普通用户可见的智能体ID列表；超管返回空列表（调用方按 superAdmin 跳过限制）。
     * ai_agent_chat_history 本身没有 user_id，必须经 ai_agent.user_id 做归属过滤。
     */
    private List<String> resolveVisibleAgentIds(Long userId, boolean superAdmin) {
        if (superAdmin || userId == null) {
            return Collections.emptyList();
        }
        List<AgentEntity> agents = agentDao.selectList(new QueryWrapper<AgentEntity>()
                .select("id").eq("user_id", userId));
        return agents.stream().map(AgentEntity::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
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
}
