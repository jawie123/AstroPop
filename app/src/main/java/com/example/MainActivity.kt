package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ads.AstroAdsManager
import com.example.ads.findActivity
import com.example.audio.SoundSynth
import com.example.auth.AuthScreen
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOut
import com.example.data.AstroRepository
import com.example.data.FirestoreProgressStore
import com.example.data.GameMode
import com.example.data.GuestProgressStore
import com.example.data.UiState
import com.example.game.GameViewModel
import com.example.ui.GameScreen
import com.example.ui.ProfileViewModel
import com.example.ui.SagaHomeScreen
import com.example.ui.StatsSettingsSheet
import com.example.ui.theme.AstroCyan
import com.example.ui.theme.AstroPopTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AstroPopTheme {
                AstroPopRoot()
            }
        }
    }
}

@Composable
private fun AstroPopRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isGuestMode by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isGuestMode = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    when {
        user != null -> {
            val uid = user.uid
            val displayName = user.displayName ?: "StarCaptain"
            key(uid) {
                AstroPopPlayFlow(
                    sessionKey = uid,
                    isGuest = false,
                    displayName = displayName,
                    onExitGuest = {}
                )
            }
        }
        isGuestMode -> {
            key("guest_local") {
                AstroPopPlayFlow(
                    sessionKey = "guest_local",
                    isGuest = true,
                    displayName = "Guest Pilot",
                    onExitGuest = { isGuestMode = false }
                )
            }
        }
        else -> {
            AuthScreen(
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    isGuestMode = false
                },
                onPlayAsGuest = {
                    isGuestMode = true
                }
            )
        }
    }
}

