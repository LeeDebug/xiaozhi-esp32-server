package xiaozhi.modules.agent.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import xiaozhi.common.constant.Constant;
import xiaozhi.modules.sys.dao.SysParamsDao;
import xiaozhi.modules.sys.redis.SysParamsRedis;

/**
 * 会话分析用的唤醒词过滤。
 *
 * <p>参数 {@code server.chat_analysis_filter_wakeup_words}（默认开）打开时，从 {@code wakeup_words} 参数
 * 读取唤醒词，在高频词 / 高频方向统计前从消息正文中去掉。只影响分析结果，不改库中记录。</p>
 *
 * <p>直接读参数 DAO + 缓存（与 UserDataScopeService 相同），不注入 SysParamsService，避免构造注入环。
 * 参数管理页修改后会刷新 Redis，因此无需重启即可生效。</p>
 */
@Component
@RequiredArgsConstructor
public class ChatWakeupWordFilter {

    private final SysParamsDao sysParamsDao;
    private final SysParamsRedis sysParamsRedis;

    /**
     * 当前生效的唤醒词（按长度从长到短）；开关关闭或未配置唤醒词时返回空列表。
     * 一次统计请求只需调用一次，再对每条消息调用 {@link #strip(String, List)}。
     */
    public List<String> activeWords() {
        if (!isEnabled()) {
            return Collections.emptyList();
        }
        return parse(readParam(Constant.WAKEUP_WORDS));
    }

    /** 参数不存在或为空时按默认开启处理，只有明确设为 false 才关闭 */
    boolean isEnabled() {
        String value = readParam(Constant.SERVER_CHAT_ANALYSIS_FILTER_WAKEUP_WORDS);
        return StringUtils.isBlank(value) || !"false".equalsIgnoreCase(value.trim());
    }

    private String readParam(String code) {
        String value = sysParamsRedis.get(code);
        if (value == null) {
            value = sysParamsDao.getValueByCode(code);
            if (value != null) {
                sysParamsRedis.set(code, value);
            }
        }
        return value;
    }

    /** 支持中英文分号、逗号、换行分隔；去重后按长度倒序，保证长词优先匹配 */
    static List<String> parse(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split("[;；,，\\r\\n]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 从文本中去掉所有唤醒词（英文不区分大小写），并去掉首尾空白。
     * 去掉后可能变成空串（消息只有唤醒词），调用方应据此跳过该条。
     */
    public static String strip(String text, List<String> words) {
        if (text == null || words == null || words.isEmpty()) {
            return text;
        }
        String result = text;
        for (String word : words) {
            result = Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(result).replaceAll("");
        }
        return result.trim();
    }
}
