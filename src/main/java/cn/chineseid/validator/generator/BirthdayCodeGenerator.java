package cn.chineseid.validator.generator;

import cn.chineseid.validator.data.DataProvider;
import cn.chineseid.validator.model.TimelineEntry;
import cn.chineseid.validator.util.StringUtil;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 生日码生成器。
 *
 * 结合地址码时间线约束，生成合法的出生日期码。
 */
public final class BirthdayCodeGenerator {

    private static final int MIN_YEAR = 1800;
    private static final int RANDOM_YEAR_MIN = 1950;
    private static final int DEFAULT_YEAR = 2000;
    private static final int MAX_MONTH = 12;
    private static final int MAX_DAY_RANDOM = 28;
    private static final int MAX_DAY = 31;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private BirthdayCodeGenerator() {}

    /**
     * 生成生日码。
     *
     * @param addressCode 地址码（6 位字符串）
     * @param address     地址名称（用于匹配时间线条目）
     * @param birthday    生日字符串（YYYYMMDD 格式，可部分填充）
     * @return 8 位生日码字符串
     */
    public static String generate(String addressCode, String address, String birthday) {
        String yearStr = StringUtil.padLeft(extract(birthday, 0, 4), 4);
        String monthStr = StringUtil.padLeft(extract(birthday, 4, 6), 2);
        String dayStr = StringUtil.padLeft(extract(birthday, 6, 8), 2);

        yearStr = datePipelineHandle(yearStr, "year");
        monthStr = datePipelineHandle(monthStr, "month");
        dayStr = datePipelineHandle(dayStr, "day");

        // 查询时间线约束
        int code = Integer.parseInt(addressCode);
        List<TimelineEntry> timeline = DataProvider.getInstance().getTimeline(code);

        String startYear = TimelineEntry.DEFAULT_START_YEAR;
        String endYear = TimelineEntry.DEFAULT_END_YEAR;

        for (TimelineEntry entry : timeline) {
            if (entry.getAddress().equals(address)) {
                if (!entry.getStartYear().isEmpty()) {
                    startYear = entry.getStartYear();
                }
                if (!entry.getEndYear().isEmpty()) {
                    endYear = entry.getEndYear();
                }
            }
        }

        int year = parseIntSafe(yearStr, DEFAULT_YEAR);
        int startY = parseIntSafe(startYear, 1);
        int endY = parseIntSafe(endYear, 9999);

        if (year < startY) {
            yearStr = startYear;
        }
        if (year > endY) {
            yearStr = endYear;
        }

        // 验证日期合法性
        String result = yearStr + monthStr + dayStr;
        if (!isValidDate(result)) {
            yearStr = StringUtil.padLeft(yearStr, 4);
            monthStr = StringUtil.padLeft(monthStr, 2);
            dayStr = StringUtil.padLeft(dayStr, 2);
            result = yearStr + monthStr + dayStr;
        }

        return result;
    }

    /**
     * 日期管道处理。超出范围的年/月/日替换为随机值。
     */
    private static String datePipelineHandle(String date, String category) {
        int dateInt = parseIntSafe(date, 0);
        int currentYear = Year.now().getValue();

        switch (category) {
            case "year":
                if (dateInt < MIN_YEAR || dateInt > currentYear) {
                    dateInt = ThreadLocalRandom.current().nextInt(currentYear - RANDOM_YEAR_MIN + 1) + RANDOM_YEAR_MIN;
                }
                return String.valueOf(dateInt);
            case "month":
                if (dateInt < 1 || dateInt > MAX_MONTH) {
                    dateInt = ThreadLocalRandom.current().nextInt(MAX_MONTH) + 1;
                }
                return String.valueOf(dateInt);
            case "day":
                if (dateInt < 1 || dateInt > MAX_DAY) {
                    dateInt = ThreadLocalRandom.current().nextInt(MAX_DAY_RANDOM) + 1;
                }
                return String.valueOf(dateInt);
            default:
                return date;
        }
    }

    private static boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static String extract(String source, int start, int end) {
        if (source == null || source.length() < end) {
            return "";
        }
        return source.substring(start, end);
    }

    private static int parseIntSafe(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
