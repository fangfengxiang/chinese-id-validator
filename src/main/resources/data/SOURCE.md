# 数据来源

本目录下的数据文件提取自 [guanguans/id-validator](https://github.com/guanguans/id-validator) Go 库（v1.1.2）。

## 文件说明

| 文件 | 说明 | 运行时用途 |
|---|---|---|
| `address_code.json` | 行政区划代码 → 名称映射（原始 JSON） | 参考 |
| `address_code.properties` | 行政区划代码 → 名称映射（Properties 格式） | 运行时加载 |
| `address_code_timeline.json` | 行政区划代码 → 历史变更时间线（原始 JSON） | 参考 |
| `address_code_timeline.bin` | 行政区划代码 → 历史变更时间线（二进制序列化） | 运行时懒加载 |
| `chinese_zodiac.json` | 十二生肖（原始 JSON） | 参考（已硬编码为 Java 常量） |
| `constellation.json` | 十二星座（原始 JSON） | 参考（已硬编码为 Java 常量） |

## 运行时加载策略

- **生肖 / 星座**：编译期常量，硬编码在 `DataProvider` 中，零 I/O
- **地址码**：`.properties` 文件，`Properties.load()` 读取，初始化时加载
- **时间线**：`.bin` 二进制文件，`DataInputStream` 反序列化，懒加载（首次查询时加载）

## 重新生成数据文件

修改 `address_code.json` 或 `address_code_timeline.json` 后，在项目根目录执行：

```sh
make data              # 重新生成 .properties 和 .bin
make data-properties   # 仅重新生成 address_code.properties
make data-timeline     # 仅重新生成 address_code_timeline.bin
```

生成器：`src/test/java/cn/chineseid/validator/tools/DataFileGenerator.java`

- `address_code.json` → `address_code.properties`（Properties 文本，UTF-8 直写，按地址码升序）
- `address_code_timeline.json` → `address_code_timeline.bin`（DataOutputStream 二进制，格式与 `DataProvider.loadTimelineMap` 严格对称）
- `chinese_zodiac.json` / `constellation.json` 已硬编码为 `DataProvider` 常量，修改后需手动同步 Java 源码。

如需从上游 Go 库重新提取原始 JSON，参考 `guanguans/id-validator` v1.1.2 的数据结构。
