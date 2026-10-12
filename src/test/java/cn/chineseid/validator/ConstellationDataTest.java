package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.model.ConstellationData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 星座数据模型单元测试。
 */
@DisplayName("星座数据模型测试")
class ConstellationDataTest {

    @Test
    @DisplayName("构造器对 null 参数抛出 NullPointerException")
    void shouldThrowNpeForNullArgs() {
        assertThrows(NullPointerException.class, () -> new ConstellationData(null, "01-20", "02-18"));
        assertThrows(NullPointerException.class, () -> new ConstellationData("水瓶座", null, "02-18"));
        assertThrows(NullPointerException.class, () -> new ConstellationData("水瓶座", "01-20", null));
    }

    @Test
    @DisplayName("getter 返回构造时传入的值")
    void shouldReturnConstructorValues() {
        ConstellationData data = new ConstellationData("水瓶座", "01-20", "02-18");
        assertEquals("水瓶座", data.getName());
        assertEquals("01-20", data.getStartDate());
        assertEquals("02-18", data.getEndDate());
    }

    @Test
    @DisplayName("getStartMonth 从 startDate 解析月份")
    void shouldParseStartMonth() {
        ConstellationData data = new ConstellationData("水瓶座", "01-20", "02-18");
        assertEquals(1, data.getStartMonth());
    }

    @Test
    @DisplayName("getStartDay 从 startDate 解析日期")
    void shouldParseStartDay() {
        ConstellationData data = new ConstellationData("水瓶座", "01-20", "02-18");
        assertEquals(20, data.getStartDay());
    }

    @Test
    @DisplayName("getStartMonth 和 getStartDay 对两位数月份正确解析")
    void shouldParseTwoDigitMonth() {
        ConstellationData data = new ConstellationData("天蝎座", "10-24", "11-22");
        assertEquals(10, data.getStartMonth());
        assertEquals(24, data.getStartDay());
    }

    @Test
    @DisplayName("toString 返回包含所有字段的字符串")
    void shouldReturnToStringWithAllFields() {
        ConstellationData data = new ConstellationData("水瓶座", "01-20", "02-18");
        String str = data.toString();
        assertTrue(str.contains("水瓶座"));
        assertTrue(str.contains("01-20"));
        assertTrue(str.contains("02-18"));
    }
}
