package xiaozhi.modules.agent.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import xiaozhi.modules.agent.dto.ChatDirectionQueryDTO;

/**
 * 会话内容高频方向统计用的明细查询。
 * 独立于 AiAgentChatHistoryDao，避免改动已有的会话查询 SQL。
 */
@Mapper
public interface ChatDirectionQueryDao {

    /**
     * 查询用户提问明细（仅 chat_type=1），附带设备所在省份。
     * 设备未解析出地域时 province 为空，由调用方归入「未知地区」。
     */
    List<Map<String, Object>> selectUserQuestionRows(@Param("q") ChatDirectionQueryDTO q);
}
