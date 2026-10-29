package es.igarciadelamo.txoka.domain.protocol

/**
 * Emulates the reader side of a NFC Forum Type 4 Tag over APDU: capability container, NDEF file
 * and the SELECT / READ BINARY command subset needed to read a single NDEF message.
 *
 * Pure Kotlin on purpose, so every sequence can be replayed in unit tests.
 */
class Type4Tag(private val ndefMessage: ByteArray, acceptedAids: List<ByteArray> = listOf(NDEF_AID)) {
    init {
        require(ndefMessage.size <= NdefMessage.MAX_MESSAGE_BYTES) {
            "NDEF message of ${ndefMessage.size} bytes exceeds the limit of ${NdefMessage.MAX_MESSAGE_BYTES}"
        }
    }

    private val aids = acceptedAids.map { aid -> aid.copyOf() }
    private val capabilityContainer = buildCapabilityContainer(NDEF_FILE_MAX_SIZE)
    private val ndefFile = byteArrayOf(
        (ndefMessage.size ushr BITS_PER_BYTE).toByte(),
        ndefMessage.size.toByte(),
    ) + ndefMessage

    private var selectedFile: FileType? = null

    fun process(command: ByteArray): ByteArray {
        if (command.size < COMMAND_HEADER_BYTES) return status(SW_WRONG_LENGTH)

        val cla = command[CLA].toInt() and BYTE_MASK
        val ins = command[INS].toInt() and BYTE_MASK
        val p1 = command[P1].toInt() and BYTE_MASK
        val p2 = command[P2].toInt() and BYTE_MASK

        if (cla != CLA_ISO) return status(SW_CLA_NOT_SUPPORTED)

        return when (ins) {
            INS_SELECT -> select(p1, p2, command)
            INS_READ_BINARY -> readBinary(p1, p2, command)
            else -> status(SW_INS_NOT_SUPPORTED)
        }
    }

    private fun select(p1: Int, p2: Int, command: ByteArray): ByteArray {
        val data = dataOf(command) ?: return status(SW_WRONG_LENGTH)

        return when (p1) {
            SELECT_BY_NAME -> selectAid(data)
            SELECT_BY_ID -> selectFile(p2, data)
            else -> status(SW_INCORRECT_P1_P2)
        }
    }

    private fun selectAid(data: ByteArray): ByteArray {
        val known = aids.any { aid -> aid.contentEquals(data) }
        return if (known) {
            selectedFile = null
            status(SW_OK)
        } else {
            status(SW_FILE_NOT_FOUND)
        }
    }

    private fun selectFile(p2: Int, data: ByteArray): ByteArray {
        if (p2 != SELECT_P2 && p2 != SELECT_P2_ALTERNATE) return status(SW_INCORRECT_P1_P2)
        if (data.size != FILE_ID_BYTES) return status(SW_WRONG_LENGTH)

        val file = FileType.entries.firstOrNull { type -> type.matches(data) }
            ?: return status(SW_FILE_NOT_FOUND)
        selectedFile = file
        return status(SW_OK)
    }

    private fun readBinary(p1: Int, p2: Int, command: ByteArray): ByteArray {
        val file = selectedFile ?: return status(SW_COMMAND_NOT_ALLOWED)
        val offset = (p1 shl BITS_PER_BYTE) or p2
        val content = file.content(capabilityContainer, ndefFile)
        if (offset > content.size) return status(SW_OFFSET_OUT_OF_RANGE)

        val requested = if (command.size > LENGTH_INDEX) command[LENGTH_INDEX].toInt() and BYTE_MASK else 0
        val length = if (requested == 0) MAX_LE_BYTES else requested
        val end = minOf(offset + length, content.size)

        return content.copyOfRange(offset, end) + status(SW_OK)
    }

