package es.igarciadelamo.txoka.ui.received

import es.igarciadelamo.txoka.domain.received.ReceivedContact
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReceivedListItemsTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 11, 28)

    @Test
    fun `puts today's cards under one header and older ones under the next`() {
        val fresh = contact(1, "Ana", today)
        val old = contact(2, "Marta", today.minusDays(3))

        assertEquals(
            listOf(
                ReceivedListItem.Header(ReceivedGroup.Today),
                ReceivedListItem.Card(fresh),
                ReceivedListItem.Header(ReceivedGroup.Earlier),
                ReceivedListItem.Card(old),
            ),
            listOf(fresh, old).toLibraryItems(today, zone),
        )
    }

    @Test
    fun `leaves out headers with no cards`() {
        val old = contact(1, "Marta", today.minusDays(1))

        assertEquals(
            listOf(
                ReceivedListItem.Header(ReceivedGroup.Earlier),
                ReceivedListItem.Card(old),
            ),
            listOf(old).toLibraryItems(today, zone),
        )
    }

    @Test
    fun `keeps the order the repository returned`() {
        val first = contact(1, "Ana", today)
        val second = contact(2, "Luis", today)

        assertEquals(
            listOf(
                ReceivedListItem.Header(ReceivedGroup.Today),
                ReceivedListItem.Card(first),
                ReceivedListItem.Card(second),
            ),
            listOf(first, second).toLibraryItems(today, zone),
        )
    }

    @Test
    fun `returns nothing for an empty library`() {
        assertEquals(
            emptyList<ReceivedListItem>(),
            emptyList<ReceivedContact>().toLibraryItems(today, zone),
        )
    }

    private fun contact(id: Long, name: String, date: LocalDate) = ReceivedContact(
        id = id,
        name = name,
        receivedAt = date.atStartOfDay(zone).toInstant().toEpochMilli(),
    )
}
