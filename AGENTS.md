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
mvn spotless:check                   # Style check (bound to validate phase, runs before any build/test)
mvn spotless:apply                   # Auto-format all Java sources (run before committing)
mvn compile spotbugs:check           # Static analysis (bytecode-level bug patterns, bound to verify)
mvn -Pbenchmark package -DskipTests  # Build JMH benchmarks jar (target/*-benchmarks.jar)
java -jar target/*-benchmarks.jar    # Run JMH benchmarks locally
```

## Maven Coordinates

- groupId: `io.github.fangfengxiang`
- artifactId: `chinese-id-validator`
- version: `0.1.0`
- Java: 8+ (bytecode target `--release 8`; build toolchain JDK 21 — Spotless/palantir-java-format requires it)
- Runtime dependencies: **zero** (only test-scoped: Gson, JUnit 5)

## CI/CD

- `ci.yml` (push/PR): style-check gates everything, then in parallel — static-analysis (SpotBugs), unit-test → integration-test chain (serial)
- `compat.yml` (CI 全绿后 workflow_run 自动触发 + workflow_dispatch 手动): compat-test matrix (JDK 8/11/17/21/25, native Temurin via setup-java) running the full test suite per JVM, with Spotless/SpotBugs/JaCoCo skipped (same bytecode, checked once by CI jobs); its `report` job renders the per-JDK compatibility table on the Summary page. Not in PR CI — same bytecode, checked once by CI; informational, never gates
- `memory.yml` (CI 全绿后 workflow_run 自动触发 + workflow_dispatch 手动): memory-probe matrix (same 5 JDKs) measuring data memory footprint per JVM via direct javac (no Maven); informational only, never gates; its `report` job renders the per-JDK memory table. A release-comparison gate (vs latest Central artifact + manual approval via environment) is planned once a second release exists
- `benchmark.yml` (CI 全绿后 workflow_run 自动触发 + workflow_dispatch 手动): JMH benchmark matrix (same 5 JDKs) running benchmarks from `src/jmh/java` (`-Pbenchmark` profile builds `target/*-benchmarks.jar`) per JVM; its `report` job pivots results into a cross-JDK comparison table (benchmark rows × JDK columns). Not in PR CI — shared-runner timing noise exceeds regression signals; raw `jmh-result-jdk*.json` + text artifacts retained
- `workflow_run`-triggered runs don't attach to the source commit: each `report` job ends by creating a check run (Compat/Memory/Benchmark) on `workflow_run.head_sha` via Checks API, so results are visible in the commit's checks popup
- JDK 8 Maven builds: `jdk8-skip-toolchain` profile (auto-activated on JDK 1.8) unbinds Spotless and SpotBugs — their mojos are Java 11+ bytecode and fail to load on JDK 8 (`-D*.skip` can't help; class loading precedes skip checks). maven-compiler-plugin auto-falls back from `--release 8` to `source/target 1.8` on JDK 8
- Style: Spotless + palantir-java-format (4-space indent; `cn.*` imports stay before `java.*` — no `importOrder` configured)
- Static analysis: SpotBugs (effort=Max, threshold=Medium), bound to `verify` so local `mvn verify` enforces it; bytecode-level complement to source-level checks
- Coverage: JaCoCo reports uploaded to Codecov from both test jobs (`unit` / `integration` flags)
- Security: Dependabot weekly for actions + maven

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

- Java 8+ source compatible (no `var`, no `List.of`/`Map.of`/`Files.readString` — `--release 8` enforces at compile time), no Lombok, no annotation processors
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
