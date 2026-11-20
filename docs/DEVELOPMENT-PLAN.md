# HerRhythm Android · 开发计划 V1.1（评审修订版）

| 项目 | 内容 |
| --- | --- |
| 需求基线 | `HerRhythm_Android_App_开发需求文档_V1.2.md`（AC-01～AC-23） |
| 设计基线 | `design/desing.pen`（HerRhythm DS v1.0 + 18 张页面稿 + 交互规范） |
| 目标设备 | `192.168.1.26:41015` · NX729J · nubia/RedMagicOS · Android 15 (SDK 35) · arm64-v8a |
| 屏幕 | 物理 1116×2480，density 480 → **逻辑 372dp 宽**（偏窄，是一等约束） |
| 设备身份 | `ro.build.type=user`，`ro.debuggable=0`；**无 root / 无 su / 无 sqlite3 / 无 tcpdump** |
| 修订说明 | V1.1 吸收 3 路评审（需求合规 / 架构风险 / 可执行性）共 20 项发现，修正 D2 决策，新增 D9–D14，补 AC 判据矩阵 |

---

## 0. 环境核验结论（全部实测）

| 项 | 结论 |
| --- | --- |
| adb | ✅ `device`，屏亮，可 tap/swipe/keyevent/screencap/uiautomator dump |
| 构建 | ✅ 必须 `JAVA_HOME=/opt/homebrew/opt/openjdk@17`（系统 `java` 是空壳桩） |
| 安装启动 | ✅ `install -r` + `am start` + `screencap` 全通，截图确认渲染正常 |
| GMS | ✅ 存在 `com.google.android.gms`（但仍用 bundled ML Kit，避免 GMS 变体差异） |
| **TTS 引擎** | ❌ **不存在**（`TTS_SERVICE → No services found`，零 tts 包）→ 推翻 V1.0 的 D2 |
| **TalkBack** | ❌ 不存在（只有 `accessibilitymenu`）→ AC-10 只能部分验证 |
| sqlite3 | ❌ 设备无 → 查库改为 `run-as ... cat` 拉回主机（含 `-wal`/`-shm`） |
| 模拟来电 | ❌ 真机无 emulator console → AC-06 用音频焦点抢占代理验证 |
| 飞行模式 | ⚠️ adb 走 `wlan0`，开飞行模式会**切断自己的调试通道** → 改用 `svc data disable` + 静态网络审计 |
| macOS 中文语音 | ✅ Tingting(zh_CN) 等 8 个 → **用它构建期生成语音素材**（见 D2′） |

---

## 1. 关键决策记录

