package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class AdminCustomSubject(
    val subjectId: String = "",
    val code: String = "",
    val name: String = "",
    val coefficient: Int = 4,
    val category: String = "Core",
    val systemCode: String = "ANGLOPHONE",
    val levelCode: String = "ADVANCED_LEVEL",
    val streamCode: String = "SCIENCE",
    val seriesCode: String = "S1",
    val seriesName: String = "Series S1",
    val description: String = "",
    val createdByEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCurriculumSubjectItem(): CurriculumSubjectItem {
        return CurriculumSubjectItem(
            code = code.trim(),
            name = name.trim(),
            coefficient = coefficient.coerceIn(1, 20),
            category = category.trim().ifBlank { "Core" },
            systemCode = systemCode.trim().ifBlank { "ANGLOPHONE" },
            levelCode = levelCode.trim().ifBlank { "ADVANCED_LEVEL" },
            streamCode = streamCode.trim().ifBlank { "SCIENCE" },
            seriesCode = seriesCode.trim().ifBlank { "GENERAL" },
            seriesName = seriesName.trim().ifBlank { seriesCode.trim().ifBlank { "General Series" } }
        )
    }

    fun toCreatePayload(): Map<String, Any> = mapOf(
        "subjectId" to subjectId.trim().take(128),
        "code" to code.trim().take(40),
        "name" to name.trim().take(160),
        "coefficient" to coefficient.coerceIn(1, 20),
        "category" to category.trim().ifBlank { "Core" }.take(80),
        "systemCode" to systemCode.trim().ifBlank { "ANGLOPHONE" }.take(60),
        "levelCode" to levelCode.trim().ifBlank { "ADVANCED_LEVEL" }.take(60),
        "streamCode" to streamCode.trim().ifBlank { "SCIENCE" }.take(60),
        "seriesCode" to seriesCode.trim().ifBlank { "GENERAL" }.take(80),
        "seriesName" to seriesName.trim().ifBlank { seriesCode.trim().ifBlank { "General Series" } }.take(120),
        "description" to description.trim().take(1000),
        "createdByEmail" to createdByEmail.trim().take(200),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        val SUBJECT_CATEGORIES = listOf(
            "Compulsory",
            "Principal",
            "Core",
            "Spécialité",
            "Elective",
            "Practical"
        )

        fun fromSnapshot(snapshot: DocumentSnapshot): AdminCustomSubject? {
            if (!snapshot.exists()) return null
            return AdminCustomSubject(
                subjectId = snapshot.getString("subjectId") ?: snapshot.id,
                code = snapshot.getString("code").orEmpty(),
                name = snapshot.getString("name").orEmpty(),
                coefficient = (snapshot.getLong("coefficient") ?: 4L).toInt().coerceIn(1, 20),
                category = snapshot.getString("category") ?: "Core",
                systemCode = snapshot.getString("systemCode") ?: "ANGLOPHONE",
                levelCode = snapshot.getString("levelCode") ?: "ADVANCED_LEVEL",
                streamCode = snapshot.getString("streamCode") ?: "SCIENCE",
                seriesCode = snapshot.getString("seriesCode") ?: "GENERAL",
                seriesName = snapshot.getString("seriesName") ?: "General Series",
                description = snapshot.getString("description").orEmpty(),
                createdByEmail = snapshot.getString("createdByEmail").orEmpty(),
                createdAt = snapshot.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                updatedAt = snapshot.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
        }
    }
}

@IgnoreExtraProperties
data class RevisionBafendDocument(
    val docId: String = "",
    val title: String = "",
    val description: String = "",
    val systemCode: String = "ANGLOPHONE",
    val levelCode: String = "ADVANCED_LEVEL",
    val streamCode: String = "SCIENCE",
    val seriesCode: String = "S1",
    val subjectCode: String = "0770",
    val subjectName: String = "Pure Mathematics With Mechanics",
    val subjectCategory: String = "Principal",
    val coefficient: Int = 5,
    val examYear: String = "2024",
    val originalPdfName: String = "",
    val bafendFileName: String = "",
    val pageCount: Int = 3,
    val fileSizeBytes: Long = 0L,
    val bafendBase64Payload: String = "",
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
    val uploadedByEmail: String = "",
    val isViewingSolutionInReader: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val formattedFileSize: String
        get() {
            val kb = (fileSizeBytes / 1024.0).coerceAtLeast(1.2)
            return if (kb >= 1024.0) {
                String.format(java.util.Locale.US, "%.2f MB", kb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.1f KB", kb)
            }
        }

    val formattedSolutionFileSize: String
        get() {
            val kb = (solutionFileSizeBytes / 1024.0).coerceAtLeast(1.2)
            return if (kb >= 1024.0) {
                String.format(java.util.Locale.US, "%.2f MB", kb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.1f KB", kb)
            }
        }

    val formattedExamSession: String
        get() = "June ${examYear.trim().ifBlank { "2024" }}"

    val resolvedPairId: String
        get() = pairId.trim().ifBlank { "pair_${examYear.trim().ifBlank { "2024" }}_${subjectCode.trim()}_${docId.takeLast(6)}" }

    val resolvedQuestionDriveId: String
        get() = questionDriveFileId.trim().ifBlank { "drive_q_${examYear.trim().ifBlank { "2024" }}_${subjectCode.trim()}_${docId.takeLast(6)}" }

    val resolvedSolutionDriveId: String
        get() = if (hasSolution) {
            solutionDriveFileId.trim().ifBlank { "drive_sol_${examYear.trim().ifBlank { "2024" }}_${subjectCode.trim()}_${docId.takeLast(6)}" }
        } else {
            ""
        }

    val systemDisplayLabel: String
        get() = EducationSystem.fromCode(systemCode)?.label ?: systemCode

    val levelDisplayLabel: String
        get() = StudentLevel.fromCode(levelCode)?.label ?: levelCode

    val streamDisplayLabel: String
        get() = StudentStream.fromCode(streamCode)?.label ?: streamCode

    fun asSolutionView(): RevisionBafendDocument = copy(isViewingSolutionInReader = true)

    fun asQuestionView(): RevisionBafendDocument = copy(isViewingSolutionInReader = false)

    fun toCreatePayload(): Map<String, Any> = mapOf(
        "docId" to docId.trim().take(128),
        "title" to title.trim().take(200),
        "description" to description.trim().take(1500),
        "systemCode" to systemCode.trim().ifBlank { "ANGLOPHONE" }.take(60),
        "levelCode" to levelCode.trim().ifBlank { "ADVANCED_LEVEL" }.take(60),
        "streamCode" to streamCode.trim().ifBlank { "SCIENCE" }.take(60),
        "seriesCode" to seriesCode.trim().ifBlank { "ALL" }.take(80),
        "subjectCode" to subjectCode.trim().ifBlank { "SUBJ" }.take(40),
        "subjectName" to subjectName.trim().ifBlank { "Subject" }.take(160),
        "subjectCategory" to subjectCategory.trim().ifBlank { "Core" }.take(80),
        "coefficient" to coefficient.coerceIn(1, 20),
        "examYear" to examYear.trim().ifBlank { "2024" }.take(20),
        "originalPdfName" to originalPdfName.trim().ifBlank { "document.pdf" }.take(240),
        "bafendFileName" to BafendFileEngine.ensureBafendExtension(bafendFileName.ifBlank { title }).take(240),
        "pageCount" to pageCount.coerceIn(1, 2000),
        "fileSizeBytes" to fileSizeBytes.coerceAtLeast(1L),
        "bafendBase64Payload" to bafendBase64Payload.take(440000),
        "questionDriveFileId" to resolvedQuestionDriveId.take(160),
        "questionDriveWebUrl" to questionDriveWebUrl.trim().ifBlank {
            "drive://admin-drive/past-questions/${examYear.trim().ifBlank { "2024" }}/${subjectCode.trim()}/${originalPdfName.trim().ifBlank { "question.pdf" }}"
        }.take(360),
        "hasSolution" to hasSolution,
        "pairId" to resolvedPairId.take(140),
        "solutionOriginalPdfName" to solutionOriginalPdfName.trim().take(240),
        "solutionBafendFileName" to (if (hasSolution) BafendFileEngine.ensureBafendExtension(solutionBafendFileName.ifBlank { "${title}_Solution" }) else "").take(240),
        "solutionPageCount" to solutionPageCount.coerceIn(0, 2000),
        "solutionFileSizeBytes" to solutionFileSizeBytes.coerceAtLeast(0L),
        "solutionBafendBase64Payload" to solutionBafendBase64Payload.take(440000),
        "solutionDriveFileId" to resolvedSolutionDriveId.take(160),
        "solutionDriveWebUrl" to (if (hasSolution) {
            solutionDriveWebUrl.trim().ifBlank {
                "drive://admin-drive/past-solutions/${examYear.trim().ifBlank { "2024" }}/${subjectCode.trim()}/${solutionOriginalPdfName.trim().ifBlank { "solution.pdf" }}"
            }
        } else "").take(360),
        "uploadedByEmail" to uploadedByEmail.trim().take(200),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        val AVAILABLE_EXAM_YEARS = listOf(
            "2025", "2024", "2023", "2022", "2021", "2020", "2019", "2018", "2017", "2016"
        )

        fun fromSnapshot(snapshot: DocumentSnapshot): RevisionBafendDocument? {
            if (!snapshot.exists()) return null
            val rawTitle = snapshot.getString("title").orEmpty()
            val rawBafend = snapshot.getString("bafendFileName").orEmpty()
            val hasSol = snapshot.getBoolean("hasSolution") ?: false
            val rawSolBafend = snapshot.getString("solutionBafendFileName").orEmpty()
            return RevisionBafendDocument(
                docId = snapshot.getString("docId") ?: snapshot.id,
                title = rawTitle,
                description = snapshot.getString("description").orEmpty(),
                systemCode = snapshot.getString("systemCode") ?: "ANGLOPHONE",
                levelCode = snapshot.getString("levelCode") ?: "ADVANCED_LEVEL",
                streamCode = snapshot.getString("streamCode") ?: "SCIENCE",
                seriesCode = snapshot.getString("seriesCode") ?: "ALL",
                subjectCode = snapshot.getString("subjectCode") ?: "SUBJ",
                subjectName = snapshot.getString("subjectName") ?: "Subject",
                subjectCategory = snapshot.getString("subjectCategory") ?: "Core",
                coefficient = (snapshot.getLong("coefficient") ?: 4L).toInt().coerceIn(1, 20),
                examYear = snapshot.getString("examYear")?.takeIf { it.isNotBlank() } ?: "2024",
                originalPdfName = snapshot.getString("originalPdfName") ?: "revision.pdf",
                bafendFileName = BafendFileEngine.ensureBafendExtension(rawBafend.ifBlank { rawTitle }),
                pageCount = (snapshot.getLong("pageCount") ?: 1L).toInt().coerceAtLeast(1),
                fileSizeBytes = (snapshot.getLong("fileSizeBytes") ?: 1024L).coerceAtLeast(1L),
                bafendBase64Payload = snapshot.getString("bafendBase64Payload").orEmpty(),
                questionDriveFileId = snapshot.getString("questionDriveFileId").orEmpty(),
                questionDriveWebUrl = snapshot.getString("questionDriveWebUrl").orEmpty(),
                hasSolution = hasSol,
                pairId = snapshot.getString("pairId").orEmpty(),
                solutionOriginalPdfName = snapshot.getString("solutionOriginalPdfName").orEmpty(),
                solutionBafendFileName = if (hasSol) {
                    BafendFileEngine.ensureBafendExtension(rawSolBafend.ifBlank { "${rawTitle}_Solution" })
                } else {
                    ""
                },
                solutionPageCount = (snapshot.getLong("solutionPageCount") ?: 0L).toInt().coerceAtLeast(0),
                solutionFileSizeBytes = (snapshot.getLong("solutionFileSizeBytes") ?: 0L).coerceAtLeast(0L),
                solutionBafendBase64Payload = snapshot.getString("solutionBafendBase64Payload").orEmpty(),
                solutionDriveFileId = snapshot.getString("solutionDriveFileId").orEmpty(),
                solutionDriveWebUrl = snapshot.getString("solutionDriveWebUrl").orEmpty(),
                uploadedByEmail = snapshot.getString("uploadedByEmail").orEmpty(),
                createdAt = snapshot.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                updatedAt = snapshot.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
        }
    }
}

data class AdminDriveUploadResult(
    val driveFileId: String,
    val driveWebUrl: String,
    val adminDriveVaultPath: String,
    val sizeBytes: Long
)

data class BafendOfflineFileResult(
    val filePath: String,
    val folderPath: String,
    val fileName: String,
    val sizeBytes: Long,
    val verifiedBafendSignature: Boolean
)

data class AdaptiveReaderLatencyProfile(
    val deviceClassLabel: String,
    val osCompatibilityLabel: String,
    val targetPageWidthPx: Int,
    val targetPageHeightPx: Int,
    val prefetchAdjacentPages: Int,
    val memoryCacheBudgetMb: Int,
    val estimatedRenderLatencyMs: Double
)
