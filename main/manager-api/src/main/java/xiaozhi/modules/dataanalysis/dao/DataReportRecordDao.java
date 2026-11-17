package xiaozhi.modules.dataanalysis.dao;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import xiaozhi.common.dao.BaseDao;
import xiaozhi.modules.dataanalysis.entity.DataReportRecordEntity;

@Mapper
public interface DataReportRecordDao extends BaseDao<DataReportRecordEntity> {

    /**
     * 数据可见范围：超管参数开启时看全库；否则只统计归属当前用户的设备。
     * 固件上报的 device_id 是 Client-Id(UUID)，保存在 ai_device.client_id；
     * 同时兼容早期直接上报 MAC / ai_device.id 的设备，以及已回填 mac_address 的记录。
     */
    String SCOPE_CONDITION = "(#{viewAll} = 1 OR EXISTS ("
            + "SELECT 1 FROM ai_device d WHERE d.user_id = #{userId} "
            + "AND (d.client_id = r.device_id OR d.id = r.device_id OR d.mac_address = r.device_id "
            + "OR d.mac_address = r.mac_address)))";

    @Select("SELECT COUNT(*) AS total, COUNT(DISTINCT r.device_id) AS deviceCount, AVG(r.value) AS avgValue, "
            + "SUM(CASE WHEN r.status = 0 THEN 1 ELSE 0 END) AS abnormal FROM data_report_record r "
            + "WHERE " + SCOPE_CONDITION)
    Map<String, Object> selectStatistics(@Param("userId") Long userId, @Param("viewAll") boolean viewAll);

    @Select("SELECT r.metric_key AS metricKey, r.metric_name AS metricName, COUNT(*) AS cnt, "
            + "AVG(r.value) AS avgVal, MAX(r.value) AS maxVal, MIN(r.value) AS minVal "
            + "FROM data_report_record r WHERE " + SCOPE_CONDITION
            + " GROUP BY r.metric_key, r.metric_name ORDER BY cnt DESC")
    List<Map<String, Object>> selectMetricDistribution(@Param("userId") Long userId,
            @Param("viewAll") boolean viewAll);

    @Select("SELECT DISTINCT r.metric_key AS metricKey, r.metric_name AS metricName "
            + "FROM data_report_record r WHERE " + SCOPE_CONDITION + " ORDER BY r.metric_key")
    List<Map<String, Object>> selectMetricList(@Param("userId") Long userId, @Param("viewAll") boolean viewAll);

    @Select("SELECT r.device_id AS deviceId, COUNT(*) AS cnt FROM data_report_record r "
            + "WHERE r.device_id IS NOT NULL AND r.device_id <> '' AND " + SCOPE_CONDITION
            + " GROUP BY r.device_id ORDER BY r.device_id")
    List<Map<String, Object>> selectDeviceList(@Param("userId") Long userId, @Param("viewAll") boolean viewAll);

    @Select("SELECT COALESCE(NULLIF(client_id, ''), id) FROM ai_device "
            + "WHERE (#{viewAll} = 1 OR user_id = #{userId}) ORDER BY id LIMIT 20")
    List<String> selectScopeDeviceIds(@Param("userId") Long userId, @Param("viewAll") boolean viewAll);

    /** 用上报携带的 MAC 建立 Client-Id 与设备归属的映射，便于历史数据也能按用户过滤。 */
    @Update("UPDATE ai_device SET client_id = #{clientId} WHERE LOWER(mac_address) = LOWER(#{mac}) "
            + "AND (client_id IS NULL OR client_id = '' OR client_id <> #{clientId})")
    int bindClientId(@Param("mac") String mac, @Param("clientId") String clientId);

    /** 上报包未携带 MAC 时，从已绑定的 Client-Id / 设备主键反查 ai_device 中的 MAC。 */
    @Select("SELECT mac_address FROM ai_device "
            + "WHERE (mac_address = #{deviceId} OR client_id = #{deviceId} OR id = #{deviceId}) "
            + "AND mac_address IS NOT NULL AND mac_address <> '' "
            + "ORDER BY CASE WHEN mac_address = #{deviceId} THEN 0 "
            + "WHEN client_id = #{deviceId} THEN 1 ELSE 2 END LIMIT 1")
    String selectMacAddressByDeviceId(@Param("deviceId") String deviceId);

    /** 演示数据需要与真实上报一样同时写入 device_id 与 mac_address。 */
    @Select("SELECT COALESCE(NULLIF(client_id, ''), id) AS deviceId, mac_address AS macAddress FROM ai_device "
            + "WHERE (#{viewAll} = 1 OR user_id = #{userId}) "
            + "AND mac_address IS NOT NULL AND mac_address <> '' ORDER BY id LIMIT 20")
    List<Map<String, Object>> selectScopeDeviceMacs(@Param("userId") Long userId,
            @Param("viewAll") boolean viewAll);

