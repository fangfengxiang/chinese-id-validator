package cn.chineseid.validator.data;

import cn.chineseid.validator.IdValidatorMessages;
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
import java.util.Arrays;
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
 *
 * 初始化生命周期：
 * - 内置数据通过 JVM 类初始化（Holder 模式）保证线程安全和一次性加载
 * - 自定义数据源通过 {@link #initialize(CustomDataSource, MergeMode)} 一次性原子发布
 * - 初始化成功后再次调用 {@link #initialize} 抛出 {@link IllegalStateException}
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
    private static final List<ConstellationData> CONSTELLATIONS = Collections.unmodifiableList(Arrays.asList(
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
            new ConstellationData("摩羯座", "12-22", "01-19")));

    /** 地址码 → 名称映射 */
    private final Map<Integer, String> addressCodeMap;

    /** 缓存的地址码 entry 列表（不可变，供假号生成器随机选择复用） */
    private final List<Map.Entry<Integer, String>> cachedEntries;

    /**
     * 自定义数据源配置（volatile 保证多线程可见性）。
     * null 表示尚未安装自定义数据源。
     */
    private volatile Configuration config;

    /**
     * 标记是否已成功初始化自定义配置。
     * 仅在 {@link #initialize} 的 synchronized 块内读写，synchronized 已保证可见性。
     */
    private boolean initialized = false;

    private DataProvider() {
        this.addressCodeMap = loadAddressCodeMap();
        this.cachedEntries = Collections.unmodifiableList(new ArrayList<>(this.addressCodeMap.entrySet()));
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

    /**
     * 不可变配置对 — 自定义数据源 + 合并策略。
     * 通过 volatile 引用原子发布，保证多线程看不到半发布状态。
     */
    private static final class Configuration {
        final CustomDataSource source;
        final MergeMode mergeMode;

        Configuration(CustomDataSource source, MergeMode mergeMode) {
            this.source = source;
            this.mergeMode = mergeMode;
        }
    }

    // ==================== 初始化 ====================

    /**
     * 一次性安装自定义数据源及合并策略。
     *
     * <p>原子发布：source 和 mergeMode 作为不可变 Configuration 对整体发布。
     * 成功后再次调用抛出 {@link IllegalStateException}。
     *
     * @param source    自定义数据源，不允许 null
     * @param mergeMode 合并策略，不允许 null
     * @throws IllegalStateException     已成功初始化后再次调用
     * @throws IllegalArgumentException  source 或 mergeMode 为 null
     */
    public synchronized void initialize(CustomDataSource source, MergeMode mergeMode) {
        if (source == null) {
            throw new IllegalArgumentException(IdValidatorMessages.NULL_DATA_SOURCE);
        }
        if (mergeMode == null) {
            throw new IllegalArgumentException(IdValidatorMessages.NULL_MERGE_MODE);
        }
        if (initialized) {
            throw new IllegalStateException(IdValidatorMessages.DUPLICATE_INITIALIZATION);
        }
        this.config = new Configuration(source, mergeMode);
        this.initialized = true;
    }

    /**
     * 显式预加载内置时间线数据（幂等）。
     *
     * <p>触发 {@link TimelineHolder} 类初始化，加载 .bin 二进制时间线数据。
     * 重复调用不会重复加载（JVM 类初始化保证）。
     */
    public void preloadTimeline() {
        // 访问 TimelineHolder.TIMELINE 触发 JVM 类初始化（线程安全、一次性）
        int size = TimelineHolder.TIMELINE.size();
        // 防止 JIT 优化掉此次访问
        if (size < 0) {
            throw new AssertionError("Timeline size cannot be negative");
        }
    }

    // ==================== 查询方法 ====================

    /**
     * 根据地址码获取行政区划名称（内置 + 自定义合并）。
     *
     * <p>合并策略由 {@link #initialize} 安装的 {@link MergeMode} 决定：
     * <ul>
     *   <li>{@link MergeMode#FALLBACK} — 内置有数据则不查自定义源
     *   <li>{@link MergeMode#MERGE}    — 总是查自定义源，自定义非 null 则优先
     * </ul>
     * <p>未安装自定义数据源时，直接返回内置数据。
     *
     * @throws cn.chineseid.validator.IdValidatorException 自定义数据源查询失败时包装原始异常
     */
    public String getAddressName(int code) {
        String builtIn = addressCodeMap.getOrDefault(code, "");
        Configuration cfg = this.config;
        if (cfg == null) {
            return builtIn;
        }

        if (cfg.mergeMode == MergeMode.FALLBACK && !builtIn.isEmpty()) {
            return builtIn;
        }
        try {
            String custom = cfg.source.getAddressName(code);
            return (custom != null) ? custom : builtIn;
        } catch (RuntimeException e) {
            throw new cn.chineseid.validator.IdValidatorException(
                    IdValidatorMessages.DATA_SOURCE_QUERY_FAILED + code, e);
        }
    }

    /**
     * 根据地址码获取时间线条目列表（内置 + 自定义合并）。
     *
     * <p>合并策略由 {@link #initialize} 安装的 {@link MergeMode} 决定：
     * <ul>
     *   <li>{@link MergeMode#FALLBACK} — 内置有数据则不查自定义源
     *   <li>{@link MergeMode#MERGE}    — 总是查自定义源，合并后自定义条目排在前面优先匹配
     * </ul>
     * <p>未安装自定义数据源时，直接返回内置数据。
     *
     * @throws cn.chineseid.validator.IdValidatorException 自定义数据源查询失败时包装原始异常
     */
    public List<TimelineEntry> getTimeline(int code) {
        List<TimelineEntry> builtIn = TimelineHolder.TIMELINE.getOrDefault(code, Collections.emptyList());
        Configuration cfg = this.config;
        if (cfg == null) {
            return builtIn;
        }

        if (cfg.mergeMode == MergeMode.FALLBACK && !builtIn.isEmpty()) {
            return builtIn;
        }
        try {
            List<TimelineEntry> custom = cfg.source.getTimeline(code);
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
        } catch (RuntimeException e) {
            throw new cn.chineseid.validator.IdValidatorException(
                    IdValidatorMessages.DATA_SOURCE_QUERY_FAILED + code, e);
        }
    }

    /**
     * 获取所有地址码（用于假号生成中的随机选择）。
     */
    public Map<Integer, String> getAllAddressCodes() {
        return Collections.unmodifiableMap(addressCodeMap);
    }

    /**
     * 获取缓存的地址码 entry 列表（不可变，供假号生成器随机选择复用）。
     *
     * <p>列表在 DataProvider 构造时创建一次，后续所有调用复用同一实例，
     * 避免每次 {@code fakeRequireId} 都 {@code new ArrayList<>(entrySet())}。
     */
    public List<Map.Entry<Integer, String>> getAddressCodeEntries() {
        return cachedEntries;
    }

    /**
     * 获取星座数据列表（防御性拷贝，调用方改动不影响内部数据）。
     */
    public List<ConstellationData> getConstellations() {
        return new ArrayList<>(CONSTELLATIONS);
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
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
