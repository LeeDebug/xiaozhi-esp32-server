package xiaozhi.modules.device.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import xiaozhi.modules.device.service.IpRegionService.Region;

class IpRegionServiceImplTest {

    @Test
    void parseChinaRecord() {
        Region region = IpRegionServiceImpl.parse("中国|广东省|深圳市|电信|CN");
        assertNotNull(region);
        assertEquals("广东省", region.province());
        assertEquals("深圳市", region.city());
    }

    @Test
    void parseMunicipality() {
        Region region = IpRegionServiceImpl.parse("中国|北京市|北京市|电信|CN");
        assertNotNull(region);
        assertEquals("北京市", region.province());
        assertEquals("北京市", region.city());
    }

    @Test
    void parseKeepsProvinceWhenCityUnknown() {
        Region region = IpRegionServiceImpl.parse("中国|四川省|0|电信|CN");
        assertNotNull(region);
        assertEquals("四川省", region.province());
        assertNull(region.city());
    }

    @Test
    void parseRejectsForeignAndReserved() {
        assertNull(IpRegionServiceImpl.parse("Australia|Queensland|Brisbane|0|AU"));
        assertNull(IpRegionServiceImpl.parse("United States|California|0|Google LLC|US"));
        assertNull(IpRegionServiceImpl.parse("Reserved|Reserved|Reserved|0|0"));
    }

    @Test
    void parseRejectsChinaWithoutProvince() {
        assertNull(IpRegionServiceImpl.parse("中国|0|0|电信|CN"));
    }

    @Test
    void parseRejectsMalformed() {
        assertNull(IpRegionServiceImpl.parse(null));
        assertNull(IpRegionServiceImpl.parse(""));
        assertNull(IpRegionServiceImpl.parse("中国|广东省"));
    }

    @Test
    void publicIpv4Accepted() {
        assertTrue(IpRegionServiceImpl.isPublicIpv4("113.108.81.189"));
        assertTrue(IpRegionServiceImpl.isPublicIpv4("8.8.8.8"));
    }

    @Test
    void privateAndSpecialAddressesRejected() {
        assertFalse(IpRegionServiceImpl.isPublicIpv4("192.168.1.1"));
        assertFalse(IpRegionServiceImpl.isPublicIpv4("10.0.0.8"));
        assertFalse(IpRegionServiceImpl.isPublicIpv4("172.16.5.4"));
        assertFalse(IpRegionServiceImpl.isPublicIpv4("127.0.0.1"));
        assertFalse(IpRegionServiceImpl.isPublicIpv4("169.254.1.1"));
        assertFalse(IpRegionServiceImpl.isPublicIpv4("0.0.0.0"));
        assertFalse(IpRegionServiceImpl.isPublicIpv4("224.0.0.1"));
    }

    @Test
    void ipv6AndGarbageRejectedWithoutDnsLookup() {
        assertFalse(IpRegionServiceImpl.isPublicIpv4("::1"));
        assertFalse(IpRegionServiceImpl.isPublicIpv4("2408:8000::1"));
        assertFalse(IpRegionServiceImpl.isPublicIpv4("example.com"));
        assertFalse(IpRegionServiceImpl.isPublicIpv4("unknown"));
        assertFalse(IpRegionServiceImpl.isPublicIpv4(""));
        assertFalse(IpRegionServiceImpl.isPublicIpv4(null));
    }

    @Test
    void realDatabaseResolvesChineseCity() {
        IpRegionServiceImpl service = new IpRegionServiceImpl();
        service.init();
        Region region = service.resolve("113.108.81.189");
        assertNotNull(region, "内置 xdb 应能解析中国公网 IP");
        assertEquals("广东省", region.province());
        // X-Forwarded-For 多级代理列表只取第一个
        assertNotNull(service.resolve("113.108.81.189, 10.0.0.1"));
        assertNull(service.resolve("192.168.1.1"));
        assertNull(service.resolve("8.8.8.8"));
        service.destroy();
    }
}
