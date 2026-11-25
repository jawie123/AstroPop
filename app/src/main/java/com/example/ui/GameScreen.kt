package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BoosterType
import com.example.data.CannonSkin
import com.example.data.GameMode
import com.example.game.BossSpec
import com.example.game.BubbleColor
import com.example.game.GameStatus
import com.example.game.GameUiState
import com.example.game.GameViewModel
import com.example.game.HexGridEngine
import com.example.game.LauncherShot
import com.example.game.LevelCatalog
import com.example.game.LevelGoal
import com.example.game.ShotType
import com.example.game.TileKind
import com.example.ui.theme.AstroCyan
import com.example.ui.theme.AstroEmerald
import com.example.ui.theme.AstroGold
import com.example.ui.theme.AstroMagenta
import com.example.ui.theme.AstroViolet
import com.example.ui.theme.CosmicAbyss
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.NebulaElevated
import com.example.ui.theme.NebulaSurface
import com.example.ui.theme.StarlightMuted
import com.example.ui.theme.StarlightWhite
import com.example.ui.theme.StardustOutline
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    isRewardedAdReady: Boolean,
    onWatchAdSecondChance: () -> Unit,
    onExitToMap: () -> Unit,
    onNextLevel: (Int) -> Unit,
    onRetry: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    BackHandler {
        if (state.status == GameStatus.PLAYING) {
            viewModel.togglePause()
        } else {
            if (state.status == GameStatus.LOST) {
                viewModel.finalizeDefeatWithoutSecondChance()
            }
            onExitToMap()
        }
    }

    LaunchedEffect(state.hapticPulseId) {
        if (state.hapticPulseId > 0L && state.hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0618), Color(0xFF150D32), Color(0xFF1E1245))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
        ) {
            GameHudBar(
                state = state,
                onPause = viewModel::togglePause,
                onToggleSound = viewModel::toggleSound
            )

            // Fever / Overdrive & Objective strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val inOverdrive = state.overdriveTurnsRemaining > 0
                Surface(
                    color = if (inOverdrive) AstroGold.copy(alpha = 0.22f) else NebulaSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (inOverdrive) 1.5.dp else 0.dp,
                            color = if (inOverdrive) AstroGold else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (inOverdrive) "OVERDRIVE 2X ACTIVE" else "FEVER CHARGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (inOverdrive) AstroGold else AstroMagenta
                            )
                            Text(
                                text = if (inOverdrive) "${state.overdriveTurnsRemaining}T" else "${state.feverProgress}/5",
                                style = MaterialTheme.typography.labelSmall,
                                color = StarlightWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { if (inOverdrive) 1f else (state.feverProgress / 5f).coerceIn(0f, 1f) },
                            color = if (inOverdrive) AstroGold else AstroMagenta,
                            trackColor = StardustOutline.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }

                Surface(
                    color = NebulaSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val goalText = when {
                        state.mode == GameMode.ENDLESS -> "Drop in ${state.endlessShotsUntilDescent}"
                        state.levelSpec.goal == LevelGoal.RESCUE_STARS -> "Stars left: ${state.starsRemaining}"
                        else -> "Clear Board"
                    }
                    Text(
                        text = goalText,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (state.isDangerPanic) DangerCrimson else AstroGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            // Boss Guardian Status Bar (Stages 6, 12, 18)
            if (state.bossMaxHp > 0) {
                val bossAccent = state.levelSpec.bossSpec?.accentColor ?: DangerCrimson
                val bossProgress = (state.bossHp.toFloat() / state.bossMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
                Surface(
                    color = NebulaSurface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                        .border(1.dp, bossAccent.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .testTag("boss_hud_bar")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = "Sector Boss",
                                    tint = bossAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = state.bossName ?: "Cosmic Guardian",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = StarlightWhite
                                )
                            }
                            Text(
                                text = if (state.bossHp > 0) {
                                    "HP ${state.bossHp}/${state.bossMaxHp} • Curse in ${state.bossCounterCountdown}"
                                } else {
                                    "SHATTERED!"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.bossCounterCountdown <= 1 && state.bossHp > 0) DangerCrimson else AstroGold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { bossProgress },
                            color = bossAccent,
                            trackColor = StardustOutline.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
            }

            // Main Playable Arena (isolated Canvas draw state for zero-recomposition 60fps)
            GameArenaCanvas(
                viewModel = viewModel,
                currentShot = state.currentShot,
                nextShot = state.nextShot,
                thirdShot = state.thirdShot,
                equippedSkin = state.equippedSkin,
                bossSpec = state.levelSpec.bossSpec,
                bossHp = state.bossHp,
                isDangerPanic = state.isDangerPanic,
                inOverdrive = state.overdriveTurnsRemaining > 0,
                pulseState = pulseState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            // Bottom Control Dock: Swap Button + Tactical Boosters (with mid-game quick-buy)
            BottomBoosterDock(
                state = state,
                onSwap = viewModel::swapShooterBubbles,
                onActivateBooster = viewModel::activateBooster,
                onBuyBooster = viewModel::buyBoosterMidGame
            )
        }

        // Pause / Victory / Defeat Overlays
        if (state.status != GameStatus.PLAYING) {
            GameOverlayModal(
                state = state,
                isRewardedAdReady = isRewardedAdReady,
                onResume = viewModel::togglePause,
                onSecondChance = viewModel::buySecondChanceExtraShots,
                onWatchAdSecondChance = onWatchAdSecondChance,
                onRetry = {
                    if (state.status == GameStatus.LOST) {
                        viewModel.finalizeDefeatWithoutSecondChance()
                    }
                    onRetry()
                },
                onNextLevel = {
                    onNextLevel((state.levelSpec.levelNumber + 1).coerceAtMost(LevelCatalog.sagaLevels.size))
                },
                onExit = {
                    if (state.status == GameStatus.LOST) {
                        viewModel.finalizeDefeatWithoutSecondChance()
                    }
                    onExitToMap()
                }
            )
        }
    }
}

@Composable
private fun GameArenaCanvas(
    viewModel: GameViewModel,
    currentShot: LauncherShot,
    nextShot: LauncherShot,
    thirdShot: LauncherShot,
    equippedSkin: CannonSkin,
    bossSpec: BossSpec?,
    bossHp: Int,
    isDangerPanic: Boolean,
    inOverdrive: Boolean,
    pulseState: State<Float>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val radius = HexGridEngine.bubbleRadius(widthPx)
        val topOffset = with(density) { 8.dp.toPx() }
        val shooterY = (heightPx - radius * 2.1f).coerceAtLeast(heightPx * 0.80f)

        LaunchedEffect(widthPx, heightPx) {
            viewModel.updateArenaMetrics(widthPx, heightPx, topOffset, shooterY)
        }

        val stars = remember(widthPx, heightPx) {
            val rng = Random(77)
            List(42) {
                Triple(
                    rng.nextFloat() * widthPx,
                    rng.nextFloat() * heightPx,
                    1f + rng.nextFloat() * 2.2f
                )
            }
        }
        val textMeasurer = rememberTextMeasurer()
        val reusablePath = remember { Path() }
        val rayDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 14f), 0f) }
        val dangerDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f) }
        val arenaBackdrop = remember {
            Brush.verticalGradient(listOf(Color(0xFF0E0823), Color(0xFF170F38)))
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> viewModel.onAimDrag(offset) },
                        onDrag = { change, _ ->
                            change.consume()
                            viewModel.onAimDrag(change.position)
                        },
                        onDragEnd = { viewModel.onAimRelease() },
                        onDragCancel = { viewModel.onAimRelease() }
                    )
                }
                .pointerInput(shooterY, widthPx) {
                    detectTapGestures { offset ->
                        val distToShooter = hypot(offset.x - widthPx / 2f, offset.y - shooterY)
                        if (distToShooter <= radius * 2.3f) {
                            viewModel.swapShooterBubbles()
                        } else {
                            viewModel.onAimDrag(offset)
                            viewModel.onAimRelease()
                        }
                    }
                }
                .testTag("game_arena_canvas")
        ) {
            // Read snapshot states strictly inside Canvas DrawScope for zero recomposition!
            val rf = viewModel.renderState.value
            val pulse = pulseState.value

            val shakeX = if (rf.screenShake > 0.2f) (Random.nextFloat() - 0.5f) * rf.screenShake else 0f
            val shakeY = if (rf.screenShake > 0.2f) (Random.nextFloat() - 0.5f) * rf.screenShake else 0f

            // Arena backdrop
            drawRect(brush = arenaBackdrop)

            // Subtle twinkling starfield
            for ((sx, sy, sr) in stars) {
                drawCircle(
                    color = StarlightWhite.copy(alpha = 0.16f + 0.22f * pulse),
                    radius = sr,
                    center = Offset(sx, sy)
                )
            }

            // Overdrive / Panic vignette
            if (isDangerPanic) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, DangerCrimson.copy(alpha = 0.14f + pulse * 0.14f))
                    )
                )
            } else if (inOverdrive) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(AstroGold.copy(alpha = 0.06f + pulse * 0.06f), Color.Transparent)
                    )
                )
            }

            // Side wall rail glows
            val leftWallX = (widthPx - HexGridEngine.COLS_EVEN * 2f * radius) / 2f
            val rightWallX = widthPx - leftWallX
            drawLine(
                color = AstroCyan.copy(alpha = 0.25f),
                start = Offset(leftWallX, 0f),
                end = Offset(leftWallX, size.height),
                strokeWidth = 2f
            )
            drawLine(
                color = AstroCyan.copy(alpha = 0.25f),
                start = Offset(rightWallX, 0f),
                end = Offset(rightWallX, size.height),
                strokeWidth = 2f
            )

            // Danger laser line
            val dangerY = topOffset + HexGridEngine.DANGER_ROW * HexGridEngine.rowSpacing(radius)
            drawLine(
                color = if (isDangerPanic) DangerCrimson.copy(alpha = 0.6f + pulse * 0.4f) else DangerCrimson.copy(alpha = 0.28f),
                start = Offset(leftWallX, dangerY),
                end = Offset(rightWallX, dangerY),
                strokeWidth = 3f,
                pathEffect = dangerDashEffect
            )

            // Score wells at bottom
            val wellY = (shooterY - radius * 2.2f).coerceAtLeast(heightPx * 0.72f)
            val wellMultipliers = listOf("x1", "x2", "x5", "x2", "x1")
            val bucketW = widthPx / 5f
            for (i in 0 until 5) {
                val bx = i * bucketW
                val isCenter = i == 2
                drawRoundRect(
                    color = if (isCenter) AstroGold.copy(alpha = 0.14f) else AstroCyan.copy(alpha = 0.08f),
                    topLeft = Offset(bx + 4f, wellY),
                    size = Size(bucketW - 8f, 28f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = wellMultipliers[i],
                    topLeft = Offset(bx + bucketW / 2f - 14f, wellY + 4f),
                    style = TextStyle(
                        color = if (isCenter) AstroGold else StarlightMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FredokaFontFamily
                    )
                )
            }

            // Cannon skin color palette
            val skinPrimary = when (equippedSkin) {
                CannonSkin.CYAN_INTERCEPTOR -> AstroCyan
                CannonSkin.SOLAR_VALKYRIE -> AstroGold
                CannonSkin.PLASMA_NOVA -> AstroMagenta
                CannonSkin.VOID_SINGULARITY -> AstroViolet
            }
            val skinTrim = when (equippedSkin) {
                CannonSkin.CYAN_INTERCEPTOR -> Color(0xFF0E2B44)
                CannonSkin.SOLAR_VALKYRIE -> Color(0xFF3E240A)
                CannonSkin.PLASMA_NOVA -> Color(0xFF3D0C30)
                CannonSkin.VOID_SINGULARITY -> Color(0xFF1D0D3B)
            }

            // Boss Guardian Aura & Crest at Top Arena
            if (bossSpec != null && bossHp > 0) {
                val bossCenter = Offset(widthPx / 2f, topOffset + radius * 0.45f)
                val hitBoost = rf.bossHitFlash.coerceIn(0f, 1f)
                val crestColor = if (hitBoost > 0.25f) Color.White else bossSpec.accentColor
                drawCircle(
                    color = bossSpec.accentColor.copy(alpha = 0.14f + pulse * 0.12f + hitBoost * 0.35f),
                    radius = radius * (2.6f + hitBoost * 0.6f),
                    center = bossCenter
                )
                drawCircle(
                    color = crestColor.copy(alpha = 0.72f),
                    radius = radius * 1.4f,
                    center = bossCenter,
                    style = Stroke(width = 3.5f + hitBoost * 3f)
                )
                drawCircle(
                    color = Color(0xFF0B051A),
                    radius = radius * 0.92f,
                    center = bossCenter
                )
                drawCircle(
                    color = crestColor.copy(alpha = 0.75f + pulse * 0.25f),
                    radius = radius * 0.45f,
                    center = bossCenter
                )
            }

            // Aiming trajectory & snap ghost
            val traj = rf.trajectory
            if (rf.flyingBubble == null && !traj.isCancelled && traj.points.size >= 2) {
                val rayColor = when (currentShot.shotType) {
                    ShotType.BOMB -> AstroMagenta
                    ShotType.LASER -> AstroCyan
                    ShotType.PRISM -> AstroGold
                    ShotType.COLORED -> if (equippedSkin == CannonSkin.CYAN_INTERCEPTOR) currentShot.color.primary else currentShot.color.primary
                }
                for (i in 0 until traj.points.size - 1) {
                    if (equippedSkin != CannonSkin.CYAN_INTERCEPTOR) {
                        drawLine(
                            color = skinPrimary.copy(alpha = 0.28f),
                            start = traj.points[i],
                            end = traj.points[i + 1],
                            strokeWidth = 10f,
                            cap = StrokeCap.Round
                        )
                    }
                    drawLine(
                        color = rayColor.copy(alpha = 0.75f),
                        start = traj.points[i],
                        end = traj.points[i + 1],
                        strokeWidth = if (currentShot.shotType == ShotType.LASER) 7f else 5f,
                        cap = StrokeCap.Round,
                        pathEffect = rayDashEffect
                    )
                }
                traj.snapCell?.let { (sr, sc) ->
                    val snapCenter = HexGridEngine.cellCenter(sr, sc, widthPx, radius, topOffset)
                    drawCircle(
                        color = rayColor.copy(alpha = 0.4f + pulse * 0.3f),
                        radius = radius * 0.92f,
                        center = snapCenter,
                        style = Stroke(width = 3.5f)
                    )
                }
            }

            // Grid bubbles
            for ((key, cell) in rf.grid) {
                val baseCenter = HexGridEngine.cellCenter(key.first, key.second, widthPx, radius, topOffset)
                val center = Offset(baseCenter.x + shakeX, baseCenter.y + shakeY)
                val wobbleScale = 1f + sin(cell.wobble * PI.toFloat() * 3f) * 0.12f * cell.wobble
                val isHighlighted = !traj.isCancelled && key in traj.highlightCells
                drawCosmicBubble(
                    center = center,
                    radius = radius * 0.94f * wobbleScale,
                    color = cell.color,
                    kind = cell.kind,
                    charge = cell.charge,
                    pulse = pulse,
                    highlighted = isHighlighted,
                    reusablePath = reusablePath
                )
            }

            // Falling detached bubbles
            for (fb in rf.fallingBubbles) {
                rotate(degrees = fb.rotation, pivot = Offset(fb.x, fb.y)) {
                    drawCosmicBubble(
                        center = Offset(fb.x, fb.y),
                        radius = radius * 0.9f,
                        color = fb.color,
                        kind = fb.kind,
                        charge = 0,
                        pulse = pulse,
                        highlighted = false,
                        reusablePath = reusablePath
                    )
                }
            }

            // Flying projectile + trail
            rf.flyingBubble?.let { fly ->
                fly.trail.forEachIndexed { idx, pt ->
                    val frac = (idx + 1).toFloat() / (fly.trail.size + 1)
                    drawCircle(
                        color = fly.shot.color.primary.copy(alpha = frac * 0.45f),
                        radius = radius * 0.45f * frac,
                        center = pt
                    )
                }
                drawLauncherProjectile(
                    center = Offset(fly.x, fly.y),
                    radius = radius * 0.95f,
                    shot = fly.shot,
                    pulse = pulse,
                    reusablePath = reusablePath
                )
            }

            // Portal warp shockwaves
            for (pfx in rf.portalFx) {
                val a = pfx.life.coerceIn(0f, 1f)
                val expand = (1f - a) * radius * 1.8f
                drawLine(
                    color = AstroCyan.copy(alpha = a * 0.5f),
                    start = pfx.from,
                    end = pfx.to,
                    strokeWidth = 3.5f,
                    pathEffect = rayDashEffect
                )
                drawCircle(
                    color = AstroMagenta.copy(alpha = a * 0.8f),
                    radius = radius + expand,
                    center = pfx.from,
                    style = Stroke(width = 4f)
                )
                drawCircle(
                    color = AstroCyan.copy(alpha = a * 0.9f),
                    radius = radius + expand,
                    center = pfx.to,
                    style = Stroke(width = 4f)
                )
            }

            // Lightning row-zapper electric arcs
            for (arc in rf.lightningArcs) {
                val a = arc.life.coerceIn(0f, 1f)
                reusablePath.reset()
                val segments = 12
                val span = (arc.rightX - arc.leftX) / segments
                reusablePath.moveTo(arc.leftX, arc.y)
                for (i in 1 until segments) {
                    val jitter = ((i % 2) * 2 - 1) * (10f + pulse * 8f)
                    reusablePath.lineTo(arc.leftX + i * span, arc.y + jitter)
                }
                reusablePath.lineTo(arc.rightX, arc.y)
                drawPath(reusablePath, color = AstroCyan.copy(alpha = a), style = Stroke(width = 9f, cap = StrokeCap.Round))
                drawPath(reusablePath, color = Color.White.copy(alpha = a), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
            }

            // Expanding radial shockwave FX
            for (sw in rf.shockwaves) {
                val a = sw.life.coerceIn(0f, 1f)
                val progress = 1f - a
                val r = sw.maxRadius * (0.2f + progress * 0.8f)
                drawCircle(
                    color = sw.color.copy(alpha = a * 0.85f),
                    radius = r,
                    center = sw.center,
                    style = Stroke(width = 6f * a + 1.5f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = a * 0.45f),
                    radius = r * 0.72f,
                    center = sw.center,
                    style = Stroke(width = 2.5f)
                )
            }

            // Particles
            for (p in rf.particles) {
                drawCircle(
                    color = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                    radius = p.radius * p.life,
                    center = Offset(p.x, p.y)
                )
            }

            // Shooter Cannon Base & Barrel (styled by equipped CannonSkin)
            val shooterCenter = Offset(widthPx / 2f, shooterY)
            rotate(
                degrees = Math.toDegrees(rf.aimAngleRad.toDouble()).toFloat() + 90f,
                pivot = shooterCenter
            ) {
                drawRoundRect(
                    color = skinTrim,
                    topLeft = Offset(shooterCenter.x - 15f, shooterCenter.y - radius * 1.5f),
                    size = Size(30f, radius * 1.5f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = if (traj.isCancelled) StarlightMuted.copy(alpha = 0.45f) else skinPrimary.copy(alpha = 0.85f),
                    topLeft = Offset(shooterCenter.x - 15f, shooterCenter.y - radius * 1.5f),
                    size = Size(30f, radius * 1.5f),
                    cornerRadius = CornerRadius(12f, 12f),
                    style = Stroke(width = 2.8f)
                )
            }
            drawCircle(
                color = NebulaSurface,
                radius = radius * 1.28f,
                center = shooterCenter
            )
            drawCircle(
                color = if (inOverdrive) AstroGold.copy(alpha = 0.9f) else skinPrimary.copy(alpha = 0.78f),
                radius = radius * 1.28f,
                center = shooterCenter,
                style = Stroke(width = if (inOverdrive) 4.5f else 3.4f)
            )
            if (rf.flyingBubble == null) {
                drawLauncherProjectile(
                    center = shooterCenter,
                    radius = radius * 0.94f,
                    shot = currentShot,
                    pulse = pulse,
                    reusablePath = reusablePath
                )
            }

            // Next Bubble Preview Orb (tap-to-swap!) + 3rd Upcoming Orb
            val nextCenter = Offset(shooterCenter.x - radius * 3.1f, shooterY + radius * 0.25f)
            drawCircle(
                color = NebulaElevated,
                radius = radius * 0.85f,
                center = nextCenter
            )
            drawLauncherProjectile(
                center = nextCenter,
                radius = radius * 0.68f,
                shot = nextShot,
                pulse = pulse,
                reusablePath = reusablePath
            )

            val thirdCenter = Offset(shooterCenter.x - radius * 5.15f, shooterY + radius * 0.42f)
            drawCircle(
                color = NebulaSurface,
                radius = radius * 0.58f,
                center = thirdCenter
            )
            drawLauncherProjectile(
                center = thirdCenter,
                radius = radius * 0.46f,
                shot = thirdShot,
                pulse = pulse,
                reusablePath = reusablePath
            )

            // Floating score popups
            for (pop in rf.popups) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = pop.text,
                    topLeft = Offset((pop.x - 65f).coerceIn(8f, widthPx - 140f), pop.y),
                    style = TextStyle(
                        color = pop.color.copy(alpha = pop.life.coerceIn(0f, 1f)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FredokaFontFamily
                    )
                )
            }

            // Outer arena border drawn inside Canvas so panic pulse doesn't recompose
            drawRoundRect(
                color = if (isDangerPanic) DangerCrimson.copy(alpha = 0.45f + pulse * 0.5f)
                else if (inOverdrive) AstroGold.copy(alpha = 0.5f + pulse * 0.3f)
                else StardustOutline.copy(alpha = 0.5f),
                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                style = Stroke(width = if (isDangerPanic || inOverdrive) 3.5.dp.toPx() else 1.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun GameHudBar(
    state: GameUiState,
    onPause: () -> Unit,
    onToggleSound: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onPause,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NebulaSurface)
                    .testTag("pause_game_button")
            ) {
                Icon(Icons.Rounded.Pause, contentDescription = "Pause game", tint = StarlightWhite)
            }
            Column {
                val stageTitle = when (state.mode) {
                    GameMode.ENDLESS -> "Endless Panic"
                    GameMode.DAILY -> "Daily Nebula"
                    GameMode.SAGA -> "Stage ${state.levelSpec.levelNumber}"
                }
                Text(
                    text = stageTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = StarlightWhite
                )
                Text(
                    text = if (state.comboStreak >= 2) "${state.comboStreak}x Streak!" else "Score: ${state.score}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (state.comboStreak >= 2) AstroGold else AstroCyan
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = NebulaSurface,
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Toll, contentDescription = "Star Coins", tint = AstroGold, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${state.starCoins}",
                        style = MaterialTheme.typography.labelLarge,
                        color = AstroGold
                    )
                }
            }

            if (state.mode == GameMode.SAGA || state.mode == GameMode.DAILY) {
                Surface(
                    color = if (state.movesRemaining <= 5) DangerCrimson.copy(alpha = 0.25f) else NebulaSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.border(
                        1.dp,
                        if (state.movesRemaining <= 5) DangerCrimson else StardustOutline,
                        RoundedCornerShape(14.dp)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("SHOTS", style = MaterialTheme.typography.labelSmall, color = StarlightMuted)
                        Text(
                            text = "${state.movesRemaining}",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (state.movesRemaining <= 5) DangerCrimson else StarlightWhite
                        )
                    }
                }
            } else {
                Surface(
                    color = NebulaSurface,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("WAVE", style = MaterialTheme.typography.labelSmall, color = StarlightMuted)
                        Text(
                            text = "${state.endlessWave + 1}",
                            style = MaterialTheme.typography.titleLarge,
                            color = AstroMagenta
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleSound,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NebulaSurface)
                    .testTag("hud_sound_button")
            ) {
                Icon(
                    imageVector = if (state.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                    contentDescription = "Toggle audio",
                    tint = if (state.soundEnabled) AstroCyan else StarlightMuted
                )
            }
        }
    }
}

@Composable
private fun BottomBoosterDock(
    state: GameUiState,
    onSwap: () -> Unit,
    onActivateBooster: (BoosterType) -> Unit,
    onBuyBooster: (BoosterType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = if (state.swapHintPulse) AstroGold.copy(alpha = 0.18f) else NebulaElevated,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onSwap)
                .border(
                    width = if (state.swapHintPulse) 2.dp else 1.dp,
                    color = if (state.swapHintPulse) AstroGold else StardustOutline,
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("swap_bubble_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Rounded.SwapHoriz,
                    contentDescription = "Swap bubbles",
                    tint = if (state.swapHintPulse) AstroGold else AstroCyan
                )
                Text(
                    text = if (state.swapHintPulse) "Swap!" else "Swap",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state.swapHintPulse) AstroGold else StarlightWhite
                )
            }
        }

        BoosterButton(
            label = "Bomb",
            count = state.bombCount,
            coinCost = BoosterType.BOMB.coinCost,
            starCoins = state.starCoins,
            icon = Icons.Rounded.Whatshot,
            accent = AstroMagenta,
            active = state.currentShot.shotType == ShotType.BOMB,
            onActivate = { onActivateBooster(BoosterType.BOMB) },
            onQuickBuy = { onBuyBooster(BoosterType.BOMB) },
            modifier = Modifier
                .weight(1f)
                .testTag("booster_bomb_button")
        )
        BoosterButton(
            label = "Prism",
            count = state.prismCount,
            coinCost = BoosterType.PRISM.coinCost,
            starCoins = state.starCoins,
            icon = Icons.Rounded.AutoAwesome,
            accent = AstroGold,
            active = state.currentShot.shotType == ShotType.PRISM,
            onActivate = { onActivateBooster(BoosterType.PRISM) },
            onQuickBuy = { onBuyBooster(BoosterType.PRISM) },
            modifier = Modifier
                .weight(1f)
                .testTag("booster_prism_button")
        )
        BoosterButton(
            label = "Laser",
            count = state.laserCount,
            coinCost = BoosterType.LASER.coinCost,
            starCoins = state.starCoins,
            icon = Icons.Rounded.Bolt,
            accent = AstroCyan,
            active = state.currentShot.shotType == ShotType.LASER,
            onActivate = { onActivateBooster(BoosterType.LASER) },
            onQuickBuy = { onBuyBooster(BoosterType.LASER) },
            modifier = Modifier
                .weight(1f)
                .testTag("booster_laser_button")
        )
    }
}

