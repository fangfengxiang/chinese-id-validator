# API 参考

所有 API 通过门面类 `IdValidator` 静态调用。

> `440308199901101512` 和 `610104620927690` 示例大陆居民身份证均为随机生成的假数据，如撞车，请联系删除。
> `810000199408230021` 和 `830000199201300022` 示例港澳台居民居住证为北京市公安局公布的居住证样式号码。

## 方法总结

| 方法 | 参数 | 返回值 | 说明 |
|---|---|---|---|
| `isValid(id, strict)` | 身份证号, 是否严格 | `boolean` | 验证合法性（strict=true 走严格，false 走宽松） |
| `isLooseValid(id)` | 身份证号 | `boolean` | 宽松验证（不校验地址码） |
| `isStrictValid(id)` | 身份证号 | `boolean` | 严格验证（校验地址码是否存在） |
| `isValidBasic(id)` | 身份证号 | `boolean` | 基础验证（仅校验顺序码/生日码/校验码） |
| `getInfo(id, strict)` | 身份证号, 是否严格 | `IdInfo` | 获取完整信息（含地址） |
| `getBasicInfo(id)` | 身份证号 | `BasicIdInfo` | 获取基础信息（不含地址） |
| `fakeId()` | 无 | `String` | 生成随机 18 位假号 |
| `fakeRequireId(builder)` | `FakeIdBuilder` | `String` | 按条件生成假号 |
| `upgradeId(id)` | 15 位身份证号 | `String` | 升级为 18 位 |
| `setCustomDataSource(ds)` | `CustomDataSource` | `void` | 注入自定义数据源 |
| `clearCustomDataSource()` | 无 | `void` | 清除自定义数据源 |

## 验证

三种验证模式，严格程度递减：

```java
// 严格：18 位 + 校验地址码是否存在
IdValidator.isValid("440308199901101512", true);  // true
IdValidator.isValid("610104620927690", true);     // true（15 位）

// 港澳台居民居住证（8 开头地址码）
IdValidator.isValid("810000199408230021", true);  // 香港特别行政区 → true
IdValidator.isValid("830000199201300022", true);  // 台湾省 → true

// 三种模式对比
IdValidator.isValid("440308199901101512", true);   // 严格：校验地址码是否存在
IdValidator.isLooseValid("440308199901101512");     // 宽松：不校验地址码
IdValidator.isValidBasic("440308199901101512");     // 基础：仅校验顺序码/生日码/校验码
```

## 获取完整信息 — `getInfo`

当身份证号合法时，返回 `IdInfo` 对象（地址、出生日期、星座、生肖、性别、校验位等），不合法抛出 `IllegalArgumentException`：

```java
IdInfo info = IdValidator.getInfo("440308199901101512", true);

info.getAddressCode();    // 440308                        — 地址码
info.getAbandoned();      // 0                             — 是否废弃（0=使用中, 1=已废弃）
info.getAddress();        // "广东省深圳市盐田区"            — 地址全称
info.getAddressTree();    // ["广东省", "深圳市", "盐田区"]  — 省市区三级列表
info.getBirthday();       // 1999-01-10                    — 出生日期（LocalDate）
info.getConstellation();  // "摩羯座"                       — 星座
info.getChineseZodiac();  // "卯兔"                         — 生肖
info.getSex();            // Sex.MALE                      — 性别
info.getLength();         // IdType.EIGHTEEN               — 号码类型（15 位或 18 位）
info.getCheckBit();       // "2"                           — 校验码
```

港澳台居民居住证只返回省级地址信息：

```java
IdInfo hkInfo = IdValidator.getInfo("810000199408230021", true);
hkInfo.getAddress();      // "香港特别行政区"
hkInfo.getAddressTree();  // ["香港特别行政区", "", ""]

IdInfo twInfo = IdValidator.getInfo("830000199201300022", true);
twInfo.getAddress();      // "台湾省"
twInfo.getAddressTree();  // ["台湾省", "", ""]
```

## 获取基础信息 — `getBasicInfo`

`getBasicInfo` 跳过地址码校验，仅返回生日、星座、生肖、性别等基础信息，适用于不关心地址的场景：

