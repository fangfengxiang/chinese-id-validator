package cn.chineseid.validator.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 身份证完整信息（不可变 DTO）。
 */
public final class IdInfo {
    /** 地址码（6 位行政区划代码） */
    private final int addressCode;
    /** 地址码是否已废弃（1=已废弃, 0=使用中） */
    private final int abandoned;
    /** 地址全称（省+市+区拼接） */
    private final String address;
    /** 地址树（[省, 市, 区]） */
    private final List<String> addressTree;
    /** 出生日期 */
    private final LocalDate birthday;
    /** 星座 */
    private final String constellation;
    /** 生肖 */
    private final String chineseZodiac;
    /** 性别 */
    private final Sex sex;
    /** 身份证类型（15 位或 18 位） */
    private final IdType length;
    /** 校验码 */
    private final String checkBit;

    public IdInfo(
            int addressCode,
            int abandoned,
            String address,
            List<String> addressTree,
            LocalDate birthday,
            String constellation,
            String chineseZodiac,
            Sex sex,
            IdType length,
            String checkBit) {
        this.addressCode = addressCode;
        this.abandoned = abandoned;
        this.address = address;
        this.addressTree =
                Collections.unmodifiableList(Objects.requireNonNull(addressTree, "addressTree must not be null"));
        this.birthday = birthday;
        this.constellation = constellation;
        this.chineseZodiac = chineseZodiac;
        this.sex = Objects.requireNonNull(sex, "sex must not be null");
        this.length = Objects.requireNonNull(length, "length must not be null");
        this.checkBit = checkBit == null ? "" : checkBit;
    }

    public int getAddressCode() {
        return addressCode;
    }

    public int getAbandoned() {
        return abandoned;
    }

    public String getAddress() {
        return address;
    }

    public List<String> getAddressTree() {
        return addressTree;
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
        return "IdInfo{addressCode=" + addressCode
                + ", abandoned=" + abandoned
                + ", address='" + address + "'"
                + ", addressTree=" + addressTree
                + ", birthday=" + birthday
                + ", constellation='" + constellation + "'"
                + ", chineseZodiac='" + chineseZodiac + "'"
                + ", sex=" + sex
                + ", length=" + length
                + ", checkBit='" + checkBit + "'}";
    }
}
