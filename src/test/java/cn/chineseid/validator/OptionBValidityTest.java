package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

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
    @DisplayName("110201（昌平县，时间线 1981 年结束）不在 ID 卡时代 [1985,now] → 无效")
    void shouldRejectPreIdCardEraCode() {
        IdCode code = CodeParser.parse("110201199001011234");
        assertFalse(Validator.checkAddressCode(code, true), "110201 strict should be invalid");
        assertFalse(Validator.checkAddressCode(code, false), "110201 loose should be invalid");
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
