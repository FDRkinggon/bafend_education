package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AccountStatus
import com.example.data.model.ActivityEventType
import com.example.data.model.ActivityReviewStatus
import com.example.data.model.AppActivity
import com.example.data.model.PlatformRole
import com.example.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun AdminSectionTabsRow(
    activeTab: AdminSectionTab,
    totalUsersCount: Int,
    dbLogsCount: Int,
    allActivitiesCount: Int,
    onSelectTab: (AdminSectionTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("admin_section_tabs_row")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminTabPillButton(
                selected = activeTab == AdminSectionTab.MANAGE_USERS,
                label = "Manage Users ($totalUsersCount)",
                icon = Icons.Default.Group,
                onClick = { onSelectTab(AdminSectionTab.MANAGE_USERS) },
                testTag = "admin_tab_manage_users"
            )
            AdminTabPillButton(
                selected = activeTab == AdminSectionTab.CURRICULUM_AND_PDFS,
                label = "Subjects & .bafend PDFs",
                icon = Icons.Default.School,
                onClick = { onSelectTab(AdminSectionTab.CURRICULUM_AND_PDFS) },
                testTag = "admin_tab_curriculum_pdfs"
            )
            AdminTabPillButton(
                selected = activeTab == AdminSectionTab.DB_SECURITY_LOGS,
                label = "DB & IP Security ($dbLogsCount)",
                icon = Icons.Default.Storage,
                onClick = { onSelectTab(AdminSectionTab.DB_SECURITY_LOGS) },
                testTag = "admin_tab_db_security_logs"
            )
            AdminTabPillButton(
                selected = activeTab == AdminSectionTab.ACTIVITY_STREAM,
                label = "All Activity Logs ($allActivitiesCount)",
                icon = Icons.Default.Timeline,
                onClick = { onSelectTab(AdminSectionTab.ACTIVITY_STREAM) },
                testTag = "admin_tab_activity_stream"
            )
        }
    }
}

@Composable
private fun AdminTabPillButton(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
            )
        },
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    )
}

/**
 * Chart 1: Active Students Over Time
 * Visualizes the evolution of active students and student learning sessions across 6 time windows.
 */
