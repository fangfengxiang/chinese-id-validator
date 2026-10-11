package cn.chineseid.validator.generator;

import cn.chineseid.validator.model.Sex;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 顺序码生成器。
 *
 * 顺序码范围 [111, 999]，按性别调整奇偶性：
 * 男(MALE=1) → 奇数，女(FEMALE=0) → 偶数。
 */
public final class OrderCodeGenerator {

    private static final int MIN_ORDER = 111;
    private static final int MAX_ORDER = 999;

    private OrderCodeGenerator() {}

    /**
     * 生成顺序码。
     *
     * @param sex 性别（MALE=1 → 奇数, FEMALE=0 → 偶数）
     * @return 3 位顺序码字符串
     */
    public static String generate(Sex sex) {
        if (sex == null) {
            throw new IllegalArgumentException("sex must not be null");
        }
        int orderCode = ThreadLocalRandom.current().nextInt(MAX_ORDER - MIN_ORDER + 1) + MIN_ORDER;
        if (sex.getCode() != orderCode % 2) {
            // 奇偶性调整：若已在下界则向上调，避免低于 MIN_ORDER
            if (orderCode == MIN_ORDER) {
                orderCode++;
            } else {
                orderCode--;
            }
        }
        return String.valueOf(orderCode);
    }
}
