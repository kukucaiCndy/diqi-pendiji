package com.diqi.demo.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/** 安全筛查答案（FR-01，三选项，均可选「不确定」）。 */
enum class ScreeningAnswer(val storageValue: String) {
    YES("yes"),
    NO("no"),
    UNSURE("unsure");

    companion object {
        fun from(value: String?): ScreeningAnswer? =
            entries.firstOrNull { it.storageValue == value }
    }
}

/**
 * 首次启动的筛查结果（FR-01 的 4 组问题）。
 *
 * ## 分流规则（需求明文，逐条落地）
 *
 * - **规则 A**：第 2、3 题任一为「是」→ 展示就医/专业评估提示，训练页常驻提示条，
 *   允许继续但**不建议加量**
 * - **规则 B**：第 1、4 题为「否/不确定」→ 建议先接受专业评估，允许继续，
 *   完成页**不给「已学会」结论**
 *
 * ## 为什么"未答完"按 fail-closed 处理
 *
 * 首版 `clinicalNoticeRequired` 只看 Q2/Q3 是否 YES，为 null（漏答）时返回 false
 * —— 筛查没做完反而更放行，与 `mustAvoidLearnedConclusion` 的 null 处理方向相反
 * （评审 C2）。现在规则 A 也把"未答完"算作需要提示。
 */
data class SafetyScreening(
    /** 1. 是否接受过盆底肌训练的专业指导？ */
    val q1ProfessionalGuidance: ScreeningAnswer? = null,
    /** 2. 是否有盆腔疼痛、血尿、排尿困难等新发异常？ */
    val q2NewSymptoms: ScreeningAnswer? = null,
    /** 3. 是否处于孕期、或有盆底肌高张/疼痛、近期相关手术？ */
    val q3PregnancyOrSurgery: ScreeningAnswer? = null,
    /** 4. 是否能感觉到盆底肌收缩？ */
    val q4CanFeelContraction: ScreeningAnswer? = null,
) {
    val isComplete: Boolean
        get() = q1ProfessionalGuidance != null && q2NewSymptoms != null &&
            q3PregnancyOrSurgery != null && q4CanFeelContraction != null

    /**
     * 真的答到了风险项（第 2、3 题任一为「是」）。
     *
     * 首页常驻就医提示条只看这个 —— 文案是"你之前回答过需要留意的情况"，
     * 不能把"没答完"也算进去，否则直接跳过引导的用户会看到一句关于
     * 自己从没答过的问题的断言（评审指出的真实缺陷）。
     */
    val hasRiskAnswers: Boolean
        get() = q2NewSymptoms == ScreeningAnswer.YES ||
            q3PregnancyOrSurgery == ScreeningAnswer.YES

    /** 筛查尚未完成（含直接跳过）。用于"建议补做筛查"的软提示，不是就医警告。 */
    val isIncomplete: Boolean get() = !isComplete

    /**
     * 规则 A（供需要"宁可多提示"的场景使用，例如训练页）：
     * 未答完或命中风险项都算需要提示。
     */
    val clinicalNoticeRequired: Boolean
        get() = !isComplete || hasRiskAnswers

    /**
     * 规则 B 的第一条推论：不得给出「已学会」结论。
     *
     * 显式写成"两者都确认为是才放行"，而不是德摩根化简 ——
     * 化简后与 [suggestsProfessionalAssessment] 表达式完全相同，
     * 两条独立需求靠隐式布尔代数凑对，改一处会静默影响另一处（评审 C3）。
     */
    val mustAvoidLearnedConclusion: Boolean
        get() = !(q1ProfessionalGuidance == ScreeningAnswer.YES &&
            q4CanFeelContraction == ScreeningAnswer.YES)

    /** 规则 B 的第二条推论：建议先接受专业评估 */
    val suggestsProfessionalAssessment: Boolean
        get() = q1ProfessionalGuidance != ScreeningAnswer.YES ||
            q4CanFeelContraction != ScreeningAnswer.YES
}

