/**
 * 数据模型（值对象）。
 *
 * <p>本包中由 {@link cn.chineseid.validator.IdValidator} 方法返回的类型
 * （{@link cn.chineseid.validator.model.IdInfo IdInfo}、
 * {@link cn.chineseid.validator.model.BasicIdInfo BasicIdInfo}、
 * {@link cn.chineseid.validator.model.Sex Sex}、
 * {@link cn.chineseid.validator.model.IdType IdType}）
 * 属于公共 API，在兼容性承诺范围内。
 *
 * <p>{@link cn.chineseid.validator.model.TimelineEntry TimelineEntry}
 * 因 {@link cn.chineseid.validator.data.CustomDataSource} SPI 的公共接口签名而暴露，
 * 同样属于公共 API。
 *
 * <p>其余类型（如 {@link cn.chineseid.validator.model.ConstellationData ConstellationData}）
 * 属于实现细节，不在兼容性承诺范围内。
 */
package cn.chineseid.validator.model;
