package com.example.game

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundSynth
import com.example.data.BoosterType
import com.example.data.CannonSkin
import com.example.data.GameMode
import com.example.data.GameProgressStore
import com.example.data.PlayerProfile
import com.example.ui.theme.AstroCyan
import com.example.ui.theme.AstroGold
import com.example.ui.theme.AstroMagenta
import com.example.ui.theme.AstroViolet
import com.example.ui.theme.DangerCrimson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

enum class GameStatus {
    PLAYING,
    PAUSED,
    WON,
    LOST
}

/**
 * Discrete HUD & modal state. Only changes on discrete shot/score/booster/status events,
 * keeping Compose layout/recomposition completely idle during 60fps flight & particle frames!
 */
data class GameUiState(
    val mode: GameMode = GameMode.SAGA,
    val levelSpec: SagaLevelSpec = LevelCatalog.getLevel(1),
    val currentShot: LauncherShot = LauncherShot(BubbleColor.CYAN),
    val nextShot: LauncherShot = LauncherShot(BubbleColor.MAGENTA),
    val thirdShot: LauncherShot = LauncherShot(BubbleColor.GOLD),
    val score: Long = 0L,
    val movesRemaining: Int = 18,
    val blitzSecondsRemaining: Int = 120,
    val endlessShotsUntilDescent: Int = 5,
    val endlessWave: Int = 0,
    val comboStreak: Int = 0,
    val maxCombo: Int = 0,
    val feverProgress: Int = 0,
    val overdriveTurnsRemaining: Int = 0,
    val totalPoppedInRun: Int = 0,
    val starsRemaining: Int = 0,
    val totalStarsInLevel: Int = 0,
    val status: GameStatus = GameStatus.PLAYING,
    val earnedStars: Int = 0,
    val earnedBooster: BoosterType? = null,
    val coinsEarnedThisRun: Long = 0L,
    val wellBonusCoins: Long = 0L,
    val starCoins: Long = 150L,
    val equippedSkin: CannonSkin = CannonSkin.CYAN_INTERCEPTOR,
    val bossName: String? = null,
    val bossHp: Int = 0,
    val bossMaxHp: Int = 0,
    val bossCounterCountdown: Int = 0,
    val isDangerPanic: Boolean = false,
    val swapHintPulse: Boolean = false,
    val canBuySecondChance: Boolean = false,
    val canWatchAdSecondChance: Boolean = false,
    val usedSecondChance: Boolean = false,
    val bombCount: Int = 0,
    val prismCount: Int = 0,
    val laserCount: Int = 0,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val hapticPulseId: Long = 0L,
    val syncErrorMessage: String? = null
)

