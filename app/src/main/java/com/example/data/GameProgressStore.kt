package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

interface GameProgressStore {
    val isGuest: Boolean
    fun observeProfile(): Flow<PlayerProfile?>
    fun observeLevels(): Flow<List<LevelProgress>>
    fun observeRecentRuns(): Flow<List<GameRun>>
    suspend fun ensureProfile(displayName: String): Result<Unit>
    suspend fun updatePreferences(soundEnabled: Boolean, hapticsEnabled: Boolean): Result<Unit>
    suspend fun consumeBooster(booster: BoosterType): Result<Unit>
    suspend fun purchaseBooster(booster: BoosterType): Result<Unit>
    suspend fun spendCoinsForExtraShots(cost: Long): Result<Unit>
    suspend fun grantAdRewardCoins(coins: Long): Result<Unit>
    suspend fun unlockOrEquipSkin(skin: CannonSkin): Result<Unit>
    suspend fun claimQuest(quest: CosmicQuest): Result<Unit>
    suspend fun claimDailySupply(todayKey: String): Result<DailySupplyTier>
    suspend fun claimWheelPrize(prize: WheelPrize, todayKey: String, isFreeDaily: Boolean): Result<Unit>
    suspend fun recordSagaLevelResult(
        levelNumber: Int,
        score: Long,
        stars: Int,
        maxCombo: Int,
        bubblesPopped: Int,
        won: Boolean,
        rewardBooster: BoosterType?,
        coinsEarned: Long
    ): Result<Unit>
    suspend fun recordEndlessResult(
        score: Long,
        maxCombo: Int,
        bubblesPopped: Int,
        coinsEarned: Long
    ): Result<Unit>
    suspend fun recordDailyChallengeResult(
        dayKey: String,
        previousDayKey: String,
        score: Long,
        stars: Int,
        maxCombo: Int,
        bubblesPopped: Int,
        won: Boolean,
        coinsEarned: Long
    ): Result<Unit>
}

class FirestoreProgressStore(
    private val repository: AstroRepository,
    private val userId: String
) : GameProgressStore {
    override val isGuest: Boolean = false

    override fun observeProfile(): Flow<PlayerProfile?> = repository.observeProfile(userId)

    override fun observeLevels(): Flow<List<LevelProgress>> = repository.observeLevels(userId)

    override fun observeRecentRuns(): Flow<List<GameRun>> = repository.observeRecentRuns(userId)

    override suspend fun ensureProfile(displayName: String): Result<Unit> =
        repository.ensureProfile(userId, displayName)

    override suspend fun updatePreferences(soundEnabled: Boolean, hapticsEnabled: Boolean): Result<Unit> =
        repository.updatePreferences(userId, soundEnabled, hapticsEnabled)

    override suspend fun consumeBooster(booster: BoosterType): Result<Unit> =
        repository.consumeBooster(userId, booster)

    override suspend fun purchaseBooster(booster: BoosterType): Result<Unit> =
        repository.purchaseBooster(userId, booster)

    override suspend fun spendCoinsForExtraShots(cost: Long): Result<Unit> =
        repository.spendCoinsForExtraShots(userId, cost)

    override suspend fun grantAdRewardCoins(coins: Long): Result<Unit> =
        repository.grantAdRewardCoins(userId, coins)

    override suspend fun unlockOrEquipSkin(skin: CannonSkin): Result<Unit> =
        repository.unlockOrEquipSkin(userId, skin)

    override suspend fun claimQuest(quest: CosmicQuest): Result<Unit> =
        repository.claimQuest(userId, quest)

    override suspend fun claimDailySupply(todayKey: String): Result<DailySupplyTier> =
        repository.claimDailySupply(userId, todayKey)

    override suspend fun claimWheelPrize(prize: WheelPrize, todayKey: String, isFreeDaily: Boolean): Result<Unit> =
        repository.claimWheelPrize(userId, prize, todayKey, isFreeDaily)

    override suspend fun recordSagaLevelResult(
        levelNumber: Int,
        score: Long,
        stars: Int,
        maxCombo: Int,
        bubblesPopped: Int,
        won: Boolean,
        rewardBooster: BoosterType?,
        coinsEarned: Long
    ): Result<Unit> = repository.recordSagaLevelResult(
        userId = userId,
        levelNumber = levelNumber,
        score = score,
        stars = stars,
        maxCombo = maxCombo,
        bubblesPopped = bubblesPopped,
        won = won,
        rewardBooster = rewardBooster,
        coinsEarned = coinsEarned
    )

    override suspend fun recordEndlessResult(
        score: Long,
        maxCombo: Int,
        bubblesPopped: Int,
        coinsEarned: Long
    ): Result<Unit> = repository.recordEndlessResult(
        userId = userId,
        score = score,
        maxCombo = maxCombo,
        bubblesPopped = bubblesPopped,
        coinsEarned = coinsEarned
    )

    override suspend fun recordDailyChallengeResult(
        dayKey: String,
        previousDayKey: String,
        score: Long,
        stars: Int,
        maxCombo: Int,
        bubblesPopped: Int,
        won: Boolean,
        coinsEarned: Long
    ): Result<Unit> = repository.recordDailyChallengeResult(
        userId = userId,
        dayKey = dayKey,
        previousDayKey = previousDayKey,
        score = score,
        stars = stars,
        maxCombo = maxCombo,
        bubblesPopped = bubblesPopped,
        won = won,
        coinsEarned = coinsEarned
    )
}

