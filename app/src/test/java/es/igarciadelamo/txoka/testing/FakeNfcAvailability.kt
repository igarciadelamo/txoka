package es.igarciadelamo.txoka.testing

import es.igarciadelamo.txoka.nfc.NfcAvailability
import es.igarciadelamo.txoka.nfc.NfcStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNfcAvailability(status: NfcStatus = NfcStatus.Enabled) : NfcAvailability {
    private val states = MutableStateFlow(status)

    var status: NfcStatus
        get() = states.value
        set(value) {
            states.value = value
        }

    override fun status(): NfcStatus = states.value

    override fun stateChanges(): Flow<NfcStatus> = states
}
