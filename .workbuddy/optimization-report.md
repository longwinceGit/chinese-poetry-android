# 诗词乐园 Android 项目 — 优化分析报告

> 基于 CodeGraph MCP 索引分析（33 Java / 32 XML / 1046 节点 / 1750 边），2026-07-01

---

## 1. 项目概况

| 指标 | 当前值 | 问题 |
|------|--------|------|
| compileSdk | 33 (Android 13) | 🔴 落后 2 个大版本 |
| targetSdk | 33 | 🔴 Google Play 要求 34+ |
| minSdk | 21 (Android 5.0) | 🟢 合理，覆盖 99%+ 设备 |
| AGP 版本 | 4.2.2 | 🔴 **严重过时**（当前 8.x） |
| Java 版本 | 1.8 | 🟡 可用 Java 11/17 获取性能提升 |
| Room | 2.4.3 | 🟡 可升 2.6.x |
| Lifecycle | 2.5.1 | 🟡 可升 2.8.x |
| Release 混淆 | **minifyEnabled = false** | 🔴 **未启用！APK 55MB 裸奔** |
| 测试代码 | 0 个测试文件 | 🔴 零测试覆盖 |
| CI/CD | 无 | 🟡 无自动化流水线 |

---

## 2. 按优先级分级的问题清单

### 🔴 P0 — 必须修复（影响安全/上架/崩溃）

#### 2.1 `minifyEnabled = false` — APK 体积 55MB 未压缩

```gradle
// app/build.gradle 第22行
release {
    minifyEnabled false  // ← R8 未启用
}
```

**影响**：assets/ 目录 54MB JSON 数据 + 代码全部裸打包。启用 R8 至少可缩减 20-30%。

**修复**：
```gradle
release {
    minifyEnabled true
    shrinkResources true
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
}
```

#### 2.2 AGP 4.2.2 — 无法编译 targetSdk 34+

Google Play 现已要求 `targetSdkVersion >= 34`。AGP 4.2.2 不支持新版 SDK 编译。

**修复**：升级到 AGP 8.2+，同步升级 Gradle 到 8.9。

#### 2.3 安全配置缺陷

| 配置项 | 当前值 | 风险 |
|--------|-------|------|
| `usesCleartextTraffic` | `true` | HTTP 明文传输 |
| `allowBackup` | `true` | 用户数据可被提取 |
| 网络安全配置 | 无 | 无证书固定 |

**修复**：
- `usesCleartextTraffic="false"`（或仅对特定域名开放）
- `allowBackup="false"` 或配置 `android:fullBackupContent`
- 添加 `network_security_config.xml`

#### 2.4 `SimpleDateFormat` 线程安全问题

```java
// LearningEngine.java 第9行 — 静态字段！
private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
```

`SimpleDateFormat` **不是线程安全的**。在多个 ViewModel 后台线程中调用 `getToday()`/`isYesterday()` 会产生产生错误日期甚至崩溃。

**修复**：使用 `java.time.LocalDate`（API 26+）或 `ThreadLocal<SimpleDateFormat>`。

---

### 🟡 P1 — 强烈建议（影响性能/稳定性）

#### 2.5 TtsManager 内存泄漏风险

```java
// DetailFragment.java 第71行
ttsManager = new TtsManager(requireContext());
```

创建时机在 `onViewCreated`，但只在 `onDestroyView` 可能调用 `shutdown()`。如果 Fragment 被重建（配置变更），旧 TTS 引擎未释放。

**修复**：
1. 在 `onDestroyView()` 中显式调用 `ttsManager.shutdown()`
2. 或转移到 ViewModel 中管理生命周期

#### 2.6 54MB JSON 资产全量加载到内存

```java
// PoemLoader.java — 30 个 JSON 文件循环加载
List<Poem> all = new ArrayList<>();
// ... 逐文件解析，全部塞入内存
```

**影响**：
- 冷启动时加载所有诗词 → 主线程等待（实际在 Thread 中，但阻塞启动流程）
- 占用 50MB+ 堆内存

**优化方案**：
| 方案 | 预期效果 |
|------|---------|
| **Android App Bundle** | 按需下载，APK 减至 10-15MB |
| **Gson/Jackson Streaming** | 边解析边释放，减 30% 内存 |
| **分朝代懒加载** | 首屏只加载"全部"预览，按需拉取 |
| **ProtoBuf** | JSON → pb，文件体积缩 60% |

#### 2.7 搜索无防抖 — 每个字符触发全量过滤

```java
// HomeFragment.java 第106行 — TextWatcher 直接调搜索
public void onTextChanged(CharSequence s, int a, int b, int c) {
    viewModel.search(s.toString());  // ← 每输入一个字都触发
}
```

**修复**：添加 300ms debounce。

```java
private Runnable searchRunnable;
etSearch.addTextChangedListener(new TextWatcher() {
    public void onTextChanged(CharSequence s, ...) {
        handler.removeCallbacks(searchRunnable);
        searchRunnable = () -> viewModel.search(s.toString());
        handler.postDelayed(searchRunnable, 300);
    }
});
```

#### 2.8 ViewModel scope 过宽

```java
// MatchGameFragment.java 第48行
viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
```

