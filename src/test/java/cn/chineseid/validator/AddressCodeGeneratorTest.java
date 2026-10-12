package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.generator.AddressCodeGenerator;
import cn.chineseid.validator.model.AddressClassification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 地址码生成器单元测试。
 */
@DisplayName("地址码生成器测试")
class AddressCodeGeneratorTest {

    @Test
    @DisplayName("classify null 返回 COUNTRY")
    void shouldClassifyNullAsCountry() {
        assertEquals(AddressClassification.COUNTRY, AddressCodeGenerator.classify(null));
    }

    @Test
    @DisplayName("classify 空字符串返回 COUNTRY")
    void shouldClassifyEmptyAsCountry() {
        assertEquals(AddressClassification.COUNTRY, AddressCodeGenerator.classify(""));
    }

    @Test
    @DisplayName("classify 8 开头返回 SPECIAL（港澳台）")
    void shouldClassifySpecialCode() {
        assertEquals(AddressClassification.SPECIAL, AddressCodeGenerator.classify("810000"));
        assertEquals(AddressClassification.SPECIAL, AddressCodeGenerator.classify("820000"));
        assertEquals(AddressClassification.SPECIAL, AddressCodeGenerator.classify("830000"));
    }

    @Test
    @DisplayName("classify 后四位为 0000 返回 PROVINCE")
    void shouldClassifyProvinceCode() {
        assertEquals(AddressClassification.PROVINCE, AddressCodeGenerator.classify("110000"));
        assertEquals(AddressClassification.PROVINCE, AddressCodeGenerator.classify("440000"));
    }

    @Test
    @DisplayName("classify 后两位为 00 返回 CITY")
    void shouldClassifyCityCode() {
        assertEquals(AddressClassification.CITY, AddressCodeGenerator.classify("110100"));
        assertEquals(AddressClassification.CITY, AddressCodeGenerator.classify("440100"));
    }

    @Test
    @DisplayName("classify 县级码返回 DISTRICT")
    void shouldClassifyDistrictCode() {
        assertEquals(AddressClassification.DISTRICT, AddressCodeGenerator.classify("110101"));
        assertEquals(AddressClassification.DISTRICT, AddressCodeGenerator.classify("440106"));
    }

    @Test
    @DisplayName("generate null 返回随机县级地址码（6 位且后两位非 00）")
    void shouldGenerateRandomDistrictForNull() {
        String code = AddressCodeGenerator.generate(null);
        assertEquals(6, code.length());
        assertNotEquals("00", code.substring(4, 6));
    }

    @Test
    @DisplayName("generate 空字符串返回随机县级地址码")
    void shouldGenerateRandomDistrictForEmpty() {
        String code = AddressCodeGenerator.generate("");
        assertEquals(6, code.length());
        assertNotEquals("00", code.substring(4, 6));
    }

    @Test
    @DisplayName("generate 省级名称北京市返回该省下随机县级地址码")
    void shouldGenerateDistrictForProvince() {
        String code = AddressCodeGenerator.generate("北京市");
        assertEquals(6, code.length());
        assertTrue(code.startsWith("11"));
        assertNotEquals("00", code.substring(4, 6));
    }

    @Test
    @DisplayName("generate 省级名称广东省返回该省下随机县级地址码")
    void shouldGenerateDistrictForGuangdong() {
        String code = AddressCodeGenerator.generate("广东省");
        assertEquals(6, code.length());
        assertTrue(code.startsWith("44"));
        assertNotEquals("00", code.substring(4, 6));
    }

    @Test
    @DisplayName("generate 未知地址返回全国随机县级地址码")
    void shouldGenerateRandomForUnknownAddress() {
        String code = AddressCodeGenerator.generate("不存在的地址");
        assertEquals(6, code.length());
    }
}
