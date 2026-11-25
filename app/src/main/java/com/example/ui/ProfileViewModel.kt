package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BoosterType
import com.example.data.CannonSkin
import com.example.data.CosmicQuest
import com.example.data.GameProgressStore
import com.example.data.GameRun
import com.example.data.LevelProgress
import com.example.data.PlayerProfile
import com.example.data.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val store: GameProgressStore,
    private val initialDisplayName: String
) : ViewModel() {

    private val _shopFeedback = MutableStateFlow<String?>(null)
    val shopFeedback: StateFlow<String?> = _shopFeedback.asStateFlow()

    val profileState: StateFlow<UiState<PlayerProfile?>> = store.observeProfile()
        .map<PlayerProfile?, UiState<PlayerProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load player profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val levelsState: StateFlow<UiState<List<LevelProgress>>> = store.observeLevels()
        .map<List<LevelProgress>, UiState<List<LevelProgress>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing levels", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load level progress"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val runsState: StateFlow<UiState<List<GameRun>>> = store.observeRecentRuns()
        .map<List<GameRun>, UiState<List<GameRun>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing recent runs", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load recent runs"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            store.ensureProfile(initialDisplayName)
        }
    }

    fun updatePreferences(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        viewModelScope.launch {
            store.updatePreferences(soundEnabled, hapticsEnabled)
        }
    }

    fun purchaseBooster(booster: BoosterType) {
        viewModelScope.launch {
            val result = store.purchaseBooster(booster)
            _shopFeedback.value = if (result.isSuccess) {
                "Equipped +1 ${booster.label}!"
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Not enough Star Coins"
            }
        }
    }

    fun unlockOrEquipSkin(skin: CannonSkin) {
        viewModelScope.launch {
            val result = store.unlockOrEquipSkin(skin)
            _shopFeedback.value = if (result.isSuccess) {
                "Equipped ${skin.title}!"
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Unable to unlock cannon skin"
            }
        }
    }

    fun claimQuest(quest: CosmicQuest) {
        viewModelScope.launch {
            val result = store.claimQuest(quest)
            _shopFeedback.value = if (result.isSuccess) {
                val boosterMsg = quest.rewardBooster?.let { " + 1 ${it.label}" } ?: ""
                "Quest Claimed: +${quest.rewardCoins}c$boosterMsg!"
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Quest not ready to claim"
            }
        }
    }

    fun clearShopFeedback() {
        _shopFeedback.value = null
    }

    private companion object {
        const val TAG = "ProfileVM"
    }
}
