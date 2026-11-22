package xiaozhi.modules.agent.vo;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会话内容分析（词云）结果。
 *
 * <p>词频统计基于用户 / 智能体的消息正文（工具调用消息不计入），分词后已过滤停用词、标点、
 * 纯数字和单字。words 为两类角色合并后的结果，userWords / agentWords 分别是单角色结果。</p>
 */
@Data
@Schema(description = "会话内容词云")
public class ChatWordCloudVO {

    @Schema(description = "汇总指标")
    private Summary summary = new Summary();

    @Schema(description = "合并后的高频词，按词频倒序")
    private List<WordItem> words = new ArrayList<>();

    @Schema(description = "用户消息高频词")
    private List<WordItem> userWords = new ArrayList<>();

    @Schema(description = "智能体消息高频词")
    private List<WordItem> agentWords = new ArrayList<>();

    @Data
    public static class Summary {
        /** 参与统计的消息条数 */
        private long messageCount;
        /** 其中用户消息条数 */
        private long userMessageCount;
        /** 其中智能体消息条数 */
        private long agentMessageCount;
        /** 过滤后的有效词总数（含重复） */
        private long totalWords;
        /** 去重后的词汇量 */
        private long distinctWords;
        /** 是否因达到读取上限而被截断，前端据此提示统计可能不完整 */
        private boolean truncated;
    }

    @Data
    public static class WordItem {
        private String word;
        private long count;
        /** 出现过该词的不同会话数（完整统计，不受 sessionIds 截断影响） */
        private long sessionCount;
        /** 关联会话ID，最多返回 50 个（按最近出现的会话优先），用于直接打开会话详情 */
        private List<String> sessionIds = new ArrayList<>();

        public WordItem() {
        }

        public WordItem(String word, long count) {
            this.word = word;
            this.count = count;
        }
    }
}
