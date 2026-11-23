package xiaozhi.modules.device.service;

import java.util.Map;

import xiaozhi.modules.device.dto.DeviceVoicePrintPermissionSaveDTO;
import xiaozhi.modules.device.entity.DeviceEntity;

/**
 * 声纹权限：按「设备 × 声纹」配置对话中可用的工具，并随设备配置下发给 xiaozhi-server。
 */
public interface DeviceVoicePrintPermissionService {

    /**
     * 组装下发给 xiaozhi-server 的 voiceprint_permission 配置；总开关关闭时返回 null。
     */
    Map<String, Object> buildDeliveryConfig(DeviceEntity device);

    /**
     * 设备管理页读取：权限项、默认权限，以及该设备所属智能体下每个声纹的权限。
     */
    Map<String, Object> getDevicePermissions(String deviceId);

    /**
     * 保存设备上各声纹的权限，只有设备归属用户可以修改。
     */
    void saveDevicePermissions(String deviceId, DeviceVoicePrintPermissionSaveDTO dto);
}
