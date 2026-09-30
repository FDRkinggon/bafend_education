package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

enum class PlatformRole(val code: String, val label: String) {
    ADMIN("ADMIN", "Administrator"),
    VISITOR("VISITOR", "Visitor"),
    STUDENT("STUDENT", "Student"),
    TEACHER("TEACHER", "Teacher"),
    SCHOOL("SCHOOL", "School"),
    PARENT("PARENT", "Parent");

    companion object {
        fun fromCode(code: String): PlatformRole {
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: VISITOR
        }
    }
}

enum class AccountStatus(val code: String, val label: String) {
    ACTIVE("ACTIVE", "Active"),
    PENDING("PENDING", "Pending"),
    SUSPENDED("SUSPENDED", "Suspended"),
    BLOCKED("BLOCKED", "Blocked");

    companion object {
        fun fromCode(code: String): AccountStatus {
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: ACTIVE
        }
    }
}

enum class EducationSystem(val code: String, val label: String) {
    ANGLOPHONE("ANGLOPHONE", "Anglophone System"),
    FRANCOPHONE("FRANCOPHONE", "Francophone System"),
    BILINGUAL("BILINGUAL", "Bilingual (Anglophone & Francophone)");

    companion object {
        fun fromCode(code: String): EducationSystem? {
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
        }
    }
}

enum class StudentLevel(val code: String, val label: String) {
    ORDINARY_LEVEL("ORDINARY_LEVEL", "Ordinary Level"),
    ADVANCED_LEVEL("ADVANCED_LEVEL", "Advanced Level");

    companion object {
        fun fromCode(code: String): StudentLevel? {
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
        }
    }
}

enum class StudentStream(val code: String, val label: String) {
    SCIENCE("SCIENCE", "Science"),
    ART("ART", "Art");

    companion object {
        fun fromCode(code: String): StudentStream? {
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
        }
    }
}

enum class SchoolOwnership(val code: String, val label: String) {
    PRIVATE("PRIVATE", "Private School"),
    PUBLIC("PUBLIC", "Public School");

