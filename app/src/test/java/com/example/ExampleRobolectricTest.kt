package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.PaymentGatewayRepository
import com.example.data.repository.PaymentProvider
import com.example.model.AspectRatioOption
import com.example.model.BgRemovalMode
import com.example.model.BgReplacementType
import com.example.model.BillingCycle
import com.example.model.CameraMovement
import com.example.model.ChromaKeyColor
import com.example.model.MotionTemplate
import com.example.model.MuseGeneratedClip
import com.example.model.SubscriptionPlan
import com.example.model.TimelineTrackType
import com.example.model.isPaidTier
import com.example.model.isUnlimitedAiTier
import com.example.model.isWatermarkFreeTier
import com.example.viewmodel.OmniModule
import com.example.viewmodel.StudioViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OmniStudio", appName)
  }

  @Test
  fun studioViewModelModuleAndTimelineOperations() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = StudioViewModel(app)

    // Initial state checks
    assertEquals(OmniModule.STUDIO, vm.uiState.value.activeModule)
    assertEquals("FREE", vm.uiState.value.subscriptionTier)
    assertEquals(5, vm.uiState.value.aiCreditsRemaining)
    val initialClipsCount = vm.uiState.value.timelineClips.size
    assertTrue(initialClipsCount >= 4)

    // Switch modules including BG_REMOVAL (5-tab architecture)
    vm.setActiveModule(OmniModule.DIRECTOR)
    assertEquals(OmniModule.DIRECTOR, vm.uiState.value.activeModule)
    vm.setActiveModule(OmniModule.MUSE_AI)
    assertEquals(OmniModule.MUSE_AI, vm.uiState.value.activeModule)
    vm.setActiveModule(OmniModule.BG_REMOVAL)
    assertEquals(OmniModule.BG_REMOVAL, vm.uiState.value.activeModule)
    assertEquals(5, OmniModule.entries.size)

    // Split & Trim clip at playhead
    vm.seekPlayhead(5f)
    vm.splitClipAtPlayhead("c_prod")
    assertEquals(initialClipsCount + 1, vm.uiState.value.timelineClips.size)

    val selectedId = vm.uiState.value.selectedClipId
    val beforeDur = vm.uiState.value.timelineClips.first { it.id == selectedId }.durationSec
    vm.trimSelectedClip(deltaDuration = -0.5f)
    val afterDur = vm.uiState.value.timelineClips.first { it.id == selectedId }.durationSec
    assertTrue(afterDur <= beforeDur)

    // One-Tap Photo-to-Ad switches back to Studio 9:16 with Dynamic Zoom Punch
    vm.oneTapPhotoToAd("content://media/external/images/media/101", "Sneaker Drop")
    assertEquals(OmniModule.STUDIO, vm.uiState.value.activeModule)
    assertEquals(AspectRatioOption.RATIO_9_16, vm.uiState.value.aspectRatio)
    assertEquals(MotionTemplate.DYNAMIC_ZOOM_PUNCH, vm.uiState.value.motionTemplate)

    // PRO upgrade modal state
    assertFalse(vm.uiState.value.showProUpgradeModal)
    vm.openProUpgradeModal("Test Upgrade")
    assertTrue(vm.uiState.value.showProUpgradeModal)
    assertEquals("Test Upgrade", vm.uiState.value.paywallReason)
    vm.dismissProUpgradeModal()
    assertFalse(vm.uiState.value.showProUpgradeModal)

    // Dynamic Light / Dark theme toggle
    assertTrue(vm.uiState.value.isDarkTheme)
    vm.toggleTheme()
    assertFalse(vm.uiState.value.isDarkTheme)
    vm.setDarkTheme(true)
    assertTrue(vm.uiState.value.isDarkTheme)

    // Muse AI Camera Movement coverage (Pan, Zoom, Tilt) & Multi-Track Timeline Injection
    assertTrue(CameraMovement.entries.any { it.axisTag.contains("Pan") })
    assertTrue(CameraMovement.entries.any { it.axisTag.contains("Zoom") })
    assertTrue(CameraMovement.entries.any { it.axisTag.contains("Tilt") })

    val beforeMuseClips = vm.uiState.value.timelineClips.size
    val museClip = MuseGeneratedClip(
      id = "muse_test_01",
      title = "Cyberpunk Tilt Shot",
      prompt = "Neon smartwatch reveal",
      cameraMovement = CameraMovement.CINEMATIC_TILT,
      panAmount = 0.4f,
      zoomFactor = 1.45f,
      tiltAngle = 0.65f,
      aspectRatio = AspectRatioOption.RATIO_16_9,
      audioCue = "Sub-bass swoosh",
      sceneDirection = "Vertical tilt reveal",
      suggestedHook = "NEXT-GEN TIMEPIECE",
      durationSec = 4.5f,
      keyframeBitmap = null
    )
    vm.importMuseClipToTimeline(museClip)

    assertEquals(AspectRatioOption.RATIO_16_9, vm.uiState.value.aspectRatio)
    assertEquals(OmniModule.STUDIO, vm.uiState.value.activeModule)
    assertEquals(beforeMuseClips + 3, vm.uiState.value.timelineClips.size)
    assertTrue(vm.uiState.value.timelineClips.any { it.id == "tl_muse_test_01" && it.trackType == TimelineTrackType.PRODUCT })
    assertTrue(vm.uiState.value.timelineClips.any { it.id == "fx_muse_test_01" && it.trackType == TimelineTrackType.EFFECT })
    assertTrue(vm.uiState.value.timelineClips.any { it.id == "aud_muse_test_01" && it.trackType == TimelineTrackType.AUDIO })
  }

  @Test
  fun subscriptionPlansAndVideoBgRemovalStateBehaveCorrectly() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = StudioViewModel(app)

    // Verify 4 structured subscription tiers and pricing/discount math
    assertEquals(0.0, SubscriptionPlan.FREE.monthlyPriceUsd, 0.01)
    assertEquals(5, SubscriptionPlan.FREE.dailyAiCredits)
    assertFalse(isPaidTier("FREE"))
    assertFalse(isWatermarkFreeTier("FREE"))
    assertFalse(isUnlimitedAiTier("FREE"))

    assertEquals(5.0, SubscriptionPlan.BASIC.monthlyPriceUsd, 0.01)
    assertEquals(4.0, SubscriptionPlan.BASIC.yearlyMonthlyPriceUsd, 0.01)
    assertEquals(48.0, SubscriptionPlan.BASIC.yearlyTotalUsd, 0.01)
    assertEquals(50, SubscriptionPlan.BASIC.dailyAiCredits)
    assertTrue(isPaidTier("BASIC"))
    assertFalse(isWatermarkFreeTier("BASIC"))
    assertFalse(isUnlimitedAiTier("BASIC"))

    assertEquals(15.0, SubscriptionPlan.PRO.monthlyPriceUsd, 0.01)
    assertEquals(12.0, SubscriptionPlan.PRO.yearlyMonthlyPriceUsd, 0.01)
    assertEquals(144.0, SubscriptionPlan.PRO.yearlyTotalUsd, 0.01)
    assertTrue(isWatermarkFreeTier("PRO"))
    assertTrue(isUnlimitedAiTier("PRO"))

    assertEquals(20.0, SubscriptionPlan.PREMIUM.monthlyPriceUsd, 0.01)
    assertEquals(17.0, SubscriptionPlan.PREMIUM.yearlyMonthlyPriceUsd, 0.01)
    assertEquals(204.0, SubscriptionPlan.PREMIUM.yearlyTotalUsd, 0.01)
    assertTrue(isWatermarkFreeTier("PREMIUM"))
    assertTrue(isUnlimitedAiTier("PREMIUM"))

    // Verified checkout rejects unauthenticated attempts gracefully (strict monetization gate)
    var checkoutOk = true
    var checkoutMsg: String? = null
    vm.completeVerifiedSubscription(
      plan = SubscriptionPlan.PRO,
      billingCycle = BillingCycle.YEARLY,
      paymentMethod = "Google Play Billing",
      billingEmail = "enjebeln@gmail.com"
    ) { ok, msg ->
      checkoutOk = ok
      checkoutMsg = msg
    }
    assertFalse(checkoutOk)
    assertNotNull(checkoutMsg)
    assertEquals("FREE", vm.uiState.value.subscriptionTier)

    // Exhausted daily credits trigger hard paywall modal on Muse/Director generation
    val prefs = app.getSharedPreferences("omnistudio_prefs", Context.MODE_PRIVATE)
    val today = com.example.data.repository.FirestoreRepository.todayDateKey()
    prefs.edit()
      .putString("last_credit_reset_date", today)
      .putInt("daily_credits_remaining", 0)
      .apply()
    val vmExhausted = StudioViewModel(app)
    vmExhausted.generateMuseVideos(
      prompt = "Product launch shot",
      cameraMovement = CameraMovement.SLOW_PUSH_IN,
      aspectRatio = AspectRatioOption.RATIO_9_16,
      audioCue = "Synth swell",
      batchCount = 1
    )
    assertTrue(vmExhausted.uiState.value.showProUpgradeModal)
    assertNotNull(vmExhausted.uiState.value.paywallReason)

    // Video BG Removal state updates cleanly
    vm.updateBgRemovalMode(BgRemovalMode.CHROMA_KEY)
    vm.updateBgChromaColor(ChromaKeyColor.GREEN)
    vm.updateBgSensitivity(0.65f)
    vm.updateBgFeather(0.25f)
    vm.updateBgReplacementType(BgReplacementType.BLUR_ORIGINAL)
    assertEquals(BgReplacementType.BLUR_ORIGINAL, vm.uiState.value.videoBgState.replacementType)

    vm.updateBgSolidColor(0xFF8B5CF6L)

    val bg = vm.uiState.value.videoBgState
    assertEquals(BgRemovalMode.CHROMA_KEY, bg.mode)
    assertEquals(ChromaKeyColor.GREEN, bg.chromaKeyColor)
    assertEquals(0.65f, bg.sensitivity, 0.01f)
    assertEquals(0.25f, bg.edgeFeather, 0.01f)
    assertEquals(BgReplacementType.SOLID_COLOR, bg.replacementType)
    assertEquals(0xFF8B5CF6L, bg.solidColorArgb)
  }

  @Test
  fun paymentGatewayDeepLinkAndCheckoutRouting() {
    val gateway = PaymentGatewayRepository()

    // Placeholder / unset PAYMENT_BACKEND_URL is safely reported as unconfigured
    assertFalse(gateway.isBackendConfigured())
    assertFalse(gateway.hasProviderPublicKey(PaymentProvider.STRIPE))
    assertFalse(gateway.hasProviderPublicKey(PaymentProvider.FLUTTERWAVE))

    // Valid Stripe & Flutterwave callback URI parsing
    val stripeSuccessUri = Uri.parse(
      "omnistudio://checkout/callback?status=successful&session_id=cs_test_123&tx_ref=pi_999&plan=PRO&cycle=YEARLY&provider=STRIPE"
    )
    val parsedSuccess = gateway.parseDeepLinkCallback(stripeSuccessUri)
    assertNotNull(parsedSuccess)
    assertTrue(parsedSuccess!!.isSuccessful)
    assertFalse(parsedSuccess.isCancelled)
    assertEquals("cs_test_123", parsedSuccess.sessionId)
    assertEquals("pi_999", parsedSuccess.transactionRef)
    assertEquals(SubscriptionPlan.PRO, parsedSuccess.plan)
    assertEquals(BillingCycle.YEARLY, parsedSuccess.billingCycle)
    assertEquals(PaymentProvider.STRIPE, parsedSuccess.provider)

    val flutterwaveCancelUri = Uri.parse(
      "omnistudio://checkout/callback?status=cancelled&plan=PREMIUM&cycle=MONTHLY&provider=FLUTTERWAVE"
    )
    val parsedCancel = gateway.parseDeepLinkCallback(flutterwaveCancelUri)
    assertNotNull(parsedCancel)
    assertFalse(parsedCancel!!.isSuccessful)
    assertTrue(parsedCancel.isCancelled)
    assertEquals(SubscriptionPlan.PREMIUM, parsedCancel.plan)
    assertEquals(BillingCycle.MONTHLY, parsedCancel.billingCycle)
    assertEquals(PaymentProvider.FLUTTERWAVE, parsedCancel.provider)

    // Non-matching schemes are safely ignored
    assertEquals(null, gateway.parseDeepLinkCallback(Uri.parse("https://example.com/callback")))

    // ViewModel payment provider selection & unauthenticated checkout gate
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = StudioViewModel(app)
    assertEquals(PaymentProvider.STRIPE, vm.uiState.value.selectedPaymentProvider)
    vm.selectPaymentProvider(PaymentProvider.FLUTTERWAVE)
    assertEquals(PaymentProvider.FLUTTERWAVE, vm.uiState.value.selectedPaymentProvider)

    var launchedUrl: String? = null
    var initOk = true
    var initMsg: String? = null
    vm.initiatePlanCheckout(
      plan = SubscriptionPlan.BASIC,
      billingCycle = BillingCycle.MONTHLY,
      provider = PaymentProvider.FLUTTERWAVE,
      billingEmail = "enjebeln@gmail.com",
      onLaunchCheckoutUrl = { launchedUrl = it }
    ) { ok, msg ->
      initOk = ok
      initMsg = msg
    }
    assertFalse(initOk)
    assertEquals(null, launchedUrl)
    assertNotNull(initMsg)
    assertNotNull(vm.uiState.value.paymentError)

    // Cancelled deep link clears processing state and records cancellation error
    var cancelOk = true
    var cancelMsg: String? = null
    vm.handleCheckoutCallbackUri(flutterwaveCancelUri) { ok, msg ->
      cancelOk = ok
      cancelMsg = msg
    }
    assertFalse(cancelOk)
    assertNotNull(cancelMsg)
    assertFalse(vm.uiState.value.isProcessingPayment)
  }
}
