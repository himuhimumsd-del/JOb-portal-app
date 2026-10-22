package com.example.jobportal.ui.employer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicantDetailSheet(
    applicant: Applicant,
    onDismiss: () -> Unit,
    onAdvanceStage: (Applicant, ApplicationStage) -> Unit,
    onScheduleInterview: (Applicant) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Avatar, Name, ATS Score & Close
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
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = applicant.candidateName.take(2).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Column {
                        Text(
                            text = applicant.candidateName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = applicant.headline,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "${applicant.location} • ${applicant.experienceYears}y experience",
                            style = MaterialTheme.typography.bodySmall.copy(color = Outline, fontSize = 11.sp)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Role Applied For Banner & Current Stage
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Target Role", style = MaterialTheme.typography.labelSmall.copy(color = Outline))
                        Text(text = applicant.jobTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(text = "Applied ${applicant.appliedTime}", style = MaterialTheme.typography.bodySmall.copy(color = Outline, fontSize = 11.sp))
                    }

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
                            text = applicant.stage.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = when (applicant.stage) {
                                    ApplicationStage.APPLIED, ApplicationStage.IN_REVIEW -> AmberText
                                    ApplicationStage.SHORTLISTED -> VioletText
                                    ApplicationStage.INTERVIEW_SCHEDULED -> EmeraldText
                                    else -> Color.White
                                },
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ATS Semantic Scoring Breakdown
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = EmeraldBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = EmeraldStatus,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "${applicant.atsScore}% Semantic Keyword & ATS Match",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = EmeraldText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Strong alignment with required competencies and architecture skills.",
                            style = MaterialTheme.typography.bodySmall.copy(color = EmeraldText)
                        )
                    }
                }
            }

            // Contact Info
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Contact Information", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Outline, modifier = Modifier.size(16.dp))
                    Text(text = applicant.email, style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Outline, modifier = Modifier.size(16.dp))
                    Text(text = applicant.phone, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Tailored Resume
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Submitted Resume Document", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = Primary)
                        Text(text = applicant.resumeFileName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Key Skills
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Evaluated Competencies", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    applicant.keySkills.forEach { skill ->
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Text(text = skill, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            // Pipeline Stage Transition Actions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Advance Pipeline Stage", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAdvanceStage(applicant, ApplicationStage.SHORTLISTED) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Shortlist", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { onAdvanceStage(applicant, ApplicationStage.OFFER) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Send Offer", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { onAdvanceStage(applicant, ApplicationStage.REJECTED) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reject", fontSize = 11.sp, color = Error)
                    }
                }
            }

            // Schedule Interview CTA
            Button(
                onClick = { onScheduleInterview(applicant) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryContainer,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("applicant_sheet_schedule_interview_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                    Text("Schedule Technical Panel Interview", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
