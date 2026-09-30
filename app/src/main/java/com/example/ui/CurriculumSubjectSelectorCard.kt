package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CurriculumCatalog
import com.example.data.model.CurriculumSubjectItem
import com.example.data.model.EducationSystem
import com.example.data.model.StudentLevel
import com.example.data.model.StudentStream
import com.example.data.model.UserProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurriculumSubjectSelectorCard(
    profile: UserProfile,
    onSaveCurriculumSelection: (
        educationSystem: String,
        studentLevel: String,
        studentStream: String,
        studentSeries: String,
        selectedSubjects: List<String>
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var jsonVersionTrigger by remember { mutableIntStateOf(0) }
    var showJsonEditor by rememberSaveable { mutableStateOf(false) }
    var customJsonDraft by rememberSaveable {
        mutableStateOf(CurriculumCatalog.getActiveJsonString(context))
    }
    var jsonStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedSystem by rememberSaveable(profile.educationSystem) {
        mutableStateOf(
            profile.educationSystem.takeIf {
                it == EducationSystem.ANGLOPHONE.code || it == EducationSystem.FRANCOPHONE.code
            } ?: EducationSystem.ANGLOPHONE.code
        )
    }
    var selectedLevel by rememberSaveable(profile.studentLevel) {
        mutableStateOf(
            profile.studentLevel.takeIf {
                it == StudentLevel.ORDINARY_LEVEL.code || it == StudentLevel.ADVANCED_LEVEL.code
            } ?: StudentLevel.ADVANCED_LEVEL.code
        )
    }
    var selectedStream by rememberSaveable(profile.studentStream) {
        mutableStateOf(
            profile.studentStream.takeIf {
                it == StudentStream.SCIENCE.code || it == StudentStream.ART.code
            } ?: StudentStream.SCIENCE.code
        )
    }

    val availableSeries = remember(selectedSystem, selectedLevel, selectedStream, jsonVersionTrigger) {
        CurriculumCatalog.getAllSeries(
            systemCode = selectedSystem,
            levelCode = selectedLevel,
            streamCode = selectedStream
        )
    }

    var selectedSeriesCode by rememberSaveable(
        profile.studentSeries,
        selectedSystem,
        selectedLevel,
        selectedStream
    ) {
        val initial = profile.studentSeries.takeIf { code ->
            code.isNotBlank() && availableSeries.any { it.code.equals(code, ignoreCase = true) }
        } ?: availableSeries.firstOrNull()?.code ?: "ALL"
        mutableStateOf(initial)
    }

    LaunchedEffect(availableSeries) {
        if (selectedSeriesCode != "ALL" && availableSeries.none { it.code == selectedSeriesCode }) {
            selectedSeriesCode = availableSeries.firstOrNull()?.code ?: "ALL"
        }
    }

    var subjectSearchQuery by rememberSaveable { mutableStateOf("") }

    val availableSubjects = remember(
        selectedSystem,
        selectedLevel,
        selectedStream,
        selectedSeriesCode,
        jsonVersionTrigger
    ) {
        CurriculumCatalog.getSubjectsFor(
            systemCode = selectedSystem,
            levelCode = selectedLevel,
            streamCode = selectedStream,
            seriesCode = selectedSeriesCode
        )
    }

    val filteredSubjects = remember(availableSubjects, subjectSearchQuery) {
        val q = subjectSearchQuery.trim().lowercase()
        if (q.isEmpty()) {
            availableSubjects
        } else {
            availableSubjects.filter { subj ->
                subj.name.lowercase().contains(q) ||
                    subj.code.lowercase().contains(q) ||
                    subj.category.lowercase().contains(q) ||
                    subj.seriesName.lowercase().contains(q)
            }
        }
    }

    val selectedSubjectsState = remember(profile.selectedSubjects) {
        val initialList = profile.selectedSubjectsList.ifEmpty {
            CurriculumCatalog.getDefaultSubjectLabelsFor(
                systemCode = selectedSystem,
                levelCode = selectedLevel,
                streamCode = selectedStream,
                seriesCode = selectedSeriesCode
            )
        }
        mutableStateListOf<String>().apply { addAll(initialList) }
    }

    val activeSeriesObj = remember(availableSeries, selectedSeriesCode) {
        availableSeries.firstOrNull { it.code == selectedSeriesCode }
    }

    val totalSelectedCoefficient = remember(selectedSubjectsState.toList(), availableSubjects) {
        availableSubjects
            .filter { subj -> isSubjectSelected(subj, selectedSubjectsState) }
            .sumOf { it.coefficient }
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(26.dp)
            )
            .testTag("curriculum_subject_selector_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Select Subjects from Curriculum JSON",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Select Your Subjects (Curriculum JSON)",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedSubjectsState.size} Subject(s) Selected • Total Coeff: $totalSelectedCoefficient",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                AssistChip(
                    onClick = { showJsonEditor = !showJsonEditor },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(if (showJsonEditor) "Hide JSON" else "JSON Source") },
                    modifier = Modifier.testTag("toggle_custom_json_editor_button")
                )
            }

            Text(
                text = "Choose your Educational System, Academic Level, Stream, and Series below to select your official subjects loaded from the curriculum JSON.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Optional Custom JSON Inspector / Loader
            AnimatedVisibility(visible = showJsonEditor) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_json_editor_panel")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Curriculum JSON Data Source (Inspect or Paste Custom JSON)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Loaded from assets/curriculum_subjects.json. You can also paste your own JSON structure below to dynamically populate the subjects.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = customJsonDraft,
                            onValueChange = { customJsonDraft = it },
                            label = { Text("Curriculum JSON Payload") },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_curriculum_json_input")
                        )

                        if (jsonStatusMessage != null) {
                            Text(
                                text = jsonStatusMessage.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val ok = CurriculumCatalog.setCustomCurriculumJson(customJsonDraft)
                                    if (ok) {
                                        jsonVersionTrigger++
                                        jsonStatusMessage = "Custom JSON curriculum applied! Subjects updated below."
                                    } else {
                                        jsonStatusMessage = "Could not parse subjects from JSON — check format."
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("apply_custom_json_button")
                            ) {
                                Text("Apply JSON Subjects")
                            }

                            OutlinedButton(
                                onClick = {
                                    CurriculumCatalog.setCustomCurriculumJson(null)
                                    customJsonDraft = CurriculumCatalog.DEFAULT_CURRICULUM_JSON.trim()
                                    jsonVersionTrigger++
                                    jsonStatusMessage = "Restored default Bilingual Curriculum JSON."
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reset_default_json_button")
                            ) {
                                Text("Reset Default")
                            }
                        }
                    }
                }
            }

            // 1. Educational System Filter
            Text(
                text = "1. Educational System",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    EducationSystem.ANGLOPHONE.code to "Anglophone (GCE)",
                    EducationSystem.FRANCOPHONE.code to "Francophone (OBC / ESG)"
                ).forEach { (sysCode, label) ->
                    FilterChip(
                        selected = selectedSystem == sysCode,
                        onClick = { selectedSystem = sysCode },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("curriculum_system_chip_$sysCode")
                    )
                }
            }

            // 2. Academic Level & Stream Filter
            Text(
                text = "2. Academic Level & Specialization Stream",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    StudentLevel.ORDINARY_LEVEL.code to if (selectedSystem == EducationSystem.FRANCOPHONE.code) {
                        "1er Cycle (BEPC)"
                    } else {
                        "Ordinary Level (O-Level)"
                    },
                    StudentLevel.ADVANCED_LEVEL.code to if (selectedSystem == EducationSystem.FRANCOPHONE.code) {
                        "2nd Cycle (Probatoire / Bacc)"
                    } else {
                        "Advanced Level (A-Level)"
                    }
                ).forEach { (lvlCode, label) ->
                    FilterChip(
                        selected = selectedLevel == lvlCode,
                        onClick = { selectedLevel = lvlCode },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("curriculum_level_chip_$lvlCode")
                    )
                }

                listOf(
                    StudentStream.SCIENCE.code to "Science Stream",
                    StudentStream.ART.code to "Arts / Literature Stream"
                ).forEach { (strCode, label) ->
                    FilterChip(
                        selected = selectedStream == strCode,
                        onClick = { selectedStream = strCode },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.testTag("curriculum_stream_chip_$strCode")
                    )
                }
            }

            // 3. Series Selection from JSON
            if (availableSeries.isNotEmpty()) {
                Text(
                    text = "3. Select Series / Combination (From JSON)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedSeriesCode == "ALL",
                        onClick = { selectedSeriesCode = "ALL" },
                        label = { Text("All Series") },
                        modifier = Modifier.testTag("curriculum_series_chip_ALL")
                    )
                    availableSeries.forEach { series ->
                        FilterChip(
                            selected = selectedSeriesCode == series.code,
                            onClick = {
                                selectedSeriesCode = series.code
                            },
                            label = { Text(series.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("curriculum_series_chip_${series.code}")
                        )
                    }
                }

                if (activeSeriesObj != null && activeSeriesObj.description.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${activeSeriesObj.name}: ${activeSeriesObj.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // 4. Subject Search & Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "4. Tap Subjects to Select (${filteredSubjects.size} available)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = {
                            availableSubjects.forEach { subj ->
                                val label = subj.formattedLabel
                                if (selectedSubjectsState.none { it.equals(label, ignoreCase = true) || it.equals(subj.name, ignoreCase = true) }) {
                                    selectedSubjectsState.add(label)
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Select All") },
                        modifier = Modifier.testTag("select_series_core_subjects_button")
                    )
                    if (selectedSubjectsState.isNotEmpty()) {
                        AssistChip(
                            onClick = { selectedSubjectsState.clear() },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Clear") },
                            modifier = Modifier.testTag("clear_selected_subjects_button")
                        )
                    }
                }
            }

            OutlinedTextField(
                value = subjectSearchQuery,
                onValueChange = { subjectSearchQuery = it },
                label = { Text("Search subjects by name, code (e.g. 0770), or category…") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (subjectSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { subjectSearchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("curriculum_subject_search_input")
            )

            // Subject List from JSON
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("curriculum_subjects_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredSubjects.forEach { subject ->
                    val isSelected = isSubjectSelected(subject, selectedSubjectsState)
                    val tagSuffix = subject.code.ifBlank {
                        subject.name.replace(" ", "_")
                    }
                    Surface(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                toggleSubjectSelection(subject, selectedSubjectsState)
                            }
                            .testTag("curriculum_subject_item_$tagSuffix")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.RadioButtonUnchecked
                                    },
                                    contentDescription = if (isSelected) "Selected" else "Not selected",
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            if (subject.code.isNotBlank()) append("Code: ${subject.code} • ")
                                            append("Category: ${subject.category}")
                                            if (subject.seriesCode.isNotBlank() && subject.seriesCode != "ALL") {
                                                append(" • ${subject.seriesCode}")
                                            }
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Coeff ${subject.coefficient}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    onSaveCurriculumSelection(
                        selectedSystem,
                        selectedLevel,
                        selectedStream,
                        if (selectedSeriesCode == "ALL") "" else selectedSeriesCode,
                        selectedSubjectsState.toList()
                    )
                },
                enabled = selectedSubjectsState.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("save_selected_subjects_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Selected Subjects (${selectedSubjectsState.size}) to Profile",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Compact inline subject & series picker used inside RoleOnboardingScreen (for Students & Teachers).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurriculumInlineSubjectPicker(
    systemCode: String,
    levelCode: String,
    streamCode: String,
    selectedSeriesCode: String,
    selectedSubjects: List<String>,
    onSelectSeries: (String) -> Unit,
    onToggleSubject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableSeries = remember(systemCode, levelCode, streamCode) {
        CurriculumCatalog.getAllSeries(
            systemCode = systemCode,
            levelCode = levelCode,
            streamCode = streamCode
        )
    }
    val availableSubjects = remember(systemCode, levelCode, streamCode, selectedSeriesCode) {
        CurriculumCatalog.getSubjectsFor(
            systemCode = systemCode,
            levelCode = levelCode,
            streamCode = streamCode,
            seriesCode = selectedSeriesCode.ifBlank { "ALL" }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("onboarding_subject_picker_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Select Your Series & Subjects (From Curriculum JSON)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            if (availableSeries.size > 1) {
                Text(
                    text = "Series / Specialty:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableSeries.forEach { series ->
                        FilterChip(
                            selected = selectedSeriesCode == series.code,
                            onClick = { onSelectSeries(series.code) },
                            label = { Text(series.name) },
                            modifier = Modifier.testTag("onboarding_series_${series.code}")
                        )
                    }
                }
            }

            Text(
                text = "Subjects (${selectedSubjects.size} selected — tap to toggle):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableSubjects.forEach { subject ->
                    val label = subject.formattedLabel
                    val isSelected = selectedSubjects.any {
                        it.equals(label, ignoreCase = true) || it.equals(subject.name, ignoreCase = true)
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleSubject(label) },
                        label = {
                            Text("${subject.name} (Coeff ${subject.coefficient})")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag(
                            "onboarding_subject_${subject.code.ifBlank { subject.name.replace(" ", "_") }}"
                        )
                    )
                }
            }
        }
    }
}

private fun isSubjectSelected(
    subject: CurriculumSubjectItem,
    selectedList: List<String>
): Boolean {
    val formatted = subject.formattedLabel
    return selectedList.any {
        it.equals(formatted, ignoreCase = true) ||
            it.equals(subject.name, ignoreCase = true) ||
            (subject.code.isNotBlank() && it.contains("(${subject.code})", ignoreCase = true))
    }
}

private fun toggleSubjectSelection(
    subject: CurriculumSubjectItem,
    selectedList: MutableList<String>
) {
    val formatted = subject.formattedLabel
    val existingIndex = selectedList.indexOfFirst {
        it.equals(formatted, ignoreCase = true) ||
            it.equals(subject.name, ignoreCase = true) ||
            (subject.code.isNotBlank() && it.contains("(${subject.code})", ignoreCase = true))
    }
    if (existingIndex >= 0) {
        selectedList.removeAt(existingIndex)
    } else {
        selectedList.add(formatted)
    }
}