@Composable
private fun AstroPopPlayFlow(
    sessionKey: String,
    isGuest: Boolean,
    displayName: String,
    onExitGuest: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val soundSynth = remember { SoundSynth() }
    val adsManager = remember(context) { AstroAdsManager(context).apply { initialize(activity) } }
    val isRewardedAdReady by adsManager.isRewardedReady.collectAsStateWithLifecycle()
    val canRequestAds by adsManager.canRequestAds.collectAsStateWithLifecycle()
    val isPrivacyOptionsRequired by adsManager.isPrivacyOptionsRequired.collectAsStateWithLifecycle()

    val progressStore = remember(sessionKey, isGuest) {
        if (isGuest) {
            GuestProgressStore(context.applicationContext)
        } else {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            FirestoreProgressStore(AstroRepository(db), sessionKey)
        }
    }

    val profileViewModel: ProfileViewModel = viewModel(
        key = "profile_$sessionKey",
        factory = viewModelFactory {
            initializer {
                ProfileViewModel(progressStore, displayName)
            }
        }
    )

    val gameViewModel: GameViewModel = viewModel(
        key = "game_$sessionKey",
        factory = viewModelFactory {
            initializer {
                GameViewModel(progressStore, soundSynth)
            }
        }
    )

    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val levelsState by profileViewModel.levelsState.collectAsStateWithLifecycle()
    val runsState by profileViewModel.runsState.collectAsStateWithLifecycle()
    val leaderboardState by profileViewModel.leaderboardState.collectAsStateWithLifecycle()
    val shopFeedback by profileViewModel.shopFeedback.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val levels = (levelsState as? UiState.Success)?.data ?: emptyList()
    val runs = (runsState as? UiState.Success)?.data ?: emptyList()
    val errorMsg = (profileState as? UiState.Error)?.message
        ?: (levelsState as? UiState.Error)?.message

    LaunchedEffect(profile) {
        gameViewModel.syncProfileMetadata(profile)
    }

    var activeMode by rememberSaveable { mutableStateOf<GameMode?>(null) }
    var activeLevel by rememberSaveable { mutableStateOf(1) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var guestAuthError by remember { mutableStateOf<String?>(null) }

    if (profileState is UiState.Loading && profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AstroCyan)
        }
        return
    }

    if (activeMode == null) {
        SagaHomeScreen(
            profile = profile,
            levels = levels,
            leaderboardState = leaderboardState,
            isGuest = isGuest,
            errorMessage = errorMsg,
            shopFeedback = shopFeedback,
            isRewardedAdReady = isRewardedAdReady,
            canRequestAds = canRequestAds,
            onPlaySagaLevel = { levelNum, booster, adFree ->
                activeLevel = levelNum
                activeMode = GameMode.SAGA
                gameViewModel.startSagaLevel(levelNum, profile, booster, adFree)
            },
            onPlayDaily = { booster, adFree ->
                activeMode = GameMode.DAILY
                gameViewModel.startDailyChallenge(profile, booster, adFree)
            },
            onPlayEndless = { booster, adFree ->
                activeMode = GameMode.ENDLESS
                gameViewModel.startEndlessMode(profile, booster, adFree)
            },
            onPlayBlitz = { booster, adFree ->
                activeMode = GameMode.BLITZ
                gameViewModel.startBlitzMode(profile, booster, adFree)
            },
            onPurchaseBooster = profileViewModel::purchaseBooster,
            onUnlockOrEquipSkin = profileViewModel::unlockOrEquipSkin,
            onClaimQuest = profileViewModel::claimQuest,
            onClaimDailySupply = profileViewModel::claimDailySupply,
            onClaimWheelPrize = profileViewModel::claimWheelPrize,
            onClaimPassReward = profileViewModel::claimPassReward,
            onOpenMysteryChest = profileViewModel::openMysteryChest,
            onWatchAdForCoins = {
                adsManager.showRewarded(
                    activity = activity,
                    onRewardEarned = { profileViewModel.grantAdRewardCoins(75L) },
                    onUnavailable = { profileViewModel.reportAdFeedback("Ad still loading, please try again shortly.") }
                )
            },
            onWatchAdForSpin = { onSpinUnlocked ->
                adsManager.showRewarded(
                    activity = activity,
                    onRewardEarned = onSpinUnlocked,
                    onUnavailable = { profileViewModel.reportAdFeedback("Ad still loading, please try again shortly.") }
                )
            },
            onWatchAdForChest = { onChestUnlocked ->
                adsManager.showRewarded(
                    activity = activity,
                    onRewardEarned = onChestUnlocked,
                    onUnavailable = { profileViewModel.reportAdFeedback("Ad still loading, please try again shortly.") }
                )
            },
            onWatchAdForPassBonus = { onBonusUnlocked ->
                adsManager.showRewarded(
                    activity = activity,
                    onRewardEarned = onBonusUnlocked,
                    onUnavailable = { profileViewModel.reportAdFeedback("Ad still loading, please try again shortly.") }
                )
            },
            onWatchAdForStarterBomb = { onStarterUnlocked ->
                adsManager.showRewarded(
                    activity = activity,
                    onRewardEarned = onStarterUnlocked,
                    onUnavailable = { profileViewModel.reportAdFeedback("Ad still loading, please try again shortly.") }
                )
            },
            onClearShopFeedback = profileViewModel::clearShopFeedback,
            onOpenSettings = {
                guestAuthError = null
                showSettingsSheet = true
            }
        )
    } else {
        GameScreen(
            viewModel = gameViewModel,
            isRewardedAdReady = isRewardedAdReady,
            onWatchAdSecondChance = {
                adsManager.showRewarded(
                    activity = activity,
                    onRewardEarned = { gameViewModel.grantSecondChanceFromAd() },
                    onUnavailable = {}
                )
            },
            onExitToMap = {
                adsManager.maybeShowInterstitial(activity) {
                    activeMode = null
                }
            },
            onNextLevel = { nextNum ->
                adsManager.maybeShowInterstitial(activity) {
                    activeLevel = nextNum
                    gameViewModel.startSagaLevel(nextNum, profile)
                }
            },
            onRetry = {
                adsManager.maybeShowInterstitial(activity) {
                    when (activeMode) {
                        GameMode.SAGA -> gameViewModel.startSagaLevel(activeLevel, profile)
                        GameMode.DAILY -> gameViewModel.startDailyChallenge(profile)
                        GameMode.BLITZ -> gameViewModel.startBlitzMode(profile)
                        GameMode.ENDLESS, null -> gameViewModel.startEndlessMode(profile)
                    }
                }
            }
        )
    }

    if (showSettingsSheet) {
        StatsSettingsSheet(
            profile = profile,
            recentRuns = runs,
            isGuest = isGuest,
            authError = guestAuthError,
            showAdPrivacyOptions = isPrivacyOptionsRequired,
            onOpenAdPrivacyOptions = { adsManager.showPrivacyOptionsForm(activity) },
            onToggleSound = { enabled ->
                soundSynth.enabled = enabled
                profileViewModel.updatePreferences(
                    soundEnabled = enabled,
                    hapticsEnabled = profile?.hapticsEnabled ?: true
                )
            },
            onToggleHaptics = { enabled ->
                profileViewModel.updatePreferences(
                    soundEnabled = profile?.soundEnabled ?: true,
                    hapticsEnabled = enabled
                )
            },
            onSignInWithGoogle = {
                guestAuthError = null
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = { showSettingsSheet = false },
                    onAuthError = { msg -> guestAuthError = msg },
                    scope = scope
                )
            },
            onExitGuest = {
                showSettingsSheet = false
                onExitGuest()
            },
            onSignOut = {
                showSettingsSheet = false
                signOut(credentialManager, scope)
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}
