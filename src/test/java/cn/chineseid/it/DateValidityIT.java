package cn.chineseid.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.chineseid.validator.IdValidator;
import cn.chineseid.validator.core.CheckBitCalculator;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 生日码三模式端到端功能测试。
 *
 * <p>精选特殊点生日码（闰年 2 月 29 的四种情况、大小月 31 日对照、月份越界），
 * 构造完整合法 18 位身份证号（固定合法地址码 + 算对校验码，使生日码成为唯一变量），
 * 通过 facade {@link IdValidator} 的 Basic / Strict / Loose 三模式端到端验证。
 *
 * <p><b>关键不变量</b>：三模式都校验生日（{@link IdValidator#isValidBasic} 内部调
 * {@code Validator.checkBirthdayCode}，Strict/Loose 同样）。故非法生日 → 三模式全 false，
 * 合法生日（地址码合法 + 校验码正确前提下）→ 三模式全 true。
 *
 * <p>与 {@code ValidatorTest} 的差异：单测隔离调 {@code Validator.checkBirthdayCode(字符串)}
 * 验证日期解析逻辑；本测试端到端构造完整 ID 走 facade，验证生日校验在完整校验链路中正确。
 *
 * <p>由 maven-failsafe-plugin 在 integration-test 阶段执行（{@code mvn verify}）。
 * 命名约定 {@code *IT.java} 区别于单元测试 {@code *Test.java}（surefire）。
 */
@DisplayName("生日码三模式端到端功能测试")
class DateValidityIT {

    /** 固定合法地址码（北京东城，时间线完整，三模式均通过），排除地址码干扰。 */
    private static final int VALID_ADDRESS_CODE = 110101;

    /** 固定顺序码。 */
    private static final String ORDER_CODE = "001";

    // ==================== 合法生日码：三模式均通过 ====================

    @ParameterizedTest
    @MethodSource("validBirthdayCodes")
    @DisplayName("合法生日码（闰年/大月边界）→ Basic + Strict + Loose 均通过")
    void validBirthdayShouldPassAllModes(String birthdayCode) {
        String id = buildId(birthdayCode);
        assertTrue(IdValidator.isValidBasic(id), "Basic 应通过: " + id);
        assertTrue(IdValidator.isStrictValid(id), "Strict 应通过: " + id);
        assertTrue(IdValidator.isLooseValid(id), "Loose 应通过: " + id);
    }

    static Stream<String> validBirthdayCodes() {
        return Stream.of(
                "20000229", // 世纪闰年（÷400）2 月 29 日合法
                "20240229", // 普通闰年（÷4 不÷100）2 月 29 日合法
                "20000131" // 大月（1 月）31 日合法
                );
    }

    // ==================== 非法生日码：三模式均拒绝 ====================

    @ParameterizedTest
    @MethodSource("invalidBirthdayCodes")
    @DisplayName("非法生日码（非闰年29/世纪非闰/小月31/月越界）→ Basic + Strict + Loose 均拒绝")
    void invalidBirthdayShouldFailAllModes(String birthdayCode) {
        String id = buildId(birthdayCode);
        assertFalse(IdValidator.isValidBasic(id), "Basic 应拒绝: " + id);
        assertFalse(IdValidator.isStrictValid(id), "Strict 应拒绝: " + id);
        assertFalse(IdValidator.isLooseValid(id), "Loose 应拒绝: " + id);
    }

    static Stream<String> invalidBirthdayCodes() {
        return Stream.of(
                "20230229", // 非闰年 2 月 29 日非法
                "21000229", // 世纪非闰年（÷100 不÷400）2 月 29 日非法
                "20000431", // 小月（4 月）31 日非法
                "20001301" // 月份越界（13 月）非法
                );
    }

    // ==================== 工具方法 ====================

    /**
     * 构造完整 18 位身份证号：地址码(6) + 生日码(8) + 顺序码(3) + 校验码(1)。
     *
     * <p>校验码由 {@link CheckBitCalculator#calculate} 对 body 加权求和算出，
     * 对任意 body（含非法日期）均能算出"正确"校验码，确保校验码不干扰断言，
     * 使生日码成为唯一变量。
     */
    private static String buildId(String birthdayCode) {
        String body = String.format("%06d", VALID_ADDRESS_CODE) + birthdayCode + ORDER_CODE;
        return body + CheckBitCalculator.calculate(body);
    }
}
