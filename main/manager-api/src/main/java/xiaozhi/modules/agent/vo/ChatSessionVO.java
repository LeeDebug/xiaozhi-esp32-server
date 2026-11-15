package xiaozhi.modules.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 聊天会话列表项（以 session_id 为维度聚合）。
 *
 * <p>时间统一返回 createdAtMs（绝对时刻毫秒）与 createdAtText（Asia/Shanghai 格式化后的
 * 中国时间字符串），与会话记录列表口径一致，前端无需处理时区。</p>
 */
@Data
@Schema(description = "聊天会话列表项")
public class ChatSessionVO {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "会话标题，未生成标题时为空")
    private String sessionTitle;

    @Schema(description = "设备MAC地址")
    private String macAddress;

    @Schema(description = "设备别名")
    private String deviceAlias;

    @Schema(description = "智能体ID")
    private String agentId;

    @Schema(description = "智能体名称")
    private String agentName;

    @Schema(description = "会话首条消息时间，epoch毫秒")
    private Long startedAtMs;

    @Schema(description = "会话首条消息时间，中国时间 yyyy-MM-dd HH:mm:ss")
    private String startedAtText;

    @Schema(description = "会话最后一条消息时间，epoch毫秒")
    private Long createdAtMs;

    @Schema(description = "会话最后一条消息时间，中国时间 yyyy-MM-dd HH:mm:ss")
    private String createdAtText;

    @Schema(description = "会话内消息条数")
    private Integer chatCount;

    @Schema(description = "会话首条消息内容摘要")
    private String summary;

    @Schema(description = "会话内是否包含语音")
    private Boolean hasAudio;

    @Schema(description = "所属用户名（会话所属智能体的归属账号）")
    private String ownerUsername;

    @Schema(description = "是否属于当前登录用户")
    private Boolean isOwn;
}
