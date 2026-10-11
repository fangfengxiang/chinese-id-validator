package cn.chineseid.it;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.IdValidator;
import cn.chineseid.validator.generator.FakeIdBuilder;
import cn.chineseid.validator.model.BasicIdInfo;
import cn.chineseid.validator.model.IdInfo;
import cn.chineseid.validator.model.IdType;
import cn.chineseid.validator.model.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 集成测试：验证 chinese-id-validator 公共 API 的端到端行为。
 *
 * <p>由 maven-failsafe-plugin 在 integration-test 阶段执行（{@code mvn verify}）。
 * 命名约定 {@code *IT.java} 区别于单元测试 {@code *Test.java}（surefire）。
 *
 * <p>验证流程：
 * <ol>
 *   <li>验证合法 18 位身份证号 → isValid 返回 true
 *   <li>验证非法身份证号 → isValid 返回 false
 *   <li>解析合法身份证号信息 → getInfo 返回完整信息
 *   <li>生成假身份证号并验证 → fakeId 产出合法号码
 *   <li>升级 15 位身份证号为 18 位 → upgradeId 正确升级
 * </ol>
 */
@DisplayName("集成测试：chinese-id-validator 公共 API 端到端验证")
class IdValidationIT {

    /** 北京市东城区合法 18 位身份证号（校验码 = 6） */
    private static final String VALID_18 = "110101199003076536";

    /** 北京市东城区 15 位身份证号（1990-03-07，顺序码 653） */
    private static final String VALID_15 = "110101900307653";

    /** 非法身份证号（校验码错误） */
    private static final String INVALID_18 = "110101199003076534";

    @Test
    @DisplayName("验证合法 18 位身份证号 → 返回 true")
    void shouldValidateValidIdCard() {
        assertTrue(IdValidator.isValid(VALID_18, true), "合法身份证号应通过严格验证");
        assertTrue(IdValidator.isValidBasic(VALID_18), "合法身份证号应通过基础验证");
    }

    @Test
    @DisplayName("验证非法身份证号 → 返回 false")
    void shouldRejectInvalidIdCard() {
        assertFalse(IdValidator.isValid(INVALID_18, true), "校验码错误应不通过");
        assertFalse(IdValidator.isValid("123456789012345678", false), "随机数字应不通过");
        assertFalse(IdValidator.isValid("", false), "空字符串应不通过");
        assertFalse(IdValidator.isValid(null, false), "null 应不通过");
    }

    @Test
    @DisplayName("解析合法身份证号完整信息")
    void shouldParseValidIdInfo() {
        IdInfo info = IdValidator.getInfo(VALID_18, true);

        assertEquals(110101, info.getAddressCode());
        assertTrue(info.getAddress().contains("东城区"), "地址应包含东城区");
        assertEquals(java.time.LocalDate.of(1990, 3, 7), info.getBirthday());
        assertEquals("双鱼座", info.getConstellation());
        assertEquals("午马", info.getChineseZodiac());
        assertEquals(Sex.MALE, info.getSex());
        assertEquals(IdType.EIGHTEEN, info.getLength());
        assertEquals("6", info.getCheckBit());
    }

    @Test
    @DisplayName("解析基础身份证信息")
    void shouldParseBasicInfo() {
        BasicIdInfo info = IdValidator.getBasicInfo(VALID_18);

        assertEquals(java.time.LocalDate.of(1990, 3, 7), info.getBirthday());
        assertEquals("双鱼座", info.getConstellation());
        assertEquals("午马", info.getChineseZodiac());
        assertEquals(Sex.MALE, info.getSex());
    }

    @Test
    @DisplayName("生成随机假身份证号并验证其合法性")
    void shouldGenerateAndValidateFakeId() {
        String fakeId = IdValidator.fakeId();
        assertEquals(18, fakeId.length(), "假号应为 18 位");
        assertTrue(IdValidator.isValid(fakeId, true), "生成的假号应通过严格验证");

        // 验证假号信息可正确解析
        IdInfo fakeInfo = IdValidator.getInfo(fakeId, true);
        assertNotNull(fakeInfo);
    }

    @Test
    @DisplayName("按指定参数生成假身份证号")
    void shouldGenerateCustomizedFakeId() {
        String fakeId = IdValidator.fakeRequireId(new FakeIdBuilder()
                .eighteen()
                .address("北京市")
                .birthday("20000101")
                .sex(Sex.FEMALE));

        assertEquals(18, fakeId.length());
        assertTrue(IdValidator.isValid(fakeId, true));

        IdInfo info = IdValidator.getInfo(fakeId, true);
        assertEquals(Sex.FEMALE, info.getSex());
        assertEquals(java.time.LocalDate.of(2000, 1, 1), info.getBirthday());
    }

    @Test
    @DisplayName("升级 15 位身份证号为 18 位")
    void shouldUpgrade15To18() {
        String upgraded = IdValidator.upgradeId(VALID_15);

        assertEquals(18, upgraded.length());
        assertTrue(IdValidator.isValid(upgraded, true), "升级后的 18 位号码应通过验证");

        // 验证升级后信息一致
        IdInfo info15 = IdValidator.getInfo(VALID_15, true);
        IdInfo info18 = IdValidator.getInfo(upgraded, true);

        assertEquals(info15.getBirthday(), info18.getBirthday());
        assertEquals(info15.getSex(), info18.getSex());
    }
}
