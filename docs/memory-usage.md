# 数据内存占用

行政区划数据打包进 JAR，运行时由 `DataProvider` 初始化加载常驻内存。本文记录各数据文件加载后的 Java 堆内存常驻增量，并提供可复现的测量脚本。

## 加载机制

| 数据 | 加载时机 | 数据结构 |
|---|---|---|
| `address_code.properties` | `DataProvider` 构造时（首次 `getInstance()`） | `HashMap<Integer, String>`（addressCodeMap） |
| `address_code_timeline.bin` | 懒加载（首次 `getTimeline(code)` 触发） | `HashMap<Integer, List<TimelineEntry>>`（timelineMap） |
| 生肖 | 编译期常量，零 I/O | `String[]` 常量 |
| 星座 | 编译期常量，零 I/O | `List<ConstellationData>` 常量 |

## 测量结果

测试环境：Docker 容器，Eclipse Temurin **JRE 8**（最低支持版本，`test/Dockerfile.probe` 运行镜像），`-Xmx128m`，测量前触发两次 GC + 300ms 等待，读取 `ManagementFactory.getMemoryMXBean()` 堆已用字节。

> 数值随 JVM 版本变化：JDK 9+ 引入 compact strings（Latin-1 字符串从 UTF-16 降为单字节），字符串密集的本库在 JDK 11+ 上占用更低；JDK 8 的数值是各版本中的上界。CI 每次构建在 JDK 8/11/17/21/25 上各测一遍并汇总对比（见文末「CI 自动测量」）。

### `address_code.properties`

| 指标 | 数值 |
|---|---|
| 条目数 | 3,209 |
| 文件大小 | 69 KB |
| 格式 | `地址码=行政区划全称`，UTF-8，按地址码升序 |
| **Java 常驻内存增量** | **0.56 MB（588,360 bytes）** |

### `address_code_timeline.bin`

| 指标 | 数值 |
|---|---|
| 有时间线的地址码数 | 3,209 |
| 时间线条目总数 | 3,501 |
| 文件大小 | 218 KB |
| **Java 常驻内存增量** | **2.05 MB（2,147,560 bytes）** |

### 内存占用全景

```
JVM 基线（未加载业务数据）:        0.30 MB
.properties 加载后:                0.86 MB  (+0.56 MB ← addressCodeMap 3,209 条)
.bin 加载后:                       2.91 MB  (+2.05 MB ← timelineMap 3,501 条)
总增量（.properties + .bin）:      2.61 MB
```

> `.bin` 占用约为 `.properties` 的 5 倍 —— `timelineMap` 是 `HashMap<Integer, List<TimelineEntry>>`，每条 `TimelineEntry` 含 `address` + `startYear` + `endYear` 三个 String 字段，对象头开销远大于纯键值对。

## 复现脚本

测量程序和 Docker 构建文件位于 `test/` 目录：

```
test/
├── MemoryProbe.java       # 内存探针（分阶段测堆内存 + 条目数）
├── Dockerfile.probe       # 多阶段构建：maven 编译 → JRE 运行
├── run-properties.sh      # 测 .properties 加载内存
└── run-timeline.sh        # 测 .properties + .bin 加载内存
```

```sh
./test/run-properties.sh    # 测 addressCodeMap 加载内存 + 条目数
./test/run-timeline.sh      # 测 timelineMap 加载内存 + 条目数
```

两个脚本各自独立，`docker build` + `docker run` 一键输出数值。镜像首次构建约 30 秒，后续命中缓存仅需数秒。

### CI 自动测量

CI 全绿后自动触发（`workflow_run`）或手动触发（`workflow_dispatch`），GitHub Actions 的 `memory-probe` 矩阵在 JDK 8/11/17/21/25 上各编译并运行一遍探针（探针零依赖，直接 `javac` 编译，不走 Maven），随后 `report` job 把 5 个版本的结果聚合成对比表格写入 run summary。`workflow_run` 触发的运行不挂在源 commit 上，`report` job 通过 Checks API 在 `head_sha` 创建 check run，结果在 commit 的 **Checks** 弹窗可直接查看，无需本地运行。不在 PR CI 中——同一字节码由 `ci.yml` 校验一次即可，内存探针仅作信息参考、不阻断。详见 `.github/workflows/memory.yml`。
