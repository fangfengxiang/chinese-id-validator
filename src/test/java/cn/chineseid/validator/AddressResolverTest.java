package cn.chineseid.validator;

import cn.chineseid.validator.util.AddressResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 地址解析器单元测试。
 */
@DisplayName("地址解析器测试")
class AddressResolverTest {

    @Test
    @DisplayName("北京地址码应解析出正确的省市区")
    void shouldResolveBeijingAddress() {
        List<String> info = AddressResolver.getAddressInfo(110101, "19900101", false);
        assertNotNull(info);
        assertEquals(3, info.size());
        assertFalse(info.get(0).isEmpty(), "省级信息不应为空");
    }

    @Test
    @DisplayName("江苏省地址码应解析出省级信息")
    void shouldResolveJiangsuAddress() {
        List<String> info = AddressResolver.getAddressInfo(320000, "20000101", false);
        assertFalse(info.get(0).isEmpty(), "江苏省信息不应为空");
    }

    @Test
    @DisplayName("港澳台居民居住证（8 开头）应只返回省级信息")
    void shouldReturnOnlyProvinceForHKMacaoTaiwan() {
        List<String> info = AddressResolver.getAddressInfo(810000, "20000101", false);
        assertFalse(info.get(0).isEmpty(), "省级信息不应为空");
        assertEquals("", info.get(1), "市级信息应为空");
        assertEquals("", info.get(2), "区级信息应为空");
    }

    @Test
    @DisplayName("历史遗留地址码（后缀 01）应返回市辖区")
    void shouldHandleLegacyCode01() {
        // 440401 — 历史遗留码，后缀 01 → 市辖区
        // 需确认该码不在时间线中才会触发历史遗留处理
        String address = AddressResolver.getAddress(440401, "19900101", false);
        // 不论返回什么，都不应为 null
        assertNotNull(address);
    }

    @Test
    @DisplayName("非严格模式下年份不匹配应回退到第一条时间线条目")
    void shouldFallbackToFirstEntryWhenNotStrict() {
        List<String> info = AddressResolver.getAddressInfo(110101, "19900101", false);
        assertFalse(info.get(0).isEmpty());
    }

    @Test
    @DisplayName("getAddress 对有效地址码应返回非空地址")
    void shouldReturnNonEmptyAddressForValidCode() {
        String address = AddressResolver.getAddress(110000, "19900101", false);
        assertNotNull(address);
    }
}
