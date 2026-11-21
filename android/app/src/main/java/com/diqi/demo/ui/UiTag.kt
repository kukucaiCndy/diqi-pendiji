package com.diqi.demo.ui

/**
 * 自动化测试锚点（决策 D14）。
 *
 * 为什么必须存在：实测本机 `uiautomator dump` 中 **Compose 节点的 resource-id 全为空**，
 * 只能靠中文 `text` 定位。而 S9 会按文案红线调整措辞 —— 改一个字就会让全部
 * E2E 脚本失效。因此从 S1 起：
 *
 *   1. 根节点挂 `Modifier.semantics { testTagsAsResourceId = true }`
 *   2. 所有需要断言的元素挂 `Modifier.testTag(UiTag.xxx)`
 *
 * 之后 `adb shell uiautomator dump` 里就能看到 `resource-id="p01.primary_button"`，
 * 脚本与文案解耦。
 *
 * 命名规范：`<页面>.<元素>`，小写下划线，全工程唯一。
 */
object UiTag {

    // ── 引导与安全筛查 P01 ────────────────────────────────────────────────
    const val P01_ROOT = "p01.root"
    const val P01_PRIMARY_BUTTON = "p01.primary_button"
    const val P01_PRIVACY_CARD = "p01.privacy_card"

    /** P01-a 主标题（设计稿 37sp 三行文案） */
    const val P01_TITLE = "p01.title"
    const val P01_SCREENING_ROOT = "p01.screening_root"
    const val P01_SCREENING_PROGRESS = "p01.screening_progress"
    const val P01_SCREENING_QUESTION = "p01.screening_question"
    const val P01_OPTION_YES = "p01.option.yes"
    const val P01_OPTION_NO = "p01.option.no"
    const val P01_OPTION_UNSURE = "p01.option.unsure"
    const val P01_SCREENING_NEXT = "p01.screening_next"
    const val P01_CLINICAL_NOTICE = "p01.clinical_notice"

    // ── 首页 P02 ──────────────────────────────────────────────────────────
    const val P02_ROOT = "p02.root"
    const val P02_GREETING = "p02.greeting"
    const val P02_TODAY_CARD = "p02.today_card"
    const val P02_TODAY_THEME = "p02.today_theme"
    const val P02_START_BUTTON = "p02.start_button"
    const val P02_DONE_CARD = "p02.done_card"
    const val P02_AGAIN_BUTTON = "p02.again_button"
    const val P02_WEEK_CHECKIN = "p02.week_checkin"
    const val P02_SENSOR_STATUS = "p02.sensor_status"
    const val P02_LAST_TRAINING = "p02.last_training"
    const val P02_CLINICAL_BANNER = "p02.clinical_banner"

    /** 本周打卡右上角「N / 7 天」（设计稿 P02-a 行头右侧） */
    const val P02_WEEK_COUNT = "p02.week_count"

    /** P02-b 已完成卡的数据行（8 次 · 3 分 12 秒 · 平均心率） */
    const val P02_DONE_SUMMARY = "p02.done_summary"

    // ── 摄像头校准 P03 ────────────────────────────────────────────────────
    const val P03_ROOT = "p03.root"
    const val P03_PREVIEW = "p03.preview"
    const val P03_FACE_BOX = "p03.face_box"
    const val P03_STATUS_TEXT = "p03.status_text"
    const val P03_SIGNAL_RING = "p03.signal_ring"
    const val P03_PRIMARY_BUTTON = "p03.primary_button"
    const val P03_SKIP_BUTTON = "p03.skip_button"
    const val P03_DIAGNOSTICS = "p03.diagnostics"
    const val P03_DIAG_SDK_VERSION = "p03.diag.sdk_version"
    const val P03_DIAG_INIT = "p03.diag.init"
    const val P03_DIAG_LICENSE = "p03.diag.license"
    const val P03_DIAG_SQI = "p03.diag.sqi"
    const val P03_FAILURE_REASON = "p03.failure_reason"

    // ── 训练中 P04 ────────────────────────────────────────────────────────
    const val P04_ROOT = "p04.root"
    const val P04_PHASE_TIMELINE = "p04.phase_timeline"
    const val P04_PHASE_NAME = "p04.phase_name"
    const val P04_COUNTDOWN = "p04.countdown"
    const val P04_RHYTHM_RING = "p04.rhythm_ring"
    /** 圆环上必须常驻的标注（需求 §5.6 合规红线：不得暗示实时肌肉状态）*/
    const val P04_RHYTHM_RING_LABEL = "p04.rhythm_ring_label"
    const val P04_REP_COUNTER = "p04.rep_counter"
    const val P04_METRIC_AREA = "p04.metric_area"
    const val P04_METRIC_HR = "p04.metric.hr"
    const val P04_METRIC_BR = "p04.metric.br"
    const val P04_METRIC_QUALITY = "p04.metric.quality"
    const val P04_METRIC_PLACEHOLDER = "p04.metric.placeholder"
    const val P04_DISCLAIMER = "p04.disclaimer"
    const val P04_PAUSE_BUTTON = "p04.pause_button"
    const val P04_RESUME_BUTTON = "p04.resume_button"
    const val P04_STOP_BUTTON = "p04.stop_button"
    const val P04_REPEAT_VOICE_BUTTON = "p04.repeat_voice_button"
    /** FR-06 第三层：语音状态 */
    const val P04_VOICE_STATUS = "p04.voice_status"
    const val P04_STOP_CONFIRM_DIALOG = "p04.stop_confirm_dialog"
    const val P04_STOP_CONFIRM = "p04.stop_confirm"
    const val P04_STOP_CANCEL = "p04.stop_cancel"
    const val P04_DEMO_WATERMARK = "p04.demo_watermark"
    const val P04_SENSOR_CHIP = "p04.sensor_chip"
    const val P04_RECOVERY_DIALOG = "p04.recovery_dialog"
    const val P04_RECOVERY_CONTINUE = "p04.recovery_continue"
    const val P04_RECOVERY_DISCARD = "p04.recovery_discard"

