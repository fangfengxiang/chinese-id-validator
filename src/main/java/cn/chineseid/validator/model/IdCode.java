package cn.chineseid.validator.model;

import java.util.Objects;

/**
 * 身份证号解析结果（不可变）。
 * 将 15 位或 18 位身份证号拆分为：地址码、生日码、顺序码、校验码。
 */
public final class IdCode {
    /** 完整 body（不含校验码的前 N-1 位） */
    private final String body;
    /** 地址码（前 6 位） */
    private final int addressCode;
    /** 生日码（8 位，15 位身份证补 "19" 前缀） */
    private final String birthdayCode;
    /** 顺序码（3 位） */
    private final String orderCode;
    /** 校验码（18 位身份证有，15 位为空字符串） */
    private final String checkBit;
    /** 身份证类型 */
    private final IdType type;

    public IdCode(String body, int addressCode, String birthdayCode, String orderCode, String checkBit, IdType type) {
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.addressCode = addressCode;
        this.birthdayCode = Objects.requireNonNull(birthdayCode, "birthdayCode must not be null");
        this.orderCode = Objects.requireNonNull(orderCode, "orderCode must not be null");
        this.checkBit = checkBit == null ? "" : checkBit;
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public String getBody() {
        return body;
    }

    public int getAddressCode() {
        return addressCode;
    }

    public String getBirthdayCode() {
        return birthdayCode;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getCheckBit() {
        return checkBit;
    }

    public IdType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "IdCode{body='" + body + "', addressCode=" + addressCode
                + ", birthdayCode='" + birthdayCode + "', orderCode='" + orderCode
                + "', checkBit='" + checkBit + "', type=" + type + "}";
    }
}
