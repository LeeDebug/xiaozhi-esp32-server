package xiaozhi.modules.agent.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.repository.IRepository;

import xiaozhi.common.page.PageData;
import xiaozhi.modules.agent.dto.AgentChatHistoryDTO;
import xiaozhi.modules.agent.dto.AgentChatSessionDTO;
import xiaozhi.modules.agent.dto.ChatRecordQueryDTO;
import xiaozhi.modules.agent.entity.AgentChatHistoryEntity;
import xiaozhi.modules.agent.vo.AgentChatHistoryUserVO;
import xiaozhi.modules.agent.vo.ChatRecordFilterVO;
import xiaozhi.modules.agent.vo.ChatRecordVO;
import xiaozhi.modules.agent.vo.ChatSessionVO;

/**
 * 智能体会话记录表处理service
 *
 * @author Goody
 * @version 1.0, 2025/4/30
 * @since 1.0.0
 */
public interface AgentChatHistoryService extends IRepository<AgentChatHistoryEntity> {

    /**
     * 根据智能体ID获取会话列表
     *
     * @param params 查询参数，包含agentId、page、limit
     * @return 分页的会话列表
     */
    PageData<AgentChatSessionDTO> getSessionListByAgentId(Map<String, Object> params);

    /**
     * 根据会话ID获取会话记录列表
     *
     * @param agentId   智能体ID
     * @param sessionId 会话ID
     * @return 会话记录列表
     */
    List<AgentChatHistoryDTO> getChatHistoryBySessionId(String agentId, String sessionId);

    /**
     * 根据会话ID获取智能体ID
     *
     * @param sessionId 会话ID
     * @return 智能体ID
     */
    String getAgentIdBySessionId(String sessionId);

    /**
     * 根据智能体ID删除会话记录
     *
     * @param agentId     智能体ID
     * @param deleteAudio 是否删除音频
     * @param deleteText  是否删除文本
     */
    void deleteByAgentId(String agentId, Boolean deleteAudio, Boolean deleteText);

    /**
     * 根据智能体ID获取最近50条用户的会话记录数据（带音频数据）
     *
     * @param agentId 智能体id
     * @return 会话记录列表（只有用户）
     */
    List<AgentChatHistoryUserVO> getRecentlyFiftyByAgentId(String agentId);

    /**
     * 根据音频数据ID获取聊天内容
     *
     * @param audioId 音频id
     * @return 聊天内容
     */
    String getContentByAudioId(String audioId);

    /**
     * 根据音频ID获取智能体ID
     *
     * @param audioId 音频ID
     * @return 智能体ID
     */
    String getAgentIdByAudioId(String audioId);


    /**
     * 查询此音频id是否属于此智能体
     *
     * @param audioId 音频id
     * @param agentId 音频id
     * @return T：属于 F：不属于
     */
    boolean isAudioOwnedByAgent(String audioId,String agentId);

    /**
     * 多条件分页查询会话记录（跨设备、跨智能体）
     *
     * <p>数据隔离：与智能体管理页 /agent/list 保持同一口径，只查询当前登录用户
     * 自己智能体下的记录；超级管理员也不放宽到全库，避免看到其他用户的会话。
     * 排序按 created_at 倒序；created_at 在库中为 UTC 字面值，与北京时间
     * 仅相差固定 8 小时偏移，因此倒序结果与中国时间最新在前完全一致。</p>
     *
     * @param query  查询条件（含分页）
     * @param userId 当前用户ID
     * @return 分页数据
     */
    PageData<ChatRecordVO> getChatRecordPage(ChatRecordQueryDTO query, Long userId);

    /**
     * 以会话（session_id）为维度多条件分页查询，供会话记录页面与会话详情弹窗共用。
     *
     * <p>筛选条件（设备 / 智能体 / 角色 / 时间范围 / 内容关键词）用于找出「会话内存在匹配消息」
     * 的会话；返回的消息数、首末时间、摘要等仍统计整个会话。排序按会话最后一条消息时间倒序，
     * 即中国时间最新的会话在最前。数据隔离规则与 {@link #getChatRecordPage} 一致。</p>
     *
     * @param query  查询条件（含分页）
     * @param userId 当前用户ID
     * @return 分页数据
     */
    PageData<ChatSessionVO> getChatSessionPage(ChatRecordQueryDTO query, Long userId);

    /**
     * 获取会话记录页面的设备/智能体筛选下拉选项。
     * 只返回当前登录用户拥有的智能体，以及绑定在这些智能体上的设备，
     * 与智能体管理、设备管理页面看到的范围一致（超管也不越账号）。
     *
     * @param userId 当前用户ID
     * @return 筛选项
     */
    ChatRecordFilterVO getChatRecordFilters(Long userId);
}
