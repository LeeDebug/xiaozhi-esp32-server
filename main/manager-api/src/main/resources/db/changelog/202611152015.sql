-- 新增系统参数：是否允许超级管理员查看所有用户的数据
--
-- 默认 false：所有账号（含超级管理员）在智能体管理、设备管理、会话记录页
--   都只能看到自己名下的数据。
-- 设为 true：仅超级管理员可以查看全库数据；普通用户不受此开关影响。
--   列表接口会同时返回归属用户名(ownerUsername)与是否本人(isOwn)，
--   前端据此标注「我的 / 其他用户 · 用户名」。
--
-- id 取 900001 高位值，避免与上游 changelog 后续新增参数冲突；
-- INSERT IGNORE 保证重复执行或参数已存在时不覆盖用户改过的值。
INSERT IGNORE INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark)
VALUES (900001, 'server.allow_super_admin_view_all', 'false', 'boolean', 1,
        '是否允许超级管理员查看所有用户的数据（智能体/设备/会话记录），默认false');
