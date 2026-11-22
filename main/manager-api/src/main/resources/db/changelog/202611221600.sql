-- 新增系统参数：会话分析是否过滤唤醒词
--
-- 默认 true：「会话内容分析丨高频词」(/chat-wordcloud) 与「会话内容分析丨高频方向」(/chat-direction)
--   统计时，先把消息中出现的 wakeup_words 参数里的唤醒词去掉再分词 / 归纳问题；
--   只影响分析结果，不修改数据库里的聊天记录。设为 false 则不过滤。
-- INSERT IGNORE 保证重复执行或参数已存在时不覆盖用户改过的值。
INSERT IGNORE INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark)
VALUES (900004, 'server.chat_analysis_filter_wakeup_words', 'true', 'boolean', 1,
        '会话分析是否过滤唤醒词：开启后高频词、高频方向统计会去掉 wakeup_words 参数中的唤醒词，默认true');
