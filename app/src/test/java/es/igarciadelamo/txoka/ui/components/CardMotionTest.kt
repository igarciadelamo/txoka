package es.igarciadelamo.txoka.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class CardMotionTest {
    @Test
    fun `first sensor sample calibrates both axes and small tilts have equal sensitivity`() {
        val projection = CardMotionProjection()
        assertEquals(CardMotion(), projection.update(.2f, -.3f))
        var result = CardMotion()
        repeat(40) { result = projection.update(.3f, -.2f) }
        assertEquals(.26f, result.x, .001f)
        assertEquals(.26f, result.y, .001f)
    }

    @Test
    fun `motion saturates and invalid samples preserve the previous pose`() {
        val projection = CardMotionProjection()
        projection.update(0f, 0f)
        var result = CardMotion()
        repeat(60) { result = projection.update(1f, -1f) }
        assertEquals(1f, result.x, .001f)
        assertEquals(-1f, result.y, .001f)
        assertEquals(result, projection.update(Float.NaN, .5f))
        assertEquals(result, projection.update(.5f, Float.POSITIVE_INFINITY))
    }

    @Test
    fun `centering establishes a fresh origin without changing sensor gain`() {
        val projection = CardMotionProjection()
        projection.update(0f, 0f)
        repeat(30) { projection.update(.5f, .5f) }
        projection.center()
        assertEquals(CardMotion(), projection.update(-.6f, .4f))
        val result = projection.update(-.5f, .3f)
        assertTrue(result.x > 0f)
        assertTrue(result.y < 0f)
    }

    @Test
    fun `manual input is bounded and disables sensors without losing the pose`() {
        val controller = CardMotionController(sensorsAvailable = true)
        controller.updateManual(3f, -2f)
        assertEquals(CardMotion(1f, -1f), controller.motion)
        assertEquals(false, controller.sensorsEnabled)
        controller.center()
        assertEquals(CardMotion(), controller.motion)
        controller.toggleSensors()
        assertEquals(true, controller.sensorsEnabled)
    }

    @Test
    fun `sensorless devices keep manual controls usable`() {
        val controller = CardMotionController(sensorsAvailable = false)
        controller.toggleSensors()
        assertEquals(false, controller.sensorsEnabled)
        controller.updateManual(.25f, -.5f)
        assertEquals(CardMotion(.25f, -.5f), controller.motion)
        assertEquals(CardMotion(0f, 1f), CardMotion(Float.NaN, 8f).bounded())
    }

    @Test
    fun `upright phones retain full sensitivity to pitch and yaw`() {
        val upright = floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f)
        val angle = (10 * PI / 180).toFloat()
        val c = cos(angle)
        val s = sin(angle)
        val tiltedHorizontally = floatArrayOf(c, 0f, s, s, 0f, -c, 0f, 1f, 0f)
        val tiltedVertically = floatArrayOf(1f, 0f, 0f, 0f, -s, -c, 0f, c, -s)
        fun project(rotation: FloatArray): CardMotion {
            val projection = CardMotionProjection()
            assertEquals(CardMotion(), projection.updateRotation(upright))
            var result = CardMotion()
            repeat(50) { result = projection.updateRotation(rotation) }
            return result
        }
        val horizontal = project(tiltedHorizontally)
        val vertical = project(tiltedVertically)
        assertEquals(.4515f, horizontal.x, .001f)
        assertEquals(0f, horizontal.y, .001f)
        assertEquals(0f, vertical.x, .001f)
        assertEquals(.4515f, vertical.y, .001f)
    }

    @Test
    fun `relative normals cross angular wraparound without jumping`() {
        fun yaw(degrees: Int): FloatArray {
            val angle = (degrees * PI / 180).toFloat()
            val c = cos(angle)
            val s = sin(angle)
            return floatArrayOf(c, 0f, s, 0f, 1f, 0f, -s, 0f, c)
        }
        val projection = CardMotionProjection()
        projection.updateRotation(yaw(179))
        var result = CardMotion()
        repeat(50) { result = projection.updateRotation(yaw(-179)) }
        assertEquals(.0907f, result.x, .001f)
        assertEquals(0f, result.y, .001f)
        assertEquals(result, projection.updateRotation(floatArrayOf(Float.NaN)))
    }

    @Test
    fun `recentering after screen rotation forgets the previous coordinate basis`() {
        val projection = CardMotionProjection()
        val first = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        val turned = floatArrayOf(0f, 0f, 1f, 0f, 1f, 0f, -1f, 0f, 0f)
        projection.updateRotation(first)
        repeat(30) { projection.updateRotation(turned) }
        projection.center()
        assertEquals(CardMotion(), projection.updateRotation(turned))
        assertEquals(CardMotion(), projection.updateRotation(turned))
    }
}
