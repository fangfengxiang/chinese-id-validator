package cn.chineseid.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.chineseid.validator.IdValidator;
import cn.chineseid.validator.core.CheckBitCalculator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 地址码三模式全量验收测试。
 *
 * <p>加载全部 7185 个时间线地址码，排序后逐个构造合法 18 位身份证号，
 * 分别跑 Basic / Loose / Strict 三种模式，验证：
 * <ul>
 *   <li>各模式通过/不通过数量符合预期
 *   <li>不变量：StrictValid ⟹ LooseValid ⟹ BasicValid
 * </ul>
 *
 * <p>构造方式：固定生日 19900101 + 顺序码 001 + 计算校验码，
 * 确保非地址码因素不影响校验结果。
 *
 * <p>由 maven-failsafe-plugin 在 integration-test 阶段执行（{@code mvn verify}）。
 * 命名约定 {@code *IT.java} 区别于单元测试 {@code *Test.java}（surefire）。
 * 属于功能测试层（全量数据验收），与端到端集成测试 {@link IdValidationIT} 同处 failsafe 层。
 *
 * <p><b>快照断言基线</b>：测试中的 7185/6177/6274 等固定数字是<b>当前数据版本的验收基线</b>，
 * 用于捕获行政区划数据变更导致的意外回归。更新数据后若数字变化，
 * 需人工核对变化原因后再更新基线，勿为让测试通过而直接改数字。
 */
class AddressCodeAcceptanceIT {

    /** 固定生日码（ID 卡时代内，确保不因生日影响地址码判断） */
    private static final String BIRTHDAY = "19900101";
    /** 固定顺序码 */
    private static final String ORDER = "001";

    /**
     * 全量地址码三模式验收。
     */
    @Test
    @DisplayName("7185 个地址码 × 三模式全量验收 + 不变量检查")
    void testAllAddressCodesThreeModes() {
        // 1. 加载全部地址码并排序
        List<Integer> codes = loadAndSortTimelineCodes();
        int total = codes.size();

        // 2. 逐个构造合法 ID 并跑三模式
        int basicPass = 0, strictPass = 0, loosePass = 0;
        int invariantViolations = 0;

        for (int code : codes) {
            String id = buildValidId(code);

            boolean basic = IdValidator.isValidBasic(id);
            boolean strict = IdValidator.isStrictValid(id);
            boolean loose = IdValidator.isLooseValid(id);

            if (basic) basicPass++;
            if (strict) strictPass++;
            if (loose) loosePass++;

            // 不变量：strict → loose → basic
            if (strict && !loose) {
                invariantViolations++;
                System.err.println("不变量违反: strict=true 但 loose=false, code=" + code);
            }
            if (loose && !basic) {
                invariantViolations++;
                System.err.println("不变量违反: loose=true 但 basic=false, code=" + code);
            }
        }

        // 3. 打印验收报告
        System.out.println("========== 地址码三模式验收报告 ==========");
        System.out.println("地址码总数: " + total);
        System.out.println("--- Basic ---  通过: " + basicPass + "  不通过: " + (total - basicPass));
        System.out.println("--- Strict --- 通过: " + strictPass + "  不通过: " + (total - strictPass));
        System.out.println("--- Loose  --- 通过: " + loosePass + "  不通过: " + (total - loosePass));
        System.out.println("不变量违反次数: " + invariantViolations);
        System.out.println("=========================================");

        // 4. 断言
        assertEquals(total, basicPass, "Basic 应全部通过（不检查地址码）");
        assertEquals(0, invariantViolations, "不变量 strict→loose→basic 不应被违反");

        // 预期数量（基于数据分析）
        // ⚠️ 数据版本基线：6177/6274 等数字与当前行政区划数据版本耦合。
        // 更新 address_code_timeline 数据后数量可能合理变化——此时必须人工核对
        // 变化原因（新增/撤销的区划是否符合预期），禁止仅为让测试变绿而直接改数字。
        assertEquals(6177, strictPass, "Strict 通过数应为 6177（数据版本基线，变更需人工核对）");
        assertEquals(1008, total - strictPass, "Strict 不通过数应为 1008（数据版本基线，变更需人工核对）");
        assertEquals(6274, loosePass, "Loose 通过数应为 6274（6177 完整时间线 + 97 历史后缀兼容；数据版本基线，变更需人工核对）");
        assertEquals(911, total - loosePass, "Loose 不通过数应为 911（数据版本基线，变更需人工核对）");
    }

