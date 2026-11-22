package xiaozhi.modules.agent.controller;

import java.util.List;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import xiaozhi.common.utils.Result;
import xiaozhi.modules.agent.dto.ChatDirectionRequestDTO;
import xiaozhi.modules.agent.service.ChatDirectionService;
import xiaozhi.modules.agent.vo.ChatDirectionAnalysisVO;
import xiaozhi.modules.agent.vo.ChatDirectionVO;
import xiaozhi.modules.security.user.SecurityUser;

@Tag(name = "会话内容高频方向")
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent/chat-direction")
public class ChatDirectionController {

    private final ChatDirectionService chatDirectionService;

    /**
     * 按地区 / 智能体 / 设备统计用户提问的高频词。时间参数为 epoch 毫秒。
     */
    @Operation(summary = "会话内容高频方向统计")
    @RequiresPermissions("sys:role:normal")
    @GetMapping("/stat")
    public Result<ChatDirectionVO> stat(@ModelAttribute ChatDirectionRequestDTO request,
            @RequestParam(value = "topN", defaultValue = "20") int topN) {
        return new Result<ChatDirectionVO>()
                .ok(chatDirectionService.getChatDirection(request, SecurityUser.getUserId(), topN));
    }

    /**
     * 通过 AI 大模型总结当前筛选结果的高频提问方向，并保留分析记录。
     */
    @Operation(summary = "通过AI大模型总结高频方向")
    @RequiresPermissions("sys:role:normal")
    @PostMapping("/analyze")
    public Result<ChatDirectionAnalysisVO> analyze(@RequestBody ChatDirectionRequestDTO request) {
        return new Result<ChatDirectionAnalysisVO>()
                .ok(chatDirectionService.analyzeByLlm(request, SecurityUser.getUserId(), request.getTopN()));
    }

    /**
     * 地区筛选下拉：当前可见设备所在的省份。
     */
    @Operation(summary = "地区筛选选项")
    @RequiresPermissions("sys:role:normal")
    @GetMapping("/provinces")
    public Result<List<String>> provinces() {
        return new Result<List<String>>().ok(chatDirectionService.listProvinces(SecurityUser.getUserId()));
    }

    /**
     * 最近一次大模型分析记录，页面打开时默认展示。
     */
    @Operation(summary = "最近一次大模型分析记录")
    @RequiresPermissions("sys:role:normal")
    @GetMapping("/latest")
    public Result<ChatDirectionAnalysisVO> latest() {
        return new Result<ChatDirectionAnalysisVO>()
                .ok(chatDirectionService.getLatestAnalysis(SecurityUser.getUserId()));
    }

    /**
     * 大模型分析历史记录，最新在前。
     */
    @Operation(summary = "大模型分析历史记录")
    @RequiresPermissions("sys:role:normal")
    @GetMapping("/history")
    public Result<List<ChatDirectionAnalysisVO>> history(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return new Result<List<ChatDirectionAnalysisVO>>()
                .ok(chatDirectionService.listAnalyses(SecurityUser.getUserId(), limit));
    }
}
