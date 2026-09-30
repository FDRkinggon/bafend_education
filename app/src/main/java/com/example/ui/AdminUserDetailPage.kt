package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountStatus
import com.example.data.model.AppActivity
import com.example.data.model.PlatformRole
import com.example.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminUserDetailPage(
    user: UserProfile,
    userActivities: List<AppActivity>,
    onBackToTable: () -> Unit,
    onChangeRole: (PlatformRole) -> Unit,
    onChangeStatus: (AccountStatus, String) -> Unit,
    onUpdatePersonalInfo: (
        displayName: String,
        headline: String,
        roleTitle: String,
        location: String,
        bio: String,
        statusMessage: String,
        phoneNumbers: String,
        schoolName: String
    ) -> Unit,
    onResetOnboarding: () -> Unit,
    onPurgeUserActivities: () -> Unit,
    onDeleteUser: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToTable)

    val effectiveRole = user.effectivePlatformRole
    val effectiveStatus = user.effectiveAccountStatus

    var statusReasonNote by rememberSaveable(user.userId) { mutableStateOf("") }
    var editDisplayName by rememberSaveable(user.userId, user.displayName) { mutableStateOf(user.displayName) }
    var editHeadline by rememberSaveable(user.userId, user.headline) { mutableStateOf(user.headline) }
    var editRoleTitle by rememberSaveable(user.userId, user.roleTitle) { mutableStateOf(user.roleTitle) }
    var editLocation by rememberSaveable(user.userId, user.location) { mutableStateOf(user.location) }
    var editPhoneNumbers by rememberSaveable(user.userId, user.phoneNumbers) { mutableStateOf(user.phoneNumbers) }
    var editSchoolName by rememberSaveable(user.userId, user.schoolName) { mutableStateOf(user.schoolName) }
    var editStatusMessage by rememberSaveable(user.userId, user.statusMessage) { mutableStateOf(user.statusMessage) }
    var editBio by rememberSaveable(user.userId, user.bio) { mutableStateOf(user.bio) }

    val dateFormatter = remember {
        SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_user_detail_page"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Bar: Back to Users Table
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onBackToTable,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("back_to_users_table_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Users Table",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back to Users Table", fontWeight = FontWeight.Bold)
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Admin User Control Page",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 1. User Identity & Personal Information Summary Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = statusAccentColor(effectiveStatus).copy(alpha = 0.55f),
                    shape = RoundedCornerShape(22.dp)
                )
                .testTag("managed_user_card_${user.userId}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
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
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("user_detail_name_text")
                        )
                        Text(
                            text = user.email.ifBlank { "No email on file" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "UID: ${user.userId} • ${userActivities.size} recorded activities",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Current Role & Account Status Badges Row
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("user_detail_role_badge")
                    ) {
                        Text(
                            text = "Role: ${effectiveRole.label}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    AccountStatusBadge(
                        status = effectiveStatus,
                        testTag = "user_status_badge_${user.userId}"
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (user.onboardingCompleted) "Onboarding: Completed" else "Onboarding: Pending",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Personal Info Snapshot Grid
                Text(
                    text = "Complete Personal Information Snapshot",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                PersonalInfoKeyValueRow("Headline / Tagline", user.headline.ifBlank { "Not specified" })
                PersonalInfoKeyValueRow("Role Title", user.roleTitle.ifBlank { effectiveRole.label })
                PersonalInfoKeyValueRow("Location", user.location.ifBlank { "Not specified" })
                PersonalInfoKeyValueRow("Status Message", user.statusMessage.ifBlank { effectiveStatus.label })
                if (user.phoneNumbers.isNotBlank()) {
                    PersonalInfoKeyValueRow("Phone Number(s)", user.phoneNumbers)
                }
                if (user.schoolName.isNotBlank() || user.educationSystem.isNotBlank()) {
                    val eduSummary = listOf(user.schoolName, user.educationSystem, user.studentLevel, user.studentStream)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    PersonalInfoKeyValueRow("Institution & Academic Track", eduSummary)
                }
                if (user.bio.isNotBlank()) {
                    PersonalInfoKeyValueRow("Bio / About", user.bio)
                }
            }
        }

        // 2. Account Access, Suspension & Blocking Control Card
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
                .testTag("admin_user_status_actions_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Account Status Actions",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Account Access, Suspension & Blocking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Activate user activities, place on pending hold, suspend temporarily, or block completely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = statusReasonNote,
                    onValueChange = { statusReasonNote = it },
                    label = { Text("Optional Admin Reason / Security Note (for Suspension, Block, or Pending)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_status_reason_input")
                )

                // Row 1: Set Active & Set Pending
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onChangeStatus(AccountStatus.ACTIVE, statusReasonNote) },
                        enabled = effectiveStatus != AccountStatus.ACTIVE,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("set_user_active_${user.userId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Activate User",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set Active", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onChangeStatus(AccountStatus.PENDING, statusReasonNote) },
                        enabled = effectiveStatus != AccountStatus.PENDING && !user.isDesignatedAdminEmail,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("set_user_pending_${user.userId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = "Set Pending",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set Pending", fontWeight = FontWeight.Bold)
                    }
                }

                // Row 2: Suspend Account & Block Account
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onChangeStatus(AccountStatus.SUSPENDED, statusReasonNote) },
                        enabled = effectiveStatus != AccountStatus.SUSPENDED && !user.isDesignatedAdminEmail,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("set_user_suspended_${user.userId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PauseCircleFilled,
                            contentDescription = "Suspend User",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Suspend Account", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onChangeStatus(AccountStatus.BLOCKED, statusReasonNote) },
                        enabled = effectiveStatus != AccountStatus.BLOCKED && !user.isDesignatedAdminEmail,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("set_user_blocked_${user.userId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Block User",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Block Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Role & Privilege Management Card (Anything -> Admin)
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
                .testTag("admin_user_role_actions_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "Change Role",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Role Changing (Promote Anything to Admin)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Current role: ${effectiveRole.label}. Select a role below to reassign immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlatformRole.entries.forEach { roleOption ->
                        val isSelected = effectiveRole == roleOption
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    onChangeRole(roleOption)
                                }
                            },
                            leadingIcon = if (roleOption == PlatformRole.ADMIN) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = "Admin Role",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = roleOption.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (roleOption == PlatformRole.ADMIN) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("change_role_${user.userId}_${roleOption.code}")
                        )
                    }
                }
            }
        }

        // 4. Edit User Personal Information Card (Admin Override)
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
                .testTag("admin_edit_personal_info_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Personal Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Edit User Personal Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Update the user's personal profile attributes, school, contact details, or status banner.",
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
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_user_name_input")
                    )
                    OutlinedTextField(
                        value = editRoleTitle,
                        onValueChange = { editRoleTitle = it },
                        label = { Text("Role Title") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_user_title_input")
                    )
                }

                OutlinedTextField(
                    value = editHeadline,
                    onValueChange = { editHeadline = it },
                    label = { Text("Headline") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_edit_user_headline_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("Location") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_user_location_input")
                    )
                    OutlinedTextField(
                        value = editPhoneNumbers,
                        onValueChange = { editPhoneNumbers = it },
                        label = { Text("Phone Number(s)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_user_phone_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editSchoolName,
                        onValueChange = { editSchoolName = it },
                        label = { Text("School / Institution") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_user_school_input")
                    )
                    OutlinedTextField(
                        value = editStatusMessage,
                        onValueChange = { editStatusMessage = it },
                        label = { Text("Status / Admin Banner") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_user_status_message_input")
                    )
                }

                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text("Biography / Personal Notes") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_edit_user_bio_input")
                )

                Button(
                    onClick = {
                        onUpdatePersonalInfo(
                            editDisplayName,
                            editHeadline,
                            editRoleTitle,
                            editLocation,
                            editBio,
                            editStatusMessage,
                            editPhoneNumbers,
                            editSchoolName
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("admin_save_user_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save Personal Info",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Personal Info Changes", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 5. Additional Admin Maintenance & Danger Zone Actions Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(22.dp)
                )
                .testTag("admin_advanced_user_actions_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Advanced Administrative & Account Lifecycle Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Reset user role onboarding, clear recorded activity logs, or permanently delete this user profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onResetOnboarding,
                        enabled = !user.isDesignatedAdminEmail,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("admin_reset_onboarding_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Onboarding",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Onboarding")
                    }

                    OutlinedButton(
                        onClick = onPurgeUserActivities,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("admin_purge_user_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Purge User Logs",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Purge Logs (${userActivities.size})")
                    }
                }

                Button(
                    onClick = onDeleteUser,
                    enabled = !user.isDesignatedAdminEmail,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("delete_user_${user.userId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Delete User Account",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Permanently Delete User Account", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 6. User's Recent Activity & Security Telemetry Trail
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
                .testTag("admin_user_activity_history_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "User Activity Trail",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "User Activity & Security Audit Trail (${userActivities.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (userActivities.isEmpty()) {
                    Text(
                        text = "No recorded activities for this user yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    userActivities.take(10).forEach { act ->
                        val timeText = act.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "Recent"
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = act.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = timeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = act.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (act.ipAddress.isNotBlank() || act.targetPath.isNotBlank()) {
                                    Text(
                                        text = "IP: ${act.ipAddress.ifBlank { "10.0.2.15" }} • Target: ${act.targetPath.ifBlank { "/users/${user.userId}" }} • DB Status: ${act.dbStatus.ifBlank { "SUCCESS" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
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

@Composable
private fun PersonalInfoKeyValueRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(150.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AccountStatusBadge(
    status: AccountStatus,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, badgeText) = when (status) {
        AccountStatus.ACTIVE -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "ACTIVE"
        )
        AccountStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "PENDING (LOCKED)"
        )
        AccountStatus.SUSPENDED -> Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
            MaterialTheme.colorScheme.onErrorContainer,
            "SUSPENDED"
        )
        AccountStatus.BLOCKED -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
            "BLOCKED"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        modifier = modifier.testTag(testTag)
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun statusAccentColor(status: AccountStatus): Color {
    return when (status) {
        AccountStatus.ACTIVE -> MaterialTheme.colorScheme.secondary
        AccountStatus.PENDING -> MaterialTheme.colorScheme.tertiary
        AccountStatus.SUSPENDED -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        AccountStatus.BLOCKED -> MaterialTheme.colorScheme.error
    }
}