| ID | 决策 | 理由 / 影响 |
| --- | --- | --- |
| D1 | 缺 FacePhys AAR → 新建 `:facephys-api` 桩模块；`RppgSensor` **只经 `FacePhysBridge.kt` 一个文件**触碰 SDK | 真实 AAR 到手只改 1 文件 + 1 行依赖。桩**永不返回生理数值**（返回 null），杜绝伪造风险 |
| **D2′** | **语音 = 构建期预生成中文音频素材**：macOS `say -v Tingting` 生成 → `afconvert` 转 AAC `.m4a` → 打包 `assets/voice/`；运行期 `MediaPlayer` + `PlaybackParams.setSpeed` 支持 0.8/1.0/1.2x | **修正 V1.0 的 TTS 决策**（该设备无 TTS 引擎，原方案不可验证）。此方案精确回到需求 §8.1「ExoPlayer（预录音频）」，完全离线、零设备依赖。已实测生成成功 |
| D3 | 摄像头做**完整真链路**（CameraX → bundled ML Kit 人脸 → 裁剪 9×9×3 RGB → 投喂），末端投给桩 | 9×9×3 是文档列明的高风险点，必须真机验证 |
| D4 | 范围 = P0 全量 + P1 设置页基础版 + 本地提醒 | 演示模式是 P0 强制项，设置页是其载体 |
| D5 | 每阶段一次 git commit | 便于回滚与增量评审 |
| D6 | **字号以需求红线为准**：正文 20sp / 正文次 18sp / 关键数字 ≥32sp / 标签 14–15sp（非正文） | 设计稿正文 13–16sp 与需求 FR-15、AC-09 冲突，且**设计稿自身红线也写「正文 ≥20sp」**。取需求为准，视觉层级靠字重/颜色维持。副产物：372dp 窄屏 + 20sp 必须全程可滚动 |
| D7 | 不引入 Hilt，手写 `AppContainer` | 少一个 KSP 插件，降低编译风险与构建时间 |
| D8 | 字体：中文用系统 Sans，数字读数用 `FontFamily.Monospace`；不引入字体资产 | Inter / IBM Plex Mono **不覆盖中文**，中文必然回退系统字体，引入等于白增体积 |
| **D9** | FR-14 演示数据**写库 + `mode=simulated` + 数据层统一过滤 + 记录页独立「演示记录」区带水印** | 需求原文自相矛盾（「不写库」vs「标记 simulated 并在记录页水印展示」）。此裁定同时满足两处：不计入打卡/统计/周报（过滤），但又按第 261 行要求在记录页以水印样式展示 |
| **D10** | 保留需求 §5.6 强制的**节律圆环 + 常驻「节律提示」标注**，同时保留设计的阶段时间轴 | 设计稿用时间轴替代了圆环，但「圆环标注节律提示」是明文「必须」的合规红线（防指标被误读为肌肉状态）。两者并存：时间轴在上，圆环+倒计时居中 |
| **D11** | 计时模型 = **纯函数**：`state = timelineAt(elapsedActiveMs)`，其中 `elapsedActiveMs = elapsedRealtime() - startRealtime - pausedMs`；100ms tick **只驱动重绘，不推进状态** | tick 累加式模型在锁屏/丢帧/暂停边界必然漂移（评审指出三条失败路径）。纯函数模型下丢帧不影响正确性，`elapsedRealtime` 天然跨深睡，AC-02/AC-05 自动成立 |
| **D12** | 相机**不绑 Activity 生命周期**：`SensorCoordinator` 为 application-scoped，`ON_STOP` 停投喂、`ON_START` 续投喂，**SDK 窗口在内存中保留** | 满足 AC-05「锁屏 30s 返回指标恢复」（窗口未丢，恢复即出值）；同时满足 §5.3「暂停保持投喂、页面销毁 release」。FGS 类型用 `mediaPlayback`（真播音频，非伪装），审核无风险 |
| **D13** | `MainActivity` 设 `launchMode="singleTop"` + `onNewIntent` 写入 `MutableStateFlow` 由 NavHost 消费；训练页为**顶级路由**（非 Tab 图内节点） | 否则通知点击会建第二实例 → 双 NavHost + 双相机绑定 + 双 tick（评审指出） |
| **D14** | S1 即落 `Modifier.semantics { testTagsAsResourceId = true }` + `UiTag` 常量表 | 实测 Compose 节点 `resource-id` 全为空，只能靠中文文案定位；而 S9 要改文案 → 不留到 S9 就是全部脚本失效 |

### D15（评审后追加）：对比度不给文本开 3:1 的口子

需求 FR-15 只写「对比度 ≥ 4.5:1」，**没有大字号豁免条款**。
首版 `Contrast` 自行引入了 WCAG 的 3:1「大字号」档，并用于 4 组配对 —— 这属于替需求做让步。
现裁定：**3:1 只用于确实不是文字的元素**（进度轨填充、状态圆点）；
所有文字（含 32sp 指标值、36sp 倒计时）一律按 4.5:1 执行。
副产物：心率 token 由 `#E11D48`（canvas 底仅 4.34:1）下调为 `#D6193F`（白底 5.16 / canvas 4.76）。

### D16（评审后追加）：`configChanges` 不含 `fontScale`

