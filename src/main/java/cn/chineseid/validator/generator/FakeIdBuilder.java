package cn.chineseid.validator.generator;

import cn.chineseid.validator.model.Sex;

/**
 * 假身份证号生成参数构建器（Builder 模式）。
 *
 * 使用 Builder 模式提供清晰的 API 和默认值。
 *
 * 用法：
 * <pre>{@code
 * String fakeId = IdValidator.fakeId(new FakeIdBuilder()
 *     .eighteen()
 *     .address("江苏省")
 *     .birthday("200001")
 *     .sex(Sex.MALE));
 * }</pre>
 */
public final class FakeIdBuilder {

    private boolean eighteen = true;
    private String address = "";
    private String birthday = "";
    private Sex sex = Sex.FEMALE;

    /**
     * 生成 18 位身份证号（默认）。
     */
    public FakeIdBuilder eighteen() {
        this.eighteen = true;
        return this;
    }

    /**
     * 生成 15 位身份证号。
     */
    public FakeIdBuilder fifteen() {
        this.eighteen = false;
        return this;
    }

    /**
     * 设置地址（省/市/区官方全称，如 "江苏省"、"北京市"、"朝阳区"）。
     * 空字符串表示全国随机。
     */
    public FakeIdBuilder address(String address) {
        this.address = address == null ? "" : address;
        return this;
    }

    /**
     * 设置出生日期（如 "2000"、"198801"、"19990101"）。
     * 空字符串表示随机。
     */
    public FakeIdBuilder birthday(String birthday) {
        this.birthday = birthday == null ? "" : birthday;
        return this;
    }

    /**
     * 设置性别。
     */
    public FakeIdBuilder sex(Sex sex) {
        this.sex = sex == null ? Sex.FEMALE : sex;
        return this;
    }

    public boolean isEighteen() {
        return eighteen;
    }

    public String getAddress() {
        return address;
    }

    public String getBirthday() {
        return birthday;
    }

    public Sex getSex() {
        return sex;
    }
}