/** 用户可见的全部本地设置（需求 §6 AppSettings + Consent）。 */
data class AppSettings(
    /** 语音速度倍率 0.8 / 1.0 / 1.2 */
    val audioSpeed: Float = 1.0f,
    /** 音量 0–1 */
    val volume: Float = 1.0f,
    val reminderEnabled: Boolean = false,
    /** "HH:mm" */
    val reminderTime: String = DEFAULT_REMINDER_TIME,
    /** 演示模式（AC-19）。默认关闭，入口在设置页底部且不醒目 */
    val isDemoMode: Boolean = false,
    val privacyNoticeAcceptedAt: Long? = null,
    val safetyAcknowledgedAt: Long? = null,
    val screening: SafetyScreening = SafetyScreening(),
    /** 相机授权同意时间。**撤销后仍保留**，用于区分"从未同意"与"同意后撤销" */
    val cameraConsentAt: Long? = null,
    val cameraRevokedAt: Long? = null,
    /** 家人周报分享开关，**默认关闭**（FR-13）*/
    val shareEnabled: Boolean = false,
    val shareRevokedAt: Long? = null,
) {
    /** 相机当前是否处于授权状态：同意过且其后没有撤销 */
    val cameraConsentActive: Boolean
        get() {
            val consented = cameraConsentAt ?: return false
            val revoked = cameraRevokedAt ?: return true
            return consented > revoked
        }

    companion object {
        const val DEFAULT_REMINDER_TIME = "20:00"
    }
}

