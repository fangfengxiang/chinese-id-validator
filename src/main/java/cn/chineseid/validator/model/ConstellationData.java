package cn.chineseid.validator.model;

import java.util.Objects;

/**
 * 星座数据（不可变）。
 */
public final class ConstellationData {
    private final String name;
    private final String startDate;
    private final String endDate;

    public ConstellationData(String name, String startDate, String endDate) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
        this.endDate = Objects.requireNonNull(endDate, "endDate must not be null");
    }

    public String getName() {
        return name;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    /**
     * 获取开始月份（1-12）。
     */
    public int getStartMonth() {
        return Integer.parseInt(startDate.substring(0, 2));
    }

    /**
     * 获取开始日期（1-31）。
     */
    public int getStartDay() {
        return Integer.parseInt(startDate.substring(3, 5));
    }

    @Override
    public String toString() {
        return "ConstellationData{name='" + name + "', startDate='" + startDate + "', endDate='" + endDate + "'}";
    }
}