@Composable
private fun BoosterButton(
    label: String,
    count: Int,
    coinCost: Long,
    starCoins: Long,
    icon: ImageVector,
    accent: Color,
    active: Boolean,
    onActivate: () -> Unit,
    onQuickBuy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canActivate = count > 0 && !active
    val canBuy = count == 0 && !active && starCoins >= coinCost
    val enabled = canActivate || canBuy

    Surface(
        color = if (active) accent.copy(alpha = 0.3f) else NebulaSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) {
                if (canActivate) onActivate() else onQuickBuy()
            }
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active || count > 0) accent.copy(alpha = 0.75f)
                else if (canBuy) AstroGold.copy(alpha = 0.65f)
                else StardustOutline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (count > 0 || active) accent else if (canBuy) AstroGold else StarlightMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = when {
                    active -> "ARMED"
                    count > 0 -> "$label ($count)"
                    else -> "+${coinCost}c"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    count > 0 || active -> StarlightWhite
                    canBuy -> AstroGold
                    else -> StarlightMuted
                }
            )
        }
    }
}

@Composable
private fun GameOverlayModal(
    state: GameUiState,
    isRewardedAdReady: Boolean,
    onResume: () -> Unit,
    onSecondChance: () -> Unit,
    onWatchAdSecondChance: () -> Unit,
    onRetry: () -> Unit,
    onNextLevel: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicAbyss.copy(alpha = 0.78f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(initialScale = 0.9f)
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = NebulaSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .border(2.dp, StardustOutline, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val headline = when (state.status) {
                        GameStatus.PAUSED -> "Mission Paused"
                        GameStatus.WON -> if (state.mode == GameMode.DAILY) "Daily Cleared!" else "Stage Cleared!"
                        GameStatus.LOST -> if (state.mode == GameMode.ENDLESS) "Nebula Overrun!" else "Out of Shots!"
                        GameStatus.PLAYING -> ""
                    }
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.headlineLarge,
                        color = when (state.status) {
                            GameStatus.WON -> AstroGold
                            GameStatus.LOST -> DangerCrimson
                            else -> AstroCyan
                        },
                        textAlign = TextAlign.Center
                    )

                    if (state.status == GameStatus.WON && state.mode != GameMode.ENDLESS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (i in 1..3) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = if (i <= state.earnedStars) AstroGold else StardustOutline,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        if (state.earnedBooster != null) {
                            Surface(
                                color = AstroMagenta.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Rounded.Flare, contentDescription = null, tint = AstroGold)
                                    Text(
                                        text = "3-Star Bonus: +1 ${state.earnedBooster.label}!",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AstroGold
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Score: ${state.score}  •  Max Combo: ${state.maxCombo}x",
                        style = MaterialTheme.typography.titleMedium,
                        color = StarlightWhite
                    )

                    if (state.status != GameStatus.PAUSED && state.coinsEarnedThisRun > 0L) {
                        Surface(
                            color = AstroGold.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.Toll, contentDescription = null, tint = AstroGold, modifier = Modifier.size(18.dp))
                                val wellStr = if (state.wellBonusCoins > 0L) " (+${state.wellBonusCoins}c Well)" else ""
                                Text(
                                    text = "+${state.coinsEarnedThisRun} Star Coins$wellStr",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AstroGold
                                )
                            }
                        }
                    }

                    if (state.syncErrorMessage != null) {
                        Text(
                            text = state.syncErrorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DangerCrimson,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (state.status == GameStatus.PAUSED) {
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(containerColor = AstroCyan, contentColor = CosmicAbyss),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("resume_button")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Resume", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    if (state.status == GameStatus.LOST && state.canBuySecondChance) {
                        Button(
                            onClick = onSecondChance,
                            colors = ButtonDefaults.buttonColors(containerColor = AstroGold, contentColor = CosmicAbyss),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("second_chance_button")
                        ) {
                            Icon(Icons.Rounded.Toll, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "Continue: +5 Shots (${GameViewModel.SECOND_CHANCE_COST}c)",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    if (state.status == GameStatus.LOST && state.canWatchAdSecondChance) {
                        Button(
                            onClick = onWatchAdSecondChance,
                            enabled = isRewardedAdReady,
                            colors = ButtonDefaults.buttonColors(containerColor = AstroEmerald, contentColor = CosmicAbyss),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("watch_ad_second_chance_button")
                        ) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = if (isRewardedAdReady) "Watch Ad: Free +5 Shots" else "Loading Free Continue Ad...",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    if (state.status == GameStatus.WON && state.mode == GameMode.SAGA && state.levelSpec.levelNumber < LevelCatalog.sagaLevels.size) {
                        Button(
                            onClick = onNextLevel,
                            colors = ButtonDefaults.buttonColors(containerColor = AstroEmerald, contentColor = CosmicAbyss),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("next_level_button")
                        ) {
                            Text("Next Stage", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.status == GameStatus.LOST) AstroMagenta else NebulaElevated,
                            contentColor = StarlightWhite
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("retry_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Play Again", style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = onExit,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("exit_to_map_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Back to Constellation Map", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCosmicBubble(
    center: Offset,
    radius: Float,
    color: BubbleColor,
    kind: TileKind,
    charge: Int,
    pulse: Float,
    highlighted: Boolean,
    reusablePath: Path
) {
    when (kind) {
        TileKind.STONE -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7D869E), Color(0xFF3E4457), Color(0xFF1E222E)),
                    center = center - Offset(radius * 0.25f, radius * 0.25f),
                    radius = radius * 1.4f
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color(0xFF282D3B),
                radius = radius * 0.28f,
                center = center + Offset(-radius * 0.25f, radius * 0.15f)
            )
            drawCircle(
                color = Color(0xFF282D3B),
                radius = radius * 0.18f,
                center = center + Offset(radius * 0.30f, -radius * 0.22f)
            )
        }
        TileKind.STAR_CORE -> {
            drawCircle(
                color = AstroGold.copy(alpha = 0.22f + pulse * 0.25f),
                radius = radius * 1.25f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, AstroGold, Color(0xFFD97706)),
                    center = center - Offset(radius * 0.2f, radius * 0.2f),
                    radius = radius * 1.3f
                ),
                radius = radius,
                center = center
            )
            reusablePath.reset()
            val rOuter = radius * 0.62f
            val rInner = radius * 0.28f
            for (i in 0 until 8) {
                val r = if (i % 2 == 0) rOuter else rInner
                val angle = (i * PI / 4.0) - PI / 2.0
                val px = center.x + (r * cos(angle)).toFloat()
                val py = center.y + (r * sin(angle)).toFloat()
                if (i == 0) reusablePath.moveTo(px, py) else reusablePath.lineTo(px, py)
            }
            reusablePath.close()
            drawPath(reusablePath, color = Color(0xFFFFFBEB))
        }
        TileKind.LIGHTNING -> {
            drawCircle(
                color = AstroCyan.copy(alpha = 0.28f + pulse * 0.28f),
                radius = radius * 1.22f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, AstroCyan, Color(0xFF0284C7)),
                    center = center - Offset(radius * 0.2f, radius * 0.2f),
                    radius = radius * 1.35f
                ),
                radius = radius,
                center = center
            )
            reusablePath.reset()
            reusablePath.moveTo(center.x + radius * 0.12f, center.y - radius * 0.62f)
            reusablePath.lineTo(center.x - radius * 0.32f, center.y + radius * 0.05f)
            reusablePath.lineTo(center.x + radius * 0.05f, center.y + radius * 0.05f)
            reusablePath.lineTo(center.x - radius * 0.14f, center.y + radius * 0.62f)
            reusablePath.lineTo(center.x + radius * 0.34f, center.y - radius * 0.06f)
            reusablePath.lineTo(center.x - radius * 0.02f, center.y - radius * 0.06f)
            reusablePath.close()
            drawPath(reusablePath, color = Color(0xFFFFFBEB))
        }
        TileKind.BLACK_HOLE -> {
            val isCharged = charge > 0
            drawCircle(
                color = (if (isCharged) AstroMagenta else AstroViolet).copy(alpha = 0.35f + pulse * 0.35f),
                radius = radius * (if (isCharged) 1.32f else 1.15f),
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF05020C), Color(0xFF1A0836), if (isCharged) AstroMagenta else AstroViolet),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = if (isCharged) AstroMagenta else AstroViolet,
                radius = radius * 0.72f,
                center = center,
                style = Stroke(width = if (isCharged) 3.8f else 2.4f)
            )
            if (isCharged) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f + pulse * 0.25f),
                    radius = radius * 0.25f,
                    center = center
                )
            }
        }
        TileKind.PORTAL -> {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(AstroCyan, AstroMagenta, AstroViolet, AstroCyan),
                    center = center
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color(0xFF0A041C),
                radius = radius * 0.56f,
                center = center
            )
            drawCircle(
                color = AstroCyan.copy(alpha = 0.65f + pulse * 0.35f),
                radius = radius * 0.36f,
                center = center,
                style = Stroke(width = 2.6f)
            )
        }
        else -> {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.highlight, color.primary, color.shadow),
                    center = center - Offset(radius * 0.3f, radius * 0.32f),
                    radius = radius * 1.45f
                ),
                radius = radius,
                center = center
            )
            // Specular gloss highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = radius * 0.24f,
                center = center - Offset(radius * 0.32f, radius * 0.34f)
            )
            // Subtle colorblind accessibility glyph in center
            drawColorGlyph(center = center, radius = radius * 0.36f, color = color, reusablePath = reusablePath)

            if (kind == TileKind.CHAMELEON) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(AstroCyan, AstroMagenta, AstroGold, AstroEmerald, AstroViolet, AstroCyan),
                        center = center
                    ),
                    radius = radius * 0.96f,
                    center = center,
                    style = Stroke(width = 4.2f)
                )
            }

            if (kind == TileKind.ICE) {
                drawCircle(
                    color = Color(0xFFBDF5FF).copy(alpha = 0.45f),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = radius * 0.94f,
                    center = center,
                    style = Stroke(width = 3.2f)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = center + Offset(-radius * 0.45f, -radius * 0.15f),
                    end = center + Offset(radius * 0.35f, radius * 0.4f),
                    strokeWidth = 2.2f
                )
            }
        }
    }

    if (highlighted) {
        drawCircle(
            color = Color.White.copy(alpha = 0.7f + pulse * 0.3f),
            radius = radius * 1.05f,
            center = center,
            style = Stroke(width = 3f)
        )
    }
}

