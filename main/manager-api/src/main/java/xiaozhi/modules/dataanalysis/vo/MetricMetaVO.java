package xiaozhi.modules.dataanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "指标元信息")
public class MetricMetaVO {

    @Schema(description = "指标key")
    private String key;

    @Schema(description = "指标名称")
    private String name;
}
