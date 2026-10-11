# Chinese ID Validator

![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.fangfengxiang/chinese-id-validator)](https://central.sonatype.com/artifact/io.github.fangfengxiang/chinese-id-validator)
![Version](https://img.shields.io/badge/version-0.1.0-blue.svg)
![CI](https://github.com/fangfengxiang/chinese-id-validator/actions/workflows/ci.yml/badge.svg)
[![codecov](https://codecov.io/gh/fangfengxiang/chinese-id-validator/graph/badge.svg)](https://codecov.io/gh/fangfengxiang/chinese-id-validator)
![Maven](https://img.shields.io/badge/Maven-3.6%2B-red.svg)
![JUnit 5](https://img.shields.io/badge/JUnit-5-brightgreen.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

中国身份证号码验证器 — 支持 **15/18 位大陆居民身份证**、**港澳台居民居住证** 号码的验证、信息解析、升级与生成。仅校验号码本身（身份证一要素），不涉及姓名、人脸等实名比对。

> 纯 Java 实现，零运行时数据源依赖（行政区划数据打包进 JAR），单例懒加载。支持注入自定义数据源

其他语言实现参考：[Go](https://github.com/guanguans/id-validator) · [Python](https://github.com/jxlwqq/id-validator.py) · [Ruby](https://github.com/renyijiu/id_validator) · [JavaScript](https://github.com/mc-zone/IDValidator)

## 特性

- **验证**：宽松 / 严格 / 基础三种模式，严格模式校验地址码是否真实存在
- **解析**：一次性输出地址（省/市/区）、生日、性别、星座、生肖、是否已废弃地址码
- **生成**：随机或按条件（地区、生日、性别）生成合法假号，便于测试
- **升级**：15 位 → 18 位自动补校验码
- **历史地址**：内置行政区划时间线，同一地址码在不同年份解析为对应名称；支持运行时注入自定义数据源（SPI）
- **自定义数据源**：实现 `CustomDataSource` 接口注入自定义行政区划数据，支持 FALLBACK（内置优先）和 MERGE（总是合并）两种策略

## 安装

**方式一：Maven 中央仓库**

```xml
<dependency>
    <groupId>io.github.fangfengxiang</groupId>
    <artifactId>chinese-id-validator</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle**（`build.gradle.kts`）：

```kotlin
dependencies {
    implementation("io.github.fangfengxiang:chinese-id-validator:0.1.0")
}
```

Groovy DSL（`build.gradle`）等价：

```groovy
dependencies {
    implementation 'io.github.fangfengxiang:chinese-id-validator:0.1.0'
}
```

> Maven 与 Gradle 用同一坐标消费，无需额外配置。

**方式二：本地安装**（需自行编译时）

```sh
git clone https://github.com/fangfengxiang/chinese-id-validator.git chinese-id-validator
cd chinese-id-validator
mvn install
```

本地安装后，Gradle 用户需在 `repositories` 中声明 `mavenLocal()` 才能解析 `~/.m2` 中的 JAR：

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}
```

要求 JDK 8+。

## 快速开始

所有 API 通过门面类 `IdValidator` 静态调用。

> 示例号码 `440308199901101512`、`610104620927690` 为随机生成的假数据；`810000199408230021`、`830000199201300022` 为官方公布的居住证样式号码。

### 验证

```java
IdValidator.isValid("440308199901101512", true);  // 严格：校验地址码是否存在
IdValidator.isLooseValid("440308199901101512");    // 宽松：不校验地址码
IdValidator.isValidBasic("440308199901101512");    // 基础：仅校验顺序码/生日码/校验码
```

### 解析

```java
IdInfo info = IdValidator.getInfo("440308199901101512", true);
info.getAddress();        // "广东省深圳市盐田区"
info.getBirthday();       // 1999-01-10
info.getConstellation();  // "摩羯座"
info.getChineseZodiac();  // "卯兔"
info.getSex();            // Sex.MALE
```

不关心地址时用 `getBasicInfo`，跳过地址码校验。港澳台居住证（8 开头地址码）只返回省级地址。

### 生成假号

```java
IdValidator.fakeId();  // 随机 18 位

IdValidator.fakeRequireId(new FakeIdBuilder()
        .eighteen()
        .address("上海市")
        .birthday("2000")
        .sex(Sex.MALE));  // 2000 年上海市男性
```

### 升级 15 位 → 18 位

```java
IdValidator.upgradeId("610104620927690");
```

### 自定义数据源

实现 `CustomDataSource` 接口，通过 `setCustomDataSource` 注入：

```java
public class MyDataSource implements CustomDataSource {
    @Override
    public List<TimelineEntry> getTimeline(int code) {
        // 返回自定义时间线，空列表 = 无自定义数据
    }

    @Override
    public String getAddressName(int code) {
        // 返回自定义地址名，null = 无自定义数据
    }

    // 可选：默认 FALLBACK，可切换 MERGE
    @Override
    public MergeMode getMergeMode() {
        return MergeMode.MERGE;
    }
}

IdValidator.setCustomDataSource(new MyDataSource());
IdValidator.clearCustomDataSource();  // 清除，恢复纯内置数据
```

| 策略 | 行为 |
|---|---|
| `FALLBACK`（默认） | 内置有数据则不查自定义源，仅填充缺失 |
| `MERGE` | 总是查自定义源，与内置合并，自定义优先 |

更多：[API 参考](docs/api.md)

## 数据维护

运行时依赖 4 类数据：

| 数据依赖 | 源数据文件 | 说明 | 加载方式 |
|---|---|---|---|
| `address_code.properties` | `address_code.json` | 地址码→名称映射 | `DataProvider` 构造时加载进 `HashMap` |
| `address_code_timeline.bin` | `address_code_timeline.json` | 地址码历史时间线 | 首次查询时懒加载进 `HashMap` |
| `CHINESE_ZODIACS` 常量 | `chinese_zodiac.json` | 生肖 | 编译期常量，无 I/O |
| `CONSTELLATIONS` 常量 | `constellation.json` | 星座 | 编译期常量，无 I/O |

源数据文件（`.json`）用于升级构造运行时数据文件（`.properties` / `.bin`），代码包运行时无依赖——仅依赖打包进 JAR 的 `.properties` / `.bin` 及硬编码常量。修改 JSON 后用 `make data` 重新生成。

更多：[数据维护](docs/data-maintenance.md)

## 运行时说明

数据加载后 Java 堆内存常驻增量（Docker / Temurin JRE 11 / `-Xmx128m` 实测）：

| 数据 | 条目数 | 堆内存增量 |
|---|---|---|
| `address_code.properties` | 3,209 条 | **0.41 MB** |
| `address_code_timeline.bin` | 3,501 条时间线 | **2.04 MB** |
| **合计业务数据增量** | — | **2.45 MB** |

JVM 总堆占用约 **3.50 MB**（含 JVM 基线 1.04 MB + 业务数据 2.45 MB）。`.bin` 占用约为 `.properties` 的 5 倍，因为时间线是 `HashMap<Integer, List<TimelineEntry>>`，对象头开销远大于纯键值对。

更多：[数据内存占用](docs/memory-usage.md)

## 测试

单元测试和集成测试均在主项目内，通过 Maven 插件分离执行：

```sh
mvn test                        # 仅单元测试（surefire，*Test.java）
mvn verify                      # 单元测试 + 集成测试（failsafe，*IT.java）
mvn failsafe:integration-test   # 仅集成测试
```

集成测试直接引用 `target/classes`，不安装 JAR 到本地仓库，零 `.m2` 污染。

## 构建

```sh
mvn verify                             # 编译 + 单测 + 集成测试 + 打包
mvn package                            # 编译 + 单测 + 打包（跳过集成测试）
docker build -t chinese-id-validator . # 用根目录 Dockerfile 构建（内含 mvn verify）
```

## 项目结构

```
chinese-id-validator/
├── pom.xml                          # 主项目（io.github.fangfengxiang:chinese-id-validator）
├── Makefile                         # 数据维护（make data ...）
├── Dockerfile                       # 运行镜像
├── LICENSE                          # MIT 许可证
├── .github/workflows/               # CI / Release（GitHub Actions）
├── docs/                            # 文档（API 参考、数据维护、内存占用）
├── test/                            # 内存测量脚本（MemoryProbe + run-*.sh + Dockerfile.probe）
├── src/main/java/cn/chineseid/validator/
│   ├── IdValidator.java             # 对外门面 API
│   ├── core/                        # 解析与校验
│   ├── data/                        # 数据加载（DataProvider / CustomDataSource / MergeMode）
│   ├── generator/                   # 假号生成
│   ├── model/                       # 数据模型
│   └── util/                        # 地址/星座/生肖计算
├── src/main/resources/data/         # 运行时数据（json + properties + bin）
└── src/test/java/
    ├── cn/chineseid/validator/      # 单元测试（*Test.java，surefire）
    └── cn/chineseid/it/             # 集成测试（*IT.java，failsafe）
```

## 参考资料

- [中华人民共和国公民身份号码](https://zh.wikipedia.org/wiki/中华人民共和国公民身份号码)
- [中华人民共和国民政部：行政区划代码](http://www.mca.gov.cn/article/sj/xzqh/)
- [中华人民共和国行政区划代码历史数据集](https://github.com/jxlwqq/address-code-of-china)
- [国务院办公厅关于印发《港澳台居民居住证申领发放办法》的通知](http://www.gov.cn/zhengce/content/2018-08/19/content_5314865.htm)
- [港澳台居民居住证](https://zh.wikipedia.org/wiki/港澳台居民居住证)

## 免责声明

**本项目仅供学习、研究、开发和测试用途，不得用于任何非法目的。**

1. **用途限制**：本项目仅提供身份证号码格式校验与信息解析功能，**不是**官方身份认证服务，不具备任何法律效力。行政区划数据来源于公开渠道（[中华人民共和国民政部](http://www.mca.gov.cn/article/sj/xzqh/)），不代表政府部门官方数据。

2. **禁止用途**：严禁将本项目用于以下行为：
   - 非法采集、存储、买卖、传播他人身份证号码等个人信息
   - 伪造、变造身份证件，或冒用他人身份
   - 实施诈骗、洗钱、侵犯公民个人信息等违法犯罪活动

3. **隐私保护**：身份证号码属于**个人敏感信息**。在处理真实身份证号码时，应严格遵守最小必要原则，采取加密、脱敏等技术措施，并确保已获得信息主体的合法授权。

4. **示例数据**：本项目中出现的身份证号码均为随机生成的假数据或官方公布的样式号码，不涉及任何真实个人信息。如偶然与真实号码重复，请联系删除。

5. **法律责任**：使用者应自行承担因不当使用本项目而产生的一切法律责任。本项目作者及贡献者不对任何因使用或滥用本项目而造成的直接或间接损失承担责任。

## 使用限制

本软件处理身份证号码等个人敏感信息，使用者有责任确保其使用方式符合所在国家或地区的法律法规，包括但不限于数据保护、隐私及身份信息相关立法，并自行取得必要的授权和许可。

本软件不得用于违反使用者所在国家或地区法律法规的活动，包括但不限于侵犯公民个人信息、破坏网络安全、危害国家安全等违法行为。使用者违反本条款即丧失使用授权。

## License

[MIT](LICENSE)
