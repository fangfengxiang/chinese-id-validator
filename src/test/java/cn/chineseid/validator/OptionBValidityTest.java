package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.core.CheckBitCalculator;
import cn.chineseid.validator.core.CodeParser;
import cn.chineseid.validator.core.Validator;
import cn.chineseid.validator.model.IdCode;
import cn.chineseid.validator.model.IdInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Option B 有效性校验测试：地址码时间线与身份证时代 [1985, now] 相交即为有效，不查出生年。
 */
@DisplayName("Option B: 地址码时间线与 ID 卡时代相交校验")
class OptionBValidityTest {

    @Test
    @DisplayName("110999 不在数据源中 → 无效（严格+宽松）")
    void shouldRejectNonExistentCode() {
        IdCode code = CodeParser.parse("110999199001011234");
        assertFalse(Validator.checkAddressCode(code, true), "110999 strict should be invalid");
        assertFalse(Validator.checkAddressCode(code, false), "110999 loose should be invalid");
    }

    @Test
    @DisplayName("130123@1980 出生年早于时间线起点 1986 → 仍有效（时间线与 ID 卡时代相交）")
    void shouldAcceptPreIdCardEraBirth() {
        // body=13012319800101001, sum=142, 142%11=10, idx=(12-10)%11=2, CHECK_BITS[2]='X'
        IdCode code = CodeParser.parse("13012319800101001X");
        assertTrue(Validator.checkAddressCode(code, true), "130123@1980 strict should be valid");
        assertTrue(Validator.checkAddressCode(code, false), "130123@1980 loose should be valid");
    }

    @Test
    @DisplayName("110101@1990 正常用例 → 有效")
    void shouldAcceptNormalCase() {
        IdCode code = CodeParser.parse("110101199003076536");
        assertTrue(Validator.checkAddressCode(code, true), "110101@1990 should be valid");
    }

    @Test
    @DisplayName("isValid 对 110999 → false")
    void shouldRejectInvalidId() {
        assertFalse(IdValidator.isValid("110999199001011234", true), "110999 should be invalid");
    }

    @Test
    @DisplayName("isValid 对 130123@1980 → true（校验码正确时）")
    void shouldAcceptValidPre1985BirthId() {
        // body=13012319800101001, sum=142, 142%11=10, idx=(12-10)%11=2, CHECK_BITS[2]='X'
        assertTrue(IdValidator.isValid("13012319800101001X", true), "130123@1980 should be valid");
    }

    // ========== 边界场景 ==========

    @Test
    @DisplayName("110201（昌平县，时间线 1981 年结束）→ Strict 无效，Loose 有效（legacy 后缀 01 + 省级+地级证据）")
    void shouldRejectPreIdCardEraCode() {
        // Loose 契约语义（docs/identity-number-domain.md 2.2/2.3）：
        // 接受"结构合理但无法证明在 1985 年后使用过"的历史码。
        // 注意：地级 110200 的证据区间（终于 2014）与 110201 自身时间线（终于 1981）时间不相交——
        // 上级证据是受约束结构推断，不证明同期历史隶属关系。这是 Loose 的明文契约，不是 bug。
        IdCode code = CodeParser.parse("110201199001011234");
        assertFalse(Validator.checkAddressCode(code, true), "110201 strict should be invalid");
        assertTrue(
                Validator.checkAddressCode(code, false), "110201 loose should be valid (legacy 01 + parent evidence)");
    }

    @Test
    @DisplayName("132701（秦皇岛市，终于 1982）→ Strict/Loose 均无效：地级 132700（唐山地区）时间线终于 1982，无身份证时代证据")
    void shouldRejectLegacyCodeWhenParentEvidenceMissing() {
        // 132701 满足 legacy 后缀 01，省级 130000（河北省）证据成立，
        // 但地级 132700 时间线终于 1982、不在映射表 → 上级证据不足 → Loose 拒绝。
        // 校验位按真实算法计算，确保基础校验通过，隔离地址码为唯一失败因素。
        String body = "13270119900101123";
        String id = body + CheckBitCalculator.calculate(body);

        assertTrue(IdValidator.isValidBasic(id), "基础校验应通过（隔离地址码因素）");
        assertFalse(IdValidator.isStrictValid(id), "132701 strict should be invalid（时间线终于 1982，不与身份证时代相交）");
        assertFalse(IdValidator.isLooseValid(id), "132701 loose should be invalid（地级 132700 无身份证时代证据）");
    }

    @Test
    @DisplayName("810000（香港特别行政区）港澳台码 → 有效")
    void shouldAcceptHmtCode() {
        IdCode code = CodeParser.parse("810000199001011234");
        assertTrue(Validator.checkAddressCode(code, true), "810000 should be valid");
    }

    @Test
    @DisplayName("130123@1980 15 位身份证（无校验码）→ isValid true")
    void shouldAcceptPre1985Birth15DigitId() {
        // 15 位：130123 800101 001 → 生日补 19 前缀 → 19800101
        assertTrue(IdValidator.isValid("130123800101001", true), "15-digit 130123@1980 should be valid");
    }

    @Test
    @DisplayName("getInfo 对 130123@1980 → 省级非空（地址解析仍以出生年为启发式）")
    void shouldResolveProvinceForPre1985Birth() {
        IdInfo info = IdValidator.getInfo("13012319800101001X", true);
        assertNotNull(info);
        assertFalse(info.getAddressTree().get(0).isEmpty(), "province should be resolved");
    }

    @Test
    @DisplayName("多个多入口码 + 1980 出生年 → 全部有效")
    void shouldAcceptMultipleMultiEntryCodes() {
        // 130123 正定县（1986 栾城 / 1993 正定）, 320581 太仓市（时间线 1993 起）
        // 360734 龙南县, 411625 鹿邑县 — 多入口码代表
        String[] multiEntryIds = {"13012319800101001X", "320581198001010013", "360734198001010019", "41162519800101001X"
        };
        for (String id : multiEntryIds) {
            assertTrue(
                    Validator.checkAddressCode(CodeParser.parse(id), true),
                    "multi-entry code should be valid: " + id.substring(0, 6));
        }
    }
}
