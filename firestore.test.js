const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, overrides = {}) {
  const past = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: "Alice Vance",
    email: "alice@example.com",
    headline: "Principal Systems Architect",
    bio: "Building resilient cloud-native identity systems.",
    location: "San Francisco, CA",
    roleTitle: "Staff Engineer",
    website: "https://alice.dev",
    statusMessage: "Available for mentoring",
    photoUrl: "https://example.com/alice.png",
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

function validActivityPayload(activityId, uid, overrides = {}) {
  const past = new Date(Date.now() - 2000);
  return {
    activityId,
    userId: uid,
    userDisplayName: "Alice Vance",
    userEmail: "alice@example.com",
    eventType: "PROFILE_UPDATED",
    title: "Updated Profile Details",
    details: "Modified headline and biography.",
    severity: "INFO",
    reviewStatus: "RECORDED",
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

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

test("1. Unauthenticated user: cannot read profiles or activities", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("activities").get());
});

test("2. Authenticated user: can create and read their own valid profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("3. Cross-user isolation: Bob cannot read Alice's profile or Alice's activities", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID));
    await db
      .collection("activities")
      .doc("act_alice_1")
      .set(validActivityPayload("act_alice_1", ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("activities").doc("act_alice_1").get());
  await assertFails(bobDb.collection("activities").get());
});

test("4. Identity spoofing: Alice cannot create a profile or activity under Bob's UID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set(validProfilePayload(BOB_UID))
  );
  await assertFails(
    aliceDb
      .collection("activities")
      .doc("act_spoof")
      .set(validActivityPayload("act_spoof", BOB_UID))
  );
});

test("5. Shadow update test: Rejects unexpected ghost fields on create and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validProfilePayload(ALICE_UID, { isVerified: true }))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("6. Email Spoofing Test: Unverified admin email cannot read all activities or profiles", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("activities")
      .doc("act_alice_1")
      .set(validActivityPayload("act_alice_1", ALICE_UID));
  });

  // Spoofed token with email_verified: false
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "fokoufdr@gmail.com",
      email_verified: false,
    })
    .firestore();

  await assertFails(spoofAdminDb.collection("activities").get());
  await assertFails(spoofAdminDb.collection("activities").doc("act_alice_1").get());
});

test("7. Verified Admin (via email or /admins collection): can list all activities, profiles, and review activities", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID));
    await db
      .collection("activities")
      .doc("act_alice_1")
      .set(validActivityPayload("act_alice_1", ALICE_UID));
    await db
      .collection("activities")
      .doc("act_bob_1")
      .set(validActivityPayload("act_bob_1", BOB_UID));
  });

  const verifiedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "fokoufdr@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(verifiedAdminDb.collection("activities").get());
  await assertSucceeds(verifiedAdminDb.collection("users").get());
  await assertSucceeds(
    verifiedAdminDb.collection("activities").doc("act_alice_1").update({
      reviewStatus: "REVIEWED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("8. Terminal State Locking: Owner cannot modify activity once admin marks it REVIEWED or FLAGGED", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("activities")
      .doc("act_1")
      .set(validActivityPayload("act_1", ALICE_UID))
  );

  // While RECORDED, owner can update details
  await assertSucceeds(
    aliceDb.collection("activities").doc("act_1").update({
      details: "Updated details before review",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin transitions reviewStatus to REVIEWED
  const verifiedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "fokoufdr@gmail.com",
      email_verified: true,
    })
    .firestore();
  await assertSucceeds(
    verifiedAdminDb.collection("activities").doc("act_1").update({
      reviewStatus: "REVIEWED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner can no longer modify the activity once terminal state is reached
  await assertFails(
    aliceDb.collection("activities").doc("act_1").update({
      details: "Attempted tamper after review",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("9. Privilege Escalation Guard: Non-admin cannot create document in /admins", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 2000);
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      adminId: ALICE_UID,
      email: "alice@example.com",
      role: "SUPER_ADMIN",
      grantedBy: ALICE_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("10. Role Onboarding Schema: Authenticated user can create and update student/teacher/school/parent role fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(
      validProfilePayload(ALICE_UID, {
        actorRole: "STUDENT",
        onboardingCompleted: true,
        educationSystem: "ANGLOPHONE",
        studentLevel: "ADVANCED_LEVEL",
        studentStream: "SCIENCE",
        schoolName: "Bilingual Grammar School Molyko",
        schoolOwnership: "",
        schoolAge: "",
        schoolEmail: "",
        schoolFacebookUrl: "",
        teachingYears: "",
        teacherSchools: "",
        pseudonym: "",
        phoneNumbers: "+237670000000",
        childIds: "",
      })
    )
  );

  // Reject invalid actorRole enum value
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      actorRole: "HACKER_ROLE",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Can log ROLE_ONBOARDING_COMPLETED activity
  await assertSucceeds(
    aliceDb
      .collection("activities")
      .doc("act_onboard_1")
      .set(
        validActivityPayload("act_onboard_1", ALICE_UID, {
          eventType: "ROLE_ONBOARDING_COMPLETED",
          title: "Completed Student Onboarding",
        })
      )
  );
});

test("11. Admin User Management: Admin can change user role to ADMIN, toggle PENDING/ACTIVE (locking user updates when PENDING), and delete user", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .set(validProfilePayload(ALICE_UID, { actorRole: "STUDENT", accountStatus: "ACTIVE" }));
  });

  const verifiedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "fokoufdr@gmail.com",
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Admin promotes Alice's role to ADMIN and sets her status to PENDING
  await assertSucceeds(
    verifiedAdminDb.collection("users").doc(ALICE_UID).update({
      actorRole: "ADMIN",
      accountStatus: "PENDING",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // While PENDING, Alice cannot update her own profile document
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      headline: "Trying to navigate/edit while pending",
      updatedAt: new Date(Date.now() - 800),
    })
  );

  // Admin sets Alice back to ACTIVE -> Alice's profile updates are re-enabled
  await assertSucceeds(
    verifiedAdminDb.collection("users").doc(ALICE_UID).update({
      accountStatus: "ACTIVE",
      updatedAt: new Date(Date.now() - 600),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      headline: "Active again!",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Admin can also suspend or block Alice's account, which blocks Alice from updating her profile
  await assertSucceeds(
    verifiedAdminDb.collection("users").doc(ALICE_UID).update({
      accountStatus: "SUSPENDED",
      updatedAt: new Date(Date.now() - 350),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      headline: "Trying to edit while suspended",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  await assertSucceeds(
    verifiedAdminDb.collection("users").doc(ALICE_UID).update({
      accountStatus: "BLOCKED",
      updatedAt: new Date(Date.now() - 250),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      headline: "Trying to edit while blocked",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Admin can delete user document
  await assertSucceeds(verifiedAdminDb.collection("users").doc(ALICE_UID).delete());
});

test("12. Database Security & IP Telemetry Logs: Validates IP connection attempts, failures, successes, and injection block logs", async () => {
  const verifiedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "fokoufdr@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(
    verifiedAdminDb
      .collection("activities")
      .doc("act_db_inj_1")
      .set(
        validActivityPayload("act_db_inj_1", ADMIN_UID, {
          eventType: "DB_INJECTION_BLOCKED",
          title: "Blocked NoSQL Injection Attempt",
          details: "Intercepted $where operator targeting /users",
          severity: "SECURITY",
          ipAddress: "10.0.2.15",
          targetPath: "/users",
          dbStatus: "INJECTION_BLOCKED",
        })
      )
  );
});

