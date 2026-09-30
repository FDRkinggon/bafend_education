package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import java.net.Inet4Address
import java.net.NetworkInterface

enum class ActivityEventType(val code: String, val label: String) {
    AUTH_SIGN_IN("AUTH_SIGN_IN", "User Sign-In (Entry)"),
    PROFILE_CREATED("PROFILE_CREATED", "Profile Provisioned (Entry)"),
    ROLE_ONBOARDING_COMPLETED("ROLE_ONBOARDING_COMPLETED", "Role Onboarding Completed"),
    STUDENT_ASSESSMENT_LOGGED("STUDENT_ASSESSMENT_LOGGED", "Student Score / Study Logged"),
    STUDENT_ORIENTATION_ANALYZED("STUDENT_ORIENTATION_ANALYZED", "Student High-Thinking Orientation"),
    PROFILE_UPDATED("PROFILE_UPDATED", "Profile Updated"),
    STATUS_CHANGED("STATUS_CHANGED", "Status Changed"),
    AI_ADVISOR_RUN("AI_ADVISOR_RUN", "AI Advisor Analysis"),
    AI_SUGGESTION_APPLIED("AI_SUGGESTION_APPLIED", "AI Suggestion Applied"),
    ADMIN_AUDIT_ACTION("ADMIN_AUDIT_ACTION", "Admin Audit Event"),
    USER_ROLE_CHANGED("USER_ROLE_CHANGED", "User Role Changed"),
    USER_STATUS_CHANGED("USER_STATUS_CHANGED", "User Account Status Changed"),
    USER_DELETED("USER_DELETED", "User Account Deleted (Exit)"),
    DB_CONNECTION_ATTEMPT("DB_CONNECTION_ATTEMPT", "IP Database Connection Attempt"),
    DB_OPERATION_SUCCESS("DB_OPERATION_SUCCESS", "Database Operation Success"),
    DB_OPERATION_FAILURE("DB_OPERATION_FAILURE", "Database Operation Failure"),
    DB_INJECTION_BLOCKED("DB_INJECTION_BLOCKED", "Database Injection / Tamper Alert"),
    AUTH_SIGN_OUT("AUTH_SIGN_OUT", "User Sign-Out (Exit)");

    val isEntryEvent: Boolean
        get() = this == AUTH_SIGN_IN || this == PROFILE_CREATED || this == ROLE_ONBOARDING_COMPLETED

    val isExitEvent: Boolean
        get() = this == AUTH_SIGN_OUT || this == USER_DELETED

    val isDatabaseSecurityLog: Boolean
        get() = this == DB_CONNECTION_ATTEMPT ||
            this == DB_OPERATION_SUCCESS ||
            this == DB_OPERATION_FAILURE ||
            this == DB_INJECTION_BLOCKED ||
            this == ADMIN_AUDIT_ACTION ||
            this == USER_ROLE_CHANGED ||
            this == USER_STATUS_CHANGED ||
            this == USER_DELETED

    companion object {
        fun fromCode(code: String): ActivityEventType {
            return entries.firstOrNull { it.code == code } ?: PROFILE_UPDATED
        }
    }
}

enum class ActivitySeverity(val code: String, val label: String) {
    INFO("INFO", "Info"),
    SUCCESS("SUCCESS", "Success"),
    HIGHLIGHT("HIGHLIGHT", "AI / Highlight"),
    SECURITY("SECURITY", "Security");

    companion object {
        fun fromCode(code: String): ActivitySeverity {
            return entries.firstOrNull { it.code == code } ?: INFO
        }
    }
}

enum class ActivityReviewStatus(val code: String, val label: String) {
    RECORDED("RECORDED", "Recorded"),
    REVIEWED("REVIEWED", "Reviewed"),
    FLAGGED("FLAGGED", "Flagged");

    companion object {
        fun fromCode(code: String): ActivityReviewStatus {
            return entries.firstOrNull { it.code == code } ?: RECORDED
        }
    }
}

object DatabaseSecurityInspector {
    private val injectionPatterns = listOf(
        Regex("(\\\$where|\\\$gt|\\\$lt|\\\$ne|\\\$nin|\\\$regex|\\\$or)", RegexOption.IGNORE_CASE),
        Regex("""('|")\s*(OR|AND)\s*('|")?\d+('|")?\s*=\s*('|")?\d+""", RegexOption.IGNORE_CASE),
        Regex("""(DROP\s+TABLE|UNION\s+SELECT|INSERT\s+INTO|DELETE\s+FROM|--\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""(<script|javascript:|onerror=|onload=|__proto__|constructor\.prototype|\.\./)""", RegexOption.IGNORE_CASE)
    )

