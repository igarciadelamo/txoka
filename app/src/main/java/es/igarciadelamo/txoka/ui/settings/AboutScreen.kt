package es.igarciadelamo.txoka.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.igarciadelamo.txoka.BuildConfig
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.ui.components.Eyebrow
import es.igarciadelamo.txoka.ui.components.NfcWaves
import es.igarciadelamo.txoka.ui.components.ScreenTopBar
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.Spacing
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

@Composable
fun AboutScreen(onBack: () -> Unit, onOpenLicences: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTopBar(title = stringResource(R.string.settings_about), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppMark()
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.padding(top = Spacing.lg),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.hub_empty_title),
                modifier = Modifier.padding(top = Spacing.md),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Column(
                modifier = Modifier.padding(top = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Eyebrow(text = stringResource(R.string.settings_legal))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Corners.sheet))
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    AboutRow(
                        title = stringResource(R.string.settings_licences),
                        onClick = onOpenLicences,
                    )
                    AboutRow(
                        title = stringResource(R.string.about_made_by, stringResource(R.string.about_author)),
                        onClick = null,
                    )
                }
            }

            Text(
                text = stringResource(R.string.hub_privacy_footer),
                modifier = Modifier.padding(top = Spacing.xl, bottom = Spacing.lg),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AppMark() {
    Box(
        modifier = Modifier
            .padding(top = Spacing.xl)
            .size(88.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        NfcWaves(
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun AboutRow(title: String, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, role = Role.Button, onClick = { onClick?.invoke() })
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (onClick != null) Icons.Filled.Info else Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    TxokaTheme {
        AboutScreen(onBack = {}, onOpenLicences = {})
    }
}
