package cn.chineseid.validator.util;

import cn.chineseid.validator.data.DataProvider;
import cn.chineseid.validator.model.ConstellationData;
import java.util.List;

/**
 * 星座计算器。
 *
 * 算法：根据出生月份和日期，与当月星座的起始日比较。
 * day >= startDay → 当月星座；否则 → 上月星座。
 */
public final class ConstellationCalculator {

    private ConstellationCalculator() {}

    /**
     * 根据生日码计算星座。
     *
     * @param birthdayCode 8 位生日码（YYYYMMDD）
     * @return 星座名称（如 "水瓶座"）
     */
    public static String calculate(String birthdayCode) {
        if (birthdayCode == null || birthdayCode.length() < 8) {
            return "";
        }

        int month;
        int day;
        try {
            month = Integer.parseInt(birthdayCode.substring(4, 6));
            day = Integer.parseInt(birthdayCode.substring(6, 8));
        } catch (NumberFormatException e) {
            return "";
        }

        // 星座数据索引 1-12 对应 1-12 月
        // JSON 数组索引 0-11 对应星座 1-12
        ConstellationData current = getConstellation(month);
        if (current == null) {
            return "";
        }

        if (day >= current.getStartDay()) {
            return current.getName();
        }

        // 上月星座
        int prevMonth = month - 1;
        if (prevMonth == 0) {
            prevMonth = 12; // 摩羯座
        }
        ConstellationData prev = getConstellation(prevMonth);
        return prev == null ? "" : prev.getName();
    }

    /**
     * 根据月份获取星座数据。
     * JSON 数组按月份顺序排列：索引 0=水瓶座(1月), 1=双鱼座(2月), ..., 11=摩羯座(12月)
     * 但实际上星座数据按 start_date 排序，需要按月份索引。
     *
     * 星座数据 JSON 格式：
     * [{"name":"水瓶座","start_date":"01-20","end_date":"02-18"}, ...]
     * 数组索引 0 = 1 月星座, 索引 1 = 2 月星座, ..., 索引 11 = 12 月星座
     */
    private static ConstellationData getConstellation(int month) {
        List<ConstellationData> constellations = DataProvider.getInstance().getConstellations();
        // 星座数据数组索引 0-11 对应月份 1-12
        int index = month - 1;
        if (index < 0 || index >= constellations.size()) {
            return null;
        }
        return constellations.get(index);
    }
}
