package cn.chineseid.it;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.IdValidator;
import cn.chineseid.validator.generator.FakeIdBuilder;
import cn.chineseid.validator.model.IdInfo;
import cn.chineseid.validator.model.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * 功能测试。
 *
 * <p>验证所有核心功能：假号生成→校验→信息解析→升级。
 *
 * <p>由 maven-failsafe-plugin 在 integration-test 阶段执行（{@code mvn verify}）。
 * 命名约定 {@code *IT.java} 区别于单元测试 {@code *Test.java}（surefire）。
 * 属于功能测试层（端到端核心流程），与全量数据验收 {@link AddressCodeAcceptanceIT} 同处 failsafe 层。
 */
@DisplayName("功能测试")
class FeatureIT {

    @Test
    @DisplayName("FakeId 生成的身份证号应通过宽松校验")
    void fakeIdShouldBeValidLoose() {
        String fakeId = IdValidator.fakeId();
        assertTrue(IdValidator.isValid(fakeId, false), "FakeId() 生成的身份证号必须通过宽松校验: " + fakeId);
    }

    @Test
    @DisplayName("FakeRequireId 生成的身份证号应通过严格校验")
    void fakeRequireIdShouldBeValidStrict() {
        String fakeId = IdValidator.fakeRequireId(
                new FakeIdBuilder().eighteen().address("江苏省").birthday("200001").sex(Sex.MALE));
        assertTrue(
                IdValidator.isValid(fakeId, true), "FakeRequireId(true, 江苏省, 200001, MALE) 生成的身份证号必须通过严格校验: " + fakeId);
    }

    @Test
    @DisplayName("GetInfo 应成功解析 FakeRequireId 生成的身份证号")
    void getInfoShouldNotThrowForFakeRequireId() {
        String fakeId = IdValidator.fakeRequireId(
                new FakeIdBuilder().eighteen().address("江苏省").birthday("200001").sex(Sex.MALE));
        IdInfo info = assertDoesNotThrow(() -> IdValidator.getInfo(fakeId, true), "GetInfo 不应抛出异常: " + fakeId);
        assertNotNull(info);
        assertEquals(18, info.getLength().getLength());
        assertEquals(Sex.MALE, info.getSex());
    }

    @Test
    @DisplayName("UpgradeId 应将 15 位身份证号升级为 18 位")
    void upgradeIdShouldReturn18Digits() {
        String upgraded = IdValidator.upgradeId("610104620927690");
        assertEquals(18, upgraded.length(), "升级后的身份证号长度必须为 18 位: " + upgraded);
    }

    @Test
    @DisplayName("升级后的 18 位身份证号应通过严格校验")
    void upgradedIdShouldBeValid() {
        String upgraded = IdValidator.upgradeId("610104620927690");
        assertTrue(IdValidator.isValid(upgraded, true), "升级后的身份证号必须通过严格校验: " + upgraded);
    }

    // ==================== 循环测试 ====================

    @RepeatedTest(value = 100, name = "循环 {currentRepetition}/100: FakeId→IsValid")
    @DisplayName("100 次循环：FakeId 生成的身份证号均应通过宽松校验")
    void fakeIdShouldAlwaysBeValid() {
        String fakeId = IdValidator.fakeId();
        assertTrue(IdValidator.isValid(fakeId, false), "第 N 次 FakeId() 生成的身份证号必须通过宽松校验: " + fakeId);
    }

    @RepeatedTest(value = 100, name = "循环 {currentRepetition}/100: FakeRequireId→IsValid(严格)")
    @DisplayName("100 次循环：FakeRequireId 生成的身份证号均应通过严格校验")
    void fakeRequireIdShouldAlwaysBeValidStrict() {
        String fakeId = IdValidator.fakeRequireId(
                new FakeIdBuilder().eighteen().address("江苏省").birthday("200001").sex(Sex.MALE));
        assertTrue(IdValidator.isValid(fakeId, true), "FakeRequireId 生成的身份证号必须通过严格校验: " + fakeId);
    }

    @RepeatedTest(value = 100, name = "循环 {currentRepetition}/100: GetInfo 无异常")
    @DisplayName("100 次循环：GetInfo 解析 FakeRequireId 不应抛出异常")
    void getInfoShouldNeverThrow() {
        String fakeId = IdValidator.fakeRequireId(
                new FakeIdBuilder().eighteen().address("江苏省").birthday("200001").sex(Sex.MALE));
        assertDoesNotThrow(() -> IdValidator.getInfo(fakeId, true), "GetInfo 不应抛出异常: " + fakeId);
    }
}
