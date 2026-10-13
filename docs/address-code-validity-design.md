# 地址码有效性校验设计决策

## 背景

### 身份证地址码的语义

身份证号前 6 位为地址码，记录的是**发证时的行政区划**，而非出生时的行政区划。这是理解本文档所有决策的前提。

关键事实：
- **发证 ≠ 出生**：一个人 1980 年出生，可能在 1985 年（16 岁达到办证年龄）或更晚才首次申领身份证，地址码反映的是发证时的区划。
- **身份证时代起点 = 1985 年**：《居民身份证条例》1985 年通过并施行，中国从此开始颁发第一代身份证。1985 年之前不存在身份证号，因此地址码不可能在 1985 年之前被使用。
- **区划变更**：同一地址码在不同时间段可能对应不同的行政区划名称（如 `130123` 在 1986–1992 年对应"栾城县"，1993 年起改为"正定县"），时间线数据记录了这些变更。

### 标准依据

- **GB 11643-1999《公民身份号码》**：定义地址码为"登记户口所在地县（区、市、镇）行政区划代码"。
- **《居民身份证条例》(1985)**：身份证制度的法律起点，确认 1985 年为身份证时代起始年。

## 问题

### 旧实现：只查省级，过于宽松

旧 `checkAddressCode` 通过 `getAddressInfo` 解析省市区，仅检查省级（`addressInfo.get(0)`）是否非空：

```java
// 旧实现
public static boolean checkAddressCode(IdCode idCode, boolean strict) {
    List<String> addressInfo =
            AddressResolver.getAddressInfo(idCode.getAddressCode(), idCode.getBirthdayCode(), strict);
    return !addressInfo.get(0).isEmpty();
}
```

省级码（如 `110000` 北京市）的时间线通常覆盖全年份（start="", end="" → [1, 9999]），因此只要前 2 位是合法省码，任何 6 位码都能通过——包括不存在的码。

**误判 `110999`**：省级 `110000`=北京市有效，但 `110999` 从未在 `address_code.properties`（3209 个当前码）或 `address_code_timeline.json`（7185 个历史码）中出现过。旧实现因只查省级而误判为有效。

### 方案对比：为什么不用出生年匹配

一种直觉的"更严格"做法是检查 6 位码的时间线是否覆盖出生年（`entry.coversYear(birthYear)`）。但这会引入误杀：

**误杀 `130123@1980`**：`130123` 的时间线为：
- 1986–1992：栾城县
- 1993–至今：正定县

出生年 1980 早于时间线起点 1986，`coversYear(1980)` 返回 false → 误判为无效。但该码是合法身份证数据：持证人 1980 年出生，1985+ 年领证，发证时的行政区划（1986 年起为栾城县）被编码进身份证号。

### 数据分析

对 `address_code_timeline.json` 中 902 个多入口码（多条时间线条目的码）的全量分析：

| 维度 | 数值 |
|------|------|
| 时间线码总数 | 7185 |
| 多入口码 | 902 |
| 当前码（`address_code.properties`） | 3209 |
| 当前码无时间线 | 0 |
| 当前码时间线不与 [1985, now] 相交 | **0** |
| `110999` 在数据源中 | **否** |

结论：全部 3209 个当前码的时间线均与 [1985, now] 相交，使用"时间线与 ID 卡时代相交"作为有效性标准不会破坏任何当前码。

## 决策：Option B

**有效性标准**：6 位地址码的时间线与身份证时代 [1985, 当前年] 相交，即为有效。**不查出生年。**

### 核心逻辑

```
有效 ⟺ ∃ entry ∈ timeline(code) : [entry.start, entry.end] ∩ [1985, now] ≠ ∅
```

两个闭区间 [a₁, a₂] 与 [b₁, b₂] 相交当且仅当 `a₁ ≤ b₂ ∧ a₂ ≥ b₁`。

### 为什么选 B

