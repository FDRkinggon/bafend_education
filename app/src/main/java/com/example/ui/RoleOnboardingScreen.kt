package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.CurriculumCatalog
import com.example.data.model.EducationSystem
import com.example.data.model.PlatformRole
import com.example.data.model.SchoolOwnership
import com.example.data.model.StudentLevel
import com.example.data.model.StudentStream
import com.example.data.model.UserProfile
import kotlin.math.cos
import kotlin.math.sin

data class RoleOnboardingSubmission(
    val selectedRole: PlatformRole,
    val educationSystem: String = "",
    val studentLevel: String = "",
    val studentStream: String = "",
    val studentSeries: String = "",
    val selectedSubjects: List<String> = emptyList(),
    val schoolName: String = "",
    val schoolOwnership: String = "",
    val schoolAge: String = "",
    val schoolEmail: String = "",
    val schoolFacebookUrl: String = "",
    val teachingYears: String = "",
    val teacherSchools: List<String> = emptyList(),
    val pseudonym: String = "",
    val phoneNumbers: List<String> = emptyList(),
    val childIds: List<String> = emptyList()
)

private enum class StudentWizardStep {
    EDUCATION_SYSTEM,
    SCHOOL_AND_LEVEL,
    STREAM,
    WELCOME_ANIMATION
}

