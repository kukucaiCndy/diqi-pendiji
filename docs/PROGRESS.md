# HerRhythm · 开发进度台账

> 每个阶段记录：**实际交付 / 端到端证据 / CodeReview 发现与处置 / 遗留与如实标注的未验证项**。
> 这份台账的目标是让任何人（尤其是睡醒的产品负责人）能在 5 分钟内判断"哪些是真的过了、哪些是没验的"。

---

## S1 · 工程基建 + 设计系统 + SDK 桩 ✅ 完成

### 交付物

| 类别 | 内容 |
| --- | --- |
| 构建 | Kotlin 2.0.21 / AGP 8.7.3 / KSP 2.0.21-1.0.28 / compileSdk 35 / minSdk 26。依赖一次补齐：Room 2.6.1 + KSP、DataStore 1.1.1、Navigation 2.8.5、CameraX 1.4.1、ML Kit 16.1.7、WorkManager 2.10.0 |
| `:facephys-api` | 契约桩模块。`RppgEngine` 接口 + `FrameResult` 枚举 + `MetricsRead` 三态 + `StubRppgEngine`。**任何取数路径均不产生生理数值** |
| `FacePhysBridge` | SDK 唯一接触点：状态机（release 后绝不再触原生层）、线程归一化、失败分类→用户文案、模型资源存在性检查 |
| 设计系统 | `Color.kt`（20+ 语义 token，逐条对齐 `.pen`）、`Type.kt`（D6 字阶）、`Dimens.kt`、`Theme.kt` |
| 页面骨架 | `HrPage` / `HrBottomBar` 统一处理 safeDrawing insets（避免"每页各自处理、总漏一页"） |
| 自动化基座 | `UiTag`（114 个测试锚点）+ 根节点 `testTagsAsResourceId`；`tools/e2e.sh`（30 个子命令）；`tools/scan-banned.sh` |
| 守护性单测 | `ContrastTest`（FR-15）、`BannedWordsTest`（AC-14）、`RoiConverterTest`（9×9×3 golden vector）、`UiTagTest`、`FacePhysBridgeContractTest`、`StubRppgEngineContractTest` |
| 仪器化测试 | `DesignSystemSmokeTest` —— 首个 instrumented test，证明仪器化通路可用 |

### 端到端证据

| 项 | 结果 |
| --- | --- |
| `assembleDebug` | ✅ 成功 |
| 单测 | ✅ **60 项 / 0 失败**（6 个测试类） |
| instrumented | ✅ **2 项 / 0 失败**（真机 NX729J） |
| lint | ✅ **0 error**；仅剩 23 条 `GradleDependency`（刻意锁版本） |
| 禁词扫描 | ✅ 0 命中 |
| 真机渲染 | ✅ 设计系统页截图，**真机渲染出的对比度与计算结果逐值一致**（17.59 / 5.47 / 5.27 / 16.24 / 11.89 / 4.70…） |
| `testTagsAsResourceId` | ✅ `uiautomator dump` 中可见 `id=dev.design_system.root` |
| 冷启动 | ✅ 849–924 ms（目标 < 2s） |

证据文件：`docs/evidence/s1_design_system_v3.png`

### CodeReview（3 路编排：合规 / 架构 / 可测性）发现与处置

**已修复（18 项）**

