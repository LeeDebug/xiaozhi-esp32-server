package xiaozhi.modules.dataanalysis.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("data_report_record")
@Schema(description = "设备上报数据记录")
public class DataReportRecordEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "记录ID")
    private String id;

    @Schema(description = "设备唯一ID")
    private String deviceId;

    @Schema(description = "指标英文key")
    private String metricKey;

    @Schema(description = "指标展示名称")
    private String metricName;

    @Schema(description = "数值")
    private BigDecimal value;

    @Schema(description = "状态 1正常 0异常")
    private Integer status;

    @Schema(description = "设备采集时间")
    private Date reportTime;

    @Schema(description = "入库时间")
    private Date createTime;
}
