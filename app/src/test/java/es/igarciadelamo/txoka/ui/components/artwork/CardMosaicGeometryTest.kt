package es.igarciadelamo.txoka.ui.components.artwork

import es.igarciadelamo.txoka.domain.profile.CardMosaic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardMosaicGeometryTest {
    @Test
    fun `polygonal mosaics cover the card without background gaps or overlapping faces`() {
        val mosaics =
            listOf(CardMosaic.TRIANGLES, CardMosaic.SQUARES, CardMosaic.CAIRO, CardMosaic.STARS, CardMosaic.HEXAGONS)
        mosaics.forEach { mosaic ->
            val polygons = CardMosaicGeometry.polygons(mosaic, 227f)
            for (x in 7..353 step 13) {
                for (y in 9..221 step 11) {
                    val point = MosaicPoint(x + .173f, y + .291f)
                    val coverage = polygons.count { contains(it.points, point) }
                    assertEquals("$mosaic at $point", 1, coverage)
                }
            }
        }
    }

    @Test
    fun `dots are large round motifs and deliberately leave the other ink visible`() {
        val polygons = CardMosaicGeometry.polygons(CardMosaic.DOTS, 227f)
        assertTrue(polygons.all { !it.firstInk && it.points.size == 32 })
        polygons.forEach { shape ->
            assertEquals(31f, shape.points.maxOf { it.x } - shape.points.minOf { it.x }, .001f)
        }
        assertTrue(polygons.none { contains(it.points, MosaicPoint(20f, 17f)) })
    }

    @Test
    fun `five arm star contours are quarter turn copies of the same prototile`() {
        val stars = CardMosaicGeometry.polygons(CardMosaic.STARS, 227f)
        assertTrue(stars.isNotEmpty())
        assertTrue(stars.all { it.points.size == 60 })
        val expectedPerimeter = perimeter(stars.first().points)
        stars.forEach { assertEquals(expectedPerimeter, perimeter(it.points), .01) }
    }

    @Test
    fun `invalid dimensions and nonprocedural selectors never create unbounded geometry`() {
        listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, 20000f).forEach { height ->
            assertTrue(CardMosaicGeometry.polygons(CardMosaic.STARS, height).isEmpty())
        }
        assertTrue(CardMosaicGeometry.polygons(CardMosaic.NATURE_FISH, 227f).isEmpty())
        assertTrue(CardMosaicGeometry.polygons(CardMosaic.NONE, 227f).isEmpty())
    }

    private fun contains(polygon: List<MosaicPoint>, point: MosaicPoint): Boolean {
        var inside = false
        var previous = polygon.last()
        polygon.forEach { current ->
            if ((current.y > point.y) != (previous.y > point.y) &&
                point.x < (previous.x - current.x) * (point.y - current.y) / (previous.y - current.y) + current.x
            ) {
                inside = !inside
            }
            previous = current
        }
        return inside
    }

    private fun perimeter(points: List<MosaicPoint>): Double = points.indices.sumOf { index ->
        val first = points[index]
        val second = points[(index + 1) % points.size]
        kotlin.math.hypot((first.x - second.x).toDouble(), (first.y - second.y).toDouble())
    }
}
