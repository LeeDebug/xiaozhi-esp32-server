package xiaozhi.modules.agent.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.lionsoul.jcseg.ISegment;
import org.lionsoul.jcseg.IWord;
import org.lionsoul.jcseg.dic.ADictionary;
import org.lionsoul.jcseg.dic.DictionaryFactory;
import org.lionsoul.jcseg.segmenter.ComplexSeg;
import org.lionsoul.jcseg.segmenter.SegmenterConfig;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 会话文本分词器（会话内容分析词云使用）。
 *
 * <p>基于 jcseg：词典是进程内单例、可共享，分词器实例非线程安全，因此每次分词新建。
 * 停用词、标点、纯数字、单字的过滤在这里自己实现，不依赖 jcseg 的 CLEAR_STOPWORD，结果可控、可测试。</p>
 */
@Slf4j
@Component
public class ChatTextTokenizer {

    /** 常见中文停用词（语气词、代词、助词、常见连接词与对话套话） */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "的", "了", "是", "我", "你", "他", "她", "它", "们", "在", "有", "和", "就", "不", "人", "都", "一", "个",
            "上", "也", "很", "到", "说", "要", "去", "会", "着", "没有", "看", "好", "自己", "这", "那", "吧", "呢", "啊",
            "啦", "嘛", "哦", "额", "嗯", "噢", "呀", "吗", "给", "让", "把", "被", "为", "以", "与", "及", "或", "而", "但", "但是",
            "因为", "所以", "如果", "那么", "这个", "那个", "什么", "怎么", "一下", "一些", "一个", "我们", "你们", "他们",
            "可以", "可能", "已经", "还是", "还有", "现在", "就是", "不是", "也是", "都是", "有点", "一点", "比较", "非常",
            "请", "想", "能", "将", "还", "当", "从", "对", "把", "于", "其", "每", "其它", "其他", "呢呢", "哈哈",
            "the", "a", "an", "and", "or", "of", "to", "in", "is", "it", "i", "you", "me", "my", "that", "this", "for", "on", "are", "was"));

    /** 分词词典是并发安全的单例，懒加载，避免应用启动变慢 */
    private volatile SegmenterConfig config;
    private volatile ADictionary dictionary;

    private void ensureReady() {
        if (dictionary != null) {
            return;
        }
        synchronized (this) {
            if (dictionary != null) {
                return;
            }
            try {
                SegmenterConfig cfg = new SegmenterConfig(true);
                // 关闭词典文件监听线程，web 应用里不需要热加载
                cfg.setAutoload(false);
                this.dictionary = DictionaryFactory.createSingletonDictionary(cfg);
                this.config = cfg;
            } catch (Exception e) {
                throw new IllegalStateException("中文分词词典初始化失败", e);
            }
        }
    }

    /**
     * 对一段文本分词并过滤，返回保留顺序的有效词列表（可重复）。
     */
    public List<String> tokenize(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        ensureReady();
        try {
            // ComplexSeg 非线程安全，每次新建
            ISegment seg = new ComplexSeg(config, dictionary);
            seg.reset(new StringReader(text));
            IWord word;
            while ((word = seg.next()) != null) {
                String token = normalize(word.getValue());
                if (isValid(token)) {
                    result.add(token);
                }
            }
        } catch (IOException e) {
            log.warn("分词失败，跳过该条文本: {}", e.getMessage());
        }
        return result;
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    /** 单字、纯数字、纯标点/符号、停用词都不计入 */
    static boolean isValid(String token) {
        if (token == null || token.length() < 2) {
            return false;
        }
        if (STOP_WORDS.contains(token)) {
            return false;
        }
        boolean hasLetter = false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
                break;
            }
        }
        // 没有任何字母（含汉字）的词，例如纯数字、标点、符号，直接丢弃
        return hasLetter;
    }
}
