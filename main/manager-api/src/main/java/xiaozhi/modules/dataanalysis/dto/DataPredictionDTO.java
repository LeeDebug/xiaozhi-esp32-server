package xiaozhi.modules.dataanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "数据预测请求")
public class DataPredictionDTO {

    @NotBlank
    @Schema(description = "指标key")
    private String metric;

    @NotNull
    @Min(1)
    @Max(72)
    @Schema(description = "预测时长(小时)")
    private Integer horizon;

    @Schema(description = "是否使用大模型预测，默认false(统计预测)")
    private Boolean useLLM;
}
