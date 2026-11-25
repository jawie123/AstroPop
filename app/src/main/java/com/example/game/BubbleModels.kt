package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.data.BoosterType
import com.example.ui.theme.AstroCoral
import com.example.ui.theme.AstroCyan
import com.example.ui.theme.AstroEmerald
import com.example.ui.theme.AstroGold
import com.example.ui.theme.AstroMagenta
import com.example.ui.theme.AstroViolet

enum class BubbleColor(
    val code: Char,
    val label: String,
    val primary: Color,
    val highlight: Color,
    val shadow: Color
) {
    CYAN('C', "Cyan", AstroCyan, Color(0xFFA6F8FF), Color(0xFF0088AA)),
    MAGENTA('M', "Magenta", AstroMagenta, Color(0xFFFFA8D5), Color(0xFF9E0B56)),
    GOLD('G', "Gold", AstroGold, Color(0xFFFFF0A8), Color(0xFFB37B00)),
    EMERALD('E', "Emerald", AstroEmerald, Color(0xFFA7FFE0), Color(0xFF068E5D)),
    VIOLET('V', "Violet", AstroViolet, Color(0xFFE0C2FF), Color(0xFF5D21A8)),
    CORAL('R', "Coral", AstroCoral, Color(0xFFFFC1B0), Color(0xFFA82C11));

    companion object {
        fun fromCode(ch: Char): BubbleColor? = entries.firstOrNull { it.code == ch.uppercaseChar() }
    }
}

enum class TileKind {
    NORMAL,
    ICE,
    STONE,
    STAR_CORE,
    LIGHTNING,
    CHAMELEON,
    BLACK_HOLE,
    PORTAL
}

enum class ShotType {
    COLORED,
    BOMB,
    PRISM,
    LASER
}

data class GridCell(
    val row: Int,
    val col: Int,
    val color: BubbleColor,
    val kind: TileKind = TileKind.NORMAL,
    val charge: Int = 0,
    val wobble: Float = 0f
)

data class LauncherShot(
    val color: BubbleColor,
    val shotType: ShotType = ShotType.COLORED
) {
    fun asBoosterOrNull(): BoosterType? = when (shotType) {
        ShotType.COLORED -> null
        ShotType.BOMB -> BoosterType.BOMB
        ShotType.PRISM -> BoosterType.PRISM
        ShotType.LASER -> BoosterType.LASER
    }
}

data class FlyingBubble(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val shot: LauncherShot,
    val trail: List<Offset> = emptyList(),
    val portalCooldown: Float = 0f
)

data class FallingBubble(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: BubbleColor,
    val kind: TileKind,
    val rotation: Float = 0f,
    val scoredInWell: Boolean = false
)

data class PopParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val color: Color,
    val life: Float = 1f,
    val isStar: Boolean = false
)

data class FloatingPopup(
    val id: Long,
    val x: Float,
    val y: Float,
    val text: String,
    val color: Color,
    val life: Float = 1f
)

data class LightningArc(
    val y: Float,
    val leftX: Float,
    val rightX: Float,
    val life: Float = 1f
)

data class PortalWarpFx(
    val from: Offset,
    val to: Offset,
    val life: Float = 1f
)

data class ShockwaveFx(
    val center: Offset,
    val maxRadius: Float,
    val color: Color,
    val life: Float = 1f
)

data class BossSpec(
    val name: String,
    val maxHp: Int,
    val counterInterval: Int,
    val accentColor: Color
)

data class AimTrajectory(
    val points: List<Offset> = emptyList(),
    val snapCell: Pair<Int, Int>? = null,
    val highlightCells: Set<Pair<Int, Int>> = emptySet(),
    val isCancelled: Boolean = false
)

/**
 * High-frequency 60fps Canvas render snapshot.
 * Isolated from GameUiState so per-frame projectile/particle updates
 * invalidate only the Canvas Draw phase without recomposing HUD composables.
 */
data class RenderFrameState(
    val grid: Map<Pair<Int, Int>, GridCell> = emptyMap(),
    val flyingBubble: FlyingBubble? = null,
    val fallingBubbles: List<FallingBubble> = emptyList(),
    val particles: List<PopParticle> = emptyList(),
    val popups: List<FloatingPopup> = emptyList(),
    val lightningArcs: List<LightningArc> = emptyList(),
    val portalFx: List<PortalWarpFx> = emptyList(),
    val shockwaves: List<ShockwaveFx> = emptyList(),
    val bossHitFlash: Float = 0f,
    val aimAngleRad: Float = (-Math.PI / 2.0).toFloat(),
    val isAiming: Boolean = false,
    val trajectory: AimTrajectory = AimTrajectory(),
    val screenShake: Float = 0f
)
