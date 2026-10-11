package cn.chineseid.validator.generator;

import cn.chineseid.validator.data.DataProvider;
import cn.chineseid.validator.model.AddressClassification;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 地址码生成器。
 *
 * 使用字符串前缀匹配 + ThreadLocalRandom（线程安全，无需手动播种）。
 */
public final class AddressCodeGenerator {

    private AddressCodeGenerator() {}

    /**
     * 根据地址名称生成地址码。
     *
     * @param address 地址名称（如 "广东省"、"北京市"、"朝阳区"），空字符串表示全国随机
     * @return 6 位地址码字符串
     */
    public static String generate(String address) {
        // 1. 按名称查找地址码
        String addressCode = "";
        if (address != null && !address.isEmpty()) {
            for (Map.Entry<Integer, String> entry :
                    DataProvider.getInstance().getAllAddressCodes().entrySet()) {
                if (address.equals(entry.getValue())) {
                    addressCode = String.format("%06d", entry.getKey());
                    break;
                }
            }
        }

        // 2. 分类
        AddressClassification classification = classify(addressCode);

        // 3. 根据分类生成随机县级地址码
        switch (classification) {
            case COUNTRY:
                return getRandomDistrictCode("");
            case PROVINCE:
                return getRandomDistrictCode(addressCode.substring(0, 2));
            case CITY:
                return getRandomDistrictCode(addressCode.substring(0, 4));
            case SPECIAL:
            case DISTRICT:
            default:
                return addressCode;
        }
    }

    /**
     * 地址码分类。
     */
    public static AddressClassification classify(String addressCode) {
        if (addressCode == null || addressCode.isEmpty()) {
            return AddressClassification.COUNTRY;
        }
        if (addressCode.charAt(0) == '8') {
            return AddressClassification.SPECIAL;
        }
        if (addressCode.length() >= 6 && "0000".equals(addressCode.substring(2, 6))) {
            return AddressClassification.PROVINCE;
        }
        if (addressCode.length() >= 6 && "00".equals(addressCode.substring(4, 6))) {
            return AddressClassification.CITY;
        }
        return AddressClassification.DISTRICT;
    }

    /**
     * 获取随机县级地址码。只返回后 2 位不为 "00" 的地址码（排除省/市级汇总码）。
     *
     * @param prefix 前缀（2 位=省级范围, 4 位=市级范围, 空=全国范围）
     * @return 随机地址码字符串
     */
    private static String getRandomDistrictCode(String prefix) {
        List<String> candidates = new ArrayList<>();
        for (Map.Entry<Integer, String> entry :
                DataProvider.getInstance().getAllAddressCodes().entrySet()) {
            String keyStr = String.format("%06d", entry.getKey());
            if (!prefix.isEmpty() && !keyStr.startsWith(prefix)) {
                continue;
            }
            if ("00".equals(keyStr.substring(4, 6))) {
                continue;
            }
            candidates.add(keyStr);
        }
        if (candidates.isEmpty()) {
            return prefix + "01";
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }
}
