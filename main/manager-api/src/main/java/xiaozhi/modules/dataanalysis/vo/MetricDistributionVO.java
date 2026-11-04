package xiaozhi.modules.dataanalysis.vo;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "指标分布")
public class MetricDistributionVO {

    @Schema(description = "指标名称")
    private String metric;

    @Schema(description = "条数")
    private Long count;

    @Schema(description = "均值")
    private BigDecimal avg;

    @Schema(description = "峰值")
    private BigDecimal max;

    @Schema(description = "谷值")
    private BigDecimal min;
}
