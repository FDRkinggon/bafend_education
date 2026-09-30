package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserProfile

data class RememberedAccount(
    val userId: String,
    val displayName: String,
    val email: String,
    val photoUrl: String,
    val headline: String,
    val roleTitle: String
)

enum class AuthMode {
    LOGIN,
    SIGN_UP
}

class PersistentSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isKeepSignedInEnabled(): Boolean {
        return prefs.getBoolean(KEY_KEEP_SIGNED_IN, true)
    }

    fun setKeepSignedInEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SIGNED_IN, enabled).apply()
    }

    fun hasCreatedAccount(): Boolean {
        return prefs.getBoolean(KEY_HAS_CREATED_ACCOUNT, false) || getRememberedAccount() != null
    }

    fun wasExplicitlySignedOut(): Boolean {
        return prefs.getBoolean(KEY_EXPLICITLY_SIGNED_OUT, false)
    }

    fun getPreferredAuthMode(): AuthMode {
        val savedMode = prefs.getString(KEY_PREFERRED_AUTH_MODE, null)
        return when {
            savedMode == AuthMode.LOGIN.name -> AuthMode.LOGIN
            savedMode == AuthMode.SIGN_UP.name -> AuthMode.SIGN_UP
            hasCreatedAccount() -> AuthMode.LOGIN
            else -> AuthMode.LOGIN
        }
    }

    fun setPreferredAuthMode(mode: AuthMode) {
        prefs.edit().putString(KEY_PREFERRED_AUTH_MODE, mode.name).apply()
    }

    fun onUserAuthenticated(
        userId: String,
        displayName: String,
        email: String,
        photoUrl: String
    ) {
        val existing = getRememberedAccount()
        val resolvedName = displayName.trim().ifEmpty {
            existing?.displayName?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").ifEmpty { "Aura Member" }
        }
        val resolvedEmail = email.trim().ifEmpty { existing?.email.orEmpty() }
        val resolvedPhoto = photoUrl.trim().ifEmpty { existing?.photoUrl.orEmpty() }

        prefs.edit()
            .putBoolean(KEY_HAS_CREATED_ACCOUNT, true)
            .putBoolean(KEY_EXPLICITLY_SIGNED_OUT, false)
            .putString(KEY_PREFERRED_AUTH_MODE, AuthMode.LOGIN.name)
            .putString(KEY_REMEMBERED_UID, userId)
            .putString(KEY_REMEMBERED_NAME, resolvedName)
            .putString(KEY_REMEMBERED_EMAIL, resolvedEmail)
            .putString(KEY_REMEMBERED_PHOTO, resolvedPhoto)
            .apply()
    }

    fun saveCachedProfile(profile: UserProfile) {
        prefs.edit()
            .putBoolean(KEY_HAS_CREATED_ACCOUNT, true)
            .putBoolean(KEY_EXPLICITLY_SIGNED_OUT, false)
            .putString(KEY_PREFERRED_AUTH_MODE, AuthMode.LOGIN.name)
            .putString(KEY_REMEMBERED_UID, profile.userId)
            .putString(KEY_REMEMBERED_NAME, profile.displayName)
            .putString(KEY_REMEMBERED_EMAIL, profile.email)
            .putString(KEY_REMEMBERED_PHOTO, profile.photoUrl)
            .putString(KEY_REMEMBERED_HEADLINE, profile.headline)
            .putString(KEY_REMEMBERED_ROLE, profile.roleTitle)
            .putString(KEY_CACHED_BIO, profile.bio)
            .putString(KEY_CACHED_LOCATION, profile.location)
            .putString(KEY_CACHED_WEBSITE, profile.website)
            .putString(KEY_CACHED_STATUS, profile.statusMessage)
            .putString(KEY_CACHED_ACTOR_ROLE, profile.actorRole)
            .putString(KEY_CACHED_ACCOUNT_STATUS, profile.accountStatus)
            .putBoolean(KEY_CACHED_ONBOARDING_COMPLETED, profile.onboardingCompleted)
            .putString(KEY_CACHED_EDU_SYSTEM, profile.educationSystem)
            .putString(KEY_CACHED_STUDENT_LEVEL, profile.studentLevel)
            .putString(KEY_CACHED_STUDENT_STREAM, profile.studentStream)
            .putString(KEY_CACHED_SCHOOL_NAME, profile.schoolName)
            .putString(KEY_CACHED_SCHOOL_OWNERSHIP, profile.schoolOwnership)
            .putString(KEY_CACHED_SCHOOL_AGE, profile.schoolAge)
            .putString(KEY_CACHED_SCHOOL_EMAIL, profile.schoolEmail)
            .putString(KEY_CACHED_SCHOOL_FB, profile.schoolFacebookUrl)
            .putString(KEY_CACHED_TEACHING_YEARS, profile.teachingYears)
            .putString(KEY_CACHED_TEACHER_SCHOOLS, profile.teacherSchools)
            .putString(KEY_CACHED_PSEUDONYM, profile.pseudonym)
            .putString(KEY_CACHED_PHONES, profile.phoneNumbers)
            .putString(KEY_CACHED_CHILD_IDS, profile.childIds)
            .apply()
    }

    fun getCachedProfile(userId: String): UserProfile? {
        val cachedUid = prefs.getString(KEY_REMEMBERED_UID, null) ?: return null
        if (cachedUid != userId) return null
        val displayName = prefs.getString(KEY_REMEMBERED_NAME, null) ?: return null
        val email = prefs.getString(KEY_REMEMBERED_EMAIL, "").orEmpty()
        val isAdminEmail = email.trim().equals("fokoufdr@gmail.com", ignoreCase = true) ||
            email.trim().equals("fokoufdr@gmailcom", ignoreCase = true)
        val defaultActor = if (isAdminEmail) "ADMIN" else "VISITOR"
        return UserProfile(
            userId = cachedUid,
            displayName = displayName,
            email = email,
            headline = prefs.getString(KEY_REMEMBERED_HEADLINE, "Verified Aura Profile Member").orEmpty(),
            bio = prefs.getString(KEY_CACHED_BIO, "Welcome to my personal identity profile.").orEmpty(),
            location = prefs.getString(KEY_CACHED_LOCATION, "").orEmpty(),
            roleTitle = if (isAdminEmail) {
                prefs.getString(KEY_REMEMBERED_ROLE, "Administrator").orEmpty().let {
                    if (it.isBlank() || it == "Visitor") "Administrator" else it
                }
            } else {
                prefs.getString(KEY_REMEMBERED_ROLE, "Visitor").orEmpty()
            },
            website = prefs.getString(KEY_CACHED_WEBSITE, "").orEmpty(),
            statusMessage = prefs.getString(KEY_CACHED_STATUS, "Active").orEmpty(),
            photoUrl = prefs.getString(KEY_REMEMBERED_PHOTO, "").orEmpty(),
            actorRole = if (isAdminEmail) {
                "ADMIN"
            } else {
                prefs.getString(KEY_CACHED_ACTOR_ROLE, defaultActor).orEmpty().ifBlank { defaultActor }
            },
            accountStatus = if (isAdminEmail) {
                "ACTIVE"
            } else {
                prefs.getString(KEY_CACHED_ACCOUNT_STATUS, "ACTIVE").orEmpty().ifBlank { "ACTIVE" }
            },
            onboardingCompleted = if (isAdminEmail) {
                true
            } else {
                prefs.getBoolean(KEY_CACHED_ONBOARDING_COMPLETED, false)
            },
            educationSystem = prefs.getString(KEY_CACHED_EDU_SYSTEM, "").orEmpty(),
            studentLevel = prefs.getString(KEY_CACHED_STUDENT_LEVEL, "").orEmpty(),
            studentStream = prefs.getString(KEY_CACHED_STUDENT_STREAM, "").orEmpty(),
            schoolName = prefs.getString(KEY_CACHED_SCHOOL_NAME, "").orEmpty(),
            schoolOwnership = prefs.getString(KEY_CACHED_SCHOOL_OWNERSHIP, "").orEmpty(),
            schoolAge = prefs.getString(KEY_CACHED_SCHOOL_AGE, "").orEmpty(),
            schoolEmail = prefs.getString(KEY_CACHED_SCHOOL_EMAIL, "").orEmpty(),
            schoolFacebookUrl = prefs.getString(KEY_CACHED_SCHOOL_FB, "").orEmpty(),
            teachingYears = prefs.getString(KEY_CACHED_TEACHING_YEARS, "").orEmpty(),
            teacherSchools = prefs.getString(KEY_CACHED_TEACHER_SCHOOLS, "").orEmpty(),
            pseudonym = prefs.getString(KEY_CACHED_PSEUDONYM, "").orEmpty(),
            phoneNumbers = prefs.getString(KEY_CACHED_PHONES, "").orEmpty(),
            childIds = prefs.getString(KEY_CACHED_CHILD_IDS, "").orEmpty()
        )
    }

    fun getRememberedAccount(): RememberedAccount? {
        val uid = prefs.getString(KEY_REMEMBERED_UID, null)?.takeIf { it.isNotBlank() } ?: return null
        val name = prefs.getString(KEY_REMEMBERED_NAME, null)?.takeIf { it.isNotBlank() } ?: return null
        val email = prefs.getString(KEY_REMEMBERED_EMAIL, "").orEmpty()
        val photo = prefs.getString(KEY_REMEMBERED_PHOTO, "").orEmpty()
        val headline = prefs.getString(KEY_REMEMBERED_HEADLINE, "Verified Aura Profile Member").orEmpty()
        val role = prefs.getString(KEY_REMEMBERED_ROLE, "Member").orEmpty()
        return RememberedAccount(
            userId = uid,
            displayName = name,
            email = email,
            photoUrl = photo,
            headline = headline,
            roleTitle = role
        )
    }

    fun hasLoggedSessionEventForUid(userId: String): Boolean {
        return prefs.getString(KEY_LAST_LOGGED_SESSION_UID, null) == userId &&
            !wasExplicitlySignedOut()
    }

    fun markSessionEventLogged(userId: String) {
        prefs.edit().putString(KEY_LAST_LOGGED_SESSION_UID, userId).apply()
    }

    fun onExplicitSignOut() {
        prefs.edit()
            .putBoolean(KEY_EXPLICITLY_SIGNED_OUT, true)
            .putString(KEY_PREFERRED_AUTH_MODE, AuthMode.LOGIN.name)
            .remove(KEY_LAST_LOGGED_SESSION_UID)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "aura_persistent_session_prefs"
        private const val KEY_KEEP_SIGNED_IN = "keep_signed_in"
        private const val KEY_HAS_CREATED_ACCOUNT = "has_created_account"
        private const val KEY_EXPLICITLY_SIGNED_OUT = "explicitly_signed_out"
        private const val KEY_PREFERRED_AUTH_MODE = "preferred_auth_mode"
        private const val KEY_REMEMBERED_UID = "remembered_uid"
        private const val KEY_REMEMBERED_NAME = "remembered_name"
        private const val KEY_REMEMBERED_EMAIL = "remembered_email"
        private const val KEY_REMEMBERED_PHOTO = "remembered_photo"
        private const val KEY_REMEMBERED_HEADLINE = "remembered_headline"
        private const val KEY_REMEMBERED_ROLE = "remembered_role"
        private const val KEY_CACHED_BIO = "cached_bio"
        private const val KEY_CACHED_LOCATION = "cached_location"
        private const val KEY_CACHED_WEBSITE = "cached_website"
        private const val KEY_CACHED_STATUS = "cached_status"
        private const val KEY_CACHED_ACTOR_ROLE = "cached_actor_role"
        private const val KEY_CACHED_ACCOUNT_STATUS = "cached_account_status"
        private const val KEY_CACHED_ONBOARDING_COMPLETED = "cached_onboarding_completed"
        private const val KEY_CACHED_EDU_SYSTEM = "cached_edu_system"
        private const val KEY_CACHED_STUDENT_LEVEL = "cached_student_level"
        private const val KEY_CACHED_STUDENT_STREAM = "cached_student_stream"
        private const val KEY_CACHED_SCHOOL_NAME = "cached_school_name"
        private const val KEY_CACHED_SCHOOL_OWNERSHIP = "cached_school_ownership"
        private const val KEY_CACHED_SCHOOL_AGE = "cached_school_age"
        private const val KEY_CACHED_SCHOOL_EMAIL = "cached_school_email"
        private const val KEY_CACHED_SCHOOL_FB = "cached_school_fb"
        private const val KEY_CACHED_TEACHING_YEARS = "cached_teaching_years"
        private const val KEY_CACHED_TEACHER_SCHOOLS = "cached_teacher_schools"
        private const val KEY_CACHED_PSEUDONYM = "cached_pseudonym"
        private const val KEY_CACHED_PHONES = "cached_phones"
        private const val KEY_CACHED_CHILD_IDS = "cached_child_ids"
        private const val KEY_LAST_LOGGED_SESSION_UID = "last_logged_session_uid"
    }
}
