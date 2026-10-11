package cn.chineseid.validator.core;

import cn.chineseid.validator.model.IdCode;
import cn.chineseid.validator.model.IdType;

import java.util.regex.Pattern;

/**
 * 身份证号解析器。
 *
 * 使用 static final Pattern 预编译正则，全生命周期复用。
 */
public final class CodeParser {

    /** 15 位身份证正则：地址码(6) + 生日码(6) + 顺序码(3) — 仅匹配数字 */
    private static final Pattern SHORT_PATTERN = Pattern.compile("(\\d{6})(\\d{6})(\\d{3})");

    /** 18 位身份证正则：(地址码(6) + 生日码(8) + 顺序码(3))(校验码(1)) — 校验码支持 0-9/X/x */
    private static final Pattern LONG_PATTERN = Pattern.compile("((\\d{6})(\\d{8})(\\d{3}))([0-9Xx])");

    private CodeParser() {
    }

    /**
     * 解析身份证号。
     *
     * @param id 15 位或 18 位身份证号
     * @return 解析结果 IdCode
     * @throws IllegalArgumentException 身份证号长度不合法
     */
    public static IdCode parse(String id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        String normalized = id.trim().toLowerCase();
        int length = normalized.length();
        if (length == 15) {
            return parse15(normalized);
        }
        if (length == 18) {
            return parse18(normalized);
        }
        throw new IllegalArgumentException("Invalid ID card number length: " + length);
    }

    /**
     * 解析 15 位身份证号。生日码前补 "19" 前缀。
     */
    private static IdCode parse15(String id) {
        var matcher = SHORT_PATTERN.matcher(id);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid 15-digit ID format: " + id);
        }
        String addressCode = matcher.group(1);
        String birthdayCode = "19" + matcher.group(2);
        String orderCode = matcher.group(3);
        String body = matcher.group(0);

        return new IdCode(
                body,
                Integer.parseInt(addressCode),
                birthdayCode,
                orderCode,
                "",
                IdType.FIFTEEN
        );
    }

    /**
     * 解析 18 位身份证号。
     */
    private static IdCode parse18(String id) {
        var matcher = LONG_PATTERN.matcher(id);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid 18-digit ID format: " + id);
        }
        String body = matcher.group(1);
        String addressCode = matcher.group(2);
        String birthdayCode = matcher.group(3);
        String orderCode = matcher.group(4);
        String checkBit = matcher.group(5).toUpperCase();

        return new IdCode(
                body,
                Integer.parseInt(addressCode),
                birthdayCode,
                orderCode,
                checkBit,
                IdType.EIGHTEEN
        );
    }
}
