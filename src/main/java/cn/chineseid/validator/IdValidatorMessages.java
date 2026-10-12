package cn.chineseid.validator;

/**
 * 集中管理异常消息常量。
 *
 * <p>所有公开 API 抛出的异常消息均从此类引用，避免散落的字符串字面量。
 */
public final class IdValidatorMessages {

    private IdValidatorMessages() {}

    public static final String DUPLICATE_INITIALIZATION =
            "IdValidator has already been initialized; initialize() can only be called once per JVM lifecycle";

    public static final String NULL_DATA_SOURCE = "CustomDataSource must not be null";

    public static final String NULL_MERGE_MODE = "MergeMode must not be null";

    public static final String NOT_VALID_ID = "Not Valid ID card number: ";

    public static final String ID_MUST_BE_15_DIGITS = "ID must be 15 digits, got: ";

    public static final String DATA_SOURCE_QUERY_FAILED = "Custom data source query failed for address code: ";

    public static final String DATA_SOURCE_NULL_TIMELINE =
            "CustomDataSource.getTimeline returned null for address code (violated non-null contract): ";
}
