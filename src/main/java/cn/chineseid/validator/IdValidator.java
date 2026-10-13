package cn.chineseid.validator;

import cn.chineseid.validator.core.CheckBitCalculator;
import cn.chineseid.validator.core.CodeParser;
import cn.chineseid.validator.core.Validator;
import cn.chineseid.validator.data.CustomDataSource;
import cn.chineseid.validator.data.DataProvider;
import cn.chineseid.validator.data.MergeMode;
import cn.chineseid.validator.generator.FakeIdBuilder;
import cn.chineseid.validator.generator.IdGenerator;
import cn.chineseid.validator.model.BasicIdInfo;
import cn.chineseid.validator.model.IdCode;
import cn.chineseid.validator.model.IdInfo;
import cn.chineseid.validator.model.IdType;
import cn.chineseid.validator.model.Sex;
import cn.chineseid.validator.util.AddressResolver;
import cn.chineseid.validator.util.ChineseZodiacCalculator;
import cn.chineseid.validator.util.ConstellationCalculator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;

/**
 * 身份证号码验证器（Facade 模式）。
 *
 * <p>统一入口，对外暴露全部公开 API。
 *
 * <h3>初始化</h3>
 * <ul>
 *   <li>普通使用无需显式初始化，首次调用业务 API 时自动加载内置数据
 *   <li>{@link #preload()} 可在应用启动阶段显式预加载内置数据（幂等）
 *   <li>{@link #initialize(CustomDataSource, MergeMode)} 一次性安装自定义数据源及合并策略
 * </ul>
 *
 * <p><b>生命周期约束</b>：{@code initialize} 须在业务线程开始校验前完成。
 * 库不支持业务调用过程中切换配置——单次校验内省/市/区多次查询各自读取当前配置，
 * 不获取跨调用快照。业务线程开始后请勿再调用 {@code initialize}。
 */
public final class IdValidator {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private IdValidator() {}

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
            parseAndValidateBasic(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ==================== 信息解析 API ====================

    /**
     * 解析并校验身份证号，返回 {@link IdCode} 对象。
     *
     * <p>内部方法，供 {@link #isValid} 和 {@link #getInfo} 共用，避免重复解析。
     *
     * @param id     身份证号
     * @param strict 是否严格模式
     * @return 解析后的 {@link IdCode}
     * @throws IllegalArgumentException 身份证号不合法
     */
    private static IdCode parseAndValidate(String id, boolean strict) {
        IdCode code = CodeParser.parse(id);
        if (!Validator.validate(code, strict)) {
            throw new IllegalArgumentException(IdValidatorMessages.NOT_VALID_ID + id);
        }
        return code;
    }

    /**
     * 获取完整身份证信息。
     *
     * @param id     身份证号
     * @param strict 是否严格模式
     * @return IdInfo 对象
     * @throws IllegalArgumentException 身份证号不合法
     */
    public static IdInfo getInfo(String id, boolean strict) {
        IdCode code = parseAndValidate(id, strict);

        List<String> addressInfo =
                AddressResolver.getAddressInfo(code.getAddressCode(), code.getBirthdayCode(), strict);
        String province = addressInfo.get(0);
        String city = addressInfo.get(1);
        String district = addressInfo.get(2);

        int abandoned =
                DataProvider.getInstance().getAddressName(code.getAddressCode()).isEmpty() ? 1 : 0;

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
                code.getCheckBit());
    }

    /**
     * 获取基础身份证信息（不含地址）。
     *
     * @param id 身份证号
     * @return BasicIdInfo 对象
     * @throws IllegalArgumentException 身份证号不合法
     */
    public static BasicIdInfo getBasicInfo(String id) {
        IdCode code = parseAndValidateBasic(id);

        LocalDate birthday = LocalDate.parse(code.getBirthdayCode(), DATE_FORMATTER);
        int sexCode = Integer.parseInt(code.getOrderCode());
        Sex sex = Sex.fromOrderDigit(sexCode % 10);

        return new BasicIdInfo(
                birthday,
                ConstellationCalculator.calculate(code.getBirthdayCode()),
                ChineseZodiacCalculator.calculate(code.getBirthdayCode()),
                sex,
                code.getType(),
                code.getCheckBit());
    }

    /**
     * 解析并基础校验身份证号，返回 {@link IdCode} 对象。
     *
     * <p>内部方法，供 {@link #isValidBasic} 和 {@link #getBasicInfo} 共用，避免重复解析。
     *
     * @param id 身份证号
     * @return 解析后的 {@link IdCode}
     * @throws IllegalArgumentException 身份证号不合法
     */
    private static IdCode parseAndValidateBasic(String id) {
        IdCode code = CodeParser.parse(id);
        if (!Validator.checkOrderCode(code.getOrderCode())) {
            throw new IllegalArgumentException(IdValidatorMessages.NOT_VALID_ID + id);
        }
        if (!Validator.checkBirthdayCode(code.getBirthdayCode())) {
            throw new IllegalArgumentException(IdValidatorMessages.NOT_VALID_ID + id);
        }
        if (code.getType() != IdType.FIFTEEN && !CheckBitCalculator.verify(code.getBody() + code.getCheckBit())) {
            throw new IllegalArgumentException(IdValidatorMessages.NOT_VALID_ID + id);
        }
        return code;
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
        IdCode code = parseAndValidate(id, true);
        if (code.getType() != IdType.FIFTEEN) {
            throw new IllegalArgumentException(IdValidatorMessages.ID_MUST_BE_15_DIGITS + id.length());
        }

        String body = String.format("%06d", code.getAddressCode()) + code.getBirthdayCode() + code.getOrderCode();

        return body + CheckBitCalculator.calculate(body);
    }

    // ==================== 初始化 API ====================

    /**
     * 预加载内置数据（地址码 + 时间线）。
     *
     * <p>幂等操作：重复调用不会重新加载或重建索引。
     * 适用于应用启动阶段主动准备数据，避免首次业务调用时的延迟。
     */
    public static void preload() {
        DataProvider.getInstance().preloadTimeline();
    }

    /**
     * 一次性安装自定义数据源及合并策略。
     *
     * <p>原子发布：数据源和合并策略作为不可变配置对整体发布，
     * 保证多线程不会看到半发布状态。成功后再次调用抛出
     * {@link IllegalStateException}，不会静默覆盖已有配置。
     *
     * <p><b>生命周期约束</b>：本方法应在应用启动阶段、业务线程开始校验前完成调用。
     * 库不支持业务调用过程中切换配置——单次校验内的多次查询不会获取配置快照，
     * 若校验进行时恰好发生一次性初始化（{@code null → 已安装}），
     * 可能混合内置与自定义两套数据。业务线程开始后请勿再调用本方法。
     *
     * @param source    自定义数据源实现，不允许 null
     * @param mergeMode 合并策略，不允许 null
     * @throws IllegalStateException    已成功初始化后再次调用
     * @throws IllegalArgumentException source 或 mergeMode 为 null
     */
    public static void initialize(CustomDataSource source, MergeMode mergeMode) {
        DataProvider.getInstance().initialize(source, mergeMode);
    }
}
