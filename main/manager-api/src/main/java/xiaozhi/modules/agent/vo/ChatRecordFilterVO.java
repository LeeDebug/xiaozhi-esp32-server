package xiaozhi.modules.agent.vo;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话记录页面的筛选下拉选项
 */
@Data
@Schema(description = "会话记录筛选选项")
public class ChatRecordFilterVO {

    @Schema(description = "可选智能体")
    private List<AgentOption> agents = new ArrayList<>();

    @Schema(description = "可选设备")
    private List<DeviceOption> devices = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "智能体选项")
    public static class AgentOption {
        private String agentId;
        private String agentName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "设备选项")
    public static class DeviceOption {
        private String macAddress;
        private String alias;
        private String agentId;
    }
}
