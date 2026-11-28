package es.igarciadelamo.txoka.ui.received

import es.igarciadelamo.txoka.domain.received.ReceivedContact
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Headers of the library, in the order they are drawn. */
enum class ReceivedGroup { Today, Earlier }

/** One row of the library: either a group header or a card. */
sealed interface ReceivedListItem {
    data class Header(val group: ReceivedGroup) : ReceivedListItem

    data class Card(val contact: ReceivedContact) : ReceivedListItem
}

/**
 * Flattens the filtered library into rows so the list can be lazy: one header per non-empty group
 * and the cards in the order the repository returned them.
 */
fun List<ReceivedContact>.toLibraryItems(today: LocalDate, zone: ZoneId): List<ReceivedListItem> =
    ReceivedGroup.entries.flatMap { group ->
        val cards = filter { contact -> contact.group(today, zone) == group }
        if (cards.isEmpty()) emptyList() else listOf(ReceivedListItem.Header(group)) + cards.map(ReceivedListItem::Card)
    }

private fun ReceivedContact.group(today: LocalDate, zone: ZoneId): ReceivedGroup =
    if (Instant.ofEpochMilli(receivedAt).atZone(zone).toLocalDate() == today) {
        ReceivedGroup.Today
    } else {
        ReceivedGroup.Earlier
    }
