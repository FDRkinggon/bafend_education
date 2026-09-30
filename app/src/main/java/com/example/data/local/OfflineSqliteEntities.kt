package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AdminCustomSubject
import com.example.data.model.AppActivity
import com.example.data.model.RevisionBafendDocument
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import java.util.Date

@Entity(tableName = "cached_user_profiles")
data class CachedUserProfileEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val email: String,
    val headline: String,
    val bio: String,
    val location: String,
    val roleTitle: String,
    val website: String,
    val statusMessage: String,
    val photoUrl: String,
    val actorRole: String,
    val accountStatus: String,
    val onboardingCompleted: Boolean,
    val educationSystem: String,
    val studentLevel: String,
    val studentStream: String,
    val studentSeries: String,
    val selectedSubjects: String,
    val schoolName: String,
    val schoolOwnership: String,
    val schoolAge: String,
    val schoolEmail: String,
    val schoolFacebookUrl: String,
    val teachingYears: String,
    val teacherSchools: String,
    val pseudonym: String,
    val phoneNumbers: String,
    val childIds: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val isPendingOfflineSync: Boolean = false
) {
    fun toDomainModel(): UserProfile = UserProfile(
        userId = userId,
        displayName = displayName,
        email = email,
        headline = headline,
        bio = bio,
        location = location,
        roleTitle = roleTitle,
        website = website,
        statusMessage = statusMessage,
        photoUrl = photoUrl,
        actorRole = actorRole,
        accountStatus = accountStatus,
        onboardingCompleted = onboardingCompleted,
        educationSystem = educationSystem,
        studentLevel = studentLevel,
        studentStream = studentStream,
        studentSeries = studentSeries,
        selectedSubjects = selectedSubjects,
        schoolName = schoolName,
        schoolOwnership = schoolOwnership,
        schoolAge = schoolAge,
        schoolEmail = schoolEmail,
        schoolFacebookUrl = schoolFacebookUrl,
        teachingYears = teachingYears,
        teacherSchools = teacherSchools,
        pseudonym = pseudonym,
        phoneNumbers = phoneNumbers,
        childIds = childIds,
        createdAt = Timestamp(Date(createdAtEpochMs)),
        updatedAt = Timestamp(Date(updatedAtEpochMs))
    )

    companion object {
        fun fromDomainModel(profile: UserProfile, isPendingOfflineSync: Boolean = false): CachedUserProfileEntity {
            val nowMs = System.currentTimeMillis()
            return CachedUserProfileEntity(
                userId = profile.userId.ifBlank { "local_user" },
                displayName = profile.displayName,
                email = profile.email,
                headline = profile.headline,
                bio = profile.bio,
                location = profile.location,
                roleTitle = profile.roleTitle,
                website = profile.website,
                statusMessage = profile.statusMessage,
                photoUrl = profile.photoUrl,
                actorRole = profile.actorRole,
                accountStatus = profile.accountStatus,
                onboardingCompleted = profile.onboardingCompleted,
                educationSystem = profile.educationSystem,
                studentLevel = profile.studentLevel,
                studentStream = profile.studentStream,
                studentSeries = profile.studentSeries,
                selectedSubjects = profile.selectedSubjects,
                schoolName = profile.schoolName,
                schoolOwnership = profile.schoolOwnership,
                schoolAge = profile.schoolAge,
                schoolEmail = profile.schoolEmail,
                schoolFacebookUrl = profile.schoolFacebookUrl,
                teachingYears = profile.teachingYears,
                teacherSchools = profile.teacherSchools,
                pseudonym = profile.pseudonym,
                phoneNumbers = profile.phoneNumbers,
                childIds = profile.childIds,
                createdAtEpochMs = profile.createdAt?.toDate()?.time ?: nowMs,
                updatedAtEpochMs = profile.updatedAt?.toDate()?.time ?: nowMs,
                isPendingOfflineSync = isPendingOfflineSync
            )
        }
    }
}

