package xiaozhi.modules.agent.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 高频方向明细查询的 MyBatis 参数对象（内部使用，不直接绑定 HTTP 参数）。
 *
 * <p>时间为与库中 created_at 同口径的 UTC 字面值；visibleAgentIds 为空表示不限制（超管 + 参数开启）。</p>
 */
@Data
public class ChatDirectionQueryDTO {

    /** 普通用户可见的智能体ID；为 null 表示不限制 */
    private List<String> visibleAgentIds;

    private String province;

    private String agentId;

    private String macAddress;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 最多读取的消息条数（最新优先） */
    private int limit;
}
