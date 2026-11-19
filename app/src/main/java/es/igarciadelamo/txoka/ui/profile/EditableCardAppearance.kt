package es.igarciadelamo.txoka.ui.profile

import es.igarciadelamo.txoka.domain.profile.CardAppearance
import es.igarciadelamo.txoka.domain.profile.CardContrast
import es.igarciadelamo.txoka.domain.profile.CardPalette

/** Promoting a legacy card to explicit appearance preserves its automatically chosen foreground. */
internal fun CardPalette.editableAppearance(): CardAppearance {
    appearance?.let { return it }
    val foreground = CardContrast.contentColor(this)
    val defaults = CardAppearance()
    return defaults.copy(
        name = defaults.name.copy(color = foreground),
        data = defaults.data.copy(color = foreground),
    ).canonical()
}
