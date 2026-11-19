@file:Suppress("MagicNumber")

package es.igarciadelamo.txoka.ui.components.artwork

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.Xml
import androidx.core.graphics.PathParser
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import kotlin.math.ceil
import kotlin.math.max

/** Only the two-ink, self-contained subset used by the bundled, hash-checked artwork is supported. */
internal object BundledSvgMask {
    private val tags =
        setOf("svg", "title", "desc", "defs", "pattern", "g", "rect", "path", "polygon", "polyline", "circle")
    private val number = Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?\\d*)(?:[eE][-+]?\\d+)?")
    private val transform = Regex("(translate|rotate)\\s*\\(([^)]+)\\)")
    private val clear = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

    fun render(source: String, width: Int, height: Int): Bitmap {
        val document = parse(source)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = max(width / document.width, height / document.height)
        canvas.translate((width - document.width * scale) / 2f, (height - document.height * scale) / 2f)
        canvas.scale(scale, scale)
        val pattern = document.pattern
        if (pattern == null) {
            draw(canvas, document.shapes)
        } else {
            drawPattern(canvas, pattern, document, scale)
        }
        return bitmap.extractAlpha().also { bitmap.recycle() }
    }

    private fun drawPattern(canvas: Canvas, pattern: SvgPattern, document: SvgDocument, scale: Float) {
        val tile = Bitmap.createBitmap(
            ceil(pattern.width * scale).toInt().coerceIn(1, 1024),
            ceil(pattern.height * scale).toInt().coerceIn(1, 1024),
            Bitmap.Config.ARGB_8888,
        )
        val tileCanvas = Canvas(tile)
        tileCanvas.scale(tile.width / pattern.width, tile.height / pattern.height)
        draw(tileCanvas, pattern.shapes)
        val shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        shader.setLocalMatrix(Matrix().apply { setScale(pattern.width / tile.width, pattern.height / tile.height) })
        canvas.drawRect(
            0f,
            0f,
            document.width,
            document.height,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.shader = shader
                isFilterBitmap = true
            },
        )
        tile.recycle()
    }

    private fun draw(canvas: Canvas, shapes: List<SvgShape>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        shapes.forEach { shape ->
            shape.fill?.let { ink ->
                paint.style = Paint.Style.FILL
                paint.xfermode = if (ink) null else clear
                canvas.drawPath(shape.path, paint)
            }
            shape.stroke?.let { ink ->
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = shape.strokeWidth
                paint.strokeCap = shape.cap
                paint.strokeJoin = shape.join
                paint.xfermode = if (ink) null else clear
                canvas.drawPath(shape.path, paint)
            }
        }
    }

    private fun parse(source: String): SvgDocument {
        require(source.length <= MAX_SOURCE_CHARS)
        require(!source.contains("<!DOCTYPE", ignoreCase = true) && !source.contains("<!ENTITY", ignoreCase = true))
        val parser = Xml.newPullParser().apply { setInput(StringReader(source)) }
        val reader = SvgReader()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.DOCDECL -> error("Document declarations are not artwork")
                XmlPullParser.START_TAG -> reader.start(parser)
                XmlPullParser.END_TAG -> reader.end(parser.name)
            }
            parser.nextToken()
        }
        return reader.finish()
    }

    private class SvgReader {
        private val document = SvgDocument()
        private val matrices = mutableListOf(Matrix())
        private var inPattern = false
        private var nodes = 0

        fun start(parser: XmlPullParser) {
            require(++nodes <= MAX_NODES && parser.name in tags)
            val attributes = parser.attributes()
            require(attributes.keys.none { it.startsWith("on") || it.endsWith("href") })
            when (parser.name) {
                "svg" -> document.readSize(attributes)
                "pattern" -> startPattern(attributes)
                "g" -> pushGroup(attributes)
                "rect", "path", "polygon", "polyline", "circle" -> addShape(parser.name, attributes)
            }
        }

        fun end(tag: String) {
            when (tag) {
                "g" -> {
                    require(matrices.size > 1)
                    matrices.removeAt(matrices.lastIndex)
                }
                "pattern" -> inPattern = false
            }
        }

        fun finish(): SvgDocument {
            require(document.width > 0f && document.height > 0f && matrices.size == 1)
            return document
        }

        private fun startPattern(attributes: Map<String, String>) {
            require(!inPattern && document.pattern == null)
            require(attributes["patternUnits"] == "userSpaceOnUse")
            document.pattern = SvgPattern(attributes.value("width"), attributes.value("height"))
            inPattern = true
        }

        private fun pushGroup(attributes: Map<String, String>) {
            val matrix = Matrix(matrices.last())
            attributes["transform"]?.let { matrix.preConcat(readTransform(it)) }
            matrices.add(matrix)
        }

        private fun addShape(tag: String, attributes: Map<String, String>) {
            if (attributes["fill"] == "url(#m)") {
                require(tag == "rect" && !inPattern && document.pattern != null)
            } else {
                val shape = readShape(tag, attributes, matrices.last())
                val shapes = if (inPattern) requireNotNull(document.pattern).shapes else document.shapes
                shapes.add(shape)
            }
        }
    }

    private fun readShape(tag: String, attributes: Map<String, String>, matrix: Matrix): SvgShape {
        val path = shapePath(tag, attributes)
        path.fillType = if (attributes["fill-rule"] == "evenodd") Path.FillType.EVEN_ODD else Path.FillType.WINDING
        path.transform(matrix)
        return SvgShape(
            path = path,
            fill = ink(attributes["fill"] ?: "#111111"),
            stroke = ink(attributes["stroke"] ?: "none"),
            strokeWidth = attributes.value("stroke-width", 1f),
            cap = strokeCap(attributes["stroke-linecap"]),
            join = strokeJoin(attributes["stroke-linejoin"]),
        )
    }

    private fun shapePath(tag: String, attributes: Map<String, String>): Path = when (tag) {
        "path" -> requireNotNull(PathParser.createPathFromPathData(requireNotNull(attributes["d"])))
        "polygon", "polyline" -> pointPath(requireNotNull(attributes["points"]), close = tag == "polygon")
        "rect" -> Path().apply {
            val x = attributes.value("x", 0f)
            val y = attributes.value("y", 0f)
            addRect(x, y, x + attributes.value("width"), y + attributes.value("height"), Path.Direction.CW)
        }
        "circle" -> Path().apply {
            addCircle(
                attributes.value("cx", 0f),
                attributes.value("cy", 0f),
                attributes.value("r"),
                Path.Direction.CW,
            )
        }
        else -> error("Unsupported shape")
    }

    private fun strokeCap(value: String?): Paint.Cap = when (value) {
        "round" -> Paint.Cap.ROUND
        "square" -> Paint.Cap.SQUARE
        else -> Paint.Cap.BUTT
    }

    private fun strokeJoin(value: String?): Paint.Join = when (value) {
        "round" -> Paint.Join.ROUND
        "bevel" -> Paint.Join.BEVEL
        else -> Paint.Join.MITER
    }

    private fun pointPath(points: String, close: Boolean): Path {
        val values = number.findAll(points).map { it.value.toFloat() }.toList()
        require(values.size >= 4 && values.size % 2 == 0 && values.all { it.isFinite() })
        return Path().apply {
            moveTo(values[0], values[1])
            for (index in 2 until values.size step 2) lineTo(values[index], values[index + 1])
            if (close) close()
        }
    }

    private fun readTransform(value: String): Matrix {
        val matches = transform.findAll(value).toList()
        require(value.replace(transform, "").all { it.isWhitespace() || it == ',' })
        return Matrix().apply {
            matches.forEach { match ->
                val values = number.findAll(match.groupValues[2]).map { it.value.toFloat() }.toList()
                require(values.all { it.isFinite() })
                when (match.groupValues[1]) {
                    "translate" -> {
                        require(values.size in 1..2)
                        preTranslate(values[0], values.getOrElse(1) { 0f })
                    }
                    "rotate" -> {
                        require(values.size == 1 || values.size == 3)
                        preRotate(values[0], values.getOrElse(1) { 0f }, values.getOrElse(2) { 0f })
                    }
                }
            }
        }
    }

    private fun ink(value: String): Boolean? = when (value) {
        "#111111" -> true
        "#FAF9F6" -> false
        "none" -> null
        else -> error("Only bundled two-ink colours are supported")
    }

    private fun XmlPullParser.attributes(): Map<String, String> =
        (0 until attributeCount).associate { getAttributeName(it) to getAttributeValue(it) }

    private fun Map<String, String>.value(name: String, default: Float? = null): Float =
        (get(name)?.toFloat() ?: requireNotNull(default)).also { require(it.isFinite()) }

    private data class SvgDocument(
        var width: Float = 0f,
        var height: Float = 0f,
        val shapes: MutableList<SvgShape> = mutableListOf(),
        var pattern: SvgPattern? = null,
    ) {
        fun readSize(attributes: Map<String, String>) {
            val box = requireNotNull(attributes["viewBox"]).split(Regex("[ ,]+")).map(String::toFloat)
            require(box.size == 4 && box[0] == 0f && box[1] == 0f)
            require(box[2] in 1f..8192f && box[3] in 1f..8192f)
            width = box[2]
            height = box[3]
        }
    }

    private data class SvgPattern(
        val width: Float,
        val height: Float,
        val shapes: MutableList<SvgShape> = mutableListOf(),
    )

    private data class SvgShape(
        val path: Path,
        val fill: Boolean?,
        val stroke: Boolean?,
        val strokeWidth: Float,
        val cap: Paint.Cap,
        val join: Paint.Join,
    )

    private const val MAX_SOURCE_CHARS = 4 * 1024 * 1024
    private const val MAX_NODES = 10000
}
