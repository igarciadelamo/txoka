package es.igarciadelamo.txoka.ui.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.protocol.VCardPayload
import es.igarciadelamo.txoka.ui.components.PrimaryButton
import es.igarciadelamo.txoka.ui.components.SecondaryButton
import es.igarciadelamo.txoka.ui.components.TxokaAppBar
import es.igarciadelamo.txoka.ui.components.contentWidth
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme
import kotlinx.coroutines.launch

@Composable
fun QrScanRoute(onBack: () -> Unit, onImported: () -> Unit, viewModel: QrScanViewModel = hiltViewModel()) {
    QrScanScreen(
        onBack = onBack,
        onPayload = { payload ->
            viewModel.offer(payload)
            onImported()
        },
    )
}

@Composable
fun QrScanScreen(onBack: () -> Unit, onPayload: (VCardPayload) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cameraRequested by rememberSaveable { mutableStateOf(false) }
    var denied by rememberSaveable { mutableStateOf(false) }
    var readingImage by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraRequested = granted
        denied = !granted
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                readingImage = true
                error = null
                val payload = readContactQrImage(context.contentResolver, uri)
                readingImage = false
                if (payload != null) onPayload(payload) else error = R.string.qr_scan_invalid
            }
        }
    }
    QrScanContent(
        cameraVisible = cameraRequested && !readingImage,
        denied = denied,
        readingImage = readingImage,
        error = error,
        onBack = onBack,
        onPayload = onPayload,
        onInvalid = { error = R.string.qr_scan_invalid },
        onUnavailable = {
            cameraRequested = false
            error = R.string.qr_scan_camera_unavailable
        },
        onCamera = {
            error = null
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                cameraRequested = true
                denied = false
            } else {
                permission.launch(Manifest.permission.CAMERA)
            }
        },
        onImage = { imagePicker.launch(arrayOf("image/*")) },
        modifier = modifier,
    )
}

@Composable
private fun QrScanContent(
    cameraVisible: Boolean,
    denied: Boolean,
    readingImage: Boolean,
    error: Int?,
    onBack: () -> Unit,
    onPayload: (VCardPayload) -> Unit,
    onInvalid: () -> Unit,
    onUnavailable: () -> Unit,
    onCamera: () -> Unit,
    onImage: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TxokaAppBar(title = stringResource(R.string.qr_scan_title), onBack = onBack)
        Column(
            Modifier.weight(1f).contentWidth().verticalScroll(rememberScrollState())
                .navigationBarsPadding().padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.qr_scan_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (cameraVisible) QrCameraPreview(onPayload, onInvalid, onUnavailable)
            if (denied) ScanStatus(R.string.qr_scan_permission_denied)
            error?.let { ScanStatus(it) }
            if (readingImage) {
                CircularProgressIndicator()
                ScanStatus(R.string.qr_scan_reading_image)
            }
            PrimaryButton(
                text = stringResource(R.string.qr_scan_camera),
                enabled = !readingImage,
                onClick = onCamera,
            )
            SecondaryButton(
                text = stringResource(R.string.qr_scan_image),
                enabled = !readingImage,
                onClick = onImage,
            )
        }
    }
}

@Composable
private fun ScanStatus(message: Int) {
    Text(
        text = stringResource(message),
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun QrScanScreenPreview() {
    TxokaTheme { QrScanScreen(onBack = {}, onPayload = {}) }
}
