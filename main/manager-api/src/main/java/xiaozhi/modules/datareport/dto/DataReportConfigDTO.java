package xiaozhi.modules.datareport.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "数据上报配置DTO")
public class DataReportConfigDTO {

    @Schema(description = "配置名称")
    private String configName;

    @NotNull(message = "是否需要上报不能为空")
    @Schema(description = "是否需要上报 0：否 1：是")
    private Integer enabled;

    @Schema(description = "上报内容(字符串数组)")
    private List<String> reportContent;

    @NotNull(message = "上报时间间隔不能为空")
    @Schema(description = "上报时间间隔(分钟)")
    private Integer reportInterval;

    @NotNull(message = "是否上报设备信息不能为空")
    @Schema(description = "是否上报设备信息 0：否 1：是")
    private Integer reportDeviceInfo;

    @NotNull(message = "是否上报地理位置不能为空")
    @Schema(description = "是否上报地理位置 0：否 1：是")
    private Integer reportLocation;

    @Schema(description = "其他配置(预留)")
    private String otherConfig;

    @Schema(description = "保存后是否设为默认/生效配置 0：否 1：是")
    private Integer isDefault;
}
