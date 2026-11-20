package com.diqi.demo.core.text

/**
 * 文案红线词表（需求 §5.6 / AC-14）。
 *
 * V1.0 计划里的词表只有 4 项，评审指出漏词：
 * 「治愈率」抓不到孤字「治愈」、「盆底肌力」抓不到孤字「肌力」、
 * 且缺「收缩正确率」「动作标准」。此处按修正后的完整词表实现，
 * 由 `BannedWordsTest` + `tools/scan-banned.sh` 双重把关。
 *
 * 注意：本词表只能抓**字面**违规。语义级违规（例如把心率描述成
 * 「你的盆底肌正在收缩」）无法靠正则发现，必须靠固定文案白名单
 * 加内容/临床评审，这一点在验收记录里如实标注。
 */
object BannedWords {

    /** 禁用词（字面匹配，大小写不敏感）。 */
    val WORDS: List<String> = listOf(
        // 疗效 / 能力断言
        "盆底肌力",
        "肌力",
        "动作正确率",
        "收缩正确率",
        "动作标准",
        "治愈",
        "康复",        // 评审 E5：全局扫描原本不设防，只在某个测试的本地表里出现过
        "显著改善",
        "疗效",
        "已学会",      // 仅允许在「专业评估后」白名单场景出现，见 ALLOWED_CONTEXTS
        // 超范围生理指标（本期不做）
        "血压",
        "血氧",
        "情绪",
        "心理评分",
        "HRV",
        "HRV 均值",
    )

    /**
     * 白名单上下文：命中这些整句时，允许其中出现某个敏感词。
     *
     * 例：FR-01 要求「第 2、3 题为是 → 展示就医/专业评估提示」，
     * 而 FR-01 同时要求筛查问题里出现「盆底肌训练」这类表述。
     * 目前只有一处：「完成页不给『已学会』结论」是**否定式**使用，
     * 需求原文即要求出现该词以作说明，故整句放行。
     */
    val ALLOWED_SENTENCES: List<String> = listOf(
        "本次不给出「已学会」结论",
        "本次不给出已学会结论",
    )

    /**
     * 扫描一段文本，返回命中的禁用词。
     * @return 命中列表（可能为空）
     */
    fun scan(text: String): List<String> {
        val withoutAllowed = ALLOWED_SENTENCES.fold(text) { acc, s -> acc.replace(s, "") }
        return WORDS.filter { word ->
            withoutAllowed.contains(word, ignoreCase = true)
        }
    }

    fun hasViolation(text: String): Boolean = scan(text).isNotEmpty()
}