1. **拒绝不存在的码**：`110999` 无时间线且非遗留码 → 无效。✅
2. **接受发证时代内使用的码**：`130123` 时间线 [1986–1992] ∪ [1993–至今] 与 [1985, now] 相交 → 有效，即使出生年为 1980。✅
3. **零侵入**：不动生日校验、假号生成器、时间线数据。生成器从 `address_code.properties` 的 3209 个码中随机选择，全部通过 → 零破坏。✅
4. **标准依据**：GB 11643 + 《居民身份证条例》(1985) 共同界定了"身份证时代"。
5. **全部 3209 当前码通过**：数据验证零误杀。

### 被否决的方案

**Option C**：查出生年，但把时间线下界从出生年"拉回"到 max(出生年, 1985)。

否决理由：
- 需要改动生日校验逻辑（MIN_YEAR 从 1800 改为 1985，或引入特殊处理），侵入性强。
- 逻辑更复杂（涉及出生年与 ID 卡时代下界的 max 运算），可维护性差。
- Option B 已经解决了全部问题，无需额外复杂度。

## 实现

### 改动范围

| 文件 | 改动 |
|------|------|
| `TimelineEntry.java` | 新增 `intersectsRange(int, int)` 方法 |
| `AddressResolver.java` | 新增 `ID_CARD_ERA_START_YEAR=1985` 常量 + `isAddressCodeValid(int)` 方法 |
| `Validator.java` | `checkAddressCode` 改为委托 `AddressResolver.isAddressCodeValid`，移除 `List` 导入 |

### 有效性与地址解析分离

| 职责 | 方法 | 策略 |
|------|------|------|
| **有效性**（该码是否可能出现在真实身份证上） | `AddressResolver.isAddressCodeValid` | 时间线与 [1985, now] 相交，不查出生年 |
| **地址解析**（该码在特定出生年对应的行政区划名称） | `AddressResolver.getAddressInfo` / `getAddress` | 以出生年为启发式线索匹配时间线条目 |

有效性校验不依赖出生年；地址解析仍以出生年为启发式。两者独立，各司其职。

### 关键代码

```java
// AddressResolver
private static final int ID_CARD_ERA_START_YEAR = 1985;

public static boolean isAddressCodeValid(int addressCode) {
    List<TimelineEntry> timeline = DataProvider.getInstance().getTimeline(addressCode);
    if (timeline.isEmpty()) {
        String codeStr = String.format("%06d", addressCode);
        return !handleLegacyCode(codeStr).isEmpty();
    }
    int currentYear = Year.now().getValue();
    for (TimelineEntry entry : timeline) {
        if (entry.intersectsRange(ID_CARD_ERA_START_YEAR, currentYear)) {
            return true;
        }
    }
    return false;
}
```

```java
// TimelineEntry
public boolean intersectsRange(int rangeStart, int rangeEnd) {
    return getStartYearInt() <= rangeEnd && getEndYearInt() >= rangeStart;
}
```

```java
// Validator
public static boolean checkAddressCode(IdCode idCode, boolean strict) {
    return AddressResolver.isAddressCodeValid(idCode.getAddressCode());
}
```

### strict 参数

`checkAddressCode` 保留 `strict` 参数签名以兼容现有 API。Option B 下，strict 不影响地址码有效性判断——两种模式均使用时间线相交检查。strict 仅影响地址解析（`getAddress` 在严格模式下精确匹配年份，宽松模式取最近条目）。

## 测试

| 用例 | 期望 | 说明 |
|------|------|------|
| `110999` 严格模式 | 无效 | 不在数据源中 |
| `110999` 宽松模式 | 无效 | 同上 |
| `130123@1980` 严格模式 | 有效 | 时间线与 [1985, now] 相交 |
| `130123@1980` 宽松模式 | 有效 | 同上 |
| `110101@1990` 严格模式 | 有效 | 正常用例 |
| `isValid("110999199001011234", true)` | false | 完整校验拒绝 |
| `isValid("13012319800101001X", true)` | true | 完整校验通过 |

全部 459 个单元测试通过（454 原有 + 5 新增），零回归。
