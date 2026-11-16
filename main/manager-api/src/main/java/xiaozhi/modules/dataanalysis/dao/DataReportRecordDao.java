package xiaozhi.modules.dataanalysis.dao;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import xiaozhi.common.dao.BaseDao;
import xiaozhi.modules.dataanalysis.entity.DataReportRecordEntity;

@Mapper
public interface DataReportRecordDao extends BaseDao<DataReportRecordEntity> {

    @Select("SELECT COUNT(*) AS total, COUNT(DISTINCT device_id) AS deviceCount, AVG(value) AS avgValue, "
            + "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS abnormal FROM data_report_record")
    Map<String, Object> selectStatistics();

    @Select("SELECT metric_key AS metricKey, metric_name AS metricName, COUNT(*) AS cnt, AVG(value) AS avgVal, "
            + "MAX(value) AS maxVal, MIN(value) AS minVal FROM data_report_record "
            + "GROUP BY metric_key, metric_name ORDER BY cnt DESC")
    List<Map<String, Object>> selectMetricDistribution();

    @Select("SELECT DISTINCT metric_key AS metricKey, metric_name AS metricName FROM data_report_record ORDER BY metric_key")
    List<Map<String, Object>> selectMetricList();

    @Select("SELECT value FROM data_report_record WHERE metric_key = #{metricKey} ORDER BY report_time ASC LIMIT #{limit}")
    List<BigDecimal> selectHistoryValues(@Param("metricKey") String metricKey, @Param("limit") int limit);

    @Select("SELECT report_time FROM data_report_record WHERE metric_key = #{metricKey} ORDER BY report_time DESC LIMIT 1")
    Date selectLatestReportTime(@Param("metricKey") String metricKey);

    @Select("SELECT COUNT(*) AS total, COUNT(DISTINCT device_id) AS deviceCount, "
            + "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS abnormal, "
            + "MIN(report_time) AS firstTime, MAX(report_time) AS lastTime FROM data_report_record")
    Map<String, Object> selectDashboardSummary();

    @Select("SELECT DATE_FORMAT(DATE_ADD(report_time, INTERVAL #{offsetHours} HOUR), '%Y-%m-%d') AS statDate, "
            + "COUNT(*) AS cnt, COUNT(DISTINCT device_id) AS deviceCount FROM data_report_record "
            + "WHERE report_time IS NOT NULL GROUP BY statDate ORDER BY statDate ASC")
    List<Map<String, Object>> selectDailyUploads(@Param("offsetHours") int offsetHours);

    @Select("SELECT device_id AS deviceId, COUNT(*) AS cnt, "
            + "DATE_FORMAT(DATE_ADD(MAX(report_time), INTERVAL #{offsetHours} HOUR), '%Y-%m-%d %H:%i:%s') AS lastTime "
            + "FROM data_report_record GROUP BY device_id ORDER BY cnt DESC LIMIT 20")
    List<Map<String, Object>> selectDeviceUploads(@Param("offsetHours") int offsetHours);

    @Select("SELECT DAYOFWEEK(DATE_ADD(report_time, INTERVAL #{offsetHours} HOUR)) AS dow, "
            + "HOUR(DATE_ADD(report_time, INTERVAL #{offsetHours} HOUR)) AS hr, COUNT(*) AS cnt "
            + "FROM data_report_record WHERE report_time IS NOT NULL GROUP BY dow, hr")
    List<Map<String, Object>> selectHourlyDistribution(@Param("offsetHours") int offsetHours);
}