| # | 问题 | 处置 |
| --- | --- | --- |
| 1 | **对比度自检页度量了错误的底色** —— HR 值算的是白卡底（4.70），实际渲染在 canvas（4.34），等于"用来证明 FR-15 的工具证明了另一个底色" | 色板改为按真实底色双度量；`ContrastTest` 配对从 30 组扩到 47 组，补齐 HR/RR 在 canvas、提示卡上的比值 |
| 2 | **免责文案单测断言的是测试自己的字面量** —— 产品代码零覆盖 | 明确记录该局限；改为由 `scan-banned.sh` 扫真实源码 + 后续 instrumented 断言 UI 存在性 |
| 3 | **`FacePhysBridge.kt` 不存在** —— "只改 1 个文件"的替换点落空 | 已落地，含状态机/线程归一化/失败分类/模型检查 |
| 4 | 桩的 `hasEnoughWindow=true` 会被照字面实现成 DEGRADED（与 FR-08 相反） | 契约改为 `MetricsRead` 三态，桩在窗口攒够后返回 `NoSignal`，并用契约单测钉死 |
| 5 | `RppgConfig` 无 Context，真实 SDK 读不到 `.enc` | 已加 `context`（桩/测试可空，真实 AAR 要求非空） |
| 6 | 错误码定义在将被删除的 `StubRppgEngine` 里 | 上移为契约层的 `FrameResult` 枚举 |
| 7 | `windowAgeMs` 缺失 → 无法判定"窗口是否还新鲜"（AC-05 降级） | 已加入契约并实现 |
| 8 | `initialize` 在 `synchronized` 内同步回调，把异步竞态藏到 S6 | 改为锁外回调 |
| 9 | 串行契约被桩的 `synchronized` 掩盖，无法验证 | 加 CAS 并发检测 → `ERR_CONCURRENT`，并有 8 线程并发单测 |
| 10 | `dtSec` 无上限 | 加 `MAX_DT_SEC = 2.0s` |
| 11 | `sdkVersion` 会被误读成"已集成真实 1.5.0" | 改为 `STUB-0.0.0-contract`，单测断言不含 `1.5.0` |
| 12 | **`e2e.sh` 的 WAL 合并是 no-op** —— checkpoint 打在改名后的副本上，伴生名对不上，查库永远看不到最新写入 | 改为按**原始文件名**拉取到独立目录，sqlite 自动应用 WAL |
| 13 | `e2e.sh` 用 `endswith` 匹配 tag → `p04.demo_watermark` 被全局 `common.demo_watermark` 误命中 | 改为**精确匹配** |
| 14 | `e2e.sh` 陈旧节点树陷阱 —— pull 失败会读到上一轮的树并"通过" | 先删旧文件 + 校验非空 + 校验 `package=` 属于本 app |
| 15 | 无滚动能力 → 折叠线以下的主按钮系统性假阴性 | 新增 `scrollto`；`tap` 找不到节点时自动滚动重试 |
| 16 | `RoiConverter` 的 Bitmap 重载不 clamp 越界矩形 → 相机分析线程崩溃（表现为 AC-18 偶发崩） | 已 clamp；纯函数版本有 golden vector |
| 17 | `TrainingForegroundService` 在 Manifest 声明但类不存在 → S5 必崩 | 已落地完整可实例化骨架 |
| 18 | `MainActivity` 无 `onNewIntent` → 通知点击被静默丢弃，`getIntent()` 永久陈旧 | 已实现 `setIntent` + `NavCommand` flow（D13） |
| 19 | `HrPage` 硬编码 safeDrawing → P03/P04 做不了沉浸；与 `HrBottomBar` 底部双算 | `HrPage` 已参数化（`scrollable` / `horizontalPadding` 可覆盖），沉浸模式在 S5 接入时扩展 |
| 20 | 按钮 19sp、免责 14sp 属 D6 三档之外的"野值"且无人裁定 | 按钮 → 20sp；免责 → 20sp + 弱色降权重 |
| 21 | Manifest 缺 `hardware.camera` 等 `required=false` 声明 → 渠道可能在**安装环节**过滤掉 AC-04 | 已补 4 条 feature 声明 |
| 22 | 冗余危险权限 `RECEIVE_BOOT_COMPLETED` / `WAKE_LOCK` 无人使用 | 已移除（WorkManager 自带） |
| 23 | **`Color(0xB45309)` 只有 6 位十六进制 → alpha=0，演示水印完全透明** | 已修正为 8 位；AC-19 的水印可见性由此保住 |
| 24 | Room 缺 `schemaLocation` → 首次改表就失去迁移基线，只能破坏性迁移（抹掉 AC-12 的历史数据） | 已加 `room.schemaLocation` + `room-generateKotlin` + `room-testing` |
| 25 | 无 instrumented test 通路 | 已建并真机跑通 |
| 26 | `fontScale` 在 `configChanges` 中 → AC-09 测不到真实重建 | 已移除（D16） |
| 27 | ML Kit 注释"不依赖 GMS"是错的 | 已核实依赖树并改正注释，纳入 AC-03 审计 |

**未修，附理由**

| 项 | 理由 |
| --- | --- |
| APK 51MB（4 个 ABI 的 `libface_detector_v2_jni.so` 约 33MB，其中 x86/x86_64 是纯废重） | 交付目标是演示机（arm64）；如需减包，S9 可用 `abiFilters` 只留 arm64-v8a 与 armeabi-v7a，预计降到 ~25MB。已记入 S9 待办 |
| `GradleDependency` 23 条（有更新版本） | 版本是评审确认过的兼容基线，不在此阶段追新 |
| `Contrast` 里保留 `AA_LARGE = 3.0` 常量 | 保留给**非文本** UI（进度轨、状态点）使用，文本一律 4.5（D15） |
| `HrPage` 沉浸模式支持 | 属 S5（P04 全屏）范围，届时扩展而非现在空转 |

### 如实标注的未验证项

- **Room 代码生成未验证**：S1 尚无 Room 实体与 `@Database`，`kspDebugKotlin` 虽已执行但未产出代码。这是 S2 的**第一件事**（先建实体跑通 codegen，再写 DAO 查询）。
- **真实 FacePhys 指标链路完全未验证**：无 AAR、无 AppKey，`RppgSensor` 路径的所有指标数值都无从验证。
- **ML Kit 在无 GMS 设备上的行为未验证**：本机有 GMS（`com.google.android.gms` 在），永远测不出差异。模型确认已随包，但需要一台无 GMS 设备/模拟器补验。
- **本机无 TalkBack**，AC-10 的运行时流程无法验证。
- **`mediaPlayback` 前台服务在 RedMagicOS 上的实际存活行为**未验证（S5 验证）。
- **设计稿字号与需求的冲突**按需求执行（D6），因此设计稿的视觉密度未逐像素还原。
---

