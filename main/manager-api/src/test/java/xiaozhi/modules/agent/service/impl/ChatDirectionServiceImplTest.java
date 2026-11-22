package xiaozhi.modules.agent.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xiaozhi.modules.agent.dao.AgentDao;
import xiaozhi.modules.agent.dao.ChatDirectionAnalysisDao;
import xiaozhi.modules.agent.dao.ChatDirectionQueryDao;
import xiaozhi.modules.agent.dto.ChatDirectionQueryDTO;
import xiaozhi.modules.agent.dto.ChatDirectionRequestDTO;
import xiaozhi.modules.agent.service.ChatTextTokenizer;
import xiaozhi.modules.agent.service.ChatWakeupWordFilter;
import xiaozhi.modules.agent.vo.ChatDirectionVO;
import xiaozhi.modules.device.dao.DeviceDao;
import xiaozhi.modules.llm.service.LLMService;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.sys.service.SysParamsService;
import xiaozhi.modules.sys.service.UserDataScopeService;

@DisplayName("会话内容高频问题统计测试")
class ChatDirectionServiceImplTest {

    private static Map<String, Object> row(String province, String content) {
        Map<String, Object> map = new HashMap<>();
        map.put("province", province);
        map.put("content", content);
        return map;
    }

    private ChatDirectionServiceImpl service(List<Map<String, Object>> rows) {
        return service(rows, new ArrayList<>());
    }

    private ChatDirectionServiceImpl service(List<Map<String, Object>> rows, List<String> wakeupWords) {
        ChatWakeupWordFilter filter = mock(ChatWakeupWordFilter.class);
        when(filter.activeWords()).thenReturn(wakeupWords);
        ChatDirectionQueryDao queryDao = mock(ChatDirectionQueryDao.class);
        when(queryDao.selectUserQuestionRows(any(ChatDirectionQueryDTO.class))).thenReturn(rows);
        UserDataScopeService scope = mock(UserDataScopeService.class);
        when(scope.canViewAllUserData()).thenReturn(true);
        ChatTextTokenizer tokenizer = mock(ChatTextTokenizer.class);
        when(tokenizer.tokenize(anyString())).thenAnswer(inv -> new ArrayList<>(
                Arrays.asList(((String) inv.getArgument(0)).split(" "))));
        return new ChatDirectionServiceImpl(queryDao, mock(ChatDirectionAnalysisDao.class), mock(AgentDao.class),
                mock(DeviceDao.class), tokenizer, scope, mock(SysParamsService.class), mock(LLMService.class),
                mock(ModelConfigService.class), filter);
    }

    @Test
    @DisplayName("开启唤醒词过滤后，问题中的唤醒词被去掉，只有唤醒词的提问不计入")
    void wakeupWordsAreStripped() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("山东省", "你好小智，水温多少"));
        rows.add(row("山东省", "水温多少"));
        rows.add(row("山东省", "小牧博士"));

        ChatDirectionVO vo = service(rows, Arrays.asList("小牧博士", "你好小智"))
                .getChatDirection(new ChatDirectionRequestDTO(), 1L, 20);

        assertEquals(2, vo.getMessageCount());
        ChatDirectionVO.Group group = vo.getRegions().get(0);
        assertEquals(1, group.getQuestions().size());
        assertEquals("水温多少", group.getQuestions().get(0).getText());
        assertEquals(2, group.getQuestions().get(0).getCount());
    }

    @Test
    @DisplayName("整句问题归一化后按出现次数倒序，标点与首尾空白不影响合并")
    void questionsAreCountedAsWholeSentences() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("山东省", "水温多少？"));
        rows.add(row("山东省", "  水温多少 "));
        rows.add(row("山东省", "水温多少。"));
        rows.add(row("山东省", "今天天气怎么样"));
        rows.add(row("四川省", "今天产蛋量多少"));
        rows.add(row(null, "嗯"));

        ChatDirectionVO vo = service(rows).getChatDirection(new ChatDirectionRequestDTO(), 1L, 20);

        // 「嗯」只有一个字，不计入统计
        assertEquals(5, vo.getMessageCount());
        ChatDirectionVO.Group shandong = vo.getRegions().get(0);
        assertEquals("山东省", shandong.getName());
        assertEquals(4, shandong.getMessageCount());
        assertEquals("水温多少", shandong.getQuestions().get(0).getText());
        assertEquals(3, shandong.getQuestions().get(0).getCount());
        assertEquals("今天天气怎么样", shandong.getQuestions().get(1).getText());
        // 高频词仍作为辅助返回
        assertTrue(shandong.getWords().stream().anyMatch(w -> w.getCount() > 0));
    }

    @Test
    @DisplayName("大模型输入以问题列表组织，并带出现次数")
    void llmInputListsQuestions() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("山东省", "水温多少"));
        rows.add(row("山东省", "水温多少"));
        ChatDirectionServiceImpl service = service(rows);

        String input = service.buildLlmInput(service.getChatDirection(new ChatDirectionRequestDTO(), 1L, 20));

        assertTrue(input.contains("山东省（2 条提问）"));
        assertTrue(input.contains("- 水温多少（2 次）"));
    }

    @Test
    @DisplayName("问题归一化：去首尾标点、压缩空白、超长截断、过短忽略")
    void normalizeQuestion() {
        assertEquals("水温 多少", ChatDirectionServiceImpl.normalizeQuestion("【水温   多少】？！"));
        assertNull(ChatDirectionServiceImpl.normalizeQuestion("。。"));
        assertNull(ChatDirectionServiceImpl.normalizeQuestion("嗯"));
        String longText = "长".repeat(150);
        assertEquals(ChatDirectionServiceImpl.MAX_QUESTION_LENGTH,
                ChatDirectionServiceImpl.normalizeQuestion(longText).length());
    }
}