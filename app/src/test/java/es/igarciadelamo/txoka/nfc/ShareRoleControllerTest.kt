package es.igarciadelamo.txoka.nfc

import es.igarciadelamo.txoka.domain.profile.ShareRole
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareRoleControllerTest {
    @Test
    fun `starts sending`() = runTest {
        val controller = ShareRoleController(backgroundScope)

        assertEquals(ShareRole.Send, controller.state.value.role)
        assertFalse(controller.state.value.returning)
    }

    @Test
    fun `switches the role`() = runTest {
        val controller = ShareRoleController(backgroundScope)

        controller.select(ShareRole.Receive)

        assertEquals(ShareRole.Receive, controller.state.value.role)
        assertFalse(controller.state.value.returning)
    }

    @Test
    fun `arms a return after receiving`() = runTest {
        val controller = ShareRoleController(backgroundScope)
        controller.select(ShareRole.Receive)

        controller.onCardReceived()

        assertEquals(ShareRole.Send, controller.state.value.role)
        assertTrue(controller.state.value.returning)
    }

    @Test
    fun `does not arm a return while sending`() = runTest {
        val controller = ShareRoleController(backgroundScope)

        controller.onCardReceived()

        assertEquals(ShareRole.Send, controller.state.value.role)
        assertFalse(controller.state.value.returning)
    }

    @Test
    fun `expires the return window`() = runTest {
        val controller = ShareRoleController(backgroundScope)
        controller.select(ShareRole.Receive)
        controller.onCardReceived()

        advanceTimeBy(RETURN_WINDOW + 1)
        runCurrent()

        assertEquals(ShareRole.Receive, controller.state.value.role)
        assertFalse(controller.state.value.returning)
    }

    @Test
    fun `cancelling the return goes back to receiving`() = runTest {
        val controller = ShareRoleController(backgroundScope)
        controller.select(ShareRole.Receive)
        controller.onCardReceived()

        controller.cancelReturn()

        assertEquals(ShareRole.Receive, controller.state.value.role)
        assertFalse(controller.state.value.returning)
    }

    @Test
    fun `a card arriving while returning ends the offer`() = runTest {
        val controller = ShareRoleController(backgroundScope)
        controller.select(ShareRole.Receive)
        controller.onCardReceived()

        controller.onCardReceived()

        assertEquals(ShareRole.Receive, controller.state.value.role)
        assertFalse(controller.state.value.returning)
    }

    @Test
    fun `choosing a role during the window keeps it`() = runTest {
        val controller = ShareRoleController(backgroundScope)
        controller.select(ShareRole.Receive)
        controller.onCardReceived()

        controller.select(ShareRole.Send)
        advanceTimeBy(RETURN_WINDOW + 1)
        runCurrent()

        assertEquals(ShareRole.Send, controller.state.value.role)
        assertFalse(controller.state.value.returning)
    }

    private companion object {
        const val RETURN_WINDOW = 60_000L
    }
}
