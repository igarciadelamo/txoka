package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.domain.profile.ShareSettings
import es.igarciadelamo.txoka.domain.protocol.Type4Tag
import es.igarciadelamo.txoka.testing.FakeProfileRepository
import es.igarciadelamo.txoka.testing.FakeShareSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HceCardPublisherTest {
    private val profile = Profile(name = "Isidro", phone = "+34600123456")

    @Test
    fun `publishes the card when profile and sharing are on`() = runTest {
        val responder = HceResponder()
        publisher(profile, ShareSettings(sharingEnabled = true), responder).publishCurrent()

        assertEquals(Type4Tag.SW_OK, status(responder.respond(selectAid(), deviceLocked = false)))
    }

    @Test
    fun `publishes nothing when sharing is off`() = runTest {
        val responder = HceResponder()
        publisher(profile, ShareSettings(sharingEnabled = false), responder).publishCurrent()

        assertEquals(Type4Tag.SW_FILE_NOT_FOUND, status(responder.respond(selectAid(), deviceLocked = false)))
    }

    @Test
    fun `publishes nothing without a profile`() = runTest {
        val responder = HceResponder()
        publisher(
            profile = null,
            settings = ShareSettings(sharingEnabled = true),
            responder = responder,
        ).publishCurrent()

        assertEquals(Type4Tag.SW_FILE_NOT_FOUND, status(responder.respond(selectAid(), deviceLocked = false)))
    }

    @Test
    fun `forwards the unlock requirement`() = runTest {
        val responder = HceResponder()
        publisher(profile, ShareSettings(sharingEnabled = true, requireDeviceUnlock = true), responder).publishCurrent()

        assertEquals(Type4Tag.SW_CONDITIONS_NOT_SATISFIED, status(responder.respond(selectAid(), deviceLocked = true)))
        assertEquals(Type4Tag.SW_OK, status(responder.respond(selectAid(), deviceLocked = false)))
    }

    @Test
    fun `publishes nothing when the card does not fit`() = runTest {
        val responder = HceResponder()
        publisher(Profile(name = "A".repeat(900)), ShareSettings(sharingEnabled = true), responder).publishCurrent()

        assertEquals(Type4Tag.SW_FILE_NOT_FOUND, status(responder.respond(selectAid(), deviceLocked = false)))
    }

    private fun publisher(profile: Profile?, settings: ShareSettings, responder: HceResponder) = HceCardPublisher(
        profileRepository = FakeProfileRepository(profile),
        settingsRepository = FakeShareSettingsRepository(settings),
        responder = responder,
        scope = CoroutineScope(Dispatchers.Unconfined),
    )

    private fun selectAid(): ByteArray =
        byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x07, 0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01)

    private fun status(response: ByteArray): Int =
        ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response.last().toInt() and 0xFF)
}
