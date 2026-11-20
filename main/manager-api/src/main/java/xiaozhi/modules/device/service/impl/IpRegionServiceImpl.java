package xiaozhi.modules.device.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.xdb.LongByteArray;
import org.lionsoul.ip2region.xdb.Searcher;
import org.lionsoul.ip2region.xdb.Version;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.modules.device.service.IpRegionService;

/**
 * 基于 ip2region v3 的 IPv4 离线定位。xdb 整个载入内存，查询不再访问磁盘。
 * 查询结果格式：国家|省|市|ISP|ISO，只采用 ISO=CN 的记录。
 */
@Slf4j
@Service
public class IpRegionServiceImpl implements IpRegionService {

    private static final String XDB_PATH = "ip2region/ip2region_v4.xdb";

    private Searcher searcher;

    @PostConstruct
    public void init() {
        try (InputStream in = new ClassPathResource(XDB_PATH).getInputStream()) {
            LongByteArray content = Searcher.loadContentFromInputStream(in);
            this.searcher = Searcher.newWithBuffer(Version.IPv4, content);
            log.info("ip2region 定位库加载成功");
        } catch (Exception e) {
            // 定位库缺失只影响地域自动填充，不阻塞应用启动
            this.searcher = null;
            log.warn("ip2region 定位库加载失败，设备地域将不会自动填充: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        if (searcher != null) {
            try {
                searcher.close();
            } catch (IOException e) {
                log.debug("关闭 ip2region 定位库失败", e);
            }
        }
    }

    @Override
    public Region resolve(String ip) {
        if (searcher == null || StringUtils.isBlank(ip)) {
            return null;
        }
        // X-Forwarded-For 可能是“真实IP, 代理IP”的列表，取第一个
        String candidate = ip.split(",")[0].trim();
        if (!isPublicIpv4(candidate)) {
            return null;
        }
        try {
            String result;
            // buffer 模式下并发查询不保证线程安全，查询耗时为微秒级，直接加锁
            synchronized (this) {
                result = searcher.search(candidate);
            }
            return parse(result);
        } catch (Exception e) {
            log.debug("IP 定位失败: {}", candidate, e);
            return null;
        }
    }

    /**
     * 解析 “国家|省|市|ISP|ISO”，只接受中国境内、省市都有效的记录。
     */
    static Region parse(String result) {
        if (StringUtils.isBlank(result)) {
            return null;
        }
        String[] parts = result.split("\\|", -1);
        if (parts.length < 5 || !"CN".equals(parts[4])) {
            return null;
        }
        String province = normalize(parts[1]);
        String city = normalize(parts[2]);
        if (province == null) {
            return null;
        }
        return new Region(province, city);
    }

    private static String normalize(String value) {
        if (StringUtils.isBlank(value) || "0".equals(value) || "Reserved".equalsIgnoreCase(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 仅放行公网 IPv4：排除 IPv6、环回、内网、链路本地、组播等。
     */
    static boolean isPublicIpv4(String ip) {
        if (StringUtils.isBlank(ip) || ip.indexOf(':') >= 0) {
            return false;
        }
        // 只允许点分十进制，避免 InetAddress 把任意字符串当主机名发起 DNS 解析
        if (!ip.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            return !(address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress());
        } catch (Exception e) {
            return false;
        }
    }
}
