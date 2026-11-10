package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.ShareRole
import es.igarciadelamo.txoka.domain.profile.ShareSettings
import es.igarciadelamo.txoka.domain.protocol.Type4Tag
import es.igarciadelamo.txoka.testing.FakeProfileRepository
import es.igarciadelamo.txoka.testing.FakeShareSettingsRepository
import es.igarciadelamo.txoka.testing.NfcTestPackets.SelectAid
import es.igarciadelamo.txoka.testing.NfcTestPackets.status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HceCardPublisherTest {
    private val profile = Profile(name = "Isidro", phone = "+34600123456")

    @Test
    fun `publishes the card while sending`() = runTest {
        val responder = HceResponder()
        publisher(profile, ShareSettings(), responder).publishCurrent()

        assertEquals(Type4Tag.SW_OK, status(responder.respond(SelectAid, deviceLocked = false)))
    }

    @Test
    fun `publishes nothing while receiving`() = runTest {
        val responder = HceResponder()
        publisher(profile, ShareSettings(), responder, role = ShareRole.Receive).publishCurrent()

        assertEquals(Type4Tag.SW_FILE_NOT_FOUND, status(responder.respond(SelectAid, deviceLocked = false)))
    }

    @Test
    fun `publishes nothing without a profile`() = runTest {
        val responder = HceResponder()
        publisher(
            profile = null,
            settings = ShareSettings(),
            responder = responder,
        ).publishCurrent()

        assertEquals(Type4Tag.SW_FILE_NOT_FOUND, status(responder.respond(SelectAid, deviceLocked = false)))
    }

    @Test
    fun `forwards the unlock requirement`() = runTest {
        val responder = HceResponder()
        publisher(profile, ShareSettings(requireDeviceUnlock = true), responder).publishCurrent()

        assertEquals(Type4Tag.SW_CONDITIONS_NOT_SATISFIED, status(responder.respond(SelectAid, deviceLocked = true)))
        assertEquals(Type4Tag.SW_OK, status(responder.respond(SelectAid, deviceLocked = false)))
    }

    @Test
    fun `start follows the share role`() = runTest {
        val roles = ShareRoleController(backgroundScope)
        val responder = HceResponder()
        HceCardPublisher(
            profileRepository = FakeProfileRepository(profile),
            settingsRepository = FakeShareSettingsRepository(),
            roles = roles,
            responder = responder,
            scope = backgroundScope,
        ).start()
        runCurrent()
        assertEquals(Type4Tag.SW_OK, status(responder.respond(SelectAid, deviceLocked = false)))

        roles.select(ShareRole.Receive)
        runCurrent()

        assertEquals(Type4Tag.SW_FILE_NOT_FOUND, status(responder.respond(SelectAid, deviceLocked = false)))
    }

    @Test
    fun `publishes nothing when the card does not fit`() = runTest {
        val responder = HceResponder()
        publisher(Profile(name = "A".repeat(900)), ShareSettings(), responder).publishCurrent()

        assertEquals(Type4Tag.SW_FILE_NOT_FOUND, status(responder.respond(SelectAid, deviceLocked = false)))
    }

    private fun publisher(
        profile: Profile?,
        settings: ShareSettings,
        responder: HceResponder,
        role: ShareRole = ShareRole.Send,
    ) = HceCardPublisher(
        profileRepository = FakeProfileRepository(profile),
        settingsRepository = FakeShareSettingsRepository(settings),
        roles = ShareRoleController(CoroutineScope(Dispatchers.Unconfined)).apply { select(role) },
        responder = responder,
        scope = CoroutineScope(Dispatchers.Unconfined),
    )
}
