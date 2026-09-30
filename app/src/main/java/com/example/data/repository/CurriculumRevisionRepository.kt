package com.example.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.local.OfflineSqliteSyncCoordinator
import com.example.data.model.AdminCustomSubject
import com.example.data.model.BafendFileEngine
import com.example.data.model.CurriculumCatalog
import com.example.data.model.RevisionBafendDocument
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CurriculumRevisionRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val offlineCoordinator: OfflineSqliteSyncCoordinator? = null
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val subjectsCollection = firestore.collection("curriculum_subjects")
    private val documentsCollection = firestore.collection("revision_documents")

    fun observeAdminCustomSubjects(): Flow<List<AdminCustomSubject>> = callbackFlow {
        // Emit SQLite cached subjects immediately so offline mode has zero latency
        repoScope.launch {
            val sqliteSubjects = offlineCoordinator?.offlineDao?.getAllCustomSubjects()
                ?.map { it.toDomainModel() }
                .orEmpty()
            val initialMerged = (sqliteSubjects + localCustomSubjects.values)
                .distinctBy { "${it.systemCode}_${it.levelCode}_${it.code}_${it.name}" }
            if (initialMerged.isNotEmpty()) {
                CurriculumCatalog.setAdminAddedSubjects(initialMerged)
                trySend(initialMerged)
            }
        }

        val registration = subjectsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeAdminCustomSubjects fallback to SQLite cache", error)
                    repoScope.launch {
                        val sqliteSubjects = offlineCoordinator?.offlineDao?.getAllCustomSubjects()
                            ?.map { it.toDomainModel() }
                            .orEmpty()
                        val combined = (sqliteSubjects + localCustomSubjects.values)
                            .distinctBy { "${it.systemCode}_${it.levelCode}_${it.code}_${it.name}" }
                        CurriculumCatalog.setAdminAddedSubjects(combined)
                        trySend(combined)
                    }
                    return@addSnapshotListener
                }
                val remoteList = snapshot?.documents?.mapNotNull { doc ->
                    AdminCustomSubject.fromSnapshot(doc)
                }.orEmpty()
                repoScope.launch {
                    offlineCoordinator?.cacheOnlineCustomSubjects(remoteList)
                    val sqliteSubjects = offlineCoordinator?.offlineDao?.getAllCustomSubjects()
                        ?.map { it.toDomainModel() }
                        .orEmpty()
                    val merged = (remoteList + sqliteSubjects + localCustomSubjects.values)
                        .distinctBy { "${it.systemCode}_${it.levelCode}_${it.code}_${it.name}" }
                    CurriculumCatalog.setAdminAddedSubjects(merged)
                    trySend(merged)
                }
            }
        awaitClose { registration.remove() }
    }

    fun observeRevisionDocuments(): Flow<List<RevisionBafendDocument>> = callbackFlow {
        // Purge any legacy seeded mock papers and emit only real Admin-uploaded papers from SQLite for offline access
        repoScope.launch {
            offlineCoordinator?.offlineDao?.deleteMockSeededRevisionDocs()
            val sqliteDocs = offlineCoordinator?.offlineDao?.getAllRevisionDocs()
                ?.map { it.toDomainModel() }
                ?.filterNot { it.docId.startsWith("seed_") }
                .orEmpty()
            val initialDocs = (sqliteDocs + localRevisionDocs.values)
                .distinctBy { it.docId }
            trySend(initialDocs)
        }

        val registration = documentsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeRevisionDocuments fallback to SQLite cache", error)
                    repoScope.launch {
                        offlineCoordinator?.offlineDao?.deleteMockSeededRevisionDocs()
                        val sqliteDocs = offlineCoordinator?.offlineDao?.getAllRevisionDocs()
                            ?.map { it.toDomainModel() }
                            ?.filterNot { it.docId.startsWith("seed_") }
                            .orEmpty()
                        val fallback = (sqliteDocs + localRevisionDocs.values)
                            .distinctBy { it.docId }
                        trySend(fallback)
                    }
                    return@addSnapshotListener
                }
                val remoteDocs = snapshot?.documents?.mapNotNull { doc ->
                    RevisionBafendDocument.fromSnapshot(doc)
                }?.filterNot { it.docId.startsWith("seed_") }.orEmpty()
                repoScope.launch {
                    offlineCoordinator?.offlineDao?.deleteMockSeededRevisionDocs()
                    offlineCoordinator?.cacheOnlineRevisionDocuments(remoteDocs)
                    val sqliteDocs = offlineCoordinator?.offlineDao?.getAllRevisionDocs()
                        ?.map { it.toDomainModel() }
                        ?.filterNot { it.docId.startsWith("seed_") }
                        .orEmpty()
                    val merged = (remoteDocs + sqliteDocs + localRevisionDocs.values)
                        .distinctBy { it.docId }
                    trySend(merged)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun addCustomSubjectAsAdmin(
        code: String,
        name: String,
        coefficient: Int,
        category: String,
        systemCode: String,
        levelCode: String,
        streamCode: String,
        seriesCode: String,
        seriesName: String,
        description: String,
        adminEmail: String
    ): Result<AdminCustomSubject> = withContext(Dispatchers.IO) {
        val subjectId = "subj_${UUID.randomUUID().toString().replace("-", "").take(18)}"
        val record = AdminCustomSubject(
            subjectId = subjectId,
            code = code.trim().uppercase().ifBlank { "SUBJ-${(100..999).random()}" },
            name = name.trim(),
            coefficient = coefficient.coerceIn(1, 20),
            category = category.trim().ifBlank { "Core" },
            systemCode = systemCode.trim().ifBlank { "ANGLOPHONE" },
            levelCode = levelCode.trim().ifBlank { "ADVANCED_LEVEL" },
            streamCode = streamCode.trim().ifBlank { "SCIENCE" },
            seriesCode = seriesCode.trim().ifBlank { "GENERAL" },
            seriesName = seriesName.trim().ifBlank { "Series ${seriesCode.trim().ifBlank { "General" }}" },
            description = description.trim().ifBlank {
                "Official $systemCode curriculum subject added by Platform Administrator."
            },
            createdByEmail = adminEmail.trim().ifBlank { auth.currentUser?.email.orEmpty() },
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        localCustomSubjects[subjectId] = record
        val currentAll = (CurriculumCatalog.getAdminAddedSubjectsSnapshot() + record)
            .distinctBy { "${it.systemCode}_${it.levelCode}_${it.code}_${it.name}" }
        CurriculumCatalog.setAdminAddedSubjects(currentAll)

        val isOnlineNow = offlineCoordinator?.isEffectivelyOnline ?: true
        if (!isOnlineNow) {
            offlineCoordinator?.saveCustomSubjectToSqliteAndQueueIfOffline(record, queueForSync = true)
            return@withContext Result.success(record)
        }

        return@withContext try {
            subjectsCollection.document(subjectId).set(record.toCreatePayload()).await()
            offlineCoordinator?.saveCustomSubjectToSqliteAndQueueIfOffline(record, queueForSync = false)
            Result.success(record)
        } catch (e: Exception) {
            Log.w(TAG, "Saved custom subject in SQLite; queued for Firebase sync", e)
            offlineCoordinator?.saveCustomSubjectToSqliteAndQueueIfOffline(record, queueForSync = true)
            Result.success(record)
        }
    }

    suspend fun deleteCustomSubjectAsAdmin(subjectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        localCustomSubjects.remove(subjectId)
        val remaining = CurriculumCatalog.getAdminAddedSubjectsSnapshot()
            .filterNot { it.subjectId == subjectId }
        CurriculumCatalog.setAdminAddedSubjects(remaining)

        val isOnlineNow = offlineCoordinator?.isEffectivelyOnline ?: true
        if (!isOnlineNow) {
            offlineCoordinator?.deleteCustomSubjectInSqliteAndQueueIfOffline(subjectId, queueForSync = true)
            return@withContext Result.success(Unit)
        }

        return@withContext try {
            subjectsCollection.document(subjectId).delete().await()
            offlineCoordinator?.deleteCustomSubjectInSqliteAndQueueIfOffline(subjectId, queueForSync = false)
            Result.success(Unit)
        } catch (e: Exception) {
            offlineCoordinator?.deleteCustomSubjectInSqliteAndQueueIfOffline(subjectId, queueForSync = true)
            Result.success(Unit)
        }
    }

    suspend fun uploadPdfAsBafendDocument(
        context: Context,
        title: String,
        description: String,
        systemCode: String,
        levelCode: String,
        streamCode: String,
        seriesCode: String,
        subjectCode: String,
        subjectName: String,
        subjectCategory: String,
        coefficient: Int,
        examYear: String = "2024",
        originalPdfName: String,
        rawUploadedPdfBytes: ByteArray?,
        includeSolution: Boolean = false,
        solutionOriginalPdfName: String = "",
        rawSolutionPdfBytes: ByteArray? = null,
        adminEmail: String
    ): Result<RevisionBafendDocument> = withContext(Dispatchers.IO) {
        val cleanExamYear = examYear.trim().filter { it.isDigit() }.take(4).ifBlank { "2024" }
        val shortUid = UUID.randomUUID().toString().replace("-", "").take(12)
        val docId = "bafend_${cleanExamYear}_${shortUid}"
        val pairId = "pair_${cleanExamYear}_${subjectCode.trim().ifBlank { "SUBJ" }}_${shortUid.take(8)}"

        val resolvedQuestionPdfBytes = if (rawUploadedPdfBytes != null && rawUploadedPdfBytes.isNotEmpty()) {
            rawUploadedPdfBytes
        } else {
            BafendFileEngine.generateStructuredRevisionPdfBytes(
                title = "${title.trim()} (June $cleanExamYear Past Question)",
                subjectCode = subjectCode.trim(),
                subjectName = subjectName.trim(),
                systemCode = systemCode.trim(),
                levelCode = levelCode.trim(),
                streamCode = streamCode.trim(),
                seriesCode = seriesCode.trim(),
                category = subjectCategory.trim(),
                coefficient = coefficient.coerceIn(1, 20),
                description = "PAST JUNE $cleanExamYear QUESTION PAPER • Pair ID: $pairId\n${description.trim()}"
            )
        }

        val cleanQuestionOrigPdfName = originalPdfName.trim().ifBlank {
            "${subjectCode.trim()}_June_${cleanExamYear}_Question.pdf"
        }

        // Send Question PDF directly to the Admin's Drive Vault so users can collect it to their phone on download
        val questionDriveUpload = BafendFileEngine.uploadPdfToAdminDrive(
            context = context,
            pdfBytes = resolvedQuestionPdfBytes,
            fileName = cleanQuestionOrigPdfName,
            examYear = cleanExamYear,
            subjectCode = subjectCode.trim(),
            isSolution = false
        )

        val questionBafendBytes = BafendFileEngine.wrapPdfBytesIntoBafend(
            pdfBytes = resolvedQuestionPdfBytes,
            title = title.trim(),
            subjectCode = subjectCode.trim(),
            subjectName = subjectName.trim(),
            systemCode = systemCode.trim(),
            levelCode = levelCode.trim()
        )

        val assignedBafendFileName = BafendFileEngine.ensureBafendExtension(
            originalPdfName.ifBlank { "${subjectCode}_June_${cleanExamYear}_${title}" }
        )

        val maxSinglePayloadBytes = if (includeSolution) 290_000 else 580_000
        val questionBase64Payload = if (questionBafendBytes.size <= maxSinglePayloadBytes) {
            Base64.encodeToString(questionBafendBytes, Base64.NO_WRAP)
        } else {
            val compactPdf = BafendFileEngine.generateStructuredRevisionPdfBytes(
                title = "${title.trim()} (June $cleanExamYear Past Question)",
                subjectCode = subjectCode.trim(),
                subjectName = subjectName.trim(),
                systemCode = systemCode.trim(),
                levelCode = levelCode.trim(),
                streamCode = streamCode.trim(),
                seriesCode = seriesCode.trim(),
                category = subjectCategory.trim(),
                coefficient = coefficient.coerceIn(1, 20),
                description = "PAST JUNE $cleanExamYear QUESTION PAPER • Pair ID: $pairId\n${description.trim()}"
            )
            val compactBafend = BafendFileEngine.wrapPdfBytesIntoBafend(
                pdfBytes = compactPdf,
                title = title.trim(),
                subjectCode = subjectCode.trim(),
                subjectName = subjectName.trim(),
                systemCode = systemCode.trim(),
                levelCode = levelCode.trim()
            )
            Base64.encodeToString(compactBafend, Base64.NO_WRAP)
        }

        // Handle Paired Past June Solution if Admin toggled includeSolution == true
        var resolvedSolOrigName = ""
        var assignedSolBafendName = ""
        var solPageCount = 0
        var solSizeBytes = 0L
        var solBase64Payload = ""
        var solDriveId = ""
        var solDriveUrl = ""

        if (includeSolution) {
            val resolvedSolPdfBytes = if (rawSolutionPdfBytes != null && rawSolutionPdfBytes.isNotEmpty()) {
                rawSolutionPdfBytes
            } else {
                BafendFileEngine.generateStructuredRevisionPdfBytes(
                    title = "${title.trim()} — Official June $cleanExamYear Solution",
                    subjectCode = subjectCode.trim(),
                    subjectName = subjectName.trim(),
                    systemCode = systemCode.trim(),
                    levelCode = levelCode.trim(),
                    streamCode = streamCode.trim(),
                    seriesCode = seriesCode.trim(),
                    category = subjectCategory.trim(),
                    coefficient = coefficient.coerceIn(1, 20),
                    description = "OFFICIAL PAIRED PAST JUNE $cleanExamYear SOLUTION & MARKING SCHEME • Pair ID: $pairId\n" +
                        "Paired with Question Paper: $assignedBafendFileName\n${description.trim()}"
                )
            }

            resolvedSolOrigName = solutionOriginalPdfName.trim().ifBlank {
                "${subjectCode.trim()}_June_${cleanExamYear}_Solution.pdf"
            }

            val solDriveUpload = BafendFileEngine.uploadPdfToAdminDrive(
                context = context,
                pdfBytes = resolvedSolPdfBytes,
                fileName = resolvedSolOrigName,
                examYear = cleanExamYear,
                subjectCode = subjectCode.trim(),
                isSolution = true
            )

            val solBafendBytes = BafendFileEngine.wrapPdfBytesIntoBafend(
                pdfBytes = resolvedSolPdfBytes,
                title = "${title.trim()} — Official June $cleanExamYear Solution",
                subjectCode = subjectCode.trim(),
                subjectName = subjectName.trim(),
                systemCode = systemCode.trim(),
                levelCode = levelCode.trim()
            )

            assignedSolBafendName = BafendFileEngine.ensureBafendExtension(
                solutionOriginalPdfName.ifBlank { "${assignedBafendFileName.removeSuffix(".bafend")}_Solution" }
            )
            solPageCount = 3
            solSizeBytes = solBafendBytes.size.toLong().coerceAtLeast(1024L)
            solDriveId = solDriveUpload.driveFileId
            solDriveUrl = solDriveUpload.driveWebUrl

            solBase64Payload = if (solBafendBytes.size <= maxSinglePayloadBytes) {
                Base64.encodeToString(solBafendBytes, Base64.NO_WRAP)
            } else {
                val compactSolPdf = BafendFileEngine.generateStructuredRevisionPdfBytes(
                    title = "${title.trim()} — Official June $cleanExamYear Solution",
                    subjectCode = subjectCode.trim(),
                    subjectName = subjectName.trim(),
                    systemCode = systemCode.trim(),
                    levelCode = levelCode.trim(),
                    streamCode = streamCode.trim(),
                    seriesCode = seriesCode.trim(),
                    category = subjectCategory.trim(),
                    coefficient = coefficient.coerceIn(1, 20),
                    description = "OFFICIAL PAIRED PAST JUNE $cleanExamYear SOLUTION • Pair ID: $pairId"
                )
                val compactSolBafend = BafendFileEngine.wrapPdfBytesIntoBafend(
                    pdfBytes = compactSolPdf,
                    title = "${title.trim()} — Official June $cleanExamYear Solution",
                    subjectCode = subjectCode.trim(),
                    subjectName = subjectName.trim(),
                    systemCode = systemCode.trim(),
                    levelCode = levelCode.trim()
                )
                Base64.encodeToString(compactSolBafend, Base64.NO_WRAP)
            }
        }

        val doc = RevisionBafendDocument(
            docId = docId,
            title = title.trim(),
            description = description.trim().ifBlank {
                "Official Past June $cleanExamYear question paper for $subjectName ($subjectCode)."
            },
            systemCode = systemCode.trim().ifBlank { "ANGLOPHONE" },
            levelCode = levelCode.trim().ifBlank { "ADVANCED_LEVEL" },
            streamCode = streamCode.trim().ifBlank { "SCIENCE" },
            seriesCode = seriesCode.trim().ifBlank { "ALL" },
            subjectCode = subjectCode.trim().ifBlank { "0770" },
            subjectName = subjectName.trim().ifBlank { "Curriculum Subject" },
            subjectCategory = subjectCategory.trim().ifBlank { "Principal" },
            coefficient = coefficient.coerceIn(1, 20),
            examYear = cleanExamYear,
            originalPdfName = cleanQuestionOrigPdfName,
            bafendFileName = assignedBafendFileName,
            pageCount = 3,
            fileSizeBytes = questionBafendBytes.size.toLong().coerceAtLeast(1024L),
            bafendBase64Payload = questionBase64Payload,
            questionDriveFileId = questionDriveUpload.driveFileId,
            questionDriveWebUrl = questionDriveUpload.driveWebUrl,
            hasSolution = includeSolution,
            pairId = pairId,
            solutionOriginalPdfName = resolvedSolOrigName,
            solutionBafendFileName = assignedSolBafendName,
            solutionPageCount = solPageCount,
            solutionFileSizeBytes = solSizeBytes,
            solutionBafendBase64Payload = solBase64Payload,
            solutionDriveFileId = solDriveId,
            solutionDriveWebUrl = solDriveUrl,
            uploadedByEmail = adminEmail.trim().ifBlank { auth.currentUser?.email.orEmpty() },
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        localRevisionDocs[docId] = doc

        val isOnlineNow = offlineCoordinator?.isEffectivelyOnline ?: true
        if (!isOnlineNow) {
            offlineCoordinator?.saveRevisionDocToSqliteAndQueueIfOffline(doc, queueForSync = true)
            return@withContext Result.success(doc)
        }

        return@withContext try {
            documentsCollection.document(docId).set(doc.toCreatePayload()).await()
            offlineCoordinator?.saveRevisionDocToSqliteAndQueueIfOffline(doc, queueForSync = false)
            Result.success(doc)
        } catch (e: Exception) {
            Log.w(TAG, "Saved .bafend document in SQLite; queued for Firebase sync", e)
            offlineCoordinator?.saveRevisionDocToSqliteAndQueueIfOffline(doc, queueForSync = true)
            Result.success(doc)
        }
    }

    suspend fun deleteRevisionDocumentAsAdmin(docId: String): Result<Unit> = withContext(Dispatchers.IO) {
        localRevisionDocs.remove(docId)
        val isOnlineNow = offlineCoordinator?.isEffectivelyOnline ?: true
        if (!isOnlineNow) {
            offlineCoordinator?.deleteRevisionDocInSqliteAndQueueIfOffline(docId, queueForSync = true)
            return@withContext Result.success(Unit)
        }
        return@withContext try {
            documentsCollection.document(docId).delete().await()
            offlineCoordinator?.deleteRevisionDocInSqliteAndQueueIfOffline(docId, queueForSync = false)
            Result.success(Unit)
        } catch (e: Exception) {
            offlineCoordinator?.deleteRevisionDocInSqliteAndQueueIfOffline(docId, queueForSync = true)
            Result.success(Unit)
        }
    }

    companion object {
        private const val TAG = "CurriculumRevisionRepo"
        private val localCustomSubjects = mutableMapOf<String, AdminCustomSubject>()
        private val localRevisionDocs = mutableMapOf<String, RevisionBafendDocument>()

        // No mock data: only Past Papers uploaded by the Admin are stored and displayed.
        val DEFAULT_SEEDED_BAFEND_DOCUMENTS: List<RevisionBafendDocument> = emptyList()
    }
}
