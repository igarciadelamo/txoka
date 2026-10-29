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
    val note: String = "",
    val receivedAt: Long = 0L,
    val card: CardPalette = CardPalette.Default,
)

sealed interface SaveResult {
    data class Saved(val id: Long) : SaveResult

    data class Duplicate(val id: Long) : SaveResult
}

interface ReceivedContactRepository {
    val contacts: Flow<List<ReceivedContact>>

    fun contact(id: Long): Flow<ReceivedContact?>

    /** Stores the card, or reports the existing contact whose phone or email already matches. */
    suspend fun save(payload: VCardPayload): SaveResult

    suspend fun updateNote(id: Long, note: String)

    suspend fun delete(id: Long)
}
