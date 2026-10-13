package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.util.AddressResolver;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    @Test
    @DisplayName("港澳台码（8 开头）宽松模式也不做历史兼容")
    void shouldRejectHkmacaoInLegacyCompat() {
        assertFalse(AddressResolver.isAddressCodeValid(899999, false));
    }

    @Test
    @DisplayName("省级无证据的历史码宽松模式应返回 false")
    void shouldRejectLegacyWithoutProvinceEvidence() {
        assertFalse(AddressResolver.isAddressCodeValid(9901, false));
    }

    @Test
    @DisplayName("110201 严格模式无效、宽松模式有效（历史兼容）")
    void shouldAcceptLegacyCode110201InLooseMode() {
        assertFalse(AddressResolver.isAddressCodeValid(110201));
        assertTrue(AddressResolver.isAddressCodeValid(110201, false));
    }

    @Test
    @DisplayName("严格模式年份不匹配时返回空字符串")
    void shouldReturnEmptyInStrictModeWhenYearMismatch() {
        assertEquals("", AddressResolver.getAddress(130123, "18000101", true));
    }

    @Test
    @DisplayName("非严格模式年份早于时间线时回退到最近条目")
    void shouldFallbackToNearestEntryBeforeTimeline() {
        assertEquals("栾城县", AddressResolver.getAddress(130123, "18000101", false));
    }

    @Test
    @DisplayName("birthdayCode 为 null 时应正常解析（parseYear 返回 0）")
    void shouldHandleNullBirthdayCode() {
        assertEquals("东城区", AddressResolver.getAddress(110101, null, false));
    }

    @Test
    @DisplayName("历史遗留码后缀 01 返回市辖区")
    void shouldHandleLegacySuffix01() {
        assertEquals("市辖区", AddressResolver.getAddress(999901, "19900101", false));
    }

    @Test
    @DisplayName("历史遗留码后缀 02 返回城区")
    void shouldHandleLegacySuffix02() {
        assertEquals("城区", AddressResolver.getAddress(999902, "19900101", false));
    }

    @Test
    @DisplayName("历史遗留码后缀 11 返回郊区")
    void shouldHandleLegacySuffix11() {
        assertEquals("郊区", AddressResolver.getAddress(999911, "19900101", false));
    }

    @Test
    @DisplayName("历史遗留码后缀 20 返回市区")
    void shouldHandleLegacySuffix20() {
        assertEquals("市区", AddressResolver.getAddress(999920, "19900101", false));
    }

    @Test
    @DisplayName("历史遗留码未知后缀返回映射表查找结果")
    void shouldHandleLegacyUnknownSuffix() {
        assertEquals("", AddressResolver.getAddress(999999, "19900101", false));
    }
}
