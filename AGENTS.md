# Chinese ID Validator — Agent Guide

Guide for AI coding agents (Copilot, Cursor, Claude Code, etc.) working in this codebase.

## Build & Test

```sh
mvn compile                          # Compile only
mvn test                             # Unit tests (surefire, *Test.java)
mvn failsafe:integration-test       # Integration tests only (failsafe, *IT.java)
mvn verify                           # Compile + unit tests + integration tests + package
mvn package                         # Compile + unit tests + package (skip integration tests)
mvn -Prelease verify                 # Full release build (sources + javadoc + GPG sign)
make data                            # Regenerate .properties and .bin from JSON source
make data-properties                 # Regenerate address_code.properties only
make data-timeline                   # Regenerate address_code_timeline.bin only
docker build -t chinese-id-validator .  # Docker build (runs mvn verify inside)
```

## Maven Coordinates

- groupId: `io.github.fangfengxiang`
- artifactId: `chinese-id-validator`
- version: `0.1.0`
- Java: 11+
- Runtime dependencies: **zero** (only test-scoped: Gson, JUnit 5)

## Architecture

```
src/main/java/cn/chineseid/validator/
├── IdValidator.java        # Public facade — all static methods
├── core/                   # Parsing & validation logic
├── data/                   # Data loading (DataProvider singleton, CustomDataSource, MergeMode)
├── generator/              # Fake ID generation (FakeIdBuilder)
├── model/                  # Data models (IdInfo, BasicIdInfo, Sex, IdType, etc.)
└── util/                   # Address/constellation/zodiac calculators
```

- `IdValidator` is the only entry point — facade with static methods (isValid, getInfo, getBasicInfo, fakeId, fakeRequireId, upgradeId, setCustomDataSource, clearCustomDataSource)
- `DataProvider` uses Singleton + Holder pattern, lazy-loads on first `getInstance()` call
- Runtime data lives in `src/main/resources/data/` (JSON source + .properties + .bin artifacts)
- Data generator tool: `src/test/java/cn/chineseid/validator/tools/DataFileGenerator.java` (test scope, not in runtime JAR)

## Code Style

- Java 11+, no Lombok, no annotation processors
- Javadoc on all public API methods (generates `*-javadoc.jar` for Maven Central)
- Error handling: invalid input throws `IllegalArgumentException`, runtime issues return `null` or `false`
- Collections are immutable after construction (defensive copies in constructors)
- Package: `cn.chineseid.validator`

## Tests

```
src/test/java/
├── cn/chineseid/validator/   # Unit tests (*Test.java → surefire)
└── cn/chineseid/it/          # Integration tests (*IT.java → failsafe)
```

Integration tests reference `target/classes` directly (no local JAR install, zero `.m2` pollution).

## Don't

- Don't add runtime dependencies — project is zero-dep by design
- Don't modify `data/*.json` without running `make data` to regenerate `.properties` / `.bin`
- Don't modify `chinese_zodiac.json` / `constellation.json` without manually syncing hardcoded constants in `DataProvider.java`
- Don't change the public API signatures without bumping major version
- Don't use reflection — data loading uses `getResourceAsStream` from classpath
