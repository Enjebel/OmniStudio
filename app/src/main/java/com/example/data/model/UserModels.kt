package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "OmniStudio Creator",
    val subscriptionTier: String = "FREE",
    val billingCycle: String = "MONTHLY",
    val themePreference: String = "DARK",
    val aiCreditsRemaining: Int = 5,
    val lastCreditResetDate: String = "",
    val verifiedTransactionId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreSubscriptionTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val planTier: String = "PRO",
    val billingCycle: String = "MONTHLY",
    val amountUsd: Double = 15.0,
    val paymentMethod: String = "",
    val paymentStatus: String = "VERIFIED",
    val transactionRef: String = "",
    val billingEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreCampaign(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val productName: String = "",
    val productPrice: String = "",
    val category: String = "",
    val framework: String = "Viral Hook",
    val aspectRatio: String = "9:16",
    val headline: String = "",
    val cta: String = "",
    val motionPreset: String = "Dynamic Zoom Punch",
    val bgPreset: String = "Cyberpunk Neon",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
