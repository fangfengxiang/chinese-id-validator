# Chinese ID Validator

![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.fangfengxiang/chinese-id-validator)](https://central.sonatype.com/artifact/io.github.fangfengxiang/chinese-id-validator)
![Version](https://img.shields.io/badge/version-0.1.0-blue.svg)
![CI](https://github.com/fangfengxiang/chinese-id-validator/actions/workflows/ci.yml/badge.svg)
[![codecov](https://codecov.io/gh/fangfengxiang/chinese-id-validator/graph/badge.svg)](https://codecov.io/gh/fangfengxiang/chinese-id-validator)
![Maven](https://img.shields.io/badge/Maven-3.6%2B-red.svg)
![JUnit 5](https://img.shields.io/badge/JUnit-5-brightgreen.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

Chinese ID number validator — supports validation, parsing, upgrading, and generation of **15/18-digit mainland resident ID cards** and **Hong Kong/Macau/Taiwan resident permits**. It only validates the number itself (single-element ID check) and does not perform real-name matching against names, faces, etc.

> Pure Java implementation with zero runtime data-source dependencies (administrative division data is bundled inside the JAR) and lazy-loaded singletons. Supports injecting custom data sources.

Implementations in other languages: [Go](https://github.com/guanguans/id-validator) · [Python](https://github.com/jxlwqq/id-validator.py) · [Ruby](https://github.com/renyijiu/id_validator) · [JavaScript](https://github.com/mc-zone/IDValidator)

> [中文文档](README.md)

## Features

- **Validation**: Three modes — Loose / Strict / Basic. Strict mode verifies whether the address code actually exists.
- **Parsing**: One-shot output of address (province/city/district), birthday, sex, constellation, Chinese zodiac, and whether the address code is deprecated.
- **Generation**: Random or conditional (region, birthday, sex) generation of legal fake numbers for testing.
- **Upgrade**: 15-digit → 18-digit with automatic checksum calculation.
- **Historical addresses**: Built-in administrative division timeline; the same address code resolves to the corresponding name in different years. Supports runtime injection of custom data sources (SPI).
- **Custom data source**: Implement the `CustomDataSource` interface to inject custom administrative division data, with two strategies — FALLBACK (built-in first) and MERGE (always merge).

## Installation

**Option 1: Maven Central**

```xml
<dependency>
    <groupId>io.github.fangfengxiang</groupId>
    <artifactId>chinese-id-validator</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle** (`build.gradle.kts`):

```kotlin
dependencies {
    implementation("io.github.fangfengxiang:chinese-id-validator:0.1.0")
}
```

Groovy DSL (`build.gradle`) equivalent:

```groovy
dependencies {
    implementation 'io.github.fangfengxiang:chinese-id-validator:0.1.0'
}
```

> Maven and Gradle consume the same coordinates; no extra configuration needed.

**Option 2: Local install** (when you need to build from source)

```sh
git clone https://github.com/fangfengxiang/chinese-id-validator.git chinese-id-validator
cd chinese-id-validator
mvn install
```

After a local install, Gradle users must declare `mavenLocal()` in `repositories` to resolve the JAR from `~/.m2`:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}
```

Requires JDK 8+.

## Quick Start

All APIs are invoked statically through the facade class `IdValidator`.

> The sample numbers `440308199901101512` and `610104620927690` are randomly generated fake data; `810000199408230021` and `830000199201300022` are officially published permit-style numbers.

### Validation

```java
IdValidator.isValid("440308199901101512", true);  // Strict: verify the address code exists
IdValidator.isLooseValid("440308199901101512");    // Loose: do not verify the address code
IdValidator.isValidBasic("440308199901101512");    // Basic: only verify sequence/birthday/checksum
```

### Parsing

```java
IdInfo info = IdValidator.getInfo("440308199901101512", true);
info.getAddress();        // "广东省深圳市盐田区"
info.getBirthday();       // 1999-01-10
info.getConstellation();  // "摩羯座"
info.getChineseZodiac();  // "卯兔"
info.getSex();            // Sex.MALE
```

When you don't care about the address, use `getBasicInfo` to skip address-code validation. Hong Kong/Macau/Taiwan resident permits (address codes starting with 8) return only the provincial-level address.

### Generating Fake IDs

```java
IdValidator.fakeId();  // Random 18-digit

IdValidator.fakeRequireId(new FakeIdBuilder()
        .eighteen()
        .address("上海市")
        .birthday("2000")
        .sex(Sex.MALE));  // Male born in Shanghai in 2000
```

### Upgrading 15-digit → 18-digit

```java
IdValidator.upgradeId("610104620927690");
```

### Custom Data Source

Implement the `CustomDataSource` interface and inject it once via `initialize`:

```java
public class MyDataSource implements CustomDataSource {
    @Override
    public List<TimelineEntry> getTimeline(int code) {
        // Return the custom timeline; empty list = no custom data
    }

    @Override
    public String getAddressName(int code) {
        // Return the custom address name; null = no custom data
    }
}

// One-time initialization (atomic publication, JVM-wide effect)
IdValidator.initialize(new MyDataSource(), MergeMode.MERGE);
```

> `initialize()` can be called only once. A repeated call throws `IllegalStateException`.
> It must be called before the business thread starts validating — the library does not support switching configuration during validation; within a single validation, the province/city/district lookups each read the current configuration and do not take cross-call snapshots.
> Normal usage does not require explicit initialization — the built-in data is loaded automatically on the first business API call.
> To preload the built-in data at application startup, call `IdValidator.preload()` (idempotent).

| Strategy | Behavior |
|---|---|
| `FALLBACK` | If the built-in source has data, the custom source is not queried; only missing entries are filled |
| `MERGE` | The custom source is always queried and merged with the built-in; custom takes precedence |

For integration with external config sources like Nacos, see [example/NacosExample.java](example/NacosExample.java).

More: [API reference](docs/api.md)

## Data Maintenance

The runtime depends on four categories of data:

| Data dependency | Source file | Description | Loading |
|---|---|---|---|
| `address_code.properties` | `address_code.json` | Address code → name mapping | Loaded into a `HashMap` when `DataProvider` is constructed |
| `address_code_timeline.bin` | `address_code_timeline.json` | Historical timeline of address codes | Lazily loaded into a `HashMap` on first query |
| `CHINESE_ZODIACS` constant | `chinese_zodiac.json` | Chinese zodiac | Compile-time constant, no I/O |
| `CONSTELLATIONS` constant | `constellation.json` | Constellations | Compile-time constant, no I/O |

The source data files (`.json`) are used to regenerate the runtime data files (`.properties` / `.bin`). The packaged runtime has no external dependencies — it relies only on the bundled `.properties` / `.bin` and hardcoded constants. After modifying the JSON, run `make data` to regenerate.

More: [Data maintenance](docs/data-maintenance.md)

## Runtime Notes

Heap memory resident footprint after data loading (Docker / Temurin JRE / `-Xmx128m` measured; JDK 8 is the upper bound across versions, see the table below for details):

| Data | Entries | Heap delta (JDK 11) | Heap delta (JDK 8 upper bound) |
|---|---|---|---|
| `address_code.properties` | 3,209 entries | **0.43 MB** | 0.56 MB |
| `address_code_timeline.bin` | 3,501 timeline entries | **2.04 MB** | 2.05 MB |
| **Total business data delta** | — | **2.47 MB** | 2.61 MB |

Total JVM heap usage is about **3.52 MB** (JDK 11, including the JVM baseline of 1.05 MB + business data of 2.47 MB). The `.bin` footprint is about 5x that of `.properties` because the timeline is a `HashMap<Integer, List<TimelineEntry>>`, whose object-header overhead far exceeds that of plain key-value pairs. JDK 9+ introduced compact strings (Latin-1 strings dropped from UTF-16 to single-byte), so this string-heavy library has a smaller footprint on JDK 11+; JDK 8 is the upper bound across versions.

More: [Data memory usage](docs/memory-usage.md)

## Testing

Both unit and integration tests live in the main project and are executed separately via Maven plugins:

```sh
mvn test                        # Unit tests only (surefire, *Test.java)
mvn verify                      # Unit tests + integration tests (failsafe, *IT.java)
mvn failsafe:integration-test   # Integration tests only
```

Integration tests reference `target/classes` directly without installing the JAR to the local repository, causing zero `.m2` pollution.

## Build

```sh
mvn verify                             # Compile + unit tests + integration tests + package
mvn package                            # Compile + unit tests + package (skip integration tests)
docker build -t chinese-id-validator . # Build the runtime image with the root Dockerfile (runs mvn verify inside)
```

## Project Structure

```
chinese-id-validator/
├── pom.xml                          # Main project (io.github.fangfengxiang:chinese-id-validator)
├── Makefile                         # Data maintenance (make data ...)
├── Dockerfile                       # Runtime image
├── LICENSE                          # MIT license
├── example/                         # Standalone reference samples (not part of the Maven build)
│   └── NacosExample.java            # Nacos integration example
├── .github/workflows/               # CI / Release (GitHub Actions)
├── docs/                            # Documentation (API reference, data maintenance, memory usage)
├── test/                            # Memory probe scripts (MemoryProbe + run-*.sh + Dockerfile.probe)
├── src/main/java/cn/chineseid/validator/
│   ├── IdValidator.java             # Public facade API
│   ├── IdValidatorException.java    # Runtime exception (data-source query failure, etc.)
│   ├── IdValidatorMessages.java     # Exception message constants
│   ├── core/                        # Parsing and validation
│   ├── data/                        # Data loading (DataProvider / CustomDataSource / MergeMode)
│   ├── generator/                   # Fake ID generation
│   ├── model/                       # Data models
│   └── util/                        # Address / constellation / zodiac calculators
├── src/main/resources/data/         # Runtime data (json + properties + bin)
└── src/test/java/
    ├── cn/chineseid/validator/      # Unit tests (*Test.java, surefire)
    └── cn/chineseid/it/             # Integration tests (*IT.java, failsafe)
```

## References

- [People's Republic of China citizen identity number (Wikipedia)](https://zh.wikipedia.org/wiki/中华人民共和国公民身份号码)
- [Ministry of Civil Affairs of the PRC: Administrative division codes](http://www.mca.gov.cn/article/sj/xzqh/)
- [Historical administrative division code dataset](https://github.com/jxlwqq/address-code-of-china)
- [General Office of the State Council: Measures for the application and issuance of residence permits for Hong Kong, Macau and Taiwan residents](http://www.gov.cn/zhengce/content/2018-08/19/content_5314865.htm)
- [Hong Kong/Macau/Taiwan resident permit (Wikipedia)](https://zh.wikipedia.org/wiki/港澳台居民居住证)

## Disclaimer

**This project is for learning, research, development, and testing purposes only. It must not be used for any illegal purposes.**

1. **Scope limitation**: This project provides only ID number format validation and information parsing. It is **not** an official identity authentication service and has no legal effect. The administrative division data is sourced from public channels ([Ministry of Civil Affairs of the PRC](http://www.mca.gov.cn/article/sj/xzqh/)) and does not represent official government data.

2. **Prohibited uses**: It is strictly forbidden to use this project for:
   - Illegally collecting, storing, trading, or disseminating others' ID numbers or other personal information
   - Forging or altering ID documents, or impersonating others
   - Carrying out fraud, money laundering, infringement of citizens' personal information, or other illegal or criminal activities

3. **Privacy protection**: ID numbers are **sensitive personal information**. When handling real ID numbers, strictly follow the principle of minimum necessity, adopt technical measures such as encryption and desensitization, and ensure that lawful authorization has been obtained from the data subject.

4. **Sample data**: All ID numbers appearing in this project are randomly generated fake data or officially published sample numbers and do not involve any real personal information. In case of accidental duplication with a real number, please contact us for removal.

5. **Legal liability**: Users shall bear all legal responsibility arising from improper use of this project. The author and contributors of this project shall not be liable for any direct or indirect losses caused by using or abusing this project.

## Usage Restrictions

This software processes sensitive personal information such as ID numbers. Users are responsible for ensuring that their use complies with the laws and regulations of their country or region, including but not limited to data protection, privacy, and identity-related legislation, and for obtaining the necessary authorizations and permissions.

This software must not be used for activities that violate the laws and regulations of the user's country or region, including but not limited to infringement of citizens' personal information, undermining cybersecurity, and endangering national security. Any user who violates this clause immediately forfeits the authorization to use this software.

## License

[MIT](LICENSE)
