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
     * startYear/endYear 为空字符串表示无下界/上界限制。
     */
    public boolean coversYear(int year) {
        int start = parseYear(startYear, 0);
        int end = parseYear(endYear, Integer.MAX_VALUE);
        return year >= start && year <= end;
    }

    private static int parseYear(String yearStr, int defaultValue) {
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
        return address.equals(that.address)
                && startYear.equals(that.startYear)
                && endYear.equals(that.endYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, startYear, endYear);
    }
}
