package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserProfileRepository(
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

    fun observeProfile(userId: String = auth.currentUser?.uid ?: "unauthenticated"): Flow<UserProfile?> {
        val docRef = db.collection(USERS_COLLECTION).document(userId)
        return docRef.snapshots()
            .map { snapshot -> UserProfile.fromSnapshot(snapshot) }
            .catch { error ->
                val actualError = (error as? java.util.concurrent.CancellationException)?.cause ?: error
                if (actualError is Exception) {
                    handleFirestoreError(actualError, OperationType.GET, docRef.path)
                }
                throw actualError
            }
    }

    suspend fun getProfileById(userId: String): Result<UserProfile?> {
        val docRef = db.collection(USERS_COLLECTION).document(userId)
        return try {
            val snapshot = docRef.get().await()
            Result.success(UserProfile.fromSnapshot(snapshot))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createProfile(profile: UserProfile): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        val normalized = profile.copy(userId = uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(normalized.toUpdatePayload()).await()
            } else {
                docRef.set(normalized.toCreatePayload()).await()
            }
            Result.success(uid)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun ensureOrInitializeProfile(
        defaultDisplayName: String,
        defaultEmail: String,
        defaultPhotoUrl: String
    ): Result<UserProfile> {
        val uid = requireUserId()
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        return try {
            val existingSnap = docRef.get().await()
            val existingProfile = UserProfile.fromSnapshot(existingSnap)
            if (existingProfile != null) {
                if (existingProfile.isDesignatedAdminEmail &&
                    (!existingSnap.getBoolean("onboardingCompleted").let { it == true } ||
                        existingSnap.getString("actorRole") != "ADMIN")
                ) {
                    val normalizedAdmin = existingProfile.copy(
                        actorRole = "ADMIN",
                        onboardingCompleted = true,
                        roleTitle = existingProfile.roleTitle.takeIf { it.isNotBlank() && it != "Visitor" } ?: "Administrator"
                    )
                    docRef.update(normalizedAdmin.toUpdatePayload()).await()
                    Result.success(normalizedAdmin)
                } else {
                    Result.success(existingProfile)
                }
            } else {
                val normalizedEmail = defaultEmail.trim().lowercase()
                val isDefaultAdmin = normalizedEmail == "fokoufdr@gmail.com" || normalizedEmail == "fokoufdr@gmailcom"
                val initialProfile = UserProfile(
                    userId = uid,
                    displayName = defaultDisplayName.trim().ifEmpty {
                        defaultEmail.substringBefore("@").ifEmpty { "Aura Member" }
                    },
                    email = defaultEmail.trim(),
                    headline = if (isDefaultAdmin) {
                        "Platform Administrator • Aura App Management"
                    } else {
                        "Verified Aura E-Learning Member"
                    },
                    bio = if (isDefaultAdmin) {
                        "Platform Administrator responsible for managing users, activities, and security across the app."
                    } else {
                        "Welcome to my personal identity and e-learning profile."
                    },
                    location = "",
                    roleTitle = if (isDefaultAdmin) "Administrator" else "Visitor",
                    website = "",
                    statusMessage = "Active",
                    photoUrl = defaultPhotoUrl.trim(),
                    actorRole = if (isDefaultAdmin) "ADMIN" else "VISITOR",
                    onboardingCompleted = isDefaultAdmin
                )
                docRef.set(initialProfile.toCreatePayload()).await()
                val createdSnap = docRef.get().await()
                Result.success(UserProfile.fromSnapshot(createdSnap) ?: initialProfile)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(profile.toUpdatePayload()).await()
            } else {
                docRef.set(profile.copy(userId = uid).toCreatePayload()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
    }
}