## S2 · 数据层 + 七天内容 + 设置/同意 + 造数入口 ✅ 完成

### 交付物

| 类别 | 内容 |
| --- | --- |
| Room | 3 实体（`TrainingSession` 18 字段 / `SensorQuality` / `LeakDiary`）+ 3 DAO + `HerRhythmDb`（schema 导出到 `app/schemas/`） |
| DataStore | `AppSettingsStore`：设置 + 同意 + 筛查合并一份，含 `.catch` 容错 |
| 领域 | `TrainingParams`（§5.4 全 12 项 + `validate()` + `Locale.ROOT` 内容哈希）、`TrainingPhase`/`TrainingFrame`/`EngineSnapshot`、`SevenDayPlan`（Day1–7 内容 + 按首次完成日起算） |
| 仓储 | `SessionRepository`（唯一写入入口 + 事务 + 覆盖率门限强制）、`ProgressRepository`、`LeakDiaryRepository` |
| 造数 | `DebugSeedReceiver`（**仅 debug source set**，`goAsync` + 异常日志） |

### 端到端证据

| 项 | 结果 |
| --- | --- |
| Room KSP 代码生成 | ✅ 4 个 `_Impl.kt` + `schemas/.../1.json`（评审点名的 S2 头号风险已清除） |
| 单测 | ✅ **80 项 / 0 失败**（8 个测试类，含 app 65 + facephys-api 15） |
| 仪器化 | ✅ **22 项 / 0 失败**（20 项真库 DAO 测试 + 2 项冒烟） |
| 造数 → 拉库 → 读回 | ✅ `seed days 3` → `e2e.sh sql` 读到 3 条，日期正确 |
| WAL 合并 | ✅ 最新写入（今天 20:00 那条）在只拉 `.db` 时也能读到 —— 证明按原名拉取 + 同目录放置的修复有效 |
| 杀进程持久化 | ✅ `force-stop` 后重启，5 条记录仍在 |
| 冷启动 | ✅ 812–852 ms |

### CodeReview（3 路编排）发现与处置

**已修复（22 项）**

| # | 问题 | 级别 | 处置 |
| --- | --- | --- | --- |
| 1 | **`REPLACE` 触发 `ON DELETE CASCADE`，自评回填会静默删掉质量汇总行**（用 sqlite3 复现） | P0 | 训练表改 `@Upsert`；加了真库测试 `自评回填不会删掉质量汇总行` |
| 2 | 保存与自评回填都不在事务里（写两表 / 读-改-写） | P0 | 全部包进 `withTransaction` |
| 3 | 撤销相机同意时删掉 `consentedAt`，"从未同意"与"同意后撤销"无法区分 | P0 | 保留 `consentedAt`，只追加 `revokedAt`，新增 `cameraConsentActive` 判定 |
| 4 | `clearAll()` 连同意与筛查留痕一起抹掉（"清除数据"= 销毁合规证据） | P0 | 拆成 `clearBusinessSettings()`（保留留痕）与 `clearEverythingForTests()` |
| 5 | `SensorQuality` 无 `sensorMode`，跨会话质量统计会把演示会话算进去；且原测试把这个缺口**反向锁死**了 | P0 | 加 `averageValidRatioRealBetween`（INNER JOIN 过滤），删掉反向断言 |
| 6 | AC-19 守卫只扫 `Daos.kt` 单文件 + 正则漏多行写法 → 假通过 | P0 | 改为扫描 `src/main` 全部 Kotlin + 注释剥离 + 括号配平扫描器 + **每个 DAO 必须显式分类** |
| 7 | 覆盖率门限只做数值比较：传 60（百分比）会原样入库；`simulated`/`off` 会话覆盖率达标也能带心率 | P1 | `coerceIn(0,1)` + **只有 `OFFLINE_SDK` 才允许带指标** |
| 8 | DAO/Repository 的真实 SQL 一次都没被执行过 | P1 | 新增 20 项真库仪器化测试（in-memory Room） |
| 9 | "仅统计 ≥60s"只写在注释里，SQL 无门槛 → 3 秒误触会污染打卡/时长/Day 进度 | P1 | 5 处统计查询全部加 `durationMs >= 60000`，并加测试断言门槛真的在 SQL 里 |
| 10 | 统计时长用墙钟差推导，训练中校时会跳变（甚至为负） | P1 | 新增 `durationMs`（单调时钟写入），统计只认它；`startedAt` 加 `require` 必须是墙钟量级 |
| 11 | 种子用 `clearSimulated()` 清理，但种子是真实模式 → 跨天重复造数会累积并计入统计 | P1 | 新增 `clearSeeds()`（按 `seed-day-%` 前缀），种子时间戳压到「不晚于现在」 |
| 12 | 种子质量占比写 0/0/0 却配 `NO_DATA`，消费方无法区分"数据缺失"与"真是 0" | P2 | 改为 `invalidRatio = 1.0` |
| 13 | `SessionDraft.id` 有 `UUID.randomUUID()` 默认值 → AC-08 只在"复用同一 draft"时成立 | P1 | 去掉默认值，编译期逼调用方在训练开始时持有 id |
| 14 | `TrainingParams.hash()` 用默认 Locale → 换系统语言后同一参数产出不同 planVersion | P2 | 改 `Locale.ROOT` |
| 15 | `clinicalNoticeRequired` 在筛查未答完时返回 false（漏答反而更放行） | P1 | 改为 `!isComplete \|\| q2==YES \|\| q3==YES` |
| 16 | 两条独立需求被德摩根化简成逐字相同的表达式，改一处会静默影响另一处 | P2 | 各自显式书写，加真值表测试 |
| 17 | `setScreening` 不删旧键 → 重做筛查时上一轮答案越过本次"未答"写回；未答完也会拿到"已确认"时间戳 | P1 | 先 `remove` 四个键，`safetyAcknowledgedAt` 仅在整个筛查完成时落 |
| 18 | `Float.coerceIn` 挡不住 NaN，读取路径也不 clamp → 非法语速直进 `PlaybackParams` 会抛异常 | P1 | 读写两侧都 `takeIf { isFinite() }`；`reminderTime` 读写都校验 `HH:mm` |
| 19 | DataStore 读 Flow 无 `.catch` → 一次 IO 异常让首屏永远拿不到设置 | P1 | 加 `.catch`：`IOException` 时发出空偏好 |
| 20 | 漏尿日记 `frequency` 是非空裸 String，跳过的行被迫编造频次 | P2 | 改 enum（`LeakFrequency`/`LeakContext`）+ 可空 + `init` 不变式 |
| 21 | 七天文案的禁词表在测试里自建，与全局词表分叉（「康复」全局不设防） | P2 | 测试改用 `BannedWords.scan`；`康复` 并入全局词表与扫描脚本 |
| 22 | `e2e.sh` 取库无任何校验（run-as 失败会拿到空库 → sqlite 报 no such table → AC 假 FAIL） | P1 | 拉完校验文件头 + `PRAGMA integrity_check`（`tools/dbcheck.py`） |