    // ── 完成与自评 P05 ────────────────────────────────────────────────────
    const val P05_ROOT = "p05.root"
    const val P05_DURATION = "p05.duration"
    const val P05_REPS = "p05.reps"
    const val P05_PAUSE_COUNT = "p05.pause_count"
    const val P05_COVERAGE = "p05.coverage"
    const val P05_AVG_HR = "p05.avg_hr"
    const val P05_DIFFICULTY_EASY = "p05.difficulty.easy"
    const val P05_DIFFICULTY_OK = "p05.difficulty.ok"
    const val P05_DIFFICULTY_HARD = "p05.difficulty.hard"
    const val P05_DISCOMFORT_TOGGLE = "p05.discomfort_toggle"
    /** 「有不适」选项按钮本身 */
    const val P05_DISCOMFORT_NOTICE = "p05.discomfort_notice"
    /**
     * 选中「有不适」后出现的**警示条**。
     *
     * 必须与上面的选项按钮分开：首版两处共用同一个 tag，于是"警示条出现了吗"
     * 这条断言会被常驻的选项按钮本身满足 —— 假通过。
     */
    const val P05_DISCOMFORT_BANNER = "p05.discomfort_banner"
    const val P05_SAVE_BUTTON = "p05.save_button"
    const val P05_DONE_BUTTON = "p05.done_button"

    // ── 记录与七天 P06 ────────────────────────────────────────────────────
    const val P06_ROOT = "p06.root"
    const val P06_CALENDAR = "p06.calendar"
    const val P06_SUMMARY_DAYS = "p06.summary.days"
    const val P06_SUMMARY_DURATION = "p06.summary.duration"
    const val P06_SUMMARY_COUNT = "p06.summary.count"
    const val P06_EMPTY_STATE = "p06.empty_state"
    const val P06_DAY_LIST = "p06.day_list"
    const val P06_WEEKLY_REPORT_ENTRY = "p06.weekly_report_entry"
    const val P06_DEMO_SECTION = "p06.demo_section"
    const val P06_LEAK_DIARY_ENTRY = "p06.leak_diary_entry"

    // ── 漏尿日记 ──────────────────────────────────────────────────────────
    const val LEAK_ROOT = "leak.root"
    const val LEAK_FREQ_NONE = "leak.freq.none"
    const val LEAK_FREQ_ONCE = "leak.freq.once"
    const val LEAK_FREQ_2_3 = "leak.freq.2_3"
    const val LEAK_FREQ_4_PLUS = "leak.freq.4_plus"
    const val LEAK_FREQ_UNSURE = "leak.freq.unsure"
    const val LEAK_CONTEXT_COUGH = "leak.context.cough"
    const val LEAK_CONTEXT_SPORT = "leak.context.sport"
    const val LEAK_CONTEXT_URGE = "leak.context.urge"
    const val LEAK_CONTEXT_OTHER = "leak.context.other"
    const val LEAK_SKIP_BUTTON = "leak.skip_button"
    const val LEAK_SAVE_BUTTON = "leak.save_button"

    // ── 家人周报 P08 ──────────────────────────────────────────────────────
    const val P08_ROOT = "p08.root"
    const val P08_PREVIEW_CARD = "p08.preview_card"
    const val P08_SHARE_BUTTON = "p08.share_button"
    const val P08_ENABLE_SHARE_TOGGLE = "p08.enable_share_toggle"

    // ── 设置 P09 ──────────────────────────────────────────────────────────
    const val P09_ROOT = "p09.root"
    const val P09_VOICE_SPEED_08 = "p09.voice_speed.0_8"
    const val P09_VOICE_SPEED_10 = "p09.voice_speed.1_0"
    const val P09_VOICE_SPEED_12 = "p09.voice_speed.1_2"
    const val P09_REMINDER_TOGGLE = "p09.reminder_toggle"
    const val P09_REMINDER_TIME = "p09.reminder_time"
    const val P09_DEMO_MODE_TOGGLE = "p09.demo_mode_toggle"
    const val P09_CLEAR_DATA = "p09.clear_data"
    const val P09_PRIVACY = "p09.privacy"

    // ── 通用 ──────────────────────────────────────────────────────────────
    const val BOTTOM_BAR = "nav.bottom_bar"
    const val TAB_HOME = "nav.tab.home"
    const val TAB_TRAINING = "nav.tab.training"
    const val TAB_RECORDS = "nav.tab.records"
    const val TAB_PROFILE = "nav.tab.profile"
    const val DEMO_WATERMARK = "common.demo_watermark"
}