@Entity(tableName = "cached_activities")
data class CachedActivityEntity(
    @PrimaryKey val activityId: String,
    val userId: String,
    val userDisplayName: String,
    val userEmail: String,
    val eventType: String,
    val title: String,
    val details: String,
    val severity: String,
    val reviewStatus: String,
    val ipAddress: String,
    val targetPath: String,
    val dbStatus: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val isPendingOfflineSync: Boolean = false
) {
    fun toDomainModel(): AppActivity = AppActivity(
        activityId = activityId,
        userId = userId,
        userDisplayName = userDisplayName,
        userEmail = userEmail,
        eventType = eventType,
        title = title,
        details = details,
        severity = severity,
        reviewStatus = reviewStatus,
        ipAddress = ipAddress,
        targetPath = targetPath,
        dbStatus = dbStatus,
        createdAt = Timestamp(Date(createdAtEpochMs)),
        updatedAt = Timestamp(Date(updatedAtEpochMs))
    )

    companion object {
        fun fromDomainModel(activity: AppActivity, isPendingOfflineSync: Boolean = false): CachedActivityEntity {
            val nowMs = System.currentTimeMillis()
            return CachedActivityEntity(
                activityId = activity.activityId.ifBlank { "act_$nowMs" },
                userId = activity.userId,
                userDisplayName = activity.userDisplayName,
                userEmail = activity.userEmail,
                eventType = activity.eventType,
                title = activity.title,
                details = activity.details,
                severity = activity.severity,
                reviewStatus = activity.reviewStatus,
                ipAddress = activity.ipAddress,
                targetPath = activity.targetPath,
                dbStatus = activity.dbStatus,
                createdAtEpochMs = activity.createdAt?.toDate()?.time ?: nowMs,
                updatedAtEpochMs = activity.updatedAt?.toDate()?.time ?: nowMs,
                isPendingOfflineSync = isPendingOfflineSync
            )
        }
    }
}

@Entity(tableName = "cached_custom_subjects")
data class CachedCustomSubjectEntity(
    @PrimaryKey val subjectId: String,
    val code: String,
    val name: String,
    val coefficient: Int,
    val category: String,
    val systemCode: String,
    val levelCode: String,
    val streamCode: String,
    val seriesCode: String,
    val seriesName: String,
    val description: String,
    val createdByEmail: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val isPendingOfflineSync: Boolean = false
) {
    fun toDomainModel(): AdminCustomSubject = AdminCustomSubject(
        subjectId = subjectId,
        code = code,
        name = name,
        coefficient = coefficient,
        category = category,
        systemCode = systemCode,
        levelCode = levelCode,
        streamCode = streamCode,
        seriesCode = seriesCode,
        seriesName = seriesName,
        description = description,
        createdByEmail = createdByEmail,
        createdAt = Timestamp(Date(createdAtEpochMs)),
        updatedAt = Timestamp(Date(updatedAtEpochMs))
    )

    companion object {
        fun fromDomainModel(subject: AdminCustomSubject, isPendingOfflineSync: Boolean = false): CachedCustomSubjectEntity {
            val nowMs = System.currentTimeMillis()
            return CachedCustomSubjectEntity(
                subjectId = subject.subjectId.ifBlank { "subj_$nowMs" },
                code = subject.code,
                name = subject.name,
                coefficient = subject.coefficient,
                category = subject.category,
                systemCode = subject.systemCode,
                levelCode = subject.levelCode,
                streamCode = subject.streamCode,
                seriesCode = subject.seriesCode,
                seriesName = subject.seriesName,
                description = subject.description,
                createdByEmail = subject.createdByEmail,
                createdAtEpochMs = subject.createdAt?.toDate()?.time ?: nowMs,
                updatedAtEpochMs = subject.updatedAt?.toDate()?.time ?: nowMs,
                isPendingOfflineSync = isPendingOfflineSync
            )
        }
    }
}

