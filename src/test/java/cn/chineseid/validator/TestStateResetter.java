package cn.chineseid.validator;

import cn.chineseid.validator.data.DataProvider;
import java.lang.reflect.Field;

/**
 * 测试辅助：通过反射重置 {@link DataProvider} 的初始化状态。
 *
 * <p>仅供单元/集成测试在 {@code @AfterEach} 中调用，
 * 不属于公开 API。生产代码不应使用此类。
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