```java
BasicIdInfo info = IdValidator.getBasicInfo("440308199901101512");
info.getBirthday();       // 1999-01-10
info.getConstellation();  // "摩羯座"
info.getChineseZodiac();  // "卯兔"
info.getSex();            // Sex.MALE
info.getLength();         // IdType.EIGHTEEN
info.getCheckBit();       // "2"
```

## 生成假号 — `fakeId` / `fakeRequireId`

`fakeId` 生成可通过校验的假身份证号，支持随机生成或按条件（地区、生日、性别、位数）生成：

```java
// 随机 18 位
IdValidator.fakeId();

// 按条件生成（Builder 模式）
IdValidator.fakeRequireId(new FakeIdBuilder()
        .eighteen()
        .address("上海市")
        .birthday("2000")
        .sex(Sex.MALE));                          // 2000 年上海市男性 18 位

IdValidator.fakeRequireId(new FakeIdBuilder()
        .fifteen()
        .address("南山区")
        .birthday("1999")
        .sex(Sex.FEMALE));                        // 1999 年深圳市南山区女性 15 位

IdValidator.fakeRequireId(new FakeIdBuilder()
        .eighteen()
        .address("江苏省")
        .birthday("200001")
        .sex(Sex.MALE));                           // 2000 年 1 月江苏省男性 18 位

IdValidator.fakeRequireId(new FakeIdBuilder()
        .eighteen()
        .address("台湾省")
        .birthday("20131010")
        .sex(Sex.FEMALE));                         // 2013 年 10 月 10 日台湾省女性居住证

IdValidator.fakeRequireId(new FakeIdBuilder()
        .eighteen()
        .address("香港特别行政区")
        .birthday("19970701")
        .sex(Sex.FEMALE));                         // 1997 年 7 月 1 日香港特别行政区女性居住证
```

**FakeIdBuilder 参数说明：**

| 参数 | 方法 | 说明 | 默认值 |
|---|---|---|---|
| 位数 | `.eighteen()` / `.fifteen()` | 生成 18 位或 15 位 | 18 位 |
| 地址 | `.address("江苏省")` | 省/市/区官方全称，如 "北京市"、"深圳市"、"朝阳区" | 全国随机 |
| 生日 | `.birthday("200001")` | 支持 "2000"、"198801"、"19990101" 三种精度 | 随机 |
| 性别 | `.sex(Sex.MALE)` | `Sex.MALE`（男）/ `Sex.FEMALE`（女） | FEMALE |

## 升级 15 位 — `upgradeId`

将 15 位身份证号升级为 18 位（自动补全出生年份前缀 "19" 并计算校验码）：

```java
IdValidator.upgradeId("610104620927690");  // → 18 位身份证号
```

## 注入自定义数据源 — `setCustomDataSource`

实现 `CustomDataSource` 接口，在构造函数中初始化数据，通过 `setCustomDataSource` 注入：

```java
// 1. 实现接口（构造函数即初始化，无需 init 方法）
public class MyDataSource implements CustomDataSource {
    public MyDataSource() {
        // 初始化数据加载
    }

    @Override
    public List<TimelineEntry> getTimeline(int code) {
        // 返回自定义时间线，空列表 = 无自定义数据
    }

    @Override
    public String getAddressName(int code) {
        // 返回自定义地址名，null = 无自定义数据
    }

    // 可选：override getMergeMode() 切换策略，默认 FALLBACK
    @Override
    public MergeMode getMergeMode() {
        return MergeMode.MERGE; // 自定义优先，覆盖内置
    }
}

// 2. 注入
IdValidator.setCustomDataSource(new MyDataSource());

// 3. 清除（恢复纯内置数据）
IdValidator.clearCustomDataSource();
```

**合并策略：**

| 策略 | 行为 | 适用场景 |
|---|---|---|
| `FALLBACK`（默认） | 内置有数据则不查自定义源，仅填充缺失 | 仅补缺，不动已有数据 |
| `MERGE` | 总是查自定义源，与内置合并，自定义优先 | 补充/覆盖内置数据 |
