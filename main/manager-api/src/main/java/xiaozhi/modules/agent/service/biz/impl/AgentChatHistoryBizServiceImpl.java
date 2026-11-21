package xiaozhi.modules.agent.service.biz.impl;

import java.util.Base64;
import java.util.Date;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.utils.JsonUtils;
import xiaozhi.modules.agent.dto.AgentChatHistoryReportDTO;
import xiaozhi.modules.agent.entity.AgentChatHistoryEntity;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.agent.service.AgentChatAudioService;
import xiaozhi.modules.agent.service.AgentChatHistoryService;
import xiaozhi.modules.agent.service.AgentChatSummaryService;
import xiaozhi.modules.agent.service.AgentService;
import xiaozhi.modules.agent.service.biz.AgentChatHistoryBizService;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.device.service.DeviceService;

/**
 * {@link AgentChatHistoryBizService} impl
 *
 * @author Goody
 * @version 1.0, 2025/4/30
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentChatHistoryBizServiceImpl implements AgentChatHistoryBizService {
    private final AgentService agentService;
    private final AgentChatHistoryService agentChatHistoryService;
    private final AgentChatAudioService agentChatAudioService;
    private final AgentChatSummaryService agentChatSummaryService;
    private final RedisUtils redisUtils;
    private final DeviceService deviceService;

    /**
     * 处理会话记录上报，包括文件上传和相关信息记录
     *
     * @param report 包含聊天上报所需信息的输入对象
     * @return 上传结果，true表示成功，false表示失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean report(AgentChatHistoryReportDTO report) {
        String macAddress = report.getMacAddress();
        Byte chatType = report.getChatType();
        Long reportTimeMillis = null != report.getReportTime() ? report.getReportTime()
                : System.currentTimeMillis();
        log.info("小智设备聊天上报请求: macAddress={}, type={} reportTime={}, roundId={}, hasMetrics={}",
                macAddress, chatType, reportTimeMillis, report.getRoundId(),
                report.getMetrics() != null && !report.getMetrics().isEmpty());
        // 本轮耗时快照的关键阶段摘要，便于在服务端日志中直接定位哪个环节慢
        logMetricsSummary(macAddress, report);

        // 根据设备MAC地址查询对应的默认智能体，判断是否需要上报
        AgentEntity agentEntity = agentService.getDefaultAgentByMacAddress(macAddress);
        if (agentEntity == null) {
            return Boolean.FALSE;
        }

        Integer chatHistoryConf = agentEntity.getChatHistoryConf();
        String agentId = agentEntity.getId();

        if (Objects.equals(chatHistoryConf, Constant.ChatHistoryConfEnum.RECORD_TEXT.getCode())) {
            saveChatText(report, agentId, macAddress, null, reportTimeMillis);
        } else if (Objects.equals(chatHistoryConf, Constant.ChatHistoryConfEnum.RECORD_TEXT_AUDIO.getCode())) {
            String audioId = saveChatAudio(report);
            saveChatText(report, agentId, macAddress, audioId, reportTimeMillis);
        }

        // 更新设备最后对话时间
        redisUtils.set(RedisKeys.getAgentDeviceLastConnectedAtById(agentId), new Date());

        // 更新设备最后连接时间
        DeviceEntity device = deviceService.getDeviceByMacAddress(macAddress);
        if (device != null) {
            deviceService.updateDeviceConnectionInfo(agentId, device.getId(), null);
        } else {
            log.warn("会话记录上报时，未找到mac地址为 {} 的设备", macAddress);
        }

        return Boolean.TRUE;
    }

    /**
     * metrics_json 是 TEXT 列（最大 64KB），设一个远小于上限的保护值，
     * 避免设备侧或中间层异常上报超大 JSON 导致写库失败、连带丢弃整条聊天记录。
     */
    private static final int MAX_METRICS_JSON_LENGTH = 8 * 1024;

    /**
     * 按阶段输出本轮耗时摘要日志（仅带 roundId 的末条回复上报会有 metrics）。
     */
    private void logMetricsSummary(String macAddress, AgentChatHistoryReportDTO report) {
        java.util.Map<String, Object> m = report.getMetrics();
        if (m == null || m.isEmpty()) {
            return;
        }
        log.info("本轮耗时摘要: macAddress={}, roundId={}, vadWait={}ms, asr={}ms, intent={}ms, memory={}ms, "
                + "llmTtft={}ms, llmTotal={}ms, llmRounds={}, tool={}ms, ttsFirst={}ms(provider={}, stream={}, retry={}), "
                + "ttsTextWait={}ms, e2eFirstAudio={}ms, firstPacketSent={}ms, e2eFromSpeechEnd={}ms, e2eTotal={}ms",
                macAddress, report.getRoundId(),
                m.get("vadWaitMs"), m.get("asrMs"), m.get("intentMs"), m.get("memoryQueryMs"),
                m.get("llmTtftMs"), m.get("llmTotalMs"), m.get("llmRounds"), m.get("toolTotalMs"),
                m.get("ttsFirstMs"), m.get("ttsProvider"), m.get("ttsIsStream"), m.get("ttsRetryCount"),
                m.get("ttsTextWaitMs"), m.get("e2eFirstAudioMs"), m.get("firstPacketSentMs"),
                m.get("e2eFromSpeechEndMs"), m.get("e2eTotalMs"));
    }

    /**
     * base64解码report.getOpusDataBase64(),存入ai_agent_chat_audio表
     */
    private String saveChatAudio(AgentChatHistoryReportDTO report) {
        String audioId = null;

        if (report.getAudioBase64() != null && !report.getAudioBase64().isEmpty()) {
            try {
                byte[] audioData = Base64.getDecoder().decode(report.getAudioBase64());
                audioId = agentChatAudioService.saveAudio(audioData);
                log.info("音频数据保存成功，audioId={}", audioId);
            } catch (Exception e) {
                log.error("音频数据保存失败", e);
                return null;
            }
        }
        return audioId;
    }

    /**
     * 组装上报数据
     */
    private void saveChatText(AgentChatHistoryReportDTO report, String agentId, String macAddress, String audioId,
            Long reportTime) {
        // 耗时快照只在带 roundId 的末条回复上报中存在；二者必须成对，
        // 否则前端无法把面板归到正确的一轮，故缺 roundId 时整组丢弃。
        String roundId = report.getRoundId();
        String metricsJson = (roundId == null || report.getMetrics() == null || report.getMetrics().isEmpty())
                ? null : JsonUtils.toJsonString(report.getMetrics());
        if (metricsJson != null && metricsJson.length() > MAX_METRICS_JSON_LENGTH) {
            // 超大时丢弃明细类字段（数组/列表），保留所有数值型耗时指标，保证写库不失败
            java.util.Map<String, Object> slim = new java.util.LinkedHashMap<>();
            report.getMetrics().forEach((k, v) -> {
                if (!(v instanceof java.util.Collection) && !(v instanceof java.util.Map)) {
                    slim.put(k, v);
                }
            });
            log.warn("metrics_json 超过 {} 字符({})，已精简明细字段: roundId={}",
                    MAX_METRICS_JSON_LENGTH, metricsJson.length(), roundId);
            metricsJson = JsonUtils.toJsonString(slim);
        }

        // 构建会话记录实体
        AgentChatHistoryEntity entity = AgentChatHistoryEntity.builder()
                .macAddress(macAddress)
                .agentId(agentId)
                .sessionId(report.getSessionId())
                .chatType(report.getChatType())
                .content(report.getContent())
                .audioId(audioId)
                .roundId(roundId)
                .metricsJson(metricsJson)
                .createdAt(new Date(reportTime))
                // NOTE(haotian): 2025/5/26 updateAt可以不设置，重点是createAt，而且这样可以看到上报延迟
                .build();

        // 保存数据
        agentChatHistoryService.save(entity);

        log.info("设备 {} 对应智能体 {} 上报成功", macAddress, agentId);
    }
}