首版在 `configChanges` 里声明了 `fontScale`，导致改系统字号时 Activity 不重建、
Compose 复用 `remember` 的测量缓存 —— AC-09「字号最大档不破版」这条最重要的验收动作
会永远测不到真实重建路径。现已移除。

### 事实更正：ML Kit 依赖链（首版注释有误）

首版注释写「bundled 版不依赖设备 GMS」。用 `./gradlew :app:dependencies` 核实后：
`com.google.mlkit:face-detection:16.1.7` 的**模型确实随包**
（APK 内 `libface_detector_v2_jni.so` 在，无 GMS 服务的设备也能检出人脸），
但传递依赖**确实包含** `play-services-mlkit-face-detection:17.1.0`、
`com.google.android.datatransport:transport-*` 与 `firebase-encoders-*` ——
即带一条遥测上传通道。注释已改正，并纳入 AC-03「完全离线」的静态审计范围。

### 数据模型裁定（D9 配套）

`TrainingSession` 全 15 字段落地，其中 4 个承重字段明确写入者：
- `signalCoverage` ← `QualityGate` 在 Complete 时汇总（VALID 样本数 / 总样本数），**FR-09 的 60% 门限依赖它**
- `avgHr` / `avgBreathingRate` ← 仅当 `signalCoverage ≥ 0.6` 时才写，否则 NULL
- `sdkVersion` ← `FacePhysBridge.diagnostics().sdkVersion`（P03 诊断区同源）
- `planVersion` ← `TrainingParams.hash()`（sha1 前 8 位），使 §12.1 临床调参可追溯

`SensorQuality` 5 字段全落地；`Consent` 补 `consentedAt`/`revokedAt`；`DailyProgress` 由 `SessionRepository` 在每次成功写入后重算（不存冗余统计，避免不一致）。

### `TrainingParams`（§5.4 全 12 项，含默认值与区间）

`prepSec 5(3–10) / contractSec 5(3–10) / holdSec 5(3–10) / relaxSec 5(5–15) / repsPerSet 8(3–12) / sets 1(1) / voiceSpeed 1.0(0.8–1.2) / previewHeight 720 / analysisFps 12(8–15) / metricsIntervalMs 1000 / hrMinSec 30 / brMinSec 60 / sqiThreshold 0.50`
→ 数据类 + `validate()` 区间校验 + `hash()` 生成 `planVersion`。演示模式 `repsPerSet=3`。

---

## 2. 技术选型（锁定）

| 类别 | 选型 |
| --- | --- |
| 语言/UI | Kotlin 2.0.21 + Compose BOM 2024.12.01，单 Activity |
| 编译 | AGP 8.7.3 / JDK 17 / minSdk 26 / targetSdk 35 / `applicationId=com.diqi.demo` |
| KSP | `2.0.21-1.0.28`（**必须与 Kotlin 严格同前缀**；已在 Gradle 中落地并构建通过） |
| 存储 | Room 2.6.1 + DataStore 1.1.1 |
| 相机 | CameraX 1.4.1 + **`com.google.mlkit:face-detection:16.1.7`（bundled，不依赖 GMS）** |
| 导航 | Navigation Compose 2.8.5 |
| 后台 | WorkManager 2.10.0（提醒）+ 前台服务（训练不停表，`mediaPlayback`） |
| 音频 | `MediaPlayer` + `AudioAttributes(USAGE_MEDIA)` + `PlaybackParams.setSpeed` |
| 架构 | MVVM + StateFlow + 手写 `AppContainer`；`TrainingEngine` 纯 Kotlin、注入 Clock |

---

## 3. 包结构

