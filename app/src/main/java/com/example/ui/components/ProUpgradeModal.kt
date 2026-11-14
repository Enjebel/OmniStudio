package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.CheckoutSessionInit
import com.example.data.repository.PaymentProvider
import com.example.model.BillingCycle
import com.example.model.SubscriptionPlan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OmniTheme
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ProUpgradeModal(
    currentTier: String,
    currentBillingCycle: String,
    creditsRemaining: Int,
    userEmail: String,
    paywallReason: String?,
    isProcessingPayment: Boolean,
    paymentError: String?,
    selectedProvider: PaymentProvider,
    activeCheckoutSession: CheckoutSessionInit?,
    isPaymentBackendConfigured: Boolean,
    isFlutterwaveConfigured: Boolean = false,
    isStripeLinkConfigured: Boolean = false,
    isStripePublishableKeyConfigured: Boolean = false,
    onSelectProvider: (PaymentProvider) -> Unit,
    onStartPlanCheckout: (
        plan: SubscriptionPlan,
        billingCycle: BillingCycle,
        provider: PaymentProvider,
        billingEmail: String
    ) -> Unit,
    onReopenCheckoutUrl: (String) -> Unit,
    onVerifyPendingCheckout: () -> Unit,
    onDowngradeToFree: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = OmniTheme.colors
    val scrollState = rememberScrollState()
    val currentPlan = SubscriptionPlan.fromTierId(currentTier)

    var selectedCycle by remember {
        mutableStateOf(
            if (currentBillingCycle.equals("YEARLY", ignoreCase = true)) BillingCycle.YEARLY
            else BillingCycle.MONTHLY
        )
    }
    var selectedPlan by remember {
        mutableStateOf(
            when (currentPlan) {
                SubscriptionPlan.FREE, SubscriptionPlan.BASIC -> SubscriptionPlan.PRO
                else -> currentPlan
            }
        )
    }
    var billingEmail by remember(userEmail) { mutableStateOf(userEmail.ifBlank { "enjebeln@gmail.com" }) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(22.dp))
                .testTag("paywall_subscription_modal"),
            color = StudioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .verticalScroll(scrollState)
                    .verticalScrollbar(scrollState)
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.accentPurple.copy(alpha = 0.14f))
                            .border(1.dp, palette.accentPurple.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = palette.accentPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "OMNISTUDIO PRO PLANS",
                            color = palette.accentPurple,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioSurfaceElevated)
                            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Paywall", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Context Banner if user exhausted daily credits or hit a locked gate
                if (!paywallReason.isNullOrBlank() || creditsRemaining <= 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.accentGold.copy(alpha = 0.12f))
                            .border(1.dp, palette.accentGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = palette.accentGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = paywallReason ?: "You've reached your daily AI limit. Choose a plan below to unlock instant access.",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "Upgrade Your Creator Suite",
                    color = TextPrimary,
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ENJEBEL TECH • Instant Cloud Unlock via Verified Webhook",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Billing Cycle Segmented Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BillingCycle.entries.forEach { cycle ->
                        val isSel = selectedCycle == cycle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSel) palette.accentPurple else Color.Transparent)
                                .pressScaleClickable { selectedCycle = cycle }
                                .testTag("billing_cycle_${cycle.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cycle.label,
                                    color = if (isSel) Color.White else TextSecondary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                if (cycle == BillingCycle.YEARLY) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CyberGreen)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "SAVE 20%",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Payment Provider Routing Selector (Stripe / Flutterwave)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PaymentProvider.entries.forEach { provider ->
                        val isSel = selectedProvider == provider
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSel) StudioSurface else Color.Transparent)
                                .border(
                                    width = if (isSel) 1.dp else 0.dp,
                                    color = if (isSel) palette.accentCyan else Color.Transparent,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .pressScaleClickable {
                                    localValidationError = null
                                    onSelectProvider(provider)
                                }
                                .padding(vertical = 8.dp, horizontal = 6.dp)
                                .testTag("provider_${provider.name}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = provider.displayName,
                                color = if (isSel) TextPrimary else TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = provider.subtitle,
                                color = TextMuted,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Billing Email Input
                OutlinedTextField(
                    value = billingEmail,
                    onValueChange = {
                        billingEmail = it
                        localValidationError = null
                    },
                    label = { Text("Billing Receipt Email", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paywall_billing_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accentPurple,
                        unfocusedBorderColor = StudioBorder,
                        focusedContainerColor = StudioSurfaceElevated,
                        unfocusedContainerColor = StudioSurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Active Checkout Session Verification Banner (after opening Stripe/Flutterwave)
                if (activeCheckoutSession != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.accentCyan.copy(alpha = 0.10f))
                            .border(1.dp, palette.accentCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = palette.accentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${activeCheckoutSession.provider.displayName} Session Active (${activeCheckoutSession.plan.badgeTitle})",
                                color = TextPrimary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text(
                            text = if (isPaymentBackendConfigured) {
                                "Complete payment in your browser. Once webhook confirms, your account unlocks automatically — or tap Verify below."
                            } else {
                                "Complete payment in your browser. When payment succeeds, your checkout redirect will unlock your account."
                            },
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onReopenCheckoutUrl(activeCheckoutSession.checkoutUrl) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Page", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (isPaymentBackendConfigured) {
                                Button(
                                    onClick = onVerifyPendingCheckout,
                                    enabled = !isProcessingPayment,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("verify_pending_checkout_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentCyan)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verify Status", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Inline Validation / Payment Error or Provider-Specific Configuration Status
                val combinedError = localValidationError ?: paymentError
                if (combinedError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.errorContainer)
                            .border(1.dp, DangerRed.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(combinedError, color = palette.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    val isProviderReady = when (selectedProvider) {
                        PaymentProvider.FLUTTERWAVE -> isPaymentBackendConfigured || isFlutterwaveConfigured
                        PaymentProvider.STRIPE -> isPaymentBackendConfigured || isStripeLinkConfigured
                    }
                    val statusText = when {
                        selectedProvider == PaymentProvider.FLUTTERWAVE && isPaymentBackendConfigured ->
                            "Flutterwave server checkout ready."
                        selectedProvider == PaymentProvider.FLUTTERWAVE && isFlutterwaveConfigured ->
                            "Flutterwave Inline v3 ready • Opens secure in-app checkout (no backend server required)."
                        selectedProvider == PaymentProvider.FLUTTERWAVE ->
                            "Add FLUTTERWAVE_PUBLIC_KEY (no backend needed) or PAYMENT_BACKEND_URL in the AI Studio Secrets panel."
                        selectedProvider == PaymentProvider.STRIPE && isPaymentBackendConfigured && isStripePublishableKeyConfigured ->
                            "Stripe PaymentSheet & Backend ready • Secure PRO upgrades via encrypted card checkout."
                        selectedProvider == PaymentProvider.STRIPE && isPaymentBackendConfigured ->
                            "Stripe server checkout ready • Add STRIPE_PUBLISHABLE_KEY for native PaymentSheet."
                        selectedProvider == PaymentProvider.STRIPE && isStripeLinkConfigured ->
                            "Stripe Payment Link ready • Opens hosted checkout in browser."
                        else ->
                            "Add PAYMENT_BACKEND_URL and STRIPE_PUBLISHABLE_KEY (or STRIPE_PAYMENT_LINK_URL) in the AI Studio Secrets panel."
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isProviderReady) CyberGreen.copy(alpha = 0.10f)
                                else StudioSurfaceElevated
                            )
                            .border(
                                width = 1.dp,
                                color = if (isProviderReady) CyberGreen.copy(alpha = 0.45f) else StudioBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(10.dp)
                            .testTag("provider_config_status_banner"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isProviderReady) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isProviderReady) CyberGreen else palette.accentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            color = if (isProviderReady) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Structured Plan Cards (Basic $5, Pro $15, Premium $20) with direct Checkout Action
                val paidPlans = listOf(SubscriptionPlan.BASIC, SubscriptionPlan.PRO, SubscriptionPlan.PREMIUM)
                paidPlans.forEach { plan ->
                    val isSelected = selectedPlan == plan
                    val isCurrentActive = currentPlan == plan
                    val effectiveMonthly = if (selectedCycle == BillingCycle.YEARLY) plan.yearlyMonthlyPriceUsd else plan.monthlyPriceUsd
                    val chargeDisplay = if (selectedCycle == BillingCycle.YEARLY) {
                        String.format(Locale.US, "$%.0f/yr", plan.yearlyTotalUsd)
                    } else {
                        String.format(Locale.US, "$%.0f/mo", plan.monthlyPriceUsd)
                    }
                    val isPopular = plan == SubscriptionPlan.PRO

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) StudioSurfaceElevated else StudioSurface)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) palette.accentPurple else StudioBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedPlan = plan }
                            .padding(14.dp)
                            .testTag("plan_card_${plan.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = plan.displayName,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (isPopular) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(palette.accentPurple.copy(alpha = 0.16f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("POPULAR", color = palette.accentPurple, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (isCurrentActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(CyberGreen.copy(alpha = 0.16f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("ACTIVE", color = CyberGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = plan.tagline,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "$%.0f/mo", effectiveMonthly),
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (selectedCycle == BillingCycle.YEARLY) {
                                        "${plan.yearlySavingsLabel} ($${plan.yearlyTotalUsd.toInt()}/yr)"
                                    } else {
                                        "Billed monthly"
                                    },
                                    color = if (selectedCycle == BillingCycle.YEARLY) CyberGreen else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = StudioBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        plan.features.forEach { feat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CyberGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = feat,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                selectedPlan = plan
                                val trimmedEmail = billingEmail.trim()
                                if (!trimmedEmail.contains("@") || trimmedEmail.length < 5) {
                                    localValidationError = "Enter a valid billing email address to create a checkout receipt."
                                } else {
                                    localValidationError = null
                                    onStartPlanCheckout(plan, selectedCycle, selectedProvider, trimmedEmail)
                                }
                            },
                            enabled = !isProcessingPayment,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("checkout_plan_btn_${plan.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) palette.accentPurple else StudioSurfaceElevated,
                                contentColor = if (isSelected) Color.White else TextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            if (isProcessingPayment && isSelected) {
                                CircularProgressIndicator(
                                    color = if (isSelected) Color.White else palette.accentPurple,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Opening ${selectedProvider.displayName}...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = if (isCurrentActive) Icons.Default.CheckCircle else Icons.Default.CreditCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCurrentActive) {
                                        "Active Plan • Renew via ${selectedProvider.displayName}"
                                    } else {
                                        "Pay $chargeDisplay with ${selectedProvider.displayName}"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (currentPlan != SubscriptionPlan.FREE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Downgrade back to Free Starter",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clickable { onDowngradeToFree() }
                            .padding(8.dp)
                            .testTag("downgrade_free_btn")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Encrypted Stripe / Flutterwave Checkout • ENJEBEL TECH",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
