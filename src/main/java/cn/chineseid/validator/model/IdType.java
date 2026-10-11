package cn.chineseid.validator.model;

/**
 * 身份证号码长度类型枚举。
 */
public enum IdType {
    FIFTEEN(15),
    EIGHTEEN(18);

    private final int length;

    IdType(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public static IdType fromLength(int length) {
        return length == 18 ? EIGHTEEN : FIFTEEN;
    }
}
