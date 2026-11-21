package es.igarciadelamo.txoka.ui.components

import android.content.Context

internal object CardMaterialCellsAssets {
    private var loaded = false
    private var cached: CardMaterialCells? = null

    @Synchronized
    fun load(context: Context): CardMaterialCells? {
        if (loaded) return cached
        cached = runCatching {
            context.assets.open("card-materials/glass-cells.u8").use { CardMaterialCells.fromBytes(it.readBytes()) }
        }.getOrNull()
        loaded = true
        return cached
    }
}
