package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.model.TimelineEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 时间线条目模型单元测试。
 */
@DisplayName("时间线条目测试")
class TimelineEntryTest {

    @Test
    @DisplayName("构造器对 null 年份自动转为空字符串")
    void shouldConvertNullYearsToEmpty() {
        TimelineEntry entry = new TimelineEntry("北京市", null, null);
        assertEquals("北京市", entry.getAddress());
        assertEquals("", entry.getStartYear());
        assertEquals("", entry.getEndYear());
    }

    @Test
    @DisplayName("coversYear 在年份区间内返回 true")
    void shouldCoverYearInRange() {
        TimelineEntry entry = new TimelineEntry("北京市", "1980", "2000");
        assertTrue(entry.coversYear(1980));
        assertTrue(entry.coversYear(1990));
        assertTrue(entry.coversYear(2000));
    }

    @Test
    @DisplayName("coversYear 在年份区间外返回 false")
    void shouldNotCoverYearOutOfRange() {
        TimelineEntry entry = new TimelineEntry("北京市", "1980", "2000");
        assertFalse(entry.coversYear(1979));
        assertFalse(entry.coversYear(2001));
    }

    @Test
    @DisplayName("coversYear 空起始年份表示无下界")
    void shouldHaveNoLowerBoundWhenStartYearEmpty() {
        TimelineEntry entry = new TimelineEntry("某地", "", "2000");
        assertTrue(entry.coversYear(1));
        assertTrue(entry.coversYear(2000));
        assertFalse(entry.coversYear(2001));
    }

    @Test
    @DisplayName("coversYear 空结束年份表示无上界")
    void shouldHaveNoUpperBoundWhenEndYearEmpty() {
        TimelineEntry entry = new TimelineEntry("某地", "2000", "");
        assertFalse(entry.coversYear(1999));
        assertTrue(entry.coversYear(2000));
        assertTrue(entry.coversYear(9999));
    }

    @Test
    @DisplayName("coversYear 未曾使用作为起始年份时取默认下界 1，公元 0 年不被覆盖")
    void shouldHandleNeverUsedStartYear() {
        TimelineEntry entry = new TimelineEntry("某地", "未曾使用", "");
        assertFalse(entry.coversYear(0));
        assertTrue(entry.coversYear(1));
        assertTrue(entry.coversYear(2024));
    }

    @Test
    @DisplayName("coversYear 非数字年份字符串取默认边界")
    void shouldHandleNonNumericYear() {
        TimelineEntry entry = new TimelineEntry("某地", "abc", "xyz");
        assertTrue(entry.coversYear(100));
    }

    @Test
    @DisplayName("toString 返回包含所有字段的字符串")
    void shouldReturnToStringWithAllFields() {
        TimelineEntry entry = new TimelineEntry("北京市", "1980", "2000");
        String str = entry.toString();
        assertTrue(str.contains("北京市"));
        assertTrue(str.contains("1980"));
        assertTrue(str.contains("2000"));
    }

    @Test
    @DisplayName("equals 相同字段返回 true 且 hashCode 一致")
    void shouldBeEqualWithSameFields() {
        TimelineEntry e1 = new TimelineEntry("北京市", "1980", "2000");
        TimelineEntry e2 = new TimelineEntry("北京市", "1980", "2000");
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    @DisplayName("equals 自反性与自身比较返回 true")
    void shouldBeEqualWithSelf() {
        TimelineEntry entry = new TimelineEntry("北京市", "1980", "2000");
        assertEquals(entry, entry);
    }

    @Test
    @DisplayName("equals 非 TimelineEntry 类型及 null 返回 false")
    void shouldNotEqualDifferentType() {
        TimelineEntry entry = new TimelineEntry("北京市", "1980", "2000");
        assertNotEquals(entry, "北京市");
        assertNotEquals(entry, null);
    }

    @Test
    @DisplayName("equals 任一字段不同返回 false")
    void shouldNotEqualWithDifferentFields() {
        TimelineEntry base = new TimelineEntry("北京市", "1980", "2000");
        assertNotEquals(base, new TimelineEntry("上海市", "1980", "2000"));
        assertNotEquals(base, new TimelineEntry("北京市", "1990", "2000"));
        assertNotEquals(base, new TimelineEntry("北京市", "1980", "2010"));
    }

    @Test
    @DisplayName("getStartYearInt/getEndYearInt 解析数字年份")
    void shouldParseNumericYearToInt() {
        TimelineEntry entry = new TimelineEntry("某地", "1980", "2000");
        assertEquals(1980, entry.getStartYearInt());
        assertEquals(2000, entry.getEndYearInt());
    }

    @Test
    @DisplayName("空年份：getStartYearInt 回退 1，getEndYearInt 回退 9999")
    void shouldFallbackEmptyYearToBoundary() {
        TimelineEntry entry = new TimelineEntry("某地", "", "");
        assertEquals(1, entry.getStartYearInt());
        assertEquals(9999, entry.getEndYearInt());
    }

    @Test
    @DisplayName("未曾使用：getStartYearInt 回退 1，getEndYearInt 回退 9999")
    void shouldFallbackNeverUsedToBoundary() {
        TimelineEntry entry = new TimelineEntry("某地", "未曾使用", "未曾使用");
        assertEquals(1, entry.getStartYearInt());
        assertEquals(9999, entry.getEndYearInt());
    }

    @Test
    @DisplayName("非数字年份：getStartYearInt 回退 1，getEndYearInt 回退 9999")
    void shouldFallbackNonNumericToBoundary() {
        TimelineEntry entry = new TimelineEntry("某地", "abc", "xyz");
        assertEquals(1, entry.getStartYearInt());
        assertEquals(9999, entry.getEndYearInt());
    }

    @Test
    @DisplayName("null 年份（构造器转空串）：getStartYearInt 回退 1，getEndYearInt 回退 9999")
    void shouldFallbackNullToBoundary() {
        TimelineEntry entry = new TimelineEntry("某地", null, null);
        assertEquals(1, entry.getStartYearInt());
        assertEquals(9999, entry.getEndYearInt());
    }

    @Test
    @DisplayName("getStartYearInt 永不小于 1：coversYear(0) 必为 false")
    void startYearIntNeverBelowOne() {
        TimelineEntry entry = new TimelineEntry("某地", "", "");
        assertTrue(entry.getStartYearInt() >= 1);
        assertFalse(entry.coversYear(0));
    }
}
