package xiaozhi.modules.device.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 设备声纹权限：某台设备上某个声纹用户单独配置的权限码。没有记录的声纹使用默认权限。
 */
@Data
@TableName("ai_device_voiceprint_permission")
public class DeviceVoicePrintPermissionEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** 设备ID(ai_device.id) */
    private String deviceId;
    /** 声纹ID(ai_agent_voice_print.id) */
    private String voiceprintId;
    /** 权限码，逗号分隔；all 表示全部权限；空串表示无任何工具权限 */
    private String permissions;

    @TableField(fill = FieldFill.INSERT)
    private Long creator;
    @TableField(fill = FieldFill.INSERT)
    private Date createDate;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updater;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateDate;
}
