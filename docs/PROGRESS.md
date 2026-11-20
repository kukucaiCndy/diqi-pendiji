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
