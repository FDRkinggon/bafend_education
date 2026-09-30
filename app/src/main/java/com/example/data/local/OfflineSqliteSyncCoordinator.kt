package com.example.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.model.AdminCustomSubject
import com.example.data.model.AppActivity
import com.example.data.model.CurriculumCatalog
import com.example.data.model.RevisionBafendDocument
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SqliteOfflineTelemetry(
    val isNetworkAvailable: Boolean = true,
    val isManualOfflineMode: Boolean = false,
    val isSyncingNow: Boolean = false,
    val cachedProfilesCount: Int = 0,
    val cachedActivitiesCount: Int = 0,
    val cachedSubjectsCount: Int = 0,
    val cachedRevisionDocsCount: Int = 0,
    val pendingActions: List<OfflineSyncActionEntity> = emptyList(),
    val lastSyncedEpochMs: Long? = null,
    val lastSyncSummary: String = "SQLite Phone Memory Ready • Auto-Sync Active"
) {
    val isEffectivelyOnline: Boolean
        get() = isNetworkAvailable && !isManualOfflineMode

    val pendingSyncCount: Int
        get() = pendingActions.size

    val totalCachedRecords: Int
        get() = cachedProfilesCount + cachedActivitiesCount + cachedSubjectsCount + cachedRevisionDocsCount
}

