package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.FirestoreCampaign
import com.example.data.model.FirestoreUserProfile
import com.example.model.BillingCycle
import com.example.model.SubscriptionPlan
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FirestoreRepository(
    private val db: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirestoreRepo"

        fun create(context: Context): FirestoreRepository {
            val dbId = context.getString(R.string.firestore_database_id)
            val instance = FirebaseFirestore.getInstance(dbId)
            return FirestoreRepository(instance)
        }

        fun todayDateKey(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private val auth get() = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before accessing cloud database")
    }

    suspend fun getOrCreateUserProfile(
        userId: String,
        displayName: String,
        email: String
    ): FirestoreUserProfile {
        val userDocRef = db.collection("users").document(userId)
        val today = todayDateKey()
        try {
            val snapshot = userDocRef.get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(FirestoreUserProfile::class.java)
                if (profile != null) {
                    val plan = SubscriptionPlan.fromTierId(profile.subscriptionTier)
                    if (profile.lastCreditResetDate != today) {
                        val resetCredits = plan.dailyAiCredits
                        updateDailyCredits(userId, resetCredits, today)
                        return profile.copy(
                            aiCreditsRemaining = resetCredits,
                            lastCreditResetDate = today
                        )
                    }
                    return profile
                }
            }

            val cleanName = displayName.ifBlank { "OmniStudio Creator" }.take(100)
            val cleanEmail = email.ifBlank { "creator@omnistudio.ai" }.take(150)
            val payload = mapOf(
                "userId" to userId,
                "displayName" to cleanName,
                "email" to cleanEmail,
                "role" to "OmniStudio Creator",
                "subscriptionTier" to "FREE",
                "billingCycle" to BillingCycle.MONTHLY.id,
                "themePreference" to "DARK",
                "aiCreditsRemaining" to SubscriptionPlan.FREE.dailyAiCredits,
                "lastCreditResetDate" to today,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userDocRef.set(payload).await()

            return FirestoreUserProfile(
                id = userId,
                userId = userId,
                displayName = cleanName,
                email = cleanEmail,
                role = "OmniStudio Creator",
                subscriptionTier = "FREE",
                billingCycle = BillingCycle.MONTHLY.id,
                themePreference = "DARK",
                aiCreditsRemaining = SubscriptionPlan.FREE.dailyAiCredits,
                lastCreditResetDate = today
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching or creating user profile: ${e.message}", e)
            return FirestoreUserProfile(
                id = userId,
                userId = userId,
                displayName = displayName,
                email = email,
                role = "OmniStudio Creator",
                subscriptionTier = "FREE",
                billingCycle = BillingCycle.MONTHLY.id,
                themePreference = "DARK",
                aiCreditsRemaining = SubscriptionPlan.FREE.dailyAiCredits,
                lastCreditResetDate = today
            )
        }
    }

    suspend fun updateThemePreference(userId: String, isDark: Boolean) {
        val userDocRef = db.collection("users").document(userId)
        try {
            userDocRef.update(
                mapOf(
                    "themePreference" to if (isDark) "DARK" else "LIGHT",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Log.d(TAG, "Updated theme preference for $userId to ${if (isDark) "DARK" else "LIGHT"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating theme preference: ${e.message}", e)
        }
    }

    suspend fun updateDailyCredits(userId: String, creditsRemaining: Int, resetDate: String = todayDateKey()) {
        val userDocRef = db.collection("users").document(userId)
        try {
            userDocRef.update(
                mapOf(
                    "aiCreditsRemaining" to creditsRemaining.coerceIn(0, 10000),
                    "lastCreditResetDate" to resetDate,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Log.d(TAG, "Updated AI credits for $userId to $creditsRemaining ($resetDate)")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating AI credits: ${e.message}", e)
        }
    }

    suspend fun recordVerifiedSubscriptionPurchase(
        userId: String,
        plan: SubscriptionPlan,
        billingCycle: BillingCycle,
        paymentMethod: String,
        billingEmail: String,
        externalTransactionRef: String? = null
    ): Result<String> {
        if (plan == SubscriptionPlan.FREE) {
            return downgradeToFreeTier(userId).map { "FREE_RESET" }
        }

        val amount = if (billingCycle == BillingCycle.YEARLY) plan.yearlyTotalUsd else plan.monthlyPriceUsd
        val txId = "tx_${plan.id.lowercase(Locale.US)}_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val txRefString = externalTransactionRef?.trim()?.take(64)?.takeIf { it.isNotBlank() }
            ?: "OMNI-${plan.id}-${System.currentTimeMillis().toString().takeLast(6)}"
        val cleanPaymentMethod = paymentMethod.trim().take(80)
        val cleanBillingEmail = billingEmail.trim().take(150)

        if (cleanPaymentMethod.length < 3 || !cleanBillingEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Valid payment method and billing email are required."))
        }

        return try {
            val userDocRef = db.collection("users").document(userId)
            val txDocRef = userDocRef.collection("transactions").document(txId)

            val txPayload = mapOf(
                "userId" to userId,
                "planTier" to plan.id,
                "billingCycle" to billingCycle.id,
                "amountUsd" to amount,
                "paymentMethod" to cleanPaymentMethod,
                "paymentStatus" to "VERIFIED",
                "transactionRef" to txRefString,
                "billingEmail" to cleanBillingEmail,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val userUpdate = mapOf(
                "subscriptionTier" to plan.id,
                "billingCycle" to billingCycle.id,
                "aiCreditsRemaining" to plan.dailyAiCredits,
                "lastCreditResetDate" to todayDateKey(),
                "verifiedTransactionId" to txId,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(txDocRef, txPayload)
            batch.update(userDocRef, userUpdate)
            batch.commit().await()

            Log.d(TAG, "Verified subscription purchase $txId (${plan.id}/${billingCycle.id}) for $userId")
            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify subscription purchase: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<FirestoreUserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toObject(FirestoreUserProfile::class.java) else null
            }
            .catch { e ->
                Log.e(TAG, "Failed to observe user profile $userId: ${e.message}")
                emit(null)
            }

    suspend fun downgradeToFreeTier(userId: String): Result<Unit> {
        return try {
            val userDocRef = db.collection("users").document(userId)
            userDocRef.update(
                mapOf(
                    "subscriptionTier" to SubscriptionPlan.FREE.id,
                    "billingCycle" to BillingCycle.MONTHLY.id,
                    "aiCreditsRemaining" to SubscriptionPlan.FREE.dailyAiCredits,
                    "lastCreditResetDate" to todayDateKey(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error downgrading to FREE tier: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getUserCampaigns(userId: String): Flow<List<FirestoreCampaign>> = flow {
        val path = "users/$userId/campaigns"
        db.collection("users").document(userId).collection("campaigns")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(FirestoreCampaign::class.java) }
            .catch { e ->
                Log.e(TAG, "Failed to fetch user campaigns at $path: ${e.message}")
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    fun observeCampaigns(): Flow<List<FirestoreCampaign>> = flow {
        val uid = auth.currentUser?.uid ?: return@flow
        getUserCampaigns(uid).collect { emit(it) }
    }

    suspend fun deleteCampaign(userId: String, campaignId: String) {
        try {
            db.collection("users").document(userId).collection("campaigns").document(campaignId).delete().await()
            Log.d(TAG, "Deleted campaign $campaignId for user $userId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete campaign: ${e.message}", e)
            throw e
        }
    }

    suspend fun saveCampaign(campaign: FirestoreCampaign) {
        val uid = requireUserId()
        val campaignsRef = db.collection("users").document(uid).collection("campaigns")
        val docRef = if (campaign.id.isBlank()) campaignsRef.document() else campaignsRef.document(campaign.id)

        val payload = mapOf(
            "userId" to uid,
            "name" to campaign.name.ifBlank { "Untitled OmniStudio Project" },
            "productName" to campaign.productName,
            "productPrice" to campaign.productPrice,
            "category" to campaign.category,
            "framework" to campaign.framework,
            "aspectRatio" to campaign.aspectRatio,
            "headline" to campaign.headline,
            "cta" to campaign.cta,
            "motionPreset" to campaign.motionPreset,
            "bgPreset" to campaign.bgPreset,
            "createdAt" to (campaign.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            Log.d(TAG, "Saved campaign ${docRef.id} for user $uid")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving campaign: ${e.message}", e)
            throw e
        }
    }
}
