package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.util.ConstellationCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 星座计算器单元测试。
 */
@DisplayName("星座计算器测试")
class ConstellationCalculatorTest {

    @Test
    @DisplayName("1月20日之后为水瓶座")
    void shouldReturnAquarius() {
        assertEquals("水瓶座", ConstellationCalculator.calculate("19900120"));
        assertEquals("水瓶座", ConstellationCalculator.calculate("19900218"));
    }

    @Test
    @DisplayName("1月19日之前为摩羯座")
    void shouldReturnCapricorn() {
        assertEquals("摩羯座", ConstellationCalculator.calculate("19900119"));
    }

    @Test
    @DisplayName("3月21日之后为白羊座")
    void shouldReturnAries() {
        assertEquals("白羊座", ConstellationCalculator.calculate("19900321"));
        assertEquals("白羊座", ConstellationCalculator.calculate("19900419"));
    }

    @Test
    @DisplayName("5月21日之后为双子座")
    void shouldReturnGemini() {
        assertEquals("双子座", ConstellationCalculator.calculate("19900521"));
    }

    @Test
    @DisplayName("10月24日之后为天蝎座")
    void shouldReturnScorpio() {
        assertEquals("天蝎座", ConstellationCalculator.calculate("19901024"));
    }

    @Test
    @DisplayName("12月22日之后为摩羯座")
    void shouldReturnCapricornForDecember() {
        assertEquals("摩羯座", ConstellationCalculator.calculate("19901222"));
    }

    @Test
    @DisplayName("11月22日之前为天蝎座")
    void shouldReturnScorpioBeforeSagittarius() {
        assertEquals("天蝎座", ConstellationCalculator.calculate("19901121"));
    }

    @Test
    @DisplayName("11月22日之后为射手座")
    void shouldReturnSagittarius() {
        assertEquals("射手座", ConstellationCalculator.calculate("19901123"));
    }

    @Test
    @DisplayName("null 输入返回空字符串")
    void shouldReturnEmptyForNull() {
        assertEquals("", ConstellationCalculator.calculate(null));
    }

    @Test
    @DisplayName("长度不足 8 返回空字符串")
    void shouldReturnEmptyForShortInput() {
        assertEquals("", ConstellationCalculator.calculate("1234567"));
    }

    @Test
    @DisplayName("月份非数字返回空字符串")
    void shouldReturnEmptyForNonNumericMonth() {
        assertEquals("", ConstellationCalculator.calculate("1990ab01"));
    }
}
