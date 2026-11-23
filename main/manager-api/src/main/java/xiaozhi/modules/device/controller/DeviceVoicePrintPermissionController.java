package xiaozhi.modules.device.controller;

import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import xiaozhi.common.utils.Result;
import xiaozhi.modules.device.dto.DeviceVoicePrintPermissionSaveDTO;
import xiaozhi.modules.device.service.DeviceVoicePrintPermissionService;

@Tag(name = "设备声纹权限")
@RestController
@RequestMapping("/device/voiceprint-permission")
@RequiredArgsConstructor
public class DeviceVoicePrintPermissionController {

    private final DeviceVoicePrintPermissionService deviceVoicePrintPermissionService;

    @GetMapping("/{deviceId}")
    @Operation(summary = "获取设备上各声纹用户的权限")
    @RequiresPermissions("sys:role:normal")
    public Result<Map<String, Object>> get(@PathVariable String deviceId) {
        return new Result<Map<String, Object>>().ok(deviceVoicePrintPermissionService.getDevicePermissions(deviceId));
    }

    @PutMapping("/{deviceId}")
    @Operation(summary = "保存设备上各声纹用户的权限")
    @RequiresPermissions("sys:role:normal")
    public Result<Void> save(@PathVariable String deviceId, @RequestBody DeviceVoicePrintPermissionSaveDTO dto) {
        deviceVoicePrintPermissionService.saveDevicePermissions(deviceId, dto);
        return new Result<>();
    }
}
