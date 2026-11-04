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
}