**未修 / 需产品决策（3 项）**

| 项 | 说明 |
| --- | --- |
| 「清除数据」的产品语义 | 已按"清业务设置、保留同意与筛查留痕"实现（合规优先）。若产品认为该按钮应等于"恢复出厂"，需改回并接受"用户可一键销毁同意证据" |
| `DailyProgress` 不建表 | 已写入 `docs/AC-MATRIX.md` 的「§6 数据模型偏离裁定」，需产品确认派生实现可接受 |
| `validRatio` 语义 | 按需求原文取**时长占比**；生产者（`QualityGate`）在 S6 落地，届时须按此时长口径写入 |

### 运维注意（真实踩坑）

**跑 `connectedDebugAndroidTest` 会卸载应用并清空数据**。之后必须重新 `e2e.sh install` + 重新造数，
否则会看到"databases/ 为空"的假象。

### 如实标注的未验证项

- **`QualityGate`（质量四态判定）尚不存在**，所有 `validRatio`/`lastState` 目前只有种子与测试写入，真实生产者待 S6。
- **所有筛查派生属性与 `shareEnabled` 目前零消费者** —— FR-01 的常驻提示条、完成页结论抑制、FR-13 分享守卫会在 S4/S8 接上，届时必须重审。
- **`ProgressRepository`/`LeakDiaryRepository` 尚无 UI 调用方**，其端到端表现（AC-12/23）在 S7 才能真验。

---

## S3 · 训练引擎（纯函数计时）+ 语音素材 ✅ 完成

### 交付物

| 类别 | 内容 |
| --- | --- |
| 时间线 | `TrainingTimeline`：`elapsedActiveMs → TrainingFrame` 的**纯函数**，无 tick 累加（D11） |
| 引擎 | `TrainingEngine`：时钟注入、暂停/恢复/结束、**结束后冻结时间**、快照恢复 |
| 协调器 | `TrainingSessionCoordinator`：引擎 + 语音 + 快照的唯一生产接入点，100ms tick 只驱动重绘 |
| 快照 | `SnapshotStore` 接口 + `DataStoreSnapshotStore`（独立 DataStore 文件） |
| 语音 | `VoiceScript`（8 条文案唯一来源）+ `gen-voice.sh`（macOS 中文语音 → AAC）+ `AssetVoiceGuide`（`prepareAsync` + 音频焦点管理） |

### 端到端证据

