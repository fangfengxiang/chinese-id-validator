package cn.chineseid.validator.generator;

import cn.chineseid.validator.core.CheckBitCalculator;
import cn.chineseid.validator.data.DataProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 假身份证号生成器。
 */
public final class IdGenerator {

    /** 15 位身份证号支持的年份范围 [1900, 1999]（升级时补 "19" 前缀） */
    private static final int FIFTEEN_BIT_YEAR_MIN = 1900;

    private static final int FIFTEEN_BIT_YEAR_MAX = 1999;
    /** 随机年份替换范围下限 */
    private static final int RANDOM_YEAR_MIN = 1950;

    private IdGenerator() {}

    /**
     * 生成随机假身份证号（18 位）。
     */
    public static String fakeId() {
        return fakeRequireId(new FakeIdBuilder());
    }

    /**
     * 按要求生成假身份证号。
     *
     * @param builder 构建参数
     * @return 假身份证号字符串
     */
    public static String fakeRequireId(FakeIdBuilder builder) {
        // 1. 生成地址码
        String addressCode;
        String address;

        if (builder.getAddress().isEmpty()) {
            // 随机选取一个地址码及其名称
            List<Map.Entry<Integer, String>> entries = new ArrayList<>(
                    DataProvider.getInstance().getAllAddressCodes().entrySet());
            Map.Entry<Integer, String> entry =
                    entries.get(ThreadLocalRandom.current().nextInt(entries.size()));
            addressCode = String.format("%06d", entry.getKey());
            address = entry.getValue();
        } else {
            addressCode = AddressCodeGenerator.generate(builder.getAddress());
            address = builder.getAddress();
        }

        // 2. 生成生日码
        String birthdayCode = BirthdayCodeGenerator.generate(addressCode, address, builder.getBirthday());

        // 3. 生成顺序码
        String orderCode = OrderCodeGenerator.generate(builder.getSex());

        // 4. 组装
        if (!builder.isEighteen()) {
            // 15 位身份证号只支持 1900-1999 年（升级时补 "19" 前缀）
            // 年份超出此范围的替换为 [1950, 1999] 范围内的随机年份
            int year = Integer.parseInt(birthdayCode.substring(0, 4));
            if (year < FIFTEEN_BIT_YEAR_MIN || year > FIFTEEN_BIT_YEAR_MAX) {
                year = ThreadLocalRandom.current().nextInt(FIFTEEN_BIT_YEAR_MAX - RANDOM_YEAR_MIN + 1)
                        + RANDOM_YEAR_MIN;
                birthdayCode = year + birthdayCode.substring(4);
            }
            // 去掉生日码前 2 位 "19"
            return addressCode + birthdayCode.substring(2) + orderCode;
        }

        // 18 位：body + 校验码
        String body = addressCode + birthdayCode + orderCode;
        return body + CheckBitCalculator.calculate(body);
    }
}
