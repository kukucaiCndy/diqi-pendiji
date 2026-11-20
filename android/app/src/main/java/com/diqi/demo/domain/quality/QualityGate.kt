package com.diqi.demo.domain.quality

import com.diqi.demo.data.db.MissingReason
import com.diqi.demo.data.db.QualityState
import com.diqi.demo.sensor.SensorSnapshot

/**
 * 质量四态判定（FR-08）。
 *
 * ## 判定规则（逐条对齐需求表）
 *
 * | 状态 | 判定 | 界面 | 系统动作 |
 * |---|---|---|---|
 * | VALID | 数据新鲜、质量与时间戳满足阈值 | 显示真实指标 | 正常记录 |
 * | DEGRADED | 部分质量下降 / 延迟高 / 字段缺失 | 只显示仍可靠项，标注「信号一般」 | 继续投喂，不记录不可信值 |
 * | INVALID | 有数据但不可信（SQI 低于门限） | 显示「--」+「信号弱」 | 不记录、不参与统计 |
 * | NO_DATA | 未授权 / SDK 不可用 / 离框 > 3s / 长时间无新值 | 显示「信号暂失」，整块置灰 | 停止分析，节律继续 |
 *
 * ## 补充规则（需求明文，最容易做错的三条）
 *
 * 1. **人脸丢失 < 3s**：指标半透明 + 轻提示，**不切换状态**
 * 2. **光照不足**：顶部提示条，质量降为 DEGRADED
 * 3. **前 30 秒**：只显示「信号建立中」，不显示任何数字（AC-15）
 *
 * 这里的实现是**纯函数**，所以上面每条规则都能用单测钉死，
 * 不需要真机、不需要摄像头。
 */
object QualityGate {

    /** 默认 SQI 门限（需求 §5.4 初值 0.50，真机标定后写入 TrainingParams）*/
    const val DEFAULT_SQI_THRESHOLD = 0.50f

    /** 人脸丢失多久后判 NO_DATA（需求：离框 > 3s）*/
    const val FACE_LOST_NO_DATA_MS = 3_000L

    /** 心率可用的最短时长（SDK 特性，§5.4）*/
    const val HR_READY_SEC = 30

    /**
     * 判定结果。
     *
     * @param showNumbers 是否允许显示任何数字。**为 false 时 UI 不得渲染数值**
     * @param dimmed 指标区是否半透明（人脸短暂丢失）
     * @param banner 需要常驻的提示文案（光照等）
     */
    data class Verdict(
        val state: QualityState,
        val showNumbers: Boolean,
        val dimmed: Boolean = false,
        val banner: String? = null,
        val reason: MissingReason = MissingReason.NONE,
    )

    /**
     * @param snapshot 适配器给的原始快照
     * @param faceLostMs 人脸已连续丢失的毫秒数（0 表示在框内）
     * @param lightOk 光照是否充足
     * @param sqiThreshold SQI 门限
     */
    fun evaluate(
        snapshot: SensorSnapshot,
        faceLostMs: Long = 0L,
        lightOk: Boolean = snapshot.lightOk,
        sqiThreshold: Float = DEFAULT_SQI_THRESHOLD,
    ): Verdict {
        // 1) 未启用感知 —— 整块不显示
        if (snapshot.missingReason == MissingReason.NOT_ENABLED) {
            return Verdict(
                state = QualityState.NO_DATA,
                showNumbers = false,
                reason = MissingReason.NOT_ENABLED,
            )
        }

        // 2) 前 30 秒：信号建立中，一个数字都不许出（AC-15）
        if (snapshot.elapsedSec < HR_READY_SEC) {
            return Verdict(
                state = QualityState.NO_DATA,
                showNumbers = false,
                reason = MissingReason.NONE,
            )
        }

        // 3) 人脸丢失：< 3s 只半透明、不切状态；≥ 3s 判 NO_DATA
        if (faceLostMs >= FACE_LOST_NO_DATA_MS) {
            return Verdict(
                state = QualityState.NO_DATA,
                showNumbers = false,
                reason = MissingReason.FACE_LOST,
                banner = "看不到脸了，请把手机放稳",
            )
        }

        // 4) SQI 低于门限：有数据但不可信 → INVALID，显示「--」
        val sqi = snapshot.sqi
        if (sqi != null && sqi < sqiThreshold) {
            return Verdict(
                state = QualityState.INVALID,
                showNumbers = false,
                reason = MissingReason.TIMEOUT,
            )
        }

        // 5) 无有效数值：NO_DATA（不是 INVALID —— 后者意味着"有数据但不可信"）
        if (snapshot.heartRateBpm == null && snapshot.breathingRatePerMin == null) {
            return Verdict(
                state = QualityState.NO_DATA,
                showNumbers = false,
                reason = snapshot.missingReason,
            )
        }

        // 6) 光照不足：降为 DEGRADED，但**仍然显示可靠项**
        if (!lightOk) {
            return Verdict(
                state = QualityState.DEGRADED,
                showNumbers = true,
                reason = MissingReason.LOW_LIGHT,
                banner = "光线不足，可以把手机换个方向",
            )
        }

        // 7) 人脸短暂丢失（< 3s）：保持 VALID，但半透明 + 轻提示
        if (faceLostMs > 0L) {
            return Verdict(
                state = QualityState.VALID,
                showNumbers = true,
                dimmed = true,
                reason = MissingReason.FACE_LOST,
            )
        }

        return Verdict(state = QualityState.VALID, showNumbers = true)
    }

    /** 质量三点指示（设计稿「三点质量指示」）：返回 0–3 */
    fun qualityDots(state: QualityState): Int = when (state) {
        QualityState.VALID -> 3
        QualityState.DEGRADED -> 2
        QualityState.INVALID -> 1
        QualityState.NO_DATA -> 0
    }
}