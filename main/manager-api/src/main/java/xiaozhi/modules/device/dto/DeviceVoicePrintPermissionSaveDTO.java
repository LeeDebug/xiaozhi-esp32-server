package xiaozhi.modules.device.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "保存设备声纹权限")
public class DeviceVoicePrintPermissionSaveDTO {

    @Schema(description = "各声纹的权限；未出现在列表里的声纹保持不变")
    private List<Item> voiceprints = new ArrayList<>();

    @Data
    public static class Item {
        @Schema(description = "声纹ID")
        private String voiceprintId;
        @Schema(description = "权限码列表；为 null 表示恢复默认权限，空列表表示没有任何工具权限")
        private List<String> permissions;
    }
}
