package es.igarciadelamo.txoka.ui.components

import android.content.Context
import es.igarciadelamo.txoka.domain.profile.CardBackground

internal object CardMaterialArtworkAssets {
    private val cached = mutableMapOf<CardBackground, CardMaterialArtwork?>()

    @Synchronized
    fun load(context: Context, background: CardBackground): CardMaterialArtwork? {
        val name = when (background) {
            CardBackground.STAINED_GLASS -> "glass-color.rgb"
            CardBackground.NACRE -> "nacre-color.rgb"
            CardBackground.BRUSHED_METAL -> "metal-color.rgb"
            else -> return null
        }
        if (cached.containsKey(background)) return cached[background]
        val artwork = runCatching {
            context.assets.open("card-materials/$name").use { CardMaterialArtwork.fromBytes(it.readBytes()) }
        }.getOrNull()
        cached[background] = artwork
        return artwork
    }
}
