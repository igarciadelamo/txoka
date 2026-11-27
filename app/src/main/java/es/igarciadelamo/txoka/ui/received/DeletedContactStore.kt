package es.igarciadelamo.txoka.ui.received

import es.igarciadelamo.txoka.domain.received.ReceivedContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the last card removed from the detail screen until the library offers the undo. It lives
 * outside the detail ViewModel because that ViewModel is gone by the time the snackbar appears.
 */
@Singleton
class DeletedContactStore @Inject constructor() {
    private val _contact = MutableStateFlow<ReceivedContact?>(null)
    val contact: StateFlow<ReceivedContact?> = _contact.asStateFlow()

    fun remember(contact: ReceivedContact) {
        _contact.value = contact
    }

    fun clear() {
        _contact.value = null
    }
}
