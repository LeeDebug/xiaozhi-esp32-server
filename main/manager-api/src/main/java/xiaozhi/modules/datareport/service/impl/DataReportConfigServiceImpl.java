package xiaozhi.modules.datareport.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import xiaozhi.common.page.PageData;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.modules.datareport.dao.DataReportConfigDao;
import xiaozhi.modules.datareport.dto.DataReportConfigDTO;
import xiaozhi.modules.datareport.entity.DataReportConfigEntity;
import xiaozhi.modules.datareport.service.DataReportConfigService;
import xiaozhi.modules.datareport.vo.DataReportConfigVO;
import xiaozhi.modules.security.user.SecurityUser;

@Service
@AllArgsConstructor
public class DataReportConfigServiceImpl extends BaseServiceImpl<DataReportConfigDao, DataReportConfigEntity>
        implements DataReportConfigService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final DataReportConfigDao dataReportConfigDao;

    @Override
    public PageData<DataReportConfigVO> page(Map<String, Object> params) {
        IPage<DataReportConfigEntity> page = getPage(params, "create_date", false);
        LambdaQueryWrapper<DataReportConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(DataReportConfigEntity::getCreateDate);
        dataReportConfigDao.selectPage(page, wrapper);
        return new PageData<>(toVOList(page.getRecords()), page.getTotal());
    }

    @Override
    public DataReportConfigVO getById(String id) {
        return toVO(dataReportConfigDao.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataReportConfigVO create(DataReportConfigDTO dto) {
        DataReportConfigEntity entity = new DataReportConfigEntity();
        entity.setConfigName(dto.getConfigName());
        entity.setEnabled(dto.getEnabled());
        entity.setReportContent(toJson(dto.getReportContent()));
        entity.setReportInterval(dto.getReportInterval());
        entity.setReportDeviceInfo(dto.getReportDeviceInfo());
        entity.setReportLocation(dto.getReportLocation());
        entity.setOtherConfig(dto.getOtherConfig());
        entity.setCreator(SecurityUser.getUserId());
        entity.setCreateDate(new Date());
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            clearDefault();
            entity.setIsDefault(1);
        } else {
            entity.setIsDefault(0);
        }
        dataReportConfigDao.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, DataReportConfigDTO dto) {
        DataReportConfigEntity entity = dataReportConfigDao.selectById(id);
        if (entity == null) {
            return;
        }
        entity.setConfigName(dto.getConfigName());
        entity.setEnabled(dto.getEnabled());
        entity.setReportContent(toJson(dto.getReportContent()));
        entity.setReportInterval(dto.getReportInterval());
        entity.setReportDeviceInfo(dto.getReportDeviceInfo());
        entity.setReportLocation(dto.getReportLocation());
        entity.setOtherConfig(dto.getOtherConfig());
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            clearDefault();
            entity.setIsDefault(1);
        }
        entity.setUpdater(SecurityUser.getUserId());
        entity.setUpdateDate(new Date());
        dataReportConfigDao.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        dataReportConfigDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(String id) {
        clearDefault();
        LambdaUpdateWrapper<DataReportConfigEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DataReportConfigEntity::getId, id).set(DataReportConfigEntity::getIsDefault, 1);
        dataReportConfigDao.update(null, wrapper);
    }

    @Override
    public DataReportConfigVO getCurrent() {
        LambdaQueryWrapper<DataReportConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataReportConfigEntity::getIsDefault, 1).last("limit 1");
        DataReportConfigEntity entity = dataReportConfigDao.selectOne(wrapper);
        if (entity == null) {
            DataReportConfigVO vo = new DataReportConfigVO();
            vo.setEnabled(0);
            vo.setReportContent(Collections.emptyList());
            vo.setReportInterval(5);
            vo.setReportDeviceInfo(1);
            vo.setReportLocation(0);
            vo.setIsDefault(0);
            return vo;
        }
        return toVO(entity);
    }

    private void clearDefault() {
        // 仅清除当前标记为生效(is_default=1)的配置，避免无 WHERE 的全表更新被 BlockAttackInnerInterceptor 拦截
        LambdaUpdateWrapper<DataReportConfigEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DataReportConfigEntity::getIsDefault, 1)
                .set(DataReportConfigEntity::getIsDefault, 0);
        dataReportConfigDao.update(null, wrapper);
    }

    private DataReportConfigVO toVO(DataReportConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        DataReportConfigVO vo = new DataReportConfigVO();
        vo.setId(entity.getId());
        vo.setConfigName(entity.getConfigName());
        vo.setEnabled(entity.getEnabled());
        vo.setReportContent(toList(entity.getReportContent()));
        vo.setReportInterval(entity.getReportInterval());
        vo.setReportDeviceInfo(entity.getReportDeviceInfo());
        vo.setReportLocation(entity.getReportLocation());
        vo.setOtherConfig(entity.getOtherConfig());
        vo.setIsDefault(entity.getIsDefault());
        vo.setCreateDate(entity.getCreateDate());
        vo.setUpdateDate(entity.getUpdateDate());
        return vo;
    }

    private List<DataReportConfigVO> toVOList(List<DataReportConfigEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> toList(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
