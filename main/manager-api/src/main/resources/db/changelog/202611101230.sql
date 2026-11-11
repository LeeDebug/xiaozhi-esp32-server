-- 补齐序号 167「模块状态」的位 0~9（Excel「管家」表 183~192 行）
-- v1 只收「有序号」的行，这些位说明行没有自己的 序号/DEC/HEX，曾被合并成一项。
-- 本变更集为增量：不改动已冻结的 202611101200.sql，已执行过的环境也能平滑升级。

ALTER TABLE data_report_metric
    ADD COLUMN bit_index int DEFAULT NULL COMMENT '寄存器位号(0-15，仅位字段有值)' AFTER seq;

-- 整体状态字：取值范围补全为完整位说明索引
UPDATE data_report_metric SET value_range = '二进制：''0000001111111000 合同默认配置的状态；0000001111111011 无鸡蛋计数器的状态；位0~9 依次为：水线冲洗、水线检测、鸡蛋计数、脱轨检测、料线系统、电压电能检测、水阀控制、称重系统、物联网（与小牧博士物联网是否有通信）、灯光调光系统（各自 0停用 1启用，已拆为独立字段）；位10~15 未定义'
 WHERE metric_key = 'mb-mcp_moduleStatus';

-- 位 0~9 拆成独立可上报字段
INSERT INTO data_report_metric
  (seq, bit_index, metric_key, metric_name, group_name, rw, default_value, hex_address, value_range)
VALUES
  (167, 0, 'mb-mcp_moduleWaterLineFlush', '模块状态·位0 水线冲洗', '设备停启用状态', 'R', '', '0xA6', '0xA6 位0；0 停用   1启用  合同选配'),
  (167, 1, 'mb-mcp_moduleWaterLineDetection', '模块状态·位1 水线检测', '设备停启用状态', 'R', '', '0xA6', '0xA6 位1；0 停用   1启用  合同选配'),
  (167, 2, 'mb-mcp_moduleEggCounting', '模块状态·位2 鸡蛋计数', '设备停启用状态', 'R', '', '0xA6', '0xA6 位2；0 停用   1启用  合同选配'),
  (167, 3, 'mb-mcp_moduleDerailmentDetection', '模块状态·位3 脱轨检测', '设备停启用状态', 'R', '', '0xA6', '0xA6 位3；0 停用   1启用  合同默认标配'),
  (167, 4, 'mb-mcp_moduleFeedLineSystem', '模块状态·位4 料线系统', '设备停启用状态', 'R', '', '0xA6', '0xA6 位4；0 停用  1启用  合同默认标配'),
  (167, 5, 'mb-mcp_moduleVoltageEnergy', '模块状态·位5 电压电能检测', '设备停启用状态', 'R', '', '0xA6', '0xA6 位5；0 停用   1启用  合同默认标配'),
  (167, 6, 'mb-mcp_moduleWaterValveControl', '模块状态·位6 水阀控制', '设备停启用状态', 'R', '', '0xA6', '0xA6 位6；0 停用   1启用  合同默认标配'),
  (167, 7, 'mb-mcp_moduleWeighingSystem', '模块状态·位7 称重系统', '设备停启用状态', 'R', '', '0xA6', '0xA6 位7；0 停用   1启用  合同默认标配'),
  (167, 8, 'mb-mcp_moduleIotCommunication', '模块状态·位8 物联网（与小牧博士物联网是否有通信）', '设备停启用状态', 'R', '', '0xA6', '0xA6 位8；0 停用   1启用  合同默认标配'),
  (167, 9, 'mb-mcp_moduleLightDimming', '模块状态·位9 灯光调光系统', '设备停启用状态', 'R', '', '0xA6', '0xA6 位9；0 停用   1启用  合同默认标配')
ON DUPLICATE KEY UPDATE
  seq = VALUES(seq), bit_index = VALUES(bit_index), metric_name = VALUES(metric_name),
  group_name = VALUES(group_name), rw = VALUES(rw),
  default_value = VALUES(default_value), hex_address = VALUES(hex_address),
  value_range = VALUES(value_range);
