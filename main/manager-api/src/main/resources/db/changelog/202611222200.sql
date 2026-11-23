-- 声纹权限：按「设备 × 声纹」配置对话中可用的功能（工具）。
-- 1) ai_device_voiceprint_permission：某台设备上某个声纹用户单独配置的权限码；没有记录的声纹使用默认权限。
-- 2) sys_params：总开关、权限项与工具名的映射、默认权限、未识别声纹的权限、未归类工具的处理方式。
--    权限项 tools 支持通配符（* ?），不区分大小写，按顺序取第一个命中的权限项，所以更具体的要放前面。

CREATE TABLE IF NOT EXISTS `ai_device_voiceprint_permission` (
    `id` VARCHAR(32) NOT NULL COMMENT '主键',
    `device_id` VARCHAR(64) NOT NULL COMMENT '设备ID(ai_device.id)',
    `voiceprint_id` VARCHAR(64) NOT NULL COMMENT '声纹ID(ai_agent_voice_print.id)',
    `permissions` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '权限码，逗号分隔，all 表示全部权限，空表示无任何工具权限',
    `creator` BIGINT DEFAULT NULL COMMENT '创建者',
    `create_date` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updater` BIGINT DEFAULT NULL COMMENT '更新者',
    `update_date` DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_voiceprint` (`device_id`, `voiceprint_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备声纹权限';

INSERT IGNORE INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark)
VALUES (900006, 'server.voiceprint_permission_enabled', 'false', 'boolean', 1,
        '声纹权限总开关：true 时按「设备 × 声纹」限制对话中可用的工具，修改后设备重新连接生效');

INSERT IGNORE INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark)
VALUES (900007, 'server.voiceprint_permission_items',
        '[{"code":"chat","name":"闲聊","tools":["get_weather","get_lunar","get_news*","web_search","play_music","change_role"]},{"code":"ttl_control","name":"操作TTL设备","tools":["*ttl*set*","*ttl*write*","*ttl*send*","*ttl*control*","*uart*send*","*uart*write*","*serial*send*","*serial*write*"]},{"code":"ttl_read","name":"查TTL设备信息","tools":["*ttl*","*uart*","*serial*"]},{"code":"knowledge_base","name":"查知识库","tools":["search_from_ragflow","*knowledge*"]},{"code":"device_read","name":"查设备信息","tools":["self_*get*","self_*status*","hass_get_state"]},{"code":"device_control","name":"操作设备","tools":["self_*","hass_*","call_device"]}]',
        'string', 1,
        '声纹权限项：JSON 数组，每项 code/name/tools(工具名通配符)，按顺序取第一个命中的权限项，更具体的放前面');

INSERT IGNORE INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark)
VALUES (900008, 'server.voiceprint_permission_default', 'chat,device_read', 'string', 1,
        '声纹权限：已注册但未在设备上单独配置的声纹用户的默认权限码，逗号分隔');

INSERT IGNORE INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark)
VALUES (900009, 'server.voiceprint_permission_unknown', 'chat', 'string', 1,
        '声纹权限：未识别出声纹（未注册或相似度低于阈值）时的权限码，逗号分隔，留空表示不能使用任何工具');

INSERT IGNORE INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark)
VALUES (900010, 'server.voiceprint_permission_unmatched_policy', 'deny', 'string', 1,
        '声纹权限：没有被任何权限项覆盖的工具如何处理，deny=只有全部权限可用，allow=所有人可用');
