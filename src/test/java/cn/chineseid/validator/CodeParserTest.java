package cn.chineseid.validator;

import cn.chineseid.validator.core.CodeParser;
import cn.chineseid.validator.model.IdCode;
import cn.chineseid.validator.model.IdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 身份证号解析器单元测试。
 */
@DisplayName("身份证号解析器测试")
class CodeParserTest {

    @Test
    @DisplayName("解析 18 位身份证号")
    void shouldParse18DigitId() {
        IdCode code = CodeParser.parse("110101199003076536");
        assertEquals(110101, code.getAddressCode());
        assertEquals("19900307", code.getBirthdayCode());
        assertEquals("653", code.getOrderCode());
        assertEquals("6", code.getCheckBit());
        assertEquals(IdType.EIGHTEEN, code.getType());
        assertEquals("11010119900307653", code.getBody());
    }

    @Test
    @DisplayName("解析 15 位身份证号")
    void shouldParse15DigitId() {
        IdCode code = CodeParser.parse("610104620927690");
        assertEquals(610104, code.getAddressCode());
        assertEquals("19620927", code.getBirthdayCode());
        assertEquals("690", code.getOrderCode());
        assertEquals("", code.getCheckBit());
        assertEquals(IdType.FIFTEEN, code.getType());
    }

    @Test
    @DisplayName("解析 15 位身份证号时生日码应补 19 前缀")
    void shouldPrepend19ToBirthdayFor15Digit() {
        IdCode code = CodeParser.parse("610104620927690");
        assertTrue(code.getBirthdayCode().startsWith("19"));
        assertEquals(8, code.getBirthdayCode().length());
    }

    @Test
    @DisplayName("解析含 X 校验码的 18 位身份证号")
    void shouldParseIdWithXCheckBit() {
        IdCode code = CodeParser.parse("11010119900307653X");
        assertEquals("X", code.getCheckBit());
    }

    @Test
    @DisplayName("解析不合法长度的身份证号应抛出异常")
    void shouldRejectInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> CodeParser.parse("12345"));
        assertThrows(IllegalArgumentException.class, () -> CodeParser.parse("1234567890123456789"));
        assertThrows(IllegalArgumentException.class, () -> CodeParser.parse(null));
    }
}
