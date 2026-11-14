package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.OmniAiEngine
import com.example.audio.StudioAudioEngine
import com.example.data.model.FirestoreCampaign
import com.example.data.repository.CheckoutLaunchMode
import com.example.data.repository.CheckoutSessionInit
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.PaymentGatewayRepository
import com.example.data.repository.PaymentProvider
import com.example.model.AdFramework
import com.example.model.AdStudioRepository
import com.example.model.AdTemplate
import com.example.model.AiCategory
import com.example.model.AspectRatioOption
import com.example.model.AudioTrackOption
import com.example.model.BackgroundPreset
import com.example.model.BatchItem
import com.example.model.BatchItemStatus
import com.example.model.BgRemovalMode
import com.example.model.BgReplacementType
import com.example.model.BillingCycle
import com.example.model.CameraMovement
import com.example.model.CanvasLayer
import com.example.model.ChromaKeyColor
import com.example.model.DemoProduct
import com.example.model.DirectorMessage
import com.example.model.GeneratedAdCopy
import com.example.model.LayerType
import com.example.model.LightingSettings
import com.example.model.MotionTemplate
import com.example.model.MuseGenUiState
import com.example.model.MuseGeneratedClip
import com.example.model.SubscriptionPlan
import com.example.model.TextAnimationType
import com.example.model.TimelineClip
import com.example.model.TimelineTrackType
import com.example.model.UserProfile
import com.example.model.VideoBgRemovalState
import com.example.model.isPaidTier
import com.example.model.isUnlimitedAiTier
import com.example.studio.CutoutProcessor
import com.example.studio.VideoSegmentationEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OmniModule(val label: String) {
    STUDIO("Studio"),
    DIRECTOR("Director"),
    MUSE_AI("Muse AI"),
    BG_REMOVAL("Bg Removal"),
    FEED("Feed")
}

enum class StudioTab(val label: String) {
    TEMPLATES("Templates"),
    AI_COPY("AI Copy"),
    MOTION("Motion"),
    PHOTO_LIGHTING("Photo & Light"),
    LAYERS("Layers"),
    AUDIO_VOICE("Audio & Voice"),
    BATCH("Batch Ads"),
    HISTORY("History"),
    EXPORT("Export")
}

