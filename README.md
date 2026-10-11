# Chinese ID Validator

![Java](https://img.shields.io/badge/Java-11%2B-orange.svg)
![Maven](https://img.shields.io/badge/Maven-3.6%2B-red.svg)
![JUnit 5](https://img.shields.io/badge/JUnit-5-brightgreen.svg)
![CI](https://github.com/fangfengxiang/chinese-id-validator/actions/workflows/ci.yml/badge.svg)
![Version](https://img.shields.io/badge/version-0.1.0-blue.svg)
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

**方式一：Maven 中央仓库**（发布后直接引用）

```xml
<dependency>
    <groupId>io.github.fangfengxiang</groupId>
    <artifactId>id-validator</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle**（`build.gradle.kts`）：

```kotlin
dependencies {
    implementation("io.github.fangfengxiang:id-validator:0.1.0")
}
```

Groovy DSL（`build.gradle`）等价：

```groovy
dependencies {
    implementation 'io.github.fangfengxiang:id-validator:0.1.0'
}
```

> 发布到 Maven Central 后，Maven 与 Gradle 用同一坐标消费，无需额外配置。

**方式二：本地安装**（发布前或需自行编译时）

```sh
git clone <repo-url> chinese-id-validator
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

要求 JDK 11+。

## 使用示例

所有 API 通过门面类 `IdValidator` 静态调用。

> `440308199901101512` 和 `610104620927690` 示例大陆居民身份证均为随机生成的假数据，如撞车，请联系删除。
> `810000199408230021` 和 `830000199201300022` 示例港澳台居民居住证为北京市公安局公布的居住证样式号码。

### 验证身份证号合法性

```java
import cn.chineseid.validator.IdValidator;

// 大陆居民身份证
IdValidator.isValid("440308199901101512", true);  // 严格：18 位，校验地址码是否存在 → true
IdValidator.isValid("610104620927690", true);     // 严格：15 位 → true

// 港澳台居民居住证（8 开头地址码）
IdValidator.isValid("810000199408230021", true);  // 香港特别行政区 → true
IdValidator.isValid("830000199201300022", true);  // 台湾省 → true

// 三种验证模式
IdValidator.isValid("440308199901101512", true);   // 严格：校验地址码是否存在
IdValidator.isLooseValid("440308199901101512");     // 宽松：不校验地址码
IdValidator.isValidBasic("440308199901101512");     // 基础：仅校验顺序码/生日码/校验码
```

### 获取身份证号信息

当身份证号合法时，返回 `IdInfo` 对象（地址、出生日期、星座、生肖、性别、校验位等），不合法抛出 `IllegalArgumentException`：

```java
import cn.chineseid.validator.IdValidator;
import cn.chineseid.validator.model.IdInfo;

IdInfo info = IdValidator.getInfo("440308199901101512", true);

info.getAddressCode();    // 440308                        — 地址码
info.getAbandoned();      // 0                             — 是否废弃（0=使用中, 1=已废弃）
info.getAddress();        // "广东省深圳市盐田区"            — 地址全称
info.getAddressTree();    // ["广东省", "深圳市", "盐田区"]  — 省市区三级列表
info.getBirthday();       // 1999-01-10                    — 出生日期（LocalDate）
info.getConstellation();  // "摩羯座"                       — 星座
info.getChineseZodiac();  // "卯兔"                         — 生肖
info.getSex();            // Sex.MALE                      — 性别
info.getLength();        // IdType.EIGHTEEN               — 号码类型（15 位或 18 位）
info.getCheckBit();       // "2"                           — 校验码
```

港澳台居民居住证只返回省级地址信息：

```java
IdInfo hkInfo = IdValidator.getInfo("810000199408230021", true);
hkInfo.getAddress();      // "香港特别行政区"
hkInfo.getAddressTree();  // ["香港特别行政区", "", ""]

IdInfo twInfo = IdValidator.getInfo("830000199201300022", true);
twInfo.getAddress();      // "台湾省"
twInfo.getAddressTree();  // ["台湾省", "", ""]
```

### 获取基础信息（不含地址）

`getBasicInfo` 跳过地址码校验，仅返回生日、星座、生肖、性别等基础信息，适用于不关心地址的场景：

```java
import cn.chineseid.validator.model.BasicIdInfo;

BasicIdInfo info = IdValidator.getBasicInfo("440308199901101512");
info.getBirthday();       // 1999-01-10
info.getConstellation();  // "摩羯座"
info.getChineseZodiac();  // "卯兔"
info.getSex();            // Sex.MALE
info.getLength();        // IdType.EIGHTEEN
info.getCheckBit();       // "2"
```

### 生成假身份证号

`fakeId` 生成可通过校验的假身份证号，支持随机生成或按条件（地区、生日、性别、位数）生成：

```java
import cn.chineseid.validator.IdValidator;
import cn.chineseid.validator.generator.FakeIdBuilder;
import cn.chineseid.validator.model.Sex;

// 随机 18 位
IdValidator.fakeId();

// 按条件生成（Builder 模式）
IdValidator.fakeRequireId(new FakeIdBuilder()
        .eighteen()
        .address("上海市")
        .birthday("2000")
        .sex(Sex.MALE));                          // 2000 年上海市男性 18 位

IdValidator.fakeRequireId(new FakeIdBuilder()
        .fifteen()
        .address("南山区")
        .birthday("1999")
        .sex(Sex.FEMALE));                        // 1999 年深圳市南山区女性 15 位

IdValidator.fakeRequireId(new FakeIdBuilder()
        .eighteen()
        .address("江苏省")
        .birthday("200001")
        .sex(Sex.MALE));                           // 2000 年 1 月江苏省男性 18 位

IdValidator.fakeRequireId(new FakeIdBuilder()
        .eighteen()
        .address("台湾省")
        .birthday("20131010")
        .sex(Sex.FEMALE));                         // 2013 年 10 月 10 日台湾省女性居住证

IdValidator.fakeRequireId(new FakeIdBuilder()
        .eighteen()
        .address("香港特别行政区")
        .birthday("19970701")
        .sex(Sex.FEMALE));                         // 1997 年 7 月 1 日香港特别行政区女性居住证
```

**FakeIdBuilder 参数说明：**

| 参数 | 方法 | 说明 | 默认值 |
|---|---|---|---|
| 位数 | `.eighteen()` / `.fifteen()` | 生成 18 位或 15 位 | 18 位 |
| 地址 | `.address("江苏省")` | 省/市/区官方全称，如 "北京市"、"深圳市"、"朝阳区" | 全国随机 |
| 生日 | `.birthday("200001")` | 支持 "2000"、"198801"、"19990101" 三种精度 | 随机 |
| 性别 | `.sex(Sex.MALE)` | `Sex.MALE`（男）/ `Sex.FEMALE`（女） | FEMALE |

### 升级 15 位身份证号

将 15 位身份证号升级为 18 位（自动补全出生年份前缀 "19" 并计算校验码）：

```java
IdValidator.upgradeId("610104620927690");  // → 18 位身份证号
```

### 注入自定义数据源

实现 `CustomDataSource` 接口，在构造函数中初始化数据，通过 `setCustomDataSource` 注入：

```java
// 1. 实现接口（构造函数即初始化，无需 init 方法）
public class MyDataSource implements CustomDataSource {
    public MyDataSource() {
        // 初始化数据加载
    }

    @Override
    public List<TimelineEntry> getTimeline(int code) {
        // 返回自定义时间线，空列表 = 无自定义数据
    }

    @Override
    public String getAddressName(int code) {
        // 返回自定义地址名，null = 无自定义数据
    }

    // 可选：override getMergeMode() 切换策略，默认 MERGE
    @Override
    public MergeMode getMergeMode() {
        return MergeMode.FALLBACK; // 内置有数据则不查自定义
    }
}

// 2. 注入
IdValidator.setCustomDataSource(new MyDataSource());

// 3. 清除（恢复纯内置数据）
IdValidator.clearCustomDataSource();
```

**合并策略：**

| 策略 | 行为 | 适用场景 |
|---|---|---|
| `MERGE`（默认） | 总是查自定义源，与内置合并，自定义优先 | 补充/覆盖内置数据 |
| `FALLBACK` | 内置有数据则不查自定义源，仅填充缺失 | 仅补缺，不动已有数据 |

## API 方法总结

| 方法 | 参数 | 返回值 | 说明 |
|---|---|---|---|
| `isValid(id, strict)` | 身份证号, 是否严格 | `boolean` | 验证合法性 |
| `isLooseValid(id)` | 身份证号 | `boolean` | 宽松验证（不校验地址码） |
| `isStrictValid(id)` | 身份证号 | `boolean` | 严格验证（校验地址码） |
| `isValidBasic(id)` | 身份证号 | `boolean` | 基础验证（仅校验顺序码/生日码/校验码） |
| `getInfo(id, strict)` | 身份证号, 是否严格 | `IdInfo` | 获取完整信息 |
| `getBasicInfo(id)` | 身份证号 | `BasicIdInfo` | 获取基础信息（不含地址） |
| `fakeId()` | 无 | `String` | 生成随机 18 位假号 |
| `fakeRequireId(builder)` | `FakeIdBuilder` | `String` | 按条件生成假号 |
| `upgradeId(id)` | 15 位身份证号 | `String` | 升级为 18 位 |
| `setCustomDataSource(ds)` | `CustomDataSource` | `void` | 注入自定义数据源 |
| `clearCustomDataSource()` | 无 | `void` | 清除自定义数据源 |

## 数据维护

运行时数据文件位于 `src/main/resources/data/`：

| 原始 JSON（可编辑） | 运行时产物 | 说明 |
|---|---|---|
| `address_code.json` | `address_code.properties` | 地址码 → 名称映射 |
| `address_code_timeline.json` | `address_code_timeline.bin` | 地址码历史时间线（二进制序列化） |
| `chinese_zodiac.json` | — | 已硬编码为 `DataProvider` 常量 |
| `constellation.json` | — | 已硬编码为 `DataProvider` 常量 |

修改 JSON 后，在项目根目录用 Makefile 重新生成 `.properties` / `.bin`：

```sh
make data              # 重新生成 .properties 和 .bin
make data-properties   # 仅重新生成 address_code.properties
make data-timeline     # 仅重新生成 address_code_timeline.bin
make help              # 查看所有 target
```

生成器源码：`src/test/java/cn/chineseid/validator/tools/DataFileGenerator.java`（开发期工具，不进运行时 JAR）。`.bin` 采用 `DataOutputStream` 序列化，读写格式与 `DataProvider.loadTimelineMap` 严格对称，按地址码升序写入以保证可复现。

> `chinese_zodiac.json` / `constellation.json` 修改后需**手动同步** `DataProvider.java` 中的 `CHINESE_ZODIACS` / `CONSTELLATIONS` 常量，脚本无法自动生成这两项。

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
├── pom.xml                          # 主项目（io.github.fangfengxiang:id-validator）
├── Makefile                         # 数据维护（make data ...）
├── Dockerfile                       # 运行镜像
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
   - 任何违反《中华人民共和国个人信息保护法》《中华人民共和国居民身份证法》《中华人民共和国网络安全法》等法律法规的行为

3. **隐私保护**：身份证号码属于**个人敏感信息**。在处理真实身份证号码时，应严格遵守最小必要原则，采取加密、脱敏等技术措施，并确保已获得信息主体的合法授权。

4. **示例数据**：本项目中出现的身份证号码均为随机生成的假数据或官方公布的样式号码，不涉及任何真实个人信息。如偶然与真实号码重复，请联系删除。

5. **法律责任**：使用者应自行承担因不当使用本项目而产生的一切法律责任。本项目作者及贡献者不对任何因使用或滥用本项目而造成的直接或间接损失承担责任。

## License

[MIT](LICENSE)
