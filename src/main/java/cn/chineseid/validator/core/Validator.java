package cn.chineseid.validator.core;

import cn.chineseid.validator.model.IdCode;
import cn.chineseid.validator.util.AddressResolver;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 身份证号校验器。
 */
public final class Validator {

    private static final int MIN_YEAR = 1800;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private Validator() {}

    /**
     * 校验地址码有效性。
     *
     * <p>有效性标准（Option B）：地址码时间线与身份证时代 [1985, 当前年] 相交，
     * 不查出生年。地址码反映发证时的行政区划（发证时间落在身份证时代内），
     * 而非出生时的行政区划，因此不依赖出生年判断有效性。
     *
     * <p>此方法将<b>有效性</b>（地址码是否可能出现在真实身份证上）与
     * <b>地址解析</b>（地址码在特定出生年对应的名称）分离。
     * 地址解析仍由 {@link AddressResolver#getAddressInfo} 负责，以出生年为启发式。
     *
     * @param idCode 解析后的身份证号
     * @param strict 是否严格模式（当前不影响地址码有效性判断——两种模式均使用
     *               时间线相交检查；strict 仅影响地址解析时的年份匹配策略）
     * @return true 如果地址码在身份证时代内被使用过
     */
    public static boolean checkAddressCode(IdCode idCode, boolean strict) {
        return AddressResolver.isAddressCodeValid(idCode.getAddressCode());
    }

    /**
     * 校验出生日期码。
     * 年份范围 [1800, 当前年]，且日期格式合法。
     *
     * @param birthdayCode 8 位生日码（YYYYMMDD）
     * @return true 如果日期合法
     */
    public static boolean checkBirthdayCode(String birthdayCode) {
        if (birthdayCode == null || birthdayCode.length() != 8) {
            return false;
        }
        int year;
        try {
            year = Integer.parseInt(birthdayCode.substring(0, 4));
        } catch (NumberFormatException e) {
            return false;
        }
        if (year < MIN_YEAR) {
            return false;
        }
        int currentYear = Year.now().getValue();
        if (year > currentYear) {
            return false;
        }
        try {
            LocalDate.parse(birthdayCode, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * 校验顺序码。长度必须为 3。
     *
     * @param orderCode 顺序码
     * @return true 如果长度为 3
     */
    public static boolean checkOrderCode(String orderCode) {
        return orderCode != null && orderCode.length() == 3;
    }

    /**
     * 综合校验：地址码 + 生日码 + 顺序码 + 校验码。
     *
     * @param idCode 解析后的身份证号
     * @param strict 是否严格模式
     * @return true 如果所有校验通过
     */
    public static boolean validate(IdCode idCode, boolean strict) {
        if (idCode.getType().getLength() == 18) {
            // 18 位：校验地址码 + 生日码 + 顺序码 + 校验码
            return checkAddressCode(idCode, strict)
                    && checkBirthdayCode(idCode.getBirthdayCode())
                    && checkOrderCode(idCode.getOrderCode())
                    && CheckBitCalculator.verify(idCode.getBody() + idCode.getCheckBit());
        }
        // 15 位：校验地址码 + 生日码 + 顺序码（无校验码）
        return checkAddressCode(idCode, strict)
                && checkBirthdayCode(idCode.getBirthdayCode())
                && checkOrderCode(idCode.getOrderCode());
    }
}
