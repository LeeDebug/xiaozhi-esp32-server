/**
 * 设备上报字段目录（自动生成，请勿手工逐条修改）
 * 数据来源：《AI语音管家与生产监控终端通讯控制逻辑表》→「管家」工作表，按序号顺序。
 * 约定：key 为存储 / 交互 / 传输的唯一标识（纯英文，统一 mb-mcp_ 前缀）；
 *       label 仅用于前端中文展示；range 为 Excel「取值范围」列的说明。
 * 说明：序号 167「模块状态」为 0xA6 的 16 位状态字，其位 0~9 已拆为独立字段（带 bit 属性）。
 */
export const METRIC_PREFIX = "mb-mcp_"

export const REPORT_METRICS = [
  { seq: 1, key: "mb-mcp_version", label: "版本号", group: "基础信息", rw: "R", defaultValue: "", hex: "0x00", range: "设备型号,不可修改" },
  { seq: 2, key: "mb-mcp_commAddress", label: "通信地址", group: "基础信息", rw: "R/W", defaultValue: "1", hex: "0x01", range: "设定设备RS485通信地址 1~127" },
  { seq: 3, key: "mb-mcp_silo1CurrentWeight", label: "1#当前重量", group: "料塔一", rw: "R", defaultValue: "", hex: "0x02", range: "32位有符号数 单位Kg" },
  { seq: 5, key: "mb-mcp_silo1RealTimeFeedAmount", label: "1#实时加料量", group: "料塔一", rw: "R", defaultValue: "", hex: "0x04", range: "16位有符号数 单位Kg" },
  { seq: 6, key: "mb-mcp_silo1RealTimeDischargeAmount", label: "1#实时卸料量", group: "料塔一", rw: "R", defaultValue: "", hex: "0x05", range: "16位有符号数 单位Kg" },
  { seq: 7, key: "mb-mcp_silo1DailyFeedTotal", label: "1#本日累计加料", group: "料塔一", rw: "R", defaultValue: "", hex: "0x06", range: "32位有符号数 单位Kg" },
  { seq: 9, key: "mb-mcp_silo1DailyDischargeTotal", label: "1#本日累计卸料", group: "料塔一", rw: "R", defaultValue: "", hex: "0x08", range: "32位有符号数 单位Kg" },
  { seq: 11, key: "mb-mcp_silo1TotalFeedAccumulated", label: "1#加料总累计", group: "料塔一", rw: "R", defaultValue: "", hex: "0x0A", range: "32位有符号数 单位Kg" },
  { seq: 13, key: "mb-mcp_silo1TotalDischargeAccumulated", label: "1#卸料总累计", group: "料塔一", rw: "R", defaultValue: "", hex: "0x0C", range: "32位有符号数 单位Kg" },
  { seq: 15, key: "mb-mcp_silo2CurrentWeight", label: "2#当前重量", group: "料塔二", rw: "R", defaultValue: "", hex: "0x0E", range: "32位有符号数 单位Kg" },
  { seq: 17, key: "mb-mcp_silo2RealTimeFeedAmount", label: "2#实时加料量", group: "料塔二", rw: "R", defaultValue: "", hex: "0x10", range: "16位有符号数 单位Kg" },
  { seq: 18, key: "mb-mcp_silo2RealTimeDischargeAmount", label: "2#实时卸料量", group: "料塔二", rw: "R", defaultValue: "", hex: "0x11", range: "16位有符号数 单位Kg" },
  { seq: 19, key: "mb-mcp_silo2DailyFeedTotal", label: "2#本日累计加料", group: "料塔二", rw: "R", defaultValue: "", hex: "0x12", range: "32位有符号数 单位Kg" },
  { seq: 21, key: "mb-mcp_silo2DailyDischargeTotal", label: "2#本日累计卸料", group: "料塔二", rw: "R", defaultValue: "", hex: "0x14", range: "32位有符号数 单位Kg" },
  { seq: 23, key: "mb-mcp_silo2TotalFeedAccumulated", label: "2#加料总累计", group: "料塔二", rw: "R", defaultValue: "", hex: "0x16", range: "32位有符号数 单位Kg" },
  { seq: 25, key: "mb-mcp_silo2TotalDischargeAccumulated", label: "2#卸料总累计", group: "料塔二", rw: "R", defaultValue: "", hex: "0x18", range: "32位有符号数 单位Kg" },
  { seq: 27, key: "mb-mcp_silo3CurrentWeight", label: "3#当前重量", group: "料塔三", rw: "R", defaultValue: "", hex: "0x1A", range: "32位有符号数 单位Kg" },
  { seq: 29, key: "mb-mcp_silo3RealTimeFeedAmount", label: "3#实时加料量", group: "料塔三", rw: "R", defaultValue: "", hex: "0x1C", range: "16位有符号数 单位Kg" },
  { seq: 30, key: "mb-mcp_silo3RealTimeDischargeAmount", label: "3#实时卸料量", group: "料塔三", rw: "R", defaultValue: "", hex: "0x1D", range: "16位有符号数 单位Kg" },
  { seq: 31, key: "mb-mcp_silo3DailyFeedTotal", label: "3#本日累计加料", group: "料塔三", rw: "R", defaultValue: "", hex: "0x1E", range: "32位有符号数 单位Kg" },
  { seq: 33, key: "mb-mcp_silo3DailyDischargeTotal", label: "3#本日累计卸料", group: "料塔三", rw: "R", defaultValue: "", hex: "0x20", range: "32位有符号数 单位Kg" },
  { seq: 35, key: "mb-mcp_silo3TotalFeedAccumulated", label: "3#加料总累计", group: "料塔三", rw: "R", defaultValue: "", hex: "0x22", range: "32位有符号数 单位Kg" },
  { seq: 37, key: "mb-mcp_silo3TotalDischargeAccumulated", label: "3#卸料总累计", group: "料塔三", rw: "R", defaultValue: "", hex: "0x24", range: "32位有符号数 单位Kg" },
  { seq: 39, key: "mb-mcp_silo4CurrentWeight", label: "4#当前重量", group: "料塔四", rw: "R", defaultValue: "", hex: "0x26", range: "32位有符号数 单位Kg" },
  { seq: 41, key: "mb-mcp_silo4RealTimeFeedAmount", label: "4#实时加料量", group: "料塔四", rw: "R", defaultValue: "", hex: "0x28", range: "16位有符号数 单位Kg" },
  { seq: 42, key: "mb-mcp_silo4RealTimeDischargeAmount", label: "4#实时卸料量", group: "料塔四", rw: "R", defaultValue: "", hex: "0x29", range: "16位有符号数 单位Kg" },
  { seq: 43, key: "mb-mcp_silo4DailyFeedTotal", label: "4#本日累计加料", group: "料塔四", rw: "R", defaultValue: "", hex: "0x2A", range: "32位有符号数 单位Kg" },
  { seq: 45, key: "mb-mcp_silo4DailyDischargeTotal", label: "4#本日累计卸料", group: "料塔四", rw: "R", defaultValue: "", hex: "0x2C", range: "32位有符号数 单位Kg" },
  { seq: 47, key: "mb-mcp_silo4TotalFeedAccumulated", label: "4#加料总累计", group: "料塔四", rw: "R", defaultValue: "", hex: "0x2E", range: "32位有符号数 单位Kg" },
  { seq: 49, key: "mb-mcp_silo4TotalDischargeAccumulated", label: "4#卸料总累计", group: "料塔四", rw: "R", defaultValue: "", hex: "0x30", range: "32位有符号数 单位Kg" },
  { seq: 51, key: "mb-mcp_dailyTotalWaterUsage", label: "本日总用水量", group: "水表通讯数据", rw: "R", defaultValue: "", hex: "0x32", range: "浮点数 范围0.00-655.35t" },
  { seq: 52, key: "mb-mcp_waterMeter1", label: "水表1", group: "水表通讯数据", rw: "R", defaultValue: "", hex: "0x33", range: "浮点数 范围0.00-655.35t" },
  { seq: 53, key: "mb-mcp_waterMeter2", label: "水表2", group: "水表通讯数据", rw: "R", defaultValue: "", hex: "0x34", range: "浮点数 范围0.00-655.35t" },
  { seq: 54, key: "mb-mcp_waterMeter3", label: "水表3", group: "水表通讯数据", rw: "R", defaultValue: "", hex: "0x35", range: "浮点数 范围0.00-655.35t" },
  { seq: 55, key: "mb-mcp_waterMeter4", label: "水表4", group: "水表通讯数据", rw: "R", defaultValue: "", hex: "0x36", range: "浮点数 范围0.00-655.35t" },
  { seq: 56, key: "mb-mcp_waterMeter5", label: "水表5", group: "水表通讯数据", rw: "R", defaultValue: "", hex: "0x37", range: "浮点数 范围0.00-655.35t" },
  { seq: 57, key: "mb-mcp_waterMeter6", label: "水表6", group: "水表通讯数据", rw: "R", defaultValue: "", hex: "0x38", range: "浮点数 范围0.00-655.35t" },
  { seq: 58, key: "mb-mcp_waterMeter7", label: "水表7", group: "水表通讯数据", rw: "R", defaultValue: "", hex: "0x39", range: "浮点数 范围0.00-655.35t" },
  { seq: 59, key: "mb-mcp_waterMeter8", label: "水表8", group: "水表通讯数据", rw: "R", defaultValue: "", hex: "0x3A", range: "浮点数 范围0.00-655.35t" },
  { seq: 60, key: "mb-mcp_currentBrightness1", label: "当前亮度1", group: "灯光通讯数据", rw: "R/W", defaultValue: "", hex: "0x3B", range: "00关灯 16位无符号数 百分比 范围0-100" },
  { seq: 61, key: "mb-mcp_currentBrightness2", label: "当前亮度2", group: "灯光通讯数据", rw: "R/W", defaultValue: "", hex: "0x3C", range: "00关灯 16位无符号数 百分比 范围0-100" },
  { seq: 62, key: "mb-mcp_currentBrightness3", label: "当前亮度3", group: "灯光通讯数据", rw: "R/W", defaultValue: "", hex: "0x3D", range: "00关灯 16位无符号数 百分比 范围0-100" },
  { seq: 63, key: "mb-mcp_currentBrightness4", label: "当前亮度4", group: "灯光通讯数据", rw: "R/W", defaultValue: "", hex: "0x3E", range: "00关灯 16位无符号数 百分比 范围0-100" },
  { seq: 64, key: "mb-mcp_voltageA1Front", label: "当前电压值A1（前端操作间）", group: "用电量通讯数据", rw: "R", defaultValue: "", hex: "0x3F", range: "16位无符号数" },
  { seq: 65, key: "mb-mcp_voltageB1Front", label: "当前电压值B1（前端操作间）", group: "用电量通讯数据", rw: "R", defaultValue: "", hex: "0x40", range: "16位无符号数" },
  { seq: 66, key: "mb-mcp_voltageC1Front", label: "当前电压值C1（前端操作间）", group: "用电量通讯数据", rw: "R", defaultValue: "", hex: "0x41", range: "16位无符号数" },
  { seq: 67, key: "mb-mcp_voltageA2Rear", label: "当前电压值A2（后端操作间）", group: "用电量通讯数据", rw: "R", defaultValue: "", hex: "0x42", range: "16位无符号数" },
  { seq: 68, key: "mb-mcp_voltageB2Rear", label: "当前电压值B2（后端操作间）", group: "用电量通讯数据", rw: "R", defaultValue: "", hex: "0x43", range: "16位无符号数" },
  { seq: 69, key: "mb-mcp_voltageC2Rear", label: "当前电压值C2（后端操作间）", group: "用电量通讯数据", rw: "R", defaultValue: "", hex: "0x44", range: "16位无符号数" },
  { seq: 70, key: "mb-mcp_energy1Front", label: "电能1（前端操作间）", group: "用电量通讯数据", rw: "R", defaultValue: "", hex: "0x45", range: "16位无符号数" },
  { seq: 71, key: "mb-mcp_energy2Rear", label: "电能2（后端操作间）", group: "用电量通讯数据", rw: "R", defaultValue: "", hex: "0x46", range: "16位无符号数" },
  { seq: 72, key: "mb-mcp_chickenCount", label: "鸡只数量", group: "系统状态", rw: "R", defaultValue: "", hex: "0x47", range: "32位无符号数" },
  { seq: 74, key: "mb-mcp_dailyMortalityCount", label: "每日死淘数量", group: "系统状态", rw: "R/W", defaultValue: "", hex: "0x49", range: "16位无符号数" },
  { seq: 75, key: "mb-mcp_waterFeedRatio", label: "水料比", group: "系统状态", rw: "R", defaultValue: "", hex: "0x4A", range: "浮点数 范围0.000-99.999" },
  { seq: 76, key: "mb-mcp_chickenFeedConsumption", label: "鸡只耗料量", group: "系统状态", rw: "R", defaultValue: "", hex: "0x4B", range: "浮点数 范围0.000-99.999" },
  { seq: 77, key: "mb-mcp_chickenWaterConsumption", label: "鸡只耗水量", group: "系统状态", rw: "R", defaultValue: "", hex: "0x4C", range: "浮点数 范围0.000-99.999" },
  { seq: 78, key: "mb-mcp_systemTime", label: "时间", group: "系统状态", rw: "W", defaultValue: "", hex: "0x4D", range: "16位无符号数" },
  { seq: 79, key: "mb-mcp_waterLineMonitorOfflineAlarm", label: "水线监测离线报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x4E", range: "0 无报警 01-1D分别对应模块1-28离线 1E多个模块离线" },
  { seq: 80, key: "mb-mcp_waterLineFlushOfflineAlarm", label: "水线冲洗离线报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x4F", range: "0 无报警 01-17分别对应模块1-22离线 1E多个模块离线" },
  { seq: 81, key: "mb-mcp_drinkingControlOfflineAlarm", label: "饮水控制离线报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x50", range: "0 无报警 01-08分别对应模块1-8离线 1E多个模块离线" },
  { seq: 82, key: "mb-mcp_dimmingModuleOfflineAlarm", label: "调光模块离线报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x51", range: "0 无报警 01-04分别对应模块1-4离线 1E多个模块离线" },
  { seq: 83, key: "mb-mcp_weighingModuleOfflineAlarm", label: "称重模块离线报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x52", range: "0 无报警 01-02分别对应模块1-2离线 1E多个模块离线" },
  { seq: 84, key: "mb-mcp_voltageMonitorOfflineAlarm", label: "电压监测离线报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x53", range: "0 无报警 01-02分别对应模块1-2离线 1E多个模块离线" },
  { seq: 85, key: "mb-mcp_column1WaterShortageAlarm", label: "一列缺水报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x54", range: "0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警" },
  { seq: 86, key: "mb-mcp_column2WaterShortageAlarm", label: "二列缺水报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x55", range: "0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警" },
  { seq: 87, key: "mb-mcp_column3WaterShortageAlarm", label: "三列缺水报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x56", range: "0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警" },
  { seq: 88, key: "mb-mcp_column4WaterShortageAlarm", label: "四列缺水报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x57", range: "0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警" },
  { seq: 89, key: "mb-mcp_column5WaterShortageAlarm", label: "五列缺水报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x58", range: "0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警" },
  { seq: 90, key: "mb-mcp_column6WaterShortageAlarm", label: "六列缺水报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x59", range: "0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警" },
  { seq: 91, key: "mb-mcp_column7WaterShortageAlarm", label: "七列缺水报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x5A", range: "0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警" },
  { seq: 92, key: "mb-mcp_column8WaterShortageAlarm", label: "八列缺水报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x5B", range: "0 无报警 01-1D分别对应水线1-28报警 1E多条水线报警" },
  { seq: 93, key: "mb-mcp_lightAlarm1", label: "灯光报警1", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x5C", range: "0 无报警 01过载 02过热 1E过载+过热报警" },
  { seq: 94, key: "mb-mcp_lightAlarm2", label: "灯光报警2", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x5D", range: "0 无报警 01过载 02过热 1E过载+过热报警" },
  { seq: 95, key: "mb-mcp_lightAlarm3", label: "灯光报警3", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x5E", range: "0 无报警 01过载 02过热 1E过载+过热报警" },
  { seq: 96, key: "mb-mcp_lightAlarm4", label: "灯光报警4", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x5F", range: "0 无报警 01过载 02过热 1E过载+过热报警" },
  { seq: 97, key: "mb-mcp_silo1LoadCellFaultAlarm", label: "1#称重传感器异常报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x60", range: "0 无报警 01报警" },
  { seq: 98, key: "mb-mcp_silo2LoadCellFaultAlarm", label: "2#称重传感器异常报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x61", range: "0 无报警 01报警" },
  { seq: 99, key: "mb-mcp_silo3LoadCellFaultAlarm", label: "3#称重传感器异常报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x62", range: "0 无报警 01报警" },
  { seq: 100, key: "mb-mcp_silo4LoadCellFaultAlarm", label: "4#称重传感器异常报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x63", range: "0 无报警 01报警" },
  { seq: 101, key: "mb-mcp_derailmentDetectionOfflineAlarm", label: "脱轨检测模块离线报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x64", range: "0 无报警 01-07分别对应模块1-7离线 1E多个模块离线" },
  { seq: 102, key: "mb-mcp_feedLineCraneControlOfflineAlarm", label: "料线行车控制模块离线报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x65", range: "0 无报警 01报警" },
  { seq: 103, key: "mb-mcp_eggCountingOfflineAlarm", label: "鸡蛋计数离线报警", group: "报警通讯", rw: "R", defaultValue: "", hex: "0x66", range: "0 无报警 01-0E分别对应模块1-14离线 1E多个模块离线" },
  { seq: 107, key: "mb-mcp_systemLog", label: "日志", group: "系统状态", rw: "R", defaultValue: "", hex: "0x6A", range: "0无数据 01-FF 对应不同的运行日志" },
  { seq: 108, key: "mb-mcp_mainWaterValve", label: "主水阀", group: "系统状态", rw: "R/W", defaultValue: "", hex: "0x6B", range: "0无 01打开 02关闭" },
  { seq: 109, key: "mb-mcp_waterTemperature", label: "水温", group: "系统状态", rw: "R", defaultValue: "", hex: "0x6C", range: "浮点数 范围0.000-99.999" },
  { seq: 110, key: "mb-mcp_dosing", label: "加药", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "", hex: "0x6D", range: "二进制1111 1111表示全部运行 0表示未运行 从高到低分别表示加药一、二、三、四、五、六、七、八" },
  { seq: 111, key: "mb-mcp_flushing", label: "冲洗", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "", hex: "0x6E", range: "0停止 01冲洗运行 02自动补水运行 03高温换水运行" },
  { seq: 112, key: "mb-mcp_autoFlushNextRunWeek", label: "自动冲洗下次运行时间（周）", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "", hex: "0x6F", range: "16位无符号数" },
  { seq: 113, key: "mb-mcp_autoFlushNextRunHour", label: "自动冲洗下次运行时间（时间-时）", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "", hex: "0x70", range: "16位无符号数" },
  { seq: 114, key: "mb-mcp_autoFlushNextRunMinute", label: "自动冲洗下次运行时间（时间-分）", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "", hex: "0x71", range: "16位无符号数" },
  { seq: 115, key: "mb-mcp_singleGroupFlushDuration", label: "单组冲洗时间", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "0-9999", hex: "0x72", range: "16位无符号数" },
  { seq: 116, key: "mb-mcp_concurrentFlushLines", label: "同时冲洗条数", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "1-7", hex: "0x73", range: "16位无符号数" },
  { seq: 117, key: "mb-mcp_flushPositionColumn", label: "冲洗位置(列)", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "1-7", hex: "0x74", range: "16位无符号数" },
  { seq: 118, key: "mb-mcp_flushPositionLayer1", label: "冲洗位置1(层)", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "1-A8", hex: "0x75", range: "16位无符号数" },
  { seq: 119, key: "mb-mcp_flushPositionLayer2", label: "冲洗位置2(层)", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "1-A8", hex: "0x76", range: "16位无符号数" },
  { seq: 120, key: "mb-mcp_flushPositionLayer3", label: "冲洗位置3(层)", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "1-A8", hex: "0x77", range: "16位无符号数" },
  { seq: 121, key: "mb-mcp_flushPositionLayer4", label: "冲洗位置4(层)", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "1-A8", hex: "0x78", range: "16位无符号数" },
  { seq: 122, key: "mb-mcp_flushPositionLayer5", label: "冲洗位置5(层)", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "1-A8", hex: "0x79", range: "16位无符号数" },
  { seq: 123, key: "mb-mcp_flushPositionLayer6", label: "冲洗位置6(层)", group: "水线冲洗的状态和控制", rw: "R/W", defaultValue: "1-A8", hex: "0x7A", range: "16位无符号数" },
  { seq: 124, key: "mb-mcp_crane3F", label: "3F行车", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x7B", range: "00无 01前端到位 02尾端到位 03行车前进 04行车返回 05运行暂停 06行车故障 07行车未满料运行" },
  { seq: 125, key: "mb-mcp_crane3FStartTime", label: "3F行车启动时间", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x7C", range: "16位无符号数" },
  { seq: 126, key: "mb-mcp_crane2F", label: "2F行车", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x7D", range: "00无 01前端到位 02尾端到位 03行车前进 04行车返回 05运行暂停 06行车故障 07行车未满料运行" },
  { seq: 127, key: "mb-mcp_crane2FStartTime", label: "2F行车启动时间", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x7E", range: "16位无符号数" },
  { seq: 128, key: "mb-mcp_crane1F", label: "1F行车", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x7F", range: "00无 01前端到位 02尾端到位 03行车前进 04行车返回 05运行暂停 06行车故障 07行车未满料运行" },
  { seq: 129, key: "mb-mcp_crane1FStartTime", label: "1F行车启动时间", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x80", range: "16位无符号数" },
  { seq: 130, key: "mb-mcp_craneMode", label: "行车模式", group: "料线行车状态和控制", rw: "R", defaultValue: "", hex: "0x81", range: "00应急 01自动 02遥控" },
  { seq: 131, key: "mb-mcp_feedProtectionTime", label: "喂料保护时间", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x82", range: "16位无符号数" },
  { seq: 132, key: "mb-mcp_silo1FeedLineMode", label: "料塔一料线模式", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x83", range: "00无 01定时补料 02自动补料 03暂停补料" },
  { seq: 133, key: "mb-mcp_silo2FeedLineMode", label: "料塔二料线模式", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x84", range: "00无 01定时补料 02自动补料 03暂停补料" },
  { seq: 134, key: "mb-mcp_silo3FeedLineMode", label: "料塔三料线模式", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x85", range: "00无 01定时补料 02自动补料 03暂停补料" },
  { seq: 135, key: "mb-mcp_silo4FeedLineMode", label: "料塔四料线模式", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x86", range: "00无 01定时补料 02自动补料 03暂停补料" },
  { seq: 136, key: "mb-mcp_feedLineStatusSilo", label: "料线状态（料塔）", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x87", range: "二进制1111表示全部运行 0表示未运行 从高到低分别表示料塔料线一、二、三、四" },
  { seq: 137, key: "mb-mcp_feedLineStatusHouse", label: "料线状态（舍内）", group: "料线行车状态和控制", rw: "R/W", defaultValue: "", hex: "0x88", range: "二进制111表示全部运行 0表示未运行 从高到低分别表示舍内料线一、二、三" },
  { seq: 138, key: "mb-mcp_houseFeedLineAlarm", label: "舍内料线报警", group: "料线行车状态和控制", rw: "R", defaultValue: "", hex: "0x89", range: "二进制111表示全部超时报警 000表示正常 从高到低分别表示舍内料线一、二、三" },
  { seq: 139, key: "mb-mcp_crane3FDerailment", label: "3F行车脱轨", group: "料线行车状态和控制", rw: "R", defaultValue: "", hex: "0x8A", range: "二进制1111 111表示全部脱轨报警 0表示未报警 从高到低分别表示行车一、二、三、四、五、六、七" },
  { seq: 140, key: "mb-mcp_crane2FDerailment", label: "2F行车脱轨", group: "料线行车状态和控制", rw: "R", defaultValue: "", hex: "0x8B", range: "二进制1111 111表示全部脱轨报警 0表示未报警 从高到低分别表示行车一、二、三、四、五、六、七" },
  { seq: 141, key: "mb-mcp_crane1FDerailment", label: "1F行车脱轨", group: "料线行车状态和控制", rw: "R", defaultValue: "", hex: "0x8C", range: "二进制1111 111表示全部脱轨报警 0表示未报警 从高到低分别表示行车一、二、三、四、五、六、七" },
  { seq: 142, key: "mb-mcp_brightness1OnTime", label: "当前亮度1开灯时间", group: "灯光状态和控制", rw: "R/W", defaultValue: "", hex: "0x8D", range: "16位无符号数" },
  { seq: 143, key: "mb-mcp_brightness1OffTime", label: "当前亮度1关灯时间", group: "灯光状态和控制", rw: "R/W", defaultValue: "", hex: "0x8E", range: "16位无符号数" },
  { seq: 144, key: "mb-mcp_brightness2OnTime", label: "当前亮度2开灯时间", group: "灯光状态和控制", rw: "R/W", defaultValue: "", hex: "0x8F", range: "16位无符号数" },
  { seq: 145, key: "mb-mcp_brightness2OffTime", label: "当前亮度2关灯时间", group: "灯光状态和控制", rw: "R/W", defaultValue: "", hex: "0x90", range: "16位无符号数" },
  { seq: 146, key: "mb-mcp_brightness3OnTime", label: "当前亮度3开灯时间", group: "灯光状态和控制", rw: "R/W", defaultValue: "", hex: "0x91", range: "16位无符号数" },
  { seq: 147, key: "mb-mcp_brightness3OffTime", label: "当前亮度3关灯时间", group: "灯光状态和控制", rw: "R/W", defaultValue: "", hex: "0x92", range: "16位无符号数" },
  { seq: 148, key: "mb-mcp_brightness4OnTime", label: "当前亮度4开灯时间", group: "灯光状态和控制", rw: "R/W", defaultValue: "", hex: "0x93", range: "16位无符号数" },
  { seq: 149, key: "mb-mcp_brightness4OffTime", label: "当前亮度4关灯时间", group: "灯光状态和控制", rw: "R/W", defaultValue: "", hex: "0x94", range: "16位无符号数" },
  { seq: 150, key: "mb-mcp_alarmPause", label: "报警暂停", group: "灯光状态和控制", rw: "R/W", defaultValue: "", hex: "0x95", range: "01暂停" },
  { seq: 151, key: "mb-mcp_eggCountTotal", label: "鸡蛋计数总量", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0x96", range: "32位无符号数" },
  { seq: 153, key: "mb-mcp_eggCount1", label: "鸡蛋计数1", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0x98", range: "16位无符号数" },
  { seq: 154, key: "mb-mcp_eggCount2", label: "鸡蛋计数2", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0x99", range: "16位无符号数" },
  { seq: 155, key: "mb-mcp_eggCount3", label: "鸡蛋计数3", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0x9A", range: "16位无符号数" },
  { seq: 156, key: "mb-mcp_eggCount4", label: "鸡蛋计数4", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0x9B", range: "16位无符号数" },
  { seq: 157, key: "mb-mcp_eggCount5", label: "鸡蛋计数5", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0x9C", range: "16位无符号数" },
  { seq: 158, key: "mb-mcp_eggCount6", label: "鸡蛋计数6", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0x9D", range: "16位无符号数" },
  { seq: 159, key: "mb-mcp_eggCount7", label: "鸡蛋计数7", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0x9E", range: "16位无符号数" },
  { seq: 160, key: "mb-mcp_eggCount8", label: "鸡蛋计数8", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0x9F", range: "16位无符号数" },
  { seq: 161, key: "mb-mcp_eggCount9", label: "鸡蛋计数9", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0xA0", range: "16位无符号数" },
  { seq: 162, key: "mb-mcp_eggCount10", label: "鸡蛋计数10", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0xA1", range: "16位无符号数" },
  { seq: 163, key: "mb-mcp_eggCount11", label: "鸡蛋计数11", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0xA2", range: "16位无符号数" },
  { seq: 164, key: "mb-mcp_eggCount12", label: "鸡蛋计数12", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0xA3", range: "16位无符号数" },
  { seq: 165, key: "mb-mcp_eggCount13", label: "鸡蛋计数13", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0xA4", range: "16位无符号数" },
  { seq: 166, key: "mb-mcp_eggCount14", label: "鸡蛋计数14", group: "鸡蛋计数", rw: "R", defaultValue: "", hex: "0xA5", range: "16位无符号数" },
  { seq: 167, key: "mb-mcp_moduleStatus", label: "模块状态", group: "设备停启用状态", rw: "R", defaultValue: "", hex: "0xA6", range: "二进制：'0000001111111000 合同默认配置的状态；0000001111111011 无鸡蛋计数器的状态；位0~9 依次为：水线冲洗、水线检测、鸡蛋计数、脱轨检测、料线系统、电压电能检测、水阀控制、称重系统、物联网（与小牧博士物联网是否有通信）、灯光调光系统（各自 0停用 1启用，已拆为独立字段）；位10~15 未定义" },
  { seq: 167, key: "mb-mcp_moduleWaterLineFlush", label: "模块状态·位0 水线冲洗", group: "设备停启用状态", rw: "R", defaultValue: "", hex: "0xA6", range: "0xA6 位0；0 停用   1启用  合同选配", bit: 0 },
  { seq: 167, key: "mb-mcp_moduleWaterLineDetection", label: "模块状态·位1 水线检测", group: "设备停启用状态", rw: "R", defaultValue: "", hex: "0xA6", range: "0xA6 位1；0 停用   1启用  合同选配", bit: 1 },
  { seq: 167, key: "mb-mcp_moduleEggCounting", label: "模块状态·位2 鸡蛋计数", group: "设备停启用状态", rw: "R", defaultValue: "", hex: "0xA6", range: "0xA6 位2；0 停用   1启用  合同选配", bit: 2 },
  { seq: 167, key: "mb-mcp_moduleDerailmentDetection", label: "模块状态·位3 脱轨检测", group: "设备停启用状态", rw: "R", defaultValue: "", hex: "0xA6", range: "0xA6 位3；0 停用   1启用  合同默认标配", bit: 3 },
  { seq: 167, key: "mb-mcp_moduleFeedLineSystem", label: "模块状态·位4 料线系统", group: "设备停启用状态", rw: "R", defaultValue: "", hex: "0xA6", range: "0xA6 位4；0 停用  1启用  合同默认标配", bit: 4 },
  { seq: 167, key: "mb-mcp_moduleVoltageEnergy", label: "模块状态·位5 电压电能检测", group: "设备停启用状态", rw: "R", defaultValue: "", hex: "0xA6", range: "0xA6 位5；0 停用   1启用  合同默认标配", bit: 5 },
  { seq: 167, key: "mb-mcp_moduleWaterValveControl", label: "模块状态·位6 水阀控制", group: "设备停启用状态", rw: "R", defaultValue: "", hex: "0xA6", range: "0xA6 位6；0 停用   1启用  合同默认标配", bit: 6 },
  { seq: 167, key: "mb-mcp_moduleWeighingSystem", label: "模块状态·位7 称重系统", group: "设备停启用状态", rw: "R", defaultValue: "", hex: "0xA6", range: "0xA6 位7；0 停用   1启用  合同默认标配", bit: 7 },
  { seq: 167, key: "mb-mcp_moduleIotCommunication", label: "模块状态·位8 物联网（与小牧博士物联网是否有通信）", group: "设备停启用状态", rw: "R", defaultValue: "", hex: "0xA6", range: "0xA6 位8；0 停用   1启用  合同默认标配", bit: 8 },
  { seq: 167, key: "mb-mcp_moduleLightDimming", label: "模块状态·位9 灯光调光系统", group: "设备停启用状态", rw: "R", defaultValue: "", hex: "0xA6", range: "0xA6 位9；0 停用   1启用  合同默认标配", bit: 9 },
]

export const REPORT_METRIC_GROUPS = (() => {
  const groups = []
  const index = {}
  REPORT_METRICS.forEach((m) => {
    if (!index[m.group]) {
      index[m.group] = { label: m.group, options: [] }
      groups.push(index[m.group])
    }
    index[m.group].options.push(m)
  })
  return groups
})()

export const METRIC_MAP = REPORT_METRICS.reduce((map, m) => {
  map[m.key] = m
  return map
}, {})

export const ALL_METRIC_KEYS = REPORT_METRICS.map((m) => m.key)

export function getMetric(key) {
  return METRIC_MAP[key] || null
}

/** 序号展示：位字段显示成 167[位0]，普通字段显示 167 */
export function metricSeqText(m) {
  if (!m) return ''
  return m.bit === undefined || m.bit === null ? String(m.seq) : `${m.seq}[位${m.bit}]`
}
