import cn.chineseid.validator.data.DataProvider;
import cn.chineseid.validator.model.TimelineEntry;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Map;

/**
 * 数据加载内存探针 — 测量 .properties / .bin 加载后的 Java 常驻堆内存增量。
 *
 * <p>用法：{@code java -cp ... MemoryProbe [properties|all]}
 * <ul>
 *   <li>{@code properties} — 只加载 .properties（addressCodeMap），输出条目数 + 内存增量
 *   <li>{@code all}（默认）— 加载 .properties + .bin（timelineMap），输出两者增量
 * </ul>
 *
 * <p>测量方式：{@code ManagementFactory.getMemoryMXBean()} 读取堆已用字节，
 * 测量前触发两次 GC + 短暂等待，尽量排除无关对象干扰。
 */
public class MemoryProbe {

    /** 1 MB 的字节数（1024 × 1024），用于 bytes→MB 换算 */
    private static final double BYTES_PER_MB = 1048576.0;

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";

        System.out.println("========================================");
        System.out.println("  chinese-id-validator 数据内存探针");
        System.out.println("========================================");
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.printf("JVM 最大堆 (-Xmx): %.2f MB%n", Runtime.getRuntime().maxMemory() / BYTES_PER_MB);
        System.out.println();

        // ---- 阶段 0：基线（未加载业务数据）----
        // 预热并丢弃首次读数：首次 measure() 会初始化 MXBean 基础设施并触发 JIT，
        // 其开销残留在堆里会抬高基线（JDK 8 上实测导致 .properties 增量算成负数）
        measure();
        long baseline = measure();
        System.out.println("=== 阶段 0：JVM 基线（未加载业务数据）===");
        System.out.printf("已用堆内存: %s%n%n", fmt(baseline));

        // ---- 阶段 1：加载 .properties（DataProvider 构造时加载 addressCodeMap）----
        DataProvider dp = DataProvider.getInstance();
        Map<Integer, String> allCodes = dp.getAllAddressCodes();

        long afterProps = measure();
        long propsDelta = afterProps - baseline;

        System.out.println("=== 阶段 1：.properties 加载完成（addressCodeMap）===");
        System.out.println("数据文件: src/main/resources/data/address_code.properties");
        System.out.printf("条目数: %,d%n", allCodes.size());
        System.out.printf("加载后已用堆: %s%n", fmt(afterProps));
        System.out.printf(".properties 常驻内存增量: %s%n", fmt(propsDelta));
        System.out.println("样本（前 5 条）:");
        allCodes.entrySet().stream().limit(5).forEach(e ->
                System.out.printf("  %d = %s%n", e.getKey(), e.getValue()));
        System.out.println();

        if ("properties".equals(mode)) {
            printSummary(baseline, afterProps, -1, allCodes.size(), -1);
            return;
        }

        // ---- 阶段 2：加载 .bin（懒加载，首次 getTimeline 触发 TimelineHolder 全量加载）----
        // 任意 code 调用即可触发整个 TimelineHolder 类初始化（110000 = 北京市，必定存在于内置数据中）
        dp.getTimeline(110000);

        long afterBin = measure();
        long binDelta = afterBin - afterProps;

        // 统计 timeline 条目数（遍历所有地址码，统计有时间线数据的）
        int timelineCodeCount = 0;
        int timelineEntryCount = 0;
        for (Integer code : allCodes.keySet()) {
            List<TimelineEntry> entries = dp.getTimeline(code);
            if (entries != null && !entries.isEmpty()) {
                timelineCodeCount++;
                timelineEntryCount += entries.size();
            }
        }

        System.out.println("=== 阶段 2：.bin 加载完成（timelineMap）===");
        System.out.println("数据文件: src/main/resources/data/address_code_timeline.bin");
        System.out.printf("有时间线的地址码数: %,d%n", timelineCodeCount);
        System.out.printf("时间线条目总数: %,d%n", timelineEntryCount);
        System.out.printf("加载后已用堆: %s%n", fmt(afterBin));
        System.out.printf(".bin 常驻内存增量: %s%n", fmt(binDelta));
        System.out.printf(".properties + .bin 总增量: %s%n%n", fmt(afterBin - baseline));

        printSummary(baseline, afterProps, afterBin, allCodes.size(), binDelta);
    }

    private static void printSummary(long baseline, long afterProps, long afterBin,
                                     int propsCount, long binDelta) {
        System.out.println("========================================");
        System.out.println("  汇总");
        System.out.println("========================================");
        System.out.printf("JVM 基线:                    %s%n", fmt(baseline));
        System.out.printf(".properties 加载后:          %s%n", fmt(afterProps));
        System.out.printf("  └ addressCodeMap 条目数:   %,d%n", propsCount);
        System.out.printf("  └ 增量:                    %s%n", fmt(afterProps - baseline));
        if (afterBin > 0) {
            System.out.printf(".bin 加载后:                 %s%n", fmt(afterBin));
            System.out.printf("  └ timelineMap 增量:       %s%n", fmt(binDelta));
            System.out.printf("  └ 总增量(.properties+.bin): %s%n", fmt(afterBin - baseline));
            // 机器可读行：CI 汇总 job 抓取此行生成跨版本对比表格（单位：字节）
            System.out.printf("PROBE_RESULT_BYTES|baseline=%d|propsDelta=%d|binDelta=%d|totalDelta=%d%n",
                    baseline, afterProps - baseline, binDelta, afterBin - baseline);
        }
        System.out.println("========================================");
    }

    /** 触发 GC 后读取堆已用字节数。 */
    private static long measure() {
        System.gc();
        System.gc();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = mbean.getHeapMemoryUsage();
        return heap.getUsed();
    }

    private static String fmt(long bytes) {
        if (bytes < 0) {
            bytes = 0;
        }
        return String.format("%.2f MB (%,d bytes)", bytes / BYTES_PER_MB, bytes);
    }
}
