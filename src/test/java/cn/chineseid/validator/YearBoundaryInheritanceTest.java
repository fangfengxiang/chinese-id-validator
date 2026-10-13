package cn.chineseid.validator;

import static org.junit.jupiter.api.Assertions.*;

import cn.chineseid.validator.data.CustomDataSource;
import cn.chineseid.validator.data.MergeMode;
import cn.chineseid.validator.model.IdInfo;
import cn.chineseid.validator.model.TimelineEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 年份边界继承测试。
 *
 * <p>验证两个已修复的 Bug：
 * <ul>
 *   <li><b>Bug A</b>：{@code normalizeBoundaries} 和 {@code AddressResolver} 的 endYear 累加器
 *       跨条目继承空年份字段，导致最后一条目的空 endYear 继承前一条目的 endYear，
 *       产生 start > end 的非法区间，使当前名称永远无法匹配。
 *       修复：空年份字段独立填充默认值，不跨条目继承。
 *   <li><b>Bug B</b>：非严格模式回退固定取某一条（首条或末条），当查询年份超出所有条目
 *       范围时语义不正确：早于所有记录返回最新而非最近，且 MERGE 模式下末条是内置数据
 *       可能返回旧区而非自定义新区。
 *       修复：回退改为契约 E — 取距查询年份最近的条目（每条都评估距离），早于所有记录
 *       返回最早条目，晚于所有返回最新条目，距离相等时 MERGE 下自定义在前优先。
 * </ul>
 *
 * <p>新增 <b>P2</b>：{@code startYear > endYear} 的非法区间应在 {@code normalizeBoundaries}
 * 中 fail-fast 抛 {@link IllegalArgumentException}，而非静默产生永远无法匹配的条目。
 *
 * <p>内置数据中受影响的典型代码：
 * <ul>
 *   <li>120110：东郊区(至1991) → 东丽区(1992起)
 *   <li>120111：西郊区(至1991) → 西青区(1992起)
 *   <li>130123：栾城县(1986-1992) → 正定县(1993起)
 *   <li>130124：正定县(1986-1992) → 栾城县(1993-2013)
 *   <li>130204：东矿区(至1994) → 古冶区(1995起)
 * </ul>
 */
@DisplayName("年份边界继承测试")
class YearBoundaryInheritanceTest {

    @AfterEach
    void cleanup() {
        TestStateResetter.resetDataProvider();
    }

    // ==================== 辅助方法 ====================

    private static String buildValidId(int addressCode, String birthday) {
        String body = String.format("%06d", addressCode) + birthday + "123";
        String checkBit = cn.chineseid.validator.core.CheckBitCalculator.calculate(body);
        return body + checkBit;
    }

    private static String districtOf(int addressCode, String birthday) {
        String id = buildValidId(addressCode, birthday);
        IdInfo info = IdValidator.getInfo(id, false);
        List<String> tree = info.getAddressTree();
        return tree.size() >= 3 ? tree.get(2) : "";
    }

    // ==================== Bug A：endYear 空值继承导致非法区间 ====================

    @Nested
    @DisplayName("Bug A：内置多条目代码 endYear 空值不应跨条目继承")
    class BugA_BuiltInMultiEntry {

        @Test
        @DisplayName("120110 改名后查询返回新名（东丽区）而非旧名（东郊区）")
        void shouldReturnCurrentNameAfterRename_120110() {
            // 数据：东郊区(start="", end="1991") → 东丽区(start="1992", end="")
            // 修复前：东丽区继承 endYear=1991 → (1992,1991) 非法区间 → 永远不匹配 → 回退东郊区
            // 修复后：东丽区 endYear="" → (1992,9999) → 2020年匹配 → 返回东丽区
            assertEquals("东丽区", districtOf(120110, "20200101"), "2020年应返回东丽区");
        }

        @Test
        @DisplayName("120110 改名前查询返回旧名（东郊区）")
        void shouldReturnOldNameBeforeRename_120110() {
            assertEquals("东郊区", districtOf(120110, "19850101"), "1985年应返回东郊区");
        }

        @Test
        @DisplayName("120110 边界年份精确匹配：1991=旧名，1992=新名")
        void shouldMatchExactBoundaryYear_120110() {
            assertEquals("东郊区", districtOf(120110, "19910101"), "1991年应返回东郊区");
            assertEquals("东丽区", districtOf(120110, "19920101"), "1992年应返回东丽区");
        }

        @Test
        @DisplayName("120111 改名后返回西青区而非西郊区")
        void shouldReturnCurrentNameAfterRename_120111() {
            assertEquals("西青区", districtOf(120111, "20200101"), "2020年应返回西青区");
            assertEquals("西郊区", districtOf(120111, "19850101"), "1985年应返回西郊区");
        }

