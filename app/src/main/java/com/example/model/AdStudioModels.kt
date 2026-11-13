package com.example.model

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

enum class AspectRatioOption(val label: String, val subtitle: String, val ratio: Float, val widthPx: Int, val heightPx: Int) {
    RATIO_9_16("9:16", "TikTok / Reels / Shorts", 9f / 16f, 1080, 1920),
    RATIO_1_1("1:1", "Instagram Feed / Square", 1f, 1080, 1080),
    RATIO_16_9("16:9", "YouTube / Landscape", 16f / 9f, 1920, 1080),
    RATIO_4_5("4:5", "Instagram Portrait Ad", 4f / 5f, 1080, 1350)
}

enum class LayerType {
    PRODUCT,
    TEXT,
    BADGE,
    STICKER,
    SHAPE
}

enum class TextAnimationType(val title: String) {
    NONE("Static"),
    BOUNCE("Bounce"),
    ZOOM_PUNCH("Zoom Punch"),
    TYPEWRITER("Typewriter"),
    FADE_SLIDE("Fade & Slide")
}

data class CanvasLayer(
    val id: String,
    val name: String,
    val type: LayerType,
    val x: Float = 0.5f, // Normalized 0..1 center
    val y: Float = 0.5f,
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val zIndex: Int = 0,
    val opacity: Float = 1.0f,
    val isLocked: Boolean = false,
    val isVisible: Boolean = true,

    // Text specific properties
    val text: String = "",
    val fontSizeSp: Float = 24f,
    val textColor: Long = 0xFFFFFFFF,
    val isGradientText: Boolean = false,
    val gradientColorStart: Long = 0xFF06B6D4,
    val gradientColorEnd: Long = 0xFF8B5CF6,
    val isNeonGlow: Boolean = false,
    val isOutline: Boolean = false,
    val isShadow: Boolean = true,
    val letterSpacingSp: Float = 0.5f,
    val textAnimation: TextAnimationType = TextAnimationType.NONE,

    // Product / AI Scene specific properties
    @DrawableRes val productDrawableRes: Int = R.drawable.prod_watch,
    val customImageUri: String? = null,
    val aiBitmap: Bitmap? = null,
    val isCutoutApplied: Boolean = false,

    // Badge specific properties
    val badgeLabel: String = "50% OFF",
    val badgeStyle: String = "SALE_PILL", // SALE_PILL, BEST_SELLER, LIMITED_STOCK, NEW_DROP
    val badgeAccentColor: Long = 0xFFEC4899
)

enum class MotionTemplate(val title: String, val subtitle: String, val iconDescription: String) {
    DYNAMIC_ZOOM_PUNCH("Dynamic Zoom Punch", "Punch zoom with camera shake", "High Energy"),
    ORBIT_3D("3D Orbit", "Smooth circular perspective motion", "Sleek 3D"),
    FLOATING_LUXURY("Floating Luxury", "Silky vertical float & soft rotation", "Elegance"),
    FLASH_SALE_GLITCH("Flash Sale Glitch", "RGB chromatic flash & strobe punch", "Urgent Sale"),
    CINEMATIC_REVEAL("Cinematic Reveal", "Masked blur-to-focus unveiling", "Premium Launch")
}

enum class BackgroundPreset(val title: String, val category: String) {
    LUXURY_MARBLE("Luxury Marble", "Classic"),
    WHITE_STUDIO("White Studio", "Clean"),
    NATURE_SUNBEAMS("Nature Sunbeams", "Organic"),
    FASHION_RUNWAY("Fashion Runway", "Vibrant"),
    CYBERPUNK_NEON("Cyberpunk Neon", "Tech"),
    GOLDEN_LUXURY("Golden Luxury", "Prestige"),
    TECH_GRID("Tech Grid", "Futuristic"),
    LUXURY_BLACK_GOLD("Black & Gold", "Luxury"),
    CUSTOM_PROMPT("Custom AI Studio", "Custom")
}

data class LightingSettings(
    val shadowOpacity: Float = 0.65f,
    val shadowBlur: Float = 24f,
    val shadowDistance: Float = 18f,
    val shadowAngle: Float = 45f,
    val brightness: Float = 0.05f,
    val contrast: Float = 1.15f,
    val exposure: Float = 0.08f,
    val temperature: Float = 0.02f, // Warm > 0, Cool < 0
    val reflectionOpacity: Float = 0.45f,
    val reflectionHeight: Float = 0.35f,
    val reflectionFade: Float = 0.85f
)

