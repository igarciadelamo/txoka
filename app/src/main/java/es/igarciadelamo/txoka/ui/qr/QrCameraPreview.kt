package es.igarciadelamo.txoka.ui.qr

import android.annotation.SuppressLint
import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.PlanarYUVLuminanceSource
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.protocol.QrVCardCodec
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.ui.theme.Corners
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("MissingPermission")
@Composable
internal fun QrCameraPreview(onPayload: (VCardPayload) -> Unit, onInvalid: () -> Unit, onUnavailable: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }
    val payloadCallback = rememberUpdatedState(onPayload)
    val invalidCallback = rememberUpdatedState(onInvalid)
    val unavailableCallback = rememberUpdatedState(onUnavailable)
    val description = stringResource(R.string.qr_scan_viewfinder)
    DisposableEffect(owner, previewView) {
        val session = QrCameraSession(context, owner, previewView)
        session.start(
            onPayload = { payloadCallback.value(it) },
            onInvalid = { invalidCallback.value() },
            onUnavailable = { unavailableCallback.value() },
        )
        onDispose { session.close() }
    }
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(Corners.card))
            .semantics { contentDescription = description },
    )
}

private class QrCameraSession(
    private val context: Context,
    private val owner: LifecycleOwner,
    private val view: PreviewView,
) : AutoCloseable {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val closed = AtomicBoolean(false)
    private val complete = AtomicBoolean(false)
    private val invalidReported = AtomicBoolean(false)
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private var provider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var analysis: ImageAnalysis? = null
    private var lastFrameAt = 0L
    private var cameraUnavailable: () -> Unit = {}

    fun start(onPayload: (VCardPayload) -> Unit, onInvalid: () -> Unit, onUnavailable: () -> Unit) {
        cameraUnavailable = onUnavailable
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                if (!closed.get()) {
                    runCatching {
                        provider = future.get()
                        bind(onPayload, onInvalid)
                    }.onFailure { onUnavailable() }
                }
            },
            mainExecutor,
        )
    }

    @SuppressLint("MissingPermission")
    private fun bind(onPayload: (VCardPayload) -> Unit, onInvalid: () -> Unit) {
        val cameraProvider = checkNotNull(provider)
        val cameraPreview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(
                ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(CAMERA_RESOLUTION, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                ).build(),
            ).build()
        preview = cameraPreview
        analysis = imageAnalysis
        imageAnalysis.setAnalyzer(executor) { image -> analyze(image, onPayload, onInvalid) }
        val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        cameraProvider.bindToLifecycle(owner, selector, cameraPreview, imageAnalysis)
    }

    private fun analyze(image: ImageProxy, onPayload: (VCardPayload) -> Unit, onInvalid: () -> Unit) {
        try {
            if (!shouldReadFrame()) return
            val text = decodeQrText(image.luminance()) ?: return
            publish(text, onPayload, onInvalid)
        } catch (_: IllegalArgumentException) {
            postIfOpen(cameraUnavailable)
        } catch (_: IndexOutOfBoundsException) {
            postIfOpen(cameraUnavailable)
        } finally {
            image.close()
        }
    }

    private fun shouldReadFrame(): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        if (closed.get() || complete.get() || now - lastFrameAt < FRAME_INTERVAL_MILLIS) return false
        lastFrameAt = now
        return true
    }

    private fun publish(text: String, onPayload: (VCardPayload) -> Unit, onInvalid: () -> Unit) {
        val payload = QrVCardCodec.decode(text)
        if (payload != null && complete.compareAndSet(false, true)) {
            postIfOpen { onPayload(payload) }
        } else if (payload == null && invalidReported.compareAndSet(false, true)) {
            postIfOpen(onInvalid)
        }
    }

    private fun postIfOpen(action: () -> Unit) {
        mainExecutor.execute { if (!closed.get()) action() }
    }

    override fun close() {
        closed.set(true)
        analysis?.clearAnalyzer()
        preview?.let { provider?.unbind(it) }
        analysis?.let { provider?.unbind(it) }
        executor.shutdown()
    }
}

private fun ImageProxy.luminance(): PlanarYUVLuminanceSource {
    val plane = planes.first()
    val buffer = plane.buffer
    val crop = cropRect
    val largestDimension = maxOf(crop.width(), crop.height())
    val sample = ((largestDimension + MAX_CAMERA_DIMENSION - 1) / MAX_CAMERA_DIMENSION).coerceAtLeast(1)
    val sampledWidth = crop.width() / sample
    val sampledHeight = crop.height() / sample
    val bytes = ByteArray(sampledWidth * sampledHeight)
    val initialPosition = buffer.position()
    for (row in 0 until sampledHeight) {
        for (column in 0 until sampledWidth) {
            val index = initialPosition + (crop.top + row * sample) * plane.rowStride +
                (crop.left + column * sample) * plane.pixelStride
            bytes[row * sampledWidth + column] = buffer.get(index)
        }
    }
    return PlanarYUVLuminanceSource(bytes, sampledWidth, sampledHeight, 0, 0, sampledWidth, sampledHeight, false)
}

private val CAMERA_RESOLUTION = Size(960, 720)
private const val MAX_CAMERA_DIMENSION = 1280
private const val FRAME_INTERVAL_MILLIS = 400L
