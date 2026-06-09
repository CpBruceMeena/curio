package com.curio.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Secondary
import com.curio.app.ui.theme.SecondaryFixed
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.SurfaceContainerLow
import kotlinx.coroutines.delay

private val CubeImageUrl = "https://lh3.googleusercontent.com/aida/AP1WRLtQJvk8dW0Mf-KwEtM45JierFl5T2sMj6Oo46aSCYpwMk3xdCXKHZ2U-ETGUmVld3ICET-RBpbw17-kyNMmiz6qKMBoHwcSdYWKCW-lxxivD7BqNjv71tjnCZgESVo6x-0OlyRD4KnKoYrCqS6WhBvjNCntej1G2Pm-eWqp_Jwn5jrnmIyiiWCwN4P5m639UkyiZQJbDD0Hlb9iLT1lZ7KyoIbzLYxch1bhf2Jbbu4xiyemAdTv_l1zqF8"

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit
) {
    // ── Staggered reveal state ──────────────────────────────────────
    var showTitle by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(800)
        showTitle = true
        delay(300)
        showTagline = true
        delay(300)
        showButton = true
    }

    // ── Floating animation for the cube background ───────────────
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "float"
    )

    // ── Entrance slide-up animations (progress: 0→1) ────────────────
    val titleProgress = remember { Animatable(0f) }
    val taglineProgress = remember { Animatable(0f) }
    val buttonProgress = remember { Animatable(0f) }

    LaunchedEffect(showTitle) {
        if (showTitle) titleProgress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(showTagline) {
        if (showTagline) taglineProgress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(showButton) {
        if (showButton) buttonProgress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }

    val titleSlide = { p: Float -> ((1f - p) * 30f).dp }
    val taglineSlide = { p: Float -> ((1f - p) * 30f).dp }
    val buttonSlide = { p: Float -> ((1f - p) * 30f).dp }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        // ── Full-screen cube background (zoomed to fill edge-to-edge) ─
        AsyncImage(
            model = CubeImageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.2f)
                .offset(y = floatOffset.dp),
            contentScale = ContentScale.Fit
        )

        // ── Dark overlay for text readability ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
        )

        // ── Content overlaid on top ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Spacer to push content slightly below center ───────
            Spacer(modifier = Modifier.weight(0.15f))

            // ── Title ───────────────────────────────────────────────
            if (showTitle) {
                Text(
                    text = "Curio",
                    style = MaterialTheme.typography.displayLarge,
                    color = Secondary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(titleProgress.value)
                        .offset(y = titleSlide(titleProgress.value))
                )
            }

            // ── Tagline ─────────────────────────────────────────────
            if (showTagline) {
                Text(
                    text = "\"One interesting thing at a time\"",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = OnSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(taglineProgress.value)
                        .padding(top = 8.dp)
                        .offset(y = taglineSlide(taglineProgress.value))
                )
            }

            // ── Fill remaining space ────────────────────────────────
            Spacer(modifier = Modifier.weight(1f))

            // ── Glass button at bottom ──────────────────────────────
            if (showButton) {
                OutlinedButton(
                    onClick = onNavigateToOnboarding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .alpha(buttonProgress.value)
                        .offset(y = buttonSlide(buttonProgress.value)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceContainerLow.copy(alpha = 0.4f),
                        contentColor = Secondary,
                    ),
                    border = BorderStroke(1.dp, SecondaryFixed.copy(alpha = 0.3f)),
                ) {
                    Text(
                        text = "START EXPLORING",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.08.sp,
                        color = Secondary,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
