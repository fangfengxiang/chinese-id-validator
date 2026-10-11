package cn.chineseid.validator.data;

import cn.chineseid.validator.model.ConstellationData;
import cn.chineseid.validator.model.TimelineEntry;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 数据提供者（Singleton，懒加载）。
 * 从 JAR 内嵌资源加载行政区划代码、时间线、星座、生肖数据。
 *
 * 加载策略：
 * - 生肖/星座：编译期常量，零 I/O
 * - 地址码：.properties 文件，初始化时加载
 * - 时间线：.bin 二进制文件，首次查询时懒加载（Holder 模式）
 */
public final class DataProvider {

    private static final String RESOURCE_DIR = "/data/";
    private static final String ADDRESS_CODE_FILE = RESOURCE_DIR + "address_code.properties";
    private static final String TIMELINE_FILE = RESOURCE_DIR + "address_code_timeline.bin";

    /** 十二生肖（编译期常量，零 I/O） */
    private static final String[] CHINESE_ZODIACS = {
        "子鼠", "丑牛", "寅虎", "卯兔", "辰龙", "巳蛇",
        "午马", "未羊", "申猴", "酉鸡", "戌狗", "亥猪"
    };

    /** 十二星座（编译期常量，零 I/O） */
    private static final List<ConstellationData> CONSTELLATIONS = List.of(
        new ConstellationData("水瓶座", "01-20", "02-18"),
        new ConstellationData("双鱼座", "02-19", "03-20"),
        new ConstellationData("白羊座", "03-21", "04-19"),
        new ConstellationData("金牛座", "04-20", "05-20"),
        new ConstellationData("双子座", "05-21", "06-21"),
        new ConstellationData("巨蟹座", "06-22", "07-22"),
        new ConstellationData("狮子座", "07-23", "08-22"),
        new ConstellationData("处女座", "08-23", "09-22"),
        new ConstellationData("天秤座", "09-23", "10-23"),
        new ConstellationData("天蝎座", "10-24", "11-22"),
        new ConstellationData("射手座", "11-23", "12-21"),
        new ConstellationData("摩羯座", "12-22", "01-19")
    );

    /** 地址码 → 名称映射 */
    private final Map<Integer, String> addressCodeMap;

    /** 自定义数据源（volatile 保证多线程可见性，默认空实现） */
    private volatile CustomDataSource customDataSource = NoopCustomDataSource.INSTANCE;

    private DataProvider() {
        this.addressCodeMap = loadAddressCodeMap();
    }

