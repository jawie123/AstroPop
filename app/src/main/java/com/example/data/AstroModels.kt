package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GameMode {
    SAGA,
    ENDLESS,
    DAILY,
    BLITZ,
    BOSS_RUSH
}

enum class BoosterType(val label: String, val coinCost: Long) {
    BOMB("Nova Bomb", 80L),
    PRISM("Prism Orb", 70L),
    LASER("Star Laser", 110L)
}

object TacticalPackSpec {
    const val TITLE = "Tactical Mega Pack"
    const val SUBTITLE = "+1 Bomb, +1 Prism & +1 Laser (Save 50c!)"
    const val COIN_COST = 210L
}

enum class PilotBadge(
    val title: String,
    val subtitle: String,
    val requirement: String
) {
    FIRST_LAUNCH("First Launch", "Ignited the plasma railgun", "Pop 25+ bubbles"),
    STAR_COLLECTOR("Star Collector", "Mastered constellation rescues", "Earn 15+ Saga stars"),
    VOID_CONQUEROR("Void Conqueror", "Cleared the Singularity sector", "Unlock Stage 19"),
    SUPERNOVA_CHAMP("Supernova Champion", "Conquered the Supernova Core", "Reach Stage 30 & 50+ Stars"),
    BLITZ_SPEEDSTER("Blitz Ace", "Master of the 2-minute storm", "Score 5,000+ in Cosmic Blitz"),
    ENDLESS_TITAN("Endless Titan", "Survived deep nebula waves", "Score 8,000+ in Endless Panic"),
    VAULT_RAIDER("Vault Raider", "Looter of galactic supply vaults", "Open 5+ Mystery Chests"),
    ARSENAL_MASTER("Arsenal Master", "Commander of elite hulls", "Unlock 3+ Cannon Skins");

    fun isUnlocked(profile: PlayerProfile): Boolean = when (this) {
        FIRST_LAUNCH -> profile.totalBubblesPopped >= 25L
        STAR_COLLECTOR -> profile.totalStars >= 15L
        VOID_CONQUEROR -> profile.highestUnlockedLevel >= 19L
        SUPERNOVA_CHAMP -> profile.highestUnlockedLevel >= 30L && profile.totalStars >= 50L
        BLITZ_SPEEDSTER -> profile.blitzHighScore >= 5000L
        ENDLESS_TITAN -> profile.endlessHighScore >= 8000L
        VAULT_RAIDER -> profile.mysteryChestsOpened >= 5L
        ARSENAL_MASTER -> profile.unlockedSkins.size >= 3
    }

    fun progressLabel(profile: PlayerProfile): String = when (this) {
        FIRST_LAUNCH -> "${profile.totalBubblesPopped.coerceAtMost(25L)} / 25 popped"
        STAR_COLLECTOR -> "${profile.totalStars.coerceAtMost(15L)} / 15 stars"
        VOID_CONQUEROR -> "Stage ${profile.highestUnlockedLevel.coerceAtMost(19L)} / 19"
        SUPERNOVA_CHAMP -> "Stage ${profile.highestUnlockedLevel.coerceAtMost(30L)}/30 • ${profile.totalStars.coerceAtMost(50L)}/50 ★"
        BLITZ_SPEEDSTER -> "${profile.blitzHighScore.coerceAtMost(5000L)} / 5,000 Blitz"
        ENDLESS_TITAN -> "${profile.endlessHighScore.coerceAtMost(8000L)} / 8,000 Endless"
        VAULT_RAIDER -> "${profile.mysteryChestsOpened.coerceAtMost(5L)} / 5 chests"
        ARSENAL_MASTER -> "${profile.unlockedSkins.size.coerceAtMost(3)} / 3 skins"
    }
}

