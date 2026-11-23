@file:Suppress("MagicNumber")

package es.igarciadelamo.txoka.ui.components

/** Labels belong to the approved RGB panes and only choose their optical orientation. */
internal class CardMaterialCells private constructor(private val labels: ByteArray) {
    fun raster(width: Int, height: Int): IntArray {
        require(width in 1..480 && height in 1..320)
        return IntArray(width * height) { index ->
            val x = ((index % width + .5f) * WIDTH / width).toInt().coerceIn(0, WIDTH - 1)
            val y = ((index / width + .5f) * HEIGHT / height).toInt().coerceIn(0, HEIGHT - 1)
            labels[y * WIDTH + x].toInt() and 255
        }
    }

    companion object {
        private const val WIDTH = 480
        private const val HEIGHT = 303
        const val CELL_COUNT = 24

        fun fromBytes(bytes: ByteArray): CardMaterialCells {
            require(bytes.size == WIDTH * HEIGHT)
            val present = BooleanArray(CELL_COUNT)
            bytes.forEach { byte ->
                val label = byte.toInt() and 255
                require(label < CELL_COUNT)
                present[label] = true
            }
            require(present.all { it })
            return CardMaterialCells(bytes.copyOf())
        }
    }
}
