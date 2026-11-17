package xiaozhi.modules.dataanalysis.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import xiaozhi.modules.dataanalysis.dao.DataReportMetricDao;
import xiaozhi.modules.dataanalysis.dao.DataReportRecordDao;
import xiaozhi.modules.dataanalysis.dto.DataPredictionDTO;
import xiaozhi.modules.dataanalysis.dto.DataReportUploadDTO;
import xiaozhi.modules.dataanalysis.entity.DataReportMetricEntity;
import xiaozhi.modules.dataanalysis.entity.DataReportRecordEntity;
import xiaozhi.modules.dataanalysis.service.DataReportRecordService;
import xiaozhi.modules.dataanalysis.vo.DataAnalysisStatisticsVO;
import xiaozhi.modules.dataanalysis.vo.DataPreviewDashboardVO;
import xiaozhi.modules.dataanalysis.vo.DataPredictionPointVO;
import xiaozhi.modules.dataanalysis.vo.DataReportRecordVO;
import xiaozhi.modules.dataanalysis.vo.MetricDistributionVO;
import xiaozhi.modules.dataanalysis.vo.MetricMetaVO;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.sys.service.UserDataScopeService;

@Slf4j
@Service
public class DataReportRecordServiceImpl extends BaseServiceImpl<DataReportRecordDao, DataReportRecordEntity>
        implements DataReportRecordService {

    /** 统一按中国时间格式化，避免受服务器 JVM 默认时区影响 */
    private static final DateTimeFormatter CHINA_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

    /** 中国时间相对 UTC 的固定偏移 */
    private static final long CHINA_OFFSET_MILLIS = 8L * 60 * 60 * 1000;

    /** 允许的设备时钟偏差；超过该值才认为设备上报的是「本地墙上时间当 UTC 戳」 */
    private static final long CLOCK_SKEW_TOLERANCE_MILLIS = 60L * 60 * 1000;

    /** 字段目录（data_report_metric 表）在进程内缓存，避免每次上报都查库 */
    private volatile Map<String, DataReportMetricEntity> metricCatalogCache;

    @Autowired
    private DataReportRecordDao dataReportRecordDao;

    @Autowired
    private DataReportMetricDao dataReportMetricDao;

    @Autowired
    private ModelConfigService modelConfigService;

    @Autowired
    private UserDataScopeService userDataScopeService;

    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }

    /** 与智能体/设备管理一致：普通用户只看自己的数据，超管需参数开启才看全库。 */
    private boolean viewAllUserData() {
        return userDataScopeService.canViewAllUserData();
    }

    private Long scopedUserId() {
        return userDataScopeService.currentUserId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upload(DataReportUploadDTO dto) {
        if (dto == null || StringUtils.isBlank(dto.getDeviceId()) || dto.getData() == null
                || dto.getData().isEmpty()) {
            return;
        }
        // 固件上报的 deviceId 是 Client-Id(UUID)；用随包携带的 MAC 建立与 ai_device 的归属映射。
        Object mac = dto.getDeviceInfo() == null ? null : dto.getDeviceInfo().get("mac");
        if (mac != null && StringUtils.isNotBlank(mac.toString())
                && !mac.toString().equalsIgnoreCase(dto.getDeviceId())) {
            dataReportRecordDao.bindClientId(mac.toString().trim(), dto.getDeviceId());
        }
        Date now = new Date();
        Date reportTime = now;
        if (dto.getTimestamp() != null) {
            long ts = dto.getTimestamp();
            // 终端拿到 OTA 下发的 server_time.timestamp 后会加上 timezone_offset 再当时戳上报，
            // 导致比真实时刻快 8 小时。只有明显晚于服务器当前时间才回拨，避免误伤正常时钟或历史补传。
            if (ts > now.getTime() + CLOCK_SKEW_TOLERANCE_MILLIS) {
                ts -= CHINA_OFFSET_MILLIS;
            }
            reportTime = new Date(ts);
        }
        List<DataReportRecordEntity> entities = new ArrayList<>();
        for (Map.Entry<String, Object> entry : dto.getData().entrySet()) {
            String key = entry.getKey();
            BigDecimal num = toBigDecimal(entry.getValue());
            if (num == null) {
                continue;
            }
            DataReportMetricEntity meta = metricCatalog().get(key);
            DataReportRecordEntity e = new DataReportRecordEntity();
            e.setDeviceId(dto.getDeviceId());
            e.setMetricKey(key);
            e.setMetricName(meta != null ? meta.getMetricName() : key);
            e.setValueRange(meta != null ? meta.getValueRange() : null);
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
        String deviceId = (String) params.get("deviceId");
        if (StringUtils.isNotBlank(deviceId)) {
            wrapper.eq(DataReportRecordEntity::getDeviceId, deviceId);
        }
        if (!viewAllUserData()) {
            wrapper.apply("EXISTS (SELECT 1 FROM ai_device d WHERE d.user_id = {0} "
                    + "AND (d.client_id = device_id OR d.id = device_id OR d.mac_address = device_id))",
                    scopedUserId());
        }
        dataReportRecordDao.selectPage(page, wrapper);
        List<DataReportRecordVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(java.util.stream.Collectors.toList());
        return new PageData<>(voList, page.getTotal());
    }

    @Override
    public DataAnalysisStatisticsVO statistics() {
        boolean viewAll = viewAllUserData();
        Long userId = scopedUserId();
        DataAnalysisStatisticsVO vo = new DataAnalysisStatisticsVO();
        Map<String, Object> stat = dataReportRecordDao.selectStatistics(userId, viewAll);
        vo.setRecordCount(toLong(stat.get("total")));
        vo.setDeviceCount(toLong(stat.get("deviceCount")));
        vo.setAvgValue(toBigDecimal(stat.get("avgValue")));
        vo.setAbnormal(toLong(stat.get("abnormal")));

        List<Map<String, Object>> dist = dataReportRecordDao.selectMetricDistribution(userId, viewAll);
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
    public DataPreviewDashboardVO dashboard() {
        DataPreviewDashboardVO vo = new DataPreviewDashboardVO();
        // DB 统一以 UTC 存储 report_time，看板固定按中国时间 (+8) 归集，与列表展示口径一致
        int offsetHours = (int) (CHINA_OFFSET_MILLIS / (60 * 60 * 1000));
        boolean viewAll = viewAllUserData();
        Long userId = scopedUserId();

        Map<String, Object> stat = dataReportRecordDao.selectDashboardSummary(userId, viewAll);
        DataPreviewDashboardVO.Summary summary = new DataPreviewDashboardVO.Summary();
        summary.setRecordCount(toLong(stat.get("total")));
        summary.setDeviceCount(toLong(stat.get("deviceCount")));
        summary.setAbnormalCount(toLong(stat.get("abnormal")));
        // MyBatis 对 @Select 返回的 Map 会按 JDBC 类型推断：DATETIME/TIMESTAMP 常被映射为
        // java.time.LocalDateTime，而实体字段是 java.util.Date。不能强转，统一走 formatTime 兼容处理。
        summary.setFirstReportTime(formatTime(stat.get("firstTime")));
        summary.setLastReportTime(formatTime(stat.get("lastTime")));
        // 看板为 0 时，用可见设备数区分「没有数据」与「Client-Id 未映射」两种情况
        summary.setVisibleDeviceCount((long) dataReportRecordDao.selectScopeDeviceIds(userId, viewAll).size());
        // 数值为 0 / 非 0 的分布，用于数据质量占比分析
        Map<String, Object> zeroDist = dataReportRecordDao.selectValueZeroDistribution(userId, viewAll);
        summary.setZeroValueCount(toLong(zeroDist.get("zeroCount")));
        summary.setNonZeroValueCount(toLong(zeroDist.get("nonZeroCount")));
        vo.setSummary(summary);

        List<DataPreviewDashboardVO.DailyUpload> daily = new ArrayList<>();
        for (Map<String, Object> m : dataReportRecordDao.selectDailyUploads(offsetHours, userId, viewAll)) {
            DataPreviewDashboardVO.DailyUpload d = new DataPreviewDashboardVO.DailyUpload();
            Object day = m.get("statDate");
            d.setDate(day != null ? day.toString() : "");
            d.setCount(toLong(m.get("cnt")));
            d.setDeviceCount(toLong(m.get("deviceCount")));
            daily.add(d);
        }
        summary.setActiveDays((long) daily.size());
        vo.setDailyUploads(daily);

        List<DataPreviewDashboardVO.DeviceUpload> devices = new ArrayList<>();
        for (Map<String, Object> m : dataReportRecordDao.selectDeviceUploads(offsetHours, userId, viewAll)) {
            DataPreviewDashboardVO.DeviceUpload d = new DataPreviewDashboardVO.DeviceUpload();
            Object id = m.get("deviceId");
            d.setDeviceId(id != null ? id.toString() : "");
            Object name = m.get("deviceName");
            d.setDeviceName(name != null && !name.toString().isEmpty() ? name.toString() : d.getDeviceId());
            Object mac = m.get("macAddress");
            d.setMacAddress(mac != null ? mac.toString() : "");
            d.setCount(toLong(m.get("cnt")));
            Object last = m.get("lastTime");
            d.setLastReportTime(last != null ? last.toString() : "");
            devices.add(d);
        }
        vo.setDeviceUploads(devices);

        List<MetricDistributionVO> distVO = new ArrayList<>();
        for (Map<String, Object> m : dataReportRecordDao.selectMetricDistribution(userId, viewAll)) {
            MetricDistributionVO d = new MetricDistributionVO();
            d.setMetric((String) m.get("metricName"));
            d.setCount(toLong(m.get("cnt")));
            d.setAvg(toBigDecimal(m.get("avgVal")));
            d.setMax(toBigDecimal(m.get("maxVal")));
            d.setMin(toBigDecimal(m.get("minVal")));
            distVO.add(d);
        }
        vo.setMetricDistribution(distVO);

        List<DataPreviewDashboardVO.HourlyUpload> hourly = new ArrayList<>();
        for (Map<String, Object> m : dataReportRecordDao.selectHourlyDistribution(offsetHours, userId, viewAll)) {
            DataPreviewDashboardVO.HourlyUpload h = new DataPreviewDashboardVO.HourlyUpload();
            h.setDayOfWeek(toLong(m.get("dow")).intValue());
            h.setHour(toLong(m.get("hr")).intValue());
            h.setCount(toLong(m.get("cnt")));
            hourly.add(h);
        }
        vo.setHourlyDistribution(hourly);
        return vo;
    }

    /**
     * 统一按中国时间格式化。入参可能是 java.util.Date（实体字段 / 显式 DAO 返回），
     * 也可能是 java.time.LocalDateTime（@Select 返回 Map 时 MyBatis 按 JDBC TIMESTAMP 推断），
     * 必须兼容两者，否则看板聚合接口会抛 ClassCastException。
     */
    private String formatTime(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Date) {
            return CHINA_TIME_FORMATTER.format(Instant.ofEpochMilli(((Date) value).getTime()));
        }
        if (value instanceof LocalDateTime) {
            // 项目口径：DB 的 datetime 列存的是 UTC 墙上值。LocalDateTime 不含时区，
            // 按 UTC 解释后再格式化到中国时间，结果与 java.util.Date 路径一致。
            return CHINA_TIME_FORMATTER.format(((LocalDateTime) value).toInstant(ZoneOffset.UTC));
        }
        if (value instanceof Instant) {
            return CHINA_TIME_FORMATTER.format((Instant) value);
        }
        if (value instanceof LocalDate) {
            return CHINA_TIME_FORMATTER.format(((LocalDate) value).atStartOfDay(ZoneOffset.UTC).toInstant());
        }
        return String.valueOf(value);
    }

    @Override
    public List<MetricMetaVO> metrics() {
        // 以字段目录为准（按 Excel 序号顺序返回全部字段，含取值范围），
        // 再补充目录中不存在、但历史记录里实际出现过的 key，保证旧数据仍可筛选。
        Map<String, MetricMetaVO> result = new LinkedHashMap<>();
        List<DataReportMetricEntity> catalog = dataReportMetricDao.selectList(
                new LambdaQueryWrapper<DataReportMetricEntity>()
                        .orderByAsc(DataReportMetricEntity::getSeq)
                        // bit_index 为 NULL 的整体状态字排在位字段之前，位字段按 0~9 排列
                        .orderByAsc(DataReportMetricEntity::getBitIndex));
        for (DataReportMetricEntity e : catalog) {
            MetricMetaVO meta = new MetricMetaVO();
            meta.setKey(e.getMetricKey());
            meta.setName(e.getMetricName());
            meta.setSeq(e.getSeq());
            meta.setBit(e.getBitIndex());
            meta.setGroup(e.getGroupName());
            meta.setRw(e.getRw());
            meta.setDefaultValue(e.getDefaultValue());
            meta.setHexAddress(e.getHexAddress());
            meta.setValueRange(e.getValueRange());
            result.put(e.getMetricKey(), meta);
        }
        for (Map<String, Object> m : dataReportRecordDao.selectMetricList(scopedUserId(), viewAllUserData())) {
            String key = (String) m.get("metricKey");
            if (StringUtils.isBlank(key) || result.containsKey(key)) {
                continue;
            }
            MetricMetaVO meta = new MetricMetaVO();
            meta.setKey(key);
            Object name = m.get("metricName");
            meta.setName(name != null ? name.toString() : key);
            result.put(key, meta);
        }
        return new ArrayList<>(result.values());
    }

    @Override
    public List<Map<String, Object>> devices() {
        return dataReportRecordDao.selectDeviceList(scopedUserId(), viewAllUserData());
    }

    /** 加载并缓存字段目录；目录表未就绪时返回空 Map，保证上报接口不受影响 */
    private Map<String, DataReportMetricEntity> metricCatalog() {
        if (metricCatalogCache == null) {
            synchronized (this) {
                if (metricCatalogCache == null) {
                    Map<String, DataReportMetricEntity> map = new HashMap<>();
                    try {
                        for (DataReportMetricEntity e : dataReportMetricDao.selectList(null)) {
                            map.put(e.getMetricKey(), e);
                        }
                    } catch (Exception ex) {
                        log.warn("加载设备上报字段目录失败，指标名称将回退为原始key: {}", ex.getMessage());
                    }
                    metricCatalogCache = map;
                }
            }
        }
        return metricCatalogCache;
    }

    @Override
    public List<DataPredictionPointVO> predict(DataPredictionDTO dto) {
        int horizon = dto.getHorizon() != null ? dto.getHorizon() : 12;
        horizon = Math.max(1, Math.min(horizon, 72));
        boolean viewAll = viewAllUserData();
        Long userId = scopedUserId();
        List<BigDecimal> history = dataReportRecordDao.selectHistoryValues(dto.getMetric(), 168, userId, viewAll);
        Date latest = dataReportRecordDao.selectLatestReportTime(dto.getMetric(), userId, viewAll);
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
        boolean viewAll = viewAllUserData();
        Long userId = scopedUserId();
        Long count = toLong(dataReportRecordDao.selectStatistics(userId, viewAll).get("total"));
        if (count > 0) {
            return -1;
        }
        // 演示数据只能落在当前可见的设备上，否则生成后用户仍然看不到
        List<String> visibleDevices = dataReportRecordDao.selectScopeDeviceIds(userId, viewAll);
        if (visibleDevices.isEmpty()) {
            return -2;
        }
        String[] devices = visibleDevices.toArray(new String[0]);
        // 演示数据使用字段目录中的真实 mb-mcp_ 指标
        Object[][] metricDefs = {
                {"mb-mcp_waterTemperature", "水温", 10.0, 30.0},
                {"mb-mcp_chickenCount", "鸡只数量", 10000.0, 30000.0},
                {"mb-mcp_silo1CurrentWeight", "1#当前重量", 500.0, 5000.0},
                {"mb-mcp_currentBrightness1", "当前亮度1", 0.0, 100.0}};
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
                    DataReportMetricEntity meta = metricCatalog().get(key);
                    e.setValueRange(meta != null ? meta.getValueRange() : null);
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
        vo.setValueRange(e.getValueRange());
        vo.setValue(e.getValue());
        vo.setStatus(e.getStatus() != null && e.getStatus() == 1 ? "normal" : "abnormal");
        vo.setTime(formatTime(e.getReportTime()));
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
            p.setTime(formatTime(new Date(latest.getTime() + h * step)));
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
                p.setTime(formatTime(new Date(latest.getTime() + h * step)));
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
