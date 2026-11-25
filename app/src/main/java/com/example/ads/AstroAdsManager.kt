package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AstroAdsManager(context: Context) {

    private val appContext = context.applicationContext
    private var initialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewarded = false
    private var completedRunCount = 0

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    fun initialize() {
        if (initialized) return
        initialized = true
        MobileAds.initialize(appContext) {
            preloadInterstitial()
            preloadRewarded()
        }
    }

    fun preloadInterstitial() {
        if (interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true
        val unitId = appContext.getString(R.string.admob_interstitial_unit_id)
        InterstitialAd.load(
            appContext,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }

    fun preloadRewarded() {
        if (rewardedAd != null || isLoadingRewarded) return
        isLoadingRewarded = true
        val unitId = appContext.getString(R.string.admob_rewarded_unit_id)
        RewardedAd.load(
            appContext,
            unitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                    _isRewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed to load: ${error.message}")
                    rewardedAd = null
                    isLoadingRewarded = false
                    _isRewardedReady.value = false
                }
            }
        )
    }

    fun maybeShowInterstitial(activity: Activity?, onContinue: () -> Unit) {
        completedRunCount++
        val ad = interstitialAd
        if (completedRunCount % 2 != 0 || ad == null || activity == null) {
            if (ad == null) preloadInterstitial()
            onContinue()
            return
        }
        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadInterstitial()
                onContinue()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                preloadInterstitial()
                onContinue()
            }
        }
        ad.show(activity)
    }

    fun showRewarded(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onUnavailable: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad == null || activity == null) {
            preloadRewarded()
            onUnavailable()
            return
        }
        rewardedAd = null
        _isRewardedReady.value = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadRewarded()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                preloadRewarded()
            }
        }
        ad.show(activity) {
            onRewardEarned()
        }
    }

    private companion object {
        const val TAG = "AstroAdsManager"
    }
}

@Composable
fun AstroBannerAd(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) return
    val context = LocalContext.current
    val unitId = remember(context) { context.getString(R.string.admob_banner_unit_id) }
    val adView = remember(context, unitId) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = unitId
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .testTag("home_banner_ad"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(factory = { adView })
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
