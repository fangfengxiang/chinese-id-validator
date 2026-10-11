package cn.chineseid.validator.data;

import cn.chineseid.validator.model.TimelineEntry;
import java.util.List;

/**
 * 自定义数据源 SPI（Service Provider Interface）。
 *
 * <p>用户实现此接口，通过 {@link cn.chineseid.validator.IdValidator#initialize} 一次性注入，
 * 补充或覆盖内置行政区划数据。合并策略由 {@code initialize} 的 {@link MergeMode} 参数指定，
 * 不属于数据源自身的职责。
 *
 * <h3>空值约定</h3>
 * <ul>
 *   <li>{@link #getTimeline(int)} 返回非 {@code null} 的空列表 = 该地址码无自定义时间线数据
 *   <li>{@link #getAddressName(int)} 返回 {@code null} = 该地址码无自定义地址名称
 * </ul>
 *
 * <h3>异常约定</h3>
 * <p>实现类在查询失败时应抛出 {@link RuntimeException}，不得返回空值伪装成无数据。
 * 库会将异常包装为 {@link cn.chineseid.validator.IdValidatorException} 向上传播。
 *
 * <h3>线程安全</h3>
 * <p>实现类自行保证线程安全。库不同步对数据源的访问。
 */
public interface CustomDataSource {

    /**
     * 获取自定义时间线条目。
     *
     * @param code 行政区划代码
     * @return 时间线条目列表，非 {@code null}；空列表表示无自定义数据
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
