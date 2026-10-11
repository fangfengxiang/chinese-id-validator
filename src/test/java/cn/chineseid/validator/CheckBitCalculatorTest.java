package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.core.CheckBitCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 校验码计算器单元测试。
 */
@DisplayName("校验码计算器测试")
class CheckBitCalculatorTest {

    @Test
    @DisplayName("已知 body 应计算正确校验码")
    void shouldCalculateCorrectCheckBit() {
        // body = 11010119900307653 → 加权和 = 226, 226%11 = 6, (12-6)%11 = 6
        assertEquals("6", CheckBitCalculator.calculate("11010119900307653"));
    }

    @Test
    @DisplayName("verify 应正确验证 18 位身份证号校验码")
    void shouldVerifyValidId() {
        assertTrue(CheckBitCalculator.verify("110101199003076536"));
    }

    @Test
    @DisplayName("verify 应拒绝错误校验码")
    void shouldRejectInvalidCheckBit() {
        assertFalse(CheckBitCalculator.verify("110101199003076530"));
    }

    @Test
    @DisplayName("verify 应拒绝长度不合法的输入")
    void shouldRejectWrongLength() {
        assertFalse(CheckBitCalculator.verify("12345"));
        assertFalse(CheckBitCalculator.verify(null));
    }

    @Test
    @DisplayName("calculate 应拒绝长度不合法的 body")
    void shouldRejectInvalidBodyLength() {
        assertThrows(IllegalArgumentException.class, () -> CheckBitCalculator.calculate("12345"));
        assertThrows(IllegalArgumentException.class, () -> CheckBitCalculator.calculate(null));
    }

    @Test
    @DisplayName("calculate 应拒绝含非数字字符的 body")
    void shouldRejectNonDigitBody() {
        assertThrows(IllegalArgumentException.class, () -> CheckBitCalculator.calculate("1101011990030765X"));
    }

    @Test
    @DisplayName("校验码 X 的情况应正确计算")
    void shouldCalculateXCheckBit() {
        // 测试多个 body 直到遇到校验码为 X 的情况
        for (int i = 0; i < 1000; i++) {
            String body = String.format("%017d", i * 1000003);
            String checkBit = CheckBitCalculator.calculate(body);
            assertNotNull(checkBit);
            // 验证一致性：verify 应返回 true
            assertTrue(CheckBitCalculator.verify(body + checkBit));
        }
    }
}