enum class CannonSkin(
    val title: String,
    val subtitle: String,
    val coinCost: Long
) {
    CYAN_INTERCEPTOR("Cyan Interceptor", "Standard plasma rail cannon", 0L),
    SOLAR_VALKYRIE("Solar Valkyrie", "Radiant gold beam & core ring", 180L),
    PLASMA_NOVA("Plasma Nova", "Hyper-magenta overdrive chassis", 280L),
    VOID_SINGULARITY("Void Singularity", "Obsidian-violet event horizon core", 420L);

    companion object {
        fun fromId(id: String?): CannonSkin =
            entries.firstOrNull { it.name == id } ?: CYAN_INTERCEPTOR
    }
}

enum class CosmicQuest(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Long,
    val rewardBooster: BoosterType?
) {
    POP_50("quest_pop_50", "First Starburst", "Pop 50 cosmic bubbles total", 60L, null),
    STARS_9("quest_stars_9", "Constellation Striker", "Earn 9 stars in Starlight Saga", 50L, BoosterType.BOMB),
    ENDLESS_2500("quest_endless_2500", "Endless Ace", "Reach 2,500+ in Endless Panic", 80L, BoosterType.PRISM),
    DAILY_FIRST("quest_daily_1", "Daily Pioneer", "Clear any Daily Nebula challenge", 100L, BoosterType.LASER),
    STAGE_12("quest_stage_12", "Void Conqueror", "Unlock Stage 13 (Singularity Void)", 150L, BoosterType.BOMB);

    fun isCompleted(profile: PlayerProfile): Boolean = when (this) {
        POP_50 -> profile.totalBubblesPopped >= 50L
        STARS_9 -> profile.totalStars >= 9L
        ENDLESS_2500 -> profile.endlessHighScore >= 2500L
        DAILY_FIRST -> profile.lastDailyDay.isNotBlank()
        STAGE_12 -> profile.highestUnlockedLevel >= 13L
    }

    fun progressText(profile: PlayerProfile): String = when (this) {
        POP_50 -> "${profile.totalBubblesPopped.coerceAtMost(50L)} / 50 popped"
        STARS_9 -> "${profile.totalStars.coerceAtMost(9L)} / 9 stars"
        ENDLESS_2500 -> "${profile.endlessHighScore.coerceAtMost(2500L)} / 2,500 score"
        DAILY_FIRST -> if (profile.lastDailyDay.isNotBlank()) "Completed" else "0 / 1 cleared"
        STAGE_12 -> "Stage ${profile.highestUnlockedLevel.coerceAtMost(13L)} / 13"
    }
}

data class DailySupplyTier(
    val day: Int,
    val title: String,
    val coins: Long,
    val bombs: Long = 0L,
    val prisms: Long = 0L,
    val lasers: Long = 0L
)

object DailySupplyCatalog {
    val tiers: List<DailySupplyTier> = listOf(
        DailySupplyTier(1, "+50 Coins", coins = 50L),
        DailySupplyTier(2, "+1 Nova Bomb", coins = 25L, bombs = 1L),
        DailySupplyTier(3, "+90 Coins", coins = 90L),
        DailySupplyTier(4, "+1 Prism Orb", coins = 30L, prisms = 1L),
        DailySupplyTier(5, "+130 Coins", coins = 130L),
        DailySupplyTier(6, "+1 Star Laser", coins = 40L, lasers = 1L),
        DailySupplyTier(7, "Mega Supply!", coins = 250L, bombs = 1L, prisms = 1L, lasers = 1L)
    )

    fun nextClaimDay(currentDay: Long): Int = ((currentDay.toInt().coerceIn(0, 7)) % 7) + 1
    fun tierForDay(day: Int): DailySupplyTier = tiers[(day - 1).coerceIn(0, tiers.lastIndex)]
}

data class SeasonPassTier(
    val tier: Int,
    val title: String,
    val requiredXp: Long,
    val freeCoins: Long,
    val freeBooster: BoosterType? = null,
    val bonusCoins: Long,
    val bonusBooster: BoosterType? = null
) {
    val freeKey: String get() = "free_$tier"
    val bonusKey: String get() = "bonus_$tier"
}

