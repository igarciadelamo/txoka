package es.igarciadelamo.txoka.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.ui.components.Eyebrow
import es.igarciadelamo.txoka.ui.components.ScreenTopBar
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

@Composable
fun LicencesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    val fontLicence = remember(resources) {
        resources.openRawResource(R.raw.manrope_ofl).bufferedReader().use { it.readText() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTopBar(title = stringResource(R.string.settings_licences), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Eyebrow(text = stringResource(R.string.licences_font_title))
            Text(
                text = stringResource(R.string.licences_font_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = fontLicence,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm)
                    .clip(RoundedCornerShape(Corners.sheet))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(Spacing.lg),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LicencesScreenPreview() {
    TxokaTheme {
        LicencesScreen(onBack = {})
    }
}
