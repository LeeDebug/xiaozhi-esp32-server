-- liquibase formatted sql

-- changeset yishen:202611152040
-- 用户昵称：仅作展示用的可读名称，不参与登录鉴权、允许为空、允许重名。
-- 老用户统一回填为登录账号，保证迁移后所有归属标签都有可读值；
-- 新用户注册时由 SysUserServiceImpl#save 写入同样的默认值。
ALTER TABLE `sys_user`
    ADD COLUMN `nickname` VARCHAR(64) NULL DEFAULT NULL COMMENT '用户昵称（展示用，可空可重复）' AFTER `username`;

UPDATE `sys_user`
SET `nickname` = `username`
WHERE `nickname` IS NULL OR `nickname` = '';

-- rollback ALTER TABLE `sys_user` DROP COLUMN `nickname`;
