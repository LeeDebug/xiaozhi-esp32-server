package xiaozhi.modules.dataanalysis.controller;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import xiaozhi.common.page.PageData;
import xiaozhi.common.utils.Result;
import xiaozhi.modules.dataanalysis.dto.DataPredictionDTO;
import xiaozhi.modules.dataanalysis.dto.DataReportUploadDTO;
import xiaozhi.modules.dataanalysis.service.DataReportRecordService;
import xiaozhi.modules.dataanalysis.vo.DataAnalysisStatisticsVO;
import xiaozhi.modules.dataanalysis.vo.DataPredictionPointVO;
import xiaozhi.modules.dataanalysis.vo.DataReportRecordVO;
import xiaozhi.modules.dataanalysis.vo.MetricMetaVO;

@RestController
@RequestMapping("/data-analysis")
@Tag(name = "数据分析")
@AllArgsConstructor
public class DataReportRecordController {

    private final DataReportRecordService dataReportRecordService;

    @PostMapping("/upload")
    @Operation(summary = "设备数据上报落库(公开接口)")
    public Result<Void> upload(@Valid @RequestBody DataReportUploadDTO dto) {
        dataReportRecordService.upload(dto);
        return new Result<>();
    }

    @GetMapping("/preview")
    @Operation(summary = "数据预览分页")
    public Result<PageData<DataReportRecordVO>> preview(@RequestParam Map<String, Object> params) {
        return new Result<PageData<DataReportRecordVO>>().ok(dataReportRecordService.preview(params));
    }

    @GetMapping("/statistics")
    @Operation(summary = "数据分析统计")
    public Result<DataAnalysisStatisticsVO> statistics() {
        return new Result<DataAnalysisStatisticsVO>().ok(dataReportRecordService.statistics());
    }

    @GetMapping("/metrics")
    @Operation(summary = "指标列表")
    public Result<List<MetricMetaVO>> metrics() {
        return new Result<List<MetricMetaVO>>().ok(dataReportRecordService.metrics());
    }

    @PostMapping("/predict")
    @Operation(summary = "数据预测")
    public Result<List<DataPredictionPointVO>> predict(@Valid @RequestBody DataPredictionDTO dto) {
        return new Result<List<DataPredictionPointVO>>().ok(dataReportRecordService.predict(dto));
    }

    @PostMapping("/demo/seed")
    @Operation(summary = "生成演示数据(管理员)")
    public Result<Integer> seedDemo() {
        return new Result<Integer>().ok(dataReportRecordService.seedDemoData());
    }
}