enum class TimelineTrackType(val trackName: String, val color: Color) {
    PRODUCT("Video & Product", Color(0xFF06B6D4)),
    TEXT("Text Overlays", Color(0xFF8B5CF6)),
    AUDIO("BGM & Audio", Color(0xFF10B981)),
    EFFECT("AI FX & Motion", Color(0xFFEC4899))
}

data class TimelineClip(
    val id: String,
    val trackType: TimelineTrackType,
    val name: String,
    val startTimeSec: Float,
    val durationSec: Float,
    val isMuted: Boolean = false,
    val isAiGenerated: Boolean = false,
    val audioCue: String = ""
)

enum class AudioTrackOption(val title: String, val genre: String, val bpm: Int, val durationSec: Float) {
    CORPORATE_UPBEAT("Silicon Valley Groove", "Corporate Pop", 120, 15f),
    LUXURY_AMBIENT("Midnight Haute Couture", "Ambient Chill", 85, 15f),
    TECH_FUTURE("Cyberpunk Velocity", "Synthwave", 128, 15f),
    MOTIVATIONAL("Apex Rise", "Cinematic Epic", 112, 15f),
    UPBEAT_TRAP("Neon Street Hype", "Urban Trap", 140, 15f)
}

data class DemoProduct(
    val id: String,
    val name: String,
    val price: String,
    val category: String,
    val description: String,
    @DrawableRes val drawableRes: Int,
    val defaultBadge: String
)

data class AdTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val platformTag: String,
    val defaultRatio: AspectRatioOption,
    val motionPreset: MotionTemplate,
    val bgPreset: BackgroundPreset,
    val defaultHeadline: String,
    val defaultOffer: String,
    val defaultCta: String
)

enum class AiCategory(val displayName: String) {
    ECOMMERCE("E-Commerce"),
    LUXURY("Luxury & Watches"),
    TECH("Tech & Audio"),
    BEAUTY("Beauty & Cosmetics"),
    FITNESS("Fitness & Sneakers")
}

data class GeneratedAdCopy(
    val hooks: List<String>,
    val headlines: List<String>,
    val offers: List<String>,
    val scripts: List<String>,
    val ctas: List<String>
)

enum class BatchItemStatus {
    WAITING,
    REMOVING_BG,
    APPLYING_TEMPLATE,
    COMPOSITING_CTA,
    COMPLETED
}

data class BatchItem(
    val product: DemoProduct,
    val status: BatchItemStatus,
    val progress: Float = 0f,
    val resultPreviewName: String = ""
)

enum class AdFramework(
    val id: String,
    val label: String,
    val hook: String,
    val cta: String,
    val motion: MotionTemplate
) {
    VIRAL_HOOK("tiktok-hook", "Viral Hook", "STOP SCROLLING!", "50% OFF TODAY ONLY", MotionTemplate.DYNAMIC_ZOOM_PUNCH),
    LUXURY_SHOWCASE("luxury-showcase", "Luxury Showcase", "REDEFINE PERFECTION", "EXPLORE PRIVATE ATELIER", MotionTemplate.FLOATING_LUXURY),
    FLASH_SALE("flash-sale", "Flash Sale", "MIDNIGHT FLASH DROP", "FIRST 100 ORDERS FREE GIFT", MotionTemplate.FLASH_SALE_GLITCH),
    PROBLEM_SOLUTION("problem-solution", "Problem-Solution", "UPGRADE YOUR SETUP", "ORDER WITH 1-CLICK", MotionTemplate.ORBIT_3D)
}

enum class BillingCycle(val id: String, val label: String, val shortLabel: String) {
    MONTHLY("MONTHLY", "Monthly", "/mo"),
    YEARLY("YEARLY", "Yearly", "/yr")
}

