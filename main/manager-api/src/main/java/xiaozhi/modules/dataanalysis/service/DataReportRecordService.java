package xiaozhi.modules.dataanalysis.service;

import java.util.List;
import java.util.Map;

import xiaozhi.common.page.PageData;
import xiaozhi.modules.dataanalysis.dto.DataPredictionDTO;
import xiaozhi.modules.dataanalysis.dto.DataReportUploadDTO;
import xiaozhi.modules.dataanalysis.vo.DataAnalysisStatisticsVO;
import xiaozhi.modules.dataanalysis.vo.DataPredictionPointVO;
import xiaozhi.modules.dataanalysis.vo.DataReportRecordVO;
import xiaozhi.modules.dataanalysis.vo.MetricMetaVO;

public interface DataReportRecordService {

    /**
     * 设备数据上报落库
     */
    void upload(DataReportUploadDTO dto);

    /**
     * 数据预览分页
     */
    PageData<DataReportRecordVO> preview(Map<String, Object> params);

    /**
     * 数据分析统计
     */
    DataAnalysisStatisticsVO statistics();

    /**
     * 指标列表
     */
    List<MetricMetaVO> metrics();

    /**
     * 数据预测（默认统计，可切换大模型）
     */
    List<DataPredictionPointVO> predict(DataPredictionDTO dto);

    /**
     * 一键生成演示数据
     *
     * @return 生成条数；若已存在数据返回 -1
     */
    int seedDemoData();
}
