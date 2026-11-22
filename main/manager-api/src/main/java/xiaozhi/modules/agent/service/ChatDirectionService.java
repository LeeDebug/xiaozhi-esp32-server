package xiaozhi.modules.agent.service;

import java.util.List;

import xiaozhi.modules.agent.dto.ChatDirectionRequestDTO;
import xiaozhi.modules.agent.vo.ChatDirectionAnalysisVO;
import xiaozhi.modules.agent.vo.ChatDirectionVO;

/**
 * 会话内容分析丨高频方向：按地区 / 智能体 / 设备统计用户提问的高频词，并支持通过大模型归纳方向。
 */
public interface ChatDirectionService {

    /**
     * 按地区 / 智能体 / 设备分别汇总用户的高频问题（整句），并附带高频词作为辅助。
     *
     * @param request 筛选条件
     * @param userId  当前登录用户，用于数据隔离
     * @param topN    每个分组保留的高频问题数（高频词同数）
     */
    ChatDirectionVO getChatDirection(ChatDirectionRequestDTO request, Long userId, int topN);

    /**
     * 调用当前默认大模型，分析当前筛选结果的高频提问偏向哪些方向，并保存分析记录。
     */
    ChatDirectionAnalysisVO analyzeByLlm(ChatDirectionRequestDTO request, Long userId, int topN);

    /**
     * 当前可见设备所在的省份列表（去重、按名称排序），用于地区筛选下拉。
     */
    List<String> listProvinces(Long userId);

    /**
     * 最近一次大模型分析记录（仅当前可见范围内），没有时返回 null。
     */
    ChatDirectionAnalysisVO getLatestAnalysis(Long userId);

    /**
     * 大模型分析历史记录，最新在前。
     */
    List<ChatDirectionAnalysisVO> listAnalyses(Long userId, int limit);
}
