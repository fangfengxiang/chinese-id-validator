package cn.chineseid.validator.model;

/**
 * 地址码分类枚举。
 */
public enum AddressClassification {
    /** 国家级（地址码为 0） */
    COUNTRY,
    /** 特别行政区/港澳台（8 开头） */
    SPECIAL,
    /** 省级（后 4 位为 0） */
    PROVINCE,
    /** 市级（后 2 位为 0） */
    CITY,
    /** 区县级（其他） */
    DISTRICT
}
