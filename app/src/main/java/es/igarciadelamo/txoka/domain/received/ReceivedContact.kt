package es.igarciadelamo.txoka.domain.received

import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import kotlinx.coroutines.flow.Flow

/**
 * A card the user accepted, with the notes they can edit afterwards.
 */
data class ReceivedContact(
    val id: Long,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val linkedin: String? = null,
    val instagram: String? = null,
    val note: String = "",
    val receivedAt: Long = 0L,
    val card: CardPalette = CardPalette.Default,
)

sealed interface SaveResult {
    data class Saved(val id: Long) : SaveResult

    data class Updated(val id: Long) : SaveResult
}

interface ReceivedContactRepository {
    val contacts: Flow<List<ReceivedContact>>

    fun contact(id: Long): Flow<ReceivedContact?>

    /** The stored contact the card belongs to, matching on phone, email or a social handle. */
    suspend fun match(payload: VCardPayload): ReceivedContact?

    /**
     * Stores the card, or refreshes the matching contact: the sender's card wins and the local
     * note is kept.
     */
    suspend fun save(payload: VCardPayload): SaveResult

    suspend fun updateNote(id: Long, note: String)

    suspend fun delete(id: Long)
}