```
com.diqi.demo
├─ MainActivity.kt / HerRhythmApp.kt(AppContainer)
├─ core/     time(Clock, MonotonicClock, TestClock) · text(BannedWords) · contrast(Contrast)
├─ data/     db(Entity×5, Dao×5, HerRhythmDb) · prefs(AppSettingsStore, ConsentStore) · repo(×4)
├─ domain/   training(TrainingEngine, TrainingState, TrainingPhase, TrainingParams)
│            plan(SevenDayPlan, DayContent) · quality(QualityGate, SensorQualityState)
├─ sensor/   SensorAdapter · NullSensor · DemoSensor · RppgSensor
│            FacePhysBridge.kt  ← 唯一 SDK 接触点（AAR 替换点）
│            SensorCoordinator(app-scoped) · camera(CameraController, FaceAnalyzer, FaceRoiExtractor)
├─ audio/    VoiceGuide(接口) · AssetVoiceGuide(MediaPlayer) · VoiceScript(文案单一来源)
├─ service/  TrainingForegroundService · TrainingRuntime(快照/恢复)
├─ share/    ShareCardBuilder
└─ ui/       theme · nav · components(×12) · screens(p01…p09) · UiTag.kt
```

---

## 4. 行动清单（9 阶段）

> 每阶段固定 7 步：**① 评审方案 → ② 编码 → ③ 端到端真机验证 → ④ 编排多角度 CodeReview → ⑤ 修复 → ⑥ 复验 → ⑦ commit**

| 阶段 | 名称 | 主覆盖 AC | 出口标准（真机可见） |
| --- | --- | --- | --- |
| **S1** | 工程基建 + 设计系统 + SDK 桩 + 自动化基座 | — | `assembleDebug` 一次成功；设计系统页截图与 `.pen` 色板/字阶一致；`uiautomator dump` 能看到 `resource-id`（testTag 生效） |
| **S2** | 数据层 + 七天内容 + 设置/同意 + 造数入口 | — | 写 session → `run-as cat` 拉库 → 主机读到 → 杀进程重启读回；`DemoModeFilter` 单测绿 |
| **S3** | 训练引擎（纯函数计时）+ 语音素材 + 播放器 | AC-02, AC-07（逻辑）, AC-13（逻辑） | 单测全绿：暂停冻结/恢复无跳变、不补造整组、锁屏跨深睡不漂移 |
| **S4** | P01 引导筛查 + 导航骨架 + P02 首页 | AC-20 | 装机→引导→筛查分流→首页 Day1 主题 + 三态状态行；完成一次后首页转 Day2 |
| **S5** | P04 训练中（圆环+时间轴）+ 前台服务 + 无感知闭环 | AC-01, AC-02, AC-05, AC-06, AC-07 | 跑满 8 次；暂停 10s 无跳变；第 3 次结束 `completedReps=2`；锁屏 30s 误差 ≤1s；冷启动 <2s |
| **S6** | 感知层（Null/Demo/四态）+ 摄像头真链路 + P03 校准 | AC-04, AC-15, AC-16, AC-17, AC-18, AC-19（水印） | 校准页四类反馈真实触发；前 30s 无数字；遮脸 3s 内转角标；进出 5 次无相机异常；演示水印 |
| **S7** | P05 完成自评 + P06 记录七天 + 漏尿日记 | AC-08, AC-11, AC-12, AC-23 | 连点完成仅 1 条；覆盖率 <60% 不显示指标；日历数字与库一致；日记全跳过 0 脏数据 |
| **S8** | P08 家人周报 + P09 设置 + 本地提醒 | AC-21, AC-22, AC-19（不进周报） | 未开分享只能预览；分享面板仅天数/时长；提醒可触发 |
| **S9** | 适老化 + 无障碍 + 红线 + 全量回归 + 性能功耗 | AC-03, AC-09, AC-10, AC-14 + 全 23 条重跑 | 1.3×/2.0× 字号不破版；content-desc 覆盖率 100%；禁词 0 命中；冷启动/掉帧/耗电/温升达标 |

---

## 5. 各阶段方案细则

### S1 · 工程基建 + 设计系统 + SDK 桩 + 自动化基座

