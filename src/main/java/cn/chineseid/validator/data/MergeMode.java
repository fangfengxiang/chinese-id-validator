package cn.chineseid.validator.data;

/**
 * 自定义数据与内置数据的合并策略。
 *
 * <ul>
 *   <li>{@link #FALLBACK} — 内置优先：内置有数据则不查自定义源（仅填充缺失，对标 {@code ResourceBundle} 父子链）
 *   <li>{@link #MERGE}    — 总是合并：内置 + 自定义，自定义条目优先匹配（对标 Spring {@code PropertySource} 有序链）
 * </ul>
 */
public enum MergeMode {

    /**
     * 内置优先：内置有数据则不调用自定义源，仅填充内置缺失的部分。
     * <p>对应场景："读到数据就不读用户函数，读不到再读用户函数"。
     */
    FALLBACK,

    /**
     * 总是合并：无论内置是否有数据，都查询自定义源，合并结果中自定义条目排在前面优先匹配。
     *
     * <p><b>遮蔽语义</b>：自定义条目覆盖年份与内置条目重叠时，自定义条目优先匹配并遮蔽内置条目，
     * 包括内置 end_year 为空（"至今"，{@code DataProvider.normalizeBoundaries} 填充为 9999）的条目。
     * 自定义数据内部若存在多个重叠区间，按提供顺序首次匹配，调用方负责处理内部重叠。
     *
     * <p>对应场景："不管读不读到数据，都读一次用户函数"。
     */
    MERGE
}
