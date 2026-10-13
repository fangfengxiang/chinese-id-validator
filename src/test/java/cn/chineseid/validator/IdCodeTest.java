package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.model.IdCode;
import cn.chineseid.validator.model.IdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 身份证号解析结果 DTO 单元测试。
 */
@DisplayName("身份证号解析结果测试")
class IdCodeTest {

    @Test
    @DisplayName("checkBit 为 null 时应归一化为空字符串")
    void shouldNormalizeNullCheckBitToEmpty() {
        IdCode code = new IdCode("610104620927690", 610104, "19620927", "690", null, IdType.FIFTEEN);
        assertEquals("", code.getCheckBit());
    }

    @Test
    @DisplayName("checkBit 非 null 时应原样保留")
    void shouldKeepNonNullCheckBit() {
        IdCode code = new IdCode("11010119900307653", 110101, "19900307", "653", "6", IdType.EIGHTEEN);
        assertEquals("6", code.getCheckBit());
        assertEquals("11010119900307653", code.getBody());
        assertEquals(IdType.EIGHTEEN, code.getType());
    }
}
