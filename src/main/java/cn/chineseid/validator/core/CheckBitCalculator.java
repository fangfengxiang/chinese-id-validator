package cn.chineseid.validator.core;

/**
 * 身份证校验码计算器。
 *
 * 算法（GB 11643-1999 附录 B）：
 * 1. 17 位 body 各位乘以对应权重，累加得 sum
 * 2. checkBit = (12 - (sum % 11)) % 11
 * 3. checkBit == 10 → "X"，否则 → 数字字符串
 *
 * 权重数组（位置 1~17 对应）：
 * [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
 *
 * 预计算 int[] 权重数组，纯整数运算，零精度风险。
 */
public final class CheckBitCalculator {

    /** GB 11643-1999 标准权重 */
    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /** 校验码对照表：(12 - (sum % 11)) % 11 → 字符 */
    private static final char[] CHECK_BITS = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private CheckBitCalculator() {}

    /**
     * 计算 17 位 body 的校验码。
     *
     * @param body 17 位数字字符串
     * @return 校验码字符（'0'-'9' 或 'X'）
     * @throws IllegalArgumentException body 长度不为 17 或含非数字字符
     */
    public static String calculate(String body) {
        if (body == null || body.length() != 17) {
            throw new IllegalArgumentException(
                    "body must be 17 digits, got: " + (body == null ? "null" : body.length()));
        }

        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char c = body.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException(
                        "body must be all digits, found non-digit at position " + i + ": " + c);
            }
            sum += (c - '0') * WEIGHTS[i];
        }

        int checkBitIndex = (12 - (sum % 11)) % 11;
        return String.valueOf(CHECK_BITS[checkBitIndex]);
    }

    /**
     * 验证 18 位身份证号的校验码是否正确。
     *
     * @param id 18 位身份证号
     * @return true 如果校验码正确
     */
    public static boolean verify(String id) {
        if (id == null || id.length() != 18) {
            return false;
        }
        String body = id.substring(0, 17);
        String checkBit = id.substring(17, 18).toUpperCase();
        try {
            return calculate(body).equals(checkBit);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
