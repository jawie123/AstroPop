package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AstroRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(userId: String): Flow<PlayerProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Mismatched userId in observeProfile" }
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PlayerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeLevels(userId: String): Flow<List<LevelProgress>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Mismatched userId in observeLevels" }
        val colRef = db.collection("users").document(uid).collection("levels")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LevelProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.levelNumber }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeRecentRuns(userId: String): Flow<List<GameRun>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Mismatched userId in observeRecentRuns" }
        val query = db.collection("users").document(uid).collection("runs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(15)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GameRun::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$uid/runs")
                    }
                    throw error
                }
        )
    }

    suspend fun ensureProfile(userId: String, displayName: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "StarCaptain" }.take(50)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "highestUnlockedLevel" to 1L,
                    "totalStars" to 0L,
                    "endlessHighScore" to 0L,
                    "totalBubblesPopped" to 0L,
                    "starCoins" to 150L,
                    "dailyStreak" to 0L,
                    "lastDailyDay" to "",
                    "bombBoosters" to 2L,
                    "prismBoosters" to 2L,
                    "laserBoosters" to 1L,
                    "soundEnabled" to true,
                    "hapticsEnabled" to true,
                    "equippedSkin" to CannonSkin.CYAN_INTERCEPTOR.name,
                    "unlockedSkins" to listOf(CannonSkin.CYAN_INTERCEPTOR.name),
                    "claimedQuests" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(
        userId: String,
        soundEnabled: Boolean,
        hapticsEnabled: Boolean
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            db.collection("users").document(uid).update(
                mapOf(
                    "soundEnabled" to soundEnabled,
                    "hapticsEnabled" to hapticsEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun consumeBooster(userId: String, booster: BoosterType): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val field = when (booster) {
                    BoosterType.BOMB -> "bombBoosters"
                    BoosterType.PRISM -> "prismBoosters"
                    BoosterType.LASER -> "laserBoosters"
                }
                val current = (snap.getLong(field) ?: 0L).coerceAtLeast(0L)
                if (current > 0L) {
                    tx.update(
                        docRef,
                        mapOf(
                            field to (current - 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun purchaseBooster(userId: String, booster: BoosterType): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val coins = (snap.getLong("starCoins") ?: 0L).coerceAtLeast(0L)
                check(coins >= booster.coinCost) { "Not enough Star Coins" }
                val field = when (booster) {
                    BoosterType.BOMB -> "bombBoosters"
                    BoosterType.PRISM -> "prismBoosters"
                    BoosterType.LASER -> "laserBoosters"
                }
                val currentCount = (snap.getLong(field) ?: 0L).coerceAtLeast(0L)
                tx.update(
                    docRef,
                    mapOf(
                        "starCoins" to (coins - booster.coinCost),
                        field to (currentCount + 1L).coerceAtMost(999L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun spendCoinsForExtraShots(userId: String, cost: Long): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val coins = (snap.getLong("starCoins") ?: 0L).coerceAtLeast(0L)
                check(coins >= cost) { "Not enough Star Coins" }
                tx.update(
                    docRef,
                    mapOf(
                        "starCoins" to (coins - cost),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun unlockOrEquipSkin(userId: String, skin: CannonSkin): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val rawUnlocked = (snap.get("unlockedSkins") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.ifEmpty { null }
                    ?: listOf(CannonSkin.CYAN_INTERCEPTOR.name)
                if (skin.name in rawUnlocked) {
                    tx.update(
                        docRef,
                        mapOf(
                            "equippedSkin" to skin.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val coins = (snap.getLong("starCoins") ?: 0L).coerceAtLeast(0L)
                    check(coins >= skin.coinCost) { "Not enough Star Coins" }
                    tx.update(
                        docRef,
                        mapOf(
                            "starCoins" to (coins - skin.coinCost),
                            "unlockedSkins" to (rawUnlocked + skin.name).distinct().take(10),
                            "equippedSkin" to skin.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimQuest(userId: String, quest: CosmicQuest): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val profile = snap.toObject(PlayerProfile::class.java)
                    ?: throw IllegalStateException("Profile not found")
                check(quest.isCompleted(profile)) { "Quest milestone not yet reached" }
                val rawClaimed = (snap.get("claimedQuests") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: emptyList()
                check(quest.id !in rawClaimed) { "Quest reward already claimed" }

                val coins = (snap.getLong("starCoins") ?: 0L).coerceAtLeast(0L)
                val updates = mutableMapOf<String, Any>(
                    "starCoins" to (coins + quest.rewardCoins).coerceAtMost(100_000_000L),
                    "claimedQuests" to (rawClaimed + quest.id).distinct().take(20),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                when (quest.rewardBooster) {
                    BoosterType.BOMB -> {
                        val prev = (snap.getLong("bombBoosters") ?: 0L).coerceAtLeast(0L)
                        updates["bombBoosters"] = (prev + 1L).coerceAtMost(999L)
                    }
                    BoosterType.PRISM -> {
                        val prev = (snap.getLong("prismBoosters") ?: 0L).coerceAtLeast(0L)
                        updates["prismBoosters"] = (prev + 1L).coerceAtMost(999L)
                    }
                    BoosterType.LASER -> {
                        val prev = (snap.getLong("laserBoosters") ?: 0L).coerceAtLeast(0L)
                        updates["laserBoosters"] = (prev + 1L).coerceAtMost(999L)
                    }
                    null -> Unit
                }
                tx.update(docRef, updates)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordSagaLevelResult(
        userId: String,
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
        val safeStars = stars.coerceIn(0, 3)
        val safeScore = score.coerceIn(0L, 10_000_000L)
        val safePopped = bubblesPopped.coerceIn(0, 100_000).toLong()
        val safeCombo = maxCombo.coerceIn(0, 10_000).toLong()
        val safeCoins = coinsEarned.coerceIn(0L, 50_000L)

        val levelDocId = "level_$safeLevel"
        val userDocPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }

            val userRef = db.collection("users").document(uid)
            val levelRef = userRef.collection("levels").document(levelDocId)
            val runRef = userRef.collection("runs").document()

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val levelSnap = tx.get(levelRef)

                val prevStars = (levelSnap.getLong("stars") ?: 0L).coerceIn(0L, 3L)
                val prevBest = levelSnap.getLong("bestScore") ?: 0L
                val prevCompleted = levelSnap.getBoolean("completed") ?: false

                val newStars = maxOf(prevStars, safeStars.toLong())
                val newBest = maxOf(prevBest, safeScore)
                val newCompleted = prevCompleted || won
                val starDelta = (newStars - prevStars).coerceAtLeast(0L)

                if (!levelSnap.exists()) {
                    tx.set(
                        levelRef,
                        mapOf(
                            "levelNumber" to safeLevel.toLong(),
                            "bestScore" to newBest,
                            "stars" to newStars,
                            "completed" to newCompleted,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.update(
                        levelRef,
                        mapOf(
                            "bestScore" to newBest,
                            "stars" to newStars,
                            "completed" to newCompleted,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val prevUnlocked = (userSnap.getLong("highestUnlockedLevel") ?: 1L).coerceIn(1L, 30L)
                val nextUnlocked = if (won) maxOf(prevUnlocked, (safeLevel + 1).coerceAtMost(30).toLong()) else prevUnlocked
                val prevTotalStars = (userSnap.getLong("totalStars") ?: 0L).coerceAtLeast(0L)
                val prevTotalPopped = (userSnap.getLong("totalBubblesPopped") ?: 0L).coerceAtLeast(0L)
                val prevCoins = (userSnap.getLong("starCoins") ?: 150L).coerceAtLeast(0L)
                val prevBombs = (userSnap.getLong("bombBoosters") ?: 2L).coerceAtLeast(0L)
                val prevPrisms = (userSnap.getLong("prismBoosters") ?: 2L).coerceAtLeast(0L)
                val prevLasers = (userSnap.getLong("laserBoosters") ?: 1L).coerceAtLeast(0L)

                val userUpdate = mutableMapOf<String, Any>(
                    "highestUnlockedLevel" to nextUnlocked,
                    "totalStars" to (prevTotalStars + starDelta).coerceAtMost(500L),
                    "totalBubblesPopped" to (prevTotalPopped + safePopped).coerceAtMost(100_000_000L),
                    "starCoins" to (prevCoins + safeCoins).coerceAtMost(100_000_000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                when (rewardBooster) {
                    BoosterType.BOMB -> userUpdate["bombBoosters"] = (prevBombs + 1L).coerceAtMost(999L)
                    BoosterType.PRISM -> userUpdate["prismBoosters"] = (prevPrisms + 1L).coerceAtMost(999L)
                    BoosterType.LASER -> userUpdate["laserBoosters"] = (prevLasers + 1L).coerceAtMost(999L)
                    null -> Unit
                }
                tx.update(userRef, userUpdate)

                tx.set(
                    runRef,
                    mapOf(
                        "mode" to GameMode.SAGA.name,
                        "levelNumber" to safeLevel.toLong(),
                        "score" to safeScore,
                        "maxCombo" to safeCombo,
                        "bubblesPopped" to safePopped,
                        "starsEarned" to safeStars.toLong(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocPath)
            Result.failure(e)
        }
    }

    suspend fun recordEndlessResult(
        userId: String,
        score: Long,
        maxCombo: Int,
        bubblesPopped: Int,
        coinsEarned: Long
    ): Result<Unit> {
        val safeScore = score.coerceIn(0L, 100_000_000L)
        val safeCombo = maxCombo.coerceIn(0, 10_000).toLong()
        val safePopped = bubblesPopped.coerceIn(0, 100_000).toLong()
        val safeCoins = coinsEarned.coerceIn(0L, 50_000L)
        val userDocPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val userRef = db.collection("users").document(uid)
            val runRef = userRef.collection("runs").document()

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val prevBest = (userSnap.getLong("endlessHighScore") ?: 0L).coerceAtLeast(0L)
                val prevTotalPopped = (userSnap.getLong("totalBubblesPopped") ?: 0L).coerceAtLeast(0L)
                val prevCoins = (userSnap.getLong("starCoins") ?: 150L).coerceAtLeast(0L)

                tx.update(
                    userRef,
                    mapOf(
                        "endlessHighScore" to maxOf(prevBest, safeScore),
                        "totalBubblesPopped" to (prevTotalPopped + safePopped).coerceAtMost(100_000_000L),
                        "starCoins" to (prevCoins + safeCoins).coerceAtMost(100_000_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    runRef,
                    mapOf(
                        "mode" to GameMode.ENDLESS.name,
                        "levelNumber" to 0L,
                        "score" to safeScore,
                        "maxCombo" to safeCombo,
                        "bubblesPopped" to safePopped,
                        "starsEarned" to 0L,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocPath)
            Result.failure(e)
        }
    }

    suspend fun recordDailyChallengeResult(
        userId: String,
        dayKey: String,
        previousDayKey: String,
        score: Long,
        stars: Int,
        maxCombo: Int,
        bubblesPopped: Int,
        won: Boolean,
        coinsEarned: Long
    ): Result<Unit> {
        val safeDay = dayKey.take(16)
        val safeScore = score.coerceIn(0L, 100_000_000L)
        val safeCombo = maxCombo.coerceIn(0, 10_000).toLong()
        val safePopped = bubblesPopped.coerceIn(0, 100_000).toLong()
        val safeStars = stars.coerceIn(0, 3).toLong()
        val safeCoins = coinsEarned.coerceIn(0L, 50_000L)
        val userDocPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val userRef = db.collection("users").document(uid)
            val runRef = userRef.collection("runs").document()

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val prevTotalPopped = (userSnap.getLong("totalBubblesPopped") ?: 0L).coerceAtLeast(0L)
                val prevCoins = (userSnap.getLong("starCoins") ?: 150L).coerceAtLeast(0L)
                val prevStreak = (userSnap.getLong("dailyStreak") ?: 0L).coerceAtLeast(0L)
                val prevLastDay = userSnap.getString("lastDailyDay") ?: ""

                val (nextStreak, nextLastDay) = if (won && prevLastDay != safeDay) {
                    val incremented = if (prevLastDay == previousDayKey) prevStreak + 1L else 1L
                    incremented.coerceAtMost(3650L) to safeDay
                } else {
                    prevStreak to prevLastDay
                }

                tx.update(
                    userRef,
                    mapOf(
                        "totalBubblesPopped" to (prevTotalPopped + safePopped).coerceAtMost(100_000_000L),
                        "starCoins" to (prevCoins + safeCoins).coerceAtMost(100_000_000L),
                        "dailyStreak" to nextStreak,
                        "lastDailyDay" to nextLastDay,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    runRef,
                    mapOf(
                        "mode" to GameMode.DAILY.name,
                        "levelNumber" to 0L,
                        "score" to safeScore,
                        "maxCombo" to safeCombo,
                        "bubblesPopped" to safePopped,
                        "starsEarned" to safeStars,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocPath)
            Result.failure(e)
        }
    }
}
