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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AuraOfflineDatabase
import com.example.data.local.SqliteOfflineTelemetry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfflineSqliteSyncBanner(
    telemetry: SqliteOfflineTelemetry,
    onToggleManualOfflineMode: (Boolean) -> Unit,
    onTriggerManualFirebaseSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val isOnline = telemetry.isEffectivelyOnline
    val hasPendingSync = telemetry.pendingSyncCount > 0

    val containerColor = when {
        !isOnline -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        hasPendingSync -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        !isOnline -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
        hasPendingSync -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.2.dp, color = borderColor, shape = RoundedCornerShape(18.dp))
            .testTag("sqlite_offline_sync_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .testTag("sqlite_banner_header_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOnline) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (telemetry.isSyncingNow) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    !isOnline -> Icons.Default.CloudOff
                                    hasPendingSync -> Icons.Default.CloudSync
                                    else -> Icons.Default.CloudDone
                                },
                                contentDescription = null,
                                tint = if (isOnline) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isOnline) {
                                    "SQLite Phone Memory + Online Firebase Sync"
                                } else {
                                    "Offline Mode Active (SQLite Phone Memory)"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("sqlite_sync_mode_title")
                            )
                            Surface(
                                color = if (hasPendingSync) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (hasPendingSync) {
                                        "${telemetry.pendingSyncCount} Queued"
                                    } else {
                                        "${telemetry.totalCachedRecords} Cached"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasPendingSync) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .testTag("sqlite_sync_badge_count")
                                )
                            }
                        }

                        Text(
                            text = telemetry.lastSyncSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = telemetry.isManualOfflineMode,
                        onClick = { onToggleManualOfflineMode(!telemetry.isManualOfflineMode) },
                        label = {
                            Text(
                                text = if (telemetry.isManualOfflineMode) "Go Online" else "Work Offline",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_sqlite_offline_mode_button")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand SQLite details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = expanded || !isOnline || hasPendingSync) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Phone SQLite DB: ${AuraOfflineDatabase.DATABASE_NAME}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (telemetry.lastSyncedEpochMs != null) {
                            val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.US)
                                .format(Date(telemetry.lastSyncedEpochMs))
                            Text(
                                text = "Last Synced: $formattedTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SqliteMetricPill(
                            label = "Profiles in SQLite",
                            count = telemetry.cachedProfilesCount,
                            tag = "sqlite_cached_profiles_pill"
                        )
                        SqliteMetricPill(
                            label = "Activities in SQLite",
                            count = telemetry.cachedActivitiesCount,
                            tag = "sqlite_cached_activities_pill"
                        )
                        SqliteMetricPill(
                            label = "Subjects in SQLite",
                            count = telemetry.cachedSubjectsCount,
                            tag = "sqlite_cached_subjects_pill"
                        )
                        SqliteMetricPill(
                            label = ".bafend Docs in SQLite",
                            count = telemetry.cachedRevisionDocsCount,
                            tag = "sqlite_cached_docs_pill"
                        )
                        SqliteMetricPill(
                            label = "Pending Firebase Sync",
                            count = telemetry.pendingSyncCount,
                            tag = "sqlite_pending_sync_pill"
                        )
                    }

                    if (telemetry.pendingActions.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(10.dp)
                                .testTag("sqlite_pending_outbox_list"),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Offline Actions Saved in SQLite (Queued for Firebase Sync):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            telemetry.pendingActions.take(4).forEach { item ->
                                Text(
                                    text = "• ${item.summaryLabel} [${item.targetCollection}/${item.targetDocumentId}]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onTriggerManualFirebaseSync,
                            enabled = !telemetry.isSyncingNow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("sync_sqlite_to_firebase_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (telemetry.isSyncingNow) {
                                    "Synchronizing…"
                                } else if (hasPendingSync) {
                                    "Sync ${telemetry.pendingSyncCount} Offline Change(s) to Firebase"
                                } else {
                                    "Verify & Sync SQLite with Firebase"
                                }
                            )
                        }

                        if (!isOnline && telemetry.isManualOfflineMode) {
                            OutlinedButton(
                                onClick = { onToggleManualOfflineMode(false) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reconnect_online_and_sync_button")
                            ) {
                                Text("Reconnect & Auto-Sync")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SqliteMetricPill(
    label: String,
    count: Int,
    tag: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag(tag)
    ) {
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
