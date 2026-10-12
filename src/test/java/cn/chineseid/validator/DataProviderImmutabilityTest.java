package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.data.CustomDataSource;
import cn.chineseid.validator.data.DataProvider;
import cn.chineseid.validator.data.MergeMode;
import cn.chineseid.validator.model.TimelineEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * DataProvider 共享数据封装与不可变性测试。
 *
 * <p>覆盖 ChatGPT 审查中识别的共享状态风险：
 * <ul>
 *   <li>缓存地址码 entry 列表真正不可变（{@code setValue} 抛异常，底层 map 不被篡改）
 *   <li>自定义数据源返回的可变 List 不泄漏到内部查询流程
 *   <li>{@code getTimeline} 返回 null 视为契约违约，fail-fast 抛 {@link IdValidatorException}
 *   <li>{@code getTimeline} 返回空列表（合法）正常回退内置，不抛异常
 * </ul>
 */
@DisplayName("DataProvider 共享数据封装与不可变性")
class DataProviderImmutabilityTest {

    @AfterEach
    void cleanup() {
        TestStateResetter.resetDataProvider();
    }

    // ==================== cachedEntries 不可变性 ====================

    @Test
    @DisplayName("getAddressCodeEntries 的 entry.setValue 抛异常，底层地址码数据不被篡改")
    void addressCodeEntriesShouldBeImmutable() {
        List<Map.Entry<Integer, String>> entries = DataProvider.getInstance().getAddressCodeEntries();
        assertFalse(entries.isEmpty(), "地址码 entry 列表不应为空");

        Map.Entry<Integer, String> first = entries.get(0);
        int code = first.getKey();
        String original = DataProvider.getInstance().getAllAddressCodes().get(code);

        // 列表结构不可变
        assertThrows(UnsupportedOperationException.class, () -> entries.add(null), "entry 列表应禁止 add");
        assertThrows(UnsupportedOperationException.class, () -> entries.clear(), "entry 列表应禁止 clear");

        // 每个 entry 不可变（SimpleImmutableEntry 切断了与底层 map 的可变关联）
        assertThrows(UnsupportedOperationException.class, () -> first.setValue("被篡改"), "entry.setValue 应抛异常，无法修改底层地址码");

        // 底层地址码数据未被影响
        assertEquals(original, DataProvider.getInstance().getAllAddressCodes().get(code), "底层地址码数据不应被 setValue 影响");
    }

    // ==================== 自定义可变 List 不泄漏 ====================

    @Test
    @DisplayName("自定义源返回可变 List，调用方后续修改不应影响 DataProvider 返回结果")
    void customMutableListShouldNotLeak() {
        // 内置不存在的地址码，走 builtIn.isEmpty() 分支（修复前会直接返回 custom 的可变 List）
        int missingCode = 999999;
        List<TimelineEntry> mutableCustom =
                new ArrayList<>(Collections.singletonList(new TimelineEntry("可变区", "1990", "2020")));

        CustomDataSource source = new CustomDataSource() {
            @Override
            public List<TimelineEntry> getTimeline(int code) {
                return (code == missingCode) ? mutableCustom : Collections.emptyList();
            }

            @Override
            public String getAddressName(int code) {
                return null;
            }
        };
        IdValidator.initialize(source, MergeMode.MERGE);

        List<TimelineEntry> result = DataProvider.getInstance().getTimeline(missingCode);
        List<TimelineEntry> snapshot = new ArrayList<>(result);

        // 返回结果应不可变
        assertThrows(
                UnsupportedOperationException.class,
                () -> result.add(new TimelineEntry("注入", "2000", "2010")),
                "返回的列表应不可变");

        // 调用方修改原始可变 List，不应影响已返回的结果
        mutableCustom.add(new TimelineEntry("后追加", "2000", "2010"));
        mutableCustom.clear();

        assertEquals(snapshot, result, "原始可变 List 的后续修改不应泄漏到已返回结果");
        assertEquals(1, result.size(), "返回结果大小应保持不变");
    }

    // ==================== null 违约抛异常 ====================

    @Test
    @DisplayName("getTimeline 返回 null 视为契约违约，抛 IdValidatorException")
    void getTimelineNullShouldThrow() {
        int code = 110101;
        AtomicReference<Integer> queriedCode = new AtomicReference<>();
        CustomDataSource nullSource = new CustomDataSource() {
            @Override
            public List<TimelineEntry> getTimeline(int c) {
                queriedCode.set(c);
                return null; // 违反非 null 契约
            }

            @Override
            public String getAddressName(int c) {
                return null;
            }
        };
        IdValidator.initialize(nullSource, MergeMode.MERGE);

        IdValidatorException ex = assertThrows(
                IdValidatorException.class,
                () -> DataProvider.getInstance().getTimeline(code),
                "null 违约应抛 IdValidatorException");
        assertTrue(ex.getMessage().contains(String.valueOf(code)), "异常消息应包含地址码");
        assertEquals(code, queriedCode.get(), "应确实查询了数据源");
    }

    @Test
    @DisplayName("getTimeline 返回空列表（合法）时正常回退内置，不抛异常")
    void getTimelineEmptyListShouldFallbackNormally() {
        CustomDataSource emptySource = new CustomDataSource() {
            @Override
            public List<TimelineEntry> getTimeline(int code) {
                return Collections.emptyList();
            }

            @Override
            public String getAddressName(int code) {
                return null;
            }
        };
        IdValidator.initialize(emptySource, MergeMode.MERGE);

        // 110101 内置有数据，自定义为空 → 回退内置，不抛异常
        List<TimelineEntry> result = DataProvider.getInstance().getTimeline(110101);
        assertFalse(result.isEmpty(), "自定义为空应回退内置时间线");
    }
}