        @Test
        @DisplayName("130204 改名后返回古冶区而非东矿区")
        void shouldReturnCurrentNameAfterRename_130204() {
            assertEquals("古冶区", districtOf(130204, "20200101"), "2020年应返回古冶区");
            assertEquals("东矿区", districtOf(130204, "19900101"), "1990年应返回东矿区");
        }

        @Test
        @DisplayName("130123 改名后返回正定县而非栾城县")
        void shouldReturnCurrentNameAfterRename_130123() {
            // 数据：栾城县(1986-1992) → 正定县(1993-至今)
            assertEquals("栾城县", districtOf(130123, "19920101"), "1992年应返回栾城县");
            assertEquals("正定县", districtOf(130123, "19930101"), "1993年应返回正定县");
            assertEquals("正定县", districtOf(130123, "20200101"), "2020年应返回正定县");
        }

        @Test
        @DisplayName("多个受影响代码批量验证：改名后均返回新名")
        void shouldReturnNewNamesForAllAffectedCodes() {
            // 从 270 个受影响代码中抽样验证
            String[][] cases = {
                {"120110", "东丽区"}, // 东郊区→东丽区
                {"120111", "西青区"}, // 西郊区→西青区
                {"120112", "津南区"}, // 南郊区→津南区
                {"120113", "北辰区"}, // 北郊区→北辰区
                {"130204", "古冶区"}, // 东矿区→古冶区
            };
            for (String[] c : cases) {
                int code = Integer.parseInt(c[0]);
                String expected = c[1];
                String actual = districtOf(code, "20200101");
                assertEquals(expected, actual, "代码 " + c[0] + " 在 2020 年应返回 " + expected);
            }
        }
    }

    // ==================== Bug A：自定义数据源多条目 endYear 空值 ====================

    @Nested
    @DisplayName("Bug A：自定义多条目时间线 endYear 空值独立处理")
    class BugA_CustomMultiEntry {

        @Test
        @DisplayName("MERGE 模式：最后一条目空 endYear 表示至今，不继承前一条目")
        void shouldNotInheritEndYearAcrossEntriesInCustomTimeline() {
            // 自定义时间线：旧区(空-1991) → 新区(1992-空)
            // 空 endYear 应表示"至今"，不应继承前一条目的 endYear=1991
            List<TimelineEntry> timeline =
                    Arrays.asList(new TimelineEntry("旧区", "", "1991"), new TimelineEntry("新区", "1992", ""));

            CustomDataSource source =
                    new TestDataSource(Collections.emptyMap(), Collections.singletonMap(110999, timeline));
            IdValidator.initialize(source, MergeMode.MERGE);

            // 2020年应匹配"新区"（1992-9999），不应回退到"旧区"
            String result = districtOf(110999, "20200101");
            assertEquals("新区", result, "最后一条目空 endYear 应表示至今，2020年应匹配新区");
        }

        @Test
        @DisplayName("MERGE 模式：第一条目空 startYear 表示自古")
        void emptyStartYearMeansFromBeginning() {
            List<TimelineEntry> timeline =
                    Arrays.asList(new TimelineEntry("旧区", "", "1991"), new TimelineEntry("新区", "1992", ""));

            CustomDataSource source =
                    new TestDataSource(Collections.emptyMap(), Collections.singletonMap(110999, timeline));
            IdValidator.initialize(source, MergeMode.MERGE);

            // 1980年应匹配"旧区"（0001-1991）
            assertEquals("旧区", districtOf(110999, "19800101"), "空 startYear 应表示自古，1980年应匹配旧区");
        }
    }

    // ==================== Bug B：非严格回退取距查询年份最近的条目（契约 E） ====================

    @Nested
    @DisplayName("Bug B：非严格回退取距查询年份最近的条目（契约 E）")
    class BugB_NonStrictFallback {

        @Test
        @DisplayName("查询年份晚于所有条目范围时返回最新条目（距离最近）")
        void shouldReturnLatestEntryWhenYearExceedsAllRanges() {
            // 130124：正定县(1986-1992) → 栾城县(1993-2013)
            // 2020年超出所有条目范围 → 非严格回退
            // 契约 E：栾城县 dist=7（2020-2013） < 正定县 dist=28（2020-1992）→ 栾城县
            String result = districtOf(130124, "20200101");
            assertEquals("栾城县", result, "年份晚于所有条目时应返回距查询年份最近的条目（栾城县）");
        }

        @Test
        @DisplayName("查询年份早于所有条目范围时返回最早条目（距离最近，非最新）")
        void shouldReturnEarliestEntryWhenYearBeforeAllRanges() {
            // 130123：栾城县(1986-1992) → 正定县(1993-至今)
            // 1980年早于第一条目的 startYear=1986
            // 契约 E：栾城县 dist=6（1986-1980） < 正定县 dist=13（1993-1980）→ 栾城县
            assertEquals("栾城县", districtOf(130123, "19800101"), "年份早于所有条目时应返回距查询年份最近的条目（栾城县，非末条正定县）");
        }

