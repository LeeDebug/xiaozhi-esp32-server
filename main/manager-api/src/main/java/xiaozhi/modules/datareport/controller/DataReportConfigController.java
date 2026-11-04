package xiaozhi.modules.datareport.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import xiaozhi.common.page.PageData;
import xiaozhi.common.utils.Result;
import xiaozhi.modules.datareport.dto.DataReportConfigDTO;
import xiaozhi.modules.datareport.service.DataReportConfigService;
import xiaozhi.modules.datareport.vo.DataReportConfigVO;

@RestController
@RequestMapping("/data-report-config")
@Tag(name = "数据上报配置")
@AllArgsConstructor
public class DataReportConfigController {

    private final DataReportConfigService dataReportConfigService;

    @GetMapping("/current")
    @Operation(summary = "设备开机获取的数据上报配置(公开)")
    public Result<DataReportConfigVO> current() {
        return new Result<DataReportConfigVO>().ok(dataReportConfigService.getCurrent());
    }

    @GetMapping("/page")
    @Operation(summary = "数据上报配置分页列表")
    public Result<PageData<DataReportConfigVO>> page(@RequestParam Map<String, Object> params) {
        return new Result<PageData<DataReportConfigVO>>().ok(dataReportConfigService.page(params));
    }

    @GetMapping("/{id}")
    @Operation(summary = "数据上报配置详情")
    public Result<DataReportConfigVO> detail(@PathVariable String id) {
        return new Result<DataReportConfigVO>().ok(dataReportConfigService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增数据上报配置")
    public Result<DataReportConfigVO> create(@Valid @RequestBody DataReportConfigDTO dto) {
        return new Result<DataReportConfigVO>().ok(dataReportConfigService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改数据上报配置")
    public Result<Void> update(@PathVariable String id, @Valid @RequestBody DataReportConfigDTO dto) {
        dataReportConfigService.update(id, dto);
        return new Result<>();
    }

    @PostMapping("/{id}/default")
    @Operation(summary = "设为默认/生效配置")
    public Result<Void> setDefault(@PathVariable String id) {
        dataReportConfigService.setDefault(id);
        return new Result<>();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据上报配置")
    public Result<Void> delete(@PathVariable String id) {
        dataReportConfigService.delete(id);
        return new Result<>();
    }
}
