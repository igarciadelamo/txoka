package es.igarciadelamo.txoka.domain.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class Type4TagTest {
    private val vCard = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Ana\r\nEND:VCARD\r\n"
    private val tag = Type4Tag(NdefMessage.encode(vCard))

    @Test
    fun `reads the capability container and the ndef message`() {
        assertEquals(Type4Tag.SW_OK, status(tag.process(selectAid())))
        assertEquals(Type4Tag.SW_OK, status(tag.process(selectFile(CC_FILE))))
        assertEquals(Type4Tag.SW_OK, status(tag.process(readBinary(offset = 0, length = 2))))

        val header = data(tag.process(readBinary(offset = 0, length = 2)))
        assertArrayEquals(byteArrayOf(0x00, 0x0F), header)

        val cc = data(tag.process(readBinary(offset = 0, length = 15)))
        assertEquals(15, cc.size)
        assertArrayEquals(
            byteArrayOf(
                0x00, 0x0F, 0x20, 0x00, 0x3B, 0x00, 0x34, 0x04,
                0x06, 0xE1.toByte(), 0x04, 0x03, 0x02, 0x00, 0x00,
            ),
            cc,
        )

        assertEquals(Type4Tag.SW_OK, status(tag.process(selectFile(NDEF_FILE))))
        val lengthBytes = data(tag.process(readBinary(offset = 0, length = 2)))
        val length = ((lengthBytes[0].toInt() and 0xFF) shl 8) or (lengthBytes[1].toInt() and 0xFF)
        val body = data(tag.process(readBinary(offset = 2, length = length)))

        assertEquals(NdefMessage.encode(vCard).size, length)
        assertEquals(vCard, NdefMessage.decode(body))
    }

    @Test
    fun `rejects an unknown aid`() {
        val response = tag.process(
            command(0x00, 0xA4, 0x04, 0x00, 0x02, 0x11, 0x22),
        )

        assertEquals(Type4Tag.SW_FILE_NOT_FOUND, status(response))
    }

    @Test
    fun `accepts a custom aid`() {
        val customAid = byteArrayOf(0xF0.toByte(), 0x54, 0x58, 0x4F, 0x4B, 0x41)
        val customTag = Type4Tag(NdefMessage.encode(vCard), acceptedAids = listOf(customAid))
        val select = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, customAid.size.toByte()) + customAid

        assertEquals(Type4Tag.SW_OK, status(customTag.process(select)))
        assertEquals(Type4Tag.SW_FILE_NOT_FOUND, status(customTag.process(selectAid())))
    }

    @Test
    fun `rejects reading without a selected file`() {
        tag.process(selectAid())

        assertEquals(Type4Tag.SW_COMMAND_NOT_ALLOWED, status(tag.process(readBinary(offset = 0, length = 2))))
    }

    @Test
    fun `rejects unknown files and instructions`() {
        tag.process(selectAid())

        assertEquals(Type4Tag.SW_FILE_NOT_FOUND, status(tag.process(selectFile(0xE1 to 0x05))))
        assertEquals(Type4Tag.SW_INS_NOT_SUPPORTED, status(tag.process(command(0x00, 0xD6, 0x00, 0x00, 0x01))))
        assertEquals(Type4Tag.SW_CLA_NOT_SUPPORTED, status(tag.process(command(0x80, 0xB0, 0x00, 0x00, 0x02))))
    }

    @Test
    fun `rejects offsets outside the file`() {
        tag.process(selectAid())
        tag.process(selectFile(CC_FILE))

        assertEquals(Type4Tag.SW_OFFSET_OUT_OF_RANGE, status(tag.process(readBinary(offset = 16, length = 2))))
    }

    @Test
    fun `sends 256 bytes when le is zero`() {
        val longCard = "BEGIN:VCARD\r\nFN:${"A".repeat(600)}\r\nEND:VCARD\r\n"
        val longTag = Type4Tag(NdefMessage.encode(longCard))
        longTag.process(selectAid())
        longTag.process(selectFile(NDEF_FILE))

        val response = data(longTag.process(command(0x00, 0xB0, 0x00, 0x00, 0x00)))

        assertEquals(256, response.size)
    }

    @Test
    fun `refuses messages above the limit`() {
        assertThrows(IllegalArgumentException::class.java) {
            Type4Tag(ByteArray(NdefMessage.MAX_MESSAGE_BYTES + 1))
        }
    }

    private fun selectAid(): ByteArray = command(0x00, 0xA4, 0x04, 0x00, 0x07, 0xD2, 0x76, 0x00, 0x00, 0x85, 0x01, 0x01)

    private fun selectFile(fileId: Pair<Int, Int>): ByteArray =
        command(0x00, 0xA4, 0x00, 0x0C, 0x02, fileId.first, fileId.second)

    private fun readBinary(offset: Int, length: Int): ByteArray =
        command(0x00, 0xB0, offset shr 8, offset and 0xFF, length)

    private fun command(vararg values: Int): ByteArray = ByteArray(values.size) { index -> values[index].toByte() }

    private fun status(response: ByteArray): Int =
        ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response.last().toInt() and 0xFF)

    private fun data(response: ByteArray): ByteArray = response.copyOfRange(0, response.size - 2)

    private companion object {
        val CC_FILE = 0xE1 to 0x03
        val NDEF_FILE = 0xE1 to 0x04
    }
}
