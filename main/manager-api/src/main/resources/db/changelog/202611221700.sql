-- 会话内容分析丨高频方向：大模型分析记录补充「原始记录」字段，用于事后回看每次分析的完整上下文。
-- 提示词可能随时在参数管理中修改，所以必须把当次实际使用的提示词快照下来，而不是展示时再去读参数。
-- 历史记录这些列为 NULL，前端按「旧记录未保存」展示。

ALTER TABLE `ai_chat_direction_analysis`
    ADD COLUMN `prompt_param_code` VARCHAR(64) DEFAULT NULL COMMENT '当次读取的提示词参数编码' AFTER `top_n`,
    ADD COLUMN `prompt_fallback` TINYINT(1) DEFAULT NULL COMMENT '是否使用了内置兜底提示词(参数未配置)' AFTER `prompt_param_code`,
    ADD COLUMN `prompt_template` MEDIUMTEXT COMMENT '当次使用的提示词模板(含占位符)' AFTER `prompt_fallback`,
    ADD COLUMN `filter_text` VARCHAR(1024) DEFAULT NULL COMMENT '填入 {filter} 的筛选条件描述' AFTER `prompt_template`,
    ADD COLUMN `full_prompt` MEDIUMTEXT COMMENT '实际发送给大模型的完整提示词' AFTER `filter_text`,
    ADD COLUMN `message_count` INT DEFAULT NULL COMMENT '参与统计的用户提问条数' AFTER `input_content`,
    ADD COLUMN `truncated` TINYINT(1) DEFAULT NULL COMMENT '统计时是否因读取上限被截断' AFTER `message_count`,
    ADD COLUMN `stat_snapshot` MEDIUMTEXT COMMENT '当次统计结果快照(JSON：按地区/智能体/设备的高频问题与高频词)' AFTER `truncated`,
    ADD COLUMN `duration_ms` BIGINT DEFAULT NULL COMMENT '大模型调用耗时(毫秒)' AFTER `model_name`;
