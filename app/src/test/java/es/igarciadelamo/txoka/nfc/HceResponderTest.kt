package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.protocol.NdefMessage
import es.igarciadelamo.txoka.domain.protocol.Type4Tag
import org.junit.Assert.assertEquals
import org.junit.Test

class HceResponderTest {
    private val vCard = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Ana\r\nEND:VCARD\r\n"
    private val responder = HceResponder()

    @Test
    fun `answers file not found while no card is published`() {
        assertEquals(Type4Tag.SW_FILE_NOT_FOUND, status(responder.respond(selectAid(), deviceLocked = false)))
    }

    @Test
    fun `serves the published card`() {
        responder.update(card())

        assertEquals(Type4Tag.SW_OK, status(responder.respond(selectAid(), deviceLocked = false)))
        assertEquals(Type4Tag.SW_OK, status(responder.respond(selectFile(0xE104), deviceLocked = false)))

        val lengthBytes = responder.respond(readBinary(offset = 0), deviceLocked = false).dropLast(2)
        val length = ((lengthBytes[0].toInt() and 0xFF) shl 8) or (lengthBytes[1].toInt() and 0xFF)
        val body = responder.respond(readBinary(offset = 2, length = length), deviceLocked = false).dropLast(2)

        assertEquals(vCard, NdefMessage.decode(body.toByteArray()))
    }

    @Test
    fun `keeps the selection between commands`() {
        responder.update(card())
        responder.respond(selectAid(), deviceLocked = false)
        responder.respond(selectFile(0xE104), deviceLocked = false)

        val response = responder.respond(readBinary(offset = 0), deviceLocked = false)

        assertEquals(Type4Tag.SW_OK, status(response))
    }

    @Test
    fun `refuses to answer while locked when unlock is required`() {
        responder.update(card(requireUnlock = true))

        assertEquals(Type4Tag.SW_CONDITIONS_NOT_SATISFIED, status(responder.respond(selectAid(), deviceLocked = true)))
        assertEquals(Type4Tag.SW_OK, status(responder.respond(selectAid(), deviceLocked = false)))
    }

    @Test
    fun `stops answering when sharing is turned off`() {
        responder.update(card())
        responder.update(null)

        assertEquals(Type4Tag.SW_FILE_NOT_FOUND, status(responder.respond(selectAid(), deviceLocked = false)))
    }

    @Test
    fun `drops the selection after deactivation`() {
        responder.update(card())
        responder.respond(selectAid(), deviceLocked = false)
        responder.respond(selectFile(0xE104), deviceLocked = false)

        responder.reset()

        assertEquals(
            Type4Tag.SW_COMMAND_NOT_ALLOWED,
            status(responder.respond(readBinary(offset = 0), deviceLocked = false)),
        )
    }

    @Test
    fun `rebuilds the tag when the payload changes`() {
        responder.update(card())
        responder.respond(selectAid(), deviceLocked = false)
        responder.respond(selectFile(0xE104), deviceLocked = false)

        responder.update(HceCard(NdefMessage.encode(vCard.replace("Ana", "Marta")), requireUnlock = false))

        assertEquals(
            Type4Tag.SW_COMMAND_NOT_ALLOWED,
            status(responder.respond(readBinary(offset = 0), deviceLocked = false)),
        )
        assertEquals(Type4Tag.SW_OK, status(responder.respond(selectAid(), deviceLocked = false)))
    }

    private fun card(requireUnlock: Boolean = false) = HceCard(NdefMessage.encode(vCard), requireUnlock)

    private fun selectAid(): ByteArray = command(0x00, 0xA4, 0x04, 0x00, 0x07, 0xD2, 0x76, 0x00, 0x00, 0x85, 0x01, 0x01)

    private fun selectFile(fileId: Int): ByteArray =
        command(0x00, 0xA4, 0x00, 0x0C, 0x02, fileId ushr 8, fileId and 0xFF)

    private fun readBinary(offset: Int, length: Int = 2): ByteArray =
        command(0x00, 0xB0, offset shr 8, offset and 0xFF, length)

    private fun command(vararg values: Int): ByteArray = ByteArray(values.size) { index -> values[index].toByte() }

    private fun status(response: List<Byte>): Int =
        ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response.last().toInt() and 0xFF)

    private fun status(response: ByteArray): Int =
        ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response.last().toInt() and 0xFF)
}
