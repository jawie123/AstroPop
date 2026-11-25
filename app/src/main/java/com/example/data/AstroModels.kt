package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GameMode {
    SAGA,
    ENDLESS,
    DAILY
}

enum class BoosterType(val label: String, val coinCost: Long) {
    BOMB("Nova Bomb", 80L),
    PRISM("Prism Orb", 70L),
    LASER("Star Laser", 110L)
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedSkin(): CannonSkin = CannonSkin.fromId(equippedSkin)
}

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
