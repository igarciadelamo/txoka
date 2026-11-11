package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.di.ApplicationScope
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.ProfileRepository
import es.igarciadelamo.txoka.domain.profile.ShareRole
import es.igarciadelamo.txoka.domain.profile.ShareSettings
import es.igarciadelamo.txoka.domain.profile.ShareSettingsRepository
import es.igarciadelamo.txoka.domain.protocol.NdefMessage
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.domain.protocol.VCardSerializer
import es.igarciadelamo.txoka.domain.protocol.toVCardPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the advertised card in sync with the stored profile, the sharing settings and the session
 * role, so the HCE service can answer even when the app is not in the foreground.
 */
@Singleton
class HceCardPublisher @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: ShareSettingsRepository,
    private val roles: ShareRoleController,
    private val responder: HceResponder,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            combine(
                profileRepository.profile,
                settingsRepository.settings,
                // The return offer ticks every second; only the role decides what gets published.
                roles.state.map { it.role }.distinctUntilChanged(),
            ) { profile, settings, role -> cardFor(profile, settings, role) }
                .collect { card -> responder.update(card) }
        }
    }

    /** Publishes an explicit payload, used by the transport contract and by the demo mode. */
    suspend fun publish(payload: VCardPayload?) {
        val settings = settingsRepository.settings.first()
        responder.update(
            payload?.let { card ->
                HceCard(
                    ndef = NdefMessage.encode(VCardSerializer.encode(card)),
                    requireUnlock = settings.requireDeviceUnlock,
                )
            },
        )
    }

    /** One-shot refresh, also used by tests to avoid depending on the collection timing. */
    suspend fun publishCurrent() {
        responder.update(
            cardFor(
                profile = profileRepository.profile.first(),
                settings = settingsRepository.settings.first(),
                role = roles.state.first().role,
            ),
        )
    }

    internal fun cardFor(profile: Profile?, settings: ShareSettings, role: ShareRole): HceCard? {
        if (profile == null || role == ShareRole.Receive) return null
        val vCard = VCardSerializer.encode(profile.toVCardPayload())
        val ndef = runCatching { NdefMessage.encode(vCard) }.getOrNull() ?: return null
        return HceCard(ndef = ndef, requireUnlock = settings.requireDeviceUnlock)
    }
}
