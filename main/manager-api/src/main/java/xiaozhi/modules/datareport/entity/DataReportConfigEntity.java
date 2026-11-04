package xiaozhi.modules.datareport.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("data_report_config")
@Schema(description = "数据上报配置")
public class DataReportConfigEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "配置ID")
    private String id;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "是否需要上报 0：否 1：是")
    private Integer enabled;

    @Schema(description = "上报内容(JSON字符串数组)")
    private String reportContent;

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

    @Schema(description = "创建者")
    private Long creator;

    @Schema(description = "创建时间")
    private Date createDate;

    @Schema(description = "更新者")
    private Long updater;

    @Schema(description = "更新时间")
    private Date updateDate;
}
