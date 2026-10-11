package cn.chineseid.validator.util;

/**
 * 字符串工具类。
 */
public final class StringUtil {

    private StringUtil() {
    }

    /**
     * 字符串截取（start 和 end 均为位置索引，非长度）。
     *
     * @param source 源字符串
     * @param start  起始位置（包含）
     * @param end    结束位置（不包含）
     * @return 截取的子串
     */
    public static String substr(String source, int start, int end) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        if (start < 0) {
            start = 0;
        }
        if (end > source.length()) {
            end = source.length();
        }
        if (start >= end) {
            return "";
        }
        return source.substring(start, end);
    }

    /**
     * 数字补零到指定位数。
     *
     * @param value  原始值
     * @param length 目标长度
     * @return 补零后的字符串
     */
    public static String padLeft(String value, int length) {
        if (value == null) {
            value = "";
        }
        if (value.length() >= length) {
            return value;
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = value.length(); i < length; i++) {
            sb.append('0');
        }
        sb.append(value);
        return sb.toString();
    }

    /**
     * 判断字符串是否为纯数字。
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
