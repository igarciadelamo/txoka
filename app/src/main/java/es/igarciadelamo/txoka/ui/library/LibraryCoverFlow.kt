package es.igarciadelamo.txoka.ui.library

import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.library.LibraryOrganization
import es.igarciadelamo.txoka.domain.received.ReceivedContact
import es.igarciadelamo.txoka.ui.components.CardMotion
import es.igarciadelamo.txoka.ui.components.PHYSICAL_CARD_RATIO
import es.igarciadelamo.txoka.ui.components.rememberCardMotionController
import es.igarciadelamo.txoka.ui.components.toComposeColor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor

@Composable
internal fun LibraryCoverFlow(
    contacts: List<ReceivedContact>,
    organization: LibraryOrganization,
    pager: PagerState,
    landscape: Boolean,
    onOpenCard: (ReceivedContact) -> Unit,
    onOpenContact: (Long) -> Unit,
    onFavorite: (Long, Boolean) -> Unit,
    onOrganize: () -> Unit,
    onCentered: (Long) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val position = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, contacts.lastIndex.toFloat())
    val lower = floor(position).toInt()
    val upper = (lower + 1).coerceAtMost(contacts.lastIndex)
    val colors = categoryColors(contacts[lower], organization)
    val next = categoryColors(contacts[upper], organization)
    val backdrop = listOf(
        lerp(colors.first, next.first, position - lower),
        lerp(
            colors.second,
            next.second,
            position - lower,
        ),
    )
    val centered = contacts[pager.currentPage.coerceIn(contacts.indices)]
    LaunchedEffect(pager, contacts) {
        snapshotFlow { pager.currentPage }.distinctUntilChanged().collect {
            contacts.getOrNull(it)?.let { contact -> onCentered(contact.id) }
        }
    }
    Column(
        modifier.fillMaxSize().background(Brush.linearGradient(backdrop)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!landscape) {
            Text(
                text = categoryName(centered, organization),
                modifier = Modifier.padding(12.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = .94f), CircleShape).padding(12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        CoverFlowStage(
            contacts = contacts,
            pager = pager,
            landscape = landscape,
            visible = visible,
            onOpenCard = onOpenCard,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        if (!landscape) {
            TextButton(
                onClick = { onOpenContact(centered.id) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = .94f), CircleShape),
            ) {
                Text(
                    centered.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            LibraryPagerControls(pager, contacts.size)
            LibraryContactActions(centered, organization, onFavorite, onOrganize)
        }
    }
}

@Composable
private fun CoverFlowStage(
    contacts: List<ReceivedContact>,
    pager: PagerState,
    landscape: Boolean,
    visible: Boolean,
    onOpenCard: (ReceivedContact) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animations = ValueAnimator.areAnimatorsEnabled()
    val scope = rememberCoroutineScope()
    val motion = if (visible && animations) rememberCardMotionController().motion else CardMotion()
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val availableWidth = (maxWidth * if (landscape) 1f else .84f).coerceAtLeast(1.dp)
        val availableHeight = (maxHeight - 8.dp).coerceAtLeast(1.dp)
        val cardWidth = minOf(availableWidth, availableHeight * PHYSICAL_CARD_RATIO)
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            pageSize = PageSize.Fixed(cardWidth),
            pageSpacing = -cardWidth * .58f,
            contentPadding = PaddingValues(horizontal = (maxWidth - cardWidth) / 2),
            beyondViewportPageCount = 1,
            key = { contacts[it].id },
        ) { page ->
            val offset = pager.currentPage - page + pager.currentPageOffsetFraction
            val distance = abs(offset).coerceAtMost(1f)
            val active = page == pager.currentPage && !pager.isScrollInProgress && visible
            val description = stringResource(R.string.library_open_card, contacts[page].name)
            Box(
                Modifier.width(cardWidth).height(cardWidth / PHYSICAL_CARD_RATIO)
                    .zIndex(1f - distance)
                    .graphicsLayer {
                        rotationY = offset.coerceIn(-1f, 1f) * 54f
                        scaleX = 1f - distance * .13f
                        scaleY = 1f - distance * .13f
                        cameraDistance = 18f * density
                    }
                    .clickable(role = Role.Button, onClick = {
                        if (page == pager.currentPage) {
                            onOpenCard(contacts[page])
                        } else {
                            scope.launch {
                                if (animations) pager.animateScrollToPage(page) else pager.scrollToPage(page)
                            }
                        }
                    })
                    .semantics { contentDescription = description },
            ) {
                LibraryCard(
                    contacts[page],
                    motion = if (active) motion else CardMotion(),
                    paused =
                    !active || !animations,
                )
            }
        }
    }
}

@Composable
internal fun LibraryPagerControls(
    pager: PagerState,
    count: Int,
    modifier: Modifier = Modifier,
    onDark: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val animations = ValueAnimator.areAnimatorsEnabled()
    fun move(delta: Int) {
        val target = (pager.currentPage + delta).coerceIn(0, count - 1)
        scope.launch { if (animations) pager.animateScrollToPage(target) else pager.scrollToPage(target) }
    }
    val tint = MaterialTheme.colorScheme.onSurface
    val buttonModifier = if (onDark) {
        Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = .94f), CircleShape)
    } else {
        Modifier
    }
    Row(
        modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = { move(-1) }, enabled = pager.currentPage > 0, modifier = buttonModifier) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.library_previous), tint = tint)
        }
        Text(
            stringResource(R.string.library_position, pager.currentPage + 1, count),
            color = tint,
            style = MaterialTheme.typography.labelMedium,
            modifier = buttonModifier.padding(8.dp),
        )
        IconButton(onClick = { move(1) }, enabled = pager.currentPage < count - 1, modifier = buttonModifier) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.library_next), tint = tint)
        }
    }
}

