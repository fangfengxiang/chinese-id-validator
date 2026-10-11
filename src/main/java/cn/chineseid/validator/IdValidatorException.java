package cn.chineseid.validator;

/**
 * 身份证验证器运行时异常。
 *
 * <p>用于包装自定义数据源查询失败等运行时故障。
 * 参数校验错误仍使用 {@link IllegalArgumentException}，
 * 重复初始化使用 {@link IllegalStateException}。
 */
public class IdValidatorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造异常实例。
     *
     * @param message 异常消息
     */
    public IdValidatorException(String message) {
        super(message);
    }

    /**
     * 构造异常实例并保留原始原因。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public IdValidatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
