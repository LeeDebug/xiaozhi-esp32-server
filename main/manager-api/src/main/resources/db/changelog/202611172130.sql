-- 数据预览设备下拉与会话记录口径统一：上报表冗余 ai_device.mac_address。
-- device_id 保留固件上报的 Client-Id(UUID)，mac_address 用于按设备展示/筛选。

ALTER TABLE data_report_record
    ADD COLUMN mac_address VARCHAR(64) DEFAULT NULL COMMENT '设备MAC地址(ai_device.mac_address)' AFTER device_id;

ALTER TABLE data_report_record
    ADD INDEX idx_mac_time (mac_address, report_time);

-- 历史数据回填：按精确 MAC、Client-Id、ai_device.id 的优先级依次匹配。
UPDATE data_report_record r
    INNER JOIN ai_device d ON d.mac_address = r.device_id
SET r.mac_address = NULLIF(d.mac_address, '')
WHERE r.mac_address IS NULL AND r.device_id IS NOT NULL AND r.device_id <> '';

UPDATE data_report_record r
    INNER JOIN ai_device d ON d.client_id = r.device_id
SET r.mac_address = NULLIF(d.mac_address, '')
WHERE r.mac_address IS NULL AND r.device_id IS NOT NULL AND r.device_id <> '';

UPDATE data_report_record r
    INNER JOIN ai_device d ON d.id = r.device_id
SET r.mac_address = NULLIF(d.mac_address, '')
WHERE r.mac_address IS NULL AND r.device_id IS NOT NULL AND r.device_id <> '';