@Composable
internal fun LibraryContactActions(
    contact: ReceivedContact,
    organization: LibraryOrganization,
    onFavorite: (Long, Boolean) -> Unit,
    onOrganize: () -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = true,
    stacked: Boolean = false,
) {
    val surface = MaterialTheme.colorScheme.surface.copy(alpha = .94f)
    val background = if (onDark) Modifier.background(surface) else Modifier
    Column(modifier.fillMaxWidth().then(background), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ContactQuickActions(contact, organization, onFavorite)
            if (!stacked) {
                TextButton(onClick = onOrganize) { Text(stringResource(R.string.library_organize)) }
            }
        }
        if (stacked) TextButton(onClick = onOrganize) { Text(stringResource(R.string.library_organize)) }
    }
}

@Composable
private fun ContactQuickActions(
    contact: ReceivedContact,
    organization: LibraryOrganization,
    onFavorite: (Long, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val favorite = contact.id in organization.favorites
    IconButton(
        onClick = {
            contact.phone?.let { launchLibraryIntent(context, Intent(Intent.ACTION_DIAL, "tel:$it".toUri())) }
        },
        enabled = contact.phone != null,
    ) { Icon(Icons.Default.Phone, stringResource(R.string.action_call)) }
    IconButton(
        onClick = {
            contact.email?.let { launchLibraryIntent(context, Intent(Intent.ACTION_SENDTO, "mailto:$it".toUri())) }
        },
        enabled = contact.email != null,
    ) { Icon(Icons.Default.Email, stringResource(R.string.action_email)) }
    IconButton(onClick = { onFavorite(contact.id, !favorite) }) {
        Icon(
            if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            stringResource(if (favorite) R.string.library_unfavorite else R.string.library_favorite),
        )
    }
}

private fun launchLibraryIntent(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.library_action_unavailable, Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.library_action_unavailable, Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun categoryName(contact: ReceivedContact, organization: LibraryOrganization): String =
    organization.categories.firstOrNull { it.id == organization.assignments[contact.id] }?.name
        ?: stringResource(R.string.library_uncategorized)

private fun categoryColors(contact: ReceivedContact, organization: LibraryOrganization): Pair<Color, Color> {
    val category = organization.categories.firstOrNull { it.id == organization.assignments[contact.id] }
    val first = category?.accent?.toComposeColor() ?: UncategorizedBackdrop
    val second = category?.accent2?.toComposeColor() ?: first
    return first to second
}

private val UncategorizedBackdrop = Color(0xFF455A64)
