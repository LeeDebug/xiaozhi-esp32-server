-- 设备上报数据记录表：AI数据分析（数据预览/数据分析/数据预测）的真实数据来源
CREATE TABLE IF NOT EXISTS data_report_record (
  id varchar(36) NOT NULL COMMENT '记录ID',
  device_id varchar(64) NOT NULL COMMENT '设备唯一ID',
  metric_key varchar(64) NOT NULL COMMENT '指标英文key',
  metric_name varchar(128) NOT NULL COMMENT '指标展示名称',
  value decimal(12,2) DEFAULT NULL COMMENT '数值',
  status tinyint NOT NULL DEFAULT 1 COMMENT '状态 1正常 0异常',
  report_time datetime DEFAULT NULL COMMENT '设备采集时间',
  create_time datetime DEFAULT NULL COMMENT '入库时间',
  primary key (id),
  key idx_metric_time (metric_key, report_time),
  key idx_device (device_id),
  key idx_report_time (report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备上报数据记录';
