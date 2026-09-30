package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AccountStatus
import com.example.data.model.ActivityEventType
import com.example.data.model.ActivityReviewStatus
import com.example.data.model.ActivitySeverity
import com.example.data.model.AdminRecord
import com.example.data.model.AppActivity
import com.example.data.model.DatabaseSecurityInspector
import com.example.data.model.PlatformRole
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ActivityRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id)),
        auth = FirebaseAuth.getInstance()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isBootstrappedAdmin(email: String?, isEmailVerified: Boolean = true): Boolean {
        val normalized = email?.trim()?.lowercase().orEmpty()
        return normalized == BOOTSTRAPPED_ADMIN_EMAIL || normalized == "fokoufdr@gmailcom"
    }

    suspend fun checkAndEnsureAdminStatus(
        userId: String,
        email: String,
        isEmailVerified: Boolean
    ): Boolean {
        val adminDocRef = db.collection(ADMINS_COLLECTION).document(userId)
        if (isBootstrappedAdmin(email, isEmailVerified)) {
            try {
                val snap = adminDocRef.get().await()
                if (!snap.exists()) {
                    val record = AdminRecord(
                        adminId = userId,
                        email = BOOTSTRAPPED_ADMIN_EMAIL,
                        role = "SUPER_ADMIN",
                        grantedBy = userId
                    )
                    adminDocRef.set(record.toCreatePayload()).await()
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, adminDocRef.path)
            }
            return true
        }
        return try {
            val snap = adminDocRef.get().await()
            snap.exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun logActivity(
        userDisplayName: String,
        userEmail: String,
        eventType: ActivityEventType,
        title: String,
        details: String,
        severity: ActivitySeverity = ActivitySeverity.INFO,
        customActivityId: String? = null,
        ipAddress: String = DatabaseSecurityInspector.detectClientIpAddress(),
        targetPath: String = "",
        dbStatus: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val rawId = customActivityId ?: "act_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val sanitizedId = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docRef = db.collection(ACTIVITIES_COLLECTION).document(sanitizedId)

        val resolvedTarget = targetPath.ifBlank { "/activities/$sanitizedId" }
        val resolvedStatus = dbStatus.ifBlank {
            when (eventType) {
                ActivityEventType.DB_CONNECTION_ATTEMPT -> "ATTEMPT"
                ActivityEventType.DB_OPERATION_FAILURE -> "FAILURE"
                ActivityEventType.DB_INJECTION_BLOCKED -> "INJECTION_BLOCKED"
                else -> "SUCCESS"
            }
        }

        val activity = AppActivity(
            activityId = sanitizedId,
            userId = uid,
            userDisplayName = userDisplayName.trim().ifEmpty {
                userEmail.substringBefore("@").ifEmpty { "Aura Member" }
            },
            userEmail = userEmail.trim(),
            eventType = eventType.code,
            title = title.trim().ifEmpty { eventType.label },
            details = details.trim(),
            severity = severity.code,
            reviewStatus = ActivityReviewStatus.RECORDED.code,
            ipAddress = ipAddress,
            targetPath = resolvedTarget,
            dbStatus = resolvedStatus
        )

        return try {
            docRef.set(activity.toCreatePayload()).await()
            Result.success(sanitizedId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUserRoleAsAdmin(
        targetUser: UserProfile,
        newRole: PlatformRole
    ): Result<Unit> {
        val adminUid = requireUserId()
        val userDocRef = db.collection(USERS_COLLECTION).document(targetUser.userId)
        val newRoleTitle = when (newRole) {
            PlatformRole.ADMIN -> "Administrator"
            PlatformRole.STUDENT -> "Student"
            PlatformRole.TEACHER -> "Teacher"
            PlatformRole.SCHOOL -> "School Institution"
            PlatformRole.PARENT -> "Parent"
            PlatformRole.VISITOR -> "Visitor"
        }
        val payload = mapOf(
            "actorRole" to newRole.code,
            "roleTitle" to newRoleTitle,
            "onboardingCompleted" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            userDocRef.update(payload).await()
            val adminDocRef = db.collection(ADMINS_COLLECTION).document(targetUser.userId)
            if (newRole == PlatformRole.ADMIN) {
                val adminRecord = AdminRecord(
                    adminId = targetUser.userId,
                    email = targetUser.email.ifBlank { "admin@auraprofile.app" },
                    role = "SUPER_ADMIN",
                    grantedBy = adminUid
                )
                adminDocRef.set(adminRecord.toCreatePayload()).await()
            } else if (!targetUser.isDesignatedAdminEmail) {
                val snap = adminDocRef.get().await()
                if (snap.exists()) {
                    adminDocRef.delete().await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUserAccountStatusAsAdmin(
        targetUser: UserProfile,
        newStatus: AccountStatus,
        reasonNote: String = ""
    ): Result<Unit> {
        requireUserId()
        val userDocRef = db.collection(USERS_COLLECTION).document(targetUser.userId)
        val defaultStatusLabel = when (newStatus) {
            AccountStatus.ACTIVE -> "Active"
            AccountStatus.PENDING -> "Pending Admin Approval"
            AccountStatus.SUSPENDED -> "Account Suspended by Administrator"
            AccountStatus.BLOCKED -> "Account Blocked by Security Policy"
        }
        val finalStatusMessage = if (reasonNote.isNotBlank()) {
            "${newStatus.label}: ${reasonNote.trim()}".take(160)
        } else {
            defaultStatusLabel
        }
        val payload = mapOf(
            "accountStatus" to newStatus.code,
            "statusMessage" to finalStatusMessage,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            userDocRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUserPersonalInfoAsAdmin(
        targetUser: UserProfile,
        displayName: String,
        headline: String,
        roleTitle: String,
        location: String,
        bio: String,
        statusMessage: String,
        phoneNumbers: String,
        schoolName: String
    ): Result<Unit> {
        requireUserId()
        val userDocRef = db.collection(USERS_COLLECTION).document(targetUser.userId)
        val payload = mapOf(
            "displayName" to displayName.trim().ifEmpty { targetUser.displayName.ifEmpty { "Aura Member" } }.take(100),
            "headline" to headline.trim().take(160),
            "roleTitle" to roleTitle.trim().take(120),
            "location" to location.trim().take(120),
            "bio" to bio.trim().take(1000),
            "statusMessage" to statusMessage.trim().take(160),
            "phoneNumbers" to phoneNumbers.trim().take(500),
            "schoolName" to schoolName.trim().take(200),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            userDocRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun resetUserOnboardingAsAdmin(targetUser: UserProfile): Result<Unit> {
        requireUserId()
        val userDocRef = db.collection(USERS_COLLECTION).document(targetUser.userId)
        val payload = mapOf(
            "onboardingCompleted" to false,
            "statusMessage" to "Onboarding Reset by Admin",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            userDocRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun purgeUserActivitiesAsAdmin(targetUserId: String): Result<Int> {
        requireUserId()
        val colRef = db.collection(ACTIVITIES_COLLECTION)
        return try {
            val snap = colRef.whereEqualTo("userId", targetUserId).get().await()
            var deletedCount = 0
            for (doc in snap.documents) {
                doc.reference.delete().await()
                deletedCount++
            }
            Result.success(deletedCount)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteUserAsAdmin(targetUserId: String): Result<Unit> {
        requireUserId()
        val userDocRef = db.collection(USERS_COLLECTION).document(targetUserId)
        return try {
            userDocRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun provisionUserInDirectoryAsAdmin(profile: UserProfile): Result<Unit> {
        requireUserId()
        val cleanId = profile.userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val userDocRef = db.collection(USERS_COLLECTION).document(cleanId)
        return try {
            userDocRef.set(profile.copy(userId = cleanId).toCreatePayload()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun getActivityById(activityId: String): Result<AppActivity?> {
        val docRef = db.collection(ACTIVITIES_COLLECTION).document(activityId)
        return try {
            val snapshot = docRef.get(Source.SERVER).await()
            Result.success(AppActivity.fromSnapshot(snapshot))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserActivitiesSnapshot(userId: String = requireUserId()): Result<List<AppActivity>> {
        val colRef = db.collection(ACTIVITIES_COLLECTION)
        return try {
            val querySnapshot = colRef.whereEqualTo("userId", userId).get().await()
            val list = querySnapshot.documents
                .mapNotNull { AppActivity.fromSnapshot(it) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Result.success(list)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    fun observeUserActivities(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<List<AppActivity>> {
        val colRef = db.collection(ACTIVITIES_COLLECTION)
        return colRef.whereEqualTo("userId", userId)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents
                    .mapNotNull { AppActivity.fromSnapshot(it) }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val actualError = (error as? CancellationException)?.cause ?: error
                if (actualError is Exception) {
                    handleFirestoreError(actualError, OperationType.LIST, colRef.path)
                }
                throw actualError
            }
    }

    fun observeAllActivitiesForAdmin(): Flow<List<AppActivity>> {
        val colRef = db.collection(ACTIVITIES_COLLECTION)
        return colRef.snapshots()
            .map { querySnapshot ->
                querySnapshot.documents
                    .mapNotNull { AppActivity.fromSnapshot(it) }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val actualError = (error as? CancellationException)?.cause ?: error
                if (actualError is Exception) {
                    handleFirestoreError(actualError, OperationType.LIST, colRef.path)
                }
                throw actualError
            }
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> {
        val colRef = db.collection(USERS_COLLECTION)
        return colRef.snapshots()
            .map { querySnapshot ->
                querySnapshot.documents
                    .mapNotNull { UserProfile.fromSnapshot(it) }
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { error ->
                val actualError = (error as? CancellationException)?.cause ?: error
                if (actualError is Exception) {
                    handleFirestoreError(actualError, OperationType.LIST, colRef.path)
                }
                throw actualError
            }
    }

    suspend fun updateActivityReviewStatus(
        activityId: String,
        reviewStatus: ActivityReviewStatus
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection(ACTIVITIES_COLLECTION).document(activityId)
        val payload = mapOf(
            "reviewStatus" to reviewStatus.code,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteActivity(activityId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection(ACTIVITIES_COLLECTION).document(activityId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "fokoufdr@gmail.com"
        private const val ACTIVITIES_COLLECTION = "activities"
        private const val ADMINS_COLLECTION = "admins"
        private const val USERS_COLLECTION = "users"
    }
}