**这是最容易被低估的一步**——评审已核实仓库当前**只有** Compose/ui/material3/activity-compose/lifecycle/core-ktx：无 Room、无 KSP 插件、无 CameraX、无 ML Kit、无 Navigation、无 WorkManager、无 DataStore、manifest 无前台服务权限、`launchMode` 未设。所以 S1 的**第一件事是补齐依赖并一次跑通 `assembleDebug`**，把所有版本冲突在一个 commit 内暴露完。

**产出**
1. **依赖与插件**：root 加 KSP 插件；app 加 Room+KSP+Navigation+CameraX+ML Kit(bundled)+WorkManager+DataStore；manifest 补 `FOREGROUND_SERVICE`/`FOREGROUND_SERVICE_MEDIA_PLAYBACK`/`POST_NOTIFICATIONS`/`RECEIVE_BOOT_COMPLETED`、service 声明、`launchMode="singleTop"`；`local.properties` → `BuildConfig.FACEPHYS_APP_KEY`（缺省空串不崩）。
2. **`:facephys-api` 桩模块**（`settings.gradle.kts` 加 `include`）：包 `com.facephys.rppg`，暴露文档所述 API 面
   `RppgConfig(encAsset, appKey)` / `initAsync(cb)`→`success` / `processFrame(roi: ByteArray, dtSec: Float)` / `hasEnoughWindow` / `readMetrics()`→`Metric?` / `getHrvAsync()` / `release()` / `reset()` / `diagnostics()`。
   **桩只维护窗口计数，任何取数一律返回 null**——宁可「永远无数据」也不产生未加水印的数字（FR-08 / AC-14）。
3. **`FacePhysBridge.kt`**：唯一 SDK 接触点，把上述封装为 suspend + `InitFailure{NOT_AUTHORIZED, NOT_ACTIVATED, PACKAGE_MISMATCH, NO_NETWORK, QUOTA_EXCEEDED, UNKNOWN}` + `diagnostics()`（sdkVersion / licenseDaysLeft / sqi）。文件头标注 **「真实 AAR 替换点：改此处 + 1 行依赖」**，并显式写明待确认项：FQN、`dtSec` 单位、ROI 字节序（RGB 交错假定）、init 回调线程、release 后误调风险。
4. **ROI 契约单测**：`FaceRoiExtractor` 用 **golden vector** 测试（3×3 已知像素 → 断言 9×9×3 逐通道值与 `size==243`），把评审指出的「字节序/单位错不崩但永远垃圾」这一风险钉死在单测里。
5. **设计系统**：`Color.kt` 全 18 语义 token（ink/ink2/ink3/line/surface/canvas/hr/rr/brand/warn/good/danger/panel/panel2/accent/onAccent/hairline/debug + 派生 bg 四色）、`Type.kt`（D6 字阶）、`Dimens.kt`（sp 4–40 / 圆角 16-12-999 / 触控 44dp）、`HerRhythmTheme.kt`。
6. **对比度单测**：`Contrast.kt` 实现 WCAG 相对亮度比 + `ContrastTest` 断言**所有实际使用的前景/背景 token 配对 ≥ 4.5:1**（正文）/ ≥ 3:1（大字号）。这直接补上评审指出的「FR-15 对比度全文 0 命中」缺口。
7. **自动化基座**：`UiTag` 常量表 + 根 `Modifier.semantics { testTagsAsResourceId = true }`；`tools/e2e.sh`（`install` / `launch` / `tap(tag)` / `shot(name)` / `db()` / `logcat-clear`）；`tools/db-pull.sh`（含 `-wal`/`-shm`，主机 python3 sqlite3 查）。

**E2E**：`assembleDebug` 成功 → 装 `DesignSystemScreen`（仅 debug 入口）→ 截图与 `.pen` 逐块比对 → `uiautomator dump` 断言 `resource-id` 非空。

---

### S2 · 数据层 + 七天内容 + 设置/同意 + 造数入口

