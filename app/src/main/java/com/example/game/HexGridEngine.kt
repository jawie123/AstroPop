package com.example.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object HexGridEngine {
    const val COLS_EVEN = 8
    const val COLS_ODD = 7
    const val MAX_ROWS = 14
    const val DANGER_ROW = 11
    private val SQRT3 = sqrt(3f)

    fun colsForRow(row: Int): Int = if (row % 2 == 0) COLS_EVEN else COLS_ODD

    fun bubbleRadius(boardWidth: Float): Float = (boardWidth / (COLS_EVEN * 2f + 0.5f)).coerceAtLeast(10f)

    fun rowSpacing(radius: Float): Float = radius * SQRT3

    fun cellCenter(
        row: Int,
        col: Int,
        boardWidth: Float,
        radius: Float,
        topOffset: Float
    ): Offset {
        val gridWidth = COLS_EVEN * 2f * radius
        val leftMargin = (boardWidth - gridWidth) / 2f
        val xOffset = if (row % 2 == 0) radius else 2f * radius
        val x = leftMargin + xOffset + col * 2f * radius
        val y = topOffset + radius + row * rowSpacing(radius)
        return Offset(x, y)
    }

    fun neighbors(row: Int, col: Int): List<Pair<Int, Int>> {
        val offsets = if (row % 2 == 0) {
            listOf(
                -1 to -1, -1 to 0,
                0 to -1, 0 to 1,
                1 to -1, 1 to 0
            )
        } else {
            listOf(
                -1 to 0, -1 to 1,
                0 to -1, 0 to 1,
                1 to 0, 1 to 1
            )
        }
        return offsets.mapNotNull { (dr, dc) ->
            val nr = row + dr
            val nc = col + dc
            if (nr in 0 until MAX_ROWS && nc in 0 until colsForRow(nr)) nr to nc else null
        }
    }

    fun findNearestEmptyCell(
        x: Float,
        y: Float,
        boardWidth: Float,
        radius: Float,
        topOffset: Float,
        grid: Map<Pair<Int, Int>, GridCell>
    ): Pair<Int, Int>? {
        val dy = rowSpacing(radius)
        val estRow = ((y - topOffset - radius) / dy).roundToInt().coerceIn(0, MAX_ROWS - 1)
        val rowRange = (estRow - 2).coerceAtLeast(0)..(estRow + 2).coerceAtMost(MAX_ROWS - 1)

        var bestAttached: Pair<Int, Int>? = null
        var bestAttachedDist = Float.MAX_VALUE
        var bestAny: Pair<Int, Int>? = null
        var bestAnyDist = Float.MAX_VALUE

        for (r in rowRange) {
            for (c in 0 until colsForRow(r)) {
                val key = r to c
                if (grid.containsKey(key)) continue
                val center = cellCenter(r, c, boardWidth, radius, topOffset)
                val dist = hypot(center.x - x, center.y - y)
                val isAnchoredOrAdjacent = r == 0 || neighbors(r, c).any { grid.containsKey(it) }
                if (isAnchoredOrAdjacent && dist < bestAttachedDist) {
                    bestAttachedDist = dist
                    bestAttached = key
                }
                if (dist < bestAnyDist) {
                    bestAnyDist = dist
                    bestAny = key
                }
            }
        }
        return bestAttached ?: bestAny
    }

    fun findPartnerPortal(
        entryKey: Pair<Int, Int>,
        grid: Map<Pair<Int, Int>, GridCell>
    ): Pair<Int, Int>? {
        return grid.entries.firstOrNull { (k, cell) ->
            k != entryKey && cell.kind == TileKind.PORTAL
        }?.key
    }

    fun raycastTrajectory(
        startX: Float,
        startY: Float,
        angleRad: Float,
        boardWidth: Float,
        radius: Float,
        topOffset: Float,
        grid: Map<Pair<Int, Int>, GridCell>,
        shotType: ShotType,
        maxBounces: Int = 2,
        shotColor: BubbleColor? = null
    ): AimTrajectory {
        if (boardWidth <= 0f) return AimTrajectory()
        val leftWall = (boardWidth - COLS_EVEN * 2f * radius) / 2f + radius
        val rightWall = boardWidth - leftWall
        val stepSize = (radius * 0.28f).coerceAtLeast(3f)
        val hitDist = radius * 1.82f

        var x = startX
        var y = startY
        var vx = cos(angleRad)
        var vy = sin(angleRad)
        if (vy > -0.15f) vy = -0.15f

        val points = mutableListOf(Offset(x, y))
        var bounces = 0
        var warped = false

        if (shotType == ShotType.LASER) {
            val pierced = mutableSetOf<Pair<Int, Int>>()
            var steps = 0
            while (y > topOffset && steps < 900) {
                steps++
                x += vx * stepSize
                y += vy * stepSize
                if (x <= leftWall) {
                    x = leftWall + (leftWall - x)
                    vx = -vx
                    points.add(Offset(x, y))
                    bounces++
                    if (bounces > maxBounces) break
                } else if (x >= rightWall) {
                    x = rightWall - (x - rightWall)
                    vx = -vx
                    points.add(Offset(x, y))
                    bounces++
                    if (bounces > maxBounces) break
                }
                for ((key, cell) in grid) {
                    if (cell.kind == TileKind.PORTAL) continue
                    val c = cellCenter(key.first, key.second, boardWidth, radius, topOffset)
                    if (hypot(c.x - x, c.y - y) <= radius * 1.25f) {
                        pierced.add(key)
                    }
                }
            }
            points.add(Offset(x, y.coerceAtLeast(topOffset)))
            return AimTrajectory(points = points, snapCell = null, highlightCells = pierced)
        }

        var hitPoint: Offset? = null
        var steps = 0
        while (steps < 900) {
            steps++
            val prevX = x
            val prevY = y
            x += vx * stepSize
            y += vy * stepSize

            if (x <= leftWall) {
                x = leftWall + (leftWall - x)
                vx = -vx
                bounces++
                points.add(Offset(leftWall, y))
                if (bounces > maxBounces) {
                    hitPoint = Offset(x, y)
                    break
                }
            } else if (x >= rightWall) {
                x = rightWall - (x - rightWall)
                vx = -vx
                bounces++
                points.add(Offset(rightWall, y))
                if (bounces > maxBounces) {
                    hitPoint = Offset(x, y)
                    break
                }
            }

            if (y <= topOffset + radius) {
                hitPoint = Offset(x, topOffset + radius)
                break
            }

            var collided = false
            for ((key, cell) in grid) {
                val c = cellCenter(key.first, key.second, boardWidth, radius, topOffset)
                if (hypot(c.x - x, c.y - y) <= hitDist) {
                    if (!warped && cell.kind == TileKind.PORTAL) {
                        val partner = findPartnerPortal(key, grid)
                        if (partner != null) {
                            warped = true
                            points.add(c)
                            val exitCenter = cellCenter(partner.first, partner.second, boardWidth, radius, topOffset)
                            x = exitCenter.x + vx * radius * 2.1f
                            y = exitCenter.y + vy * radius * 2.1f
                            points.add(exitCenter)
                            break
                        }
                    }
                    hitPoint = Offset(prevX, prevY)
                    collided = true
                    break
                }
            }
            if (collided) break
        }

        val finalPt = hitPoint ?: Offset(x, y)
        points.add(finalPt)
        val snap = findNearestEmptyCell(finalPt.x, finalPt.y, boardWidth, radius, topOffset, grid)
        val highlights = when {
            shotType == ShotType.BOMB && snap != null -> computeBombBlastCells(snap.first, snap.second, grid)
            shotType == ShotType.COLORED && shotColor != null && snap != null -> {
                val cluster = findColorMatchCluster(snap.first, snap.second, shotColor, grid)
                if (cluster.size >= 3) cluster - snap else emptySet()
            }
            else -> emptySet()
        }
        return AimTrajectory(points = points, snapCell = snap, highlightCells = highlights)
    }

    fun findColorMatchCluster(
        startRow: Int,
        startCol: Int,
        targetColor: BubbleColor,
        grid: Map<Pair<Int, Int>, GridCell>
    ): Set<Pair<Int, Int>> {
        val start = startRow to startCol
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (next in neighbors(current.first, current.second)) {
                if (next in visited) continue
                val cell = grid[next] ?: continue
                val isMatchable = cell.kind == TileKind.NORMAL || cell.kind == TileKind.CHAMELEON
                if (isMatchable && cell.color == targetColor) {
                    visited.add(next)
                    queue.add(next)
                }
            }
        }
        return visited
    }

    fun findFloatingDetachedCells(grid: Map<Pair<Int, Int>, GridCell>): Set<Pair<Int, Int>> {
        if (grid.isEmpty()) return emptySet()
        val anchored = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        for ((key, _) in grid) {
            if (key.first == 0) {
                anchored.add(key)
                queue.add(key)
            }
        }

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            for (next in neighbors(curr.first, curr.second)) {
                if (next !in anchored && grid.containsKey(next)) {
                    anchored.add(next)
                    queue.add(next)
                }
            }
        }

        return grid.keys.filter { it !in anchored }.toSet()
    }

    fun computeBombBlastCells(
        centerRow: Int,
        centerCol: Int,
        grid: Map<Pair<Int, Int>, GridCell>
    ): Set<Pair<Int, Int>> {
        val result = mutableSetOf<Pair<Int, Int>>()
        val ring1 = neighbors(centerRow, centerCol)
        for (cell in ring1) {
            if (grid.containsKey(cell)) result.add(cell)
            for (ring2 in neighbors(cell.first, cell.second)) {
                if (grid.containsKey(ring2)) result.add(ring2)
            }
        }
        if (grid.containsKey(centerRow to centerCol)) {
            result.add(centerRow to centerCol)
        }
        return result
    }

    fun computeRowCells(row: Int, grid: Map<Pair<Int, Int>, GridCell>): Set<Pair<Int, Int>> =
        grid.keys.filter { it.first == row }.toSet()

    fun scoreWellMultiplier(x: Float, boardWidth: Float): Int {
        if (boardWidth <= 0f) return 1
        val fraction = (x / boardWidth).coerceIn(0f, 0.999f)
        val bucket = (fraction * 5f).toInt()
        return when (bucket) {
            0, 4 -> 1
            1, 3 -> 2
            2 -> 5
            else -> 1
        }
    }
}
