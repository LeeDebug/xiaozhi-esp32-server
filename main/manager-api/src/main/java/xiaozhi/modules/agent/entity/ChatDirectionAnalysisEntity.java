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

    @TableField("input_content")
    private String inputContent;

    private String result;

    @TableField("model_name")
    private String modelName;

    @TableField("created_at")
    private Date createdAt;
}
