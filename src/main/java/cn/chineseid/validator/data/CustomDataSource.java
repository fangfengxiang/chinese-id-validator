package cn.chineseid.validator.data;

import cn.chineseid.validator.model.TimelineEntry;

import java.util.List;

/**
 * 自定义数据源 SPI（Service Provider Interface）。
 *
 * <p>用户实现此接口，通过 {@link cn.chineseid.validator.IdValidator#setCustomDataSource} 注入，
 * 补充或覆盖内置行政区划数据。初始化在实现类的构造函数中完成，无需显式 {@code init()} 方法
 * （与 JDBC {@code Driver}、SLF4J {@code ILoggerFactory} 等 Java 标准 SPI 一致）。
 *
 * <h3>合并策略</h3>
 * <ul>
 *   <li>{@link MergeMode#FALLBACK}（默认）— 内置有数据则不调用本接口，仅填充缺失
 *   <li>{@link MergeMode#MERGE} — 总是调用本接口，与内置合并，自定义条目优先匹配
 * </ul>
 *
 * <h3>空值约定</h3>
 * <ul>
 *   <li>{@link #getTimeline(int)} 返回空列表 = 该地址码无自定义时间线数据
 *   <li>{@link #getAddressName(int)} 返回 {@code null} = 该地址码无自定义地址名称
 * </ul>
 */
public interface CustomDataSource {

    /**
     * 合并策略，默认 {@link MergeMode#FALLBACK}。
     *
     * <p>实现类可 override 此方法以切换策略。例如需自定义优先覆盖内置则返回 {@link MergeMode#MERGE}。
     *
     * @return 合并策略
     */
    default MergeMode getMergeMode() {
        return MergeMode.FALLBACK;
    }

    /**
     * 获取自定义时间线条目。
     *
     * @param code 行政区划代码
     * @return 时间线条目列表，空列表表示无自定义数据
     */
    List<TimelineEntry> getTimeline(int code);

    /**
     * 获取自定义地址名称。
     *
     * @param code 行政区划代码
     * @return 地址名称，{@code null} 表示无自定义数据
     */
    String getAddressName(int code);
}
