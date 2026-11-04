-- 数据上报配置：设备开机拉取的上报字段与内容下发

DROP TABLE IF EXISTS data_report_config;

CREATE TABLE data_report_config (
  id varchar(36) NOT NULL COMMENT 'id',
  config_name varchar(100) NOT NULL COMMENT '配置名称',
  enabled tinyint NOT NULL DEFAULT 0 COMMENT '是否需要上报 0：否 1：是',
  report_content text COMMENT '上报内容(JSON字符串数组)',
  report_interval int NOT NULL DEFAULT 5 COMMENT '上报时间间隔(分钟)',
  report_device_info tinyint NOT NULL DEFAULT 1 COMMENT '是否上报设备信息 0：否 1：是',
  report_location tinyint NOT NULL DEFAULT 0 COMMENT '是否上报地理位置 0：否 1：是',
  other_config varchar(2000) COMMENT '其他配置(预留)',
  is_default tinyint NOT NULL DEFAULT 0 COMMENT '是否默认/生效配置 0：否 1：是',
  creator bigint COMMENT '创建者',
  create_date datetime COMMENT '创建时间',
  updater bigint COMMENT '更新者',
  update_date datetime COMMENT '更新时间',
  primary key (id),
  key idx_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据上报配置';
