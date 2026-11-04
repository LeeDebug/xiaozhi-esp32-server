package xiaozhi.modules.datareport.service;

import java.util.List;
import java.util.Map;

import xiaozhi.common.page.PageData;
import xiaozhi.modules.datareport.dto.DataReportConfigDTO;
import xiaozhi.modules.datareport.vo.DataReportConfigVO;

public interface DataReportConfigService {

    /**
     * 分页列表
     */
    PageData<DataReportConfigVO> page(Map<String, Object> params);

    /**
     * 详情
     */
    DataReportConfigVO getById(String id);

    /**
     * 新增
     */
    DataReportConfigVO create(DataReportConfigDTO dto);

    /**
     * 修改
     */
    void update(String id, DataReportConfigDTO dto);

    /**
     * 删除
     */
    void delete(String id);

    /**
     * 设为默认/生效配置（同时清除其它默认标记）
     */
    void setDefault(String id);

    /**
     * 设备开机拉取：返回当前生效配置，无生效配置时返回内置默认值
     */
    DataReportConfigVO getCurrent();
}
