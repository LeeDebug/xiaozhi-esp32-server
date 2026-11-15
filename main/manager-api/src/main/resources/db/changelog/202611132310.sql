-- 聊天记录多条件分页查询：补充「智能体/设备 + 时间」组合索引
--
-- 说明：ai_agent_chat_history 现有字段已完整覆盖本页所需维度
--   mac_address(设备) / agent_id(智能体) / session_id(会话) / content(内容)
--   / created_at(时间) / audio_id(音频) / round_id + metrics_json(耗时)，
-- 设备别名、智能体名称、会话标题分别来自 ai_device、ai_agent、ai_agent_chat_title，
-- 因此不新增业务列，仅补充查询所需的组合索引。
--
-- 注意：内容关键词为 LIKE '%kw%'，B-Tree 索引无法命中，查询依赖智能体、
-- 设备或时间范围条件先行缩小扫描范围（前端默认提供时间快捷范围）。
-- 采用防御式写法，避免索引已存在时 Liquibase 启动失败。

SET @idx_agent_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_chat_history'
      AND INDEX_NAME = 'idx_chat_rec_agent_created'
);
SET @sql_agent = IF(@idx_agent_exists = 0,
    'ALTER TABLE ai_agent_chat_history ADD INDEX idx_chat_rec_agent_created (agent_id, created_at)',
    'SELECT ''idx_chat_rec_agent_created already exists'' AS msg');
PREPARE stmt_agent FROM @sql_agent;
EXECUTE stmt_agent;
DEALLOCATE PREPARE stmt_agent;

SET @idx_mac_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent_chat_history'
      AND INDEX_NAME = 'idx_chat_rec_mac_created'
);
SET @sql_mac = IF(@idx_mac_exists = 0,
    'ALTER TABLE ai_agent_chat_history ADD INDEX idx_chat_rec_mac_created (mac_address, created_at)',
    'SELECT ''idx_chat_rec_mac_created already exists'' AS msg');
PREPARE stmt_mac FROM @sql_mac;
EXECUTE stmt_mac;
DEALLOCATE PREPARE stmt_mac;