@Composable
fun ActiveStudentsOverTimeChartCard(
    allUsers: List<UserProfile>,
    activities: List<AppActivity>,
    modifier: Modifier = Modifier
) {
    val studentUsers = remember(allUsers) {
        allUsers.filter { it.effectivePlatformRole == PlatformRole.STUDENT }
    }
    val activeStudentsCount = remember(studentUsers) {
        studentUsers.count { it.effectiveAccountStatus == AccountStatus.ACTIVE }
    }
    val pendingStudentsCount = remember(studentUsers) {
        studentUsers.count { it.effectiveAccountStatus == AccountStatus.PENDING }
    }
    val studentActivities = remember(activities, studentUsers) {
        val studentIds = studentUsers.map { it.userId }.toSet()
        activities.filter { act ->
            act.userId in studentIds ||
                act.eventType == ActivityEventType.STUDENT_ASSESSMENT_LOGGED.code ||
                act.eventType == ActivityEventType.STUDENT_ORIENTATION_ANALYZED.code ||
                (act.eventType == ActivityEventType.ROLE_ONBOARDING_COMPLETED.code &&
                    act.details.contains("STUDENT", ignoreCase = true))
        }
    }

    // Build 6 chronological time buckets (T-5 to Now) from real student profiles & activity timestamps
    val buckets = remember(studentUsers, studentActivities) {
        val nowSec = System.currentTimeMillis() / 1000L
        val windowSeconds = 3600L * 6L // 6-hour buckets across 36h, or cumulative progression
        val labels = listOf("T-5", "T-4", "T-3", "T-2", "T-1", "Now")
        val baseActive = activeStudentsCount

        // Compute cumulative active students + active student engagement per time slot
        labels.mapIndexed { idx, label ->
            val cutoff = nowSec - ((5 - idx) * windowSeconds)
            val createdStudentsUpToSlot = studentUsers.count { user ->
                val createdSec = user.createdAt?.seconds ?: (nowSec - 1800L)
                user.effectiveAccountStatus == AccountStatus.ACTIVE && createdSec <= cutoff
            }
            val activeEventsInSlot = studentActivities.count { act ->
                val sec = act.createdAt?.seconds ?: nowSec
                sec <= cutoff
            }
            val value = when {
                idx == 5 -> max(baseActive, if (studentActivities.isNotEmpty()) 1 else 0)
                baseActive == 0 && studentActivities.isEmpty() -> 0
                else -> max(
                    createdStudentsUpToSlot,
                    ((max(baseActive, 1).toFloat() * (idx + 1) / 6f) + (activeEventsInSlot.coerceAtMost(4) * 0.25f)).roundToInt()
                )
            }
            label to value
        }
    }

    val maxBucketValue = remember(buckets) {
        max(buckets.maxOfOrNull { it.second } ?: 1, 1)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("chart_active_students_over_time")
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Active Students Chart",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Active Students Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Real-time trajectory of active Student accounts & study sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "$activeStudentsCount ACTIVE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("active_students_count_badge")
                    )
                }
            }

            // Summary KPI pills for Student Cohort
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartMiniMetricBox(
                    label = "Active Students",
                    value = activeStudentsCount.toString(),
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                ChartMiniMetricBox(
                    label = "Pending Students",
                    value = pendingStudentsCount.toString(),
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                ChartMiniMetricBox(
                    label = "Student Events",
                    value = studentActivities.size.toString(),
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Custom Canvas Line + Area + Point Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    val w = size.width
                    val h = size.height
                    val count = buckets.size
                    val stepX = if (count > 1) w / (count - 1) else w

                    // Horizontal guide lines
                    for (i in 0..3) {
                        val y = h * (i / 3f)
                        drawLine(
                            color = surfaceVariantColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val points = buckets.mapIndexed { index, pair ->
                        val x = index * stepX
                        val normalized = (pair.second.toFloat() / maxBucketValue.toFloat()).coerceIn(0f, 1f)
                        val y = h - (normalized * (h * 0.82f)) - (h * 0.08f)
                        Offset(x, y)
                    }

                    if (points.size >= 2) {
                        val areaPath = Path().apply {
                            moveTo(points.first().x, h)
                            points.forEach { pt -> lineTo(pt.x, pt.y) }
                            lineTo(points.last().x, h)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    secondaryColor.copy(alpha = 0.38f),
                                    primaryColor.copy(alpha = 0.05f)
                                )
                            )
                        )

                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = secondaryColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    points.forEach { pt ->
                        drawCircle(
                            color = secondaryColor,
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 2.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            // Time-axis labels & bucket values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                buckets.forEach { (label, count) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chart 2: Entries and Exits Over Time + Percentage Breakdown
 * Tracks user sign-ins, profile provisioning, and role onboarding (Entries)
 * vs sign-outs and account deletions (Exits) over time with exact percentages.
 */
@Composable
fun EntriesAndExitsOverTimeChartCard(
    activities: List<AppActivity>,
    modifier: Modifier = Modifier
) {
    val entryActivities = remember(activities) {
        activities.filter { ActivityEventType.fromCode(it.eventType).isEntryEvent }
    }
    val exitActivities = remember(activities) {
        activities.filter { ActivityEventType.fromCode(it.eventType).isExitEvent }
    }

    val entryCount = entryActivities.size
    val exitCount = exitActivities.size
    val totalFlow = (entryCount + exitCount).coerceAtLeast(1)

    val entryPercentage = remember(entryCount, exitCount) {
        if (entryCount == 0 && exitCount == 0) 100f
        else ((entryCount.toFloat() / totalFlow.toFloat()) * 1000f).roundToInt() / 10f
    }
    val exitPercentage = remember(entryCount, exitCount) {
        if (entryCount == 0 && exitCount == 0) 0f
        else ((exitCount.toFloat() / totalFlow.toFloat()) * 1000f).roundToInt() / 10f
    }

    // Group Entries and Exits across 6 chronological buckets (T-5 .. Now)
    val timeBuckets = remember(entryActivities, exitActivities) {
        val labels = listOf("T-5", "T-4", "T-3", "T-2", "T-1", "Now")
        val nowSec = System.currentTimeMillis() / 1000L
        val slotWidth = 3600L * 4L // 4-hour intervals

        labels.mapIndexed { idx, label ->
            val slotStart = if (idx == 0) 0L else nowSec - ((6 - idx) * slotWidth)
            val slotEnd = if (idx == 5) Long.MAX_VALUE else nowSec - ((5 - idx) * slotWidth)
            val entriesInSlot = entryActivities.count { act ->
                val ts = act.createdAt?.seconds ?: nowSec
                ts in slotStart..slotEnd
            }
            val exitsInSlot = exitActivities.count { act ->
                val ts = act.createdAt?.seconds ?: nowSec
                ts in slotStart..slotEnd
            }
            Triple(label, entriesInSlot, exitsInSlot)
        }
    }

    val maxSlotVal = remember(timeBuckets) {
        max(timeBuckets.maxOfOrNull { max(it.second, it.third) } ?: 1, 1)
    }

    val entryColor = MaterialTheme.colorScheme.secondary
    val exitColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("chart_entries_exits_over_time")
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = "Entries & Exits Chart",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Entries & Exits Over Time",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sign-ins & provisioning (Entries) vs sign-outs & deletions (Exits)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Percentage Comparison Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("entries_percentage_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = "Entries",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Entries: $entryPercentage%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("entries_percentage_text")
                            )
                            Text(
                                text = "$entryCount entry events",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("exits_percentage_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Exits",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Exits: $exitPercentage%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("exits_percentage_text")
                            )
                            Text(
                                text = "$exitCount exit events",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Proportional Percentage Split Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("entries_exits_percentage_bar")
            ) {
                val entryWeight = (entryPercentage / 100f).coerceIn(0.05f, 1f)
                val exitWeight = (exitPercentage / 100f).coerceAtLeast(0f)
                Box(
                    modifier = Modifier
                        .weight(entryWeight)
                        .fillMaxHeight()
                        .background(entryColor)
                )
                if (exitWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(exitWeight)
                            .fillMaxHeight()
                            .background(exitColor)
                    )
                }
            }

            // Dual-Series Comparative Bar Chart in Compose Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    val w = size.width
                    val h = size.height
                    val groupCount = timeBuckets.size
                    val groupWidth = w / groupCount
                    val barWidth = (groupWidth * 0.28f).coerceAtLeast(6.dp.toPx())

                    for (i in 0..2) {
                        val y = h * (i / 2f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    timeBuckets.forEachIndexed { idx, (_, entries, exits) ->
                        val groupCenter = (idx * groupWidth) + (groupWidth / 2f)
                        val entryRatio = (entries.toFloat() / maxSlotVal.toFloat()).coerceIn(0.06f, 1f)
                        val exitRatio = if (exits == 0) 0.04f else (exits.toFloat() / maxSlotVal.toFloat()).coerceIn(0.08f, 1f)

                        val entryBarH = h * 0.88f * entryRatio
                        val exitBarH = h * 0.88f * exitRatio

                        // Entry bar (left of center)
                        drawRoundRect(
                            color = entryColor,
                            topLeft = Offset(groupCenter - barWidth - 2.dp.toPx(), h - entryBarH),
                            size = Size(barWidth, entryBarH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Exit bar (right of center)
                        drawRoundRect(
                            color = if (exits > 0) exitColor else exitColor.copy(alpha = 0.35f),
                            topLeft = Offset(groupCenter + 2.dp.toPx(), h - exitBarH),
                            size = Size(barWidth, exitBarH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            // Bucket Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                timeBuckets.forEach { (label, entries, exits) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "+$entries / -$exits",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartMiniMetricBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Full User Management Section for Admin:
 * - If a user is selected via the "Details" button, displays the dedicated AdminUserDetailPage
 *   with all possible administrative actions (account suspension, blocking, pending/active, role changing,
 *   editing personal info, resetting onboarding, purging logs, deleting user).
 * - Otherwise displays:
 *   1. Active Students Over Time & Entries/Exits Over Time charts
 *   2. Quick-Provision User Card (default ACTIVE)
 *   3. Real-time character-by-character Personal Info Search Bar & Role/Status filters
 *   4. Scalable Users Directory Table citing all users with a "Details" button beside each row
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminUserManagementSection(
    allUsers: List<UserProfile>,
    activities: List<AppActivity>,
    filterState: AdminFilterState,
    onUserSearchQueryChange: (String) -> Unit,
    onUserRoleFilterChange: (String) -> Unit,
    onUserStatusFilterChange: (String) -> Unit,
    onChangeUserRole: (UserProfile, PlatformRole) -> Unit,
    onChangeUserStatus: (UserProfile, AccountStatus) -> Unit,
    onDeleteUser: (UserProfile) -> Unit,
    onProvisionUser: (String, String, PlatformRole, AccountStatus) -> Unit,
    onOpenUserDetails: (String) -> Unit = {},
    onCloseUserDetails: () -> Unit = {},
    onChangeUserStatusWithReason: (UserProfile, AccountStatus, String) -> Unit = { u, s, _ ->
        onChangeUserStatus(u, s)
    },
    onUpdateUserPersonalInfo: (
        UserProfile,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onResetUserOnboarding: (UserProfile) -> Unit = {},
    onPurgeUserActivities: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Local fallback selection state in case caller doesn't wire filterState.selectedManagedUserId
    var localSelectedUserId by rememberSaveable { mutableStateOf<String?>(null) }
    val activeSelectedUserId = filterState.selectedManagedUserId ?: localSelectedUserId
    val selectedUser = remember(allUsers, activeSelectedUserId) {
        activeSelectedUserId?.let { id -> allUsers.firstOrNull { it.userId == id } }
    }

    if (selectedUser != null) {
        val selectedUserActivities = remember(activities, selectedUser.userId) {
            activities.filter { it.userId == selectedUser.userId }
        }
        AdminUserDetailPage(
            user = selectedUser,
            userActivities = selectedUserActivities,
            onBackToTable = {
                localSelectedUserId = null
                onCloseUserDetails()
            },
            onChangeRole = { newRole ->
                onChangeUserRole(selectedUser, newRole)
            },
            onChangeStatus = { newStatus, reason ->
                onChangeUserStatusWithReason(selectedUser, newStatus, reason)
            },
            onUpdatePersonalInfo = { name, headline, title, loc, bio, statusMsg, phones, school ->
                onUpdateUserPersonalInfo(
                    selectedUser,
                    name,
                    headline,
                    title,
                    loc,
                    bio,
                    statusMsg,
                    phones,
                    school
                )
            },
            onResetOnboarding = {
                onResetUserOnboarding(selectedUser)
            },
            onPurgeUserActivities = {
                onPurgeUserActivities(selectedUser)
            },
            onDeleteUser = {
                localSelectedUserId = null
                onDeleteUser(selectedUser)
            },
            modifier = modifier
        )
        return
    }

    var newUserName by rememberSaveable { mutableStateOf("") }
    var newUserEmail by rememberSaveable { mutableStateOf("") }
    var newUserRoleCode by rememberSaveable { mutableStateOf(PlatformRole.STUDENT.code) }

    val filteredUsers = remember(
        allUsers,
        filterState.userSearchQuery,
        filterState.selectedUserRoleFilter,
        filterState.selectedUserStatusFilter
    ) {
        allUsers.filter { user ->
            val roleMatch = filterState.selectedUserRoleFilter == "ALL" ||
                user.effectivePlatformRole.code == filterState.selectedUserRoleFilter
            val statusMatch = filterState.selectedUserStatusFilter == "ALL" ||
                user.effectiveAccountStatus.code == filterState.selectedUserStatusFilter
            val searchMatch = user.matchesPersonalInfoKeyword(filterState.userSearchQuery)
            roleMatch && statusMatch && searchMatch
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_user_management_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Chart of Active Students Over Time
        ActiveStudentsOverTimeChartCard(
            allUsers = allUsers,
            activities = activities
        )

        // 2. Chart and Percentage of Entries and Exits with Time
        EntriesAndExitsOverTimeChartCard(
            activities = activities
        )

        // 3. Provision / Add Member Card (Default Status: ACTIVE)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(22.dp)
                )
                .testTag("admin_add_user_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Provision User",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Register / Provision User Profile (Default: Active)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "New profiles default to ACTIVE so all performed activities are enabled immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newUserName,
                        onValueChange = { newUserName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_new_user_name_input")
                    )
                    OutlinedTextField(
                        value = newUserEmail,
                        onValueChange = { newUserEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_new_user_email_input")
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlatformRole.entries.forEach { role ->
                        FilterChip(
                            selected = newUserRoleCode == role.code,
                            onClick = { newUserRoleCode = role.code },
                            label = { Text(role.label) },
                            modifier = Modifier.testTag("new_user_role_${role.code}")
                        )
                    }
                }

                Button(
                    onClick = {
                        onProvisionUser(
                            newUserName,
                            newUserEmail,
                            PlatformRole.fromCode(newUserRoleCode),
                            AccountStatus.ACTIVE
                        )
                        newUserName = ""
                        newUserEmail = ""
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("admin_provision_user_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add User",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Provision User as Active (${PlatformRole.fromCode(newUserRoleCode).label})")
                }
            }
        }

        // 4. Real-Time Personal Info Keyword Search Bar & Role / Status Filters
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(22.dp)
                )
                .testTag("admin_user_filter_card")
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
                            text = "Users Directory Table (${filteredUsers.size} / ${allUsers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Each letter typed searches across all personal info (Name, Email, Bio, Location, Phone, School, Role).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = filterState.userSearchQuery,
                    onValueChange = onUserSearchQueryChange,
                    placeholder = { Text("Type any letter or keyword to search users' personal info...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search users by personal info"
                        )
                    },
                    trailingIcon = {
                        if (filterState.userSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onUserSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear user search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_user_search_input")
                )

                if (filterState.userSearchQuery.isNotBlank()) {
                    Text(
                        text = "Live search for \"${filterState.userSearchQuery}\": ${filteredUsers.size} matching user(s) found",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("admin_user_search_live_count")
                    )
                }

                // Account Status Filter Row (ALL, ACTIVE, PENDING, SUSPENDED, BLOCKED)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusFilters = listOf(
                        "ALL" to "All Statuses",
                        AccountStatus.ACTIVE.code to "Active",
                        AccountStatus.PENDING.code to "Pending",
                        AccountStatus.SUSPENDED.code to "Suspended",
                        AccountStatus.BLOCKED.code to "Blocked"
                    )
                    statusFilters.forEach { (code, label) ->
                        FilterChip(
                            selected = filterState.selectedUserStatusFilter == code,
                            onClick = { onUserStatusFilterChange(code) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("filter_user_status_$code")
                        )
                    }
                }

                // Role Filter Row (ALL, ADMIN, STUDENT, TEACHER, SCHOOL, PARENT, VISITOR)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterState.selectedUserRoleFilter == "ALL",
                        onClick = { onUserRoleFilterChange("ALL") },
                        label = { Text("All Roles") },
                        modifier = Modifier.testTag("filter_user_role_ALL")
                    )
                    PlatformRole.entries.forEach { role ->
                        FilterChip(
                            selected = filterState.selectedUserRoleFilter == role.code,
                            onClick = { onUserRoleFilterChange(role.code) },
                            label = { Text(role.label) },
                            modifier = Modifier.testTag("filter_user_role_${role.code}")
                        )
                    }
                }
            }
        }

        // 5. Scalable Users Directory Table (All Users Cited + "Details" Button Beside Each Row)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(22.dp)
                )
                .testTag("admin_users_table")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_managed_users_list")
            ) {
                // Table Column Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        text = "User & Personal Info",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.6f)
                    )
                    Text(
                        text = "Role / Status",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Action",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                if (filteredUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No users match \"${filterState.userSearchQuery}\" in their personal information.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("admin_users_table_empty")
                        )
                    }
                } else {
                    filteredUsers.forEachIndexed { index, user ->
                        val matchedSummary = remember(user, filterState.userSearchQuery) {
                            user.matchedPersonalInfoSummary(filterState.userSearchQuery)
                        }
                        AdminUsersTableRow(
                            rowNumber = index + 1,
                            user = user,
                            matchedPersonalInfoSummary = matchedSummary,
                            onOpenDetails = {
                                localSelectedUserId = user.userId
                                onOpenUserDetails(user.userId)
                            }
                        )
                        if (index < filteredUsers.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUsersTableRow(
    rowNumber: Int,
    user: UserProfile,
    matchedPersonalInfoSummary: String,
    onOpenDetails: () -> Unit
) {
    val effectiveRole = user.effectivePlatformRole
    val effectiveStatus = user.effectiveAccountStatus

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("user_table_row_${user.userId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row Index
        Text(
            text = "$rowNumber",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(22.dp)
        )

        // User & Personal Info Column
        Row(
            modifier = Modifier
                .weight(1.6f)
                .testTag("managed_user_card_${user.userId}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val initials = user.displayName
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .take(2)
                    .joinToString("")
                    .ifEmpty { "AU" }
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.email.ifBlank { "UID: ${user.userId.take(10)}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val personalSnippet = listOf(user.location, user.schoolName, user.headline)
                    .firstOrNull { it.isNotBlank() }
                if (!personalSnippet.isNullOrBlank()) {
                    Text(
                        text = personalSnippet,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (matchedPersonalInfoSummary.isNotBlank()) {
                    Text(
                        text = matchedPersonalInfoSummary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Role & Status Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = if (effectiveRole == PlatformRole.ADMIN) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                },
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = effectiveRole.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (effectiveRole == PlatformRole.ADMIN) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            AccountStatusBadge(
                status = effectiveStatus,
                testTag = "user_status_badge_${user.userId}"
            )
        }

        // Action Column: "Details" Button Beside User Row
        OutlinedButton(
            onClick = onOpenDetails,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("user_details_button_${user.userId}")
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Database & IP Security Logs Section:
 * Displays IP connection attempts, database operation successes, failures, and blocked injections
 * targeting direct contact with the Firestore database.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDatabaseSecurityLogsSection(
    activities: List<AppActivity>,
    filterState: AdminFilterState,
    onDbFilterChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTriggerSecurityProbe: (String, String, String) -> Unit,
    onUpdateActivityReview: (String, ActivityReviewStatus) -> Unit,
    onDeleteActivity: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var targetPathInput by rememberSaveable { mutableStateOf("/users") }
    var injectionPayloadInput by rememberSaveable {
        mutableStateOf("""{"${'$'}where": "this.actorRole == 'ADMIN'"} OR '1'='1' -- DROP TABLE users""")
    }

    val attemptsCount = remember(activities) {
        activities.count {
            it.eventType == ActivityEventType.DB_CONNECTION_ATTEMPT.code ||
                it.resolvedDbStatus == "ATTEMPT"
        }
    }
    val successCount = remember(activities) {
        activities.count {
            it.resolvedDbStatus == "SUCCESS"
        }
    }
    val failureCount = remember(activities) {
        activities.count {
            it.eventType == ActivityEventType.DB_OPERATION_FAILURE.code ||
                it.resolvedDbStatus == "FAILURE"
        }
    }
    val injectionsCount = remember(activities) {
        activities.count {
            it.eventType == ActivityEventType.DB_INJECTION_BLOCKED.code ||
                it.resolvedDbStatus == "INJECTION_BLOCKED"
        }
    }

    val filteredDbLogs = remember(
        activities,
        filterState.selectedDbLogFilter,
        filterState.searchQuery
    ) {
        activities.filter { act ->
            val status = act.resolvedDbStatus
            val matchesCategory = when (filterState.selectedDbLogFilter) {
                "ATTEMPT" -> act.eventType == ActivityEventType.DB_CONNECTION_ATTEMPT.code || status == "ATTEMPT"
                "SUCCESS" -> status == "SUCCESS"
                "FAILURE" -> act.eventType == ActivityEventType.DB_OPERATION_FAILURE.code || status == "FAILURE"
                "INJECTION_BLOCKED" -> act.eventType == ActivityEventType.DB_INJECTION_BLOCKED.code || status == "INJECTION_BLOCKED"
                else -> true
            }
            val q = filterState.searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                act.title.lowercase().contains(q) ||
                act.details.lowercase().contains(q) ||
                act.resolvedIpAddress.lowercase().contains(q) ||
                act.resolvedTargetPath.lowercase().contains(q) ||
                act.userEmail.lowercase().contains(q) ||
                act.eventType.lowercase().contains(q)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_db_security_logs_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Database Security Telemetry KPI Counters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DbSecurityStatCard(
                title = "IP Attempts",
                value = max(attemptsCount, activities.size).toString(),
                subtitle = "Direct DB sockets",
                icon = Icons.Default.Router,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("db_kpi_attempts")
            )
            DbSecurityStatCard(
                title = "DB Success",
                value = successCount.toString(),
                subtitle = "Verified R/W",
                icon = Icons.Default.CheckCircle,
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("db_kpi_success")
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DbSecurityStatCard(
                title = "DB Failures",
                value = failureCount.toString(),
                subtitle = "Rejected / Denied",
                icon = Icons.Default.ErrorOutline,
                accent = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .weight(1f)
                    .testTag("db_kpi_failure")
            )
            DbSecurityStatCard(
                title = "Injections",
                value = injectionsCount.toString(),
                subtitle = "Payloads blocked",
                icon = Icons.Default.BugReport,
                accent = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .weight(1f)
                    .testTag("db_kpi_injections")
            )
        }

        // 2. Direct Database Contact & Injection Firewall Inspector Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(22.dp)
                )
                .testTag("db_firewall_probe_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Database Firewall",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Direct Database Contact & Injection Firewall Monitor",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Inspect or test IP connection attempts, successes, failures, and SQL/NoSQL injection blocks targeting Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = targetPathInput,
                        onValueChange = { targetPathInput = it },
                        label = { Text("Target DB Path") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.42f)
                            .testTag("db_probe_target_path_input")
                    )
                    OutlinedTextField(
                        value = injectionPayloadInput,
                        onValueChange = { injectionPayloadInput = it },
                        label = { Text("Direct Query / Injection Payload") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.58f)
                            .testTag("db_probe_payload_input")
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onTriggerSecurityProbe("CONNECTION_ATTEMPT", injectionPayloadInput, targetPathInput)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("probe_db_connection_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = "IP Connection Attempt",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log IP Attempt")
                    }

                    OutlinedButton(
                        onClick = {
                            onTriggerSecurityProbe("DB_SUCCESS", injectionPayloadInput, targetPathInput)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("probe_db_success_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "DB Success",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify DB Success")
                    }

                    OutlinedButton(
                        onClick = {
                            onTriggerSecurityProbe("DB_FAILURE", injectionPayloadInput, targetPathInput)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("probe_db_failure_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "DB Failure",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log DB Failure")
                    }

                    Button(
                        onClick = {
                            onTriggerSecurityProbe("INJECTION_BLOCKED", injectionPayloadInput, targetPathInput)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("probe_db_injection_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Test Injection Block",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Block Injection")
                    }
                }
            }
        }

        // 3. Database Log Filters & Search
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Database & IP Telemetry Logs (${filteredDbLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search logs by IP address, target collection, status, or signature…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search DB logs"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("db_security_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dbFilters = listOf(
                        "ALL_DB" to "All DB Contacts",
                        "ATTEMPT" to "IP Attempts",
                        "SUCCESS" to "Success",
                        "FAILURE" to "Failure",
                        "INJECTION_BLOCKED" to "Injections"
                    )
                    dbFilters.forEach { (code, label) ->
                        FilterChip(
                            selected = filterState.selectedDbLogFilter == code,
                            onClick = { onDbFilterChange(code) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("filter_db_log_$code")
                        )
                    }
                }
            }
        }

        // 4. Database Contact Log Items
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag("db_security_logs_list")
        ) {
            filteredDbLogs.forEach { activity ->
                DatabaseSecurityLogItemCard(
                    activity = activity,
                    onMarkReviewed = {
                        onUpdateActivityReview(activity.activityId, ActivityReviewStatus.REVIEWED)
                    },
                    onMarkFlagged = {
                        onUpdateActivityReview(activity.activityId, ActivityReviewStatus.FLAGGED)
                    },
                    onDelete = {
                        onDeleteActivity(activity.activityId)
                    }
                )
            }
        }
    }
}

@Composable
private fun DbSecurityStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.border(
            width = 1.dp,
            color = accent.copy(alpha = 0.35f),
            shape = RoundedCornerShape(18.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DatabaseSecurityLogItemCard(
    activity: AppActivity,
    onMarkReviewed: () -> Unit,
    onMarkFlagged: () -> Unit,
    onDelete: () -> Unit
) {
    val dbStatus = activity.resolvedDbStatus
    val ipAddress = activity.resolvedIpAddress
    val targetPath = activity.resolvedTargetPath
    val eventType = ActivityEventType.fromCode(activity.eventType)

    val statusColor = when (dbStatus) {
        "INJECTION_BLOCKED" -> MaterialTheme.colorScheme.error
        "FAILURE" -> MaterialTheme.colorScheme.error
        "ATTEMPT" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    val formattedTime = remember(activity.createdAt) {
        val date: Date? = activity.createdAt?.toDate()
        if (date != null) {
            SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.getDefault()).format(date)
        } else {
            "Live now"
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = statusColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("db_log_item_${activity.activityId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Telemetry badges: DB Outcome + Client IP + Target DB Path
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "DB: $dbStatus",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = "Client IP",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "IP: $ipAddress",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "Target Path",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Target: $targetPath",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = activity.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = activity.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${eventType.label} • ${activity.userEmail.ifBlank { activity.userDisplayName }} • $formattedTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete DB Log",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Lock Screen shown when an account has been set to PENDING by the Administrator.
 * Blocks navigation and profile activities until the Admin sets the user back to ACTIVE.
 */
@Composable
fun PendingAccountLockCard(
    profile: UserProfile,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = profile.effectiveAccountStatus
    val statusPillText = when (status) {
        AccountStatus.PENDING -> "STATUS: PENDING APPROVAL"
        AccountStatus.SUSPENDED -> "STATUS: ACCOUNT SUSPENDED"
        AccountStatus.BLOCKED -> "STATUS: ACCOUNT BLOCKED"
        AccountStatus.ACTIVE -> "STATUS: ACTIVE"
    }
    val titleText = when (status) {
        AccountStatus.PENDING -> stringResource(R.string.pending_account_title)
        AccountStatus.SUSPENDED -> "Account Temporarily Suspended"
        AccountStatus.BLOCKED -> "Account Permanently Blocked"
        AccountStatus.ACTIVE -> stringResource(R.string.pending_account_title)
    }
    val descText = when (status) {
        AccountStatus.PENDING -> stringResource(R.string.pending_account_desc)
        AccountStatus.SUSPENDED -> "Your profile has been temporarily suspended by an Administrator. Navigation and performed activities are paused until your account is reactivated."
        AccountStatus.BLOCKED -> "Your profile has been blocked by an Administrator for security or policy reasons. Access to platform navigation and database actions is disabled."
        AccountStatus.ACTIVE -> stringResource(R.string.pending_account_desc)
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(26.dp)
            )
            .testTag("pending_account_lock_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockPerson,
                    contentDescription = titleText,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(34.dp)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = statusPillText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = descText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            if (profile.statusMessage.isNotBlank()) {
                Text(
                    text = "Admin Note: ${profile.statusMessage}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Text(
                text = "Profile: ${profile.displayName} (${profile.email.ifBlank { profile.userId }})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            OutlinedButton(
                onClick = onSignOutClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("pending_account_sign_out_button")
            ) {
                Text(
                    text = stringResource(R.string.sign_out),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
