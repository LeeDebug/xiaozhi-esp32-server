package xiaozhi.modules.dataanalysis.vo;

import java.math.BigDecimal;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "数据记录VO")
public class DataReportRecordVO {

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "指标key")
    private String metricKey;

    @Schema(description = "指标名称")
    private String metric;

    @Schema(description = "取值范围说明")
    private String valueRange;

    @Schema(description = "数值")
    private BigDecimal value;

    @Schema(description = "状态 normal/abnormal")
    private String status;

    @Schema(description = "采集时间(格式化)")
    private String time;

    @Schema(description = "采集时间")
    private Date reportTime;
}
