package com.diqi.demo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * AC-19 的**数据层**防线：演示数据不得进入任何统计。
 *
 * ## 为什么是"读源码"的静态检查
 *
 * 首版用反射读 `@Query`，实际跑不起来：Room 的 `@Query` 是
 * `AnnotationRetention.BINARY`（Java 的 `RetentionPolicy.CLASS`），
 * **运行期不可见**，反射取不到任何注解 —— 测试会"通过"得毫无意义。
 *
 * ## 为什么不用正则在全文上直接匹配 `@Query(...)`
 *
 * 首版正则有两个致命盲区（评审 B2 指出）：
 *   1. 只扫 `Daos.kt` 一个文件 —— 新增 `MetricsDao.kt` 就永久逃出守卫
 *   2. 漏带尾随逗号的多行写法，只解析到少数单行查询，而"漏解析"会让
 *      过滤检查变成**假通过**
 *
 * 现在的做法：
 *   · 扫描 `src/main` 下**全部** Kotlin 文件
 *   · 先剥掉注释，避免注释里的 `@Query` 被当成真查询
 *   · 用**括号配平扫描器**（而非正则）提取每条 SQL，兼容 `"..."` 与 `"""..."""`
 *   · 自校验「已解析的 @Dao 数」等于「源码中含 @Dao 的文件数」
 */
class SimulatedFilterTest {

    private val allowList = setOf(
        "upsert", "findById",
        "simulatedSessions",   // 记录页「演示记录」区（D9）
        "countAll", "deleteAll", "deleteSimulated", "deleteSeeds",
        // 按所属会话主键读取质量行：调用方已持有会话对象，不存在跨会话统计
        "findBySession",
    )

    /**
     * **每个 DAO 都必须在这里被显式分类**：它的表是否含"演示维度"。
     *
     * 之所以要显式列出而不是自动推断：一个 DAO 该不该过滤演示数据，
     * 取决于它的表有没有 `sensorMode` 语义 —— 这只能由人判断。
     * 新增 DAO 而未分类时，下面的测试会直接红，逼着做决定。
     *
     * · `true`  = 表有演示维度，所有查询必须带 `sensorMode != 'simulated'`
     * · `false` = 表与演示无关（漏尿日记是用户事实，不区分数据源）
     */
    private val daoRequiresSimulatedFilter = mapOf(
        "TrainingSessionDao" to true,
        "SensorQualityDao" to true,   // 质量表自身无列，但跨会话聚合必须 JOIN 过滤
        "LeakDiaryDao" to false,
    )

    private data class QueryEntry(val dao: String, val name: String, val sql: String)

    private val srcMain: File by lazy {
        listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull { it.isDirectory }
            ?: error("找不到 src/main/java，工作目录=${File(".").absolutePath}")
    }

    // ── 源码处理 ──────────────────────────────────────────────────────────

    /** 去掉行注释与块注释，但保留字符串内容。 */
    private fun stripComments(src: String): String {
        val sb = StringBuilder(src.length)
        var i = 0
        var inString = false
        var inTriple = false
        while (i < src.length) {
            val c = src[i]
            val next = if (i + 1 < src.length) src[i + 1] else '\u0000'
            when {
                inTriple -> {
                    if (c == '"' && next == '"' && i + 2 < src.length && src[i + 2] == '"') {
                        sb.append("\"\"\""); i += 3; inTriple = false
                    } else { sb.append(c); i++ }
                }
                inString -> {
                    sb.append(c)
                    if (c == '\\' && i + 1 < src.length) { sb.append(next); i += 2; continue }
                    if (c == '"') inString = false
                    i++
                }
                c == '"' && next == '"' && i + 2 < src.length && src[i + 2] == '"' -> {
                    sb.append("\"\"\""); i += 3; inTriple = true
                }
                c == '"' -> { sb.append(c); inString = true; i++ }
                c == '/' && next == '/' -> { while (i < src.length && src[i] != '\n') i++ }
                c == '/' && next == '*' -> {
                    i += 2
                    while (i + 1 < src.length && !(src[i] == '*' && src[i + 1] == '/')) i++
                    i += 2
                }
                else -> { sb.append(c); i++ }
            }
        }
        return sb.toString()
    }