object SeasonPassCatalog {
    val tiers: List<SeasonPassTier> = listOf(
        SeasonPassTier(1, "Cadet Launch", 100L, freeCoins = 40L, bonusCoins = 80L),
        SeasonPassTier(2, "Orbital Spark", 350L, freeCoins = 50L, freeBooster = BoosterType.BOMB, bonusCoins = 75L, bonusBooster = BoosterType.PRISM),
        SeasonPassTier(3, "Nebula Scout", 700L, freeCoins = 70L, bonusCoins = 110L),
        SeasonPassTier(4, "Ion Cruiser", 1200L, freeCoins = 45L, freeBooster = BoosterType.PRISM, bonusCoins = 90L, bonusBooster = BoosterType.BOMB),
        SeasonPassTier(5, "Comet Ace", 1900L, freeCoins = 95L, bonusCoins = 140L),
        SeasonPassTier(6, "Starbreaker", 2800L, freeCoins = 60L, freeBooster = BoosterType.LASER, bonusCoins = 120L, bonusBooster = BoosterType.LASER),
        SeasonPassTier(7, "Void Runner", 3900L, freeCoins = 120L, bonusCoins = 175L),
        SeasonPassTier(8, "Solar Vanguard", 5200L, freeCoins = 75L, freeBooster = BoosterType.BOMB, bonusCoins = 150L, bonusBooster = BoosterType.PRISM),
        SeasonPassTier(9, "Pulsar Knight", 6800L, freeCoins = 150L, bonusCoins = 210L),
        SeasonPassTier(10, "Supernova Core", 8500L, freeCoins = 90L, freeBooster = BoosterType.PRISM, bonusCoins = 180L, bonusBooster = BoosterType.LASER),
        SeasonPassTier(11, "Singularity Ace", 10200L, freeCoins = 180L, bonusCoins = 260L),
        SeasonPassTier(12, "Galactic Crown", 12000L, freeCoins = 250L, freeBooster = BoosterType.LASER, bonusCoins = 350L, bonusBooster = BoosterType.BOMB)
    )
}

enum class MysteryChestReward(
    val title: String,
    val description: String,
    val coins: Long,
    val bombs: Long = 0L,
    val prisms: Long = 0L,
    val lasers: Long = 0L
) {
    STARLIGHT_POUCH("Starlight Pouch", "+75 Star Coins", coins = 75L),
    NOVA_CRATE("Nova Bomb Crate", "+35 Coins & +1 Nova Bomb", coins = 35L, bombs = 1L),
    PRISM_CACHE("Prism Orb Cache", "+40 Coins & +1 Prism Orb", coins = 40L, prisms = 1L),
    LASER_POD("Star Laser Pod", "+30 Coins & +1 Star Laser", coins = 30L, lasers = 1L),
    COMET_HOARD("Comet Coin Hoard", "+150 Star Coins!", coins = 150L),
    SUPERNOVA_JACKPOT("Supernova Jackpot!", "+220 Coins & +1 All Boosters!", coins = 220L, bombs = 1L, prisms = 1L, lasers = 1L);

    companion object {
        fun rollRandom(): MysteryChestReward {
            val roll = (0 until 100).random()
            return when {
                roll < 24 -> STARLIGHT_POUCH
                roll < 44 -> NOVA_CRATE
                roll < 64 -> PRISM_CACHE
                roll < 80 -> LASER_POD
                roll < 94 -> COMET_HOARD
                else -> SUPERNOVA_JACKPOT
            }
        }
    }
}

enum class WheelPrize(
    val shortLabel: String,
    val fullLabel: String,
    val coins: Long,
    val booster: BoosterType?
) {
    COINS_40("+40c", "+40 Star Coins", 40L, null),
    BOMB_1("Bomb", "+1 Nova Bomb", 0L, BoosterType.BOMB),
    COINS_100("+100c", "+100 Star Coins", 100L, null),
    PRISM_1("Prism", "+1 Prism Orb", 0L, BoosterType.PRISM),
    COINS_65("+65c", "+65 Star Coins", 65L, null),
    LASER_1("Laser", "+1 Star Laser", 0L, BoosterType.LASER),
    JACKPOT_200("200c!", "JACKPOT +200 Star Coins!", 200L, null),
    COINS_120("+120c", "+120 Star Coins", 120L, null)
}

