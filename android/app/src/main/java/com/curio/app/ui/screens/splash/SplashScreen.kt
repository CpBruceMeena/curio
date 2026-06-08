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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlin.math.pow
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
    val isTwinkler: Boolean,     // small white stars that twinkle
    val isNebula: Boolean,       // larger coloured specks
    val color: Color,
    val orbitAngle: Float,
    val orbitRadius: Float,
    val orbitSpeed: Float,
)

private data class ShootingStar(
    val startX: Float,
    val startY: Float,
    val angle: Float,        // direction in radians
    val speed: Float,
    val length: Float,
    var life: Float = 0f,    // 0..1
)

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit
) {
    // ── Phase state machine ──────────────────────────────────────
    var phase by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        phase = 1; delay(200)   // stars + nebula glow
        phase = 2; delay(300)   // star icon
        phase = 3; delay(400)   // title
        phase = 4; delay(350)   // tagline
        phase = 5; delay(400)   // shooting stars
        phase = 6; delay(400)   // button
        delay(1200)
        onNavigateToOnboarding()
    }

    // ── Continuous animations ────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "space")

    // Nebula pulse
    val nebulaScale by infiniteTransition.animateFloat(
        initialValue = 0.90f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nebulaScale",
    )

    // Star icon rotation (slow)
    val starRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "starRotation",
    )

    // Star gentle bob
    val starBob by infiniteTransition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "starBob",
    )

    // Expanding rings
    val ringProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "ringProgress",
    )

    // Particle time
    val particleTime by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "particleTime",
    )

    // ── Phase-driven animatables ─────────────────────────────────
    val starAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(12f) }
    val taglineAlpha = remember { Animatable(0f) }
    val taglineOffset = remember { Animatable(12f) }
    val buttonAlpha = remember { Animatable(0f) }

    LaunchedEffect(phase) {
        when (phase) {
            2 -> { starAlpha.snapTo(0f); starAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
            3 -> { titleAlpha.snapTo(0f); titleOffset.snapTo(16f); titleAlpha.animateTo(1f, tween(600)); titleOffset.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
            4 -> { taglineAlpha.snapTo(0f); taglineOffset.snapTo(16f); taglineAlpha.animateTo(1f, tween(600)); taglineOffset.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
            6 -> { buttonAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
        }
    }

    // ── Build star field ─────────────────────────────────────────
    val stars = remember {
        val rng = Random(42)
        buildList {
            // Large nebulous coloured specks (8)
            repeat(8) {
                add(StarParticle(
                    size = 2.5f + rng.nextFloat() * 3f,
                    baseAlpha = 0.25f + rng.nextFloat() * 0.2f,
                    twinkleSpeed = 0.6f + rng.nextFloat() * 0.8f,
                    twinkleOffset = rng.nextFloat() * 6.28f,
                    isTwinkler = false,
                    isNebula = true,
                    color = listOf(CyanGlow, PurpleGlow, TealGlow, GoldGlow)[rng.nextInt(4)],
                    orbitAngle = rng.nextFloat() * 6.28f,
                    orbitRadius = 40f + rng.nextFloat() * 100f,
                    orbitSpeed = 0.15f + rng.nextFloat() * 0.25f,
                ))
            }
            // Tiny twinkling stars (50)
            repeat(50) {
                add(StarParticle(
                    size = 0.8f + rng.nextFloat() * 1.8f,
                    baseAlpha = 0.3f + rng.nextFloat() * 0.7f,
                    twinkleSpeed = 1.5f + rng.nextFloat() * 3f,
                    twinkleOffset = rng.nextFloat() * 6.28f,
                    isTwinkler = true,
                    isNebula = false,
                    color = StarWhite,
                    orbitAngle = rng.nextFloat() * 6.28f,
                    orbitRadius = 20f + rng.nextFloat() * 180f,
                    orbitSpeed = 0.05f + rng.nextFloat() * 0.15f,
                ))
            }
            // Medium dim stars for constellation lines (20)
            repeat(20) {
                add(StarParticle(
                    size = 1.2f + rng.nextFloat() * 1.2f,
                    baseAlpha = 0.2f + rng.nextFloat() * 0.3f,
                    twinkleSpeed = 1f + rng.nextFloat() * 1.5f,
                    twinkleOffset = rng.nextFloat() * 6.28f,
                    isTwinkler = false,
                    isNebula = false,
                    color = StarWhite.copy(alpha = 0.5f),
                    orbitAngle = rng.nextFloat() * 6.28f,
                    orbitRadius = 50f + rng.nextFloat() * 130f,
                    orbitSpeed = 0.08f + rng.nextFloat() * 0.12f,
                ))
            }
        }
    }

    // ── Shooting star generator ──────────────────────────────────
    val shootingStars = remember {
        mutableListOf<ShootingStar>()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .statusBarsPadding()
    ) {
        // ── Deep space gradient background ─────────────────────────
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x1A103632),  // teal centre
                        Color(0x120A1822),  // purple mid
                        Color(0x08000000),  // deep void
                    ),
                    radius = 1000f,
                )
            )
        )

        // ── Nebula glow ────────────────────────────────────────────
        if (phase >= 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 160.dp)
                    .size(300.dp)
                    .scale(nebulaScale)
                    .alpha(0.6f)
            ) {
                // Cyan nebula
                Box(
                    modifier = Modifier
                        .size(300.dp).blur(120.dp)
                        .background(CyanGlow.copy(alpha = 0.08f), CircleShape)
                )
                // Purple nebula (offset)
                Box(
                    modifier = Modifier
                        .offset(x = (-40).dp, y = 20.dp)
                        .size(240.dp).blur(100.dp)
                        .background(PurpleGlow.copy(alpha = 0.10f), CircleShape)
                )
                // Gold core
                Box(
                    modifier = Modifier
                        .offset(x = 30.dp, y = (-20).dp)
                        .size(160.dp).blur(80.dp)
                        .background(GoldGlow.copy(alpha = 0.06f), CircleShape)
                )
            }

            // ── Expanding rings ────────────────────────────────────
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cX = size.width / 2f
                val cY = size.height * 0.36f
                val maxR = size.width * 0.85f
                val r1 = ringProgress * maxR * 0.25f
                val r2 = ringProgress * maxR * 0.50f
                val r3 = ringProgress * maxR * 0.75f

                drawCircle(Color(0x0A00F4FE), r1, Offset(cX, cY), style = Stroke(0.5f))
                drawCircle(Color(0x0800F4FE), r2, Offset(cX, cY), style = Stroke(0.4f))
                drawCircle(Color(0x0600F4FE), r3, Offset(cX, cY), style = Stroke(0.3f))
            }
        }

        // ── Star field + constellation lines ───────────────────────
        if (phase >= 1) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cX = size.width / 2f
                val cY = size.height * 0.36f
                val t = particleTime

                // Compute star positions
                val positions = stars.map { s ->
                    val angle = s.orbitAngle + t * 0.001f * s.orbitSpeed
                    val px = cX + cos(angle) * s.orbitRadius
                    val py = cY + sin(angle) * s.orbitRadius * 0.55f
                    Offset(px, py)
                }

                // Constellation lines between medium-dim stars (indices 58..77)
                val constStars = stars.indices.filter { it in 58..<78 && it < positions.size }
                for (i in constStars.indices) {
                    for (j in i + 1 until constStars.size) {
                        val a = positions[constStars[i]]
                        val b = positions[constStars[j]]
                        val dist = (a - b).getDistance()
                        if (dist < 120f) {
                            val lineAlpha = 0.08f * (1f - dist / 120f)
                            drawLine(
                                StarWhite.copy(alpha = lineAlpha),
                                a, b, strokeWidth = 0.5f,
                            )
                        }
                    }
                }

                // Draw stars
                stars.forEachIndexed { idx, s ->
                    val pos = positions[idx]
                    val twinkle = 0.5f + 0.5f * sin(t * 0.005f * s.twinkleSpeed + s.twinkleOffset)
                    val fadeAlpha = s.baseAlpha * if (s.isTwinkler) twinkle else 1f

                    if (s.isTwinkler) {
                        // Tiny twinkling star (draw as 4-point cross)
                        val crossSize = s.size
                        drawLine(s.color.copy(alpha = fadeAlpha * 0.6f),
                            Offset(pos.x - crossSize, pos.y), Offset(pos.x + crossSize, pos.y),
                            strokeWidth = 0.8f)
                        drawLine(s.color.copy(alpha = fadeAlpha * 0.6f),
                            Offset(pos.x, pos.y - crossSize), Offset(pos.x, pos.y + crossSize),
                            strokeWidth = 0.8f)
                        // Core dot
                        drawCircle(s.color.copy(alpha = fadeAlpha), 0.6f, pos)
                    } else if (s.isNebula) {
                        drawCircle(s.color.copy(alpha = fadeAlpha * 0.5f), s.size, pos)
                    } else {
                        drawCircle(s.color.copy(alpha = fadeAlpha * 0.4f), s.size, pos)
                    }
                }

                // ── Shooting stars ─────────────────────────────────
                if (phase >= 5) {
                    // Periodically spawn a new shooting star
                    val spawnPhase = (t * 0.2f).toInt() % 15
                    if (spawnPhase == 0 && shootingStars.size < 3) {
                        val rng = Random(t.toInt() + 1000)
                        shootingStars.add(ShootingStar(
                            startX = cX + (rng.nextFloat() - 0.5f) * size.width * 0.6f,
                            startY = -20f,
                            angle = 0.6f + rng.nextFloat() * 0.5f,
                            speed = 400f + rng.nextFloat() * 300f,
                            length = 40f + rng.nextFloat() * 60f,
                        ))
                    }

                    val toRemove = mutableListOf<ShootingStar>()
                    for (ss in shootingStars) {
                        ss.life += 0.03f
                        if (ss.life >= 1f) { toRemove.add(ss); continue }
                        val sx = ss.startX + cos(ss.angle) * ss.speed * ss.life
                        val sy = ss.startY + sin(ss.angle) * ss.speed * ss.life
                        val tailX = ss.startX + cos(ss.angle) * (ss.speed * ss.life - ss.length)
                        val tailY = ss.startY + sin(ss.angle) * (ss.speed * ss.life - ss.length)
                        val ssAlpha = (1f - ss.life).coerceIn(0f, 0.6f)
                        drawLine(
                            ShootingStarColor.copy(alpha = ssAlpha),
                            Offset(tailX, tailY), Offset(sx, sy),
                            strokeWidth = 1.5f,
                        )
                        drawCircle(ShootingStarColor.copy(alpha = ssAlpha + 0.2f), 2f, Offset(sx, sy))
                    }
                    shootingStars.removeAll(toRemove)
                }
            }
        }

        // ── Scrollable content layer ───────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // ── Star icon ───────────────────────────────────────────
            if (phase >= 2) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .alpha(starAlpha.value)
                        .rotate(starRotation)
                        .offset(y = starBob.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Glass orb
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0x281A3C38), Color(0x180B221F))
                                )
                            ).blur(3.dp)
                    )
                    // Star symbol
                    Text(
                        text = "\u2606",
                        style = MaterialTheme.typography.displayLarge,
                        color = SecondaryContainer.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.scale(1.4f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Title ───────────────────────────────────────────────
            if (phase >= 3) {
                Text(
                    text = "Curio",
                    style = MaterialTheme.typography.displayLarge,
                    color = OnSurface,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(titleAlpha.value)
                        .offset(y = titleOffset.value.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Tagline ─────────────────────────────────────────────
            if (phase >= 4) {
                Text(
                    text = "One interesting thing at a time.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(taglineAlpha.value)
                        .offset(y = taglineOffset.value.dp),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Get Started button ──────────────────────────────────
            if (phase >= 6) {
                Button(
                    onClick = onNavigateToOnboarding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
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
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
