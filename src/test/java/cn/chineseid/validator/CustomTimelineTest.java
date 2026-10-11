package cn.chineseid.validator;

import cn.chineseid.validator.data.CustomDataSource;
import cn.chineseid.validator.data.MergeMode;
import cn.chineseid.validator.model.IdInfo;
import cn.chineseid.validator.model.TimelineEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 自定义数据源单元测试。
 *
 * 验证 {@link CustomDataSource} SPI 两种合并策略的行为：
 * <ul>
 *   <li>{@link MergeMode#FALLBACK} — 内置有数据则不查自定义源
 *   <li>{@link MergeMode#MERGE} — 总是合并，自定义优先
 * </ul>
 */
@DisplayName("自定义数据源测试")
class CustomTimelineTest {

    @AfterEach
    void cleanup() {
        IdValidator.clearCustomDataSource();
    }

    // ==================== 辅助：构造合法身份证号 ====================

    private static String buildValidId(int addressCode, String birthday) {
        String body = String.format("%06d", addressCode) + birthday + "123";
        String checkBit = cn.chineseid.validator.core.CheckBitCalculator.calculate(body);
        return body + checkBit;
    }

    // ==================== FALLBACK 模式测试 ====================

    @Test
    @DisplayName("FALLBACK 模式：内置有数据时不查自定义源")
    void shouldNotCallCustomWhenBuiltinHasDataInFallbackMode() {
        // 110101（东城区）内置有数据，自定义源提供不同的地址名
        // FALLBACK 模式下应返回内置数据，不使用自定义
        CustomDataSource source = new TestDataSource(
                MergeMode.FALLBACK,
                Map.of(110101, "自定义东城区"),
                Collections.emptyMap()
        );
        IdValidator.setCustomDataSource(source);

        String id = buildValidId(110101, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("东城区"), "FALLBACK 模式应使用内置数据");
        assertFalse(info.getAddress().contains("自定义"), "不应包含自定义地址名");
    }

    @Test
    @DisplayName("FALLBACK 模式：内置无数据时使用自定义源")
    void shouldUseCustomWhenBuiltinMissingInFallbackMode() {
        // 110999 不在官方数据中，自定义源补充
        CustomDataSource source = new TestDataSource(
                MergeMode.FALLBACK,
                Collections.emptyMap(),
                Map.of(110999, List.of(new TimelineEntry("测试区", "1990", "2020")))
        );
        IdValidator.setCustomDataSource(source);

        String id = buildValidId(110999, "19950101");
        IdInfo info = IdValidator.getInfo(id, false);

        assertEquals(110999, info.getAddressCode());
        assertTrue(info.getAddress().contains("测试区"), "内置缺失时应使用自定义时间线");
    }

    // ==================== MERGE 模式测试 ====================

    @Test
    @DisplayName("MERGE 模式：内置有数据时也查自定义源，自定义优先")
    void shouldAlwaysCallCustomInMergeMode() {
        // 110101 内置有"东城区"，自定义源提供"自定义东城区"
        // MERGE 模式下自定义条目排在前面优先匹配
        CustomDataSource source = new TestDataSource(
                MergeMode.MERGE,
                Map.of(110101, "自定义东城区"),
                Map.of(110101, List.of(new TimelineEntry("自定义东城区", "1980", "2020")))
        );
        IdValidator.setCustomDataSource(source);

        String id = buildValidId(110101, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("自定义东城区"), "MERGE 模式应使用自定义地址");
    }

    @Test
    @DisplayName("MERGE 模式：自定义无数据时回退内置")
    void shouldFallbackToBuiltinWhenCustomEmptyInMergeMode() {
        // 110101 内置有数据，自定义源返回空
        CustomDataSource source = new TestDataSource(
                MergeMode.MERGE,
                Collections.emptyMap(),
                Collections.emptyMap()
        );
        IdValidator.setCustomDataSource(source);

        String id = buildValidId(110101, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("东城区"), "自定义为空时应回退内置数据");
    }

    @Test
    @DisplayName("MERGE 模式：内置无数据时仅用自定义")
    void shouldUseCustomOnlyWhenBuiltinMissingInMergeMode() {
        // 110999 不在官方数据中
        CustomDataSource source = new TestDataSource(
                MergeMode.MERGE,
                Collections.emptyMap(),
                Map.of(110999, List.of(new TimelineEntry("测试区", "1990", "2020")))
        );
        IdValidator.setCustomDataSource(source);

        String id = buildValidId(110999, "19950101");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("测试区"), "内置缺失时应使用自定义数据");
    }

    // ==================== 默认模式 + 清除测试 ====================

    @Test
    @DisplayName("默认模式为 FALLBACK（不 override getMergeMode）")
    void defaultModeShouldBeFallback() {
        // 不 override getMergeMode()，默认 FALLBACK
        // 内置 110101 有数据，FALLBACK 下不查自定义，应返回内置
        CustomDataSource source = new TestDataSource(
                null, // null = 不 override，用默认 FALLBACK
                Map.of(110101, "自定义东城区"),
                Map.of(110101, List.of(new TimelineEntry("自定义东城区", "1980", "2020")))
        );
        IdValidator.setCustomDataSource(source);

        String id = buildValidId(110101, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("东城区"), "默认 FALLBACK 模式应使用内置数据");
        assertFalse(info.getAddress().contains("自定义"), "默认 FALLBACK 模式不应使用自定义数据");
    }

    @Test
    @DisplayName("clearCustomDataSource 后恢复仅内置数据")
    void shouldRestoreAfterClear() {
        CustomDataSource source = new TestDataSource(
                MergeMode.MERGE,
                Map.of(110101, "自定义东城区"),
                Map.of(110101, List.of(new TimelineEntry("自定义东城区", "1980", "2020")))
        );
        IdValidator.setCustomDataSource(source);
        IdValidator.clearCustomDataSource();

        String id = buildValidId(110101, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertFalse(info.getAddress().contains("自定义"), "清除后不应包含自定义数据");
        assertTrue(info.getAddress().contains("东城区"), "清除后应恢复内置数据");
    }

    @Test
    @DisplayName("setCustomDataSource(null) 等同 clearCustomDataSource")
    void nullShouldActAsClear() {
        IdValidator.setCustomDataSource(null);

        String id = buildValidId(110101, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("东城区"), "null 注入应恢复为内置数据");
    }

    // ==================== 测试用 DataSource 实现 ====================

    /**
     * 测试用 CustomDataSource 实现。
     *
     * @param mode null 表示不 override（用默认 FALLBACK）
     */
    private static final class TestDataSource implements CustomDataSource {

        private final MergeMode mode;
        private final Map<Integer, String> addressNames;
        private final Map<Integer, List<TimelineEntry>> timelines;

        TestDataSource(MergeMode mode,
                       Map<Integer, String> addressNames,
                       Map<Integer, List<TimelineEntry>> timelines) {
            this.mode = mode;
            this.addressNames = (addressNames == null) ? Collections.emptyMap() : addressNames;
            this.timelines = (timelines == null) ? Collections.emptyMap() : timelines;
        }

        @Override
        public MergeMode getMergeMode() {
            return (mode != null) ? mode : CustomDataSource.super.getMergeMode();
        }

        @Override
        public List<TimelineEntry> getTimeline(int code) {
            return timelines.getOrDefault(code, Collections.emptyList());
        }

        @Override
        public String getAddressName(int code) {
            return addressNames.get(code);
        }
    }
}
