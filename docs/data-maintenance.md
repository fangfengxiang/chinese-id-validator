# 数据维护

运行时数据文件位于 `src/main/resources/data/`。

## 数据文件对照

| 原始 JSON（可编辑） | 运行时产物 | 说明 |
|---|---|---|
| `address_code.json` | `address_code.properties` | 地址码 → 名称映射 |
| `address_code_timeline.json` | `address_code_timeline.bin` | 地址码历史时间线（二进制序列化） |
| `chinese_zodiac.json` | — | 已硬编码为 `DataProvider` 常量 |
| `constellation.json` | — | 已硬编码为 `DataProvider` 常量 |

## 重新生成产物

修改 JSON 后，在项目根目录用 Makefile 重新生成 `.properties` / `.bin`：

```sh
make data              # 重新生成 .properties 和 .bin
make data-properties   # 仅重新生成 address_code.properties
make data-timeline     # 仅重新生成 address_code_timeline.bin
make help              # 查看所有 target
```

## 生成器源码

`src/test/java/cn/chineseid/validator/tools/DataFileGenerator.java`（开发期工具，不进运行时 JAR）。

`.bin` 采用 `DataOutputStream` 序列化，读写格式与 `DataProvider.loadTimelineMap` 严格对称，按地址码升序写入以保证可复现。

## 星座 / 生肖手动同步

`chinese_zodiac.json` / `constellation.json` 修改后需**手动同步** `DataProvider.java` 中的 `CHINESE_ZODIACS` / `CONSTELLATIONS` 常量，脚本无法自动生成这两项。
