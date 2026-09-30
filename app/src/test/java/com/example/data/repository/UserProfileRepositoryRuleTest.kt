package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class UserProfileRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueAliceEmail() = "alice_${java.util.UUID.randomUUID()}@test.com"
    private fun uniqueBobEmail() = "bob_${java.util.UUID.randomUUID()}@test.com"

    @Test
    fun createAndGetProfile_authenticatedOwner_succeeds() = runBlocking {
        val email = uniqueAliceEmail()
        val aliceUid = signInTestUser(email)
        val repository = UserProfileRepository(firestore, auth)

        val profile = UserProfile(
            userId = aliceUid,
            displayName = "Alice Vance",
            email = email,
            headline = "Staff Security Engineer",
            bio = "Designing zero-trust mobile architectures.",
            location = "Seattle, WA",
            roleTitle = "Principal Engineer",
            website = "https://alice.example.com",
            statusMessage = "Online"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProfile(profile)
        }
        assertTrue("Expected profile creation to succeed", createResult.isSuccess)
        assertEquals(aliceUid, createResult.getOrThrow())

        val fetchedResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProfileById(aliceUid)
        }
        assertTrue(fetchedResult.isSuccess)
        val fetched = fetchedResult.getOrThrow()
        assertNotNull(fetched)
        assertEquals("Alice Vance", fetched?.displayName)
        assertEquals("Staff Security Engineer", fetched?.headline)
    }

    @Test
    fun saveProfile_updatesExistingDocument_emitsRealtimeFlowUpdate() = runBlocking {
        val email = uniqueAliceEmail()
        val aliceUid = signInTestUser(email)
        val repository = UserProfileRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureOrInitializeProfile(
                defaultDisplayName = "Alice Initial",
                defaultEmail = email,
                defaultPhotoUrl = ""
            ).getOrThrow()
        }

        val updatedProfile = UserProfile(
            userId = aliceUid,
            displayName = "Alice Updated",
            email = email,
            headline = "Updated Headline",
            bio = "Updated bio content",
            location = "New York, NY",
            roleTitle = "VP of Engineering",
            website = "https://alice.io",
            statusMessage = "Focusing",
            actorRole = "STUDENT",
            onboardingCompleted = true,
            educationSystem = "ANGLOPHONE",
            studentLevel = "ADVANCED_LEVEL",
            studentStream = "SCIENCE"
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveProfile(updatedProfile)
        }
        assertTrue("Expected profile update to succeed", saveResult.isSuccess)

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(aliceUid).first { it?.displayName == "Alice Updated" }
        }
        assertNotNull(emitted)
        assertEquals("VP of Engineering", emitted?.roleTitle)
        assertEquals("STUDENT", emitted?.actorRole)
        assertEquals("ACTIVE", emitted?.accountStatus)
        assertEquals(true, emitted?.onboardingCompleted)
    }

    @Test
    fun logDatabaseSecurityTelemetry_withIpAndTargetPath_succeeds() = runBlocking {
        val email = uniqueAliceEmail()
        val aliceUid = signInTestUser(email)
        val activityRepo = ActivityRepository(firestore, auth)

        val logResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            activityRepo.logActivity(
                userDisplayName = "Alice Security",
                userEmail = email,
                eventType = com.example.data.model.ActivityEventType.DB_INJECTION_BLOCKED,
                title = "Blocked Database Injection Attempt",
                details = "Blocked suspicious payload (\$where) targeting /users/$aliceUid",
                severity = com.example.data.model.ActivitySeverity.SECURITY,
                ipAddress = "10.0.2.15",
                targetPath = "/users/$aliceUid",
                dbStatus = "INJECTION_BLOCKED"
            )
        }
        assertTrue("Expected DB security telemetry log to succeed", logResult.isSuccess)
    }

    @Test
    fun getProfileById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val email = uniqueAliceEmail()
        val aliceUid = signInTestUser(email)
        val aliceRepo = UserProfileRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureOrInitializeProfile("Alice", email, "").getOrThrow()
        }

        signInTestUser(uniqueBobEmail())
        val bobRepo = UserProfileRepository(firestore, auth)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProfileById(aliceUid)
        }
        assertTrue("Expected cross-user read to fail", crossReadResult.isFailure)
        val exception = crossReadResult.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull("Expected FirebaseFirestoreException", exception)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
    }

    @Test
    fun observeProfile_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val email = uniqueAliceEmail()
        val aliceUid = signInTestUser(email)
        val repo = UserProfileRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureOrInitializeProfile("Alice", email, "").getOrThrow()
        }

        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeProfile(aliceUid).first()
            }
            fail("Expected FirebaseFirestoreException.Code.PERMISSION_DENIED for unauthenticated user")
        } catch (e: Throwable) {
            val firestoreEx = generateSequence(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            assertNotNull("Expected FirebaseFirestoreException in cause chain", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_profile@test.com"
        const val BOB_EMAIL = "bob_profile@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
