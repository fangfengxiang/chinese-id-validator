package cn.chineseid.validator.model;

import java.util.Objects;

/**
 * 行政区划时间线条目（不可变）。
 * 同一地址码在不同时间段可能对应不同行政区划名称。
 */
public final class TimelineEntry {

    /** 默认起始年份（时间线条目无 start_year 时作为下界） */
    public static final String DEFAULT_START_YEAR = "0001";

    /** 默认结束年份（时间线条目无 end_year 时作为上界） */
    public static final String DEFAULT_END_YEAR = "9999";

    /** 默认起始年份整数值（对应 {@link #DEFAULT_START_YEAR}，格里高利历无公元 0 年，1 是最早合法年份） */
    private static final int DEFAULT_START_YEAR_INT = 1;

    /** 默认结束年份整数值（对应 {@link #DEFAULT_END_YEAR}，"足够远的未来"语义标记值） */
    private static final int DEFAULT_END_YEAR_INT = 9999;

    private final String address;
    private final String startYear;
    private final String endYear;

    public TimelineEntry(String address, String startYear, String endYear) {
        this.address = Objects.requireNonNull(address, "address must not be null");
        this.startYear = startYear == null ? "" : startYear;
        this.endYear = endYear == null ? "" : endYear;
    }

    public String getAddress() {
        return address;
    }

    public String getStartYear() {
        return startYear;
    }

    public String getEndYear() {
        return endYear;
    }

    /**
     * 判断该条目是否覆盖给定年份。
     *
     * <p>空字符串、"未曾使用"、非数字 startYear 视为无下界（回退 {@value #DEFAULT_START_YEAR}，
     * 格里高利历无公元 0 年，1 是最早合法年份，故下界为 1 而非 0）；endYear 同理回退
     * {@value #DEFAULT_END_YEAR}。与 {@code DataProvider.normalizeBoundaries} 的填充值一致。
     *
     * @param year 待判断的公元年份（无 0 年，最小合法值为 1）
     * @return 该年份落在 [startYear, endYear] 区间内返回 true
     */
    public boolean coversYear(int year) {
        return year >= getStartYearInt() && year <= getEndYearInt();
    }

    /**
     * 解析起始年份为整数。
     *
     * <p>空字符串、"未曾使用"、非数字均回退为 {@value #DEFAULT_START_YEAR}（公元 1 年，
     * 因格里高利历无公元 0 年，1 是最早的合法年份）。
     *
     * @return 起始年份整数值，永不小于 1
     */
    public int getStartYearInt() {
        return parseYearToInt(startYear, DEFAULT_START_YEAR_INT);
    }

    /**
     * 解析结束年份为整数。
     *
     * <p>空字符串、"未曾使用"、非数字均回退为 {@value #DEFAULT_END_YEAR}（9999，
     * "足够远的未来"语义标记值，非 {@code Integer.MAX_VALUE} 机器极值）。
     *
     * @return 结束年份整数值，永不大于 9999
     */
    public int getEndYearInt() {
        return parseYearToInt(endYear, DEFAULT_END_YEAR_INT);
    }

    private static int parseYearToInt(String yearStr, int defaultValue) {
        if (yearStr == null || yearStr.isEmpty() || "未曾使用".equals(yearStr)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public String toString() {
        return "TimelineEntry{address='" + address + "', startYear='" + startYear + "', endYear='" + endYear + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimelineEntry)) return false;
        TimelineEntry that = (TimelineEntry) o;
        return address.equals(that.address) && startYear.equals(that.startYear) && endYear.equals(that.endYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, startYear, endYear);
    }
}