使用 `requireActivity()` 作为 owner，多个 Game Fragment 共享同一个 ViewModel 实例。如果两个游戏页面同时存在于回退栈中，状态会互相干扰。

**修复**：改为 `ViewModelProvider(this)` 使用 Fragment 自身 scope。

#### 2.9 静态单例 Repository — 无 DI

```java
// HomeViewModel.java 第19行
private PoemRepository repo = PoemRepository.getInstance();
```

- 所有 ViewModel 硬编码依赖 `PoemRepository.getInstance()`
- **无法 mock**，**无法单元测试**
- `LearningDatabase` 也是通过 `getInstance(app)` 在 ViewModel 构造中创建

**修复**：引入 Hilt/Koin 做依赖注入，或至少通过 ViewModelFactory 传参。

---

### 🟢 P2 — 建议优化（改善体验/维护性）

#### 2.10 无暗色主题

```xml
<!-- styles.xml — 只有 Light 主题 -->
<style name="Theme.Poetry" parent="Theme.AppCompat.Light.NoActionBar">
```

**修复**：继承 `Theme.AppCompat.DayNight.NoActionBar` 并配置 `values-night/colors.xml`。

#### 2.11 硬编码色值/尺寸

```java
// MatchCardAdapter.java 第63行
drawable.setStroke(2, Color.parseColor("#E0E0E0"));
tv.setTextColor(Color.parseColor("#333333"));
tv.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, 120)); // ← 硬编码120dp
```

这些值应移至 `colors.xml` 和 `dimens.xml`。

#### 2.12 资源字符串缺失

`strings.xml` 仅 1 行：
```xml
<string name="app_name">诗词乐园</string>
```

而代码中大量硬编码中文：
- `"正在加载诗词..."` (HomeViewModel)
- `"🎉 全部配对成功！"` (MatchGameFragment)
- 等级名称数组 (LearningEngine)

这些都应提取到 `strings.xml` 便于国际化。

#### 2.13 缺少网络状态处理

`AndroidManifest.xml` 声明了 `INTERNET` 权限，搜索功能"可选使用网络"，但代码中没有：
- 网络状态检测
- 超时处理
- 离线优雅降级

#### 2.14 无崩溃上报

未集成 Firebase Crashlytics / Bugly。生产环境崩溃无法感知。

---

## 3. 结构化优化路线图

### Phase 1 — 安全与合规（1-2天）
```
□ 升级 AGP 4.2.2 → 8.2+
□ 升级 compileSdk 33 → 35, targetSdk 33 → 35
□ minifyEnabled = true + shrinkResources = true
□ 添加 proguard-rules.pro
□ usesCleartextTraffic = false
□ allowBackup = false
```

### Phase 2 — 性能与稳定性（2-3天）
```
□ SimpleDateFormat → java.time
□ TtsManager shutdown 生命周期修复
□ 搜索 300ms debounce
□ ViewModel scope 从 requireActivity 改为 Fragment
□ assets 54MB JSON → App Bundle 按需分发
```

### Phase 3 — 架构升级（3-5天）
```
□ 引入 Hilt DI（或 ViewModelFactory）
□ Room 2.4.3 → 2.6.x + 迁移测试
□ Lifecycle 2.5.1 → 2.8.x
□ Java 1.8 → Java 11（性能 + 语法糖）
□ 集成 Crashlytics / Bugly
```

### Phase 4 — 体验优化（持续）
```
□ DayNight 主题
□ 硬编码字符串 → strings.xml
□ 硬编码颜色 → colors.xml
□ 添加单元测试（至少 Repository 层）
□ CI/CD（GitHub Actions 自动构建 APK）
```

---

## 4. 代码质量速查表

| 文件 | 行数 | 问题 |
|------|------|------|
| `DetailFragment.java` | ~200+ | TTS 未在 onDestroyView 清理 |
| `HomeFragment.java` | ~200+ | 搜索无 debounce |
| `HomeViewModel.java` | 150 | `new Thread()` 裸用，无线程池 |
| `MatchCardAdapter.java` | 86 | 硬编码色值、尺寸；`getItemId` 返回 position（反模式） |
| `GameViewModel.java` | 148 | `savePoints` 中 `new Thread()` |
| `MatchGameFragment.java` | 127 | ViewModel scope = requireActivity |
| `QuizGenerator.java` | ~37 symbols | 需确认题目 shuffle 质量 |
| `LearningEngine.java` | ~100 | `SimpleDateFormat` 线程不安全 |
| `PoemLoader.java` | 114 | 无 InputStream try-with-resources |
| `confettiView.java` | 51 symbols | 自定义 View 需确认 onDraw 效率 |

---

## 5. 总结

**核心三件事，按紧迫度排**：

1. **🔴 立即**：AGP + targetSdk 升级 + `minifyEnabled=true` — 否则上不了 Google Play
2. **🟡 短期**：54MB assets → App Bundle、搜索 debounce、TTS 内存泄漏、SimpleDateFormat
3. **🟢 中期**：Hilt DI、暗色主题、字符串国际化、测试覆盖、Crashlytics

整体代码结构清晰（MVVM + Repository + Room），基础架构方向正确。主要短板在**工程化**（构建、测试、混淆）和**细节打磨**（线程安全、内存管理、资源规范）。
