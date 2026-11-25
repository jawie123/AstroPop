package com.example.game

import com.example.ui.theme.AstroCyan
import com.example.ui.theme.AstroMagenta
import com.example.ui.theme.AstroViolet
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

enum class LevelGoal {
    RESCUE_STARS,
    CLEAR_BOARD
}

data class SagaLevelSpec(
    val levelNumber: Int,
    val chapter: String,
    val title: String,
    val subtitle: String,
    val goal: LevelGoal,
    val movesLimit: Int,
    val starThresholds: Triple<Int, Int, Int>,
    val allowedColors: List<BubbleColor>,
    val rowsPattern: List<String>,
    val bossSpec: BossSpec? = null
)

object LevelCatalog {
    // Pattern token legend:
    // C/M/G/E/V/R = normal color bubble
    // c/m/g/e/v/r = ICE-encased bubble (lowercase)
    // X           = STONE asteroid
    // S           = STAR_CORE rescue orb
    // L           = LIGHTNING row-zapper
    // H           = CHAMELEON color-morphing bubble
    // B           = BLACK_HOLE (absorbs 1 hit, implodes on 2nd!)
    // P           = WARP PORTAL pair
    // .           = empty slot
    val sagaLevels: List<SagaLevelSpec> = listOf(
        SagaLevelSpec(
            levelNumber = 1,
            chapter = "Nebula Sector",
            title = "Nebula Nursery",
            subtitle = "Pop clusters to rescue the 2 Star Cores!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 18,
            starThresholds = Triple(900, 1800, 2800),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD),
            rowsPattern = listOf(
                "C C S M M S G G",
                " C C M M M G G ",
                "M M C C G G M M",
                " M C C G G M M "
            )
        ),
        SagaLevelSpec(
            levelNumber = 2,
            chapter = "Nebula Sector",
            title = "Twin Comets",
            subtitle = "Bank shots off the walls to drop whole wings!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 20,
            starThresholds = Triple(1200, 2400, 3600),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD),
            rowsPattern = listOf(
                "C S C M M G S G",
                " C C M M G G E ",
                "E E C C G G M M",
                " E C . . . G M ",
                "C C . . . . M M"
            )
        ),
        SagaLevelSpec(
            levelNumber = 3,
            chapter = "Nebula Sector",
            title = "Crystal Arch",
            subtitle = "Break through the archway to trigger a cosmic avalanche.",
            goal = LevelGoal.CLEAR_BOARD,
            movesLimit = 22,
            starThresholds = Triple(1400, 2600, 3900),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD),
            rowsPattern = listOf(
                "M M G G C C E E",
                " M G G C C E E ",
                "G . C C E E . M",
                " G . C E E . M ",
                "C C . . . . G G"
            )
        ),
        SagaLevelSpec(
            levelNumber = 4,
            chapter = "Nebula Sector",
            title = "Frost Belt",
            subtitle = "Pop next to Ice bubbles to crack them open!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 22,
            starThresholds = Triple(1500, 2800, 4200),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD),
            rowsPattern = listOf(
                "C S C E E G S G",
                " c c e e g g m ",
                "M M C C E E G G",
                " M m c c e g G ",
                "C C M M E E G G"
            )
        ),
        SagaLevelSpec(
            levelNumber = 5,
            chapter = "Nebula Sector",
            title = "Asteroid Bridge",
            subtitle = "Asteroids won't pop—cut their anchor above to drop them!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 24,
            starThresholds = Triple(1800, 3200, 4800),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.VIOLET),
            rowsPattern = listOf(
                "V V C C M M G G",
                " S X C M M X S ",
                "V X X C G X X G",
                " C C M M G G V ",
                "  C M M G G V  "
            )
        ),
        SagaLevelSpec(
            levelNumber = 6,
            chapter = "Nebula Sector",
            title = "BOSS: Nebula Hydra",
            subtitle = "Guardian Boss! Pop top clusters & combos to shatter its shield!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 25,
            starThresholds = Triple(2200, 3800, 5600),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD, BubbleColor.VIOLET),
            rowsPattern = listOf(
                "S C M G E V S V",
                " C M G E V C M ",
                "G E V C M G E V",
                " e v c m g e v ",
                "C C M M G G E E"
            ),
            bossSpec = BossSpec("Nebula Hydra", maxHp = 22, counterInterval = 4, accentColor = AstroMagenta)
        ),
        SagaLevelSpec(
            levelNumber = 7,
            chapter = "Ion Storm",
            title = "Thunder Surge",
            subtitle = "Trigger Lightning Zappers to fry an entire row!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 23,
            starThresholds = Triple(2200, 3900, 5600),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD),
            rowsPattern = listOf(
                "E E S C C S M M",
                " C L C M M L E ",
                "M M G G C C E E",
                " G G C C E E M ",
                "C C M M G G E E"
            )
        ),
        SagaLevelSpec(
            levelNumber = 8,
            chapter = "Ion Storm",
            title = "Prism Chameleons",
            subtitle = "Chameleon bubbles shift color every turn—time your shot!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 25,
            starThresholds = Triple(2400, 4200, 6000),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD, BubbleColor.VIOLET),
            rowsPattern = listOf(
                "S C M V V G E S",
                " C H M V G H E ",
                "M M C C G G E E",
                " V V H C M H G ",
                "C C M M V V G G"
            )
        ),
        SagaLevelSpec(
            levelNumber = 9,
            chapter = "Ion Storm",
            title = "Glacier Vault",
            subtitle = "Crack frozen shields and ignite dual Lightning rows.",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 25,
            starThresholds = Triple(2500, 4400, 6300),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD, BubbleColor.VIOLET),
            rowsPattern = listOf(
                "E E S C C S V V",
                " e c c m m v v ",
                "C L M M G L V V",
                " c m g g e v c ",
                "M M G G E E C C"
            )
        ),
        SagaLevelSpec(
            levelNumber = 10,
            chapter = "Ion Storm",
            title = "Meteor Gauntlet",
            subtitle = "Thread bank shots around twin asteroid pillars.",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 26,
            starThresholds = Triple(2700, 4600, 6500),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD, BubbleColor.CORAL),
            rowsPattern = listOf(
                "R R S E E S C C",
                " R X E E G X C ",
                "M X C L G G X E",
                " M C C G G E E ",
                "C C M M R R G G"
            )
        ),
        SagaLevelSpec(
            levelNumber = 11,
            chapter = "Ion Storm",
            title = "Solar Flare",
            subtitle = "Clear every last bubble before your tank runs dry!",
            goal = LevelGoal.CLEAR_BOARD,
            movesLimit = 26,
            starThresholds = Triple(2800, 4800, 6800),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.CORAL, BubbleColor.VIOLET),
            rowsPattern = listOf(
                "G G R R V V C C",
                " G R H V V H M ",
                "R V V C C M M G",
                " v c c m m g r ",
                "C C M M G G R R"
            )
        ),
        SagaLevelSpec(
            levelNumber = 12,
            chapter = "Ion Storm",
            title = "BOSS: Ion Empress",
            subtitle = "Defeat the Ion Empress & rescue 4 royal Star Cores!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 28,
            starThresholds = Triple(3200, 5400, 7800),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD, BubbleColor.VIOLET, BubbleColor.CORAL),
            rowsPattern = listOf(
                "S C M S S G E S",
                " c m v v g e r ",
                "C M X L L X E R",
                " M V C G E R C ",
                "V C G E R C M V"
            ),
            bossSpec = BossSpec("Ion Empress", maxHp = 32, counterInterval = 4, accentColor = AstroCyan)
        ),
        SagaLevelSpec(
            levelNumber = 13,
            chapter = "Singularity Void",
            title = "Event Horizon",
            subtitle = "Hit a Black Hole twice to implode surrounding bubbles!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 26,
            starThresholds = Triple(3200, 5400, 7600),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD, BubbleColor.VIOLET),
            rowsPattern = listOf(
                "C S M G G E S V",
                " C M B G E B V ",
                "M M G G E E V V",
                " C M G E V C M ",
                "G E V C M G E V"
            )
        ),
        SagaLevelSpec(
            levelNumber = 14,
            chapter = "Singularity Void",
            title = "Wormhole Shortcut",
            subtitle = "Shoot into a Warp Portal to teleport behind asteroids!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 26,
            starThresholds = Triple(3400, 5600, 7800),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD, BubbleColor.VIOLET),
            rowsPattern = listOf(
                "C S C M M S G G",
                " X X P M G X X ",
                "C C M M G G E E",
                " V V E E C C M ",
                "M M P . . C C G"
            )
        ),
        SagaLevelSpec(
            levelNumber = 15,
            chapter = "Singularity Void",
            title = "Cryo Citadel",
            subtitle = "Deep freeze meets Black Hole singularity cores.",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 28,
            starThresholds = Triple(3600, 5900, 8200),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD, BubbleColor.VIOLET, BubbleColor.CORAL),
            rowsPattern = listOf(
                "C S M G G E S V",
                " c m X g e X v ",
                "M M B g e B v v",
                " m g H r c H m ",
                "G G E E R R C C"
            )
        ),
        SagaLevelSpec(
            levelNumber = 16,
            chapter = "Singularity Void",
            title = "Quantum Relay",
            subtitle = "Chain Warp Portals into Lightning Zappers!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 28,
            starThresholds = Triple(3800, 6200, 8600),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD, BubbleColor.VIOLET, BubbleColor.CORAL),
            rowsPattern = listOf(
                "S R C M M C R S",
                " X P L G E L X ",
                "R C M G E V R C",
                " c m g e v r c ",
                "M G P . . V C R"
            )
        ),
        SagaLevelSpec(
            levelNumber = 17,
            chapter = "Singularity Void",
            title = "Chaos Nexus",
            subtitle = "Morphing Chameleons guard twin Black Holes.",
            goal = LevelGoal.CLEAR_BOARD,
            movesLimit = 30,
            starThresholds = Triple(4000, 6500, 9000),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD, BubbleColor.VIOLET, BubbleColor.CORAL),
            rowsPattern = listOf(
                "C H M G H E V R",
                " c m B g e B v ",
                "M G E V R C M G",
                " g e L r c L m ",
                "E V R C M G E V"
            )
        ),
        SagaLevelSpec(
            levelNumber = 18,
            chapter = "Singularity Void",
            title = "BOSS: Void Leviathan",
            subtitle = "Final Singularity Boss! Overdrive, Portals & x5 drops!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 32,
            starThresholds = Triple(4800, 7600, 10800),
            allowedColors = listOf(BubbleColor.CYAN, BubbleColor.MAGENTA, BubbleColor.GOLD, BubbleColor.EMERALD, BubbleColor.VIOLET, BubbleColor.CORAL),
            rowsPattern = listOf(
                "S V R S S R E S",
                " v r c P m r e ",
                "X B C L G L B X",
                " r c H g H c v ",
                "C M G P V R C M",
                " c m g e v r c "
            ),
            bossSpec = BossSpec("Void Leviathan", maxHp = 42, counterInterval = 3, accentColor = AstroViolet)
        )
    )

    fun getLevel(number: Int): SagaLevelSpec {
        val index = (number - 1).coerceIn(0, sagaLevels.lastIndex)
        return sagaLevels[index]
    }

    fun todayKey(): String {
        val cal = Calendar.getInstance()
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun yesterdayKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun buildDailyChallengeSpec(dayKey: String = todayKey()): SagaLevelSpec {
        val seed = dayKey.hashCode()
        val rng = Random(seed)
        val palette = BubbleColor.entries.shuffled(rng).take(5)
        val rows = mutableListOf<String>()
        for (row in 0 until 6) {
            val cols = HexGridEngine.colsForRow(row)
            val tokens = mutableListOf<String>()
            for (col in 0 until cols) {
                val roll = rng.nextInt(100)
                val token = when {
                    row == 0 && (col == 1 || col == 4 || col == 6) -> "S"
                    row == 2 && (col == 2 || col == 5) -> "L"
                    row == 3 && col == 3 -> "B"
                    row == 1 && roll < 18 -> palette[rng.nextInt(palette.size)].code.lowercaseChar().toString()
                    row >= 2 && roll < 12 -> "H"
                    row >= 2 && roll in 12..19 -> "X"
                    else -> palette[rng.nextInt(palette.size)].code.toString()
                }
                tokens.add(token)
            }
            rows.add(tokens.joinToString(" "))
        }
        return SagaLevelSpec(
            levelNumber = 0,
            chapter = "Daily Event",
            title = "Daily Nebula ($dayKey)",
            subtitle = "Rescue the 3 Daily Star Cores for +120 Star Coins!",
            goal = LevelGoal.RESCUE_STARS,
            movesLimit = 26,
            starThresholds = Triple(2500, 4500, 7000),
            allowedColors = palette,
            rowsPattern = rows
        )
    }

    fun buildGridForLevel(spec: SagaLevelSpec): Map<Pair<Int, Int>, GridCell> {
        val grid = mutableMapOf<Pair<Int, Int>, GridCell>()
        spec.rowsPattern.forEachIndexed { row, rawLine ->
            val tokens = rawLine.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val maxCols = HexGridEngine.colsForRow(row)
            tokens.take(maxCols).forEachIndexed { col, token ->
                val ch = token.first()
                when (ch) {
                    '.' -> Unit
                    'X' -> grid[row to col] = GridCell(row, col, BubbleColor.VIOLET, TileKind.STONE)
                    'S' -> grid[row to col] = GridCell(row, col, BubbleColor.GOLD, TileKind.STAR_CORE)
                    'L' -> grid[row to col] = GridCell(row, col, BubbleColor.CYAN, TileKind.LIGHTNING)
                    'H' -> grid[row to col] = GridCell(row, col, spec.allowedColors[(row + col) % spec.allowedColors.size], TileKind.CHAMELEON)
                    'B' -> grid[row to col] = GridCell(row, col, BubbleColor.VIOLET, TileKind.BLACK_HOLE, charge = 0)
                    'P' -> grid[row to col] = GridCell(row, col, BubbleColor.MAGENTA, TileKind.PORTAL)
                    else -> {
                        val color = BubbleColor.fromCode(ch) ?: spec.allowedColors.first()
                        val kind = if (ch.isLowerCase()) TileKind.ICE else TileKind.NORMAL
                        grid[row to col] = GridCell(row, col, color, kind)
                    }
                }
            }
        }
        return grid
    }

    fun buildEndlessStartGrid(seed: Int = 42): Map<Pair<Int, Int>, GridCell> {
        val rng = Random(seed)
        val colors = listOf(
            BubbleColor.CYAN,
            BubbleColor.MAGENTA,
            BubbleColor.GOLD,
            BubbleColor.EMERALD,
            BubbleColor.VIOLET
        )
        val grid = mutableMapOf<Pair<Int, Int>, GridCell>()
        for (row in 0 until 6) {
            val cols = HexGridEngine.colsForRow(row)
            for (col in 0 until cols) {
                val roll = rng.nextInt(100)
                when {
                    row >= 2 && roll < 6 -> {
                        grid[row to col] = GridCell(row, col, BubbleColor.VIOLET, TileKind.STONE)
                    }
                    row >= 2 && roll == 6 -> {
                        grid[row to col] = GridCell(row, col, BubbleColor.CYAN, TileKind.LIGHTNING)
                    }
                    row >= 1 && roll in 7..16 -> {
                        grid[row to col] = GridCell(row, col, colors[rng.nextInt(colors.size)], TileKind.ICE)
                    }
                    row >= 2 && roll == 17 -> {
                        grid[row to col] = GridCell(row, col, colors[rng.nextInt(colors.size)], TileKind.CHAMELEON)
                    }
                    else -> {
                        val c = if (col > 0 && grid[row to (col - 1)]?.kind == TileKind.NORMAL && rng.nextInt(100) < 42) {
                            grid[row to (col - 1)]!!.color
                        } else {
                            colors[rng.nextInt(colors.size)]
                        }
                        grid[row to col] = GridCell(row, col, c, TileKind.NORMAL)
                    }
                }
            }
        }
        return grid
    }

    fun generateEndlessTopRows(waveIndex: Int, activeColors: List<BubbleColor>): Map<Pair<Int, Int>, GridCell> {
        val rng = Random(1000 + waveIndex * 37)
        val palette = if (activeColors.size >= 3) activeColors else BubbleColor.entries
        val newRows = mutableMapOf<Pair<Int, Int>, GridCell>()
        for (row in 0..1) {
            for (col in 0 until HexGridEngine.colsForRow(row)) {
                val roll = rng.nextInt(100)
                val kind = when {
                    waveIndex > 0 && waveIndex % 3 == 0 && row == 0 && col == 3 -> TileKind.BLACK_HOLE
                    waveIndex >= 2 && roll < 7 -> TileKind.STONE
                    waveIndex >= 1 && roll == 7 -> TileKind.LIGHTNING
                    waveIndex >= 1 && roll in 8..17 -> TileKind.ICE
                    waveIndex >= 3 && roll == 18 -> TileKind.CHAMELEON
                    else -> TileKind.NORMAL
                }
                val color = if (kind == TileKind.STONE || kind == TileKind.BLACK_HOLE) BubbleColor.VIOLET else palette[rng.nextInt(palette.size)]
                newRows[row to col] = GridCell(row, col, color, kind)
            }
        }
        return newRows
    }
}
