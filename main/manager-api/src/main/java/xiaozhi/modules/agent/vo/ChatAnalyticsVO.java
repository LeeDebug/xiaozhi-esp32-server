package xiaozhi.modules.agent.vo;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会话分析看板聚合结果。
 *
 * <p>所有时间分布（按天、按小时、星期 x 小时）统一按中国时间（Asia/Shanghai）统计，
 * 时长统一返回秒。「有效时长」= 各轮耗时快照 e2eTotalMs 之和（系统实际在处理用户问答的时间）；
 * 「等待时长」= 会话总跨度 - 有效时长（用户思考、设备空闲等待的间隔，最小为 0）。
 * 历史会话没有耗时快照时有效时长为 0，前端通过 metricSessionCount 提示覆盖率。</p>
 */
@Data
@Schema(description = "会话分析看板")
public class ChatAnalyticsVO {

    @Schema(description = "汇总指标")
    private Summary summary = new Summary();

    @Schema(description = "按天的会话数与消息数")
    private List<DailyItem> daily = new ArrayList<>();

    @Schema(description = "按设备的会话数与消息数")
    private List<DeviceItem> devices = new ArrayList<>();

    @Schema(description = "按智能体的会话数与消息数")
    private List<AgentItem> agents = new ArrayList<>();

    @Schema(description = "一天 24 小时的会话开始分布，固定 24 项")
    private List<Integer> hourly = new ArrayList<>();

    @Schema(description = "星期 x 小时热力图数据：[小时, 星期(0=周一), 条数]")
    private List<List<Integer>> weekHour = new ArrayList<>();

    @Schema(description = "会话总跨度分布（分桶）")
    private List<BucketItem> durationBuckets = new ArrayList<>();

    @Schema(description = "会话轮数分布（分桶）")
    private List<BucketItem> roundBuckets = new ArrayList<>();

    @Schema(description = "时长最长的会话，用于有效/等待时长堆叠图（最多 15 条）")
    private List<SessionDuration> topSessions = new ArrayList<>();

    @Schema(description = "会话散点样本：轮数 x 总跨度（最多 300 个）")
    private List<ScatterItem> scatter = new ArrayList<>();

    @Data
    public static class Summary {
        private long sessionCount;
        private long messageCount;
        private long userMessageCount;
        private long deviceCount;
        private long agentCount;
        /** 平均每个会话的消息条数 */
        private double avgMessages;
        /** 平均每个会话的用户提问轮数 */
        private double avgRounds;
        /** 平均会话总跨度（秒） */
        private double avgSpanSeconds;
        /** 累计有效时长（秒） */
        private long activeSeconds;
        /** 累计等待时长（秒） */
        private long waitSeconds;
        /** 有耗时快照的会话数，用于判断有效时长的覆盖率 */
        private long metricSessionCount;
        /** 平均端到端首音频耗时（毫秒），无快照时为 0 */
        private double avgFirstAudioMs;
        private String firstTime;
        private String lastTime;
    }

    @Data
    public static class DailyItem {
        private String date;
        private long sessions;
        private long messages;
    }

    @Data
    public static class DeviceItem {
        private String macAddress;
        private String name;
        private long sessions;
        private long messages;
        private long activeSeconds;
        private long waitSeconds;
    }

    @Data
    public static class AgentItem {
        private String agentId;
        private String name;
        private long sessions;
        private long messages;
    }

    @Data
    public static class BucketItem {
        private String label;
        private long count;
    }

    @Data
    public static class SessionDuration {
        private String sessionId;
        private String title;
        private String deviceName;
        private long activeSeconds;
        private long waitSeconds;
        private long rounds;
    }

    @Data
    public static class ScatterItem {
        private long rounds;
        private long spanSeconds;
        private String title;
    }
}
