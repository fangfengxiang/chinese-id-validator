package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.data.DataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 数据提供者单元测试。
 */
@DisplayName("数据提供者测试")
class DataProviderTest {

    @Test
    @DisplayName("getChineseZodiac 合法索引返回生肖")
    void shouldReturnZodiacForValidIndex() {
        assertEquals("子鼠", DataProvider.getInstance().getChineseZodiac(0));
        assertEquals("亥猪", DataProvider.getInstance().getChineseZodiac(11));
    }

    @Test
    @DisplayName("getChineseZodiac 负数索引返回空字符串")
    void shouldReturnEmptyForNegativeIndex() {
        assertEquals("", DataProvider.getInstance().getChineseZodiac(-1));
    }

    @Test
    @DisplayName("getChineseZodiac 越界索引返回空字符串")
    void shouldReturnEmptyForOutOfRangeIndex() {
        assertEquals("", DataProvider.getInstance().getChineseZodiac(12));
    }
}
