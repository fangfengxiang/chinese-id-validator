package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.core.CodeParser;
import cn.chineseid.validator.core.Validator;
import cn.chineseid.validator.model.IdCode;
import cn.chineseid.validator.model.IdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 身份证号校验器单元测试。
 */
@DisplayName("身份证号校验器测试")
class ValidatorTest {

    @Test
    @DisplayName("checkBirthdayCode 合法日期返回 true")
    void shouldAcceptValidBirthday() {
        assertTrue(Validator.checkBirthdayCode("19900101"));
        assertTrue(Validator.checkBirthdayCode("20000229"));
    }

    @Test
    @DisplayName("checkBirthdayCode 非闰年 2 月 29 日返回 false")
    void shouldRejectFeb29InNonLeapYear() {
        assertFalse(Validator.checkBirthdayCode("20230229"));
        assertFalse(Validator.checkBirthdayCode("21000229"));
    }

    @Test
    @DisplayName("checkBirthdayCode null 返回 false")
    void shouldRejectNullBirthday() {
        assertFalse(Validator.checkBirthdayCode(null));
    }

    @Test
    @DisplayName("checkBirthdayCode 长度不为 8 返回 false")
    void shouldRejectWrongLengthBirthday() {
        assertFalse(Validator.checkBirthdayCode("12345"));
        assertFalse(Validator.checkBirthdayCode("1990011"));
        assertFalse(Validator.checkBirthdayCode("199001011"));
    }

    @Test
    @DisplayName("checkBirthdayCode 年份非数字返回 false")
    void shouldRejectNonNumericYear() {
        assertFalse(Validator.checkBirthdayCode("abcd0101"));
    }

    @Test
    @DisplayName("checkBirthdayCode 年份小于 1800 返回 false")
    void shouldRejectYearBefore1800() {
        assertFalse(Validator.checkBirthdayCode("17990101"));
    }

    @Test
    @DisplayName("checkBirthdayCode 年份大于当前年返回 false")
    void shouldRejectFutureYear() {
        assertFalse(Validator.checkBirthdayCode("29990101"));
    }

    @Test
    @DisplayName("checkBirthdayCode 非法日期返回 false")
    void shouldRejectInvalidDate() {
        assertFalse(Validator.checkBirthdayCode("19900001"));
        assertFalse(Validator.checkBirthdayCode("19901301"));
        assertFalse(Validator.checkBirthdayCode("19900132"));
    }

    @Test
    @DisplayName("checkOrderCode 合法顺序码返回 true")
    void shouldAcceptValidOrderCode() {
        assertTrue(Validator.checkOrderCode("001"));
        assertTrue(Validator.checkOrderCode("999"));
    }

    @Test
    @DisplayName("checkOrderCode null 返回 false")
    void shouldRejectNullOrderCode() {
        assertFalse(Validator.checkOrderCode(null));
    }

    @Test
    @DisplayName("checkOrderCode 长度不为 3 返回 false")
    void shouldRejectWrongLengthOrderCode() {
        assertFalse(Validator.checkOrderCode("01"));
        assertFalse(Validator.checkOrderCode("0001"));
    }

    @Test
    @DisplayName("validate 18 位身份证号校验通过")
    void shouldValidate18DigitId() {
        IdCode idCode = CodeParser.parse("110101199003076536");
        assertTrue(Validator.validate(idCode, true));
    }

    @Test
    @DisplayName("validate 15 位身份证号校验通过")
    void shouldValidate15DigitId() {
        IdCode idCode = CodeParser.parse("610104620927690");
        assertTrue(Validator.validate(idCode, false));
    }

    @Test
    @DisplayName("validate 校验码错误返回 false")
    void shouldRejectWrongCheckBit() {
        IdCode idCode = CodeParser.parse("110101199003076537");
        assertFalse(Validator.validate(idCode, true));
    }

    @Test
    @DisplayName("checkAddressCode 有效地址码返回 true")
    void shouldAcceptValidAddressCode() {
        IdCode idCode = CodeParser.parse("110101199003076536");
        assertTrue(Validator.checkAddressCode(idCode, true));
    }

    @Test
    @DisplayName("validate 18 位顺序码非法返回 false")
    void shouldReject18DigitBadOrderCode() {
        IdCode code = new IdCode("11010119900307012", 110101, "19900307", "01", "6", IdType.EIGHTEEN);
        assertFalse(Validator.validate(code, true));
    }

    @Test
    @DisplayName("validate 15 位地址码非法返回 false")
    void shouldReject15DigitBadAddressCode() {
        IdCode code = new IdCode("999999800101123", 999999, "19800101", "123", "", IdType.FIFTEEN);
        assertFalse(Validator.validate(code, true));
    }

    @Test
    @DisplayName("validate 15 位生日码非法返回 false")
    void shouldReject15DigitBadBirthdayCode() {
        IdCode code = new IdCode("110101800230123", 110101, "19800230", "123", "", IdType.FIFTEEN);
        assertFalse(Validator.validate(code, true));
    }

    @Test
    @DisplayName("validate 15 位顺序码非法返回 false")
    void shouldReject15DigitBadOrderCode() {
        IdCode code = new IdCode("110101800101123", 110101, "19800101", "12", "", IdType.FIFTEEN);
        assertFalse(Validator.validate(code, true));
    }
}
