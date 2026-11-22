-- 会话内容分析丨高频方向：大模型分析记录支持「转化为可视化页面」。
-- 1) ai_chat_direction_analysis 新增可视化结果列：重新生成时直接覆盖，不保留历史版本。
-- 2) sys_params 新增可视化提示词：把文字分析结论转成稳定结构的 JSON 对象，供前端 echarts 展示。
--    占位符：{conversation} 文字分析结论，{questions} 当次高频问题，{filter} 当次筛选条件。

ALTER TABLE `ai_chat_direction_analysis`
    ADD COLUMN `visual_json` MEDIUMTEXT COMMENT '可视化结果(大模型输出的 JSON 对象)',
    ADD COLUMN `visual_prompt_template` MEDIUMTEXT COMMENT '生成可视化结果时使用的提示词模板',
    ADD COLUMN `visual_model_name` VARCHAR(128) DEFAULT NULL COMMENT '生成可视化结果使用的模型',
    ADD COLUMN `visual_duration_ms` BIGINT DEFAULT NULL COMMENT '生成可视化结果的大模型耗时(毫秒)',
    ADD COLUMN `visual_generated_at` DATETIME DEFAULT NULL COMMENT '可视化结果生成时间';

INSERT IGNORE INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark)
VALUES (900005, 'server.chat_direction_visual_prompt',
        '你是一名数据结构化助手。下面是一次「用户高频提问方向分析」的文字结论，以及当时参与分析的按地区汇总的高频问题。请把文字结论转化为一个稳定的 JSON 对象，用于前端图表展示。要求：\n1、只输出一个 JSON 对象，不要输出任何解释或前后缀文字，也不要使用 Markdown 代码块\n2、必须严格使用下面的字段名和层级，不得增加、删除或改名字段，字段顺序保持一致\n3、没有对应信息的字段一律填 null，数组字段没有内容时填 []，不要编造数据\n4、ratio 表示占比，取 0～100 的数字（例如 35.5 表示 35.5%），不带百分号，无法确定时填 null\n5、count 和 questionCount 表示提问次数，取整数，无法确定时填 null，可参考「当时的高频问题」中的次数\n6、directions 按占比从高到低排序，rank 从 1 开始，topDirection 必须与 directions 的第一项一致\n7、priority 只能取「高」「中」「低」或 null\n8、字符串使用简体中文\n\nJSON 结构如下：\n{"summary":"一句话总体结论","totalQuestions":整数或null,"topDirection":{"name":"排名第一的方向","ratio":数字或null,"count":整数或null,"description":"说明"},"directions":[{"rank":1,"name":"方向名称","ratio":数字或null,"count":整数或null,"keywords":["关键词"],"representativeQuestions":["代表性问题"],"description":"说明"}],"regions":[{"name":"地区名称","questionCount":整数或null,"topDirection":"该地区排名第一的方向","directions":[{"name":"方向名称","ratio":数字或null,"count":整数或null}],"keywords":["关键词"],"difference":"与其他地区相比的差异"}],"suggestions":[{"rank":1,"title":"建议标题","content":"建议内容","priority":"高","targetDirection":"针对的方向","targetRegion":"针对的地区"}],"comprehensiveAdvice":"综合建议","dataQuality":{"sufficient":true或false或null,"notes":"数据不足或需要注意的地方"}}\n\n筛选条件：\n{filter}\n\n当时的高频问题：\n{questions}\n\n文字分析结论：\n{conversation}',
        'string', 1,
        '会话内容分析丨高频方向：把 AI 分析结论转为可视化 JSON 的提示词，{conversation} 为文字结论，{questions} 为当次高频问题，{filter} 为筛选条件');
