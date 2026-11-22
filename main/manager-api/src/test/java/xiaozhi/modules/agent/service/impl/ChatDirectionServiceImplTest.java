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
    @DisplayName("提示词预览只组装内容，不调用大模型；未配置参数时标记为兜底")
    void previewDoesNotCallLlm() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("山东省", "水温多少"));
        ChatDirectionQueryDao queryDao = mock(ChatDirectionQueryDao.class);
        when(queryDao.selectUserQuestionRows(any(ChatDirectionQueryDTO.class))).thenReturn(rows);
        UserDataScopeService scope = mock(UserDataScopeService.class);
        when(scope.canViewAllUserData()).thenReturn(true);
        ChatTextTokenizer tokenizer = mock(ChatTextTokenizer.class);
        when(tokenizer.tokenize(anyString())).thenReturn(new ArrayList<>());
        SysParamsService params = mock(SysParamsService.class);
        when(params.getValue(anyString(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn("条件：{filter}\n问题：\n{conversation}");
        LLMService llm = mock(LLMService.class);
        when(llm.isAvailable()).thenReturn(true);
        ChatDirectionServiceImpl service = new ChatDirectionServiceImpl(queryDao, mock(ChatDirectionAnalysisDao.class),
                mock(AgentDao.class), mock(DeviceDao.class), tokenizer, scope, params, llm,
                mock(ModelConfigService.class), mock(ChatWakeupWordFilter.class));

        xiaozhi.modules.agent.vo.ChatDirectionPromptPreviewVO vo =
                service.previewPrompt(new ChatDirectionRequestDTO(), 1L, 20);

        assertTrue(vo.getTemplate().contains("{conversation}"));
        assertTrue(vo.getFullPrompt().contains("- 水温多少（1 次）"));
        assertTrue(vo.getFullPrompt().contains("地区=全部"));
        assertTrue(vo.isLlmAvailable());
        org.junit.jupiter.api.Assertions.assertFalse(vo.isFallback());
        org.mockito.Mockito.verify(llm, org.mockito.Mockito.never()).generateSummary(anyString(), anyString());
    }

    @Test
    @DisplayName("AI 分析会快照当次提示词、模型、耗时与统计数据；原始记录不能查看他人数据")
    void analyzeSavesRawRecord() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("山东省", "水温多少"));
        ChatDirectionQueryDao queryDao = mock(ChatDirectionQueryDao.class);
        when(queryDao.selectUserQuestionRows(any(ChatDirectionQueryDTO.class))).thenReturn(rows);
        UserDataScopeService scope = mock(UserDataScopeService.class);
        when(scope.canViewAllUserData()).thenReturn(false);
        when(scope.getUsernameMap(any())).thenReturn(new HashMap<>());
        when(scope.getNicknameMap(any())).thenReturn(new HashMap<>());
        AgentDao agentDao = mock(AgentDao.class);
        xiaozhi.modules.agent.entity.AgentEntity agent = new xiaozhi.modules.agent.entity.AgentEntity();
        agent.setId("a1");
        when(agentDao.selectList(any())).thenReturn(new ArrayList<>(List.of(agent)));
        ChatTextTokenizer tokenizer = mock(ChatTextTokenizer.class);
        when(tokenizer.tokenize(anyString())).thenReturn(new ArrayList<>());
        SysParamsService params = mock(SysParamsService.class);
        when(params.getValue(anyString(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn("条件：{filter}\n问题：\n{conversation}");
        LLMService llm = mock(LLMService.class);
        when(llm.isAvailable()).thenReturn(true);
        when(llm.generateSummary(anyString(), anyString())).thenReturn("主要方向：水温监测");
        ChatDirectionAnalysisDao analysisDao = mock(ChatDirectionAnalysisDao.class);
        ChatDirectionServiceImpl service = new ChatDirectionServiceImpl(queryDao, analysisDao, agentDao,
                mock(DeviceDao.class), tokenizer, scope, params, llm,
                mock(ModelConfigService.class), mock(ChatWakeupWordFilter.class));

        service.analyzeByLlm(new ChatDirectionRequestDTO(), 1L, 20);

        org.mockito.ArgumentCaptor<xiaozhi.modules.agent.entity.ChatDirectionAnalysisEntity> captor =
                org.mockito.ArgumentCaptor.forClass(xiaozhi.modules.agent.entity.ChatDirectionAnalysisEntity.class);
        org.mockito.Mockito.verify(analysisDao).insert(captor.capture());
        xiaozhi.modules.agent.entity.ChatDirectionAnalysisEntity saved = captor.getValue();
        assertEquals("条件：{filter}\n问题：\n{conversation}", saved.getPromptTemplate());
        assertTrue(saved.getFullPrompt().contains("- 水温多少（1 次）"));
        assertTrue(saved.getFullPrompt().contains("地区=全部"));
        assertEquals(1, saved.getMessageCount());
        assertTrue(saved.getDurationMs() != null && saved.getDurationMs() >= 0);
        assertTrue(saved.getStatSnapshot().contains("水温多少"));
        org.junit.jupiter.api.Assertions.assertFalse(saved.getPromptFallback());

        // 他人的记录按不存在处理
        saved.setId("r1");
        saved.setUserId(2L);
        when(analysisDao.selectById("r1")).thenReturn(saved);
        org.junit.jupiter.api.Assertions.assertThrows(xiaozhi.common.exception.RenException.class,
                () -> service.getAnalysisDetail("r1", 1L));

        // 自己的记录可以查看，统计快照能解析回来
        saved.setUserId(1L);
        xiaozhi.modules.agent.vo.ChatDirectionAnalysisDetailVO detail = service.getAnalysisDetail("r1", 1L);
        assertEquals("主要方向：水温监测", detail.getResult());
        assertEquals("山东省", detail.getStatSnapshot().getRegions().get(0).getName());
        org.junit.jupiter.api.Assertions.assertFalse(detail.isLegacy());
    }

    @Test
    @DisplayName("可视化：从回复中提取 JSON 并覆盖保存；非 JSON 报错；他人记录不可生成")
    void generateVisualSavesJson() {
        assertNull(ChatDirectionServiceImpl.parseVisualJson("不是 JSON"));
        Map<String, Object> parsed = ChatDirectionServiceImpl.parseVisualJson(
                "```json\n{\"summary\":\"水温为主\",\"topDirection\":null}\n```");
        assertEquals("水温为主", parsed.get("summary"));
        assertTrue(parsed.containsKey("topDirection"));

        ChatDirectionAnalysisDao analysisDao = mock(ChatDirectionAnalysisDao.class);
        xiaozhi.modules.agent.entity.ChatDirectionAnalysisEntity record = new xiaozhi.modules.agent.entity.ChatDirectionAnalysisEntity();
        record.setId("r1");
        record.setUserId(1L);
        record.setResult("主要方向：水温监测");
        record.setInputContent("- 水温多少（3 次）");
        when(analysisDao.selectById("r1")).thenReturn(record);
        UserDataScopeService scope = mock(UserDataScopeService.class);
        when(scope.canViewAllUserData()).thenReturn(false);
        SysParamsService params = mock(SysParamsService.class);
        when(params.getValue(anyString(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn("问题：{questions}\n结论：{conversation}");
        LLMService llm = mock(LLMService.class);
        when(llm.isAvailable()).thenReturn(true);
        when(llm.generateSummary(anyString(), anyString())).thenReturn(
                "好的：{\"summary\":\"水温为主\",\"directions\":[{\"rank\":1,\"name\":\"水温监测\",\"ratio\":80}]}");
        ChatDirectionServiceImpl service = new ChatDirectionServiceImpl(mock(ChatDirectionQueryDao.class), analysisDao,
                mock(AgentDao.class), mock(DeviceDao.class), mock(ChatTextTokenizer.class), scope, params, llm,
                mock(ModelConfigService.class), mock(ChatWakeupWordFilter.class));

        // 预览只组装内容、不调用大模型、不写库，且与实际发送内容一致
        xiaozhi.modules.agent.vo.ChatDirectionPromptPreviewVO preview = service.previewVisualPrompt("r1", 1L);
        assertEquals("问题：- 水温多少（3 次）\n结论：主要方向：水温监测", preview.getFullPrompt());
        assertEquals("server.chat_direction_visual_prompt", preview.getParamCode());
        assertTrue(preview.isLlmAvailable());
        org.mockito.Mockito.verify(llm, org.mockito.Mockito.never()).generateSummary(anyString(), anyString());
        org.mockito.Mockito.verify(analysisDao, org.mockito.Mockito.never())
                .updateById(any(xiaozhi.modules.agent.entity.ChatDirectionAnalysisEntity.class));

        xiaozhi.modules.agent.vo.ChatDirectionAnalysisVO vo = service.generateVisual("r1", 1L);

        org.mockito.Mockito.verify(llm).generateSummary("主要方向：水温监测", "问题：- 水温多少（3 次）\n结论：{conversation}");
        org.mockito.Mockito.verify(analysisDao).updateById(record);
        assertTrue(vo.getHasVisual());
        assertEquals("水温为主", vo.getVisual().get("summary"));
        assertTrue(record.getVisualJson().contains("水温监测"));

        when(llm.generateSummary(anyString(), anyString())).thenReturn("抱歉，无法处理");
        org.junit.jupiter.api.Assertions.assertThrows(xiaozhi.common.exception.RenException.class,
                () -> service.generateVisual("r1", 1L));

        record.setUserId(2L);
        org.junit.jupiter.api.Assertions.assertThrows(xiaozhi.common.exception.RenException.class,
                () -> service.generateVisual("r1", 1L));
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