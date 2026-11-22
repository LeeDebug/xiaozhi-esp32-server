-- 会话内容分析丨高频方向：按地区 / 智能体 / 设备统计用户提问方向，并支持通过大模型总结。
--
-- 1) ai_chat_direction_analysis：保留每一次大模型分析的记录，页面默认展示最近一次。
--    user_id 用于数据隔离：普通用户只能看到自己触发的分析记录。
-- 2) sys_params：大模型总结提示词放在参数管理里，可在线调整，不写死在代码中。

CREATE TABLE IF NOT EXISTS `ai_chat_direction_analysis` (
    `id` VARCHAR(32) NOT NULL COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '触发分析的用户ID',
    `province` VARCHAR(32) DEFAULT NULL COMMENT '分析时筛选的省份，空表示全部',
    `agent_id` VARCHAR(32) DEFAULT NULL COMMENT '分析时筛选的智能体，空表示全部',
    `mac_address` VARCHAR(50) DEFAULT NULL COMMENT '分析时筛选的设备MAC，空表示全部',
    `start_time` DATETIME DEFAULT NULL COMMENT '分析时筛选的起始时间(UTC字面值)',
    `end_time` DATETIME DEFAULT NULL COMMENT '分析时筛选的结束时间(UTC字面值)',
    `top_n` INT DEFAULT NULL COMMENT '每个地区参与分析的高频词数',
    `input_content` MEDIUMTEXT COMMENT '发送给大模型的高频提问内容（已汇总）',
    `result` MEDIUMTEXT COMMENT '大模型分析结果',
    `model_name` VARCHAR(128) DEFAULT NULL COMMENT '使用的模型名称',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_chat_direction_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话内容高频方向大模型分析记录';

-- id 取 900002 高位值，与 900001(server.allow_super_admin_view_all) 同段，避免与上游后续 changelog 冲突；
-- INSERT IGNORE 保证重复执行或参数已存在时不覆盖用户改过的值。
INSERT IGNORE INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark)
VALUES (900002, 'server.chat_direction_prompt',
        '你是一名资深的用户行为与需求分析专家。下面是按地区汇总的用户高频提问关键词（格式：地区：关键词(出现次数)），请分析这些提问内容主要属于哪一个或哪几个方向，要求：\n1、先给出整体结论：用户提问整体偏向哪些方向，并按占比从高到低排列\n2、再按地区逐一说明：该地区用户最关注的方向、代表性的关键词、与其他地区相比的差异\n3、方向名称要简洁明确（例如：养殖环境监测、产量统计、设备操作、天气农事、餐饮生活、娱乐闲聊等），可根据实际内容自行归纳\n4、最后给出 1～3 条可以落地的运营或产品建议\n5、只基于提供的数据下结论，不要编造数据，数据不足时请直接说明\n6、使用简体中文，用清晰的小标题和列表输出，总字数控制在 800 字以内\n\n筛选条件：\n{filter}\n\n用户高频提问关键词：\n{conversation}',
        'string', 1,
        '会话内容分析丨高频方向：大模型总结提示词，{filter} 为筛选条件描述，{conversation} 为按地区汇总的高频词');
