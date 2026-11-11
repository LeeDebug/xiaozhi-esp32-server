package xiaozhi.modules.dataanalysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 设备上报字段目录。
 * 数据来源：《AI语音管家与生产监控终端通讯控制逻辑表》「管家」工作表，按 Excel 序号顺序。
 * metricKey 统一使用 mb-mcp_ 前缀，中文仅用于 metricName / valueRange 展示。
 */
@Data
@TableName("data_report_metric")
@Schema(description = "设备上报字段目录")
public class DataReportMetricEntity {

    @TableId(type = IdType.INPUT)
    @Schema(description = "指标英文key（mb-mcp_ 前缀）")
    private String metricKey;

    @Schema(description = "Excel序号")
    private Integer seq;

    @Schema(description = "寄存器位号(0-15，仅位字段有值)")
    private Integer bitIndex;

    @Schema(description = "指标中文名称")
    private String metricName;

    @Schema(description = "字段分组")
    private String groupName;

    @Schema(description = "读写属性 R / W / R/W")
    private String rw;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "寄存器HEX地址")
    private String hexAddress;

    @Schema(description = "取值范围说明")
    private String valueRange;
}
