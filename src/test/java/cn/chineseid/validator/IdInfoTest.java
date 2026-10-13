package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.model.IdInfo;
import cn.chineseid.validator.model.IdType;
import cn.chineseid.validator.model.Sex;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 完整身份证信息 DTO 单元测试。
 */
@DisplayName("完整身份证信息测试")
class IdInfoTest {

    @Test
    @DisplayName("checkBit 为 null 时应归一化为空字符串")
    void shouldNormalizeNullCheckBitToEmpty() {
        IdInfo info = new IdInfo(
                610104,
                0,
                "陕西省西安市碑林区",
                Arrays.asList("陕西省", "西安市", "碑林区"),
                LocalDate.of(1962, 9, 27),
                "天秤座",
                "寅虎",
                Sex.FEMALE,
                IdType.FIFTEEN,
                null);
        assertEquals("", info.getCheckBit());
    }

    @Test
    @DisplayName("checkBit 非 null 时应原样保留")
    void shouldKeepNonNullCheckBit() {
        IdInfo info = new IdInfo(
                110101,
                0,
                "北京市市辖区东城区",
                Arrays.asList("北京市", "市辖区", "东城区"),
                LocalDate.of(1990, 3, 7),
                "双鱼座",
                "午马",
                Sex.MALE,
                IdType.EIGHTEEN,
                "6");
        assertEquals("6", info.getCheckBit());
        assertEquals(110101, info.getAddressCode());
        assertEquals(0, info.getAbandoned());
        assertEquals(3, info.getAddressTree().size());
    }
}
