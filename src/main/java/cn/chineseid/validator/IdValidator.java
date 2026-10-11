package cn.chineseid.validator;

import cn.chineseid.validator.core.CheckBitCalculator;
import cn.chineseid.validator.core.CodeParser;
import cn.chineseid.validator.core.Validator;
import cn.chineseid.validator.data.CustomDataSource;
import cn.chineseid.validator.data.DataProvider;
import cn.chineseid.validator.generator.FakeIdBuilder;
import cn.chineseid.validator.generator.IdGenerator;
import cn.chineseid.validator.model.BasicIdInfo;
import cn.chineseid.validator.model.IdCode;
import cn.chineseid.validator.model.IdInfo;
import cn.chineseid.validator.model.IdType;
import cn.chineseid.validator.model.Sex;
import cn.chineseid.validator.util.AddressResolver;
import cn.chineseid.validator.util.ConstellationCalculator;
import cn.chineseid.validator.util.ChineseZodiacCalculator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 身份证号码验证器（Facade 模式）。
 *
 * 统一入口，对外暴露全部公开 API。
 */
public final class IdValidator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private IdValidator() {
    }

    // ==================== 验证 API ====================

    /**
     * 验证身份证号合法性。
     *
     * @param id     身份证号（15 位或 18 位）
     * @param strict true=严格模式（校验地址码），false=宽松模式
     * @return true 如果身份证号合法
     */
    public static boolean isValid(String id, boolean strict) {
        try {
            IdCode code = CodeParser.parse(id);
            return Validator.validate(code, strict);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 宽松验证（strict=false）。
     */
    public static boolean isLooseValid(String id) {
        return isValid(id, false);
    }

    /**
     * 严格验证（strict=true）。
     */
    public static boolean isStrictValid(String id) {
        return isValid(id, true);
    }

    /**
     * 基础验证（不含地址码校验）。
     *
     * @param id 身份证号
     * @return true 如果顺序码、生日码、校验码均合法
     */
    public static boolean isValidBasic(String id) {
        try {
            IdCode code = CodeParser.parse(id);
            if (!Validator.checkOrderCode(code.getOrderCode())) {
                return false;
            }
            if (!Validator.checkBirthdayCode(code.getBirthdayCode())) {
                return false;
            }
            if (code.getType() == IdType.FIFTEEN) {
                return true;
            }
            return CheckBitCalculator.verify(code.getBody() + code.getCheckBit());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ==================== 信息解析 API ====================

    /**
     * 获取完整身份证信息。
     *
     * @param id     身份证号
     * @param strict 是否严格模式
     * @return IdInfo 对象
     * @throws IllegalArgumentException 身份证号不合法
     */
    public static IdInfo getInfo(String id, boolean strict) {
        if (!isValid(id, strict)) {
            throw new IllegalArgumentException("Not Valid ID card number: " + id);
        }

        IdCode code = CodeParser.parse(id);

        List<String> addressInfo = AddressResolver.getAddressInfo(
                code.getAddressCode(), code.getBirthdayCode(), strict);
        String province = addressInfo.get(0);
        String city = addressInfo.get(1);
        String district = addressInfo.get(2);

        int abandoned = DataProvider.getInstance().getAddressName(code.getAddressCode()).isEmpty() ? 1 : 0;

        LocalDate birthday = LocalDate.parse(code.getBirthdayCode(), DATE_FORMATTER);

        int sexCode = Integer.parseInt(code.getOrderCode());
        Sex sex = Sex.fromOrderDigit(sexCode % 10);

        return new IdInfo(
                code.getAddressCode(),
                abandoned,
                province + city + district,
                addressInfo,
                birthday,
                ConstellationCalculator.calculate(code.getBirthdayCode()),
                ChineseZodiacCalculator.calculate(code.getBirthdayCode()),
                sex,
                code.getType(),
                code.getCheckBit()
        );
    }

    /**
     * 获取基础身份证信息（不含地址）。
     *
     * @param id 身份证号
     * @return BasicIdInfo 对象
     * @throws IllegalArgumentException 身份证号不合法
     */
    public static BasicIdInfo getBasicInfo(String id) {
        if (!isValidBasic(id)) {
            throw new IllegalArgumentException("Not Valid ID card number: " + id);
        }

        IdCode code = CodeParser.parse(id);

        LocalDate birthday = LocalDate.parse(code.getBirthdayCode(), DATE_FORMATTER);
        int sexCode = Integer.parseInt(code.getOrderCode());
        Sex sex = Sex.fromOrderDigit(sexCode % 10);

        return new BasicIdInfo(
                birthday,
                ConstellationCalculator.calculate(code.getBirthdayCode()),
                ChineseZodiacCalculator.calculate(code.getBirthdayCode()),
                sex,
                code.getType(),
                code.getCheckBit()
        );
    }

    // ==================== 假号生成 API ====================

    /**
     * 生成随机假身份证号（18 位）。
     */
    public static String fakeId() {
        return IdGenerator.fakeId();
    }

    /**
     * 按要求生成假身份证号。
     *
     * @param builder 构建参数
     * @return 假身份证号字符串
     */
    public static String fakeRequireId(FakeIdBuilder builder) {
        return IdGenerator.fakeRequireId(builder);
    }

    // ==================== 升级 API ====================

    /**
     * 将 15 位身份证号升级为 18 位。
     *
     * @param id 15 位身份证号
     * @return 18 位身份证号
     * @throws IllegalArgumentException 身份证号不合法或非 15 位
     */
    public static String upgradeId(String id) {
        if (!isValid(id, true)) {
            throw new IllegalArgumentException("Not Valid ID card number: " + id);
        }

        IdCode code = CodeParser.parse(id);
        if (code.getType() != IdType.FIFTEEN) {
            throw new IllegalArgumentException("ID must be 15 digits, got: " + id.length());
        }

        String body = String.format("%06d", code.getAddressCode())
                + code.getBirthdayCode()
                + code.getOrderCode();

        return body + CheckBitCalculator.calculate(body);
    }

    // ==================== 自定义数据源配置 API ====================

    /**
     * 注入自定义数据源。
     *
     * <p>用户实现 {@link CustomDataSource} 接口，在构造函数中完成数据初始化，
     * 通过此方法注入。注入后，所有地址解析和假号生成查询均会按
     * {@link CustomDataSource#getMergeMode()} 指定的策略与内置数据合并。</p>
     *
     * @param dataSource 自定义数据源实现，传 null 等同于 {@link #clearCustomDataSource}
     */
    public static void setCustomDataSource(CustomDataSource dataSource) {
        DataProvider.getInstance().setCustomDataSource(dataSource);
    }

    /**
     * 清除自定义数据源，恢复为仅使用内置数据。
     */
    public static void clearCustomDataSource() {
        DataProvider.getInstance().clearCustomDataSource();
    }
}
