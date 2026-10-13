package cn.chineseid.validator.core;

import cn.chineseid.validator.model.IdCode;
import cn.chineseid.validator.util.AddressResolver;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * 身份证号校验器。
 */
public final class Validator {

    private static final int MIN_YEAR = 1800;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private Validator() {}

    /**
     * 校验地址码有效性。
     *
     * <p>按校验模式分流：
     * <ul>
     *   <li><b>Strict</b>：仅完整六位码时间线与 [1985, 当前年] 相交 → 有效
     *   <li><b>Loose</b>：在 Strict 基础上额外允许历史后缀兼容
     *       （后缀 01/02/11/20 + 省级证据 + 地级证据）
     * </ul>
     *
     * <p>有效性（地址码是否可能出现在真实身份证上）与地址解析（地址码在特定
     * 出生年对应的名称）分离。地址解析仍由 {@link AddressResolver#getAddressInfo} 负责。
     *
     * @param idCode 解析后的身份证号
     * @param strict true=严格模式（仅完整时间线），false=宽松模式（允许历史兼容）
     * @return true 如果地址码在给定模式下有效
     */
    public static boolean checkAddressCode(IdCode idCode, boolean strict) {
        return AddressResolver.isAddressCodeValid(idCode.getAddressCode(), strict);
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
