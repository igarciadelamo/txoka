package es.igarciadelamo.txoka.ui.components.artwork

import es.igarciadelamo.txoka.domain.profile.CardMosaic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory

class CardArtworkCatalogTest {
    private val directory = listOf(File("src/main/assets/card-artwork"), File("app/src/main/assets/card-artwork"))
        .first(File::isDirectory)

    @Test
    fun `every extended NFC selector resolves to exactly one bundled vector`() {
        val extended = CardMosaic.entries.filter { it.assetId != null }.map { it.wireId }.toSet()
        assertEquals(23, extended.size)
        assertEquals(extended, CardArtworkCatalog.entries.map { it.wireId }.toSet())
        assertEquals(extended.size, CardArtworkCatalog.entries.size)
        assertEquals(CardArtworkCatalog.entries.size, CardArtworkCatalog.entries.map { it.file }.toSet().size)
    }

    @Test
    fun `approved drawings and supplied geometries retain their exact source bytes`() {
        CardArtworkCatalog.entries.forEach { artwork ->
            val file = File(directory, artwork.file)
            assertTrue("Missing ${artwork.file}", file.isFile)
            val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                .joinToString("") { "%02x".format(it) }
            assertEquals(artwork.file, artwork.sha256, digest)
        }
        val packaged = directory.walkTopDown().filter { it.isFile && it.extension == "svg" }
            .map { it.relativeTo(directory).invariantSeparatorsPath }.toSet()
        assertEquals(CardArtworkCatalog.entries.map { it.file }.toSet(), packaged)
        assertFalse(directory.walkTopDown().any { it.isFile && it.extension in setOf("png", "jpg", "webp") })
    }

    @Test
    fun `all sources stay within the local two ink SVG subset without executable or external content`() {
        val parser = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }.newDocumentBuilder()
        CardArtworkCatalog.entries.forEach { artwork ->
            val root = parser.parse(File(directory, artwork.file)).documentElement
            val elements = root.getElementsByTagName("*")
            for (index in 0 until elements.length) {
                assertSupported(elements.item(index) as Element, artwork.file)
            }
        }
    }

    @Test
    fun `nature artwork remains vector paths with interior detail`() {
        CardArtworkCatalog.entries.filter { it.wireId in 160..162 }.forEach { artwork ->
            val source = File(directory, artwork.file).readText()
            assertTrue("Missing detailed path ${artwork.file}", source.length > 100_000)
            assertTrue(source.contains("fill-rule=\"evenodd\""))
            assertFalse(source.contains("<image"))
        }
    }

    private fun assertSupported(element: Element, file: String) {
        val allowed =
            setOf("svg", "title", "desc", "defs", "pattern", "g", "rect", "path", "polygon", "polyline", "circle")
        assertTrue("Unsupported ${element.tagName} in $file", element.tagName in allowed)
        for (attribute in 0 until element.attributes.length) {
            val node = element.attributes.item(attribute)
            assertFalse(node.nodeName.startsWith("on") || node.nodeName.endsWith("href"))
            if (node.nodeName in setOf("fill", "stroke")) {
                assertTrue(node.nodeValue in setOf("#111111", "#FAF9F6", "none", "url(#m)"))
            }
        }
    }
}
