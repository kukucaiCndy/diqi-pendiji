package com.diqi.demo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * HerRhythm DS v1.0 · Spacing & Radius
 *
 * 取自 `design/desing.pen` 的「区块 03 间距与圆角」：
 *   间距序列 sp4 sp8 sp12 sp16 sp20 sp24 sp32 sp40
 *   圆角：卡片 16 / 控件 12 / 胶囊 999
 *
 * 适老化硬约束（FR-15）：热区 ≥ 44dp。
 */
object HrSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 40.dp

    /** 页面左右安全边距（设计稿内容区 padding 26，这里取 20 以适配 372dp 窄屏）*/
    val pageHorizontal: Dp = 20.dp
    /** 卡片内边距（设计稿 18–20）*/
    val cardInner: Dp = 18.dp
}

object HrRadius {
    /** 卡片 */
    val card: Dp = 16.dp
    /** 控件（按钮、输入框、提示条）*/
    val control: Dp = 12.dp
    /** 胶囊 */
    val capsule: Dp = 999.dp

    val cardShape = RoundedCornerShape(card)
    val controlShape = RoundedCornerShape(control)
    val capsuleShape = RoundedCornerShape(capsule)
}

object HrSize {
    /** 适老化最小热区（FR-15）*/
    val minTouchTarget: Dp = 44.dp
    /** 主按钮高度（设计稿 44–48）*/
    val primaryButton: Dp = 56.dp
    /** 次要按钮高度 */
    val secondaryButton: Dp = 52.dp
    /** 胶囊高度（设计稿 26）*/
    val capsule: Dp = 32.dp
    /** 进度轨粗细（设计稿 4–6）*/
    val progressTrack: Dp = 6.dp
    /** 周打卡圆点（设计稿 8）*/
    val weekDot: Dp = 12.dp
    /** 时间轴节点 */
    val timelineDot: Dp = 10.dp
    /** 分隔线 */
    val hairline: Dp = 1.dp
    /** 人脸框（设计稿 200×248 @468 宽，按比例换算）*/
    val faceBoxWidth: Dp = 172.dp
    val faceBoxHeight: Dp = 214.dp
}