package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.model.BasicIdInfo;
import cn.chineseid.validator.model.IdType;
import cn.chineseid.validator.model.Sex;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 基础身份证信息 DTO 单元测试。
 */
@DisplayName("基础身份证信息测试")
class BasicIdInfoTest {

    @Test
    @DisplayName("checkBit 为 null 时应归一化为空字符串")
    void shouldNormalizeNullCheckBitToEmpty() {
        BasicIdInfo info = new BasicIdInfo(LocalDate.of(1962, 9, 27), "天秤座", "寅虎", Sex.FEMALE, IdType.FIFTEEN, null);
        assertEquals("", info.getCheckBit());
    }

    @Test
    @DisplayName("checkBit 非 null 时应原样保留")
    void shouldKeepNonNullCheckBit() {
        BasicIdInfo info = new BasicIdInfo(LocalDate.of(1990, 3, 7), "双鱼座", "午马", Sex.MALE, IdType.EIGHTEEN, "6");
        assertEquals("6", info.getCheckBit());
        assertEquals(Sex.MALE, info.getSex());
        assertEquals(IdType.EIGHTEEN, info.getLength());
    }
}
