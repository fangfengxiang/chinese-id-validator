package cn.chineseid.validator.util;

import cn.chineseid.validator.data.DataProvider;
import cn.chineseid.validator.model.TimelineEntry;
import java.time.LocalDate;
import java.time.ZoneId;
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

    /**
     * 身份证时代起始年份。
     *
     * <p>1985 年《居民身份证条例》施行，中国开始颁发第一代身份证。
     * 地址码反映的是<b>发证时</b>的行政区划，而非出生时的行政区划。
     * 因此，地址码有效性校验以 [1985, 当前年] 为基准——
     * 只要该地址码的时间线在身份证时代内被使用过，即为有效。
     */
    private static final int ID_CARD_ERA_START_YEAR = 1985;

    /**
     * 中国标准时间对应的时区。
     *
     * <p>行政区划时间线用于中国居民身份证号码的地址码校验。
     * 现有校验规则以当前年份作为时间线上界，
     * 因此统一采用中国标准时间获取当前年份，
     * 保证不同服务器部署环境下的判断结果一致。
     *
     * <p>这只是本类库的时间边界政策，
     * 不表示行政区划有效性由身份证签发当天决定。
     *
     * <p>不会修改 JVM 默认时区或宿主应用的时间设置。
     */
    private static final ZoneId CHINA_STANDARD_TIME_ZONE = ZoneId.of("Asia/Shanghai");

    /** 历史后缀：市辖区（01） */
    private static final String LEGACY_SUFFIX_CITY_DISTRICT = "01";

    /** 历史后缀：城区（02） */
    private static final String LEGACY_SUFFIX_URBAN = "02";

    /** 历史后缀：郊区（11） */
    private static final String LEGACY_SUFFIX_SUBURB = "11";

    /** 历史后缀：市区（20） */
    private static final String LEGACY_SUFFIX_CITY_PROPER = "20";

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
     * 判断地址码是否有效（Strict 模式：仅完整时间线证据）。
     *
     * <p>有效性标准：地址码时间线与身份证时代 [1985, 当前年] 相交，即为有效。
     * 不查出生年——地址码反映发证时的行政区划，发证时间一定落在身份证时代内
     * （身份证 1985 年才开始颁发），因此只要该码的时间线在身份证时代被使用过即为有效。
     *
     * <p>此方法为 <b>Strict</b> 路径：仅认完整六位码的时间线证据，
     * 不做历史后缀兼容推断。{@link #isAddressCodeValid(int, boolean)} 的
     * Loose 路径在此基础上额外允许 {@link #isLegacyCompatible} 兼容。
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
     * <p>对 110201（昌平县，时间线结束于 1981 年），时间线不与 [1985, now]
     * 相交 → Strict 返回 false。但该码后缀为 01 且省级 110000、地级 110200
     * 均有证据 → Loose 通过 {@link #isLegacyCompatible} 兼容。
     *
     * <p>对 110999（不在任何数据源中），无时间线且非遗留码 → 无效。
     *
     * @param addressCode 6 位地址码（整数）
     * @return true 如果该地址码在身份证时代内被使用过（完整时间线证据）
     */
    public static boolean isAddressCodeValid(int addressCode) {
        List<TimelineEntry> timeline = DataProvider.getInstance().getTimeline(addressCode);

        if (timeline.isEmpty()) {
            return false;
        }

        int currentYearInChina = LocalDate.now(CHINA_STANDARD_TIME_ZONE).getYear();
        for (TimelineEntry entry : timeline) {
            if (entry.intersectsRange(ID_CARD_ERA_START_YEAR, currentYearInChina)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断地址码是否有效（按校验模式分流）。
     *
     * <p><b>Strict</b>：仅完整六位码时间线与 [1985, now] 相交 → 有效。
     * <p><b>Loose</b>：在 Strict 基础上，额外允许受约束的历史后缀兼容
     * （后缀 01/02/11/20 + 省级证据 + 地级证据）。
     *
     * <p>不变量保证：Strict 通过 ⟹ Loose 必通过。
     *
     * @param addressCode 6 位地址码（整数）
     * @param strict      true=严格模式（仅完整时间线），false=宽松模式（允许历史兼容）
     * @return true 如果该地址码在给定模式下有效
     */
    public static boolean isAddressCodeValid(int addressCode, boolean strict) {
        if (isAddressCodeValid(addressCode)) {
            return true;
        }
        if (strict) {
            return false;
        }
        // Loose：检查受约束的历史后缀兼容
        return isLegacyCompatible(addressCode);
    }

    /**
     * 受约束的历史编码兼容判断（Loose 模式专用，private）。
     *
     * <p>当完整六位码缺少与 [1985, now] 相交的时间线时，检查以下三条件
     * 是否同时满足：
     * <ol>
     *   <li>后缀属于历史编码形式（01=市辖区 / 02=城区 / 11=郊区 / 20=市区）
     *   <li>省级代码（前 2 位 + 0000）具有数据证据
     *   <li>地级代码（前 4 位 + 00）具有数据证据
     * </ol>
     *
     * <p>港澳台码（8 开头）不套用大陆县级行政区划的六位层级结构，
     * 直接返回 false。
     *
     * <p>这是对 PHP jxlwqq/id-validator 规则的收紧——增加上级区划约束，
     * 降低单凭后缀放行的误判风险。结构推断不能完全证明历史代码真实存在，
     * 因此只用于 Loose，不用于 Strict。
     *
     * @param addressCode 6 位地址码（整数）
     * @return true 如果后缀为历史形式且省级+地级均有证据
     */
    private static boolean isLegacyCompatible(int addressCode) {
        if (addressCode / 100000 == 8) {
            return false;
        }
        String suffix = String.format("%02d", addressCode % 100);
        if (!suffix.equals(LEGACY_SUFFIX_CITY_DISTRICT)
                && !suffix.equals(LEGACY_SUFFIX_URBAN)
                && !suffix.equals(LEGACY_SUFFIX_SUBURB)
                && !suffix.equals(LEGACY_SUFFIX_CITY_PROPER)) {
            return false;
        }
        int provinceCode = addressCode / 10000 * 10000;
        int parentCode = addressCode / 100 * 100;
        return hasAddressEvidence(provinceCode) && hasAddressEvidence(parentCode);
    }

    /**
     * 判断地址码是否有数据证据（时间线相交或在映射表中）。
     *
     * <p>用于 {@link #isLegacyCompatible} 的上级区划证据检查：
     * 省级/地级码只要有时间线与 [1985, now] 相交，或在 address_code 映射表中，
     * 即视为有证据。
     *
     * @param code 行政区划码（省级/地级）
     * @return true 如果该码有时间线相交或在映射表中
     */
    private static boolean hasAddressEvidence(int code) {
        if (isAddressCodeValid(code)) {
            return true;
        }
        return !DataProvider.getInstance().getAddressName(code).isEmpty();
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
            case LEGACY_SUFFIX_CITY_DISTRICT:
                return "市辖区";
            case LEGACY_SUFFIX_URBAN:
                return "城区";
            case LEGACY_SUFFIX_SUBURB:
                return "郊区";
            case LEGACY_SUFFIX_CITY_PROPER:
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