class GameViewModel(
    private val store: GameProgressStore,
    private val soundSynth: SoundSynth
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    // Dedicated high-frequency Compose Snapshot state read directly in Canvas onDraw
    private val _renderState: MutableState<RenderFrameState> = mutableStateOf(RenderFrameState())
    val renderState: State<RenderFrameState> = _renderState

    private var boardWidth: Float = 0f
    private var boardHeight: Float = 0f
    private var topOffset: Float = 24f
    private var shooterPos: Offset = Offset.Zero
    private var loopJob: Job? = null
    private var popupCounter = 0L
    private var resultSaved = false
    private var activeDayKey: String = ""
    private var blitzTimerAccum = 0f

    companion object {
        private const val MAX_PARTICLES = 90
        const val SECOND_CHANCE_COST = 60L
        const val BLITZ_DURATION_SECONDS = 120
    }

    fun syncProfileMetadata(profile: PlayerProfile?) {
        if (profile == null) return
        soundSynth.enabled = profile.soundEnabled
        _state.update {
            it.copy(
                starCoins = profile.starCoins,
                equippedSkin = profile.resolvedSkin(),
                bombCount = profile.bombBoosters.toInt().coerceAtLeast(0),
                prismCount = profile.prismBoosters.toInt().coerceAtLeast(0),
                laserCount = profile.laserBoosters.toInt().coerceAtLeast(0),
                soundEnabled = profile.soundEnabled,
                hapticsEnabled = profile.hapticsEnabled
            )
        }
    }

    fun startSagaLevel(
        levelNumber: Int,
        profile: PlayerProfile?,
        starterBooster: BoosterType? = null,
        starterAdFree: Boolean = false
    ) {
        val spec = LevelCatalog.getLevel(levelNumber)
        startBoardSession(
            mode = GameMode.SAGA,
            spec = spec,
            initialGrid = LevelCatalog.buildGridForLevel(spec),
            profile = profile,
            starterBooster = starterBooster,
            starterAdFree = starterAdFree
        )
    }

    fun startDailyChallenge(
        profile: PlayerProfile?,
        starterBooster: BoosterType? = null,
        starterAdFree: Boolean = false
    ) {
        activeDayKey = LevelCatalog.todayKey()
        val spec = LevelCatalog.buildDailyChallengeSpec(activeDayKey)
        startBoardSession(
            mode = GameMode.DAILY,
            spec = spec,
            initialGrid = LevelCatalog.buildGridForLevel(spec),
            profile = profile,
            starterBooster = starterBooster,
            starterAdFree = starterAdFree
        )
    }

    fun startEndlessMode(
        profile: PlayerProfile?,
        starterBooster: BoosterType? = null,
        starterAdFree: Boolean = false
    ) {
        startArcadeSession(
            mode = GameMode.ENDLESS,
            profile = profile,
            starterBooster = starterBooster,
            starterAdFree = starterAdFree
        )
    }

    fun startBlitzMode(
        profile: PlayerProfile?,
        starterBooster: BoosterType? = null,
        starterAdFree: Boolean = false
    ) {
        startArcadeSession(
            mode = GameMode.BLITZ,
            profile = profile,
            starterBooster = starterBooster,
            starterAdFree = starterAdFree
        )
    }

    private fun startArcadeSession(
        mode: GameMode,
        profile: PlayerProfile?,
        starterBooster: BoosterType?,
        starterAdFree: Boolean
    ) {
        val initialGrid = LevelCatalog.buildEndlessStartGrid((System.currentTimeMillis() % 10000).toInt())
        val spec = LevelCatalog.getLevel(1)
        resultSaved = false
        blitzTimerAccum = 0f
        syncProfileMetadata(profile)
        soundSynth.warmUp()

        val colors = availableBoardColors(initialGrid, BubbleColor.entries)
        _renderState.value = RenderFrameState(grid = initialGrid)
        _state.update { prev ->
            prev.copy(
                mode = mode,
                levelSpec = spec,
                currentShot = LauncherShot(colors.random()),
                nextShot = LauncherShot(colors.random()),
                thirdShot = LauncherShot(colors.random()),
                score = 0L,
                movesRemaining = 999,
                blitzSecondsRemaining = BLITZ_DURATION_SECONDS,
                endlessShotsUntilDescent = if (mode == GameMode.BLITZ) 4 else 5,
                endlessWave = 0,
                comboStreak = 0,
                maxCombo = 0,
                feverProgress = 0,
                overdriveTurnsRemaining = 0,
                totalPoppedInRun = 0,
                starsRemaining = 0,
                totalStarsInLevel = 0,
                status = GameStatus.PLAYING,
                earnedStars = 0,
                earnedBooster = null,
                coinsEarnedThisRun = 0L,
                wellBonusCoins = 0L,
                bossName = null,
                bossHp = 0,
                bossMaxHp = 0,
                bossCounterCountdown = 0,
                canBuySecondChance = false,
                canWatchAdSecondChance = false,
                usedSecondChance = false,
                isDangerPanic = checkPanic(initialGrid),
                syncErrorMessage = null
            )
        }
        applyStarterBooster(starterBooster, starterAdFree)
        recomputeTrajectory()
        startGameLoop()
    }

    private fun startBoardSession(
        mode: GameMode,
        spec: SagaLevelSpec,
        initialGrid: Map<Pair<Int, Int>, GridCell>,
        profile: PlayerProfile?,
        starterBooster: BoosterType?,
        starterAdFree: Boolean
    ) {
        val starsTotal = initialGrid.values.count { it.kind == TileKind.STAR_CORE }
        val boss = spec.bossSpec
        resultSaved = false
        blitzTimerAccum = 0f
        syncProfileMetadata(profile)
        soundSynth.warmUp()

        val colors = availableBoardColors(initialGrid, spec.allowedColors)
        _renderState.value = RenderFrameState(grid = initialGrid)
        _state.update { prev ->
            prev.copy(
                mode = mode,
                levelSpec = spec,
                currentShot = LauncherShot(colors.random()),
                nextShot = LauncherShot(colors.random()),
                thirdShot = LauncherShot(colors.random()),
                score = 0L,
                movesRemaining = spec.movesLimit,
                blitzSecondsRemaining = BLITZ_DURATION_SECONDS,
                comboStreak = 0,
                maxCombo = 0,
                feverProgress = 0,
                overdriveTurnsRemaining = 0,
                totalPoppedInRun = 0,
                starsRemaining = starsTotal,
                totalStarsInLevel = starsTotal,
                status = GameStatus.PLAYING,
                earnedStars = 0,
                earnedBooster = null,
                coinsEarnedThisRun = 0L,
                wellBonusCoins = 0L,
                bossName = boss?.name,
                bossHp = boss?.maxHp ?: 0,
                bossMaxHp = boss?.maxHp ?: 0,
                bossCounterCountdown = boss?.counterInterval ?: 0,
                canBuySecondChance = false,
                canWatchAdSecondChance = false,
                usedSecondChance = false,
                isDangerPanic = checkPanic(initialGrid),
                syncErrorMessage = null
            )
        }
        applyStarterBooster(starterBooster, starterAdFree)
        recomputeTrajectory()
        startGameLoop()
    }

    private fun applyStarterBooster(starterBooster: BoosterType?, starterAdFree: Boolean) {
        if (starterBooster == null) return
        if (starterAdFree) {
            val shotType = when (starterBooster) {
                BoosterType.BOMB -> ShotType.BOMB
                BoosterType.PRISM -> ShotType.PRISM
                BoosterType.LASER -> ShotType.LASER
            }
            _state.update { prev ->
                prev.copy(currentShot = prev.currentShot.copy(shotType = shotType))
            }
        } else {
            activateBooster(starterBooster)
        }
    }

    fun updateArenaMetrics(width: Float, height: Float, topMargin: Float, shooterY: Float) {
        if (width <= 0f || height <= 0f) return
        val changed = boardWidth != width || boardHeight != height || shooterPos.y != shooterY
        boardWidth = width
        boardHeight = height
        topOffset = topMargin
        shooterPos = Offset(width / 2f, shooterY)
        if (changed) recomputeTrajectory()
    }

    fun onAimDrag(touchPos: Offset) {
        val s = _state.value
        val rf = _renderState.value
        if (s.status != GameStatus.PLAYING || rf.flyingBubble != null) return
        val dx = touchPos.x - shooterPos.x
        val dy = touchPos.y - shooterPos.y

        // Dragging down onto/below the cannon safely cancels aim!
        if (dy > -22f) {
            _renderState.value = rf.copy(
                isAiming = false,
                trajectory = rf.trajectory.copy(isCancelled = true)
            )
            return
        }
        val rawAngle = atan2(dy, dx)
        val clamped = rawAngle.coerceIn((-PI + 0.16).toFloat(), (-0.16).toFloat())
        _renderState.value = rf.copy(aimAngleRad = clamped, isAiming = true)
        recomputeTrajectory()
    }

    fun onAimRelease() {
        val s = _state.value
        val rf = _renderState.value
        if (s.status != GameStatus.PLAYING || rf.flyingBubble != null || !rf.isAiming || rf.trajectory.isCancelled) {
            _renderState.value = rf.copy(isAiming = false)
            return
        }
        fireCurrentShot(rf.aimAngleRad)
    }

    fun swapShooterBubbles() {
        val s = _state.value
        val rf = _renderState.value
        if (s.status != GameStatus.PLAYING || rf.flyingBubble != null) return
        soundSynth.playSnap()
        _state.update {
            it.copy(
                currentShot = it.nextShot,
                nextShot = it.currentShot
            )
        }
        recomputeTrajectory()
    }

    fun activateBooster(booster: BoosterType) {
        val s = _state.value
        val rf = _renderState.value
        if (s.status != GameStatus.PLAYING || rf.flyingBubble != null) return
        if (s.currentShot.shotType != ShotType.COLORED) return
        val available = when (booster) {
            BoosterType.BOMB -> s.bombCount
            BoosterType.PRISM -> s.prismCount
            BoosterType.LASER -> s.laserCount
        }
        if (available <= 0) return

        val shotType = when (booster) {
            BoosterType.BOMB -> ShotType.BOMB
            BoosterType.PRISM -> ShotType.PRISM
            BoosterType.LASER -> ShotType.LASER
        }
        soundSynth.playPop(5)
        _state.update { prev ->
            prev.copy(
                currentShot = prev.currentShot.copy(shotType = shotType),
                bombCount = if (booster == BoosterType.BOMB) prev.bombCount - 1 else prev.bombCount,
                prismCount = if (booster == BoosterType.PRISM) prev.prismCount - 1 else prev.prismCount,
                laserCount = if (booster == BoosterType.LASER) prev.laserCount - 1 else prev.laserCount
            )
        }
        recomputeTrajectory()
        viewModelScope.launch {
            store.consumeBooster(booster)
        }
    }

    fun buyBoosterMidGame(booster: BoosterType) {
        val s = _state.value
        if (s.starCoins < booster.coinCost) return
        viewModelScope.launch {
            val res = store.purchaseBooster(booster)
            if (res.isSuccess) {
                soundSynth.playDrop()
                _state.update { prev ->
                    prev.copy(
                        starCoins = (prev.starCoins - booster.coinCost).coerceAtLeast(0L),
                        bombCount = if (booster == BoosterType.BOMB) prev.bombCount + 1 else prev.bombCount,
                        prismCount = if (booster == BoosterType.PRISM) prev.prismCount + 1 else prev.prismCount,
                        laserCount = if (booster == BoosterType.LASER) prev.laserCount + 1 else prev.laserCount
                    )
                }
            }
        }
    }

    fun buySecondChanceExtraShots() {
        val s = _state.value
        if (!s.canBuySecondChance || s.usedSecondChance || s.starCoins < SECOND_CHANCE_COST) return
        resultSaved = false
        viewModelScope.launch {
            val res = store.spendCoinsForExtraShots(SECOND_CHANCE_COST)
            if (res.isSuccess) {
                soundSynth.playOverdrive()
                _state.update {
                    it.copy(
                        status = GameStatus.PLAYING,
                        movesRemaining = it.movesRemaining + 5,
                        canBuySecondChance = false,
                        canWatchAdSecondChance = false,
                        usedSecondChance = true,
                        starCoins = (it.starCoins - SECOND_CHANCE_COST).coerceAtLeast(0L)
                    )
                }
            }
        }
    }

    fun grantSecondChanceFromAd() {
        val s = _state.value
        if (!s.canWatchAdSecondChance || s.usedSecondChance) return
        resultSaved = false
        soundSynth.playOverdrive()
        _state.update {
            it.copy(
                status = GameStatus.PLAYING,
                movesRemaining = it.movesRemaining + 5,
                canBuySecondChance = false,
                canWatchAdSecondChance = false,
                usedSecondChance = true
            )
        }
    }

    fun togglePause() {
        _state.update { s ->
            when (s.status) {
                GameStatus.PLAYING -> s.copy(status = GameStatus.PAUSED)
                GameStatus.PAUSED -> s.copy(status = GameStatus.PLAYING)
                else -> s
            }
        }
        _renderState.value = _renderState.value.copy(isAiming = false)
    }

    fun toggleSound() {
        val next = !_state.value.soundEnabled
        soundSynth.enabled = next
        _state.update { it.copy(soundEnabled = next) }
        viewModelScope.launch {
            store.updatePreferences(next, _state.value.hapticsEnabled)
        }
    }

    private fun fireCurrentShot(angleRad: Float) {
        val s = _state.value
        if (boardWidth <= 0f) return

        if (s.currentShot.shotType == ShotType.LASER) {
            executeLaserBlast(angleRad)
            return
        }

        val speed = (boardWidth * 2.65f).coerceAtLeast(1150f)
        val vx = cos(angleRad) * speed
        val vy = sin(angleRad) * speed
        soundSynth.playShoot()

        _renderState.value = _renderState.value.copy(
            flyingBubble = FlyingBubble(
                x = shooterPos.x,
                y = shooterPos.y,
                vx = vx,
                vy = vy,
                shot = s.currentShot
            ),
            isAiming = false,
            trajectory = AimTrajectory()
        )
    }

    private fun executeLaserBlast(angleRad: Float) {
        val rf = _renderState.value
        val radius = HexGridEngine.bubbleRadius(boardWidth)
        val traj = HexGridEngine.raycastTrajectory(
            shooterPos.x, shooterPos.y, angleRad,
            boardWidth, radius, topOffset, rf.grid, ShotType.LASER
        )
        val targets = traj.highlightCells
        soundSynth.playBomb()
        val mutableGrid = rf.grid.toMutableMap()
        val particles = rf.particles.toMutableList()
        val arcs = rf.lightningArcs.toMutableList()
        val popups = rf.popups.toMutableList()

        for (cellKey in targets) {
            val cell = mutableGrid.remove(cellKey) ?: continue
            val c = HexGridEngine.cellCenter(cellKey.first, cellKey.second, boardWidth, radius, topOffset)
            spawnPopParticles(particles, c, if (cell.kind == TileKind.STAR_CORE) AstroGold else cell.color.primary, 6)
            if (cell.kind == TileKind.LIGHTNING) {
                triggerLightningRow(cellKey.first, mutableGrid, particles, arcs, radius)
            }
        }

        val detached = HexGridEngine.findFloatingDetachedCells(mutableGrid)
        val falling = rf.fallingBubbles.toMutableList()
        for (key in detached) {
            val cell = mutableGrid.remove(key) ?: continue
            val c = HexGridEngine.cellCenter(key.first, key.second, boardWidth, radius, topOffset)
            falling.add(
                FallingBubble(
                    x = c.x, y = c.y,
                    vx = Random.nextFloat() * 140f - 70f,
                    vy = -120f - Random.nextFloat() * 120f,
                    color = cell.color,
                    kind = cell.kind
                )
            )
        }

        val poppedTotal = targets.size + detached.size
        val mult = if (_state.value.overdriveTurnsRemaining > 0) 2L else 1L
        val points = (targets.size * 65L + detached.size * 95L) * mult
        val shockwaves = rf.shockwaves.toMutableList()
        if (poppedTotal > 0) {
            popups.add(
                FloatingPopup(++popupCounter, shooterPos.x, topOffset + radius * 4f, "STAR LASER! +$points", AstroCyan)
            )
            shockwaves.add(ShockwaveFx(Offset(boardWidth / 2f, topOffset + radius * 3f), boardWidth * 0.45f, AstroCyan))
        }

        finalizeTurn(
            newGrid = mutableGrid,
            falling = falling,
            particles = particles,
            popups = popups,
            arcs = arcs,
            portalFx = rf.portalFx,
            shockwaves = shockwaves,
            gainedScore = points,
            matchedCount = poppedTotal,
            topRowHits = targets.count { it.first <= 1 },
            avalancheCount = detached.size,
            shakeAmount = 14f,
            wasSpecialShot = true
        )
    }

    private fun startGameLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (true) {
                delay(16L)
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                lastTime = now
                tick(dt)
            }
        }
    }

    private fun tick(dt: Float) {
        val currentState = _state.value
        if (currentState.status == GameStatus.PAUSED) return
        if (currentState.mode == GameMode.BLITZ && currentState.status == GameStatus.PLAYING) {
            blitzTimerAccum += dt
            if (blitzTimerAccum >= 1f) {
                val ticks = blitzTimerAccum.toInt()
                blitzTimerAccum -= ticks
                val nextSec = (currentState.blitzSecondsRemaining - ticks).coerceAtLeast(0)
                _state.update { it.copy(blitzSecondsRemaining = nextSec) }
                if (nextSec == 0) {
                    finalizeBlitzTimeUp()
                    return
                }
            }
        }
        val rf = _renderState.value

        var anyChange = false

        val updatedGrid = if (rf.grid.values.any { it.wobble > 0.01f }) {
            anyChange = true
            rf.grid.mapValues { (_, cell) ->
                if (cell.wobble > 0.01f) cell.copy(wobble = (cell.wobble - dt * 4.8f).coerceAtLeast(0f)) else cell
            }
        } else rf.grid

        val updatedParticles = if (rf.particles.isNotEmpty()) {
            anyChange = true
            rf.particles.mapNotNull { p ->
                val nl = p.life - dt * 2.2f
                if (nl <= 0f) null
                else p.copy(x = p.x + p.vx * dt, y = p.y + p.vy * dt, vy = p.vy + 540f * dt, life = nl)
            }
        } else rf.particles

        val updatedPopups = if (rf.popups.isNotEmpty()) {
            anyChange = true
            rf.popups.mapNotNull { pop ->
                val nl = pop.life - dt * 0.95f
                if (nl <= 0f) null else pop.copy(y = pop.y - 68f * dt, life = nl)
            }
        } else rf.popups

        val updatedArcs = if (rf.lightningArcs.isNotEmpty()) {
            anyChange = true
            rf.lightningArcs.mapNotNull { arc ->
                val nl = arc.life - dt * 3.2f
                if (nl <= 0f) null else arc.copy(life = nl)
            }
        } else rf.lightningArcs

        val updatedPortals = if (rf.portalFx.isNotEmpty()) {
            anyChange = true
            rf.portalFx.mapNotNull { pfx ->
                val nl = pfx.life - dt * 2.4f
                if (nl <= 0f) null else pfx.copy(life = nl)
            }
        } else rf.portalFx

        val updatedShockwaves = if (rf.shockwaves.isNotEmpty()) {
            anyChange = true
            rf.shockwaves.mapNotNull { sw ->
                val nl = sw.life - dt * 2.6f
                if (nl <= 0f) null else sw.copy(life = nl)
            }
        } else rf.shockwaves

        val nextBossFlash = if (rf.bossHitFlash > 0f) {
            anyChange = true
            (rf.bossHitFlash - dt * 4.0f).coerceAtLeast(0f)
        } else 0f

        val radius = if (boardWidth > 0f) HexGridEngine.bubbleRadius(boardWidth) else 24f
        var bonusWellScore = 0L
        var bonusWellCoins = 0L
        val extraPopups = mutableListOf<FloatingPopup>()
        val extraParticles = mutableListOf<PopParticle>()

        val updatedFalling = if (rf.fallingBubbles.isNotEmpty()) {
            anyChange = true
            val overdriveMult = if (_state.value.overdriveTurnsRemaining > 0) 2L else 1L
            rf.fallingBubbles.mapNotNull { fb ->
                val nx = fb.x + fb.vx * dt
                val ny = fb.y + fb.vy * dt
                val nvy = fb.vy + 1480f * dt
                val wellLine = (shooterPos.y - radius * 2.2f).coerceAtLeast(boardHeight * 0.72f)
                if (!fb.scoredInWell && ny >= wellLine) {
                    val mult = HexGridEngine.scoreWellMultiplier(nx, boardWidth)
                    val pts = (if (fb.kind == TileKind.STAR_CORE) 250L else 55L) * mult * overdriveMult
                    bonusWellScore += pts
                    if (mult == 5) {
                        bonusWellCoins += 3L
                        soundSynth.playCoin()
                        extraPopups.add(
                            FloatingPopup(++popupCounter, nx.coerceIn(48f, boardWidth - 48f), wellLine - 24f, "+$pts x5 +3c!", AstroGold)
                        )
                    } else {
                        soundSynth.playDrop()
                        extraPopups.add(
                            FloatingPopup(++popupCounter, nx.coerceIn(48f, boardWidth - 48f), wellLine - 18f, "+$pts x$mult", AstroGold)
                        )
                    }
                    spawnPopParticles(extraParticles, Offset(nx, wellLine), fb.color.primary, 4)
                    null
                } else if (ny > boardHeight + 100f) {
                    null
                } else {
                    fb.copy(x = nx, y = ny, vy = nvy, rotation = fb.rotation + dt * 240f)
                }
            }
        } else rf.fallingBubbles

        val nextShake = if (rf.screenShake > 0f) {
            anyChange = true
            (rf.screenShake - dt * 36f).coerceAtLeast(0f)
        } else 0f

        if (bonusWellScore > 0L || bonusWellCoins > 0L) {
            _state.update {
                it.copy(
                    score = it.score + bonusWellScore,
                    wellBonusCoins = it.wellBonusCoins + bonusWellCoins
                )
            }
        }

        val flying = rf.flyingBubble
        if (flying == null || boardWidth <= 0f) {
            if (anyChange) {
                _renderState.value = rf.copy(
                    grid = updatedGrid,
                    fallingBubbles = updatedFalling,
                    particles = (updatedParticles + extraParticles).takeLast(MAX_PARTICLES),
                    popups = updatedPopups + extraPopups,
                    lightningArcs = updatedArcs,
                    portalFx = updatedPortals,
                    shockwaves = updatedShockwaves,
                    bossHitFlash = nextBossFlash,
                    screenShake = nextShake
                )
            }
            return
        }

        // Sub-step flying projectile for zero tunneling + Portal warp!
        val leftWall = (boardWidth - HexGridEngine.COLS_EVEN * 2f * radius) / 2f + radius
        val rightWall = boardWidth - leftWall
        val hitDist = radius * 1.82f

        val subSteps = 4
        val subDt = dt / subSteps
        var fx = flying.x
        var fy = flying.y
        var fvx = flying.vx
        var fvy = flying.vy
        var portalCd = (flying.portalCooldown - dt).coerceAtLeast(0f)
        val trail = flying.trail.toMutableList()
        val portalList = updatedPortals.toMutableList()

        for (step in 0 until subSteps) {
            fx += fvx * subDt
            fy += fvy * subDt
            if (fx <= leftWall) {
                fx = leftWall + (leftWall - fx)
                fvx = -fvx
                soundSynth.playBounce()
            } else if (fx >= rightWall) {
                fx = rightWall - (fx - rightWall)
                fvx = -fvx
                soundSynth.playBounce()
            }

            if (fy <= topOffset + radius) {
                _renderState.value = rf.copy(
                    grid = updatedGrid,
                    fallingBubbles = updatedFalling,
                    particles = (updatedParticles + extraParticles).takeLast(MAX_PARTICLES),
                    popups = updatedPopups + extraPopups,
                    lightningArcs = updatedArcs,
                    portalFx = portalList,
                    shockwaves = updatedShockwaves,
                    bossHitFlash = nextBossFlash,
                    screenShake = nextShake
                )
                handleProjectileImpact(fx, topOffset + radius, flying.shot)
                return
            }

            for ((key, cell) in updatedGrid) {
                val center = HexGridEngine.cellCenter(key.first, key.second, boardWidth, radius, topOffset)
                if (hypot(center.x - fx, center.y - fy) <= hitDist) {
                    if (portalCd <= 0f && cell.kind == TileKind.PORTAL) {
                        val partner = HexGridEngine.findPartnerPortal(key, updatedGrid)
                        if (partner != null) {
                            val exitCenter = HexGridEngine.cellCenter(partner.first, partner.second, boardWidth, radius, topOffset)
                            val speed = hypot(fvx, fvy).coerceAtLeast(1f)
                            fx = exitCenter.x + (fvx / speed) * radius * 2.1f
                            fy = exitCenter.y + (fvy / speed) * radius * 2.1f
                            portalCd = 0.25f
                            trail.clear()
                            portalList.add(PortalWarpFx(center, exitCenter))
                            soundSynth.playPortal()
                            continue
                        }
                    }

                    _renderState.value = rf.copy(
                        grid = updatedGrid,
                        fallingBubbles = updatedFalling,
                        particles = (updatedParticles + extraParticles).takeLast(MAX_PARTICLES),
                        popups = updatedPopups + extraPopups,
                        lightningArcs = updatedArcs,
                        portalFx = portalList,
                        shockwaves = updatedShockwaves,
                        bossHitFlash = nextBossFlash,
                        screenShake = nextShake
                    )
                    handleProjectileImpact(fx, fy, flying.shot)
                    return
                }
            }
        }

        trail.add(Offset(fx, fy))
        if (trail.size > 10) trail.removeAt(0)
        _renderState.value = rf.copy(
            grid = updatedGrid,
            flyingBubble = flying.copy(x = fx, y = fy, vx = fvx, vy = fvy, trail = trail, portalCooldown = portalCd),
            fallingBubbles = updatedFalling,
            particles = (updatedParticles + extraParticles).takeLast(MAX_PARTICLES),
            popups = updatedPopups + extraPopups,
            lightningArcs = updatedArcs,
            portalFx = portalList,
            shockwaves = updatedShockwaves,
            bossHitFlash = nextBossFlash,
            screenShake = nextShake
        )
    }

    private fun handleProjectileImpact(x: Float, y: Float, shot: LauncherShot) {
        val s = _state.value
        val rf = _renderState.value
        val radius = HexGridEngine.bubbleRadius(boardWidth)
        val snap = HexGridEngine.findNearestEmptyCell(x, y, boardWidth, radius, topOffset, rf.grid)
        if (snap == null) {
            _renderState.value = rf.copy(flyingBubble = null)
            return
        }

        val mutableGrid = rf.grid.toMutableMap()
        val particles = rf.particles.toMutableList()
        val popups = rf.popups.toMutableList()
        val falling = rf.fallingBubbles.toMutableList()
        val arcs = rf.lightningArcs.toMutableList()
        val shockwaves = rf.shockwaves.toMutableList()
        val snapCenter = HexGridEngine.cellCenter(snap.first, snap.second, boardWidth, radius, topOffset)

        var poppedCells = mutableSetOf<Pair<Int, Int>>()
        var shake = 0f

        when (shot.shotType) {
            ShotType.BOMB -> {
                poppedCells.addAll(HexGridEngine.computeBombBlastCells(snap.first, snap.second, mutableGrid))
                soundSynth.playBomb()
                shake = 16f
                spawnPopParticles(particles, snapCenter, AstroMagenta, 16, isStar = true)
                shockwaves.add(ShockwaveFx(snapCenter, radius * 5.5f, AstroMagenta))
            }
            ShotType.PRISM -> {
                val neighborColors = HexGridEngine.neighbors(snap.first, snap.second)
                    .mapNotNull { mutableGrid[it] }
                    .filter { it.kind == TileKind.NORMAL || it.kind == TileKind.CHAMELEON }
                    .map { it.color }
                    .distinct()
                var bestCluster = emptySet<Pair<Int, Int>>()
                var bestColor = shot.color
                for (c in neighborColors) {
                    mutableGrid[snap] = GridCell(snap.first, snap.second, c, TileKind.NORMAL)
                    val cluster = HexGridEngine.findColorMatchCluster(snap.first, snap.second, c, mutableGrid)
                    if (cluster.size > bestCluster.size) {
                        bestCluster = cluster
                        bestColor = c
                    }
                }
                if (bestCluster.size >= 2) {
                    poppedCells.addAll(bestCluster)
                    soundSynth.playPop(s.comboStreak + 2)
                    shake = 10f
                    shockwaves.add(ShockwaveFx(snapCenter, radius * 4.2f, bestColor.primary))
                } else {
                    mutableGrid[snap] = GridCell(snap.first, snap.second, bestColor, TileKind.NORMAL, wobble = 1f)
                    soundSynth.playSnap()
                }
            }
            else -> {
                mutableGrid[snap] = GridCell(snap.first, snap.second, shot.color, TileKind.NORMAL, wobble = 1f)
                val cluster = HexGridEngine.findColorMatchCluster(snap.first, snap.second, shot.color, mutableGrid)
                if (cluster.size >= 3) {
                    poppedCells.addAll(cluster)
                    soundSynth.playPop(s.comboStreak)
                    shake = if (cluster.size >= 5) 11f else 5f
                } else {
                    // Check direct neighbor interactions: Black Hole charging & implosion!
                    var triggeredSpecial = false
                    for (n in HexGridEngine.neighbors(snap.first, snap.second)) {
                        val existing = mutableGrid[n] ?: continue
                        when (existing.kind) {
                            TileKind.BLACK_HOLE -> {
                                triggeredSpecial = true
                                val nc = HexGridEngine.cellCenter(n.first, n.second, boardWidth, radius, topOffset)
                                if (existing.charge == 0) {
                                    mutableGrid[n] = existing.copy(charge = 1, wobble = 1f)
                                    mutableGrid.remove(snap) // Swallowed!
                                    popups.add(FloatingPopup(++popupCounter, nc.x, nc.y, "CHARGED!", AstroViolet))
                                    spawnPopParticles(particles, nc, AstroViolet, 8)
                                    soundSynth.playPortal()
                                } else {
                                    // Implosion!
                                    val imploded = HexGridEngine.computeBombBlastCells(n.first, n.second, mutableGrid)
                                    poppedCells.addAll(imploded)
                                    poppedCells.add(snap)
                                    popups.add(FloatingPopup(++popupCounter, nc.x, nc.y, "IMPLOSION!", AstroMagenta))
                                    shockwaves.add(ShockwaveFx(nc, radius * 6.0f, AstroViolet))
                                    soundSynth.playBomb()
                                    shake = 15f
                                }
                            }
                            else -> mutableGrid[n] = existing.copy(wobble = 0.7f)
                        }
                    }
                    if (!triggeredSpecial) soundSynth.playSnap()
                }
            }
        }

        // Process popped cells + chain-trigger Lightning / Ice / Star Cores / Black Holes
        val rescuedOrCracked = mutableSetOf<Pair<Int, Int>>()
        val lightningRowsTriggered = mutableSetOf<Int>()
        val processQueue = ArrayDeque(poppedCells)
        val actuallyRemoved = mutableSetOf<Pair<Int, Int>>()

        while (processQueue.isNotEmpty()) {
            val pKey = processQueue.removeFirst()
            if (pKey in actuallyRemoved) continue
            val cell = mutableGrid.remove(pKey) ?: continue
            actuallyRemoved.add(pKey)
            val c = HexGridEngine.cellCenter(pKey.first, pKey.second, boardWidth, radius, topOffset)
            spawnPopParticles(particles, c, cell.color.primary, 5)

            for (nKey in HexGridEngine.neighbors(pKey.first, pKey.second)) {
                if (nKey in actuallyRemoved || nKey in rescuedOrCracked) continue
                val nCell = mutableGrid[nKey] ?: continue
                when (nCell.kind) {
                    TileKind.ICE -> {
                        mutableGrid[nKey] = nCell.copy(kind = TileKind.NORMAL, wobble = 1f)
                        rescuedOrCracked.add(nKey)
                        val nc = HexGridEngine.cellCenter(nKey.first, nKey.second, boardWidth, radius, topOffset)
                        spawnPopParticles(particles, nc, Color.White, 4)
                    }
                    TileKind.SHIELDED -> {
                        mutableGrid[nKey] = nCell.copy(kind = TileKind.NORMAL, wobble = 1f)
                        rescuedOrCracked.add(nKey)
                        val nc = HexGridEngine.cellCenter(nKey.first, nKey.second, boardWidth, radius, topOffset)
                        spawnPopParticles(particles, nc, Color(0xFFE2E8F0), 6)
                        popups.add(FloatingPopup(++popupCounter, nc.x, nc.y, "ARMOR CRACKED!", AstroCyan))
                    }
                    TileKind.STAR_CORE -> {
                        mutableGrid.remove(nKey)
                        actuallyRemoved.add(nKey)
                        rescuedOrCracked.add(nKey)
                        val nc = HexGridEngine.cellCenter(nKey.first, nKey.second, boardWidth, radius, topOffset)
                        spawnPopParticles(particles, nc, AstroGold, 10, isStar = true)
                        popups.add(FloatingPopup(++popupCounter, nc.x, nc.y, "STAR RESCUED! +300", AstroGold))
                    }
                    TileKind.LIGHTNING -> {
                        if (lightningRowsTriggered.add(nKey.first)) {
                            val rowCells = triggerLightningRow(nKey.first, mutableGrid, particles, arcs, radius)
                            for (rc in rowCells) processQueue.add(rc)
                            popups.add(FloatingPopup(++popupCounter, boardWidth / 2f, c.y, "LIGHTNING ZAP!", AstroCyan))
                            shake = maxOf(shake, 12f)
                        }
                    }
                    else -> Unit
                }
            }
        }

        // Morph all remaining CHAMELEON bubbles to the next color!
        val activeColors = availableBoardColors(mutableGrid, s.levelSpec.allowedColors)
        if (activeColors.size > 1) {
            for ((k, cell) in mutableGrid.entries.toList()) {
                if (cell.kind == TileKind.CHAMELEON) {
                    val idx = activeColors.indexOf(cell.color)
                    val nextColor = activeColors[(idx + 1) % activeColors.size]
                    mutableGrid[k] = cell.copy(color = nextColor, wobble = 0.5f)
                }
            }
        }

        // Detach floating islands
        val detached = if (actuallyRemoved.isNotEmpty()) {
            HexGridEngine.findFloatingDetachedCells(mutableGrid)
        } else emptySet()

        for (dKey in detached) {
            val cell = mutableGrid.remove(dKey) ?: continue
            val dc = HexGridEngine.cellCenter(dKey.first, dKey.second, boardWidth, radius, topOffset)
            falling.add(
                FallingBubble(
                    x = dc.x,
                    y = dc.y,
                    vx = Random.nextFloat() * 140f - 70f,
                    vy = -90f - Random.nextFloat() * 130f,
                    color = cell.color,
                    kind = cell.kind
                )
            )
        }

        val totalCleared = actuallyRemoved.size + detached.size
        val nextStreak = if (totalCleared > 0) s.comboStreak + 1 else 0
        val multiplier = (1f + (nextStreak.coerceAtLeast(1) - 1) * 0.25f) *
            if (s.overdriveTurnsRemaining > 0) 2f else 1f
        val starBonus = rescuedOrCracked.count { !mutableGrid.containsKey(it) } * 300L
        val rawPoints = (actuallyRemoved.size * 45L + detached.size * 80L) * multiplier + starBonus
        val gainedScore = rawPoints.toLong()

        if (totalCleared > 0) {
            val label = when {
                detached.size >= 6 -> "AVALANCHE! +$gainedScore"
                s.overdriveTurnsRemaining > 0 -> "OVERDRIVE 2x! +$gainedScore"
                nextStreak >= 3 -> "COMBO x$nextStreak! +$gainedScore"
                else -> "+$gainedScore"
            }
            popups.add(
                FloatingPopup(
                    ++popupCounter,
                    snapCenter.x.coerceIn(64f, boardWidth - 64f),
                    snapCenter.y,
                    label,
                    if (detached.size >= 4) AstroGold else AstroCyan
                )
            )
        }

        finalizeTurn(
            newGrid = mutableGrid,
            falling = falling,
            particles = particles,
            popups = popups,
            arcs = arcs,
            portalFx = rf.portalFx,
            shockwaves = shockwaves,
            gainedScore = gainedScore,
            matchedCount = totalCleared,
            topRowHits = actuallyRemoved.count { it.first <= 1 },
            avalancheCount = detached.size,
            shakeAmount = shake,
            wasSpecialShot = shot.shotType != ShotType.COLORED
        )
    }

    private fun triggerLightningRow(
        row: Int,
        mutableGrid: MutableMap<Pair<Int, Int>, GridCell>,
        particles: MutableList<PopParticle>,
        arcs: MutableList<LightningArc>,
        radius: Float
    ): Set<Pair<Int, Int>> {
        soundSynth.playLightning()
        val rowY = HexGridEngine.cellCenter(row, 0, boardWidth, radius, topOffset).y
        arcs.add(LightningArc(y = rowY, leftX = 8f, rightX = boardWidth - 8f))
        val targets = HexGridEngine.computeRowCells(row, mutableGrid)
        for (t in targets) {
            val c = HexGridEngine.cellCenter(t.first, t.second, boardWidth, radius, topOffset)
            spawnPopParticles(particles, c, AstroCyan, 4)
        }
        return targets
    }

    private fun finalizeTurn(
        newGrid: MutableMap<Pair<Int, Int>, GridCell>,
        falling: List<FallingBubble>,
        particles: List<PopParticle>,
        popups: List<FloatingPopup>,
        arcs: List<LightningArc>,
        portalFx: List<PortalWarpFx>,
        shockwaves: List<ShockwaveFx>,
        gainedScore: Long,
        matchedCount: Int,
        topRowHits: Int,
        avalancheCount: Int,
        shakeAmount: Float,
        wasSpecialShot: Boolean
    ) {
        val s = _state.value
        val didClear = matchedCount > 0
        val nextCombo = if (didClear) s.comboStreak + 1 else 0
        val maxCombo = maxOf(s.maxCombo, nextCombo)
        val mutablePopups = popups.toMutableList()
        val mutableFalling = falling.toMutableList()
        val mutableShockwaves = shockwaves.toMutableList()
        val mutableParticles = particles.toMutableList()
        var extraShake = shakeAmount
        var bossFlash = _renderState.value.bossHitFlash

        var nextOverdrive = (s.overdriveTurnsRemaining - 1).coerceAtLeast(0)
        var nextFever = if (didClear) s.feverProgress + 1 else (s.feverProgress - 1).coerceAtLeast(0)
        var feverBonusShot: ShotType? = null
        if (nextFever >= 5) {
            nextFever = 0
            nextOverdrive = 3
            feverBonusShot = if (Random.nextBoolean()) ShotType.BOMB else ShotType.PRISM
            soundSynth.playOverdrive()
            mutableShockwaves.add(ShockwaveFx(shooterPos, boardWidth * 0.55f, AstroGold))
        }

        var nextMoves = s.movesRemaining
        var nextShotsUntilDescent = s.endlessShotsUntilDescent
        var nextWave = s.endlessWave

        if (s.mode == GameMode.SAGA || s.mode == GameMode.DAILY) {
            if (!wasSpecialShot) {
                nextMoves = (s.movesRemaining - 1).coerceAtLeast(0)
            }
            if (avalancheCount >= 6) {
                nextMoves += 1
                mutablePopups.add(
                    FloatingPopup(++popupCounter, boardWidth / 2f, shooterPos.y - 90f, "AVALANCHE! +1 SHOT", AstroGold)
                )
            }
        } else {
            val dropInterval = if (s.mode == GameMode.BLITZ) 4 else 5
            val lowestRow = newGrid.keys.maxOfOrNull { it.first } ?: -1
            if (s.mode == GameMode.BLITZ && (newGrid.isEmpty() || lowestRow < 2)) {
                nextShotsUntilDescent = dropInterval
                nextWave++
                shiftEndlessGridDown(newGrid, nextWave)
                mutablePopups.add(
                    FloatingPopup(++popupCounter, boardWidth / 2f, topOffset + 64f, "WAVE $nextWave INCOMING!", AstroGold)
                )
            } else if (!didClear) {
                nextShotsUntilDescent--
                if (nextShotsUntilDescent <= 0) {
                    nextShotsUntilDescent = dropInterval
                    nextWave++
                    shiftEndlessGridDown(newGrid, nextWave)
                }
            }
        }

        // Cosmic Guardian Boss Combat (Stages 6, 12, 18)
        var nextBossHp = s.bossHp
        var nextBossCountdown = s.bossCounterCountdown
        var bossClearBonusScore = 0L
        var bossClearBonusCoins = 0L
        val bossSpec = s.levelSpec.bossSpec
        if (bossSpec != null && s.bossMaxHp > 0 && nextBossHp > 0) {
            val damage = (if (didClear) (matchedCount / 2).coerceAtLeast(1) else 0) +
                topRowHits * 2 +
                (avalancheCount / 2) +
                if (wasSpecialShot && didClear) 4 else 0

            if (damage > 0) {
                nextBossHp = (nextBossHp - damage).coerceAtLeast(0)
                bossFlash = 1f
                extraShake = maxOf(extraShake, 11f)
                soundSynth.playBossHit()
                val bossCenter = Offset(boardWidth / 2f, topOffset + 32f)
                mutableShockwaves.add(ShockwaveFx(bossCenter, boardWidth * 0.42f, bossSpec.accentColor))
                mutablePopups.add(
                    FloatingPopup(++popupCounter, bossCenter.x, bossCenter.y + 24f, "BOSS -$damage HP!", bossSpec.accentColor)
                )
                if (damage >= 4) {
                    nextBossCountdown = bossSpec.counterInterval
                }
                if (nextBossHp == 0) {
                    bossClearBonusScore = 1500L
                    bossClearBonusCoins = 40L
                    soundSynth.playBomb()
                    mutableShockwaves.add(ShockwaveFx(bossCenter, boardWidth * 0.85f, AstroGold))
                    mutablePopups.add(
                        FloatingPopup(++popupCounter, bossCenter.x, bossCenter.y + 52f, "BOSS SHATTERED! +1500", AstroGold)
                    )
                    val radius = if (boardWidth > 0f) HexGridEngine.bubbleRadius(boardWidth) else 24f
                    for (cell in newGrid.values.toList()) {
                        val c = HexGridEngine.cellCenter(cell.row, cell.col, boardWidth, radius, topOffset)
                        mutableFalling.add(
                            FallingBubble(
                                x = c.x, y = c.y,
                                vx = Random.nextFloat() * 140f - 70f,
                                vy = -120f - Random.nextFloat() * 110f,
                                color = cell.color,
                                kind = cell.kind
                            )
                        )
                    }
                    newGrid.clear()
                }
            } else {
                nextBossCountdown--
                if (nextBossCountdown <= 0) {
                    nextBossCountdown = bossSpec.counterInterval
                    val normalCandidates = newGrid.entries
                        .filter { it.value.kind == TileKind.NORMAL }
                        .shuffled()
                        .take(2)
                    for ((k, cell) in normalCandidates) {
                        newGrid[k] = cell.copy(kind = TileKind.ICE, wobble = 1f)
                    }
                    if (normalCandidates.isNotEmpty()) {
                        extraShake = maxOf(extraShake, 9f)
                        mutablePopups.add(
                            FloatingPopup(++popupCounter, boardWidth / 2f, topOffset + 70f, "${bossSpec.name}: ICE CURSE!", DangerCrimson)
                        )
                    }
                }
            }
        }

        val starsLeft = newGrid.values.count { it.kind == TileKind.STAR_CORE }
        val dangerReached = newGrid.keys.any { it.first >= HexGridEngine.DANGER_ROW }
        val panic = checkPanic(newGrid)

        val isVictory = when (s.mode) {
            GameMode.SAGA, GameMode.DAILY -> when {
                s.bossMaxHp > 0 && nextBossHp <= 0 -> true
                s.bossMaxHp > 0 -> false
                s.levelSpec.goal == LevelGoal.RESCUE_STARS -> starsLeft == 0 || newGrid.isEmpty()
                else -> newGrid.isEmpty()
            }
            GameMode.ENDLESS, GameMode.BLITZ -> false
        }

        val isDefeat = when (s.mode) {
            GameMode.SAGA, GameMode.DAILY -> !isVictory && (dangerReached || nextMoves <= 0)
            GameMode.ENDLESS, GameMode.BLITZ -> dangerReached
        }

        val isSagaOrDaily = s.mode == GameMode.SAGA || s.mode == GameMode.DAILY
        val moveBonus = if (isVictory && isSagaOrDaily) nextMoves * 150L else 0L
        val fireworksBonusCoins = if (isVictory && isSagaOrDaily && nextMoves > 0) nextMoves * 3L else 0L
        val finalScore = s.score + gainedScore + bossClearBonusScore + moveBonus
        val totalPopped = s.totalPoppedInRun + matchedCount

        val allowed = if (isSagaOrDaily) s.levelSpec.allowedColors else BubbleColor.entries
        val activeColors = availableBoardColors(newGrid, allowed)

        val upcomingThird = if (feverBonusShot != null) {
            LauncherShot(activeColors.random(), feverBonusShot)
        } else {
            LauncherShot(activeColors.random(), ShotType.COLORED)
        }
        val queuedCurrent = if (s.nextShot.color in activeColors || s.nextShot.shotType != ShotType.COLORED) {
            s.nextShot
        } else {
            LauncherShot(activeColors.random(), ShotType.COLORED)
        }
        val queuedNext = if (s.thirdShot.color in activeColors || s.thirdShot.shotType != ShotType.COLORED) {
            s.thirdShot
        } else {
            LauncherShot(activeColors.random(), ShotType.COLORED)
        }

        val nextStatus = when {
            isVictory -> GameStatus.WON
            isDefeat -> GameStatus.LOST
            else -> GameStatus.PLAYING
        }

        val starsEarned = if (isVictory && isSagaOrDaily) {
            val (s1, s2, s3) = s.levelSpec.starThresholds
            when {
                finalScore >= s3 -> 3
                finalScore >= s2 -> 2
                finalScore >= s1 -> 1
                else -> 1
            }
        } else 0

        val rewardBooster = if (isVictory && s.mode == GameMode.SAGA && starsEarned == 3) {
            BoosterType.entries[s.levelSpec.levelNumber % BoosterType.entries.size]
        } else null

        val baseCoinsEarned = when {
            nextStatus == GameStatus.WON && s.mode == GameMode.DAILY -> 120L + starsEarned * 15L + fireworksBonusCoins
            nextStatus == GameStatus.WON && s.mode == GameMode.SAGA -> 35L + starsEarned * 15L + bossClearBonusCoins + fireworksBonusCoins
            nextStatus == GameStatus.LOST && (s.mode == GameMode.ENDLESS || s.mode == GameMode.BLITZ) -> (totalPopped / 3L).coerceAtLeast(10L)
            nextStatus == GameStatus.LOST -> (totalPopped / 5L).coerceAtLeast(5L)
            else -> 0L
        }
        val coinsEarned = baseCoinsEarned + s.wellBonusCoins

        if (nextStatus == GameStatus.WON) {
            soundSynth.playVictory()
            if (nextMoves > 0 && s.mode != GameMode.ENDLESS) {
                spawnPopParticles(mutableParticles, shooterPos, AstroGold, 22, isStar = true)
                mutableShockwaves.add(ShockwaveFx(shooterPos, boardWidth * 0.6f, AstroGold))
                mutablePopups.add(
                    FloatingPopup(
                        ++popupCounter,
                        shooterPos.x,
                        shooterPos.y - 80f,
                        "FIREWORKS! +$moveBonus & +${fireworksBonusCoins}c",
                        AstroGold
                    )
                )
            }
        } else if (nextStatus == GameStatus.LOST) {
            soundSynth.playGameOver()
        } else if (nextCombo >= 3 && didClear) {
            val hypeText = when {
                nextCombo >= 8 -> "COSMIC GODLIKE ${nextCombo}x!"
                nextCombo >= 5 -> "SUPERNOVA ${nextCombo}x!"
                else -> "STELLAR ${nextCombo}x!"
            }
            mutablePopups.add(
                FloatingPopup(++popupCounter, shooterPos.x, shooterPos.y - 110f, hypeText, AstroGold)
            )
        }

        if (feverBonusShot != null && nextStatus == GameStatus.PLAYING) {
            mutablePopups.add(
                FloatingPopup(++popupCounter, shooterPos.x, shooterPos.y - 60f, "OVERDRIVE 2x!", AstroMagenta)
            )
        }

        _renderState.value = _renderState.value.copy(
            grid = newGrid,
            flyingBubble = null,
            fallingBubbles = mutableFalling,
            particles = mutableParticles.takeLast(MAX_PARTICLES),
            popups = mutablePopups,
            lightningArcs = arcs,
            portalFx = portalFx,
            shockwaves = mutableShockwaves,
            bossHitFlash = bossFlash,
            screenShake = extraShake
        )

        val canAdSecondChance = nextStatus == GameStatus.LOST &&
            s.mode != GameMode.ENDLESS &&
            s.mode != GameMode.BLITZ &&
            !dangerReached &&
            !s.usedSecondChance
        val canSecondChance = canAdSecondChance && s.starCoins >= SECOND_CHANCE_COST

        val exposedColors = newGrid.values
            .filter { it.row >= (newGrid.keys.maxOfOrNull { k -> k.first } ?: 0) - 2 }
            .map { it.color }
            .toSet()
        val suggestSwap = nextStatus == GameStatus.PLAYING &&
            queuedCurrent.shotType == ShotType.COLORED &&
            queuedCurrent.color !in exposedColors &&
            queuedNext.shotType == ShotType.COLORED &&
            queuedNext.color in exposedColors

        _state.update {
            it.copy(
                currentShot = queuedCurrent,
                nextShot = queuedNext,
                thirdShot = upcomingThird,
                score = finalScore,
                movesRemaining = nextMoves,
                endlessShotsUntilDescent = nextShotsUntilDescent,
                endlessWave = nextWave,
                comboStreak = nextCombo,
                maxCombo = maxCombo,
                feverProgress = nextFever,
                overdriveTurnsRemaining = nextOverdrive,
                totalPoppedInRun = totalPopped,
                starsRemaining = starsLeft,
                bossHp = nextBossHp,
                bossCounterCountdown = nextBossCountdown,
                status = nextStatus,
                earnedStars = starsEarned,
                earnedBooster = rewardBooster,
                coinsEarnedThisRun = coinsEarned,
                canBuySecondChance = canSecondChance,
                canWatchAdSecondChance = canAdSecondChance,
                isDangerPanic = panic,
                swapHintPulse = suggestSwap,
                hapticPulseId = if (didClear && it.hapticsEnabled) it.hapticPulseId + 1 else it.hapticPulseId
            )
        }

        recomputeTrajectory()

        if ((nextStatus == GameStatus.WON || nextStatus == GameStatus.LOST) && !resultSaved && !canSecondChance && !canAdSecondChance) {
            saveRunNow(nextStatus == GameStatus.WON)
        }
    }

    private fun finalizeBlitzTimeUp() {
        val s = _state.value
        if (s.status != GameStatus.PLAYING || s.mode != GameMode.BLITZ) return
        soundSynth.playVictory()
        val coinsEarned = (s.score / 110L).coerceIn(25L, 400L) + s.wellBonusCoins
        val rf = _renderState.value
        val particles = rf.particles.toMutableList()
        val shockwaves = rf.shockwaves.toMutableList()
        val popups = rf.popups.toMutableList()
        spawnPopParticles(particles, shooterPos, AstroGold, 24, isStar = true)
        shockwaves.add(ShockwaveFx(shooterPos, boardWidth * 0.6f, AstroGold))
        popups.add(FloatingPopup(++popupCounter, shooterPos.x, shooterPos.y - 80f, "TIME UP! +${coinsEarned}c", AstroGold))
        _renderState.value = rf.copy(
            flyingBubble = null,
            particles = particles.takeLast(MAX_PARTICLES),
            shockwaves = shockwaves,
            popups = popups
        )
        _state.update {
            it.copy(
                status = GameStatus.WON,
                blitzSecondsRemaining = 0,
                coinsEarnedThisRun = coinsEarned,
                canBuySecondChance = false,
                canWatchAdSecondChance = false
            )
        }
        if (!resultSaved) {
            saveRunNow(won = true)
        }
    }

    fun finalizeDefeatWithoutSecondChance() {
        if (!resultSaved && _state.value.status == GameStatus.LOST) {
            saveRunNow(won = false)
        }
    }

    private fun saveRunNow(won: Boolean) {
        resultSaved = true
        val s = _state.value
        viewModelScope.launch {
            val result = when (s.mode) {
                GameMode.SAGA -> store.recordSagaLevelResult(
                    levelNumber = s.levelSpec.levelNumber,
                    score = s.score,
                    stars = s.earnedStars,
                    maxCombo = s.maxCombo,
                    bubblesPopped = s.totalPoppedInRun,
                    won = won,
                    rewardBooster = s.earnedBooster,
                    coinsEarned = s.coinsEarnedThisRun
                )
                GameMode.DAILY -> store.recordDailyChallengeResult(
                    dayKey = activeDayKey.ifBlank { LevelCatalog.todayKey() },
                    previousDayKey = LevelCatalog.yesterdayKey(),
                    score = s.score,
                    stars = s.earnedStars,
                    maxCombo = s.maxCombo,
                    bubblesPopped = s.totalPoppedInRun,
                    won = won,
                    coinsEarned = s.coinsEarnedThisRun
                )
                GameMode.ENDLESS -> store.recordEndlessResult(
                    score = s.score,
                    maxCombo = s.maxCombo,
                    bubblesPopped = s.totalPoppedInRun,
                    coinsEarned = s.coinsEarnedThisRun
                )
                GameMode.BLITZ -> store.recordBlitzResult(
                    score = s.score,
                    maxCombo = s.maxCombo,
                    bubblesPopped = s.totalPoppedInRun,
                    coinsEarned = s.coinsEarnedThisRun
                )
            }
            if (result.isFailure) {
                _state.update {
                    it.copy(syncErrorMessage = "Save paused: ${result.exceptionOrNull()?.localizedMessage ?: "offline"}")
                }
            }
        }
    }

    private fun shiftEndlessGridDown(grid: MutableMap<Pair<Int, Int>, GridCell>, waveIndex: Int) {
        val shifted = mutableMapOf<Pair<Int, Int>, GridCell>()
        for ((key, cell) in grid) {
            val nr = key.first + 2
            if (nr < HexGridEngine.MAX_ROWS) {
                shifted[nr to key.second] = cell.copy(row = nr)
            }
        }
        val activeColors = availableBoardColors(shifted, BubbleColor.entries)
        val topRows = LevelCatalog.generateEndlessTopRows(waveIndex, activeColors)
        grid.clear()
        grid.putAll(topRows)
        grid.putAll(shifted)
    }

    private fun availableBoardColors(
        grid: Map<Pair<Int, Int>, GridCell>,
        fallback: List<BubbleColor>
    ): List<BubbleColor> {
        val present = grid.values
            .filter {
                it.kind == TileKind.NORMAL ||
                    it.kind == TileKind.ICE ||
                    it.kind == TileKind.SHIELDED ||
                    it.kind == TileKind.CHAMELEON
            }
            .map { it.color }
            .distinct()
        return if (present.isNotEmpty()) present else fallback
    }

    private fun checkPanic(grid: Map<Pair<Int, Int>, GridCell>): Boolean =
        grid.keys.any { it.first >= HexGridEngine.DANGER_ROW - 2 }

    private fun recomputeTrajectory() {
        val s = _state.value
        val rf = _renderState.value
        if (boardWidth <= 0f || shooterPos == Offset.Zero) return
        val radius = HexGridEngine.bubbleRadius(boardWidth)
        val traj = HexGridEngine.raycastTrajectory(
            startX = shooterPos.x,
            startY = shooterPos.y,
            angleRad = rf.aimAngleRad,
            boardWidth = boardWidth,
            radius = radius,
            topOffset = topOffset,
            grid = rf.grid,
            shotType = s.currentShot.shotType,
            shotColor = s.currentShot.color
        )
        _renderState.value = rf.copy(trajectory = traj)
    }

    private fun spawnPopParticles(
        target: MutableList<PopParticle>,
        center: Offset,
        color: Color,
        count: Int,
        isStar: Boolean = false
    ) {
        if (target.size >= MAX_PARTICLES) return
        val allowed = (MAX_PARTICLES - target.size).coerceAtMost(count)
        for (i in 0 until allowed) {
            val angle = Random.nextFloat() * (2f * PI).toFloat()
            val speed = 120f + Random.nextFloat() * 340f
            target.add(
                PopParticle(
                    x = center.x,
                    y = center.y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 80f,
                    radius = if (isStar) 7f else 4f + Random.nextFloat() * 5f,
                    color = color,
                    life = 1f,
                    isStar = isStar
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        loopJob?.cancel()
    }
}
