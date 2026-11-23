package xiaozhi.modules.device.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.utils.JsonUtils;
import xiaozhi.modules.agent.dao.AgentVoicePrintDao;
import xiaozhi.modules.agent.entity.AgentVoicePrintEntity;
import xiaozhi.modules.device.dao.DeviceDao;
import xiaozhi.modules.device.dao.DeviceVoicePrintPermissionDao;
import xiaozhi.modules.device.dto.DeviceVoicePrintPermissionSaveDTO;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.device.entity.DeviceVoicePrintPermissionEntity;
import xiaozhi.modules.device.service.DeviceVoicePrintPermissionService;
import xiaozhi.modules.sys.dao.SysParamsDao;
import xiaozhi.modules.sys.redis.SysParamsRedis;
import xiaozhi.modules.sys.service.UserDataScopeService;

/**
 * 直接读参数 DAO + 缓存（与 UserDataScopeService 相同），不注入 SysParamsService，避免构造注入环。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceVoicePrintPermissionServiceImpl implements DeviceVoicePrintPermissionService {

    static final String PARAM_ENABLED = "server.voiceprint_permission_enabled";
    static final String PARAM_ITEMS = "server.voiceprint_permission_items";
    static final String PARAM_DEFAULT = "server.voiceprint_permission_default";
    static final String PARAM_UNKNOWN = "server.voiceprint_permission_unknown";
    static final String PARAM_UNMATCHED = "server.voiceprint_permission_unmatched_policy";
    static final String ALL = "all";

    private final DeviceVoicePrintPermissionDao permissionDao;
    private final DeviceDao deviceDao;
    private final AgentVoicePrintDao agentVoicePrintDao;
    private final SysParamsDao sysParamsDao;
    private final SysParamsRedis sysParamsRedis;
    private final UserDataScopeService userDataScopeService;

    @Override
    public Map<String, Object> buildDeliveryConfig(DeviceEntity device) {
        if (device == null || !isEnabled()) {
            return null;
        }
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("enabled", true);
        config.put("unknownPermissions", parseCodes(readParam(PARAM_UNKNOWN)));
        config.put("defaultPermissions", parseCodes(readParam(PARAM_DEFAULT)));
        config.put("unmatchedToolPolicy", unmatchedPolicy());
        config.put("items", items());

        Set<String> voiceprintIds = agentVoicePrints(device.getAgentId()).stream()
                .map(AgentVoicePrintEntity::getId)
                .collect(Collectors.toSet());
        Map<String, List<String>> speakers = new HashMap<>();
        for (DeviceVoicePrintPermissionEntity row : rowsOfDevice(device.getId())) {
            if (voiceprintIds.contains(row.getVoiceprintId())) {
                speakers.put(row.getVoiceprintId(), parseCodes(row.getPermissions()));
            }
        }
        config.put("speakers", speakers);
        return config;
    }

    @Override
    public Map<String, Object> getDevicePermissions(String deviceId) {
        DeviceEntity device = visibleDevice(deviceId);
        List<String> defaults = parseCodes(readParam(PARAM_DEFAULT));
        Map<String, String> configured = rowsOfDevice(device.getId()).stream()
                .collect(Collectors.toMap(DeviceVoicePrintPermissionEntity::getVoiceprintId,
                        r -> r.getPermissions() == null ? "" : r.getPermissions(), (a, b) -> a));

        List<Map<String, Object>> voiceprints = new ArrayList<>();
        for (AgentVoicePrintEntity vp : agentVoicePrints(device.getAgentId())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("voiceprintId", vp.getId());
            row.put("sourceName", vp.getSourceName());
            row.put("introduce", vp.getIntroduce());
            boolean custom = configured.containsKey(vp.getId());
            row.put("configured", custom);
            row.put("permissions", custom ? parseCodes(configured.get(vp.getId())) : defaults);
            voiceprints.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", isEnabled());
        result.put("readonly", !userDataScopeService.isOwn(device.getUserId()));
        result.put("items", items().stream().map(item -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", item.get("code"));
            m.put("name", item.get("name"));
            m.put("tools", item.get("tools"));
            return m;
        }).collect(Collectors.toList()));
        result.put("defaultPermissions", defaults);
        result.put("unknownPermissions", parseCodes(readParam(PARAM_UNKNOWN)));
        result.put("unmatchedToolPolicy", unmatchedPolicy());
        result.put("voiceprints", voiceprints);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDevicePermissions(String deviceId, DeviceVoicePrintPermissionSaveDTO dto) {
        DeviceEntity device = visibleDevice(deviceId);
        if (!userDataScopeService.isOwn(device.getUserId())) {
            throw new RenException("只能修改自己的设备的声纹权限");
        }
        if (dto == null || dto.getVoiceprints() == null) {
            return;
        }
        Set<String> validCodes = items().stream()
                .map(item -> String.valueOf(item.get("code")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        validCodes.add(ALL);
        Set<String> voiceprintIds = agentVoicePrints(device.getAgentId()).stream()
                .map(AgentVoicePrintEntity::getId)
                .collect(Collectors.toSet());

        for (DeviceVoicePrintPermissionSaveDTO.Item item : dto.getVoiceprints()) {
            if (item == null || !voiceprintIds.contains(item.getVoiceprintId())) {
                throw new RenException("声纹不属于该设备的智能体");
            }
            LambdaQueryWrapper<DeviceVoicePrintPermissionEntity> where = new LambdaQueryWrapper<DeviceVoicePrintPermissionEntity>()
                    .eq(DeviceVoicePrintPermissionEntity::getDeviceId, device.getId())
                    .eq(DeviceVoicePrintPermissionEntity::getVoiceprintId, item.getVoiceprintId());
            // null = 恢复默认权限：删掉单独配置
            if (item.getPermissions() == null) {
                permissionDao.delete(where);
                continue;
            }
            List<String> codes = item.getPermissions().stream()
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());
            for (String code : codes) {
                if (!validCodes.contains(code)) {
                    throw new RenException("未知的权限项：" + code);
                }
            }
            String value = codes.contains(ALL) ? ALL : String.join(",", codes);
            DeviceVoicePrintPermissionEntity existing = permissionDao.selectOne(where);
            if (existing == null) {
                DeviceVoicePrintPermissionEntity entity = new DeviceVoicePrintPermissionEntity();
                entity.setDeviceId(device.getId());
                entity.setVoiceprintId(item.getVoiceprintId());
                entity.setPermissions(value);
                permissionDao.insert(entity);
            } else {
                existing.setPermissions(value);
                permissionDao.updateById(existing);
            }
        }
    }

    private DeviceEntity visibleDevice(String deviceId) {
        DeviceEntity device = StringUtils.isBlank(deviceId) ? null : deviceDao.selectById(deviceId);
        if (device == null || !(userDataScopeService.isOwn(device.getUserId())
                || userDataScopeService.canViewAllUserData())) {
            throw new RenException("设备不存在");
        }
        return device;
    }

    private List<AgentVoicePrintEntity> agentVoicePrints(String agentId) {
        if (StringUtils.isBlank(agentId)) {
            return Collections.emptyList();
        }
        return agentVoicePrintDao.selectList(new LambdaQueryWrapper<AgentVoicePrintEntity>()
                .eq(AgentVoicePrintEntity::getAgentId, agentId)
                .orderByAsc(AgentVoicePrintEntity::getCreateDate));
    }

    private List<DeviceVoicePrintPermissionEntity> rowsOfDevice(String deviceId) {
        return permissionDao.selectList(new LambdaQueryWrapper<DeviceVoicePrintPermissionEntity>()
                .eq(DeviceVoicePrintPermissionEntity::getDeviceId, deviceId));
    }

    boolean isEnabled() {
        String value = readParam(PARAM_ENABLED);
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    private String unmatchedPolicy() {
        String value = readParam(PARAM_UNMATCHED);
        return value != null && "allow".equalsIgnoreCase(value.trim()) ? "allow" : "deny";
    }

    /** 只保留有 code 的权限项；参数缺失或 JSON 有误时返回空列表（此时所有工具都按未归类处理） */
    List<Map<String, Object>> items() {
        String raw = readParam(PARAM_ITEMS);
        if (StringUtils.isBlank(raw) || "null".equals(raw.trim())) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> list = JsonUtils.parseMapList(raw);
            if (list == null) {
                return Collections.emptyList();
            }
            return list.stream()
                    .filter(m -> m != null && StringUtils.isNotBlank(String.valueOf(m.getOrDefault("code", ""))))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("声纹权限项参数 {} 不是合法 JSON：{}", PARAM_ITEMS, e.getMessage());
            return Collections.emptyList();
        }
    }

    static List<String> parseCodes(String raw) {
        if (StringUtils.isBlank(raw) || "null".equals(raw.trim())) {
            return new ArrayList<>();
        }
        return Arrays.stream(raw.split("[,，;；\\s]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private String readParam(String code) {
        String value = sysParamsRedis.get(code);
        if (value == null) {
            value = sysParamsDao.getValueByCode(code);
            if (value != null) {
                sysParamsRedis.set(code, value);
            }
        }
        return value;
    }
}
