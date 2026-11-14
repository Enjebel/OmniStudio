package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.configureAppCheck
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AiCopyGeneratorPanel
import com.example.ui.components.AudioVoicePanel
import com.example.ui.components.BatchCreatorPanel
import com.example.ui.components.BgRemovalScreen
import com.example.ui.components.DirectorChatScreen
import com.example.ui.components.ExportDialog
import com.example.ui.components.FlutterwaveCheckoutModal
import com.example.ui.components.HistoryPanel
import com.example.ui.components.LayerManagerPanel
import com.example.ui.components.MotionTemplatePanel
import com.example.ui.components.MuseGeneratorScreen
import com.example.ui.components.PhotoAndLightingPanel
import com.example.ui.components.ProUpgradeModal
import com.example.ui.components.ProfileDrawerSidebar
import com.example.ui.components.StudioCanvas
import com.example.ui.components.StudioTimeline
import com.example.ui.components.StudioTopBar
import com.example.ui.components.TemplateMarketplacePanel
import com.example.ui.components.pressScaleClickable
import com.example.ui.components.verticalScrollbar
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGradient
import com.example.ui.theme.NeonTriGradient
import com.example.ui.theme.OmniTheme
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.OmniModule
import com.example.viewmodel.StudioTab
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must initialize App Check before any Firebase / ViewModel calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        intent?.data?.let { uri ->
            viewModel.handleCheckoutCallbackUri(uri)
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = state.isDarkTheme) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val credentialManager = remember { CredentialManager.create(context) }

                var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

                DisposableEffect(Unit) {
                    val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                        currentUser = auth.currentUser
                        val user = auth.currentUser
                        if (user != null) {
                            viewModel.syncUserProfileFromCloud(
                                userId = user.uid,
                                displayName = user.displayName ?: "OmniStudio Creator",
                                email = user.email ?: ""
                            )
                        }
                    }
                    Firebase.auth.addAuthStateListener(authStateListener)
                    onDispose {
                        Firebase.auth.removeAuthStateListener(authStateListener)
                    }
                }

                LaunchedEffect(currentUser?.uid) {
                    currentUser?.let { user ->
                        viewModel.syncUserProfileFromCloud(
                            userId = user.uid,
                            displayName = user.displayName ?: "OmniStudio Creator",
                            email = user.email ?: ""
                        )
                    }
                }

                if (currentUser == null) {
                    AuthScreen(
                        isDarkTheme = state.isDarkTheme,
                        onToggleTheme = { viewModel.toggleTheme() },
                        onAuthSuccess = { uid, name, mail ->
                            currentUser = Firebase.auth.currentUser
                            viewModel.syncUserProfileFromCloud(uid, name, mail)
                        }
                    )
                } else {
                    OmniStudioApp(
                        viewModel = viewModel,
                        onSignOut = {
                            signOutUser(
                                credentialManager = credentialManager,
                                scope = scope,
                                onComplete = {
                                    currentUser = null
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { callbackUri ->
            viewModel.handleCheckoutCallbackUri(callbackUri) { success, message ->
                Toast.makeText(
                    this,
                    if (success) "Subscription verified & unlocked!" else (message ?: "Payment verification pending."),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

private fun launchCheckoutUrl(context: android.content.Context, checkoutUrl: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Unable to open payment browser window.", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniStudioApp(
    viewModel: StudioViewModel,
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExportDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val quickPhotoToAdLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.oneTapPhotoToAd(uri.toString(), "Quick Ad Product")
            Toast.makeText(context, "Photo converted into 9:16 social ad!", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = !drawerState.isOpen && state.activeModule != OmniModule.STUDIO && state.activeBottomSheetTab == null) {
        viewModel.setActiveModule(OmniModule.STUDIO)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProfileDrawerSidebar(
                userProfile = state.user,
                activeModule = state.activeModule,
                isDarkTheme = state.isDarkTheme,
                onToggleTheme = { dark -> viewModel.setDarkTheme(dark) },
                onSelectModule = { module -> viewModel.setActiveModule(module) },
                onOpenPaywall = { viewModel.openProUpgradeModal() },
                onSaveToCloud = {
                    viewModel.saveCurrentCampaignToCloud { success, err ->
                        Toast.makeText(
                            context,
                            if (success) "Campaign synced to Firestore!" else (err ?: "Could not save campaign."),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onSignOut = onSignOut,
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioBackground)
                .statusBarsPadding(),
            containerColor = StudioBackground,
            bottomBar = {
                OmniBottomNavBar(
                    activeModule = state.activeModule,
                    onSelectModule = { viewModel.setActiveModule(it) }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Unified OmniStudio Top Bar
                StudioTopBar(
                    aspectRatio = state.aspectRatio,
                    onRatioSelected = { viewModel.setAspectRatio(it) },
                    currentFramework = state.adFramework,
                    onFrameworkSelected = { viewModel.setAdFramework(it) },
                    userProfile = state.user,
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    zoomLevel = state.zoomLevel,
                    onZoomSelected = { viewModel.setZoomLevel(it) },
                    onExportClicked = { showExportDialog = true },
                    onSaveCloudClicked = {
                        viewModel.saveCurrentCampaignToCloud { success, err ->
                            Toast.makeText(
                                context,
                                if (success) "Campaign synced to Firestore!" else (err ?: "Could not save campaign."),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onSignOutClicked = onSignOut,
                    onOpenUpgradeClicked = { viewModel.openProUpgradeModal() },
                    onOpenProfileSidebar = { scope.launch { drawerState.open() } },
                    isDarkTheme = state.isDarkTheme,
                    onToggleTheme = { viewModel.toggleTheme() }
                )

            // 2. Active Module Viewport
            Crossfade(
                targetState = state.activeModule,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "omni_module_switch"
            ) { module ->
                when (module) {
                    OmniModule.STUDIO -> {
                        StudioWorkspaceContent(
                            state = state,
                            viewModel = viewModel,
                            onOneTapPhotoToAd = {
                                quickPhotoToAdLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onOpenExport = { showExportDialog = true }
                        )
                    }

                    OmniModule.DIRECTOR -> {
                        DirectorChatScreen(
                            messages = state.directorMessages,
                            isThinking = state.isDirectorThinking,
                            aspectRatio = state.aspectRatio,
                            motionTemplate = state.motionTemplate,
                            totalDurationSec = state.totalDurationSec,
                            clipCount = state.timelineClips.size,
                            subscriptionTier = state.subscriptionTier,
                            creditsRemaining = state.aiCreditsRemaining,
                            onSendPrompt = { prompt, uri, name ->
                                viewModel.sendDirectorPrompt(prompt, uri, name)
                            },
                            onJumpToStudio = {
                                viewModel.setActiveModule(OmniModule.STUDIO)
                            },
                            onOpenUpgradeModal = { viewModel.openProUpgradeModal() }
                        )
                    }

                    OmniModule.MUSE_AI -> {
                        MuseGeneratorScreen(
                            museGenState = state.museGenState,
                            generatedClips = state.museGeneratedClips,
                            subscriptionTier = state.subscriptionTier,
                            creditsRemaining = state.aiCreditsRemaining,
                            onGenerateClicked = { prompt, camera, pan, zoom, tilt, ratio, audioCue, batchCount, refUri ->
                                viewModel.generateMuseVideos(
                                    prompt = prompt,
                                    cameraMovement = camera,
                                    panAmount = pan,
                                    zoomFactor = zoom,
                                    tiltAngle = tilt,
                                    aspectRatio = ratio,
                                    audioCue = audioCue,
                                    batchCount = batchCount,
                                    referenceUri = refUri
                                )
                            },
                            onImportClipToTimeline = { clip ->
                                viewModel.importMuseClipToTimeline(clip)
                                Toast.makeText(context, "Injected \"${clip.title}\" onto Multi-Track Timeline!", Toast.LENGTH_SHORT).show()
                            },
                            onOpenUpgradeModal = { viewModel.openProUpgradeModal() }
                        )
                    }

                    OmniModule.BG_REMOVAL -> {
                        BgRemovalScreen(
                            bgState = state.videoBgState,
                            onPickVideo = { uri -> viewModel.loadVideoForBgRemoval(uri) },
                            onScrubPosition = { timeMs -> viewModel.scrubVideoBgFrame(timeMs) },
                            onModeChanged = { mode -> viewModel.updateBgRemovalMode(mode) },
                            onChromaKeyChanged = { color -> viewModel.updateBgChromaColor(color) },
                            onSensitivityChanged = { value -> viewModel.updateBgSensitivity(value) },
                            onFeatherChanged = { value -> viewModel.updateBgFeather(value) },
                            onReplacementChanged = { type -> viewModel.updateBgReplacementType(type) },
                            onSolidColorChanged = { argb -> viewModel.updateBgSolidColor(argb) },
                            onCustomBackdropPicked = { uri -> viewModel.loadCustomBackdropImage(uri) },
                            onInjectToStudioTimeline = {
                                viewModel.injectBgRemovedClipToTimeline()
                                Toast.makeText(context, "Cutout video clip added to Studio Timeline!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    OmniModule.FEED -> {
                        HistoryPanel(
                            campaigns = state.campaignHistory,
                            isLoading = state.isHistoryLoading,
                            onLoadCampaign = { campaign ->
                                viewModel.loadCampaignIntoCanvas(campaign)
                                Toast.makeText(context, "Loaded \"${campaign.name}\" into Studio!", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteCampaign = { campaignId ->
                                Firebase.auth.currentUser?.uid?.let { uid ->
                                    viewModel.deleteCampaign(uid, campaignId)
                                }
                            },
                            onRefresh = {
                                Firebase.auth.currentUser?.uid?.let { uid ->
                                    viewModel.fetchUserCampaigns(uid)
                                }
                            },
                            onOneTapPhotoToAd = { uri, name ->
                                viewModel.oneTapPhotoToAd(uri, name)
                                Toast.makeText(context, "Photo converted to 9:16 social ad!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
        }

        // Draggable Bottom Sheet for Studio Tool Suites
        val sheetTab = state.activeBottomSheetTab
        if (sheetTab != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissToolSheet() },
                sheetState = sheetState,
                containerColor = StudioSurface,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .heightIn(min = 320.dp, max = 580.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sheetTab.label.uppercase(),
                            color = OmniTheme.colors.accentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = { viewModel.dismissToolSheet() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Panel", tint = TextSecondary)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (sheetTab) {
                            StudioTab.TEMPLATES -> {
                                TemplateMarketplacePanel(
                                    selectedProduct = state.selectedProduct,
                                    onProductSelected = { viewModel.selectProduct(it) },
                                    onTemplateSelected = { viewModel.applyTemplate(it) },
                                    onImportCustomProduct = { uri, name -> viewModel.importCustomProductImage(uri, name) }
                                )
                            }
                            StudioTab.AI_COPY -> {
                                AiCopyGeneratorPanel(
                                    selectedCategory = state.aiCategory,
                                    onCategorySelected = { viewModel.setAiCategory(it) },
                                    copy = state.generatedCopy,
                                    onAddTextToCanvas = { text, isHead -> viewModel.addTextLayer(text, isHead) },
                                    onAddBadgeToCanvas = { badge -> viewModel.addBadgeLayer(badge) },
                                    onSpeakScript = { script -> viewModel.speakScript(script) }
                                )
                            }
                            StudioTab.MOTION -> {
                                MotionTemplatePanel(
                                    selectedMotion = state.motionTemplate,
                                    onMotionSelected = { viewModel.setMotionTemplate(it) },
                                    motionSpeed = state.motionSpeed,
                                    onSpeedChanged = { viewModel.setMotionSpeed(it) },
                                    motionIntensity = state.motionIntensity,
                                    onIntensityChanged = { viewModel.setMotionIntensity(it) }
                                )
                            }
                            StudioTab.PHOTO_LIGHTING -> {
                                PhotoAndLightingPanel(
                                    selectedBgPreset = state.bgPreset,
                                    onBgPresetSelected = { viewModel.setBackgroundPreset(it) },
                                    customPrompt = state.customBgPrompt,
                                    onCustomPromptSubmit = { viewModel.setCustomBgPrompt(it) },
                                    lighting = state.lighting,
                                    onUpdateLighting = { viewModel.updateLighting(it) },
                                    onTriggerAiCutout = {
                                        viewModel.cutoutProcessor.performAiAutoCutout()
                                        viewModel.updateLayer("prod_layer") { it.copy(isCutoutApplied = true) }
                                    },
                                    onImportCustomProduct = { uri, name -> viewModel.importCustomProductImage(uri, name) }
                                )
                            }
                            StudioTab.LAYERS -> {
                                LayerManagerPanel(
                                    layers = state.layers,
                                    selectedLayerId = state.selectedLayerId,
                                    onSelectLayer = { viewModel.selectLayer(it) },
                                    onUpdateLayer = { id, updater -> viewModel.updateLayer(id, updater) },
                                    onDeleteLayer = { viewModel.deleteLayer(it) },
                                    onBringForward = { viewModel.bringLayerForward(it) },
                                    onSendBackward = { viewModel.sendLayerBackward(it) },
                                    onAddTextLayer = { text, isHead -> viewModel.addTextLayer(text, isHead) },
                                    onAddBadgeLayer = { badge -> viewModel.addBadgeLayer(badge) }
                                )
                            }
                            StudioTab.AUDIO_VOICE -> {
                                AudioVoicePanel(
                                    selectedTrack = state.selectedAudioTrack,
                                    onTrackSelected = { viewModel.selectAudioTrack(it) },
                                    isPlayingMusic = state.isPlaying,
                                    onToggleMusic = { viewModel.togglePlayPause() },
                                    bgmVolume = state.bgmVolume,
                                    onVolumeChanged = { viewModel.setBgmVolume(it) },
                                    voicePitch = state.voicePitch,
                                    onPitchChanged = { viewModel.setVoicePitch(it) },
                                    voiceSpeed = state.voiceSpeed,
                                    onSpeedChanged = { viewModel.setVoiceSpeed(it) },
                                    onTestVoice = {
                                        viewModel.speakScript(state.generatedCopy.hooks.firstOrNull() ?: "Experience ultimate performance.")
                                    },
                                    onStopVoice = { viewModel.stopVoiceover() }
                                )
                            }
                            StudioTab.BATCH -> {
                                BatchCreatorPanel(
                                    batchItems = state.batchItems,
                                    isBatchRunning = state.isBatchRunning,
                                    onStartBatch = { viewModel.startBatchGeneration() }
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // Export Dialog
        if (showExportDialog) {
            ExportDialog(
                layers = state.layers,
                bgPreset = state.bgPreset,
                lighting = state.lighting,
                aspectRatio = state.aspectRatio,
                subscriptionTier = state.subscriptionTier,
                onUpgradeToPro = {
                    showExportDialog = false
                    viewModel.openProUpgradeModal()
                },
                onSaveToCloud = {
                    viewModel.saveCurrentCampaignToCloud { success, _ ->
                        if (success) {
                            Toast.makeText(context, "Saved to OmniStudio Feed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = { showExportDialog = false }
            )
        }

        // Tiered Monetization Paywall Gateway Modal (Stripe / Flutterwave Routing)
        if (state.showProUpgradeModal) {
            ProUpgradeModal(
                currentTier = state.subscriptionTier,
                currentBillingCycle = state.billingCycle,
                creditsRemaining = state.aiCreditsRemaining,
                userEmail = state.user.email,
                paywallReason = state.paywallReason,
                isProcessingPayment = state.isProcessingPayment,
                paymentError = state.paymentError,
                selectedProvider = state.selectedPaymentProvider,
                activeCheckoutSession = state.activeCheckoutSession,
                isPaymentBackendConfigured = viewModel.isPaymentBackendConfigured(),
                isFlutterwaveConfigured = viewModel.isFlutterwaveDirectConfigured(),
                isStripeLinkConfigured = viewModel.isStripePaymentLinkConfigured(),
                onSelectProvider = { provider -> viewModel.selectPaymentProvider(provider) },
                onStartPlanCheckout = { plan, cycle, provider, email ->
                    viewModel.initiatePlanCheckout(
                        plan = plan,
                        billingCycle = cycle,
                        provider = provider,
                        billingEmail = email,
                        onLaunchCheckoutUrl = { checkoutUrl ->
                            launchCheckoutUrl(context, checkoutUrl)
                        }
                    )
                },
                onReopenCheckoutUrl = { url ->
                    launchCheckoutUrl(context, url)
                },
                onVerifyPendingCheckout = {
                    viewModel.verifyPendingCheckoutSession { success, msg ->
                        Toast.makeText(
                            context,
                            if (success) "Payment verified! Tier unlocked." else (msg ?: "Verification pending."),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onDowngradeToFree = {
                    viewModel.downgradeToFreeTier()
                    Toast.makeText(context, "Switched to Free Tier.", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { viewModel.dismissProUpgradeModal() }
            )
        }

        // In-App Flutterwave Inline v3 WebView Checkout Modal
        val inlineWebSession = state.activeInlineWebViewSession
        if (inlineWebSession != null) {
            FlutterwaveCheckoutModal(
                session = inlineWebSession,
                onPaymentCompleted = { status, txId, txRef ->
                    viewModel.handleFlutterwaveInlineSuccess(
                        session = inlineWebSession,
                        status = status,
                        transactionId = txId,
                        txRef = txRef
                    ) { success, msg ->
                        Toast.makeText(
                            context,
                            if (success) "${inlineWebSession.plan.badgeTitle} unlocked!" else (msg ?: "Payment could not be completed."),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onDeepLinkIntercepted = { uri ->
                    viewModel.handleCheckoutCallbackUri(uri) { success, msg ->
                        Toast.makeText(
                            context,
                            if (success) "Subscription verified & unlocked!" else (msg ?: "Payment verification pending."),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onDismiss = { viewModel.dismissFlutterwaveInlineModal() }
            )
        }
    }
}

@Composable
private fun StudioWorkspaceContent(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onOneTapPhotoToAd: () -> Unit,
    onOpenExport: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val canvasHeight = (maxHeight * 0.58f).coerceIn(280.dp, 460.dp)
        val workspaceScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(workspaceScrollState)
                .verticalScrollbar(workspaceScrollState)
        ) {
            // 1. Immersive Interactive Canvas Preview (Bounded responsive height so entire workspace scrolls)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
            ) {
                StudioCanvas(
                    aspectRatio = state.aspectRatio,
                    zoomLevel = state.zoomLevel,
                    bgPreset = state.bgPreset,
                    motionTemplate = state.motionTemplate,
                    motionSpeed = state.motionSpeed,
                    motionIntensity = state.motionIntensity,
                    lighting = state.lighting,
                    layers = state.layers,
                    selectedLayerId = state.selectedLayerId,
                    playheadSec = state.playheadSec,
                    isPlaying = state.isPlaying,
                    onSelectLayer = { viewModel.selectLayer(it) },
                    onUpdateLayer = { id, updater -> viewModel.updateLayer(id, updater) },
                    onFinishTransform = { viewModel.finishLayerTransform() },
                    onDuplicateLayer = { viewModel.duplicateLayer(it) },
                    onDeleteLayer = { viewModel.deleteLayer(it) },
                    onBringForward = { viewModel.bringLayerForward(it) },
                    onSendBackward = { viewModel.sendLayerBackward(it) },
                    subscriptionTier = state.subscriptionTier
                )
            }

            // 2. Hybrid CapCut-Style Multi-Track Timeline
            StudioTimeline(
                clips = state.timelineClips,
                selectedClipId = state.selectedClipId,
                playheadSec = state.playheadSec,
                totalDurationSec = state.totalDurationSec,
                isPlaying = state.isPlaying,
                onTogglePlay = { viewModel.togglePlayPause() },
                onSeekPlayhead = { viewModel.seekPlayhead(it) },
                onSelectClip = { viewModel.selectClip(it) },
                onSplitClip = { viewModel.splitClipAtPlayhead() },
                onTrimClip = { viewModel.trimSelectedClip() },
                onUpdateClip = { id, start, dur -> viewModel.updateClip(id, start, dur) }
            )

            // 3. Thumb-First Tool Strip (Triggers Native Draggable Bottom Sheets + 1-Tap Photo-to-Ad)
            StudioToolStrip(
                activeSheetTab = state.activeBottomSheetTab,
                onOneTapPhotoToAd = onOneTapPhotoToAd,
                onOpenTool = { tab ->
                    if (tab == StudioTab.EXPORT) {
                        onOpenExport()
                    } else {
                        viewModel.openToolSheet(tab)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private data class ToolChipItem(val tab: StudioTab, val icon: ImageVector)

@Composable
private fun StudioToolStrip(
    activeSheetTab: StudioTab?,
    onOneTapPhotoToAd: () -> Unit,
    onOpenTool: (StudioTab) -> Unit
) {
    val tools = listOf(
        ToolChipItem(StudioTab.TEMPLATES, Icons.Default.Dashboard),
        ToolChipItem(StudioTab.MOTION, Icons.Default.PlayCircle),
        ToolChipItem(StudioTab.AI_COPY, Icons.Default.TextFields),
        ToolChipItem(StudioTab.PHOTO_LIGHTING, Icons.Default.Lightbulb),
        ToolChipItem(StudioTab.LAYERS, Icons.Default.Layers),
        ToolChipItem(StudioTab.AUDIO_VOICE, Icons.Default.MusicNote),
        ToolChipItem(StudioTab.BATCH, Icons.Default.AutoAwesome),
        ToolChipItem(StudioTab.EXPORT, Icons.Default.FileDownload)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioBackground)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1-Tap E-Commerce Photo-to-Ad Converter Pill
        Row(
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(NeonGradient)
                .pressScaleClickable { onOneTapPhotoToAd() }
                .padding(horizontal = 12.dp)
                .testTag("one_tap_photo_to_ad_btn"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "1-Tap Photo-to-Ad",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "1-Tap Ad",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        tools.forEach { item ->
            val isSelected = item.tab == activeSheetTab
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) StudioSurfaceElevated else StudioSurface)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) OmniTheme.colors.accentPurple else StudioBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .pressScaleClickable { onOpenTool(item.tab) }
                    .padding(horizontal = 12.dp)
                    .testTag("tool_chip_${item.tab.name}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.tab.label,
                    tint = if (isSelected) OmniTheme.colors.accentCyan else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.tab.label,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private data class NavDestination(
    val module: OmniModule,
    val icon: ImageVector,
    val label: String
)

@Composable
private fun OmniBottomNavBar(
    activeModule: OmniModule,
    onSelectModule: (OmniModule) -> Unit
) {
    val destinations = listOf(
        NavDestination(OmniModule.STUDIO, Icons.Default.Movie, "Studio"),
        NavDestination(OmniModule.DIRECTOR, Icons.Default.Psychology, "Director"),
        NavDestination(OmniModule.MUSE_AI, Icons.Default.MovieCreation, "Muse AI"),
        NavDestination(OmniModule.BG_REMOVAL, Icons.Default.AutoFixHigh, "Bg Removal"),
        NavDestination(OmniModule.FEED, Icons.Default.DynamicFeed, "Feed")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(StudioSurface)
                .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 5.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { dest ->
                val isSelected = activeModule == dest.module
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.02f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "nav_scale_${dest.module.name}"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .scale(scale)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            if (isSelected) OmniTheme.colors.accentPurple.copy(alpha = 0.14f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) OmniTheme.colors.accentPurple.copy(alpha = 0.45f) else Color.Transparent,
                            shape = RoundedCornerShape(15.dp)
                        )
                        .pressScaleClickable { onSelectModule(dest.module) }
                        .padding(horizontal = 2.dp, vertical = 4.dp)
                        .testTag("nav_${dest.module.name}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.label,
                        tint = if (isSelected) OmniTheme.colors.accentPurple else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dest.label,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