    /** 从 `(` 开始括号配平，返回括号内文本与结束位置。 */
    private fun balancedParen(src: String, openIndex: Int): Pair<String, Int> {
        var depth = 0
        var i = openIndex
        var inString = false
        var inTriple = false
        while (i < src.length) {
            val c = src[i]
            val next = if (i + 1 < src.length) src[i + 1] else '\u0000'
            if (inTriple) {
                if (c == '"' && next == '"' && i + 2 < src.length && src[i + 2] == '"') {
                    i += 3; inTriple = false; continue
                }
                i++; continue
            }
            if (inString) {
                if (c == '\\') { i += 2; continue }
                if (c == '"') inString = false
                i++; continue
            }
            when {
                c == '"' && next == '"' -> { inTriple = true; i += 3 }
                c == '"' -> { inString = true; i++ }
                c == '(' -> { depth++; i++ }
                c == ')' -> { depth--; if (depth == 0) return src.substring(openIndex + 1, i) to i; i++ }
                else -> i++
            }
        }
        return src.substring(openIndex + 1) to src.length
    }

    /** 把 `"..."` 与 `"""..."""` 片段拼接成一段 SQL。 */
    private fun extractSql(raw: String): String {
        val parts = mutableListOf<String>()
        var i = 0
        while (i < raw.length) {
            when {
                raw.startsWith("\"\"\"", i) -> {
                    val end = raw.indexOf("\"\"\"", i + 3)
                    if (end < 0) { i = raw.length; continue }
                    parts += raw.substring(i + 3, end).trimIndent()
                    i = end + 3
                }
                raw[i] == '"' -> {
                    val sb = StringBuilder()
                    var j = i + 1
                    while (j < raw.length && raw[j] != '"') {
                        if (raw[j] == '\\' && j + 1 < raw.length) { sb.append(raw[j + 1]); j += 2 }
                        else { sb.append(raw[j]); j++ }
                    }
                    parts += sb.toString()
                    i = j + 1
                }
                else -> i++
            }
        }
        return parts.joinToString(" ").replace(Regex("\\s+"), " ").trim()
    }

    private data class DaoBlock(val name: String, val body: String)

    private fun firstIndexWhere(s: String, from: Int, predicate: (Char) -> Boolean): Int {
        for (i in from until s.length) if (predicate(s[i])) return i
        return -1
    }

    /** 找出源码里所有 `@Dao` 标注的 interface 及其主体。 */
    private fun daoBlocks(code: String): List<DaoBlock> {
        val result = mutableListOf<DaoBlock>()
        val marker = Regex("@Dao\\b")
        var searchFrom = 0
        while (true) {
            val m = marker.find(code, searchFrom) ?: break
            val ifaceIdx = code.indexOf("interface ", m.range.last)
            if (ifaceIdx < 0) break
            val nameEnd = firstIndexWhere(code, ifaceIdx + 10) { !it.isLetterOrDigit() && it != '_' }
            val name = code.substring(ifaceIdx + 10, if (nameEnd < 0) code.length else nameEnd).trim()
            val braceIdx = code.indexOf('{', ifaceIdx)
            if (braceIdx < 0) break
            var depth = 0
            var i = braceIdx
            while (i < code.length) {
                if (code[i] == '{') depth++
                if (code[i] == '}') { depth--; if (depth == 0) break }
                i++
            }
            result += DaoBlock(name, code.substring(braceIdx, minOf(i + 1, code.length)))
            searchFrom = i + 1
        }
        return result
    }

    private fun queriesIn(block: DaoBlock): List<QueryEntry> {
        val out = mutableListOf<QueryEntry>()
        val marker = Regex("@Query\\s*\\(")
        var from = 0
        while (true) {
            val m = marker.find(block.body, from) ?: break
            val openIdx = block.body.indexOf('(', m.range.first)
            val (inner, closeIdx) = balancedParen(block.body, openIdx)
            val funMatch = Regex("(?:suspend\\s+)?fun\\s+(\\w+)")
                .find(block.body.substring(closeIdx)) ?: break
            out += QueryEntry(block.name, funMatch.groupValues[1], extractSql(inner))
            from = closeIdx + 1
        }
        return out
    }

    private fun mainKotlinFiles(): List<File> =
        srcMain.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun allQueries(): List<QueryEntry> = mainKotlinFiles()
        .flatMap { f -> daoBlocks(stripComments(f.readText())).flatMap { queriesIn(it) } }

