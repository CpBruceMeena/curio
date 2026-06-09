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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curio.app.ui.theme.OnSurface
import com.curio.app.ui.theme.OnSurfaceVariant
import com.curio.app.ui.theme.Primary
import com.curio.app.ui.theme.SecondaryContainer
import com.curio.app.ui.theme.Surface
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── Space colour references ────────────────────────────────────
private val CyanGlow = Color(0x4400F4FE)
private val PurpleGlow = Color(0x33A78BFA)
private val GoldGlow = Color(0x28E9C400)
private val TealGlow = Color(0x1AA8CEC8)
private val StarWhite = Color(0xCCFFFFFF)
private val ShootingStarColor = Color(0x99E6FEFF)

// ── Particle types ─────────────────────────────────────────────
private data class StarParticle(
    var x: Float = 0f,
    var y: Float = 0f,
    val size: Float,
    val baseAlpha: Float,
    val twinkleSpeed: Float,
    val twinkleOffset: Float,
    val isTwinkler: Boolean,
    val isNebula: Boolean,
    val color: Color,
    val orbitAngle: Float,
    val orbitRadius: Float,
    val orbitSpeed: Float,
)

private data class ShootingStar(
    val startX: Float,
    val startY: Float,
    val angle: Float,
    val speed: Float,
    val length: Float,
    var life: Float = 0f,
)

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit
) {
    var phase by remember { mutableStateOf(0) }

    // Fast phase transitions so content appears quickly
    LaunchedEffect(Unit) {
        phase = 1; delay(100)   // stars + nebula glow (fast)
        phase = 2; delay(200)   // star icon
        phase = 3; delay(250)   // title
        phase = 4; delay(200)   // tagline
        phase = 5; delay(250)   // shooting stars
        phase = 6; delay(200)   // button
        delay(5000)             // hold for 5s
        onNavigateToOnboarding()
    }

    // ── Continuous animations ────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "space")

    val nebulaScale by infiniteTransition.animateFloat(
        initialValue = 0.90f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nebulaScale",
    )

    val starRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "starRotation",
    )

    val starBob by infiniteTransition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "starBob",
    )

    val ringProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "ringProgress",
    )

    val particleTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "particleTime",
    )

    // ── Phase-driven animations ──────────────────────────────────
    val starAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(12f) }
    val taglineAlpha = remember { Animatable(0f) }
    val taglineOffset = remember { Animatable(12f) }
    val buttonAlpha = remember { Animatable(0f) }

    LaunchedEffect(phase) {
        when (phase) {
            2 -> { starAlpha.snapTo(0f); starAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
            3 -> { titleAlpha.snapTo(0f); titleOffset.snapTo(16f); titleAlpha.animateTo(1f, tween(500)); titleOffset.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) }
            4 -> { taglineAlpha.snapTo(0f); taglineOffset.snapTo(16f); taglineAlpha.animateTo(1f, tween(500)); taglineOffset.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) }
            6 -> { buttonAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        }
    }

    // ── Build star field ─────────────────────────────────────────
    val stars = remember {
        val rng = Random(42)
        buildList {
            repeat(8) {
                add(StarParticle(
                    size = 2.5f + rng.nextFloat() * 3f,
                    baseAlpha = 0.25f + rng.nextFloat() * 0.2f,
                    twinkleSpeed = 0.6f + rng.nextFloat() * 0.8f,
                    twinkleOffset = rng.nextFloat() * 6.28f,
                    isTwinkler = false, isNebula = true,
                    color = listOf(CyanGlow, PurpleGlow, TealGlow, GoldGlow)[rng.nextInt(4)],
                    orbitAngle = rng.nextFloat() * 6.28f,
                    orbitRadius = 40f + rng.nextFloat() * 100f,
                    orbitSpeed = 0.15f + rng.nextFloat() * 0.25f,
                ))
            }
            repeat(50) {
                add(StarParticle(
                    size = 0.8f + rng.nextFloat() * 1.8f,
                    baseAlpha = 0.3f + rng.nextFloat() * 0.7f,
                    twinkleSpeed = 1.5f + rng.nextFloat() * 3f,
                    twinkleOffset = rng.nextFloat() * 6.28f,
                    isTwinkler = true, isNebula = false,
                    color = StarWhite,
                    orbitAngle = rng.nextFloat() * 6.28f,
                    orbitRadius = 20f + rng.nextFloat() * 180f,
                    orbitSpeed = 0.05f + rng.nextFloat() * 0.15f,
                ))
            }
            repeat(20) {
                add(StarParticle(
                    size = 1.2f + rng.nextFloat() * 1.2f,
                    baseAlpha = 0.2f + rng.nextFloat() * 0.3f,
                    twinkleSpeed = 1f + rng.nextFloat() * 1.5f,
                    twinkleOffset = rng.nextFloat() * 6.28f,
                    isTwinkler = false, isNebula = false,
                    color = StarWhite.copy(alpha = 0.5f),
                    orbitAngle = rng.nextFloat() * 6.28f,
                    orbitRadius = 50f + rng.nextFloat() * 130f,
                    orbitSpeed = 0.08f + rng.nextFloat() * 0.12f,
                ))
            }
        }
    }

    val shootingStars = remember { mutableListOf<ShootingStar>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        // ── Deep space gradient ──────────────────────────────────
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(Color(0x1A103632), Color(0x120A1822), Color(0x08000000)),
                    radius = 1000f,
                )
            )
        )

        // ── Nebula glow ───────────────────────────────────────────
        if (phase >= 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 160.dp)
                    .size(300.dp)
                    .scale(nebulaScale)
                    .alpha(0.6f)
            ) {
                Box(Modifier.size(300.dp).blur(120.dp).background(CyanGlow.copy(alpha = 0.08f), CircleShape))
                Box(Modifier.offset(x = (-40).dp, y = 20.dp).size(240.dp).blur(100.dp).background(PurpleGlow.copy(alpha = 0.10f), CircleShape))
                Box(Modifier.offset(x = 30.dp, y = (-20).dp).size(160.dp).blur(80.dp).background(GoldGlow.copy(alpha = 0.06f), CircleShape))
            }

            // Expanding rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cX = size.width / 2f
                val cY = size.height * 0.36f
                val maxR = size.width * 0.85f
                drawCircle(Color(0x0A00F4FE), ringProgress * maxR * 0.25f, Offset(cX, cY), style = Stroke(0.5f))
                drawCircle(Color(0x0800F4FE), ringProgress * maxR * 0.50f, Offset(cX, cY), style = Stroke(0.4f))
                drawCircle(Color(0x0600F4FE), ringProgress * maxR * 0.75f, Offset(cX, cY), style = Stroke(0.3f))
            }
        }

        // ── Star field ────────────────────────────────────────────
        if (phase >= 1) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cX = size.width / 2f
                val cY = size.height * 0.36f
                val t = particleTime

                val positions = stars.map { s ->
                    val angle = s.orbitAngle + t * 0.001f * s.orbitSpeed
                    Offset(cX + cos(angle) * s.orbitRadius, cY + sin(angle) * s.orbitRadius * 0.55f)
                }

                stars.forEachIndexed { idx, s ->
                    val pos = positions[idx]
                    val twinkle = 0.5f + 0.5f * sin(t * 0.005f * s.twinkleSpeed + s.twinkleOffset)
                    val fadeAlpha = s.baseAlpha * if (s.isTwinkler) twinkle else 1f

                    if (s.isTwinkler) {
                        val cs = s.size
                        drawLine(s.color.copy(alpha = fadeAlpha * 0.6f), Offset(pos.x - cs, pos.y), Offset(pos.x + cs, pos.y), strokeWidth = 0.8f)
                        drawLine(s.color.copy(alpha = fadeAlpha * 0.6f), Offset(pos.x, pos.y - cs), Offset(pos.x, pos.y + cs), strokeWidth = 0.8f)
                        drawCircle(s.color.copy(alpha = fadeAlpha), 0.6f, pos)
                    } else if (s.isNebula) {
                        drawCircle(s.color.copy(alpha = fadeAlpha * 0.5f), s.size, pos)
                    } else {
                        drawCircle(s.color.copy(alpha = fadeAlpha * 0.4f), s.size, pos)
                    }
                }

                // Shooting stars
                if (phase >= 5) {
                    if ((t * 0.2f).toInt() % 15 == 0 && shootingStars.size < 3) {
                        val rng = Random(t.toInt() + 1000)
                        shootingStars.add(ShootingStar(
                            startX = cX + (rng.nextFloat() - 0.5f) * size.width * 0.6f,
                            startY = -20f, angle = 0.6f + rng.nextFloat() * 0.5f,
                            speed = 400f + rng.nextFloat() * 300f, length = 40f + rng.nextFloat() * 60f,
                        ))
                    }
                    val toRemove = mutableListOf<ShootingStar>()
                    for (ss in shootingStars) {
                        ss.life += 0.03f
                        if (ss.life >= 1f) { toRemove.add(ss); continue }
                        val sx = ss.startX + cos(ss.angle) * ss.speed * ss.life
                        val sy = ss.startY + sin(ss.angle) * ss.speed * ss.life
                        val tx = ss.startX + cos(ss.angle) * (ss.speed * ss.life - ss.length)
                        val ty = ss.startY + sin(ss.angle) * (ss.speed * ss.life - ss.length)
                        val ssAlpha = (1f - ss.life).coerceIn(0f, 0.6f)
                        drawLine(ShootingStarColor.copy(alpha = ssAlpha), Offset(tx, ty), Offset(sx, sy), strokeWidth = 1.5f)
                        drawCircle(ShootingStarColor.copy(alpha = ssAlpha + 0.2f), 2f, Offset(sx, sy))
                    }
                    shootingStars.removeAll(toRemove)
                }
            }
        }

        // ── Main layout: content centered + button at bottom ─────────
        // Uses a Column with weight so the content fills space above the button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Content area (centered within available space) ──
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Star icon
                    if (phase >= 2) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .alpha(starAlpha.value)
                                .rotate(starRotation)
                                .offset(y = starBob.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Brush.linearGradient(listOf(Color(0x281A3C38), Color(0x180B221F))))
                                    .blur(3.dp)
                            )
                            Text(
                                text = "\u2606",
                                style = MaterialTheme.typography.displayLarge,
                                color = SecondaryContainer.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.scale(1.4f)
                            )
                        }
                    }

                    if (phase >= 2) Spacer(modifier = Modifier.height(20.dp))

                    // Title
                    if (phase >= 3) {
                        Text(
                            text = "Curio",
                            style = MaterialTheme.typography.displayLarge,
                            color = OnSurface,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .alpha(titleAlpha.value)
                                .offset(y = titleOffset.value.dp)
                        )
                    }

                    if (phase >= 3) Spacer(modifier = Modifier.height(10.dp))

                    // Tagline
                    if (phase >= 4) {
                        Text(
                            text = "One interesting thing at a time.",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceVariant.copy(alpha = 0.80f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .alpha(taglineAlpha.value)
                                .offset(y = taglineOffset.value.dp)
                        )
                    }
                }
            }

            // ── Button at bottom ──
            if (phase >= 6) {
                Button(
                    onClick = onNavigateToOnboarding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .alpha(buttonAlpha.value),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryContainer,
                        contentColor = Color(0xFF002021),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                ) {
                    Text("Get Started", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
