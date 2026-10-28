package es.igarciadelamo.txoka.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.igarciadelamo.txoka.ui.navigation.TxokaNavHost

@Composable
fun TxokaApp(modifier: Modifier = Modifier) {
    TxokaNavHost(modifier = modifier)
}
