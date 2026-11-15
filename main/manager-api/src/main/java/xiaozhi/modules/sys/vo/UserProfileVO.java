package xiaozhi.modules.sys.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 当前登录用户的完整资料（个人信息页）。
 *
 * <p>只暴露可读字段，绝不包含密码、token、密钥等敏感信息。
 * displayName 统一按「昵称优先，回退登录账号」计算，前端不必自行判断。</p>
 */
@Data
@Schema(description = "当前登录用户资料")
public class UserProfileVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "登录账号 / 手机号")
    private String username;

    @Schema(description = "用户昵称，可为空")
    private String nickname;

    @Schema(description = "展示名称：昵称优先，为空时回退登录账号")
    private String displayName;

    @Schema(description = "超级管理员 0：否 1：是")
    private Integer superAdmin;

    @Schema(description = "状态 0：停用 1：正常")
    private Integer status;

    @Schema(description = "注册时间")
    private Date createDate;

    @Schema(description = "资料最后更新时间")
    private Date updateDate;

    @Schema(description = "名下设备数量")
    private Long deviceCount;

    @Schema(description = "名下智能体数量")
    private Long agentCount;

    @Schema(description = "是否可查看全部用户的数据")
    private Boolean canViewAllUserData;
}
