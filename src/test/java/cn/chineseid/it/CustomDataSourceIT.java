package cn.chineseid.it;

import cn.chineseid.validator.IdValidator;
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
 * 集成测试：自定义数据源 SPI 两种合并策略端到端验证。
 *
 * <p>由 maven-failsafe-plugin 在 integration-test 阶段执行（{@code mvn verify}）。
 *
 * <p>测试场景：
 * <ol>
 *   <li>FALLBACK 模式：内置有数据 → 不查自定义；内置无数据 → 查自定义
 *   <li>MERGE 模式：总是查自定义，与内置合并，自定义优先
 *   <li>清除后恢复纯内置行为
 * </ol>
 */
@DisplayName("集成测试：自定义数据源合并策略")
class CustomDataSourceIT {

    /** 内置数据中存在的地址码（北京东城区） */
    private static final int BUILTIN_CODE = 110101;

    /** 内置数据中不存在的地址码（用户补充） */
    private static final int MISSING_CODE = 110999;

    @AfterEach
    void cleanup() {
        IdValidator.clearCustomDataSource();
    }

    private static String buildValidId(int addressCode, String birthday) {
        String body = String.format("%06d", addressCode) + birthday + "123";
        String checkBit = cn.chineseid.validator.core.CheckBitCalculator.calculate(body);
        return body + checkBit;
    }

    private static final class CustomTestSource implements CustomDataSource {

        private final MergeMode mode;
        private final Map<Integer, String> addressNames;
        private final Map<Integer, List<TimelineEntry>> timelines;

        CustomTestSource(MergeMode mode,
                         Map<Integer, String> addressNames,
                         Map<Integer, List<TimelineEntry>> timelines) {
            this.mode = mode;
            this.addressNames = (addressNames == null) ? Collections.emptyMap() : addressNames;
            this.timelines = (timelines == null) ? Collections.emptyMap() : timelines;
        }

        @Override
        public MergeMode getMergeMode() {
            return mode;
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

    // ==================== FALLBACK 模式 ====================

    @Test
    @DisplayName("FALLBACK 模式：内置有数据时地址解析不受自定义影响")
    void fallbackShouldUseBuiltinWhenPresent() {
        CustomDataSource source = new CustomTestSource(
                MergeMode.FALLBACK,
                Map.of(BUILTIN_CODE, "自定义东城区"),
                Map.of(BUILTIN_CODE, List.of(new TimelineEntry("自定义东城区", "1980", "2020")))
        );
        IdValidator.setCustomDataSource(source);

        String id = buildValidId(BUILTIN_CODE, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("东城区"), "应解析出地址");
        assertFalse(info.getAddress().contains("自定义"), "FALLBACK 模式不应使用自定义数据");
    }

    @Test
    @DisplayName("FALLBACK 模式：内置无数据时使用自定义时间线解析地址")
    void fallbackShouldUseCustomWhenBuiltinMissing() {
        CustomDataSource source = new CustomTestSource(
                MergeMode.FALLBACK,
                Collections.emptyMap(),
                Map.of(MISSING_CODE, List.of(new TimelineEntry("补充测试区", "1990", "2020")))
        );
        IdValidator.setCustomDataSource(source);

        String id = buildValidId(MISSING_CODE, "19950101");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("补充测试区"), "应使用自定义时间线解析");
    }

    // ==================== MERGE 模式 ====================

    @Test
    @DisplayName("MERGE 模式：内置有数据时自定义优先覆盖地址名")
    void mergeShouldOverrideWithCustomWhenPresent() {
        CustomDataSource source = new CustomTestSource(
                MergeMode.MERGE,
                Map.of(BUILTIN_CODE, "自定义东城区"),
                Map.of(BUILTIN_CODE, List.of(new TimelineEntry("自定义东城区", "1980", "2020")))
        );
        IdValidator.setCustomDataSource(source);

        String id = buildValidId(BUILTIN_CODE, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("自定义东城区"), "MERGE 模式应使用自定义地址");
    }

    @Test
    @DisplayName("MERGE 模式：内置无数据时使用自定义时间线")
    void mergeShouldUseCustomWhenBuiltinMissing() {
        CustomDataSource source = new CustomTestSource(
                MergeMode.MERGE,
                Collections.emptyMap(),
                Map.of(MISSING_CODE, List.of(new TimelineEntry("合并测试区", "1990", "2020")))
        );
        IdValidator.setCustomDataSource(source);

        String id = buildValidId(MISSING_CODE, "19950101");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("合并测试区"), "应使用自定义时间线解析");
    }

    @Test
    @DisplayName("MERGE 模式：自定义无数据时回退内置")
    void mergeShouldFallbackToBuiltinWhenCustomEmpty() {
        CustomDataSource source = new CustomTestSource(
                MergeMode.MERGE,
                Collections.emptyMap(),
                Collections.emptyMap()
        );
        IdValidator.setCustomDataSource(source);

        String id = buildValidId(BUILTIN_CODE, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("东城区"), "自定义为空时应回退内置数据");
    }

    // ==================== 清除后恢复 ====================

    @Test
    @DisplayName("清除自定义数据源后恢复纯内置行为")
    void shouldRestoreBuiltinOnlyAfterClear() {
        CustomDataSource source = new CustomTestSource(
                MergeMode.MERGE,
                Map.of(BUILTIN_CODE, "自定义东城区"),
                Map.of(BUILTIN_CODE, List.of(new TimelineEntry("自定义东城区", "1980", "2020")))
        );
        IdValidator.setCustomDataSource(source);
        IdValidator.clearCustomDataSource();

        String id = buildValidId(BUILTIN_CODE, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertFalse(info.getAddress().contains("自定义"), "清除后不应包含自定义数据");
        assertTrue(info.getAddress().contains("东城区"), "清除后应恢复内置数据");
    }

    @Test
    @DisplayName("null 注入等同于清除")
    void nullInjectionShouldActAsClear() {
        IdValidator.setCustomDataSource(null);

        String id = buildValidId(BUILTIN_CODE, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("东城区"), "null 注入应恢复为内置数据");
    }
}
