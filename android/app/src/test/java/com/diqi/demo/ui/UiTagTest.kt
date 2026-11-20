package com.diqi.demo.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UiTag 的自检。
 *
 * 背景：评审指出首版 UiTag 有 114 个常量但**全工程引用 0 次** ——
 * 接线对了（dump 里确实出现了 resource-id），内容却是死的。
 * 死常量比没有常量更危险：它让人以为"已经有了断言锚点"。
 *
 * 这里能做的是把"常量本身必须合法"钉死（唯一、格式规范、不带歧义后缀），
 * 至于"每个常量必须被 testTag 引用"，只能在每个阶段实现对应页面时逐一接线，
 * 由 `docs/AC-MATRIX.md` 的逐条验证来保证 —— 无法用单测替代。
 */
class UiTagTest {

    /** `const val` 在 object 里编译为静态字段。 */
    private val allTags: Map<String, String> = UiTag::class.java.declaredFields
        .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == String::class.java }
        .associate { field ->
            field.isAccessible = true
            field.name to (field.get(null) as String)
        }

    @Test
    fun `至少存在一个 tag`() {
        assertTrue("UiTag 反射不到任何常量", allTags.isNotEmpty())
    }

    @Test
    fun `所有 tag 值全局唯一`() {
        val duplicates = allTags.entries
            .groupBy({ it.value }, { it.key })
            .filterValues { it.size > 1 }
        assertEquals("存在重复的 tag 值：$duplicates", emptyMap<String, List<String>>(), duplicates)
    }

    @Test
    fun `所有 tag 非空`() {
        val empties = allTags.filterValues { it.isBlank() }.keys
        assertEquals("存在空白 tag：$empties", emptySet<String>(), empties)
    }

    @Test
    fun `tag 命名规范为 小写点分子 或 下划线`() {
        val bad = allTags.filterValues { value ->
            !value.matches(Regex("^[a-z0-9_.]+$"))
        }
        assertEquals(
            "tag 只允许小写字母/数字/点/下划线（避免与 resource-id 解析冲突）：$bad",
            emptyMap<String, String>(),
            bad,
        )
    }

    /**
     * AC-19 的自动化判据是「训练页存在演示水印」。
     * 若存在 `p04.demo_watermark` 与 `common.demo_watermark` 两个后缀相同、
     * 前缀不同的 tag，用"后缀匹配"的定位器会把任意一个当成另一个 ——
     * 于是"训练页水印存在"会被全局水印满足，判据形同虚设。
     *
     * e2e.sh 已改为**精确匹配**，这里再补一道：验证这两个 tag 确实不同值。
     */
    @Test
    fun `训练页水印与全局水印必须是两个不同的 tag`() {
        assertTrue(UiTag.P04_DEMO_WATERMARK != UiTag.DEMO_WATERMARK)
        assertTrue(UiTag.P04_DEMO_WATERMARK.endsWith("demo_watermark"))
        assertTrue(UiTag.DEMO_WATERMARK.endsWith("demo_watermark"))
        // 若二者之一被改名导致不再同后缀，这个断言会提醒同步更新 AC-MATRIX
        assertTrue(
            "两个水印 tag 若失去共同后缀，说明命名约定变了，需同步 docs/AC-MATRIX.md",
            UiTag.P04_DEMO_WATERMARK.substringAfter('.')
                .endsWith(UiTag.DEMO_WATERMARK.substringAfter('.')),
        )
    }

    @Test
    fun `关键合规锚点必须存在`() {
        // 这些 tag 承载的是需求硬性合规要求，缺失即视为回归
        assertTrue(UiTag.P04_DISCLAIMER.isNotBlank())          // §5.6 指标区常驻免责
        assertTrue(UiTag.P04_RHYTHM_RING_LABEL.isNotBlank())   // §5.6 圆环标注「节律提示」
        assertTrue(UiTag.P04_METRIC_PLACEHOLDER.isNotBlank())  // FR-08 无数据不显示数字
        assertTrue(UiTag.P01_CLINICAL_NOTICE.isNotBlank())     // FR-01 就医提示
        assertTrue(UiTag.P05_DISCOMFORT_NOTICE.isNotBlank())   // FR-09 不适提示
    }
}