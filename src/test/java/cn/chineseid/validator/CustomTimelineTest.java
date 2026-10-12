package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.data.CustomDataSource;
import cn.chineseid.validator.data.MergeMode;
import cn.chineseid.validator.model.IdInfo;
import cn.chineseid.validator.model.TimelineEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 自定义数据源单元测试。
 *
 * 验证 {@link CustomDataSource} SPI 两种合并策略的行为：
 * <ul>
 *   <li>{@link MergeMode#FALLBACK} — 内置有数据则不查自定义源
 *   <li>{@link MergeMode#MERGE} — 总是合并，自定义优先
 * </ul>
 *
 * <p>每个测试方法调用 {@link IdValidator#initialize} 安装自定义数据源，
 * {@code @AfterEach} 通过 {@link TestStateResetter} 重置状态以支持下一次初始化。
 */
@DisplayName("自定义数据源测试")
class CustomTimelineTest {

    @AfterEach
    void cleanup() {
        TestStateResetter.resetDataProvider();
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
        CustomDataSource source =
                new TestDataSource(Collections.singletonMap(110101, "自定义东城区"), Collections.emptyMap());
        IdValidator.initialize(source, MergeMode.FALLBACK);

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
                Collections.emptyMap(),
                Collections.singletonMap(110999, Collections.singletonList(new TimelineEntry("测试区", "1990", "2020"))));
        IdValidator.initialize(source, MergeMode.FALLBACK);

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
                Collections.singletonMap(110101, "自定义东城区"),
                Collections.singletonMap(
                        110101, Collections.singletonList(new TimelineEntry("自定义东城区", "1980", "2020"))));
        IdValidator.initialize(source, MergeMode.MERGE);

        String id = buildValidId(110101, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("自定义东城区"), "MERGE 模式应使用自定义地址");
    }

    @Test
    @DisplayName("MERGE 模式：自定义无数据时回退内置")
    void shouldFallbackToBuiltinWhenCustomEmptyInMergeMode() {
        // 110101 内置有数据，自定义源返回空
        CustomDataSource source = new TestDataSource(Collections.emptyMap(), Collections.emptyMap());
        IdValidator.initialize(source, MergeMode.MERGE);

        String id = buildValidId(110101, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("东城区"), "自定义为空时应回退内置数据");
    }

    @Test
    @DisplayName("MERGE 模式：内置无数据时仅用自定义")
    void shouldUseCustomOnlyWhenBuiltinMissingInMergeMode() {
        // 110999 不在官方数据中
        CustomDataSource source = new TestDataSource(
                Collections.emptyMap(),
                Collections.singletonMap(110999, Collections.singletonList(new TimelineEntry("测试区", "1990", "2020"))));
        IdValidator.initialize(source, MergeMode.MERGE);

        String id = buildValidId(110999, "19950101");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("测试区"), "内置缺失时应使用自定义数据");
    }

    @Test
    @DisplayName("MERGE 模式：年份边界不跨数据源继承")
    void mergeShouldNotInheritYearBoundariesAcrossSources() {
        // 110101 内置时间线: [{address:"东城区", startYear:"", endYear:""}]（空边界=覆盖所有年份）
        // 自定义时间线: [{address:"自定义东城区", startYear:"1990", endYear:"1995"}]
        // MERGE 合并后: [Custom(1990-1995), BuiltIn(空边界)]
        //
        // 无修复时 AddressResolver 年份继承会跨源泄漏：
        //   BuiltIn 继承 Custom 的 endYear="1995" → BuiltIn 覆盖范围变为 1990-1995
        //   查询年份 2000 → 两条都不匹配 → 非严格模式回退第一条（Custom）→ 返回"自定义东城区"（错误）
        //
        // 修复后各源独立规范化边界：
        //   Custom(1990-1995), BuiltIn(0001-9999)
        //   查询年份 2000 → Custom 不匹配，BuiltIn 匹配 → 返回"东城区"（正确）
        CustomDataSource source = new TestDataSource(
                Collections.emptyMap(),
                Collections.singletonMap(
                        110101, Collections.singletonList(new TimelineEntry("自定义东城区", "1990", "1995"))));
        IdValidator.initialize(source, MergeMode.MERGE);

        // 年份 2000 在自定义范围(1990-1995)外，应匹配内置"东城区"
        String id = buildValidId(110101, "20000101");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("东城区"), "年份在自定义范围外应匹配内置数据");
        assertFalse(info.getAddress().contains("自定义"), "不应回退到自定义条目");
    }

    // ==================== 初始化生命周期测试 ====================

    @Test
    @DisplayName("未安装自定义数据源时默认使用内置数据")
    void shouldUseBuiltinWhenNoCustomSourceInstalled() {
        // 不调用 initialize()，直接使用内置数据
        String id = buildValidId(110101, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("东城区"), "未安装自定义源时应使用内置数据");
        assertFalse(info.getAddress().contains("自定义"), "不应包含自定义数据");
    }

    @Test
    @DisplayName("重复初始化抛出 IllegalStateException")
    void shouldThrowOnDuplicateInitialization() {
        CustomDataSource source = new TestDataSource(Collections.emptyMap(), Collections.emptyMap());
        IdValidator.initialize(source, MergeMode.FALLBACK);

        assertThrows(
                IllegalStateException.class,
                () -> {
                    IdValidator.initialize(source, MergeMode.MERGE);
                },
                "重复初始化应抛出 IllegalStateException");
    }

    @Test
    @DisplayName("initialize(null, MERGE) 抛出 IllegalArgumentException")
    void shouldThrowOnNullSource() {
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    IdValidator.initialize(null, MergeMode.MERGE);
                },
                "null 数据源应抛出 IllegalArgumentException");
    }

    @Test
    @DisplayName("initialize(source, null) 抛出 IllegalArgumentException")
    void shouldThrowOnNullMergeMode() {
        CustomDataSource source = new TestDataSource(Collections.emptyMap(), Collections.emptyMap());
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    IdValidator.initialize(source, null);
                },
                "null 合并策略应抛出 IllegalArgumentException");
    }

    @Test
    @DisplayName("preload() 可重复调用，多次调用无副作用且内置数据正常")
    void preloadShouldBeIdempotent() {
        // preload() 依赖 JVM 类初始化保证幂等：重复调用不应抛异常
        IdValidator.preload();
        IdValidator.preload();
        IdValidator.preload();

        // 预加载后内置数据仍可正常解析
        String id = buildValidId(110101, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);
        assertTrue(info.getAddress().contains("东城区"), "preload 后内置数据应正常可用");
    }

    // ==================== 测试用 DataSource 实现 ====================

    /**
     * 测试用 CustomDataSource 实现（无 getMergeMode，合并策略由 initialize 参数指定）。
     */
    private static final class TestDataSource implements CustomDataSource {

        private final Map<Integer, String> addressNames;
        private final Map<Integer, List<TimelineEntry>> timelines;

        TestDataSource(Map<Integer, String> addressNames, Map<Integer, List<TimelineEntry>> timelines) {
            this.addressNames = (addressNames == null) ? Collections.emptyMap() : addressNames;
            this.timelines = (timelines == null) ? Collections.emptyMap() : timelines;
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
