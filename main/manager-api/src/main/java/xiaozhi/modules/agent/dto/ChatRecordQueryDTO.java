package xiaozhi.modules.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 聊天记录多条件分页查询参数
 *
 * <p>时间参数统一使用 epoch 毫秒（绝对时刻），由前端按中国时间（UTC+8）解释后传入，
 * 避免直接传递 "yyyy-MM-dd HH:mm:ss" 字符串带来的服务器/浏览器时区歧义。</p>
 */
@Data
@Schema(description = "聊天记录查询参数")
public class ChatRecordQueryDTO {

    @Schema(description = "当前页码，从1开始")
    private Integer page = 1;

    @Schema(description = "每页条数，最大100")
    private Integer limit = 10;

    @Schema(description = "设备MAC地址，精确匹配")
    private String macAddress;

    @Schema(description = "智能体ID，精确匹配")
    private String agentId;

    @Schema(description = "会话ID，精确匹配")
    private String sessionId;

    @Schema(description = "聊天内容关键词，模糊匹配")
    private String keyword;

    @Schema(description = "消息类型：1-用户，2-智能体，3-工具")
    private Integer chatType;

    @Schema(description = "起始时间，epoch毫秒（含）")
    private Long startTime;

    @Schema(description = "结束时间，epoch毫秒（含）")
    private Long endTime;
}
