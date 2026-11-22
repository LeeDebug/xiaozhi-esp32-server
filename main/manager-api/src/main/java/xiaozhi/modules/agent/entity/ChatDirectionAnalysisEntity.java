package xiaozhi.modules.agent.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 会话内容高频方向的大模型分析记录。每次点击「通过AI大模型总结」都会保留一条。
 */
@Data
@TableName("ai_chat_direction_analysis")
public class ChatDirectionAnalysisEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 触发分析的用户ID，用于数据隔离 */
    @TableField("user_id")
    private Long userId;

    private String province;

    @TableField("agent_id")
    private String agentId;

    @TableField("mac_address")
    private String macAddress;

    @TableField("start_time")
    private Date startTime;

    @TableField("end_time")
    private Date endTime;

    @TableField("top_n")
    private Integer topN;

    /** 当次读取的提示词参数编码 */
    @TableField("prompt_param_code")
    private String promptParamCode;

    /** 是否使用了内置兜底提示词（参数管理中未配置） */
    @TableField("prompt_fallback")
    private Boolean promptFallback;

    /** 当次使用的提示词模板（含占位符），提示词后续修改也不影响回看 */
    @TableField("prompt_template")
    private String promptTemplate;

    /** 填入 {filter} 的筛选条件描述 */
    @TableField("filter_text")
    private String filterText;

    /** 实际发送给大模型的完整提示词 */
    @TableField("full_prompt")
    private String fullPrompt;

    /** 填入 {conversation} 的高频问题内容 */
    @TableField("input_content")
    private String inputContent;

    @TableField("message_count")
    private Integer messageCount;

    private Boolean truncated;

    /** 当次统计结果快照（ChatDirectionVO 的 JSON） */
    @TableField("stat_snapshot")
    private String statSnapshot;

    private String result;

    @TableField("model_name")
    private String modelName;

    /** 大模型调用耗时（毫秒） */
    @TableField("duration_ms")
    private Long durationMs;

    @TableField("created_at")
    private Date createdAt;
}
