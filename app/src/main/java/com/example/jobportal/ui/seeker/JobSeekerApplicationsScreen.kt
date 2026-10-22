package com.example.jobportal.ui.seeker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobportal.data.model.Application
import com.example.jobportal.data.model.ApplicationStage
import com.example.jobportal.ui.theme.*

@Composable
fun JobSeekerApplicationsScreen(
    applications: List<Application>,
    onApplicationClick: (Application) -> Unit,
    onViewInterviewClick: (Application) -> Unit
) {
    var selectedStageFilter by remember { mutableStateOf<ApplicationStage?>(null) }

    val filteredApps = remember(applications, selectedStageFilter) {
        if (selectedStageFilter == null) applications else applications.filter { it.status == selectedStageFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("my_applications_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Application Pipeline",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Track real-time hiring stages and interview briefings",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        // Filter chips row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedStageFilter == null,
                        onClick = { selectedStageFilter = null },
                        label = { Text("All (${applications.size})") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                items(ApplicationStage.values()) { stage ->
                    val count = applications.count { it.status == stage }
                    FilterChip(
                        selected = selectedStageFilter == stage,
                        onClick = { selectedStageFilter = if (selectedStageFilter == stage) null else stage },
                        label = { Text("${stage.label} ($count)") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        if (filteredApps.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkOutline,
                            contentDescription = null,
                            tint = Outline,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No applications in this stage",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Browse recommended positions on the Home or Explore feeds to submit new applications.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredApps) { app ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onApplicationClick(app) }
                        .testTag("application_card_${app.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.jobTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${app.company} • Submitted ${app.appliedDate}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            // Status badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = when (app.status) {
                                    ApplicationStage.APPLIED -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    ApplicationStage.IN_REVIEW -> AmberBg
                                    ApplicationStage.SHORTLISTED -> VioletBg
                                    ApplicationStage.INTERVIEW_SCHEDULED -> EmeraldBg
                                    ApplicationStage.OFFER -> PrimaryContainer
                                    ApplicationStage.REJECTED -> ErrorContainer
                                }
                            ) {
                                Text(
                                    text = app.status.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = when (app.status) {
                                            ApplicationStage.APPLIED -> MaterialTheme.colorScheme.onSurface
                                            ApplicationStage.IN_REVIEW -> AmberText
                                            ApplicationStage.SHORTLISTED -> VioletText
                                            ApplicationStage.INTERVIEW_SCHEDULED -> EmeraldText
                                            ApplicationStage.OFFER -> Color.White
                                            ApplicationStage.REJECTED -> OnErrorContainer
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Tailored Resume version used
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Outline,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = app.resumeVersion,
                                style = MaterialTheme.typography.bodySmall.copy(color = Outline, fontSize = 11.sp)
                            )
                        }

                        // Special Interview Scheduled Card Banner
                        if (app.status == ApplicationStage.INTERVIEW_SCHEDULED) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(EmeraldStatus)
                                            )
                                            Text(
                                                text = "Upcoming Interview",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Primary
                                                )
                                            )
                                        }
                                        Text(
                                            text = "${app.interviewDate ?: "Thursday, Oct 22"} at ${app.interviewTime ?: "2:30 PM PST"}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }

                                    Button(
                                        onClick = { onViewInterviewClick(app) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryContainer,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Briefing",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
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
