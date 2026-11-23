@file:Suppress("MagicNumber")

package es.igarciadelamo.txoka.ui.components

import kotlin.math.floor

/** Immutable, palette-free surface heights. Only the two local material atlases are retained. */
internal class CardMaterialRelief(values: FloatArray) {
    init {
        require(values.size == SIDE * SIDE && values.all { it.isFinite() && it in 0f..1f })
    }

    private val heights = values.copyOf()

    fun sample(u: Float, v: Float): Float {
        val x = u.coerceIn(0f, 1f) * (SIDE - 1)
        val y = v.coerceIn(0f, 1f) * (SIDE - 1)
        val ix = floor(x).toInt()
        val iy = floor(y).toInt()
        val fx = x - ix
        val fy = y - iy
        val right = minOf(ix + 1, SIDE - 1)
        val bottom = minOf(iy + 1, SIDE - 1)
        val a = heights[iy * SIDE + ix]
        val b = heights[iy * SIDE + right]
        val c = heights[bottom * SIDE + ix]
        val d = heights[bottom * SIDE + right]
        return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy
    }

    companion object {
        const val SIDE = 256

        fun fromBytes(bytes: ByteArray): CardMaterialRelief {
            require(bytes.size == SIDE * SIDE)
            return CardMaterialRelief(FloatArray(bytes.size) { (bytes[it].toInt() and 255) / 255f })
        }
    }
}
