package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.auth.PersistentSessionManager
import com.example.data.local.OfflineSqliteSyncCoordinator
import com.example.data.local.SqliteOfflineTelemetry
import com.example.data.model.AccountStatus
import com.example.data.model.ActivityEventType
import com.example.data.model.ActivityReviewStatus
import com.example.data.model.ActivitySeverity
import com.example.data.model.AdminCustomSubject
import com.example.data.model.AppActivity
import com.example.data.model.BafendFileEngine
import com.example.data.model.DatabaseSecurityInspector
import com.example.data.model.EducationSystem
import com.example.data.model.PlatformRole
import com.example.data.model.RevisionBafendDocument
import com.example.data.model.SchoolOwnership
import com.example.data.model.StudentLevel
import com.example.data.model.StudentStream
import com.example.data.model.UserProfile
import com.example.data.remote.GeminiThinkingService
import com.example.data.remote.StudentOrientationAnalysis
import com.example.data.repository.ActivityRepository
import com.example.data.repository.CurriculumRevisionRepository
import com.example.data.repository.UserProfileRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profile: UserProfile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

sealed interface StudentOrientationUiState {
    data object Idle : StudentOrientationUiState
    data object Analyzing : StudentOrientationUiState
    data class ResultReady(val analysis: StudentOrientationAnalysis) : StudentOrientationUiState
    data class Error(val message: String) : StudentOrientationUiState
}

sealed interface ActivitiesUiState {
    data object Loading : ActivitiesUiState
    data class Success(val activities: List<AppActivity>, val isGlobalAdminStream: Boolean) : ActivitiesUiState
    data class Error(val message: String) : ActivitiesUiState
}

enum class AuthenticatedDestination {
    PROFILE,
    REVISION_HUB,
    LOCAL_DOWNLOADS,
    USER_MANAGEMENT,
    ADMIN_PORTAL
}

enum class AdminSectionTab(val code: String, val label: String) {
    MANAGE_USERS("MANAGE_USERS", "Manage Users"),
    CURRICULUM_AND_PDFS("CURRICULUM_AND_PDFS", "Subjects & .bafend PDFs"),
    DB_SECURITY_LOGS("DB_SECURITY_LOGS", "DB & IP Security Logs"),
    ACTIVITY_STREAM("ACTIVITY_STREAM", "All Activity Logs")
}

data class ProfileEditorDraft(
    val displayName: String = "",
    val headline: String = "",
    val roleTitle: String = "",
    val bio: String = "",
    val location: String = "",
    val website: String = "",
    val statusMessage: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val statusBannerMessage: String? = null
)

