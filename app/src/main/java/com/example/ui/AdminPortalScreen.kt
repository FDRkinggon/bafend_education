package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.data.model.ActivitySeverity
import com.example.data.model.AdminCustomSubject
import com.example.data.model.AppActivity
import com.example.data.model.PlatformRole
import com.example.data.model.RevisionBafendDocument
import com.example.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPortalContent(
    activitiesState: ActivitiesUiState,
    allUsers: List<UserProfile>,
    currentProfile: UserProfile?,
    isAdmin: Boolean,
    filterState: AdminFilterState,
    onSearchQueryChange: (String) -> Unit,
    onEventTypeFilterChange: (String) -> Unit,
    onReviewStatusFilterChange: (String) -> Unit,
    onLogManualAudit: () -> Unit,
    onUpdateActivityReview: (String, ActivityReviewStatus) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onDismissFeedback: () -> Unit,
    onSelectAdminTab: (AdminSectionTab) -> Unit = {},
    onUserSearchQueryChange: (String) -> Unit = {},
    onUserRoleFilterChange: (String) -> Unit = {},
    onUserStatusFilterChange: (String) -> Unit = {},
    onChangeUserRole: (UserProfile, PlatformRole) -> Unit = { _, _ -> },
    onChangeUserStatus: (UserProfile, AccountStatus) -> Unit = { _, _ -> },
    onDeleteUser: (UserProfile) -> Unit = {},
    onProvisionUser: (String, String, PlatformRole, AccountStatus) -> Unit = { _, _, _, _ -> },
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
    onDbFilterChange: (String) -> Unit = {},
    onTriggerSecurityProbe: (String, String, String) -> Unit = { _, _, _ -> },
    adminCustomSubjects: List<AdminCustomSubject> = emptyList(),
    revisionDocuments: List<RevisionBafendDocument> = emptyList(),
    onAddSubjectUnderSubsystem: (
        String,
        String,
        Int,
        String,
        String,
        String,
        String,
        String,
        String,
        String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onDeleteCustomSubject: (String) -> Unit = {},
    onUploadPdfForSubject: (
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        Int,
        String,
        String,
        ByteArray?,
        Boolean,
        String,
        ByteArray?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onOpenBafendReader: (RevisionBafendDocument) -> Unit = {},
    onDeleteRevisionDocument: (RevisionBafendDocument) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 760.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("admin_portal_screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Status Feedback Banner
        AnimatedVisibility(visible = filterState.statusFeedback != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismissFeedback() }
                    .testTag("admin_feedback_banner")
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
                        text = filterState.statusFeedback.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // 1. Admin Portal Hero Banner & Audit Trigger
        AdminHeroBannerCard(
            isAdmin = isAdmin,
            currentEmail = currentProfile?.email.orEmpty(),
            onLogManualAudit = onLogManualAudit
        )

        if (!isAdmin) {
            AdminAccessRestrictedCard(
                currentEmail = currentProfile?.email.orEmpty()
            )
            Spacer(modifier = Modifier.height(24.dp))
            return@Column
        }

        when (activitiesState) {
            is ActivitiesUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .testTag("admin_loading_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Synchronizing live activity stream…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is ActivitiesUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_error_state")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Activity Stream Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activitiesState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            is ActivitiesUiState.Success -> {
                val allActivities = activitiesState.activities
                val dbLogsCount = remember(allActivities) {
                    allActivities.size
                }

                // Segmented Admin Tabs: Manage Users | DB & IP Security Logs | All Activity Logs
                AdminSectionTabsRow(
                    activeTab = filterState.activeTab,
                    totalUsersCount = allUsers.size,
                    dbLogsCount = dbLogsCount,
                    allActivitiesCount = allActivities.size,
                    onSelectTab = onSelectAdminTab
                )

                when (filterState.activeTab) {
                    AdminSectionTab.MANAGE_USERS -> {
                        AdminUserManagementSection(
                            allUsers = allUsers,
                            activities = allActivities,
                            filterState = filterState,
                            onUserSearchQueryChange = onUserSearchQueryChange,
                            onUserRoleFilterChange = onUserRoleFilterChange,
                            onUserStatusFilterChange = onUserStatusFilterChange,
                            onChangeUserRole = onChangeUserRole,
                            onChangeUserStatus = onChangeUserStatus,
                            onDeleteUser = onDeleteUser,
                            onProvisionUser = onProvisionUser,
                            onOpenUserDetails = onOpenUserDetails,
                            onCloseUserDetails = onCloseUserDetails,
                            onChangeUserStatusWithReason = onChangeUserStatusWithReason,
                            onUpdateUserPersonalInfo = onUpdateUserPersonalInfo,
                            onResetUserOnboarding = onResetUserOnboarding,
                            onPurgeUserActivities = onPurgeUserActivities
                        )
                    }

                    AdminSectionTab.CURRICULUM_AND_PDFS -> {
                        AdminCurriculumAndPdfSection(
                            adminCustomSubjects = adminCustomSubjects,
                            revisionDocuments = revisionDocuments,
                            onAddSubjectUnderSubsystem = onAddSubjectUnderSubsystem,
                            onDeleteCustomSubject = onDeleteCustomSubject,
                            onUploadPdfForSubject = onUploadPdfForSubject,
                            onOpenBafendReader = onOpenBafendReader,
                            onDeleteRevisionDocument = onDeleteRevisionDocument
                        )
                    }

                    AdminSectionTab.DB_SECURITY_LOGS -> {
                        AdminDatabaseSecurityLogsSection(
                            activities = allActivities,
                            filterState = filterState,
                            onDbFilterChange = onDbFilterChange,
                            onSearchQueryChange = onSearchQueryChange,
                            onTriggerSecurityProbe = onTriggerSecurityProbe,
                            onUpdateActivityReview = onUpdateActivityReview,
                            onDeleteActivity = onDeleteActivity
                        )
                    }

                    AdminSectionTab.ACTIVITY_STREAM -> {
                        val filteredActivities = remember(
                            allActivities,
                            filterState.searchQuery,
                            filterState.selectedEventType,
                            filterState.selectedReviewStatus
                        ) {
                            allActivities.filter { activity ->
                                val matchesType = filterState.selectedEventType == "ALL" ||
                                    activity.eventType == filterState.selectedEventType
                                val matchesReview = filterState.selectedReviewStatus == "ALL" ||
                                    activity.reviewStatus == filterState.selectedReviewStatus
                                val q = filterState.searchQuery.trim().lowercase()
                                val matchesSearch = q.isEmpty() ||
                                    activity.title.lowercase().contains(q) ||
                                    activity.details.lowercase().contains(q) ||
                                    activity.userDisplayName.lowercase().contains(q) ||
                                    activity.userEmail.lowercase().contains(q) ||
                                    activity.eventType.lowercase().contains(q) ||
                                    activity.resolvedIpAddress.lowercase().contains(q) ||
                                    activity.resolvedTargetPath.lowercase().contains(q) ||
                                    activity.userId.lowercase().contains(q)
                                matchesType && matchesReview && matchesSearch
                            }
                        }

                        // 2. Live KPI Telemetry Grid
                        AdminKpiMetricsGrid(
                            activities = allActivities,
                            allUsers = allUsers
                        )

                        // 3. Activity Distribution Bar Card
                        ActivityDistributionCard(activities = allActivities)

                        // 4. Registered Users Directory Overview
                        if (allUsers.isNotEmpty()) {
                            AdminUsersOverviewCard(
                                users = allUsers,
                                activities = allActivities,
                                isAdmin = isAdmin
                            )
                        }

                        // 5. Search & Multi-Filter Control Card
                        AdminFilterControlCard(
                            filterState = filterState,
                            totalCount = allActivities.size,
                            filteredCount = filteredActivities.size,
                            onSearchQueryChange = onSearchQueryChange,
                            onEventTypeFilterChange = onEventTypeFilterChange,
                            onReviewStatusFilterChange = onReviewStatusFilterChange
                        )

                        // 6. Live Activity Feed
                        if (filteredActivities.isEmpty()) {
                            AdminEmptyActivitiesCard()
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.testTag("admin_activity_list")
                            ) {
                                filteredActivities.forEach { activity ->
                                    AdminActivityItemCard(
                                        activity = activity,
                                        isAdmin = isAdmin,
                                        onMarkReviewed = {
                                            onUpdateActivityReview(
                                                activity.activityId,
                                                ActivityReviewStatus.REVIEWED
                                            )
                                        },
                                        onMarkFlagged = {
                                            onUpdateActivityReview(
                                                activity.activityId,
                                                ActivityReviewStatus.FLAGGED
                                            )
                                        },
                                        onDelete = {
                                            onDeleteActivity(activity.activityId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AdminHeroBannerCard(
    isAdmin: Boolean,
    currentEmail: String,
    onLogManualAudit: () -> Unit
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(26.dp)
            )
            .testTag("admin_hero_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isAdmin) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.VerifiedUser,
                                contentDescription = "Portal Role",
                                tint = if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAdmin) "GLOBAL ADMIN PORTAL" else "SCOPED ACTIVITY PORTAL",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isAdmin) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE FIRESTORE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.admin_portal_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (isAdmin) {
                        "Full zero-trust visibility enabled for $currentEmail. Monitoring all sign-ins, profile updates, AI advisor runs, and security events across the app."
                    } else {
                        "Viewing real-time activity audit trail for $currentEmail. Global cross-user telemetry and review actions are enforced via Firestore RBAC rules."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onLogManualAudit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("admin_log_audit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = stringResource(R.string.admin_log_audit_button),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.admin_log_audit_button),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminKpiMetricsGrid(
    activities: List<AppActivity>,
    allUsers: List<UserProfile>
) {
    val totalEvents = activities.size
    val uniqueUserCount = maxOf(
        allUsers.size,
        activities.map { it.userId }.filter { it.isNotBlank() }.distinct().size
    )
    val aiRunsCount = activities.count {
        it.eventType == ActivityEventType.AI_ADVISOR_RUN.code ||
            it.eventType == ActivityEventType.AI_SUGGESTION_APPLIED.code
    }
    val reviewedOrFlaggedCount = activities.count {
        it.reviewStatus == ActivityReviewStatus.REVIEWED.code ||
            it.reviewStatus == ActivityReviewStatus.FLAGGED.code
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiStatCard(
                title = "Total Activities",
                value = totalEvents.toString(),
                subtitle = "Real-time events",
                icon = Icons.Default.Timeline,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_total_events")
            )
            KpiStatCard(
                title = "Active Users",
                value = uniqueUserCount.toString(),
                subtitle = "Verified accounts",
                icon = Icons.Default.Group,
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_active_users")
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiStatCard(
                title = "AI Advisor Runs",
                value = aiRunsCount.toString(),
                subtitle = "ThinkingLevel.HIGH",
                icon = Icons.Default.Psychology,
                accentColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_ai_runs")
            )
            KpiStatCard(
                title = "Audited / Flagged",
                value = reviewedOrFlaggedCount.toString(),
                subtitle = "Governance status",
                icon = Icons.Default.AdminPanelSettings,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_reviewed_flagged")
            )
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            shape = RoundedCornerShape(20.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
            )
        }
    }
}

@Composable
private fun ActivityDistributionCard(activities: List<AppActivity>) {
    val total = activities.size.coerceAtLeast(1)
    val authCount = activities.count {
        it.eventType == ActivityEventType.AUTH_SIGN_IN.code ||
            it.eventType == ActivityEventType.AUTH_SIGN_OUT.code
    }
    val profileCount = activities.count {
        it.eventType == ActivityEventType.PROFILE_CREATED.code ||
            it.eventType == ActivityEventType.ROLE_ONBOARDING_COMPLETED.code ||
            it.eventType == ActivityEventType.STUDENT_ASSESSMENT_LOGGED.code ||
            it.eventType == ActivityEventType.PROFILE_UPDATED.code ||
            it.eventType == ActivityEventType.STATUS_CHANGED.code
    }
    val aiCount = activities.count {
        it.eventType == ActivityEventType.STUDENT_ORIENTATION_ANALYZED.code ||
            it.eventType == ActivityEventType.AI_ADVISOR_RUN.code ||
            it.eventType == ActivityEventType.AI_SUGGESTION_APPLIED.code
    }
    val securityCount = activities.count {
        it.eventType == ActivityEventType.ADMIN_AUDIT_ACTION.code
    }

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
            .testTag("admin_distribution_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Activity Telemetry Breakdown",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Stacked visual progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (activities.isNotEmpty()) {
                    if (authCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(authCount.toFloat() / total)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    if (profileCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(profileCount.toFloat() / total)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                    if (aiCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(aiCount.toFloat() / total)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                    }
                    if (securityCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(securityCount.toFloat() / total)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DistributionLegendItem(
                    label = "Auth ($authCount)",
                    color = MaterialTheme.colorScheme.primary
                )
                DistributionLegendItem(
                    label = "Profile ($profileCount)",
                    color = MaterialTheme.colorScheme.secondary
                )
                DistributionLegendItem(
                    label = "AI ($aiCount)",
                    color = MaterialTheme.colorScheme.tertiary
                )
                DistributionLegendItem(
                    label = "Audit ($securityCount)",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DistributionLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AdminUsersOverviewCard(
    users: List<UserProfile>,
    activities: List<AppActivity>,
    isAdmin: Boolean
) {
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
            .testTag("admin_users_directory_card")
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
                    text = if (isAdmin) "Registered User Profiles (${users.size})" else "Monitored Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Completeness & Events",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            users.take(5).forEachIndexed { index, user ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
                val userEventCount = activities.count { it.userId == user.userId }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
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
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${user.email.ifBlank { user.userId }} • Actor: ${user.effectivePlatformRole.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$userEventCount events",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { user.completionPercentage / 100f },
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .width(64.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminFilterControlCard(
    filterState: AdminFilterState,
    totalCount: Int,
    filteredCount: Int,
    onSearchQueryChange: (String) -> Unit,
    onEventTypeFilterChange: (String) -> Unit,
    onReviewStatusFilterChange: (String) -> Unit
) {
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
            .testTag("admin_filter_card")
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
                    text = "Activity Log Stream",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Showing $filteredCount of $totalCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.admin_search_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search activities"
                    )
                },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_search_input")
            )

            // Event Type Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val eventFilters = listOf(
                    "ALL" to "All Events",
                    ActivityEventType.AUTH_SIGN_IN.code to "Sign-Ins",
                    ActivityEventType.PROFILE_CREATED.code to "Provisioned",
                    ActivityEventType.ROLE_ONBOARDING_COMPLETED.code to "Role Setup",
                    ActivityEventType.STUDENT_ASSESSMENT_LOGGED.code to "Student Scores",
                    ActivityEventType.STUDENT_ORIENTATION_ANALYZED.code to "Student Orientation",
                    ActivityEventType.PROFILE_UPDATED.code to "Profile Edits",
                    ActivityEventType.STATUS_CHANGED.code to "Status Updates",
                    ActivityEventType.ADMIN_AUDIT_ACTION.code to "Admin Audits"
                )
                eventFilters.forEach { (code, label) ->
                    FilterChip(
                        selected = filterState.selectedEventType == code,
                        onClick = { onEventTypeFilterChange(code) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("filter_event_$code")
                    )
                }
            }

            // Review Status Filter Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val reviewFilters = listOf(
                    "ALL" to "All Statuses",
                    ActivityReviewStatus.RECORDED.code to "Recorded",
                    ActivityReviewStatus.REVIEWED.code to "Reviewed",
                    ActivityReviewStatus.FLAGGED.code to "Flagged"
                )
                reviewFilters.forEach { (code, label) ->
                    FilterChip(
                        selected = filterState.selectedReviewStatus == code,
                        onClick = { onReviewStatusFilterChange(code) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("filter_review_$code")
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminEmptyActivitiesCard() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("admin_empty_state")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = stringResource(R.string.admin_empty_activities_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = stringResource(R.string.admin_empty_activities_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.admin_empty_activities_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdminActivityItemCard(
    activity: AppActivity,
    isAdmin: Boolean,
    onMarkReviewed: () -> Unit,
    onMarkFlagged: () -> Unit,
    onDelete: () -> Unit
) {
    val eventType = ActivityEventType.fromCode(activity.eventType)
    val severity = ActivitySeverity.fromCode(activity.severity)
    val reviewStatus = ActivityReviewStatus.fromCode(activity.reviewStatus)

    val formattedTime = remember(activity.createdAt) {
        val date = activity.createdAt?.toDate()
        if (date != null) {
            SimpleDateFormat("MMM d, yyyy • h:mm:ss a", Locale.getDefault()).format(date)
        } else {
            "Just now"
        }
    }

    val (eventIcon, accentColor) = when (eventType) {
        ActivityEventType.AUTH_SIGN_IN, ActivityEventType.AUTH_SIGN_OUT ->
            Icons.AutoMirrored.Filled.Login to MaterialTheme.colorScheme.primary
        ActivityEventType.PROFILE_CREATED ->
            Icons.Default.PersonAdd to MaterialTheme.colorScheme.secondary
        ActivityEventType.ROLE_ONBOARDING_COMPLETED ->
            Icons.Default.VerifiedUser to MaterialTheme.colorScheme.secondary
        ActivityEventType.STUDENT_ASSESSMENT_LOGGED ->
            Icons.Default.EditNote to MaterialTheme.colorScheme.secondary
        ActivityEventType.STUDENT_ORIENTATION_ANALYZED ->
            Icons.Default.AutoAwesome to MaterialTheme.colorScheme.tertiary
        ActivityEventType.PROFILE_UPDATED, ActivityEventType.STATUS_CHANGED ->
            Icons.Default.EditNote to MaterialTheme.colorScheme.secondary
        ActivityEventType.AI_ADVISOR_RUN, ActivityEventType.AI_SUGGESTION_APPLIED ->
            Icons.Default.AutoAwesome to MaterialTheme.colorScheme.tertiary
        ActivityEventType.USER_ROLE_CHANGED,
        ActivityEventType.USER_STATUS_CHANGED ->
            Icons.Default.AdminPanelSettings to MaterialTheme.colorScheme.secondary
        ActivityEventType.USER_DELETED ->
            Icons.Default.DeleteOutline to MaterialTheme.colorScheme.error
        ActivityEventType.DB_CONNECTION_ATTEMPT,
        ActivityEventType.DB_OPERATION_SUCCESS ->
            Icons.Default.Security to MaterialTheme.colorScheme.primary
        ActivityEventType.DB_OPERATION_FAILURE,
        ActivityEventType.DB_INJECTION_BLOCKED ->
            Icons.Default.Flag to MaterialTheme.colorScheme.error
        ActivityEventType.ADMIN_AUDIT_ACTION ->
            Icons.Default.Security to MaterialTheme.colorScheme.primary
    }

    val reviewPillColor = when (reviewStatus) {
        ActivityReviewStatus.RECORDED -> MaterialTheme.colorScheme.surfaceVariant
        ActivityReviewStatus.REVIEWED -> MaterialTheme.colorScheme.secondaryContainer
        ActivityReviewStatus.FLAGGED -> MaterialTheme.colorScheme.errorContainer
    }
    val reviewPillTextColor = when (reviewStatus) {
        ActivityReviewStatus.RECORDED -> MaterialTheme.colorScheme.onSurfaceVariant
        ActivityReviewStatus.REVIEWED -> MaterialTheme.colorScheme.onSecondaryContainer
        ActivityReviewStatus.FLAGGED -> MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when (reviewStatus) {
                    ActivityReviewStatus.FLAGGED -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    ActivityReviewStatus.REVIEWED -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                    ActivityReviewStatus.RECORDED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("activity_item_${activity.activityId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Event Icon + Type Label + Review Status Pill
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
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = eventIcon,
                            contentDescription = eventType.label,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = eventType.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = reviewPillColor,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = reviewStatus.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = reviewPillTextColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Title & Details
            Text(
                text = activity.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (activity.details.isNotBlank()) {
                Text(
                    text = activity.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // User Attribution Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = activity.userDisplayName.firstOrNull()?.uppercase() ?: "U"
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = activity.userDisplayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${activity.userEmail.ifBlank { "Verified UID" }} • UID: ${activity.userId.take(10)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = severity.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Admin Governance Actions Row (when signed in as Admin)
            if (isAdmin) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onMarkReviewed,
                        enabled = reviewStatus != ActivityReviewStatus.REVIEWED,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("review_activity_${activity.activityId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Mark Reviewed",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve")
                    }

                    OutlinedButton(
                        onClick = onMarkFlagged,
                        enabled = reviewStatus != ActivityReviewStatus.FLAGGED,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("flag_activity_${activity.activityId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Flag Activity",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Flag")
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_activity_${activity.activityId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete activity log",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAccessRestrictedCard(currentEmail: String) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("admin_restricted_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = stringResource(R.string.admin_restricted_title),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = stringResource(R.string.admin_restricted_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.admin_restricted_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (currentEmail.isNotBlank()) {
                Text(
                    text = "Current account: $currentEmail (Default Actor: Visitor)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