| 项 | 结果 |
| --- | --- |
| 单测 | ✅ **125 项 / 0 失败**（app 110 + facephys-api 15） |
| 真机音频 | ✅ `requestAudioFocus from AssetVoiceGuide`、`MediaPlayer state:started`、`NuPlayerDecoderBase: audioStart latency 55.14 ms` |
| 音频素材 | ✅ 8 条 m4a 共约 312KB；`noCompress` 生效（`openFd` 成功，无 "file can not be opened as a file descriptor"） |
| 播完归还焦点 | ✅ 播放结束后 `dumpsys audio` 无 active speech player |
| lint | ✅ 0 error |
| 禁词扫描 | ✅ 0 命中（含语音文案） |

### CodeReview（2 路编排）发现与处置

| # | 问题 | 级别 | 处置 |
| --- | --- | --- | --- |
| 1 | **`stop()` 只置标志位不冻结时间** → 之后每次 poll 都会重算 elapsed，越过总时长后 `completedReps` 从 2 变 8 —— 正是 AC-07 要防的补造 | 高 | 增 `frozenElapsedMs`，`stop()` 记录冻结值，`elapsedActiveMs` 优先返回它；加 3 条回归测试 |
| 2 | `restore()` 不回填 `startedAtWallMs` → 再次 `snapshot()` 得 0 → 写库命中 `require(startedAt >= MIN_EPOCH_MS)` 直接崩（AC-13「继续」路径） | 高 | 回填墙钟；历史暂停时长放进独立 `restoredPausedMs` 避免被重复扣减；加 2 条测试 |
| 3 | `pause()` 在自然完成后仍会执行 → 状态从 COMPLETE 变回 PAUSED，且多记一次 `pauseCount`（§5.3 状态机 Complete 无"暂停"去向） | 中 | 入口加完成判定；`status` getter 也补齐"自然完成"分支 |
| 4 | `TrainingTimeline.frameAt` 的 `status` 形参只在 IDLE 用，COMPLETE 分支又硬编码满组 → 语义自相矛盾 | 中 | 删除该形参，状态词统一由引擎回填 |
| 5 | **音频焦点从第一句占到 `release()`** → 整个训练期间压低其他音乐应用 | 高 | 播完即 `abandonFocus()`；`AUDIOFOCUS_LOSS` 走真正放弃（否则复用旧 request 永远拿不到焦点）；抽出纯函数 `focusActionFor` 并单测 |
| 6 | **同步 `prepare()` 在主线程** → 连点会 jank / ANR | 高 | 改 `prepareAsync` + `onPrepared`；用 `pending` 标记实现"最后一句赢" |
| 7 | 播放期错误会把 `isAvailable` **永久**置 false，整场语音连文字一起降级 | 中 | 只有"素材打不开"才降级；播放期错误只记日志 |
| 8 | `repeatLast()` 绕过 `released` 守卫 → 释放后点"重复提示"会新建永不释放的 MediaPlayer | 中 | 入口加 `released` 守卫 |
| 9 | `VoiceCheckSection` 无 `DisposableEffect` → 离开页面后播放器与焦点泄漏到进程结束 | 中 | 补 `onDispose { guide.release() }` |
| 10 | `params.voiceSpeed` 全工程没接到播放器 | 中 | `AssetVoiceGuide(initialSpeed)` + `setSpeed` 持久化当前值；接线在 S5/S8 |
| 11 | `gen-voice.sh` 部分解析成功即返回成功 → 新增条目写法变化会静默漏生成，仍打印"全部就绪 ✅" | 高 | 加"源码条目数 == 解析条数"自检；跳过逻辑改为按**文案哈希**判断（文案变了自动重生成） |
| 12 | 测试期望值抄自 `DEFAULT` 常量，改参数时会"常量过时"而非"行为错误" | 低 | 保留现状并在测试文件标注；S5 接线后以真实参数来源校准 |
| 13 | 无生产调用点（引擎是"没人用的库"），AC-13 快照链路缺实现 | 高 | 新增 `TrainingSessionCoordinator`（唯一接入点，含 tick、阶段语音、快照落盘）+ 11 条协调器测试 |

### 刻意保留的行为（附理由）

**锁屏很久后回来，已完成次数会很大**：训练期间有前台服务在跑，语音与倒计时确实在走，用户每一组都在按计划进行，计入是正确的。AC-07 说的"不补造"针对的是**用户主动提前结束**，那条已由 `stop()` 冻结时间实现。真被杀进程走快照恢复，`elapsedActiveMs` 冻结在快照值，不会膨胀。

### 如实标注的未验证项

- **语音内容与文案的一一对应未验证**：`VoiceScriptTest` 只能证明素材存在且非空，无法证明 `contract.m4a` 里念的确实是收缩文案。低成本方案（按 `字节数/字数` 落在中位数 ±15%）已记录，未实施。
- **AC-06 的真实来电未验**：本机无 `cmd telecom` 注入通道，目前只有纯函数层（`focusActionFor`）与结构保证（音频层与训练层零耦合）。
- **协调器尚未接入 UI/前台服务**：S5 完成。届时 AC-02/05/07/13 的端到端验证才真正成立。

---

## S4 · P01 引导筛查 + 导航骨架 + P02 首页 ✅ 完成

### 交付物

