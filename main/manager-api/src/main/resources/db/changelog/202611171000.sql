-- 固件上报数据使用 Board UUID（OTA 的 Client-Id）作为 deviceId，
-- 而 ai_device 主键保存的是 MAC（OTA 的 Device-Id），两者不是同一个值。
-- 新增 client_id 列保存两者映射，供数据预览/分析按用户归属过滤。

ALTER TABLE ai_device
    ADD COLUMN client_id VARCHAR(64) DEFAULT NULL COMMENT '固件Client-Id(UUID)，用于数据上报归属识别' AFTER mac_address;

ALTER TABLE ai_device
    ADD INDEX idx_ai_device_client_id (client_id);
