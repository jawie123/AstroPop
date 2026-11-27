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
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

class AstroAdsManager(context: Context) {

    private val appContext = context.applicationContext
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(appContext)
    private val mobileAdsInitialized = AtomicBoolean(false)
    private var consentRequested = false
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewarded = false
    private var completedRunCount = 0

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    private val _canRequestAds = MutableStateFlow(consentInformation.canRequestAds())
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(
        consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    )
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    fun initialize(activity: Activity? = null) {
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk()
        }
        if (consentRequested || activity == null) return
        consentRequested = true

        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "UMP consent form error: ${formError.message}")
                    }
                    refreshConsentFlags()
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk()
                    }
                }
            },
            { requestError ->
                Log.w(TAG, "UMP consent info update failed: ${requestError.message}")
                refreshConsentFlags()
                if (consentInformation.canRequestAds()) {
                    initializeMobileAdsSdk()
                }
            }
        )
    }

    private fun refreshConsentFlags() {
        _canRequestAds.value = consentInformation.canRequestAds()
        _isPrivacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun initializeMobileAdsSdk() {
        _canRequestAds.value = true
        if (mobileAdsInitialized.getAndSet(true)) return
        MobileAds.initialize(appContext) {
            preloadInterstitial()
            preloadRewarded()
        }
    }

    fun showPrivacyOptionsForm(activity: Activity?, onDismissed: () -> Unit = {}) {
        if (activity == null) {
            onDismissed()
            return
        }
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options form error: ${formError.message}")
            }
            refreshConsentFlags()
            if (consentInformation.canRequestAds()) {
                initializeMobileAdsSdk()
            }
            onDismissed()
        }
    }

    fun preloadInterstitial() {
        if (!consentInformation.canRequestAds()) return
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
        if (!consentInformation.canRequestAds()) return
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
        if (!consentInformation.canRequestAds() || completedRunCount % 2 != 0 || ad == null || activity == null) {
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
        if (!consentInformation.canRequestAds() || ad == null || activity == null) {
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
fun AstroBannerAd(
    canRequestAds: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current || !canRequestAds) return
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
