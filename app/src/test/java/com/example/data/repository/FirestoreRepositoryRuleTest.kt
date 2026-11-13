package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.FirestoreCampaign
import com.example.model.BillingCycle
import com.example.model.SubscriptionPlan
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun getOrCreateUserProfile_and_toggleTheme_succeedsForOwner() = runBlocking {
        val uid = signInTestUser("creator1_${UUID.randomUUID()}@omnistudio.ai")
        val repo = FirestoreRepository(firestore)

        val profile = withTimeout(5000L) {
            repo.getOrCreateUserProfile(uid, "Creator One", "creator1@omnistudio.ai")
        }
        assertEquals(uid, profile.userId)
        assertEquals("FREE", profile.subscriptionTier)
        assertEquals(5, profile.aiCreditsRemaining)

        withTimeout(5000L) {
            repo.updateThemePreference(uid, isDark = false)
        }
        val reloaded = withTimeout(5000L) {
            repo.getOrCreateUserProfile(uid, "Creator One", "creator1@omnistudio.ai")
        }
        assertEquals("LIGHT", reloaded.themePreference)
    }

    @Test
    fun verifiedSubscriptionPurchase_writesTransactionAndUpgradesTierAtomically() = runBlocking {
        val uid = signInTestUser("buyer_${UUID.randomUUID()}@omnistudio.ai")
        val repo = FirestoreRepository(firestore)

        withTimeout(5000L) {
            repo.getOrCreateUserProfile(uid, "Pro Buyer", "buyer@omnistudio.ai")
        }

        val purchaseResult = withTimeout(5000L) {
            repo.recordVerifiedSubscriptionPurchase(
                userId = uid,
                plan = SubscriptionPlan.PRO,
                billingCycle = BillingCycle.YEARLY,
                paymentMethod = "Visa •••• 4242",
                billingEmail = "buyer@omnistudio.ai"
            )
        }
        assertTrue(purchaseResult.isSuccess)
        val txId = purchaseResult.getOrThrow()
        assertTrue(txId.isNotBlank())

        val upgraded = withTimeout(5000L) {
            repo.getOrCreateUserProfile(uid, "Pro Buyer", "buyer@omnistudio.ai")
        }
        assertEquals("PRO", upgraded.subscriptionTier)
        assertEquals("YEARLY", upgraded.billingCycle)
        assertEquals(txId, upgraded.verifiedTransactionId)
    }

    @Test
    fun campaignSaveAndCrossUserIsolation_enforced() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@omnistudio.ai")
        val repo = FirestoreRepository(firestore)
        withTimeout(5000L) {
            repo.getOrCreateUserProfile(aliceUid, "Alice", "alice@omnistudio.ai")
        }

        val campaignId = "camp_${UUID.randomUUID()}"
        withTimeout(5000L) {
            repo.saveCampaign(
                FirestoreCampaign(
                    id = campaignId,
                    userId = aliceUid,
                    name = "Luxury Watch Ad",
                    productName = "Chronos Watch",
                    productPrice = "$299",
                    framework = "Viral Hook",
                    aspectRatio = "9:16",
                    headline = "STOP SCROLLING!",
                    cta = "SHOP NOW"
                )
            )
        }

        // Bob tries to read Alice's campaigns -> PERMISSION_DENIED
        signInTestUser("bob_${UUID.randomUUID()}@omnistudio.ai")
        try {
            withTimeout(3000L) {
                repo.getUserCampaigns(aliceUid).first()
            }
            // Note: catch block in getUserCampaigns emits emptyList on permission error,
            // so direct collection query verifies hard PERMISSION_DENIED at Firestore SDK layer:
        } catch (_: Exception) {}

        var caught: Throwable? = null
        try {
            withTimeout(5000L) {
                firestore.collection("users").document(aliceUid).collection("campaigns").document(campaignId).get()
                    .let { com.google.android.gms.tasks.Tasks.await(it) }
            }
            fail("Expected PERMISSION_DENIED for cross-user campaign access")
        } catch (e: Exception) {
            caught = e
        }
        val fsEx = caught?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }
}
