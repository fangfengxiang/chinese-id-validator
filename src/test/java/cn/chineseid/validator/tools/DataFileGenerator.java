package cn.chineseid.validator.tools;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 数据文件生成器（开发期工具，不进入运行时 JAR）。
 *
 * 从原始 JSON 生成运行时使用的二进制/Properties 数据文件：
 *   address_code.json           -> address_code.properties  (Properties 文本格式，UTF-8)
 *   address_code_timeline.json  -> address_code_timeline.bin (DataOutputStream 二进制序列化)
 *
 * chinese_zodiac.json / constellation.json 已硬编码为 DataProvider 常量，无需生成。
 *
 * 二进制格式（与 DataProvider.loadTimelineMap 严格对应，读写对称）：
 *   mapSize(int) [code(int) entryCount(int) [address(UTF) startYear(UTF) endYear(UTF)]*]*
 *
 * 运行（需在项目根目录，Maven classpath 含 gson）：
 *   make data              # 生成全部
 *   make data-properties   # 仅 address_code.properties
 *   make data-timeline     # 仅 address_code_timeline.bin
 */
public final class DataFileGenerator {

    private static final Path DATA_DIR = Paths.get("src/main/resources/data");
    private static final Gson GSON = new Gson();

    public static void main(String[] args) throws Exception {
        String cmd = args.length > 0 ? args[0] : "all";
        switch (cmd) {
            case "properties":
                generateProperties();
                break;
            case "timeline":
                generateTimelineBin();
                break;
            case "all":
                generateProperties();
                generateTimelineBin();
                break;
            default:
                System.err.println("未知参数: " + cmd + "（可选: all | properties | timeline）");
                System.exit(1);
        }
    }

    /**
     * address_code.json -> address_code.properties
     *
     * 按地址码升序输出，UTF-8 直写（与现有文件一致，无 Properties.store 的时间戳头），
     * 保证生成结果可复现、可 diff。
     */
    static void generateProperties() throws IOException {
        Path in = DATA_DIR.resolve("address_code.json");
        Path out = DATA_DIR.resolve("address_code.properties");

        Map<String, String> map = readJsonMap(in);
        List<String> codes = sortedCodes(map);

        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(out.toFile()), StandardCharsets.UTF_8))) {
            for (String code : codes) {
                w.write(code);
                w.write('=');
                w.write(escapeValue(map.get(code)));
                w.write('\n');
            }
        }
        System.out.println("[OK] " + in.getFileName() + " -> " + out.getFileName()
                + " (" + codes.size() + " 条)");
    }

    /**
     * address_code_timeline.json -> address_code_timeline.bin
     *
     * 按地址码升序写入，保证可复现。字段顺序 address/start_year/end_year
     * 与 DataProvider.loadTimelineMap 的 readUTF 顺序严格对称。
     */
    static void generateTimelineBin() throws IOException {
        Path in = DATA_DIR.resolve("address_code_timeline.json");
        Path out = DATA_DIR.resolve("address_code_timeline.bin");

        String json = Files.readString(in);
        Type type = new TypeToken<Map<String, List<Map<String, String>>>>() {}.getType();
        Map<String, List<Map<String, String>>> raw = GSON.fromJson(json, type);
        if (raw == null) {
            throw new IOException("无法解析 JSON: " + in);
        }

        List<String> codes = new ArrayList<>(raw.keySet());
        codes.sort(Comparator.comparingInt(Integer::parseInt));

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(out.toFile())))) {
            dos.writeInt(codes.size());
            for (String code : codes) {
                List<Map<String, String>> entries = raw.get(code);
                if (entries == null) {
                    entries = Collections.emptyList();
                }
                dos.writeInt(Integer.parseInt(code));
                dos.writeInt(entries.size());
                for (Map<String, String> e : entries) {
                    dos.writeUTF(e.getOrDefault("address", ""));
                    dos.writeUTF(e.getOrDefault("start_year", ""));
                    dos.writeUTF(e.getOrDefault("end_year", ""));
                }
            }
        }
        System.out.println("[OK] " + in.getFileName() + " -> " + out.getFileName()
                + " (" + codes.size() + " 个地址码)");
    }

    // ---- 工具方法 ----------------------------------------------------------

    private static Map<String, String> readJsonMap(Path path) throws IOException {
        String json = Files.readString(path);
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> map = GSON.fromJson(json, type);
        if (map == null) {
            throw new IOException("无法解析 JSON: " + path);
        }
        return map;
    }

    private static List<String> sortedCodes(Map<String, String> map) {
        List<String> codes = new ArrayList<>(map.keySet());
        codes.sort(Comparator.comparingInt(Integer::parseInt));
        return codes;
    }

    /**
     * Properties value 转义（Java Properties 规范）：
     * 反斜杠、换行、回车、制表符、换页符转义；行首 #/! 转义。
     * 中文 UTF-8 直写（与现有文件一致，不做 Unicode 转义）。
     */
    private static String escapeValue(String v) {
        if (v == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(v.length());
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if ((c == '#' || c == '!') && sb.length() == 0) {
                        sb.append('\\');
                    }
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private DataFileGenerator() {
    }
}