**产出**
- Room 5 实体全字段（见 §1 数据模型裁定）+ DAO；`@TypeConverters`；`id` 幂等 `upsert`。
- **统一过滤**：`SessionQueries` 只暴露已过滤接口；`DemoModeFilterTest` 用反射遍历所有 `@Query` 断言含 `mode != 'simulated'`。
- DataStore 两套：`AppSettings`、`Consent`（含 `consentedAt`/`revokedAt`）。
- `SevenDayPlan`：Day1–7 目标/首页主题/一句提示/能力开关；`dayIndexFor(firstCompletionAt, now)` 纯函数。
- `SessionRepository` 单一写入入口 + `DailyProgress` 重算。
- **debug 造数入口**：`am broadcast -a com.diqi.demo.DEBUG_SEED -e days 3`（`BuildConfig.DEBUG` 关死），解决评审指出的「无 root 改不了系统时间 → AC-12/AC-20 无合法造数路径」。

**E2E**：写入 → `tools/db-pull.sh` 拉库 → 主机断言字段 → `am force-stop` 重启读回 → 单测绿。

---

### S3 · 训练引擎（纯函数计时）+ 语音素材

**产出**
- `TrainingEngine`：**D11 纯函数模型**。输入 `elapsedActiveMs`，输出 `TrainingFrame(state, phase, phaseRemainingMs, phaseTotalMs, repIndex, totalReps, progress)`。累计时长求和的纯计算 → 无 tick 累加、无暂停边界跳变、锁屏跨深睡零漂移。
  - 暂停：`pausedMs += now - pauseStartedAt`；恢复后 `elapsedActiveMs` 连续。
  - 结束：`completedReps` = 已完整走完 Contract→Hold→Relax 的次数，**不补造**。
  - 快照 `EngineSnapshot` → DataStore，供「继续/丢弃」。
- **语音素材生成脚本** `tools/gen-voice.sh`：`say -v Tingting -r 165` → `afconvert -f m4af -d aac -b 64000` → `app/src/main/assets/voice/*.m4a`，覆盖 §5.5 全 6 条 + 4 个阶段提示 + 完成语，共 ~12 条。音频由 `VoiceScript` 单一来源驱动（文案与音频同名同源）。
- `AssetVoiceGuide`：`MediaPlayer` + `AudioAttributes(USAGE_MEDIA, CONTENT_TYPE_SPEECH)`；`setSpeed(audioSpeed)`；**异步不阻塞状态推进**；音频焦点被 transient 抢占 → 本句跳过、下一阶段正常（AC-06）；`repeat()` 重播；`release()` 幂等。
- 单测：`TrainingEngineTest`（覆盖暂停/恢复/结束/锁屏大跳/参数变更）、`VoiceScriptTest`（每条音频资源存在且非空）。

**方案评审要点**：引擎零 Android 依赖（可纯 JVM 单测）；`elapsedActiveMs` 由调用方从 `SystemClock.elapsedRealtime()` 计算，引擎不自己去读时钟。

---

### S4 · P01 + 导航骨架 + P02 首页

**产出**：P01-a 欢迎隐私（37sp 主标题 + 隐私三条）、P01-b 筛查（4 题 + 进度条 + 三分流）、导航骨架（底部 4 Tab + 顶级训练路由 + `singleTop` + `onNewIntent`）、P02-a/b 首页（今日卡两态、Day N 主题、本周 7 点、上次训练、感知状态三态行）。
**E2E**：清数据重装 → 驱动走完引导 → 截图比对；点「开始今天的训练」→ 造一条记录 → 回首页比对 P02-b + 断言 Day2。

---

### S5 · P04 训练中 + 前台服务 + 无感知闭环

