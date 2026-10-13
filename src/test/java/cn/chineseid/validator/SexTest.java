package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.model.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 性别枚举单元测试。
 */
@DisplayName("性别枚举测试")
class SexTest {

    @Test
    @DisplayName("fromCode(1) 返回 MALE")
    void shouldReturnMaleForCode1() {
        assertEquals(Sex.MALE, Sex.fromCode(1));
        assertEquals(1, Sex.fromCode(1).getCode());
    }

    @Test
    @DisplayName("fromCode(0) 返回 FEMALE")
    void shouldReturnFemaleForCode0() {
        assertEquals(Sex.FEMALE, Sex.fromCode(0));
        assertEquals(0, Sex.fromCode(0).getCode());
    }

    @Test
    @DisplayName("fromOrderDigit 奇数返回 MALE，偶数返回 FEMALE")
    void shouldInferSexFromOrderDigitParity() {
        assertEquals(Sex.MALE, Sex.fromOrderDigit(1));
        assertEquals(Sex.MALE, Sex.fromOrderDigit(9));
        assertEquals(Sex.FEMALE, Sex.fromOrderDigit(0));
        assertEquals(Sex.FEMALE, Sex.fromOrderDigit(8));
    }
}
