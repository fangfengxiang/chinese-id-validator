package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.util.StringUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 字符串工具类单元测试。
 */
@DisplayName("字符串工具类测试")
class StringUtilTest {

    @Test
    @DisplayName("substr 正常截取")
    void shouldSubstrNormally() {
        assertEquals("abc", StringUtil.substr("abcdef", 0, 3));
        assertEquals("def", StringUtil.substr("abcdef", 3, 6));
    }

    @Test
    @DisplayName("substr null 或空字符串返回空")
    void shouldReturnEmptyForNullOrEmpty() {
        assertEquals("", StringUtil.substr(null, 0, 3));
        assertEquals("", StringUtil.substr("", 0, 3));
    }

    @Test
    @DisplayName("substr start 为负数时自动归零")
    void shouldClampNegativeStart() {
        assertEquals("abc", StringUtil.substr("abcdef", -1, 3));
    }

    @Test
    @DisplayName("substr end 超过长度时自动截断")
    void shouldClampEndBeyondLength() {
        assertEquals("def", StringUtil.substr("abcdef", 3, 100));
    }

    @Test
    @DisplayName("substr start 不小于 end 时返回空")
    void shouldReturnEmptyWhenStartGeEnd() {
        assertEquals("", StringUtil.substr("abcdef", 3, 3));
        assertEquals("", StringUtil.substr("abcdef", 5, 2));
    }

    @Test
    @DisplayName("padLeft 正常补零")
    void shouldPadLeftNormally() {
        assertEquals("00123", StringUtil.padLeft("123", 5));
        assertEquals("00000", StringUtil.padLeft("", 5));
    }

    @Test
    @DisplayName("padLeft null 当作空字符串")
    void shouldTreatNullAsEmpty() {
        assertEquals("00000", StringUtil.padLeft(null, 5));
    }

    @Test
    @DisplayName("padLeft 长度已达目标时原样返回")
    void shouldReturnAsIsWhenLongEnough() {
        assertEquals("123456", StringUtil.padLeft("123456", 5));
        assertEquals("12345", StringUtil.padLeft("12345", 5));
    }

    @Test
    @DisplayName("isNumeric 纯数字返回 true")
    void shouldReturnTrueForNumeric() {
        assertTrue(StringUtil.isNumeric("123456"));
        assertTrue(StringUtil.isNumeric("0"));
    }

    @Test
    @DisplayName("isNumeric null 或空返回 false")
    void shouldReturnFalseForNullOrEmpty() {
        assertFalse(StringUtil.isNumeric(null));
        assertFalse(StringUtil.isNumeric(""));
    }

    @Test
    @DisplayName("isNumeric 含非数字字符返回 false")
    void shouldReturnFalseForNonNumeric() {
        assertFalse(StringUtil.isNumeric("12a456"));
        assertFalse(StringUtil.isNumeric("12.34"));
        assertFalse(StringUtil.isNumeric(" 123"));
    }
}