class OfflineSqliteSyncCoordinator(
    context: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    val offlineDao: AuraOfflineDao = AuraOfflineDatabase.getInstance(context).offlineDao()
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    private val _isNetworkAvailable = MutableStateFlow(true)
    private val _isManualOfflineMode = MutableStateFlow(false)
    private val _isSyncingNow = MutableStateFlow(false)
    private val _lastSyncedEpochMs = MutableStateFlow<Long?>(null)
    private val _lastSyncSummary = MutableStateFlow("SQLite Phone Memory Ready • Mirroring Online Data")

    val isManualOfflineMode: StateFlow<Boolean> = _isManualOfflineMode.asStateFlow()

    val isEffectivelyOnline: Boolean
        get() = _isNetworkAvailable.value && !_isManualOfflineMode.value

    val telemetryFlow: Flow<SqliteOfflineTelemetry> = combine(
        combine(_isNetworkAvailable, _isManualOfflineMode, _isSyncingNow) { net, manual, syncing ->
            Triple(net, manual, syncing)
        },
        combine(
            offlineDao.observeCachedProfilesCount(),
            offlineDao.observeCachedActivitiesCount(),
            offlineDao.observeCachedSubjectsCount(),
            offlineDao.observeCachedRevisionDocsCount()
        ) { pCount, aCount, sCount, dCount ->
            intArrayOf(pCount, aCount, sCount, dCount)
        },
        offlineDao.observePendingSyncActions(),
        _lastSyncedEpochMs,
        _lastSyncSummary
    ) { netTriple, counts, pendingList, lastSyncMs, summary ->
        SqliteOfflineTelemetry(
            isNetworkAvailable = netTriple.first,
            isManualOfflineMode = netTriple.second,
            isSyncingNow = netTriple.third,
            cachedProfilesCount = counts[0],
            cachedActivitiesCount = counts[1],
            cachedSubjectsCount = counts[2],
            cachedRevisionDocsCount = counts[3],
            pendingActions = pendingList,
            lastSyncedEpochMs = lastSyncMs,
            lastSyncSummary = summary
        )
    }

    init {
        scope.launch {
            _isNetworkAvailable.value = checkActiveNetwork(appContext)
            registerConnectivityMonitor()
        }
    }

    private fun checkActiveNetwork(context: Context): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(true)
    }

    private fun registerConnectivityMonitor() {
        runCatching {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isNetworkAvailable.value = true
                        if (!_isManualOfflineMode.value) {
                            scope.launch {
                                synchronizePendingOutboxWithFirebase()
                            }
                        }
                    }

                    override fun onLost(network: Network) {
                        _isNetworkAvailable.value = checkActiveNetwork(appContext)
                    }
                }
            )
        }
    }

    fun setManualOfflineMode(enabled: Boolean) {
        _isManualOfflineMode.value = enabled
        if (enabled) {
            _lastSyncSummary.value = "Offline SQLite Mode Active: Changes saved to phone memory & queued for Firebase sync."
            runCatching { db.disableNetwork() }
        } else {
            _lastSyncSummary.value = "Reconnected to Online Firebase: Synchronizing SQLite outbox…"
            runCatching { db.enableNetwork() }
            scope.launch {
                synchronizePendingOutboxWithFirebase()
            }
        }
    }

    // =========================================================================
    // 1. CACHE EVERYTHING THE USER SEES WHEN ONLINE INTO SQLITE PHONE MEMORY
    // =========================================================================

    suspend fun cacheOnlineUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        val existing = offlineDao.getUserProfileById(profile.userId)
        if (existing?.isPendingOfflineSync == true) {
            return@withContext
        }
        offlineDao.upsertUserProfile(
            CachedUserProfileEntity.fromDomainModel(profile, isPendingOfflineSync = false)
        )
    }

    suspend fun cacheOnlineUserProfiles(profiles: List<UserProfile>) = withContext(Dispatchers.IO) {
        val entities = profiles.mapNotNull { profile ->
            val existing = offlineDao.getUserProfileById(profile.userId)
            if (existing?.isPendingOfflineSync == true) {
                null
            } else {
                CachedUserProfileEntity.fromDomainModel(profile, isPendingOfflineSync = false)
            }
        }
        if (entities.isNotEmpty()) {
            offlineDao.upsertUserProfiles(entities)
        }
    }

    suspend fun cacheOnlineActivities(activities: List<AppActivity>) = withContext(Dispatchers.IO) {
        val entities = activities.mapNotNull { act ->
            val existing = offlineDao.getActivityById(act.activityId)
            if (existing?.isPendingOfflineSync == true) {
                null
            } else {
                CachedActivityEntity.fromDomainModel(act, isPendingOfflineSync = false)
            }
        }
        if (entities.isNotEmpty()) {
            offlineDao.upsertActivities(entities)
        }
    }

    suspend fun cacheOnlineCustomSubjects(subjects: List<AdminCustomSubject>) = withContext(Dispatchers.IO) {
        val entities = subjects.mapNotNull { subj ->
            val existing = offlineDao.getCustomSubjectById(subj.subjectId)
            if (existing?.isPendingOfflineSync == true) {
                null
            } else {
                CachedCustomSubjectEntity.fromDomainModel(subj, isPendingOfflineSync = false)
            }
        }
        if (entities.isNotEmpty()) {
            offlineDao.upsertCustomSubjects(entities)
        }
    }

    suspend fun cacheOnlineRevisionDocuments(docs: List<RevisionBafendDocument>) = withContext(Dispatchers.IO) {
        val entities = docs.filterNot { it.docId.startsWith("seed_") }.mapNotNull { doc ->
            val existing = offlineDao.getRevisionDocById(doc.docId)
            if (existing?.isPendingOfflineSync == true) {
                null
            } else {
                CachedRevisionDocEntity.fromDomainModel(
                    doc = doc,
                    isPendingOfflineSync = false,
                    isDownloadedLocally = existing?.isDownloadedLocally ?: false,
                    isSolutionDownloadedLocally = existing?.isSolutionDownloadedLocally ?: false
                )
            }
        }
        if (entities.isNotEmpty()) {
            offlineDao.upsertRevisionDocs(entities)
        }
    }

    // =========================================================================
    // 2. SAVE WHAT THE USER DOES OFFLINE TO SQLITE & QUEUE FOR FIREBASE SYNC
    // =========================================================================

    suspend fun saveProfileToSqliteAndQueueIfOffline(
        profile: UserProfile,
        queueForSync: Boolean
    ) = withContext(Dispatchers.IO) {
        offlineDao.upsertUserProfile(
            CachedUserProfileEntity.fromDomainModel(
                profile = profile.copy(updatedAt = Timestamp.now()),
                isPendingOfflineSync = queueForSync
            )
        )
        if (queueForSync) {
            offlineDao.deleteMatchingSyncActions(
                actionType = OfflineSyncActionType.UPSERT_USER_PROFILE,
                targetDocId = profile.userId
            )
            offlineDao.enqueueSyncAction(
                OfflineSyncActionEntity(
                    actionType = OfflineSyncActionType.UPSERT_USER_PROFILE,
                    targetCollection = "users",
                    targetDocumentId = profile.userId,
                    summaryLabel = "Sync Profile: ${profile.displayName.ifBlank { profile.email }}",
                    payloadJson = serializeUserProfileToJson(profile)
                )
            )
            _lastSyncSummary.value = "Saved profile changes to SQLite phone memory (Queued for Firebase sync)."
        }
    }

    suspend fun saveActivityToSqliteAndQueueIfOffline(
        activity: AppActivity,
        queueForSync: Boolean
    ) = withContext(Dispatchers.IO) {
        offlineDao.upsertActivity(
            CachedActivityEntity.fromDomainModel(
                activity = activity,
                isPendingOfflineSync = queueForSync
            )
        )
        if (queueForSync) {
            offlineDao.enqueueSyncAction(
                OfflineSyncActionEntity(
                    actionType = OfflineSyncActionType.LOG_ACTIVITY,
                    targetCollection = "activities",
                    targetDocumentId = activity.activityId,
                    summaryLabel = "Sync Activity: ${activity.title}",
                    payloadJson = serializeActivityToJson(activity)
                )
            )
            _lastSyncSummary.value = "Saved activity '${activity.title}' to SQLite phone memory (Queued for Firebase sync)."
        }
    }

    suspend fun updateActivityReviewInSqliteAndQueueIfOffline(
        activityId: String,
        reviewStatus: String,
        queueForSync: Boolean
    ) = withContext(Dispatchers.IO) {
        offlineDao.updateActivityReviewStatus(
            activityId = activityId,
            reviewStatus = reviewStatus,
            isPending = queueForSync
        )
        if (queueForSync) {
            val json = JSONObject().apply {
                put("activityId", activityId)
                put("reviewStatus", reviewStatus)
            }.toString()
            offlineDao.enqueueSyncAction(
                OfflineSyncActionEntity(
                    actionType = OfflineSyncActionType.UPDATE_ACTIVITY_REVIEW,
                    targetCollection = "activities",
                    targetDocumentId = activityId,
                    summaryLabel = "Sync Activity Review ($reviewStatus)",
                    payloadJson = json
                )
            )
        }
    }

    suspend fun deleteActivityInSqliteAndQueueIfOffline(
        activityId: String,
        queueForSync: Boolean
    ) = withContext(Dispatchers.IO) {
        offlineDao.deleteActivityById(activityId)
        if (queueForSync) {
            offlineDao.enqueueSyncAction(
                OfflineSyncActionEntity(
                    actionType = OfflineSyncActionType.DELETE_ACTIVITY,
                    targetCollection = "activities",
                    targetDocumentId = activityId,
                    summaryLabel = "Delete Activity ($activityId)",
                    payloadJson = JSONObject().put("activityId", activityId).toString()
                )
            )
        }
    }

    suspend fun saveCustomSubjectToSqliteAndQueueIfOffline(
        subject: AdminCustomSubject,
        queueForSync: Boolean
    ) = withContext(Dispatchers.IO) {
        offlineDao.upsertCustomSubject(
            CachedCustomSubjectEntity.fromDomainModel(
                subject = subject,
                isPendingOfflineSync = queueForSync
            )
        )
        val allCached = offlineDao.getAllCustomSubjects().map { it.toDomainModel() }
        CurriculumCatalog.setAdminAddedSubjects(allCached)

        if (queueForSync) {
            offlineDao.enqueueSyncAction(
                OfflineSyncActionEntity(
                    actionType = OfflineSyncActionType.ADD_CUSTOM_SUBJECT,
                    targetCollection = "curriculum_subjects",
                    targetDocumentId = subject.subjectId,
                    summaryLabel = "Sync Subject: ${subject.name} (${subject.code})",
                    payloadJson = serializeCustomSubjectToJson(subject)
                )
            )
            _lastSyncSummary.value = "Subject '${subject.name}' saved in SQLite phone memory (Queued for Firebase sync)."
        }
    }

    suspend fun deleteCustomSubjectInSqliteAndQueueIfOffline(
        subjectId: String,
        queueForSync: Boolean
    ) = withContext(Dispatchers.IO) {
        offlineDao.deleteCustomSubjectById(subjectId)
        val allCached = offlineDao.getAllCustomSubjects().map { it.toDomainModel() }
        CurriculumCatalog.setAdminAddedSubjects(allCached)

        if (queueForSync) {
            offlineDao.enqueueSyncAction(
                OfflineSyncActionEntity(
                    actionType = OfflineSyncActionType.DELETE_CUSTOM_SUBJECT,
                    targetCollection = "curriculum_subjects",
                    targetDocumentId = subjectId,
                    summaryLabel = "Delete Subject ($subjectId)",
                    payloadJson = JSONObject().put("subjectId", subjectId).toString()
                )
            )
        }
    }

    suspend fun saveRevisionDocToSqliteAndQueueIfOffline(
        doc: RevisionBafendDocument,
        queueForSync: Boolean
    ) = withContext(Dispatchers.IO) {
        offlineDao.upsertRevisionDoc(
            CachedRevisionDocEntity.fromDomainModel(
                doc = doc,
                isPendingOfflineSync = queueForSync
            )
        )
        if (queueForSync) {
            offlineDao.enqueueSyncAction(
                OfflineSyncActionEntity(
                    actionType = OfflineSyncActionType.UPLOAD_REVISION_BAFEND,
                    targetCollection = "revision_documents",
                    targetDocumentId = doc.docId,
                    summaryLabel = "Sync .bafend PDF: ${doc.bafendFileName}",
                    payloadJson = serializeRevisionDocToJson(doc)
                )
            )
            _lastSyncSummary.value = "Document '${doc.bafendFileName}' saved in SQLite phone memory (Queued for Firebase sync)."
        }
    }

    suspend fun deleteRevisionDocInSqliteAndQueueIfOffline(
        docId: String,
        queueForSync: Boolean
    ) = withContext(Dispatchers.IO) {
        offlineDao.deleteRevisionDocById(docId)
        if (queueForSync) {
            offlineDao.enqueueSyncAction(
                OfflineSyncActionEntity(
                    actionType = OfflineSyncActionType.DELETE_REVISION_BAFEND,
                    targetCollection = "revision_documents",
                    targetDocumentId = docId,
                    summaryLabel = "Delete .bafend Document ($docId)",
                    payloadJson = JSONObject().put("docId", docId).toString()
                )
            )
        }
    }

    suspend fun deleteUserInSqliteAndQueueIfOffline(
        userId: String,
        queueForSync: Boolean
    ) = withContext(Dispatchers.IO) {
        offlineDao.deleteUserProfileById(userId)
        if (queueForSync) {
            offlineDao.enqueueSyncAction(
                OfflineSyncActionEntity(
                    actionType = OfflineSyncActionType.DELETE_USER_PROFILE,
                    targetCollection = "users",
                    targetDocumentId = userId,
                    summaryLabel = "Delete User ($userId)",
                    payloadJson = JSONObject().put("userId", userId).toString()
                )
            )
        }
    }

    // =========================================================================
    // 3. SYNCHRONIZE OFFLINE SQLITE OUTBOX WITH ONLINE FIREBASE FIRESTORE
    // =========================================================================

    suspend fun synchronizePendingOutboxWithFirebase(): Int = withContext(Dispatchers.IO) {
        if (!isEffectivelyOnline) {
            _lastSyncSummary.value = "Device is offline. Pending changes remain safely stored in SQLite phone memory."
            return@withContext 0
        }
        syncMutex.withLock {
            val pendingActions = offlineDao.getPendingSyncActions()
            if (pendingActions.isEmpty()) {
                _lastSyncedEpochMs.value = System.currentTimeMillis()
                _lastSyncSummary.value = "All SQLite phone memory data is 100% synchronized with Firebase."
                return@withLock 0
            }

            _isSyncingNow.value = true
            var syncedCount = 0
            try {
                for (action in pendingActions) {
                    val success = executeSingleSyncAction(action)
                    if (success) {
                        offlineDao.deleteSyncActionById(action.queueId)
                        syncedCount++
                    }
                }
                val remaining = offlineDao.getPendingSyncActions().size
                _lastSyncedEpochMs.value = System.currentTimeMillis()
                _lastSyncSummary.value = if (remaining == 0) {
                    "Synchronized $syncedCount offline action(s) from SQLite to Firebase!"
                } else {
                    "Synchronized $syncedCount action(s); $remaining item(s) still queued."
                }
            } finally {
                _isSyncingNow.value = false
            }
            syncedCount
        }
    }

    private suspend fun executeSingleSyncAction(action: OfflineSyncActionEntity): Boolean {
        return try {
            when (action.actionType) {
                OfflineSyncActionType.UPSERT_USER_PROFILE -> {
                    val profile = deserializeUserProfileFromJson(action.payloadJson)
                    val docRef = db.collection("users").document(profile.userId)
                    val snap = docRef.get().await()
                    if (snap.exists()) {
                        docRef.set(profile.toUpdatePayload(), SetOptions.merge()).await()
                    } else {
                        docRef.set(profile.toCreatePayload()).await()
                    }
                    val localEntity = offlineDao.getUserProfileById(profile.userId)
                    if (localEntity != null) {
                        offlineDao.upsertUserProfile(localEntity.copy(isPendingOfflineSync = false))
                    }
                    true
                }

                OfflineSyncActionType.DELETE_USER_PROFILE -> {
                    db.collection("users").document(action.targetDocumentId).delete().await()
                    true
                }

                OfflineSyncActionType.LOG_ACTIVITY -> {
                    val activity = deserializeActivityFromJson(action.payloadJson)
                    val uid = auth.currentUser?.uid ?: activity.userId
                    val normalized = activity.copy(userId = uid.ifBlank { activity.userId })
                    db.collection("activities")
                        .document(normalized.activityId)
                        .set(normalized.toCreatePayload())
                        .await()
                    val localEntity = offlineDao.getActivityById(normalized.activityId)
                    if (localEntity != null) {
                        offlineDao.upsertActivity(localEntity.copy(isPendingOfflineSync = false))
                    }
                    true
                }

                OfflineSyncActionType.UPDATE_ACTIVITY_REVIEW -> {
                    val obj = JSONObject(action.payloadJson)
                    val reviewStatus = obj.optString("reviewStatus", "REVIEWED")
                    db.collection("activities")
                        .document(action.targetDocumentId)
                        .update(
                            mapOf(
                                "reviewStatus" to reviewStatus,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        .await()
                    offlineDao.updateActivityReviewStatus(
                        activityId = action.targetDocumentId,
                        reviewStatus = reviewStatus,
                        isPending = false
                    )
                    true
                }

                OfflineSyncActionType.DELETE_ACTIVITY -> {
                    db.collection("activities").document(action.targetDocumentId).delete().await()
                    true
                }

                OfflineSyncActionType.ADD_CUSTOM_SUBJECT -> {
                    val subject = deserializeCustomSubjectFromJson(action.payloadJson)
                    db.collection("curriculum_subjects")
                        .document(subject.subjectId)
                        .set(subject.toCreatePayload())
                        .await()
                    val localEntity = offlineDao.getCustomSubjectById(subject.subjectId)
                    if (localEntity != null) {
                        offlineDao.upsertCustomSubject(localEntity.copy(isPendingOfflineSync = false))
                    }
                    true
                }

                OfflineSyncActionType.DELETE_CUSTOM_SUBJECT -> {
                    db.collection("curriculum_subjects").document(action.targetDocumentId).delete().await()
                    true
                }

                OfflineSyncActionType.UPLOAD_REVISION_BAFEND -> {
                    val doc = deserializeRevisionDocFromJson(action.payloadJson)
                    db.collection("revision_documents")
                        .document(doc.docId)
                        .set(doc.toCreatePayload())
                        .await()
                    val localEntity = offlineDao.getRevisionDocById(doc.docId)
                    if (localEntity != null) {
                        offlineDao.upsertRevisionDoc(localEntity.copy(isPendingOfflineSync = false))
                    }
                    true
                }

                OfflineSyncActionType.DELETE_REVISION_BAFEND -> {
                    db.collection("revision_documents").document(action.targetDocumentId).delete().await()
                    true
                }

                else -> true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Deferred sync for action ${action.actionType} (${action.targetDocumentId})", e)
            offlineDao.recordSyncActionError(
                queueId = action.queueId,
                error = e.localizedMessage ?: "Sync deferred"
            )
            false
        }
    }

    companion object {
        private const val TAG = "OfflineSqliteSync"

        @Volatile
        private var sharedCoordinator: OfflineSqliteSyncCoordinator? = null

        fun getInstance(
            context: Context,
            db: FirebaseFirestore,
            auth: FirebaseAuth = FirebaseAuth.getInstance()
        ): OfflineSqliteSyncCoordinator {
            return sharedCoordinator ?: synchronized(this) {
                sharedCoordinator ?: OfflineSqliteSyncCoordinator(
                    context = context.applicationContext,
                    db = db,
                    auth = auth
                ).also { sharedCoordinator = it }
            }
        }

        fun serializeUserProfileToJson(profile: UserProfile): String {
            return JSONObject().apply {
                put("userId", profile.userId)
                put("displayName", profile.displayName)
                put("email", profile.email)
                put("headline", profile.headline)
                put("bio", profile.bio)
                put("location", profile.location)
                put("roleTitle", profile.roleTitle)
                put("website", profile.website)
                put("statusMessage", profile.statusMessage)
                put("photoUrl", profile.photoUrl)
                put("actorRole", profile.actorRole)
                put("accountStatus", profile.accountStatus)
                put("onboardingCompleted", profile.onboardingCompleted)
                put("educationSystem", profile.educationSystem)
                put("studentLevel", profile.studentLevel)
                put("studentStream", profile.studentStream)
                put("studentSeries", profile.studentSeries)
                put("selectedSubjects", profile.selectedSubjects)
                put("schoolName", profile.schoolName)
                put("schoolOwnership", profile.schoolOwnership)
                put("schoolAge", profile.schoolAge)
                put("schoolEmail", profile.schoolEmail)
                put("schoolFacebookUrl", profile.schoolFacebookUrl)
                put("teachingYears", profile.teachingYears)
                put("teacherSchools", profile.teacherSchools)
                put("pseudonym", profile.pseudonym)
                put("phoneNumbers", profile.phoneNumbers)
                put("childIds", profile.childIds)
            }.toString()
        }

        fun deserializeUserProfileFromJson(json: String): UserProfile {
            val obj = JSONObject(json)
            return UserProfile(
                userId = obj.optString("userId"),
                displayName = obj.optString("displayName"),
                email = obj.optString("email"),
                headline = obj.optString("headline"),
                bio = obj.optString("bio"),
                location = obj.optString("location"),
                roleTitle = obj.optString("roleTitle"),
                website = obj.optString("website"),
                statusMessage = obj.optString("statusMessage"),
                photoUrl = obj.optString("photoUrl"),
                actorRole = obj.optString("actorRole", "VISITOR"),
                accountStatus = obj.optString("accountStatus", "ACTIVE"),
                onboardingCompleted = obj.optBoolean("onboardingCompleted", false),
                educationSystem = obj.optString("educationSystem"),
                studentLevel = obj.optString("studentLevel"),
                studentStream = obj.optString("studentStream"),
                studentSeries = obj.optString("studentSeries"),
                selectedSubjects = obj.optString("selectedSubjects"),
                schoolName = obj.optString("schoolName"),
                schoolOwnership = obj.optString("schoolOwnership"),
                schoolAge = obj.optString("schoolAge"),
                schoolEmail = obj.optString("schoolEmail"),
                schoolFacebookUrl = obj.optString("schoolFacebookUrl"),
                teachingYears = obj.optString("teachingYears"),
                teacherSchools = obj.optString("teacherSchools"),
                pseudonym = obj.optString("pseudonym"),
                phoneNumbers = obj.optString("phoneNumbers"),
                childIds = obj.optString("childIds"),
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        }

        fun serializeActivityToJson(activity: AppActivity): String {
            return JSONObject().apply {
                put("activityId", activity.activityId)
                put("userId", activity.userId)
                put("userDisplayName", activity.userDisplayName)
                put("userEmail", activity.userEmail)
                put("eventType", activity.eventType)
                put("title", activity.title)
                put("details", activity.details)
                put("severity", activity.severity)
                put("reviewStatus", activity.reviewStatus)
                put("ipAddress", activity.ipAddress)
                put("targetPath", activity.targetPath)
                put("dbStatus", activity.dbStatus)
                put("createdAtMs", activity.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            }.toString()
        }

        fun deserializeActivityFromJson(json: String): AppActivity {
            val obj = JSONObject(json)
            val createdMs = obj.optLong("createdAtMs", System.currentTimeMillis())
            return AppActivity(
                activityId = obj.optString("activityId"),
                userId = obj.optString("userId"),
                userDisplayName = obj.optString("userDisplayName"),
                userEmail = obj.optString("userEmail"),
                eventType = obj.optString("eventType"),
                title = obj.optString("title"),
                details = obj.optString("details"),
                severity = obj.optString("severity", "INFO"),
                reviewStatus = obj.optString("reviewStatus", "RECORDED"),
                ipAddress = obj.optString("ipAddress"),
                targetPath = obj.optString("targetPath"),
                dbStatus = obj.optString("dbStatus", "SUCCESS"),
                createdAt = Timestamp(Date(createdMs)),
                updatedAt = Timestamp.now()
            )
        }

        fun serializeCustomSubjectToJson(subject: AdminCustomSubject): String {
            return JSONObject().apply {
                put("subjectId", subject.subjectId)
                put("code", subject.code)
                put("name", subject.name)
                put("coefficient", subject.coefficient)
                put("category", subject.category)
                put("systemCode", subject.systemCode)
                put("levelCode", subject.levelCode)
                put("streamCode", subject.streamCode)
                put("seriesCode", subject.seriesCode)
                put("seriesName", subject.seriesName)
                put("description", subject.description)
                put("createdByEmail", subject.createdByEmail)
            }.toString()
        }

        fun deserializeCustomSubjectFromJson(json: String): AdminCustomSubject {
            val obj = JSONObject(json)
            return AdminCustomSubject(
                subjectId = obj.optString("subjectId"),
                code = obj.optString("code"),
                name = obj.optString("name"),
                coefficient = obj.optInt("coefficient", 4),
                category = obj.optString("category", "Principal"),
                systemCode = obj.optString("systemCode", "ANGLOPHONE"),
                levelCode = obj.optString("levelCode", "ADVANCED_LEVEL"),
                streamCode = obj.optString("streamCode", "SCIENCE"),
                seriesCode = obj.optString("seriesCode", "S1"),
                seriesName = obj.optString("seriesName", "Series S1"),
                description = obj.optString("description"),
                createdByEmail = obj.optString("createdByEmail"),
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        }

        fun serializeRevisionDocToJson(doc: RevisionBafendDocument): String {
            return JSONObject().apply {
                put("docId", doc.docId)
                put("title", doc.title)
                put("description", doc.description)
                put("systemCode", doc.systemCode)
                put("levelCode", doc.levelCode)
                put("streamCode", doc.streamCode)
                put("seriesCode", doc.seriesCode)
                put("subjectCode", doc.subjectCode)
                put("subjectName", doc.subjectName)
                put("subjectCategory", doc.subjectCategory)
                put("coefficient", doc.coefficient)
                put("examYear", doc.examYear)
                put("originalPdfName", doc.originalPdfName)
                put("bafendFileName", doc.bafendFileName)
                put("pageCount", doc.pageCount)
                put("fileSizeBytes", doc.fileSizeBytes)
                put("bafendBase64Payload", doc.bafendBase64Payload)
                put("questionDriveFileId", doc.resolvedQuestionDriveId)
                put("questionDriveWebUrl", doc.questionDriveWebUrl)
                put("hasSolution", doc.hasSolution)
                put("pairId", doc.resolvedPairId)
                put("solutionOriginalPdfName", doc.solutionOriginalPdfName)
                put("solutionBafendFileName", doc.solutionBafendFileName)
                put("solutionPageCount", doc.solutionPageCount)
                put("solutionFileSizeBytes", doc.solutionFileSizeBytes)
                put("solutionBafendBase64Payload", doc.solutionBafendBase64Payload)
                put("solutionDriveFileId", doc.resolvedSolutionDriveId)
                put("solutionDriveWebUrl", doc.solutionDriveWebUrl)
                put("uploadedByEmail", doc.uploadedByEmail)
            }.toString()
        }

        fun deserializeRevisionDocFromJson(json: String): RevisionBafendDocument {
            val obj = JSONObject(json)
            return RevisionBafendDocument(
                docId = obj.optString("docId"),
                title = obj.optString("title"),
                description = obj.optString("description"),
                systemCode = obj.optString("systemCode", "ANGLOPHONE"),
                levelCode = obj.optString("levelCode", "ADVANCED_LEVEL"),
                streamCode = obj.optString("streamCode", "SCIENCE"),
                seriesCode = obj.optString("seriesCode", "ALL"),
                subjectCode = obj.optString("subjectCode", "0770"),
                subjectName = obj.optString("subjectName", "Subject"),
                subjectCategory = obj.optString("subjectCategory", "Principal"),
                coefficient = obj.optInt("coefficient", 4),
                examYear = obj.optString("examYear", "2024").ifBlank { "2024" },
                originalPdfName = obj.optString("originalPdfName"),
                bafendFileName = obj.optString("bafendFileName"),
                pageCount = obj.optInt("pageCount", 3),
                fileSizeBytes = obj.optLong("fileSizeBytes", 2048L),
                bafendBase64Payload = obj.optString("bafendBase64Payload"),
                questionDriveFileId = obj.optString("questionDriveFileId"),
                questionDriveWebUrl = obj.optString("questionDriveWebUrl"),
                hasSolution = obj.optBoolean("hasSolution", false),
                pairId = obj.optString("pairId"),
                solutionOriginalPdfName = obj.optString("solutionOriginalPdfName"),
                solutionBafendFileName = obj.optString("solutionBafendFileName"),
                solutionPageCount = obj.optInt("solutionPageCount", 0),
                solutionFileSizeBytes = obj.optLong("solutionFileSizeBytes", 0L),
                solutionBafendBase64Payload = obj.optString("solutionBafendBase64Payload"),
                solutionDriveFileId = obj.optString("solutionDriveFileId"),
                solutionDriveWebUrl = obj.optString("solutionDriveWebUrl"),
                uploadedByEmail = obj.optString("uploadedByEmail"),
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        }
    }
}