    private fun dataOf(command: ByteArray): ByteArray? {
        if (command.size <= LENGTH_INDEX) return null
        val length = command[LENGTH_INDEX].toInt() and BYTE_MASK
        val start = LENGTH_INDEX + 1
        if (command.size < start + length) return null
        return command.copyOfRange(start, start + length)
    }

    private fun status(sw: Int): ByteArray = byteArrayOf((sw ushr BITS_PER_BYTE).toByte(), sw.toByte())

    private enum class FileType(private val id: Int) {
        CapabilityContainer(CC_FILE_ID),
        Ndef(NDEF_FILE_ID),
        ;

        fun matches(data: ByteArray): Boolean {
            if (data.size != FILE_ID_BYTES) return false
            val fileId = ((data[0].toInt() and BYTE_MASK) shl BITS_PER_BYTE) or (data[1].toInt() and BYTE_MASK)
            return fileId == id
        }

        fun content(capabilityContainer: ByteArray, ndefFile: ByteArray): ByteArray =
            if (this == CapabilityContainer) capabilityContainer else ndefFile
    }

    companion object {
        val NDEF_AID = byteArrayOf(0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01)

        const val SW_OK = 0x9000
        const val SW_FILE_NOT_FOUND = 0x6A82
        const val SW_INCORRECT_P1_P2 = 0x6A86
        const val SW_WRONG_LENGTH = 0x6700
        const val SW_COMMAND_NOT_ALLOWED = 0x6986
        const val SW_INS_NOT_SUPPORTED = 0x6D00
        const val SW_CLA_NOT_SUPPORTED = 0x6E00
        const val SW_OFFSET_OUT_OF_RANGE = 0x6B00

        private const val CLA_ISO = 0x00
        private const val INS_SELECT = 0xA4
        private const val INS_READ_BINARY = 0xB0
        private const val SELECT_BY_NAME = 0x04
        private const val SELECT_BY_ID = 0x00
        private const val SELECT_P2 = 0x0C
        private const val SELECT_P2_ALTERNATE = 0x00
        private const val CC_FILE_ID = 0xE103
        private const val NDEF_FILE_ID = 0xE104
        private const val FILE_ID_BYTES = 2
        private const val COMMAND_HEADER_BYTES = 4
        private const val LENGTH_INDEX = 4
        private const val CLA = 0
        private const val INS = 1
        private const val P1 = 2
        private const val P2 = 3
        private const val BYTE_MASK = 0xFF
        private const val BITS_PER_BYTE = 8
        private const val MAX_LE_BYTES = 256
        private const val NLEN_BYTES = 2
        private const val NDEF_FILE_MAX_SIZE = NLEN_BYTES + NdefMessage.MAX_MESSAGE_BYTES

        private const val CC_VERSION_2_0 = 0x20
        private const val MLE_HIGH = 0x00
        private const val MLE_LOW = 0x3B
        private const val MLC_HIGH = 0x00
        private const val MLC_LOW = 0x34
        private const val NDEF_FILE_CONTROL_TLV = 0x04
        private const val NDEF_FILE_CONTROL_LENGTH = 0x06
        private const val ACCESS_GRANTED = 0x00

        private fun buildCapabilityContainer(maxNdefFileSize: Int): ByteArray = byteArrayOf(
            0x00, 0x0F,
            CC_VERSION_2_0.toByte(),
            MLE_HIGH.toByte(), MLE_LOW.toByte(),
            MLC_HIGH.toByte(), MLC_LOW.toByte(),
            NDEF_FILE_CONTROL_TLV.toByte(), NDEF_FILE_CONTROL_LENGTH.toByte(),
            (NDEF_FILE_ID ushr BITS_PER_BYTE).toByte(), (NDEF_FILE_ID and BYTE_MASK).toByte(),
            (maxNdefFileSize ushr BITS_PER_BYTE).toByte(), maxNdefFileSize.toByte(),
            ACCESS_GRANTED.toByte(), ACCESS_GRANTED.toByte(),
        )
    }
}
