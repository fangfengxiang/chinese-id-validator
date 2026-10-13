package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.generator.FakeIdBuilder;
import cn.chineseid.validator.generator.IdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 假身份证号生成器单元测试。
 *
 * <p>15 位身份证号年份约束 [1900, 1999]：超出范围的指定年份
 * 应被替换为 [1950, 1999] 内的随机年份。
 */
@DisplayName("假号生成器测试")
class IdGeneratorTest {

    @Test
    @DisplayName("15 位：年份在 [1900, 1999] 内时保留原年份")
    void shouldKeepYearWithinFifteenBitRange() {
        String id = IdGenerator.fakeRequireId(
                new FakeIdBuilder().fifteen().address("东城区").birthday("19880101"));
        assertEquals(15, id.length());
        assertEquals("88", id.substring(6, 8), "年份在范围内不应被替换");
        assertTrue(IdValidator.isValid(id, false));
    }

    @Test
    @DisplayName("15 位：年份大于 1999 时替换为随机年份")
    void shouldReplaceYearAfter1999() {
        String id = IdGenerator.fakeRequireId(
                new FakeIdBuilder().fifteen().address("东城区").birthday("20000101"));
        assertEquals(15, id.length());
        assertEquals("0101", id.substring(8, 12), "年份替换后月日应保持不变");
        assertTrue(IdValidator.isValid(id, false));
    }

    @Test
    @DisplayName("15 位：年份小于 1900 时替换为随机年份")
    void shouldReplaceYearBefore1900() {
        String id = IdGenerator.fakeRequireId(
                new FakeIdBuilder().fifteen().address("东城区").birthday("18500101"));
        assertEquals(15, id.length());
        assertEquals("0101", id.substring(8, 12), "年份替换后月日应保持不变");
        assertTrue(IdValidator.isValid(id, false));
    }
}
