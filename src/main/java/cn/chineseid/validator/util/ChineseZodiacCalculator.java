package cn.chineseid.validator.util;

import cn.chineseid.validator.data.DataProvider;

/**
 * 生肖计算器。
 *
 * 算法：(出生年份 - 1900) % 12 → 生肖索引。
 * 1900 年为子鼠，1901 年为丑牛，依此类推。
 */
public final class ChineseZodiacCalculator {

    /** 生肖基准年（1900 年 = 子鼠） */
    private static final int BASE_YEAR = 1900;

    /** 生肖总数 */
    private static final int ZODIAC_COUNT = 12;

    private ChineseZodiacCalculator() {
    }

    /**
     * 根据生日码计算生肖。
     *
     * @param birthdayCode 8 位生日码（YYYYMMDD）
     * @return 生肖名称（如 "子鼠"）
     */
    public static String calculate(String birthdayCode) {
        if (birthdayCode == null || birthdayCode.length() < 4) {
            return "";
        }

        int year;
        try {
            year = Integer.parseInt(birthdayCode.substring(0, 4));
        } catch (NumberFormatException e) {
            return "";
        }
        int index = (year - BASE_YEAR) % ZODIAC_COUNT;

        // 处理负数取模（年份 < 1900 的情况）
        if (index < 0) {
            index += ZODIAC_COUNT;
        }

        return DataProvider.getInstance().getChineseZodiac(index);
    }
}
