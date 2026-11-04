package xiaozhi.modules.dataanalysis.vo;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "数据分析统计结果")
public class DataAnalysisStatisticsVO {

    @Schema(description = "接入设备数")
    private Long deviceCount;

    @Schema(description = "数据总量")
    private Long recordCount;

    @Schema(description = "平均数值")
    private BigDecimal avgValue;

    @Schema(description = "异常条目")
    private Long abnormal;

    @Schema(description = "指标分布")
    private List<MetricDistributionVO> distribution;

    @Schema(description = "指标列表")
    private List<MetricMetaVO> metrics;
}
