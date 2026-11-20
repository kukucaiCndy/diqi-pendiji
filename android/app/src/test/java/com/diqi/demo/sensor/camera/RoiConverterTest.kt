package com.diqi.demo.sensor.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Test

/**
 * Golden vector 测试：把 ROI 转换的字节序、长度、采样语义钉死。
 *
 * 为什么必须写：需求 §11 明确「9×9×3 ROI 转换错误 → 始终无指标，
 * 先跑通官方 demo 对齐转换」。而这类错误的特征是**不崩溃、只是永远没有数据**，
 * 最容易被误判成 SDK 问题而排到最晚才被发现。
 */
class RoiConverterTest {

    private fun argb(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun `输出长度恒为 243`() {
        val out = RoiConverter.toRoiBytes(IntArray(81), 9, 9)
        assertEquals(243, out.size)
    }

    @Test
    fun `9x9 源图逐像素精确映射且通道顺序为 RGB 交错`() {
        // 构造 9x9，每个像素唯一：R = x*10, G = y*10, B = 200
        val pixels = IntArray(81)
        for (y in 0 until 9) {
            for (x in 0 until 9) {
                pixels[y * 9 + x] = argb(x * 10, y * 10, 200)
            }
        }

        val out = RoiConverter.toRoiBytes(pixels, 9, 9)

        // 抽查四个角与中心
        fun assertCell(gx: Int, gy: Int) {
            val i = (gy * 9 + gx) * 3
            val r = out[i].toInt() and 0xFF
            val g = out[i + 1].toInt() and 0xFF
            val b = out[i + 2].toInt() and 0xFF
            assertEquals("cell($gx,$gy).R", gx * 10, r)
            assertEquals("cell($gx,$gy).G", gy * 10, g)
            assertEquals("cell($gx,$gy).B", 200, b)
        }
        assertCell(0, 0)
        assertCell(8, 0)
        assertCell(0, 8)
        assertCell(8, 8)
        assertCell(4, 4)
    }

    @Test
    fun `通道不会串位 —— R G B 三通道独立可分辨`() {
        // 纯红图：R=255, G=0, B=0。若字节序写错（BGR），第一个字节会是 0 而不是 255
        val pureRed = IntArray(81) { argb(255, 0, 0) }
        val out = RoiConverter.toRoiBytes(pureRed, 9, 9)
        assertEquals(243, out.size)
        for (i in 0 until 243 step 3) {
            assertEquals("第 ${i} 字节应为 R=255", 255, out[i].toInt() and 0xFF)
            assertEquals("第 ${i + 1} 字节应为 G=0", 0, out[i + 1].toInt() and 0xFF)
            assertEquals("第 ${i + 2} 字节应为 B=0", 0, out[i + 2].toInt() and 0xFF)
        }
    }

    @Test
    fun `纯蓝图不会退化成纯红 —— 防止 R B 通道互换`() {
        val pureBlue = IntArray(81) { argb(0, 0, 255) }
        val out = RoiConverter.toRoiBytes(pureBlue, 9, 9)
        for (i in 0 until 243 step 3) {
            assertEquals(0, out[i].toInt() and 0xFF)
            assertEquals(0, out[i + 1].toInt() and 0xFF)
            assertEquals(255, out[i + 2].toInt() and 0xFF)
        }
    }

    @Test
    fun `18x18 源图按 2x2 平均后与等价 9x9 结果一致`() {
        val nineGrid = IntArray(81) { i ->
            val x = i % 9
            val y = i / 9
            argb(x * 7, y * 5, 128)
        }
        val expected = RoiConverter.toRoiBytes(nineGrid, 9, 9)

        // 把每个像素扩成 2x2 同色块
        val eighteen = IntArray(18 * 18)
        for (y in 0 until 18) {
            for (x in 0 until 18) {
                eighteen[y * 18 + x] = nineGrid[(y / 2) * 9 + (x / 2)]
            }
        }
        val actual = RoiConverter.toRoiBytes(eighteen, 18, 18)

        assertArrayEquals(expected, actual)
    }

    @Test
    fun `窄源图不会越界且每格均被采样`() {
        // 5x5 小于 9 格，floor/ceil 策略必须保证每格至少 1 像素
        val pixels = IntArray(25) { argb(10, 20, 30) }
        val out = RoiConverter.toRoiBytes(pixels, 5, 5)
        assertEquals(243, out.size)
        for (i in 0 until 243 step 3) {
            assertEquals(10, out[i].toInt() and 0xFF)
            assertEquals(20, out[i + 1].toInt() and 0xFF)
            assertEquals(30, out[i + 2].toInt() and 0xFF)
        }
    }

    @Test
    fun `非 9 的倍数源图也能稳定输出 243 字节`() {
        val out = RoiConverter.toRoiBytes(IntArray(37 * 41) { argb(1, 2, 3) }, 37, 41)
        assertEquals(243, out.size)
    }

    @Test
    fun `全黑与全白分别映射到 0 与 255`() {
        val black = RoiConverter.toRoiBytes(IntArray(81) { argb(0, 0, 0) }, 9, 9)
        val white = RoiConverter.toRoiBytes(IntArray(81) { argb(255, 255, 255) }, 9, 9)
        assertEquals(0, black[0].toInt() and 0xFF)
        assertEquals(255, white[0].toInt() and 0xFF)
        assertEquals(255, white[242].toInt() and 0xFF)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `像素数组不足时抛异常而不是静默产出脏数据`() {
        RoiConverter.toRoiBytes(IntArray(10), 9, 9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `尺寸为 0 时抛异常`() {
        RoiConverter.toRoiBytes(IntArray(0), 0, 0)
    }

    @Test
    fun `与 facephys 契约的常量一致`() {
        // 若有人改了 RoiConverter.BYTE_COUNT 却没改 SDK 契约，这里会失败
        assertEquals(com.facephys.rppg.RppgEngine.ROI_BYTE_COUNT, RoiConverter.BYTE_COUNT)
    }
}