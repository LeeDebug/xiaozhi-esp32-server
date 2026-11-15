package xiaozhi.modules.agent.vo;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会话记录列表项
 *
 * <p>created_at 在库中是 UTC 字面值（JDBC 连接使用 serverTimezone=UTC），
 * 直接把 LocalDateTime 返回给前端会得到 UTC 墙上时间。这里统一返回
 * createdAtMs（绝对时刻毫秒）与 createdAtText（已按 Asia/Shanghai 格式化），
 * 前端无需再依赖任何 Jackson/浏览器时区配置即可显示中国时间。</p>
 */
@Data
@Schema(description = "会话记录列表项")
public class ChatRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "中国时间 yyyy-MM-dd HH:mm:ss（UTC+8）")
    private String createdAtText;

    @Schema(description = "绝对时刻，epoch毫秒")
    private Long createdAtMs;

    @Schema(description = "设备MAC地址")
    private String macAddress;

    @Schema(description = "设备别名")
    private String deviceAlias;

    @Schema(description = "智能体ID")
    private String agentId;

    @Schema(description = "智能体名称")
    private String agentName;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "会话标题")
    private String sessionTitle;

    @Schema(description = "消息类型：1-用户，2-智能体，3-工具")
    private Byte chatType;

    @Schema(description = "聊天内容（列表最多返回1000字符）")
    private String content;

    @Schema(description = "内容是否被截断")
    private Boolean contentTruncated;

    @Schema(description = "是否带音频")
    private Boolean hasAudio;

    @Schema(description = "本轮问答ID")
    private String roundId;

    @Schema(description = "本轮耗时指标，单位毫秒")
    private Map<String, Object> metrics;

    @Schema(description = "所属用户名（记录所属智能体的归属账号）")
    private String ownerUsername;

    @Schema(description = "所属用户昵称，为空时前端回退显示 ownerUsername")
    private String ownerNickname;

    @Schema(description = "是否属于当前登录用户")
    private Boolean isOwn;
}
