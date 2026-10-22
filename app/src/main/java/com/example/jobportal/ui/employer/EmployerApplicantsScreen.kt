package com.example.jobportal.ui.employer

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
import com.example.jobportal.data.model.Applicant
import com.example.jobportal.data.model.ApplicationStage
import com.example.jobportal.ui.theme.*

@Composable
fun EmployerApplicantsScreen(
    applicants: List<Applicant>,
    onApplicantClick: (Applicant) -> Unit,
    onAdvanceApplicant: (Applicant, ApplicationStage) -> Unit,
    onScheduleInterview: (Applicant) -> Unit
) {
    var selectedStageFilter by remember { mutableStateOf<ApplicationStage?>(null) }
    var candidateForStageMenu by remember { mutableStateOf<Applicant?>(null) }

    val filteredApplicants = remember(applicants, selectedStageFilter) {
        if (selectedStageFilter == null) applicants else applicants.filter { it.stage == selectedStageFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("employer_applicants_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Candidate Talent Vetting",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "AI-ranked applicant pool and pipeline stage progression",
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
                        label = { Text("All Candidates (${applicants.size})") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                items(ApplicationStage.values()) { stage ->
                    val count = applicants.count { it.stage == stage }
                    FilterChip(
                        selected = selectedStageFilter == stage,
                        onClick = { selectedStageFilter = if (selectedStageFilter == stage) null else stage },
                        label = { Text("${stage.label} ($count)") },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        items(filteredApplicants) { applicant ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onApplicantClick(applicant) }
                    .testTag("applicant_vetting_item_${applicant.id}")
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = applicant.candidateName.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Column {
                                Text(
                                    text = applicant.candidateName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = applicant.headline,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${applicant.location} • ${applicant.experienceYears}y exp",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Outline, fontSize = 11.sp)
                                )
                            }
                        }

                        // ATS Match Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldStatus,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${applicant.atsScore}% ATS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = EmeraldText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    // Skills Tags
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(applicant.keySkills) { skill ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Text(
                                    text = skill,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

                    // Stage status and Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when (applicant.stage) {
                                ApplicationStage.APPLIED, ApplicationStage.IN_REVIEW -> AmberBg
                                ApplicationStage.SHORTLISTED -> VioletBg
                                ApplicationStage.INTERVIEW_SCHEDULED -> EmeraldBg
                                else -> PrimaryContainer
                            }
                        ) {
                            Text(
                                text = "Stage: ${applicant.stage.label}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = when (applicant.stage) {
                                        ApplicationStage.APPLIED, ApplicationStage.IN_REVIEW -> AmberText
                                        ApplicationStage.SHORTLISTED -> VioletText
                                        ApplicationStage.INTERVIEW_SCHEDULED -> EmeraldText
                                        else -> Color.White
                                    },
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { candidateForStageMenu = applicant },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Advance Stage ▾", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                            }

                            Button(
                                onClick = { onScheduleInterview(applicant) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryContainer,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Interview", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }

    // Stage advance menu
    candidateForStageMenu?.let { cand ->
        AlertDialog(
            onDismissRequest = { candidateForStageMenu = null },
            title = { Text("Advance ${cand.candidateName}'s Pipeline") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApplicationStage.values().forEach { st ->
                        Surface(
                            onClick = {
                                onAdvanceApplicant(cand, st)
                                candidateForStageMenu = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (cand.stage == st) PrimaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(st.label, fontWeight = if (cand.stage == st) FontWeight.Bold else FontWeight.Normal)
                                if (cand.stage == st) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { candidateForStageMenu = null }) {
                    Text("Close")
                }
            }
        )
    }
}
