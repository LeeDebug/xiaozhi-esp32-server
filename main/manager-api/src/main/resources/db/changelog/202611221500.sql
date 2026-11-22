-- 会话内容分析丨高频方向：统计口径由「高频词」改为「高频问题」（整句提问按出现次数统计）。
--
-- 202611221200 已执行过，Liquibase 不允许修改已执行的变更集，所以这里新增一个参数：
--   server.chat_direction_question_prompt：以「高频问题列表」为输入的大模型总结提示词。
-- 不直接 UPDATE 旧参数的原因：参数读取优先走 Redis 缓存，改库后旧值仍会被缓存命中；
-- 换用新 param_code 可立即生效。旧参数 server.chat_direction_prompt 面向关键词，已不再使用，一并删除。

INSERT IGNORE INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark)
VALUES (900003, 'server.chat_direction_question_prompt',
        '你是一名资深的用户需求分析专家。下面是按地区汇总的用户高频提问（格式：地区（N 条提问）下列出若干条「问题（出现次数）」，问题原文可能来自语音识别，存在错别字或口语化表达），请分析这些提问主要属于哪一个或哪几个方向，要求：\n1、先把意思相近、只是说法不同的问题合并成同一类（例如「水温多少」「现在水温是多少」「水温正常吗」都属于水温相关），再给出整体结论：用户提问整体偏向哪些方向，并按提问次数占比从高到低排列\n2、再按地区逐一说明：该地区用户最关注的方向、对应的代表性问题、与其他地区相比的差异\n3、方向名称要简洁明确（例如：水温监测、产蛋量统计、温湿度监测、设备操作、天气农事、生活闲聊等），可根据实际内容自行归纳\n4、最后给出 1～3 条可以落地的运营或产品建议\n5、只基于提供的数据下结论，不要编造数据，数据不足时请直接说明\n6、使用简体中文，用清晰的小标题和列表输出，总字数控制在 1000 字以内\n\n筛选条件：\n{filter}\n\n用户高频提问：\n{conversation}',
        'string', 1,
        '会话内容分析丨高频方向：大模型总结提示词，{filter} 为筛选条件描述，{conversation} 为按地区汇总的高频问题');

DELETE FROM `sys_params` WHERE param_code = 'server.chat_direction_prompt';
