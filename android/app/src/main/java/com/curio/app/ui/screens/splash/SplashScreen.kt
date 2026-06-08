package com.curio.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import com.curio.app.ui.theme.Tertiary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ── Colour references (kept local to avoid import noise) ─────────────────
private val CyanGlow = Color(0x4400F4FE)
private val GoldGlow = Color(0x33E9C400)
private val TealGlow = Color(0x22A8CEC8)
private val RingColor = Color(0x1A00F4FE)

// ── Particle data class ──────────────────────────────────────────────────
private data class Particle(
    var x: Float,
    var y: Float,
    val size: Float,
    val speedY: Float,
    val speedX: Float,
    val alpha: Float,
    val color: Color,
    val phaseOffset: Float,   // used by the infinite animation loop
)

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit
) {
    // ── Phase state machine ──────────────────────────────────────────────
    var phase by remember { mutableStateOf(0) }          // 0…7

    LaunchedEffect(Unit) {
        // stagger the reveals
        phase = 1   // glow orb + particles start
        delay(200)
        phase = 2   // diamond icon
        delay(250)
        phase = 3   // "Curio" title
        delay(350)
        phase = 4   // tagline
        delay(400)
        phase = 5   // beam sweep hint
        delay(300)
        phase = 6   // button starts fading in
        // wait before auto-navigating
        delay(1500)
        onNavigateToOnboarding()
    }

    // ── Continuous animation values ──────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Glow orb pulse
    val orbScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbScale",
    )

    // Diamond rotation
    val diamondRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "diamondRotation",
    )

    // Diamond slow bob
    val diamondBob by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "diamondBob",
    )

    // Expanding rings
    val ringProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringProgress",
    )

    // Beam sweep angle
    val beamAngle by infiniteTransition.animateFloat(
        initialValue = -90f,
        targetValue = 270f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "beamAngle",
    )

    // Particle positions (computed in the Canvas draw)
    // We'll animate a single "globalTime" float that particles use
    val particleTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particleTime",
    )

    // ── Phase-driven animatables ─────────────────────────────────────────
    val diamondAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(12f) }
    val taglineAlpha = remember { Animatable(0f) }
    val taglineOffset = remember { Animatable(12f) }
    val buttonAlpha = remember { Animatable(0f) }

    LaunchedEffect(phase) {
        when (phase) {
            2 -> {
                diamondAlpha.snapTo(0f)
                diamondAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
            }
            3 -> {
                titleAlpha.snapTo(0f)
                titleOffset.snapTo(16f)
                titleAlpha.animateTo(1f, tween(600))
                titleOffset.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
            }
            4 -> {
                taglineAlpha.snapTo(0f)
                taglineOffset.snapTo(16f)
                taglineAlpha.animateTo(1f, tween(600))
                taglineOffset.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
            }
            6 -> {
                buttonAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
            }
        }
    }

    // ── Build particle list ──────────────────────────────────────────────
    val particles = remember {
        val colors = listOf(
            SecondaryContainer.copy(alpha = 0.5f),
            Primary.copy(alpha = 0.35f),
            Tertiary.copy(alpha = 0.3f),
            OnSurface.copy(alpha = 0.2f),
        )
        List(30) { i ->
            val angle = (i.toFloat() / 30f) * PI.toFloat() * 2f
            val radius = 60f + (i % 7) * 25f
            Particle(
                x = 0f,
                y = 0f,
                size = 1.5f + (i % 4) * 1.2f,
                speedY = -(0.15f + (i % 5) * 0.06f),
                speedX = sin(angle) * 0.12f,
                alpha = 0.3f + (i % 6) * 0.1f,
                color = colors[i % colors.size],
                phaseOffset = (i.toFloat() / 30f) * 1000f,
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .statusBarsPadding()
    ) {
        // ── Deep gradient background ────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x18103632), Color(0x000B1514)),
                        radius = 900f,
                    )
                )
        )

        // ── Sweeping beam light ─────────────────────────────────────────
        if (phase >= 5) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cX = size.width / 2f
                    val cY = size.height * 0.42f
                    val angleRad = beamAngle * PI.toFloat() / 180f
                    val beamLen = size.height * 1.5f
                    val beamWidth = size.width * 0.5f

                    // Soft light beam
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CyanGlow.copy(alpha = 0.06f),
                                Color.Transparent,
                            ),
                            startY = cY - 80f,
                            endY = cY + 150f,
                        ),
                        topLeft = Offset(
                            cX + cos(angleRad) * beamLen * 0.3f - beamWidth / 2f,
                            cY + sin(angleRad) * beamLen * 0.3f - 80f,
                        ),
                        size = androidx.compose.ui.geometry.Size(beamWidth, 230f),
                        alpha = 0.5f + 0.5f * sin(particleTime * 0.02f),
                        style = Fill,
                    )
                }
            }
        }

        // ── Particle system ─────────────────────────────────────────────
        if (phase >= 1) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cX = size.width / 2f
                val cY = size.height * 0.42f
                val t = particleTime

                particles.forEachIndexed { idx, p ->
                    // base position: random-ish orbit around center
                    val angle = (idx.toFloat() / particles.size) * PI.toFloat() * 2f +
                                t * 0.0008f
                    val radius = 80f + (idx % 7) * 35f + 20f * sin(t * 0.003f + p.phaseOffset)

                    val px = cX + cos(angle) * radius + sin(t * 0.001f + p.phaseOffset) * 30f
                    val py = cY + sin(angle) * radius * 0.6f +
                            (t * p.speedY * 0.5f) % size.height +
                            (idx * 7f)

                    // wrap Y
                    val wrapY = ((py % size.height) + size.height) % size.height
                    val fadeAlpha = p.alpha * (0.4f + 0.6f * (0.5f + 0.5f * sin(t * 0.005f + p.phaseOffset)))

                    drawCircle(
                        color = p.color.copy(alpha = fadeAlpha * 0.5f),
                        radius = p.size,
                        center = Offset(px, wrapY),
                    )
                }
            }
        }

        // ── Expanding rings ─────────────────────────────────────────────
        if (phase >= 1) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cX = size.width / 2f
                val cY = size.height * 0.42f
                val maxR = size.width * 0.9f
                val r1 = ringProgress * maxR * 0.3f
                val r2 = ringProgress * maxR * 0.6f

                val ringAlpha1 = (1f - ringProgress).coerceIn(0f, 0.25f)
                val ringAlpha2 = (1f - ringProgress).coerceIn(0f, 0.12f)

                drawCircle(
                    color = RingColor.copy(alpha = ringAlpha1),
                    radius = r1,
                    center = Offset(cX, cY),
                    style = Stroke(width = 1f),
                )
                drawCircle(
                    color = RingColor.copy(alpha = ringAlpha2),
                    radius = r2,
                    center = Offset(cX, cY),
                    style = Stroke(width = 0.5f),
                )
            }
        }

        // ── Central content ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // ── Glow orb behind diamond ────────────────────────────────
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(orbScale)
                    .alpha(if (phase >= 1) 1f else 0f),
                contentAlignment = Alignment.Center,
            ) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .blur(70.dp)
                        .background(CyanGlow.copy(alpha = 0.12f), CircleShape)
                )
                // Mid glow
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .blur(44.dp)
                        .background(TealGlow.copy(alpha = 0.15f), CircleShape)
                )
                // Inner warm glow
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .blur(26.dp)
                        .background(GoldGlow.copy(alpha = 0.10f), CircleShape)
                )

                // ── Diamond / Knowledge Cube ─────────────────────────────
                if (phase >= 2) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .alpha(diamondAlpha.value)
                            .rotate(diamondRotation)
                            .offset(y = diamondBob.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Glassmorphic backing
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0x331A3C38),
                                            Color(0x220B221F),
                                        )
                                    )
                                )
                                .blur(4.dp)
                        )
                        // Diamond symbol
                        Text(
                            text = "\u25C7",
                            style = MaterialTheme.typography.displayLarge,
                            color = SecondaryContainer.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Light,
                            modifier = Modifier.scale(1.3f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Curio title ─────────────────────────────────────────────
            if (phase >= 3) {
                Text(
                    text = "Curio",
                    style = MaterialTheme.typography.displayLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .alpha(titleAlpha.value)
                        .offset(y = titleOffset.value.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Tagline ─────────────────────────────────────────────────
            if (phase >= 4) {
                Text(
                    text = "One interesting thing at a time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(taglineAlpha.value)
                        .offset(y = taglineOffset.value.dp),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Get Started button ──────────────────────────────────────
            if (phase >= 6) {
                Button(
                    onClick = onNavigateToOnboarding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp)
                        .alpha(buttonAlpha.value),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryContainer,
                        contentColor = Color(0xFF002021),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
