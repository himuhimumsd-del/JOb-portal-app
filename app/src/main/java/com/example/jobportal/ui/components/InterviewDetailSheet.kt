package com.example.jobportal.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobportal.data.model.Application
import com.example.jobportal.data.model.ApplicationStage
import com.example.jobportal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewDetailSheet(
    application: Application,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isInterviewScheduled = application.status == ApplicationStage.INTERVIEW_SCHEDULED

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isInterviewScheduled) SecondaryContainer else PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isInterviewScheduled) Icons.Default.VideoCall else Icons.Default.Work,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Text(
                        text = if (isInterviewScheduled) "Interview Briefing" else "Application Status Tracker",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Hero Card for Meeting Date/Time or Application Stage
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when (application.status) {
                                ApplicationStage.INTERVIEW_SCHEDULED -> EmeraldBg
                                ApplicationStage.SHORTLISTED -> VioletBg
                                ApplicationStage.OFFER -> PrimaryContainer
                                else -> AmberBg
                            }
                        ) {
                            Text(
                                text = application.status.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = when (application.status) {
                                        ApplicationStage.INTERVIEW_SCHEDULED -> EmeraldText
                                        ApplicationStage.SHORTLISTED -> VioletText
                                        ApplicationStage.OFFER -> Color.White
                                        else -> AmberText
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = if (isInterviewScheduled) "45 mins" else "Submitted ${application.appliedDate}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Outline)
                        )
                    }

                    Text(
                        text = application.jobTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${application.company} • Executive Role",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Divider(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    if (isInterviewScheduled) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Primary
                            )
                            Text(
                                text = application.interviewDate ?: "Thursday, Oct 22",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "•",
                                color = Outline
                            )
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Primary
                            )
                            Text(
                                text = application.interviewTime ?: "2:30 PM PST",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = Primary)
                            Text(
                                text = "Attached: ${application.resumeVersion}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }

            if (isInterviewScheduled) {
                // Join Video Call Button
                Button(
                    onClick = {
                        val url = application.interviewMeetingUrl ?: "https://meet.google.com"
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // ignore if no browser in container
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryContainer,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("join_video_call_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VideoCall, contentDescription = null)
                        Text("Join Google Meet Call", fontWeight = FontWeight.Bold)
                    }
                }

                // Prep checklist & notes
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Preparation Checklist",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    val items = listOf(
                        "Test camera, microphone, and background lighting in advance",
                        "Have your portfolio & project architecture diagrams ready for screen sharing",
                        "Prepare 2-3 thoughtful questions about company engineering philosophy",
                        application.prepNotes ?: "Review recent platform updates and mobile architecture challenges"
                    )

                    items.forEach { tip ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldStatus,
                                modifier = Modifier
                                    .size(18.dp)
                                    .offset(y = 2.dp)
                            )
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            } else {
                // Application timeline steps
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Application Timeline",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    val stages = listOf(
                        "Application Submitted" to "Received and indexed in talent database",
                        "ATS Semantic Screening" to "Resume matched with core requirements",
                        "Recruiter & Hiring Manager Review" to if (application.status == ApplicationStage.APPLIED) "Pending review (typically 3-5 days)" else "Completed",
                        "Technical Panel Interview" to if (application.status == ApplicationStage.SHORTLISTED) "Next step: Scheduling soon" else "Awaiting shortlist"
                    )

                    stages.forEachIndexed { idx, (stageTitle, stageDesc) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (idx == 0 || (idx == 1 && application.status != ApplicationStage.APPLIED)) Primary else MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${idx + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(stageTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text(stageDesc, style = MaterialTheme.typography.bodySmall.copy(color = Outline, fontSize = 11.sp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