@Entity(tableName = "cached_revision_docs")
data class CachedRevisionDocEntity(
    @PrimaryKey val docId: String,
    val title: String,
    val description: String,
    val systemCode: String,
    val levelCode: String,
    val streamCode: String,
    val seriesCode: String,
    val subjectCode: String,
    val subjectName: String,
    val subjectCategory: String,
    val coefficient: Int,
    val examYear: String = "2024",
    val originalPdfName: String,
    val bafendFileName: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val bafendBase64Payload: String,
    val questionDriveFileId: String = "",
    val questionDriveWebUrl: String = "",
    val hasSolution: Boolean = false,
    val pairId: String = "",
    val solutionOriginalPdfName: String = "",
    val solutionBafendFileName: String = "",
    val solutionPageCount: Int = 0,
    val solutionFileSizeBytes: Long = 0L,
    val solutionBafendBase64Payload: String = "",
    val solutionDriveFileId: String = "",
    val solutionDriveWebUrl: String = "",
    val uploadedByEmail: String,
    val isDownloadedLocally: Boolean = false,
    val isSolutionDownloadedLocally: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val isPendingOfflineSync: Boolean = false
) {
    fun toDomainModel(): RevisionBafendDocument = RevisionBafendDocument(
        docId = docId,
        title = title,
        description = description,
        systemCode = systemCode,
        levelCode = levelCode,
        streamCode = streamCode,
        seriesCode = seriesCode,
        subjectCode = subjectCode,
        subjectName = subjectName,
        subjectCategory = subjectCategory,
        coefficient = coefficient,
        examYear = examYear.ifBlank { "2024" },
        originalPdfName = originalPdfName,
        bafendFileName = bafendFileName,
        pageCount = pageCount,
        fileSizeBytes = fileSizeBytes,
        bafendBase64Payload = bafendBase64Payload,
        questionDriveFileId = questionDriveFileId,
        questionDriveWebUrl = questionDriveWebUrl,
        hasSolution = hasSolution,
        pairId = pairId,
        solutionOriginalPdfName = solutionOriginalPdfName,
        solutionBafendFileName = solutionBafendFileName,
        solutionPageCount = solutionPageCount,
        solutionFileSizeBytes = solutionFileSizeBytes,
        solutionBafendBase64Payload = solutionBafendBase64Payload,
        solutionDriveFileId = solutionDriveFileId,
        solutionDriveWebUrl = solutionDriveWebUrl,
        uploadedByEmail = uploadedByEmail,
        createdAt = Timestamp(Date(createdAtEpochMs)),
        updatedAt = Timestamp(Date(updatedAtEpochMs))
    )

    companion object {
        fun fromDomainModel(
            doc: RevisionBafendDocument,
            isPendingOfflineSync: Boolean = false,
            isDownloadedLocally: Boolean = false,
            isSolutionDownloadedLocally: Boolean = false
        ): CachedRevisionDocEntity {
            val nowMs = System.currentTimeMillis()
            return CachedRevisionDocEntity(
                docId = doc.docId.ifBlank { "bafend_$nowMs" },
                title = doc.title,
                description = doc.description,
                systemCode = doc.systemCode,
                levelCode = doc.levelCode,
                streamCode = doc.streamCode,
                seriesCode = doc.seriesCode,
                subjectCode = doc.subjectCode,
                subjectName = doc.subjectName,
                subjectCategory = doc.subjectCategory,
                coefficient = doc.coefficient,
                examYear = doc.examYear.ifBlank { "2024" },
                originalPdfName = doc.originalPdfName,
                bafendFileName = doc.bafendFileName,
                pageCount = doc.pageCount,
                fileSizeBytes = doc.fileSizeBytes,
                bafendBase64Payload = doc.bafendBase64Payload,
                questionDriveFileId = doc.resolvedQuestionDriveId,
                questionDriveWebUrl = doc.questionDriveWebUrl,
                hasSolution = doc.hasSolution,
                pairId = doc.resolvedPairId,
                solutionOriginalPdfName = doc.solutionOriginalPdfName,
                solutionBafendFileName = doc.solutionBafendFileName,
                solutionPageCount = doc.solutionPageCount,
                solutionFileSizeBytes = doc.solutionFileSizeBytes,
                solutionBafendBase64Payload = doc.solutionBafendBase64Payload,
                solutionDriveFileId = doc.resolvedSolutionDriveId,
                solutionDriveWebUrl = doc.solutionDriveWebUrl,
                uploadedByEmail = doc.uploadedByEmail,
                isDownloadedLocally = isDownloadedLocally,
                isSolutionDownloadedLocally = isSolutionDownloadedLocally,
                createdAtEpochMs = doc.createdAt?.toDate()?.time ?: nowMs,
                updatedAtEpochMs = doc.updatedAt?.toDate()?.time ?: nowMs,
                isPendingOfflineSync = isPendingOfflineSync
            )
        }
    }
}

@Entity(tableName = "offline_sync_outbox")
data class OfflineSyncActionEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0L,
    val actionType: String,
    val targetCollection: String,
    val targetDocumentId: String,
    val summaryLabel: String,
    val payloadJson: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null
)

object OfflineSyncActionType {
    const val UPSERT_USER_PROFILE = "UPSERT_USER_PROFILE"
    const val DELETE_USER_PROFILE = "DELETE_USER_PROFILE"
    const val LOG_ACTIVITY = "LOG_ACTIVITY"
    const val UPDATE_ACTIVITY_REVIEW = "UPDATE_ACTIVITY_REVIEW"
    const val DELETE_ACTIVITY = "DELETE_ACTIVITY"
    const val ADD_CUSTOM_SUBJECT = "ADD_CUSTOM_SUBJECT"
    const val DELETE_CUSTOM_SUBJECT = "DELETE_CUSTOM_SUBJECT"
    const val UPLOAD_REVISION_BAFEND = "UPLOAD_REVISION_BAFEND"
    const val DELETE_REVISION_BAFEND = "DELETE_REVISION_BAFEND"
}
