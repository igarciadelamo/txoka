package es.igarciadelamo.txoka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.ui.profile.AppearanceMotionControls
import es.igarciadelamo.txoka.ui.theme.Spacing

/** Uses the current window orientation without changing the activity's rotation policy. */
@Composable
fun CardViewer(profile: Profile, onDismiss: () -> Unit) {
    val motion = rememberCardMotionController()
    var paused by rememberSaveable { mutableStateOf(false) }
    var qrVisible by rememberSaveable(profile) { mutableStateOf(false) }
    val qrAvailable = remember(profile) { profile.canShowQr() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            CardViewerSystemBars(immersive = maxWidth > maxHeight)
            CardViewerContent(
                profile = profile,
                motion = motion,
                paused = paused,
                qrVisible = qrVisible,
                onQrVisibleChange = { qrVisible = it },
                onPausedChange = { paused = it },
                qrAvailable = qrAvailable,
                onDismiss = onDismiss,
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            )
        }
    }
}

@Composable
private fun CardViewerSystemBars(immersive: Boolean) {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return
    val lightBackground = MaterialTheme.colorScheme.background.luminance() > .5f
    DisposableEffect(window, view, lightBackground, immersive) {
        val controller = WindowCompat.getInsetsController(window, view)
        val previousStatusBars = controller.isAppearanceLightStatusBars
        val previousNavigationBars = controller.isAppearanceLightNavigationBars
        val previousBehavior = controller.systemBarsBehavior
        val insets = ViewCompat.getRootWindowInsets(view)
        val statusWasVisible = insets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true
        val navigationWasVisible = insets?.isVisible(WindowInsetsCompat.Type.navigationBars()) ?: true
        controller.isAppearanceLightStatusBars = lightBackground
        controller.isAppearanceLightNavigationBars = lightBackground
        if (immersive) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller.isAppearanceLightStatusBars = previousStatusBars
            controller.isAppearanceLightNavigationBars = previousNavigationBars
            controller.systemBarsBehavior = previousBehavior
            if (statusWasVisible) {
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
            if (navigationWasVisible) {
                controller.show(WindowInsetsCompat.Type.navigationBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
}

@Composable
private fun CardViewerContent(
    profile: Profile,
    motion: CardMotionController,
    paused: Boolean,
    qrVisible: Boolean,
    onQrVisibleChange: (Boolean) -> Unit,
    onPausedChange: (Boolean) -> Unit,
    qrAvailable: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            val controlsWidth = 112.dp
            val availableWidth = (maxWidth - controlsWidth - Spacing.md - Spacing.sm * 2).coerceAtLeast(1.dp)
            val availableHeight = (maxHeight - Spacing.xs * 2).coerceAtLeast(1.dp)
            val cardWidth = minOf(availableWidth, availableHeight * PHYSICAL_CARD_RATIO)
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ViewerCard(
                    profile,
                    motion,
                    paused,
                    qrVisible,
                    onQrVisibleChange,
                    Modifier.width(cardWidth).verticalScroll(rememberScrollState()),
                )
                Column(
                    Modifier.width(controlsWidth).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterVertically),
                ) {
                    ViewerActions(qrVisible, qrAvailable, onQrVisibleChange, onDismiss)
                    AppearanceMotionControls(motion, paused, onPausedChange, stacked = true)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(Spacing.lg),
            ) {
                ViewerActions(qrVisible, qrAvailable, onQrVisibleChange, onDismiss)
                PortraitViewer(
                    profile = profile,
                    motion = motion,
                    paused = paused,
                    qrVisible = qrVisible,
                    onQrChange = onQrVisibleChange,
                    onPausedChange = onPausedChange,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ViewerActions(
    qrVisible: Boolean,
    qrAvailable: Boolean,
    onQrChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { onQrChange(!qrVisible) },
            enabled = qrAvailable,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Text(stringResource(if (qrVisible) R.string.qr_back_to_card else R.string.qr_show))
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.card_viewer_close))
        }
    }
}

@Composable
private fun PortraitViewer(
    profile: Profile,
    motion: CardMotionController,
    paused: Boolean,
    qrVisible: Boolean,
    onQrChange: (Boolean) -> Unit,
    onPausedChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val availableHeight = (maxHeight - 112.dp).coerceAtLeast(80.dp)
        val cardWidth = minOf(maxWidth, availableHeight * PHYSICAL_CARD_RATIO)
        Column(
            Modifier.width(cardWidth).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ViewerCard(profile, motion, paused, qrVisible, onQrChange, Modifier.fillMaxWidth())
            AppearanceMotionControls(motion, paused, onPausedChange)
        }
    }
}

@Composable
private fun ViewerCard(
    profile: Profile,
    motion: CardMotionController,
    paused: Boolean,
    qrVisible: Boolean,
    onQrChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
    Box(modifier) {
        ContactCard(
            profile = profile,
            motion = motion.motion,
            paused = paused,
            qrEnabled = true,
            qrVisible = qrVisible,
            onQrVisibleChange = onQrChange,
            showQrControls = false,
        )
    }
}