        @Test
        @DisplayName("自定义数据：晚于所有返回最近（B区），早于所有返回最近（A区）")
        void shouldReturnNearestEntryForCustomTimelineWithExplicitYears() {
            List<TimelineEntry> timeline =
                    Arrays.asList(new TimelineEntry("A区", "1990", "2000"), new TimelineEntry("B区", "2005", "2010"));

            CustomDataSource source =
                    new TestDataSource(Collections.emptyMap(), Collections.singletonMap(110999, timeline));
            IdValidator.initialize(source, MergeMode.MERGE);

            // 2020年超出所有条目范围 → B区 dist=10（2020-2010）最近
            assertEquals("B区", districtOf(110999, "20200101"), "晚于范围应返回距查询年份最近的 B区");
            // 1980年早于所有条目范围 → A区 dist=10（1990-1980）最近（非末条 B区）
            assertEquals("A区", districtOf(110999, "19800101"), "早于范围应返回距查询年份最近的 A区");
        }

        @Test
        @DisplayName("等距时取第一个条目（MERGE 下自定义在前即自定义优先）")
        void shouldReturnFirstEntryWhenDistanceTie() {
            // A区(1990-1995) + B区(2005-2010)，查询 2000年
            // A区 dist=5（2000-1995），B区 dist=5（2005-2000）→ 等距，取第一个 A区
            List<TimelineEntry> timeline =
                    Arrays.asList(new TimelineEntry("A区", "1990", "1995"), new TimelineEntry("B区", "2005", "2010"));

            CustomDataSource source =
                    new TestDataSource(Collections.emptyMap(), Collections.singletonMap(110999, timeline));
            IdValidator.initialize(source, MergeMode.MERGE);

            assertEquals("A区", districtOf(110999, "20000101"), "等距时应取第一个条目（MERGE 下自定义在前即自定义优先）");
        }

        @Test
        @DisplayName("中间缺口年份：返回缺口两侧中距离较小的条目")
        void shouldReturnNearestSideOfGap() {
            // A区(1990-2000) + B区(2005-2010)，查询 2002年（缺口中间）
            // A区 dist=2（2002-2000） < B区 dist=3（2005-2002）→ A区
            List<TimelineEntry> timeline =
                    Arrays.asList(new TimelineEntry("A区", "1990", "2000"), new TimelineEntry("B区", "2005", "2010"));

            CustomDataSource source =
                    new TestDataSource(Collections.emptyMap(), Collections.singletonMap(110999, timeline));
            IdValidator.initialize(source, MergeMode.MERGE);

            assertEquals("A区", districtOf(110999, "20020101"), "缺口中间应返回距离较近的 A区");
        }
    }

    // ==================== P2：start>end 非法区间校验 ====================

    @Nested
    @DisplayName("P2：start>end 非法区间应 fail-fast 抛 IllegalArgumentException")
    class P2_InvalidYearRange {

        @Test
        @DisplayName("自定义数据源 start>end 时 isValid 抛 IllegalArgumentException（配置错误不误判为号码非法）")
        void isValidThrowsWhenStartGreaterThanEnd() {
            List<TimelineEntry> timeline = Collections.singletonList(new TimelineEntry("异常区", "2000", "1990"));

            CustomDataSource source =
                    new TestDataSource(Collections.emptyMap(), Collections.singletonMap(110999, timeline));
            IdValidator.initialize(source, MergeMode.MERGE);

            // start=2000 > end=1990 → normalizeBoundaries 抛 IllegalArgumentException
            // isValid 只捕获输入解析异常，数据配置错误属于系统异常，向调用方传播
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> IdValidator.isValid(buildValidId(110999, "20200101"), false),
                    "start>end 非法区间应使 isValid 抛 IllegalArgumentException");
            assertTrue(ex.getMessage().contains("start=2000"), "异常消息应包含 start=2000");
            assertTrue(ex.getMessage().contains("end=1990"), "异常消息应包含 end=1990");
        }

