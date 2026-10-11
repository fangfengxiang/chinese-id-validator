package cn.chineseid.validator.data;

import cn.chineseid.validator.model.TimelineEntry;
import java.util.Collections;
import java.util.List;

/**
 * 空实现（Null Object 模式）。
 *
 * <p>所有方法返回空值，表示无自定义数据。{@link DataProvider} 默认使用此实例，
 * 跳过自定义源查询，行为等同于不注入。
 */
public final class NoopCustomDataSource implements CustomDataSource {

    public static final NoopCustomDataSource INSTANCE = new NoopCustomDataSource();

    private NoopCustomDataSource() {}

    @Override
    public List<TimelineEntry> getTimeline(int code) {
        return Collections.emptyList();
    }

    @Override
    public String getAddressName(int code) {
        return null;
    }
}
