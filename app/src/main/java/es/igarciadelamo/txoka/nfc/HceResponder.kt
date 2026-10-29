package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.protocol.Type4Tag

/**
 * The card currently advertised in front of a reader, if any.
 */
data class HceCard(val ndef: ByteArray, val requireUnlock: Boolean)

/**
 * Bridges the HCE service with the current card. Selection state has to survive between APDU
 * commands, so the Type 4 Tag instance is kept until the payload changes.
 */
class HceResponder {
    private var tag: Type4Tag? = null
    private var card: HceCard? = null

    @Synchronized
    fun update(newCard: HceCard?) {
        val unchanged = card?.ndef?.contentEquals(newCard?.ndef ?: ByteArray(0)) == true &&
            card?.requireUnlock == newCard?.requireUnlock
        card = newCard
        if (!unchanged || newCard == null) {
            tag = newCard?.let { current -> Type4Tag(current.ndef) }
        }
    }

    @Synchronized
    fun reset() {
        tag = card?.let { current -> Type4Tag(current.ndef) }
    }

    @Synchronized
    fun respond(command: ByteArray, deviceLocked: Boolean): ByteArray {
        val current = tag ?: return Type4Tag.statusWord(Type4Tag.SW_FILE_NOT_FOUND)
        if (card?.requireUnlock == true && deviceLocked) {
            return Type4Tag.statusWord(Type4Tag.SW_CONDITIONS_NOT_SATISFIED)
        }
        return current.process(command)
    }
}
