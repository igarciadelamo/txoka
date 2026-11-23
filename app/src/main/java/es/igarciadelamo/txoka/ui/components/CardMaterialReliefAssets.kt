package es.igarciadelamo.txoka.ui.components

import android.content.Context
import es.igarciadelamo.txoka.domain.profile.CardBackground

internal object CardMaterialReliefAssets {
    private val cached = mutableMapOf<CardBackground, CardMaterialRelief?>()

    @Synchronized
    fun load(context: Context, background: CardBackground): CardMaterialRelief? {
        val name = when (background) {
            CardBackground.STAINED_GLASS -> "glass-relief.u8"
            CardBackground.NACRE -> "nacre-relief.u8"
            else -> return null
        }
        if (cached.containsKey(background)) return cached[background]
        val relief = runCatching {
            context.assets.open("card-materials/$name").use { CardMaterialRelief.fromBytes(it.readBytes()) }
        }.getOrNull()
        cached[background] = relief
        return relief
    }
}