data class StudioUiState(
    val activeModule: OmniModule = OmniModule.STUDIO,
    val activeTab: StudioTab = StudioTab.TEMPLATES,
    val activeBottomSheetTab: StudioTab? = null,
    val aspectRatio: AspectRatioOption = AspectRatioOption.RATIO_9_16,
    val zoomLevel: Float = 1.0f,
    val selectedProduct: DemoProduct = AdStudioRepository.demoProducts[0],
    val bgPreset: BackgroundPreset = BackgroundPreset.CYBERPUNK_NEON,
    val customBgPrompt: String = "Cyberpunk neon spotlight with reflective wet asphalt floor",
    val motionTemplate: MotionTemplate = MotionTemplate.DYNAMIC_ZOOM_PUNCH,
    val motionSpeed: Float = 1.0f,
    val motionIntensity: Float = 1.0f,
    val lighting: LightingSettings = LightingSettings(),

    // Layers
    val layers: List<CanvasLayer> = emptyList(),
    val selectedLayerId: String? = null,

    // Timeline & Playback
    val isPlaying: Boolean = false,
    val playheadSec: Float = 0f,
    val totalDurationSec: Float = 10f,
    val timelineClips: List<TimelineClip> = emptyList(),
    val selectedClipId: String? = null,

    // Audio & Voice
    val selectedAudioTrack: AudioTrackOption = AudioTrackOption.TECH_FUTURE,
    val isMusicPlaying: Boolean = false,
    val bgmVolume: Float = 0.7f,
    val voicePitch: Float = 1.0f,
    val voiceSpeed: Float = 1.0f,

    // AI Copy
    val aiCategory: AiCategory = AiCategory.ECOMMERCE,
    val generatedCopy: GeneratedAdCopy = AdStudioRepository.generateCopyForProduct("Chronos Royal Diamond", AiCategory.ECOMMERCE),

    // Batch Creator
    val isBatchRunning: Boolean = false,
    val batchItems: List<BatchItem> = emptyList(),

    // History & Cloud Persistence
    val campaignHistory: List<FirestoreCampaign> = emptyList(),
    val isHistoryLoading: Boolean = false,

    // Director Chat (Conversational AI Editor)
    val directorMessages: List<DirectorMessage> = listOf(
        DirectorMessage(
            id = "welcome_director",
            isUser = false,
            text = "I'm your OmniStudio AI Director. Attach a photo or video frame and tell me how to transform this edit — I can adjust pacing, split clips, trim dead air, swap motion presets, and rewrite hooks directly on your timeline.",
            followUpSuggestions = listOf(
                "Make this punchier for TikTok",
                "Cut out dead air",
                "Add a suspenseful transition here"
            )
        )
    ),
    val isDirectorThinking: Boolean = false,

    // Muse AI Video Generator
    val museGenState: MuseGenUiState = MuseGenUiState.Idle,
    val museGeneratedClips: List<MuseGeneratedClip> = emptyList(),
    val aiCreditsRemaining: Int = 5,

    // Tiered Subscription & Payment Checkout Routing ("FREE", "BASIC", "PRO", "PREMIUM")
    val subscriptionTier: String = "FREE",
    val billingCycle: String = "MONTHLY",
    val showProUpgradeModal: Boolean = false,
    val paywallReason: String? = null,
    val isProcessingPayment: Boolean = false,
    val paymentError: String? = null,
    val selectedPaymentProvider: PaymentProvider = PaymentProvider.STRIPE,
    val activeCheckoutSession: CheckoutSessionInit? = null,
    val activeInlineWebViewSession: CheckoutSessionInit? = null,
    val checkoutStatusBanner: String? = null,

    // Video Background Removal Studio
    val videoBgState: VideoBgRemovalState = VideoBgRemovalState(),

    // Ad Framework & User Profile
    val adFramework: AdFramework = AdFramework.VIRAL_HOOK,
    val user: UserProfile = UserProfile(),

    // Theme Mode (true = Neutral Dark #0B0F19, false = Clean Light #F8FAFC)
    val isDarkTheme: Boolean = true,

    // Undo / Redo
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val audioEngine = StudioAudioEngine(application)
    val cutoutProcessor = CutoutProcessor()
    private val omniAiEngine by lazy { OmniAiEngine() }
    private val firestoreRepo by lazy { FirestoreRepository.create(getApplication()) }
    private val paymentGatewayRepo by lazy { PaymentGatewayRepository() }

    private val undoStack = mutableListOf<List<CanvasLayer>>()
    private val redoStack = mutableListOf<List<CanvasLayer>>()

    private var playbackJob: Job? = null
    private var campaignsJob: Job? = null
    private var profileObserverJob: Job? = null

    private val prefs = application.getSharedPreferences("omnistudio_prefs", Context.MODE_PRIVATE)

    init {
        val savedDark = prefs.getBoolean("is_dark_theme", true)
        val today = FirestoreRepository.todayDateKey()
        val savedDate = prefs.getString("last_credit_reset_date", "") ?: ""
        val initialCredits = if (savedDate == today) {
            prefs.getInt("daily_credits_remaining", SubscriptionPlan.FREE.dailyAiCredits)
        } else {
            SubscriptionPlan.FREE.dailyAiCredits
        }
        _uiState.update { state ->
            state.copy(
                isDarkTheme = savedDark,
                aiCreditsRemaining = initialCredits,
                user = state.user.copy(
                    aiCreditsRemaining = initialCredits,
                    lastCreditResetDate = savedDate
                )
            )
        }
        applyTemplate(AdStudioRepository.templates[0])
        initBatchItems()
    }

    private fun initBatchItems() {
        _uiState.update { state ->
            state.copy(
                batchItems = AdStudioRepository.demoProducts.map { prod ->
                    BatchItem(product = prod, status = BatchItemStatus.WAITING)
                }
            )
        }
    }

    private fun saveUndoState() {
        val currentLayers = _uiState.value.layers
        if (undoStack.size >= 15) undoStack.removeAt(0)
        undoStack.add(currentLayers)
        redoStack.clear()
        _uiState.update { it.copy(canUndo = true, canRedo = false) }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val last = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_uiState.value.layers)
            _uiState.update {
                it.copy(
                    layers = last,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = true
                )
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_uiState.value.layers)
            _uiState.update {
                it.copy(
                    layers = next,
                    canUndo = true,
                    canRedo = redoStack.isNotEmpty()
                )
            }
        }
    }

    fun setActiveModule(module: OmniModule) {
        _uiState.update { it.copy(activeModule = module, activeBottomSheetTab = null) }
    }

    fun openToolSheet(tab: StudioTab) {
        _uiState.update { it.copy(activeTab = tab, activeBottomSheetTab = tab) }
    }

    fun dismissToolSheet() {
        _uiState.update { it.copy(activeBottomSheetTab = null) }
    }

    fun openProUpgradeModal(reason: String? = null) {
        _uiState.update {
            it.copy(
                showProUpgradeModal = true,
                paywallReason = reason,
                paymentError = null
            )
        }
    }

    fun dismissProUpgradeModal() {
        _uiState.update {
            it.copy(
                showProUpgradeModal = false,
                paywallReason = null,
                paymentError = null,
                isProcessingPayment = false
            )
        }
    }

    fun toggleTheme() {
        setDarkTheme(!_uiState.value.isDarkTheme)
    }

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
        val themeTag = if (isDark) "DARK" else "LIGHT"
        _uiState.update {
            it.copy(
                isDarkTheme = isDark,
                user = it.user.copy(themePreference = themeTag)
            )
        }
        val uid = try { Firebase.auth.currentUser?.uid } catch (_: Exception) { null }
        if (uid != null) {
            viewModelScope.launch {
                firestoreRepo.updateThemePreference(uid, isDark)
            }
        }
    }

    fun setActiveTab(tab: StudioTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setAspectRatio(ratio: AspectRatioOption) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }

    fun setZoomLevel(zoom: Float) {
        _uiState.update { it.copy(zoomLevel = zoom.coerceIn(0.5f, 2.0f)) }
    }

    fun selectProduct(product: DemoProduct) {
        saveUndoState()
        _uiState.update { state ->
            val updatedLayers = state.layers.map { layer ->
                if (layer.type == LayerType.PRODUCT) {
                    layer.copy(
                        name = product.name,
                        productDrawableRes = product.drawableRes,
                        customImageUri = null,
                        aiBitmap = null
                    )
                } else layer
            }
            state.copy(
                selectedProduct = product,
                layers = updatedLayers,
                generatedCopy = AdStudioRepository.generateCopyForProduct(product.name, state.aiCategory)
            )
        }
    }

    fun applyTemplate(template: AdTemplate) {
        saveUndoState()
        val defaultLayers = listOf(
            CanvasLayer(
                id = "prod_layer",
                name = _uiState.value.selectedProduct.name,
                type = LayerType.PRODUCT,
                x = 0.5f,
                y = 0.48f,
                scale = 1.0f,
                zIndex = 1,
                productDrawableRes = _uiState.value.selectedProduct.drawableRes
            ),
            CanvasLayer(
                id = "headline_layer",
                name = "Headline",
                type = LayerType.TEXT,
                x = 0.5f,
                y = 0.18f,
                scale = 1.0f,
                text = template.defaultHeadline,
                fontSizeSp = 28f,
                textColor = 0xFFFFFFFF,
                isGradientText = true,
                gradientColorStart = 0xFF06B6D4,
                gradientColorEnd = 0xFF8B5CF6,
                isNeonGlow = true,
                textAnimation = TextAnimationType.ZOOM_PUNCH,
                zIndex = 2
            ),
            CanvasLayer(
                id = "offer_layer",
                name = "Offer Badge",
                type = LayerType.BADGE,
                x = 0.5f,
                y = 0.74f,
                scale = 1.0f,
                badgeLabel = template.defaultOffer,
                badgeAccentColor = 0xFFEC4899,
                zIndex = 3
            ),
            CanvasLayer(
                id = "cta_layer",
                name = "CTA Button",
                type = LayerType.TEXT,
                x = 0.5f,
                y = 0.85f,
                scale = 0.9f,
                text = template.defaultCta,
                fontSizeSp = 18f,
                textColor = 0xFF06B6D4,
                isOutline = true,
                zIndex = 4
            )
        )

        val clips = listOf(
            TimelineClip("c_prod", TimelineTrackType.PRODUCT, "Main Visual Track", 0f, 10f),
            TimelineClip("c_text", TimelineTrackType.TEXT, "Hook & CTA Overlay", 0.5f, 9.5f),
            TimelineClip("c_audio", TimelineTrackType.AUDIO, "BGM Synth Beat", 0f, 10f),
            TimelineClip("c_fx", TimelineTrackType.EFFECT, template.motionPreset.title, 0f, 10f)
        )

        _uiState.update { state ->
            state.copy(
                aspectRatio = template.defaultRatio,
                motionTemplate = template.motionPreset,
                bgPreset = template.bgPreset,
                layers = defaultLayers,
                selectedLayerId = "prod_layer",
                timelineClips = clips,
                selectedClipId = "c_prod",
                playheadSec = 0f,
                totalDurationSec = 10f
            )
        }
    }

    fun setAdFramework(framework: AdFramework) {
        saveUndoState()
        _uiState.update { state ->
            val updatedLayers = state.layers.map { layer ->
                when (layer.id) {
                    "headline_layer" -> layer.copy(text = framework.hook)
                    "offer_layer" -> layer.copy(badgeLabel = framework.cta)
                    "cta_layer" -> layer.copy(text = framework.cta)
                    else -> layer
                }
            }
            state.copy(
                adFramework = framework,
                motionTemplate = framework.motion,
                layers = updatedLayers
            )
        }
    }

    fun setBackgroundPreset(preset: BackgroundPreset) {
        _uiState.update { it.copy(bgPreset = preset) }
    }

    fun setCustomBgPrompt(prompt: String) {
        _uiState.update { it.copy(customBgPrompt = prompt, bgPreset = BackgroundPreset.CUSTOM_PROMPT) }
    }

    fun setMotionTemplate(motion: MotionTemplate) {
        _uiState.update { state ->
            val updatedClips = state.timelineClips.map {
                if (it.trackType == TimelineTrackType.EFFECT) it.copy(name = motion.title) else it
            }
            state.copy(motionTemplate = motion, timelineClips = updatedClips)
        }
    }

    fun setMotionSpeed(speed: Float) {
        _uiState.update { it.copy(motionSpeed = speed.coerceIn(0.5f, 2.5f)) }
    }

    fun setMotionIntensity(intensity: Float) {
        _uiState.update { it.copy(motionIntensity = intensity.coerceIn(0.2f, 2.0f)) }
    }

    fun updateLighting(updater: (LightingSettings) -> LightingSettings) {
        _uiState.update { it.copy(lighting = updater(it.lighting)) }
    }

    // Layer Management
    fun selectLayer(id: String?) {
        _uiState.update { it.copy(selectedLayerId = id) }
    }

    fun updateLayer(id: String, updater: (CanvasLayer) -> CanvasLayer) {
        _uiState.update { state ->
            val updated = state.layers.map { if (it.id == id) updater(it) else it }
            state.copy(layers = updated)
        }
    }

    fun finishLayerTransform() {
        saveUndoState()
    }

    fun duplicateLayer(id: String) {
        saveUndoState()
        _uiState.update { state ->
            val layer = state.layers.find { it.id == id } ?: return@update state
            val newLayer = layer.copy(
                id = "layer_${System.currentTimeMillis()}",
                name = "${layer.name} Copy",
                x = (layer.x + 0.05f).coerceIn(0.1f, 0.9f),
                y = (layer.y + 0.05f).coerceIn(0.1f, 0.9f),
                zIndex = state.layers.size + 1
            )
            state.copy(layers = state.layers + newLayer, selectedLayerId = newLayer.id)
        }
    }

    fun deleteLayer(id: String) {
        saveUndoState()
        _uiState.update { state ->
            state.copy(
                layers = state.layers.filter { it.id != id },
                selectedLayerId = if (state.selectedLayerId == id) null else state.selectedLayerId
            )
        }
    }

    fun bringLayerForward(id: String) {
        saveUndoState()
        _uiState.update { state ->
            val layers = state.layers.toMutableList()
            val index = layers.indexOfFirst { it.id == id }
            if (index in 0 until layers.size - 1) {
                val temp = layers[index]
                layers[index] = layers[index + 1]
                layers[index + 1] = temp
                layers.forEachIndexed { i, l -> layers[i] = l.copy(zIndex = i) }
            }
            state.copy(layers = layers)
        }
    }

    fun sendLayerBackward(id: String) {
        saveUndoState()
        _uiState.update { state ->
            val layers = state.layers.toMutableList()
            val index = layers.indexOfFirst { it.id == id }
            if (index > 0) {
                val temp = layers[index]
                layers[index] = layers[index - 1]
                layers[index - 1] = temp
                layers.forEachIndexed { i, l -> layers[i] = l.copy(zIndex = i) }
            }
            state.copy(layers = layers)
        }
    }

    fun addTextLayer(text: String, isHeadline: Boolean = false) {
        saveUndoState()
        val newLayer = CanvasLayer(
            id = "text_${System.currentTimeMillis()}",
            name = if (isHeadline) "Headline" else "Subhead",
            type = LayerType.TEXT,
            x = 0.5f,
            y = if (isHeadline) 0.22f else 0.76f,
            text = text,
            fontSizeSp = if (isHeadline) 26f else 18f,
            textColor = 0xFFFFFFFF,
            isGradientText = isHeadline,
            isNeonGlow = isHeadline,
            zIndex = _uiState.value.layers.size + 1
        )
        _uiState.update { it.copy(layers = it.layers + newLayer, selectedLayerId = newLayer.id) }
    }

    fun addBadgeLayer(badgeText: String, color: Long = 0xFFEC4899) {
        saveUndoState()
        val newLayer = CanvasLayer(
            id = "badge_${System.currentTimeMillis()}",
            name = "Badge: $badgeText",
            type = LayerType.BADGE,
            x = 0.5f,
            y = 0.72f,
            badgeLabel = badgeText,
            badgeAccentColor = color,
            zIndex = _uiState.value.layers.size + 1
        )
        _uiState.update { it.copy(layers = it.layers + newLayer, selectedLayerId = newLayer.id) }
    }

    // Hybrid CapCut-Style Timeline Controls
    fun selectClip(clipId: String?) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        _uiState.update { it.copy(isPlaying = true) }
        audioEngine.playMusic(_uiState.value.selectedAudioTrack)

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val stepSec = 0.033f
            while (_uiState.value.isPlaying) {
                delay(33)
                _uiState.update { state ->
                    val next = state.playheadSec + (stepSec * state.motionSpeed)
                    if (next >= state.totalDurationSec) {
                        state.copy(playheadSec = 0f)
                    } else {
                        state.copy(playheadSec = next)
                    }
                }
            }
        }
    }

    fun pausePlayback() {
        _uiState.update { it.copy(isPlaying = false) }
        playbackJob?.cancel()
        playbackJob = null
        audioEngine.stopMusic()
    }

    fun seekPlayhead(sec: Float) {
        _uiState.update { it.copy(playheadSec = sec.coerceIn(0f, it.totalDurationSec)) }
    }

    fun updateClip(clipId: String, startTime: Float, duration: Float) {
        _uiState.update { state ->
            val updated = state.timelineClips.map {
                if (it.id == clipId) it.copy(
                    startTimeSec = startTime.coerceAtLeast(0f),
                    durationSec = duration.coerceIn(1f, (state.totalDurationSec - startTime).coerceAtLeast(1f))
                ) else it
            }
            state.copy(timelineClips = updated)
        }
    }

    fun splitClipAtPlayhead(clipId: String? = _uiState.value.selectedClipId) {
        _uiState.update { state ->
            val targetId = clipId ?: state.timelineClips.firstOrNull()?.id ?: return@update state
            val target = state.timelineClips.find { it.id == targetId } ?: return@update state
            val splitPoint = state.playheadSec
            if (splitPoint <= target.startTimeSec + 0.4f || splitPoint >= target.startTimeSec + target.durationSec - 0.4f) {
                // If playhead is near edges, split down the middle for instant usability
                val mid = target.startTimeSec + (target.durationSec / 2f)
                val firstPart = target.copy(durationSec = (mid - target.startTimeSec).coerceAtLeast(0.5f))
                val secondPart = target.copy(
                    id = "${target.id}_split_${System.currentTimeMillis()}",
                    name = "${target.name} (B)",
                    startTimeSec = mid,
                    durationSec = (target.startTimeSec + target.durationSec - mid).coerceAtLeast(0.5f)
                )
                val nextClips = state.timelineClips.flatMap { if (it.id == target.id) listOf(firstPart, secondPart) else listOf(it) }
                return@update state.copy(timelineClips = nextClips, selectedClipId = secondPart.id)
            }
            val firstPart = target.copy(durationSec = splitPoint - target.startTimeSec)
            val secondPart = target.copy(
                id = "${target.id}_split_${System.currentTimeMillis()}",
                name = "${target.name} (B)",
                startTimeSec = splitPoint,
                durationSec = (target.startTimeSec + target.durationSec) - splitPoint
            )
            val nextClips = state.timelineClips.flatMap { if (it.id == target.id) listOf(firstPart, secondPart) else listOf(it) }
            state.copy(timelineClips = nextClips, selectedClipId = secondPart.id)
        }
    }

    fun trimSelectedClip(deltaStart: Float = 0f, deltaDuration: Float = -0.5f) {
        _uiState.update { state ->
            val targetId = state.selectedClipId ?: state.timelineClips.firstOrNull()?.id ?: return@update state
            val updated = state.timelineClips.map { clip ->
                if (clip.id == targetId) {
                    val newStart = (clip.startTimeSec + deltaStart).coerceIn(0f, state.totalDurationSec - 1f)
                    val newDur = (clip.durationSec + deltaDuration).coerceIn(1f, state.totalDurationSec - newStart)
                    clip.copy(startTimeSec = newStart, durationSec = newDur)
                } else clip
            }
            state.copy(timelineClips = updated)
        }
    }

    // One-Tap Photo-to-Ad Converter
    fun oneTapPhotoToAd(uri: String, name: String = "Product Highlight") {
        saveUndoState()
        cutoutProcessor.performAiAutoCutout()
        val prod = DemoProduct(
            id = "quick_ad_${System.currentTimeMillis()}",
            name = name,
            price = "$79.00",
            category = "E-Commerce",
            description = "1-Tap Photo-to-Ad Creation",
            drawableRes = R.drawable.prod_watch,
            defaultBadge = "FLASH DEAL • 40% OFF"
        )
        _uiState.update { state ->
            val updatedLayers = state.layers.map { layer ->
                when (layer.type) {
                    LayerType.PRODUCT -> layer.copy(
                        name = name,
                        customImageUri = uri,
                        aiBitmap = null,
                        isCutoutApplied = true
                    )
                    else -> when (layer.id) {
                        "headline_layer" -> layer.copy(text = "STOP SCROLLING! MEET YOUR NEW OBSESSION")
                        "offer_layer" -> layer.copy(badgeLabel = "40% OFF • LIMITED DROP")
                        "cta_layer" -> layer.copy(text = "TAP TO SHOP NOW")
                        else -> layer
                    }
                }
            }
            state.copy(
                selectedProduct = prod,
                aspectRatio = AspectRatioOption.RATIO_9_16,
                motionTemplate = MotionTemplate.DYNAMIC_ZOOM_PUNCH,
                motionSpeed = 1.4f,
                motionIntensity = 1.3f,
                bgPreset = BackgroundPreset.CYBERPUNK_NEON,
                layers = updatedLayers,
                activeModule = OmniModule.STUDIO,
                activeBottomSheetTab = null
            )
        }
    }

    // Audio & Voice
    fun selectAudioTrack(track: AudioTrackOption) {
        _uiState.update { it.copy(selectedAudioTrack = track) }
        if (_uiState.value.isPlaying) {
            audioEngine.playMusic(track)
        }
    }

    fun setBgmVolume(volume: Float) {
        _uiState.update { it.copy(bgmVolume = volume) }
        audioEngine.bgmVolume = volume
    }

    fun setVoicePitch(pitch: Float) {
        _uiState.update { it.copy(voicePitch = pitch) }
        audioEngine.voicePitch = pitch
    }

    fun setVoiceSpeed(speed: Float) {
        _uiState.update { it.copy(voiceSpeed = speed) }
        audioEngine.voiceSpeed = speed
    }

    fun speakScript(script: String) {
        audioEngine.speak(script)
    }

    fun stopVoiceover() {
        audioEngine.stopSpeaking()
    }

    // AI Copy Category
    fun setAiCategory(category: AiCategory) {
        _uiState.update { state ->
            state.copy(
                aiCategory = category,
                generatedCopy = AdStudioRepository.generateCopyForProduct(state.selectedProduct.name, category)
            )
        }
    }

    // Batch Creator
    fun startBatchGeneration() {
        _uiState.update { it.copy(isBatchRunning = true) }
        viewModelScope.launch {
            val items = _uiState.value.batchItems.toMutableList()
            for (i in items.indices) {
                items[i] = items[i].copy(status = BatchItemStatus.REMOVING_BG, progress = 0.25f)
                _uiState.update { it.copy(batchItems = items.toList()) }
                delay(500)

                items[i] = items[i].copy(status = BatchItemStatus.APPLYING_TEMPLATE, progress = 0.55f)
                _uiState.update { it.copy(batchItems = items.toList()) }
                delay(500)

                items[i] = items[i].copy(status = BatchItemStatus.COMPOSITING_CTA, progress = 0.85f)
                _uiState.update { it.copy(batchItems = items.toList()) }
                delay(400)

                items[i] = items[i].copy(
                    status = BatchItemStatus.COMPLETED,
                    progress = 1.0f,
                    resultPreviewName = "OmniStudio_Ad_${i + 1}.mp4"
                )
                _uiState.update { it.copy(batchItems = items.toList()) }
                delay(250)
            }
            _uiState.update { it.copy(isBatchRunning = false) }
        }
    }

    private fun checkAndResetDailyCredits(): Boolean {
        val today = FirestoreRepository.todayDateKey()
        val currentState = _uiState.value
        val plan = SubscriptionPlan.fromTierId(currentState.subscriptionTier)
        val savedDate = currentState.user.lastCreditResetDate.ifBlank {
            prefs.getString("last_credit_reset_date", "") ?: ""
        }

        if (savedDate != today) {
            val freshCredits = plan.dailyAiCredits
            prefs.edit()
                .putString("last_credit_reset_date", today)
                .putInt("daily_credits_remaining", freshCredits)
                .apply()
            _uiState.update { s ->
                s.copy(
                    aiCreditsRemaining = freshCredits,
                    user = s.user.copy(
                        aiCreditsRemaining = freshCredits,
                        lastCreditResetDate = today
                    )
                )
            }
            val uid = try { Firebase.auth.currentUser?.uid } catch (_: Exception) { null }
            if (uid != null) {
                viewModelScope.launch { firestoreRepo.updateDailyCredits(uid, freshCredits, today) }
            }
            return true
        }
        return true
    }

    private fun deductSingleAiCredit() {
        val currentState = _uiState.value
        if (isUnlimitedAiTier(currentState.subscriptionTier)) return
        val today = FirestoreRepository.todayDateKey()
        val nextCredits = (currentState.aiCreditsRemaining - 1).coerceAtLeast(0)
        prefs.edit()
            .putString("last_credit_reset_date", today)
            .putInt("daily_credits_remaining", nextCredits)
            .apply()
        _uiState.update { s ->
            s.copy(
                aiCreditsRemaining = nextCredits,
                user = s.user.copy(
                    aiCreditsRemaining = nextCredits,
                    lastCreditResetDate = today
                )
            )
        }
        val uid = try { Firebase.auth.currentUser?.uid } catch (_: Exception) { null }
        if (uid != null) {
            viewModelScope.launch {
                firestoreRepo.updateDailyCredits(uid, nextCredits, today)
            }
        }
    }

    // Director Chat (Conversational AI Editor via Firebase AI Logic)
    fun sendDirectorPrompt(
        prompt: String,
        attachedMediaUri: String? = null,
        attachedMediaName: String? = null
    ) {
        if (prompt.isBlank()) return
        checkAndResetDailyCredits()
        val preState = _uiState.value
        if (!isUnlimitedAiTier(preState.subscriptionTier) && preState.aiCreditsRemaining <= 0) {
            openProUpgradeModal("Daily AI credits exhausted for Director Chat. Upgrade to Basic ($5/mo), Pro ($15/mo), or Premium ($20/mo).")
            return
        }

        val userMsg = DirectorMessage(
            id = "u_${System.currentTimeMillis()}",
            isUser = true,
            text = prompt,
            attachedMediaUri = attachedMediaUri,
            attachedMediaName = attachedMediaName
        )
        _uiState.update { state ->
            state.copy(
                directorMessages = state.directorMessages + userMsg,
                isDirectorThinking = true
            )
        }

        viewModelScope.launch {
            val stateSnapshot = _uiState.value
            val attachedBitmap = attachedMediaUri?.let { decodeScaledBitmap(it) }
            val summary = "Product=${stateSnapshot.selectedProduct.name}, Ratio=${stateSnapshot.aspectRatio.label}, Motion=${stateSnapshot.motionTemplate.title}, Speed=${stateSnapshot.motionSpeed}x, Clips=${stateSnapshot.timelineClips.size}, Duration=${stateSnapshot.totalDurationSec}s, Playhead=${stateSnapshot.playheadSec}s"

            val result = omniAiEngine.runDirectorChat(
                userPrompt = prompt,
                currentContextSummary = summary,
                attachedBitmap = attachedBitmap,
                isProTier = isPaidTier(stateSnapshot.subscriptionTier)
            )

            result.fold(
                onSuccess = { cmd ->
                    val executedActions = mutableListOf<String>()
                    saveUndoState()

                    _uiState.update { current ->
                        var nextRatio = current.aspectRatio
                        var nextMotion = current.motionTemplate
                        var nextSpeed = current.motionSpeed
                        var nextIntensity = current.motionIntensity
                        var nextBg = current.bgPreset
                        var nextAudio = current.selectedAudioTrack
                        var nextDuration = current.totalDurationSec
                        var nextClips = current.timelineClips.toMutableList()
                        var nextLayers = current.layers.toMutableList()

                        cmd.newAspectRatio?.let {
                            nextRatio = it
                            executedActions.add("Format → ${it.label}")
                        }
                        cmd.motionPreset?.let {
                            nextMotion = it
                            executedActions.add("Motion → ${it.title}")
                        }
                        cmd.motionSpeed?.let {
                            nextSpeed = it
                            executedActions.add("Speed → ${String.format("%.1fx", it)}")
                        }
                        cmd.motionIntensity?.let {
                            nextIntensity = it
                            executedActions.add("Intensity → ${(it * 50).toInt()}%")
                        }
                        cmd.newBgPreset?.let {
                            nextBg = it
                            executedActions.add("Scene → ${it.title}")
                        }
                        cmd.newAudioTrack?.let {
                            nextAudio = it
                            executedActions.add("Audio → ${it.title}")
                        }
                        cmd.newHeadline?.let { hl ->
                            nextLayers = nextLayers.map { l ->
                                if (l.id == "headline_layer") l.copy(text = hl) else l
                            }.toMutableList()
                            executedActions.add("Hook Updated")
                        }
                        cmd.newCta?.let { cta ->
                            nextLayers = nextLayers.map { l ->
                                when (l.id) {
                                    "cta_layer" -> l.copy(text = cta)
                                    "offer_layer" -> l.copy(badgeLabel = cta)
                                    else -> l
                                }
                            }.toMutableList()
                            executedActions.add("CTA Updated")
                        }
                        if (attachedMediaUri != null) {
                            nextLayers = nextLayers.map { l ->
                                if (l.type == LayerType.PRODUCT) l.copy(customImageUri = attachedMediaUri, aiBitmap = null) else l
                            }.toMutableList()
                            executedActions.add("Media Loaded")
                        }
                        if (cmd.trimDeadAir) {
                            nextDuration = (nextDuration * 0.8f).coerceAtLeast(5.0f)
                            nextClips = nextClips.map { c ->
                                val trimmedDur = (c.durationSec * 0.78f).coerceIn(1.5f, nextDuration)
                                c.copy(startTimeSec = c.startTimeSec.coerceAtMost(nextDuration - trimmedDur), durationSec = trimmedDur)
                            }.toMutableList()
                            executedActions.add("Trimmed Dead Air (-20%)")
                        }
                        if (cmd.addSuspenseTransition) {
                            nextClips.add(
                                TimelineClip(
                                    id = "fx_suspense_${System.currentTimeMillis()}",
                                    trackType = TimelineTrackType.EFFECT,
                                    name = "Suspense Drop FX",
                                    startTimeSec = current.playheadSec.coerceIn(0f, nextDuration - 1.5f),
                                    durationSec = 1.5f,
                                    isAiGenerated = true
                                )
                            )
                            executedActions.add("Added Suspense Transition")
                        }
                        if (cmd.splitAtPlayhead && nextClips.isNotEmpty()) {
                            val first = nextClips.first()
                            val mid = first.startTimeSec + (first.durationSec / 2f)
                            nextClips[0] = first.copy(durationSec = mid - first.startTimeSec)
                            nextClips.add(1, first.copy(id = "${first.id}_cut", name = "${first.name} Cut", startTimeSec = mid, durationSec = first.durationSec / 2f))
                            executedActions.add("Split Clip at Playhead")
                        }

                        val aiMsg = DirectorMessage(
                            id = "ai_${System.currentTimeMillis()}",
                            isUser = false,
                            text = cmd.reply,
                            executedActions = executedActions,
                            followUpSuggestions = cmd.followUps
                        )

                        current.copy(
                            aspectRatio = nextRatio,
                            motionTemplate = nextMotion,
                            motionSpeed = nextSpeed,
                            motionIntensity = nextIntensity,
                            bgPreset = nextBg,
                            selectedAudioTrack = nextAudio,
                            totalDurationSec = nextDuration,
                            timelineClips = nextClips,
                            layers = nextLayers,
                            directorMessages = current.directorMessages + aiMsg,
                            isDirectorThinking = false
                        )
                    }
                    deductSingleAiCredit()
                },
                onFailure = { error ->
                    val errMsg = DirectorMessage(
                        id = "err_${System.currentTimeMillis()}",
                        isUser = false,
                        text = error.localizedMessage ?: "Director AI encountered an error. Check Firebase AI Logic / App Check setup.",
                        isError = true
                    )
                    _uiState.update { state ->
                        state.copy(
                            directorMessages = state.directorMessages + errMsg,
                            isDirectorThinking = false
                        )
                    }
                }
            )
        }
    }

    // Muse Engine (AI Video Generator via Firebase AI Logic)
    fun generateMuseVideos(
        prompt: String,
        cameraMovement: CameraMovement,
        panAmount: Float = cameraMovement.defaultPan,
        zoomFactor: Float = cameraMovement.defaultZoom,
        tiltAngle: Float = cameraMovement.defaultTilt,
        aspectRatio: AspectRatioOption,
        audioCue: String,
        batchCount: Int,
        referenceUri: String? = null
    ) {
        checkAndResetDailyCredits()
        val state = _uiState.value
        val isUnlimited = isUnlimitedAiTier(state.subscriptionTier)
        if (!isUnlimited && state.aiCreditsRemaining <= 0) {
            openProUpgradeModal("Daily AI credits exhausted for Muse Video Engine. Choose Basic ($5/mo), Pro ($15/mo), or Premium ($20/mo).")
            return
        }
        if (prompt.isBlank()) {
            _uiState.update { it.copy(museGenState = MuseGenUiState.Error("Please enter a visual prompt for Muse Engine.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(museGenState = MuseGenUiState.Loading("Muse Engine: Directing shots & rendering AI keyframes..."))
            }

            val refBitmap = referenceUri?.let { decodeScaledBitmap(it) }
            val result = omniAiEngine.generateMuseVideoClips(
                prompt = prompt,
                cameraMovement = cameraMovement,
                panAmount = panAmount,
                zoomFactor = zoomFactor,
                tiltAngle = tiltAngle,
                aspectRatio = aspectRatio,
                audioCue = audioCue,
                batchCount = batchCount,
                referenceBitmap = refBitmap,
                referenceUri = referenceUri
            )

            result.fold(
                onSuccess = { clips ->
                    _uiState.update { current ->
                        current.copy(
                            museGenState = MuseGenUiState.Success(clips),
                            museGeneratedClips = clips + current.museGeneratedClips
                        )
                    }
                    deductSingleAiCredit()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(museGenState = MuseGenUiState.Error(error.localizedMessage ?: "Muse AI generation failed."))
                    }
                }
            )
        }
    }

    fun importMuseClipToTimeline(clip: MuseGeneratedClip) {
        saveUndoState()
        _uiState.update { state ->
            val updatedLayers = state.layers.map { layer ->
                when (layer.type) {
                    LayerType.PRODUCT -> layer.copy(
                        name = clip.title,
                        aiBitmap = clip.keyframeBitmap,
                        customImageUri = clip.referenceUri,
                        scale = clip.zoomFactor.coerceIn(0.6f, 1.8f),
                        x = (0.5f + clip.panAmount * 0.15f).coerceIn(0.2f, 0.8f),
                        y = (0.5f - clip.tiltAngle * 0.15f).coerceIn(0.2f, 0.8f)
                    )
                    else -> when (layer.id) {
                        "headline_layer" -> layer.copy(text = clip.suggestedHook)
                        else -> layer
                    }
                }
            }

            val insertStart = state.playheadSec.coerceAtLeast(0f)
            val nextTotalDuration = maxOf(state.totalDurationSec, insertStart + clip.durationSec)

            val newVideoClip = TimelineClip(
                id = "tl_${clip.id}",
                trackType = TimelineTrackType.PRODUCT,
                name = clip.title,
                startTimeSec = insertStart,
                durationSec = clip.durationSec,
                isAiGenerated = true,
                audioCue = clip.audioCue
            )

            val newFxClip = TimelineClip(
                id = "fx_${clip.id}",
                trackType = TimelineTrackType.EFFECT,
                name = "${clip.cameraMovement.label} (${String.format(java.util.Locale.US, "%.1fx", clip.zoomFactor)})",
                startTimeSec = insertStart,
                durationSec = clip.durationSec,
                isAiGenerated = true
            )

            val injectedClips = mutableListOf(newVideoClip, newFxClip)
            if (clip.audioCue.isNotBlank()) {
                injectedClips.add(
                    TimelineClip(
                        id = "aud_${clip.id}",
                        trackType = TimelineTrackType.AUDIO,
                        name = clip.audioCue.take(20),
                        startTimeSec = insertStart,
                        durationSec = clip.durationSec,
                        isAiGenerated = true,
                        audioCue = clip.audioCue
                    )
                )
            }

            val updatedMuseList = state.museGeneratedClips.map {
                if (it.id == clip.id) it.copy(isImportedToTimeline = true) else it
            }

            state.copy(
                aspectRatio = clip.aspectRatio,
                motionTemplate = clip.cameraMovement.motionMapping,
                motionIntensity = (kotlin.math.abs(clip.panAmount) + kotlin.math.abs(clip.tiltAngle) + 0.6f).coerceIn(0.4f, 2.0f),
                totalDurationSec = nextTotalDuration,
                layers = updatedLayers,
                timelineClips = state.timelineClips + injectedClips,
                selectedClipId = newVideoClip.id,
                museGeneratedClips = updatedMuseList,
                activeModule = OmniModule.STUDIO
            )
        }
    }

    private suspend fun decodeScaledBitmap(uriString: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                val raw = BitmapFactory.decodeStream(stream) ?: return@withContext null
                val maxDim = 1024
                if (raw.width > maxDim || raw.height > maxDim) {
                    val ratio = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height)
                    Bitmap.createScaledBitmap(
                        raw,
                        (raw.width * ratio).toInt().coerceAtLeast(1),
                        (raw.height * ratio).toInt().coerceAtLeast(1),
                        true
                    )
                } else raw
            }
        } catch (_: Exception) {
            null
        }
    }

    fun setUserProfile(
        name: String,
        email: String,
        role: String = "OmniStudio Creator",
        subscriptionTier: String = "FREE",
        billingCycle: String = "MONTHLY",
        themePreference: String = if (_uiState.value.isDarkTheme) "DARK" else "LIGHT",
        aiCreditsRemaining: Int = 5,
        lastCreditResetDate: String = FirestoreRepository.todayDateKey(),
        verifiedTransactionId: String = ""
    ) {
        val isDark = themePreference.equals("DARK", ignoreCase = true)
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
        _uiState.update { state ->
            state.copy(
                subscriptionTier = subscriptionTier,
                billingCycle = billingCycle,
                aiCreditsRemaining = aiCreditsRemaining,
                isDarkTheme = isDark,
                user = UserProfile(
                    name = name,
                    email = email,
                    role = role,
                    subscriptionTier = subscriptionTier,
                    billingCycle = billingCycle,
                    themePreference = themePreference,
                    aiCreditsRemaining = aiCreditsRemaining,
                    lastCreditResetDate = lastCreditResetDate,
                    verifiedTransactionId = verifiedTransactionId
                )
            )
        }
    }

    fun syncUserProfileFromCloud(userId: String, displayName: String, email: String) {
        viewModelScope.launch {
            try {
                val profile = firestoreRepo.getOrCreateUserProfile(userId, displayName, email)
                setUserProfile(
                    name = profile.displayName,
                    email = profile.email,
                    role = profile.role,
                    subscriptionTier = profile.subscriptionTier,
                    billingCycle = profile.billingCycle,
                    themePreference = profile.themePreference,
                    aiCreditsRemaining = profile.aiCreditsRemaining,
                    lastCreditResetDate = profile.lastCreditResetDate,
                    verifiedTransactionId = profile.verifiedTransactionId
                )
                fetchUserCampaigns(userId)
                startRealtimeProfileObserver(userId)
            } catch (_: Exception) {
                setUserProfile(displayName, email, "OmniStudio Creator", "FREE", "MONTHLY", if (_uiState.value.isDarkTheme) "DARK" else "LIGHT", 5)
                fetchUserCampaigns(userId)
            }
        }
    }

    private fun startRealtimeProfileObserver(userId: String) {
        profileObserverJob?.cancel()
        profileObserverJob = viewModelScope.launch {
            firestoreRepo.observeUserProfile(userId).collect { cloudProfile ->
                if (cloudProfile != null) {
                    val prevTier = _uiState.value.subscriptionTier
                    val tierChangedToPaid = prevTier != cloudProfile.subscriptionTier &&
                        isPaidTier(cloudProfile.subscriptionTier)
                    setUserProfile(
                        name = cloudProfile.displayName,
                        email = cloudProfile.email,
                        role = cloudProfile.role,
                        subscriptionTier = cloudProfile.subscriptionTier,
                        billingCycle = cloudProfile.billingCycle,
                        themePreference = cloudProfile.themePreference,
                        aiCreditsRemaining = cloudProfile.aiCreditsRemaining,
                        lastCreditResetDate = cloudProfile.lastCreditResetDate,
                        verifiedTransactionId = cloudProfile.verifiedTransactionId
                    )
                    if (tierChangedToPaid) {
                        val today = FirestoreRepository.todayDateKey()
                        prefs.edit()
                            .putString("last_credit_reset_date", today)
                            .putInt("daily_credits_remaining", cloudProfile.aiCreditsRemaining)
                            .apply()
                        _uiState.update {
                            it.copy(
                                showProUpgradeModal = false,
                                activeCheckoutSession = null,
                                isProcessingPayment = false,
                                paymentError = null,
                                checkoutStatusBanner = "${SubscriptionPlan.fromTierId(cloudProfile.subscriptionTier).badgeTitle} unlocked via verified payment!"
                            )
                        }
                    }
                }
            }
        }
    }

    fun fetchUserCampaigns(userId: String) {
        campaignsJob?.cancel()
        _uiState.update { it.copy(isHistoryLoading = true) }
        campaignsJob = viewModelScope.launch {
            firestoreRepo.getUserCampaigns(userId).collect { campaigns ->
                _uiState.update { it.copy(campaignHistory = campaigns, isHistoryLoading = false) }
            }
        }
    }

    fun loadCampaignIntoCanvas(campaign: FirestoreCampaign) {
        saveUndoState()
        val matchedFramework = AdFramework.entries.find { it.label.equals(campaign.framework, ignoreCase = true) }
            ?: AdFramework.VIRAL_HOOK
        val matchedRatio = AspectRatioOption.entries.find { it.label.equals(campaign.aspectRatio, ignoreCase = true) }
            ?: AspectRatioOption.RATIO_9_16
        val matchedMotion = MotionTemplate.entries.find { it.title.equals(campaign.motionPreset, ignoreCase = true) }
            ?: MotionTemplate.DYNAMIC_ZOOM_PUNCH
        val matchedBg = BackgroundPreset.entries.find { it.title.equals(campaign.bgPreset, ignoreCase = true) }
            ?: BackgroundPreset.CYBERPUNK_NEON

        val matchedProduct = AdStudioRepository.demoProducts.find { it.name.equals(campaign.productName, ignoreCase = true) }
            ?: DemoProduct(
                id = "loaded_prod_${System.currentTimeMillis()}",
                name = campaign.productName.ifBlank { "Custom Product" },
                price = campaign.productPrice.ifBlank { "$99" },
                category = campaign.category.ifBlank { "E-Commerce" },
                description = "Saved OmniStudio Project",
                drawableRes = R.drawable.prod_watch,
                defaultBadge = campaign.cta.ifBlank { "50% OFF" }
            )

        _uiState.update { state ->
            val updatedLayers = state.layers.map { layer ->
                when (layer.id) {
                    "prod_layer" -> layer.copy(
                        name = matchedProduct.name,
                        productDrawableRes = matchedProduct.drawableRes,
                        customImageUri = null,
                        aiBitmap = null
                    )
                    "headline_layer" -> layer.copy(text = campaign.headline.ifBlank { matchedFramework.hook })
                    "offer_layer" -> layer.copy(badgeLabel = campaign.cta.ifBlank { matchedFramework.cta })
                    "cta_layer" -> layer.copy(text = campaign.cta.ifBlank { matchedFramework.cta })
                    else -> layer
                }
            }
            state.copy(
                selectedProduct = matchedProduct,
                aspectRatio = matchedRatio,
                motionTemplate = matchedMotion,
                bgPreset = matchedBg,
                adFramework = matchedFramework,
                layers = updatedLayers,
                activeModule = OmniModule.STUDIO,
                activeBottomSheetTab = null
            )
        }
    }

    fun deleteCampaign(userId: String, campaignId: String) {
        viewModelScope.launch {
            try {
                firestoreRepo.deleteCampaign(userId, campaignId)
            } catch (_: Exception) {
            }
        }
    }

    fun importCustomProductImage(uri: String, name: String = "Custom Product") {
        saveUndoState()
        val customProduct = DemoProduct(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            price = "$49.99",
            category = "Custom Import",
            description = "Custom media imported from device gallery",
            drawableRes = R.drawable.prod_watch,
            defaultBadge = "NEW ARRIVAL"
        )
        _uiState.update { state ->
            val updatedLayers = state.layers.map { layer ->
                if (layer.type == LayerType.PRODUCT) {
                    layer.copy(
                        name = name,
                        customImageUri = uri,
                        aiBitmap = null,
                        isCutoutApplied = false
                    )
                } else layer
            }
            state.copy(
                selectedProduct = customProduct,
                layers = updatedLayers,
                generatedCopy = AdStudioRepository.generateCopyForProduct(name, state.aiCategory)
            )
        }
    }

    private var cachedBackdropBitmap: Bitmap? = null

    fun loadVideoForBgRemoval(uriString: String) {
        _uiState.update { it.copy(videoBgState = it.videoBgState.copy(videoUri = uriString, isProcessing = true, errorMessage = null)) }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val uri = Uri.parse(uriString)
            val meta = withContext(Dispatchers.IO) { VideoSegmentationEngine.inspectLocalVideo(ctx, uri) }
            val startPos = (meta.durationMs / 4L).coerceAtLeast(0L)
            val frame = withContext(Dispatchers.IO) { VideoSegmentationEngine.extractVideoFrame(ctx, uri, startPos) }
            if (frame == null) {
                _uiState.update {
                    it.copy(
                        videoBgState = it.videoBgState.copy(
                            videoTitle = meta.title,
                            durationMs = meta.durationMs,
                            widthPx = meta.widthPx,
                            heightPx = meta.heightPx,
                            isProcessing = false,
                            errorMessage = "Could not decode keyframe from this video file."
                        )
                    )
                }
                return@launch
            }
            val currentBg = _uiState.value.videoBgState.copy(
                videoUri = uriString,
                videoTitle = meta.title,
                durationMs = meta.durationMs,
                widthPx = meta.widthPx,
                heightPx = meta.heightPx,
                scrubPositionMs = startPos,
                originalFrame = frame,
                isProcessing = true,
                errorMessage = null
            )
            _uiState.update { it.copy(videoBgState = currentBg) }
            processBgSegmentationState(currentBg)
        }
    }

    fun scrubVideoBgFrame(timeMs: Long) {
        val currentBg = _uiState.value.videoBgState
        val uriStr = currentBg.videoUri ?: return
        _uiState.update { it.copy(videoBgState = it.videoBgState.copy(scrubPositionMs = timeMs, isProcessing = true)) }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val frame = withContext(Dispatchers.IO) {
                VideoSegmentationEngine.extractVideoFrame(ctx, Uri.parse(uriStr), timeMs)
            }
            val next = _uiState.value.videoBgState.copy(
                scrubPositionMs = timeMs,
                originalFrame = frame ?: currentBg.originalFrame,
                isProcessing = false
            )
            _uiState.update { it.copy(videoBgState = next) }
            processBgSegmentationState(next)
        }
    }

    fun updateBgRemovalMode(mode: BgRemovalMode) {
        val next = _uiState.value.videoBgState.copy(mode = mode)
        _uiState.update { it.copy(videoBgState = next) }
        viewModelScope.launch { processBgSegmentationState(next) }
    }

    fun updateBgChromaColor(color: ChromaKeyColor) {
        val next = _uiState.value.videoBgState.copy(chromaKeyColor = color)
        _uiState.update { it.copy(videoBgState = next) }
        viewModelScope.launch { processBgSegmentationState(next) }
    }

    fun updateBgSensitivity(sensitivity: Float) {
        val next = _uiState.value.videoBgState.copy(sensitivity = sensitivity.coerceIn(0.05f, 0.95f))
        _uiState.update { it.copy(videoBgState = next) }
        viewModelScope.launch { processBgSegmentationState(next) }
    }

    fun updateBgFeather(feather: Float) {
        val next = _uiState.value.videoBgState.copy(edgeFeather = feather.coerceIn(0f, 0.9f))
        _uiState.update { it.copy(videoBgState = next) }
        viewModelScope.launch { processBgSegmentationState(next) }
    }

    fun updateBgReplacementType(type: BgReplacementType) {
        val next = _uiState.value.videoBgState.copy(replacementType = type)
        _uiState.update { it.copy(videoBgState = next) }
        viewModelScope.launch { processBgSegmentationState(next) }
    }

    fun updateBgSolidColor(argb: Long) {
        val next = _uiState.value.videoBgState.copy(
            replacementType = BgReplacementType.SOLID_COLOR,
            solidColorArgb = argb
        )
        _uiState.update { it.copy(videoBgState = next) }
        viewModelScope.launch { processBgSegmentationState(next) }
    }

    fun loadCustomBackdropImage(uriString: String) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            cachedBackdropBitmap = withContext(Dispatchers.IO) {
                VideoSegmentationEngine.decodeBackdropBitmap(ctx, Uri.parse(uriString))
            }
            val next = _uiState.value.videoBgState.copy(
                replacementType = BgReplacementType.CUSTOM_IMAGE,
                customBackdropUri = uriString
            )
            _uiState.update { it.copy(videoBgState = next) }
            processBgSegmentationState(next)
        }
    }

    private suspend fun processBgSegmentationState(bgState: VideoBgRemovalState) {
        val src = bgState.originalFrame ?: return
        val segmented = withContext(Dispatchers.Default) {
            VideoSegmentationEngine.processFrame(
                sourceFrame = src,
                mode = bgState.mode,
                chromaKeyColor = bgState.chromaKeyColor,
                sensitivity = bgState.sensitivity,
                edgeFeather = bgState.edgeFeather,
                replacementType = bgState.replacementType,
                solidColorArgb = bgState.solidColorArgb,
                customBackdrop = cachedBackdropBitmap
            )
        }
        _uiState.update {
            it.copy(
                videoBgState = it.videoBgState.copy(
                    cutoutAlphaFrame = segmented.foregroundCutout,
                    segmentedPreviewFrame = segmented.compositedPreview,
                    isProcessing = false
                )
            )
        }
    }

    fun injectBgRemovedClipToTimeline() {
        val bgState = _uiState.value.videoBgState
        val cutout = bgState.cutoutAlphaFrame ?: bgState.segmentedPreviewFrame ?: return
        saveUndoState()
        _uiState.update { state ->
            val updatedLayers = state.layers.map { layer ->
                if (layer.type == LayerType.PRODUCT) {
                    layer.copy(
                        name = bgState.videoTitle,
                        aiBitmap = cutout,
                        customImageUri = null,
                        isCutoutApplied = true
                    )
                } else layer
            }
            val start = state.playheadSec.coerceAtLeast(0f)
            val dur = (bgState.durationMs / 1000f).coerceIn(2f, 8f)
            val nextTotal = maxOf(state.totalDurationSec, start + dur)
            val cutoutClip = TimelineClip(
                id = "tl_cutout_${System.currentTimeMillis()}",
                trackType = TimelineTrackType.PRODUCT,
                name = "${bgState.mode.label}: ${bgState.videoTitle.take(14)}",
                startTimeSec = start,
                durationSec = dur,
                isAiGenerated = true
            )
            val fxClip = TimelineClip(
                id = "fx_cutout_${System.currentTimeMillis()}",
                trackType = TimelineTrackType.EFFECT,
                name = "Alpha Matte (${bgState.replacementType.label})",
                startTimeSec = start,
                durationSec = dur,
                isAiGenerated = true
            )
            state.copy(
                layers = updatedLayers,
                timelineClips = state.timelineClips + listOf(cutoutClip, fxClip),
                totalDurationSec = nextTotal,
                selectedClipId = cutoutClip.id,
                activeModule = OmniModule.STUDIO
            )
        }
    }

    fun selectPaymentProvider(provider: PaymentProvider) {
        _uiState.update { it.copy(selectedPaymentProvider = provider, paymentError = null) }
    }

    fun isPaymentBackendConfigured(): Boolean = paymentGatewayRepo.isBackendConfigured()

    fun isFlutterwaveDirectConfigured(): Boolean = paymentGatewayRepo.isFlutterwaveDirectConfigured()

    fun isStripePaymentLinkConfigured(): Boolean = paymentGatewayRepo.isStripePaymentLinkConfigured()

    fun dismissFlutterwaveInlineModal() {
        _uiState.update {
            it.copy(
                activeInlineWebViewSession = null,
                isProcessingPayment = false,
                checkoutStatusBanner = null
            )
        }
    }

    fun initiatePlanCheckout(
        plan: SubscriptionPlan,
        billingCycle: BillingCycle,
        provider: PaymentProvider,
        billingEmail: String,
        onLaunchCheckoutUrl: (String) -> Unit,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val uid = try { Firebase.auth.currentUser?.uid } catch (_: Exception) { null }
        if (uid == null) {
            val errMsg = "Sign in with your creator account before initiating payment checkout."
            _uiState.update { it.copy(paymentError = errMsg, isProcessingPayment = false) }
            onResult(false, errMsg)
            return
        }

        _uiState.update {
            it.copy(
                isProcessingPayment = true,
                paymentError = null,
                selectedPaymentProvider = provider,
                checkoutStatusBanner = "Creating secure ${provider.displayName} session..."
            )
        }

        val customerName = _uiState.value.user.name
        viewModelScope.launch {
            val sessionResult = paymentGatewayRepo.createCheckoutSession(
                userId = uid,
                email = billingEmail,
                plan = plan,
                billingCycle = billingCycle,
                provider = provider,
                customerName = customerName
            )

            sessionResult.fold(
                onSuccess = { session ->
                    if (session.launchMode == CheckoutLaunchMode.FLUTTERWAVE_INLINE_WEBVIEW) {
                        _uiState.update {
                            it.copy(
                                isProcessingPayment = false,
                                activeInlineWebViewSession = session,
                                activeCheckoutSession = null,
                                checkoutStatusBanner = "Flutterwave checkout open. Complete payment to unlock ${plan.badgeTitle}."
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isProcessingPayment = false,
                                activeCheckoutSession = session,
                                activeInlineWebViewSession = null,
                                checkoutStatusBanner = "${provider.displayName} checkout open. Complete payment to unlock ${plan.badgeTitle} instantly."
                            )
                        }
                        onLaunchCheckoutUrl(session.checkoutUrl)
                    }
                    onResult(true, session.sessionId)
                },
                onFailure = { error ->
                    val errText = error.localizedMessage ?: "Unable to initialize ${provider.displayName} checkout session."
                    _uiState.update {
                        it.copy(
                            isProcessingPayment = false,
                            paymentError = errText,
                            checkoutStatusBanner = null
                        )
                    }
                    onResult(false, errText)
                }
            )
        }
    }

    fun handleFlutterwaveInlineSuccess(
        session: CheckoutSessionInit,
        status: String,
        transactionId: String,
        txRef: String,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val uid = try { Firebase.auth.currentUser?.uid } catch (_: Exception) { null }
        if (uid == null) {
            val errMsg = "Authenticated account required to activate subscription."
            _uiState.update {
                it.copy(
                    activeInlineWebViewSession = null,
                    isProcessingPayment = false,
                    paymentError = errMsg
                )
            }
            onComplete(false, errMsg)
            return
        }

        val normalizedStatus = status.trim().lowercase(java.util.Locale.US)
        val isSuccess = normalizedStatus in setOf("successful", "completed", "paid", "succeeded", "success")
        if (!isSuccess) {
            val errMsg = "Flutterwave payment returned status \"$status\". Please try again."
            _uiState.update {
                it.copy(
                    activeInlineWebViewSession = null,
                    isProcessingPayment = false,
                    paymentError = errMsg,
                    checkoutStatusBanner = null
                )
            }
            onComplete(false, errMsg)
            return
        }

        val rawRef = transactionId.ifBlank { txRef }.ifBlank { session.sessionId }
        val safeRef = ("flw_${rawRef}_${session.sessionId}").take(100).let {
            if (it.length < 6) "flw_tx_$it" else it
        }

        _uiState.update {
            it.copy(
                activeInlineWebViewSession = null,
                isProcessingPayment = true,
                paymentError = null,
                checkoutStatusBanner = "Confirming Flutterwave payment receipt..."
            )
        }

        if (paymentGatewayRepo.isBackendConfigured()) {
            viewModelScope.launch {
                val verifyRes = paymentGatewayRepo.verifyCheckoutSession(
                    userId = uid,
                    sessionId = session.sessionId,
                    provider = PaymentProvider.FLUTTERWAVE,
                    expectedPlan = session.plan,
                    expectedCycle = session.billingCycle
                )
                verifyRes.fold(
                    onSuccess = { verification ->
                        if (verification.verified) {
                            completeVerifiedSubscription(
                                plan = verification.plan,
                                billingCycle = verification.billingCycle,
                                paymentMethod = "Flutterwave Checkout",
                                billingEmail = session.billingEmail,
                                externalTransactionRef = verification.transactionRef.ifBlank { safeRef },
                                onComplete = onComplete
                            )
                        } else {
                            val pendingMsg = verification.message ?: "Flutterwave verification pending."
                            _uiState.update { it.copy(isProcessingPayment = false, paymentError = pendingMsg) }
                            onComplete(false, pendingMsg)
                        }
                    },
                    onFailure = { err ->
                        val msg = err.localizedMessage ?: "Flutterwave backend verification failed."
                        _uiState.update { it.copy(isProcessingPayment = false, paymentError = msg) }
                        onComplete(false, msg)
                    }
                )
            }
        } else {
            completeVerifiedSubscription(
                plan = session.plan,
                billingCycle = session.billingCycle,
                paymentMethod = "Flutterwave Checkout",
                billingEmail = session.billingEmail,
                externalTransactionRef = safeRef,
                onComplete = onComplete
            )
        }
    }

    fun verifyPendingCheckoutSession(onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        val uid = try { Firebase.auth.currentUser?.uid } catch (_: Exception) { null }
        val session = _uiState.value.activeCheckoutSession
        if (uid == null || session == null) {
            val msg = "No active checkout session found."
            _uiState.update { it.copy(paymentError = msg) }
            onComplete(false, msg)
            return
        }

        _uiState.update { it.copy(isProcessingPayment = true, paymentError = null) }
        viewModelScope.launch {
            val verifyRes = paymentGatewayRepo.verifyCheckoutSession(
                userId = uid,
                sessionId = session.sessionId,
                provider = session.provider,
                expectedPlan = session.plan,
                expectedCycle = session.billingCycle
            )

            verifyRes.fold(
                onSuccess = { verification ->
                    if (!verification.verified) {
                        val pendingMsg = verification.message ?: "Payment not completed yet. Finish checkout and tap verify again."
                        _uiState.update { it.copy(isProcessingPayment = false, paymentError = pendingMsg) }
                        onComplete(false, pendingMsg)
                    } else {
                        completeVerifiedSubscription(
                            plan = verification.plan,
                            billingCycle = verification.billingCycle,
                            paymentMethod = "${verification.provider.displayName} Checkout",
                            billingEmail = session.billingEmail,
                            externalTransactionRef = verification.transactionRef,
                            onComplete = onComplete
                        )
                    }
                },
                onFailure = { err ->
                    val msg = err.localizedMessage ?: "Verification check failed."
                    _uiState.update { it.copy(isProcessingPayment = false, paymentError = msg) }
                    onComplete(false, msg)
                }
            )
        }
    }

    fun handleCheckoutCallbackUri(
        uri: Uri?,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val callback = paymentGatewayRepo.parseDeepLinkCallback(uri) ?: return
        if (callback.isCancelled) {
            _uiState.update {
                it.copy(
                    isProcessingPayment = false,
                    activeInlineWebViewSession = null,
                    paymentError = "Checkout was cancelled before payment completed.",
                    checkoutStatusBanner = null
                )
            }
            onComplete(false, "Checkout cancelled.")
            return
        }

        val uid = try { Firebase.auth.currentUser?.uid } catch (_: Exception) { null } ?: return
        val activeSession = _uiState.value.activeCheckoutSession ?: _uiState.value.activeInlineWebViewSession
        val plan = callback.plan ?: activeSession?.plan ?: return
        val cycle = callback.billingCycle ?: activeSession?.billingCycle ?: BillingCycle.MONTHLY
        val provider = callback.provider ?: activeSession?.provider ?: _uiState.value.selectedPaymentProvider
        val sessionId = callback.sessionId.ifBlank { activeSession?.sessionId.orEmpty() }
        val email = _uiState.value.user.email.ifBlank { activeSession?.billingEmail.orEmpty() }

        _uiState.update { it.copy(activeInlineWebViewSession = null) }

        if (sessionId.isNotBlank() && paymentGatewayRepo.isBackendConfigured()) {
            _uiState.update {
                it.copy(
                    isProcessingPayment = true,
                    paymentError = null,
                    checkoutStatusBanner = "Verifying ${provider.displayName} receipt..."
                )
            }
            viewModelScope.launch {
                val res = paymentGatewayRepo.verifyCheckoutSession(uid, sessionId, provider, plan, cycle)
                res.fold(
                    onSuccess = { v ->
                        if (v.verified) {
                            completeVerifiedSubscription(
                                plan = v.plan,
                                billingCycle = v.billingCycle,
                                paymentMethod = "${provider.displayName} Checkout",
                                billingEmail = email,
                                externalTransactionRef = v.transactionRef,
                                onComplete = onComplete
                            )
                        } else {
                            val errMsg = v.message ?: "Payment webhook confirmation pending."
                            _uiState.update { it.copy(isProcessingPayment = false, paymentError = errMsg) }
                            onComplete(false, errMsg)
                        }
                    },
                    onFailure = { e ->
                        val msg = e.localizedMessage ?: "Failed to verify callback receipt."
                        _uiState.update { it.copy(isProcessingPayment = false, paymentError = msg) }
                        onComplete(false, msg)
                    }
                )
            }
        } else if (callback.isSuccessful && callback.transactionRef.isNotBlank()) {
            val safeRef = callback.transactionRef.let { ref ->
                if (ref.length < 6) "gw_ref_$ref" else ref.take(120)
            }
            completeVerifiedSubscription(
                plan = plan,
                billingCycle = cycle,
                paymentMethod = "${provider.displayName} Webhook",
                billingEmail = email,
                externalTransactionRef = safeRef,
                onComplete = onComplete
            )
        }
    }

    fun completeVerifiedSubscription(
        plan: SubscriptionPlan,
        billingCycle: BillingCycle,
        paymentMethod: String,
        billingEmail: String,
        externalTransactionRef: String? = null,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val uid = try { Firebase.auth.currentUser?.uid } catch (_: Exception) { null }
        if (uid == null) {
            val errMsg = "Authenticated Google account required to verify & process payment."
            _uiState.update { it.copy(paymentError = errMsg, isProcessingPayment = false) }
            onComplete(false, errMsg)
            return
        }

        _uiState.update { it.copy(isProcessingPayment = true, paymentError = null) }
        viewModelScope.launch {
            val result = firestoreRepo.recordVerifiedSubscriptionPurchase(
                userId = uid,
                plan = plan,
                billingCycle = billingCycle,
                paymentMethod = paymentMethod,
                billingEmail = billingEmail,
                externalTransactionRef = externalTransactionRef
            )
            result.fold(
                onSuccess = { txId ->
                    val today = FirestoreRepository.todayDateKey()
                    prefs.edit()
                        .putString("last_credit_reset_date", today)
                        .putInt("daily_credits_remaining", plan.dailyAiCredits)
                        .apply()

                    _uiState.update { state ->
                        state.copy(
                            subscriptionTier = plan.id,
                            billingCycle = billingCycle.id,
                            aiCreditsRemaining = plan.dailyAiCredits,
                            showProUpgradeModal = false,
                            paywallReason = null,
                            isProcessingPayment = false,
                            paymentError = null,
                            activeCheckoutSession = null,
                            activeInlineWebViewSession = null,
                            checkoutStatusBanner = "${plan.badgeTitle} activated!",
                            user = state.user.copy(
                                subscriptionTier = plan.id,
                                billingCycle = billingCycle.id,
                                aiCreditsRemaining = plan.dailyAiCredits,
                                lastCreditResetDate = today,
                                verifiedTransactionId = txId
                            )
                        )
                    }
                    onComplete(true, txId)
                },
                onFailure = { error ->
                    val errText = error.localizedMessage ?: "Payment verification failed."
                    _uiState.update {
                        it.copy(
                            isProcessingPayment = false,
                            paymentError = errText
                        )
                    }
                    onComplete(false, errText)
                }
            )
        }
    }

    fun downgradeToFreeTier() {
        val uid = try { Firebase.auth.currentUser?.uid } catch (_: Exception) { null }
        val today = FirestoreRepository.todayDateKey()
        _uiState.update { state ->
            state.copy(
                subscriptionTier = SubscriptionPlan.FREE.id,
                billingCycle = BillingCycle.MONTHLY.id,
                aiCreditsRemaining = SubscriptionPlan.FREE.dailyAiCredits,
                showProUpgradeModal = false,
                paywallReason = null,
                activeCheckoutSession = null,
                activeInlineWebViewSession = null,
                paymentError = null,
                user = state.user.copy(
                    subscriptionTier = SubscriptionPlan.FREE.id,
                    billingCycle = BillingCycle.MONTHLY.id,
                    aiCreditsRemaining = SubscriptionPlan.FREE.dailyAiCredits,
                    lastCreditResetDate = today
                )
            )
        }
        if (uid != null) {
            viewModelScope.launch {
                firestoreRepo.downgradeToFreeTier(uid)
            }
        }
    }

    fun saveCurrentCampaignToCloud(onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val headline = state.layers.find { it.id == "headline_layer" }?.text ?: state.adFramework.hook
                val cta = state.layers.find { it.id == "cta_layer" }?.text ?: state.adFramework.cta
                val campaign = FirestoreCampaign(
                    name = "${state.selectedProduct.name} - ${state.adFramework.label}",
                    productName = state.selectedProduct.name,
                    productPrice = state.selectedProduct.price,
                    category = state.selectedProduct.category,
                    framework = state.adFramework.label,
                    aspectRatio = state.aspectRatio.label,
                    headline = headline,
                    cta = cta,
                    motionPreset = state.motionTemplate.title,
                    bgPreset = state.bgPreset.title
                )
                firestoreRepo.saveCampaign(campaign)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        campaignsJob?.cancel()
        profileObserverJob?.cancel()
        audioEngine.release()
        playbackJob?.cancel()
    }
}
