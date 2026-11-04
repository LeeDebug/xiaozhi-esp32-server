package xiaozhi.modules.dataanalysis.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.page.PageData;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.modules.dataanalysis.dao.DataReportRecordDao;
import xiaozhi.modules.dataanalysis.dto.DataPredictionDTO;
import xiaozhi.modules.dataanalysis.dto.DataReportUploadDTO;
import xiaozhi.modules.dataanalysis.entity.DataReportRecordEntity;
import xiaozhi.modules.dataanalysis.service.DataReportRecordService;
import xiaozhi.modules.dataanalysis.vo.DataAnalysisStatisticsVO;
import xiaozhi.modules.dataanalysis.vo.DataPredictionPointVO;
import xiaozhi.modules.dataanalysis.vo.DataReportRecordVO;
import xiaozhi.modules.dataanalysis.vo.MetricDistributionVO;
import xiaozhi.modules.dataanalysis.vo.MetricMetaVO;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.ModelConfigService;

@Slf4j
@Service
public class DataReportRecordServiceImpl extends BaseServiceImpl<DataReportRecordDao, DataReportRecordEntity>
        implements DataReportRecordService {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Map<String, String> METRIC_NAMES = Map.of(
            "temperature", "温度", "humidity", "湿度", "volume", "音量",
            "onlineDuration", "在线时长", "tempProbe", "温度探头",
            "envTemperature", "环境温度", "envHumidity", "环境湿度",
            "chickenCount", "鸡只数量", "silo1Weight", "料塔1重量",
            "silo2Weight", "料塔2重量", "powerStatistics", "电能统计");

    @Autowired
    private DataReportRecordDao dataReportRecordDao;

    @Autowired
    private ModelConfigService modelConfigService;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upload(DataReportUploadDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getDeviceId()) || dto.getData() == null
                || dto.getData().isEmpty()) {
            return;
        }
        Date reportTime = dto.getTimestamp() != null ? new Date(dto.getTimestamp()) : new Date();
        Date now = new Date();
        List<DataReportRecordEntity> entities = new ArrayList<>();
        for (Map.Entry<String, Object> entry : dto.getData().entrySet()) {
            String key = entry.getKey();
            BigDecimal num = toBigDecimal(entry.getValue());
            if (num == null) {
                continue;
            }
            DataReportRecordEntity e = new DataReportRecordEntity();
            e.setDeviceId(dto.getDeviceId());
            e.setMetricKey(key);
            e.setMetricName(METRIC_NAMES.getOrDefault(key, key));
            e.setValue(num);
            e.setStatus(1);
            e.setReportTime(reportTime);
            e.setCreateTime(now);
            entities.add(e);
        }
        if (!entities.isEmpty()) {
            insertBatch(entities);
        }
    }

    @Override
    public PageData<DataReportRecordVO> preview(Map<String, Object> params) {
        IPage<DataReportRecordEntity> page = getPage(params, "report_time", false);
        LambdaQueryWrapper<DataReportRecordEntity> wrapper = new LambdaQueryWrapper<>();
        String keyword = (String) params.get("keyword");
        if (StringUtils.isNotBlank(keyword)) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w.like(DataReportRecordEntity::getDeviceId, like)
                    .or().like(DataReportRecordEntity::getMetricName, like)
                    .or().like(DataReportRecordEntity::getMetricKey, like));
        }
        String metric = (String) params.get("metric");
        if (StringUtils.isNotBlank(metric)) {
            wrapper.eq(DataReportRecordEntity::getMetricKey, metric);
        }
        dataReportRecordDao.selectPage(page, wrapper);
        List<DataReportRecordVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(java.util.stream.Collectors.toList());
        return new PageData<>(voList, page.getTotal());
    }

    @Override
    public DataAnalysisStatisticsVO statistics() {
        DataAnalysisStatisticsVO vo = new DataAnalysisStatisticsVO();
        Map<String, Object> stat = dataReportRecordDao.selectStatistics();
        vo.setRecordCount(toLong(stat.get("total")));
        vo.setDeviceCount(toLong(stat.get("deviceCount")));
        vo.setAvgValue(toBigDecimal(stat.get("avgValue")));
        vo.setAbnormal(toLong(stat.get("abnormal")));

        List<Map<String, Object>> dist = dataReportRecordDao.selectMetricDistribution();
        List<MetricDistributionVO> distVO = new ArrayList<>();
        for (Map<String, Object> m : dist) {
            MetricDistributionVO d = new MetricDistributionVO();
            d.setMetric((String) m.get("metricName"));
            d.setCount(toLong(m.get("cnt")));
            d.setAvg(toBigDecimal(m.get("avgVal")));
            d.setMax(toBigDecimal(m.get("maxVal")));
            d.setMin(toBigDecimal(m.get("minVal")));
            distVO.add(d);
        }
        vo.setDistribution(distVO);
        vo.setMetrics(metrics());
        return vo;
    }

    @Override
    public List<MetricMetaVO> metrics() {
        List<Map<String, Object>> list = dataReportRecordDao.selectMetricList();
        List<MetricMetaVO> result = new ArrayList<>();
        for (Map<String, Object> m : list) {
            MetricMetaVO meta = new MetricMetaVO();
            meta.setKey((String) m.get("metricKey"));
            meta.setName((String) m.get("metricName"));
            result.add(meta);
        }
        return result;
    }

    @Override
    public List<DataPredictionPointVO> predict(DataPredictionDTO dto) {
        int horizon = dto.getHorizon() != null ? dto.getHorizon() : 12;
        horizon = Math.max(1, Math.min(horizon, 72));
        List<BigDecimal> history = dataReportRecordDao.selectHistoryValues(dto.getMetric(), 168);
        Date latest = dataReportRecordDao.selectLatestReportTime(dto.getMetric());
        if (history == null || history.isEmpty() || latest == null) {
            return Collections.emptyList();
        }
        boolean useLLM = Boolean.TRUE.equals(dto.getUseLLM()) && llmAvailable();
        if (useLLM) {
            List<DataPredictionPointVO> llm = llmForecast(dto.getMetric(), history, latest, horizon);
            if (llm != null && !llm.isEmpty()) {
                return llm;
            }
        }
        return statisticalForecast(history, latest, horizon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int seedDemoData() {
        Long count = baseDao.selectCount(null);
        if (count != null && count > 0) {
            return -1;
        }
        String[] devices = {"livingroom-speaker-001", "bedroom-speaker-002",
                "workshop-gateway-003", "frontdesk-speaker-004"};
        Object[][] metricDefs = {
                {"temperature", "温度", 18.0, 30.0},
                {"humidity", "湿度", 40.0, 75.0},
                {"volume", "音量", 20.0, 65.0},
                {"onlineDuration", "在线时长", 1.0, 24.0}};
        int points = 72;
        long now = System.currentTimeMillis();
        long step = 3600L * 1000L;
        Date createTime = new Date();
        List<DataReportRecordEntity> entities = new ArrayList<>();
        Random rnd = new Random(20261104L);
        for (String dev : devices) {
            for (Object[] md : metricDefs) {
                String key = (String) md[0];
                String name = (String) md[1];
                double lo = (double) md[2];
                double hi = (double) md[3];
                double mid = (lo + hi) / 2.0;
                for (int i = points - 1; i >= 0; i--) {
                    double v = mid + (rnd.nextDouble() - 0.5) * (hi - lo) * 0.8
                            + Math.sin(i / 6.0) * (hi - lo) * 0.15;
                    v = Math.max(lo, Math.min(hi, v));
                    DataReportRecordEntity e = new DataReportRecordEntity();
                    e.setDeviceId(dev);
                    e.setMetricKey(key);
                    e.setMetricName(name);
                    e.setValue(BigDecimal.valueOf(Math.round(v * 10) / 10.0));
                    e.setStatus(1);
                    e.setReportTime(new Date(now - i * step));
                    e.setCreateTime(createTime);
                    entities.add(e);
                }
            }
        }
        insertBatch(entities);
        return entities.size();
    }

    private DataReportRecordVO toVO(DataReportRecordEntity e) {
        if (e == null) {
            return null;
        }
        DataReportRecordVO vo = new DataReportRecordVO();
        vo.setDeviceId(e.getDeviceId());
        vo.setMetricKey(e.getMetricKey());
        vo.setMetric(e.getMetricName());
        vo.setValue(e.getValue());
        vo.setStatus(e.getStatus() != null && e.getStatus() == 1 ? "normal" : "abnormal");
        vo.setTime(e.getReportTime() != null ? SDF.format(e.getReportTime()) : "");
        vo.setReportTime(e.getReportTime());
        return vo;
    }

    /**
     * 统计预测：线性回归 + 预测区间（基于历史残差的标准误）
     */
    private List<DataPredictionPointVO> statisticalForecast(List<BigDecimal> history, Date latest, int horizon) {
        int n = history.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double x = i;
            double yi = history.get(i).doubleValue();
            y[i] = yi;
            sumX += x;
            sumY += yi;
            sumXY += x * yi;
            sumXX += x * x;
        }
        double meanX = (n - 1) / 2.0;
        double sxx = sumXX - sumX * sumX / n;
        double slope = sxx != 0 ? (sumXY - sumX * sumY / n) / sxx : 0;
        double intercept = sumY / n - slope * meanX;
        double sse = 0;
        for (int i = 0; i < n; i++) {
            double diff = y[i] - (intercept + slope * i);
            sse += diff * diff;
        }
        double sigma = n > 2 ? Math.sqrt(sse / (n - 2))
                : (Math.abs(history.get(n - 1).doubleValue()) * 0.1 + 1);
        List<DataPredictionPointVO> result = new ArrayList<>();
        long step = 3600L * 1000L;
        for (int h = 1; h <= horizon; h++) {
            double xf = (n - 1) + h;
            double yhat = intercept + slope * xf;
            double se = sigma * Math.sqrt(1 + 1.0 / n
                    + (sxx > 0 ? (xf - meanX) * (xf - meanX) / sxx : 1));
            double band = 1.96 * se;
            DataPredictionPointVO p = new DataPredictionPointVO();
            p.setTime(SDF.format(new Date(latest.getTime() + h * step)));
            p.setPredicted(round(yhat));
            p.setLower(round(Math.max(0, yhat - band)));
            p.setUpper(round(yhat + band));
            result.add(p);
        }
        return result;
    }

    /**
     * 大模型预测：调用系统默认 LLM，失败时返回 null 交由统计预测兜底
     */
    private List<DataPredictionPointVO> llmForecast(String metric, List<BigDecimal> history, Date latest,
            int horizon) {
        try {
            ModelConfigEntity cfg = defaultLLMConfig();
            if (cfg == null || cfg.getConfigJson() == null) {
                return null;
            }
            JSONObject c = cfg.getConfigJson();
            String baseUrl = c.getStr("base_url");
            String apiKey = c.getStr("api_key");
            String model = c.getStr("model_name");
            if (StringUtils.isBlank(baseUrl) || StringUtils.isBlank(apiKey)) {
                return null;
            }
            int start = Math.max(0, history.size() - 24);
            StringBuilder series = new StringBuilder();
            for (int i = start; i < history.size(); i++) {
                series.append(history.get(i).toPlainString()).append(",");
            }
            String prompt = String.format(
                    "你是一个时序预测助手。以下是指标「%s」最近的历史观测值（按时间先后，每小时一个点）：\n%s\n"
                            + "请基于趋势预测未来%d小时的数值。只返回一个JSON对象，不要任何解释，格式为："
                            + "{\"predicted\":[...],\"lower\":[...],\"upper\":[...]}，"
                            + "三个数组长度均为%d，分别为每个未来小时的预测值、下限、上限（下限<=预测值<=上限）。",
                    metric, series, horizon, horizon);

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("model", model != null ? model : "gpt-3.5-turbo");
            Map<String, String> msg = new java.util.HashMap<>();
            msg.put("role", "user");
            msg.put("content", prompt);
            body.put("messages", List.of(msg));
            body.put("temperature", 0.2);
            body.put("max_tokens", 1500);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String apiUrl = baseUrl;
            if (!apiUrl.endsWith("/chat/completions")) {
                if (!apiUrl.endsWith("/")) {
                    apiUrl += "/";
                }
                apiUrl += "chat/completions";
            }
            ResponseEntity<String> resp = restTemplate.postForEntity(apiUrl, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return null;
            }
            JSONObject rj = JSONUtil.parseObj(resp.getBody());
            JSONArray choices = rj.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            String content = choices.getJSONObject(0).getJSONObject("message").getStr("content");
            JSONObject fj = extractJsonObject(content);
            if (fj == null) {
                return null;
            }
            JSONArray pred = fj.getJSONArray("predicted");
            JSONArray low = fj.getJSONArray("lower");
            JSONArray up = fj.getJSONArray("upper");
            if (pred == null) {
                return null;
            }
            List<DataPredictionPointVO> result = new ArrayList<>();
            long step = 3600L * 1000L;
            int len = Math.min(pred.size(), horizon);
            for (int h = 1; h <= len; h++) {
                DataPredictionPointVO p = new DataPredictionPointVO();
                p.setTime(SDF.format(new Date(latest.getTime() + h * step)));
                p.setPredicted(toBD(pred.get(h - 1)));
                p.setLower(low != null ? toBD(low.get(h - 1)) : null);
                p.setUpper(up != null ? toBD(up.get(h - 1)) : null);
                result.add(p);
            }
            return result;
        } catch (Exception e) {
            log.warn("LLM预测调用失败，将回退统计预测: {}", e.getMessage());
            return null;
        }
    }

    private boolean llmAvailable() {
        try {
            ModelConfigEntity cfg = defaultLLMConfig();
            if (cfg == null || cfg.getConfigJson() == null) {
                return false;
            }
            JSONObject c = cfg.getConfigJson();
            return StringUtils.isNotBlank(c.getStr("base_url"))
                    && StringUtils.isNotBlank(c.getStr("api_key"));
        } catch (Exception e) {
            return false;
        }
    }

    private ModelConfigEntity defaultLLMConfig() {
        try {
            List<ModelConfigEntity> list = modelConfigService.getEnabledModelsByType("LLM");
            if (list == null || list.isEmpty()) {
                return null;
            }
            for (ModelConfigEntity c : list) {
                if (c.getIsDefault() != null && c.getIsDefault() == 1) {
                    return c;
                }
            }
            return list.get(0);
        } catch (Exception e) {
            log.warn("获取默认LLM配置失败: {}", e.getMessage());
            return null;
        }
    }

    private JSONObject extractJsonObject(String text) {
        if (text == null) {
            return null;
        }
        int s = text.indexOf('{');
        int e = text.lastIndexOf('}');
        if (s < 0 || e < 0 || e <= s) {
            return null;
        }
        try {
            return JSONUtil.parseObj(text.substring(s, e + 1));
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal round(double v) {
        return BigDecimal.valueOf(Math.round(v * 100) / 100.0).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toBD(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof BigDecimal) {
            return (BigDecimal) o;
        }
        if (o instanceof Number) {
            return BigDecimal.valueOf(((Number) o).doubleValue());
        }
        try {
            return new BigDecimal(o.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object o) {
        return toBD(o);
    }

    private Long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (Exception ex) {
            return 0L;
        }
    }
}
