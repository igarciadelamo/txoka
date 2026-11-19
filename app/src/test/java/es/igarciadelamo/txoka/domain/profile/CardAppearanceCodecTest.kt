package es.igarciadelamo.txoka.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Base64

class CardAppearanceCodecTest {
    private val firstCorner = RgbColor(1, 2, 3)
    private val secondCorner = RgbColor(4, 5, 6)
    private val nameColor = RgbColor(7, 8, 9)
    private val dataColor = RgbColor(10, 11, 12)

    @Test
    fun `matches the fourteen independent contract goldens`() {
        val independentText = CardAppearance(
            name = CardTextStyle(TextFinish.EMBOSSED, TextSize.S, RgbColor(0x12, 0x34, 0x56)),
            data = CardTextStyle(TextFinish.RAISED, TextSize.XL),
        )
        val ordinaryMaximum = CardAppearance(
            mosaic = CardMosaic.HEXAGONS,
            effect = CardEffect.FIRE,
            topRight = firstCorner,
            bottomLeft = secondCorner,
            name = CardTextStyle(color = nameColor),
            data = CardTextStyle(TextFinish.BUBBLE, color = dataColor),
        )
        val extendedMaximum = ordinaryMaximum.copy(
            background = CardBackground.HOLOGRAM,
            mosaic = CardMosaic.GEO_OPTICAL_WOVEN_GRID,
            name = CardTextStyle(TextFinish.METAL, TextSize.XL, nameColor),
            data = CardTextStyle(TextFinish.RAISED, TextSize.XL, dataColor),
        )
        val goldens = listOf(
            "AGgB" to CardAppearance(),
            "BmgB" to CardAppearance(background = CardBackground.FLAT),
            "FmgB" to CardAppearance(background = CardBackground.FLAT, mosaic = CardMosaic.TRIANGLES),
            "cWgBoA" to CardAppearance(background = CardBackground.HOLOGRAM, mosaic = CardMosaic.NATURE_FISH),
            "cGgFoQ" to CardAppearance(mosaic = CardMosaic.NATURE_BIRDS, effect = CardEffect.WATER),
            "dmgNog" to CardAppearance(
                background = CardBackground.FLAT,
                mosaic = CardMosaic.NATURE_REPTILES,
                effect = CardEffect.WIND,
            ),
            "cWgJwA" to CardAppearance(
                background = CardBackground.HOLOGRAM,
                mosaic = CardMosaic.GEO_TRUCHET_LABYRINTH,
                effect = CardEffect.EARTH,
            ),
            "dmgR0w" to CardAppearance(
                background = CardBackground.FLAT,
                mosaic = CardMosaic.GEO_OPTICAL_WOVEN_GRID,
                effect = CardEffect.FIRE,
            ),
            "ABEjEjRW" to independentText,
            "WGhxAQIDBAUGBwgJCgsM" to ordinaryMaximum,
            "edRz0wECAwQFBgcICQoLDA" to extendedMaximum,
            "AG0B" to CardAppearance(name = CardTextStyle(TextFinish.BUBBLE)),
            "AEUhEjRW" to CardAppearance(
                name = CardTextStyle(TextFinish.BUBBLE, color = RgbColor(0x12, 0x34, 0x56)),
                data = CardTextStyle(TextFinish.PLAIN),
            ),
            "edVz0wECAwQFBgcICQoLDA" to extendedMaximum.copy(
                name = extendedMaximum.name.copy(finish = TextFinish.BUBBLE),
            ),
        )
        goldens.forEach { (encoded, appearance) ->
            assertEquals(appearance, CardAppearanceCodec.decode("1:$encoded"))
            assertEquals("1:$encoded", CardAppearanceCodec.encode(appearance))
        }
    }

    @Test
    fun `only the exact twenty three escaped selectors are supported unsigned`() {
        val known = setOf(160, 161, 162) + (192..211)
        (0..255).forEach { selector ->
            val decoded = CardAppearanceCodec.decode(encoded(0x70, 0x68, 0x01, selector))
            assertEquals("selector $selector", selector in known, decoded != null)
            if (decoded != null) assertEquals(selector, decoded.mosaic.wireId)
        }
        assertEquals(30, CardMosaic.entries.size)
        assertEquals(30, CardMosaic.entries.map { it.wireId }.toSet().size)
        assertEquals(23, CardMosaic.entries.count { it.assetId != null })
    }

