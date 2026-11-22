package xiaozhi.modules.agent.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 大模型分析记录（列表 / 详情共用）。
 */
@Data
@Schema(description = "高频方向大模型分析记录")
public class ChatDirectionAnalysisVO {

    private String id;

    @Schema(description = "分析时筛选的省份，空表示全部")
    private String province;

    @Schema(description = "分析时筛选的智能体名称（找不到时为 ID）")
    private String agentName;

    @Schema(description = "分析时筛选的智能体ID")
    private String agentId;

    @Schema(description = "分析时筛选的设备MAC")
    private String macAddress;

    @Schema(description = "分析时筛选的起始时间，已格式化为中国时间")
    private String startTime;

    @Schema(description = "分析时筛选的结束时间，已格式化为中国时间")
    private String endTime;

    @Schema(description = "大模型分析结果")
    private String result;

    @Schema(description = "使用的模型名称")
    private String modelName;

    @Schema(description = "分析时间，已格式化为中国时间")
    private String createdAt;

    @Schema(description = "这条记录是否属于当前登录用户")
    private Boolean isOwn;

    @Schema(description = "分析人账号名（仅查看他人数据时展示）")
    private String ownerUsername;

    @Schema(description = "分析人昵称")
    private String ownerNickname;
}
