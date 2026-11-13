const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_01";
const BOB_UID = "bob_creator_02";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const serverTs = () => new Date(Date.now() - 1000);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profiles or campaigns", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("campaigns").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated user can create FREE profile and toggle themePreference", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Alice Creator",
      email: "alice@omnistudio.ai",
      role: "OmniStudio Creator",
      subscriptionTier: "FREE",
      billingCycle: "MONTHLY",
      themePreference: "DARK",
      aiCreditsRemaining: 5,
      lastCreditResetDate: "2026-11-13",
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(
    profileRef.update({
      themePreference: "LIGHT",
      updatedAt: serverTs(),
    })
  );
});

test("User cannot self-assign PRO tier without a verified transaction in same batch", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  // Cannot create directly as PRO
  await assertFails(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Alice Creator",
      email: "alice@omnistudio.ai",
      role: "OmniStudio Creator",
      subscriptionTier: "PRO",
      aiCreditsRemaining: 999,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Create FREE first
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Alice Creator",
      email: "alice@omnistudio.ai",
      role: "OmniStudio Creator",
      subscriptionTier: "FREE",
      aiCreditsRemaining: 5,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Cannot bypass payment by updating subscriptionTier alone
  await assertFails(
    profileRef.update({
      subscriptionTier: "PRO",
      updatedAt: serverTs(),
    })
  );
});

test("User CAN upgrade to BASIC/PRO/PREMIUM when atomic batch writes verified transaction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  const txId = "tx_verified_pro_001";
  const txRef = profileRef.collection("transactions").doc(txId);

  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Alice Creator",
      email: "alice@omnistudio.ai",
      role: "OmniStudio Creator",
      subscriptionTier: "FREE",
      aiCreditsRemaining: 5,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  const batch = aliceDb.batch();
  batch.set(txRef, {
    userId: ALICE_UID,
    planTier: "PRO",
    billingCycle: "YEARLY",
    amountUsd: 144.0,
    paymentMethod: "Visa **** 4242",
    paymentStatus: "VERIFIED",
    transactionRef: "OMNI-TX-2026-001",
    billingEmail: "alice@omnistudio.ai",
    createdAt: serverTs(),
  });
  batch.update(profileRef, {
    subscriptionTier: "PRO",
    billingCycle: "YEARLY",
    aiCreditsRemaining: 9999,
    verifiedTransactionId: txId,
    updatedAt: serverTs(),
  });

  await assertSucceeds(batch.commit());
});

test("Cross-user isolation: Bob cannot read or write Alice profile, campaigns, or transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@omnistudio.ai",
      role: "Creator",
      subscriptionTier: "FREE",
      aiCreditsRemaining: 5,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("campaigns").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});