    @Test
    fun `rejects unknown versions noncanonical base64 and invalid lengths`() {
        listOf(
            "", "1:", "0:AGgB", "2:AGgB", "01:AGgB", " 1:AGgB", "1:AGgB ",
            "1:AGgB=", "1:AGgB==", "1:AGgB\n", "1:AGg+", "1:AGg/", "1:AGgB!", "1:AGg", "1:AAAAA",
            "1:cWgBoB", // Same decoded bytes as cWgBoA, but nonzero unused Base64 bits.
            "1:${"A".repeat(23)}",
        ).forEach { assertNull(it, CardAppearanceCodec.decode(it)) }
        val maximum = intArrayOf(0x79, 0xD4, 0x73, 0xD3, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        (0 until maximum.size).forEach { size ->
            assertNull("truncated at $size", CardAppearanceCodec.decode(encoded(*maximum.copyOf(size))))
        }
        assertNull(CardAppearanceCodec.decode(encoded(*maximum, 0)))
        assertNull(CardAppearanceCodec.decode(encoded(0, 0x68, 1, 0)))
    }

    @Test
    fun `rejects every reserved field and flat four corner flag`() {
        listOf(2, 3, 4, 5, 7, 0x80).forEach { assertNull(CardAppearanceCodec.decode(encoded(it, 0x68, 1))) }
        listOf(6, 7).forEach { finish ->
            assertNull(CardAppearanceCodec.decode(encoded(0, 0x68 or finish, 1)))
            assertNull(CardAppearanceCodec.decode(encoded(0, 0x40 or (finish shl 3), 1)))
        }
        listOf(5, 6, 7).forEach { effect ->
            assertNull(CardAppearanceCodec.decode(encoded(0, 0x68, 1 or (effect shl 2))))
        }
        assertNull(CardAppearanceCodec.decode(encoded(0, 0x68, 0x81)))
        assertNull(CardAppearanceCodec.decode(encoded(0x0E, 0x68, 1, 1, 2, 3, 4, 5, 6)))
    }

    @Test
    fun `omits default text RGBs and rejects their explicit noncanonical encoding`() {
        TextFinish.entries.forEach { finish ->
            val default = finish.defaultColor
            val explicit = CardAppearance(name = CardTextStyle(finish = finish, color = default))
            val absent = explicit.copy(name = explicit.name.copy(color = null))
            assertEquals(CardAppearanceCodec.encode(absent), CardAppearanceCodec.encode(explicit))
            assertEquals(absent, CardAppearanceCodec.decode(CardAppearanceCodec.encode(explicit)))
            assertNull(
                CardAppearanceCodec.decode(
                    encoded(0, 0x68 or finish.wireId, 0x21, default.red, default.green, default.blue),
                ),
            )
            assertNull(
                CardAppearanceCodec.decode(
                    encoded(0, 0x40 or (finish.wireId shl 3), 0x41, default.red, default.green, default.blue),
                ),
            )
        }
    }

    @Test
    fun `refuses incomplete corner colours and corners on flat backgrounds`() {
        assertThrows(IllegalArgumentException::class.java) {
            CardAppearanceCodec.encode(CardAppearance(topRight = firstCorner))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CardAppearanceCodec.encode(CardAppearance(bottomLeft = secondCorner))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CardAppearanceCodec.encode(
                CardAppearance(background = CardBackground.FLAT, topRight = firstCorner, bottomLeft = secondCorner),
            )
        }
    }

    @Test
    @Suppress("NestedBlockDepth")
    fun `round trips all 1728000 flag combinations and matches independent length distribution`() {
        val histogram = sortedMapOf<Int, Int>()
        val backgrounds = listOf(
            CardBackground.GRADIENT to false,
            CardBackground.GRADIENT to true,
            CardBackground.HOLOGRAM to false,
            CardBackground.HOLOGRAM to true,
            CardBackground.FLAT to false,
        )
        for ((background, corners) in backgrounds) {
            for (mosaic in CardMosaic.entries) {
                for (effect in CardEffect.entries) {
                    for (nameFinish in TextFinish.entries) {
                        for (dataFinish in TextFinish.entries) {
                            verifyTextVariants(
                                CardAppearance(
                                    background,
                                    mosaic,
                                    effect,
                                    topRight = if (corners) firstCorner else null,
                                    bottomLeft = if (corners) secondCorner else null,
                                ),
                                nameFinish,
                                dataFinish,
                                histogram,
                            )
                        }
                    }
                }
            }
        }
        assertEquals(1_728_000, histogram.values.sum())
        assertEquals(
            sortedMapOf(
                3 to 60_480, 4 to 198_720, 6 to 120_960, 7 to 397_440, 9 to 100_800,
                10 to 331_200, 12 to 80_640, 13 to 264_960, 15 to 40_320, 16 to 132_480,
            ),
            histogram,
        )
        assertTrue(histogram.keys.max() <= 16)
    }

    @Suppress("NestedBlockDepth")
    private fun verifyTextVariants(
        base: CardAppearance,
        nameFinish: TextFinish,
        dataFinish: TextFinish,
        histogram: MutableMap<Int, Int>,
    ) {
        for (nameSize in TextSize.entries) {
            for (dataSize in TextSize.entries) {
                for (flags in 0..3) {
                    val appearance = base.copy(
                        name = CardTextStyle(nameFinish, nameSize, if (flags and 1 != 0) nameColor else null),
                        data = CardTextStyle(dataFinish, dataSize, if (flags and 2 != 0) dataColor else null),
                    )
                    val encoded = CardAppearanceCodec.encode(appearance)
                    val decoded = CardAppearanceCodec.decode(encoded)
                    if (appearance != decoded) fail("Round trip failed for $appearance ($encoded): $decoded")
                    val rawLength = Base64.getUrlDecoder().decode(encoded.removePrefix("1:")).size
                    histogram[rawLength] = histogram.getOrDefault(rawLength, 0) + 1
                }
            }
        }
    }

    private fun encoded(vararg values: Int): String = "1:" + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(ByteArray(values.size) { values[it].toByte() })
}
