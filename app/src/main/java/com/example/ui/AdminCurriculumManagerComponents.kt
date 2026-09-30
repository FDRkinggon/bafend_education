package com.example.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminCustomSubject
import com.example.data.model.BafendFileEngine
import com.example.data.model.CurriculumCatalog
import com.example.data.model.EducationSystem
import com.example.data.model.RevisionBafendDocument
import com.example.data.model.StudentLevel
import com.example.data.model.StudentStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCurriculumAndPdfSection(
    adminCustomSubjects: List<AdminCustomSubject>,
    revisionDocuments: List<RevisionBafendDocument>,
    onAddSubjectUnderSubsystem: (
        code: String,
        name: String,
        coefficient: Int,
        category: String,
        systemCode: String,
        levelCode: String,
        streamCode: String,
        seriesCode: String,
        seriesName: String,
        description: String
    ) -> Unit,
    onDeleteCustomSubject: (String) -> Unit,
    onUploadPdfForSubject: (
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
        examYear: String,
        originalPdfName: String,
        rawPdfBytes: ByteArray?,
        includeSolution: Boolean,
        solutionOriginalPdfName: String,
        rawSolutionPdfBytes: ByteArray?
    ) -> Unit,
    onOpenBafendReader: (RevisionBafendDocument) -> Unit,
    onDeleteRevisionDocument: (RevisionBafendDocument) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // --- State for Section 1: Add Subject under Any Subsystem ---
    var subjSystem by rememberSaveable { mutableStateOf(EducationSystem.ANGLOPHONE.code) }
    var subjLevel by rememberSaveable { mutableStateOf(StudentLevel.ADVANCED_LEVEL.code) }
    var subjStream by rememberSaveable { mutableStateOf(StudentStream.SCIENCE.code) }
    var subjSeriesCode by rememberSaveable { mutableStateOf("S1") }
    var subjSeriesName by rememberSaveable { mutableStateOf("Series S1 (Maths, Physics, Chemistry)") }
    var subjName by rememberSaveable { mutableStateOf("") }
    var subjCode by rememberSaveable { mutableStateOf("") }
    var subjCoefficientText by rememberSaveable { mutableStateOf("5") }
    var subjCategory by rememberSaveable { mutableStateOf("Principal") }
    var subjDescription by rememberSaveable { mutableStateOf("") }

    // --- State for Section 2: Upload PDF -> Assigned .bafend Extension ---
    var pdfSystem by rememberSaveable { mutableStateOf(EducationSystem.ANGLOPHONE.code) }
    var pdfLevel by rememberSaveable { mutableStateOf(StudentLevel.ADVANCED_LEVEL.code) }
    var pdfStream by rememberSaveable { mutableStateOf(StudentStream.SCIENCE.code) }

    val availableSubjectsForPdf = remember(pdfSystem, pdfLevel, pdfStream, adminCustomSubjects) {
        CurriculumCatalog.getSubjectsFor(
            systemCode = pdfSystem,
            levelCode = pdfLevel,
            streamCode = pdfStream
        )
    }

    var selectedTargetSubjectCode by rememberSaveable { mutableStateOf("0770") }
    var selectedTargetSubjectName by rememberSaveable { mutableStateOf("Pure Mathematics With Mechanics") }
    var selectedTargetSubjectCategory by rememberSaveable { mutableStateOf("Principal") }
    var selectedTargetSubjectCoeff by rememberSaveable { mutableStateOf(5) }
    var selectedTargetSeriesCode by rememberSaveable { mutableStateOf("S1") }

    LaunchedEffect(availableSubjectsForPdf) {
        val first = availableSubjectsForPdf.firstOrNull()
        if (first != null && availableSubjectsForPdf.none { it.code == selectedTargetSubjectCode && it.name == selectedTargetSubjectName }) {
            selectedTargetSubjectCode = first.code.ifBlank { "SUBJ" }
            selectedTargetSubjectName = first.name
            selectedTargetSubjectCategory = first.category
            selectedTargetSubjectCoeff = first.coefficient
            selectedTargetSeriesCode = first.seriesCode.ifBlank { "ALL" }
        }
    }

    var pdfDocTitle by rememberSaveable { mutableStateOf("") }
    var pdfDocDescription by rememberSaveable { mutableStateOf("") }
    var pdfExamYear by rememberSaveable { mutableStateOf("2024") }
    var pickedPdfFileName by rememberSaveable { mutableStateOf("") }
    var pickedPdfBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Toggle for including a Paired Past June Solution
    var includePastJuneSolution by rememberSaveable { mutableStateOf(false) }
    var pickedSolutionPdfFileName by rememberSaveable { mutableStateOf("") }
    var pickedSolutionPdfBytes by remember { mutableStateOf<ByteArray?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                var name = "past_june_question.pdf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) {
                        name = cursor.getString(idx) ?: name
                    }
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    pickedPdfFileName = name
                    pickedPdfBytes = bytes
                    if (pdfDocTitle.isBlank()) {
                        pdfDocTitle = name.removeSuffix(".pdf").removeSuffix(".PDF").replace("_", " ")
                    }
                }
            }
        }
    }

    val solutionPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                var name = "past_june_solution.pdf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) {
                        name = cursor.getString(idx) ?: name
                    }
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    pickedSolutionPdfFileName = name
                    pickedSolutionPdfBytes = bytes
                }
            }
        }
    }

    val previewAssignedBafendName = remember(pickedPdfFileName, pdfDocTitle, selectedTargetSubjectCode, pdfExamYear) {
        val cleanYear = pdfExamYear.trim().ifBlank { "2024" }
        val base = pickedPdfFileName.ifBlank {
            "${selectedTargetSubjectCode}_June_${cleanYear}_${pdfDocTitle.ifBlank { selectedTargetSubjectName }}"
        }
        BafendFileEngine.ensureBafendExtension(base)
    }

    val previewAssignedSolutionBafendName = remember(pickedSolutionPdfFileName, previewAssignedBafendName) {
        val base = pickedSolutionPdfFileName.ifBlank {
            "${previewAssignedBafendName.removeSuffix(".bafend")}_Solution"
        }
        BafendFileEngine.ensureBafendExtension(base)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_curriculum_and_pdf_section"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // =========================================================================
        // CARD 1: ADMIN — ADD A SUBJECT UNDER ANY SUBSYSTEM WITH CHARACTERISTICS
        // =========================================================================
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("admin_add_subject_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Add Subject Under Any Subsystem (Admin)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Configure Subsystem, Level, Stream, Series, Code, Coefficient & Category",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1. Subsystem Selection
                Text(
                    text = "1. Target Educational Subsystem",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        EducationSystem.ANGLOPHONE.code to "Anglophone (GCE)",
                        EducationSystem.FRANCOPHONE.code to "Francophone (OBC / ESG)",
                        EducationSystem.BILINGUAL.code to "Bilingual (Both Subsystems)"
                    ).forEach { (sysCode, label) ->
                        FilterChip(
                            selected = subjSystem == sysCode,
                            onClick = {
                                subjSystem = sysCode
                                if (sysCode == EducationSystem.FRANCOPHONE.code && subjSeriesCode == "S1") {
                                    subjSeriesCode = "SERIE_C"
                                    subjSeriesName = "Série C (Maths & Physique)"
                                } else if (sysCode == EducationSystem.ANGLOPHONE.code && subjSeriesCode == "SERIE_C") {
                                    subjSeriesCode = "S1"
                                    subjSeriesName = "Series S1 (Maths, Physics, Chemistry)"
                                }
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("admin_subj_system_$sysCode")
                        )
                    }
                }

                // 2. Level & Stream Selection
                Text(
                    text = "2. Academic Level & Specialization Stream",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        StudentLevel.ORDINARY_LEVEL.code to "Ordinary Level / 1er Cycle",
                        StudentLevel.ADVANCED_LEVEL.code to "Advanced Level / 2nd Cycle"
                    ).forEach { (lvl, label) ->
                        FilterChip(
                            selected = subjLevel == lvl,
                            onClick = { subjLevel = lvl },
                            label = { Text(label) },
                            modifier = Modifier.testTag("admin_subj_level_$lvl")
                        )
                    }
                    listOf(
                        StudentStream.SCIENCE.code to "Science Stream",
                        StudentStream.ART.code to "Arts / Literature Stream"
                    ).forEach { (str, label) ->
                        FilterChip(
                            selected = subjStream == str,
                            onClick = { subjStream = str },
                            label = { Text(label) },
                            modifier = Modifier.testTag("admin_subj_stream_$str")
                        )
                    }
                }

                // 3. Subject Characteristics Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = subjName,
                        onValueChange = { subjName = it },
                        label = { Text("Subject Name (e.g. Robotics & AI)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("admin_new_subject_name_input")
                    )
                    OutlinedTextField(
                        value = subjCode,
                        onValueChange = { subjCode = it },
                        label = { Text("Code (e.g. 0795)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_new_subject_code_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = subjCoefficientText,
                        onValueChange = { subjCoefficientText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Coefficient (1-10)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_new_subject_coeff_input")
                    )
                    OutlinedTextField(
                        value = subjSeriesCode,
                        onValueChange = { subjSeriesCode = it },
                        label = { Text("Series Code (e.g. S1, SERIE_C)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("admin_new_subject_series_input")
                    )
                }

                Text(
                    text = "3. Subject Category / Characteristic Status",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AdminCustomSubject.SUBJECT_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = subjCategory.equals(cat, ignoreCase = true),
                            onClick = { subjCategory = cat },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("admin_subj_category_$cat")
                        )
                    }
                }

                OutlinedTextField(
                    value = subjDescription,
                    onValueChange = { subjDescription = it },
                    label = { Text("Subject Characteristics, Syllabus Competencies & Paper Structure") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_new_subject_description_input")
                )

                Button(
                    onClick = {
                        val cleanName = subjName.trim()
                        if (cleanName.isNotEmpty()) {
                            val cleanCode = subjCode.trim().ifBlank {
                                cleanName.take(4).uppercase() + "-01"
                            }
                            val coeff = subjCoefficientText.toIntOrNull()?.coerceIn(1, 20) ?: 4
                            onAddSubjectUnderSubsystem(
                                cleanCode,
                                cleanName,
                                coeff,
                                subjCategory,
                                subjSystem,
                                subjLevel,
                                subjStream,
                                subjSeriesCode.trim().ifBlank { "GENERAL" },
                                subjSeriesName.trim().ifBlank { "Series ${subjSeriesCode.trim()}" },
                                subjDescription.trim()
                            )
                            // Pre-select the newly created subject in the PDF uploader below
                            pdfSystem = subjSystem
                            pdfLevel = subjLevel
                            pdfStream = subjStream
                            selectedTargetSubjectCode = cleanCode
                            selectedTargetSubjectName = cleanName
                            selectedTargetSubjectCategory = subjCategory
                            selectedTargetSubjectCoeff = coeff
                            selectedTargetSeriesCode = subjSeriesCode.trim().ifBlank { "GENERAL" }
                            subjName = ""
                            subjCode = ""
                            subjDescription = ""
                        }
                    },
                    enabled = subjName.trim().isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("admin_submit_new_subject_button")
                ) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Subject to Subsystem & Platform Catalog")
                }

                // List of Admin-Added Subjects
                if (adminCustomSubjects.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Text(
                        text = "Subjects Added via Platform (${adminCustomSubjects.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        adminCustomSubjects.forEach { item ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_added_subject_row_${item.code}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${item.name} (${item.code})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${item.systemCode} • ${item.levelCode} • ${item.streamCode} • Series: ${item.seriesCode} • Coeff: ${item.coefficient} • ${item.category}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteCustomSubject(item.subjectId) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete subject",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CARD 2: ADMIN — UPLOAD PDF FOR A SPECIFIC SUBJECT -> ASSIGNED .BAFEND
        // =========================================================================
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("admin_upload_bafend_pdf_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Upload Past Question Paper & Paired Solution (Admin Drive)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Specify Exam Year, send PDF directly to Admin Drive, and optionally toggle a paired Past June Solution",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Filter target subsystem/level/stream for subject picker
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        EducationSystem.ANGLOPHONE.code to "Anglophone",
                        EducationSystem.FRANCOPHONE.code to "Francophone",
                        EducationSystem.BILINGUAL.code to "Bilingual"
                    ).forEach { (sys, label) ->
                        FilterChip(
                            selected = pdfSystem == sys,
                            onClick = { pdfSystem = sys },
                            label = { Text(label) },
                            modifier = Modifier.testTag("admin_pdf_system_$sys")
                        )
                    }
                    listOf(
                        StudentLevel.ORDINARY_LEVEL.code to "O-Level / 1er Cycle",
                        StudentLevel.ADVANCED_LEVEL.code to "A-Level / 2nd Cycle"
                    ).forEach { (lvl, label) ->
                        FilterChip(
                            selected = pdfLevel == lvl,
                            onClick = { pdfLevel = lvl },
                            label = { Text(label) },
                            modifier = Modifier.testTag("admin_pdf_level_$lvl")
                        )
                    }
                    listOf(
                        StudentStream.SCIENCE.code to "Science",
                        StudentStream.ART.code to "Arts"
                    ).forEach { (str, label) ->
                        FilterChip(
                            selected = pdfStream == str,
                            onClick = { pdfStream = str },
                            label = { Text(label) },
                            modifier = Modifier.testTag("admin_pdf_stream_$str")
                        )
                    }
                }

                Text(
                    text = "Select Target Subject (${availableSubjectsForPdf.size} available in this subsystem):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableSubjectsForPdf.take(16).forEach { subj ->
                        val isSelected = selectedTargetSubjectCode == subj.code &&
                            selectedTargetSubjectName == subj.name
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTargetSubjectCode = subj.code.ifBlank { "SUBJ" }
                                selectedTargetSubjectName = subj.name
                                selectedTargetSubjectCategory = subj.category
                                selectedTargetSubjectCoeff = subj.coefficient
                                selectedTargetSeriesCode = subj.seriesCode.ifBlank { "ALL" }
                            },
                            label = { Text("${subj.name} (${subj.code})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("admin_pdf_target_subject_${subj.code}")
                        )
                    }
                }

                // Precise Year in Which the Past Paper Was Written
                Text(
                    text = "Precise Year the Past Paper Was Written (Past June Session):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RevisionBafendDocument.AVAILABLE_EXAM_YEARS.forEach { yr ->
                        FilterChip(
                            selected = pdfExamYear.trim() == yr,
                            onClick = { pdfExamYear = yr },
                            label = { Text("June $yr") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("admin_pdf_year_chip_$yr")
                        )
                    }
                }

                OutlinedTextField(
                    value = pdfExamYear,
                    onValueChange = { pdfExamYear = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Exam Year Written (e.g. 2024, 2023, 2022)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pdf_exam_year_input")
                )

                // Pick Past Question PDF File from Device
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf", "*/*"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("admin_pick_pdf_file_button")
                    ) {
                        Icon(imageVector = Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (pickedPdfFileName.isNotBlank()) {
                                "Selected Question PDF: $pickedPdfFileName"
                            } else {
                                "Select Past Question PDF File from Device"
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = pdfDocTitle,
                    onValueChange = { pdfDocTitle = it },
                    label = { Text("Past Question Paper Title (e.g. GCE June $pdfExamYear Paper 1 & 2)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pdf_title_input")
                )

                OutlinedTextField(
                    value = pdfDocDescription,
                    onValueChange = { pdfDocDescription = it },
                    label = { Text("Past Question Paper Structure, Instructions & Topics Covered") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pdf_description_input")
                )

                // TOGGLE BUTTON: Include Past June Solution (Manipulated as a Pair)
                Surface(
                    color = if (includePastJuneSolution) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (includePastJuneSolution) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("admin_solution_toggle_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include Past June Solution (Pair Question + Solution)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (includePastJuneSolution) {
                                        "Enabled: Upload the Past June ${pdfExamYear.ifBlank { "2024" }} solution so both are referenced and manipulated as a pair."
                                    } else {
                                        "Toggle ON to attach the official Past June solution PDF alongside this question paper."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Switch(
                                checked = includePastJuneSolution,
                                onCheckedChange = { includePastJuneSolution = it },
                                modifier = Modifier.testTag("admin_include_solution_toggle")
                            )
                        }

                        if (includePastJuneSolution) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            OutlinedButton(
                                onClick = {
                                    solutionPdfPickerLauncher.launch(arrayOf("application/pdf", "*/*"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("admin_pick_solution_pdf_button")
                            ) {
                                Icon(imageVector = Icons.Default.AttachFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (pickedSolutionPdfFileName.isNotBlank()) {
                                        "Selected Past June Solution PDF: $pickedSolutionPdfFileName"
                                    } else {
                                        "Upload Past June ${pdfExamYear.ifBlank { "2024" }} Solution PDF"
                                    }
                                )
                            }

                            OutlinedTextField(
                                value = pickedSolutionPdfFileName,
                                onValueChange = { pickedSolutionPdfFileName = it },
                                label = { Text("Past June Solution PDF File Name (Paired with Question)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_solution_pdf_name_input")
                            )

                            Text(
                                text = "Paired Reference: Question ($previewAssignedBafendName) ↔ Solution ($previewAssignedSolutionBafendName)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.testTag("admin_paired_solution_preview_text")
                            )
                        }
                    }
                }

                // Admin Drive Vault + Assigned .bafend Extension Preview Box
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_bafend_extension_preview")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Admin Drive Destination: drive://admin-drive/Past_June_Questions/${pdfExamYear.ifBlank { "2024" }}/$selectedTargetSubjectCode/$previewAssignedBafendName",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Session: June ${pdfExamYear.ifBlank { "2024" }} • Target Subject: $selectedTargetSubjectName ($selectedTargetSubjectCode)" +
                                    if (includePastJuneSolution) " • Paired with Solution ($previewAssignedSolutionBafendName)" else " • Question Only",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val cleanYear = pdfExamYear.trim().ifBlank { "2024" }
                        val finalTitle = pdfDocTitle.trim().ifBlank {
                            "$selectedTargetSubjectName ($selectedTargetSubjectCode) — Past June $cleanYear Paper"
                        }
                        val origName = pickedPdfFileName.ifBlank {
                            "${selectedTargetSubjectCode}_June_${cleanYear}_${finalTitle.take(24)}.pdf"
                        }
                        val solOrigName = if (includePastJuneSolution) {
                            pickedSolutionPdfFileName.ifBlank {
                                "${selectedTargetSubjectCode}_June_${cleanYear}_Solution.pdf"
                            }
                        } else {
                            ""
                        }
                        onUploadPdfForSubject(
                            finalTitle,
                            pdfDocDescription.trim().ifBlank {
                                "Official Past June $cleanYear examination paper for $selectedTargetSubjectName ($selectedTargetSubjectCode)."
                            },
                            pdfSystem,
                            pdfLevel,
                            pdfStream,
                            selectedTargetSeriesCode,
                            selectedTargetSubjectCode,
                            selectedTargetSubjectName,
                            selectedTargetSubjectCategory,
                            selectedTargetSubjectCoeff,
                            cleanYear,
                            origName,
                            pickedPdfBytes,
                            includePastJuneSolution,
                            solOrigName,
                            pickedSolutionPdfBytes
                        )
                        pdfDocTitle = ""
                        pdfDocDescription = ""
                        pickedPdfFileName = ""
                        pickedPdfBytes = null
                        pickedSolutionPdfFileName = ""
                        pickedSolutionPdfBytes = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("admin_upload_bafend_submit_button")
                ) {
                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (includePastJuneSolution) {
                            "Send Question + Solution Pair (June ${pdfExamYear.ifBlank { "2024" }}) to Admin Drive"
                        } else {
                            "Send Past Question PDF (June ${pdfExamYear.ifBlank { "2024" }}) to Admin Drive"
                        }
                    )
                }

                // List of Admin-Uploaded Past Papers in Admin Studio
                if (revisionDocuments.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Text(
                        text = "Admin Drive Past Papers Catalog (${revisionDocuments.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("admin_uploaded_past_papers_list")
                    ) {
                        revisionDocuments.forEach { doc ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_uploaded_doc_row_${doc.docId}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${doc.title} • ${doc.formattedExamSession}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${doc.subjectName} (${doc.subjectCode}) • Drive ID: ${doc.resolvedQuestionDriveId}" +
                                                if (doc.hasSolution) " • Paired Solution (${doc.resolvedPairId})" else " • Question Only",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onOpenBafendReader(doc.asQuestionView()) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "Preview Past Question",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteRevisionDocument(doc) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Past Paper",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
