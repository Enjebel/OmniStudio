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
import java.util.concurrent.TimeUnit

enum class PaymentProvider(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    STRIPE("STRIPE", "Stripe", "Cards • Apple Pay • Google Pay"),
    FLUTTERWAVE("FLUTTERWAVE", "Flutterwave", "Cards • Mobile Money • Bank")
}

data class CheckoutSessionInit(
    val sessionId: String,
    val checkoutUrl: String,
    val provider: PaymentProvider,
    val plan: SubscriptionPlan,
    val billingCycle: BillingCycle,
    val amountUsd: Double,
    val billingEmail: String
)

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

    fun isBackendConfigured(): Boolean {
        val backend = BuildConfig.PAYMENT_BACKEND_URL.trim()
        return backend.isNotBlank() &&
            !backend.startsWith("YOUR_") &&
            (backend.startsWith("https://") || backend.startsWith("http://"))
    }

    fun hasProviderPublicKey(provider: PaymentProvider): Boolean {
        val key = when (provider) {
            PaymentProvider.STRIPE -> BuildConfig.STRIPE_PUBLISHABLE_KEY.trim()
            PaymentProvider.FLUTTERWAVE -> BuildConfig.FLUTTERWAVE_PUBLIC_KEY.trim()
        }
        return key.isNotBlank() && !key.startsWith("YOUR_")
    }

    suspend fun createCheckoutSession(
        userId: String,
        email: String,
        plan: SubscriptionPlan,
        billingCycle: BillingCycle,
        provider: PaymentProvider
    ): Result<CheckoutSessionInit> = withContext(Dispatchers.IO) {
        if (plan == SubscriptionPlan.FREE) {
            return@withContext Result.failure(IllegalArgumentException("Select Basic, Pro, or Premium to start checkout."))
        }
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
            return@withContext Result.failure(IllegalArgumentException("Enter a valid billing email address."))
        }

        val backendUrl = BuildConfig.PAYMENT_BACKEND_URL.trim().trimEnd('/')
        if (!isBackendConfigured()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Payment backend URL not configured. Set PAYMENT_BACKEND_URL and your ${provider.displayName} key in the AI Studio Secrets panel."
                )
            )
        }

        val amountUsd = if (billingCycle == BillingCycle.YEARLY) plan.yearlyTotalUsd else plan.monthlyPriceUsd
        val publicKey = when (provider) {
            PaymentProvider.STRIPE -> BuildConfig.STRIPE_PUBLISHABLE_KEY.trim()
            PaymentProvider.FLUTTERWAVE -> BuildConfig.FLUTTERWAVE_PUBLIC_KEY.trim()
        }.takeUnless { it.startsWith("YOUR_") }.orEmpty()

        try {
            val payload = JSONObject().apply {
                put("userId", userId)
                put("email", cleanEmail)
                put("planTier", plan.id)
                put("billingCycle", billingCycle.id)
                put("amountUsd", amountUsd)
                put("currency", "USD")
                put("provider", provider.id)
                put("successUrl", "$CALLBACK_DEEP_LINK?status=successful&plan=${plan.id}&cycle=${billingCycle.id}&provider=${provider.id}")
                put("cancelUrl", "$CALLBACK_DEEP_LINK?status=cancelled&plan=${plan.id}&cycle=${billingCycle.id}&provider=${provider.id}")
            }

            val requestBuilder = Request.Builder()
                .url("$backendUrl/checkout/session")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .addHeader("Accept", "application/json")
            if (publicKey.isNotBlank()) {
                requestBuilder.addHeader("X-Payment-Public-Key", publicKey)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val serverMsg = runCatching { JSONObject(rawBody).optString("error") }.getOrNull()
                    return@withContext Result.failure(
                        IllegalStateException(
                            serverMsg?.takeIf { it.isNotBlank() }
                                ?: "Checkout session failed (HTTP ${response.code})."
                        )
                    )
                }

                val json = JSONObject(rawBody)
                val sessionId = json.optString("sessionId").ifBlank { json.optString("id") }
                val checkoutUrl = json.optString("checkoutUrl").ifBlank { json.optString("url") }
                if (sessionId.isBlank() || checkoutUrl.isBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("Payment backend returned an incomplete checkout session payload.")
                    )
                }

                Result.success(
                    CheckoutSessionInit(
                        sessionId = sessionId,
                        checkoutUrl = checkoutUrl,
                        provider = provider,
                        plan = plan,
                        billingCycle = billingCycle,
                        amountUsd = amountUsd,
                        billingEmail = cleanEmail
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Checkout session error: ${e.message}", e)
            Result.failure(
                IllegalStateException("Unable to reach payment checkout server: ${e.localizedMessage ?: "Network error"}")
            )
        }
    }

    suspend fun verifyCheckoutSession(
        userId: String,
        sessionId: String,
        provider: PaymentProvider,
        expectedPlan: SubscriptionPlan,
        expectedCycle: BillingCycle
    ): Result<CheckoutVerificationResult> = withContext(Dispatchers.IO) {
        val backendUrl = BuildConfig.PAYMENT_BACKEND_URL.trim().trimEnd('/')
        if (!isBackendConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Payment backend URL is required to verify session receipts.")
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
                    json.optString("status").equals("successful", ignoreCase = true)
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
