package com.example

import com.example.game.BubbleColor
import com.example.game.GridCell
import com.example.game.HexGridEngine
import com.example.game.LevelCatalog
import com.example.game.LevelGoal
import com.example.game.SagaLevelSpec
import com.example.game.ShotType
import com.example.game.TileKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class HexGridEngineTest {

    @Test
    fun colorMatchCluster_findsConnectedSameColorNormalBubbles() {
        val grid = mapOf(
            (0 to 0) to GridCell(0, 0, BubbleColor.CYAN),
            (0 to 1) to GridCell(0, 1, BubbleColor.CYAN),
            (1 to 0) to GridCell(1, 0, BubbleColor.CYAN),
            (1 to 1) to GridCell(1, 1, BubbleColor.MAGENTA),
            (0 to 2) to GridCell(0, 2, BubbleColor.CYAN, TileKind.ICE) // ICE shouldn't auto-pop in color match
        )
        val cluster = HexGridEngine.findColorMatchCluster(0, 0, BubbleColor.CYAN, grid)
        assertEquals(setOf(0 to 0, 0 to 1, 1 to 0), cluster)
    }

    @Test
    fun floatingDetachedCells_detectsIslandsDisconnectedFromCeiling() {
        val grid = mapOf(
            (0 to 0) to GridCell(0, 0, BubbleColor.CYAN),
            (1 to 0) to GridCell(1, 0, BubbleColor.MAGENTA),
            // Floating island at row 3 not connected to row 0
            (3 to 2) to GridCell(3, 2, BubbleColor.GOLD),
            (4 to 2) to GridCell(4, 2, BubbleColor.VIOLET, TileKind.STONE)
        )
        val detached = HexGridEngine.findFloatingDetachedCells(grid)
        assertEquals(setOf(3 to 2, 4 to 2), detached)
        assertFalse((0 to 0) in detached)
        assertFalse((1 to 0) in detached)
    }

    @Test
    fun raycastTrajectory_bouncesOffWallAndFindsSnapCell() {
        val width = 800f
        val radius = HexGridEngine.bubbleRadius(width)
        val grid = mapOf(
            (0 to 0) to GridCell(0, 0, BubbleColor.CYAN),
            (0 to 1) to GridCell(0, 1, BubbleColor.CYAN)
        )
        // Aim angled left so it bounces off left wall
        val angle = (-PI * 0.80).toFloat()
        val traj = HexGridEngine.raycastTrajectory(
            startX = width / 2f,
            startY = 1100f,
            angleRad = angle,
            boardWidth = width,
            radius = radius,
            topOffset = 20f,
            grid = grid,
            shotType = ShotType.COLORED
        )
        assertTrue(traj.points.size >= 3)
        assertTrue(traj.snapCell != null)
    }

    @Test
    fun chameleonTiles_joinColorMatchClusterWhenSameColor() {
        val grid = mapOf(
            (0 to 0) to GridCell(0, 0, BubbleColor.GOLD),
            (0 to 1) to GridCell(0, 1, BubbleColor.GOLD, TileKind.CHAMELEON),
            (1 to 0) to GridCell(1, 0, BubbleColor.GOLD),
            (1 to 1) to GridCell(1, 1, BubbleColor.CYAN, TileKind.CHAMELEON)
        )
        val cluster = HexGridEngine.findColorMatchCluster(0, 0, BubbleColor.GOLD, grid)
        assertEquals(setOf(0 to 0, 0 to 1, 1 to 0), cluster)
    }

    @Test
    fun portalPair_warpsRaycastTrajectoryToPartnerExit() {
        val width = 800f
        val radius = HexGridEngine.bubbleRadius(width)
        val topOffset = 20f
        val entry = 3 to 3
        val exit = 1 to 4
        val grid = mapOf(
            entry to GridCell(entry.first, entry.second, BubbleColor.MAGENTA, TileKind.PORTAL),
            exit to GridCell(exit.first, exit.second, BubbleColor.MAGENTA, TileKind.PORTAL)
        )
        assertEquals(exit, HexGridEngine.findPartnerPortal(entry, grid))

        val entryCenter = HexGridEngine.cellCenter(entry.first, entry.second, width, radius, topOffset)
        val dx = entryCenter.x - width / 2f
        val dy = entryCenter.y - 1100f
        val angle = kotlin.math.atan2(dy, dx)
        val traj = HexGridEngine.raycastTrajectory(
            startX = width / 2f,
            startY = 1100f,
            angleRad = angle,
            boardWidth = width,
            radius = radius,
            topOffset = topOffset,
            grid = grid,
            shotType = ShotType.COLORED
        )
        val exitCenter = HexGridEngine.cellCenter(exit.first, exit.second, width, radius, topOffset)
        assertTrue(traj.points.any { kotlin.math.hypot(it.x - exitCenter.x, it.y - exitCenter.y) < 1f })
    }

    @Test
    fun computeRowCells_returnsOnlyCellsInTargetRow() {
        val grid = mapOf(
            (2 to 0) to GridCell(2, 0, BubbleColor.CYAN),
            (2 to 3) to GridCell(2, 3, BubbleColor.GOLD),
            (1 to 1) to GridCell(1, 1, BubbleColor.MAGENTA)
        )
        assertEquals(setOf(2 to 0, 2 to 3), HexGridEngine.computeRowCells(2, grid))
    }

    @Test
    fun raycastTrajectory_highlightsClusterOnlyWhenThreeOrMoreMatch() {
        val width = 800f
        val radius = HexGridEngine.bubbleRadius(width)
        val topOffset = 20f
        // Two connected CYAN bubbles near top center (snap + 2 = 3+ match)
        val matchGrid = mapOf(
            (0 to 3) to GridCell(0, 3, BubbleColor.CYAN),
            (0 to 4) to GridCell(0, 4, BubbleColor.CYAN)
        )
        val targetCenter = HexGridEngine.cellCenter(0, 3, width, radius, topOffset)
        val angle = kotlin.math.atan2(targetCenter.y - 1100f, targetCenter.x - width / 2f)
        val trajMatch = HexGridEngine.raycastTrajectory(
            startX = width / 2f,
            startY = 1100f,
            angleRad = angle,
            boardWidth = width,
            radius = radius,
            topOffset = topOffset,
            grid = matchGrid,
            shotType = ShotType.COLORED,
            shotColor = BubbleColor.CYAN
        )
        assertTrue(trajMatch.snapCell != null)
        assertTrue(trajMatch.highlightCells.containsAll(setOf(0 to 3, 0 to 4)))

        // Single CYAN bubble (snap + 1 = 2 < 3 -> no highlight)
        val nonMatchGrid = mapOf(
            (0 to 3) to GridCell(0, 3, BubbleColor.CYAN),
            (0 to 4) to GridCell(0, 4, BubbleColor.MAGENTA)
        )
        val trajNoMatch = HexGridEngine.raycastTrajectory(
            startX = width / 2f,
            startY = 1100f,
            angleRad = angle,
            boardWidth = width,
            radius = radius,
            topOffset = topOffset,
            grid = nonMatchGrid,
            shotType = ShotType.COLORED,
            shotColor = BubbleColor.CYAN
        )
        assertTrue(trajNoMatch.snapCell != null)
        assertTrue(trajNoMatch.highlightCells.isEmpty())
    }

    @Test
    fun shieldedTiles_doNotJoinColorMatchClusterUntilCracked() {
        val grid = mapOf(
            (0 to 0) to GridCell(0, 0, BubbleColor.CYAN),
            (0 to 1) to GridCell(0, 1, BubbleColor.CYAN),
            (1 to 0) to GridCell(1, 0, BubbleColor.CYAN, TileKind.SHIELDED)
        )
        val cluster = HexGridEngine.findColorMatchCluster(0, 0, BubbleColor.CYAN, grid)
        assertEquals(setOf(0 to 0, 0 to 1), cluster)
    }

    @Test
    fun levelCatalog_has30StagesAcross4SectorsWithSupernovaBossesAndShieldedTokens() {
        val levels = LevelCatalog.sagaLevels
        assertEquals(30, levels.size)
        assertEquals((1..30).toList(), levels.map { it.levelNumber })
        assertEquals("Supernova Core", LevelCatalog.getLevel(19).chapter)
        assertEquals("Solar Titan", LevelCatalog.getLevel(24).bossSpec?.name)
        assertEquals(52, LevelCatalog.getLevel(24).bossSpec?.maxHp)
        assertEquals("Omega Singularity", LevelCatalog.getLevel(30).bossSpec?.name)
        assertEquals(65, LevelCatalog.getLevel(30).bossSpec?.maxHp)

        val customSpec = SagaLevelSpec(
            levelNumber = 99,
            chapter = "Test",
            title = "Shield Test",
            subtitle = "Test",
            goal = LevelGoal.CLEAR_BOARD,
            movesLimit = 10,
            starThresholds = Triple(100, 200, 300),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA),
            rowsPattern = listOf("AC AM C")
        )
        val parsed = LevelCatalog.buildGridForLevel(customSpec)
        assertEquals(TileKind.SHIELDED, parsed[0 to 0]?.kind)
        assertEquals(BubbleColor.CYAN, parsed[0 to 0]?.color)
        assertEquals(TileKind.SHIELDED, parsed[0 to 1]?.kind)
        assertEquals(BubbleColor.MAGENTA, parsed[0 to 1]?.color)
        assertEquals(TileKind.NORMAL, parsed[0 to 2]?.kind)
    }
}
