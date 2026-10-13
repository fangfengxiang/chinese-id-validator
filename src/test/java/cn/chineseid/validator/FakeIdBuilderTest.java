package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.generator.FakeIdBuilder;
import cn.chineseid.validator.model.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 假身份证号生成参数构建器单元测试。
 */
@DisplayName("假号生成参数构建器测试")
class FakeIdBuilderTest {

    @Test
    @DisplayName("address(null) 应归一化为空字符串")
    void shouldNormalizeNullAddressToEmpty() {
        FakeIdBuilder builder = new FakeIdBuilder().address(null);
        assertEquals("", builder.getAddress());
    }

    @Test
    @DisplayName("birthday(null) 应归一化为空字符串")
    void shouldNormalizeNullBirthdayToEmpty() {
        FakeIdBuilder builder = new FakeIdBuilder().birthday(null);
        assertEquals("", builder.getBirthday());
    }

    @Test
    @DisplayName("sex(null) 应回退为 FEMALE")
    void shouldFallbackToFemaleForNullSex() {
        FakeIdBuilder builder = new FakeIdBuilder().sex(null);
        assertEquals(Sex.FEMALE, builder.getSex());
    }

    @Test
    @DisplayName("默认参数为 18 位 + 全国随机 + FEMALE")
    void shouldHaveExpectedDefaults() {
        FakeIdBuilder builder = new FakeIdBuilder();
        assertTrue(builder.isEighteen());
        assertEquals("", builder.getAddress());
        assertEquals("", builder.getBirthday());
        assertEquals(Sex.FEMALE, builder.getSex());
    }

    @Test
    @DisplayName("链式调用应生效")
    void shouldSupportChainedCalls() {
        FakeIdBuilder builder =
                new FakeIdBuilder().fifteen().address("江苏省").birthday("200001").sex(Sex.MALE);
        assertFalse(builder.isEighteen());
        assertEquals("江苏省", builder.getAddress());
        assertEquals("200001", builder.getBirthday());
        assertEquals(Sex.MALE, builder.getSex());
    }
}