    private fun allDaoNames(): List<String> = mainKotlinFiles()
        .flatMap { f -> daoBlocks(stripComments(f.readText())).map { it.name } }

    // ── 断言 ──────────────────────────────────────────────────────────────

    @Test
    fun `每个 DAO 都必须显式分类 —— 新增 DAO 不会悄悄逃出守卫`() {
        val scanned = allDaoNames().distinct().sorted()
        assertTrue("扫描到 0 个 DAO，扫描逻辑失效", scanned.isNotEmpty())
        assertEquals(
            "源码里的 DAO 与已分类列表不一致：新增/删除 DAO 时请更新 daoRequiresSimulatedFilter。" +
                " 扫描到=$scanned，已分类=${daoRequiresSimulatedFilter.keys.sorted()}",
            scanned,
            daoRequiresSimulatedFilter.keys.sorted(),
        )
    }

    @Test
    fun `所有统计查询都排除演示数据`() {
        val queries = allQueries()
        assertTrue("解析到 0 条 @Query，解析逻辑失效", queries.isNotEmpty())

        val violations = queries
            .filter { q ->
                daoRequiresSimulatedFilter[q.dao] == true &&
                    q.name !in allowList &&
                    !q.sql.contains("sensorMode != 'simulated'")
            }
            .map { "${it.dao}.${it.name} → ${it.sql}" }

        assertTrue(
            "以下查询未排除演示数据（AC-19 红线，必须补 sensorMode != 'simulated'）：\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun `除白名单外不允许出现只查模拟数据的查询`() {
        val suspicious = allQueries()
            .filter { it.name !in allowList && it.sql.contains("sensorMode = 'simulated'") }
        assertTrue("演示数据只允许经 simulatedSessions 读取：$suspicious", suspicious.isEmpty())
    }

    @Test
    fun `确实存在足量带过滤的查询 —— 防止过滤条件被整体删除后测试假通过`() {
        val withFilter = allQueries().count { it.sql.contains("sensorMode != 'simulated'") }
        assertTrue("应有多条带过滤的查询，实际只有 $withFilter 条", withFilter >= 8)
    }

    @Test
    fun `带过滤的查询覆盖了所有统计口径`() {
        val names = allQueries().map { it.name }.toSet()
        listOf(
            "latestReal",
            "observeRecentReal",
            "countRealBetween",
            "totalDurationMsRealBetween",
            "realStartTimesBetween",
            "firstRealStartAt",
            "realBetween",
            "averageValidRatioRealBetween",
        ).forEach { required ->
            assertTrue("缺少统计查询 $required", required in names)
        }
    }

    @Test
    fun `质量表的跨会话聚合必须 JOIN 训练表 —— 它自己没有 sensorMode 列`() {
        val q = allQueries().first { it.name == "averageValidRatioRealBetween" }
        assertTrue(
            "质量聚合必须 JOIN training_session 才能过滤演示数据，实际：${q.sql}",
            q.sql.contains("JOIN training_session", ignoreCase = true) &&
                q.sql.contains("simulated"),
        )
    }

    @Test
    fun `统计用时长必须是 durationMs 而不是墙钟差`() {
        val durationQueries = allQueries().filter {
            it.name in setOf("totalDurationMsRealBetween", "realBetween", "countRealBetween")
        }
        assertTrue(
            "统计查询不得出现 endedAt - startedAt（墙钟差会被校时污染）",
            durationQueries.none { it.sql.contains("endedAt - startedAt") },
        )
        val sum = allQueries().first { it.name == "totalDurationMsRealBetween" }
        assertTrue("累计时长应 SUM(durationMs)，实际：${sum.sql}", sum.sql.contains("SUM(durationMs)"))
    }

    @Test
    fun `时长门槛必须真的出现在 SQL 里 —— 不能只写在注释里`() {
        // 评审 D1：首版注释写「仅统计 ≥60s」但 SQL 无条件，属实现与文档不符
        // 注意：SQL 里的门槛是 `durationMs >= 60000` 由常量拼接而成，
        // 提取器只能拿到字符串字面量部分，故匹配 `durationMs >=` 前缀即可。
        val withThreshold = allQueries().count { it.sql.contains("durationMs >=") }
        // 五个口径：打卡计数、累计时长、日历点、Day 起点、七天明细
        assertEquals("上述五类查询都必须带时长门槛", 5, withThreshold)
    }
}