enum class SubscriptionPlan(
    val id: String,
    val displayName: String,
    val badgeTitle: String,
    val monthlyPriceUsd: Double,
    val yearlyMonthlyPriceUsd: Double,
    val yearlyTotalUsd: Double,
    val yearlySavingsLabel: String,
    val dailyAiCredits: Int,
    val removesWatermark: Boolean,
    val maxResolutionLabel: String,
    val tagline: String,
    val features: List<String>
) {
    FREE(
        id = "FREE",
        displayName = "Free Starter",
        badgeTitle = "FREE",
        monthlyPriceUsd = 0.0,
        yearlyMonthlyPriceUsd = 0.0,
        yearlyTotalUsd = 0.0,
        yearlySavingsLabel = "",
        dailyAiCredits = 5,
        removesWatermark = false,
        maxResolutionLabel = "720p HD • Watermarked",
        tagline = "Get started with core Studio, Director & Muse AI",
        features = listOf(
            "5 Daily AI Credits (Muse & Director)",
            "Standard Chroma Key BG Removal",
            "OmniStudio AI Free Watermark on Exports"
        )
    ),
    BASIC(
        id = "BASIC",
        displayName = "Plan 1 • Basic",
        badgeTitle = "BASIC",
        monthlyPriceUsd = 5.0,
        yearlyMonthlyPriceUsd = 4.0,
        yearlyTotalUsd = 48.0,
        yearlySavingsLabel = "Save $1/mo",
        dailyAiCredits = 50,
        removesWatermark = false,
        maxResolutionLabel = "1080p HD • 30 FPS",
        tagline = "Ideal for daily Reels & TikTok creators",
        features = listOf(
            "50 Daily AI Credits (10x Free allocation)",
            "HD 1080p Exports & Precision Chroma Key",
            "Save $1/mo ($48/yr) when billed yearly"
        )
    ),
    PRO(
        id = "PRO",
        displayName = "Plan 2 • Pro",
        badgeTitle = "PRO",
        monthlyPriceUsd = 15.0,
        yearlyMonthlyPriceUsd = 12.0,
        yearlyTotalUsd = 144.0,
        yearlySavingsLabel = "Save $3/mo",
        dailyAiCredits = 9999,
        removesWatermark = true,
        maxResolutionLabel = "1080p/4K • 60 FPS",
        tagline = "Everything in Basic + Unlimited AI & Zero Watermark",
        features = listOf(
            "Unlimited AI Credits + Advanced Multi-Track Tools",
            "Removes 'OmniStudio AI Free' Watermark",
            "60 FPS 1080p/4K Exports + Director Pro Models"
        )
    ),
    PREMIUM(
        id = "PREMIUM",
        displayName = "Plan 3 • Premium",
        badgeTitle = "PREMIUM",
        monthlyPriceUsd = 20.0,
        yearlyMonthlyPriceUsd = 17.0,
        yearlyTotalUsd = 204.0,
        yearlySavingsLabel = "Save $3/mo",
        dailyAiCredits = 9999,
        removesWatermark = true,
        maxResolutionLabel = "4K Ultra-HD • Priority GPU",
        tagline = "Flagship suite for agencies & faceless channels",
        features = listOf(
            "Everything in Pro + Priority GPU Rendering",
            "Ultra-HD 4K Mastering & Multi-Clip Batch Automation",
            "Commercial Studio License + AI Segmentation Pro"
        )
    );

    companion object {
        fun fromTierId(tier: String): SubscriptionPlan =
            entries.find { it.id.equals(tier, ignoreCase = true) } ?: FREE
    }
}

fun isWatermarkFreeTier(tier: String): Boolean =
    tier.uppercase() in setOf("PRO", "PREMIUM")

fun isUnlimitedAiTier(tier: String): Boolean =
    tier.uppercase() in setOf("PRO", "PREMIUM")

fun isPaidTier(tier: String): Boolean =
    tier.uppercase() in setOf("BASIC", "PRO", "PREMIUM")

enum class BgRemovalMode(val label: String, val subtitle: String) {
    AI_SEGMENTATION("AI Matte", "Auto foreground subject isolation"),
    CHROMA_KEY("Chroma Key", "Precision screen color stripping")
}

enum class ChromaKeyColor(val label: String, val argb: Long, val rgbTriple: Triple<Int, Int, Int>) {
    GREEN("Green Screen", 0xFF10B981, Triple(0, 200, 70)),
    BLUE("Blue Screen", 0xFF3B82F6, Triple(20, 90, 220)),
    WHITE("White Sweep", 0xFFF8FAFC, Triple(242, 242, 245)),
    BLACK("Dark Studio", 0xFF0F172A, Triple(16, 18, 24))
}

enum class BgReplacementType(val label: String) {
    TRANSPARENT("Alpha PNG"),
    SOLID_COLOR("Studio Color"),
    BLUR_ORIGINAL("Cinematic Blur"),
    CUSTOM_IMAGE("Custom Image")
}

