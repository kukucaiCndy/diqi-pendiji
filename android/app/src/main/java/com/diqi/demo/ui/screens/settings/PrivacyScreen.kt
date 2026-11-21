package com.diqi.demo.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diqi.demo.ui.UiTag

/**
 * L2-06 隐私说明（详情页）。
 *
 * 从 P07 设置页进入。完整版隐私说明，文案逐条照抄 docs/DESIGN-SPEC.md L2-06。
 * 正文 ≥20sp（FR-15 适老化基线）。
 */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HrPrivCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(UiTag.P09_PRIVACY_ROOT),
    ) {
        // 导航栏：返回 + 标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(role = Role.Button) { onBack() }
                    .testTag(UiTag.P09_PRIVACY_BACK),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "返回",
                    tint = HrPrivInk,
                    modifier = Modifier.size(23.dp),
                )
            }
            Text("隐私说明", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HrPrivInk)
            Spacer(Modifier.size(23.dp))
        }

        // 内容区
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PrivacyClause(
                title = "画面只在本地处理",
                body = "摄像头采集的每一帧都在手机内存中完成人脸检测与信号计算，处理完立即丢弃，不写入存储、不缓存截图。",
            )
            PrivacyClause(
                title = "不录像、不上传",
                body = "应用没有录制功能，也不会把画面或帧数据发送到任何服务器。关闭应用后没有任何图像残留。",
            )
            PrivacyClause(
                title = "只有激活数据出网",
                body = "首次使用时会发送设备标识与 License 请求到激活服务，用于确认本机使用权。此外没有任何网络请求。",
            )
            PrivacyClause(
                title = "你可以随时关闭",
                body = "训练中点击右上角摄像头角标即可暂停感知，节律仍会继续，指标会变灰而非报错。",
            )
            PrivacyClause(
                title = "本机数据可删除",
                body = "设置 → 清除本机数据，可删除全部训练记录、日记与设置，不可恢复。",
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "隐私条款版本 1.1　·　2026-11-20 生效",
                fontSize = 11.sp,
                color = HrPrivTertiary,
            )
            Spacer(Modifier.height(22.dp))
        }
    }
}

/** 单条隐私条款卡：标题 15/700 + 正文 ≥20sp（FR-15）。 */
@Composable
private fun PrivacyClause(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HrPrivClauseBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HrPrivInk)
        Text(body, fontSize = 20.sp, color = HrPrivMuted, lineHeight = 28.sp)
    }
}

private val HrPrivCanvas = Color(0xFFF4F6F8)
private val HrPrivClauseBg = Color(0xFFF8FAFC)
private val HrPrivInk = Color(0xFF0F1A24)
private val HrPrivMuted = Color(0xFF5C6B7A)
private val HrPrivTertiary = Color(0xFF94A3B8)
