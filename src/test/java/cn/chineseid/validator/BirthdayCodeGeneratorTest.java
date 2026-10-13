package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.core.Validator;
import cn.chineseid.validator.generator.BirthdayCodeGenerator;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 生日码生成器单元测试。
 */
@DisplayName("生日码生成器测试")
class BirthdayCodeGeneratorTest {

    @Test
    @DisplayName("年份大于时间线 endYear 时应收紧到 endYear")
    void shouldClampYearToTimelineEndYear() {
        // 130123 栾城县时间线 [1986, 1992]，指定 2000 年应收紧到 1992
        String result = BirthdayCodeGenerator.generate("130123", "栾城县", "20000101");
        assertTrue(result.startsWith("1992"), "超出时间线 endYear 应收紧: " + result);
        assertTrue(Validator.checkBirthdayCode(result));
    }

    @Test
    @DisplayName("年份小于时间线 startYear 时应提升到 startYear")
    void shouldRaiseYearToTimelineStartYear() {
        String result = BirthdayCodeGenerator.generate("130123", "栾城县", "19800101");
        assertTrue(result.startsWith("1986"), "低于时间线 startYear 应提升: " + result);
    }

    @Test
    @DisplayName("年份大于当前年时应替换为随机年份")
    void shouldReplaceFutureYear() {
        String result = BirthdayCodeGenerator.generate("110101", "东城区", "29990101");
        int year = Integer.parseInt(result.substring(0, 4));
        assertTrue(year >= 1950 && year <= Year.now().getValue(), "未来年份应替换为 [1950, 当前年]: " + result);
    }

    @Test
    @DisplayName("月份超出 [1, 12] 时应替换为随机月份")
    void shouldReplaceOutOfRangeMonth() {
        String high = BirthdayCodeGenerator.generate("110101", "东城区", "20001301");
        int highMonth = Integer.parseInt(high.substring(4, 6));
        assertTrue(highMonth >= 1 && highMonth <= 12, "月份 13 应替换为合法月份: " + high);

        String low = BirthdayCodeGenerator.generate("110101", "东城区", "20000001");
        int lowMonth = Integer.parseInt(low.substring(4, 6));
        assertTrue(lowMonth >= 1 && lowMonth <= 12, "月份 0 应替换为合法月份: " + low);
    }

    @Test
    @DisplayName("日期超出 [1, 31] 时应替换为随机日期")
    void shouldReplaceOutOfRangeDay() {
        String high = BirthdayCodeGenerator.generate("110101", "东城区", "20000132");
        int highDay = Integer.parseInt(high.substring(6, 8));
        assertTrue(highDay >= 1 && highDay <= 31, "日期 32 应替换为合法日期: " + high);

        String low = BirthdayCodeGenerator.generate("110101", "东城区", "20000100");
        int lowDay = Integer.parseInt(low.substring(6, 8));
        assertTrue(lowDay >= 1 && lowDay <= 31, "日期 0 应替换为合法日期: " + low);
    }

    @Test
    @DisplayName("birthday 为 null 时应生成完整随机生日码")
    void shouldGenerateRandomBirthdayForNull() {
        String result = BirthdayCodeGenerator.generate("110101", "东城区", null);
        assertEquals(8, result.length());
        assertTrue(Validator.checkBirthdayCode(result), "随机生日码应为合法日期: " + result);
    }

    @Test
    @DisplayName("批量随机生成：生日均不晚于中国今天（生成物自身可通过校验）")
    void shouldNeverGenerateFutureBirthday() {
        // 随机填充可能组合出当前年内的未来月份（如当前年随机到 12 月），
        // 未来生日将被 Validator.checkBirthdayCode 拒绝——批量验证该回归不复发
        for (int i = 0; i < 200; i++) {
            String result = BirthdayCodeGenerator.generate("110101", "东城区", null);
            assertTrue(Validator.checkBirthdayCode(result), "生成生日不得晚于中国今天: " + result);
        }
    }

    @Test
    @DisplayName("显式未来生日：年份回退后可通过校验（与超范围替换设计一致）")
    void shouldFallbackYearForExplicitFutureBirthday() {
        String tomorrow =
                LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).format(DateTimeFormatter.ofPattern("uuuuMMdd"));
        String result = BirthdayCodeGenerator.generate("110101", "东城区", tomorrow);
        assertTrue(Validator.checkBirthdayCode(result), "未来生日回退后应可通过校验: " + result);
    }
}
