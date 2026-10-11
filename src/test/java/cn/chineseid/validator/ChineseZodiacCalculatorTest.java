package cn.chineseid.validator;

import cn.chineseid.validator.util.ChineseZodiacCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}
