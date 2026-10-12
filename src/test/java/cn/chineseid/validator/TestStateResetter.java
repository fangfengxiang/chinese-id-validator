package cn.chineseid.validator;

import cn.chineseid.validator.data.DataProvider;
import java.lang.reflect.Field;

/**
 * 测试辅助：通过反射重置 {@link DataProvider} 的初始化状态。
 *
 * <p>仅供单元/集成测试在 {@code @AfterEach} 中调用，
 * 不属于公开 API。生产代码不应使用此类。
 *
 * <h3>设计说明</h3>
 * <p>生产代码的 {@link DataProvider#initialize} 是一次性初始化（JVM 生命周期内仅允许调用一次）。
 * 测试需要反复安装不同配置来验证各种场景，因此通过反射绕过该约束。
 *
 * <p><b>并行测试风险</b>：反射重置的是全局共享状态，若 JUnit 并行执行测试，
 * 可能出现一个测试重置状态时另一个测试正在进行校验，导致不确定行为。
 * 当前测试以串行模式运行（surefire/failsafe 默认 forkCount=1）。
 * 一次性初始化的真实并发安全保证由 CI 矩阵（JDK 8/11/17/21/25 各为独立 JVM）覆盖。
 */
public final class TestStateResetter {

    private TestStateResetter() {}

    /**
     * 重置 DataProvider 的 initialized 标志和 config 引用，
     * 使后续 {@link IdValidator#initialize} 可以再次调用。
     */
    public static void resetDataProvider() {
        try {
            DataProvider dp = DataProvider.getInstance();
            Field initialized = DataProvider.class.getDeclaredField("initialized");
            initialized.setAccessible(true);
            initialized.setBoolean(dp, false);

            Field config = DataProvider.class.getDeclaredField("config");
            config.setAccessible(true);
            config.set(dp, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Failed to reset DataProvider state for testing", e);
        }
    }
}
