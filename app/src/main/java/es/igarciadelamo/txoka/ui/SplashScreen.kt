package es.igarciadelamo.txoka.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.igarciadelamo.txoka.R
import es.igarciadelamo.txoka.ui.theme.TxokaTheme

/**
 * Branded splash shown once per cold start. It uses the card gradient, so it never reads as an
 * empty white screen, and carries no copy beyond the app name.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        SPLASH_MID,
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
            )
            // Swallow taps so the hub behind cannot be touched while the splash is up.
            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_splash_logo),
                contentDescription = null,
                modifier = Modifier.size(132.dp),
            )
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.padding(top = 18.dp),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                ),
                color = Color.White,
            )
        }
    }
}

private val SPLASH_MID = Color(0xFF3E7BE8)

@Preview(showBackground = true)
@Composable
private fun SplashPreview() {
    TxokaTheme {
        SplashScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashDarkPreview() {
    TxokaTheme(darkTheme = true) {
        SplashScreen()
    }
}
