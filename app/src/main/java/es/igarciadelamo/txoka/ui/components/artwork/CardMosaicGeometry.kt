@file:Suppress("MagicNumber")

package es.igarciadelamo.txoka.ui.components.artwork

import es.igarciadelamo.txoka.domain.profile.CardMosaic
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

internal data class MosaicPoint(val x: Float, val y: Float)

internal data class MosaicPolygon(val points: List<MosaicPoint>, val firstInk: Boolean)

internal object CardMosaicGeometry {
    const val WIDTH = 360f

    fun polygons(mosaic: CardMosaic, height: Float): List<MosaicPolygon> {
        if (!height.isFinite() || height <= 0f || height > 16384f) return emptyList()
        return when (mosaic) {
            CardMosaic.TRIANGLES -> triangles(height)
            CardMosaic.SQUARES -> squares(height)
            CardMosaic.CAIRO, CardMosaic.STARS -> cairo(height, mosaic == CardMosaic.STARS)
            CardMosaic.HEXAGONS -> hexagons(height)
            CardMosaic.DOTS -> dots(height)
            else -> emptyList()
        }
    }

    private fun squares(height: Float): List<MosaicPolygon> = buildList {
        val side = 40f
        for (row in 0 until ceil(height / side).toInt()) {
            for (col in 0 until ceil(WIDTH / side).toInt()) {
                val x = col * side
                val y = row * side
                add(polygon(row + col, x to y, x + side to y, x + side to y + side, x to y + side))
            }
        }
    }

    private fun triangles(height: Float): List<MosaicPolygon> = buildList {
        val side = 48f
        val rowHeight = side * sqrt(3f) / 2f
        for (row in 0 until ceil(height / rowHeight).toInt()) {
            val offset = parity(row) * side / 2f
            val y = row * rowHeight
            for (col in -1 until ceil(WIDTH / side).toInt()) {
                val x = col * side + offset
                add(polygon(0, x to y, x + side to y, x + side / 2f to y + rowHeight))
                add(polygon(1, x + side to y, x + side * 1.5f to y + rowHeight, x + side / 2f to y + rowHeight))
            }
        }
    }

    private fun hexagons(height: Float): List<MosaicPolygon> = buildList {
        val radius = 24f
        val rowHeight = sqrt(3f) * radius
        for (col in -1..ceil(WIDTH / (radius * 1.5f)).toInt()) {
            for (row in -1..ceil(height / rowHeight).toInt()) {
                add(
                    MosaicPolygon(
                        regular(col * radius * 1.5f, (row + parity(col) / 2f) * rowHeight, radius, 6),
                        parity(row + col) == 0,
                    ),
                )
            }
        }
    }

    private fun dots(height: Float): List<MosaicPolygon> = buildList {
        val spacing = 40f
        val rowHeight = spacing * sqrt(3f) / 2f
        for (row in -1..ceil(height / rowHeight).toInt()) {
            for (col in -1..ceil(WIDTH / spacing).toInt()) {
                add(
                    MosaicPolygon(
                        regular(col * spacing + parity(row) * spacing / 2f, row * rowHeight, 15.5f, 32),
                        firstInk = false,
                    ),
                )
            }
        }
    }

    private fun cairo(height: Float, star: Boolean): List<MosaicPolygon> = buildList {
        val side = if (star) 28f else 22f
        val root = sqrt(3f)
        val pentagon = listOf(
            MosaicPoint(0f, 0f),
            MosaicPoint(side, 0f),
            MosaicPoint(1.5f * side, root * side / 2f),
            MosaicPoint((3f - root) * side / 2f, (1f + root) * side / 2f),
            MosaicPoint(0f, side),
        )
        val tile = if (star) starContour(pentagon) else pentagon
        val ux = (3f + root) * side / 2f
        val uy = -(3f - root) * side / 2f
        val vx = -uy
        val vy = ux
        val area = ux * ux + uy * uy
        val margin = side * 2f
        val corners = listOf(
            -margin to -margin,
            WIDTH + margin to -margin,
            WIDTH + margin to height + margin,
            -margin to height + margin,
        )
        val rows = corners.map { (x, y) -> (x * vx + y * vy) / area }
        val cols = corners.map { (x, y) -> (x * ux + y * uy) / area }
        for (row in floor(rows.min()).toInt()..ceil(rows.max()).toInt()) {
            for (col in floor(cols.min()).toInt()..ceil(cols.max()).toInt()) {
                val cx = col * ux + row * vx
                val cy = col * uy + row * vy
                for (turn in 0 until 4) {
                    val co = intArrayOf(1, 0, -1, 0)[turn]
                    val si = intArrayOf(0, 1, 0, -1)[turn]
                    add(
                        MosaicPolygon(
                            tile.map { (x, y) -> MosaicPoint(cx + x * co - y * si, cy + x * si + y * co) },
                            firstInk = parity(turn) == 0,
                        ),
                    )
                }
            }
        }
    }

    private fun starContour(pentagon: List<MosaicPoint>): List<MosaicPoint> = buildList {
        val halfRoot = sqrt(3f) / 8f
        val offsets = listOf(
            0f, .125f, halfRoot, .25f, halfRoot, .125f, 0f, -.125f, -halfRoot, -.25f,
            -halfRoot, -.125f,
        )
        pentagon.forEachIndexed { index, (x, y) ->
            val end = pentagon[(index + 1) % pentagon.size]
            val dx = end.x - x
            val dy = end.y - y
            offsets.forEachIndexed { step, offset ->
                val t = step.toFloat() / offsets.size
                add(MosaicPoint(x + t * dx - offset * dy, y + t * dy + offset * dx))
            }
        }
    }

    private fun regular(cx: Float, cy: Float, radius: Float, sides: Int): List<MosaicPoint> = List(sides) { index ->
        val angle = index * PI * 2 / sides
        MosaicPoint(cx + cos(angle).toFloat() * radius, cy + sin(angle).toFloat() * radius)
    }

    private fun polygon(ink: Int, vararg points: Pair<Float, Float>) =
        MosaicPolygon(points.map { (x, y) -> MosaicPoint(x, y) }, parity(ink) == 0)

    private fun parity(value: Int): Int = Math.floorMod(value, 2)
}
