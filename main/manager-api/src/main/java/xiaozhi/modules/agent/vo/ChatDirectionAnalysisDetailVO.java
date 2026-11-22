package xiaozhi.modules.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 大模型分析记录的「原始记录」：当次实际使用的提示词、用户问题、模型、耗时与统计快照。
 * 只在点击「查看原始记录」时按 id 查询，列表接口不返回这些大字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "高频方向大模型分析原始记录")
public class ChatDirectionAnalysisDetailVO extends ChatDirectionAnalysisVO {

    @Schema(description = "当次读取的提示词参数编码")
    private String promptParamCode;

    @Schema(description = "是否使用了内置兜底提示词")
    private Boolean promptFallback;

    @Schema(description = "当次使用的提示词模板（含占位符）")
    private String promptTemplate;

    @Schema(description = "填入 {filter} 的筛选条件描述")
    private String filterText;

    @Schema(description = "填入 {conversation} 的高频问题内容")
    private String inputContent;

    @Schema(description = "实际发送给大模型的完整提示词")
    private String fullPrompt;

    @Schema(description = "每组参与分析的条数")
    private Integer topN;

    @Schema(description = "参与统计的用户提问条数")
    private Integer messageCount;

    @Schema(description = "统计时是否被截断")
    private Boolean truncated;

    @Schema(description = "大模型调用耗时（毫秒）")
    private Long durationMs;

    @Schema(description = "当次统计结果快照；旧记录为空")
    private ChatDirectionVO statSnapshot;

    @Schema(description = "是否为未保存原始记录的旧数据（添加该功能前生成）")
    private boolean legacy;
}
