package xiaozhi.modules.sys.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import lombok.RequiredArgsConstructor;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.user.UserDetail;
import xiaozhi.modules.security.user.SecurityUser;
import xiaozhi.modules.sys.dao.SysParamsDao;
import xiaozhi.modules.sys.dao.SysUserDao;
import xiaozhi.modules.sys.entity.SysUserEntity;
import xiaozhi.modules.sys.enums.SuperAdminEnum;
import xiaozhi.modules.sys.redis.SysParamsRedis;
import xiaozhi.modules.sys.service.UserDataScopeService;

@Service
@RequiredArgsConstructor
public class UserDataScopeServiceImpl implements UserDataScopeService {

    // 直接读参数 DAO + 缓存，避免与依赖 AgentService 的业务服务形成构造注入环
    private final SysParamsDao sysParamsDao;
    private final SysParamsRedis sysParamsRedis;
    private final SysUserDao sysUserDao;

    @Override
    public boolean canViewAllUserData() {
        UserDetail user = SecurityUser.getUser();
        if (user == null || !Integer.valueOf(SuperAdminEnum.YES.value()).equals(user.getSuperAdmin())) {
            return false;
        }
        // 与 SysParamsService#getValue(code, true) 口径一致：参数管理页保存时会刷新该缓存，改完即时生效
        String value = sysParamsRedis.get(Constant.SERVER_ALLOW_SUPER_ADMIN_VIEW_ALL);
        if (value == null) {
            value = sysParamsDao.getValueByCode(Constant.SERVER_ALLOW_SUPER_ADMIN_VIEW_ALL);
            sysParamsRedis.set(Constant.SERVER_ALLOW_SUPER_ADMIN_VIEW_ALL, value);
        }
        return "true".equalsIgnoreCase(value == null ? null : value.trim());
    }

    @Override
    public Long currentUserId() {
        UserDetail user = SecurityUser.getUser();
        return user == null ? null : user.getId();
    }

    @Override
    public boolean isOwn(Long ownerUserId) {
        Long current = currentUserId();
        return ownerUserId != null && ownerUserId.equals(current);
    }

    @Override
    public Map<Long, String> getUsernameMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUserEntity> users = sysUserDao.selectList(new QueryWrapper<SysUserEntity>()
                .select("id", "username").in("id", ids));
        Map<Long, String> map = new HashMap<>();
        for (SysUserEntity user : users) {
            if (user.getId() != null) {
                map.put(user.getId(), user.getUsername());
            }
        }
        return map;
    }
}
