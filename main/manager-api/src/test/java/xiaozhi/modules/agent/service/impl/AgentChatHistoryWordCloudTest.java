package xiaozhi.modules.agent.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import xiaozhi.modules.agent.dao.AgentChatTitleDao;
import xiaozhi.modules.agent.dao.AgentDao;
import xiaozhi.modules.agent.dao.AiAgentChatHistoryDao;
import xiaozhi.modules.agent.dto.ChatRecordQueryDTO;
import xiaozhi.modules.agent.dto.ChatSessionQueryDTO;
import xiaozhi.modules.agent.service.AgentChatTitleService;
import xiaozhi.modules.agent.service.ChatTextTokenizer;
import xiaozhi.modules.agent.vo.ChatWordCloudVO;
import xiaozhi.modules.device.dao.DeviceDao;
import xiaozhi.modules.sys.service.UserDataScopeService;

@DisplayName("会话内容词云统计测试")
class AgentChatHistoryWordCloudTest {

    private static final long USER_ID = 1L;

    private static Map<String, Object> row(int chatType, String content) {
        Map<String, Object> map = new HashMap<>();
        map.put("chat_type", chatType);
        map.put("content", content);
        return map;
    }

    private AgentChatHistoryServiceImpl service(AiAgentChatHistoryDao dao, UserDataScopeService scope,
            AgentDao agentDao, ChatTextTokenizer tokenizer) {
        AgentChatHistoryServiceImpl service = new AgentChatHistoryServiceImpl(
                mock(AgentChatTitleService.class), agentDao, mock(AgentChatTitleDao.class),
                mock(DeviceDao.class), scope, tokenizer);
        ReflectionTestUtils.setField(service, "baseMapper", dao);
        return service;
    }

    private UserDataScopeService viewAllScope() {
        UserDataScopeService scope = mock(UserDataScopeService.class);
        when(scope.canViewAllUserData()).thenReturn(true);
        return scope;
    }

    /** 用文本按空格切开的简单分词器，让服务层逐辑可以独立验证 */
    private ChatTextTokenizer spaceTokenizer() {
        ChatTextTokenizer tokenizer = mock(ChatTextTokenizer.class);
        when(tokenizer.tokenize(anyString())).thenAnswer(inv -> {
            String text = inv.getArgument(0);
            return new ArrayList<>(Arrays.asList(text.split(" ")));
        });
        return tokenizer;
    }

    @Test
    @DisplayName("词频按出现次数倒序，相同词频按词本身排序，并区分用户与智能体")
    void countsAreSortedAndSplitByRole() {
        AiAgentChatHistoryDao dao = mock(AiAgentChatHistoryDao.class);
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row(1, "apple banana apple"));
        rows.add(row(2, "banana cherry"));
        rows.add(row(1, "apple"));
        when(dao.selectContentRows(any(ChatSessionQueryDTO.class))).thenReturn(rows);

        ChatWordCloudVO vo = service(dao, viewAllScope(), mock(AgentDao.class), spaceTokenizer())
                .getChatWordCloud(new ChatRecordQueryDTO(), USER_ID, 50);

