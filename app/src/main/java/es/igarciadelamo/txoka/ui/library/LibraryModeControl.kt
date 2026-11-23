package es.igarciadelamo.txoka.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.library.LibraryViewMode

@Composable
internal fun LibraryModeControl(
    mode: LibraryViewMode,
    onChange: (LibraryViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        LibraryViewMode.entries.forEach { option ->
            val label = stringResource(
                when (option) {
                    LibraryViewMode.SHELVES -> R.string.library_mode_shelves
                    LibraryViewMode.COVER_FLOW -> R.string.library_mode_coverflow
                    LibraryViewMode.LIST -> R.string.library_mode_list
                },
            )
            val color = if (mode ==
                option
            ) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            IconToggleButton(
                checked = mode == option,
                onCheckedChange = { onChange(option) },
                modifier = Modifier.semantics { contentDescription = label },
            ) {
                Canvas(Modifier.size(24.dp)) {
                    val stroke = Stroke(1.8.dp.toPx())
                    when (option) {
                        LibraryViewMode.SHELVES -> repeat(2) { row ->
                            drawRoundRect(
                                color,
                                Offset(1.dp.toPx(), (2 + row * 12).dp.toPx()),
                                Size(22.dp.toPx(), 8.dp.toPx()),
                                CornerRadius(2.dp.toPx()),
                                stroke,
                            )
                        }
                        LibraryViewMode.COVER_FLOW -> {
                            drawRoundRect(
                                color,
                                Offset(5.dp.toPx(), 4.dp.toPx()),
                                Size(14.dp.toPx(), 16.dp.toPx()),
                                CornerRadius(2.dp.toPx()),
                                stroke,
                            )
                            drawLine(
                                color,
                                Offset(1.dp.toPx(), 6.dp.toPx()),
                                Offset(1.dp.toPx(), 18.dp.toPx()),
                                2.dp.toPx(),
                            )
                            drawLine(
                                color,
                                Offset(23.dp.toPx(), 6.dp.toPx()),
                                Offset(23.dp.toPx(), 18.dp.toPx()),
                                2.dp.toPx(),
                            )
                        }
                        LibraryViewMode.LIST -> repeat(3) { row ->
                            drawLine(
                                color,
                                Offset(2.dp.toPx(), (5 + row * 7).dp.toPx()),
                                Offset(22.dp.toPx(), (5 + row * 7).dp.toPx()),
                                2.dp.toPx(),
                            )
                        }
                    }
                }
            }
        }
    }
}
