package xiaozhi.modules.agent.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import xiaozhi.modules.agent.dao.AgentChatTitleDao;
import xiaozhi.modules.agent.dao.AgentDao;
import xiaozhi.modules.agent.dao.AiAgentChatHistoryDao;
import xiaozhi.modules.agent.dto.ChatRecordQueryDTO;
import xiaozhi.modules.agent.dto.ChatSessionQueryDTO;
import xiaozhi.modules.agent.service.AgentChatTitleService;
import xiaozhi.modules.agent.service.ChatTextTokenizer;
import xiaozhi.modules.agent.service.ChatWakeupWordFilter;
import xiaozhi.modules.agent.vo.ChatAnalyticsVO;
import xiaozhi.modules.device.dao.DeviceDao;
import xiaozhi.modules.sys.service.UserDataScopeService;

@DisplayName("会话分析看板聚合逻辑测试")
class AgentChatHistoryAnalyticsTest {

    private static final long USER_ID = 1L;

    /** 库中 created_at 保存的是 UTC 墙上时间，这里按 UTC 构造 */
    private static Timestamp utc(int month, int day, int hour, int minute, int second) {
        return Timestamp.from(LocalDateTime.of(2026, month, day, hour, minute, second)
                .toInstant(ZoneOffset.UTC));
    }

    private static Map<String, Object> row(String session, String mac, String agent, int chatType,
            Timestamp at, String metricsJson) {
        Map<String, Object> map = new HashMap<>();
        map.put("session_id", session);
        map.put("mac_address", mac);
        map.put("agent_id", agent);
        map.put("chat_type", chatType);
        map.put("created_at", at);
        map.put("metrics_json", metricsJson);
        return map;
    }

    private AgentChatHistoryServiceImpl service(AiAgentChatHistoryDao dao, UserDataScopeService scope,
            AgentDao agentDao) {
        AgentChatHistoryServiceImpl service = new AgentChatHistoryServiceImpl(
                mock(AgentChatTitleService.class), agentDao, mock(AgentChatTitleDao.class),
                mock(DeviceDao.class), scope, mock(ChatTextTokenizer.class), mock(ChatWakeupWordFilter.class));
        ReflectionTestUtils.setField(service, "baseMapper", dao);
        return service;
    }

    private UserDataScopeService viewAllScope() {
        UserDataScopeService scope = mock(UserDataScopeService.class);
        when(scope.canViewAllUserData()).thenReturn(true);
        return scope;
    }

    @Test
    @DisplayName("一个会话：有效时长取轮次耗时之和，等待时长 = 跨度 - 有效时长")
    void activeAndWaitSecondsAreDerivedFromSpanAndMetrics() {
        AiAgentChatHistoryDao dao = mock(AiAgentChatHistoryDao.class);
        List<Map<String, Object>> rows = new ArrayList<>();
        // 跨度 100 秒：首条用户消息 -> 100 秒后末条回复（带 e2eTotalMs=30000）
        rows.add(row("s1", "AA", "ag1", 1, utc(11, 19, 2, 0, 0), null));
        rows.add(row("s1", "AA", "ag1", 2, utc(11, 19, 2, 1, 40),
                "{\"e2eTotalMs\":30000,\"e2eFirstAudioMs\":800}"));
        when(dao.selectAnalyticsRows(any(ChatSessionQueryDTO.class))).thenReturn(rows);

        ChatAnalyticsVO vo = service(dao, viewAllScope(), mock(AgentDao.class))
                .getChatAnalytics(new ChatRecordQueryDTO(), USER_ID);

        assertEquals(1, vo.getSummary().getSessionCount());
        assertEquals(2, vo.getSummary().getMessageCount());
        assertEquals(1, vo.getSummary().getUserMessageCount());
        assertEquals(30, vo.getSummary().getActiveSeconds());
        assertEquals(70, vo.getSummary().getWaitSeconds());
        assertEquals(1, vo.getSummary().getMetricSessionCount());
        assertEquals(800.0, vo.getSummary().getAvgFirstAudioMs());
        // 100 秒落在 "1-3m" 桶（下标 3）
        assertEquals("1-3m", vo.getDurationBuckets().get(3).getLabel());
        assertEquals(1, vo.getDurationBuckets().get(3).getCount());
    }

