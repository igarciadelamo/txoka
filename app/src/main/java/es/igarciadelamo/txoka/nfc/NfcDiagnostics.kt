package es.igarciadelamo.txoka.nfc

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The last NFC intents the app was handed. Debug builds show them so a phone that never receives a
 * tag can be told apart from one that receives it and drops it.
 */
@Singleton
class NfcDiagnostics @Inject constructor() {
    private val _events = MutableStateFlow<List<String>>(emptyList())
    val events: StateFlow<List<String>> = _events.asStateFlow()

    fun record(event: String) {
        _events.update { current -> (current + event).takeLast(MAX_EVENTS) }
    }

    private companion object {
        const val MAX_EVENTS = 3
    }
}
