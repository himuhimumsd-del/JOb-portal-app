package com.example.jobportal.ui.employer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jobportal.data.model.Applicant
import com.example.jobportal.ui.theme.PrimaryContainer

@Composable
fun ScheduleInterviewDialog(
    applicant: Applicant,
    companyName: String,
    onDismiss: () -> Unit,
    onScheduleConfirmed: (date: String, time: String, interviewer: String) -> Unit
) {
    var date by remember { mutableStateOf("Tomorrow, Oct 20") }
    var time by remember { mutableStateOf("11:00 AM PST") }
    var interviewer by remember { mutableStateOf("Sarah Chen (Engineering VP)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Schedule Executive Interview")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Candidate: ${applicant.candidateName} (${applicant.jobTitle})",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Interview Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Interview Time Slot") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = interviewer,
                    onValueChange = { interviewer = it },
                    label = { Text("Lead Interviewer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onScheduleConfirmed(date, time, interviewer)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_schedule_interview_button")
            ) {
                Text("Send Invite", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
