package es.igarciadelamo.txoka.domain.profile

import java.io.ByteArrayOutputStream
import java.util.Base64

/** Canonical STYLE v1, shared by NFC, DataStore and Room. Unknown data falls back atomically. */
@Suppress("MagicNumber")
object CardAppearanceCodec {
    private const val PREFIX = "1:"
    private const val FIXED_BYTES = 3
    private const val MAX_BYTES = 16
    private const val BYTE_MASK = 0xFF
    private const val MOSAIC_ESCAPE = 7
    private const val FOUR_CORNERS = 0x08
    private const val NAME_RGB = 0x20
    private const val DATA_RGB = 0x40
    private const val RESERVED = 0x80

    private val EncodedPattern = Regex("[A-Za-z0-9_-]{4,22}")
    private val Encoder = Base64.getUrlEncoder().withoutPadding()
    private val Decoder = Base64.getUrlDecoder()

    fun encode(appearance: CardAppearance): String {
        require((appearance.topRight == null) == (appearance.bottomLeft == null)) {
            "Four-corner colour requires both additional corners"
        }
        require(appearance.background != CardBackground.FLAT || !appearance.hasFourCorners) {
            "Flat backgrounds cannot carry four corners"
        }
        val style = appearance.canonical()
        val mosaic = if (style.mosaic.assetId == null) style.mosaic.wireId else MOSAIC_ESCAPE
        val bytes = ByteArrayOutputStream(MAX_BYTES).apply {
            write(style.background.wireId or (if (style.hasFourCorners) FOUR_CORNERS else 0) or (mosaic shl 4))
            write(style.name.finish.wireId or (style.data.finish.wireId shl 3) or (style.name.size.wireId shl 6))
            write(
                style.data.size.wireId or (style.effect.wireId shl 2) or
                    (if (style.name.color != null) NAME_RGB else 0) or
                    (if (style.data.color != null) DATA_RGB else 0),
            )
            if (mosaic == MOSAIC_ESCAPE) write(style.mosaic.wireId)
            style.topRight?.let { writeColor(it) }
            style.bottomLeft?.let { writeColor(it) }
            style.name.color?.let { writeColor(it) }
            style.data.color?.let { writeColor(it) }
        }.toByteArray()
        return PREFIX + Encoder.encodeToString(bytes)
    }

    @Suppress("ReturnCount")
    fun decode(value: String): CardAppearance? {
        if (!value.startsWith(PREFIX)) return null
        val encoded = value.removePrefix(PREFIX)
        if (!EncodedPattern.matches(encoded)) return null
        val bytes = decodeBytes(encoded) ?: return null
        if (bytes.size !in FIXED_BYTES..MAX_BYTES) return null
        val header = readHeader(bytes) ?: return null
        if (bytes.size != header.expectedLength) return null
        return readAppearance(bytes, header)
    }

    private fun decodeBytes(value: String): ByteArray? = try {
        Decoder.decode(value).takeIf { Encoder.encodeToString(it) == value }
    } catch (_: IllegalArgumentException) {
        null
    }

    @Suppress("ReturnCount")
    private fun readHeader(bytes: ByteArray): Header? {
        val first = bytes[0].toInt() and BYTE_MASK
        val second = bytes[1].toInt() and BYTE_MASK
        val third = bytes[2].toInt() and BYTE_MASK
        if (first and RESERVED != 0 || third and RESERVED != 0) return null
        val background = CardBackground.entries.find { it.wireId == (first and 7) } ?: return null
        val nameFinish = TextFinish.entries.find { it.wireId == (second and 7) } ?: return null
        val dataFinish = TextFinish.entries.find { it.wireId == ((second shr 3) and 7) } ?: return null
        val effect = CardEffect.entries.find { it.wireId == ((third shr 2) and 7) } ?: return null
        val fourCorners = first and FOUR_CORNERS != 0
        if (background == CardBackground.FLAT && fourCorners) return null
        return Header(
            background = background,
            mosaic = (first shr 4) and 7,
            fourCorners = fourCorners,
            name = CardTextStyle(nameFinish, TextSize.entries.first { it.wireId == (second ushr 6) }),
            data = CardTextStyle(dataFinish, TextSize.entries.first { it.wireId == (third and 3) }),
            effect = effect,
            hasNameColor = third and NAME_RGB != 0,
            hasDataColor = third and DATA_RGB != 0,
        )
    }

    private fun readAppearance(bytes: ByteArray, header: Header): CardAppearance? {
        val cursor = Cursor(bytes)
        val mosaicId = if (header.mosaic == MOSAIC_ESCAPE) cursor.byte() else header.mosaic
        val mosaic = CardMosaic.entries.find { it.wireId == mosaicId } ?: return null
        if (header.mosaic == MOSAIC_ESCAPE && mosaic.assetId == null) return null
        val topRight = if (header.fourCorners) cursor.color() else null
        val bottomLeft = if (header.fourCorners) cursor.color() else null
        val nameColor = if (header.hasNameColor) cursor.color() else null
        val dataColor = if (header.hasDataColor) cursor.color() else null
        if (nameColor == header.name.finish.defaultColor || dataColor == header.data.finish.defaultColor) return null
        return CardAppearance(
            background = header.background,
            mosaic = mosaic,
            effect = header.effect,
            name = header.name.copy(color = nameColor),
            data = header.data.copy(color = dataColor),
            topRight = topRight,
            bottomLeft = bottomLeft,
        )
    }

    private fun ByteArrayOutputStream.writeColor(color: RgbColor) {
        write(color.red)
        write(color.green)
        write(color.blue)
    }

    private class Cursor(private val bytes: ByteArray) {
        private var index = FIXED_BYTES

        fun byte(): Int = bytes[index++].toInt() and BYTE_MASK

        fun color(): RgbColor = RgbColor(byte(), byte(), byte())
    }

    private data class Header(
        val background: CardBackground,
        val mosaic: Int,
        val fourCorners: Boolean,
        val name: CardTextStyle,
        val data: CardTextStyle,
        val effect: CardEffect,
        val hasNameColor: Boolean,
        val hasDataColor: Boolean,
    ) {
        val expectedLength: Int
            get() = FIXED_BYTES + (if (mosaic == MOSAIC_ESCAPE) 1 else 0) +
                (if (fourCorners) 6 else 0) + (if (hasNameColor) 3 else 0) + (if (hasDataColor) 3 else 0)
    }
}
