package xiaozhi.modules.sys.dto;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 修改用户昵称的请求体。
 * 昵称允许为空（清空后回退显示登录账号），允许重复，仅作展示用。
 */
@Data
@Schema(description = "修改用户昵称")
public class NicknameDTO implements Serializable {

    @Schema(description = "用户昵称，最长64个字符，可为空")
    private String nickname;
}