private enum class SchoolWizardStep {
    OWNERSHIP_TYPE,
    QUESTIONNAIRE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoleOnboardingScreen(
    profile: UserProfile,
    isSaving: Boolean,
    canCancel: Boolean,
    onCompleteOnboarding: (RoleOnboardingSubmission) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeRole by rememberSaveable { mutableStateOf<PlatformRole?>(null) }

    // Student Wizard State
    var studentStep by rememberSaveable { mutableStateOf(StudentWizardStep.EDUCATION_SYSTEM) }
    var studentSystem by rememberSaveable {
        mutableStateOf(
            profile.educationSystem.takeIf {
                it == EducationSystem.ANGLOPHONE.code || it == EducationSystem.FRANCOPHONE.code
            }.orEmpty()
        )
    }
    var studentSchoolInput by rememberSaveable { mutableStateOf(profile.schoolName) }
    var studentLevel by rememberSaveable { mutableStateOf(profile.studentLevel) }
    var studentStream by rememberSaveable { mutableStateOf(profile.studentStream) }
    var studentSeries by rememberSaveable { mutableStateOf(profile.studentSeries) }
    val studentSelectedSubjects = remember {
        mutableStateListOf<String>().apply { addAll(profile.selectedSubjectsList) }
    }

    // Teacher Wizard State
    var teacherYears by rememberSaveable { mutableStateOf(profile.teachingYears) }
    var teacherSystem by rememberSaveable {
        mutableStateOf(profile.educationSystem.ifBlank { EducationSystem.ANGLOPHONE.code })
    }
    val teacherSelectedSubjects = remember {
        mutableStateListOf<String>().apply { addAll(profile.selectedSubjectsList) }
    }
    var teacherPseudonym by rememberSaveable { mutableStateOf(profile.pseudonym) }
    var teacherSchoolDraft by rememberSaveable { mutableStateOf("") }
    val teacherSchoolsList = remember {
        mutableStateListOf<String>().apply { addAll(profile.teacherSchoolsList) }
    }
    var teacherPhoneDraft by rememberSaveable { mutableStateOf("") }
    val teacherPhonesList = remember {
        mutableStateListOf<String>().apply { addAll(profile.phoneNumbersList) }
    }

    // School Wizard State
    var schoolStep by rememberSaveable {
        mutableStateOf(
            if (profile.schoolOwnership.isNotBlank()) {
                SchoolWizardStep.QUESTIONNAIRE
            } else {
                SchoolWizardStep.OWNERSHIP_TYPE
            }
        )
    }
    var schoolOwnership by rememberSaveable { mutableStateOf(profile.schoolOwnership) }
    var schoolName by rememberSaveable { mutableStateOf(profile.schoolName) }
    var schoolEmail by rememberSaveable { mutableStateOf(profile.schoolEmail) }
    var schoolFacebook by rememberSaveable { mutableStateOf(profile.schoolFacebookUrl) }
    var schoolAge by rememberSaveable { mutableStateOf(profile.schoolAge) }
    var schoolPhoneDraft by rememberSaveable { mutableStateOf("") }
    val schoolPhonesList = remember {
        mutableStateListOf<String>().apply { addAll(profile.phoneNumbersList) }
    }

    // Parent Wizard State
    var parentPhoneDraft by rememberSaveable { mutableStateOf("") }
    val parentPhonesList = remember {
        mutableStateListOf<String>().apply { addAll(profile.phoneNumbersList) }
    }
    var parentChildIdDraft by rememberSaveable { mutableStateOf("") }
    val parentChildIdsList = remember {
        mutableStateListOf<String>().apply { addAll(profile.childIdsList) }
    }

    BackHandler(enabled = activeRole != null || canCancel) {
        when {
            activeRole == PlatformRole.STUDENT && studentStep != StudentWizardStep.EDUCATION_SYSTEM -> {
                studentStep = when (studentStep) {
                    StudentWizardStep.WELCOME_ANIMATION -> StudentWizardStep.STREAM
                    StudentWizardStep.STREAM -> StudentWizardStep.SCHOOL_AND_LEVEL
                    StudentWizardStep.SCHOOL_AND_LEVEL -> StudentWizardStep.EDUCATION_SYSTEM
                    StudentWizardStep.EDUCATION_SYSTEM -> StudentWizardStep.EDUCATION_SYSTEM
                }
            }
            activeRole == PlatformRole.SCHOOL && schoolStep == SchoolWizardStep.QUESTIONNAIRE -> {
                schoolStep = SchoolWizardStep.OWNERSHIP_TYPE
            }
            activeRole != null -> {
                activeRole = null
            }
            canCancel -> {
                onCancel()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("role_onboarding_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner Card showing Default Actor Policy & Current Progress
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (profile.isDesignatedAdminEmail) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("default_actor_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (profile.isDesignatedAdminEmail) {
                                        Icons.Default.AdminPanelSettings
                                    } else {
                                        Icons.Default.Badge
                                    },
                                    contentDescription = "Default Actor Role",
                                    tint = if (profile.isDesignatedAdminEmail) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (profile.isDesignatedAdminEmail) {
                                        "DEFAULT ACTOR: ADMIN (fokoufdr@gmail.com)"
                                    } else {
                                        "DEFAULT ACTOR: VISITOR"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (profile.isDesignatedAdminEmail) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }

                        if (activeRole != null) {
                            OutlinedButton(
                                onClick = { activeRole = null },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("change_selected_role_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Roles",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("All Roles", style = MaterialTheme.typography.labelMedium)
                            }
                        } else if (canCancel) {
                            IconButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("close_role_onboarding_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close role selector"
                                )
                            }
                        }
                    }

                    Text(
                        text = when (activeRole) {
                            null -> "Choose Your Role to Continue"
                            PlatformRole.VISITOR -> "Visitor Access Selected"
                            PlatformRole.STUDENT -> "Student E-Learning Setup"
                            PlatformRole.TEACHER -> "Teacher Profile Questionnaire"
                            PlatformRole.SCHOOL -> "School Institution Onboarding"
                            PlatformRole.PARENT -> "Parent & Child ID Linking"
                            PlatformRole.ADMIN -> "Administrator Configuration"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (profile.isDesignatedAdminEmail) {
                            "Signed in as ${profile.email} — You have exclusive Admin Portal access. Select your active platform role below."
                        } else {
                            "Welcome, ${profile.displayName}! Select whether you are a Visitor, Student, Teacher, School, or Parent."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedContent(
            targetState = activeRole,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "role_wizard_transition"
        ) { currentRole ->
            when (currentRole) {
                null -> {
                    RoleSelectionGrid(
                        isSaving = isSaving,
                        onSelectVisitor = {
                            // If visitor selected, return the final interface immediately
                            onCompleteOnboarding(
                                RoleOnboardingSubmission(
                                    selectedRole = PlatformRole.VISITOR
                                )
                            )
                        },
                        onSelectRole = { role ->
                            if (role == PlatformRole.VISITOR) {
                                onCompleteOnboarding(
                                    RoleOnboardingSubmission(
                                        selectedRole = PlatformRole.VISITOR
                                    )
                                )
                            } else {
                                if (role == PlatformRole.STUDENT) {
                                    studentStep = StudentWizardStep.EDUCATION_SYSTEM
                                }
                                if (role == PlatformRole.SCHOOL) {
                                    schoolStep = SchoolWizardStep.OWNERSHIP_TYPE
                                }
                                activeRole = role
                            }
                        }
                    )
                }

                PlatformRole.STUDENT -> {
                    StudentOnboardingWizard(
                        step = studentStep,
                        selectedSystem = studentSystem,
                        schoolInput = studentSchoolInput,
                        selectedLevel = studentLevel,
                        selectedStream = studentStream,
                        selectedSeries = studentSeries,
                        selectedSubjects = studentSelectedSubjects,
                        studentName = profile.displayName,
                        isSaving = isSaving,
                        onSelectSystem = { sys ->
                            studentSystem = sys.code
                            studentStep = StudentWizardStep.SCHOOL_AND_LEVEL
                        },
                        onSchoolInputChange = { studentSchoolInput = it },
                        onSelectLevel = { level ->
                            studentLevel = level.code
                            studentStep = StudentWizardStep.STREAM
                        },
                        onSelectStream = { stream ->
                            studentStream = stream.code
                            val seriesForStream = CurriculumCatalog.getAllSeries(
                                systemCode = studentSystem,
                                levelCode = studentLevel,
                                streamCode = stream.code
                            )
                            val firstSeries = seriesForStream.firstOrNull()?.code.orEmpty()
                            studentSeries = firstSeries
                            studentSelectedSubjects.clear()
                            studentSelectedSubjects.addAll(
                                CurriculumCatalog.getDefaultSubjectLabelsFor(
                                    systemCode = studentSystem,
                                    levelCode = studentLevel,
                                    streamCode = stream.code,
                                    seriesCode = firstSeries.ifBlank { "ALL" }
                                )
                            )
                            studentStep = StudentWizardStep.WELCOME_ANIMATION
                        },
                        onSelectSeries = { newSeriesCode ->
                            studentSeries = newSeriesCode
                            studentSelectedSubjects.clear()
                            studentSelectedSubjects.addAll(
                                CurriculumCatalog.getDefaultSubjectLabelsFor(
                                    systemCode = studentSystem,
                                    levelCode = studentLevel,
                                    streamCode = studentStream,
                                    seriesCode = newSeriesCode.ifBlank { "ALL" }
                                )
                            )
                        },
                        onToggleSubject = { subjectLabel ->
                            val idx = studentSelectedSubjects.indexOfFirst {
                                it.equals(subjectLabel, ignoreCase = true)
                            }
                            if (idx >= 0) {
                                studentSelectedSubjects.removeAt(idx)
                            } else {
                                studentSelectedSubjects.add(subjectLabel)
                            }
                        },
                        onStepBack = {
                            studentStep = when (studentStep) {
                                StudentWizardStep.EDUCATION_SYSTEM -> {
                                    activeRole = null
                                    StudentWizardStep.EDUCATION_SYSTEM
                                }
                                StudentWizardStep.SCHOOL_AND_LEVEL -> StudentWizardStep.EDUCATION_SYSTEM
                                StudentWizardStep.STREAM -> StudentWizardStep.SCHOOL_AND_LEVEL
                                StudentWizardStep.WELCOME_ANIMATION -> StudentWizardStep.STREAM
                            }
                        },
                        onFinishStudentOnboarding = {
                            val finalSubjects = if (studentSelectedSubjects.isEmpty()) {
                                CurriculumCatalog.getDefaultSubjectLabelsFor(
                                    systemCode = studentSystem,
                                    levelCode = studentLevel,
                                    streamCode = studentStream,
                                    seriesCode = studentSeries.ifBlank { "ALL" }
                                )
                            } else {
                                studentSelectedSubjects.toList()
                            }
                            onCompleteOnboarding(
                                RoleOnboardingSubmission(
                                    selectedRole = PlatformRole.STUDENT,
                                    educationSystem = studentSystem,
                                    schoolName = studentSchoolInput.trim(),
                                    studentLevel = studentLevel,
                                    studentStream = studentStream,
                                    studentSeries = studentSeries,
                                    selectedSubjects = finalSubjects
                                )
                            )
                        }
                    )
                }

                PlatformRole.TEACHER -> {
                    TeacherOnboardingForm(
                        teachingYears = teacherYears,
                        onTeachingYearsChange = { teacherYears = it },
                        selectedSystem = teacherSystem,
                        onSelectSystem = { teacherSystem = it.code },
                        selectedSubjects = teacherSelectedSubjects,
                        onToggleSubject = { subjectLabel ->
                            val idx = teacherSelectedSubjects.indexOfFirst {
                                it.equals(subjectLabel, ignoreCase = true)
                            }
                            if (idx >= 0) {
                                teacherSelectedSubjects.removeAt(idx)
                            } else {
                                teacherSelectedSubjects.add(subjectLabel)
                            }
                        },
                        pseudonym = teacherPseudonym,
                        onPseudonymChange = { teacherPseudonym = it },
                        schoolDraft = teacherSchoolDraft,
                        onSchoolDraftChange = { teacherSchoolDraft = it },
                        schoolsList = teacherSchoolsList,
                        onAddSchool = {
                            val trimmed = teacherSchoolDraft.trim()
                            if (trimmed.isNotEmpty() && !teacherSchoolsList.contains(trimmed)) {
                                teacherSchoolsList.add(trimmed)
                                teacherSchoolDraft = ""
                            }
                        },
                        onRemoveSchool = { teacherSchoolsList.remove(it) },
                        phoneDraft = teacherPhoneDraft,
                        onPhoneDraftChange = { teacherPhoneDraft = it },
                        phonesList = teacherPhonesList,
                        onAddPhone = {
                            val trimmed = teacherPhoneDraft.trim()
                            if (trimmed.isNotEmpty() && !teacherPhonesList.contains(trimmed)) {
                                teacherPhonesList.add(trimmed)
                                teacherPhoneDraft = ""
                            }
                        },
                        onRemovePhone = { teacherPhonesList.remove(it) },
                        isSaving = isSaving,
                        onSubmit = {
                            val finalSchools = buildList {
                                addAll(teacherSchoolsList)
                                val pendingSchool = teacherSchoolDraft.trim()
                                if (pendingSchool.isNotEmpty() && !contains(pendingSchool)) {
                                    add(pendingSchool)
                                }
                            }
                            val finalPhones = buildList {
                                addAll(teacherPhonesList)
                                val pendingPhone = teacherPhoneDraft.trim()
                                if (pendingPhone.isNotEmpty() && !contains(pendingPhone)) {
                                    add(pendingPhone)
                                }
                            }
                            onCompleteOnboarding(
                                RoleOnboardingSubmission(
                                    selectedRole = PlatformRole.TEACHER,
                                    teachingYears = teacherYears.trim().ifEmpty { "1" },
                                    educationSystem = teacherSystem.ifBlank { EducationSystem.ANGLOPHONE.code },
                                    selectedSubjects = teacherSelectedSubjects.toList(),
                                    teacherSchools = finalSchools,
                                    pseudonym = teacherPseudonym.trim(),
                                    phoneNumbers = finalPhones
                                )
                            )
                        }
                    )
                }

                PlatformRole.SCHOOL -> {
                    SchoolOnboardingWizard(
                        step = schoolStep,
                        selectedOwnership = schoolOwnership,
                        schoolName = schoolName,
                        onSchoolNameChange = { schoolName = it },
                        schoolEmail = schoolEmail,
                        onSchoolEmailChange = { schoolEmail = it },
                        schoolFacebook = schoolFacebook,
                        onSchoolFacebookChange = { schoolFacebook = it },
                        schoolAge = schoolAge,
                        onSchoolAgeChange = { schoolAge = it },
                        phoneDraft = schoolPhoneDraft,
                        onPhoneDraftChange = { schoolPhoneDraft = it },
                        phonesList = schoolPhonesList,
                        onAddPhone = {
                            val trimmed = schoolPhoneDraft.trim()
                            if (trimmed.isNotEmpty() && !schoolPhonesList.contains(trimmed)) {
                                schoolPhonesList.add(trimmed)
                                schoolPhoneDraft = ""
                            }
                        },
                        onRemovePhone = { schoolPhonesList.remove(it) },
                        onSelectOwnership = { ownership ->
                            schoolOwnership = ownership.code
                            schoolStep = SchoolWizardStep.QUESTIONNAIRE
                        },
                        onChangeOwnershipStep = {
                            schoolStep = SchoolWizardStep.OWNERSHIP_TYPE
                        },
                        isSaving = isSaving,
                        onSubmit = {
                            val finalPhones = buildList {
                                addAll(schoolPhonesList)
                                val pendingPhone = schoolPhoneDraft.trim()
                                if (pendingPhone.isNotEmpty() && !contains(pendingPhone)) {
                                    add(pendingPhone)
                                }
                            }
                            onCompleteOnboarding(
                                RoleOnboardingSubmission(
                                    selectedRole = PlatformRole.SCHOOL,
                                    schoolOwnership = schoolOwnership.ifBlank { SchoolOwnership.PRIVATE.code },
                                    schoolName = schoolName.trim().ifEmpty { "Registered School" },
                                    phoneNumbers = finalPhones,
                                    schoolEmail = schoolEmail.trim(),
                                    schoolFacebookUrl = schoolFacebook.trim(),
                                    schoolAge = schoolAge.trim()
                                )
                            )
                        }
                    )
                }

                PlatformRole.PARENT -> {
                    ParentOnboardingForm(
                        phoneDraft = parentPhoneDraft,
                        onPhoneDraftChange = { parentPhoneDraft = it },
                        phonesList = parentPhonesList,
                        onAddPhone = {
                            val trimmed = parentPhoneDraft.trim()
                            if (trimmed.isNotEmpty() && !parentPhonesList.contains(trimmed)) {
                                parentPhonesList.add(trimmed)
                                parentPhoneDraft = ""
                            }
                        },
                        onRemovePhone = { parentPhonesList.remove(it) },
                        childIdDraft = parentChildIdDraft,
                        onChildIdDraftChange = { parentChildIdDraft = it },
                        childIdsList = parentChildIdsList,
                        onAddChildId = {
                            val trimmed = parentChildIdDraft.trim()
                            if (trimmed.isNotEmpty() &&
                                parentChildIdsList.size < UserProfile.MAX_CHILD_IDS &&
                                !parentChildIdsList.contains(trimmed)
                            ) {
                                parentChildIdsList.add(trimmed)
                                parentChildIdDraft = ""
                            }
                        },
                        onRemoveChildId = { parentChildIdsList.remove(it) },
                        isSaving = isSaving,
                        onSubmit = {
                            val finalPhones = buildList {
                                addAll(parentPhonesList)
                                val pendingPhone = parentPhoneDraft.trim()
                                if (pendingPhone.isNotEmpty() && !contains(pendingPhone)) {
                                    add(pendingPhone)
                                }
                            }
                            val finalChildren = buildList {
                                addAll(parentChildIdsList)
                                val pendingChild = parentChildIdDraft.trim()
                                if (pendingChild.isNotEmpty() &&
                                    size < UserProfile.MAX_CHILD_IDS &&
                                    !contains(pendingChild)
                                ) {
                                    add(pendingChild)
                                }
                            }.take(UserProfile.MAX_CHILD_IDS)

                            onCompleteOnboarding(
                                RoleOnboardingSubmission(
                                    selectedRole = PlatformRole.PARENT,
                                    phoneNumbers = finalPhones,
                                    childIds = finalChildren
                                )
                            )
                        }
                    )
                }

                PlatformRole.VISITOR, PlatformRole.ADMIN -> {
                    // Handled immediately on click
                }
            }
        }
    }
}

@Composable
private fun RoleSelectionGrid(
    isSaving: Boolean,
    onSelectVisitor: () -> Unit,
    onSelectRole: (PlatformRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("role_selection_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RoleOptionCard(
            role = PlatformRole.VISITOR,
            title = "Visitor",
            subtitle = "Explore the final interface immediately without an academic questionnaire.",
            badgeText = "Instant Access • Default Actor",
            icon = Icons.Default.Explore,
            accentColor = MaterialTheme.colorScheme.secondary,
            enabled = !isSaving,
            onClick = onSelectVisitor
        )

        RoleOptionCard(
            role = PlatformRole.STUDENT,
            title = "Student",
            subtitle = "Choose Anglophone or Francophone system, school (optional), Ordinary/Advanced level, and Science or Art.",
            badgeText = "E-Learning Platform",
            icon = Icons.Default.School,
            accentColor = MaterialTheme.colorScheme.primary,
            enabled = !isSaving,
            onClick = { onSelectRole(PlatformRole.STUDENT) }
        )

        RoleOptionCard(
            role = PlatformRole.TEACHER,
            title = "Teacher",
            subtitle = "Share your years of teaching, system taught, schools (optional), pseudonym (optional), and phone numbers (optional).",
            badgeText = "Educator Hub",
            icon = Icons.Default.HistoryEdu,
            accentColor = MaterialTheme.colorScheme.tertiary,
            enabled = !isSaving,
            onClick = { onSelectRole(PlatformRole.TEACHER) }
        )

        RoleOptionCard(
            role = PlatformRole.SCHOOL,
            title = "School",
            subtitle = "Select Private or Public institution first, then enter school name, numbers, email, Facebook link, and age.",
            badgeText = "Institution Portal",
            icon = Icons.Default.AccountBalance,
            accentColor = MaterialTheme.colorScheme.primary,
            enabled = !isSaving,
            onClick = { onSelectRole(PlatformRole.SCHOOL) }
        )

        RoleOptionCard(
            role = PlatformRole.PARENT,
            title = "Parent",
            subtitle = "Link 1 to 20 Child IDs to follow your children's progress and add optional parent contact numbers.",
            badgeText = "Up to 20 Child IDs",
            icon = Icons.Default.FamilyRestroom,
            accentColor = MaterialTheme.colorScheme.secondary,
            enabled = !isSaving,
            onClick = { onSelectRole(PlatformRole.PARENT) }
        )
    }
}

@Composable
private fun RoleOptionCard(
    role: PlatformRole,
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("role_card_${role.code}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Select $title",
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudentOnboardingWizard(
    step: StudentWizardStep,
    selectedSystem: String,
    schoolInput: String,
    selectedLevel: String,
    selectedStream: String,
    selectedSeries: String,
    selectedSubjects: List<String>,
    studentName: String,
    isSaving: Boolean,
    onSelectSystem: (EducationSystem) -> Unit,
    onSchoolInputChange: (String) -> Unit,
    onSelectLevel: (StudentLevel) -> Unit,
    onSelectStream: (StudentStream) -> Unit,
    onSelectSeries: (String) -> Unit,
    onToggleSubject: (String) -> Unit,
    onStepBack: () -> Unit,
    onFinishStudentOnboarding: () -> Unit
) {
    val stepIndex = when (step) {
        StudentWizardStep.EDUCATION_SYSTEM -> 1
        StudentWizardStep.SCHOOL_AND_LEVEL -> 2
        StudentWizardStep.STREAM -> 3
        StudentWizardStep.WELCOME_ANIMATION -> 4
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("student_wizard_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step Progress Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Student Step $stepIndex of 4",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (step != StudentWizardStep.EDUCATION_SYSTEM) {
                    OutlinedButton(
                        onClick = onStepBack,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("student_step_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous step",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back")
                    }
                }
            }

            LinearProgressIndicator(
                progress = { stepIndex / 4f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )

            when (step) {
                StudentWizardStep.EDUCATION_SYSTEM -> {
                    Text(
                        text = "1. Which Educational System do you study in?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select either the Anglophone or Francophone educational system to tailor your e-learning curriculum.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SelectionChoiceCard(
                        title = "Anglophone Educational System",
                        subtitle = "GCE Ordinary Level & Advanced Level curriculum in English",
                        icon = Icons.Default.Language,
                        isSelected = selectedSystem == EducationSystem.ANGLOPHONE.code,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { onSelectSystem(EducationSystem.ANGLOPHONE) },
                        testTag = "student_system_ANGLOPHONE"
                    )

                    SelectionChoiceCard(
                        title = "Francophone Educational System",
                        subtitle = "Probatoire & Baccalauréat / BEPC curriculum in French",
                        icon = Icons.Default.Public,
                        isSelected = selectedSystem == EducationSystem.FRANCOPHONE.code,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = { onSelectSystem(EducationSystem.FRANCOPHONE) },
                        testTag = "student_system_FRANCOPHONE"
                    )
                }

                StudentWizardStep.SCHOOL_AND_LEVEL -> {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "System: ${EducationSystem.fromCode(selectedSystem)?.label ?: selectedSystem}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Text(
                        text = "2. Select Your School (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pick your school from the list below or type it in case you don't find it (optional).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = schoolInput,
                        onValueChange = onSchoolInputChange,
                        label = { Text("School Name (Optional — select below or type yours)") },
                        placeholder = { Text("e.g. Bilingual Grammar School Molyko") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.School, contentDescription = null)
                        },
                        trailingIcon = {
                            if (schoolInput.isNotEmpty()) {
                                IconButton(onClick = { onSchoolInputChange("") }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear school")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_school_input")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserProfile.SUGGESTED_SCHOOLS.take(8).forEachIndexed { index, school ->
                            FilterChip(
                                selected = schoolInput.equals(school, ignoreCase = true),
                                onClick = { onSchoolInputChange(school) },
                                label = { Text(school) },
                                modifier = Modifier.testTag("student_suggested_school_$index")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    Text(
                        text = "3. Do you do Ordinary Level or Advanced Level?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select your current academic level in school to continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SelectionChoiceCard(
                        title = "Ordinary Level (O-Level / 1er Cycle)",
                        subtitle = "Foundation secondary cycle preparing for Ordinary Level examinations",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        isSelected = selectedLevel == StudentLevel.ORDINARY_LEVEL.code,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { onSelectLevel(StudentLevel.ORDINARY_LEVEL) },
                        testTag = "student_level_ORDINARY_LEVEL"
                    )

                    SelectionChoiceCard(
                        title = "Advanced Level (A-Level / 2nd Cycle)",
                        subtitle = "High school cycle preparing for Advanced Level & university entrance",
                        icon = Icons.Default.WorkspacePremium,
                        isSelected = selectedLevel == StudentLevel.ADVANCED_LEVEL.code,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = { onSelectLevel(StudentLevel.ADVANCED_LEVEL) },
                        testTag = "student_level_ADVANCED_LEVEL"
                    )
                }

                StudentWizardStep.STREAM -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = EducationSystem.fromCode(selectedSystem)?.label ?: selectedSystem,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = StudentLevel.fromCode(selectedLevel)?.label ?: selectedLevel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Text(
                        text = "4. Do you study Science or Art?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select your academic specialization to unlock your personalized e-learning welcome.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SelectionChoiceCard(
                        title = "Science",
                        subtitle = "Mathematics, Physics, Chemistry, Biology, Computer Science & Engineering",
                        icon = Icons.Default.Science,
                        isSelected = selectedStream == StudentStream.SCIENCE.code,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { onSelectStream(StudentStream.SCIENCE) },
                        testTag = "student_stream_SCIENCE"
                    )

                    SelectionChoiceCard(
                        title = "Art",
                        subtitle = "Literature, History, Philosophy, Languages, Economics & Humanities",
                        icon = Icons.Default.Palette,
                        isSelected = selectedStream == StudentStream.ART.code,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        onClick = { onSelectStream(StudentStream.ART) },
                        testTag = "student_stream_ART"
                    )
                }

                StudentWizardStep.WELCOME_ANIMATION -> {
                    StudentSweetWelcomeAnimation(
                        studentName = studentName,
                        educationSystemCode = selectedSystem,
                        educationSystem = EducationSystem.fromCode(selectedSystem)?.label ?: selectedSystem,
                        schoolName = schoolInput.trim().ifEmpty { "Independent E-Learner" },
                        levelCode = selectedLevel,
                        levelLabel = StudentLevel.fromCode(selectedLevel)?.label ?: selectedLevel,
                        streamCode = selectedStream,
                        streamLabel = StudentStream.fromCode(selectedStream)?.label ?: selectedStream,
                        selectedSeries = selectedSeries,
                        selectedSubjects = selectedSubjects,
                        onSelectSeries = onSelectSeries,
                        onToggleSubject = onToggleSubject,
                        isSaving = isSaving,
                        onEnterPlatform = onFinishStudentOnboarding
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentSweetWelcomeAnimation(
    studentName: String,
    educationSystemCode: String,
    educationSystem: String,
    schoolName: String,
    levelCode: String,
    levelLabel: String,
    streamCode: String,
    streamLabel: String,
    selectedSeries: String,
    selectedSubjects: List<String>,
    onSelectSeries: (String) -> Unit,
    onToggleSubject: (String) -> Unit,
    isSaving: Boolean,
    onEnterPlatform: () -> Unit
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
    }

    val badgeScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "welcome_badge_spring"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "elearning_welcome_orbit")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("student_welcome_animation_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated Orbiting Celebration Canvas + Trophy Badge
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(badgeScale),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.36f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.32f),
                            secondaryColor.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.35f * pulseGlow
                    ),
                    radius = baseRadius * 1.35f * pulseGlow,
                    center = center
                )

                drawCircle(
                    color = primaryColor.copy(alpha = 0.35f),
                    radius = baseRadius * 1.12f,
                    center = center,
                    style = Stroke(width = 3f)
                )

                // 8 orbiting celebratory sparkles
                for (i in 0 until 8) {
                    val angleRad = Math.toRadians((orbitAngle + i * 45f).toDouble())
                    val orbitRadius = baseRadius * (if (i % 2 == 0) 1.12f else 0.92f)
                    val sparkCenter = Offset(
                        x = center.x + (orbitRadius * cos(angleRad)).toFloat(),
                        y = center.y + (orbitRadius * sin(angleRad)).toFloat()
                    )
                    val sparkColor = when (i % 3) {
                        0 -> primaryColor
                        1 -> secondaryColor
                        else -> tertiaryColor
                    }
                    drawCircle(
                        color = sparkColor,
                        radius = if (i % 2 == 0) 8f * pulseGlow else 5f,
                        center = sparkCenter
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulseGlow * 0.95f)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        )
                    )
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = "Welcome to Aura E-Learning Platform",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(50)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AURA E-LEARNING PLATFORM UNLOCKED",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "Welcome to the E-Learning Platform, ${studentName.ifBlank { "Scholar" }}!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Your personalized $streamLabel learning hub for the $educationSystem ($levelLabel) is ready.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Student Track Summary Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryBadgeRow("Educational System", educationSystem)
                SummaryBadgeRow("School", schoolName)
                SummaryBadgeRow("Academic Level", levelLabel)
                SummaryBadgeRow("Specialization Stream", streamLabel)
                if (selectedSeries.isNotBlank()) {
                    SummaryBadgeRow("Selected Series", selectedSeries)
                }
                SummaryBadgeRow("Subjects Selected", "${selectedSubjects.size} Subject(s)")
            }
        }

        // JSON Curriculum Subject & Series Picker
        CurriculumInlineSubjectPicker(
            systemCode = educationSystemCode,
            levelCode = levelCode,
            streamCode = streamCode,
            selectedSeriesCode = selectedSeries,
            selectedSubjects = selectedSubjects,
            onSelectSeries = onSelectSeries,
            onToggleSubject = onToggleSubject
        )

        Button(
            onClick = onEnterPlatform,
            enabled = !isSaving,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("student_enter_platform_button")
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Opening E-Learning Hub…")
            } else {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enter E-Learning Platform",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SummaryBadgeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeacherOnboardingForm(
    teachingYears: String,
    onTeachingYearsChange: (String) -> Unit,
    selectedSystem: String,
    onSelectSystem: (EducationSystem) -> Unit,
    selectedSubjects: List<String>,
    onToggleSubject: (String) -> Unit,
    pseudonym: String,
    onPseudonymChange: (String) -> Unit,
    schoolDraft: String,
    onSchoolDraftChange: (String) -> Unit,
    schoolsList: List<String>,
    onAddSchool: () -> Unit,
    onRemoveSchool: (String) -> Unit,
    phoneDraft: String,
    onPhoneDraftChange: (String) -> Unit,
    phonesList: List<String>,
    onAddPhone: () -> Unit,
    onRemovePhone: (String) -> Unit,
    isSaving: Boolean,
    onSubmit: () -> Unit
) {
    val availableSubjects = remember(selectedSystem) {
        CurriculumCatalog.getSubjectsFor(
            systemCode = selectedSystem,
            levelCode = "",
            streamCode = "",
            seriesCode = "ALL"
        ).distinctBy { it.name.lowercase() }.take(16)
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
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("teacher_onboarding_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Teacher Experience & Credentials",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            // 1. Years of teaching
            OutlinedTextField(
                value = teachingYears,
                onValueChange = onTeachingYearsChange,
                label = { Text("Years of Teaching Experience") },
                placeholder = { Text("e.g. 8") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_years_input")
            )

            // 2. Educational System taught
            Text(
                text = "Educational System Taught",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EducationSystem.entries.forEach { system ->
                    FilterChip(
                        selected = selectedSystem == system.code,
                        onClick = { onSelectSystem(system) },
                        label = { Text(system.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.testTag("teacher_system_${system.code}")
                    )
                }
            }

            // 2b. Subjects Taught (From Curriculum JSON)
            Text(
                text = "Subjects You Teach (Select from Curriculum JSON)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("teacher_subjects_list")
            ) {
                availableSubjects.forEach { subject ->
                    val label = subject.formattedLabel
                    val isSelected = selectedSubjects.any {
                        it.equals(label, ignoreCase = true) || it.equals(subject.name, ignoreCase = true)
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleSubject(label) },
                        label = { Text(subject.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag(
                            "teacher_subject_${subject.code.ifBlank { subject.name.replace(" ", "_") }}"
                        )
                    )
                }
            }

            // 3. Schools where taught (Optional)
            MultiValueInputSection(
                title = "Schools Where You Taught (Optional)",
                subtitle = "Add one or multiple schools where you have taught.",
                draftValue = schoolDraft,
                onDraftValueChange = onSchoolDraftChange,
                placeholder = "Enter a school name…",
                items = schoolsList,
                onAddItem = onAddSchool,
                onRemoveItem = onRemoveSchool,
                inputTestTag = "teacher_schools_input",
                addButtonTestTag = "teacher_add_school_button"
            )

            // 4. Pseudonym (Optional)
            OutlinedTextField(
                value = pseudonym,
                onValueChange = onPseudonymChange,
                label = { Text("Pseudonym (Optional — to remain anonymous)") },
                placeholder = { Text("e.g. Prof. Euler") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_pseudonym_input")
            )

            // 5. Phone Numbers (Optional - supports many)
            MultiValueInputSection(
                title = "Phone Numbers (Optional — add many if needed)",
                subtitle = "Add your contact numbers if you have one or several.",
                draftValue = phoneDraft,
                onDraftValueChange = onPhoneDraftChange,
                placeholder = "e.g. +237 670 00 00 00",
                items = phonesList,
                onAddItem = onAddPhone,
                onRemoveItem = onRemovePhone,
                inputTestTag = "teacher_phone_input",
                addButtonTestTag = "teacher_add_phone_button",
                keyboardType = KeyboardType.Phone
            )

            Button(
                onClick = onSubmit,
                enabled = !isSaving && teachingYears.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("teacher_submit_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving Teacher Profile…")
                } else {
                    Text(
                        text = "Complete Teacher Onboarding",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SchoolOnboardingWizard(
    step: SchoolWizardStep,
    selectedOwnership: String,
    schoolName: String,
    onSchoolNameChange: (String) -> Unit,
    schoolEmail: String,
    onSchoolEmailChange: (String) -> Unit,
    schoolFacebook: String,
    onSchoolFacebookChange: (String) -> Unit,
    schoolAge: String,
    onSchoolAgeChange: (String) -> Unit,
    phoneDraft: String,
    onPhoneDraftChange: (String) -> Unit,
    phonesList: List<String>,
    onAddPhone: () -> Unit,
    onRemovePhone: (String) -> Unit,
    onSelectOwnership: (SchoolOwnership) -> Unit,
    onChangeOwnershipStep: () -> Unit,
    isSaving: Boolean,
    onSubmit: () -> Unit
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("school_onboarding_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (step) {
                SchoolWizardStep.OWNERSHIP_TYPE -> {
                    Text(
                        text = "Is the School Private or Public?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Before starting the school questionnaire, please select whether your institution is Private or Public.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SelectionChoiceCard(
                        title = "Private School",
                        subtitle = "Independent, confessional, or privately managed academic institution",
                        icon = Icons.Default.Domain,
                        isSelected = selectedOwnership == SchoolOwnership.PRIVATE.code,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { onSelectOwnership(SchoolOwnership.PRIVATE) },
                        testTag = "school_ownership_PRIVATE"
                    )

                    SelectionChoiceCard(
                        title = "Public School",
                        subtitle = "Government or state-operated public educational institution",
                        icon = Icons.Default.AccountBalance,
                        isSelected = selectedOwnership == SchoolOwnership.PUBLIC.code,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = { onSelectOwnership(SchoolOwnership.PUBLIC) },
                        testTag = "school_ownership_PUBLIC"
                    )
                }

                SchoolWizardStep.QUESTIONNAIRE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = SchoolOwnership.fromCode(selectedOwnership)?.label ?: "School",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        AssistChip(
                            onClick = onChangeOwnershipStep,
                            label = { Text("Change Private / Public") },
                            modifier = Modifier.testTag("school_change_ownership_button")
                        )
                    }

                    Text(
                        text = "School Institution Questionnaire",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = onSchoolNameChange,
                        label = { Text("School Name") },
                        placeholder = { Text("e.g. Collège Libermann Douala") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.School, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("school_name_input")
                    )

                    MultiValueInputSection(
                        title = "School Phone Numbers (Optional)",
                        subtitle = "Add one or multiple official contact phone numbers.",
                        draftValue = phoneDraft,
                        onDraftValueChange = onPhoneDraftChange,
                        placeholder = "e.g. +237 233 00 00 00",
                        items = phonesList,
                        onAddItem = onAddPhone,
                        onRemoveItem = onRemovePhone,
                        inputTestTag = "school_phone_input",
                        addButtonTestTag = "school_add_phone_button",
                        keyboardType = KeyboardType.Phone
                    )

                    OutlinedTextField(
                        value = schoolEmail,
                        onValueChange = onSchoolEmailChange,
                        label = { Text("School Email (Optional)") },
                        placeholder = { Text("contact@school.edu") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("school_email_input")
                    )

                    OutlinedTextField(
                        value = schoolFacebook,
                        onValueChange = onSchoolFacebookChange,
                        label = { Text("School Facebook Page Link (Optional)") },
                        placeholder = { Text("https://facebook.com/yourschool") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Public, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("school_facebook_input")
                    )

                    OutlinedTextField(
                        value = schoolAge,
                        onValueChange = onSchoolAgeChange,
                        label = { Text("School Age / Years of Existence (Optional)") },
                        placeholder = { Text("e.g. 25 years") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("school_age_input")
                    )

                    Button(
                        onClick = onSubmit,
                        enabled = !isSaving && schoolName.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("school_submit_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving School Profile…")
                        } else {
                            Text(
                                text = "Complete School Onboarding",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParentOnboardingForm(
    phoneDraft: String,
    onPhoneDraftChange: (String) -> Unit,
    phonesList: List<String>,
    onAddPhone: () -> Unit,
    onRemovePhone: (String) -> Unit,
    childIdDraft: String,
    onChildIdDraftChange: (String) -> Unit,
    childIdsList: List<String>,
    onAddChildId: () -> Unit,
    onRemoveChildId: (String) -> Unit,
    isSaving: Boolean,
    onSubmit: () -> Unit
) {
    val effectiveChildCount = childIdsList.size +
        if (childIdDraft.trim().isNotEmpty() && !childIdsList.contains(childIdDraft.trim())) 1 else 0
    val maxReached = childIdsList.size >= UserProfile.MAX_CHILD_IDS

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("parent_onboarding_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Parent Profile & Child ID Linking",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            // 1. Parent Numbers (Optional)
            MultiValueInputSection(
                title = "Parent Phone Numbers (Optional)",
                subtitle = "Add one or multiple contact phone numbers.",
                draftValue = phoneDraft,
                onDraftValueChange = onPhoneDraftChange,
                placeholder = "e.g. +237 690 00 00 00",
                items = phonesList,
                onAddItem = onAddPhone,
                onRemoveItem = onRemovePhone,
                inputTestTag = "parent_phone_input",
                addButtonTestTag = "parent_add_phone_button",
                keyboardType = KeyboardType.Phone
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // 2. Child IDs (1 to 20 max)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Child ID(s) (Add 1 to 20 max)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add multiple Child IDs if you have more than 1 child (maximum 20).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = if (maxReached) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("parent_child_count_badge")
                ) {
                    Text(
                        text = "${childIdsList.size} / ${UserProfile.MAX_CHILD_IDS}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (maxReached) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = childIdDraft,
                    onValueChange = onChildIdDraftChange,
                    enabled = !maxReached,
                    label = {
                        Text(
                            if (maxReached) {
                                "Maximum of 20 Child IDs reached"
                            } else {
                                "Enter Child ID (e.g. STD-2026-001)"
                            }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("parent_child_id_input")
                )

                Button(
                    onClick = onAddChildId,
                    enabled = !maxReached && childIdDraft.trim().isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("parent_add_child_id_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Child ID",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            AnimatedVisibility(visible = maxReached) {
                Text(
                    text = "You have reached the maximum limit of 20 Child IDs.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("parent_max_children_warning")
                )
            }

            if (childIdsList.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("parent_child_ids_list")
                ) {
                    childIdsList.forEachIndexed { index, childId ->
                        InputChip(
                            selected = true,
                            onClick = { onRemoveChildId(childId) },
                            label = { Text("#${index + 1}: $childId") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove $childId",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Button(
                onClick = onSubmit,
                enabled = !isSaving && effectiveChildCount in 1..UserProfile.MAX_CHILD_IDS,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("parent_submit_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving Parent Profile…")
                } else {
                    Text(
                        text = "Complete Parent Onboarding",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiValueInputSection(
    title: String,
    subtitle: String,
    draftValue: String,
    onDraftValueChange: (String) -> Unit,
    placeholder: String,
    items: List<String>,
    onAddItem: () -> Unit,
    onRemoveItem: (String) -> Unit,
    inputTestTag: String,
    addButtonTestTag: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draftValue,
                onValueChange = onDraftValueChange,
                placeholder = { Text(placeholder) },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag(inputTestTag)
            )
            OutlinedButton(
                onClick = onAddItem,
                enabled = draftValue.trim().isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(addButtonTestTag)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add item",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }

        if (items.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEach { item ->
                    InputChip(
                        selected = true,
                        onClick = { onRemoveItem(item) },
                        label = { Text(item) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove $item",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                accentColor.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isSelected) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.AutoMirrored.Filled.ArrowForward
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
