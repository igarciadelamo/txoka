package es.igarciadelamo.txoka.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.ui.theme.Corners
import es.igarciadelamo.txoka.ui.theme.Spacing

private val ButtonShape = RoundedCornerShape(Corners.field)

/** Small uppercase label that opens a section. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Wordmark header used by the hub. */
@Composable
fun TxokaHeader(modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        TxokaHeaderRow(modifier = modifier, actions = actions)
    }
}

@Composable
private fun TxokaHeaderRow(modifier: Modifier, actions: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/** Title bar for secondary screens: back arrow, large title and optional actions. */
@Composable
fun ScreenTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        ScreenTopBarRow(title = title, onBack = onBack, modifier = modifier, actions = actions)
    }
}

@Composable
private fun ScreenTopBarRow(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = if (onBack != null) Spacing.md else Spacing.xl, end = Spacing.md, top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack != null) Spacing.xs else 0.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        enabled = enabled,
        shape = ButtonShape,
        border = ButtonDefaults.outlinedButtonBorder(enabled = enabled),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Quiet action on a raised surface, used for the hub quick actions. */
@Composable
fun SurfaceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
    ) {
        if (leadingIcon != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon()
                Text(text = text, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Column with the shared screen padding, so every screen starts from the same grid. */
@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xl),
        content = content,
    )
}
