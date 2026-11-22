package xiaozhi.modules.agent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xiaozhi.common.constant.Constant;
import xiaozhi.modules.sys.dao.SysParamsDao;
import xiaozhi.modules.sys.redis.SysParamsRedis;

@DisplayName("会话分析唤醒词过滤测试")
class ChatWakeupWordFilterTest {

    private ChatWakeupWordFilter filter(String enabled, String words) {
        SysParamsRedis redis = mock(SysParamsRedis.class);
        SysParamsDao dao = mock(SysParamsDao.class);
        when(dao.getValueByCode(Constant.SERVER_CHAT_ANALYSIS_FILTER_WAKEUP_WORDS)).thenReturn(enabled);
        when(dao.getValueByCode(Constant.WAKEUP_WORDS)).thenReturn(words);
        return new ChatWakeupWordFilter(dao, redis);
    }

    @Test
    @DisplayName("开关开启或参数缺失时生效，只有明确 false 才关闭")
    void switchDefaultsToOn() {
        String words = "小牧博士;你好小智;你好小志;小爱同学";
        assertEquals(4, filter("true", words).activeWords().size());
        assertEquals(4, filter(null, words).activeWords().size());
        assertTrue(filter("false", words).activeWords().isEmpty());
    }

    @Test
    @DisplayName("解析分号分隔、去重，长词优先")
    void parse() {
        List<String> words = ChatWakeupWordFilter.parse(" 小智 ;你好小智；小智;;");
        assertEquals(Arrays.asList("你好小智", "小智"), words);
    }

    @Test
    @DisplayName("去掉文本中的唤醒词，只有唤醒词时变为空串")
    void strip() {
        List<String> words = ChatWakeupWordFilter.parse("小牧博士;你好小智");
        assertEquals("，水温多少", ChatWakeupWordFilter.strip("你好小智，水温多少", words));
        assertEquals("", ChatWakeupWordFilter.strip(" 小牧博士 ", words));
        assertEquals("水温", ChatWakeupWordFilter.strip("水温", List.of()));
    }
}
