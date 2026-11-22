package xiaozhi.modules.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 大模型总结前的提示词预览。只组装内容，不调用大模型，不消耗次数。
 */
@Data
@Schema(description = "高频方向大模型提示词预览")
public class ChatDirectionPromptPreviewVO {

    @Schema(description = "提示词参数编码")
    private String paramCode;

    @Schema(description = "参数管理中的原始提示词模板（含 {filter} / {conversation} 占位符）")
    private String template;

    @Schema(description = "参数未配置、使用了内置兜底提示词")
    private boolean fallback;

    @Schema(description = "填入 {filter} 的筛选条件")
    private String filterText;

    @Schema(description = "填入 {conversation} 的高频问题内容")
    private String input;

    @Schema(description = "实际发送给大模型的完整提示词")
    private String fullPrompt;

    @Schema(description = "将要使用的默认大模型名称")
    private String modelName;

    @Schema(description = "当前是否有可用的默认大模型")
    private boolean llmAvailable;
}
