package es.igarciadelamo.txoka

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Guards the Spanish translation against drift: every user-facing string must stay in sync. */
class StringResourcesTest {
    private val default = strings("values")
    private val spanish = strings("values-es")

    @Test
    fun `every translatable string has a Spanish translation`() {
        val missing = default.filterValues { it.translatable }.keys - spanish.keys
        assertTrue("Missing Spanish strings: $missing", missing.isEmpty())
    }

    @Test
    fun `Spanish has no strings that the default locale does not`() {
        val extra = spanish.keys - default.keys
        assertTrue("Strings only in values-es: $extra", extra.isEmpty())
    }

    @Test
    fun `format placeholders match`() {
        val mismatched = default.keys.intersect(spanish.keys).filter { name ->
            default.getValue(name).placeholders != spanish.getValue(name).placeholders
        }
        assertEquals(emptyList<String>(), mismatched)
    }

    @Test
    fun `plural quantities cover the default locale`() {
        val missing = default.filterValues { it.plurals }
            .mapValues { (_, entry) -> entry.quantities - spanish.getValue(entry.name).quantities }
            .filterValues { it.isNotEmpty() }
        assertTrue("Missing plural quantities: $missing", missing.isEmpty())
    }

    private data class Entry(
        val name: String,
        val translatable: Boolean,
        val placeholders: Set<String>,
        val plurals: Boolean = false,
        val quantities: Set<String> = emptySet(),
    )

    private fun strings(locale: String): Map<String, Entry> {
        val candidates = listOf(
            File("src/main/res/$locale/strings.xml"),
            File("app/src/main/res/$locale/strings.xml"),
        )
        val file = candidates.firstOrNull { it.exists() } ?: error("Cannot find $locale strings.xml")
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        val entries = mutableMapOf<String, Entry>()

        for (index in 0 until root.childNodes.length) {
            val element = root.childNodes.item(index) as? Element ?: continue
            val name = element.getAttribute("name")
            when (element.tagName) {
                "string" -> entries[name] = Entry(
                    name = name,
                    translatable = element.getAttribute("translatable") != "false",
                    placeholders = element.placeholders(),
                )

                "plurals" -> {
                    val quantities = mutableSetOf<String>()
                    val placeholders = mutableSetOf<String>()
                    val items = element.getElementsByTagName("item")
                    for (itemIndex in 0 until items.length) {
                        val item = items.item(itemIndex) as Element
                        quantities += item.getAttribute("quantity")
                        placeholders += item.placeholders()
                    }
                    entries[name] = Entry(
                        name = name,
                        translatable = true,
                        placeholders = placeholders,
                        plurals = true,
                        quantities = quantities,
                    )
                }
            }
        }
        return entries
    }

    private fun Element.placeholders(): Set<String> = PLACEHOLDER.findAll(textContent).map { it.value }.toSet()

    private companion object {
        val PLACEHOLDER = Regex("""%\d*\$?[sd]""")
    }
}
