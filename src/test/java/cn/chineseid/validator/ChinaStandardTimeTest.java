package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.core.Validator;
import cn.chineseid.validator.data.CustomDataSource;
import cn.chineseid.validator.data.MergeMode;
import cn.chineseid.validator.model.TimelineEntry;
import cn.chineseid.validator.util.AddressResolver;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 中国标准时间（Asia/Shanghai）业务日期基准测试。
 *
 * <p>覆盖三类边界：
 * <ol>
 *   <li>出生日期边界：中国今天/明天出生、闰日
 *   <li>时区独立性：JVM 默认时区为美国时，仍按中国日期/年份判断，
 *       即不同 JVM 默认时区下同一瞬间得到相同的校验结果
 *   <li>环境隔离：校验不修改 JVM 默认时区
 * </ol>
 *
 * <p>修改 JVM 默认时区的用例在 finally 中恢复原值，避免影响其他测试。
 */
@DisplayName("中国标准时间业务基准")
class ChinaStandardTimeTest {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneId US_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("uuuuMMdd");

    @AfterEach
    void cleanup() {
        TestStateResetter.resetDataProvider();
    }

    private static String todayInChina() {
        return LocalDate.now(CHINA_ZONE).format(FORMATTER);
    }

    private static String tomorrowInChina() {
        return LocalDate.now(CHINA_ZONE).plusDays(1).format(FORMATTER);
    }

    // ==================== 出生日期边界 ====================

    @Test
    @DisplayName("中国今天出生：生日规则通过")
    void bornTodayInChinaShouldPass() {
        assertTrue(Validator.checkBirthdayCode(todayInChina()), "中国今天出生应通过");
    }

    @Test
    @DisplayName("中国明天出生：生日规则不通过（当年内未来日期被拒绝）")
    void bornTomorrowInChinaShouldFail() {
        assertFalse(Validator.checkBirthdayCode(tomorrowInChina()), "中国明天出生应被拒绝");
    }

    @Test
    @DisplayName("闰日校验：非法闰日不通过，合法闰日通过")
    void leapDayShouldBeValidatedStrictly() {
        assertFalse(Validator.checkBirthdayCode("20230229"), "2023 年非闰年，2 月 29 日不存在");
        assertTrue(Validator.checkBirthdayCode("20240229"), "2024 年为闰年，2 月 29 日合法");
    }

    // ==================== 时区独立性 ====================

    @Test
    @DisplayName("JVM 默认时区为美国时，未来生日判断仍按中国日期")
    void shouldJudgeFutureBirthdayByChinaDateRegardlessOfJvmZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(US_ZONE));

            assertTrue(Validator.checkBirthdayCode(todayInChina()), "美国时区下中国今天出生仍应通过");
            assertFalse(Validator.checkBirthdayCode(tomorrowInChina()), "美国时区下中国明天出生仍应拒绝");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    @DisplayName("JVM 默认时区为美国时，行政区划时间线上界仍按中国当前年份")
    void shouldUseChinaYearForTimelineUpperBoundRegardlessOfJvmZone() {
        int thisYearInChina = LocalDate.now(CHINA_ZONE).getYear();
        Map<Integer, List<TimelineEntry>> timelines = new HashMap<>();
        // 110998/110999 均不在内置数据（时间线与映射表均无），MERGE 合并结果仅含自定义条目
        timelines.put(
                110999,
                Collections.singletonList(
                        new TimelineEntry("测今生区", String.valueOf(thisYearInChina), String.valueOf(thisYearInChina))));
        timelines.put(
                110998,
                Collections.singletonList(new TimelineEntry(
                        "测未来区", String.valueOf(thisYearInChina + 1), String.valueOf(thisYearInChina + 1))));
        IdValidator.initialize(new MapTimelineSource(timelines), MergeMode.MERGE);

        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(US_ZONE));

            assertTrue(AddressResolver.isAddressCodeValid(110999), "时间线终于中国当前年应有效");
            assertFalse(AddressResolver.isAddressCodeValid(110998), "时间线始于中国未来年应无效");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    // ==================== 环境隔离 ====================

    @Test
    @DisplayName("完整校验不修改 JVM 默认时区")
    void shouldNotMutateJvmDefaultTimeZone() {
        TimeZone before = TimeZone.getDefault();

        // 触发完整校验链（生日判断 + 地址码时间线当前年份判断）
        String id = IdValidator.fakeId();
        IdValidator.isValid(id, true);
        Validator.checkBirthdayCode(todayInChina());
        AddressResolver.isAddressCodeValid(110101);

        assertEquals(before, TimeZone.getDefault(), "校验不应修改 JVM 默认时区");
    }

    /** 极简 Map 时间线数据源：仅提供时间线，地址名恒为 null */
    private static final class MapTimelineSource implements CustomDataSource {
        private final Map<Integer, List<TimelineEntry>> timelines;

        MapTimelineSource(Map<Integer, List<TimelineEntry>> timelines) {
            this.timelines = timelines;
        }

        @Override
        public List<TimelineEntry> getTimeline(int code) {
            return timelines.getOrDefault(code, Collections.emptyList());
        }

        @Override
        public String getAddressName(int code) {
            return null;
        }
    }
}
