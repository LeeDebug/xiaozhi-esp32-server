package xiaozhi.modules.agent.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/**
 * 会话维度分页查询的 MyBatis 参数对象。
 *
 * <p>与 HTTP 层的 {@link ChatRecordQueryDTO} 分离：这里的时间已转换成与库中 created_at
 * 同口径的 UTC 字面值，并额外携带权限隔离用的可见智能体列表与分页偏移量。
 * 该对象不直接绑定请求参数，避免把内部权限字段暴露成可伪造的查询条件。</p>
 */
@Data
public class ChatSessionQueryDTO {

    /** 普通用户可见的智能体ID；为空表示超级管理员，不加限制 */
    private List<String> visibleAgentIds;

    /** 设备MAC，精确匹配消息所属设备 */
    private String macAddress;

    /** 智能体ID，精确匹配 */
    private String agentId;

    /** 内容关键词，用于找出「会话内存在匹配消息」的会话 */
    private String keyword;

    /** 消息类型，用于找出「会话内存在该角色消息」的会话 */
    private Integer chatType;

    /** 起始时间，已转换成与 created_at 同口径的 UTC 字面值 */
    private LocalDateTime startTime;

    /** 结束时间，已转换成与 created_at 同口径的 UTC 字面值 */
    private LocalDateTime endTime;

    /** 分页偏移量 */
    private long offset;

    /** 每页条数 */
    private int limit;

    /** 需要补充首条内容的会话ID列表 */
    private List<String> sessionIds;
}