    companion object {
        fun fromCode(code: String): SchoolOwnership? {
            return entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
        }
    }
}

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val headline: String = "",
    val bio: String = "",
    val location: String = "",
    val roleTitle: String = "",
    val website: String = "",
    val statusMessage: String = "",
    val photoUrl: String = "",
    val actorRole: String = PlatformRole.VISITOR.code,
    val accountStatus: String = AccountStatus.ACTIVE.code,
    val onboardingCompleted: Boolean = false,
    val educationSystem: String = "",
    val studentLevel: String = "",
    val studentStream: String = "",
    val studentSeries: String = "",
    val selectedSubjects: String = "",
    val schoolName: String = "",
    val schoolOwnership: String = "",
    val schoolAge: String = "",
    val schoolEmail: String = "",
    val schoolFacebookUrl: String = "",
    val teachingYears: String = "",
    val teacherSchools: String = "",
    val pseudonym: String = "",
    val phoneNumbers: String = "",
    val childIds: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val isDesignatedAdminEmail: Boolean
        get() = email.trim().equals("fokoufdr@gmail.com", ignoreCase = true) ||
            email.trim().equals("fokoufdr@gmailcom", ignoreCase = true)

    val effectivePlatformRole: PlatformRole
        get() = if (isDesignatedAdminEmail) PlatformRole.ADMIN else PlatformRole.fromCode(actorRole)

    val effectiveAccountStatus: AccountStatus
        get() = if (isDesignatedAdminEmail) AccountStatus.ACTIVE else AccountStatus.fromCode(accountStatus)

    val isPendingAccount: Boolean
        get() = effectiveAccountStatus != AccountStatus.ACTIVE

    val isRestrictedAccount: Boolean
        get() = effectiveAccountStatus != AccountStatus.ACTIVE

    fun matchesPersonalInfoKeyword(rawQuery: String): Boolean {
        val q = rawQuery.trim().lowercase()
        if (q.isEmpty()) return true
        val personalInfoHaystack = listOf(
            displayName,
            email,
            userId,
            headline,
            bio,
            location,
            roleTitle,
            website,
            statusMessage,
            effectivePlatformRole.label,
            effectivePlatformRole.code,
            effectiveAccountStatus.label,
            effectiveAccountStatus.code,
            educationSystem,
            studentLevel,
            studentStream,
            studentSeries,
            selectedSubjects,
            schoolName,
            schoolOwnership,
            schoolEmail,
            schoolAge,
            teachingYears,
            teacherSchools,
            pseudonym,
            phoneNumbers,
            childIds
        ).joinToString(" ").lowercase()

        // Matches if the full query or every space-separated keyword is contained in the user's personal info
        return personalInfoHaystack.contains(q) ||
            q.split(" ").filter { it.isNotBlank() }.all { token -> personalInfoHaystack.contains(token) }
    }

    fun matchedPersonalInfoSummary(rawQuery: String): String {
        val q = rawQuery.trim().lowercase()
        if (q.isEmpty()) return ""
        val matchedFields = mutableListOf<String>()
        if (displayName.lowercase().contains(q)) matchedFields.add("Name")
        if (email.lowercase().contains(q)) matchedFields.add("Email")
        if (userId.lowercase().contains(q)) matchedFields.add("UID")
        if (headline.lowercase().contains(q) || roleTitle.lowercase().contains(q)) matchedFields.add("Title/Headline")
        if (bio.lowercase().contains(q)) matchedFields.add("Bio")
        if (location.lowercase().contains(q)) matchedFields.add("Location")
        if (phoneNumbers.lowercase().contains(q)) matchedFields.add("Phone")
        if (schoolName.lowercase().contains(q) || teacherSchools.lowercase().contains(q)) matchedFields.add("School")
        if (pseudonym.lowercase().contains(q)) matchedFields.add("Pseudonym")
        if (website.lowercase().contains(q)) matchedFields.add("Website")
        if (statusMessage.lowercase().contains(q)) matchedFields.add("Status Note")
        if (effectivePlatformRole.label.lowercase().contains(q)) matchedFields.add("Role")
        if (effectiveAccountStatus.label.lowercase().contains(q)) matchedFields.add("Account State")
        if (selectedSubjects.lowercase().contains(q) || studentSeries.lowercase().contains(q)) {
            matchedFields.add("Subjects/Series")
        }
        if (educationSystem.lowercase().contains(q) || studentLevel.lowercase().contains(q) || studentStream.lowercase().contains(q)) {
            matchedFields.add("Academic Info")
        }
        return if (matchedFields.isEmpty()) {
            "Personal Info Match"
        } else {
            "Matched in: ${matchedFields.take(4).joinToString(", ")}"
        }
    }

    val requiresRoleOnboarding: Boolean
        get() = !isDesignatedAdminEmail && effectivePlatformRole != PlatformRole.ADMIN && !onboardingCompleted

    val selectedSubjectsList: List<String>
        get() = selectedSubjects
            .split("|", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    val phoneNumbersList: List<String>
        get() = phoneNumbers
            .split("|", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    val teacherSchoolsList: List<String>
        get() = teacherSchools
            .split("|", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    val childIdsList: List<String>
        get() = childIds
            .split("|", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_CHILD_IDS)

    fun toCreatePayload(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "displayName" to displayName.trim().ifEmpty { "Aura Member" }.take(100),
            "email" to email.trim().take(200),
            "headline" to headline.trim().take(160),
            "bio" to bio.trim().take(1000),
            "location" to location.trim().take(120),
            "roleTitle" to roleTitle.trim().take(120),
            "website" to website.trim().take(250),
            "statusMessage" to statusMessage.trim().take(160),
            "photoUrl" to photoUrl.trim().take(500),
            "actorRole" to if (isDesignatedAdminEmail) PlatformRole.ADMIN.code else actorRole.trim().ifEmpty { PlatformRole.VISITOR.code },
            "accountStatus" to if (isDesignatedAdminEmail) AccountStatus.ACTIVE.code else AccountStatus.fromCode(accountStatus).code,
            "onboardingCompleted" to if (isDesignatedAdminEmail) true else onboardingCompleted,
            "educationSystem" to educationSystem.trim().take(60),
            "studentLevel" to studentLevel.trim().take(60),
            "studentStream" to studentStream.trim().take(60),
            "studentSeries" to studentSeries.trim().take(100),
            "selectedSubjects" to selectedSubjects.trim().take(1200),
            "schoolName" to schoolName.trim().take(200),
            "schoolOwnership" to schoolOwnership.trim().take(40),
            "schoolAge" to schoolAge.trim().take(40),
            "schoolEmail" to schoolEmail.trim().take(200),
            "schoolFacebookUrl" to schoolFacebookUrl.trim().take(300),
            "teachingYears" to teachingYears.trim().take(40),
            "teacherSchools" to teacherSchools.trim().take(500),
            "pseudonym" to pseudonym.trim().take(100),
            "phoneNumbers" to phoneNumbers.trim().take(500),
            "childIds" to childIds.trim().take(600),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toUpdatePayload(): Map<String, Any> {
        return mapOf(
            "displayName" to displayName.trim().ifEmpty { "Aura Member" }.take(100),
            "email" to email.trim().take(200),
            "headline" to headline.trim().take(160),
            "bio" to bio.trim().take(1000),
            "location" to location.trim().take(120),
            "roleTitle" to roleTitle.trim().take(120),
            "website" to website.trim().take(250),
            "statusMessage" to statusMessage.trim().take(160),
            "photoUrl" to photoUrl.trim().take(500),
            "actorRole" to if (isDesignatedAdminEmail) PlatformRole.ADMIN.code else actorRole.trim().ifEmpty { PlatformRole.VISITOR.code },
            "accountStatus" to if (isDesignatedAdminEmail) AccountStatus.ACTIVE.code else AccountStatus.fromCode(accountStatus).code,
            "onboardingCompleted" to if (isDesignatedAdminEmail) true else onboardingCompleted,
            "educationSystem" to educationSystem.trim().take(60),
            "studentLevel" to studentLevel.trim().take(60),
            "studentStream" to studentStream.trim().take(60),
            "studentSeries" to studentSeries.trim().take(100),
            "selectedSubjects" to selectedSubjects.trim().take(1200),
            "schoolName" to schoolName.trim().take(200),
            "schoolOwnership" to schoolOwnership.trim().take(40),
            "schoolAge" to schoolAge.trim().take(40),
            "schoolEmail" to schoolEmail.trim().take(200),
            "schoolFacebookUrl" to schoolFacebookUrl.trim().take(300),
            "teachingYears" to teachingYears.trim().take(40),
            "teacherSchools" to teacherSchools.trim().take(500),
            "pseudonym" to pseudonym.trim().take(100),
            "phoneNumbers" to phoneNumbers.trim().take(500),
            "childIds" to childIds.trim().take(600),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    val completionPercentage: Int
        get() {
            val fields = listOf(
                displayName.isNotBlank(),
                email.isNotBlank(),
                headline.isNotBlank(),
                bio.isNotBlank(),
                roleTitle.isNotBlank(),
                statusMessage.isNotBlank(),
                onboardingCompleted
            )
            val filledCount = fields.count { it }
            return ((filledCount.toFloat() / fields.size) * 100).toInt()
        }

    companion object {
        const val MAX_CHILD_IDS = 20

        val SUGGESTED_SCHOOLS = listOf(
            "Bilingual Grammar School Molyko (BGS Molyko)",
            "Government Bilingual High School Yaoundé (GBHS Yaoundé)",
            "Lycée Général Leclerc Yaoundé",
            "Sacred Heart College Mankon",
            "Collège Vogt Yaoundé",
            "Collège Libermann Douala",
            "Saint Joseph's College Sasse",
            "Lycée Joss Douala",
            "Cameroon Protestant College Bali (CPC Bali)",
            "Our Lady of Lourdes College Mankon",
            "Presbyterian Secondary School Mankon (PSS Mankon)",
            "Lycée Bilingue de Deïdo"
        )

        fun joinListField(items: List<String>, maxItems: Int = 50): String {
            return items
                .map { it.trim().replace("|", " ") }
                .filter { it.isNotEmpty() }
                .take(maxItems)
                .joinToString(" | ")
        }

        fun fromSnapshot(snapshot: DocumentSnapshot): UserProfile? {
            if (!snapshot.exists()) return null
            val email = snapshot.getString("email").orEmpty()
            val isDefaultAdmin = email.trim().equals("fokoufdr@gmail.com", ignoreCase = true) ||
                email.trim().equals("fokoufdr@gmailcom", ignoreCase = true)
            val defaultActorRole = if (isDefaultAdmin) PlatformRole.ADMIN.code else PlatformRole.VISITOR.code

            return snapshot.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.copy(
                userId = snapshot.getString("userId") ?: snapshot.id,
                actorRole = if (isDefaultAdmin) {
                    PlatformRole.ADMIN.code
                } else {
                    snapshot.getString("actorRole")?.takeIf { it.isNotBlank() } ?: defaultActorRole
                },
                accountStatus = if (isDefaultAdmin) {
                    AccountStatus.ACTIVE.code
                } else {
                    snapshot.getString("accountStatus")?.takeIf { it.isNotBlank() } ?: AccountStatus.ACTIVE.code
                },
                onboardingCompleted = if (isDefaultAdmin) {
                    true
                } else {
                    snapshot.getBoolean("onboardingCompleted") ?: false
                },
                createdAt = snapshot.getTimestamp(
                    "createdAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ),
                updatedAt = snapshot.getTimestamp(
                    "updatedAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        }
    }
}
