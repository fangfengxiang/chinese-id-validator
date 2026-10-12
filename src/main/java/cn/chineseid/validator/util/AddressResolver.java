package cn.chineseid.validator.util;

import cn.chineseid.validator.data.DataProvider;
import cn.chineseid.validator.model.TimelineEntry;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 地址解析器。
 *
 * 核心逻辑：
 * 1. 地址码拆分为省/市/区三级（前 2 位 + 0000、前 4 位 + 00、完整 6 位）
 * 2. 港澳台居民居住证（8 开头）只查省级信息
 * 3. 时间线匹配：根据出生年份在时间线条目中查找对应地址
 * 4. 历史遗留地址码处理：后缀 01/02/11/20 → 市辖区/城区/郊区/市区
 * 5. 非严格模式：年份不匹配时取距查询年份最近的时间线条目
 *
 * <p><b>配置一致性</b>：{@link #getAddressInfo(int, String, boolean)} 的省/市/区多次查询
 * 各自调用 {@link DataProvider#getTimeline(int)}，每次读取当前自定义配置，
 * 不获取跨调用快照。因此自定义数据源的初始化（{@code IdValidator.initialize}）
 * 须在业务线程开始校验前完成，避免校验进行时发生一次性配置切换导致混合数据。
 */
public final class AddressResolver {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 时间线年份下界回退值（当 startYear 缺失/无法解析时使用，对应 {@link TimelineEntry#DEFAULT_START_YEAR}） */
    private static final int MIN_TIMELINE_YEAR = 1;

    /** 时间线年份上界回退值（当 endYear 缺失/无法解析时使用，表示无上界，对应 {@link TimelineEntry#DEFAULT_END_YEAR}） */
    private static final int MAX_TIMELINE_YEAR = 9999;

    private AddressResolver() {}

    /**
     * 获取省市区地址信息。
     *
     * @param addressCode  地址码（6 位整数）
     * @param birthdayCode 生日码（8 位字符串 YYYYMMDD）
     * @param strict       是否严格模式
     * @return [province, city, district] 三个元素的列表，港澳台只有 province 非空
     */
    public static List<String> getAddressInfo(int addressCode, String birthdayCode, boolean strict) {
        String codeStr = String.format("%06d", addressCode);
        String provinceCode = codeStr.substring(0, 2) + "0000";
        boolean isHKMacaoTaiwan = codeStr.charAt(0) == '8';

        String province = getAddress(Integer.parseInt(provinceCode), birthdayCode, strict);
        String city = "";
        String district = "";

        if (!isHKMacaoTaiwan) {
            String cityCode = codeStr.substring(0, 4) + "00";
            city = getAddress(Integer.parseInt(cityCode), birthdayCode, strict);
            district = getAddress(addressCode, birthdayCode, strict);
        }

        List<String> result = new ArrayList<>(3);
        result.add(province);
        result.add(city);
        result.add(district);
        return result;
    }

    /**
     * 获取单个地址码对应的地址名称。
     *
     * @param addressCode  地址码（6 位整数）
     * @param birthdayCode 生日码（8 位字符串 YYYYMMDD）
     * @param strict       是否严格模式
     * @return 地址名称，未找到返回空字符串
     */
    public static String getAddress(int addressCode, String birthdayCode, boolean strict) {
        String codeStr = String.format("%06d", addressCode);

        // 1. 查询时间线（内置 + 用户自定义合并）
        List<TimelineEntry> timeline = DataProvider.getInstance().getTimeline(addressCode);

        // 2. 时间线为空 → 历史遗留地址码处理
        if (timeline.isEmpty()) {
            return handleLegacyCode(codeStr);
        }

        // 3. 时间线年份匹配
        String address = "";
        int year = parseYear(birthdayCode);

        for (TimelineEntry entry : timeline) {
            // 各条目独立匹配年份：空 startYear 表示无下界（取 MIN_TIMELINE_YEAR），
            // 空 endYear 表示无上界（取 MAX_TIMELINE_YEAR），不跨条目继承。
            int startY = parseYearSafe(entry.getStartYear(), MIN_TIMELINE_YEAR);
            int endY = parseYearSafe(entry.getEndYear(), MAX_TIMELINE_YEAR);
            if (year >= startY && year <= endY) {
                address = entry.getAddress();
                break;
            }
        }

        // 4. 非严格模式回退：取距查询年份最近的条目（每条都评估，非固定取某条）
        if (address.isEmpty() && !strict && !timeline.isEmpty()) {
            TimelineEntry nearest = timeline.get(0);
            int sY0 = parseYearSafe(nearest.getStartYear(), MIN_TIMELINE_YEAR);
            int eY0 = parseYearSafe(nearest.getEndYear(), MAX_TIMELINE_YEAR);
            int minDist = (year < sY0) ? (sY0 - year) : (year > eY0) ? (year - eY0) : 0;
            for (int i = 1; i < timeline.size(); i++) {
                TimelineEntry entry = timeline.get(i);
                int sY = parseYearSafe(entry.getStartYear(), MIN_TIMELINE_YEAR);
                int eY = parseYearSafe(entry.getEndYear(), MAX_TIMELINE_YEAR);
                int dist = (year < sY) ? (sY - year) : (year > eY) ? (year - eY) : 0;
                if (dist < minDist) {
                    minDist = dist;
                    nearest = entry;
                }
            }
            address = nearest.getAddress();
        }

        return address;
    }

    /**
     * 历史遗留地址码处理。
     *
     * 后缀映射：
     * - 01 → 市辖区
     * - 02 → 城区
     * - 11 → 郊区
     * - 20 → 市区
     */
    private static String handleLegacyCode(String codeStr) {
        String suffix = codeStr.substring(4, 6);
        switch (suffix) {
            case "01":
                return "市辖区";
            case "02":
                return "城区";
            case "11":
                return "郊区";
            case "20":
                return "市区";
            default:
                // 查找地址码映射表
                return DataProvider.getInstance().getAddressName(Integer.parseInt(codeStr));
        }
    }

    private static int parseYear(String birthdayCode) {
        if (birthdayCode == null || birthdayCode.length() < 4) {
            return 0;
        }
        return parseYearSafe(birthdayCode.substring(0, 4), 0);
    }

    private static int parseYearSafe(String yearStr, int defaultValue) {
        try {
            return Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
