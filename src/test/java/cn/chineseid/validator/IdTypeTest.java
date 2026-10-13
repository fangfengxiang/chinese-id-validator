package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.model.IdType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 身份证长度类型枚举单元测试。
 */
@DisplayName("身份证长度类型枚举测试")
class IdTypeTest {

    @Test
    @DisplayName("fromLength(18) 返回 EIGHTEEN")
    void shouldReturnEighteenForLength18() {
        assertEquals(IdType.EIGHTEEN, IdType.fromLength(18));
        assertEquals(18, IdType.fromLength(18).getLength());
    }

    @Test
    @DisplayName("fromLength(15) 返回 FIFTEEN")
    void shouldReturnFifteenForLength15() {
        assertEquals(IdType.FIFTEEN, IdType.fromLength(15));
        assertEquals(15, IdType.fromLength(15).getLength());
    }
}
