package es.igarciadelamo.txoka.nfc

import android.app.KeyguardManager
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import es.igarciadelamo.txoka.domain.protocol.Type4Tag
import javax.inject.Inject

/**
 * Answers NFC readers with the current card. Android binds this service whenever the device is
 * tapped, even if the app is not running in the foreground.
 */
@AndroidEntryPoint
class NdefHceService : HostApduService() {
    @Inject
    lateinit var responder: HceResponder

    @Inject
    lateinit var keyguardManager: KeyguardManager

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val command = commandApdu ?: return Type4Tag.statusWord(Type4Tag.SW_WRONG_LENGTH)
        return responder.respond(command = command, deviceLocked = keyguardManager.isDeviceLocked)
    }

    override fun onDeactivated(reason: Int) {
        responder.reset()
    }
}
