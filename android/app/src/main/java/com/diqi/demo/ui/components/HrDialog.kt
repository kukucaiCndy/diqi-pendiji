package com.diqi.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 居中弹窗（对应设计稿 **M03** 的规格）。
 *
 * ## 为什么不用 Material `AlertDialog`
 *
 * 设计稿 M03 是一个 400 宽、圆角 20、内边距 24、纵向堆叠按钮的卡片：
 * 标题 20/700、说明 14/400 `#5C6B7A`、主按钮 h52 r26 深色、
 * 次按钮 h48 无底色红字。Material 默认 AlertDialog 的标题/按钮排布与它差得远。
 *
 * 深色遮罩用设计稿的 `#0F1A2480`（50% 黑蓝），不是 Material 默认的 scrim。
 */
@Composable
fun HrCenterDialog(
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    secondaryIsDanger: Boolean = true,
    onDismiss: (() -> Unit)? = null,
    tag: String? = null,
    primaryTag: String? = null,
    secondaryTag: String? = null,
) {
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onDismiss != null,
            dismissOnClickOutside = onDismiss != null,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x800F1A24))
                .padding(horizontal = 34.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(24.dp)
                    .let { m -> if (tag != null) m.testTag(tag) else m },
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HrDlgInk)
                Text(message, fontSize = 14.sp, color = HrDlgMuted, lineHeight = 20.sp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(HrDlgInk)
                        .let { m -> if (primaryTag != null) m.testTag(primaryTag) else m }
                        .clickable(onClick = onPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(primaryLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (secondaryLabel != null && onSecondary != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .let { m -> if (secondaryTag != null) m.testTag(secondaryTag) else m }
                            .clickable(onClick = onSecondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            secondaryLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (secondaryIsDanger) HrDlgDanger else HrDlgMuted,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 底部弹窗（对应设计稿 **M01** 结束确认）。
 *
 * 设计稿：底部 300 高、圆角 `[24,24,0,0]`、内边距 `[10,22,22,22]`、
 * 顶部 44×5 拉手、标题 21/700、说明 14/400、
 * 「继续练习」h54 r27 深色（主操作）、「仍然结束」h50 红字（次操作、无底色）。
 *
 * 注意**主次顺序**：设计稿把「继续练习」做成主按钮、「仍然结束」做成文字按钮 ——
 * 即"避免用户误结束"是这个弹窗的设计意图。
 */
@Composable
fun HrBottomSheetDialog(
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x800F1A24)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
                    .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(HrDlgTrack),
                    )
                }
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = HrDlgInk)
                Text(message, fontSize = 14.sp, color = HrDlgMuted, lineHeight = 20.sp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(HrDlgInk)
                        .clickable(onClick = onPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(primaryLabel, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .clickable(onClick = onSecondary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(secondaryLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HrDlgDanger)
                }
            }
        }
    }
}

private val HrDlgInk = Color(0xFF0F1A24)
private val HrDlgMuted = Color(0xFF5C6B7A)
private val HrDlgDanger = Color(0xFFB91C1C)
private val HrDlgTrack = Color(0xFFCBD5E1)