/**
 * 设置与同意信息的唯一来源。
 *
 * ## 为什么 AppSettings 与 Consent 共用同一个 DataStore 文件
 *
 * 两者读写场景完全重叠（引导页、设置页、周报页），都是一次性小数据。
 * 拆两个 DataStore 会带来"同一时刻两个文件同时初始化"的竞态风险
 * （DataStore 同名多实例抛 IllegalStateException），收益为零。
 * 用字段语义区分，而不是用文件区分。
 *
 * ## 但「清除数据」必须区分范围（评审 A4）
 *
 * 首版 `clearAll()` 是 `prefs.clear()`，连 `camera_consent_at` / `screening_*` /
 * `privacy_accepted_at` 一起抹掉 —— 设置页的"清除数据"于是变成"销毁同意证据"。
 * 合规留痕不应被用户误触销毁。故拆成两个方法：
 * - [clearBusinessSettings]：清训练偏好，保留同意与筛查墓碑（设置页使用）
 * - [clearEverythingForTests]：全清（仅测试与调试使用）
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "herrhythm_settings",
)

class AppSettingsStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    /**
     * 读路径必须能容忍坏文件。
     *
     * 没有 `.catch` 时，一次 IO 异常或 `CorruptedDataStoreException` 会让 Flow
     * 直接抛给收集方 —— 首屏永远拿不到设置（评审 C6）。
     */
    val settings: Flow<AppSettings> = dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { it.toAppSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setAudioSpeed(speed: Float) = edit {
        it[AUDIO_SPEED] = speed.takeIf { v -> v.isFinite() }?.coerceIn(MIN_SPEED, MAX_SPEED) ?: 1.0f
    }

    suspend fun setVolume(volume: Float) = edit {
        it[VOLUME] = volume.takeIf { v -> v.isFinite() }?.coerceIn(0f, 1f) ?: 1.0f
    }

    suspend fun setReminderEnabled(enabled: Boolean) = edit { it[REMINDER_ENABLED] = enabled }

    suspend fun setReminderTime(hhmm: String) = edit {
        // 非法时间不写库，避免设置页显示 "25:99" 这种值
        if (isValidHhmm(hhmm)) it[REMINDER_TIME] = hhmm
    }

    /** 演示模式开关（FR-14）。真实数据源切换由 SensorCoordinator 负责。 */
    suspend fun setDemoMode(enabled: Boolean) = edit { it[DEMO_MODE] = enabled }

    suspend fun acceptPrivacyNotice(atMs: Long) = edit { it[PRIVACY_ACCEPTED_AT] = atMs }

    /**
     * 保存筛查结果。
     *
     * 必须**先清除旧答案**：首版只 `?.let { prefs[Qn] = it }`，null 不覆盖旧值，
     * 于是重做筛查时上一轮答案会越过本次"未答"写回，读回 YES 就会影响分流
     * （评审 C4）。`safetyAcknowledgedAt` 也只在整个筛查答完时落，
     * 否则"未完成"也会被记成"已确认"。
     */
    suspend fun setScreening(screening: SafetyScreening, atMs: Long) = edit { prefs ->
        listOf(Q1, Q2, Q3, Q4).forEach { prefs.remove(it) }
        screening.q1ProfessionalGuidance?.let { prefs[Q1] = it.storageValue }
        screening.q2NewSymptoms?.let { prefs[Q2] = it.storageValue }
        screening.q3PregnancyOrSurgery?.let { prefs[Q3] = it.storageValue }
        screening.q4CanFeelContraction?.let { prefs[Q4] = it.storageValue }
        if (screening.isComplete) {
            prefs[SAFETY_ACK_AT] = atMs
        } else {
            prefs.remove(SAFETY_ACK_AT)
        }
    }

    /**
     * 用户选择「跳过安全筛查，直接开始」。
     *
     * 只写 `safetyAcknowledgedAt`，**不写任何答案** ——
     * 于是库里能区分"答完"（四个答案 + 时间戳）与"跳过"（只有时间戳），
     * 而两者都不会在下次启动时重新弹引导。
     *
     * 首版跳过按钮完全没有写库动作，导致跳过的人每次冷启动都重走引导，
     * 且答完 4 题后再按跳过一次丢失全部答案（评审指出）。
     */
    suspend fun acknowledgeScreeningSkipped(atMs: Long) = edit { prefs ->
        prefs.remove(Q1); prefs.remove(Q2); prefs.remove(Q3); prefs.remove(Q4)
        prefs[SAFETY_ACK_AT] = atMs
    }

    /**
     * 记录相机同意。
     *
     * 撤销时**保留** `consentedAt`，只追加 `revokedAt` ——
     * 需求 §6 的 Consent 表要求两个时间戳成对存在，以便区分
     * "从未同意"（两个都空）与"同意后撤销"（两个都有，revoked 更晚）。
     * 首版撤销时把 consentedAt 删了，等于销毁同意历史（评审 A3）。
     */
    suspend fun setCameraConsent(consented: Boolean, atMs: Long) = edit { prefs ->
        if (consented) {
            prefs[CAMERA_CONSENT_AT] = atMs
            prefs.remove(CAMERA_REVOKED_AT)
        } else {
            prefs[CAMERA_REVOKED_AT] = atMs
            // 不删 CAMERA_CONSENT_AT：那是"曾经同意过"的证据
        }
    }

    /** 周报分享开关（FR-13：默认关闭；未开启时 UI 不得发起分享）*/
    suspend fun setShareEnabled(enabled: Boolean, atMs: Long) = edit { prefs ->
        prefs[SHARE_ENABLED] = enabled
        if (enabled) prefs.remove(SHARE_REVOKED_AT) else prefs[SHARE_REVOKED_AT] = atMs
    }

    /**
     * 清除业务偏好，**保留**同意与筛查留痕（设置页「清除数据」使用）。
     */
    suspend fun clearBusinessSettings() = edit { prefs ->
        // 逐键取值再逐键回写。不用 `Pair<Key, Any>` 泛型列表：
        // `prefs.set(key, value)` 要求键值类型匹配，通配符列表编译不过。
        val privacy = prefs[PRIVACY_ACCEPTED_AT]
        val ack = prefs[SAFETY_ACK_AT]
        val q1 = prefs[Q1]
        val q2 = prefs[Q2]
        val q3 = prefs[Q3]
        val q4 = prefs[Q4]
        val consentAt = prefs[CAMERA_CONSENT_AT]
        val revokedAt = prefs[CAMERA_REVOKED_AT]
        val shareOn = prefs[SHARE_ENABLED]
        val shareRevokedAt = prefs[SHARE_REVOKED_AT]

        prefs.clear()

        privacy?.let { prefs[PRIVACY_ACCEPTED_AT] = it }
        ack?.let { prefs[SAFETY_ACK_AT] = it }
        q1?.let { prefs[Q1] = it }
        q2?.let { prefs[Q2] = it }
        q3?.let { prefs[Q3] = it }
        q4?.let { prefs[Q4] = it }
        consentAt?.let { prefs[CAMERA_CONSENT_AT] = it }
        revokedAt?.let { prefs[CAMERA_REVOKED_AT] = it }
        shareOn?.let { prefs[SHARE_ENABLED] = it }
        shareRevokedAt?.let { prefs[SHARE_REVOKED_AT] = it }
    }

    /** 全清。**仅测试与调试入口可用**，不要接到用户可见的按钮上。 */
    suspend fun clearEverythingForTests() = edit { it.clear() }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        // 读路径也 clamp：手改 DataStore 或旧版本写入的非法值不能直进播放器
        // （Float.coerceIn 对 NaN 无效，故先 takeIf isFinite）
        audioSpeed = (this[AUDIO_SPEED] ?: 1.0f)
            .takeIf { it.isFinite() }?.coerceIn(MIN_SPEED, MAX_SPEED) ?: 1.0f,
        volume = (this[VOLUME] ?: 1.0f)
            .takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 1.0f,
        reminderEnabled = this[REMINDER_ENABLED] ?: false,
        reminderTime = (this[REMINDER_TIME] ?: AppSettings.DEFAULT_REMINDER_TIME)
            .takeIf { isValidHhmm(it) } ?: AppSettings.DEFAULT_REMINDER_TIME,
        isDemoMode = this[DEMO_MODE] ?: false,
        privacyNoticeAcceptedAt = this[PRIVACY_ACCEPTED_AT],
        safetyAcknowledgedAt = this[SAFETY_ACK_AT],
        screening = SafetyScreening(
            q1ProfessionalGuidance = ScreeningAnswer.from(this[Q1]),
            q2NewSymptoms = ScreeningAnswer.from(this[Q2]),
            q3PregnancyOrSurgery = ScreeningAnswer.from(this[Q3]),
            q4CanFeelContraction = ScreeningAnswer.from(this[Q4]),
        ),
        cameraConsentAt = this[CAMERA_CONSENT_AT],
        cameraRevokedAt = this[CAMERA_REVOKED_AT],
        shareEnabled = this[SHARE_ENABLED] ?: false,
        shareRevokedAt = this[SHARE_REVOKED_AT],
    )

    private companion object {
        const val MIN_SPEED = 0.8f
        const val MAX_SPEED = 1.2f

        val AUDIO_SPEED = floatPreferencesKey("audio_speed")
        val VOLUME = floatPreferencesKey("volume")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
        val PRIVACY_ACCEPTED_AT = longPreferencesKey("privacy_accepted_at")
        val SAFETY_ACK_AT = longPreferencesKey("safety_ack_at")
        val Q1 = stringPreferencesKey("screening_q1")
        val Q2 = stringPreferencesKey("screening_q2")
        val Q3 = stringPreferencesKey("screening_q3")
        val Q4 = stringPreferencesKey("screening_q4")
        val CAMERA_CONSENT_AT = longPreferencesKey("camera_consent_at")
        val CAMERA_REVOKED_AT = longPreferencesKey("camera_revoked_at")
        val SHARE_ENABLED = booleanPreferencesKey("share_enabled")
        val SHARE_REVOKED_AT = longPreferencesKey("share_revoked_at")

        fun isValidHhmm(value: String): Boolean {
            val m = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$").matchEntire(value) ?: return false
            return m.groupValues.size == 3
        }
    }
}