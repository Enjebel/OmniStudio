package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.CheckoutLaunchMode
import com.example.data.repository.CheckoutSessionInit
import com.example.data.repository.PaymentGatewayRepository
import com.example.data.repository.PaymentProvider
import com.example.data.repository.StripePaymentSheetPayload
import org.json.JSONObject
import com.example.model.AspectRatioOption
import com.example.model.BgRemovalMode
import com.example.model.BgReplacementType
import com.example.model.BillingCycle
import com.example.model.CameraMovement
import com.example.model.ChromaKeyColor
import com.example.model.ColorFilterPreset
import com.example.model.LayerType
import com.example.model.MotionTemplate
import com.example.model.MuseGeneratedClip
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.ai.DirectorCommandResult
import com.example.model.CanvasLayer
import com.example.model.SubscriptionPlan
import com.example.model.TimelineClip
import com.example.model.TimelineTrackType
import com.example.model.TransitionEffect
import com.example.model.isPaidTier
import com.example.model.isUnlimitedAiTier
import com.example.model.isWatermarkFreeTier
import com.example.studio.ColorGradingUtils
import com.example.ui.components.FiltersScreen
import com.example.ui.components.StudioTimeline
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.OmniModule
import com.example.viewmodel.StudioViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

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

    // Switch modules including TRANSITIONS, FILTERS, and BG_REMOVAL (7-tab architecture)
    vm.setActiveModule(OmniModule.DIRECTOR)
    assertEquals(OmniModule.DIRECTOR, vm.uiState.value.activeModule)
    vm.setActiveModule(OmniModule.MUSE_AI)
    assertEquals(OmniModule.MUSE_AI, vm.uiState.value.activeModule)
    vm.setActiveModule(OmniModule.TRANSITIONS)
    assertEquals(OmniModule.TRANSITIONS, vm.uiState.value.activeModule)
    vm.setActiveModule(OmniModule.FILTERS)
    assertEquals(OmniModule.FILTERS, vm.uiState.value.activeModule)
    vm.setActiveModule(OmniModule.BG_REMOVAL)
    assertEquals(OmniModule.BG_REMOVAL, vm.uiState.value.activeModule)
    assertEquals(7, OmniModule.entries.size)

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

    // Transitions tab controls: apply to single clip & apply between all multi-layered clips
    vm.selectTransitionPreset(TransitionEffect.SLIDE_LEFT)
    vm.setTransitionDuration(0.75f)
    vm.applyTransitionToClip("tl_muse_test_01", TransitionEffect.SLIDE_LEFT, 0.75f)
    val updatedSingle = vm.uiState.value.timelineClips.first { it.id == "tl_muse_test_01" }
    assertEquals(TransitionEffect.SLIDE_LEFT, updatedSingle.transitionIn)
    assertEquals(0.75f, updatedSingle.transitionDurationSec, 0.01f)
    assertTrue(vm.uiState.value.timelineClips.any { it.id == "fx_trans_tl_muse_test_01" && it.trackType == TimelineTrackType.EFFECT })

    vm.applyTransitionBetweenAllClips(TransitionEffect.ZOOM_IN, 0.5f)
    val layerClips = vm.uiState.value.timelineClips.filter {
      it.trackType == TimelineTrackType.PRODUCT || it.trackType == TimelineTrackType.TEXT
    }
    assertTrue(layerClips.isNotEmpty())
    assertTrue(layerClips.all { it.transitionIn == TransitionEffect.ZOOM_IN && it.transitionDurationSec == 0.5f })

    // Verify DirectorCommandResult supports transition directives
    val dirCmd = DirectorCommandResult(
      reply = "Added zoom transitions between your clips.",
      followUps = listOf("Make it faster"),
      transitionEffect = TransitionEffect.ZOOM_IN,
      transitionDurationSec = 0.45f,
      applyTransitionToAll = true
    )
    assertEquals(TransitionEffect.ZOOM_IN, dirCmd.transitionEffect)
    assertTrue(dirCmd.applyTransitionToAll)

    // Product Color Filter presets & intensity updates propagate to PRODUCT layers
    assertEquals(ColorFilterPreset.NONE, vm.uiState.value.activeProductFilter)
    vm.applyProductFilter(ColorFilterPreset.CINEMATIC, 0.85f)
    assertEquals(ColorFilterPreset.CINEMATIC, vm.uiState.value.activeProductFilter)
    assertEquals(0.85f, vm.uiState.value.productFilterIntensity, 0.01f)
    val gradedProdLayers = vm.uiState.value.layers.filter { it.type == LayerType.PRODUCT }
    assertTrue(gradedProdLayers.isNotEmpty())
    assertTrue(gradedProdLayers.all {
      it.colorFilter == ColorFilterPreset.CINEMATIC && kotlin.math.abs(it.filterIntensity - 0.85f) < 0.01f
    })

    vm.setProductFilterIntensity(0.45f)
    assertEquals(0.45f, vm.uiState.value.productFilterIntensity, 0.01f)
    assertTrue(vm.uiState.value.layers.filter { it.type == LayerType.PRODUCT }.all {
      kotlin.math.abs(it.filterIntensity - 0.45f) < 0.01f
    })

    vm.resetProductFilter()
    assertEquals(ColorFilterPreset.NONE, vm.uiState.value.activeProductFilter)
    assertTrue(vm.uiState.value.layers.filter { it.type == LayerType.PRODUCT }.all {
      it.colorFilter == ColorFilterPreset.NONE
    })
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

    // Placeholder / unset PAYMENT_BACKEND_URL & serverless provider keys are safely reported
    assertFalse(gateway.isBackendConfigured())
    assertFalse(gateway.hasProviderPublicKey(PaymentProvider.STRIPE))
    assertFalse(gateway.hasProviderPublicKey(PaymentProvider.FLUTTERWAVE))
    assertFalse(gateway.isStripePaymentLinkConfigured())
    assertFalse(gateway.isFlutterwaveDirectConfigured())
    assertFalse(gateway.isProviderReady(PaymentProvider.STRIPE))
    assertFalse(gateway.isProviderReady(PaymentProvider.FLUTTERWAVE))

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

    // Flutterwave redirect URI parsing with tx_ref fallback & completed status
    val flwTxRefUri = Uri.parse(
      "omnistudio://checkout/callback?status=completed&tx_ref=omni_pro_abc123456&transaction_id=987654&plan=PRO&cycle=MONTHLY&provider=FLUTTERWAVE"
    )
    val parsedFlw = gateway.parseDeepLinkCallback(flwTxRefUri)
    assertNotNull(parsedFlw)
    assertTrue(parsedFlw!!.isSuccessful)
    assertEquals("omni_pro_abc123456", parsedFlw.sessionId)
    assertEquals("omni_pro_abc123456", parsedFlw.transactionRef)
    assertEquals(PaymentProvider.FLUTTERWAVE, parsedFlw.provider)

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
    assertFalse(vm.isPaymentBackendConfigured())
    assertFalse(vm.isFlutterwaveDirectConfigured())
    assertFalse(vm.isStripePaymentLinkConfigured())

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

    // Inline Flutterwave WebView handler enforces authenticated Firebase session
    val sampleFlwSession = CheckoutSessionInit(
      sessionId = "omni_pro_test123",
      checkoutUrl = "https://checkout.flutterwave.com/v3.js",
      provider = PaymentProvider.FLUTTERWAVE,
      plan = SubscriptionPlan.PRO,
      billingCycle = BillingCycle.MONTHLY,
      amountUsd = 15.0,
      billingEmail = "enjebeln@gmail.com",
      launchMode = CheckoutLaunchMode.FLUTTERWAVE_INLINE_WEBVIEW,
      inlinePublicKey = "FLWPUBK_TEST-demo"
    )
    var inlineOk = true
    var inlineMsg: String? = null
    vm.handleFlutterwaveInlineSuccess(
      session = sampleFlwSession,
      status = "successful",
      transactionId = "1234567",
      txRef = "omni_pro_test123"
    ) { ok, msg ->
      inlineOk = ok
      inlineMsg = msg
    }
    assertFalse(inlineOk)
    assertNotNull(inlineMsg)
    assertEquals(null, vm.uiState.value.activeInlineWebViewSession)

    vm.dismissFlutterwaveInlineModal()
    assertEquals(null, vm.uiState.value.activeInlineWebViewSession)
    assertFalse(vm.uiState.value.isProcessingPayment)

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

    // Stripe PaymentSheet backend payload parsing (with customer + ephemeralKey + fallback key)
    val paymentSheetJson = JSONObject().apply {
      put("paymentIntent", "pi_3QxProTest_secret_987654")
      put("customer", "cus_test_omni_pro")
      put("ephemeralKey", "ek_test_omni_pro")
    }
    val parsedSheetRes = gateway.parseBackendSessionPayload(
      json = paymentSheetJson,
      provider = PaymentProvider.STRIPE,
      plan = SubscriptionPlan.PRO,
      billingCycle = BillingCycle.MONTHLY,
      amountUsd = 15.0,
      billingEmail = "enjebeln@gmail.com",
      customerName = "Creator Pro",
      fallbackPublishableKey = "pk_test_demo_key_123"
    )
    assertTrue(parsedSheetRes.isSuccess)
    val sheetSession = parsedSheetRes.getOrThrow()
    assertEquals(CheckoutLaunchMode.STRIPE_PAYMENT_SHEET, sheetSession.launchMode)
    assertEquals("pi_3QxProTest", sheetSession.sessionId)
    assertNotNull(sheetSession.stripePaymentSheet)
    assertEquals("pk_test_demo_key_123", sheetSession.stripePaymentSheet!!.publishableKey)
    assertEquals("pi_3QxProTest_secret_987654", sheetSession.stripePaymentSheet!!.paymentIntentClientSecret)
    assertEquals("cus_test_omni_pro", sheetSession.stripePaymentSheet!!.customerId)
    assertEquals("ek_test_omni_pro", sheetSession.stripePaymentSheet!!.ephemeralKeySecret)

    // PaymentSheet payload fails cleanly if no STRIPE_PUBLISHABLE_KEY is available
    val missingKeySheetRes = gateway.parseBackendSessionPayload(
      json = paymentSheetJson,
      provider = PaymentProvider.STRIPE,
      plan = SubscriptionPlan.PRO,
      billingCycle = BillingCycle.MONTHLY,
      amountUsd = 15.0,
      billingEmail = "enjebeln@gmail.com",
      customerName = "Creator Pro",
      fallbackPublishableKey = "YOUR_STRIPE_PUBLISHABLE_KEY"
    )
    assertTrue(missingKeySheetRes.isFailure)

    // Hosted Checkout URL payload still parses as EXTERNAL_BROWSER_URL
    val hostedJson = JSONObject().apply {
      put("sessionId", "cs_test_hosted_pro")
      put("checkoutUrl", "https://checkout.stripe.com/c/pay/cs_test_hosted_pro")
    }
    val hostedRes = gateway.parseBackendSessionPayload(
      json = hostedJson,
      provider = PaymentProvider.STRIPE,
      plan = SubscriptionPlan.PRO,
      billingCycle = BillingCycle.YEARLY,
      amountUsd = 144.0,
      billingEmail = "enjebeln@gmail.com",
      customerName = "Creator Pro"
    )
    assertTrue(hostedRes.isSuccess)
    assertEquals(CheckoutLaunchMode.EXTERNAL_BROWSER_URL, hostedRes.getOrThrow().launchMode)

    // Stripe PaymentSheet result handler behavior (cancel, fail, and unauthenticated complete)
    assertFalse(vm.isStripePublishableKeyConfigured())
    var stripeCancelOk = true
    vm.handleStripePaymentSheetResult(
      session = sheetSession,
      completed = false,
      cancelled = true,
      errorMessage = null
    ) { ok, _ -> stripeCancelOk = ok }
    assertFalse(stripeCancelOk)
    assertEquals(null, vm.uiState.value.activeStripePaymentSheetSession)
    assertNotNull(vm.uiState.value.paymentError)

    var stripeFailMsg: String? = null
    vm.handleStripePaymentSheetResult(
      session = sheetSession,
      completed = false,
      cancelled = false,
      errorMessage = "Your card was declined."
    ) { _, msg -> stripeFailMsg = msg }
    assertEquals("Your card was declined.", stripeFailMsg)
    assertEquals("Your card was declined.", vm.uiState.value.paymentError)

    var stripeAuthGateOk = true
    vm.handleStripePaymentSheetResult(
      session = sheetSession,
      completed = true,
      cancelled = false,
      errorMessage = null
    ) { ok, _ -> stripeAuthGateOk = ok }
    assertFalse(stripeAuthGateOk)
    assertFalse(vm.uiState.value.isProcessingPayment)
  }

  @Test
  fun studioTimelineTransitionIndicatorsAndSelectionMenu() {
    val clips = listOf(
      TimelineClip(
        id = "c_part_1",
        trackType = TimelineTrackType.PRODUCT,
        name = "Clip A",
        startTimeSec = 0f,
        durationSec = 5f
      ),
      TimelineClip(
        id = "c_part_2",
        trackType = TimelineTrackType.PRODUCT,
        name = "Clip B",
        startTimeSec = 5f,
        durationSec = 5f
      )
    )
    var appliedClipId: String? = null
    var appliedEffect: TransitionEffect? = null
    var appliedDuration = 0f

    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        StudioTimeline(
          clips = clips,
          selectedClipId = "c_part_2",
          playheadSec = 2.5f,
          totalDurationSec = 10f,
          isPlaying = false,
          onTogglePlay = {},
          onSeekPlayhead = {},
          onSelectClip = {},
          onApplyTransitionToClip = { id, effect, dur ->
            appliedClipId = id
            appliedEffect = effect
            appliedDuration = dur
          },
          onUpdateClip = { _, _, _ -> }
        )
      }
    }

    composeTestRule.waitForIdle()

    // Inter-clip transition junction indicator appears at the boundary of c_part_2
    composeTestRule.onNodeWithTag("transition_junction_c_part_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("transition_junction_c_part_2").performClick()
    composeTestRule.waitForIdle()

    // Menu opens with Fade, Slide, and Zoom selection options
    composeTestRule.onNodeWithTag("timeline_transition_menu").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timeline_menu_fade").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timeline_menu_slide").assertIsDisplayed()
    composeTestRule.onNodeWithTag("timeline_menu_zoom").assertIsDisplayed()

    // Selecting Fade applies FADE transition to c_part_2
    composeTestRule.onNodeWithTag("timeline_menu_fade").performClick()
    composeTestRule.waitForIdle()
    assertEquals("c_part_2", appliedClipId)
    assertEquals(TransitionEffect.FADE, appliedEffect)
    assertEquals(TransitionEffect.FADE.defaultDurationSec, appliedDuration, 0.01f)

    // Reopen via timeline toolbar Trans menu and select Slide & Zoom
    composeTestRule.onNodeWithTag("timeline_transitions_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("timeline_menu_slide").performClick()
    composeTestRule.waitForIdle()
    assertEquals(TransitionEffect.SLIDE_LEFT, appliedEffect)

    composeTestRule.onNodeWithTag("timeline_transitions_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("timeline_menu_zoom").performClick()
    composeTestRule.waitForIdle()
    assertEquals(TransitionEffect.ZOOM_IN, appliedEffect)
  }

  @Test
  fun colorGradingMatrixAndFiltersScreenInteractions() {
    // Verify ColorGradingUtils matrix generation under Android graphics runtime
    assertNull(ColorGradingUtils.toComposeColorFilter(ColorFilterPreset.NONE, 1.0f))
    assertNull(ColorGradingUtils.toAndroidColorFilter(ColorFilterPreset.CINEMATIC, 0.0f))
    assertNotNull(ColorGradingUtils.toComposeColorFilter(ColorFilterPreset.CINEMATIC, 1.0f))
    assertNotNull(ColorGradingUtils.toAndroidColorFilter(ColorFilterPreset.VINTAGE, 0.8f))

    val identity = ColorGradingUtils.buildColorMatrixArray(ColorFilterPreset.NONE, 1.0f)
    assertEquals(1.0f, identity[0], 0.001f)
    assertEquals(1.0f, identity[6], 0.001f)
    assertEquals(1.0f, identity[12], 0.001f)
    assertEquals(0.0f, identity[4], 0.001f)

    // Noir B&W collapses RGB rows equally (luma monochrome)
    val noir = ColorGradingUtils.buildColorMatrixArray(ColorFilterPreset.NOIR_MONO, 1.0f)
    assertEquals(noir[0], noir[5], 0.001f)
    assertEquals(noir[5], noir[10], 0.001f)

    // Vintage & Golden Hour apply warm red > blue bias
    val vintage = ColorGradingUtils.buildColorMatrixArray(ColorFilterPreset.VINTAGE, 1.0f)
    assertTrue(vintage[4] > vintage[14])

    // Render FiltersScreen and verify preset selection & reset actions
    var selectedPreset = ColorFilterPreset.NONE
    var resetCalled = false
    var previewStudioCalled = false
    val sampleLayers = listOf(
      CanvasLayer(
        id = "prod_layer",
        name = "Product",
        type = LayerType.PRODUCT,
        colorFilter = ColorFilterPreset.NONE
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        FiltersScreen(
          layers = sampleLayers,
          activeFilter = selectedPreset,
          intensity = 1.0f,
          onSelectFilter = { selectedPreset = it },
          onIntensityChanged = {},
          onResetFilter = { resetCalled = true },
          onPreviewInStudio = { previewStudioCalled = true }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("filters_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("active_css_filter_spec").assertIsDisplayed()

    // Select Cinematic, Vibrant, and Vintage presets via LazyColumn scroll
    composeTestRule.onNodeWithTag("filters_screen").performScrollToNode(hasTestTag("filter_preset_CINEMATIC"))
    composeTestRule.onNodeWithTag("filter_preset_CINEMATIC").performClick()
    composeTestRule.waitForIdle()
    assertEquals(ColorFilterPreset.CINEMATIC, selectedPreset)

    composeTestRule.onNodeWithTag("filters_screen").performScrollToNode(hasTestTag("filter_preset_VIBRANT"))
    composeTestRule.onNodeWithTag("filter_preset_VIBRANT").performClick()
    composeTestRule.waitForIdle()
    assertEquals(ColorFilterPreset.VIBRANT, selectedPreset)

    composeTestRule.onNodeWithTag("filters_screen").performScrollToNode(hasTestTag("filter_preset_VINTAGE"))
    composeTestRule.onNodeWithTag("filter_preset_VINTAGE").performClick()
    composeTestRule.waitForIdle()
    assertEquals(ColorFilterPreset.VINTAGE, selectedPreset)

    // Compare toggle & Reset / Preview actions work
    composeTestRule.onNodeWithTag("filters_screen").performScrollToNode(hasTestTag("filters_compare_toggle"))
    composeTestRule.onNodeWithTag("filters_compare_toggle").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("filters_screen").performScrollToNode(hasTestTag("filters_reset_btn"))
    composeTestRule.onNodeWithTag("filters_reset_btn").performClick()
    composeTestRule.waitForIdle()
    assertTrue(resetCalled)

    composeTestRule.onNodeWithTag("filters_apply_studio_btn").performClick()
    composeTestRule.waitForIdle()
    assertTrue(previewStudioCalled)
  }
}
