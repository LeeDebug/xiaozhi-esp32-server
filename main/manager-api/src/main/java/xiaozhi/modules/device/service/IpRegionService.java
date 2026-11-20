package xiaozhi.modules.device.service;

/**
 * IP 地域解析服务（离线 ip2region）。
 * 解析失败、内网/保留地址、境外 IP 一律返回 null，调用方据此跳过地域更新，不影响主流程。
 */
public interface IpRegionService {

    /**
     * 解析公网 IP 所在的省、市。
     *
     * @param ip IPv4 字符串
     * @return 中国境内公网 IP 返回省/市（完整名称），其余情况返回 null
     */
    Region resolve(String ip);

    /**
     * 省/市解析结果。
     */
    record Region(String province, String city) {
    }
}
