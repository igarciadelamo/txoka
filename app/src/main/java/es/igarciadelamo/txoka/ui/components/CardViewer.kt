package es.igarciadelamo.txoka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.Profile
import es.igarciadelamo.txoka.ui.profile.AppearanceMotionControls
import es.igarciadelamo.txoka.ui.theme.Spacing

/** Uses the current window orientation without changing the activity's rotation policy. */
@Composable
fun CardViewer(profile: Profile, onDismiss: () -> Unit) {
    val motion = rememberCardMotionController()
    var paused by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
                .padding(Spacing.lg),
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.card_viewer_close))
            }
            CardViewerContent(profile, motion, paused, onPausedChange = { paused = it }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CardViewerContent(
    profile: Profile,
    motion: CardMotionController,
    paused: Boolean,
    onPausedChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val landscape = maxWidth > maxHeight
        val availableWidth = if (landscape) (maxWidth - 144.dp).coerceAtLeast(120.dp) else maxWidth
        val availableHeight = if (landscape) maxHeight else (maxHeight - 112.dp).coerceAtLeast(80.dp)
        val cardWidth = minOf(availableWidth, availableHeight * PHYSICAL_CARD_RATIO)
        val card: @Composable () -> Unit = {
            Column(
                modifier = Modifier.width(cardWidth).verticalScroll(rememberScrollState()),
            ) {
                ContactCard(profile = profile, motion = motion.motion, paused = paused)
            }
        }
        if (landscape) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                card()
                AppearanceMotionControls(motion, paused, onPausedChange, modifier = Modifier.width(132.dp))
            }
        } else {
            Column(
                modifier = Modifier.width(cardWidth).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                ContactCard(profile = profile, motion = motion.motion, paused = paused)
                AppearanceMotionControls(motion, paused, onPausedChange)
            }
        }
    }
}
