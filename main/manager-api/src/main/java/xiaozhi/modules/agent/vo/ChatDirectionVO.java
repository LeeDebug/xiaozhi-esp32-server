package xiaozhi.modules.agent.vo;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会话内容高频方向统计结果：按地区 / 智能体 / 设备分别汇总用户提问的高频词。
 */
@Data
@Schema(description = "会话内容高频方向")
public class ChatDirectionVO {

    @Schema(description = "参与统计的用户提问条数")
    private long messageCount;

    @Schema(description = "是否因达到读取上限而被截断")
    private boolean truncated;

    @Schema(description = "按地区汇总")
    private List<Group> regions = new ArrayList<>();

    @Schema(description = "按智能体汇总")
    private List<Group> agents = new ArrayList<>();

    @Schema(description = "按设备汇总")
    private List<Group> devices = new ArrayList<>();

    /** 一个分组（某个地区 / 智能体 / 设备）及其高频词 */
    @Data
    public static class Group {
        /** 分组 key：地区名称 / agentId / mac，用于前端筛选 */
        private String key;
        /** 展示名：地区名 / 智能体名 / 设备别名或 MAC */
        private String name;
        /** 该分组的提问条数 */
        private long messageCount;
        private List<Word> words = new ArrayList<>();
    }

    @Data
    public static class Word {
        private String word;
        private long count;

        public Word() {
        }

        public Word(String word, long count) {
            this.word = word;
            this.count = count;
        }
    }
}
