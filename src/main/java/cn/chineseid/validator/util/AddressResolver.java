package cn.chineseid.validator.util;

import cn.chineseid.validator.data.DataProvider;
import cn.chineseid.validator.model.TimelineEntry;
import java.time.Year;
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

    /**
     * 身份证时代起始年份。
     *
     * <p>1985 年《居民身份证条例》施行，中国开始颁发第一代身份证。
     * 地址码反映的是<b>发证时</b>的行政区划，而非出生时的行政区划。
     * 因此，地址码有效性校验以 [1985, 当前年] 为基准——
     * 只要该地址码的时间线在身份证时代内被使用过，即为有效。
     */
    private static final int ID_CARD_ERA_START_YEAR = 1985;

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
     * 判断地址码是否有效。
     *
     * <p><b>有效性标准</b>（Option B）：地址码时间线与身份证时代 [1985, 当前年] 相交，
     * 不查出生年。地址码反映发证时的行政区划，发证时间一定落在身份证时代内
     * （身份证 1985 年才开始颁发），因此只要该码的时间线在身份证时代被使用过即为有效。
     *
     * <p>此方法将<b>有效性</b>（该地址码是否可能出现在真实身份证上）与
     * <b>地址解析</b>（该地址码在特定出生年对应的行政区划名称）分离：
     * <ul>
     *   <li>{@code isAddressCodeValid} — 判断地址码是否有效，不依赖出生年
     *   <li>{@link #getAddress} — 解析地址名称，以出生年为启发式线索
     * </ul>
     *
     * <p>对 130123（正定县，时间线 1986–1992 栾城县 / 1993–至今 正定县），
     * 即使出生年为 1980（早于时间线起点 1986），该码仍然有效——
     * 因为其时间线与 [1985, now] 相交，发证时间落在身份证时代内。
     *
     * <p>对 110999（不在任何数据源中），无时间线且非遗留码 → 无效。
     *
     * @param addressCode 6 位地址码（整数）
     * @return true 如果该地址码在身份证时代内被使用过
     */
    public static boolean isAddressCodeValid(int addressCode) {
        List<TimelineEntry> timeline = DataProvider.getInstance().getTimeline(addressCode);

        if (timeline.isEmpty()) {
            // 无时间线：检查是否为遗留码（后缀 01/02/11/20）或在地址码映射表中
            String codeStr = String.format("%06d", addressCode);
            return !handleLegacyCode(codeStr).isEmpty();
        }

        // 有时间线：检查是否有任何条目与 [ID_CARD_ERA_START_YEAR, 当前年] 相交
        int currentYear = Year.now().getValue();
        for (TimelineEntry entry : timeline) {
            if (entry.intersectsRange(ID_CARD_ERA_START_YEAR, currentYear)) {
                return true;
            }
        }
        return false;
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

        // 3. 时间线年份匹配（年份边界解析收归 TimelineEntry，避免重复逻辑）
        String address = "";
        int year = parseYear(birthdayCode);

        for (TimelineEntry entry : timeline) {
            if (entry.coversYear(year)) {
                address = entry.getAddress();
                break;
            }
        }

        // 4. 非严格模式回退：取距查询年份最近的条目（每条都评估，非固定取某条）
        if (address.isEmpty() && !strict && !timeline.isEmpty()) {
            TimelineEntry nearest = timeline.get(0);
            int sY0 = nearest.getStartYearInt();
            int eY0 = nearest.getEndYearInt();
            int minDist = (year < sY0) ? (sY0 - year) : (year > eY0) ? (year - eY0) : 0;
            for (int i = 1; i < timeline.size(); i++) {
                TimelineEntry entry = timeline.get(i);
                int sY = entry.getStartYearInt();
                int eY = entry.getEndYearInt();
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
        try {
            return Integer.parseInt(birthdayCode.substring(0, 4));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
