package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.auth.signOutUser
import com.example.data.model.EducationSystem
import com.example.data.model.PlatformRole
import com.example.data.model.SchoolOwnership
import com.example.data.model.StudentLevel
import com.example.data.model.StudentStream
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    isEmailVerified: Boolean,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val isAdminState by viewModel.isAdmin.collectAsStateWithLifecycle()
    val keepSignedIn by viewModel.keepSignedIn.collectAsStateWithLifecycle()
    val isRoleOnboardingActive by viewModel.isRoleOnboardingActive.collectAsStateWithLifecycle()
    val isCompletingOnboarding by viewModel.isCompletingOnboarding.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val activitiesState by viewModel.activitiesState.collectAsStateWithLifecycle()
    val allUsersState by viewModel.allUsersState.collectAsStateWithLifecycle()
    val adminFilterState by viewModel.adminFilterState.collectAsStateWithLifecycle()
    val editorDraft by viewModel.editorDraft.collectAsStateWithLifecycle()
    val studentOrientationState by viewModel.studentOrientationState.collectAsStateWithLifecycle()
    val studentTargetObjective by viewModel.studentTargetObjective.collectAsStateWithLifecycle()
    val adminCustomSubjects by viewModel.adminCustomSubjectsState.collectAsStateWithLifecycle()
    val revisionDocuments by viewModel.revisionDocumentsState.collectAsStateWithLifecycle()
    val activeReaderDocument by viewModel.activeReaderDocument.collectAsStateWithLifecycle()
    val activeReaderViewingSolution by viewModel.activeReaderViewingSolution.collectAsStateWithLifecycle()
    val sqliteTelemetry by viewModel.sqliteTelemetryState.collectAsStateWithLifecycle()

    val currentProfile = (profileState as? ProfileUiState.Success)?.profile
    val canAccessAdminPortal = isAdminState == true ||
        currentProfile?.isDesignatedAdminEmail == true ||
        currentProfile?.effectivePlatformRole == PlatformRole.ADMIN
    val isInAdminDestination = (currentDestination == AuthenticatedDestination.ADMIN_PORTAL ||
        currentDestination == AuthenticatedDestination.USER_MANAGEMENT) && canAccessAdminPortal
    val isInRevisionHub = currentDestination == AuthenticatedDestination.REVISION_HUB ||
        currentDestination == AuthenticatedDestination.LOCAL_DOWNLOADS

    BackHandler(enabled = isInAdminDestination || isInRevisionHub) {
        if (activeReaderDocument != null) {
            viewModel.closeBafendReader()
        } else {
            viewModel.navigateTo(AuthenticatedDestination.PROFILE)
        }
    }

    BackHandler(enabled = currentDestination == AuthenticatedDestination.PROFILE && editorDraft.isEditing) {
        viewModel.cancelEditing()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isInAdminDestination) {
                                stringResource(R.string.admin_portal_title)
                            } else {
                                stringResource(R.string.app_name)
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isInAdminDestination) {
                                Icons.Default.AdminPanelSettings
                            } else {
                                Icons.Default.Verified
                            },
                            contentDescription = stringResource(R.string.cd_verified_icon),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    if (canAccessAdminPortal) {
                        IconButton(
                            onClick = {
                                val nextDest = if (isInAdminDestination) {
                                    AuthenticatedDestination.PROFILE
                                } else {
                                    AuthenticatedDestination.USER_MANAGEMENT
                                }
                                viewModel.navigateTo(nextDest)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_admin_portal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = stringResource(R.string.cd_open_admin_portal),
                                tint = if (isInAdminDestination) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                    if (currentDestination == AuthenticatedDestination.PROFILE &&
                        currentProfile != null &&
                        !currentProfile.isPendingAccount &&
                        currentProfile.onboardingCompleted &&
                        !isRoleOnboardingActive &&
                        !editorDraft.isEditing
                    ) {
                        IconButton(
                            onClick = { viewModel.startEditing(currentProfile) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_edit_profile),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.logSignOutAndClearSession(currentProfile) {
                                signOutUser(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onSignOutComplete = onSignedOut,
                                    scope = scope
                                )
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.cd_sign_out),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            val isLockedOrOnboarding = !canAccessAdminPortal &&
                (currentProfile?.isPendingAccount == true ||
                    currentProfile?.requiresRoleOnboarding == true ||
                    isRoleOnboardingActive)
            if (!isLockedOrOnboarding) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("main_bottom_navigation")
                ) {
                    NavigationBarItem(
                        selected = currentDestination == AuthenticatedDestination.PROFILE,
                        onClick = {
                            viewModel.closeBafendReader()
                            viewModel.navigateTo(AuthenticatedDestination.PROFILE)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(R.string.nav_profile)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_profile)) },
                        modifier = Modifier.testTag("nav_profile_tab")
                    )
                    NavigationBarItem(
                        selected = currentDestination == AuthenticatedDestination.REVISION_HUB,
                        onClick = {
                            viewModel.navigateTo(AuthenticatedDestination.REVISION_HUB)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Past Papers"
                            )
                        },
                        label = { Text("Past Papers") },
                        modifier = Modifier.testTag("nav_revision_hub_tab")
                    )
                    NavigationBarItem(
                        selected = currentDestination == AuthenticatedDestination.LOCAL_DOWNLOADS,
                        onClick = {
                            viewModel.closeBafendReader()
                            viewModel.navigateTo(AuthenticatedDestination.LOCAL_DOWNLOADS)
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = "Local Tab (Offline Downloads)"
                            )
                        },
                        label = { Text("Local Tab") },
                        modifier = Modifier.testTag("nav_local_downloads_tab")
                    )
                    if (canAccessAdminPortal) {
                        NavigationBarItem(
                            selected = currentDestination == AuthenticatedDestination.USER_MANAGEMENT ||
                                (isInAdminDestination && adminFilterState.activeTab == AdminSectionTab.MANAGE_USERS),
                            onClick = {
                                viewModel.closeBafendReader()
                                viewModel.selectAdminSectionTab(AdminSectionTab.MANAGE_USERS)
                                viewModel.navigateTo(AuthenticatedDestination.USER_MANAGEMENT)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = stringResource(R.string.nav_manage_users)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_manage_users)) },
                            modifier = Modifier.testTag("nav_manage_users_tab")
                        )
                        NavigationBarItem(
                            selected = currentDestination == AuthenticatedDestination.ADMIN_PORTAL &&
                                adminFilterState.activeTab != AdminSectionTab.MANAGE_USERS,
                            onClick = {
                                viewModel.closeBafendReader()
                                viewModel.selectAdminSectionTab(AdminSectionTab.CURRICULUM_AND_PDFS)
                                viewModel.navigateTo(AuthenticatedDestination.ADMIN_PORTAL)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = stringResource(R.string.nav_admin_portal)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_admin_portal)) },
                            modifier = Modifier.testTag("nav_admin_portal_tab")
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activeReaderDocument == null) {
                OfflineSqliteSyncBanner(
                    telemetry = sqliteTelemetry,
                    onToggleManualOfflineMode = viewModel::toggleManualOfflineMode,
                    onTriggerManualFirebaseSync = viewModel::triggerManualFirebaseSync,
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
            if (isInRevisionHub) {
                RevisionHubScreen(
                    currentProfile = currentProfile,
                    isAdmin = canAccessAdminPortal,
                    revisionDocuments = revisionDocuments,
                    adminCustomSubjects = adminCustomSubjects,
                    activeReaderDocument = activeReaderDocument,
                    activeViewingSolution = activeReaderViewingSolution,
                    initialTabMode = if (currentDestination == AuthenticatedDestination.LOCAL_DOWNLOADS) {
                        "LOCAL_DOWNLOADS"
                    } else {
                        "CATALOG"
                    },
                    statusBannerMessage = editorDraft.statusBannerMessage,
                    onDismissBanner = viewModel::dismissStatusBanner,
                    onOpenBafendReader = viewModel::openBafendReader,
                    onCloseBafendReader = viewModel::closeBafendReader,
                    onDownloadBafendOffline = { doc ->
                        viewModel.downloadBafendDocumentOffline(doc, currentProfile)
                    },
                    onDownloadSolutionOffline = { doc ->
                        viewModel.downloadSolutionDocumentOffline(doc, currentProfile)
                    },
                    onSwitchReaderMode = viewModel::switchBafendReaderMode,
                    onDeleteOfflineLocalCopy = viewModel::removeOfflineDownloadedCopy,
                    onOpenAdminCurriculumStudio = {
                        viewModel.selectAdminSectionTab(AdminSectionTab.CURRICULUM_AND_PDFS)
                        viewModel.navigateTo(AuthenticatedDestination.ADMIN_PORTAL)
                    },
                    onDeleteRevisionDocument = viewModel::adminDeleteRevisionDocument
                )
            } else if (isInAdminDestination) {
                AdminPortalContent(
                    activitiesState = activitiesState,
                    allUsers = allUsersState,
                    currentProfile = currentProfile,
                    isAdmin = canAccessAdminPortal,
                    filterState = adminFilterState,
                    onSearchQueryChange = viewModel::updateAdminSearchQuery,
                    onEventTypeFilterChange = viewModel::updateAdminEventTypeFilter,
                    onReviewStatusFilterChange = viewModel::updateAdminReviewStatusFilter,
                    onLogManualAudit = { viewModel.logManualAuditEvent(currentProfile) },
                    onUpdateActivityReview = viewModel::updateActivityReview,
                    onDeleteActivity = viewModel::deleteActivityLog,
                    onDismissFeedback = viewModel::dismissAdminFeedback,
                    onSelectAdminTab = viewModel::selectAdminSectionTab,
                    onUserSearchQueryChange = viewModel::updateAdminUserSearchQuery,
                    onUserRoleFilterChange = viewModel::updateAdminUserRoleFilter,
                    onUserStatusFilterChange = viewModel::updateAdminUserStatusFilter,
                    onChangeUserRole = { targetUser, newRole ->
                        viewModel.adminChangeUserRole(targetUser, newRole, currentProfile)
                    },
                    onChangeUserStatus = { targetUser, newStatus ->
                        viewModel.adminChangeUserStatus(targetUser, newStatus, currentProfile)
                    },
                    onDeleteUser = { targetUser ->
                        viewModel.adminDeleteUser(targetUser, currentProfile)
                    },
                    onProvisionUser = { name, email, role, status ->
                        viewModel.adminProvisionUser(name, email, role, status, currentProfile)
                    },
                    onOpenUserDetails = viewModel::openManagedUserDetails,
                    onCloseUserDetails = viewModel::closeManagedUserDetails,
                    onChangeUserStatusWithReason = { targetUser, newStatus, reasonNote ->
                        viewModel.adminChangeUserStatus(targetUser, newStatus, currentProfile, reasonNote)
                    },
                    onUpdateUserPersonalInfo = { targetUser, name, headline, title, loc, bio, statusMsg, phones, school ->
                        viewModel.adminUpdateUserPersonalInfo(
                            targetUser = targetUser,
                            displayName = name,
                            headline = headline,
                            roleTitle = title,
                            location = loc,
                            bio = bio,
                            statusMessage = statusMsg,
                            phoneNumbers = phones,
                            schoolName = school,
                            adminProfile = currentProfile
                        )
                    },
                    onResetUserOnboarding = { targetUser ->
                        viewModel.adminResetUserOnboarding(targetUser, currentProfile)
                    },
                    onPurgeUserActivities = { targetUser ->
                        viewModel.adminPurgeUserActivities(targetUser, currentProfile)
                    },
                    onDbFilterChange = viewModel::updateAdminDbLogFilter,
                    onTriggerSecurityProbe = { probeType, payload, targetPath ->
                        viewModel.logDatabaseSecurityProbe(probeType, payload, targetPath, currentProfile)
                    },
                    adminCustomSubjects = adminCustomSubjects,
                    revisionDocuments = revisionDocuments,
                    onAddSubjectUnderSubsystem = { code, name, coeff, cat, sys, lvl, strm, serCode, serName, desc ->
                        viewModel.adminAddSubjectUnderSubsystem(
                            code = code,
                            name = name,
                            coefficient = coeff,
                            category = cat,
                            systemCode = sys,
                            levelCode = lvl,
                            streamCode = strm,
                            seriesCode = serCode,
                            seriesName = serName,
                            description = desc,
                            adminProfile = currentProfile
                        )
                    },
                    onDeleteCustomSubject = viewModel::adminDeleteCustomSubject,
                    onUploadPdfForSubject = { title, desc, sys, lvl, strm, ser, subjCode, subjName, subjCat, coeff, origName, bytes, examYear, includeSol, origSolName, solBytes ->
                        viewModel.adminUploadPdfAsBafend(
                            title = title,
                            description = desc,
                            systemCode = sys,
                            levelCode = lvl,
                            streamCode = strm,
                            seriesCode = ser,
                            subjectCode = subjCode,
                            subjectName = subjName,
                            subjectCategory = subjCat,
                            coefficient = coeff,
                            originalPdfName = origName,
                            rawPdfBytes = bytes,
                            examYear = examYear,
                            includeSolution = includeSol,
                            originalSolutionPdfName = origSolName,
                            rawSolutionPdfBytes = solBytes,
                            adminProfile = currentProfile
                        )
                    },
                    onOpenBafendReader = { doc ->
                        viewModel.openBafendReader(doc)
                        viewModel.navigateTo(AuthenticatedDestination.REVISION_HUB)
                    },
                    onDeleteRevisionDocument = viewModel::adminDeleteRevisionDocument
                )
            } else {
                when (val state = profileState) {
                    is ProfileUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("profile_loading_state"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Loading your verified profile…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is ProfileUiState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 640.dp)
                                .padding(24.dp)
                                .testTag("profile_error_state")
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Unable to load profile",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    is ProfileUiState.Success -> {
                        val profile = state.profile
                        if (!canAccessAdminPortal && profile.isPendingAccount) {
                            PendingAccountLockCard(
                                profile = profile,
                                onSignOutClick = {
                                    viewModel.logSignOutAndClearSession(profile) {
                                        signOutUser(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onSignOutComplete = onSignedOut,
                                            scope = scope
                                        )
                                    }
                                }
                            )
                        } else if (!canAccessAdminPortal && (profile.requiresRoleOnboarding || isRoleOnboardingActive)) {
                            RoleOnboardingScreen(
                                profile = profile,
                                isSaving = isCompletingOnboarding,
                                canCancel = profile.onboardingCompleted,
                                onCompleteOnboarding = { submission ->
                                    viewModel.completeRoleOnboarding(profile, submission)
                                },
                                onCancel = {
                                    viewModel.closeRoleOnboarding()
                                }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 680.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                                    .testTag("final_profile_interface"),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                // Status banner feedback
                                AnimatedVisibility(visible = editorDraft.statusBannerMessage != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.dismissStatusBanner() }
                                            .testTag("profile_status_banner")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = stringResource(R.string.cd_verified_icon),
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = editorDraft.statusBannerMessage.orEmpty(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }

                                // 1. Hero Profile Header Card
                                ProfileHeroHeaderCard(
                                    profile = profile,
                                    canAccessAdminPortal = canAccessAdminPortal,
                                    isEditing = editorDraft.isEditing,
                                    onEditClick = { viewModel.startEditing(profile) },
                                    onOpenRoleSelector = { viewModel.openRoleOnboarding() },
                                    onOpenAdminPortal = { viewModel.navigateTo(AuthenticatedDestination.ADMIN_PORTAL) },
                                    onSignOutClick = {
                                        signOutUser(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onSignOutComplete = onSignedOut,
                                            scope = scope
                                        )
                                    }
                                )

                                // 2. E-Learning & Actor Role Summary Card
                                RoleIdentitySummaryCard(
                                    profile = profile,
                                    canAccessAdminPortal = canAccessAdminPortal,
                                    onChangeRoleClick = { viewModel.openRoleOnboarding() }
                                )

                                // 2B. Interactive JSON Curriculum & Subject Selection Card
                                CurriculumSubjectSelectorCard(
                                    profile = profile,
                                    onSaveCurriculumSelection = { sys, lvl, strm, series, subjects ->
                                        viewModel.saveCurriculumSelection(
                                            currentProfile = profile,
                                            educationSystem = sys,
                                            studentLevel = lvl,
                                            studentStream = strm,
                                            studentSeries = series,
                                            selectedSubjects = subjects
                                        )
                                    }
                                )

                                // 3. View / Edit Profile Details Card
                                if (editorDraft.isEditing) {
                                    ProfileEditorCard(
                                        draft = editorDraft,
                                        onDraftChange = viewModel::updateDraftField,
                                        onSave = { viewModel.saveProfileChanges(profile) },
                                        onCancel = { viewModel.cancelEditing() }
                                    )
                                } else {
                                    ProfileDetailsDisplayCard(
                                        profile = profile,
                                        isEmailVerified = isEmailVerified,
                                        keepSignedIn = keepSignedIn,
                                        onTogglePersistentSession = viewModel::togglePersistentSession
                                    )
                                }

                                // 4. Student-Only High-Thinking Level, Revision & Career Orientation Hub
                                if (profile.effectivePlatformRole == PlatformRole.STUDENT) {
                                    val userActivities = (activitiesState as? ActivitiesUiState.Success)
                                        ?.activities
                                        ?.filter { it.userId == profile.userId }
                                        .orEmpty()
                                    StudentHighThinkingOrientationCard(
                                        profile = profile,
                                        userActivities = userActivities,
                                        targetObjective = studentTargetObjective,
                                        orientationState = studentOrientationState,
                                        onTargetObjectiveChange = viewModel::updateStudentTargetObjective,
                                        onLogAcademicActivity = { subject, score, note ->
                                            viewModel.logStudentAcademicActivity(
                                                currentProfile = profile,
                                                subject = subject,
                                                scoreText = score,
                                                learningNote = note
                                            )
                                        },
                                        onTriggerCollectAndAnalyze = {
                                            viewModel.collectAndAnalyzeStudentOrientation(profile)
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ProfileHeroHeaderCard(
    profile: UserProfile,
    canAccessAdminPortal: Boolean,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onOpenRoleSelector: () -> Unit,
    onOpenAdminPortal: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(26.dp)
            )
            .testTag("profile_hero_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Overlapping Banner + Avatar
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = R.drawable.img_profile_banner,
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                // Status Pill in top-right of banner
                if (profile.statusMessage.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = profile.statusMessage,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Avatar overlapping bottom of banner
                Box(
                    modifier = Modifier
                        .padding(start = 22.dp)
                        .align(Alignment.BottomStart)
                        .offset(y = 34.dp)
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = profile.photoUrl,
                            contentDescription = stringResource(R.string.cd_user_avatar),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val initials = profile.displayName
                            .split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .take(2)
                            .joinToString("")
                            .ifEmpty { "AU" }
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(42.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_display_name")
                    )
                    Surface(
                        color = if (canAccessAdminPortal) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("profile_actor_role_badge")
                    ) {
                        Text(
                            text = if (canAccessAdminPortal) {
                                "ROLE: ADMIN"
                            } else {
                                "ACTOR: ${profile.effectivePlatformRole.label.uppercase()}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (canAccessAdminPortal) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                if (profile.headline.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile.headline,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("profile_headline")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = profile.email.ifBlank { "No email listed" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("profile_email")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile Completeness Meter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile Completeness",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${profile.completionPercentage}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { profile.completionPercentage / 100f },
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isEditing) {
                        Button(
                            onClick = onEditClick,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("hero_edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_profile),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.edit_profile))
                        }
                    }
                    OutlinedButton(
                        onClick = onSignOutClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("hero_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.sign_out),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.sign_out))
                    }
                }

                if (!canAccessAdminPortal) {
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onOpenRoleSelector,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("open_role_selector_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = stringResource(R.string.change_role_button),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.change_role_button),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (canAccessAdminPortal) {
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onOpenAdminPortal,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("hero_admin_portal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = stringResource(R.string.nav_admin_portal),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Admin Activity Portal",
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun RoleIdentitySummaryCard(
    profile: UserProfile,
    canAccessAdminPortal: Boolean,
    onChangeRoleClick: () -> Unit
) {
    val role = if (canAccessAdminPortal) PlatformRole.ADMIN else profile.effectivePlatformRole
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("role_identity_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (canAccessAdminPortal) {
                            "Platform Administration (Role: Admin)"
                        } else {
                            "E-Learning & Role Profile (${role.label})"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (canAccessAdminPortal) {
                            "Role: Admin (fokoufdr@gmail.com) • App Management & Global Activity Portal"
                        } else {
                            "Default Actor: Visitor • Selected Role: ${role.label}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!canAccessAdminPortal) {
                    AssistChip(
                        onClick = onChangeRoleClick,
                        label = { Text("Change Role") },
                        modifier = Modifier.testTag("summary_change_role_chip")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            when (role) {
                PlatformRole.ADMIN -> {
                    ProfileDetailRow(
                        icon = Icons.Default.AdminPanelSettings,
                        label = "Administrator Responsibilities",
                        value = "App Management, User Directory Oversight & Security Audit Portal (No questionnaire required)"
                    )
                }

                PlatformRole.VISITOR -> {
                    ProfileDetailRow(
                        icon = Icons.Default.Badge,
                        label = "Active Access Mode",
                        value = "Visitor Final Interface"
                    )
                }

                PlatformRole.STUDENT -> {
                    ProfileDetailRow(
                        icon = Icons.Default.Language,
                        label = "Educational System",
                        value = EducationSystem.fromCode(profile.educationSystem)?.label
                            ?: profile.educationSystem.ifBlank { "Not specified" }
                    )
                    ProfileDetailRow(
                        icon = Icons.Default.School,
                        label = "School (Optional)",
                        value = profile.schoolName.ifBlank { "Independent E-Learner" }
                    )
                    val seriesSuffix = profile.studentSeries.takeIf { it.isNotBlank() && it != "ALL" }
                        ?.let { " • Series: $it" }
                        .orEmpty()
                    ProfileDetailRow(
                        icon = Icons.Default.Work,
                        label = "Academic Level, Stream & Series",
                        value = "${StudentLevel.fromCode(profile.studentLevel)?.label ?: profile.studentLevel} • ${StudentStream.fromCode(profile.studentStream)?.label ?: profile.studentStream}$seriesSuffix"
                    )
                    if (profile.selectedSubjectsList.isNotEmpty()) {
                        ProfileDetailRow(
                            icon = Icons.Default.Badge,
                            label = "Selected JSON Subjects (${profile.selectedSubjectsList.size})",
                            value = profile.selectedSubjectsList.joinToString(", ")
                        )
                    }
                }

                PlatformRole.TEACHER -> {
                    ProfileDetailRow(
                        icon = Icons.Default.Work,
                        label = "Years of Teaching & System",
                        value = "${profile.teachingYears.ifBlank { "1" }} years • ${EducationSystem.fromCode(profile.educationSystem)?.label ?: profile.educationSystem}"
                    )
                    if (profile.selectedSubjectsList.isNotEmpty()) {
                        ProfileDetailRow(
                            icon = Icons.Default.Badge,
                            label = "Subjects Taught (${profile.selectedSubjectsList.size})",
                            value = profile.selectedSubjectsList.joinToString(", ")
                        )
                    }
                    if (profile.pseudonym.isNotBlank()) {
                        ProfileDetailRow(
                            icon = Icons.Default.Person,
                            label = "Anonymous Pseudonym",
                            value = profile.pseudonym
                        )
                    }
                    if (profile.teacherSchoolsList.isNotEmpty()) {
                        ProfileDetailRow(
                            icon = Icons.Default.School,
                            label = "Schools Taught",
                            value = profile.teacherSchoolsList.joinToString(", ")
                        )
                    }
                    if (profile.phoneNumbersList.isNotEmpty()) {
                        ProfileDetailRow(
                            icon = Icons.Default.Phone,
                            label = "Contact Phone Numbers",
                            value = profile.phoneNumbersList.joinToString(", ")
                        )
                    }
                }

                PlatformRole.SCHOOL -> {
                    ProfileDetailRow(
                        icon = Icons.Default.School,
                        label = "School Name & Type",
                        value = "${profile.schoolName.ifBlank { "School" }} (${SchoolOwnership.fromCode(profile.schoolOwnership)?.label ?: profile.schoolOwnership})"
                    )
                    if (profile.schoolAge.isNotBlank()) {
                        ProfileDetailRow(
                            icon = Icons.Default.Schedule,
                            label = "School Age",
                            value = profile.schoolAge
                        )
                    }
                    if (profile.schoolEmail.isNotBlank()) {
                        ProfileDetailRow(
                            icon = Icons.Default.Email,
                            label = "School Email",
                            value = profile.schoolEmail
                        )
                    }
                    if (profile.schoolFacebookUrl.isNotBlank()) {
                        ProfileDetailRow(
                            icon = Icons.Default.Language,
                            label = "Facebook Page Link",
                            value = profile.schoolFacebookUrl
                        )
                    }
                    if (profile.phoneNumbersList.isNotEmpty()) {
                        ProfileDetailRow(
                            icon = Icons.Default.Phone,
                            label = "School Phone Numbers",
                            value = profile.phoneNumbersList.joinToString(", ")
                        )
                    }
                }

                PlatformRole.PARENT -> {
                    ProfileDetailRow(
                        icon = Icons.Default.Badge,
                        label = "Linked Child IDs (${profile.childIdsList.size} / ${UserProfile.MAX_CHILD_IDS})",
                        value = profile.childIdsList.joinToString(", ").ifBlank { "None linked" }
                    )
                    if (profile.phoneNumbersList.isNotEmpty()) {
                        ProfileDetailRow(
                            icon = Icons.Default.Phone,
                            label = "Parent Phone Numbers",
                            value = profile.phoneNumbersList.joinToString(", ")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailsDisplayCard(
    profile: UserProfile,
    isEmailVerified: Boolean,
    keepSignedIn: Boolean = true,
    onTogglePersistentSession: (Boolean) -> Unit = {}
) {
    val displayTimestamp = profile.updatedAt ?: profile.createdAt ?: Timestamp.now()
    val formattedDate = remember(displayTimestamp) {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(displayTimestamp.toDate())
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("profile_details_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.section_about),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = profile.bio.ifBlank { "No biography added yet. Tap Edit Profile to introduce yourself." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("profile_bio_text")
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Text(
                text = stringResource(R.string.section_details),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            ProfileDetailRow(
                icon = Icons.Default.Work,
                label = "Role / Title",
                value = profile.roleTitle.ifBlank { "Not specified" }
            )
            ProfileDetailRow(
                icon = Icons.Default.LocationOn,
                label = "Location",
                value = profile.location.ifBlank { "Not specified" }
            )
            ProfileDetailRow(
                icon = Icons.Default.Language,
                label = "Website",
                value = profile.website.ifBlank { "Not specified" }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Text(
                text = stringResource(R.string.section_account_security),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("profile_persistent_session_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.persistent_session_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (keepSignedIn) {
                                "Active — You stay logged in across restarts (Login Once)"
                            } else {
                                "Disabled — You will be prompted to sign in next time"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = keepSignedIn,
                    onCheckedChange = onTogglePersistentSession,
                    modifier = Modifier.testTag("profile_persistent_session_switch")
                )
            }

            ProfileDetailRow(
                icon = Icons.Default.Fingerprint,
                label = "Authenticated UID",
                value = profile.userId
            )
            ProfileDetailRow(
                icon = Icons.Default.Badge,
                label = "Google Verification",
                value = if (isEmailVerified) "Verified Google Account" else "Authenticated Google Session"
            )
            ProfileDetailRow(
                icon = Icons.Default.Schedule,
                label = "Last Cloud Sync",
                value = formattedDate
            )
        }
    }
}

@Composable
private fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileEditorCard(
    draft: ProfileEditorDraft,
    onDraftChange: (
        displayName: String?,
        headline: String?,
        roleTitle: String?,
        bio: String?,
        location: String?,
        website: String?,
        statusMessage: String?
    ) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("profile_editor_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.edit_profile),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = draft.displayName,
                onValueChange = { onDraftChange(it, null, null, null, null, null, null) },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_display_name")
            )

            OutlinedTextField(
                value = draft.headline,
                onValueChange = { onDraftChange(null, it, null, null, null, null, null) },
                label = { Text("Headline / Tagline") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_headline")
            )

            OutlinedTextField(
                value = draft.roleTitle,
                onValueChange = { onDraftChange(null, null, it, null, null, null, null) },
                label = { Text("Role / Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_role_title")
            )

            OutlinedTextField(
                value = draft.statusMessage,
                onValueChange = { onDraftChange(null, null, null, null, null, null, it) },
                label = { Text("Status Badge (e.g. Available, Focusing)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_status_message")
            )

            OutlinedTextField(
                value = draft.location,
                onValueChange = { onDraftChange(null, null, null, null, it, null, null) },
                label = { Text("Location") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_location")
            )

            OutlinedTextField(
                value = draft.website,
                onValueChange = { onDraftChange(null, null, null, null, null, it, null) },
                label = { Text("Website URL") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_website")
            )

            OutlinedTextField(
                value = draft.bio,
                onValueChange = { onDraftChange(null, null, null, it, null, null, null) },
                label = { Text("Biography") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_bio")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !draft.isSaving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("cancel_edit_button")
                ) {
                    Text(stringResource(R.string.cancel_edit))
                }

                Button(
                    onClick = onSave,
                    enabled = !draft.isSaving && draft.displayName.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("save_profile_button")
                ) {
                    if (draft.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.saving_profile))
                    } else {
                        Text(stringResource(R.string.save_profile))
                    }
                }
            }
        }
    }
}

