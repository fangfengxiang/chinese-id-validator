package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.generator.OrderCodeGenerator;
import cn.chineseid.validator.model.Sex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 顺序码生成器单元测试。
 */
@DisplayName("顺序码生成器测试")
class OrderCodeGeneratorTest {

    @Test
    @DisplayName("sex 为 null 应抛出 IllegalArgumentException")
    void shouldThrowForNullSex() {
        assertThrows(IllegalArgumentException.class, () -> OrderCodeGenerator.generate(null));
    }

    @Test
    @DisplayName("MALE 生成的顺序码应为奇数")
    void shouldGenerateOddOrderCodeForMale() {
        for (int i = 0; i < 50; i++) {
            String orderCode = OrderCodeGenerator.generate(Sex.MALE);
            assertEquals(3, orderCode.length());
            assertEquals(1, Integer.parseInt(orderCode) % 2, "男性顺序码应为奇数");
        }
    }

    @Test
    @DisplayName("FEMALE 生成的顺序码应为偶数")
    void shouldGenerateEvenOrderCodeForFemale() {
        for (int i = 0; i < 50; i++) {
            String orderCode = OrderCodeGenerator.generate(Sex.FEMALE);
            assertEquals(3, orderCode.length());
            assertEquals(0, Integer.parseInt(orderCode) % 2, "女性顺序码应为偶数");
        }
    }
}
