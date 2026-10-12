package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.data.CustomDataSource;
import cn.chineseid.validator.data.MergeMode;
import cn.chineseid.validator.model.IdInfo;
import cn.chineseid.validator.model.TimelineEntry;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 并发回归测试：多线程同时调用校验/解析/生成 API，验证结果一致性。
 *
 * <p>覆盖 ChatGPT 审查中「条件性风险」的回归面：
 * <ul>
 *   <li>普通多线程并发 {@code isValid}/{@code getInfo}/{@code fakeId} 不产生数据污染
 *   <li>{@code initialize} 完成后多线程校验结果稳定一致
 * </ul>
 *
 * <p>这些测试不证明绝对线程安全，只作并发回归防线：若共享数据封装被破坏，
 * 多线程下应出现结果漂移或异常。
 */
@DisplayName("并发一致性回归测试")
class ConcurrentValidationTest {

    @AfterEach
    void cleanup() {
        TestStateResetter.resetDataProvider();
    }

    private static String buildValidId(int addressCode, String birthday) {
        String body = String.format("%06d", addressCode) + birthday + "123";
        String checkBit = cn.chineseid.validator.core.CheckBitCalculator.calculate(body);
        return body + checkBit;
    }

    @Test
    @DisplayName("多线程并发 isValid/getInfo/fakeId 结果一致且无异常")
    void concurrentValidationShouldBeConsistent() throws Exception {
        String validId = buildValidId(110101, "19900307");

        int threads = 32;
        int iterations = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicInteger validCount = new AtomicInteger(0);
        AtomicInteger infoCount = new AtomicInteger(0);
        AtomicInteger fakeCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < iterations; j++) {
                        if (IdValidator.isValid(validId, true)) {
                            validCount.incrementAndGet();
                        }
                        IdInfo info = IdValidator.getInfo(validId, false);
                        if (info.getAddress().contains("东城区")) {
                            infoCount.incrementAndGet();
                        }
                        String fake = IdValidator.fakeId();
                        if (fake != null && fake.length() == 18) {
                            fakeCount.incrementAndGet();
                        }
                    }
                } catch (Throwable t) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "所有线程应在超时前完成");
        pool.shutdownNow();

        assertEquals(0, errors.get(), "并发过程中不应抛出任何异常");
        assertEquals(threads * iterations, validCount.get(), "每次校验都应通过");
        assertEquals(threads * iterations, infoCount.get(), "每次解析都应包含东城区");
        assertEquals(threads * iterations, fakeCount.get(), "每次生成都应产生 18 位假号");
    }

    @Test
    @DisplayName("initialize 完成后多线程校验结果稳定一致")
    void validationConsistentAfterInitialize() throws Exception {
        // 安装返回空的自定义源（MERGE 模式，全回退内置），验证初始化后并发校验结果不漂移
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

        String validId = buildValidId(110101, "19900307");
        String expectedAddress = IdValidator.getInfo(validId, false).getAddress();

        int threads = 16;
        int iterations = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger mismatches = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    for (int j = 0; j < iterations; j++) {
                        IdInfo info = IdValidator.getInfo(validId, false);
                        if (!expectedAddress.equals(info.getAddress())) {
                            mismatches.incrementAndGet();
                        }
                    }
                } catch (Throwable t) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS), "所有线程应在超时前完成");
        pool.shutdownNow();

        assertEquals(0, errors.get(), "不应抛出异常");
        assertEquals(0, mismatches.get(), "所有线程的解析结果应与初始一致，无配置漂移");
    }
}