    /**
     * 110201（昌平县，时间线 1981 年结束）边界验证。
     * Strict 不通过，Loose 通过（后缀 01 + 省级 110000 + 地级 110200 证据齐全）。
     */
    @Test
    @DisplayName("110201 边界：Strict 不通过 → Loose 通过（legacy 兼容）")
    void testLegacyCodeBoundary() {
        String id = buildValidId(110201);

        boolean strict = IdValidator.isStrictValid(id);
        boolean loose = IdValidator.isLooseValid(id);
        boolean basic = IdValidator.isValidBasic(id);

        assertTrue(basic, "110201 Basic 应通过");
        assertTrue(!strict, "110201 Strict 应不通过（时间线 1981 结束）");
        assertTrue(loose, "110201 Loose 应通过（后缀 01 + 省级+地级证据）");
        assertTrue(!loose || basic, "不变量：loose 通过则 basic 必通过");
        assertTrue(!strict || loose, "不变量：strict 通过则 loose 必通过");
    }

    /**
     * 130123（正定县，时间线 1986 起）验证。
     * Strict + Loose 均通过（时间线与 [1985, now] 相交）。
     */
    @Test
    @DisplayName("130123 正常用例：Strict + Loose 均通过")
    void testNormalCode() {
        String id = buildValidId(130123);

        assertTrue(IdValidator.isStrictValid(id), "130123 Strict 应通过");
        assertTrue(IdValidator.isLooseValid(id), "130123 Loose 应通过");
        assertTrue(IdValidator.isValidBasic(id), "130123 Basic 应通过");
    }

    /**
     * 110999（不在任何数据源中）验证。
     * Strict + Loose 均不通过。
     */
    @Test
    @DisplayName("110999 不存在的码：Strict + Loose 均不通过")
    void testNonExistentCode() {
        String id = buildValidId(110999);

        assertTrue(!IdValidator.isStrictValid(id), "110999 Strict 应不通过");
        assertTrue(!IdValidator.isLooseValid(id), "110999 Loose 应不通过");
        assertTrue(IdValidator.isValidBasic(id), "110999 Basic 应通过（不检查地址码）");
    }

    // ==================== 工具方法 ====================

    /**
     * 构造合法 18 位身份证号。
     * body = 地址码(6) + 生日码(8) + 顺序码(3)，校验码由 CheckBitCalculator 计算。
     */
    private static String buildValidId(int addressCode) {
        String body = String.format("%06d", addressCode) + BIRTHDAY + ORDER;
        String checkBit = CheckBitCalculator.calculate(body);
        return body + checkBit;
    }

    /**
     * 从 address_code_timeline.json 加载全部地址码并排序。
     */
    @SuppressWarnings("unchecked")
    private static List<Integer> loadAndSortTimelineCodes() {
        Gson gson = new Gson();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                AddressCodeAcceptanceIT.class.getResourceAsStream("/data/address_code_timeline.json"),
                StandardCharsets.UTF_8))) {

            Map<String, List<Map<String, String>>> timeline =
                    gson.fromJson(reader, new TypeToken<Map<String, List<Map<String, String>>>>() {}.getType());

            List<Integer> codes = new ArrayList<>(timeline.size());
            for (String codeStr : timeline.keySet()) {
                codes.add(Integer.parseInt(codeStr));
            }
            Collections.sort(codes);
            return codes;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load timeline data", e);
        }
    }
}
