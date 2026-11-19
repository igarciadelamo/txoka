package es.igarciadelamo.txoka.ui.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.domain.profile.CardEffect
import es.igarciadelamo.txoka.domain.profile.CardPalette
import es.igarciadelamo.txoka.domain.profile.CardTextStyle
import es.igarciadelamo.txoka.domain.profile.RgbColor
import es.igarciadelamo.txoka.domain.profile.TextFinish
import es.igarciadelamo.txoka.domain.profile.TextSize
import es.igarciadelamo.txoka.ui.components.CardMotion
import es.igarciadelamo.txoka.ui.components.CardSurface
import es.igarciadelamo.txoka.ui.components.FinishedCardText

@Composable
internal fun EffectPreview(effect: CardEffect, card: CardPalette) {
    CardSurface(
        palette = card.copy(appearance = (card.editableAppearance()).copy(effect = effect)),
        motion = CardMotion(.12f, -.08f),
        paused = true,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
internal fun LetteringPreview(finish: TextFinish, color: RgbColor?) {
    FinishedCardText(
        text = stringResource(R.string.appearance_sample_letters),
        finish = CardTextStyle(finish = finish, size = TextSize.M, color = color),
        motion = CardMotion(.15f, -.1f),
        baseSize = 24.sp,
    )
}
