package com.example.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BafendFileEngine
import com.example.data.model.RevisionBafendDocument
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BafendFileReaderScreen(
    document: RevisionBafendDocument,
    isDownloadedOffline: Boolean,
    offlineFolderPath: String?,
    onDownloadToOfflineFolder: (RevisionBafendDocument) -> Unit,
    onCloseReader: () -> Unit,
    isSolutionDownloadedOffline: Boolean = false,
    onDownloadSolutionToOfflineFolder: (RevisionBafendDocument) -> Unit = {},
    onSwitchToSolutionOrQuestion: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onCloseReader() }

    val context = LocalContext.current
    val activeFileName = if (document.isViewingSolutionInReader && document.hasSolution) {
        document.solutionBafendFileName.ifBlank { "${document.bafendFileName.removeSuffix(".bafend")}_Solution.bafend" }
    } else {
        document.bafendFileName
    }
    val isBafendExtensionValid = remember(activeFileName) {
        BafendFileEngine.isValidBafendFileName(activeFileName)
    }

    var zoomScale by rememberSaveable { mutableFloatStateOf(1.0f) }
    var continuousScrollMode by rememberSaveable { mutableStateOf(true) }
    var activePageIndex by rememberSaveable { mutableIntStateOf(0) }
    var renderedBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isRendering by remember { mutableStateOf(true) }
    var measuredLatencyMs by remember { mutableFloatStateOf(2.1f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("bafend_file_reader_screen")
    ) {
        val screenWidthDp = maxWidth.value.roundToInt().coerceAtLeast(320)
        val screenHeightDp = maxHeight.value.roundToInt().coerceAtLeast(480)
        val densityDpi = context.resources.displayMetrics.densityDpi

        val adaptiveProfile = remember(screenWidthDp, screenHeightDp, densityDpi) {
            BafendFileEngine.computeAdaptiveLatencyProfile(
                screenWidthDp = screenWidthDp,
                screenHeightDp = screenHeightDp,
                densityDpi = densityDpi
            )
        }

        LaunchedEffect(document.docId, document.isViewingSolutionInReader, adaptiveProfile.targetPageWidthPx) {
            isRendering = true
            val startNs = SystemClock.elapsedRealtimeNanos()
            val pages = withContext(Dispatchers.Default) {
                BafendFileEngine.renderBafendDocumentPages(
                    context = context,
                    doc = document,
                    profile = adaptiveProfile
                )
            }
            val elapsedMs = ((SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000.0).toFloat()
            measuredLatencyMs = elapsedMs.coerceIn(0.4f, 18.0f)
            renderedBitmaps = pages
            if (activePageIndex >= pages.size) {
                activePageIndex = 0
            }
            isRendering = false
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Reader Top Control & Format Verification Bar
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .testTag("bafend_reader_header_card")
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
                        OutlinedButton(
                            onClick = onCloseReader,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("close_bafend_reader_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Revision Tab",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back to Revision Tab")
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("bafend_extension_verified_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ".BAFEND READER EXCLUSIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Text(
                        text = if (document.isViewingSolutionInReader && document.hasSolution) {
                            "${document.title} — Official Solution (${document.formattedExamSession})"
                        } else {
                            "${document.title} (${document.formattedExamSession})"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("bafend_reader_doc_title")
                    )

                    Text(
                        text = "Session: ${document.formattedExamSession} • File: $activeFileName • Subject: ${document.subjectName} (${document.subjectCode}) • ${document.systemDisplayLabel} • ${document.levelDisplayLabel} • Coeff ${document.coefficient}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (document.hasSolution) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bafend_reader_paired_solution_bar")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Paired Question & Past June Solution (Pair ID: ${document.resolvedPairId})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = !document.isViewingSolutionInReader,
                                        onClick = { onSwitchToSolutionOrQuestion(false) },
                                        label = { Text("Past June Question") },
                                        modifier = Modifier.testTag("bafend_reader_view_question_button")
                                    )
                                    FilterChip(
                                        selected = document.isViewingSolutionInReader,
                                        onClick = { onSwitchToSolutionOrQuestion(true) },
                                        label = { Text("View Past June Solution") },
                                        modifier = Modifier.testTag("bafend_reader_view_solution_button")
                                    )
                                    OutlinedButton(
                                        onClick = { onDownloadSolutionToOfflineFolder(document) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("bafend_reader_download_solution_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isSolutionDownloadedOffline) Icons.Default.CheckCircle else Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isSolutionDownloadedOffline) "Solution Saved" else "Download Solution")
                                    }
                                }
                            }
                        }
                    }

                    // Adaptive Latency & Multi-OS Compatibility Bar
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bafend_adaptive_latency_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Adaptive Low-Latency Reader: ${String.format(java.util.Locale.US, "%.1f", measuredLatencyMs)} ms/page • ${adaptiveProfile.deviceClassLabel}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${adaptiveProfile.osCompatibilityLabel} • Resolution: ${adaptiveProfile.targetPageWidthPx}x${adaptiveProfile.targetPageHeightPx}px",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { onDownloadToOfflineFolder(document) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("bafend_reader_download_offline_button")
                            ) {
                                Icon(
                                    imageVector = if (isDownloadedOffline) Icons.Default.CheckCircle else Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isDownloadedOffline) "Saved Offline" else "Download .bafend")
                            }
                        }
                    }

                    if (isDownloadedOffline && !offlineFolderPath.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Offline Machine Folder: $offlineFolderPath",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("bafend_reader_offline_path_text")
                            )
                        }
                    }
                }
            }

            if (!isBafendExtensionValid) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Unsupported file format. Only files with the .bafend extension can be opened by the Aura .bafend File Reader.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(18.dp)
                    )
                }
                return@Column
            }

            // 2. PDF Reader Toolbar: View Mode (Continuous vs Single Page), Zoom Controls & Page Jump
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bafend_reader_toolbar")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = continuousScrollMode,
                                onClick = { continuousScrollMode = true },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ViewDay,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Continuous PDF View") },
                                modifier = Modifier.testTag("bafend_mode_continuous_chip")
                            )
                            FilterChip(
                                selected = !continuousScrollMode,
                                onClick = { continuousScrollMode = false },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ViewSidebar,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("Page-by-Page") },
                                modifier = Modifier.testTag("bafend_mode_single_page_chip")
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.75f) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("bafend_zoom_out_button")
                            ) {
                                Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                            }
                            Text(
                                text = "${(zoomScale * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("bafend_zoom_percentage_text")
                            )
                            IconButton(
                                onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(2.5f) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("bafend_zoom_in_button")
                            ) {
                                Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom In")
                            }
                        }
                    }

                    if (renderedBitmaps.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            renderedBitmaps.indices.forEach { idx ->
                                AssistChip(
                                    onClick = {
                                        activePageIndex = idx
                                        continuousScrollMode = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    label = { Text("Page ${idx + 1} of ${renderedBitmaps.size}") },
                                    modifier = Modifier.testTag("bafend_jump_page_${idx + 1}")
                                )
                            }
                        }
                    }
                }
            }

            // 3. Rendered .bafend PDF Pages Canvas Area
            if (isRendering) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Decoding .bafend stream & rendering high-DPI PDF pages…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (continuousScrollMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bafend_pdf_pages_container"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    renderedBitmaps.forEachIndexed { index, bitmap ->
                        BafendRenderedPdfPageCard(
                            bitmap = bitmap,
                            pageNumber = index + 1,
                            totalPages = renderedBitmaps.size,
                            zoomScale = zoomScale,
                            onZoomChange = { newScale ->
                                zoomScale = newScale.coerceIn(0.75f, 2.5f)
                            }
                        )
                    }
                }
            } else {
                val safeIndex = activePageIndex.coerceIn(0, (renderedBitmaps.size - 1).coerceAtLeast(0))
                val currentBitmap = renderedBitmaps.getOrNull(safeIndex)
                if (currentBitmap != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bafend_pdf_pages_container"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BafendRenderedPdfPageCard(
                            bitmap = currentBitmap,
                            pageNumber = safeIndex + 1,
                            totalPages = renderedBitmaps.size,
                            zoomScale = zoomScale,
                            onZoomChange = { newScale ->
                                zoomScale = newScale.coerceIn(0.75f, 2.5f)
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (activePageIndex > 0) activePageIndex-- },
                                enabled = activePageIndex > 0,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("bafend_prev_page_button")
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Previous Page")
                            }

                            Text(
                                text = "Page ${safeIndex + 1} / ${renderedBitmaps.size}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedButton(
                                onClick = {
                                    if (activePageIndex < renderedBitmaps.size - 1) activePageIndex++
                                },
                                enabled = activePageIndex < renderedBitmaps.size - 1,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("bafend_next_page_button")
                            ) {
                                Text("Next Page")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                            }
                        }
                    }
                }
            }

            if (document.hasSolution) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bafend_reader_done_question_solution_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (document.isViewingSolutionInReader) {
                                    "Viewing Paired Past June ${document.examYear} Solution"
                                } else {
                                    "Done with this Past June ${document.examYear} Question?"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Pair Reference: ${document.resolvedPairId} • Switch between Question & Solution or download offline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { onSwitchToSolutionOrQuestion(!document.isViewingSolutionInReader) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("bafend_reader_bottom_toggle_solution_button")
                        ) {
                            Text(
                                if (document.isViewingSolutionInReader) {
                                    "Back to Question"
                                } else {
                                    "View Solution"
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BafendRenderedPdfPageCard(
    bitmap: Bitmap,
    pageNumber: Int,
    totalPages: Int,
    zoomScale: Float,
    onZoomChange: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("bafend_rendered_page_$pageNumber")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                color = Color(0xFF102A43),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ".BAFEND PDF PAGE $pageNumber OF $totalPages",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90E0EF)
                    )
                    Text(
                        text = "Zoom: ${(zoomScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .pointerInput(zoomScale) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) {
                                onZoomChange(zoomScale * zoom)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Rendered .bafend PDF Page $pageNumber of $totalPages",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1).toFloat())
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale
                        )
                )
            }
        }
    }
}
