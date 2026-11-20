package com.diqi.demo.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AC-14「文案检索无禁词」的构建期手段之一。
 *
 * 另一个手段是 `tools/scan-banned.sh` 扫全部字符串资源；
 * 两者互补：脚本抓仓库里的静态文案，本测试抓词表自身的正确性
 * （确保词表真能抓到评审点名的那几个漏网词）。
 */
class BannedWordsTest {

    @Test
    fun `评审点名的三个漏词现在都能抓到`() {
        // V1.0 词表只有「治愈率」，抓不到孤字「治愈」
        assertTrue(BannedWords.hasViolation("症状已治愈"))
        // V1.0 只有「盆底肌力」，抓不到孤字「肌力」
        assertTrue(BannedWords.hasViolation("提升肌力"))
        // V1.0 完全缺「收缩正确率」
        assertTrue(BannedWords.hasViolation("收缩正确率 92%"))
        // V1.0 缺「动作标准」
        assertTrue(BannedWords.hasViolation("动作标准度良好"))
    }

    @Test
    fun `完整禁词表逐项可命中`() {
        val samples = mapOf(
            "盆底肌力" to "你的盆底肌力提升了",
            "肌力" to "肌力评估",
            "动作正确率" to "动作正确率 100%",
            "收缩正确率" to "收缩正确率很高",
            "动作标准" to "动作标准",
            "治愈" to "可以治愈",
            "显著改善" to "显著改善",
            "疗效" to "疗效确切",
            "血压" to "血压 120/80",
            "血氧" to "血氧饱和度 98%",
            "情绪" to "情绪评分",
            "心理评分" to "心理评分 8 分",
            "HRV" to "HRV 均值 42ms",
        )
        samples.forEach { (word, text) ->
            assertTrue("应命中「$word」：$text", BannedWords.hasViolation(text))
            assertTrue("scan 应返回「$word」", BannedWords.scan(text).contains(word))
        }
    }

    @Test
    fun `合规文案不会被误伤`() {
        val safe = listOf(
            "反映身体状态，不代表盆底肌收缩质量",   // 需求指定的免责文案（含「盆底肌」但非「盆底肌力」）
            "节律提示",
            "保持住，继续自然呼吸，不要憋气。",
            "轻轻提起盆底，像忍住排气一样，不要向下用力。",
            "今天，我们一起练习照顾自己的身体。",
            "今天已完成，可再练一次",
            "信号建立中",
            "信号暂失",
            "未启用感知",
            "光线有点暗，请面向光源",
            "已就绪",
            "正在建立信号 12s / 约 30s",
        )
        safe.forEach { text ->
            assertFalse("不应命中：$text（实际命中 ${BannedWords.scan(text)}）", BannedWords.hasViolation(text))
        }
    }

    @Test
    fun `免责文案必须包含关键否定语 —— 防止被改成肯定句`() {
        val disclaimer = "反映身体状态，不代表盆底肌收缩质量"
        assertTrue("免责文案必须含「不代表」", disclaimer.contains("不代表"))
        assertTrue("免责文案必须提「身体状态」", disclaimer.contains("身体状态"))
    }

    @Test
    fun `白名单句子内的已学会不被误判`() {
        // FR-01 要求完成页不给「已学会」结论，该说明句本身必须能正常展示
        val allowed = "本次不给出「已学会」结论"
        assertTrue("白名单句不应命中", !BannedWords.hasViolation(allowed))
        // 但白名单之外的「已学会」仍要抓
        assertTrue(BannedWords.hasViolation("已学会收缩技巧"))
    }

    @Test
    fun `大小写不敏感`() {
        assertTrue(BannedWords.hasViolation("hrv 数据"))
        assertTrue(BannedWords.hasViolation("Hrv 数据"))
    }

    @Test
    fun `空串与纯标点不命中`() {
        assertEquals(emptyList<String>(), BannedWords.scan(""))
        assertEquals(emptyList<String>(), BannedWords.scan("。，、；！？"))
    }
}