package com.example

import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AuthMode
import com.example.auth.PersistentSessionManager
import com.example.data.local.AuraOfflineDatabase
import com.example.data.local.CachedUserProfileEntity
import com.example.data.local.OfflineSqliteSyncCoordinator
import com.example.data.local.SqliteOfflineTelemetry
import com.example.data.model.AccountStatus
import com.example.data.model.ActivityEventType
import com.example.data.model.ActivityReviewStatus
import com.example.data.model.ActivitySeverity
import com.example.data.model.AdminCustomSubject
import com.example.data.model.AppActivity
import com.example.data.model.BafendFileEngine
import com.example.data.model.CurriculumCatalog
import com.example.data.model.DatabaseSecurityInspector
import com.example.data.model.EducationSystem
import com.example.data.model.PlatformRole
import com.example.data.model.RevisionBafendDocument
import com.example.data.model.StudentLevel
import com.example.data.model.StudentStream
import com.example.data.model.UserProfile
import com.example.data.remote.GeminiThinkingService
import com.example.data.repository.CurriculumRevisionRepository
import com.example.ui.ActivitiesUiState
import com.example.ui.AdminCurriculumAndPdfSection
import com.example.ui.AdminFilterState
import com.example.ui.AdminPortalContent
import com.example.ui.AdminSectionTab
import com.example.ui.AuthScreen
import com.example.ui.CurriculumSubjectSelectorCard
import com.example.ui.OfflineSqliteSyncBanner
import com.example.ui.PendingAccountLockCard
import com.example.ui.RevisionHubScreen
import com.example.ui.RoleOnboardingScreen
import com.example.ui.RoleOnboardingSubmission
import com.example.ui.StudentHighThinkingOrientationCard
import com.example.ui.StudentOrientationUiState
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aura Profile", appName)
    }

    @Test
    fun `auth screen displays login option and remembered account when account already created`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = PersistentSessionManager(context)
        sessionManager.saveCachedProfile(
            UserProfile(
                userId = "uid_existing_123",
                displayName = "Alice Vance",
                email = "alice@example.com",
                headline = "Principal Cloud Architect",
                roleTitle = "Architect"
            )
        )
        sessionManager.onExplicitSignOut()

        assertTrue(sessionManager.hasCreatedAccount())
        assertTrue(sessionManager.isKeepSignedInEnabled())
        assertEquals(AuthMode.LOGIN, sessionManager.getPreferredAuthMode())
        assertNotNull(sessionManager.getCachedProfile("uid_existing_123"))

        composeTestRule.setContent {
            MyApplicationTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_mode_switcher").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_tab_login").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_tab_signup").assertIsDisplayed()
        composeTestRule.onNodeWithTag("existing_account_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("existing_account_login_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("persistent_session_switch").assertIsDisplayed()

        // Switch to Create Account tab and confirm "Already created an account? Log In" is visible
        composeTestRule.onNodeWithTag("auth_tab_signup").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("google_sign_up_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Already created an account? Log In").assertIsDisplayed()
    }

    @Test
    fun `default admin email policy grants admin only to fokoufdr at gmail com and visitor to others`() {
        val adminProfile = UserProfile(
            userId = "uid_admin",
            displayName = "Fokou Admin",
            email = "fokoufdr@gmail.com"
        )
        val visitorProfile = UserProfile(
            userId = "uid_visitor",
            displayName = "Alice Visitor",
            email = "alice@example.com"
        )

        assertTrue(adminProfile.isDesignatedAdminEmail)
        assertEquals(PlatformRole.ADMIN, adminProfile.effectivePlatformRole)
        assertFalse(adminProfile.requiresRoleOnboarding)

        assertFalse(visitorProfile.isDesignatedAdminEmail)
        assertEquals(PlatformRole.VISITOR, visitorProfile.effectivePlatformRole)
        assertTrue(visitorProfile.requiresRoleOnboarding)
    }

    @Test
    fun `student role onboarding wizard walks through system level stream and sweet welcome animation`() {
        var capturedSubmission: RoleOnboardingSubmission? = null
        val newProfile = UserProfile(
            userId = "uid_student_1",
            displayName = "Jean Paul",
            email = "jean@example.com",
            onboardingCompleted = false
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                RoleOnboardingScreen(
                    profile = newProfile,
                    isSaving = false,
                    canCancel = false,
                    onCompleteOnboarding = { capturedSubmission = it },
                    onCancel = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("role_onboarding_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("role_card_VISITOR").assertIsDisplayed()
        composeTestRule.onNodeWithTag("role_card_STUDENT").assertIsDisplayed()

        // 1. Select Student Role
        composeTestRule.onNodeWithTag("role_card_STUDENT").performClick()
        composeTestRule.waitForIdle()

        // 2. Select Anglophone System
        composeTestRule.onNodeWithTag("student_system_ANGLOPHONE").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // 3. Optional school + Select Advanced Level
        composeTestRule.onNodeWithTag("student_school_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_level_ADVANCED_LEVEL").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 4. Select Science Stream
        composeTestRule.onNodeWithTag("student_stream_SCIENCE").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // 5. Verify sweet welcome animation is displayed and enter platform
        composeTestRule.onNodeWithTag("student_welcome_animation_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_enter_platform_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertNotNull(capturedSubmission)
        assertEquals(PlatformRole.STUDENT, capturedSubmission?.selectedRole)
        assertEquals("ANGLOPHONE", capturedSubmission?.educationSystem)
        assertEquals("ADVANCED_LEVEL", capturedSubmission?.studentLevel)
        assertEquals("SCIENCE", capturedSubmission?.studentStream)
    }

    @Test
    fun `student high thinking orientation button collects activities and displays level revision series schools and careers`() {
        val studentProfile = UserProfile(
            userId = "uid_student_science",
            displayName = "Nadia Nkeng",
            email = "nadia@student.org",
            actorRole = PlatformRole.STUDENT.code,
            onboardingCompleted = true,
            educationSystem = "FRANCOPHONE",
            studentLevel = "ORDINARY_LEVEL",
            studentStream = "SCIENCE",
            schoolName = "Lycée Général Leclerc"
        )
        val collectedActivities = listOf(
            AppActivity(
                activityId = "act_score_1",
                userId = "uid_student_science",
                userDisplayName = "Nadia Nkeng",
                userEmail = "nadia@student.org",
                eventType = ActivityEventType.STUDENT_ASSESSMENT_LOGGED.code,
                title = "Mathematics & Physics — Score: 17/20 (85%)",
                details = "Mastered differential calculus; difficulty with electromagnetic induction."
            ),
            AppActivity(
                activityId = "act_score_2",
                userId = "uid_student_science",
                userDisplayName = "Nadia Nkeng",
                userEmail = "nadia@student.org",
                eventType = ActivityEventType.ROLE_ONBOARDING_COMPLETED.code,
                title = "Completed Student Role Onboarding",
                details = "Actor Role: STUDENT"
            )
        )

        val geminiService = GeminiThinkingService()
        var currentState by mutableStateOf<StudentOrientationUiState>(StudentOrientationUiState.Idle)
        var targetObjective by mutableStateOf("Software Engineering at Polytechnic (ENSPY)")

        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    StudentHighThinkingOrientationCard(
                        profile = studentProfile,
                        userActivities = collectedActivities,
                        targetObjective = targetObjective,
                        orientationState = currentState,
                        onTargetObjectiveChange = { targetObjective = it },
                        onLogAcademicActivity = { _, _, _ -> },
                        onTriggerCollectAndAnalyze = {
                            val analysis = geminiService.buildDeepStudentOrientationSynthesis(
                                profile = studentProfile,
                                activities = collectedActivities,
                                targetObjectives = targetObjective
                            )
                            currentState = StudentOrientationUiState.ResultReady(analysis)
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("student_high_thinking_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("trigger_student_orientation_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify all required student orientation sections are displayed
        composeTestRule.onNodeWithTag("student_orientation_result_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_probability_of_success_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_level_diagnostic_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_understandings_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_difficulties_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_evolution_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_revision_excellence_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_revise_better_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_focus_on_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_reach_excellence_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_school_series_orientation_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_after_ordinary_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_after_advanced_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("student_future_careers_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `admin portal dashboard displays activities for admin and restricts non-admin`() {
        val sampleActivities = listOf(
            AppActivity(
                activityId = "act_1",
                userId = "uid_alice",
                userDisplayName = "Alice Vance",
                userEmail = "alice@example.com",
                eventType = ActivityEventType.PROFILE_UPDATED.code,
                title = "Updated Personal Profile Details",
                details = "Synced bio and headline to Firestore.",
                severity = ActivitySeverity.SUCCESS.code,
                reviewStatus = ActivityReviewStatus.RECORDED.code
            ),
            AppActivity(
                activityId = "act_2",
                userId = "uid_bob",
                userDisplayName = "Bob Chen",
                userEmail = "bob@example.com",
                eventType = ActivityEventType.STUDENT_ORIENTATION_ANALYZED.code,
                title = "High-Thinking Student Level & Career Orientation Completed",
                details = "Collected 5 activities • Success Probability: 88%",
                severity = ActivitySeverity.HIGHLIGHT.code,
                reviewStatus = ActivityReviewStatus.REVIEWED.code
            )
        )
        val sampleUsers = listOf(
            UserProfile(
                userId = "uid_alice",
                displayName = "Alice Vance",
                email = "fokoufdr@gmail.com",
                roleTitle = "Principal Architect"
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                AdminPortalContent(
                    activitiesState = ActivitiesUiState.Success(
                        activities = sampleActivities,
                        isGlobalAdminStream = true
                    ),
                    allUsers = sampleUsers,
                    currentProfile = sampleUsers.first(),
                    isAdmin = true,
                    filterState = AdminFilterState(activeTab = AdminSectionTab.ACTIVITY_STREAM),
                    onSearchQueryChange = {},
                    onEventTypeFilterChange = {},
                    onReviewStatusFilterChange = {},
                    onLogManualAudit = {},
                    onUpdateActivityReview = { _, _ -> },
                    onDeleteActivity = {},
                    onDismissFeedback = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_portal_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_hero_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kpi_total_events").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Updated Personal Profile Details").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("High-Thinking Student Level & Career Orientation Completed").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `admin user management tab displays active students chart, entries and exits chart, role changer to admin, pending or active toggle, and delete user`() {
        var updatedRoleForBob: PlatformRole? = null
        var updatedStatusForBob: AccountStatus? = null
        var deletedUserId: String? = null

        val adminUser = UserProfile(
            userId = "uid_admin",
            displayName = "Platform Admin",
            email = "fokoufdr@gmail.com",
            actorRole = PlatformRole.ADMIN.code,
            accountStatus = AccountStatus.ACTIVE.code,
            onboardingCompleted = true
        )
        val studentUser = UserProfile(
            userId = "uid_bob",
            displayName = "Bob Student",
            email = "bob@student.edu",
            location = "Buea Molyko",
            bio = "Preparing for Advanced Level Physics and Mathematics",
            schoolName = "BGS Molyko",
            phoneNumbers = "+237670001122",
            actorRole = PlatformRole.STUDENT.code,
            accountStatus = AccountStatus.ACTIVE.code,
            onboardingCompleted = true
        )
        // Verify each letter/keyword typed searches across all personal info fields
        assertTrue(studentUser.matchesPersonalInfoKeyword("b"))
        assertTrue(studentUser.matchesPersonalInfoKeyword("molyko"))
        assertTrue(studentUser.matchesPersonalInfoKeyword("physics"))
        assertTrue(studentUser.matchesPersonalInfoKeyword("67000"))
        assertFalse(studentUser.matchesPersonalInfoKeyword("nonexistent_xyz"))

        val sampleActivities = listOf(
            AppActivity(
                activityId = "act_entry_1",
                userId = "uid_bob",
                userDisplayName = "Bob Student",
                userEmail = "bob@student.edu",
                eventType = ActivityEventType.AUTH_SIGN_IN.code,
                title = "Authenticated Session Started (Entry)",
                details = "Signed in via Google Identity"
            ),
            AppActivity(
                activityId = "act_exit_1",
                userId = "uid_bob",
                userDisplayName = "Bob Student",
                userEmail = "bob@student.edu",
                eventType = ActivityEventType.AUTH_SIGN_OUT.code,
                title = "User Signed Out (Exit)",
                details = "Session closed"
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                AdminPortalContent(
                    activitiesState = ActivitiesUiState.Success(
                        activities = sampleActivities,
                        isGlobalAdminStream = true
                    ),
                    allUsers = listOf(adminUser, studentUser),
                    currentProfile = adminUser,
                    isAdmin = true,
                    filterState = AdminFilterState(activeTab = AdminSectionTab.MANAGE_USERS),
                    onSearchQueryChange = {},
                    onEventTypeFilterChange = {},
                    onReviewStatusFilterChange = {},
                    onLogManualAudit = {},
                    onUpdateActivityReview = { _, _ -> },
                    onDeleteActivity = {},
                    onDismissFeedback = {},
                    onChangeUserRole = { user, role ->
                        if (user.userId == "uid_bob") updatedRoleForBob = role
                    },
                    onChangeUserStatus = { user, status ->
                        if (user.userId == "uid_bob") updatedStatusForBob = status
                    },
                    onDeleteUser = { user ->
                        deletedUserId = user.userId
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        // Verify Active Students Over Time chart & Entries/Exits Over Time chart with percentages
        composeTestRule.onNodeWithTag("chart_active_students_over_time").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("chart_entries_exits_over_time").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("entries_percentage_text").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("exits_percentage_text").performScrollTo().assertIsDisplayed()

        // Verify Users Directory Table & Bob's table row with "Details" button beside it
        composeTestRule.onNodeWithTag("admin_users_table").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_table_row_uid_bob").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_details_button_uid_bob").performScrollTo().assertIsDisplayed()

        // Click "Details" button beside Bob's row to open the dedicated Admin User Detail & Actions Page
        composeTestRule.onNodeWithTag("user_details_button_uid_bob").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify dedicated User Detail Page is displayed with all admin actions
        composeTestRule.onNodeWithTag("admin_user_detail_page").assertExists()
        composeTestRule.onNodeWithTag("back_to_users_table_button").performScrollTo().assertIsDisplayed()

        // Verify changing Bob's role to ADMIN on the detail page
        composeTestRule.onNodeWithTag("change_role_uid_bob_ADMIN").performScrollTo().performClick()
        assertEquals(PlatformRole.ADMIN, updatedRoleForBob)

        // Verify setting Bob to PENDING, SUSPENDED, and BLOCKED on the detail page
        composeTestRule.onNodeWithTag("set_user_pending_uid_bob").performScrollTo().performClick()
        assertEquals(AccountStatus.PENDING, updatedStatusForBob)

        composeTestRule.onNodeWithTag("set_user_suspended_uid_bob").performScrollTo().performClick()
        assertEquals(AccountStatus.SUSPENDED, updatedStatusForBob)

        composeTestRule.onNodeWithTag("set_user_blocked_uid_bob").performScrollTo().performClick()
        assertEquals(AccountStatus.BLOCKED, updatedStatusForBob)

        // Verify deleting Bob returns to the Users Table
        composeTestRule.onNodeWithTag("delete_user_uid_bob").performScrollTo().performClick()
        assertEquals("uid_bob", deletedUserId)
    }

    @Test
    fun `pending user profile is locked from navigation and database security logs display IP attempts and blocked injections`() {
        val defaultActiveUser = UserProfile(
            userId = "uid_student_1",
            displayName = "Claire Ngu",
            email = "claire@example.com"
        )
        assertEquals(AccountStatus.ACTIVE, defaultActiveUser.effectiveAccountStatus)
        assertFalse(defaultActiveUser.isPendingAccount)

        val pendingUser = defaultActiveUser.copy(accountStatus = AccountStatus.PENDING.code)
        assertEquals(AccountStatus.PENDING, pendingUser.effectiveAccountStatus)
        assertTrue(pendingUser.isPendingAccount)

        // Verify DatabaseSecurityInspector detects SQL & NoSQL injections targeting direct database contact
        assertTrue(DatabaseSecurityInspector.containsInjectionSignature("""{"${'$'}where": "1 == 1"}"""))
        assertTrue(DatabaseSecurityInspector.containsInjectionSignature("""' OR '1'='1' -- DROP TABLE users"""))
        assertFalse(DatabaseSecurityInspector.containsInjectionSignature("Normal student bio and school name"))

        composeTestRule.setContent {
            MyApplicationTheme {
                PendingAccountLockCard(
                    profile = pendingUser,
                    onSignOutClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pending_account_lock_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("STATUS: PENDING APPROVAL").assertIsDisplayed()
    }

    @Test
    fun `curriculum subject selector card loads subjects from json and allows user subject selection and custom json import`() {
        val initialProfile = UserProfile(
            userId = "uid_student_json",
            displayName = "Emmanuel Tchoua",
            email = "emmanuel@student.org",
            actorRole = PlatformRole.STUDENT.code,
            educationSystem = EducationSystem.ANGLOPHONE.code,
            studentLevel = StudentLevel.ADVANCED_LEVEL.code,
            studentStream = StudentStream.SCIENCE.code,
            onboardingCompleted = true
        )

        var savedSeries = ""
        var savedSubjects = emptyList<String>()

        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    CurriculumSubjectSelectorCard(
                        profile = initialProfile,
                        onSaveCurriculumSelection = { _, _, _, series, subjects ->
                            savedSeries = series
                            savedSubjects = subjects
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("curriculum_subject_selector_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_selected_subjects_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue(savedSeries.isNotBlank())
        assertTrue(savedSubjects.isNotEmpty())
    }

    @Test
    fun `admin can add subject under subsystem and upload pdf assigned bafend extension and users can browse download offline and open in adaptive bafend file reader`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify .bafend extension assignment and exclusive validation
        val assignedName = BafendFileEngine.ensureBafendExtension("0770_Pure_Mathematics_Notes.pdf")
        assertEquals("0770_Pure_Mathematics_Notes.bafend", assignedName)
        assertTrue(BafendFileEngine.isValidBafendFileName(assignedName))
        assertFalse(BafendFileEngine.isValidBafendFileName("0770_Pure_Mathematics_Notes.pdf"))

        // 2. Verify adaptive latency profile across compact phones & tablets / iOS & Android
        val compactProfile = BafendFileEngine.computeAdaptiveLatencyProfile(
            screenWidthDp = 360,
            screenHeightDp = 740,
            densityDpi = 320
        )
        val tabletProfile = BafendFileEngine.computeAdaptiveLatencyProfile(
            screenWidthDp = 900,
            screenHeightDp = 1280,
            densityDpi = 320
        )
        assertTrue(compactProfile.targetPageWidthPx in 540..1680)
        assertTrue(tabletProfile.targetPageWidthPx >= compactProfile.targetPageWidthPx)

        // 3. Verify offline folder creation on the user's machine and .bafend container round-trip
        val sampleDoc = CurriculumRevisionRepository.DEFAULT_SEEDED_BAFEND_DOCUMENTS.first()
        val offlineResult = BafendFileEngine.saveBafendToOfflineFolder(context, sampleDoc)
        assertTrue(offlineResult.fileName.endsWith(".bafend"))
        assertTrue(offlineResult.verifiedBafendSignature)
        assertTrue(BafendFileEngine.isDocumentDownloadedOffline(context, sampleDoc))

        // 4. Verify Admin Curriculum & PDF Studio combined with Revision Hub & .bafend File Reader
        var activeDoc by mutableStateOf<RevisionBafendDocument?>(null)
        var downloadedDocId by mutableStateOf("")

        composeTestRule.setContent {
            MyApplicationTheme {
                RevisionHubScreen(
                    currentProfile = UserProfile(
                        userId = "uid_student_rev",
                        displayName = "Nadia Kemajou",
                        email = "nadia@student.org",
                        actorRole = PlatformRole.STUDENT.code,
                        onboardingCompleted = true
                    ),
                    isAdmin = true,
                    revisionDocuments = CurriculumRevisionRepository.DEFAULT_SEEDED_BAFEND_DOCUMENTS,
                    adminCustomSubjects = listOf(
                        AdminCustomSubject(
                            subjectId = "custom_robotics",
                            code = "0799",
                            name = "Robotics & Embedded Systems",
                            coefficient = 5,
                            category = "Principal",
                            systemCode = EducationSystem.ANGLOPHONE.code,
                            levelCode = StudentLevel.ADVANCED_LEVEL.code,
                            streamCode = StudentStream.SCIENCE.code,
                            seriesCode = "S1",
                            seriesName = "Series S1"
                        )
                    ),
                    activeReaderDocument = activeDoc,
                    statusBannerMessage = null,
                    onDismissBanner = {},
                    onOpenBafendReader = { doc -> activeDoc = doc },
                    onCloseBafendReader = { activeDoc = null },
                    onDownloadBafendOffline = { doc -> downloadedDocId = doc.docId },
                    onOpenAdminCurriculumStudio = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("revision_hub_screen").assertIsDisplayed()

        // Click Download (.bafend) on the first seeded document
        composeTestRule.onNodeWithTag("download_bafend_button_${sampleDoc.docId}").performScrollTo().performClick()
        assertEquals(sampleDoc.docId, downloadedDocId)

        // Click Open .bafend Reader on the first seeded document
        composeTestRule.onNodeWithTag("open_bafend_reader_button_${sampleDoc.docId}").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify the dedicated .bafend File Reader opens and renders the document as a PDF reader
        composeTestRule.onNodeWithTag("bafend_file_reader_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bafend_adaptive_latency_banner").assertIsDisplayed()
    }

    @Test
    fun `sqlite phone memory caches online viewed data and queues offline actions for firebase synchronization`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inMemoryDb = Room.inMemoryDatabaseBuilder(
            context,
            AuraOfflineDatabase::class.java
        ).allowMainThreadQueries().build()
        val dao = inMemoryDb.offlineDao()

        try {
            // 1. Simulate viewing/loading profile, activities, subjects, and .bafend docs when online -> stored in SQLite
            val onlineProfile = UserProfile(
                userId = "uid_sqlite_student",
                displayName = "Samuel Fotso",
                email = "samuel@student.org",
                actorRole = PlatformRole.STUDENT.code,
                educationSystem = EducationSystem.ANGLOPHONE.code,
                studentLevel = StudentLevel.ADVANCED_LEVEL.code,
                studentStream = StudentStream.SCIENCE.code,
                studentSeries = "S1",
                selectedSubjects = "0770 - Pure Mathematics With Mechanics\n0780 - Physics",
                onboardingCompleted = true
            )
            dao.upsertUserProfile(CachedUserProfileEntity.fromDomainModel(onlineProfile, isPendingOfflineSync = false))

            val loadedFromSqlite = dao.getUserProfileById("uid_sqlite_student")?.toDomainModel()
            assertNotNull(loadedFromSqlite)
            assertEquals("Samuel Fotso", loadedFromSqlite?.displayName)
            assertEquals("S1", loadedFromSqlite?.studentSeries)

            // 2. Simulate performing an offline profile update -> saved to SQLite & queued in offline_sync_outbox
            val offlineUpdatedProfile = onlineProfile.copy(
                headline = "Updated Offline in Phone SQLite",
                selectedSubjects = "0770 - Pure Mathematics With Mechanics\n0780 - Physics\n0715 - Chemistry"
            )
            dao.upsertUserProfile(CachedUserProfileEntity.fromDomainModel(offlineUpdatedProfile, isPendingOfflineSync = true))
            dao.enqueueSyncAction(
                com.example.data.local.OfflineSyncActionEntity(
                    actionType = com.example.data.local.OfflineSyncActionType.UPSERT_USER_PROFILE,
                    targetCollection = "users",
                    targetDocumentId = offlineUpdatedProfile.userId,
                    summaryLabel = "Sync Profile: Samuel Fotso",
                    payloadJson = OfflineSqliteSyncCoordinator.serializeUserProfileToJson(offlineUpdatedProfile)
                )
            )

            val pendingOutbox = dao.getPendingSyncActions()
            assertEquals(1, pendingOutbox.size)
            assertEquals("users", pendingOutbox.first().targetCollection)

            // 3. Verify OfflineSqliteSyncBanner UI displays SQLite telemetry, queued actions, and sync trigger
            var syncClicked = false
            var offlineToggledTo: Boolean? = null

            composeTestRule.setContent {
                MyApplicationTheme {
                    OfflineSqliteSyncBanner(
                        telemetry = SqliteOfflineTelemetry(
                            isNetworkAvailable = true,
                            isManualOfflineMode = true,
                            cachedProfilesCount = 1,
                            cachedActivitiesCount = 3,
                            cachedSubjectsCount = 2,
                            cachedRevisionDocsCount = 4,
                            pendingActions = pendingOutbox
                        ),
                        onToggleManualOfflineMode = { enabled -> offlineToggledTo = enabled },
                        onTriggerManualFirebaseSync = { syncClicked = true }
                    )
                }
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("sqlite_offline_sync_banner").assertIsDisplayed()
            composeTestRule.onNodeWithTag("sqlite_pending_outbox_list").assertIsDisplayed()
            composeTestRule.onNodeWithTag("sync_sqlite_to_firebase_button").performClick()
            assertTrue(syncClicked)
            composeTestRule.onNodeWithTag("toggle_sqlite_offline_mode_button").performClick()
            assertEquals(false, offlineToggledTo)
        } finally {
            inMemoryDb.close()
        }
    }
}
