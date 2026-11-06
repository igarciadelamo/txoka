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
            tag = newCard?.let(::tagFor)
        }
    }

    @Synchronized
    fun reset() {
        tag = card?.let(::tagFor)
    }

    @Synchronized
    fun respond(command: ByteArray, deviceLocked: Boolean): ByteArray {
        val current = tag ?: return Type4Tag.statusWord(Type4Tag.SW_FILE_NOT_FOUND)
        if (card?.requireUnlock == true && deviceLocked) {
            return Type4Tag.statusWord(Type4Tag.SW_CONDITIONS_NOT_SATISFIED)
        }
        return current.process(command)
    }

    private fun tagFor(card: HceCard): Type4Tag = Type4Tag(card.ndef, ACCEPTED_AIDS)

    private companion object {
        /** The NDEF Forum AID plus the Txoka one declared in `xml/apduservice.xml`. */
        val ACCEPTED_AIDS = listOf(
            Type4Tag.NDEF_AID,
            byteArrayOf(0xF0.toByte(), 0x54, 0x58, 0x4F, 0x4B, 0x41, 0x01),
        )
    }
}
