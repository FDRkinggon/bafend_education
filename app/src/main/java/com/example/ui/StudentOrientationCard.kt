package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppActivity
import com.example.data.model.CurriculumCatalog
import com.example.data.model.EducationSystem
import com.example.data.model.StudentLevel
import com.example.data.model.StudentStream
import com.example.data.model.UserProfile
import com.example.data.remote.StudentOrientationAnalysis

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentHighThinkingOrientationCard(
    profile: UserProfile,
    userActivities: List<AppActivity>,
    targetObjective: String,
    orientationState: StudentOrientationUiState,
    onTargetObjectiveChange: (String) -> Unit,
    onLogAcademicActivity: (subject: String, score: String, note: String) -> Unit,
    onTriggerCollectAndAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isScience = profile.studentStream != StudentStream.ART.code
    val systemLabel = EducationSystem.fromCode(profile.educationSystem)?.label ?: "Bilingual System"
    val levelLabel = StudentLevel.fromCode(profile.studentLevel)?.label ?: "Student Level"
    val streamLabel = StudentStream.fromCode(profile.studentStream)?.label ?: "Academic Stream"
    val activeSubjects = profile.selectedSubjectsList.ifEmpty {
        CurriculumCatalog.getDefaultSubjectLabelsFor(
            systemCode = profile.educationSystem,
            levelCode = profile.studentLevel,
            streamCode = profile.studentStream,
            seriesCode = profile.studentSeries
        )
    }

    var showScoreLogger by rememberSaveable { mutableStateOf(false) }
    var subjectInput by rememberSaveable {
        mutableStateOf(
            activeSubjects.firstOrNull()
                ?: if (isScience) "Mathematics & Physics" else "Literature & History"
        )
    }
    var scoreInput by rememberSaveable { mutableStateOf("16/20 (80%)") }
    var noteInput by rememberSaveable {
        mutableStateOf(
            if (isScience) {
                "Mastered differential calculus; difficulty with electromagnetic induction derivations"
            } else {
                "Strong dissertation thesis structure; difficulty with timed comparative commentary"
            }
        )
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(26.dp)
            )
            .testTag("student_high_thinking_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "High-Thinking Student Orientation",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "High-Thinking Student Level & Career Orientation",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "STUDENT EXCLUSIVE • $systemLabel • $levelLabel ($streamLabel)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "Collects all your recorded platform activities and academic scores to evaluate your current student level (difficulties, understandings, evolution, probability of success), how to revise better, what to focus on, how to reach excellence, which Series & Schools to choose after Ordinary and Advanced Level, and future career orientation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Activity Telemetry Pill Bar
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Collected User Activities Ready: ${userActivities.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    AssistChip(
                        onClick = { showScoreLogger = !showScoreLogger },
                        label = {
                            Text(if (showScoreLogger) "Hide Score Logger" else "+ Log Score / Study")
                        },
                        modifier = Modifier.testTag("toggle_score_logger_button")
                    )
                }
            }

            // Collapsible Student Score & Study Activity Logger
            AnimatedVisibility(visible = showScoreLogger) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_score_logger_panel")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Log Academic Assessment or Study Activity",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        if (activeSubjects.isNotEmpty()) {
                            Text(
                                text = "Select one of your JSON Curriculum Subjects:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.testTag("orientation_json_subject_chips")
                            ) {
                                activeSubjects.forEach { subjName ->
                                    AssistChip(
                                        onClick = { subjectInput = subjName },
                                        label = { Text(subjName) }
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isScience) {
                                AssistChip(
                                    onClick = {
                                        subjectInput = "Pure Mathematics & Calculus"
                                        scoreInput = "17/20 (85%)"
                                        noteInput = "Strong integration mastery; difficulty with 3D vector proofs"
                                    },
                                    label = { Text("Math 17/20") },
                                    modifier = Modifier.testTag("preset_score_math")
                                )
                                AssistChip(
                                    onClick = {
                                        subjectInput = "Physics & Chemistry Lab"
                                        scoreInput = "14/20 (70%)"
                                        noteInput = "Good stoichiometry; difficulty with AC circuit impedance"
                                    },
                                    label = { Text("Physics/Chem 14/20") },
                                    modifier = Modifier.testTag("preset_score_science")
                                )
                            } else {
                                AssistChip(
                                    onClick = {
                                        subjectInput = "Literature & Philosophy"
                                        scoreInput = "16/20 (80%)"
                                        noteInput = "Strong textual analysis; difficulty with timed dialectical synthesis"
                                    },
                                    label = { Text("Lit/Philo 16/20") },
                                    modifier = Modifier.testTag("preset_score_arts")
                                )
                                AssistChip(
                                    onClick = {
                                        subjectInput = "History & Economics"
                                        scoreInput = "15/20 (75%)"
                                        noteInput = "Clear macroeconomic grasp; difficulty with international treaties chronology"
                                    },
                                    label = { Text("Hist/Econ 15/20") },
                                    modifier = Modifier.testTag("preset_score_econ")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = subjectInput,
                            onValueChange = { subjectInput = it },
                            label = { Text("Subject / Paper") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_subject_input")
                        )

                        OutlinedTextField(
                            value = scoreInput,
                            onValueChange = { scoreInput = it },
                            label = { Text("Score (e.g. 16/20 or 82%)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_score_input")
                        )

                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            label = { Text("Understandings & Difficulties Noted") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_note_input")
                        )

                        OutlinedButton(
                            onClick = {
                                onLogAcademicActivity(subjectInput, scoreInput, noteInput)
                                showScoreLogger = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("log_student_activity_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddTask,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Activity to Student Record")
                        }
                    }
                }
            }

            // Target Objective Presets + Input
            Text(
                text = "Your Target Academic & Career Objective",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isScience) {
                    AssistChip(
                        onClick = {
                            onTargetObjectiveChange("Software Engineering & AI at Polytechnic (ENSPY) / FET Buea")
                        },
                        label = { Text("Polytechnic / Software Eng") },
                        modifier = Modifier.testTag("objective_chip_polytech")
                    )
                    AssistChip(
                        onClick = {
                            onTargetObjectiveChange("Medicine & Biomedical Sciences at FMBS / FHS")
                        },
                        label = { Text("Medicine (FMBS / FHS)") },
                        modifier = Modifier.testTag("objective_chip_medicine")
                    )
                } else {
                    AssistChip(
                        onClick = {
                            onTargetObjectiveChange("International Relations, Diplomacy & Law at IRIC / ENAM / FSJP")
                        },
                        label = { Text("Diplomacy & Law (IRIC / ENAM)") },
                        modifier = Modifier.testTag("objective_chip_diplomacy")
                    )
                    AssistChip(
                        onClick = {
                            onTargetObjectiveChange("Corporate Finance, Economics & Management at ESSEC / HEC")
                        },
                        label = { Text("Finance & Business (ESSEC)") },
                        modifier = Modifier.testTag("objective_chip_business")
                    )
                }
            }

            OutlinedTextField(
                value = targetObjective,
                onValueChange = onTargetObjectiveChange,
                label = { Text("Target Objective (Series, School, or Dream Career)") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_target_objective_input")
            )

            // Primary Trigger Button: Collect All Activities & Run High-Thinking Orientation
            Button(
                onClick = onTriggerCollectAndAnalyze,
                enabled = orientationState !is StudentOrientationUiState.Analyzing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("trigger_student_orientation_button")
            ) {
                if (orientationState is StudentOrientationUiState.Analyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onTertiary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Collecting Activities & Running High-Thinking Analysis…")
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Collect All Activities & Analyse Student Level & Orientation",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Results Display
            when (orientationState) {
                is StudentOrientationUiState.Idle -> Unit
                is StudentOrientationUiState.Analyzing -> Unit
                is StudentOrientationUiState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_orientation_error_box")
                    ) {
                        Text(
                            text = orientationState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                is StudentOrientationUiState.ResultReady -> {
                    StudentOrientationResultDashboard(analysis = orientationState.analysis)
                }
            }
        }
    }
}

@Composable
private fun StudentOrientationResultDashboard(
    analysis: StudentOrientationAnalysis
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_orientation_result_container"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // 1. Probability of Success & Student Level Banner Card
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("student_probability_of_success_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STUDENT LEVEL & SUCCESS PROBABILITY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "${analysis.successProbabilityPercent}% Success Probability",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = analysis.studentLevelSummary,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                LinearProgressIndicator(
                    progress = { (analysis.successProbabilityPercent / 100f).coerceIn(0f, 1f) },
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(50))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Activities Collected: ${analysis.collectedActivitiesCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Mastery Index: ${analysis.averageScorePercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = analysis.probabilityOfSuccessNarrative,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 2. Student Level Diagnostic: Understandings, Difficulties & Evolution
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("student_level_diagnostic_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionBadgeHeader(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    title = "Student Level Diagnostic: Understandings, Difficulties & Evolution",
                    tint = MaterialTheme.colorScheme.primary
                )

                // Understandings
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_understandings_section"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Understandings & Demonstrated Strengths",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    analysis.understandings.forEach { item ->
                        BulletPointRow(
                            icon = Icons.Default.CheckCircle,
                            text = item,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Difficulties
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_difficulties_section"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Identified Difficulties & Bottlenecks",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    analysis.difficulties.forEach { item ->
                        BulletPointRow(
                            icon = Icons.Default.WarningAmber,
                            text = item,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Evolution
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_evolution_section"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Academic Evolution & Trajectory",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = analysis.evolution,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 3. High-Thinking Revision & Excellence Blueprint
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("student_revision_excellence_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionBadgeHeader(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "High-Thinking Revision & Path to Excellence",
                    tint = MaterialTheme.colorScheme.tertiary
                )

                // How to Revise Better
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_revise_better_section"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "How to Revise Better",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    analysis.howToReviseBetter.forEach { item ->
                        BulletPointRow(
                            icon = Icons.Default.Lightbulb,
                            text = item,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // What to Focus On
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_focus_on_section"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "What to Focus On Immediately",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    analysis.whatToFocusOn.forEach { item ->
                        BulletPointRow(
                            icon = Icons.Default.Insights,
                            text = item,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // How to Reach Excellence
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_reach_excellence_section"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "How to Reach Academic Excellence",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    analysis.howToReachExcellence.forEach { item ->
                        BulletPointRow(
                            icon = Icons.Default.EmojiEvents,
                            text = item,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // 4. Series & School Orientation (After Ordinary Level & After Advanced Level)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("student_school_series_orientation_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionBadgeHeader(
                    icon = Icons.Default.School,
                    title = "Series & Schools Orientation (Post-Ordinary & Post-Advanced Level)",
                    tint = MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_after_ordinary_section"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Where to Go After Ordinary Level (Recommended Series & High Schools)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = analysis.afterOrdinaryLevelOrientation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_after_advanced_section"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "What Awaits After Advanced Level & Where to Go (Grandes Écoles, Faculties & Schools)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = analysis.afterAdvancedLevelOrientation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Text(
                    text = analysis.targetObjectiveAlignment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 5. Future Career Orientation Based on Scores & Potential
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("student_future_careers_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("student_future_careers_section"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionBadgeHeader(
                    icon = Icons.Default.Work,
                    title = "Orientation on Future Careers (Based on Scores & Potential)",
                    tint = MaterialTheme.colorScheme.secondary
                )

                analysis.futureCareerOrientation.forEach { career ->
                    BulletPointRow(
                        icon = Icons.Default.Work,
                        text = career,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionBadgeHeader(
    icon: ImageVector,
    title: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BulletPointRow(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