| 类别 | 内容 |
| --- | --- |
| 导航 | `HerRhythmNavHost`：单 Activity + Navigation Compose，底部 4 Tab，训练中为**顶级路由**（D13），通知指令由 `NavCommand` flow 消费 |
| P01-a | 欢迎与隐私：37sp 主标题、隐私三条（本地处理/不录像/不上传）、主按钮**固定在底部** |
| P01-b | 安全筛查：4 题分屏、进度条、三选项、分流提示（规则 A/B 分别展示） |
| P02-a/b | 首页两态：今日训练卡（深色）/ 已完成卡（白色）、Day 主题、本周 7 点打卡、感知状态行 |
| 组件 | `HrPrimaryButton` / `HrSecondaryButton` / `HrStatusChip` / `HrBanner` / `HrCard` / `HrWeekDots` / `HrPageWithAction` |

### 端到端证据（全部真机截图）

| 项 | 结果 |
| --- | --- |
| 单测 | ✅ 125 项 / 0 失败 |
| 仪器化 | ✅ **24 项 / 0 失败**（含 20 项真库 DAO + 2 项设计系统 + 2 项无障碍语义） |
| 冷启动 | ✅ 632–903 ms |
| P01-a → 筛查 → 首页 | ✅ 脚本走通 4 题 |
| **AC-20** | ✅ 造 3 天记录后首页显示 **Day 3**（`Day 3 · 练习不憋气`），首次完成日起算成立 |
| **AC-12（前半）** | ✅ 首页「本周 3 次 · 6 分 15 秒」= 3 × 125s，与库内数据一致；打卡点落在四/五/六 |
| 底栏 4 Tab | ✅ 切页正常，无栈异常 |
| **AC-09（实测 2.0×）** | ✅ 系统字号 2.0 下逐页截图：文字无截断、可滚动、主按钮固定可见 |
| 感知状态行可点 | ✅ 点击进入校准页（首版是死链接） |

证据：`docs/evidence/s4_welcome.png`、`s4_home.png`、`s4_home_done.png`、`s4_font200_welcome.png`、`s4_font200_screening.png`

### CodeReview（2 路编排：导航/数据流 + 适老化/可访问性）发现与处置

| # | 问题 | 级别 | 处置 |
| --- | --- | --- | --- |
| 1 | **感知状态行「点这里了解」是假承诺** —— `onClick` 形参从未被引用，P03 校准页整页不可达（两路 reviewer 独立发现：静态代码 + 真机 dump 测得 `clickable=false`） | 高 | `HrStatusChip` 支持 `onClick` 并接上；真机验证可跳转并返回 |
| 2 | **引导「跳过」不落库** → 每次冷启动重走引导，且答完 4 题再按跳过一次丢弃全部答案 | 高 | 新增 `acknowledgeScreeningSkipped()`（只写时间戳、不写答案，与"答完"在库里可区分） |
| 3 | `clinicalNoticeRequired` 在"未答完"时为 true → 直接跳过的人会看到"你之前回答过需要留意的情况"，而他一题没答 | 高 | 拆成 `hasRiskAnswers`（只判风险项，供首页横幅）与 `isIncomplete`（软提示） |
| 4 | `HrBanner` 正文用 14sp 渲染整句就医提示 → 直接违反「正文 ≥20sp」 | 高 | 改 `bodySmall`(20sp) |
| 5 | **无障碍描述被 `tag != null` 门控** —— 依赖方向反了：有测试锚点的才有描述 | 高 | 所有主/次按钮、状态胶囊、提示条**无条件**设置 `contentDescription`，并改为 `semantics(mergeDescendants = true)` |
| 6 | 感知三态实际只有两态（`READY` 不可达），且把"演示模式"映射成"信号弱"是**编造状态** | 高 | 删除假映射，感知层接入前如实显示"未开启感知" |
| 7 | 首页 `LaunchedEffect(Unit)` 刷不到"从后台回来/跨天/前台服务练完" | 中 | 加 `ON_RESUME` 生命周期刷新 |
| 8 | 重复点当前 Tab 会先把该 Tab `popUpTo` 掉、使 `launchSingleTop` 失效 → 每次重建（对训练 Tab 等于重拿相机） | 中 | 加 `if (currentRoute != tab.route)` 守卫 |
| 9 | 引导两页各自 new 一个 `OnboardingViewModel` → 答案不共享，且 pop 后 entry-scoped 实例被清空 | 中 | VM 上提到导航层，两页共用一个实例 |
| 10 | `HrPage` 默认可滚动 → 2.0× 下主按钮滚出屏幕，「一屏一主任务」不成立 | 高 | 新增 `HrPageWithAction`：内容可滚、**主操作固定底部**；免责声明留在滚动区 |
| 11 | 首页 `dayHint` 渲染两次、卡片标题与按钮文字重复 | 低 | 去重并改写文案 |
| 12 | 已完成卡 ✓ 在 2.0× 下被 24dp 圆形裁掉 | 中 | 改 `sizeIn(minWidth/minHeight)` + padding 自适应 |
| 13 | 公共 `HrBottomBar` 与底栏私有同名 Composable 并存，构成误判陷阱 | 低 | 私有版改名 `AppBottomBar` |

