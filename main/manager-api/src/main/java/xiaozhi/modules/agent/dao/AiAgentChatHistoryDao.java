package xiaozhi.modules.agent.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import xiaozhi.modules.agent.dto.ChatSessionQueryDTO;
import xiaozhi.modules.agent.entity.AgentChatHistoryEntity;

/**
 * {@link AgentChatHistoryEntity} 智能体聊天历史记录Dao对象
 *
 * @author Goody
 * @version 1.0, 2025/4/30
 * @since 1.0.0
 */
@Mapper
public interface AiAgentChatHistoryDao extends BaseMapper<AgentChatHistoryEntity> {

    /**
     * 根据智能体ID删除聊天历史记录
     *
     * @param agentId 智能体ID
     */
    void deleteHistoryByAgentId(String agentId);

    /**
     * 根据智能体ID删除音频ID
     *
     * @param agentId 智能体ID
     */
    void deleteAudioIdByAgentId(String agentId);

    /**
     * 根据智能体ID获取所有音频ID列表
     *
     * @param agentId 智能体ID
     * @return 音频ID列表
     */
    List<String> getAudioIdsByAgentId(String agentId);

    /**
     * 批量删除音频
     *
     * @param audioIds 音频ID列表
     */
    void deleteAudioByIds(@Param("audioIds") List<String> audioIds);

    /**
     * 会话维度聚合分页：按 session_id 统计整个会话的消息数、首末消息时间等。
     * 筛选条件只用于「找出命中的会话」，统计仍覆盖会话内全部消息。
     */
    List<Map<String, Object>> selectChatSessions(@Param("q") ChatSessionQueryDTO q);

    /** 会话维度聚合分页的总数（去重 session_id） */
    long countChatSessions(@Param("q") ChatSessionQueryDTO q);

    /** 批量取每个会话首条消息的正文，用于列表摘要 */
    List<Map<String, Object>> selectFirstContents(@Param("q") ChatSessionQueryDTO q);
}
