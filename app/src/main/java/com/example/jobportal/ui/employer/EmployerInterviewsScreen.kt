package com.example.jobportal.ui.employer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.jobportal.data.model.InterviewItem
import com.example.jobportal.ui.theme.*

@Composable
fun EmployerInterviewsScreen(
    interviews: List<InterviewItem>,
    onAddScorecard: (InterviewItem, Int, String) -> Unit,
    onRescheduleInterview: (InterviewItem, String, String) -> Unit = { _, _, _ -> },
    onCancelInterview: (InterviewItem) -> Unit = {}
) {
    var scorecardTarget by remember { mutableStateOf<InterviewItem?>(null) }
    var scoreInput by remember { mutableIntStateOf(5) }
    var notesInput by remember { mutableStateOf("") }

    var rescheduleTarget by remember { mutableStateOf<InterviewItem?>(null) }
    var newDateInput by remember { mutableStateOf("") }
    var newTimeInput by remember { mutableStateOf("") }

    var cancelTarget by remember { mutableStateOf<InterviewItem?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("employer_interviews_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Scheduled Interviews (${interviews.size})",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Coordinate technical panels, scorecards, and executive evaluations",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        items(interviews) { item ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().testTag("interview_card_${item.id}")
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
                                text = item.candidateName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${item.candidateRole} • ${item.duration}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = "Lead: ${item.interviewerName}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Outline, fontSize = 11.sp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when (item.status) {
                                "Cancelled" -> ErrorContainer
                                "Rescheduled" -> AmberBg
                                else -> EmeraldBg
                            }
                        ) {
                            Text(
                                text = item.status,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = when (item.status) {
                                        "Cancelled" -> OnErrorContainer
                                        "Rescheduled" -> AmberText
                                        else -> EmeraldText
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Meeting time badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                            Text(text = "${item.date} • ${item.time}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    if (item.feedbackNotes.isNotBlank() || item.scoreTechnical > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = VioletBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Scorecard: ${item.scoreTechnical}/5 Stars",
                                    style = MaterialTheme.typography.labelSmall.copy(color = VioletText, fontWeight = FontWeight.Bold)
                                )
                                if (item.feedbackNotes.isNotBlank()) {
                                    Text(
                                        text = item.feedbackNotes,
                                        style = MaterialTheme.typography.bodySmall.copy(color = VioletText)
                                    )
                                }
                            }
                        }
                    }

                    // Actions
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scorecardTarget = item
                                notesInput = item.feedbackNotes
                                scoreInput = if (item.scoreTechnical in 1..5) item.scoreTechnical else 5
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scorecard", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(item.meetingUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryContainer,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Launch Call", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    // Secondary action buttons: Reschedule and Cancel
                    if (item.status != "Cancelled") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    rescheduleTarget = item
                                    newDateInput = item.date
                                    newTimeInput = item.time
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reschedule", fontSize = 11.sp)
                            }

                            TextButton(
                                onClick = { cancelTarget = item },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = Error, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", fontSize = 11.sp, color = Error)
                            }
                        }
                    }
                }
            }
        }
    }

    scorecardTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { scorecardTarget = null },
            title = { Text("Candidate Scorecard: ${target.candidateName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Technical & Architecture Rating (1 - 5)")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { scoreInput = star }) {
                                Icon(
                                    imageVector = if (star <= scoreInput) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (star <= scoreInput) AmberStatus else Outline
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Evaluation Feedback & Hiring Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddScorecard(target, scoreInput, notesInput)
                        scorecardTarget = null
                    }
                ) {
                    Text("Save Evaluation")
                }
            },
            dismissButton = {
                TextButton(onClick = { scorecardTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    rescheduleTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { rescheduleTarget = null },
            title = { Text("Reschedule Interview: ${target.candidateName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newDateInput,
                        onValueChange = { newDateInput = it },
                        label = { Text("New Date") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newTimeInput,
                        onValueChange = { newTimeInput = it },
                        label = { Text("New Time Slot") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDateInput.isNotBlank() && newTimeInput.isNotBlank()) {
                            onRescheduleInterview(target, newDateInput, newTimeInput)
                            rescheduleTarget = null
                        }
                    }
                ) {
                    Text("Confirm Reschedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { rescheduleTarget = null }) {
                    Text("Back")
                }
            }
        )
    }

    cancelTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Cancel Interview") },
            text = {
                Text("Are you sure you want to cancel the interview session with ${target.candidateName}? The candidate's application will be moved back to In Review.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelInterview(target)
                        cancelTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) {
                    Text("Keep Interview")
                }
            }
        )
    }
}
