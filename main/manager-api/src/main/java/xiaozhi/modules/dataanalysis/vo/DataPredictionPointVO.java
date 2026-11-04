package xiaozhi.modules.dataanalysis.vo;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "预测点")
public class DataPredictionPointVO {

    @Schema(description = "时间")
    private String time;

    @Schema(description = "预测值")
    private BigDecimal predicted;

    @Schema(description = "下限")
    private BigDecimal lower;

    @Schema(description = "上限")
    private BigDecimal upper;
}
