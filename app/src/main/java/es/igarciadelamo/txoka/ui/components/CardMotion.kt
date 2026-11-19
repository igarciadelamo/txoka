package es.igarciadelamo.txoka.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

data class CardMotion(val x: Float = 0f, val y: Float = 0f) {
    fun bounded(): CardMotion = CardMotion(x.finiteUnit(), y.finiteUnit())
}

private fun Float.finiteUnit(): Float = if (isFinite()) coerceIn(-1f, 1f) else 0f

/** Projects the screen normal relative to its calibrated pose, avoiding Euler-angle wraparound. */
internal class CardMotionProjection {
    private var origin: CardMotion? = null
    private var originRotation: FloatArray? = null
    private var smoothed = CardMotion()

    fun center() {
        origin = null
        originRotation = null
        smoothed = CardMotion()
    }

    fun update(screenGravityX: Float, screenGravityY: Float): CardMotion {
        if (!screenGravityX.isFinite() || !screenGravityY.isFinite()) return smoothed
        val sample = CardMotion(screenGravityX, screenGravityY)
        val baseline = origin ?: sample.also { origin = it }
        val target = CardMotion(
            (sample.x - baseline.x) * SENSOR_GAIN,
            (sample.y - baseline.y) * SENSOR_GAIN,
        ).bounded()
        smoothed = CardMotion(
            smoothed.x * PREVIOUS_WEIGHT + target.x * SAMPLE_WEIGHT,
            smoothed.y * PREVIOUS_WEIGHT + target.y * SAMPLE_WEIGHT,
        )
        return smoothed
    }

    fun updateRotation(rotation: FloatArray): CardMotion {
        if (rotation.size != ROTATION_MATRIX_SIZE || rotation.any { !it.isFinite() }) return smoothed
        val baseline = originRotation ?: rotation.copyOf().also { originRotation = it }
        val normalX = baseline[XX] * rotation[XZ] + baseline[YX] * rotation[YZ] + baseline[ZX] * rotation[ZZ]
        val normalY = baseline[XY] * rotation[XZ] + baseline[YY] * rotation[YZ] + baseline[ZY] * rotation[ZZ]
        return update(normalX, -normalY)
    }
}

class CardMotionController internal constructor(val sensorsAvailable: Boolean) {
    var motion: CardMotion by mutableStateOf(CardMotion())
        private set
    var sensorsEnabled: Boolean by mutableStateOf(sensorsAvailable)
        private set
    private val projection = CardMotionProjection()

    fun toggleSensors() {
        if (sensorsAvailable) {
            sensorsEnabled = !sensorsEnabled
            center()
        }
    }

    fun center() {
        projection.center()
        motion = CardMotion()
    }

    fun updateManual(x: Float, y: Float) {
        sensorsEnabled = false
        motion = CardMotion(x, y).bounded()
    }

    internal fun updateSensor(rotation: FloatArray) {
        if (sensorsEnabled) motion = projection.updateRotation(rotation)
    }
}

@Composable
fun rememberCardMotionController(): CardMotionController {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val sensor = remember(manager) {
        manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }
    val controller = remember(manager, sensor) { CardMotionController(sensor != null) }
    val owner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    DisposableEffect(manager, sensor, controller.sensorsEnabled, owner, configuration.orientation) {
        if (controller.sensorsEnabled) controller.center()
        val listener = rotationListener(context, controller)
        fun register() {
            if (controller.sensorsEnabled && sensor != null) {
                manager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (controller.sensorsEnabled) controller.center()
                    register()
                }
                Lifecycle.Event.ON_PAUSE -> manager?.unregisterListener(listener)
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) register()
        onDispose {
            manager?.unregisterListener(listener)
            owner.lifecycle.removeObserver(observer)
        }
    }
    return controller
}

private fun rotationListener(context: Context, controller: CardMotionController): SensorEventListener =
    object : SensorEventListener {
        private val rotation = FloatArray(ROTATION_MATRIX_SIZE)
        private val screen = FloatArray(ROTATION_MATRIX_SIZE)
        private var previousScreenRotation: Int? = null
        override fun onSensorChanged(event: SensorEvent) {
            if (!controller.sensorsEnabled) return
            SensorManager.getRotationMatrixFromVector(rotation, event.values)
            val displayRotation = screenRotation(context)
            if (previousScreenRotation != displayRotation) {
                controller.center()
                previousScreenRotation = displayRotation
            }
            val axes = screenAxes(displayRotation)
            if (SensorManager.remapCoordinateSystem(rotation, axes.first, axes.second, screen)) {
                controller.updateSensor(screen)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

@Suppress("DEPRECATION")
private fun screenRotation(context: Context): Int =
    context.getSystemService(WindowManager::class.java).defaultDisplay.rotation

private fun screenAxes(rotation: Int): Pair<Int, Int> = when (rotation) {
    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
}

private const val SENSOR_GAIN = 2.6f
private const val PREVIOUS_WEIGHT = 0.78f
private const val SAMPLE_WEIGHT = 0.22f
private const val ROTATION_MATRIX_SIZE = 9
private const val XX = 0
private const val XY = 1
private const val XZ = 2
private const val YX = 3
private const val YY = 4
private const val YZ = 5
private const val ZX = 6
private const val ZY = 7
private const val ZZ = 8
