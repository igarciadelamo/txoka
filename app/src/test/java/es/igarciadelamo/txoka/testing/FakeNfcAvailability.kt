package es.igarciadelamo.txoka.testing

import es.igarciadelamo.txoka.nfc.NfcAvailability
import es.igarciadelamo.txoka.nfc.NfcStatus

class FakeNfcAvailability(var status: NfcStatus = NfcStatus.Enabled) : NfcAvailability {
    override fun status(): NfcStatus = status
}