data class PilotRankInfo(
    val rankNumber: Int,
    val title: String,
    val xp: Long,
    val nextRankXp: Long?,
    val progress: Float
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "StarCaptain",
    val highestUnlockedLevel: Long = 1L,
    val totalStars: Long = 0L,
    val endlessHighScore: Long = 0L,
    val blitzHighScore: Long = 0L,
    val totalBubblesPopped: Long = 0L,
    val starCoins: Long = 150L,
    val dailyStreak: Long = 0L,
    val lastDailyDay: String = "",
    val bombBoosters: Long = 2L,
    val prismBoosters: Long = 2L,
    val laserBoosters: Long = 1L,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val equippedSkin: String = CannonSkin.CYAN_INTERCEPTOR.name,
    val unlockedSkins: List<String> = listOf(CannonSkin.CYAN_INTERCEPTOR.name),
    val claimedQuests: List<String> = emptyList(),
    val claimedPassTiers: List<String> = emptyList(),
    val loginRewardDay: Long = 0L,
    val lastLoginRewardDate: String = "",
    val lastWheelSpinDate: String = "",
    val lastMysteryChestDate: String = "",
    val mysteryChestsOpened: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedSkin(): CannonSkin = CannonSkin.fromId(equippedSkin)

    fun pilotRank(): PilotRankInfo {
        val xp = totalBubblesPopped * 2L + totalStars * 120L + endlessHighScore / 15L + blitzHighScore / 15L
        val thresholds = listOf(
            0L to "Star Cadet",
            500L to "Orbit Striker",
            1500L to "Nebula Ace",
            3500L to "Solar Valkyrie",
            7000L to "Void Commander",
            13000L to "Galactic Legend"
        )
        var tierIdx = 0
        for (i in thresholds.indices) {
            if (xp >= thresholds[i].first) tierIdx = i
        }
        val floor = thresholds[tierIdx].first
        val ceiling = thresholds.getOrNull(tierIdx + 1)?.first
        val progress = if (ceiling == null) 1f else ((xp - floor).toFloat() / (ceiling - floor).coerceAtLeast(1L)).coerceIn(0f, 1f)
        return PilotRankInfo(
            rankNumber = tierIdx + 1,
            title = thresholds[tierIdx].second,
            xp = xp,
            nextRankXp = ceiling,
            progress = progress
        )
    }

    fun toLeaderboardEntry(): LeaderboardEntry {
        val rank = pilotRank()
        return LeaderboardEntry(
            userId = userId,
            displayName = displayName,
            endlessHighScore = endlessHighScore,
            blitzHighScore = blitzHighScore,
            totalStars = totalStars,
            pilotRankNumber = rank.rankNumber.toLong(),
            pilotRankTitle = rank.title,
            equippedSkin = resolvedSkin().name
        )
    }
}

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "StarCaptain",
    val endlessHighScore: Long = 0L,
    val blitzHighScore: Long = 0L,
    val totalStars: Long = 0L,
    val pilotRankNumber: Long = 1L,
    val pilotRankTitle: String = "Star Cadet",
    val equippedSkin: String = CannonSkin.CYAN_INTERCEPTOR.name,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LevelProgress(
    @DocumentId val id: String = "",
    val levelNumber: Long = 1L,
    val bestScore: Long = 0L,
    val stars: Long = 0L,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GameRun(
    @DocumentId val id: String = "",
    val mode: String = GameMode.SAGA.name,
    val levelNumber: Long = 1L,
    val score: Long = 0L,
    val maxCombo: Long = 0L,
    val bubblesPopped: Long = 0L,
    val starsEarned: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun resolvedMode(): GameMode =
        GameMode.entries.firstOrNull { it.name == mode } ?: GameMode.SAGA
}
