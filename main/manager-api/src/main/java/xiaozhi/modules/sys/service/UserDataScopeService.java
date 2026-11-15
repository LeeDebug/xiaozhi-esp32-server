package xiaozhi.modules.sys.service;

import java.util.Collection;
import java.util.Map;

/**
 * 用户数据可见范围与归属展示。
 *
 * <p>由参数 {@code server.allow_super_admin_view_all} 控制：默认 false 时，
 * 任何账号（含超级管理员）都只能读取自己名下的智能体、设备与会话记录；
 * 设为 true 时，仅超级管理员可以读取全库数据。普通用户始终只能读自己的数据。</p>
 */
public interface UserDataScopeService {

    /**
     * 当前登录用户是否可以查看全部用户的数据（超级管理员 + 参数开启）。
     */
    boolean canViewAllUserData();

    /**
     * 当前登录用户ID。
     */
    Long currentUserId();

    /**
     * 指定归属用户是否就是当前登录用户。
     */
    boolean isOwn(Long ownerUserId);

    /**
     * 批量查询用户名，用于在列表数据上标注归属。
     *
     * @param userIds 归属用户ID集合
     * @return userId -> username
     */
    Map<Long, String> getUsernameMap(Collection<Long> userIds);
}
