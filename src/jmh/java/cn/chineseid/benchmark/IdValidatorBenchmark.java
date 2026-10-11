package cn.chineseid.benchmark;

import cn.chineseid.validator.IdValidator;
import cn.chineseid.validator.model.IdInfo;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * 核心 API 微基准。CI 全绿后自动触发（benchmark.yml workflow_run），多 JDK 矩阵并行跑，report 汇总横向对比表。
 * 不进 PR 门禁（CI 共享 runner 噪声大于测量信号）。也可手动触发（workflow_dispatch）。
 * 运行：mvn -Pbenchmark package -DskipTests && java -jar target/*-benchmarks.jar
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class IdValidatorBenchmark {

    /** 合法 18 位身份证（与测试数据集一致） */
    private static final String VALID_18 = "110101199003076536";

    /** 校验位错误的 18 位证（走到校验位计算才被拒，覆盖接近完整路径） */
    private static final String INVALID_CHECKBIT_18 = "110101199003076534";

    @Benchmark
    public boolean isValid_valid() {
        return IdValidator.isValid(VALID_18, false);
    }

    @Benchmark
    public boolean isValid_invalidCheckBit() {
        return IdValidator.isValid(INVALID_CHECKBIT_18, false);
    }

    @Benchmark
    public boolean isValidBasic_valid() {
        return IdValidator.isValidBasic(VALID_18);
    }

    @Benchmark
    public IdInfo getInfo_valid() {
        return IdValidator.getInfo(VALID_18, false);
    }
}
