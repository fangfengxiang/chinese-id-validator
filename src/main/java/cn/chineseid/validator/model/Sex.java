package cn.chineseid.validator.model;

/**
 * 性别枚举。
 * 身份证号顺序码最后一位：奇数=男，偶数=女。
 */
public enum Sex {
    MALE(1),
    FEMALE(0);

    private final int code;

    Sex(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据顺序码最后一位的奇偶性推断性别。
     *
     * @param orderCodeLastDigit 顺序码最后一位数字
     * @return Sex 枚举值
     */
    public static Sex fromOrderDigit(int orderCodeLastDigit) {
        // 负数取模结果为负，须用 != 0 判定奇偶（IM_BAD_CHECK_FOR_ODD）
        return orderCodeLastDigit % 2 != 0 ? MALE : FEMALE;
    }

    /**
     * 根据 code 值构建（1=男, 0=女）。
     */
    public static Sex fromCode(int code) {
        return code == 1 ? MALE : FEMALE;
    }
}
