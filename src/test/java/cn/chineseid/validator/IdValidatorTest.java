package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.generator.FakeIdBuilder;
import cn.chineseid.validator.model.BasicIdInfo;
import cn.chineseid.validator.model.IdInfo;
import cn.chineseid.validator.model.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * IdValidator 主 API 测试。
 */
@DisplayName("IdValidator 主 API 测试")
class IdValidatorTest {

    @Test
    @DisplayName("18 位身份证号严格校验通过")
    void shouldValidate18DigitIdStrictly() {
        // 生成一个可用的假号
        String fakeId = IdValidator.fakeRequireId(
                new FakeIdBuilder().eighteen().address("江苏省").birthday("200001").sex(Sex.MALE));
        assertTrue(IdValidator.isValid(fakeId, true));
        assertTrue(IdValidator.isStrictValid(fakeId));
    }

    @Test
    @DisplayName("18 位身份证号宽松校验通过")
    void shouldValidate18DigitIdLoosely() {
        String fakeId = IdValidator.fakeId();
        assertTrue(IdValidator.isValid(fakeId, false));
        assertTrue(IdValidator.isLooseValid(fakeId));
    }

    @Test
    @DisplayName("无效身份证号校验不通过")
    void shouldRejectInvalidId() {
        assertFalse(IdValidator.isValid("123", false));
        assertFalse(IdValidator.isValid("123456789012345678", false));
        assertFalse(IdValidator.isValid(null, false));
        assertFalse(IdValidator.isValid("", false));
    }

    @Test
    @DisplayName("15 位身份证号校验通过")
    void shouldValidate15DigitId() {
        String fakeId15 = IdValidator.fakeRequireId(
                new FakeIdBuilder().fifteen().address("江苏省").birthday("200001").sex(Sex.FEMALE));
        assertEquals(15, fakeId15.length());
        assertTrue(IdValidator.isValid(fakeId15, false));
    }

    @Test
    @DisplayName("isValidBasic 应跳过地址码校验")
    void shouldValidateBasicWithoutAddress() {
        String fakeId = IdValidator.fakeId();
        assertTrue(IdValidator.isValidBasic(fakeId));
    }

    @Test
    @DisplayName("getInfo 返回完整信息")
    void shouldReturnFullInfo() {
        String fakeId = IdValidator.fakeRequireId(new FakeIdBuilder()
                .eighteen()
                .address("北京市")
                .birthday("19900101")
                .sex(Sex.MALE));
        IdInfo info = IdValidator.getInfo(fakeId, true);

        assertEquals(18, info.getLength().getLength());
        assertEquals(Sex.MALE, info.getSex());
        assertNotNull(info.getBirthday());
        assertNotNull(info.getConstellation());
        assertNotNull(info.getChineseZodiac());
        assertFalse(info.getAddress().isEmpty());
        assertEquals(3, info.getAddressTree().size());
        assertEquals("1990-01-01", info.getBirthday().toString());
    }

    @Test
    @DisplayName("getBasicInfo 返回不含地址的基础信息")
    void shouldReturnBasicInfo() {
        String fakeId = IdValidator.fakeRequireId(new FakeIdBuilder()
                .eighteen()
                .address("北京市")
                .birthday("19900101")
                .sex(Sex.FEMALE));
        BasicIdInfo info = IdValidator.getBasicInfo(fakeId);

        assertEquals(Sex.FEMALE, info.getSex());
        assertEquals(18, info.getLength().getLength());
        assertNotNull(info.getConstellation());
        assertNotNull(info.getChineseZodiac());
        assertEquals("1990-01-01", info.getBirthday().toString());
    }

    @Test
    @DisplayName("getInfo 对无效身份证号应抛出异常")
    void shouldThrowForInvalidIdGetInfo() {
        assertThrows(IllegalArgumentException.class, () -> IdValidator.getInfo("invalid", false));
    }

    @Test
    @DisplayName("getBasicInfo 对无效身份证号应抛出异常")
    void shouldThrowForInvalidIdGetBasicInfo() {
        assertThrows(IllegalArgumentException.class, () -> IdValidator.getBasicInfo("invalid"));
    }

    @Test
    @DisplayName("upgradeId 升级后应通过校验")
    void upgradedIdShouldPassValidation() {
        String upgraded = IdValidator.upgradeId("610104620927690");
        assertEquals(18, upgraded.length());
        assertTrue(IdValidator.isValid(upgraded, true));
    }

    @Test
    @DisplayName("upgradeId 对 18 位身份证号应抛出异常")
    void shouldThrowFor18DigitUpgrade() {
        assertThrows(IllegalArgumentException.class, () -> IdValidator.upgradeId("110101199003076534"));
    }
}
