-- 智能体聊天记录：新增「本次问答耗时」信息面板所需字段
-- round_id 复用 xiaozhi-server chat() 生成的 sentence_id，用于把同一轮的
-- 用户消息 / 工具调用 / 多段回复归组；metrics_json 存结构化耗时（单位毫秒）。
-- 两列均可空：历史数据、以及尚未升级的 xiaozhi-server 上报均不受影响。
ALTER TABLE ai_agent_chat_history
    ADD COLUMN round_id VARCHAR(32) NULL COMMENT '本轮问答ID（对应 sentence_id）' AFTER audio_id,
    ADD COLUMN metrics_json TEXT NULL COMMENT '本轮耗时指标JSON（毫秒），仅末条回复记录携带' AFTER round_id,
    ADD INDEX idx_ai_agent_chat_history_round_id (round_id);
