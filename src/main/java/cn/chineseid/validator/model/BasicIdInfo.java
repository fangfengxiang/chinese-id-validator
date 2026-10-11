package cn.chineseid.validator.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 基础身份证信息（不含地址信息，不可变 DTO）。
 */
public final class BasicIdInfo {
    private final LocalDate birthday;
    private final String constellation;
    private final String chineseZodiac;
    private final Sex sex;
    private final IdType length;
    private final String checkBit;

    public BasicIdInfo(LocalDate birthday, String constellation,
                       String chineseZodiac, Sex sex,
                       IdType length, String checkBit) {
        this.birthday = birthday;
        this.constellation = constellation;
        this.chineseZodiac = chineseZodiac;
        this.sex = Objects.requireNonNull(sex, "sex must not be null");
        this.length = Objects.requireNonNull(length, "length must not be null");
        this.checkBit = checkBit == null ? "" : checkBit;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public String getConstellation() {
        return constellation;
    }

    public String getChineseZodiac() {
        return chineseZodiac;
    }

    public Sex getSex() {
        return sex;
    }

    public IdType getLength() {
        return length;
    }

    public String getCheckBit() {
        return checkBit;
    }

    @Override
    public String toString() {
        return "BasicIdInfo{birthday=" + birthday
                + ", constellation='" + constellation + "'"
                + ", chineseZodiac='" + chineseZodiac + "'"
                + ", sex=" + sex
                + ", length=" + length
                + ", checkBit='" + checkBit + "'}";
    }
}
