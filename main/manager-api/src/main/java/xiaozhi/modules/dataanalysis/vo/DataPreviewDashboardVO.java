package xiaozhi.modules.dataanalysis.vo;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "数据预览看板聚合结果")
public class DataPreviewDashboardVO {

    @Schema(description = "汇总指标")
    private Summary summary;

    @Schema(description = "按天上传统计")
    private List<DailyUpload> dailyUploads;

    @Schema(description = "设备上传排行")
    private List<DeviceUpload> deviceUploads;

    @Schema(description = "指标分布")
    private List<MetricDistributionVO> metricDistribution;

    @Schema(description = "星期×小时上传热力分布")
    private List<HourlyUpload> hourlyDistribution;

    @Data
    @Schema(description = "看板汇总指标")
    public static class Summary {

        @Schema(description = "上传设备数")
        private Long deviceCount;

        @Schema(description = "数据总条数")
        private Long recordCount;

        @Schema(description = "有上传的天数")
        private Long activeDays;

        @Schema(description = "异常数据条数")
        private Long abnormalCount;

        @Schema(description = "最早上传时间")
        private String firstReportTime;

        @Schema(description = "最近上传时间")
        private String lastReportTime;

        @Schema(description = "可见设备数（当前账号可见的已绑定设备），用于诊断数据为 0 的原因")
        private Long visibleDeviceCount;

        @Schema(description = "数值为 0（或空）的记录数")
        private Long zeroValueCount;

        @Schema(description = "数值非 0 的记录数")
        private Long nonZeroValueCount;
    }

    @Data
    @Schema(description = "按天上传统计")
    public static class DailyUpload {

        @Schema(description = "日期 yyyy-MM-dd（按 JVM 时区）")
        private String date;

        @Schema(description = "当天上传条数")
        private Long count;

        @Schema(description = "当天上传设备数")
        private Long deviceCount;
    }

    @Data
    @Schema(description = "设备上传统计")
    public static class DeviceUpload {

        @Schema(description = "设备ID（固件 Client-Id UUID）")
        private String deviceId;

        @Schema(description = "设备可读名称（别名/MAC/设备ID，优先别名）")
        private String deviceName;

        @Schema(description = "设备MAC地址")
        private String macAddress;

        @Schema(description = "上传条数")
        private Long count;

        @Schema(description = "最近上传时间")
        private String lastReportTime;
    }

    @Data
    @Schema(description = "星期×小时上传统计")
    public static class HourlyUpload {

        @Schema(description = "星期 1=周日 ... 7=周六（按 JVM 时区）")
        private Integer dayOfWeek;

        @Schema(description = "小时 0-23（按 JVM 时区）")
        private Integer hour;

        @Schema(description = "上传条数")
        private Long count;
    }
}