        assertEquals(3, vo.getSummary().getMessageCount());
        assertEquals(2, vo.getSummary().getUserMessageCount());
        assertEquals(1, vo.getSummary().getAgentMessageCount());
        assertEquals(6, vo.getSummary().getTotalWords());
        assertEquals(3, vo.getSummary().getDistinctWords());
        // apple=3, banana=2, cherry=1
        assertEquals("apple", vo.getWords().get(0).getWord());
        assertEquals(3, vo.getWords().get(0).getCount());
        assertEquals("banana", vo.getWords().get(1).getWord());
        assertEquals("cherry", vo.getWords().get(2).getWord());
        // 用户侧只有 apple(3) 和 banana(1)，智能体侧只有 banana(1) 和 cherry(1)
        assertEquals(2, vo.getUserWords().size());
        assertEquals(3, vo.getUserWords().get(0).getCount());
        assertEquals(2, vo.getAgentWords().size());
        // 词频相同(banana=1,cherry=1)时按字典序
        assertEquals("banana", vo.getAgentWords().get(0).getWord());
    }

    @Test
    @DisplayName("正文是 {speaker, content} JSON 时只统计 content，不把 speaker 当成词")
    void jsonContentIsUnwrappedBeforeTokenizing() {
        AiAgentChatHistoryDao dao = mock(AiAgentChatHistoryDao.class);
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row(1, "{\"speaker\":\"unknown\",\"content\":\"weather today\"}"));
        when(dao.selectContentRows(any(ChatSessionQueryDTO.class))).thenReturn(rows);

        ChatWordCloudVO vo = service(dao, viewAllScope(), mock(AgentDao.class), spaceTokenizer())
                .getChatWordCloud(new ChatRecordQueryDTO(), USER_ID, 50);

        List<String> words = new ArrayList<>();
        vo.getWords().forEach(w -> words.add(w.getWord()));
        assertTrue(words.contains("weather"));
        assertTrue(words.contains("today"));
        assertFalse(words.contains("speaker"));
        assertFalse(words.contains("unknown"));
    }

    @Test
    @DisplayName("未指定角色或指定工具(3)时只查用户+智能体；指定 1 时只查用户")
    void chatTypeFilterNeverIncludesToolMessages() {
        AiAgentChatHistoryDao dao = mock(AiAgentChatHistoryDao.class);
        when(dao.selectContentRows(any(ChatSessionQueryDTO.class))).thenReturn(Collections.emptyList());
        AgentChatHistoryServiceImpl service = service(dao, viewAllScope(), mock(AgentDao.class), spaceTokenizer());
        ArgumentCaptor<ChatSessionQueryDTO> captor = forClass(ChatSessionQueryDTO.class);

        service.getChatWordCloud(new ChatRecordQueryDTO(), USER_ID, 50);
        ChatRecordQueryDTO tool = new ChatRecordQueryDTO();
        tool.setChatType(3);
        service.getChatWordCloud(tool, USER_ID, 50);
        ChatRecordQueryDTO userOnly = new ChatRecordQueryDTO();
        userOnly.setChatType(1);
        service.getChatWordCloud(userOnly, USER_ID, 50);

        verify(dao, org.mockito.Mockito.times(3)).selectContentRows(captor.capture());
        List<ChatSessionQueryDTO> queries = captor.getAllValues();
        assertEquals(Arrays.asList(1, 2), queries.get(0).getChatTypes());
        assertEquals(Arrays.asList(1, 2), queries.get(1).getChatTypes());
        assertEquals(Collections.singletonList(1), queries.get(2).getChatTypes());
    }

    @Test
    @DisplayName("topN 被限制在 10 到 200 之间")
    void topNIsClamped() {
        AiAgentChatHistoryDao dao = mock(AiAgentChatHistoryDao.class);
        List<Map<String, Object>> rows = new ArrayList<>();
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            many.append("w").append(i).append(' ');
        }
        rows.add(row(1, many.toString().trim()));
        when(dao.selectContentRows(any(ChatSessionQueryDTO.class))).thenReturn(rows);
        AgentChatHistoryServiceImpl service = service(dao, viewAllScope(), mock(AgentDao.class), spaceTokenizer());

        assertEquals(10, service.getChatWordCloud(new ChatRecordQueryDTO(), USER_ID, 1).getWords().size());
        assertEquals(200, service.getChatWordCloud(new ChatRecordQueryDTO(), USER_ID, 9999).getWords().size());
    }

    @Test
    @DisplayName("普通用户没有智能体时直接返回空结果，且不查明细")
    void userWithoutAgentsGetsEmptyResultWithoutQuery() {
        AiAgentChatHistoryDao dao = mock(AiAgentChatHistoryDao.class);
        UserDataScopeService scope = mock(UserDataScopeService.class);
        when(scope.canViewAllUserData()).thenReturn(false);
        AgentDao agentDao = mock(AgentDao.class);
        when(agentDao.selectList(any())).thenReturn(Collections.emptyList());

        ChatWordCloudVO vo = service(dao, scope, agentDao, spaceTokenizer())
                .getChatWordCloud(new ChatRecordQueryDTO(), USER_ID, 50);

        assertEquals(0, vo.getSummary().getMessageCount());
        assertTrue(vo.getWords().isEmpty());
        verify(dao, never()).selectContentRows(any());
    }

    @Test
    @DisplayName("真实分词器：中文按词切分，过滤停用词、标点、纯数字和单字")
    void realTokenizerSegmentsChineseAndFiltersNoise() {
        ChatTextTokenizer tokenizer = new ChatTextTokenizer();

        List<String> tokens = tokenizer.tokenize("今天天气真不错，我想听一首周杰伦的歌，再讲个笑话给我听吧 hello 12345");

        assertTrue(tokens.contains("天气"), "应切出「天气」，实际: " + tokens);
        assertTrue(tokens.contains("周杰伦"), "应识别人名「周杰伦」，实际: " + tokens);
        assertTrue(tokens.contains("笑话"), "应切出「笑话」，实际: " + tokens);
        assertTrue(tokens.contains("hello"));
        // 停用词、标点、纯数字、单字都不应出现
        assertFalse(tokens.contains("的"));
        assertFalse(tokens.contains("我"));
        assertFalse(tokens.contains("，"));
        assertFalse(tokens.contains("12345"));
        assertTrue(tokens.stream().allMatch(t -> t.length() >= 2), "不应有单字: " + tokens);
    }
}
