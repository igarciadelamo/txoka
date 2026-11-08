package es.igarciadelamo.txoka.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.SocialNetwork

/** Brand marks are bundled as vectors and tinted by the surface that hosts them. */
fun SocialNetwork.iconRes(): Int = when (this) {
    SocialNetwork.LinkedIn -> R.drawable.ic_linkedin
    SocialNetwork.Instagram -> R.drawable.ic_instagram
}

@Composable
fun SocialNetwork.label(): String = when (this) {
    SocialNetwork.LinkedIn -> stringResource(R.string.field_linkedin)
    SocialNetwork.Instagram -> stringResource(R.string.field_instagram)
}

@Composable
fun SocialIcon(network: SocialNetwork, tint: Color, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(network.iconRes()),
        contentDescription = null,
        modifier = modifier,
        tint = tint,
    )
}