    public static DataProvider getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        static final DataProvider INSTANCE = new DataProvider();
    }

    /** 时间线懒加载 Holder（首次 getTimeline 调用时触发） */
    private static final class TimelineHolder {
        static final Map<Integer, List<TimelineEntry>> TIMELINE = loadTimelineMap();
    }

    // ==================== 自定义数据源 ====================

    /**
     * 注入自定义数据源。
     *
     * @param dataSource 自定义数据源实现，null 恢复为 Noop
     */
    public void setCustomDataSource(CustomDataSource dataSource) {
        this.customDataSource = (dataSource == null) ? NoopCustomDataSource.INSTANCE : dataSource;
    }

    /**
     * 清除自定义数据源，恢复为 Noop（仅内置数据）。
     */
    public void clearCustomDataSource() {
        this.customDataSource = NoopCustomDataSource.INSTANCE;
    }

    // ==================== 查询方法 ====================

    /**
     * 根据地址码获取行政区划名称（内置 + 自定义合并）。
     *
     * <p>合并策略由 {@link CustomDataSource#getMergeMode()} 决定：
     * <ul>
     *   <li>{@link MergeMode#FALLBACK} — 内置有数据则不查自定义源
     *   <li>{@link MergeMode#MERGE}    — 总是查自定义源，自定义非 null 则优先
     * </ul>
     */
    public String getAddressName(int code) {
        String builtIn = addressCodeMap.getOrDefault(code, "");
        MergeMode mode = customDataSource.getMergeMode();

        if (mode == MergeMode.FALLBACK && !builtIn.isEmpty()) {
            return builtIn;
        }
        String custom = customDataSource.getAddressName(code);
        return (custom != null) ? custom : builtIn;
    }

    /**
     * 根据地址码获取时间线条目列表（内置 + 自定义合并）。
     *
     * <p>合并策略由 {@link CustomDataSource#getMergeMode()} 决定：
     * <ul>
     *   <li>{@link MergeMode#FALLBACK} — 内置有数据则不查自定义源
     *   <li>{@link MergeMode#MERGE}    — 总是查自定义源，合并后自定义条目排在前面优先匹配
     * </ul>
     */
    public List<TimelineEntry> getTimeline(int code) {
        List<TimelineEntry> builtIn = TimelineHolder.TIMELINE.getOrDefault(code, Collections.emptyList());
        MergeMode mode = customDataSource.getMergeMode();

        if (mode == MergeMode.FALLBACK && !builtIn.isEmpty()) {
            return builtIn;
        }
        List<TimelineEntry> custom = customDataSource.getTimeline(code);
        if (custom == null || custom.isEmpty()) {
            return builtIn;
        }
        if (builtIn.isEmpty()) {
            return custom;
        }
        // MERGE：自定义在前，内置在后
        List<TimelineEntry> merged = new ArrayList<>(custom.size() + builtIn.size());
        merged.addAll(custom);
        merged.addAll(builtIn);
        return Collections.unmodifiableList(merged);
    }

    /**
     * 获取所有地址码（用于假号生成中的随机选择）。
     */
    public Map<Integer, String> getAllAddressCodes() {
        return Collections.unmodifiableMap(addressCodeMap);
    }

    /**
     * 获取星座数据列表。
     */
    public List<ConstellationData> getConstellations() {
        return CONSTELLATIONS;
    }

    /**
     * 根据索引获取生肖名称（0=子鼠, 1=丑牛, ..., 11=亥猪）。
     */
    public String getChineseZodiac(int index) {
        if (index < 0 || index >= CHINESE_ZODIACS.length) {
            return "";
        }
        return CHINESE_ZODIACS[index];
    }

    /**
     * 获取生肖数据数组。
     */
    public String[] getAllChineseZodiacs() {
        return CHINESE_ZODIACS.clone();
    }

    // ==================== 数据加载 ====================

    /**
     * 从 .properties 文件加载行政区划代码。
     */
    private static Map<Integer, String> loadAddressCodeMap() {
        Properties props = new Properties();
        InputStream is = DataProvider.class.getResourceAsStream(ADDRESS_CODE_FILE);
        if (is == null) {
            throw new IllegalStateException("Resource not found: " + ADDRESS_CODE_FILE);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            props.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resource: " + ADDRESS_CODE_FILE, e);
        }
        Map<Integer, String> result = new HashMap<>(props.size());
        props.forEach((k, v) -> result.put(Integer.parseInt((String) k), (String) v));
        return Collections.unmodifiableMap(result);
    }

    /**
     * 从 .bin 二进制文件加载时间线（懒加载，首次查询时触发）。
     *
     * 二进制格式（兼容 DataOutputStream/DataInputStream）：
     * mapSize(int) + [code(int) + entryCount(int) + [address(UTF) + startYear(UTF) + endYear(UTF)]*]*
     */
    private static Map<Integer, List<TimelineEntry>> loadTimelineMap() {
        InputStream is = DataProvider.class.getResourceAsStream(TIMELINE_FILE);
        if (is == null) {
            throw new IllegalStateException("Resource not found: " + TIMELINE_FILE);
        }
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(is))) {
            int mapSize = dis.readInt();
            Map<Integer, List<TimelineEntry>> result = new HashMap<>(mapSize);
            for (int i = 0; i < mapSize; i++) {
                int code = dis.readInt();
                int listSize = dis.readInt();
                List<TimelineEntry> entries = new ArrayList<>(listSize);
                for (int j = 0; j < listSize; j++) {
                    String address = dis.readUTF();
                    String startYear = dis.readUTF();
                    String endYear = dis.readUTF();
                    entries.add(new TimelineEntry(address, startYear, endYear));
                }
                result.put(code, Collections.unmodifiableList(entries));
            }
            return Collections.unmodifiableMap(result);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resource: " + TIMELINE_FILE, e);
        }
    }
}
