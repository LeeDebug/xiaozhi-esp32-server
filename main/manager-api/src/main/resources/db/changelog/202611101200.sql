-- 设备上报字段目录（来源：《AI语音管家与生产监控终端通讯控制逻辑表》「管家」工作表）
-- 1) 新建字段目录表并按 Excel 序号顺序写入全部字段（key 统一 mb-mcp_ 前缀）
-- 2) data_report_record 增加 value_range 列，用于前端展示「取值范围」
-- 3) 历史数据迁移：旧英文 key 统一补 mb-mcp_ 前缀，中文 key 由后端过滤丢弃

CREATE TABLE IF NOT EXISTS data_report_metric (
  seq int NOT NULL COMMENT 'Excel序号',
  metric_key varchar(96) NOT NULL COMMENT '指标英文key（mb-mcp_ 前缀）',
  metric_name varchar(128) NOT NULL COMMENT '指标中文名称',
  group_name varchar(64) DEFAULT NULL COMMENT '字段分组',
  rw varchar(16) DEFAULT NULL COMMENT '读写属性 R / W / R/W',
  default_value varchar(64) DEFAULT NULL COMMENT '默认值',
  hex_address varchar(16) DEFAULT NULL COMMENT '寄存器HEX地址',
  value_range varchar(512) DEFAULT NULL COMMENT '取值范围说明',
  PRIMARY KEY (metric_key),
  KEY idx_report_metric_seq (seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备上报字段目录';

INSERT INTO data_report_metric
  (seq, metric_key, metric_name, group_name, rw, default_value, hex_address, value_range)
VALUES
  (1, 'mb-mcp_version', '版本号', '基础信息', 'R', '', '0x00', '设备型号,不可修改'),
  (2, 'mb-mcp_commAddress', '通信地址', '基础信息', 'R/W', '1', '0x01', '设定设备RS485通信地址 1~127'),
  (3, 'mb-mcp_silo1CurrentWeight', '1#当前重量', '料塔一', 'R', '', '0x02', '32位有符号数 单位Kg'),
  (5, 'mb-mcp_silo1RealTimeFeedAmount', '1#实时加料量', '料塔一', 'R', '', '0x04', '16位有符号数 单位Kg'),
  (6, 'mb-mcp_silo1RealTimeDischargeAmount', '1#实时卸料量', '料塔一', 'R', '', '0x05', '16位有符号数 单位Kg'),
  (7, 'mb-mcp_silo1DailyFeedTotal', '1#本日累计加料', '料塔一', 'R', '', '0x06', '32位有符号数 单位Kg'),
  (9, 'mb-mcp_silo1DailyDischargeTotal', '1#本日累计卸料', '料塔一', 'R', '', '0x08', '32位有符号数 单位Kg'),
  (11, 'mb-mcp_silo1TotalFeedAccumulated', '1#加料总累计', '料塔一', 'R', '', '0x0A', '32位有符号数 单位Kg'),
  (13, 'mb-mcp_silo1TotalDischargeAccumulated', '1#卸料总累计', '料塔一', 'R', '', '0x0C', '32位有符号数 单位Kg'),
  (15, 'mb-mcp_silo2CurrentWeight', '2#当前重量', '料塔二', 'R', '', '0x0E', '32位有符号数 单位Kg'),
  (17, 'mb-mcp_silo2RealTimeFeedAmount', '2#实时加料量', '料塔二', 'R', '', '0x10', '16位有符号数 单位Kg'),
  (18, 'mb-mcp_silo2RealTimeDischargeAmount', '2#实时卸料量', '料塔二', 'R', '', '0x11', '16位有符号数 单位Kg'),
  (19, 'mb-mcp_silo2DailyFeedTotal', '2#本日累计加料', '料塔二', 'R', '', '0x12', '32位有符号数 单位Kg'),
  (21, 'mb-mcp_silo2DailyDischargeTotal', '2#本日累计卸料', '料塔二', 'R', '', '0x14', '32位有符号数 单位Kg'),
  (23, 'mb-mcp_silo2TotalFeedAccumulated', '2#加料总累计', '料塔二', 'R', '', '0x16', '32位有符号数 单位Kg'),
  (25, 'mb-mcp_silo2TotalDischargeAccumulated', '2#卸料总累计', '料塔二', 'R', '', '0x18', '32位有符号数 单位Kg'),
  (27, 'mb-mcp_silo3CurrentWeight', '3#当前重量', '料塔三', 'R', '', '0x1A', '32位有符号数 单位Kg'),
  (29, 'mb-mcp_silo3RealTimeFeedAmount', '3#实时加料量', '料塔三', 'R', '', '0x1C', '16位有符号数 单位Kg'),
  (30, 'mb-mcp_silo3RealTimeDischargeAmount', '3#实时卸料量', '料塔三', 'R', '', '0x1D', '16位有符号数 单位Kg'),
  (31, 'mb-mcp_silo3DailyFeedTotal', '3#本日累计加料', '料塔三', 'R', '', '0x1E', '32位有符号数 单位Kg'),
  (33, 'mb-mcp_silo3DailyDischargeTotal', '3#本日累计卸料', '料塔三', 'R', '', '0x20', '32位有符号数 单位Kg'),
  (35, 'mb-mcp_silo3TotalFeedAccumulated', '3#加料总累计', '料塔三', 'R', '', '0x22', '32位有符号数 单位Kg'),
  (37, 'mb-mcp_silo3TotalDischargeAccumulated', '3#卸料总累计', '料塔三', 'R', '', '0x24', '32位有符号数 单位Kg'),
  (39, 'mb-mcp_silo4CurrentWeight', '4#当前重量', '料塔四', 'R', '', '0x26', '32位有符号数 单位Kg'),
  (41, 'mb-mcp_silo4RealTimeFeedAmount', '4#实时加料量', '料塔四', 'R', '', '0x28', '16位有符号数 单位Kg'),
  (42, 'mb-mcp_silo4RealTimeDischargeAmount', '4#实时卸料量', '料塔四', 'R', '', '0x29', '16位有符号数 单位Kg'),
  (43, 'mb-mcp_silo4DailyFeedTotal', '4#本日累计加料', '料塔四', 'R', '', '0x2A', '32位有符号数 单位Kg'),
  (45, 'mb-mcp_silo4DailyDischargeTotal', '4#本日累计卸料', '料塔四', 'R', '', '0x2C', '32位有符号数 单位Kg'),
  (47, 'mb-mcp_silo4TotalFeedAccumulated', '4#加料总累计', '料塔四', 'R', '', '0x2E', '32位有符号数 单位Kg'),
  (49, 'mb-mcp_silo4TotalDischargeAccumulated', '4#卸料总累计', '料塔四', 'R', '', '0x30', '32位有符号数 单位Kg'),
  (51, 'mb-mcp_dailyTotalWaterUsage', '本日总用水量', '水表通讯数据', 'R', '', '0x32', '浮点数 范围0.00-655.35t'),
  (52, 'mb-mcp_waterMeter1', '水表1', '水表通讯数据', 'R', '', '0x33', '浮点数 范围0.00-655.35t'),
  (53, 'mb-mcp_waterMeter2', '水表2', '水表通讯数据', 'R', '', '0x34', '浮点数 范围0.00-655.35t'),
  (54, 'mb-mcp_waterMeter3', '水表3', '水表通讯数据', 'R', '', '0x35', '浮点数 范围0.00-655.35t'),
  (55, 'mb-mcp_waterMeter4', '水表4', '水表通讯数据', 'R', '', '0x36', '浮点数 范围0.00-655.35t'),
  (56, 'mb-mcp_waterMeter5', '水表5', '水表通讯数据', 'R', '', '0x37', '浮点数 范围0.00-655.35t'),
  (57, 'mb-mcp_waterMeter6', '水表6', '水表通讯数据', 'R', '', '0x38', '浮点数 范围0.00-655.35t'),
  (58, 'mb-mcp_waterMeter7', '水表7', '水表通讯数据', 'R', '', '0x39', '浮点数 范围0.00-655.35t'),
  (59, 'mb-mcp_waterMeter8', '水表8', '水表通讯数据', 'R', '', '0x3A', '浮点数 范围0.00-655.35t'),
  (60, 'mb-mcp_currentBrightness1', '当前亮度1', '灯光通讯数据', 'R/W', '', '0x3B', '00关灯 16位无符号数 百分比 范围0-100'),
  (61, 'mb-mcp_currentBrightness2', '当前亮度2', '灯光通讯数据', 'R/W', '', '0x3C', '00关灯 16位无符号数 百分比 范围0-100'),
  (62, 'mb-mcp_currentBrightness3', '当前亮度3', '灯光通讯数据', 'R/W', '', '0x3D', '00关灯 16位无符号数 百分比 范围0-100'),
  (63, 'mb-mcp_currentBrightness4', '当前亮度4', '灯光通讯数据', 'R/W', '', '0x3E', '00关灯 16位无符号数 百分比 范围0-100'),
  (64, 'mb-mcp_voltageA1Front', '当前电压值A1（前端操作间）', '用电量通讯数据', 'R', '', '0x3F', '16位无符号数'),
  (65, 'mb-mcp_voltageB1Front', '当前电压值B1（前端操作间）', '用电量通讯数据', 'R', '', '0x40', '16位无符号数'),
  (66, 'mb-mcp_voltageC1Front', '当前电压值C1（前端操作间）', '用电量通讯数据', 'R', '', '0x41', '16位无符号数'),
  (67, 'mb-mcp_voltageA2Rear', '当前电压值A2（后端操作间）', '用电量通讯数据', 'R', '', '0x42', '16位无符号数'),
  (68, 'mb-mcp_voltageB2Rear', '当前电压值B2（后端操作间）', '用电量通讯数据', 'R', '', '0x43', '16位无符号数'),
  (69, 'mb-mcp_voltageC2Rear', '当前电压值C2（后端操作间）', '用电量通讯数据', 'R', '', '0x44', '16位无符号数'),
  (70, 'mb-mcp_energy1Front', '电能1（前端操作间）', '用电量通讯数据', 'R', '', '0x45', '16位无符号数'),
  (71, 'mb-mcp_energy2Rear', '电能2（后端操作间）', '用电量通讯数据', 'R', '', '0x46', '16位无符号数'),
  (72, 'mb-mcp_chickenCount', '鸡只数量', '系统状态', 'R', '', '0x47', '32位无符号数'),
  (74, 'mb-mcp_dailyMortalityCount', '每日死淘数量', '系统状态', 'R/W', '', '0x49', '16位无符号数'),
  (75, 'mb-mcp_waterFeedRatio', '水料比', '系统状态', 'R', '', '0x4A', '浮点数 范围0.000-99.999'),
  (76, 'mb-mcp_chickenFeedConsumption', '鸡只耗料量', '系统状态', 'R', '', '0x4B', '浮点数 范围0.000-99.999'),
  (77, 'mb-mcp_chickenWaterConsumption', '鸡只耗水量', '系统状态', 'R', '', '0x4C', '浮点数 范围0.000-99.999'),
  (78, 'mb-mcp_systemTime', '时间', '系统状态', 'W', '', '0x4D', '16位无符号数'),
  (79, 'mb-mcp_waterLineMonitorOfflineAlarm', '水线监测离线报警', '报警通讯', 'R', '', '0x4E', '0 无报警 01-1D分别对应模块1-28离线 1E多个模块离线'),
  (80, 'mb-mcp_waterLineFlushOfflineAlarm', '水线冲洗离线报警', '报警通讯', 'R', '', '0x4F', '0 无报警 01-17分别对应模块1-22离线 1E多个模块离线'),
  (81, 'mb-mcp_drinkingControlOfflineAlarm', '饮水控制离线报警', '报警通讯', 'R', '', '0x50', '0 无报警 01-08分别对应模块1-8离线 1E多个模块离线'),
  (82, 'mb-mcp_dimmingModuleOfflineAlarm', '调光模块离线报警', '报警通讯', 'R', '', '0x51', '0 无报警 01-04分别对应模块1-4离线 1E多个模块离线'),
  (83, 'mb-mcp_weighingModuleOfflineAlarm', '称重模块离线报警', '报警通讯', 'R', '', '0x52', '0 无报警 01-02分别对应模块1-2离线 1E多个模块离线'),
  (84, 'mb-mcp_voltageMonitorOfflineAlarm', '电压监测离线报警', '报警通讯', 'R', '', '0x53', '0 无报警 01-02分别对应模块1-2离线 1E多个模块离线'),
  (85, 'mb-mcp_column1WaterShortageAlarm', '一列缺水报警', '报警通讯', 'R', '', '0x54', '0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警'),
  (86, 'mb-mcp_column2WaterShortageAlarm', '二列缺水报警', '报警通讯', 'R', '', '0x55', '0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警'),
  (87, 'mb-mcp_column3WaterShortageAlarm', '三列缺水报警', '报警通讯', 'R', '', '0x56', '0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警'),
  (88, 'mb-mcp_column4WaterShortageAlarm', '四列缺水报警', '报警通讯', 'R', '', '0x57', '0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警'),
  (89, 'mb-mcp_column5WaterShortageAlarm', '五列缺水报警', '报警通讯', 'R', '', '0x58', '0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警'),
  (90, 'mb-mcp_column6WaterShortageAlarm', '六列缺水报警', '报警通讯', 'R', '', '0x59', '0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警'),
  (91, 'mb-mcp_column7WaterShortageAlarm', '七列缺水报警', '报警通讯', 'R', '', '0x5A', '0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警'),
  (92, 'mb-mcp_column8WaterShortageAlarm', '八列缺水报警', '报警通讯', 'R', '', '0x5B', '0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警'),
  (93, 'mb-mcp_lightAlarm1', '灯光报警1', '报警通讯', 'R', '', '0x5C', '0 无报警 01过载 02过热 1E过载+过热报警'),
  (94, 'mb-mcp_lightAlarm2', '灯光报警2', '报警通讯', 'R', '', '0x5D', '0 无报警 01过载 02过热 1E过载+过热报警'),
  (95, 'mb-mcp_lightAlarm3', '灯光报警3', '报警通讯', 'R', '', '0x5E', '0 无报警 01过载 02过热 1E过载+过热报警'),
  (96, 'mb-mcp_lightAlarm4', '灯光报警4', '报警通讯', 'R', '', '0x5F', '0 无报警 01过载 02过热 1E过载+过热报警'),
  (97, 'mb-mcp_silo1LoadCellFaultAlarm', '1#称重传感器异常报警', '报警通讯', 'R', '', '0x60', '0 无报警 01报警'),
  (98, 'mb-mcp_silo2LoadCellFaultAlarm', '2#称重传感器异常报警', '报警通讯', 'R', '', '0x61', '0 无报警 01报警'),
  (99, 'mb-mcp_silo3LoadCellFaultAlarm', '3#称重传感器异常报警', '报警通讯', 'R', '', '0x62', '0 无报警 01报警'),
  (100, 'mb-mcp_silo4LoadCellFaultAlarm', '4#称重传感器异常报警', '报警通讯', 'R', '', '0x63', '0 无报警 01报警'),
  (101, 'mb-mcp_derailmentDetectionOfflineAlarm', '脱轨检测模块离线报警', '报警通讯', 'R', '', '0x64', '0 无报警 01-07分别对应模块1-7离线 1E多个模块离线'),
  (102, 'mb-mcp_feedLineCraneControlOfflineAlarm', '料线行车控制模块离线报警', '报警通讯', 'R', '', '0x65', '0 无报警 01报警'),
  (103, 'mb-mcp_eggCountingOfflineAlarm', '鸡蛋计数离线报警', '报警通讯', 'R', '', '0x66', '0 无报警 01-0E分别对应模块1-14离线 1E多个模块离线'),
  (107, 'mb-mcp_systemLog', '日志', '系统状态', 'R', '', '0x6A', '0无数据 01-FF 对应不同的运行日志'),
  (108, 'mb-mcp_mainWaterValve', '主水阀', '系统状态', 'R/W', '', '0x6B', '0无 01打开 02关闭'),
  (109, 'mb-mcp_waterTemperature', '水温', '系统状态', 'R', '', '0x6C', '浮点数 范围0.000-99.999'),
  (110, 'mb-mcp_dosing', '加药', '水线冲洗的状态和控制', 'R/W', '', '0x6D', '二进制1111 1111表示全部运行 0表示未运行 从高到低分别表示加药一、二、三、四、五、六、七、八'),
  (111, 'mb-mcp_flushing', '冲洗', '水线冲洗的状态和控制', 'R/W', '', '0x6E', '0停止 01冲洗运行 02自动补水运行 03高温换水运行'),
  (112, 'mb-mcp_autoFlushNextRunWeek', '自动冲洗下次运行时间（周）', '水线冲洗的状态和控制', 'R/W', '', '0x6F', '16位无符号数'),
  (113, 'mb-mcp_autoFlushNextRunHour', '自动冲洗下次运行时间（时间-时）', '水线冲洗的状态和控制', 'R/W', '', '0x70', '16位无符号数'),
  (114, 'mb-mcp_autoFlushNextRunMinute', '自动冲洗下次运行时间（时间-分）', '水线冲洗的状态和控制', 'R/W', '', '0x71', '16位无符号数'),
  (115, 'mb-mcp_singleGroupFlushDuration', '单组冲洗时间', '水线冲洗的状态和控制', 'R/W', '0-9999', '0x72', '16位无符号数'),
  (116, 'mb-mcp_concurrentFlushLines', '同时冲洗条数', '水线冲洗的状态和控制', 'R/W', '1-7', '0x73', '16位无符号数'),
  (117, 'mb-mcp_flushPositionColumn', '冲洗位置(列)', '水线冲洗的状态和控制', 'R/W', '1-7', '0x74', '16位无符号数'),
  (118, 'mb-mcp_flushPositionLayer1', '冲洗位置1(层)', '水线冲洗的状态和控制', 'R/W', '1-A8', '0x75', '16位无符号数'),
  (119, 'mb-mcp_flushPositionLayer2', '冲洗位置2(层)', '水线冲洗的状态和控制', 'R/W', '1-A8', '0x76', '16位无符号数'),
  (120, 'mb-mcp_flushPositionLayer3', '冲洗位置3(层)', '水线冲洗的状态和控制', 'R/W', '1-A8', '0x77', '16位无符号数'),
  (121, 'mb-mcp_flushPositionLayer4', '冲洗位置4(层)', '水线冲洗的状态和控制', 'R/W', '1-A8', '0x78', '16位无符号数'),
  (122, 'mb-mcp_flushPositionLayer5', '冲洗位置5(层)', '水线冲洗的状态和控制', 'R/W', '1-A8', '0x79', '16位无符号数'),
  (123, 'mb-mcp_flushPositionLayer6', '冲洗位置6(层)', '水线冲洗的状态和控制', 'R/W', '1-A8', '0x7A', '16位无符号数'),
  (124, 'mb-mcp_crane3F', '3F行车', '料线行车状态和控制', 'R/W', '', '0x7B', '00无 01前端到位 02尾端到位 03行车前进 04行车返回 05运行暂停 06行车故障 07行车未满料运行'),
  (125, 'mb-mcp_crane3FStartTime', '3F行车启动时间', '料线行车状态和控制', 'R/W', '', '0x7C', '16位无符号数'),
  (126, 'mb-mcp_crane2F', '2F行车', '料线行车状态和控制', 'R/W', '', '0x7D', '00无 01前端到位 02尾端到位 03行车前进 04行车返回 05运行暂停 06行车故障 07行车未满料运行'),
  (127, 'mb-mcp_crane2FStartTime', '2F行车启动时间', '料线行车状态和控制', 'R/W', '', '0x7E', '16位无符号数'),
  (128, 'mb-mcp_crane1F', '1F行车', '料线行车状态和控制', 'R/W', '', '0x7F', '00无 01前端到位 02尾端到位 03行车前进 04行车返回 05运行暂停 06行车故障 07行车未满料运行'),
  (129, 'mb-mcp_crane1FStartTime', '1F行车启动时间', '料线行车状态和控制', 'R/W', '', '0x80', '16位无符号数'),
  (130, 'mb-mcp_craneMode', '行车模式', '料线行车状态和控制', 'R', '', '0x81', '00应急 01自动 02遥控'),
  (131, 'mb-mcp_feedProtectionTime', '喂料保护时间', '料线行车状态和控制', 'R/W', '', '0x82', '16位无符号数'),
  (132, 'mb-mcp_silo1FeedLineMode', '料塔一料线模式', '料线行车状态和控制', 'R/W', '', '0x83', '00无 01定时补料 02自动补料 03暂停补料'),
  (133, 'mb-mcp_silo2FeedLineMode', '料塔二料线模式', '料线行车状态和控制', 'R/W', '', '0x84', '00无 01定时补料 02自动补料 03暂停补料'),
  (134, 'mb-mcp_silo3FeedLineMode', '料塔三料线模式', '料线行车状态和控制', 'R/W', '', '0x85', '00无 01定时补料 02自动补料 03暂停补料'),
  (135, 'mb-mcp_silo4FeedLineMode', '料塔四料线模式', '料线行车状态和控制', 'R/W', '', '0x86', '00无 01定时补料 02自动补料 03暂停补料'),
  (136, 'mb-mcp_feedLineStatusSilo', '料线状态（料塔）', '料线行车状态和控制', 'R/W', '', '0x87', '二进制1111表示全部运行 0表示未运行 从高到低分别表示料塔料线一、二、三、四'),
  (137, 'mb-mcp_feedLineStatusHouse', '料线状态（舍内）', '料线行车状态和控制', 'R/W', '', '0x88', '二进制111表示全部运行 0表示未运行 从高到低分别表示舍内料线一、二、三'),
  (138, 'mb-mcp_houseFeedLineAlarm', '舍内料线报警', '料线行车状态和控制', 'R', '', '0x89', '二进制111表示全部超时报警 000表示正常 从高到低分别表示舍内料线一、二、三'),
  (139, 'mb-mcp_crane3FDerailment', '3F行车脱轨', '料线行车状态和控制', 'R', '', '0x8A', '二进制1111 111表示全部脱轨报警 0表示未报警 从高到低分别表示行车一、二、三、四、五、六、七'),
  (140, 'mb-mcp_crane2FDerailment', '2F行车脱轨', '料线行车状态和控制', 'R', '', '0x8B', '二进制1111 111表示全部脱轨报警 0表示未报警 从高到低分别表示行车一、二、三、四、五、六、七'),
  (141, 'mb-mcp_crane1FDerailment', '1F行车脱轨', '料线行车状态和控制', 'R', '', '0x8C', '二进制1111 111表示全部脱轨报警 0表示未报警 从高到低分别表示行车一、二、三、四、五、六、七'),
  (142, 'mb-mcp_brightness1OnTime', '当前亮度1开灯时间', '灯光状态和控制', 'R/W', '', '0x8D', '16位无符号数'),
  (143, 'mb-mcp_brightness1OffTime', '当前亮度1关灯时间', '灯光状态和控制', 'R/W', '', '0x8E', '16位无符号数'),
  (144, 'mb-mcp_brightness2OnTime', '当前亮度2开灯时间', '灯光状态和控制', 'R/W', '', '0x8F', '16位无符号数'),
  (145, 'mb-mcp_brightness2OffTime', '当前亮度2关灯时间', '灯光状态和控制', 'R/W', '', '0x90', '16位无符号数'),
  (146, 'mb-mcp_brightness3OnTime', '当前亮度3开灯时间', '灯光状态和控制', 'R/W', '', '0x91', '16位无符号数'),
  (147, 'mb-mcp_brightness3OffTime', '当前亮度3关灯时间', '灯光状态和控制', 'R/W', '', '0x92', '16位无符号数'),
  (148, 'mb-mcp_brightness4OnTime', '当前亮度4开灯时间', '灯光状态和控制', 'R/W', '', '0x93', '16位无符号数'),
  (149, 'mb-mcp_brightness4OffTime', '当前亮度4关灯时间', '灯光状态和控制', 'R/W', '', '0x94', '16位无符号数'),
  (150, 'mb-mcp_alarmPause', '报警暂停', '灯光状态和控制', 'R/W', '', '0x95', '01暂停'),
  (151, 'mb-mcp_eggCountTotal', '鸡蛋计数总量', '鸡蛋计数', 'R', '', '0x96', '32位无符号数'),
  (153, 'mb-mcp_eggCount1', '鸡蛋计数1', '鸡蛋计数', 'R', '', '0x98', '16位无符号数'),
  (154, 'mb-mcp_eggCount2', '鸡蛋计数2', '鸡蛋计数', 'R', '', '0x99', '16位无符号数'),
  (155, 'mb-mcp_eggCount3', '鸡蛋计数3', '鸡蛋计数', 'R', '', '0x9A', '16位无符号数'),
  (156, 'mb-mcp_eggCount4', '鸡蛋计数4', '鸡蛋计数', 'R', '', '0x9B', '16位无符号数'),
  (157, 'mb-mcp_eggCount5', '鸡蛋计数5', '鸡蛋计数', 'R', '', '0x9C', '16位无符号数'),
  (158, 'mb-mcp_eggCount6', '鸡蛋计数6', '鸡蛋计数', 'R', '', '0x9D', '16位无符号数'),
  (159, 'mb-mcp_eggCount7', '鸡蛋计数7', '鸡蛋计数', 'R', '', '0x9E', '16位无符号数'),
  (160, 'mb-mcp_eggCount8', '鸡蛋计数8', '鸡蛋计数', 'R', '', '0x9F', '16位无符号数'),
  (161, 'mb-mcp_eggCount9', '鸡蛋计数9', '鸡蛋计数', 'R', '', '0xA0', '16位无符号数'),
  (162, 'mb-mcp_eggCount10', '鸡蛋计数10', '鸡蛋计数', 'R', '', '0xA1', '16位无符号数'),
  (163, 'mb-mcp_eggCount11', '鸡蛋计数11', '鸡蛋计数', 'R', '', '0xA2', '16位无符号数'),
  (164, 'mb-mcp_eggCount12', '鸡蛋计数12', '鸡蛋计数', 'R', '', '0xA3', '16位无符号数'),
  (165, 'mb-mcp_eggCount13', '鸡蛋计数13', '鸡蛋计数', 'R', '', '0xA4', '16位无符号数'),
  (166, 'mb-mcp_eggCount14', '鸡蛋计数14', '鸡蛋计数', 'R', '', '0xA5', '16位无符号数'),
  (167, 'mb-mcp_moduleStatus', '模块状态', '设备停启用状态', 'R', '', '0xA6', '二进制：''0000001111111000 合同默认配置的状态 0000001111111011 无鸡蛋计数器的状态')
ON DUPLICATE KEY UPDATE
  seq = VALUES(seq), metric_name = VALUES(metric_name), group_name = VALUES(group_name),
  rw = VALUES(rw), default_value = VALUES(default_value),
  hex_address = VALUES(hex_address), value_range = VALUES(value_range);

ALTER TABLE data_report_record
    MODIFY COLUMN metric_key varchar(96) NOT NULL COMMENT '指标英文key（mb-mcp_ 前缀）',
    ADD COLUMN value_range varchar(512) DEFAULT NULL COMMENT '取值范围说明（来自字段目录）' AFTER metric_name;

-- 历史记录：旧英文 key 补前缀（已带前缀的不重复处理）
UPDATE data_report_record SET metric_key = CONCAT('mb-mcp_', metric_key)
 WHERE metric_key IS NOT NULL AND metric_key NOT LIKE 'mb-mcp%';

-- 历史记录：按字段目录回填中文名与取值范围
-- 注意：项目启用了 BlockAttackInnerInterceptor，无 WHERE 的 UPDATE 会被拦截，故必须带条件
UPDATE data_report_record r
  JOIN data_report_metric m ON r.metric_key = m.metric_key
   SET r.metric_name = m.metric_name, r.value_range = m.value_range
 WHERE r.metric_key LIKE 'mb-mcp%';

-- 历史配置：report_content 中旧英文 key 补前缀（含引号，避免重复替换）
UPDATE data_report_config SET report_content = REPLACE(report_content, '"perChickenData"', '"mb-mcp_perChickenData"')
 WHERE report_content LIKE '%"perChickenData"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"chickenCount"', '"mb-mcp_chickenCount"')
 WHERE report_content LIKE '%"chickenCount"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"envExtremes"', '"mb-mcp_envExtremes"')
 WHERE report_content LIKE '%"envExtremes"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"mortalityRate"', '"mb-mcp_mortalityRate"')
 WHERE report_content LIKE '%"mortalityRate"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"waterFeedAmount"', '"mb-mcp_waterFeedAmount"')
 WHERE report_content LIKE '%"waterFeedAmount"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"eggLaying"', '"mb-mcp_eggLaying"')
 WHERE report_content LIKE '%"eggLaying"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"eggYield"', '"mb-mcp_eggYield"')
 WHERE report_content LIKE '%"eggYield"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"waterUsage"', '"mb-mcp_waterUsage"')
 WHERE report_content LIKE '%"waterUsage"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"silo1Weight"', '"mb-mcp_silo1Weight"')
 WHERE report_content LIKE '%"silo1Weight"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"silo2Weight"', '"mb-mcp_silo2Weight"')
 WHERE report_content LIKE '%"silo2Weight"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"envVentilation"', '"mb-mcp_envVentilation"')
 WHERE report_content LIKE '%"envVentilation"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"deviceStatus"', '"mb-mcp_deviceStatus"')
 WHERE report_content LIKE '%"deviceStatus"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"fanGroup"', '"mb-mcp_fanGroup"')
 WHERE report_content LIKE '%"fanGroup"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"windowCurtain"', '"mb-mcp_windowCurtain"')
 WHERE report_content LIKE '%"windowCurtain"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"specialFan"', '"mb-mcp_specialFan"')
 WHERE report_content LIKE '%"specialFan"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"cranePosition"', '"mb-mcp_cranePosition"')
 WHERE report_content LIKE '%"cranePosition"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"tempProbe"', '"mb-mcp_tempProbe"')
 WHERE report_content LIKE '%"tempProbe"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"envTemperature"', '"mb-mcp_envTemperature"')
 WHERE report_content LIKE '%"envTemperature"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"envHumidity"', '"mb-mcp_envHumidity"')
 WHERE report_content LIKE '%"envHumidity"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"otherEnvMetrics"', '"mb-mcp_otherEnvMetrics"')
 WHERE report_content LIKE '%"otherEnvMetrics"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"light"', '"mb-mcp_light"')
 WHERE report_content LIKE '%"light"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"light1Brightness"', '"mb-mcp_light1Brightness"')
 WHERE report_content LIKE '%"light1Brightness"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"light2Brightness"', '"mb-mcp_light2Brightness"')
 WHERE report_content LIKE '%"light2Brightness"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"temperature"', '"mb-mcp_temperature"')
 WHERE report_content LIKE '%"temperature"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"humidity"', '"mb-mcp_humidity"')
 WHERE report_content LIKE '%"humidity"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"ozone"', '"mb-mcp_ozone"')
 WHERE report_content LIKE '%"ozone"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"airQuality"', '"mb-mcp_airQuality"')
 WHERE report_content LIKE '%"airQuality"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"powerStatistics"', '"mb-mcp_powerStatistics"')
 WHERE report_content LIKE '%"powerStatistics"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"volume"', '"mb-mcp_volume"')
 WHERE report_content LIKE '%"volume"%';
UPDATE data_report_config SET report_content = REPLACE(report_content, '"onlineDuration"', '"mb-mcp_onlineDuration"')
 WHERE report_content LIKE '%"onlineDuration"%';