**产出**：P04-a/b/c 三态。布局自上而下：视频区（未开感知=静态占位「未启用感知」）/ 阶段时间轴（三色节点）/ **节律圆环 + 常驻「节律提示」标注** + 32sp 阶段名 + 34sp 倒计时 / 指标区（本阶段空态）/ 固定免责文案 / 暂停+结束（二次确认）。**全区可滚动**（D6 窄屏）。`TrainingForegroundService`（`mediaPlayback`，常驻通知可回训练页）；中断恢复「继续/丢弃」；`BackHandler` 二次确认。
**E2E**：跑满 8 次 → 暂停 10s 恢复验无跳变 → 第 3 次结束验 `completedReps=2` → `KEYCODE_POWER` 锁屏 30s 回来验误差 ≤1s → `am force-stop` 验「继续/丢弃」→ `am start -W` 验冷启动 <2s。

---

### S6 · 感知层 + 摄像头真链路 + P03 校准

**产出**
- `SensorAdapter` + `NullSensor` + `DemoSensor`（含**故障注入**：`injectFaceLost()` / `injectLowLight()` / `injectLowSqi()`，仅 debug 与演示可见——用于把 AC-16/17 的逻辑路径变成可自动断言）+ `RppgSensor`（经 Bridge）。
- `QualityGate` 四态 + 全部阈值规则（3s 人脸丢失、SQI 0.50、光照、前 30s 建信号、60s 出呼吸率）。
- `SensorCoordinator`（**D12** application-scoped，`ON_STOP` 停投喂、`ON_START` 续、窗口保留）。
- `CameraController`（前置镜像 / `STRATEGY_KEEP_ONLY_LATEST` / `ImageProxy.close()` 严格配对）+ `FaceAnalyzer`（最大人脸 + 占比 20%–60% + 亮度统计）+ `FaceRoiExtractor`（9×9×3）。
- 单线程串行投喂：`Channel(capacity=1, onBufferOverflow=DROP_OLDEST)`。
- P03 三态：建信号中（进度环 + 「正在建立信号 12s / 约 30s」）/ 人脸未对准（22sp 主提示 + 三条提示卡）/ 就绪（诊断折叠区四行 + 主按钮）；「跳过，直接训练」常驻。
- 指标区四态 + 演示全屏水印。

**E2E**：`pm grant` 授权 → 真机预览绿框 → 故障注入验四类反馈与四态渲染 → 前 30s 截图断言无数字 → 遮脸注入 3s 内转角标 → 脚本循环进出 5 次 + `logcat` 断言无 `CameraDevice` 异常。

---

### S7 · P05 完成自评 + P06 记录七天 + 漏尿日记

**产出**：P05-a/b（时长/次数/暂停；**仅 `signalCoverage ≥ 0.6`** 才显示覆盖率与平均心率；难度三选 + 有无不适；「有不适」→ 停止/就医提示、**无加量建议**）；P06-a/b（日历打卡、近 7 天三项累计、空态零模拟数据、周报入口）；漏尿日记（频次+情境、全可跳过、仅本地、不进周报）；完成页幂等（AC-08）。
**E2E**：连点两次完成 → 拉库断言 1 条；`DEBUG_SEED days=3` → 比对日历数字与 Day 进度；日记全跳过 → 断言 0 脏数据；「有不适」→ 截图断言提示且无加量词。

---

### S8 · P08 家人周报 + P09 设置 + 提醒

**产出**：周报预览卡（仅本周完成天数、累计时长、鼓励文案）→ `ACTION_SEND` 系统面板；默认不开分享；`ShareCardBuilder` 单测断言输出串**不含**心率/呼吸/HRV/漏尿。设置页：语音速度档位、提醒开关+时间、隐私说明、演示模式（底部次要位）、清除数据。`ReminderWorker`（WorkManager 每日本地通知）。
**E2E**：未开分享 → 截图断言仅预览；开启 → 截图系统面板 + 断言无敏感项；演示模式跑一次 → 断言不计入打卡/周报（AC-19 后半）；改提醒时间触发通知。

---