class GuestProgressStore(context: Context) : GameProgressStore {
    override val isGuest: Boolean = true

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val profileFlow = MutableStateFlow<PlayerProfile?>(loadProfile())
    private val levelsFlow = MutableStateFlow<List<LevelProgress>>(loadLevels())
    private val runsFlow = MutableStateFlow<List<GameRun>>(loadRuns())

    override fun observeProfile(): Flow<PlayerProfile?> = profileFlow.asStateFlow()

    override fun observeLevels(): Flow<List<LevelProgress>> = levelsFlow.asStateFlow()

    override fun observeRecentRuns(): Flow<List<GameRun>> = runsFlow.asStateFlow()

    override suspend fun ensureProfile(displayName: String): Result<Unit> {
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            val initial = PlayerProfile(
                id = GUEST_ID,
                userId = GUEST_ID,
                displayName = displayName.ifBlank { "Guest Pilot" },
                highestUnlockedLevel = 1L,
                totalStars = 0L,
                endlessHighScore = 0L,
                totalBubblesPopped = 0L,
                starCoins = 150L,
                dailyStreak = 0L,
                lastDailyDay = "",
                bombBoosters = 2L,
                prismBoosters = 2L,
                laserBoosters = 1L,
                soundEnabled = true,
                hapticsEnabled = true
            )
            saveProfile(initial)
        }
        return Result.success(Unit)
    }

    override suspend fun updatePreferences(soundEnabled: Boolean, hapticsEnabled: Boolean): Result<Unit> {
        val current = profileFlow.value ?: loadProfile()
        saveProfile(current.copy(soundEnabled = soundEnabled, hapticsEnabled = hapticsEnabled))
        return Result.success(Unit)
    }

    override suspend fun consumeBooster(booster: BoosterType): Result<Unit> {
        val current = profileFlow.value ?: loadProfile()
        val updated = when (booster) {
            BoosterType.BOMB -> current.copy(bombBoosters = (current.bombBoosters - 1L).coerceAtLeast(0L))
            BoosterType.PRISM -> current.copy(prismBoosters = (current.prismBoosters - 1L).coerceAtLeast(0L))
            BoosterType.LASER -> current.copy(laserBoosters = (current.laserBoosters - 1L).coerceAtLeast(0L))
        }
        saveProfile(updated)
        return Result.success(Unit)
    }

    override suspend fun purchaseBooster(booster: BoosterType): Result<Unit> {
        val current = profileFlow.value ?: loadProfile()
        if (current.starCoins < booster.coinCost) {
            return Result.failure(IllegalStateException("Not enough Star Coins"))
        }
        val nextCoins = current.starCoins - booster.coinCost
        val updated = when (booster) {
            BoosterType.BOMB -> current.copy(starCoins = nextCoins, bombBoosters = current.bombBoosters + 1L)
            BoosterType.PRISM -> current.copy(starCoins = nextCoins, prismBoosters = current.prismBoosters + 1L)
            BoosterType.LASER -> current.copy(starCoins = nextCoins, laserBoosters = current.laserBoosters + 1L)
        }
        saveProfile(updated)
        return Result.success(Unit)
    }

    override suspend fun spendCoinsForExtraShots(cost: Long): Result<Unit> {
        val current = profileFlow.value ?: loadProfile()
        if (current.starCoins < cost) {
            return Result.failure(IllegalStateException("Not enough Star Coins"))
        }
        saveProfile(current.copy(starCoins = current.starCoins - cost))
        return Result.success(Unit)
    }

    override suspend fun grantAdRewardCoins(coins: Long): Result<Unit> {
        val current = profileFlow.value ?: loadProfile()
        val safeCoins = coins.coerceIn(1L, 1000L)
        saveProfile(current.copy(starCoins = current.starCoins + safeCoins))
        return Result.success(Unit)
    }

    override suspend fun unlockOrEquipSkin(skin: CannonSkin): Result<Unit> {
        val current = profileFlow.value ?: loadProfile()
        return if (skin.name in current.unlockedSkins) {
            saveProfile(current.copy(equippedSkin = skin.name))
            Result.success(Unit)
        } else if (current.starCoins < skin.coinCost) {
            Result.failure(IllegalStateException("Not enough Star Coins"))
        } else {
            saveProfile(
                current.copy(
                    starCoins = current.starCoins - skin.coinCost,
                    unlockedSkins = (current.unlockedSkins + skin.name).distinct(),
                    equippedSkin = skin.name
                )
            )
            Result.success(Unit)
        }
    }

    override suspend fun claimQuest(quest: CosmicQuest): Result<Unit> {
        val current = profileFlow.value ?: loadProfile()
        if (!quest.isCompleted(current)) {
            return Result.failure(IllegalStateException("Quest milestone not yet reached"))
        }
        if (quest.id in current.claimedQuests) {
            return Result.failure(IllegalStateException("Quest reward already claimed"))
        }
        var updated = current.copy(
            starCoins = current.starCoins + quest.rewardCoins,
            claimedQuests = (current.claimedQuests + quest.id).distinct()
        )
        updated = when (quest.rewardBooster) {
            BoosterType.BOMB -> updated.copy(bombBoosters = updated.bombBoosters + 1L)
            BoosterType.PRISM -> updated.copy(prismBoosters = updated.prismBoosters + 1L)
            BoosterType.LASER -> updated.copy(laserBoosters = updated.laserBoosters + 1L)
            null -> updated
        }
        saveProfile(updated)
        return Result.success(Unit)
    }

    override suspend fun claimDailySupply(todayKey: String): Result<DailySupplyTier> {
        val current = profileFlow.value ?: loadProfile()
        if (current.lastLoginRewardDate == todayKey) {
            return Result.failure(IllegalStateException("Today's Star Supply already claimed!"))
        }
        val nextDay = DailySupplyCatalog.nextClaimDay(current.loginRewardDay)
        val tier = DailySupplyCatalog.tierForDay(nextDay)
        saveProfile(
            current.copy(
                starCoins = current.starCoins + tier.coins,
                bombBoosters = (current.bombBoosters + tier.bombs).coerceAtMost(999L),
                prismBoosters = (current.prismBoosters + tier.prisms).coerceAtMost(999L),
                laserBoosters = (current.laserBoosters + tier.lasers).coerceAtMost(999L),
                loginRewardDay = nextDay.toLong(),
                lastLoginRewardDate = todayKey
            )
        )
        return Result.success(tier)
    }

    override suspend fun claimWheelPrize(
        prize: WheelPrize,
        todayKey: String,
        isFreeDaily: Boolean
    ): Result<Unit> {
        val current = profileFlow.value ?: loadProfile()
        if (isFreeDaily && current.lastWheelSpinDate == todayKey) {
            return Result.failure(IllegalStateException("Daily free spin already used today!"))
        }
        var updated = current.copy(
            starCoins = current.starCoins + prize.coins,
            lastWheelSpinDate = if (isFreeDaily) todayKey else current.lastWheelSpinDate
        )
        updated = when (prize.booster) {
            BoosterType.BOMB -> updated.copy(bombBoosters = (updated.bombBoosters + 1L).coerceAtMost(999L))
            BoosterType.PRISM -> updated.copy(prismBoosters = (updated.prismBoosters + 1L).coerceAtMost(999L))
            BoosterType.LASER -> updated.copy(laserBoosters = (updated.laserBoosters + 1L).coerceAtMost(999L))
            null -> updated
        }
        saveProfile(updated)
        return Result.success(Unit)
    }

    override suspend fun recordSagaLevelResult(
        levelNumber: Int,
        score: Long,
        stars: Int,
        maxCombo: Int,
        bubblesPopped: Int,
        won: Boolean,
        rewardBooster: BoosterType?,
        coinsEarned: Long
    ): Result<Unit> {
        val safeLevel = levelNumber.coerceIn(1, 30)
        val safeStars = stars.coerceIn(0, 3).toLong()
        val safeScore = score.coerceAtLeast(0L)
        val currentLevels = levelsFlow.value.associateBy { it.levelNumber.toInt() }.toMutableMap()
        val prev = currentLevels[safeLevel]

        val prevStars = prev?.stars ?: 0L
        val newStars = maxOf(prevStars, safeStars)
        val newBest = maxOf(prev?.bestScore ?: 0L, safeScore)
        val newCompleted = (prev?.completed ?: false) || won
        val starDelta = (newStars - prevStars).coerceAtLeast(0L)

        currentLevels[safeLevel] = LevelProgress(
            id = "level_$safeLevel",
            levelNumber = safeLevel.toLong(),
            bestScore = newBest,
            stars = newStars,
            completed = newCompleted
        )
        val sortedLevels = currentLevels.values.sortedBy { it.levelNumber }
        saveLevels(sortedLevels)

        val currentProfile = profileFlow.value ?: loadProfile()
        val nextUnlocked = if (won) {
            maxOf(currentProfile.highestUnlockedLevel, (safeLevel + 1).coerceAtMost(30).toLong())
        } else {
            currentProfile.highestUnlockedLevel
        }

        var updatedProfile = currentProfile.copy(
            highestUnlockedLevel = nextUnlocked,
            totalStars = currentProfile.totalStars + starDelta,
            totalBubblesPopped = currentProfile.totalBubblesPopped + bubblesPopped.coerceAtLeast(0).toLong(),
            starCoins = currentProfile.starCoins + coinsEarned.coerceAtLeast(0L)
        )
        updatedProfile = when (rewardBooster) {
            BoosterType.BOMB -> updatedProfile.copy(bombBoosters = updatedProfile.bombBoosters + 1L)
            BoosterType.PRISM -> updatedProfile.copy(prismBoosters = updatedProfile.prismBoosters + 1L)
            BoosterType.LASER -> updatedProfile.copy(laserBoosters = updatedProfile.laserBoosters + 1L)
            null -> updatedProfile
        }
        saveProfile(updatedProfile)

        prependRun(
            GameRun(
                id = UUID.randomUUID().toString(),
                mode = GameMode.SAGA.name,
                levelNumber = safeLevel.toLong(),
                score = safeScore,
                maxCombo = maxCombo.coerceAtLeast(0).toLong(),
                bubblesPopped = bubblesPopped.coerceAtLeast(0).toLong(),
                starsEarned = safeStars
            )
        )
        return Result.success(Unit)
    }

    override suspend fun recordEndlessResult(
        score: Long,
        maxCombo: Int,
        bubblesPopped: Int,
        coinsEarned: Long
    ): Result<Unit> {
        val safeScore = score.coerceAtLeast(0L)
        val currentProfile = profileFlow.value ?: loadProfile()
        saveProfile(
            currentProfile.copy(
                endlessHighScore = maxOf(currentProfile.endlessHighScore, safeScore),
                totalBubblesPopped = currentProfile.totalBubblesPopped + bubblesPopped.coerceAtLeast(0).toLong(),
                starCoins = currentProfile.starCoins + coinsEarned.coerceAtLeast(0L)
            )
        )
        prependRun(
            GameRun(
                id = UUID.randomUUID().toString(),
                mode = GameMode.ENDLESS.name,
                levelNumber = 0L,
                score = safeScore,
                maxCombo = maxCombo.coerceAtLeast(0).toLong(),
                bubblesPopped = bubblesPopped.coerceAtLeast(0).toLong(),
                starsEarned = 0L
            )
        )
        return Result.success(Unit)
    }

    override suspend fun recordDailyChallengeResult(
        dayKey: String,
        previousDayKey: String,
        score: Long,
        stars: Int,
        maxCombo: Int,
        bubblesPopped: Int,
        won: Boolean,
        coinsEarned: Long
    ): Result<Unit> {
        val current = profileFlow.value ?: loadProfile()
        val (nextStreak, nextLastDay) = if (won && current.lastDailyDay != dayKey) {
            val inc = if (current.lastDailyDay == previousDayKey) current.dailyStreak + 1L else 1L
            inc to dayKey
        } else {
            current.dailyStreak to current.lastDailyDay
        }
        saveProfile(
            current.copy(
                totalBubblesPopped = current.totalBubblesPopped + bubblesPopped.coerceAtLeast(0).toLong(),
                starCoins = current.starCoins + coinsEarned.coerceAtLeast(0L),
                dailyStreak = nextStreak,
                lastDailyDay = nextLastDay
            )
        )
        prependRun(
            GameRun(
                id = UUID.randomUUID().toString(),
                mode = GameMode.DAILY.name,
                levelNumber = 0L,
                score = score.coerceAtLeast(0L),
                maxCombo = maxCombo.coerceAtLeast(0).toLong(),
                bubblesPopped = bubblesPopped.coerceAtLeast(0).toLong(),
                starsEarned = stars.coerceIn(0, 3).toLong()
            )
        )
        return Result.success(Unit)
    }

    private fun loadProfile(): PlayerProfile {
        return PlayerProfile(
            id = GUEST_ID,
            userId = GUEST_ID,
            displayName = prefs.getString(KEY_DISPLAY_NAME, "Guest Pilot") ?: "Guest Pilot",
            highestUnlockedLevel = prefs.getLong(KEY_HIGHEST_LEVEL, 1L).coerceIn(1L, 30L),
            totalStars = prefs.getLong(KEY_TOTAL_STARS, 0L).coerceAtLeast(0L),
            endlessHighScore = prefs.getLong(KEY_ENDLESS_HIGH, 0L).coerceAtLeast(0L),
            totalBubblesPopped = prefs.getLong(KEY_TOTAL_POPPED, 0L).coerceAtLeast(0L),
            starCoins = prefs.getLong(KEY_STAR_COINS, 150L).coerceAtLeast(0L),
            dailyStreak = prefs.getLong(KEY_DAILY_STREAK, 0L).coerceAtLeast(0L),
            lastDailyDay = prefs.getString(KEY_LAST_DAILY, "") ?: "",
            bombBoosters = prefs.getLong(KEY_BOMBS, 2L).coerceAtLeast(0L),
            prismBoosters = prefs.getLong(KEY_PRISMS, 2L).coerceAtLeast(0L),
            laserBoosters = prefs.getLong(KEY_LASERS, 1L).coerceAtLeast(0L),
            soundEnabled = prefs.getBoolean(KEY_SOUND, true),
            hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
            equippedSkin = prefs.getString(KEY_EQUIPPED_SKIN, CannonSkin.CYAN_INTERCEPTOR.name)
                ?: CannonSkin.CYAN_INTERCEPTOR.name,
            unlockedSkins = decodeStringList(
                prefs.getString(KEY_UNLOCKED_SKINS, null),
                listOf(CannonSkin.CYAN_INTERCEPTOR.name)
            ),
            claimedQuests = decodeStringList(prefs.getString(KEY_CLAIMED_QUESTS, null), emptyList()),
            loginRewardDay = prefs.getLong(KEY_LOGIN_REWARD_DAY, 0L).coerceIn(0L, 7L),
            lastLoginRewardDate = prefs.getString(KEY_LAST_LOGIN_REWARD, "") ?: "",
            lastWheelSpinDate = prefs.getString(KEY_LAST_WHEEL_SPIN, "") ?: ""
        )
    }

    private fun saveProfile(profile: PlayerProfile) {
        prefs.edit()
            .putBoolean(KEY_INITIALIZED, true)
            .putString(KEY_DISPLAY_NAME, profile.displayName)
            .putLong(KEY_HIGHEST_LEVEL, profile.highestUnlockedLevel)
            .putLong(KEY_TOTAL_STARS, profile.totalStars)
            .putLong(KEY_ENDLESS_HIGH, profile.endlessHighScore)
            .putLong(KEY_TOTAL_POPPED, profile.totalBubblesPopped)
            .putLong(KEY_STAR_COINS, profile.starCoins)
            .putLong(KEY_DAILY_STREAK, profile.dailyStreak)
            .putString(KEY_LAST_DAILY, profile.lastDailyDay)
            .putLong(KEY_BOMBS, profile.bombBoosters)
            .putLong(KEY_PRISMS, profile.prismBoosters)
            .putLong(KEY_LASERS, profile.laserBoosters)
            .putBoolean(KEY_SOUND, profile.soundEnabled)
            .putBoolean(KEY_HAPTICS, profile.hapticsEnabled)
            .putString(KEY_EQUIPPED_SKIN, profile.equippedSkin)
            .putString(KEY_UNLOCKED_SKINS, JSONArray(profile.unlockedSkins).toString())
            .putString(KEY_CLAIMED_QUESTS, JSONArray(profile.claimedQuests).toString())
            .putLong(KEY_LOGIN_REWARD_DAY, profile.loginRewardDay)
            .putString(KEY_LAST_LOGIN_REWARD, profile.lastLoginRewardDate)
            .putString(KEY_LAST_WHEEL_SPIN, profile.lastWheelSpinDate)
            .apply()
        profileFlow.value = profile
    }

    private fun decodeStringList(raw: String?, fallback: List<String>): List<String> {
        if (raw.isNullOrBlank()) return fallback
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i, "")
                    if (v.isNotBlank()) add(v)
                }
            }.ifEmpty { fallback }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun loadLevels(): List<LevelProgress> {
        val raw = prefs.getString(KEY_LEVELS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val num = obj.optLong("levelNumber", 1L)
                    add(
                        LevelProgress(
                            id = "level_$num",
                            levelNumber = num,
                            bestScore = obj.optLong("bestScore", 0L),
                            stars = obj.optLong("stars", 0L),
                            completed = obj.optBoolean("completed", false)
                        )
                    )
                }
            }.sortedBy { it.levelNumber }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveLevels(levels: List<LevelProgress>) {
        val arr = JSONArray()
        for (lvl in levels) {
            arr.put(
                JSONObject().apply {
                    put("levelNumber", lvl.levelNumber)
                    put("bestScore", lvl.bestScore)
                    put("stars", lvl.stars)
                    put("completed", lvl.completed)
                }
            )
        }
        prefs.edit().putString(KEY_LEVELS_JSON, arr.toString()).apply()
        levelsFlow.value = levels
    }

    private fun loadRuns(): List<GameRun> {
        val raw = prefs.getString(KEY_RUNS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        GameRun(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            mode = obj.optString("mode", GameMode.SAGA.name),
                            levelNumber = obj.optLong("levelNumber", 0L),
                            score = obj.optLong("score", 0L),
                            maxCombo = obj.optLong("maxCombo", 0L),
                            bubblesPopped = obj.optLong("bubblesPopped", 0L),
                            starsEarned = obj.optLong("starsEarned", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun prependRun(run: GameRun) {
        val updated = (listOf(run) + runsFlow.value).take(15)
        val arr = JSONArray()
        for (item in updated) {
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("mode", item.mode)
                    put("levelNumber", item.levelNumber)
                    put("score", item.score)
                    put("maxCombo", item.maxCombo)
                    put("bubblesPopped", item.bubblesPopped)
                    put("starsEarned", item.starsEarned)
                }
            )
        }
        prefs.edit().putString(KEY_RUNS_JSON, arr.toString()).apply()
        runsFlow.value = updated
    }

    private companion object {
        const val PREFS_NAME = "astropop_guest_progress"
        const val GUEST_ID = "guest_local"
        const val KEY_INITIALIZED = "initialized"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_HIGHEST_LEVEL = "highest_level"
        const val KEY_TOTAL_STARS = "total_stars"
        const val KEY_ENDLESS_HIGH = "endless_high"
        const val KEY_TOTAL_POPPED = "total_popped"
        const val KEY_STAR_COINS = "star_coins"
        const val KEY_DAILY_STREAK = "daily_streak"
        const val KEY_LAST_DAILY = "last_daily_day"
        const val KEY_BOMBS = "bombs"
        const val KEY_PRISMS = "prisms"
        const val KEY_LASERS = "lasers"
        const val KEY_SOUND = "sound"
        const val KEY_HAPTICS = "haptics"
        const val KEY_EQUIPPED_SKIN = "equipped_skin"
        const val KEY_UNLOCKED_SKINS = "unlocked_skins"
        const val KEY_CLAIMED_QUESTS = "claimed_quests"
        const val KEY_LOGIN_REWARD_DAY = "login_reward_day"
        const val KEY_LAST_LOGIN_REWARD = "last_login_reward"
        const val KEY_LAST_WHEEL_SPIN = "last_wheel_spin"
        const val KEY_LEVELS_JSON = "levels_json"
        const val KEY_RUNS_JSON = "runs_json"
    }
}