    @Test
    @DisplayName("有效时长不会超过会话总跨度，等待时长不会为负")
    void activeIsCappedBySpan() {
        AiAgentChatHistoryDao dao = mock(AiAgentChatHistoryDao.class);
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("s1", "AA", "ag1", 1, utc(11, 19, 2, 0, 0), null));
        // 跨度只有 10 秒，但快照上报 60 秒
        rows.add(row("s1", "AA", "ag1", 2, utc(11, 19, 2, 0, 10), "{\"e2eTotalMs\":60000}"));
        when(dao.selectAnalyticsRows(any(ChatSessionQueryDTO.class))).thenReturn(rows);

        ChatAnalyticsVO vo = service(dao, viewAllScope(), mock(AgentDao.class))
                .getChatAnalytics(new ChatRecordQueryDTO(), USER_ID);

        assertEquals(10, vo.getSummary().getActiveSeconds());
        assertEquals(0, vo.getSummary().getWaitSeconds());
    }

    @Test
    @DisplayName("时段分布按中国时间统计：UTC 02:00 应落在北京时间 10 点")
    void hourlyDistributionUsesChinaTime() {
        AiAgentChatHistoryDao dao = mock(AiAgentChatHistoryDao.class);
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("s1", "AA", "ag1", 1, utc(11, 19, 2, 0, 0), null));
        // UTC 20:00 -> 北京时间次日 04:00，验证跨天
        rows.add(row("s2", "AA", "ag1", 1, utc(11, 19, 20, 0, 0), null));
        when(dao.selectAnalyticsRows(any(ChatSessionQueryDTO.class))).thenReturn(rows);

        ChatAnalyticsVO vo = service(dao, viewAllScope(), mock(AgentDao.class))
                .getChatAnalytics(new ChatRecordQueryDTO(), USER_ID);

        assertEquals(24, vo.getHourly().size());
        assertEquals(1, vo.getHourly().get(10));
        assertEquals(1, vo.getHourly().get(4));
        assertEquals(2, vo.getDaily().size());
        assertEquals("2026-11-19", vo.getDaily().get(0).getDate());
        assertEquals("2026-11-20", vo.getDaily().get(1).getDate());
    }

    @Test
    @DisplayName("轮数分桶边界：1 轮、五轮、六轮分别落入不同桶")
    void roundBucketsRespectBoundaries() {
        AiAgentChatHistoryDao dao = mock(AiAgentChatHistoryDao.class);
        List<Map<String, Object>> rows = new ArrayList<>();
        addUserRounds(rows, "one", 1);
        addUserRounds(rows, "five", 5);
        addUserRounds(rows, "six", 6);
        when(dao.selectAnalyticsRows(any(ChatSessionQueryDTO.class))).thenReturn(rows);

        ChatAnalyticsVO vo = service(dao, viewAllScope(), mock(AgentDao.class))
                .getChatAnalytics(new ChatRecordQueryDTO(), USER_ID);

        // 桶: 1,2,3,4-5,6-10,11-20,>20
        assertEquals(1, vo.getRoundBuckets().get(0).getCount());
        assertEquals(1, vo.getRoundBuckets().get(3).getCount());
        assertEquals(1, vo.getRoundBuckets().get(4).getCount());
        assertEquals(3, vo.getSummary().getSessionCount());
    }

    private void addUserRounds(List<Map<String, Object>> rows, String session, int rounds) {
        for (int i = 0; i < rounds; i++) {
            rows.add(row(session, "AA", "ag1", 1, utc(11, 19, 2, 0, i), null));
        }
    }

    @Test
    @DisplayName("普通用户没有任何智能体时直接返回空结果，且不查明细")
    void userWithoutAgentsGetsEmptyResultWithoutQuery() {
        AiAgentChatHistoryDao dao = mock(AiAgentChatHistoryDao.class);
        UserDataScopeService scope = mock(UserDataScopeService.class);
        when(scope.canViewAllUserData()).thenReturn(false);
        AgentDao agentDao = mock(AgentDao.class);
        when(agentDao.selectList(any())).thenReturn(Collections.emptyList());

        ChatAnalyticsVO vo = service(dao, scope, agentDao)
                .getChatAnalytics(new ChatRecordQueryDTO(), USER_ID);

        assertEquals(0, vo.getSummary().getSessionCount());
        assertEquals(24, vo.getHourly().size());
        assertTrue(vo.getDaily().isEmpty());
        verify(dao, never()).selectAnalyticsRows(any());
    }
}