    fun detectClientIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "10.0.2.15"
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank()) return host
                    }
                }
            }
            "10.0.2.15"
        } catch (_: Exception) {
            "10.0.2.15"
        }
    }

    fun containsInjectionSignature(payload: String): Boolean {
        if (payload.isBlank()) return false
        return injectionPatterns.any { it.containsMatchIn(payload) }
    }

    fun matchedSignatureDescription(payload: String): String {
        if (payload.isBlank()) return "Clean payload"
        return when {
            injectionPatterns[0].containsMatchIn(payload) -> "NoSQL Operator Injection (\$where / \$ne / \$gt)"
            injectionPatterns[1].containsMatchIn(payload) -> "Boolean Tautology Injection (' OR '1'='1)"
            injectionPatterns[2].containsMatchIn(payload) -> "SQL DDL/DML Command Injection (DROP / UNION SELECT)"
            injectionPatterns[3].containsMatchIn(payload) -> "XSS / Prototype / Path Traversal Payload"
            else -> "Suspicious Direct Database Query Pattern"
        }
    }
}

data class AppActivity(
    val activityId: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val userEmail: String = "",
    val eventType: String = ActivityEventType.PROFILE_UPDATED.code,
    val title: String = "",
    val details: String = "",
    val severity: String = ActivitySeverity.INFO.code,
    val reviewStatus: String = ActivityReviewStatus.RECORDED.code,
    val ipAddress: String = "",
    val targetPath: String = "",
    val dbStatus: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val resolvedIpAddress: String
        get() = ipAddress.ifBlank {
            // Extract IP from details if embedded or fallback to detected client IP
            Regex("""IP:\s*([0-9a-fA-F.:]+)""").find(details)?.groupValues?.getOrNull(1)
                ?: "10.0.2.15"
        }

    val resolvedTargetPath: String
        get() = targetPath.ifBlank {
            when (ActivityEventType.fromCode(eventType)) {
                ActivityEventType.PROFILE_CREATED,
                ActivityEventType.PROFILE_UPDATED,
                ActivityEventType.ROLE_ONBOARDING_COMPLETED,
                ActivityEventType.STATUS_CHANGED,
                ActivityEventType.USER_ROLE_CHANGED,
                ActivityEventType.USER_STATUS_CHANGED,
                ActivityEventType.USER_DELETED -> "/users/$userId"
                ActivityEventType.ADMIN_AUDIT_ACTION -> "/admins/$userId"
                else -> "/activities/$activityId"
            }
        }

    val resolvedDbStatus: String
        get() = dbStatus.ifBlank {
            when (ActivityEventType.fromCode(eventType)) {
                ActivityEventType.DB_CONNECTION_ATTEMPT -> "ATTEMPT"
                ActivityEventType.DB_OPERATION_FAILURE -> "FAILURE"
                ActivityEventType.DB_INJECTION_BLOCKED -> "INJECTION_BLOCKED"
                else -> "SUCCESS"
            }
        }

    fun toCreatePayload(): Map<String, Any> {
        val base = mutableMapOf<String, Any>(
            "activityId" to activityId.trim().take(128),
            "userId" to userId.trim().take(128),
            "userDisplayName" to userDisplayName.trim().ifEmpty { "Aura Member" }.take(100),
            "userEmail" to userEmail.trim().take(200),
            "eventType" to eventType.trim(),
            "title" to title.trim().ifEmpty { "Application Activity" }.take(160),
            "details" to details.trim().take(1000),
            "severity" to severity.trim(),
            "reviewStatus" to ActivityReviewStatus.RECORDED.code,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (ipAddress.isNotBlank()) {
            base["ipAddress"] = ipAddress.trim().take(100)
        }
        if (targetPath.isNotBlank()) {
            base["targetPath"] = targetPath.trim().take(200)
        }
        if (dbStatus.isNotBlank()) {
            base["dbStatus"] = dbStatus.trim().take(60)
        }
        return base
    }

    companion object {
        fun fromSnapshot(snapshot: DocumentSnapshot): AppActivity? {
            if (!snapshot.exists()) return null
            return snapshot.toObject(
                AppActivity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.copy(
                activityId = snapshot.getString("activityId") ?: snapshot.id,
                userId = snapshot.getString("userId").orEmpty(),
                ipAddress = snapshot.getString("ipAddress").orEmpty(),
                targetPath = snapshot.getString("targetPath").orEmpty(),
                dbStatus = snapshot.getString("dbStatus").orEmpty(),
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

data class AdminRecord(
    val adminId: String = "",
    val email: String = "",
    val role: String = "SUPER_ADMIN",
    val grantedBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreatePayload(): Map<String, Any> {
        return mapOf(
            "adminId" to adminId.trim().take(128),
            "email" to email.trim().ifEmpty { "admin@auraprofile.app" }.take(200),
            "role" to role.trim(),
            "grantedBy" to grantedBy.trim().take(128),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}
