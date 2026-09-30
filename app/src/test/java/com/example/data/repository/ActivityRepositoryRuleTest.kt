package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ActivityEventType
import com.example.data.model.ActivitySeverity
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ActivityRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueAliceEmail() = "alice_act_${UUID.randomUUID()}@test.com"
    private fun uniqueBobEmail() = "bob_act_${UUID.randomUUID()}@test.com"

    @Test
    fun logActivity_authenticatedOwner_createsAndObservesActivity() = runBlocking {
        val email = uniqueAliceEmail()
        val aliceUid = signInTestUser(email)
        val repository = ActivityRepository(firestore, auth)

        val logResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.logActivity(
                userDisplayName = "Alice Vance",
                userEmail = email,
                eventType = ActivityEventType.PROFILE_UPDATED,
                title = "Updated Profile Bio",
                details = "Changed bio and headline in Aura Profile.",
                severity = ActivitySeverity.INFO
            )
        }
        assertTrue("Expected logActivity to succeed", logResult.isSuccess)
        val activityId = logResult.getOrThrow()

        val fetchedResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getActivityById(activityId)
        }
        assertTrue("Expected owner getActivityById to succeed", fetchedResult.isSuccess)
        val fetched = fetchedResult.getOrThrow()
        assertNotNull(fetched)
        assertEquals("Updated Profile Bio", fetched?.title)
        assertEquals(aliceUid, fetched?.userId)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserActivities(aliceUid).first { list ->
                list.any { it.activityId == activityId }
            }
        }
        assertTrue(observed.any { it.activityId == activityId })
    }

    @Test
    fun getActivityById_crossUserNonAdmin_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = uniqueAliceEmail()
        signInTestUser(aliceEmail)
        val aliceRepo = ActivityRepository(firestore, auth)
        val activityId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.logActivity(
                userDisplayName = "Alice Vance",
                userEmail = aliceEmail,
                eventType = ActivityEventType.AUTH_SIGN_IN,
                title = "Signed in with Google",
                details = "Session authenticated via Credential Manager.",
                severity = ActivitySeverity.SUCCESS
            ).getOrThrow()
        }

        val bobEmail = uniqueBobEmail()
        signInTestUser(bobEmail)
        val bobRepo = ActivityRepository(firestore, auth)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getActivityById(activityId)
        }
        assertTrue("Expected cross-user activity read by non-admin to fail", crossReadResult.isFailure)
        val exception = crossReadResult.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull("Expected FirebaseFirestoreException", exception)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
    }

    @Test
    fun observeAllActivitiesForAdmin_nonAdminUser_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = uniqueAliceEmail()
        signInTestUser(aliceEmail)
        val aliceRepo = ActivityRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.logActivity(
                userDisplayName = "Alice Vance",
                userEmail = aliceEmail,
                eventType = ActivityEventType.PROFILE_CREATED,
                title = "Profile Created",
                details = "Initial profile created.",
                severity = ActivitySeverity.SUCCESS
            ).getOrThrow()
        }

        val bobEmail = uniqueBobEmail()
        signInTestUser(bobEmail)
        val bobRepo = ActivityRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeAllActivitiesForAdmin().first()
            }
            fail("Expected PERMISSION_DENIED when non-admin lists all activities without filter")
        } catch (e: Throwable) {
            val firestoreEx = generateSequence(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            assertNotNull("Expected FirebaseFirestoreException in cause chain", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun observeUserActivities_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceEmail = uniqueAliceEmail()
        val aliceUid = signInTestUser(aliceEmail)
        val repo = ActivityRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.logActivity(
                userDisplayName = "Alice Vance",
                userEmail = aliceEmail,
                eventType = ActivityEventType.AUTH_SIGN_IN,
                title = "Signed In",
                details = "Authenticated session.",
                severity = ActivitySeverity.INFO
            ).getOrThrow()
        }

        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeUserActivities(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated user")
        } catch (e: Throwable) {
            val firestoreEx = generateSequence(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            assertNotNull("Expected FirebaseFirestoreException in cause chain", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