        @Test
        @DisplayName("自定义数据源 start>end 时 getInfo 抛 IllegalArgumentException")
        void getInfoThrowsWhenStartGreaterThanEnd() {
            List<TimelineEntry> timeline = Collections.singletonList(new TimelineEntry("异常区", "2000", "1990"));

            CustomDataSource source =
                    new TestDataSource(Collections.emptyMap(), Collections.singletonMap(110999, timeline));
            IdValidator.initialize(source, MergeMode.MERGE);

            String id = buildValidId(110999, "20200101");
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> IdValidator.getInfo(id, false),
                    "start>end 非法区间应使 getInfo 抛 IllegalArgumentException");
            assertTrue(ex.getMessage().contains("start=2000"), "异常消息应包含 start=2000");
            assertTrue(ex.getMessage().contains("end=1990"), "异常消息应包含 end=1990");
        }

        @Test
        @DisplayName("合法区间（start<end）正常工作，不误报")
        void validRangeShouldNotThrow() {
            List<TimelineEntry> timeline = Collections.singletonList(new TimelineEntry("合法区", "1990", "2000"));

            CustomDataSource source =
                    new TestDataSource(Collections.emptyMap(), Collections.singletonMap(110999, timeline));
            IdValidator.initialize(source, MergeMode.MERGE);

            assertEquals("合法区", districtOf(110999, "19950101"), "合法区间应正常匹配");
        }

        @Test
        @DisplayName("空年份边界不触发校验（start 空 end 显式 视为无下界）")
        void emptyBoundariesShouldNotTriggerValidation() {
            // 空 startYear → DEFAULT_START_YEAR，不与显式 endYear 比较触发 start>end
            List<TimelineEntry> timeline = Collections.singletonList(new TimelineEntry("半空区", "", "1990"));

            CustomDataSource source =
                    new TestDataSource(Collections.emptyMap(), Collections.singletonMap(110999, timeline));
            IdValidator.initialize(source, MergeMode.MERGE);

            assertEquals("半空区", districtOf(110999, "19850101"), "空 startYear 不应触发 start>end 校验");
        }
    }

    // ==================== 跨数据源不继承（回归守卫） ====================

    @Nested
    @DisplayName("跨数据源年份边界不继承（回归守卫）")
    class CrossSourceNonInheritance {

        @Test
        @DisplayName("MERGE 模式：自定义条目年份范围不扩展到内置数据")
        void shouldNotExpandCustomRangeToBuiltin() {
            // 内置 110101：东城区(空-空 = 0001-9999)
            // 自定义 110101：自定义东城区(1990-1995)
            // MERGE 后：[Custom(1990-1995), BuiltIn(0001-9999)]
            // 查询 2000年：Custom 不匹配(2000>1995)，BuiltIn 匹配 → 东城区
            CustomDataSource source = new TestDataSource(
                    Collections.emptyMap(),
                    Collections.singletonMap(
                            110101, Collections.singletonList(new TimelineEntry("自定义东城区", "1990", "1995"))));
            IdValidator.initialize(source, MergeMode.MERGE);

            IdInfo info = IdValidator.getInfo(buildValidId(110101, "20000101"), false);
            assertTrue(info.getAddress().contains("东城区"), "2000年应匹配内置东城区");
            assertFalse(info.getAddress().contains("自定义"), "不应回退到自定义条目");
        }

        @Test
        @DisplayName("MERGE 模式：自定义优先匹配同一年份")
        void shouldPreferCustomForOverlappingYear() {
            // 内置 110101：东城区(空-空)
            // 自定义 110101：自定义东城区(1990-1995)
            // 查询 1992年：Custom(1990-1995) 先匹配 → 自定义东城区
            CustomDataSource source = new TestDataSource(
                    Collections.emptyMap(),
                    Collections.singletonMap(
                            110101, Collections.singletonList(new TimelineEntry("自定义东城区", "1990", "1995"))));
            IdValidator.initialize(source, MergeMode.MERGE);

            IdInfo info = IdValidator.getInfo(buildValidId(110101, "19920101"), false);
            assertTrue(info.getAddress().contains("自定义东城区"), "1992年应优先匹配自定义数据");
        }
    }

    // ==================== 单条目空年份全覆盖 ====================

    @Nested
    @DisplayName("单条目空年份覆盖所有年份")
    class SingleEntryEmptyYears {

        @Test
        @DisplayName("内置单条目代码（空年份）覆盖所有年份")
        void shouldCoverAllYearsForSingleEntryWithEmptyYears() {
            // 110101 东城区：单条目 start="" end="" → (0001-9999) 覆盖所有年份
            assertTrue(districtOf(110101, "19600101").contains("东城区"), "1960年应返回东城区");
            assertTrue(districtOf(110101, "20200101").contains("东城区"), "2020年应返回东城区");
        }
    }

    // ==================== 测试用 DataSource 实现 ====================

    private static final class TestDataSource implements CustomDataSource {

        private final Map<Integer, String> addressNames;
        private final Map<Integer, List<TimelineEntry>> timelines;

        TestDataSource(Map<Integer, String> addressNames, Map<Integer, List<TimelineEntry>> timelines) {
            this.addressNames = (addressNames == null) ? Collections.emptyMap() : addressNames;
            this.timelines = (timelines == null) ? Collections.emptyMap() : timelines;
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
