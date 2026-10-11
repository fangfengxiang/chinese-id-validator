package cn.chineseid.it;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.IdValidator;
import cn.chineseid.validator.IdValidatorException;
import cn.chineseid.validator.TestStateResetter;
import cn.chineseid.validator.data.CustomDataSource;
import cn.chineseid.validator.data.MergeMode;
import cn.chineseid.validator.model.IdInfo;
import cn.chineseid.validator.model.TimelineEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 集成测试：自定义数据源 SPI 两种合并策略端到端验证。
 *
 * <p>由 maven-failsafe-plugin 在 integration-test 阶段执行（{@code mvn verify}）。
 *
 * <p>测试场景：
 * <ol>
 *   <li>FALLBACK 模式：内置有数据 → 不查自定义；内置无数据 → 查自定义
 *   <li>MERGE 模式：总是查自定义，与内置合并，自定义优先
 *   <li>初始化生命周期：重复初始化、null 参数校验
 *   <li>异常传播：自定义数据源抛异常时包装为 IdValidatorException
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
        TestStateResetter.resetDataProvider();
    }

    private static String buildValidId(int addressCode, String birthday) {
        String body = String.format("%06d", addressCode) + birthday + "123";
        String checkBit = cn.chineseid.validator.core.CheckBitCalculator.calculate(body);
        return body + checkBit;
    }

    private static final class CustomTestSource implements CustomDataSource {

        private final Map<Integer, String> addressNames;
        private final Map<Integer, List<TimelineEntry>> timelines;

        CustomTestSource(Map<Integer, String> addressNames, Map<Integer, List<TimelineEntry>> timelines) {
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

    // ==================== FALLBACK 模式 ====================

    @Test
    @DisplayName("FALLBACK 模式：内置有数据时地址解析不受自定义影响")
    void fallbackShouldUseBuiltinWhenPresent() {
        CustomDataSource source = new CustomTestSource(
                Collections.singletonMap(BUILTIN_CODE, "自定义东城区"),
                Collections.singletonMap(
                        BUILTIN_CODE, Collections.singletonList(new TimelineEntry("自定义东城区", "1980", "2020"))));
        IdValidator.initialize(source, MergeMode.FALLBACK);

        String id = buildValidId(BUILTIN_CODE, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("东城区"), "应解析出地址");
        assertFalse(info.getAddress().contains("自定义"), "FALLBACK 模式不应使用自定义数据");
    }

    @Test
    @DisplayName("FALLBACK 模式：内置无数据时使用自定义时间线解析地址")
    void fallbackShouldUseCustomWhenBuiltinMissing() {
        CustomDataSource source = new CustomTestSource(
                Collections.emptyMap(),
                Collections.singletonMap(
                        MISSING_CODE, Collections.singletonList(new TimelineEntry("补充测试区", "1990", "2020"))));
        IdValidator.initialize(source, MergeMode.FALLBACK);

        String id = buildValidId(MISSING_CODE, "19950101");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("补充测试区"), "应使用自定义时间线解析");
    }

    // ==================== MERGE 模式 ====================

    @Test
    @DisplayName("MERGE 模式：内置有数据时自定义优先覆盖地址名")
    void mergeShouldOverrideWithCustomWhenPresent() {
        CustomDataSource source = new CustomTestSource(
                Collections.singletonMap(BUILTIN_CODE, "自定义东城区"),
                Collections.singletonMap(
                        BUILTIN_CODE, Collections.singletonList(new TimelineEntry("自定义东城区", "1980", "2020"))));
        IdValidator.initialize(source, MergeMode.MERGE);

        String id = buildValidId(BUILTIN_CODE, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("自定义东城区"), "MERGE 模式应使用自定义地址");
    }

    @Test
    @DisplayName("MERGE 模式：内置无数据时使用自定义时间线")
    void mergeShouldUseCustomWhenBuiltinMissing() {
        CustomDataSource source = new CustomTestSource(
                Collections.emptyMap(),
                Collections.singletonMap(
                        MISSING_CODE, Collections.singletonList(new TimelineEntry("合并测试区", "1990", "2020"))));
        IdValidator.initialize(source, MergeMode.MERGE);

        String id = buildValidId(MISSING_CODE, "19950101");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("合并测试区"), "应使用自定义时间线解析");
    }

    @Test
    @DisplayName("MERGE 模式：自定义无数据时回退内置")
    void mergeShouldFallbackToBuiltinWhenCustomEmpty() {
        CustomDataSource source = new CustomTestSource(Collections.emptyMap(), Collections.emptyMap());
        IdValidator.initialize(source, MergeMode.MERGE);

        String id = buildValidId(BUILTIN_CODE, "19900307");
        IdInfo info = IdValidator.getInfo(id, false);

        assertTrue(info.getAddress().contains("东城区"), "自定义为空时应回退内置数据");
    }

    // ==================== 初始化生命周期 ====================

    @Test
    @DisplayName("重复初始化抛出 IllegalStateException")
    void shouldThrowOnDuplicateInitialization() {
        CustomDataSource source = new CustomTestSource(Collections.emptyMap(), Collections.emptyMap());
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
        CustomDataSource source = new CustomTestSource(Collections.emptyMap(), Collections.emptyMap());
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    IdValidator.initialize(source, null);
                },
                "null 合并策略应抛出 IllegalArgumentException");
    }

    @Test
    @DisplayName("并发初始化：仅一个线程成功，其余抛 IllegalStateException")
    void concurrentInitializeShouldAllowOnlyOneSuccess() throws Exception {
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger illegalStateCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    IdValidator.initialize(
                            new CustomTestSource(Collections.emptyMap(), Collections.emptyMap()), MergeMode.MERGE);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    illegalStateCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "所有线程应在超时前完成");
        pool.shutdownNow();

        assertEquals(1, successCount.get(), "应仅有 1 个线程初始化成功");
        assertEquals(threads - 1, illegalStateCount.get(), "其余线程应抛 IllegalStateException");
    }

    // ==================== 异常传播 ====================

    @Test
    @DisplayName("自定义数据源抛 RuntimeException 时包装为 IdValidatorException")
    void shouldWrapDataSourceException() {
        CustomDataSource failingSource = new CustomDataSource() {
            @Override
            public List<TimelineEntry> getTimeline(int code) {
                throw new RuntimeException("模拟 Nacos 连接失败");
            }

            @Override
            public String getAddressName(int code) {
                throw new RuntimeException("模拟 Nacos 连接失败");
            }
        };
        IdValidator.initialize(failingSource, MergeMode.MERGE);

        String id = buildValidId(BUILTIN_CODE, "19900307");

        assertThrows(
                IdValidatorException.class,
                () -> {
                    IdValidator.getInfo(id, false);
                },
                "数据源异常应包装为 IdValidatorException");
    }
}
