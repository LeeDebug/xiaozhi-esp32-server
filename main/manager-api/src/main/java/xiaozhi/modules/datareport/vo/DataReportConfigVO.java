package xiaozhi.modules.datareport.vo;

import java.util.Date;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "数据上报配置VO")
public class DataReportConfigVO {

    @Schema(description = "配置ID")
    private String id;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "是否需要上报 0：否 1：是")
    private Integer enabled;

    @Schema(description = "上报内容(字符串数组)")
    private List<String> reportContent;

    @Schema(description = "上报时间间隔(分钟)")
    private Integer reportInterval;

    @Schema(description = "是否上报设备信息 0：否 1：是")
    private Integer reportDeviceInfo;

    @Schema(description = "是否上报地理位置 0：否 1：是")
    private Integer reportLocation;

    @Schema(description = "其他配置(预留)")
    private String otherConfig;

    @Schema(description = "是否默认/生效配置 0：否 1：是")
    private Integer isDefault;

    @Schema(description = "创建时间")
    private Date createDate;

    @Schema(description = "更新时间")
    private Date updateDate;
}
