package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.di.ApplicationScope
import es.igarciadelamo.txoka.domain.profile.ShareRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The role picked for this session, plus whether the phone is currently offering a return card.
 */
data class ShareRoleState(val role: ShareRole = ShareRole.Send, val returning: Boolean = false)

/**
 * Keeps the share role in memory and drives the one-shot return offer.
 *
 * A tap only travels one way, so after receiving a card the phone offers its own for a short window.
 * Only a phone that was receiving arms the offer and the offer is consumed by the next card it gets,
 * which is what stops two phones from bouncing cards forever.
 */
@Singleton
class ShareRoleController @Inject constructor(@ApplicationScope private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(ShareRoleState())
    val state: StateFlow<ShareRoleState> = _state.asStateFlow()

    private var window: Job? = null

    fun select(role: ShareRole) {
        window?.cancel()
        window = null
        _state.value = ShareRoleState(role = role)
    }

    /** Called when a card has just been read from another phone. */
    fun onCardReceived() {
        val current = _state.value
        when {
            current.returning -> endReturn()
            current.role == ShareRole.Receive -> startReturn()
        }
    }

    fun cancelReturn() = endReturn()

    private fun startReturn() {
        _state.value = ShareRoleState(role = ShareRole.Send, returning = true)
        window = scope.launch {
            delay(RETURN_WINDOW_MS)
            _state.value = ShareRoleState(role = ShareRole.Receive)
            window = null
        }
    }

    private fun endReturn() {
        window?.cancel()
        window = null
        if (_state.value.returning) {
            _state.value = ShareRoleState(role = ShareRole.Receive)
        }
    }

    private companion object {
        /** Long enough to line the phones up again, short enough to not drift into a tap later. */
        const val RETURN_WINDOW_MS = 60_000L
    }
}
