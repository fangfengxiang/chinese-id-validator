/**
 * 数据加载与自定义数据源 SPI。
 *
 * <p><b>内部包</b>：本包中的 {@link cn.chineseid.validator.data.DataProvider} 等类型属于实现细节，
 * <b>不在公共 API 兼容性承诺范围内</b>。后续版本可能调整其可见性、方法签名或内部数据结构。
 *
 * <p>应用代码应仅通过 {@link cn.chineseid.validator.IdValidator} 门面类访问功能。
 * 唯一面向用户的公开类型是 {@link cn.chineseid.validator.data.CustomDataSource} 接口和
 * {@link cn.chineseid.validator.data.MergeMode} 枚举。
 */
package cn.chineseid.validator.data;