### S9 · 适老化 + 无障碍 + 红线 + 全量回归 + 性能功耗

**产出**
- `font_scale` 1.3 与 2.0 逐页巡检修破版（AC-09）。
- `contentDescription` 全覆盖 + `uiautomator dump` 断言 `content-desc` 非空率 100%（AC-10 部分验证，运行时 TalkBack 本机不可装，如实标注）。
- 禁词扫描 `tools/scan-banned.sh`，词表按评审修正为：
  `盆底肌力|肌力|动作正确率|收缩正确率|动作标准|治愈|显著改善|疗效|血压|血氧|情绪|心理评分|HRV`
  + instrumented 断言「免责文案存在」+「圆环含『节律提示』」（纯文本扫描抓不到 UI 存在性）。
- AC-03：`svc data disable` + 静态网络审计（`INTERNET` 仅用于 SDK 激活路径），如实标注「未真正断 WiFi，因会切断 adb 自身」。
- **性能功耗出口**：`am start -W` 冷启动 <2s；`dumpsys gfxinfo com.diqi.demo framestats` 掉帧 <5%；`dumpsys batterystats` 10 分钟训练耗电 ≤10%；`dumpsys thermalservice` 温升记录。
- 全 23 条 AC 重跑 + 证据归档 `docs/evidence/`。

---

## 6. AC 判据矩阵

见 [`docs/AC-MATRIX.md`](AC-MATRIX.md)：每条 AC 的**验证手段 / 判据字段 / 取证形式 / 能否自动化 / 已知局限**。

---

## 7. 风险登记（含评审新增）

| 风险 | 等级 | 应对 |
| --- | --- | --- |
| 无真实 AAR，真实指标链路无法验证 | 高 | 桩 + 完整真链路 + ROI golden vector 单测；验收如实标注未验证项 |
| 设备无 TTS 引擎 | **已消除** | D2′ 改为构建期生成音频素材 |
| 设备无 TalkBack，AC-10 无法完整验收 | 高 | content-desc 全量 dump 断言 + 如实标注运行时未验 |
| 设备无 root/sqlite3/tcpdump | 中 | `run-as cat` 拉库；抓包不可执行 → 静态网络审计替代并标注 |
| 飞行模式会切断 adb（自毁） | 高 | `svc data disable` + 静态审计，标注代理性质 |
| 无模拟来电通道 | 中 | 音频焦点抢占代理 + 真人补验 |
| AC-16/17 需物理遮脸/打光 | 中 | 故障注入覆盖逻辑路径 + 真机注入验证；物理部分如实标注 |
| 372dp 窄屏 × 20sp 正文 × 2.0 字号 | 高 | 全部页面 ScrollView 化；S4 起每阶段巡检 1.3×，S9 收口 2.0× |
| Compose 无 resource-id | **已消除** | D14 在 S1 落 `testTagsAsResourceId` + `UiTag` |
| 远程 adb 掉线 | 中 | `tools/e2e.sh` 每次自检 + 自动重连 3 次 |
| 国产 ROM 后台杀进程 | 中 | FGS + 电池优化豁免引导；真机锁屏 30s 验 |
| 演示数据混入统计（合规） | 高 | D9 统一过滤 + 反射单测断言 |
| 9×9×3 字节序/单位错 | 中 | golden vector 单测 + Bridge 显式待确认清单 |
| 版本组合编译失败 | 中 | S1 一次跑通 `assembleDebug` 暴露全部冲突 |

---

## 8. 遗留与待确认（不阻塞开发）

1. 训练参数默认值需临床签字（已用 `planVersion` 隔离，改参数不破坏历史记录可追溯性）。
2. SQI 门限 0.50 需真实 AAR 真机标定（当前写在 `TrainingParams`）。
3. AppKey 绑定包名是否确为 `com.diqi.demo`。
4. 语音素材由 macOS Tingting 合成，正式版是否换真人录音（`VoiceScript` 单一来源，替换只换 assets）。