    @Select("SELECT r.value FROM data_report_record r WHERE r.metric_key = #{metricKey} AND " + SCOPE_CONDITION
            + " ORDER BY r.report_time ASC LIMIT #{limit}")
    List<BigDecimal> selectHistoryValues(@Param("metricKey") String metricKey, @Param("limit") int limit,
            @Param("userId") Long userId, @Param("viewAll") boolean viewAll);

    @Select("SELECT r.report_time FROM data_report_record r WHERE r.metric_key = #{metricKey} AND "
            + SCOPE_CONDITION + " ORDER BY r.report_time DESC LIMIT 1")
    Date selectLatestReportTime(@Param("metricKey") String metricKey, @Param("userId") Long userId,
            @Param("viewAll") boolean viewAll);

    @Select("SELECT SUM(CASE WHEN r.value = 0 OR r.value IS NULL THEN 1 ELSE 0 END) AS zeroCount, "
            + "SUM(CASE WHEN r.value <> 0 AND r.value IS NOT NULL THEN 1 ELSE 0 END) AS nonZeroCount "
            + "FROM data_report_record r WHERE " + SCOPE_CONDITION)
    Map<String, Object> selectValueZeroDistribution(@Param("userId") Long userId, @Param("viewAll") boolean viewAll);

    @Select("SELECT COUNT(*) AS total, COUNT(DISTINCT r.device_id) AS deviceCount, "
            + "SUM(CASE WHEN r.status = 0 THEN 1 ELSE 0 END) AS abnormal, "
            + "MIN(r.report_time) AS firstTime, MAX(r.report_time) AS lastTime "
            + "FROM data_report_record r WHERE " + SCOPE_CONDITION)
    Map<String, Object> selectDashboardSummary(@Param("userId") Long userId, @Param("viewAll") boolean viewAll);

    @Select("SELECT DATE_FORMAT(DATE_ADD(r.report_time, INTERVAL #{offsetHours} HOUR), '%Y-%m-%d') AS statDate, "
            + "COUNT(*) AS cnt, COUNT(DISTINCT r.device_id) AS deviceCount FROM data_report_record r "
            + "WHERE " + SCOPE_CONDITION + " AND r.report_time IS NOT NULL GROUP BY statDate ORDER BY statDate ASC")
    List<Map<String, Object>> selectDailyUploads(@Param("offsetHours") int offsetHours,
            @Param("userId") Long userId, @Param("viewAll") boolean viewAll);

    @Select("SELECT r.device_id AS deviceId, COUNT(*) AS cnt, "
            + "DATE_FORMAT(DATE_ADD(MAX(r.report_time), INTERVAL #{offsetHours} HOUR), '%Y-%m-%d %H:%i:%s') AS lastTime, "
            + "(SELECT COALESCE(NULLIF(d2.alias, ''), NULLIF(d2.mac_address, ''), d2.id) FROM ai_device d2 "
            + "WHERE d2.client_id = r.device_id OR d2.id = r.device_id OR d2.mac_address = r.device_id LIMIT 1) AS deviceName, "
            + "(SELECT d3.mac_address FROM ai_device d3 "
            + "WHERE d3.client_id = r.device_id OR d3.id = r.device_id OR d3.mac_address = r.device_id LIMIT 1) AS macAddress "
            + "FROM data_report_record r WHERE r.device_id IS NOT NULL AND r.device_id <> '' AND "
            + SCOPE_CONDITION + " GROUP BY r.device_id ORDER BY cnt DESC LIMIT 20")
    List<Map<String, Object>> selectDeviceUploads(@Param("offsetHours") int offsetHours,
            @Param("userId") Long userId, @Param("viewAll") boolean viewAll);

    @Select("SELECT DAYOFWEEK(DATE_ADD(r.report_time, INTERVAL #{offsetHours} HOUR)) AS dow, "
            + "HOUR(DATE_ADD(r.report_time, INTERVAL #{offsetHours} HOUR)) AS hr, COUNT(*) AS cnt "
            + "FROM data_report_record r WHERE " + SCOPE_CONDITION
            + " AND r.report_time IS NOT NULL GROUP BY dow, hr")
    List<Map<String, Object>> selectHourlyDistribution(@Param("offsetHours") int offsetHours,
            @Param("userId") Long userId, @Param("viewAll") boolean viewAll);
}
