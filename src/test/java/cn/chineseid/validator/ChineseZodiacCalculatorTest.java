package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.util.ChineseZodiacCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 生肖计算器单元测试。
 */
@DisplayName("生肖计算器测试")
class ChineseZodiacCalculatorTest {

    @Test
    @DisplayName("1900 年为子鼠")
    void shouldReturnZiFor1900() {
        assertEquals("子鼠", ChineseZodiacCalculator.calculate("19000101"));
    }

    @Test
    @DisplayName("1901 年为丑牛")
    void shouldReturnChouFor1901() {
        assertEquals("丑牛", ChineseZodiacCalculator.calculate("19010101"));
    }

    @Test
    @DisplayName("1988 年为辰龙")
    void shouldReturnChenFor1988() {
        assertEquals("辰龙", ChineseZodiacCalculator.calculate("19880101"));
    }

    @Test
    @DisplayName("2000 年为辰龙")
    void shouldReturnChenFor2000() {
        assertEquals("辰龙", ChineseZodiacCalculator.calculate("20000101"));
    }

    @Test
    @DisplayName("2024 年为辰龙")
    void shouldReturnChenFor2024() {
        assertEquals("辰龙", ChineseZodiacCalculator.calculate("20240101"));
    }

    @Test
    @DisplayName("2025 年为巳蛇")
    void shouldReturnSiFor2025() {
        assertEquals("巳蛇", ChineseZodiacCalculator.calculate("20250101"));
    }

    @Test
    @DisplayName("2026 年为午马")
    void shouldReturnWuFor2026() {
        assertEquals("午马", ChineseZodiacCalculator.calculate("20260101"));
    }

    @Test
    @DisplayName("null 输入返回空字符串")
    void shouldReturnEmptyForNull() {
        assertEquals("", ChineseZodiacCalculator.calculate(null));
    }

    @Test
    @DisplayName("长度不足 4 返回空字符串")
    void shouldReturnEmptyForShortInput() {
        assertEquals("", ChineseZodiacCalculator.calculate("123"));
    }

    @Test
    @DisplayName("年份非数字返回空字符串")
    void shouldReturnEmptyForNonNumericYear() {
        assertEquals("", ChineseZodiacCalculator.calculate("abcd0101"));
    }

    @Test
    @DisplayName("1899 年（基准年前）负数取模修正为亥猪")
    void shouldReturnHaiForYearBeforeBase() {
        assertEquals("亥猪", ChineseZodiacCalculator.calculate("18990101"));
    }
}
