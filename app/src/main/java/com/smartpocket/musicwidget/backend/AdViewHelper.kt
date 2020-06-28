package com.smartpocket.musicwidget.backend

import android.app.Activity
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.*
import java.util.logging.Level
import java.util.logging.Logger

// This is an ad unit ID for a test ad. Replace with your own banner ad unit ID.
private const val AD_UNIT_ID = "ca-app-pub-6954073861191346/2819818113" //REAL ADS
//private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"  //TEST ADS

class AdViewHelper(private val adViewContainer: ViewGroup, private val activity: Activity) {

    private val logger = Logger.getLogger(javaClass.simpleName)
    private var adView: AdView? = null
    private var isLoaded = false

    fun showBanner(doShow: Boolean) {
        if (doShow) {
            if (isLoaded.not()) {
                loadBanner()
            }
        } else {
            destroy()
        }
    }

    fun destroy() {
        isLoaded = false
        adViewContainer.visibility = View.GONE
        adView?.destroy()
        adView = null
    }

    private fun loadBanner() {
        isLoaded = true
        adViewContainer.visibility = View.VISIBLE
        // Create an ad request. Check your logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        val localAdView = AdView(activity).apply {
            adUnitId = AD_UNIT_ID
            adListener = object : AdListener() {

                override fun onAdFailedToLoad(p0: Int) {
                    val error = when (p0) {
                        AdRequest.ERROR_CODE_INTERNAL_ERROR -> "Internal error"
                        AdRequest.ERROR_CODE_INVALID_REQUEST -> "Invalid request"
                        AdRequest.ERROR_CODE_NETWORK_ERROR -> "Network connectivity error"
                        AdRequest.ERROR_CODE_NO_FILL -> "Lack of ad inventory"
                        else -> "Unspecified error"
                    }
                    logger.log(Level.INFO, "Ad failed to load: $error")
                    // Will retry again automatically in 60 seconds
                    isLoaded = false
                }

                override fun onAdLoaded() {
                    logger.log(Level.INFO, "Ad loaded successfully")
                    isLoaded = true
                }
            }
        }

        adView = localAdView
        adViewContainer.removeAllViews()
        adViewContainer.addView(localAdView)

        val adSize: AdSize = adSize
        localAdView.adSize = adSize

        val testDevices = listOf(
                "97EB45A0B9C0380783B9EC4628B453EB", // Galaxy S8+
                "3CCDD6B7DFE0C26EB206729F862B8B39", // LG G3
                AdRequest.DEVICE_ID_EMULATOR
        )

        val requestConfiguration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDevices)
                .build()
        MobileAds.setRequestConfiguration(requestConfiguration)

        val adRequest = AdRequest.Builder().build()
        // Start loading the ad in the background.
        localAdView.loadAd(adRequest)
    }

    // Determine the screen width (less decorations) to use for the ad width.
    // If the ad hasn't been laid out, default to the full screen width.
    private val adSize: AdSize
        get() {
            val display = activity.windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val density = outMetrics.density

            var adWidthPixels = adViewContainer.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
        }
}