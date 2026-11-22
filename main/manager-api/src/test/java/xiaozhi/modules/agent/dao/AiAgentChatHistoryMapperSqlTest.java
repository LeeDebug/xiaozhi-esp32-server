package xiaozhi.modules.agent.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xiaozhi.modules.agent.dto.ChatSessionQueryDTO;

/**
 * 不连数据库，直接让 MyBatis 解析 mapper XML 并渲染 SQL，验证动态 SQL 拼接正确。
 * 解决「DAO 被 mock 后 XML 从未真正执行」的盲区：标签写错、参数属性不存在、foreach 语法错误都会在这里暴露。
 */
@DisplayName("会话记录 mapper 动态 SQL 渲染测试")
class AiAgentChatHistoryMapperSqlTest {

    private static final String NAMESPACE = "xiaozhi.modules.agent.dao.AiAgentChatHistoryDao";

    private Configuration load() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/agent/AiAgentChatHistoryDao.xml";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    private String sql(String id, ChatSessionQueryDTO q) throws Exception {
        MappedStatement statement = load().getMappedStatement(NAMESPACE + "." + id);
        Map<String, Object> param = new HashMap<>();
        param.put("q", q);
        BoundSql bound = statement.getBoundSql(param);
        return bound.getSql().replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("无任何筛选条件时渲染出完整、不带 WHERE 的词云查询")
    void contentRowsWithoutFilters() throws Exception {
        ChatSessionQueryDTO q = new ChatSessionQueryDTO();
        q.setLimit(100);

        String sql = sql("selectContentRows", q);

        assertTrue(sql.contains("FROM ai_agent_chat_history m"), sql);
        assertFalse(sql.contains("WHERE"), sql);
        assertTrue(sql.contains("ORDER BY m.created_at DESC, m.id DESC LIMIT ?"), sql);
    }

    @Test
    @DisplayName("chatTypes 渲染成 IN (?,?)，且与可见智能体、设备、时间条件用 AND 正确连接")
    void contentRowsWithAllFilters() throws Exception {
        ChatSessionQueryDTO q = new ChatSessionQueryDTO();
        q.setVisibleAgentIds(Arrays.asList("a1", "a2"));
        q.setAgentId("a1");
        q.setMacAddress("AA:BB");
        q.setChatTypes(Arrays.asList(1, 2));
        q.setStartTime(LocalDateTime.of(2026, 11, 1, 0, 0));
        q.setEndTime(LocalDateTime.of(2026, 11, 20, 0, 0));
        q.setLimit(100);

        String sql = sql("selectContentRows", q);

        assertTrue(sql.contains("WHERE"), sql);
        assertTrue(sql.contains("m.agent_id IN (?,?)"), sql);
        assertTrue(sql.contains("AND m.agent_id = ?"), sql);
        assertTrue(sql.contains("AND m.mac_address = ?"), sql);
        assertTrue(sql.contains("AND m.chat_type IN (?,?)"), sql);
        assertTrue(sql.contains("m.created_at >= ?"), sql);
        assertTrue(sql.contains("m.created_at <= ?"), sql);
        // 首个条件前的多余 AND 会被 <where> 自动去掉，不应出现 "WHERE AND"
        assertFalse(sql.contains("WHERE AND"), sql);
    }

    @Test
    @DisplayName("只有 chatTypes 一个条件时，WHERE 后不带多余的 AND")
    void contentRowsOnlyChatTypes() throws Exception {
        ChatSessionQueryDTO q = new ChatSessionQueryDTO();
        q.setChatTypes(Collections.singletonList(1));
        q.setLimit(100);

        String sql = sql("selectContentRows", q);

        assertTrue(sql.contains("WHERE m.chat_type IN (?)"), sql);
    }

    @Test
    @DisplayName("matchSessionIds 渲染为 session_id IN，且同样会被会话列表查询应用；为空时不限制")
    void matchSessionIdsAppliesToSharedFragment() throws Exception {
        ChatSessionQueryDTO q = new ChatSessionQueryDTO();
        q.setMatchSessionIds(Arrays.asList("s1", "s2", "s3"));
        q.setVisibleAgentIds(Collections.singletonList("a1"));
        q.setLimit(10);

        // 共用片段：对会话列表的两条查询都应该生效
        String content = sql("selectContentRows", q);
        String count = sql("countChatSessions", q);
        assertTrue(content.contains("m.session_id IN (?,?,?)"), content);
        assertTrue(count.contains("m.session_id IN (?,?,?)"), count);
        // 可见智能体限制不能被会话ID过滤「顶掉」，两个条件必须同时存在
        assertTrue(count.contains("m.agent_id IN (?)"), count);

        ChatSessionQueryDTO none = new ChatSessionQueryDTO();
        none.setMatchSessionIds(Collections.emptyList());
        none.setLimit(10);
        assertFalse(sql("selectContentRows", none).contains("session_id IN"));
    }

    @Test
    @DisplayName("空的 chatTypes 列表不生成空 IN () 语法错误")
    void emptyChatTypesProducesNoInClause() throws Exception {
        ChatSessionQueryDTO q = new ChatSessionQueryDTO();
        q.setChatTypes(Collections.emptyList());
        q.setLimit(100);

        String sql = sql("selectContentRows", q);

        assertFalse(sql.contains("IN ()"), sql);
        assertFalse(sql.contains("WHERE"), sql);
    }

    @Test
    @DisplayName("会话分析明细 SQL 同样能正确解析并渲染")
    void analyticsRowsRender() throws Exception {
        ChatSessionQueryDTO q = new ChatSessionQueryDTO();
        q.setMacAddress("AA:BB");
        q.setLimit(100);

        String sql = sql("selectAnalyticsRows", q);

        assertTrue(sql.contains("m.metrics_json AS metrics_json"), sql);
        assertTrue(sql.contains("WHERE m.mac_address = ?"), sql);
        assertTrue(sql.contains("ORDER BY m.created_at ASC, m.id ASC LIMIT ?"), sql);
    }
}
