package xiaozhi.modules.device.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备更新DTO
 */
@Data
public class DeviceUpdateDTO implements Serializable {
    /**
    * 自动更新状态
    */
    @Max(1)
    @Min(0)
    private Integer autoUpdate;

    /**
    * 设备别名
    */
    @Size(max = 64)
    private String alias;

    /**
     * 所在省份（完整名称，如「广东省」）。与 city/district 一起提交；三者都为空字符串表示清除手动地域。
     */
    @Size(max = 32)
    private String province;

    /**
     * 所在城市（完整名称，如「深圳市」）
     */
    @Size(max = 32)
    private String city;

    /**
     * 所在区县（完整名称，如「南山区」）
     */
    @Size(max = 32)
    private String district;

    private static final long serialVersionUID = 1L;
}