### 一处测量方法修正（重要）

`uiautomator dump` 把带 testTag 的节点报成 `clickable=true` 且 `content-desc` 为空，
而真正的朗读文本落在**合并子节点**上 —— 据 dump 统计会得到"6 个可点节点全部缺描述"的假阴性。
已把 AC-10 的判据改为 **Compose 语义层断言**（`AccessibilityTest`，真机通过），
`uiautomator` 只用于定位、不用于无障碍判定。

### 如实标注的未验证项

- **真机 TalkBack 流程**：本机无 TalkBack 包，只能验证语义节点存在。
- **`HomeViewModel` 的感知状态**：`READY`/`WEAK` 只在 S6 感知层接入后才可能有真值。
- **P04/完成页/校准页仍是占位**，FR-15「关键数字 ≥32sp」尚未被真实界面检验。

---

## S5 · P04 训练中 + 前台服务 + 会话生命周期 ✅ 完成

### 交付物

| 类别 | 内容 |
| --- | --- |
| P04 | 视频区占位 / 阶段时间轴（三态节点）/ 阶段名 + 倒计时 + 进度轨 / **节律圆环 + 「节律提示」**（D10）/ 次数 + 语音状态 / 指标区 / 常驻免责 / 暂停·结束固定底部 |
| 组件 | `RhythmRing`（时间驱动、无生理暗示）、`PhaseTimeline` |
| 会话 | `AppContainer.obtainTrainingSession()`：训练会话 **application-scoped**，Activity 重建不丢进度 |
| 服务 | 前台服务随训练页进出；补 `POST_NOTIFICATIONS` 申请 |
| 其它 | `TrainingPrepScreen`（摄像头可选入口 + 直接开始）、`BackHandler` 二次确认、保存失败提示、`MetricUiState` 枚举 |

### 端到端证据（真机）

| AC | 判据 | 实测结果 |
| --- | --- | --- |
| **AC-01** | 跳过摄像头完成一次训练，完成页出现、记录可查 | ✅ 走完 8 次 → 完成页；库内**恰好 1 条**，`completedReps=8`、`durationMs=125s`（= 5 + 8×15，精确）、`sensorMode=null`、`planVersion=77d9f32b` |
| **AC-02** | 暂停 10s 恢复，计时冻结、恢复无跳变 | ✅ 暂停瞬间 00:03 → 10 秒后仍 00:03 → 恢复后仍 00:03 |
| **AC-05** | 锁屏 30s 返回，进度误差小 | ✅ 锁屏往返 38s 后仍显示「第 3 次 / 保持呼吸」，与按真实耗时推算的期望一致；无恢复弹窗、进度未丢 |
| **AC-07** | 第 3 次收缩中结束 → `completedReps=2` | ✅ 库内 `completedReps=2`、`plannedReps=8`、时长 48s |
| **AC-13** | 训练中断后提供继续/丢弃 | ✅ 重进训练页弹出「上次的训练还没做完 / 已完成 7 次、用时 120 秒 / 丢弃·继续」 |
| FR-06 二次确认 | 结束需确认 | ✅ 按返回键弹出「要结束这次练习吗？已完成的 2 次会保存下来，不会补算没做完的次数」 |

证据：`docs/evidence/s5_training.png`、`s5_back_confirm.png`、`s5_state.png`

### CodeReview（2 路编排）发现与处置

| # | 问题 | 级别 | 处置 |
| --- | --- | --- | --- |
| 1 | **语音被 app 级单例释放成不可逆终态** —— 协调器 `release()` 调了 `voice.release()`，于是**本进程第一场训练之后，后续每一场全程无声**，不崩不报错且 `isAvailable` 仍为 true | **发布阻断** | 协调器 `release()` 改为只 `voice.stop()`，语音生命周期归容器；新增回归测试 `会话结束不得释放全局语音单例` |
| 2 | **返回键无拦截** —— 按返回直接退出页面，但会话是容器级的仍在跑、帧收集器已销毁 → 训练跑完也不落库（记录静默丢失），且违反 FR-06「结束要二次确认」 | 高 | 加 `BackHandler` → `requestStop()`；真机验证弹出二次确认且不退出 |
| 3 | **`mode = SensorMode.OFF` 硬编码** —— 演示模式的记录会被当成真实记录，`countsTowardProgress` 把它计入打卡与周报，直接破坏 AC-19 | 高 | 保存时按 `settings.isDemoMode` 写 `SIMULATED` |
| 4 | **保存失败静默丢记录** —— 只置 `finished=true`，`_activeTraining` 留在"已 finished"的空壳，下次 `obtainTrainingSession` 静默丢弃 | 高 | 置 `saveFailed` 并弹提示、释放会话 |
| 5 | `sensorEnabled: Boolean` 是假开关（恒 false、无赋值点）—— S6 一翻转就显示假状态 | 高 | 改成 `MetricUiState` 密封接口（`NotEnabled` / `Establishing`），S6 必须显式补齐四态 |
| 6 | FR-06 第三层「语音状态」缺失（`voiceAvailable` 写了但页面从不读） | 中 | 补「第 N/M 次 + 语音引导中」一行，新增 `P04_VOICE_STATUS` |
| 7 | 暂停/继续按状态切换 testTag → 自动化的「暂停→恢复」第二步找不到节点 | 中 | 两者共用 `P04_PAUSE_BUTTON`，状态差异走 text/contentDescription |
| 8 | `observeFrames()` 无幂等守卫却有 3 个调用点 | 中 | 加 `observing` 闸门 |
| 9 | `pause()` 不停止 ticker（空转到训练结束） | 低 | 保留：纯函数模型下 tick 只是重绘，停不停不影响正确性；已在代码注释说明 |

