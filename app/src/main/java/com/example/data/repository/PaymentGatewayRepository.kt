package com.example.data.repository

import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.model.BillingCycle
import com.example.model.SubscriptionPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class PaymentProvider(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    STRIPE("STRIPE", "Stripe", "Cards • Apple Pay • Google Pay"),
    FLUTTERWAVE("FLUTTERWAVE", "Flutterwave", "Cards • Mobile Money • Bank")
}

enum class CheckoutLaunchMode {
    EXTERNAL_BROWSER_URL,
    FLUTTERWAVE_INLINE_WEBVIEW,
    STRIPE_PAYMENT_SHEET
}

data class StripePaymentSheetPayload(
    val publishableKey: String,
    val paymentIntentClientSecret: String,
    val customerId: String? = null,
    val ephemeralKeySecret: String? = null
)

data class CheckoutSessionInit(
    val sessionId: String,
    val checkoutUrl: String,
    val provider: PaymentProvider,
    val plan: SubscriptionPlan,
    val billingCycle: BillingCycle,
    val amountUsd: Double,
    val billingEmail: String,
    val launchMode: CheckoutLaunchMode = CheckoutLaunchMode.EXTERNAL_BROWSER_URL,
    val inlinePublicKey: String = "",
    val customerName: String = "OmniStudio Creator",
    val stripePaymentSheet: StripePaymentSheetPayload? = null
)

class VerificationEndpointUnavailableException(message: String) : IllegalStateException(message)

data class CheckoutVerificationResult(
    val verified: Boolean,
    val transactionRef: String,
    val plan: SubscriptionPlan,
    val billingCycle: BillingCycle,
    val provider: PaymentProvider,
    val message: String? = null
)

data class DeepLinkCheckoutCallback(
    val status: String,
    val sessionId: String,
    val transactionRef: String,
    val plan: SubscriptionPlan?,
    val billingCycle: BillingCycle?,
    val provider: PaymentProvider?
) {
    val isSuccessful: Boolean
        get() = status.lowercase(Locale.US) in setOf("successful", "completed", "paid", "succeeded", "success")

    val isCancelled: Boolean
        get() = status.lowercase(Locale.US) in setOf("cancelled", "canceled", "failed")
}

class PaymentGatewayRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "PaymentGatewayRepo"
        const val CALLBACK_DEEP_LINK = "omnistudio://checkout/callback"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private fun cleanSecret(raw: String): String =
        raw.trim().trim('"', '\'').trim()

    private fun normalizeHttpUrl(raw: String): String {
        val cleaned = cleanSecret(raw)
        if (cleaned.isBlank() || cleaned.startsWith("YOUR_")) return ""
        return when {
            cleaned.startsWith("https://", ignoreCase = true) ||
                cleaned.startsWith("http://", ignoreCase = true) -> cleaned.trimEnd('/')
            cleaned.contains(".") && !cleaned.contains(" ") -> "https://${cleaned.trimEnd('/')}"
            else -> ""
        }
    }

    fun isBackendConfigured(): Boolean = normalizeHttpUrl(BuildConfig.PAYMENT_BACKEND_URL).isNotBlank()

    fun getStripePublishableKey(): String {
        val key = cleanSecret(BuildConfig.STRIPE_PUBLISHABLE_KEY)
        return key.takeIf { it.isNotBlank() && !it.startsWith("YOUR_") }.orEmpty()
    }

    fun getPaymentBackendUrl(): String = normalizeHttpUrl(BuildConfig.PAYMENT_BACKEND_URL)

    fun hasProviderPublicKey(provider: PaymentProvider): Boolean {
        val key = when (provider) {
            PaymentProvider.STRIPE -> cleanSecret(BuildConfig.STRIPE_PUBLISHABLE_KEY)
            PaymentProvider.FLUTTERWAVE -> cleanSecret(BuildConfig.FLUTTERWAVE_PUBLIC_KEY)
        }
        return key.isNotBlank() && !key.startsWith("YOUR_")
    }

    fun isStripePaymentLinkConfigured(): Boolean =
        normalizeHttpUrl(BuildConfig.STRIPE_PAYMENT_LINK_URL).isNotBlank()

    fun isFlutterwaveDirectConfigured(): Boolean =
        hasProviderPublicKey(PaymentProvider.FLUTTERWAVE)

    fun isProviderReady(provider: PaymentProvider): Boolean = when (provider) {
        PaymentProvider.FLUTTERWAVE -> isBackendConfigured() || isFlutterwaveDirectConfigured()
        PaymentProvider.STRIPE -> isBackendConfigured() || isStripePaymentLinkConfigured()
    }

    fun parseBackendSessionPayload(
        json: JSONObject,
        provider: PaymentProvider,
        plan: SubscriptionPlan,
        billingCycle: BillingCycle,
        amountUsd: Double,
        billingEmail: String,
        customerName: String,
        fallbackPublishableKey: String = getStripePublishableKey()
    ): Result<CheckoutSessionInit> {
        val cleanCustomerName = customerName.ifBlank { "OmniStudio Creator" }

        // Check for native Stripe PaymentSheet fields first
        if (provider == PaymentProvider.STRIPE) {
            val clientSecret = json.optString("paymentIntentClientSecret")
                .ifBlank { json.optString("paymentIntent") }
                .ifBlank { json.optString("clientSecret") }
                .ifBlank { json.optString("client_secret") }

            if (clientSecret.isNotBlank()) {
                val publishableKey = cleanSecret(
                    json.optString("publishableKey")
                        .ifBlank { json.optString("publishable_key") }
                ).takeIf { it.isNotBlank() && !it.startsWith("YOUR_") }
                    ?: cleanSecret(fallbackPublishableKey).takeIf { it.isNotBlank() && !it.startsWith("YOUR_") }
                    ?: return Result.failure(
                        IllegalStateException(
                            "STRIPE_PUBLISHABLE_KEY is required for Stripe PaymentSheet. Add it in the AI Studio Secrets panel."
                        )
                    )

                val customerId = json.optString("customer")
                    .ifBlank { json.optString("customerId") }
                    .ifBlank { json.optString("customer_id") }
                    .takeIf { it.isNotBlank() }
                val ephemeralKey = json.optString("ephemeralKey")
                    .ifBlank { json.optString("ephemeralKeySecret") }
                    .ifBlank { json.optString("ephemeral_key") }
                    .takeIf { it.isNotBlank() }

                val piId = json.optString("sessionId")
                    .ifBlank { json.optString("paymentIntentId") }
                    .ifBlank { json.optString("id") }
                    .ifBlank { clientSecret.substringBefore("_secret_").ifBlank { "pi_${UUID.randomUUID().toString().replace("-", "").take(12)}" } }

                return Result.success(
                    CheckoutSessionInit(
                        sessionId = piId,
                        checkoutUrl = "",
                        provider = PaymentProvider.STRIPE,
                        plan = plan,
                        billingCycle = billingCycle,
                        amountUsd = amountUsd,
                        billingEmail = billingEmail,
                        launchMode = CheckoutLaunchMode.STRIPE_PAYMENT_SHEET,
                        inlinePublicKey = publishableKey,
                        customerName = cleanCustomerName,
                        stripePaymentSheet = StripePaymentSheetPayload(
                            publishableKey = publishableKey,
                            paymentIntentClientSecret = clientSecret,
                            customerId = customerId,
                            ephemeralKeySecret = ephemeralKey
                        )
                    )
                )
            }
        }

        // Standard hosted checkout session URL fallback
        val sessionId = json.optString("sessionId").ifBlank { json.optString("id") }
        val checkoutUrl = json.optString("checkoutUrl").ifBlank { json.optString("url") }
        if (sessionId.isBlank() || checkoutUrl.isBlank()) {
            return Result.failure(
                IllegalStateException("Payment backend returned an incomplete checkout session payload.")
            )
        }

        return Result.success(
            CheckoutSessionInit(
                sessionId = sessionId,
                checkoutUrl = checkoutUrl,
                provider = provider,
                plan = plan,
                billingCycle = billingCycle,
                amountUsd = amountUsd,
                billingEmail = billingEmail,
                launchMode = CheckoutLaunchMode.EXTERNAL_BROWSER_URL,
                customerName = cleanCustomerName
            )
        )
    }

    suspend fun createCheckoutSession(
        userId: String,
        email: String,
        plan: SubscriptionPlan,
        billingCycle: BillingCycle,
        provider: PaymentProvider,
        customerName: String = "OmniStudio Creator"
    ): Result<CheckoutSessionInit> = withContext(Dispatchers.IO) {
        if (plan == SubscriptionPlan.FREE) {
            return@withContext Result.failure(IllegalArgumentException("Select Basic, Pro, or Premium to start checkout."))
        }
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
            return@withContext Result.failure(IllegalArgumentException("Enter a valid billing email address."))
        }

        val amountUsd = if (billingCycle == BillingCycle.YEARLY) plan.yearlyTotalUsd else plan.monthlyPriceUsd
        val backendUrl = normalizeHttpUrl(BuildConfig.PAYMENT_BACKEND_URL)

        // 1. Server-backed checkout (supports both Stripe PaymentSheet intents & hosted Checkout URLs)
        if (backendUrl.isNotBlank()) {
            val publicKey = when (provider) {
                PaymentProvider.STRIPE -> getStripePublishableKey()
                PaymentProvider.FLUTTERWAVE -> cleanSecret(BuildConfig.FLUTTERWAVE_PUBLIC_KEY)
            }.takeUnless { it.startsWith("YOUR_") }.orEmpty()

            return@withContext try {
                val payload = JSONObject().apply {
                    put("userId", userId)
                    put("email", cleanEmail)
                    put("customerName", customerName.ifBlank { "OmniStudio Creator" })
                    put("planTier", plan.id)
                    put("billingCycle", billingCycle.id)
                    put("amountUsd", amountUsd)
                    put("amountCents", kotlin.math.round(amountUsd * 100.0).toLong())
                    put("currency", if (provider == PaymentProvider.STRIPE) "usd" else "USD")
                    put("provider", provider.id)
                    put("successUrl", "$CALLBACK_DEEP_LINK?status=successful&plan=${plan.id}&cycle=${billingCycle.id}&provider=${provider.id}")
                    put("cancelUrl", "$CALLBACK_DEEP_LINK?status=cancelled&plan=${plan.id}&cycle=${billingCycle.id}&provider=${provider.id}")
                }

                val endpoints = if (provider == PaymentProvider.STRIPE) {
                    listOf("$backendUrl/checkout/session", "$backendUrl/payment-sheet")
                } else {
                    listOf("$backendUrl/checkout/session")
                }

                var lastError: Throwable? = null
                for ((idx, endpointUrl) in endpoints.withIndex()) {
                    val requestBuilder = Request.Builder()
                        .url(endpointUrl)
                        .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .addHeader("Accept", "application/json")
                    if (publicKey.isNotBlank()) {
                        requestBuilder.addHeader("X-Payment-Public-Key", publicKey)
                    }

                    client.newCall(requestBuilder.build()).execute().use { response ->
                        val rawBody = response.body?.string().orEmpty()
                        if (response.code == 404 && idx < endpoints.lastIndex) {
                            return@use
                        }
                        if (!response.isSuccessful) {
                            val serverMsg = runCatching { JSONObject(rawBody).optString("error") }.getOrNull()
                            lastError = IllegalStateException(
                                serverMsg?.takeIf { it.isNotBlank() }
                                    ?: "Checkout session failed (HTTP ${response.code})."
                            )
                            return@withContext Result.failure(lastError!!)
                        }

                        val json = JSONObject(rawBody)
                        return@withContext parseBackendSessionPayload(
                            json = json,
                            provider = provider,
                            plan = plan,
                            billingCycle = billingCycle,
                            amountUsd = amountUsd,
                            billingEmail = cleanEmail,
                            customerName = customerName,
                            fallbackPublishableKey = publicKey
                        )
                    }
                }

                Result.failure(
                    lastError ?: IllegalStateException("Payment backend endpoint not found (HTTP 404).")
                )
            } catch (e: Exception) {
                Log.e(TAG, "Checkout session error: ${e.message}", e)
                Result.failure(
                    IllegalStateException("Unable to reach payment checkout server: ${e.localizedMessage ?: "Network error"}")
                )
            }
        }

        // 2. Serverless Flutterwave Inline v3 Checkout (uses FLUTTERWAVE_PUBLIC_KEY directly in WebView)
        if (provider == PaymentProvider.FLUTTERWAVE && isFlutterwaveDirectConfigured()) {
            val flwKey = cleanSecret(BuildConfig.FLUTTERWAVE_PUBLIC_KEY)
            val txRef = "omni_${plan.id.lowercase(Locale.US)}_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            return@withContext Result.success(
                CheckoutSessionInit(
                    sessionId = txRef,
                    checkoutUrl = "https://checkout.flutterwave.com/v3.js",
                    provider = PaymentProvider.FLUTTERWAVE,
                    plan = plan,
                    billingCycle = billingCycle,
                    amountUsd = amountUsd,
                    billingEmail = cleanEmail,
                    launchMode = CheckoutLaunchMode.FLUTTERWAVE_INLINE_WEBVIEW,
                    inlinePublicKey = flwKey,
                    customerName = customerName.ifBlank { "OmniStudio Creator" }
                )
            )
        }

        // 3. Serverless Stripe Payment Link Checkout (uses STRIPE_PAYMENT_LINK_URL directly)
        if (provider == PaymentProvider.STRIPE && isStripePaymentLinkConfigured()) {
            val baseLink = normalizeHttpUrl(BuildConfig.STRIPE_PAYMENT_LINK_URL)
            val linkUri = Uri.parse(baseLink).buildUpon()
                .appendQueryParameter("client_reference_id", "${userId}_${plan.id}_${billingCycle.id}")
                .appendQueryParameter("prefilled_email", cleanEmail)
                .build()
                .toString()
            val sessionId = "stripe_link_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            return@withContext Result.success(
                CheckoutSessionInit(
                    sessionId = sessionId,
                    checkoutUrl = linkUri,
                    provider = PaymentProvider.STRIPE,
                    plan = plan,
                    billingCycle = billingCycle,
                    amountUsd = amountUsd,
                    billingEmail = cleanEmail,
                    launchMode = CheckoutLaunchMode.EXTERNAL_BROWSER_URL,
                    customerName = customerName.ifBlank { "OmniStudio Creator" }
                )
            )
        }

        // 4. Actionable setup message tailored to the selected provider
        val missingMsg = when (provider) {
            PaymentProvider.FLUTTERWAVE ->
                "Flutterwave checkout needs FLUTTERWAVE_PUBLIC_KEY (e.g. FLWPUBK_TEST-...) or PAYMENT_BACKEND_URL set in the AI Studio Secrets panel."
            PaymentProvider.STRIPE ->
                "Stripe checkout needs PAYMENT_BACKEND_URL + STRIPE_PUBLISHABLE_KEY (or STRIPE_PAYMENT_LINK_URL) set in the AI Studio Secrets panel."
        }
        Result.failure(IllegalStateException(missingMsg))
    }

    suspend fun verifyCheckoutSession(
        userId: String,
        sessionId: String,
        provider: PaymentProvider,
        expectedPlan: SubscriptionPlan,
        expectedCycle: BillingCycle
    ): Result<CheckoutVerificationResult> = withContext(Dispatchers.IO) {
        val backendUrl = normalizeHttpUrl(BuildConfig.PAYMENT_BACKEND_URL)
        if (backendUrl.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Payment backend URL is required to manually poll receipt verification.")
            )
        }

        try {
            val payload = JSONObject().apply {
                put("userId", userId)
                put("sessionId", sessionId)
                put("provider", provider.id)
                put("expectedPlan", expectedPlan.id)
                put("expectedCycle", expectedCycle.id)
            }

            val request = Request.Builder()
                .url("$backendUrl/checkout/verify")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (response.code == 404) {
                    return@withContext Result.failure(
                        VerificationEndpointUnavailableException("Verification endpoint not configured on backend (HTTP 404).")
                    )
                }
                if (!response.isSuccessful) {
                    val serverMsg = runCatching { JSONObject(rawBody).optString("error") }.getOrNull()
                    return@withContext Result.failure(
                        IllegalStateException(
                            serverMsg?.takeIf { it.isNotBlank() }
                                ?: "Payment verification returned HTTP ${response.code}."
                        )
                    )
                }

                val json = JSONObject(rawBody)
                val verified = json.optBoolean("verified", false) ||
                    json.optString("status").equals("paid", ignoreCase = true) ||
                    json.optString("status").equals("successful", ignoreCase = true) ||
                    json.optString("status").equals("completed", ignoreCase = true)
                val txRef = json.optString("transactionRef")
                    .ifBlank { json.optString("tx_ref") }
                    .ifBlank { sessionId }
                val plan = json.optString("planTier").takeIf { it.isNotBlank() }
                    ?.let { SubscriptionPlan.fromTierId(it) } ?: expectedPlan
                val cycle = if (json.optString("billingCycle").equals("YEARLY", ignoreCase = true)) {
                    BillingCycle.YEARLY
                } else {
                    expectedCycle
                }

                Result.success(
                    CheckoutVerificationResult(
                        verified = verified,
                        transactionRef = txRef,
                        plan = plan,
                        billingCycle = cycle,
                        provider = provider,
                        message = json.optString("message").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Verification error: ${e.message}", e)
            Result.failure(
                IllegalStateException("Payment verification failed: ${e.localizedMessage ?: "Network error"}")
            )
        }
    }

    fun parseDeepLinkCallback(uri: Uri?): DeepLinkCheckoutCallback? {
        if (uri == null) return null
        if (uri.scheme != "omnistudio" || uri.host != "checkout") return null

        val status = uri.getQueryParameter("status").orEmpty()
        val sessionId = uri.getQueryParameter("session_id")
            ?: uri.getQueryParameter("sessionId")
            ?: uri.getQueryParameter("tx_ref")
            ?: ""
        val txRef = uri.getQueryParameter("tx_ref")
            ?: uri.getQueryParameter("transaction_id")
            ?: sessionId
        val plan = uri.getQueryParameter("plan")
            ?.takeIf { it.isNotBlank() }
            ?.let { SubscriptionPlan.fromTierId(it) }
        val cycle = uri.getQueryParameter("cycle")?.let {
            if (it.equals("YEARLY", ignoreCase = true)) BillingCycle.YEARLY else BillingCycle.MONTHLY
        }
        val provider = uri.getQueryParameter("provider")?.let { p ->
            PaymentProvider.entries.find { it.id.equals(p, ignoreCase = true) }
        }

        return DeepLinkCheckoutCallback(
            status = status,
            sessionId = sessionId,
            transactionRef = txRef,
            plan = plan,
            billingCycle = cycle,
            provider = provider
        )
    }
}
