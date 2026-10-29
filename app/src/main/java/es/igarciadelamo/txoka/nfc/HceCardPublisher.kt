package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.di.ApplicationScope
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.ProfileRepository
import es.igarciadelamo.txoka.domain.profile.ShareSettings
import es.igarciadelamo.txoka.domain.profile.ShareSettingsRepository
import es.igarciadelamo.txoka.domain.protocol.NdefMessage
import es.igarciadelamo.txoka.domain.protocol.VCardSerializer
import es.igarciadelamo.txoka.domain.protocol.toVCardPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the advertised card in sync with the stored profile and sharing settings, so the HCE
 * service can answer even when the app is not in the foreground.
 */
@Singleton
class HceCardPublisher @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: ShareSettingsRepository,
    private val responder: HceResponder,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            combine(profileRepository.profile, settingsRepository.settings) { profile, settings ->
                cardFor(profile, settings)
            }.collect { card -> responder.update(card) }
        }
    }

    /** One-shot refresh, also used by tests to avoid depending on the collection timing. */
    suspend fun publishCurrent() {
        responder.update(
            cardFor(
                profile = profileRepository.profile.first(),
                settings = settingsRepository.settings.first(),
            ),
        )
    }

    internal fun cardFor(profile: Profile?, settings: ShareSettings): HceCard? {
        if (profile == null || !settings.sharingEnabled) return null
        val vCard = VCardSerializer.encode(profile.toVCardPayload())
        val ndef = runCatching { NdefMessage.encode(vCard) }.getOrNull() ?: return null
        return HceCard(ndef = ndef, requireUnlock = settings.requireDeviceUnlock)
    }
}