private fun DrawScope.drawLauncherProjectile(
    center: Offset,
    radius: Float,
    shot: LauncherShot,
    pulse: Float,
    reusablePath: Path
) {
    when (shot.shotType) {
        ShotType.BOMB -> {
            drawCircle(
                color = AstroMagenta.copy(alpha = 0.28f + pulse * 0.25f),
                radius = radius * 1.25f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AstroGold, AstroMagenta, Color(0xFF4C0519)),
                    center = center - Offset(radius * 0.2f, radius * 0.2f),
                    radius = radius * 1.3f
                ),
                radius = radius,
                center = center
            )
        }
        ShotType.PRISM -> {
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(AstroCyan, AstroMagenta, AstroGold, AstroEmerald, AstroViolet, AstroCyan),
                    center = center
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = radius * 0.34f,
                center = center
            )
        }
        ShotType.LASER -> {
            drawCircle(
                color = AstroCyan.copy(alpha = 0.35f + pulse * 0.3f),
                radius = radius * 1.3f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, AstroCyan, Color(0xFF0369A1)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
        ShotType.COLORED -> {
            drawCosmicBubble(
                center = center,
                radius = radius,
                color = shot.color,
                kind = TileKind.NORMAL,
                charge = 0,
                pulse = pulse,
                highlighted = false,
                reusablePath = reusablePath
            )
        }
    }
}

