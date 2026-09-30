package com.example.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import android.util.Base64
import com.example.data.model.AdminCustomSubject
import com.example.data.model.BafendFileEngine
import com.example.data.model.CurriculumCatalog
import com.example.data.model.EducationSystem
import com.example.data.model.RevisionBafendDocument
import com.example.data.model.StudentLevel
import com.example.data.model.StudentStream
import com.example.data.model.UserProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RevisionHubScreen(
    currentProfile: UserProfile?,
    isAdmin: Boolean,
    revisionDocuments: List<RevisionBafendDocument>,
    adminCustomSubjects: List<AdminCustomSubject>,
    activeReaderDocument: RevisionBafendDocument?,
    statusBannerMessage: String?,
    onDismissBanner: () -> Unit,
    onOpenBafendReader: (RevisionBafendDocument) -> Unit,
    onCloseBafendReader: () -> Unit,
    onDownloadBafendOffline: (RevisionBafendDocument) -> Unit,
    onOpenAdminCurriculumStudio: () -> Unit,
    onDeleteRevisionDocument: (RevisionBafendDocument) -> Unit = {},
    onDownloadSolutionBafendOffline: (RevisionBafendDocument) -> Unit = {},
    onSwitchReaderQuestionOrSolution: (Boolean) -> Unit = {},
    onDeleteLocalDownloadedPaper: (RevisionBafendDocument) -> Unit = {},
    initialSubTab: String = "CATALOG",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var offlineRefreshTick by remember { mutableIntStateOf(0) }
    var localFileError by rememberSaveable { mutableStateOf<String?>(null) }
    var activeSubTab by rememberSaveable(initialSubTab) { mutableStateOf(initialSubTab) }

    // If a .bafend document is currently open in the dedicated .bafend File Reader, display it full-screen
    if (activeReaderDocument != null) {
        val isDownloaded = remember(activeReaderDocument.docId, offlineRefreshTick) {
            BafendFileEngine.isDocumentDownloadedOffline(context, activeReaderDocument)
        }
        val isSolDownloaded = remember(activeReaderDocument.docId, offlineRefreshTick) {
            BafendFileEngine.isSolutionDownloadedOffline(context, activeReaderDocument)
        }
        val offlineFile = remember(activeReaderDocument.docId, activeReaderDocument.isViewingSolutionInReader, offlineRefreshTick) {
            BafendFileEngine.getOfflineFileForDocument(context, activeReaderDocument)
        }
        BafendFileReaderScreen(
            document = activeReaderDocument,
            isDownloadedOffline = isDownloaded,
            offlineFolderPath = offlineFile.parentFile?.absolutePath,
            onDownloadToOfflineFolder = { doc ->
                onDownloadBafendOffline(doc)
                offlineRefreshTick++
            },
            onCloseReader = onCloseBafendReader,
            isSolutionDownloadedOffline = isSolDownloaded,
            onDownloadSolutionToOfflineFolder = { doc ->
                onDownloadSolutionBafendOffline(doc)
                offlineRefreshTick++
            },
            onSwitchToSolutionOrQuestion = onSwitchReaderQuestionOrSolution,
            modifier = modifier
        )
        return
    }

    // Launcher to open an existing .bafend file from the user's machine storage
    val openExternalBafendLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                var displayName = "external_revision.bafend"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                }
                if (!BafendFileEngine.isValidBafendFileName(displayName)) {
                    localFileError = "Only files with the .bafend extension can be opened in the Aura .bafend File Reader (Selected: $displayName)."
                } else {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        localFileError = null
                        val externalDoc = RevisionBafendDocument(
                            docId = "external_${displayName.hashCode()}",
                            title = displayName.removeSuffix(".bafend").replace("_", " "),
                            description = "Opened directly from local machine storage ($displayName).",
                            systemCode = currentProfile?.educationSystem?.ifBlank { "ANGLOPHONE" } ?: "ANGLOPHONE",
                            levelCode = currentProfile?.studentLevel?.ifBlank { "ADVANCED_LEVEL" } ?: "ADVANCED_LEVEL",
                            streamCode = currentProfile?.studentStream?.ifBlank { "SCIENCE" } ?: "SCIENCE",
                            subjectCode = "LOCAL",
                            subjectName = "Local .bafend File",
                            originalPdfName = displayName,
                            bafendFileName = BafendFileEngine.ensureBafendExtension(displayName),
                            fileSizeBytes = bytes.size.toLong(),
                            bafendBase64Payload = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        )
                        onOpenBafendReader(externalDoc)
                    }
                }
            }.onFailure { err ->
                localFileError = "Could not read .bafend file: ${err.localizedMessage ?: "File error"}"
            }
        }
    }

    var selectedSubsystem by rememberSaveable { mutableStateOf("ALL") }
    var selectedLevel by rememberSaveable { mutableStateOf("ALL") }
    var selectedStream by rememberSaveable { mutableStateOf("ALL") }
    var selectedExamYear by rememberSaveable { mutableStateOf("ALL") }
    var selectedSubjectFilter by rememberSaveable { mutableStateOf("ALL") }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Dedicated Offline Search & Filter state for the Local Tab
    var localSearchQuery by rememberSaveable { mutableStateOf("") }
    var localSubsystemFilter by rememberSaveable { mutableStateOf("ALL") }
    var localYearFilter by rememberSaveable { mutableStateOf("ALL") }
    var localPairedOnlyFilter by rememberSaveable { mutableStateOf(false) }

    val availableSubjectsInCatalog = remember(
        selectedSubsystem,
        selectedLevel,
        selectedStream,
        adminCustomSubjects
    ) {
        CurriculumCatalog.getSubjectsFor(
            systemCode = if (selectedSubsystem == "ALL") "" else selectedSubsystem,
            levelCode = if (selectedLevel == "ALL") "" else selectedLevel,
            streamCode = if (selectedStream == "ALL") "" else selectedStream
        )
    }

    val filteredDocuments = remember(
        revisionDocuments,
        selectedSubsystem,
        selectedLevel,
        selectedStream,
        selectedExamYear,
        selectedSubjectFilter,
        searchQuery
    ) {
        val q = searchQuery.trim().lowercase()
        revisionDocuments.filter { doc ->
            val sysMatch = selectedSubsystem == "ALL" ||
                doc.systemCode.equals(selectedSubsystem, ignoreCase = true) ||
                doc.systemCode.equals("BILINGUAL", ignoreCase = true)
            val lvlMatch = selectedLevel == "ALL" ||
                doc.levelCode.equals(selectedLevel, ignoreCase = true)
            val strMatch = selectedStream == "ALL" ||
                doc.streamCode.equals(selectedStream, ignoreCase = true)
            val yrMatch = selectedExamYear == "ALL" ||
                doc.examYear.equals(selectedExamYear, ignoreCase = true)
            val subjMatch = selectedSubjectFilter == "ALL" ||
                doc.subjectCode.equals(selectedSubjectFilter, ignoreCase = true) ||
                doc.subjectName.equals(selectedSubjectFilter, ignoreCase = true)
            val queryMatch = q.isEmpty() ||
                doc.title.lowercase().contains(q) ||
                doc.subjectName.lowercase().contains(q) ||
                doc.subjectCode.lowercase().contains(q) ||
                doc.examYear.lowercase().contains(q) ||
                doc.formattedExamSession.lowercase().contains(q) ||
                doc.bafendFileName.lowercase().contains(q) ||
                doc.solutionBafendFileName.lowercase().contains(q) ||
                doc.resolvedPairId.lowercase().contains(q) ||
                doc.description.lowercase().contains(q) ||
                doc.seriesCode.lowercase().contains(q)
            sysMatch && lvlMatch && strMatch && yrMatch && subjMatch && queryMatch
        }
    }

    val groupedBySubject = remember(filteredDocuments) {
        filteredDocuments.groupBy { "${it.subjectName} (${it.subjectCode})" }
    }

    val downloadedPapersList = remember(revisionDocuments, offlineRefreshTick) {
        revisionDocuments.filter { doc ->
            BafendFileEngine.isDocumentDownloadedOffline(context, doc) ||
                BafendFileEngine.isSolutionDownloadedOffline(context, doc)
        }
    }

    val filteredLocalDownloadedPapers = remember(
        downloadedPapersList,
        localSearchQuery,
        localSubsystemFilter,
        localYearFilter,
        localPairedOnlyFilter
    ) {
        val q = localSearchQuery.trim().lowercase()
        downloadedPapersList.filter { doc ->
            val sysOk = localSubsystemFilter == "ALL" ||
                doc.systemCode.equals(localSubsystemFilter, ignoreCase = true) ||
                doc.systemCode.equals("BILINGUAL", ignoreCase = true)
            val yrOk = localYearFilter == "ALL" ||
                doc.examYear.equals(localYearFilter, ignoreCase = true)
            val pairOk = !localPairedOnlyFilter || doc.hasSolution
            val searchOk = q.isEmpty() ||
                doc.title.lowercase().contains(q) ||
                doc.subjectName.lowercase().contains(q) ||
                doc.subjectCode.lowercase().contains(q) ||
                doc.examYear.lowercase().contains(q) ||
                doc.formattedExamSession.lowercase().contains(q) ||
                doc.bafendFileName.lowercase().contains(q) ||
                doc.solutionBafendFileName.lowercase().contains(q) ||
                doc.resolvedPairId.lowercase().contains(q) ||
                doc.seriesCode.lowercase().contains(q)
            sysOk && yrOk && pairOk && searchOk
        }
    }

    val downloadedFilesList = remember(revisionDocuments, offlineRefreshTick) {
        BafendFileEngine.listAllDownloadedBafendFiles(context)
    }
    val offlineRootPath = remember {
        BafendFileEngine.getOfflineRootDirectory(context).absolutePath
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("revision_hub_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Banner
        AnimatedVisibility(visible = statusBannerMessage != null || localFileError != null) {
            Surface(
                color = if (localFileError != null) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        localFileError = null
                        onDismissBanner()
                    }
                    .testTag("revision_hub_status_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (localFileError != null) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = localFileError ?: statusBannerMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (localFileError != null) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }
        }

        // 1. Revision Hub Hero Card + Offline Folder Status
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(26.dp)
                )
                .testTag("revision_hub_hero_card")
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Revision Hub",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Past June Papers & Local Offline Vault (.bafend)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${revisionDocuments.size} Admin Drive Paper(s) • ${downloadedPapersList.size} Paper(s) in Local Tab (${downloadedFilesList.size} files)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text = "Admin-uploaded Past June Question Papers and their Paired Solutions are stored in the Admin Drive and collected to your phone on download. Switch to the Local Tab anytime—even when offline—to search and read your downloaded papers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sub-Tab Switcher: Past Papers Catalog vs Local Tab (Offline Downloaded Papers)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = activeSubTab == "CATALOG",
                        onClick = { activeSubTab = "CATALOG" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text("Past Papers Catalog (${revisionDocuments.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("revision_subtab_online_catalog")
                    )

                    FilterChip(
                        selected = activeSubTab == "LOCAL_DOWNLOADS",
                        onClick = { activeSubTab = "LOCAL_DOWNLOADS" },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text("Local Tab • Offline (${downloadedPapersList.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("revision_subtab_local_downloads")
                    )
                }

                // Offline Machine Storage Folder Info Box
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offline_machine_folder_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Offline Machine Folder (${downloadedFilesList.size} .bafend files saved)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Path: $offlineRootPath/<Subsystem>/<Level>/<Subject>/",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { openExternalBafendLauncher.launch(arrayOf("*/*")) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("open_external_bafend_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Local .bafend File")
                    }

                    if (isAdmin) {
                        Button(
                            onClick = onOpenAdminCurriculumStudio,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("revision_hub_admin_upload_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Admin: Add Subject / PDF")
                        }
                    }
                }
            }
        }

        if (activeSubTab == "LOCAL_DOWNLOADS") {
            LocalDownloadedPapersTabSection(
                downloadedPapers = downloadedPapersList,
                filteredLocalPapers = filteredLocalDownloadedPapers,
                localSearchQuery = localSearchQuery,
                onLocalSearchQueryChange = { localSearchQuery = it },
                localSubsystemFilter = localSubsystemFilter,
                onLocalSubsystemFilterChange = { localSubsystemFilter = it },
                localYearFilter = localYearFilter,
                onLocalYearFilterChange = { localYearFilter = it },
                localPairedOnlyFilter = localPairedOnlyFilter,
                onLocalPairedOnlyFilterChange = { localPairedOnlyFilter = it },
                offlineRootPath = offlineRootPath,
                offlineRefreshTick = offlineRefreshTick,
                onOpenQuestionInReader = { doc -> onOpenBafendReader(doc.asQuestionView()) },
                onOpenSolutionInReader = { doc -> onOpenBafendReader(doc.asSolutionView()) },
                onDownloadQuestionOffline = { doc ->
                    onDownloadBafendOffline(doc)
                    offlineRefreshTick++
                },
                onDownloadSolutionOffline = { doc ->
                    onDownloadSolutionBafendOffline(doc)
                    offlineRefreshTick++
                },
                onDeleteLocalPaper = { doc ->
                    onDeleteLocalDownloadedPaper(doc)
                    offlineRefreshTick++
                },
                onSwitchToCatalogTab = { activeSubTab = "CATALOG" }
            )
            Spacer(modifier = Modifier.height(24.dp))
            return@Column
        }

        // 2. Multi-Level Organized Filters (Subsystem -> Level -> Stream -> Exam Year -> Subject)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("revision_hub_filters_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Filter Past June Papers by Subsystem, Level, Year & Subject",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by subject, code (0770), year (2024), pair ID, or .bafend filename…") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("revision_hub_search_input")
                )

                // Subsystem Filter Row
                Text(
                    text = "Educational Subsystem:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "All Subsystems",
                        EducationSystem.ANGLOPHONE.code to "Anglophone (GCE)",
                        EducationSystem.FRANCOPHONE.code to "Francophone (OBC)",
                        EducationSystem.BILINGUAL.code to "Bilingual"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = selectedSubsystem == code,
                            onClick = {
                                selectedSubsystem = code
                                selectedSubjectFilter = "ALL"
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("revision_filter_subsystem_$code")
                        )
                    }
                }

                // Level & Stream Filter Row
                Text(
                    text = "Academic Level & Stream:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "All Levels",
                        StudentLevel.ORDINARY_LEVEL.code to "Ordinary Level / 1er Cycle",
                        StudentLevel.ADVANCED_LEVEL.code to "Advanced Level / 2nd Cycle"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = selectedLevel == code,
                            onClick = {
                                selectedLevel = code
                                selectedSubjectFilter = "ALL"
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("revision_filter_level_$code")
                        )
                    }

                    listOf(
                        "ALL" to "All Streams",
                        StudentStream.SCIENCE.code to "Science",
                        StudentStream.ART.code to "Arts / Literature"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = selectedStream == code,
                            onClick = {
                                selectedStream = code
                                selectedSubjectFilter = "ALL"
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("revision_filter_stream_$code")
                        )
                    }
                }

                // Exam Year Filter Row
                val distinctYearsInDocs = remember(revisionDocuments) {
                    (revisionDocuments.map { it.examYear.ifBlank { "2024" } } + listOf("2025", "2024", "2023", "2022"))
                        .distinct()
                        .sortedDescending()
                }
                Text(
                    text = "Exam Year (Past June Session):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedExamYear == "ALL",
                        onClick = { selectedExamYear = "ALL" },
                        label = { Text("All Years") },
                        modifier = Modifier.testTag("revision_filter_year_ALL")
                    )
                    distinctYearsInDocs.forEach { yr ->
                        FilterChip(
                            selected = selectedExamYear == yr,
                            onClick = { selectedExamYear = yr },
                            label = { Text("June $yr") },
                            modifier = Modifier.testTag("revision_filter_year_$yr")
                        )
                    }
                }

                // Subject Filter Chips (includes Admin-added subjects + subjects with PDFs)
                val distinctSubjectChips = remember(revisionDocuments, availableSubjectsInCatalog, selectedSubsystem) {
                    val fromDocs = revisionDocuments
                        .filter {
                            selectedSubsystem == "ALL" ||
                                it.systemCode.equals(selectedSubsystem, ignoreCase = true) ||
                                it.systemCode.equals("BILINGUAL", ignoreCase = true)
                        }
                        .map { it.subjectCode to "${it.subjectName} (${it.subjectCode})" }
                    val fromAdmin = adminCustomSubjects
                        .filter {
                            selectedSubsystem == "ALL" ||
                                it.systemCode.equals(selectedSubsystem, ignoreCase = true) ||
                                it.systemCode.equals("BILINGUAL", ignoreCase = true)
                        }
                        .map { it.code to "${it.name} (${it.code})" }
                    (fromDocs + fromAdmin).distinctBy { it.first }
                }

                if (distinctSubjectChips.isNotEmpty()) {
                    Text(
                        text = "Browse by Subject:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedSubjectFilter == "ALL",
                            onClick = { selectedSubjectFilter = "ALL" },
                            label = { Text("All Subjects (${filteredDocuments.size})") },
                            modifier = Modifier.testTag("revision_filter_subject_ALL")
                        )
                        distinctSubjectChips.forEach { (subjCode, subjLabel) ->
                            FilterChip(
                                selected = selectedSubjectFilter == subjCode,
                                onClick = { selectedSubjectFilter = subjCode },
                                label = { Text(subjLabel) },
                                modifier = Modifier.testTag("revision_filter_subject_$subjCode")
                            )
                        }
                    }
                }
            }
        }

        // 3. Custom Subjects Added by Admin Banner (so users can see newly added subjects & their characteristics)
        if (adminCustomSubjects.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("revision_hub_admin_subjects_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Platform Subjects Added by Admin (${adminCustomSubjects.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        adminCustomSubjects.forEach { subj ->
                            AssistChip(
                                onClick = { selectedSubjectFilter = subj.code },
                                label = {
                                    Text("${subj.name} (${subj.code}) • ${subj.systemCode} • Coeff ${subj.coefficient} • ${subj.category}")
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Organized Subject Sections & .bafend Document Cards
        if (groupedBySubject.isEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("revision_empty_catalog_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (revisionDocuments.isEmpty()) {
                            "No Past Question Papers Uploaded Yet"
                        } else {
                            "No .bafend past papers match the current filter."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (revisionDocuments.isEmpty()) {
                            "All mock papers have been removed. Past question papers and paired Past June solutions appear here as soon as the Administrator uploads them to the Admin Drive."
                        } else {
                            "Try selecting 'All Subsystems', 'All Years', or 'All Subjects', or use the Admin PDF Upload tool to publish a new Past Paper."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenAdminCurriculumStudio,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("empty_catalog_admin_upload_button")
                        ) {
                            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Admin: Upload Past Question Paper")
                        }
                    }
                }
            }
        } else {
            groupedBySubject.forEach { (subjectGroupHeader, docsInSubject) ->
                val sampleDoc = docsInSubject.first()
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
                        .testTag("revision_subject_group_${sampleDoc.subjectCode}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Subject Group Header with Subject Characteristics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subjectGroupHeader,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${sampleDoc.systemDisplayLabel} • ${sampleDoc.levelDisplayLabel} • ${sampleDoc.streamDisplayLabel} • Series: ${sampleDoc.seriesCode} • Category: ${sampleDoc.subjectCategory} • Coeff: ${sampleDoc.coefficient}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${docsInSubject.size} Past Paper(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        docsInSubject.forEach { doc ->
                            val isOfflineReady = remember(doc.docId, offlineRefreshTick) {
                                BafendFileEngine.isDocumentDownloadedOffline(context, doc)
                            }
                            val isSolOfflineReady = remember(doc.docId, offlineRefreshTick) {
                                BafendFileEngine.isSolutionDownloadedOffline(context, doc)
                            }
                            val offlineFile = remember(doc.docId, offlineRefreshTick) {
                                BafendFileEngine.getOfflineFileForDocument(context, doc)
                            }

                            RevisionBafendDocumentRowCard(
                                doc = doc,
                                isOfflineReady = isOfflineReady,
                                isSolutionOfflineReady = isSolOfflineReady,
                                offlineFolderPath = offlineFile.parentFile?.absolutePath.orEmpty(),
                                isAdmin = isAdmin,
                                onOpenInReader = { onOpenBafendReader(doc.asQuestionView()) },
                                onOpenSolutionInReader = { onOpenBafendReader(doc.asSolutionView()) },
                                onDownloadOffline = {
                                    onDownloadBafendOffline(doc)
                                    offlineRefreshTick++
                                },
                                onDownloadSolutionOffline = {
                                    onDownloadSolutionBafendOffline(doc)
                                    offlineRefreshTick++
                                },
                                onDelete = { onDeleteRevisionDocument(doc) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocalDownloadedPapersTabSection(
    downloadedPapers: List<RevisionBafendDocument>,
    filteredLocalPapers: List<RevisionBafendDocument>,
    localSearchQuery: String,
    onLocalSearchQueryChange: (String) -> Unit,
    localSubsystemFilter: String,
    onLocalSubsystemFilterChange: (String) -> Unit,
    localYearFilter: String,
    onLocalYearFilterChange: (String) -> Unit,
    localPairedOnlyFilter: Boolean,
    onLocalPairedOnlyFilterChange: (Boolean) -> Unit,
    offlineRootPath: String,
    offlineRefreshTick: Int,
    onOpenQuestionInReader: (RevisionBafendDocument) -> Unit,
    onOpenSolutionInReader: (RevisionBafendDocument) -> Unit,
    onDownloadQuestionOffline: (RevisionBafendDocument) -> Unit,
    onDownloadSolutionOffline: (RevisionBafendDocument) -> Unit,
    onDeleteLocalPaper: (RevisionBafendDocument) -> Unit,
    onSwitchToCatalogTab: () -> Unit
) {
    val context = LocalContext.current
    val distinctDownloadedYears = remember(downloadedPapers) {
        downloadedPapers.map { it.examYear.ifBlank { "2024" } }.distinct().sortedDescending()
    }
    val groupedLocalBySubject = remember(filteredLocalPapers) {
        filteredLocalPapers.groupBy { "${it.subjectName} (${it.subjectCode})" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("local_downloads_tab_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Offline Search & Organization Card for Local Tab
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(22.dp)
                )
                .testTag("local_downloads_search_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Local Tab — Offline Downloaded Past Papers (${downloadedPapers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "100% Offline Ready • Search and read your downloaded Past June Questions & Paired Solutions without internet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = localSearchQuery,
                    onValueChange = onLocalSearchQueryChange,
                    label = { Text("Search Local Tab offline (subject, code, June year, pair ID, or filename)…") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (localSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onLocalSearchQueryChange("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("local_downloads_search_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "All Subsystems",
                        EducationSystem.ANGLOPHONE.code to "Anglophone",
                        EducationSystem.FRANCOPHONE.code to "Francophone"
                    ).forEach { (sysCode, label) ->
                        FilterChip(
                            selected = localSubsystemFilter == sysCode,
                            onClick = { onLocalSubsystemFilterChange(sysCode) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("local_filter_subsystem_$sysCode")
                        )
                    }

                    FilterChip(
                        selected = localPairedOnlyFilter,
                        onClick = { onLocalPairedOnlyFilterChange(!localPairedOnlyFilter) },
                        label = { Text("Paired Question + Solution Only") },
                        modifier = Modifier.testTag("local_filter_paired_only_chip")
                    )
                }

                if (distinctDownloadedYears.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = localYearFilter == "ALL",
                            onClick = { onLocalYearFilterChange("ALL") },
                            label = { Text("All Years") },
                            modifier = Modifier.testTag("local_filter_year_ALL")
                        )
                        distinctDownloadedYears.forEach { yr ->
                            FilterChip(
                                selected = localYearFilter == yr,
                                onClick = { onLocalYearFilterChange(yr) },
                                label = { Text("June $yr") },
                                modifier = Modifier.testTag("local_filter_year_$yr")
                            )
                        }
                    }
                }
            }
        }

        if (filteredLocalPapers.isEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("local_downloads_empty_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (downloadedPapers.isEmpty()) {
                            "No Downloaded Past Papers in Local Tab Yet"
                        } else {
                            "No downloaded papers match your offline search"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (downloadedPapers.isEmpty()) {
                            "Collect any Past June Question Paper (or Paired Solution) from the Past Papers Catalog to save it in your phone's Local Tab ($offlineRootPath) for offline reading."
                        } else {
                            "Clear the offline search or year filter to see all ${downloadedPapers.size} downloaded paper(s)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onSwitchToCatalogTab,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("local_empty_go_to_catalog_button")
                    ) {
                        Text("Browse Past Papers Catalog")
                    }
                }
            }
        } else {
            groupedLocalBySubject.forEach { (subjectHeader, docsInSubject) ->
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(22.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subjectHeader,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${docsInSubject.size} Offline Paper(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        docsInSubject.forEach { doc ->
                            val isQDownloaded = remember(doc.docId, offlineRefreshTick) {
                                BafendFileEngine.isDocumentDownloadedOffline(context, doc)
                            }
                            val isSolDownloaded = remember(doc.docId, offlineRefreshTick) {
                                BafendFileEngine.isSolutionDownloadedOffline(context, doc)
                            }
                            val offlineFile = remember(doc.docId, offlineRefreshTick) {
                                BafendFileEngine.getOfflineFileForDocument(context, doc.asQuestionView())
                            }

                            RevisionBafendDocumentRowCard(
                                doc = doc,
                                isOfflineReady = isQDownloaded,
                                isSolutionOfflineReady = isSolDownloaded,
                                offlineFolderPath = offlineFile.parentFile?.absolutePath.orEmpty(),
                                isAdmin = false,
                                isLocalTabCard = true,
                                onOpenInReader = { onOpenQuestionInReader(doc) },
                                onOpenSolutionInReader = { onOpenSolutionInReader(doc) },
                                onDownloadOffline = { onDownloadQuestionOffline(doc) },
                                onDownloadSolutionOffline = { onDownloadSolutionOffline(doc) },
                                onDelete = { onDeleteLocalPaper(doc) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevisionBafendDocumentRowCard(
    doc: RevisionBafendDocument,
    isOfflineReady: Boolean,
    isSolutionOfflineReady: Boolean = false,
    offlineFolderPath: String,
    isAdmin: Boolean,
    isLocalTabCard: Boolean = false,
    onOpenInReader: () -> Unit,
    onOpenSolutionInReader: () -> Unit = {},
    onDownloadOffline: () -> Unit,
    onDownloadSolutionOffline: () -> Unit = {},
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isOfflineReady || isSolutionOfflineReady) {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("revision_doc_card_${doc.docId}")
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
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${doc.title} (${doc.formattedExamSession})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = doc.bafendFileName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("revision_doc_filename_${doc.docId}")
                            )
                        }
                    }
                }

                Surface(
                    color = if (isOfflineReady) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("revision_doc_status_badge_${doc.docId}")
                ) {
                    Text(
                        text = if (isOfflineReady) "IN LOCAL TAB (${doc.formattedExamSession})" else "ADMIN DRIVE • ${doc.formattedExamSession}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOfflineReady) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (doc.description.isNotBlank()) {
                Text(
                    text = doc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Written: ${doc.formattedExamSession} • Drive ID: ${doc.resolvedQuestionDriveId} • Size: ${doc.formattedFileSize} • Subsystem: ${doc.systemCode} • Level: ${doc.levelCode} • Coeff: ${doc.coefficient}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Paired Past June Solution Badge & Controls ("referenced and manipulated par paires")
            if (doc.hasSolution) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("revision_doc_paired_solution_box_${doc.docId}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Paired Past June Solution Included • Pair ID: ${doc.resolvedPairId}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Solution File: ${doc.solutionBafendFileName.ifBlank { "${doc.bafendFileName.removeSuffix(".bafend")}_Solution.bafend" }} • Admin Drive ID: ${doc.resolvedSolutionDriveId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenSolutionInReader,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_solution_reader_button_${doc.docId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View Past June Solution")
                            }

                            OutlinedButton(
                                onClick = onDownloadSolutionOffline,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("download_solution_button_${doc.docId}")
                            ) {
                                Icon(
                                    imageVector = if (isSolutionOfflineReady) Icons.Default.CheckCircle else Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isSolutionOfflineReady) "Solution in Local Tab" else "Download Solution")
                            }
                        }
                    }
                }
            }

            if ((isOfflineReady || isSolutionOfflineReady) && offlineFolderPath.isNotBlank()) {
                Text(
                    text = "Local Phone Storage: $offlineFolderPath/${doc.bafendFileName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("revision_doc_offline_path_${doc.docId}")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenInReader,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("open_bafend_reader_button_${doc.docId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Read Past Question")
                }

                OutlinedButton(
                    onClick = onDownloadOffline,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("download_bafend_button_${doc.docId}")
                ) {
                    Icon(
                        imageVector = if (isOfflineReady) Icons.Default.CheckCircle else Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isOfflineReady) "Saved in Local Tab" else "Download Question")
                }

                if (isLocalTabCard || (isAdmin && !doc.docId.startsWith("seed_"))) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag(if (isLocalTabCard) "local_delete_downloaded_button_${doc.docId}" else "delete_bafend_doc_${doc.docId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete document",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
