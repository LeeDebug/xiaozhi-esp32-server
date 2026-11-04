package xiaozhi.modules.dataanalysis.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "设备数据上报请求")
public class DataReportUploadDTO {

    @Schema(description = "设备唯一ID")
    private String deviceId;

    @Schema(description = "采集时间戳(毫秒)")
    private Long timestamp;

    @Schema(description = "指标数据 key->value")
    private Map<String, Object> data;

    @Schema(description = "设备信息(可选)")
    private Map<String, Object> deviceInfo;

    @Schema(description = "地理位置(可选)")
    private Map<String, Object> location;
}