private fun DrawScope.drawColorGlyph(
    center: Offset,
    radius: Float,
    color: BubbleColor,
    reusablePath: Path
) {
    val stroke = Stroke(width = 2.2f)
    val ink = Color.White.copy(alpha = 0.35f)
    when (color) {
        BubbleColor.CYAN -> drawCircle(color = ink, radius = radius * 0.72f, center = center, style = stroke)
        BubbleColor.MAGENTA -> {
            reusablePath.reset()
            reusablePath.moveTo(center.x, center.y - radius)
            reusablePath.lineTo(center.x + radius, center.y)
            reusablePath.lineTo(center.x, center.y + radius)
            reusablePath.lineTo(center.x - radius, center.y)
            reusablePath.close()
            drawPath(reusablePath, color = ink, style = stroke)
        }
        BubbleColor.GOLD -> {
            reusablePath.reset()
            reusablePath.moveTo(center.x, center.y - radius)
            reusablePath.lineTo(center.x + radius * 0.88f, center.y + radius * 0.72f)
            reusablePath.lineTo(center.x - radius * 0.88f, center.y + radius * 0.72f)
            reusablePath.close()
            drawPath(reusablePath, color = ink, style = stroke)
        }
        BubbleColor.EMERALD -> {
            drawLine(ink, center + Offset(-radius * 0.7f, 0f), center + Offset(radius * 0.7f, 0f), strokeWidth = 2.4f)
            drawLine(ink, center + Offset(0f, -radius * 0.7f), center + Offset(0f, radius * 0.7f), strokeWidth = 2.4f)
        }
        BubbleColor.VIOLET -> {
            drawRect(
                color = ink,
                topLeft = center - Offset(radius * 0.65f, radius * 0.65f),
                size = Size(radius * 1.3f, radius * 1.3f),
                style = stroke
            )
        }
        BubbleColor.CORAL -> {
            drawLine(ink, center + Offset(-radius * 0.6f, -radius * 0.6f), center + Offset(radius * 0.6f, radius * 0.6f), strokeWidth = 2.4f)
            drawLine(ink, center + Offset(radius * 0.6f, -radius * 0.6f), center + Offset(-radius * 0.6f, radius * 0.6f), strokeWidth = 2.4f)
        }
    }
}
