/**
 * Nacos 集成示例 — chinese-id-validator 自定义数据源接入参考。
 *
 * <p>此文件为独立参考示例，不纳入核心库 Maven 构建和测试生命周期。
 * 运行时所需外部依赖（Nacos client、JSON parser）由使用者自行提供。
 *
 * <p>使用步骤：
 * <ol>
 *   <li>应用启动时从 Nacos 获取自定义行政区划配置</li>
 *   <li>将配置解析为 {@code Map<Integer, String>} 和 {@code Map<Integer, List<TimelineEntry>>}</li>
 *   <li>实现 {@link cn.chineseid.validator.data.CustomDataSource} 接口</li>
 *   <li>调用 {@link cn.chineseid.validator.IdValidator#initialize} 一次性安装</li>
 *   <li>后续正常调用 IdValidator 静态 API</li>
 * </ol>
 *
 * <p>外部依赖（使用者自行在 pom.xml 中声明）：
 * <pre>{@code
 * <dependency>
 *     <groupId>com.alibaba.nacos</groupId>
 *     <artifactId>nacos-client</artifactId>
 *     <version>2.x</version>
 * </dependency>
 * }</pre>
 *
 * <p>核心库本身零依赖，不引入 Nacos、Spring、Jackson 等。
 */
import cn.chineseid.validator.IdValidator;
import cn.chineseid.validator.data.CustomDataSource;
import cn.chineseid.validator.data.MergeMode;
import cn.chineseid.validator.model.TimelineEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 以下为示例代码框架，实际使用时需要引入 Nacos client SDK
// import com.alibaba.nacos.client.config.NacosConfigService;
// import com.alibaba.nacos.api.config.ConfigService;
// import com.alibaba.nacos.api.config.listener.Listener;

public class NacosExample {

    public static void main(String[] args) {
        // 1. 从 Nacos 获取配置（伪代码，需自行实现 Nacos 连接）
        // ConfigService configService = createNacosConfigService("127.0.0.1:8848");
        // String json = configService.getConfig("address-code-data", "DEFAULT_GROUP", 5000);

        // 2. 解析配置为数据结构（伪代码，需自行实现 JSON 解析）
        // Map<Integer, String> addressNames = parseAddressNames(json);
        // Map<Integer, List<TimelineEntry>> timelines = parseTimelines(json);

        // 3. 构建 CustomDataSource（使用真实数据替换 mock 数据）
        Map<Integer, String> addressNames = Collections.emptyMap(); // 替换为真实数据
        Map<Integer, List<TimelineEntry>> timelines = Collections.emptyMap(); // 替换为真实数据
        CustomDataSource source = new NacosDataSource(addressNames, timelines);

        // 4. 一次性初始化（原子发布，全 JVM 生效）
        IdValidator.initialize(source, MergeMode.MERGE);

        // 5. 正常使用 IdValidator 静态 API
        boolean valid = IdValidator.isValid("440308199901101512", true);
        System.out.println("Valid: " + valid);

        // 注意：initialize() 只能调用一次，再次调用会抛 IllegalStateException。
        // 如需刷新数据，需重启 JVM 或使用独立类加载器。
    }

    /**
     * 基于 Nacos 配置的自定义数据源实现。
     *
     * <p>数据在构造时确定，后续不可变。线程安全（只读访问）。
     */
    static final class NacosDataSource implements CustomDataSource {

        private final Map<Integer, String> addressNames;
        private final Map<Integer, List<TimelineEntry>> timelines;

        NacosDataSource(
                Map<Integer, String> addressNames,
                Map<Integer, List<TimelineEntry>> timelines) {
            this.addressNames = Collections.unmodifiableMap(new HashMap<>(addressNames));

            Map<Integer, List<TimelineEntry>> defensive = new HashMap<>();
            for (Map.Entry<Integer, List<TimelineEntry>> entry : timelines.entrySet()) {
                List<TimelineEntry> raw = entry.getValue();
                List<TimelineEntry> safe =
                        (raw == null)
                                ? Collections.emptyList()
                                : new ArrayList<>(raw);
                defensive.put(entry.getKey(), Collections.unmodifiableList(safe));
            }
            this.timelines = Collections.unmodifiableMap(defensive);
        }

        @Override
        public List<TimelineEntry> getTimeline(int code) {
            return timelines.getOrDefault(code, Collections.emptyList());
        }

        @Override
        public String getAddressName(int code) {
            return addressNames.get(code);
        }
    }
}