data class AdminFilterState(
    val activeTab: AdminSectionTab = AdminSectionTab.MANAGE_USERS,
    val selectedManagedUserId: String? = null,
    val searchQuery: String = "",
    val userSearchQuery: String = "",
    val selectedUserRoleFilter: String = "ALL",
    val selectedUserStatusFilter: String = "ALL",
    val selectedEventType: String = "ALL",
    val selectedReviewStatus: String = "ALL",
    val selectedDbLogFilter: String = "ALL_DB",
    val statusFeedback: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    application: Application,
    private val currentUserId: String,
    private val defaultDisplayName: String,
    private val defaultEmail: String,
    private val defaultPhotoUrl: String,
    private val isEmailVerified: Boolean = true
) : AndroidViewModel(application) {

    // CRITICAL: Always initialize Firestore with R.string.firestore_database_id
    private val databaseId: String = application.getString(R.string.firestore_database_id)
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(databaseId)
    val offlineSyncCoordinator: OfflineSqliteSyncCoordinator =
        OfflineSqliteSyncCoordinator.getInstance(application, db, Firebase.auth)
    private val repository: UserProfileRepository = UserProfileRepository(db, Firebase.auth)
    private val activityRepository: ActivityRepository = ActivityRepository(db, Firebase.auth)
    private val curriculumRevisionRepository: CurriculumRevisionRepository =
        CurriculumRevisionRepository(db, Firebase.auth, offlineSyncCoordinator)
    private val geminiService: GeminiThinkingService = GeminiThinkingService()
    private val sessionManager: PersistentSessionManager = PersistentSessionManager(application)

    val sqliteTelemetryState: StateFlow<SqliteOfflineTelemetry> = offlineSyncCoordinator
        .telemetryFlow
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SqliteOfflineTelemetry()
        )

    private val _activeReaderDocument = MutableStateFlow<RevisionBafendDocument?>(null)
    val activeReaderDocument: StateFlow<RevisionBafendDocument?> = _activeReaderDocument.asStateFlow()

    val adminCustomSubjectsState: StateFlow<List<AdminCustomSubject>> = curriculumRevisionRepository
        .observeAdminCustomSubjects()
        .catch { emit(emptyList()) }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val revisionDocumentsState: StateFlow<List<RevisionBafendDocument>> = curriculumRevisionRepository
        .observeRevisionDocuments()
        .catch { emit(CurriculumRevisionRepository.DEFAULT_SEEDED_BAFEND_DOCUMENTS) }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CurriculumRevisionRepository.DEFAULT_SEEDED_BAFEND_DOCUMENTS
        )

    private val _currentDestination = MutableStateFlow(AuthenticatedDestination.PROFILE)
    val currentDestination: StateFlow<AuthenticatedDestination> = _currentDestination.asStateFlow()

    private val _isAdmin = MutableStateFlow<Boolean?>(null)
    val isAdmin: StateFlow<Boolean?> = _isAdmin.asStateFlow()

    private val _keepSignedIn = MutableStateFlow(sessionManager.isKeepSignedInEnabled())
    val keepSignedIn: StateFlow<Boolean> = _keepSignedIn.asStateFlow()

    private val _isRoleOnboardingActive = MutableStateFlow(false)
    val isRoleOnboardingActive: StateFlow<Boolean> = _isRoleOnboardingActive.asStateFlow()

    private val _isCompletingOnboarding = MutableStateFlow(false)
    val isCompletingOnboarding: StateFlow<Boolean> = _isCompletingOnboarding.asStateFlow()

    private val _editorDraft = MutableStateFlow(ProfileEditorDraft())
    val editorDraft: StateFlow<ProfileEditorDraft> = _editorDraft.asStateFlow()

    private val _adminFilterState = MutableStateFlow(AdminFilterState())
    val adminFilterState: StateFlow<AdminFilterState> = _adminFilterState.asStateFlow()

    private val _studentOrientationState = MutableStateFlow<StudentOrientationUiState>(StudentOrientationUiState.Idle)
    val studentOrientationState: StateFlow<StudentOrientationUiState> = _studentOrientationState.asStateFlow()

    private val _studentTargetObjective = MutableStateFlow(
        "Achieve Distinction at Advanced Level & gain admission into a top Engineering, Medical, or Leadership Institution"
    )
    val studentTargetObjective: StateFlow<String> = _studentTargetObjective.asStateFlow()

    // Two-tier Kotlin Flow + SQLite phone memory cache for UserProfile
    val profileState: StateFlow<ProfileUiState> = combine(
        repository.observeProfile(currentUserId)
            .onEach { remoteProfile ->
                if (remoteProfile != null) {
                    sessionManager.saveCachedProfile(remoteProfile)
                    offlineSyncCoordinator.cacheOnlineUserProfile(remoteProfile)
                }
            }
            .catch { error ->
                Log.w(TAG, "Firestore profile stream offline, falling back to SQLite phone memory", error)
                emit(null)
            },
        offlineSyncCoordinator.offlineDao.observeUserProfile(currentUserId)
    ) { remoteProfile, sqliteEntity ->
        val sqliteProfile = sqliteEntity?.toDomainModel()
        val resolved = when {
            sqliteEntity?.isPendingOfflineSync == true && sqliteProfile != null -> sqliteProfile
            !offlineSyncCoordinator.isEffectivelyOnline && sqliteProfile != null -> sqliteProfile
            remoteProfile != null -> remoteProfile
            sqliteProfile != null -> sqliteProfile
            else -> sessionManager.getCachedProfile(currentUserId)
        }
        if (resolved != null) {
            sessionManager.saveCachedProfile(resolved)
            ProfileUiState.Success(resolved)
        } else {
            ProfileUiState.Loading
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = sessionManager.getCachedProfile(currentUserId)?.let {
                ProfileUiState.Success(it)
            } ?: ProfileUiState.Loading
        )

    // Two-tier Kotlin Flow + SQLite phone memory cache for Activities (Admin Global vs Scoped User)
    val activitiesState: StateFlow<ActivitiesUiState> = _isAdmin
        .flatMapLatest { adminStatus ->
            when (adminStatus) {
                null -> flowOf(ActivitiesUiState.Loading)
                true -> combine(
                    activityRepository.observeAllActivitiesForAdmin()
                        .onEach { onlineList ->
                            offlineSyncCoordinator.cacheOnlineActivities(onlineList)
                        }
                        .catch { error ->
                            Log.w(TAG, "Admin activity stream offline, falling back to SQLite", error)
                            emit(emptyList())
                        },
                    offlineSyncCoordinator.offlineDao.observeAllActivities()
                ) { onlineList, sqliteEntities ->
                    val sqliteList = sqliteEntities.map { it.toDomainModel() }
                    val merged = (sqliteList.filter { entity ->
                        sqliteEntities.any { it.activityId == entity.activityId && it.isPendingOfflineSync }
                    } + onlineList + sqliteList)
                        .distinctBy { it.activityId }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ActivitiesUiState.Success(activities = merged, isGlobalAdminStream = true)
                }
                false -> combine(
                    activityRepository.observeUserActivities(currentUserId)
                        .onEach { onlineList ->
                            offlineSyncCoordinator.cacheOnlineActivities(onlineList)
                        }
                        .catch { error ->
                            Log.w(TAG, "User activity stream offline, falling back to SQLite", error)
                            emit(emptyList())
                        },
                    offlineSyncCoordinator.offlineDao.observeActivitiesForUser(currentUserId)
                ) { onlineList, sqliteEntities ->
                    val sqliteList = sqliteEntities.map { it.toDomainModel() }
                    val merged = (sqliteList + onlineList)
                        .distinctBy { it.activityId }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    ActivitiesUiState.Success(activities = merged, isGlobalAdminStream = false)
                }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ActivitiesUiState.Loading
        )

    // All registered user profiles for the Admin Portal directory (mirrored to SQLite phone memory)
    val allUsersState: StateFlow<List<UserProfile>> = _isAdmin
        .flatMapLatest { adminStatus ->
            if (adminStatus == true) {
                combine(
                    activityRepository.observeAllUsersForAdmin()
                        .onEach { onlineUsers ->
                            offlineSyncCoordinator.cacheOnlineUserProfiles(onlineUsers)
                        }
                        .catch { error ->
                            Log.w(TAG, "All users stream offline, serving from SQLite phone memory", error)
                            emit(emptyList())
                        },
                    offlineSyncCoordinator.offlineDao.observeAllUserProfiles()
                ) { onlineUsers, sqliteUsers ->
                    val localModels = sqliteUsers.map { it.toDomainModel() }
                    val pendingLocals = sqliteUsers.filter { it.isPendingOfflineSync }.map { it.toDomainModel() }
                    (pendingLocals + onlineUsers + localModels).distinctBy { it.userId }
                }
            } else {
                combine(
                    repository.observeProfile(currentUserId).catch { emit(null) },
                    offlineSyncCoordinator.offlineDao.observeUserProfile(currentUserId)
                ) { remote, local ->
                    listOfNotNull(local?.toDomainModel() ?: remote)
                }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    init {
        initializeSessionAndProfile()
    }

    private fun initializeSessionAndProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.onUserAuthenticated(
                userId = currentUserId,
                displayName = defaultDisplayName,
                email = defaultEmail,
                photoUrl = defaultPhotoUrl
            )
            val adminVerified = activityRepository.checkAndEnsureAdminStatus(
                userId = currentUserId,
                email = defaultEmail,
                isEmailVerified = isEmailVerified
            )
            _isAdmin.value = adminVerified

            val existingResult = repository.getProfileById(currentUserId)
            val wasNewProfile = existingResult.getOrNull() == null

            val initResult = repository.ensureOrInitializeProfile(
                defaultDisplayName = defaultDisplayName,
                defaultEmail = defaultEmail,
                defaultPhotoUrl = defaultPhotoUrl
            )
            val resolvedProfile = initResult.getOrNull()
                ?: offlineSyncCoordinator.offlineDao.getUserProfileById(currentUserId)?.toDomainModel()
                ?: sessionManager.getCachedProfile(currentUserId)
            if (resolvedProfile != null) {
                sessionManager.saveCachedProfile(resolvedProfile)
                offlineSyncCoordinator.cacheOnlineUserProfile(resolvedProfile)
            }
            if (offlineSyncCoordinator.isEffectivelyOnline) {
                offlineSyncCoordinator.synchronizePendingOutboxWithFirebase()
            }
            val resolvedName = resolvedProfile?.displayName ?: defaultDisplayName.ifBlank { "Aura Member" }
            val resolvedEmail = resolvedProfile?.email ?: defaultEmail

            if (!sessionManager.hasLoggedSessionEventForUid(currentUserId)) {
                sessionManager.markSessionEventLogged(currentUserId)
                val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
                activityRepository.logActivity(
                    userDisplayName = resolvedName,
                    userEmail = resolvedEmail,
                    eventType = ActivityEventType.DB_CONNECTION_ATTEMPT,
                    title = "Firestore TLS Session Handshake (IP: $clientIp)",
                    details = "Direct client connection established to database '$databaseId' targeting /users/$currentUserId • IP: $clientIp • Auth UID verified.",
                    severity = ActivitySeverity.INFO,
                    ipAddress = clientIp,
                    targetPath = "/users/$currentUserId",
                    dbStatus = "ATTEMPT"
                )
                if (wasNewProfile) {
                    activityRepository.logActivity(
                        userDisplayName = resolvedName,
                        userEmail = resolvedEmail,
                        eventType = ActivityEventType.PROFILE_CREATED,
                        title = "Provisioned Verified Cloud Profile",
                        details = "Initial profile created and synced to Firestore for $resolvedName (Default Status: ACTIVE).",
                        severity = ActivitySeverity.SUCCESS,
                        ipAddress = clientIp,
                        targetPath = "/users/$currentUserId",
                        dbStatus = "SUCCESS"
                    )
                } else {
                    activityRepository.logActivity(
                        userDisplayName = resolvedName,
                        userEmail = resolvedEmail,
                        eventType = ActivityEventType.AUTH_SIGN_IN,
                        title = "Authenticated Session Started (Entry)",
                        details = "Signed in via Google Identity Credential Manager (${if (adminVerified) "Administrator" else "Verified Member"}) • IP: $clientIp.",
                        severity = ActivitySeverity.INFO,
                        ipAddress = clientIp,
                        targetPath = "/users/$currentUserId",
                        dbStatus = "SUCCESS"
                    )
                }
            }
        }
    }

    fun logSignOutAndClearSession(currentProfile: UserProfile?, onReadyToSignOut: () -> Unit) {
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val name = currentProfile?.displayName ?: defaultDisplayName.ifBlank { "Aura Member" }
            val email = currentProfile?.email ?: defaultEmail
            try {
                activityRepository.logActivity(
                    userDisplayName = name,
                    userEmail = email,
                    eventType = ActivityEventType.AUTH_SIGN_OUT,
                    title = "User Signed Out (Session Exit)",
                    details = "Authenticated session closed for $name ($email) • Client IP: $clientIp.",
                    severity = ActivitySeverity.INFO,
                    ipAddress = clientIp,
                    targetPath = "/users/$currentUserId",
                    dbStatus = "SUCCESS"
                )
            } catch (_: Exception) {
                // Ignore if already signed out
            }
            onReadyToSignOut()
        }
    }

    fun togglePersistentSession(enabled: Boolean) {
        sessionManager.setKeepSignedInEnabled(enabled)
        _keepSignedIn.value = enabled
        _editorDraft.update {
            it.copy(
                statusBannerMessage = if (enabled) {
                    "Persistent session enabled (Login Once)"
                } else {
                    "Persistent session disabled"
                }
            )
        }
    }

    fun navigateTo(destination: AuthenticatedDestination) {
        val currentProfile = (profileState.value as? ProfileUiState.Success)?.profile
        val hasAdminAccess = _isAdmin.value == true || currentProfile?.isDesignatedAdminEmail == true ||
            currentProfile?.effectivePlatformRole == PlatformRole.ADMIN
        if ((destination == AuthenticatedDestination.ADMIN_PORTAL || destination == AuthenticatedDestination.USER_MANAGEMENT) && !hasAdminAccess) {
            _editorDraft.update {
                it.copy(statusBannerMessage = "Admin Portal & User Management access is restricted to Administrators")
            }
            return
        }
        if (destination == AuthenticatedDestination.USER_MANAGEMENT) {
            _adminFilterState.update { it.copy(activeTab = AdminSectionTab.MANAGE_USERS) }
        }
        _currentDestination.value = destination
    }

    fun selectAdminSectionTab(tab: AdminSectionTab) {
        _adminFilterState.update { it.copy(activeTab = tab, selectedManagedUserId = null) }
        if (tab == AdminSectionTab.MANAGE_USERS) {
            _currentDestination.value = AuthenticatedDestination.USER_MANAGEMENT
        } else if (_currentDestination.value == AuthenticatedDestination.USER_MANAGEMENT) {
            _currentDestination.value = AuthenticatedDestination.ADMIN_PORTAL
        }
    }

    fun openManagedUserDetails(userId: String) {
        _adminFilterState.update {
            it.copy(
                activeTab = AdminSectionTab.MANAGE_USERS,
                selectedManagedUserId = userId
            )
        }
    }

    fun closeManagedUserDetails() {
        _adminFilterState.update {
            it.copy(selectedManagedUserId = null)
        }
    }

    fun openRoleOnboarding() {
        val currentProfile = (profileState.value as? ProfileUiState.Success)?.profile
        if (_isAdmin.value == true || currentProfile?.isDesignatedAdminEmail == true || currentProfile?.effectivePlatformRole == PlatformRole.ADMIN) {
            _isRoleOnboardingActive.value = false
            return
        }
        if (currentProfile?.isPendingAccount == true) {
            _editorDraft.update {
                it.copy(statusBannerMessage = "Account is PENDING approval. Navigation and role changes are locked.")
            }
            return
        }
        _isRoleOnboardingActive.value = true
    }

    fun closeRoleOnboarding() {
        _isRoleOnboardingActive.value = false
    }

    fun completeRoleOnboarding(
        currentProfile: UserProfile,
        submission: RoleOnboardingSubmission
    ) {
        if (currentProfile.isPendingAccount && !currentProfile.isDesignatedAdminEmail) {
            _editorDraft.update {
                it.copy(statusBannerMessage = "Account is PENDING approval. Activities are disabled until set to ACTIVE.")
            }
            return
        }
        _isCompletingOnboarding.value = true
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val combinedPayload = listOf(
                submission.schoolName,
                submission.schoolEmail,
                submission.schoolFacebookUrl,
                submission.pseudonym,
                submission.studentSeries,
                submission.selectedSubjects.joinToString(" "),
                submission.phoneNumbers.joinToString(" "),
                submission.childIds.joinToString(" ")
            ).joinToString(" ")

            if (DatabaseSecurityInspector.containsInjectionSignature(combinedPayload)) {
                val sig = DatabaseSecurityInspector.matchedSignatureDescription(combinedPayload)
                _isCompletingOnboarding.value = false
                _editorDraft.update {
                    it.copy(statusBannerMessage = "Security Alert: Blocked database injection pattern ($sig)")
                }
                activityRepository.logActivity(
                    userDisplayName = currentProfile.displayName,
                    userEmail = currentProfile.email,
                    eventType = ActivityEventType.DB_INJECTION_BLOCKED,
                    title = "Blocked Database Injection in Role Onboarding",
                    details = "Signature: $sig • Payload blocked before writing to /users/$currentUserId • IP: $clientIp",
                    severity = ActivitySeverity.SECURITY,
                    ipAddress = clientIp,
                    targetPath = "/users/$currentUserId",
                    dbStatus = "INJECTION_BLOCKED"
                )
                return@launch
            }

            val isDefaultAdmin = currentProfile.isDesignatedAdminEmail || _isAdmin.value == true
            val resolvedRoleTitle = when (submission.selectedRole) {
                PlatformRole.VISITOR -> if (isDefaultAdmin) "Administrator • Visitor" else "Visitor"
                PlatformRole.STUDENT -> {
                    val sys = EducationSystem.fromCode(submission.educationSystem)?.label ?: "Student"
                    val stream = StudentStream.fromCode(submission.studentStream)?.label ?: ""
                    val seriesPart = submission.studentSeries.takeIf { it.isNotBlank() && it != "ALL" }.orEmpty()
                    listOf(sys, stream, seriesPart).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Student" }
                }
                PlatformRole.TEACHER -> {
                    val namePart = submission.pseudonym.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                    "Teacher$namePart • ${submission.teachingYears.ifBlank { "1" }} yrs"
                }
                PlatformRole.SCHOOL -> {
                    val own = SchoolOwnership.fromCode(submission.schoolOwnership)?.label ?: "School"
                    "$own • ${submission.schoolName.ifBlank { "Institution" }}"
                }
                PlatformRole.PARENT -> {
                    "Parent • ${submission.childIds.size} Linked Child ID(s)"
                }
                PlatformRole.ADMIN -> "Administrator"
            }

            val resolvedHeadline = when (submission.selectedRole) {
                PlatformRole.VISITOR -> if (isDefaultAdmin) {
                    "Platform Administrator (fokoufdr@gmail.com) • Full Portal Access"
                } else {
                    "Verified Visitor • Aura E-Learning & Identity Platform"
                }
                PlatformRole.STUDENT -> {
                    val level = StudentLevel.fromCode(submission.studentLevel)?.label ?: "Scholar"
                    val stream = StudentStream.fromCode(submission.studentStream)?.label ?: ""
                    val school = submission.schoolName.takeIf { it.isNotBlank() } ?: "Aura E-Learning Platform"
                    "$level $stream Student at $school".trim()
                }
                PlatformRole.TEACHER -> {
                    val sys = EducationSystem.fromCode(submission.educationSystem)?.label ?: "Educator"
                    "$sys Educator (${submission.teachingYears.ifBlank { "1" }} Years Experience)"
                }
                PlatformRole.SCHOOL -> {
                    val own = SchoolOwnership.fromCode(submission.schoolOwnership)?.label ?: "Educational Institution"
                    "${submission.schoolName.ifBlank { "Partner School" }} ($own)"
                }
                PlatformRole.PARENT -> {
                    "Parent Account • Monitoring ${submission.childIds.size} Student(s)"
                }
                PlatformRole.ADMIN -> "Platform Administrator"
            }

            val updated = currentProfile.copy(
                userId = currentUserId,
                actorRole = submission.selectedRole.code,
                onboardingCompleted = true,
                roleTitle = resolvedRoleTitle.take(120),
                headline = resolvedHeadline.take(160),
                educationSystem = submission.educationSystem.trim().take(60),
                studentLevel = submission.studentLevel.trim().take(60),
                studentStream = submission.studentStream.trim().take(60),
                studentSeries = submission.studentSeries.trim().take(80),
                selectedSubjects = UserProfile.joinListField(
                    submission.selectedSubjects,
                    maxItems = 35
                ).take(1500),
                schoolName = submission.schoolName.trim().take(200),
                schoolOwnership = submission.schoolOwnership.trim().take(40),
                schoolAge = submission.schoolAge.trim().take(40),
                schoolEmail = submission.schoolEmail.trim().take(200),
                schoolFacebookUrl = submission.schoolFacebookUrl.trim().take(300),
                teachingYears = submission.teachingYears.trim().take(40),
                teacherSchools = UserProfile.joinListField(submission.teacherSchools, maxItems = 25),
                pseudonym = submission.pseudonym.trim().take(100),
                phoneNumbers = UserProfile.joinListField(submission.phoneNumbers, maxItems = 25),
                childIds = UserProfile.joinListField(submission.childIds, maxItems = UserProfile.MAX_CHILD_IDS)
            )

            sessionManager.saveCachedProfile(updated)
            val isOnlineNow = offlineSyncCoordinator.isEffectivelyOnline
            val saveResult = if (isOnlineNow) {
                repository.saveProfile(updated)
            } else {
                Result.failure(IllegalStateException("Offline SQLite mode active"))
            }
            saveResult.fold(
                onSuccess = {
                    offlineSyncCoordinator.saveProfileToSqliteAndQueueIfOffline(updated, queueForSync = false)
                    _isCompletingOnboarding.value = false
                    _isRoleOnboardingActive.value = false
                    _editorDraft.update {
                        it.copy(
                            statusBannerMessage = "Welcome! Role configured as ${submission.selectedRole.label} (Saved in SQLite & Firebase)."
                        )
                    }
                    recordActivityWithSqliteFallback(
                        userDisplayName = updated.displayName,
                        userEmail = updated.email,
                        eventType = ActivityEventType.ROLE_ONBOARDING_COMPLETED,
                        title = "Completed ${submission.selectedRole.label} Role Onboarding",
                        details = "Actor Role: ${submission.selectedRole.code} • ${updated.headline}",
                        severity = ActivitySeverity.SUCCESS,
                        ipAddress = clientIp,
                        targetPath = "/users/$currentUserId",
                        dbStatus = "SUCCESS"
                    )
                },
                onFailure = {
                    offlineSyncCoordinator.saveProfileToSqliteAndQueueIfOffline(updated, queueForSync = true)
                    _isCompletingOnboarding.value = false
                    _isRoleOnboardingActive.value = false
                    _editorDraft.update {
                        it.copy(
                            statusBannerMessage = "Role saved to SQLite phone memory (Queued to sync with Firebase when online)."
                        )
                    }
                    recordActivityWithSqliteFallback(
                        userDisplayName = updated.displayName,
                        userEmail = updated.email,
                        eventType = ActivityEventType.ROLE_ONBOARDING_COMPLETED,
                        title = "Completed ${submission.selectedRole.label} Role Onboarding (Offline SQLite)",
                        details = "Actor Role: ${submission.selectedRole.code} • Saved in phone SQLite & queued for Firebase sync • IP: $clientIp",
                        severity = ActivitySeverity.SUCCESS,
                        ipAddress = clientIp,
                        targetPath = "/users/$currentUserId",
                        dbStatus = "SUCCESS"
                    )
                }
            )
        }
    }

    fun updateAdminSearchQuery(query: String) {
        _adminFilterState.update { it.copy(searchQuery = query) }
    }

    fun updateAdminUserSearchQuery(query: String) {
        _adminFilterState.update { it.copy(userSearchQuery = query) }
    }

    fun updateAdminUserRoleFilter(roleCode: String) {
        _adminFilterState.update { it.copy(selectedUserRoleFilter = roleCode) }
    }

    fun updateAdminUserStatusFilter(statusCode: String) {
        _adminFilterState.update { it.copy(selectedUserStatusFilter = statusCode) }
    }

    fun updateAdminEventTypeFilter(eventType: String) {
        _adminFilterState.update { it.copy(selectedEventType = eventType) }
    }

    fun updateAdminReviewStatusFilter(reviewStatus: String) {
        _adminFilterState.update { it.copy(selectedReviewStatus = reviewStatus) }
    }

    fun updateAdminDbLogFilter(dbFilter: String) {
        _adminFilterState.update { it.copy(selectedDbLogFilter = dbFilter) }
    }

    fun dismissAdminFeedback() {
        _adminFilterState.update { it.copy(statusFeedback = null) }
    }

    fun adminChangeUserRole(
        targetUser: UserProfile,
        newRole: PlatformRole,
        adminProfile: UserProfile?
    ) {
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val adminName = adminProfile?.displayName ?: defaultDisplayName.ifBlank { "Administrator" }
            val adminEmail = adminProfile?.email ?: defaultEmail
            val result = activityRepository.updateUserRoleAsAdmin(targetUser, newRole)
            result.fold(
                onSuccess = {
                    if (targetUser.userId == currentUserId) {
                        sessionManager.saveCachedProfile(
                            targetUser.copy(
                                actorRole = newRole.code,
                                onboardingCompleted = true
                            )
                        )
                    }
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Updated ${targetUser.displayName}'s role to ${newRole.label}")
                    }
                    activityRepository.logActivity(
                        userDisplayName = adminName,
                        userEmail = adminEmail,
                        eventType = ActivityEventType.USER_ROLE_CHANGED,
                        title = "Role Changed: ${targetUser.displayName} → ${newRole.label}",
                        details = "Admin ($adminEmail) updated ${targetUser.email.ifBlank { targetUser.userId }} from ${targetUser.effectivePlatformRole.label} to ${newRole.label} (${newRole.code}) • Target: /users/${targetUser.userId} • IP: $clientIp",
                        severity = if (newRole == PlatformRole.ADMIN) ActivitySeverity.SECURITY else ActivitySeverity.SUCCESS,
                        ipAddress = clientIp,
                        targetPath = "/users/${targetUser.userId}",
                        dbStatus = "SUCCESS"
                    )
                },
                onFailure = { err ->
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Role update failed: ${err.localizedMessage ?: "Permission denied"}")
                    }
                    activityRepository.logActivity(
                        userDisplayName = adminName,
                        userEmail = adminEmail,
                        eventType = ActivityEventType.DB_OPERATION_FAILURE,
                        title = "Failed Role Update on /users/${targetUser.userId}",
                        details = "Attempted to change role to ${newRole.code} • Error: ${err.localizedMessage ?: "Permission denied"} • IP: $clientIp",
                        severity = ActivitySeverity.SECURITY,
                        ipAddress = clientIp,
                        targetPath = "/users/${targetUser.userId}",
                        dbStatus = "FAILURE"
                    )
                }
            )
        }
    }

    fun adminChangeUserStatus(
        targetUser: UserProfile,
        newStatus: AccountStatus,
        adminProfile: UserProfile?,
        reasonNote: String = ""
    ) {
        if (targetUser.isDesignatedAdminEmail && newStatus != AccountStatus.ACTIVE) {
            _adminFilterState.update {
                it.copy(statusFeedback = "Primary Administrator account (fokoufdr@gmail.com) always remains ACTIVE.")
            }
            return
        }
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val adminName = adminProfile?.displayName ?: defaultDisplayName.ifBlank { "Administrator" }
            val adminEmail = adminProfile?.email ?: defaultEmail
            val result = activityRepository.updateUserAccountStatusAsAdmin(targetUser, newStatus, reasonNote)
            result.fold(
                onSuccess = {
                    val statusDesc = when (newStatus) {
                        AccountStatus.ACTIVE -> "ACTIVE (All profile activities enabled)"
                        AccountStatus.PENDING -> "PENDING (Profile navigation locked)"
                        AccountStatus.SUSPENDED -> "SUSPENDED (Account temporarily suspended)"
                        AccountStatus.BLOCKED -> "BLOCKED (Account permanently blocked)"
                    }
                    _adminFilterState.update {
                        it.copy(statusFeedback = "${targetUser.displayName} set to $statusDesc")
                    }
                    activityRepository.logActivity(
                        userDisplayName = adminName,
                        userEmail = adminEmail,
                        eventType = ActivityEventType.USER_STATUS_CHANGED,
                        title = "Account Status: ${targetUser.displayName} → ${newStatus.label}",
                        details = "Admin set accountStatus=${newStatus.code} for ${targetUser.email.ifBlank { targetUser.userId }}. $statusDesc ${reasonNote.takeIf { it.isNotBlank() }?.let { "• Reason: $it" }.orEmpty()} • Target: /users/${targetUser.userId} • IP: $clientIp",
                        severity = if (newStatus != AccountStatus.ACTIVE) ActivitySeverity.SECURITY else ActivitySeverity.SUCCESS,
                        ipAddress = clientIp,
                        targetPath = "/users/${targetUser.userId}",
                        dbStatus = "SUCCESS"
                    )
                },
                onFailure = { err ->
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Account status update failed: ${err.localizedMessage ?: "Permission denied"}")
                    }
                    activityRepository.logActivity(
                        userDisplayName = adminName,
                        userEmail = adminEmail,
                        eventType = ActivityEventType.DB_OPERATION_FAILURE,
                        title = "Failed Status Update on /users/${targetUser.userId}",
                        details = "Attempted status=${newStatus.code} • Error: ${err.localizedMessage ?: "Permission denied"} • IP: $clientIp",
                        severity = ActivitySeverity.SECURITY,
                        ipAddress = clientIp,
                        targetPath = "/users/${targetUser.userId}",
                        dbStatus = "FAILURE"
                    )
                }
            )
        }
    }

    fun adminUpdateUserPersonalInfo(
        targetUser: UserProfile,
        displayName: String,
        headline: String,
        roleTitle: String,
        location: String,
        bio: String,
        statusMessage: String,
        phoneNumbers: String,
        schoolName: String,
        adminProfile: UserProfile?
    ) {
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val adminName = adminProfile?.displayName ?: defaultDisplayName.ifBlank { "Administrator" }
            val adminEmail = adminProfile?.email ?: defaultEmail
            val combinedPayload = "$displayName $headline $roleTitle $location $bio $statusMessage $phoneNumbers $schoolName"
            if (DatabaseSecurityInspector.containsInjectionSignature(combinedPayload)) {
                val sig = DatabaseSecurityInspector.matchedSignatureDescription(combinedPayload)
                _adminFilterState.update {
                    it.copy(statusFeedback = "Blocked injection attempt in user update: $sig")
                }
                activityRepository.logActivity(
                    userDisplayName = adminName,
                    userEmail = adminEmail,
                    eventType = ActivityEventType.DB_INJECTION_BLOCKED,
                    title = "Blocked Injection Attempt on /users/${targetUser.userId}",
                    details = "Signature: $sig • Blocked direct database write • IP: $clientIp",
                    severity = ActivitySeverity.SECURITY,
                    ipAddress = clientIp,
                    targetPath = "/users/${targetUser.userId}",
                    dbStatus = "INJECTION_BLOCKED"
                )
                return@launch
            }

            val result = activityRepository.updateUserPersonalInfoAsAdmin(
                targetUser = targetUser,
                displayName = displayName,
                headline = headline,
                roleTitle = roleTitle,
                location = location,
                bio = bio,
                statusMessage = statusMessage,
                phoneNumbers = phoneNumbers,
                schoolName = schoolName
            )
            result.fold(
                onSuccess = {
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Updated personal info for ${displayName.ifBlank { targetUser.displayName }}")
                    }
                    activityRepository.logActivity(
                        userDisplayName = adminName,
                        userEmail = adminEmail,
                        eventType = ActivityEventType.PROFILE_UPDATED,
                        title = "Admin Updated Personal Info: ${displayName.ifBlank { targetUser.displayName }}",
                        details = "Updated profile fields for ${targetUser.email.ifBlank { targetUser.userId }} • Target: /users/${targetUser.userId} • IP: $clientIp",
                        severity = ActivitySeverity.SUCCESS,
                        ipAddress = clientIp,
                        targetPath = "/users/${targetUser.userId}",
                        dbStatus = "SUCCESS"
                    )
                },
                onFailure = { err ->
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Personal info update failed: ${err.localizedMessage ?: "Error"}")
                    }
                }
            )
        }
    }

    fun adminResetUserOnboarding(
        targetUser: UserProfile,
        adminProfile: UserProfile?
    ) {
        if (targetUser.isDesignatedAdminEmail) {
            _adminFilterState.update {
                it.copy(statusFeedback = "Primary Administrator does not require role onboarding.")
            }
            return
        }
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val adminName = adminProfile?.displayName ?: defaultDisplayName.ifBlank { "Administrator" }
            val adminEmail = adminProfile?.email ?: defaultEmail
            val result = activityRepository.resetUserOnboardingAsAdmin(targetUser)
            result.fold(
                onSuccess = {
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Reset onboarding for ${targetUser.displayName}")
                    }
                    activityRepository.logActivity(
                        userDisplayName = adminName,
                        userEmail = adminEmail,
                        eventType = ActivityEventType.ADMIN_AUDIT_ACTION,
                        title = "Onboarding Reset: ${targetUser.displayName}",
                        details = "Admin reset onboardingCompleted=false for ${targetUser.email.ifBlank { targetUser.userId }} • Target: /users/${targetUser.userId} • IP: $clientIp",
                        severity = ActivitySeverity.HIGHLIGHT,
                        ipAddress = clientIp,
                        targetPath = "/users/${targetUser.userId}",
                        dbStatus = "SUCCESS"
                    )
                },
                onFailure = { err ->
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Onboarding reset failed: ${err.localizedMessage ?: "Error"}")
                    }
                }
            )
        }
    }

    fun adminPurgeUserActivities(
        targetUser: UserProfile,
        adminProfile: UserProfile?
    ) {
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val adminName = adminProfile?.displayName ?: defaultDisplayName.ifBlank { "Administrator" }
            val adminEmail = adminProfile?.email ?: defaultEmail
            val result = activityRepository.purgeUserActivitiesAsAdmin(targetUser.userId)
            result.fold(
                onSuccess = { count ->
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Purged $count activity log(s) for ${targetUser.displayName}")
                    }
                    activityRepository.logActivity(
                        userDisplayName = adminName,
                        userEmail = adminEmail,
                        eventType = ActivityEventType.ADMIN_AUDIT_ACTION,
                        title = "Purged $count Activity Logs for ${targetUser.displayName}",
                        details = "Admin cleared $count recorded activities for userId=${targetUser.userId} • IP: $clientIp",
                        severity = ActivitySeverity.SECURITY,
                        ipAddress = clientIp,
                        targetPath = "/activities",
                        dbStatus = "SUCCESS"
                    )
                },
                onFailure = { err ->
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Failed to purge user logs: ${err.localizedMessage ?: "Error"}")
                    }
                }
            )
        }
    }

    fun adminDeleteUser(
        targetUser: UserProfile,
        adminProfile: UserProfile?
    ) {
        if (targetUser.isDesignatedAdminEmail || targetUser.userId == currentUserId) {
            _adminFilterState.update {
                it.copy(statusFeedback = "Cannot delete your own active Administrator account.")
            }
            return
        }
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val adminName = adminProfile?.displayName ?: defaultDisplayName.ifBlank { "Administrator" }
            val adminEmail = adminProfile?.email ?: defaultEmail
            val result = activityRepository.deleteUserAsAdmin(targetUser.userId)
            result.fold(
                onSuccess = {
                    _adminFilterState.update {
                        it.copy(
                            selectedManagedUserId = if (it.selectedManagedUserId == targetUser.userId) null else it.selectedManagedUserId,
                            statusFeedback = "Deleted user ${targetUser.displayName} from Firestore"
                        )
                    }
                    activityRepository.logActivity(
                        userDisplayName = adminName,
                        userEmail = adminEmail,
                        eventType = ActivityEventType.USER_DELETED,
                        title = "Deleted User Account: ${targetUser.displayName} (Exit)",
                        details = "Removed user profile /users/${targetUser.userId} (${targetUser.email}) • Role: ${targetUser.effectivePlatformRole.label} • IP: $clientIp",
                        severity = ActivitySeverity.SECURITY,
                        ipAddress = clientIp,
                        targetPath = "/users/${targetUser.userId}",
                        dbStatus = "SUCCESS"
                    )
                },
                onFailure = { err ->
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Delete user failed: ${err.localizedMessage ?: "Permission denied"}")
                    }
                    activityRepository.logActivity(
                        userDisplayName = adminName,
                        userEmail = adminEmail,
                        eventType = ActivityEventType.DB_OPERATION_FAILURE,
                        title = "Failed Delete on /users/${targetUser.userId}",
                        details = "Error deleting user: ${err.localizedMessage ?: "Permission denied"} • IP: $clientIp",
                        severity = ActivitySeverity.SECURITY,
                        ipAddress = clientIp,
                        targetPath = "/users/${targetUser.userId}",
                        dbStatus = "FAILURE"
                    )
                }
            )
        }
    }

    fun adminProvisionUser(
        displayName: String,
        email: String,
        role: PlatformRole,
        status: AccountStatus = AccountStatus.ACTIVE,
        adminProfile: UserProfile?
    ) {
        val cleanName = displayName.trim()
        val cleanEmail = email.trim()
        if (cleanName.isEmpty() || cleanEmail.isEmpty()) {
            _adminFilterState.update {
                it.copy(statusFeedback = "Please provide both a display name and email to register a user.")
            }
            return
        }
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val adminName = adminProfile?.displayName ?: defaultDisplayName.ifBlank { "Administrator" }
            val adminEmail = adminProfile?.email ?: defaultEmail
            val combinedInput = "$cleanName $cleanEmail"
            if (DatabaseSecurityInspector.containsInjectionSignature(combinedInput)) {
                val sig = DatabaseSecurityInspector.matchedSignatureDescription(combinedInput)
                _adminFilterState.update {
                    it.copy(statusFeedback = "Blocked injection attempt in user provisioning: $sig")
                }
                activityRepository.logActivity(
                    userDisplayName = adminName,
                    userEmail = adminEmail,
                    eventType = ActivityEventType.DB_INJECTION_BLOCKED,
                    title = "Blocked Injection Attempt Targeting /users",
                    details = "Signature: $sig • Input: '${combinedInput.take(120)}' • Blocked direct database write • IP: $clientIp",
                    severity = ActivitySeverity.SECURITY,
                    ipAddress = clientIp,
                    targetPath = "/users",
                    dbStatus = "INJECTION_BLOCKED"
                )
                return@launch
            }

            val newUid = "usr_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val newProfile = UserProfile(
                userId = newUid,
                displayName = cleanName,
                email = cleanEmail,
                headline = "${role.label} • Aura E-Learning Platform",
                bio = "Provisioned by Administrator ($adminEmail) with default status ${status.label}.",
                roleTitle = role.label,
                statusMessage = if (status == AccountStatus.PENDING) "Pending Admin Approval" else "Active",
                actorRole = role.code,
                accountStatus = status.code,
                onboardingCompleted = true,
                educationSystem = if (role == PlatformRole.STUDENT) EducationSystem.BILINGUAL.code else "",
                studentLevel = if (role == PlatformRole.STUDENT) StudentLevel.ADVANCED_LEVEL.code else "",
                studentStream = if (role == PlatformRole.STUDENT) StudentStream.SCIENCE.code else ""
            )
            val result = activityRepository.provisionUserInDirectoryAsAdmin(newProfile)
            result.fold(
                onSuccess = {
                    if (role == PlatformRole.ADMIN) {
                        activityRepository.updateUserRoleAsAdmin(newProfile, PlatformRole.ADMIN)
                    }
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Provisioned ${newProfile.displayName} (${role.label}, ${status.label})")
                    }
                    activityRepository.logActivity(
                        userDisplayName = newProfile.displayName,
                        userEmail = newProfile.email,
                        eventType = ActivityEventType.PROFILE_CREATED,
                        title = "Provisioned User: ${newProfile.displayName} (${role.label})",
                        details = "Created user document /users/$newUid • Role: ${role.code} • Status: ${status.code} • Provisioned by $adminEmail • IP: $clientIp",
                        severity = ActivitySeverity.SUCCESS,
                        ipAddress = clientIp,
                        targetPath = "/users/$newUid",
                        dbStatus = "SUCCESS"
                    )
                },
                onFailure = { err ->
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Provisioning failed: ${err.localizedMessage ?: "Error"}")
                    }
                    activityRepository.logActivity(
                        userDisplayName = adminName,
                        userEmail = adminEmail,
                        eventType = ActivityEventType.DB_OPERATION_FAILURE,
                        title = "Database Provisioning Failure on /users/$newUid",
                        details = "Failed to create user document: ${err.localizedMessage ?: "Permission denied"} • IP: $clientIp",
                        severity = ActivitySeverity.SECURITY,
                        ipAddress = clientIp,
                        targetPath = "/users/$newUid",
                        dbStatus = "FAILURE"
                    )
                }
            )
        }
    }

    fun logDatabaseSecurityProbe(
        probeType: String,
        payloadInput: String,
        targetPathInput: String,
        currentProfile: UserProfile?
    ) {
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val name = currentProfile?.displayName ?: defaultDisplayName.ifBlank { "Aura Admin" }
            val email = currentProfile?.email ?: defaultEmail
            val cleanTarget = targetPathInput.trim().ifBlank { "/users/$currentUserId" }

            when (probeType) {
                "CONNECTION_ATTEMPT" -> {
                    activityRepository.logActivity(
                        userDisplayName = name,
                        userEmail = email,
                        eventType = ActivityEventType.DB_CONNECTION_ATTEMPT,
                        title = "Direct Database Connection Attempt (IP: $clientIp)",
                        details = "Verified TLS socket & Firestore RPC channel to database '$databaseId' targeting $cleanTarget • Source IP: $clientIp",
                        severity = ActivitySeverity.INFO,
                        ipAddress = clientIp,
                        targetPath = cleanTarget,
                        dbStatus = "ATTEMPT"
                    )
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Logged IP connection attempt ($clientIp → $cleanTarget)")
                    }
                }

                "DB_SUCCESS" -> {
                    repository.getProfileById(currentUserId)
                    activityRepository.logActivity(
                        userDisplayName = name,
                        userEmail = email,
                        eventType = ActivityEventType.DB_OPERATION_SUCCESS,
                        title = "Verified Direct Database Read/Write Success",
                        details = "Authenticated Firestore transaction succeeded on $cleanTarget • Zero-trust schema validated • Source IP: $clientIp",
                        severity = ActivitySeverity.SUCCESS,
                        ipAddress = clientIp,
                        targetPath = cleanTarget,
                        dbStatus = "SUCCESS"
                    )
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Logged verified database success on $cleanTarget")
                    }
                }

                "DB_FAILURE" -> {
                    activityRepository.logActivity(
                        userDisplayName = name,
                        userEmail = email,
                        eventType = ActivityEventType.DB_OPERATION_FAILURE,
                        title = "Rejected Unauthorized Direct Database Contact",
                        details = "PERMISSION_DENIED: Unverified or out-of-scope write rejected by Firestore security rules on $cleanTarget • Source IP: $clientIp",
                        severity = ActivitySeverity.SECURITY,
                        ipAddress = clientIp,
                        targetPath = cleanTarget,
                        dbStatus = "FAILURE"
                    )
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Logged database failure telemetry for $cleanTarget")
                    }
                }

                "INJECTION_BLOCKED" -> {
                    val samplePayload = payloadInput.trim().ifBlank {
                        """{"${'$'}where": "this.actorRole == 'ADMIN'"} OR '1'='1' -- DROP TABLE users"""
                    }
                    val sig = DatabaseSecurityInspector.matchedSignatureDescription(samplePayload)
                    activityRepository.logActivity(
                        userDisplayName = name,
                        userEmail = email,
                        eventType = ActivityEventType.DB_INJECTION_BLOCKED,
                        title = "Blocked Database Injection Attack ($sig)",
                        details = "Intercepted direct database payload targeting $cleanTarget • Signature: $sig • Payload: '${samplePayload.take(140)}' • Source IP: $clientIp",
                        severity = ActivitySeverity.SECURITY,
                        ipAddress = clientIp,
                        targetPath = cleanTarget,
                        dbStatus = "INJECTION_BLOCKED"
                    )
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Firewall intercepted & logged injection attempt ($sig)")
                    }
                }
            }
        }
    }

    fun logManualAuditEvent(currentProfile: UserProfile?) {
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val name = currentProfile?.displayName ?: defaultDisplayName.ifBlank { "Aura Admin" }
            val email = currentProfile?.email ?: defaultEmail
            val result = activityRepository.logActivity(
                userDisplayName = name,
                userEmail = email,
                eventType = ActivityEventType.ADMIN_AUDIT_ACTION,
                title = "Portal Security & Telemetry Snapshot",
                details = "Manual audit checkpoint recorded from Admin Portal Dashboard by $name • Client IP: $clientIp.",
                severity = ActivitySeverity.SECURITY,
                ipAddress = clientIp,
                targetPath = "/admins/$currentUserId",
                dbStatus = "SUCCESS"
            )
            if (result.isSuccess) {
                _adminFilterState.update {
                    it.copy(statusFeedback = "Audit checkpoint logged to Firestore (IP: $clientIp)")
                }
            }
        }
    }

    fun updateActivityReview(activityId: String, newStatus: ActivityReviewStatus) {
        viewModelScope.launch {
            val result = activityRepository.updateActivityReviewStatus(activityId, newStatus)
            result.fold(
                onSuccess = {
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Activity marked as ${newStatus.label}")
                    }
                },
                onFailure = { err ->
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Review update failed: ${err.localizedMessage ?: "Permission denied"}")
                    }
                }
            )
        }
    }

    fun deleteActivityLog(activityId: String) {
        viewModelScope.launch {
            val result = activityRepository.deleteActivity(activityId)
            result.fold(
                onSuccess = {
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Activity log removed from Firestore")
                    }
                },
                onFailure = { err ->
                    _adminFilterState.update {
                        it.copy(statusFeedback = "Delete failed: ${err.localizedMessage ?: "Permission denied"}")
                    }
                }
            )
        }
    }

    fun startEditing(currentProfile: UserProfile) {
        _editorDraft.value = ProfileEditorDraft(
            displayName = currentProfile.displayName,
            headline = currentProfile.headline,
            roleTitle = currentProfile.roleTitle,
            bio = currentProfile.bio,
            location = currentProfile.location,
            website = currentProfile.website,
            statusMessage = currentProfile.statusMessage,
            isEditing = true,
            isSaving = false,
            statusBannerMessage = null
        )
    }

    fun cancelEditing() {
        _editorDraft.update { it.copy(isEditing = false, isSaving = false) }
    }

    fun updateDraftField(
        displayName: String? = null,
        headline: String? = null,
        roleTitle: String? = null,
        bio: String? = null,
        location: String? = null,
        website: String? = null,
        statusMessage: String? = null
    ) {
        _editorDraft.update { current ->
            current.copy(
                displayName = displayName ?: current.displayName,
                headline = headline ?: current.headline,
                roleTitle = roleTitle ?: current.roleTitle,
                bio = bio ?: current.bio,
                location = location ?: current.location,
                website = website ?: current.website,
                statusMessage = statusMessage ?: current.statusMessage,
                statusBannerMessage = null
            )
        }
    }

    fun saveProfileChanges(currentProfile: UserProfile) {
        if (currentProfile.isPendingAccount && !currentProfile.isDesignatedAdminEmail) {
            _editorDraft.update {
                it.copy(
                    isSaving = false,
                    statusBannerMessage = "Account is PENDING approval. Profile edits are disabled until set to ACTIVE."
                )
            }
            return
        }
        val draft = _editorDraft.value
        _editorDraft.update { it.copy(isSaving = true, statusBannerMessage = null) }
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val combinedInput = listOf(
                draft.displayName,
                draft.headline,
                draft.roleTitle,
                draft.bio,
                draft.location,
                draft.website,
                draft.statusMessage
            ).joinToString(" ")

            if (DatabaseSecurityInspector.containsInjectionSignature(combinedInput)) {
                val sig = DatabaseSecurityInspector.matchedSignatureDescription(combinedInput)
                _editorDraft.update {
                    it.copy(
                        isSaving = false,
                        statusBannerMessage = "Security Alert: Database injection pattern blocked ($sig)"
                    )
                }
                activityRepository.logActivity(
                    userDisplayName = currentProfile.displayName,
                    userEmail = currentProfile.email,
                    eventType = ActivityEventType.DB_INJECTION_BLOCKED,
                    title = "Blocked Database Injection on Profile Save ($sig)",
                    details = "Intercepted payload targeting /users/$currentUserId • Signature: $sig • Source IP: $clientIp",
                    severity = ActivitySeverity.SECURITY,
                    ipAddress = clientIp,
                    targetPath = "/users/$currentUserId",
                    dbStatus = "INJECTION_BLOCKED"
                )
                return@launch
            }

            val updated = currentProfile.copy(
                userId = currentUserId,
                displayName = draft.displayName.trim().ifEmpty { currentProfile.displayName },
                headline = draft.headline.trim(),
                roleTitle = draft.roleTitle.trim(),
                bio = draft.bio.trim(),
                location = draft.location.trim(),
                website = draft.website.trim(),
                statusMessage = draft.statusMessage.trim()
            )
            val statusChanged = updated.statusMessage != currentProfile.statusMessage
            sessionManager.saveCachedProfile(updated)
            val isOnlineNow = offlineSyncCoordinator.isEffectivelyOnline
            val result = if (isOnlineNow) {
                repository.saveProfile(updated)
            } else {
                Result.failure(IllegalStateException("Offline SQLite mode active"))
            }
            result.fold(
                onSuccess = {
                    offlineSyncCoordinator.saveProfileToSqliteAndQueueIfOffline(updated, queueForSync = false)
                    _editorDraft.update {
                        it.copy(
                            isEditing = false,
                            isSaving = false,
                            statusBannerMessage = "Profile saved to SQLite phone memory & synced to Firebase"
                        )
                    }
                    recordActivityWithSqliteFallback(
                        userDisplayName = updated.displayName,
                        userEmail = updated.email,
                        eventType = if (statusChanged) ActivityEventType.STATUS_CHANGED else ActivityEventType.PROFILE_UPDATED,
                        title = if (statusChanged) "Updated Status Badge to \"${updated.statusMessage}\"" else "Updated Personal Profile Details",
                        details = "Synced profile changes (Role: ${updated.roleTitle.ifBlank { "Member" }}, Completeness: ${updated.completionPercentage}%) • IP: $clientIp.",
                        severity = ActivitySeverity.SUCCESS,
                        ipAddress = clientIp,
                        targetPath = "/users/$currentUserId",
                        dbStatus = "SUCCESS"
                    )
                },
                onFailure = {
                    offlineSyncCoordinator.saveProfileToSqliteAndQueueIfOffline(updated, queueForSync = true)
                    _editorDraft.update {
                        it.copy(
                            isEditing = false,
                            isSaving = false,
                            statusBannerMessage = "Saved offline in SQLite phone memory (Queued for Firebase sync)"
                        )
                    }
                    recordActivityWithSqliteFallback(
                        userDisplayName = updated.displayName,
                        userEmail = updated.email,
                        eventType = if (statusChanged) ActivityEventType.STATUS_CHANGED else ActivityEventType.PROFILE_UPDATED,
                        title = if (statusChanged) "Updated Status Badge to \"${updated.statusMessage}\" (Offline SQLite)" else "Updated Profile Details (Offline SQLite)",
                        details = "Saved in phone SQLite & queued for Firebase sync (Role: ${updated.roleTitle.ifBlank { "Member" }}) • IP: $clientIp.",
                        severity = ActivitySeverity.SUCCESS,
                        ipAddress = clientIp,
                        targetPath = "/users/$currentUserId",
                        dbStatus = "SUCCESS"
                    )
                }
            )
        }
    }

    fun saveCurriculumSelection(
        currentProfile: UserProfile,
        educationSystem: String,
        studentLevel: String,
        studentStream: String,
        studentSeries: String,
        selectedSubjects: List<String>
    ) {
        if (currentProfile.isPendingAccount && !currentProfile.isDesignatedAdminEmail) {
            _editorDraft.update {
                it.copy(statusBannerMessage = "Account is PENDING approval. Curriculum changes are disabled until set to ACTIVE.")
            }
            return
        }
        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val combinedPayload = "$educationSystem $studentLevel $studentStream $studentSeries ${selectedSubjects.joinToString(" ")}"
            if (DatabaseSecurityInspector.containsInjectionSignature(combinedPayload)) {
                val sig = DatabaseSecurityInspector.matchedSignatureDescription(combinedPayload)
                _editorDraft.update {
                    it.copy(statusBannerMessage = "Security Alert: Blocked database injection pattern ($sig)")
                }
                return@launch
            }

            val joinedSubjects = UserProfile.joinListField(
                selectedSubjects,
                maxItems = 35
            ).take(1500)
            val updated = currentProfile.copy(
                userId = currentUserId,
                educationSystem = educationSystem.trim().take(60),
                studentLevel = studentLevel.trim().take(60),
                studentStream = studentStream.trim().take(60),
                studentSeries = studentSeries.trim().take(80),
                selectedSubjects = joinedSubjects
            )
            sessionManager.saveCachedProfile(updated)
            val isOnlineNow = offlineSyncCoordinator.isEffectivelyOnline
            val result = if (isOnlineNow) {
                repository.saveProfile(updated)
            } else {
                Result.failure(IllegalStateException("Offline SQLite mode active"))
            }
            result.fold(
                onSuccess = {
                    offlineSyncCoordinator.saveProfileToSqliteAndQueueIfOffline(updated, queueForSync = false)
                    _editorDraft.update {
                        it.copy(
                            statusBannerMessage = "Saved ${selectedSubjects.size} JSON curriculum subject(s) (${studentSeries.ifBlank { studentStream }})"
                        )
                    }
                    recordActivityWithSqliteFallback(
                        userDisplayName = updated.displayName,
                        userEmail = updated.email,
                        eventType = ActivityEventType.PROFILE_UPDATED,
                        title = "Updated JSON Curriculum Subjects (${selectedSubjects.size} Selected)",
                        details = "System: $educationSystem • Level: $studentLevel • Stream: $studentStream • Series: $studentSeries • Subjects: ${selectedSubjects.take(8).joinToString(", ")}",
                        severity = ActivitySeverity.SUCCESS,
                        ipAddress = clientIp,
                        targetPath = "/users/$currentUserId",
                        dbStatus = "SUCCESS"
                    )
                },
                onFailure = {
                    offlineSyncCoordinator.saveProfileToSqliteAndQueueIfOffline(updated, queueForSync = true)
                    _editorDraft.update {
                        it.copy(
                            statusBannerMessage = "Saved ${selectedSubjects.size} subject(s) to SQLite phone memory (Queued for Firebase sync)"
                        )
                    }
                    recordActivityWithSqliteFallback(
                        userDisplayName = updated.displayName,
                        userEmail = updated.email,
                        eventType = ActivityEventType.PROFILE_UPDATED,
                        title = "Updated Curriculum Subjects Offline (${selectedSubjects.size} Selected)",
                        details = "System: $educationSystem • Level: $studentLevel • Series: $studentSeries • Queued in SQLite for Firebase sync",
                        severity = ActivitySeverity.SUCCESS,
                        ipAddress = clientIp,
                        targetPath = "/users/$currentUserId",
                        dbStatus = "SUCCESS"
                    )
                }
            )
        }
    }

    fun updateStudentTargetObjective(objective: String) {
        _studentTargetObjective.value = objective
    }

    fun logStudentAcademicActivity(
        currentProfile: UserProfile,
        subject: String,
        scoreText: String,
        learningNote: String
    ) {
        if (currentProfile.effectivePlatformRole != PlatformRole.STUDENT) return
        if (currentProfile.isPendingAccount) {
            _editorDraft.update {
                it.copy(statusBannerMessage = "Account is PENDING approval. Student activities are paused until set to ACTIVE.")
            }
            return
        }
        val cleanSubject = subject.trim().ifEmpty { "Core Subject Assessment" }
        val cleanScore = scoreText.trim().ifEmpty { "15/20 (75%)" }
        val cleanNote = learningNote.trim().ifEmpty { "Completed structured study & mock assessment." }

        viewModelScope.launch {
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            val syncedOnline = recordActivityWithSqliteFallback(
                userDisplayName = currentProfile.displayName,
                userEmail = currentProfile.email,
                eventType = ActivityEventType.STUDENT_ASSESSMENT_LOGGED,
                title = "$cleanSubject — Score: $cleanScore",
                details = cleanNote,
                severity = ActivitySeverity.SUCCESS,
                ipAddress = clientIp,
                targetPath = "/activities",
                dbStatus = "SUCCESS"
            )
            _editorDraft.update {
                it.copy(
                    statusBannerMessage = if (syncedOnline) {
                        "Logged student activity: $cleanSubject ($cleanScore)"
                    } else {
                        "Saved '$cleanSubject ($cleanScore)' in SQLite phone memory (Queued for Firebase sync)"
                    }
                )
            }
        }
    }

    fun collectAndAnalyzeStudentOrientation(currentProfile: UserProfile) {
        // Strictly available only for active students
        if (currentProfile.effectivePlatformRole != PlatformRole.STUDENT || currentProfile.isPendingAccount) {
            return
        }
        _studentOrientationState.value = StudentOrientationUiState.Analyzing
        viewModelScope.launch {
            // 1. Collect all activities of the user from Firestore + observed user activities
            val snapshotActivities = activityRepository.getUserActivitiesSnapshot(currentUserId)
                .getOrDefault(emptyList())
            val observedActivities = (activitiesState.value as? ActivitiesUiState.Success)
                ?.activities
                ?.filter { it.userId == currentUserId }
                .orEmpty()

            val allUserActivities = (snapshotActivities + observedActivities)
                .distinctBy { it.activityId.ifBlank { "${it.eventType}_${it.title}" } }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }

            // 2. Run High-Thinking Student Level & Orientation Analysis
            val targetObjective = _studentTargetObjective.value.trim()
            val result = geminiService.analyzeStudentActivitiesWithHighThinking(
                profile = currentProfile,
                activities = allUserActivities,
                targetObjectives = targetObjective
            )

            result.fold(
                onSuccess = { analysis ->
                    _studentOrientationState.value = StudentOrientationUiState.ResultReady(analysis)
                    activityRepository.logActivity(
                        userDisplayName = currentProfile.displayName,
                        userEmail = currentProfile.email,
                        eventType = ActivityEventType.STUDENT_ORIENTATION_ANALYZED,
                        title = "High-Thinking Student Level & Career Orientation Completed",
                        details = "Collected ${analysis.collectedActivitiesCount} activities • Success Probability: ${analysis.successProbabilityPercent}% • Level: ${analysis.studentLevelSummary.take(140)}",
                        severity = ActivitySeverity.HIGHLIGHT
                    )
                },
                onFailure = { error ->
                    _studentOrientationState.value = StudentOrientationUiState.Error(
                        error.localizedMessage ?: "Student orientation analysis failed."
                    )
                }
            )
        }
    }

    fun dismissStatusBanner() {
        _editorDraft.update { it.copy(statusBannerMessage = null) }
    }

    fun openBafendReader(document: RevisionBafendDocument) {
        _activeReaderDocument.value = document
    }

    fun switchReaderBetweenQuestionAndSolution(viewSolution: Boolean) {
        val current = _activeReaderDocument.value ?: return
        if (viewSolution && current.hasSolution) {
            _activeReaderDocument.value = current.asSolutionView()
        } else {
            _activeReaderDocument.value = current.asQuestionView()
        }
    }

    fun closeBafendReader() {
        _activeReaderDocument.value = null
    }

    fun downloadBafendDocumentOffline(
        document: RevisionBafendDocument,
        currentProfile: UserProfile?,
        downloadSolutionToo: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = BafendFileEngine.saveBafendToOfflineFolder(getApplication(), document.asQuestionView())
            var solResultFileName: String? = null
            if ((downloadSolutionToo || document.isViewingSolutionInReader) && document.hasSolution) {
                val solRes = BafendFileEngine.saveSolutionBafendToOfflineFolder(getApplication(), document)
                solResultFileName = solRes.fileName
            }
            val isSolNowOffline = BafendFileEngine.isSolutionDownloadedOffline(getApplication(), document)
            offlineSyncCoordinator.offlineDao.updateRevisionDocLocalDownloadStatus(
                docId = document.docId,
                questionDownloaded = true,
                solutionDownloaded = isSolNowOffline
            )
            val msg = if (solResultFileName != null) {
                "Collected Pair (${result.fileName} + $solResultFileName) from Admin Drive to Local Tab!"
            } else {
                "Collected ${result.fileName} (${document.formattedExamSession}) from Admin Drive to Local Tab!"
            }
            _editorDraft.update { it.copy(statusBannerMessage = msg) }
            _adminFilterState.update { it.copy(statusFeedback = msg) }

            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            activityRepository.logActivity(
                userDisplayName = currentProfile?.displayName ?: defaultDisplayName.ifBlank { "Aura Learner" },
                userEmail = currentProfile?.email ?: defaultEmail,
                eventType = ActivityEventType.PROFILE_UPDATED,
                title = "Collected Past Paper from Admin Drive: ${result.fileName}",
                details = "Year: ${document.formattedExamSession} • Drive ID: ${document.resolvedQuestionDriveId} • Local Folder: ${result.folderPath} (${result.sizeBytes} bytes) • Subject: ${document.subjectName} (${document.subjectCode})",
                severity = ActivitySeverity.SUCCESS,
                ipAddress = clientIp,
                targetPath = "/revision_documents/${document.docId}",
                dbStatus = "SUCCESS"
            )
        }
    }

    fun downloadSolutionBafendOffline(
        document: RevisionBafendDocument,
        currentProfile: UserProfile?
    ) {
        if (!document.hasSolution) return
        viewModelScope.launch(Dispatchers.IO) {
            val solRes = BafendFileEngine.saveSolutionBafendToOfflineFolder(getApplication(), document)
            val isQOffline = BafendFileEngine.isDocumentDownloadedOffline(getApplication(), document)
            offlineSyncCoordinator.offlineDao.updateRevisionDocLocalDownloadStatus(
                docId = document.docId,
                questionDownloaded = isQOffline,
                solutionDownloaded = true
            )
            val msg = "Collected Paired Solution (${solRes.fileName} • ${document.formattedExamSession}) from Admin Drive to Local Tab!"
            _editorDraft.update { it.copy(statusBannerMessage = msg) }
            _adminFilterState.update { it.copy(statusFeedback = msg) }

            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()
            activityRepository.logActivity(
                userDisplayName = currentProfile?.displayName ?: defaultDisplayName.ifBlank { "Aura Learner" },
                userEmail = currentProfile?.email ?: defaultEmail,
                eventType = ActivityEventType.PROFILE_UPDATED,
                title = "Collected Past June Solution from Admin Drive: ${solRes.fileName}",
                details = "Pair ID: ${document.resolvedPairId} • Drive ID: ${document.resolvedSolutionDriveId} • Local Folder: ${solRes.folderPath}",
                severity = ActivitySeverity.SUCCESS,
                ipAddress = clientIp,
                targetPath = "/revision_documents/${document.docId}",
                dbStatus = "SUCCESS"
            )
        }
    }

    fun deleteLocalDownloadedBafend(document: RevisionBafendDocument) {
        viewModelScope.launch(Dispatchers.IO) {
            BafendFileEngine.deleteDownloadedOfflineFiles(getApplication(), document)
            offlineSyncCoordinator.offlineDao.updateRevisionDocLocalDownloadStatus(
                docId = document.docId,
                questionDownloaded = false,
                solutionDownloaded = false
            )
            val msg = "Removed ${document.bafendFileName} from Local Phone Downloads."
            _editorDraft.update { it.copy(statusBannerMessage = msg) }
        }
    }

    fun adminAddSubjectUnderSubsystem(
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
        adminProfile: UserProfile?
    ) {
        viewModelScope.launch {
            val adminName = adminProfile?.displayName ?: defaultDisplayName.ifBlank { "Administrator" }
            val adminEmail = adminProfile?.email ?: defaultEmail
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()

            val result = curriculumRevisionRepository.addCustomSubjectAsAdmin(
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
                adminEmail = adminEmail
            )
            result.onSuccess { added ->
                val feedback = "Added subject '${added.name} (${added.code})' under ${added.systemCode} • Coeff ${added.coefficient} (${added.category})"
                _adminFilterState.update { it.copy(statusFeedback = feedback) }
                _editorDraft.update { it.copy(statusBannerMessage = feedback) }
                activityRepository.logActivity(
                    userDisplayName = adminName,
                    userEmail = adminEmail,
                    eventType = ActivityEventType.ADMIN_AUDIT_ACTION,
                    title = "Admin Added Subject: ${added.name} (${added.code})",
                    details = "Subsystem: ${added.systemCode} • Level: ${added.levelCode} • Stream: ${added.streamCode} • Series: ${added.seriesCode} • Coeff: ${added.coefficient} • Category: ${added.category}",
                    severity = ActivitySeverity.HIGHLIGHT,
                    ipAddress = clientIp,
                    targetPath = "/curriculum_subjects/${added.subjectId}",
                    dbStatus = "SUCCESS"
                )
            }
        }
    }

    fun adminDeleteCustomSubject(subjectId: String) {
        viewModelScope.launch {
            curriculumRevisionRepository.deleteCustomSubjectAsAdmin(subjectId)
            _adminFilterState.update {
                it.copy(statusFeedback = "Removed custom subject ($subjectId) from catalog.")
            }
        }
    }

    fun adminUploadPdfAsBafend(
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
        rawPdfBytes: ByteArray?,
        includeSolution: Boolean = false,
        solutionOriginalPdfName: String = "",
        rawSolutionPdfBytes: ByteArray? = null,
        adminProfile: UserProfile?
    ) {
        viewModelScope.launch {
            val adminName = adminProfile?.displayName ?: defaultDisplayName.ifBlank { "Administrator" }
            val adminEmail = adminProfile?.email ?: defaultEmail
            val clientIp = DatabaseSecurityInspector.detectClientIpAddress()

            val result = curriculumRevisionRepository.uploadPdfAsBafendDocument(
                context = getApplication(),
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
                examYear = examYear,
                originalPdfName = originalPdfName,
                rawUploadedPdfBytes = rawPdfBytes,
                includeSolution = includeSolution,
                solutionOriginalPdfName = solutionOriginalPdfName,
                rawSolutionPdfBytes = rawSolutionPdfBytes,
                adminEmail = adminEmail
            )
            result.onSuccess { doc ->
                val pairSuffix = if (doc.hasSolution) {
                    " + Paired Solution '${doc.solutionBafendFileName}' (Pair ID: ${doc.resolvedPairId})"
                } else {
                    ""
                }
                val feedback = "Uploaded Past Paper (${doc.formattedExamSession}) '${doc.bafendFileName}'$pairSuffix to Admin Drive (${doc.resolvedQuestionDriveId})!"
                _adminFilterState.update { it.copy(statusFeedback = feedback) }
                _editorDraft.update { it.copy(statusBannerMessage = feedback) }
                activityRepository.logActivity(
                    userDisplayName = adminName,
                    userEmail = adminEmail,
                    eventType = ActivityEventType.ADMIN_AUDIT_ACTION,
                    title = "Admin Uploaded Past Paper (${doc.formattedExamSession}): ${doc.bafendFileName}",
                    details = "Subject: ${doc.subjectName} (${doc.subjectCode}) • Year: ${doc.examYear} • Drive ID: ${doc.resolvedQuestionDriveId} • Paired Solution: ${doc.hasSolution} (Pair: ${doc.resolvedPairId})",
                    severity = ActivitySeverity.HIGHLIGHT,
                    ipAddress = clientIp,
                    targetPath = "/revision_documents/${doc.docId}",
                    dbStatus = "SUCCESS"
                )
            }
        }
    }

    fun adminDeleteRevisionDocument(doc: RevisionBafendDocument) {
        viewModelScope.launch {
            curriculumRevisionRepository.deleteRevisionDocumentAsAdmin(doc.docId)
            _adminFilterState.update {
                it.copy(statusFeedback = "Removed ${doc.bafendFileName} from Revision Tab.")
            }
        }
    }

    fun toggleManualOfflineMode(enabled: Boolean) {
        offlineSyncCoordinator.setManualOfflineMode(enabled)
        val msg = if (enabled) {
            "Offline SQLite Mode enabled: All views load from phone SQLite & actions queue for Firebase sync."
        } else {
            "Online Mode restored: Synchronizing SQLite outbox with Firebase…"
        }
        _editorDraft.update { it.copy(statusBannerMessage = msg) }
        _adminFilterState.update { it.copy(statusFeedback = msg) }
    }

    fun triggerManualFirebaseSync() {
        viewModelScope.launch {
            if (offlineSyncCoordinator.isManualOfflineMode.value) {
                offlineSyncCoordinator.setManualOfflineMode(false)
            }
            val count = offlineSyncCoordinator.synchronizePendingOutboxWithFirebase()
            val msg = if (count > 0) {
                "Synchronized $count offline SQLite action(s) with online Firebase!"
            } else {
                "SQLite phone memory is up-to-date and synchronized with Firebase."
            }
            _editorDraft.update { it.copy(statusBannerMessage = msg) }
            _adminFilterState.update { it.copy(statusFeedback = msg) }
        }
    }

    private suspend fun recordActivityWithSqliteFallback(
        userDisplayName: String,
        userEmail: String,
        eventType: ActivityEventType,
        title: String,
        details: String,
        severity: ActivitySeverity = ActivitySeverity.INFO,
        ipAddress: String = DatabaseSecurityInspector.detectClientIpAddress(),
        targetPath: String = "",
        dbStatus: String = "SUCCESS"
    ): Boolean {
        val rawId = "act_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val sanitizedId = rawId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val localActivity = AppActivity(
            activityId = sanitizedId,
            userId = currentUserId,
            userDisplayName = userDisplayName.trim().ifEmpty { "Aura Member" },
            userEmail = userEmail.trim(),
            eventType = eventType.code,
            title = title.trim(),
            details = details.trim(),
            severity = severity.code,
            reviewStatus = ActivityReviewStatus.RECORDED.code,
            ipAddress = ipAddress,
            targetPath = targetPath.ifBlank { "/activities/$sanitizedId" },
            dbStatus = dbStatus,
            createdAt = com.google.firebase.Timestamp.now(),
            updatedAt = com.google.firebase.Timestamp.now()
        )
        if (!offlineSyncCoordinator.isEffectivelyOnline) {
            offlineSyncCoordinator.saveActivityToSqliteAndQueueIfOffline(localActivity, queueForSync = true)
            return false
        }
        val res = activityRepository.logActivity(
            userDisplayName = userDisplayName,
            userEmail = userEmail,
            eventType = eventType,
            title = title,
            details = details,
            severity = severity,
            customActivityId = sanitizedId,
            ipAddress = ipAddress,
            targetPath = targetPath,
            dbStatus = dbStatus
        )
        return if (res.isSuccess) {
            offlineSyncCoordinator.saveActivityToSqliteAndQueueIfOffline(localActivity, queueForSync = false)
            true
        } else {
            offlineSyncCoordinator.saveActivityToSqliteAndQueueIfOffline(localActivity, queueForSync = true)
            false
        }
    }

    class Factory(
        private val application: Application,
        private val currentUserId: String,
        private val defaultDisplayName: String,
        private val defaultEmail: String,
        private val defaultPhotoUrl: String,
        private val isEmailVerified: Boolean = true
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(
                application = application,
                currentUserId = currentUserId,
                defaultDisplayName = defaultDisplayName,
                defaultEmail = defaultEmail,
                defaultPhotoUrl = defaultPhotoUrl,
                isEmailVerified = isEmailVerified
            ) as T
        }
    }

    private companion object {
        const val TAG = "ProfileViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