data class VideoBgRemovalState(
    val videoUri: String? = null,
    val videoTitle: String = "No video selected",
    val durationMs: Long = 0L,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val scrubPositionMs: Long = 0L,
    val mode: BgRemovalMode = BgRemovalMode.AI_SEGMENTATION,
    val chromaKeyColor: ChromaKeyColor = ChromaKeyColor.GREEN,
    val sensitivity: Float = 0.42f,
    val edgeFeather: Float = 0.28f,
    val replacementType: BgReplacementType = BgReplacementType.TRANSPARENT,
    val solidColorArgb: Long = 0xFF06B6D4,
    val customBackdropUri: String? = null,
    val originalFrame: Bitmap? = null,
    val segmentedPreviewFrame: Bitmap? = null,
    val cutoutAlphaFrame: Bitmap? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

data class UserProfile(
    val name: String = "OmniStudio Creator",
    val email: String = "enjebeln@gmail.com",
    val role: String = "OmniStudio Creator",
    val subscriptionTier: String = "FREE", // "FREE", "BASIC", "PRO", "PREMIUM"
    val billingCycle: String = "MONTHLY",
    val themePreference: String = "DARK",
    val aiCreditsRemaining: Int = 5,
    val lastCreditResetDate: String = "",
    val verifiedTransactionId: String = ""
)

// Director Chat Models
data class DirectorMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val attachedMediaUri: String? = null,
    val attachedMediaName: String? = null,
    val executedActions: List<String> = emptyList(),
    val followUpSuggestions: List<String> = emptyList(),
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

// Muse Engine (AI Video Generator) Models
enum class MuseGenerationMode(val label: String) {
    TEXT_TO_VIDEO("Text to Video"),
    IMAGE_TO_VIDEO("Image to Video")
}

enum class CameraMovement(
    val label: String,
    val subtitle: String,
    val axisTag: String,
    val defaultPan: Float,
    val defaultZoom: Float,
    val defaultTilt: Float,
    val motionMapping: MotionTemplate
) {
    SLOW_PUSH_IN(
        label = "Slow Zoom In",
        subtitle = "Intimate dolly zoom forward",
        axisTag = "Zoom",
        defaultPan = 0f,
        defaultZoom = 1.35f,
        defaultTilt = 0f,
        motionMapping = MotionTemplate.CINEMATIC_REVEAL
    ),
    CINEMATIC_PAN(
        label = "Cinematic Pan",
        subtitle = "Smooth lateral studio sweep",
        axisTag = "Pan",
        defaultPan = 0.65f,
        defaultZoom = 1.1f,
        defaultTilt = 0f,
        motionMapping = MotionTemplate.FLOATING_LUXURY
    ),
    CINEMATIC_TILT(
        label = "Dynamic Tilt",
        subtitle = "Vertical low-to-high crane reveal",
        axisTag = "Tilt",
        defaultPan = 0f,
        defaultZoom = 1.15f,
        defaultTilt = 0.6f,
        motionMapping = MotionTemplate.CINEMATIC_REVEAL
    ),
    DRONE_PULL_BACK(
        label = "Pull-Back Orbit",
        subtitle = "Expansive aerial 3D zoom-out",
        axisTag = "Zoom + Pan",
        defaultPan = -0.35f,
        defaultZoom = 0.75f,
        defaultTilt = 0.25f,
        motionMapping = MotionTemplate.ORBIT_3D
    ),
    WHIP_ZOOM(
        label = "TikTok Whip Zoom",
        subtitle = "High-energy punch transition",
        axisTag = "Zoom",
        defaultPan = 0.2f,
        defaultZoom = 1.75f,
        defaultTilt = -0.15f,
        motionMapping = MotionTemplate.DYNAMIC_ZOOM_PUNCH
    ),
    GLITCH_PULSE(
        label = "Glitch Pulse",
        subtitle = "Cyberpunk strobe shake",
        axisTag = "Pan + Tilt",
        defaultPan = -0.25f,
        defaultZoom = 1.25f,
        defaultTilt = -0.3f,
        motionMapping = MotionTemplate.FLASH_SALE_GLITCH
    )
}

data class MuseGeneratedClip(
    val id: String,
    val title: String,
    val prompt: String,
    val cameraMovement: CameraMovement,
    val panAmount: Float = cameraMovement.defaultPan,
    val zoomFactor: Float = cameraMovement.defaultZoom,
    val tiltAngle: Float = cameraMovement.defaultTilt,
    val aspectRatio: AspectRatioOption,
    val audioCue: String,
    val sceneDirection: String,
    val suggestedHook: String,
    val durationSec: Float,
    val keyframeBitmap: Bitmap?,
    val referenceUri: String? = null,
    val isImportedToTimeline: Boolean = false
)

sealed interface MuseGenUiState {
    data object Idle : MuseGenUiState
    data class Loading(val stageText: String) : MuseGenUiState
    data class Success(val clips: List<MuseGeneratedClip>) : MuseGenUiState
    data class Error(val message: String) : MuseGenUiState
}
