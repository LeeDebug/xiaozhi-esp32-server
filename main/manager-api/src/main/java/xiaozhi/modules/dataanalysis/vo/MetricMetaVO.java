package xiaozhi.modules.dataanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "指标元信息")
public class MetricMetaVO {

    @Schema(description = "指标key（mb-mcp_ 前缀）")
    private String key;

    @Schema(description = "指标名称（中文展示名）")
    private String name;

    @Schema(description = "Excel序号")
    private Integer seq;

    @Schema(description = "寄存器位号(0-15，仅位字段有值)")
    private Integer bit;

    @Schema(description = "字段分组")
    private String group;

    @Schema(description = "读写属性 R / W / R/W")
    private String rw;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "寄存器HEX地址")
    private String hexAddress;

    @Schema(description = "取值范围说明")
    private String valueRange;
}