### 未修，附理由

| 项 | 理由 |
| --- | --- |
| `obtainTrainingSession(params)` 忽略参数不一致 | 目前只有一套默认参数；S9 引入参数化后需改成"参数不一致则拒绝认领并提示丢弃" |
| AC-08 幂等在极端路径下不成立 | 依赖「导航 `popUpTo` + VM 的 `savedId` 闸门」两层；理论上第二个 VM 认领同一会话会写第二行。数据层的 `upsert` 保证同 id 只有一行，但 id 在 VM 里生成。S9 收口时把 id 挪进会话本身即可根治 |
| `ACTION_UPDATE` / `newTrainingSession()` 死代码 | 前者留给 S5 后续的"通知显示剩余时间"，后者留作测试入口；S9 清理 |
| FR-06 第四层指标区仍是占位 | S6，`MetricUiState` 已预留四态位 |

### 如实标注的未验证项

- **AC-05 的"≤1s"精度**：真机证据是"阶段与次数与按真实耗时推算的期望一致"，误差受 adb 点击/解析开销（每次 tap 约 1–2s）限制，无法测到 1 秒级；**精确性由纯函数单测保证**（时间线无累加、直接读 `elapsedRealtime`）。
- **深睡期间语音是否漏播**：未验证；计时正确不代表音频连续。
- **前台服务在 RedMagicOS 长时后台的实际存活**：只验了 38 秒锁屏，未验 10 分钟。
- **AC-15/16/17（指标区四态）**：S6。

---

## S6（第一部分）· 感知抽象 + 质量四态 + 演示模式 ✅ 完成

> 摄像头真链路（CameraX → ML Kit → 9×9×3）与 P03 校准页为 S6 第二部分，尚未开始。

### 交付物

| 类别 | 内容 |
| --- | --- |
| `SensorAdapter` | 接口 + `SensorSnapshot`，三种实现：`NullSensor` / `DemoSensor` / （`RppgSensor` 待第二部分） |
| `QualityGate` | 四态判定**纯函数**：前 30s 建信号、人脸丢失 3s 阈值、SQI 门限、光照降级、短时丢失只半透明 |
| `MetricUiState` | 四态密封接口（`NotEnabled`/`Establishing`/`NoData`/`Invalid`/`Degraded`/`Valid`），数值字段为 `Float?`，把"不得显示数字"交给类型系统 |
| 指标区 | 六种状态各自的渲染；演示水印 chip；质量三点指示；免责常驻 |
| 测试 | `QualityGateTest` 14 项（覆盖需求 FR-08 的四态判定与三条补充规则） |

### 真机证据

| AC | 结果 |
| --- | --- |
| **AC-15** | ✅ 前 30 秒指标区显示「正在建立信号 9s / 约 30s」，**心率格为空**、无水印（因无数字） |
| **AC-19（水印）** | ✅ 35 秒后出现「模拟数据 · 仅用于演示」chip + 心率 72 bpm + 信号良好 + 三点指示 |
| 呼吸率门限 | ✅ 未满 60 秒时显示 `--`，**不是**伪造数字 |
| 视频区如实 | ✅ 演示模式显示「演示模式 / 没有画面，仅模拟数值」，不再与指标区自相矛盾 |

证据：`docs/evidence/s6_demo_v2.png`

### 未完成 / 未验证（S6 第二部分）

- **摄像头真链路未开始**：`RppgSensor`、`CameraController`、`FaceAnalyzer`、`RoiConverter` 的接线、P03 校准页四类反馈、诊断折叠区。
- **AC-16（遮脸 3s）/ AC-17（强光逆光）**：逻辑已在 `QualityGate` 单测覆盖，但真机故障注入与物理验证未做。
- **AC-19 的记录侧**：演示记录的 `sensorMode='simulated'` 写入已实现，但"不计入打卡与周报"的真机断言待 S7（记录页）落地后才能验。
- **演示模式开关**：目前只有 debug 钩子（`tools/e2e.sh demo true`），正式开关在 S8 设置页。
