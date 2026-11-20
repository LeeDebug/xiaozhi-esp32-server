package xiaozhi.modules.device.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import xiaozhi.common.redis.RedisUtils;
import xiaozhi.modules.device.dao.DeviceDao;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.device.service.DeviceAddressBookService;
import xiaozhi.modules.device.service.OtaService;
import xiaozhi.modules.device.vo.UserShowDeviceListVO;
import xiaozhi.modules.sys.service.SysParamsService;
import xiaozhi.modules.sys.service.SysUserUtilService;
import xiaozhi.modules.sys.service.UserDataScopeService;

@DisplayName("设备分析看板：可见设备列表的数据范围")
class DeviceVisibleListTest {

    private static final long USER_ID = 1L;

    @Test
    @DisplayName("普通用户：查询必须带 user_id 条件，且不限定智能体")
    @SuppressWarnings("unchecked")
    void ordinaryUserQueryIsScopedToOwnDevices() {
        UserDataScopeService scope = mock(UserDataScopeService.class);
        when(scope.canViewAllUserData()).thenReturn(false);
        DeviceDao dao = mock(DeviceDao.class);
        when(dao.selectList(any())).thenReturn(new ArrayList<>());

        service(dao, scope).getVisibleDeviceList(USER_ID);

        QueryWrapper<DeviceEntity> wrapper = capturedWrapper(dao);
        assertTrue(wrapper.getSqlSegment().contains("user_id"), "普通用户必须按 user_id 过滤，否则会看到别人的设备");
        assertFalse(wrapper.getSqlSegment().contains("agent_id"), "看板要的是全部智能体的设备");
    }

    @Test
    @DisplayName("超管且开启查看全部：不加 user_id 条件")
    @SuppressWarnings("unchecked")
    void superAdminWithViewAllSeesEveryone() {
        UserDataScopeService scope = mock(UserDataScopeService.class);
        when(scope.canViewAllUserData()).thenReturn(true);
        DeviceDao dao = mock(DeviceDao.class);
        when(dao.selectList(any())).thenReturn(new ArrayList<>());

        service(dao, scope).getVisibleDeviceList(USER_ID);

        assertFalse(capturedWrapper(dao).getSqlSegment().contains("user_id"));
    }

    @Test
    @DisplayName("返回的 VO 带省市区、地域来源和归属信息")
    void voCarriesRegionAndOwnership() {
        UserDataScopeService scope = mock(UserDataScopeService.class);
        when(scope.canViewAllUserData()).thenReturn(false);
        when(scope.getUsernameMap(anySet())).thenReturn(new HashMap<>());
        when(scope.getNicknameMap(anySet())).thenReturn(new HashMap<>());
        when(scope.isOwn(USER_ID)).thenReturn(true);

        DeviceEntity entity = new DeviceEntity();
        entity.setId("dev-1");
        entity.setUserId(USER_ID);
        entity.setProvince("广东省");
        entity.setCity("深圳市");
        entity.setDistrict("南山区");
        entity.setRegionSource("manual");
        DeviceDao dao = mock(DeviceDao.class);
        when(dao.selectList(any())).thenReturn(List.of(entity));

        List<UserShowDeviceListVO> result = service(dao, scope).getVisibleDeviceList(USER_ID);

        assertEquals(1, result.size());
        UserShowDeviceListVO vo = result.get(0);
        assertEquals("广东省", vo.getProvince());
        assertEquals("深圳市", vo.getCity());
        assertEquals("南山区", vo.getDistrict());
        assertEquals("manual", vo.getRegionSource());
        assertTrue(vo.getIsOwn());
    }

    @SuppressWarnings("unchecked")
    private QueryWrapper<DeviceEntity> capturedWrapper(DeviceDao dao) {
        ArgumentCaptor<QueryWrapper<DeviceEntity>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        Mockito.verify(dao).selectList(captor.capture());
        return captor.getValue();
    }

    private DeviceServiceImpl service(DeviceDao dao, UserDataScopeService scope) {
        return new DeviceServiceImpl(dao, mock(SysUserUtilService.class), mock(SysParamsService.class),
                mock(RedisUtils.class), mock(OtaService.class), mock(DeviceAddressBookService.class), scope) {
            {
                this.baseDao = dao;
            }
        };
    }
}
