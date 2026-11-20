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