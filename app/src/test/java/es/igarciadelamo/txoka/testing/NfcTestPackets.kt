package es.igarciadelamo.txoka.testing

/** APDUs and status word helpers shared by the NFC tests. */
object NfcTestPackets {
    /** `SELECT AID` for the NFC Forum NDEF application. */
    val SelectAid = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
        0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01,
    )

    fun status(response: ByteArray): Int =
        ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response.last().toInt() and 0xFF)
}
