package xiaozhi.modules.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会话内容高频方向查询参数。时间参数与会话记录页一致，使用 epoch 毫秒。
 */
@Data
@Schema(description = "会话内容高频方向查询参数")
public class ChatDirectionRequestDTO {

    @Schema(description = "省份完整名称，空表示全部地区")
    private String province;

    @Schema(description = "智能体ID，精确匹配")
    private String agentId;

    @Schema(description = "设备MAC地址，精确匹配")
    private String macAddress;

    @Schema(description = "起始时间，epoch毫秒（含）")
    private Long startTime;

    @Schema(description = "结束时间，epoch毫秒（含）")
    private Long endTime;

    @Schema(description = "每个分组参与分析的高频词数，默认 20")
    private int topN = 20;
}
