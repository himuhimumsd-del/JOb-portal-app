package com.example.jobportal.ui.employer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.jobportal.data.model.EmploymentType
import com.example.jobportal.data.model.WorkplaceType
import com.example.jobportal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerPostJobWizard(
    companyName: String,
    onDismiss: () -> Unit,
    onJobCreated: (
        title: String,
        company: String,
        location: String,
        workplaceType: WorkplaceType,
        employmentType: EmploymentType,
        minSalary: Int,
        maxSalary: Int,
        category: String,
        tags: List<String>,
        description: String,
        responsibilities: List<String>,
        requirements: List<String>,
        benefits: List<String>
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    // Form fields
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("San Francisco, CA") }
    var workplaceType by remember { mutableStateOf(WorkplaceType.REMOTE) }
    var employmentType by remember { mutableStateOf(EmploymentType.FULL_TIME) }
    var category by remember { mutableStateOf("Tech & Dev") }

    var minSalary by remember { mutableIntStateOf(160000) }
    var maxSalary by remember { mutableIntStateOf(210000) }
    var skillInput by remember { mutableStateOf("Kotlin, Compose, Distributed Systems") }

    var description by remember { mutableStateOf("We are seeking an exceptional engineer to lead mission-critical platform initiatives and mentor a team of high-performing developers.") }

    var publishedSuccess by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header with Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (publishedSuccess) "Job Published!" else "Post Executive Job (Step $step of 5)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = when (step) {
                            1 -> "Role Basics & Workplace"
                            2 -> "Skills & Requirements"
                            3 -> "Compensation & Benefits"
                            4 -> "ATS Screening & Process"
                            else -> "Review & Live Publish"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Progress Indicator
            LinearProgressIndicator(
                progress = { step / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(6.dp)
                    .clip(CircleShape),
                color = Primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainer
            )

            // Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (publishedSuccess) {
                    // Step 5: Published Confirmation
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(EmeraldBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldStatus,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Text(
                            text = "Opening is Now Live!",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "'$title' has been indexed and distributed to 124,000+ active candidates on भर्ती-App.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Compensation: $$minSalary - $$maxSalary", fontWeight = FontWeight.Bold)
                                Text(text = "Location: $location • ${workplaceType.label}", color = Outline)
                                Text(text = "Company: $companyName", color = Outline)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("wizard_done_button")
                        ) {
                            Text("Done & Return to Dashboard", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    when (step) {
                        1 -> {
                            // Step 1: Basics
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Job Title *") },
                                placeholder = { Text("e.g. Staff Distributed Systems Engineer") },
                                modifier = Modifier.fillMaxWidth().testTag("wizard_job_title_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Primary Location *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Workplace Classification", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WorkplaceType.values().forEach { wp ->
                                        FilterChip(
                                            selected = workplaceType == wp,
                                            onClick = { workplaceType = wp },
                                            label = { Text(wp.label) },
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Employment Agreement", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    EmploymentType.values().take(3).forEach { emp ->
                                        FilterChip(
                                            selected = employmentType == emp,
                                            onClick = { employmentType = emp },
                                            label = { Text(emp.label) },
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Step 2: Requirements & Skills
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Role Summary & Mission *") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 5,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = skillInput,
                                onValueChange = { skillInput = it },
                                label = { Text("Required Competencies & Skills (comma separated)") },
                                placeholder = { Text("Kotlin, Architecture, GraphQL, Docker") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Industry Classification", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                val catOptions = listOf("Tech & Dev", "Product & UX", "Data & AI", "Sales & Growth")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    catOptions.take(2).forEach { c ->
                                        FilterChip(
                                            selected = category == c,
                                            onClick = { category = c },
                                            label = { Text(c) }
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    catOptions.drop(2).forEach { c ->
                                        FilterChip(
                                            selected = category == c,
                                            onClick = { category = c },
                                            label = { Text(c) }
                                        )
                                    }
                                }
                            }
                        }
                        3 -> {
                            // Step 3: Compensation & Benefits
                            Text(
                                text = "Base Salary Range: $${minSalary / 1000}k - $${maxSalary / 1000}k",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Primary)
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Minimum Salary: $$minSalary", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = minSalary.toFloat(),
                                    onValueChange = { minSalary = it.toInt() },
                                    valueRange = 80000f..250000f,
                                    steps = 16
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Maximum Salary: $$maxSalary", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = maxSalary.coerceAtLeast(minSalary + 10000).toFloat(),
                                    onValueChange = { maxSalary = it.toInt() },
                                    valueRange = 100000f..350000f,
                                    steps = 24
                                )
                            }

                            Text("Executive Perks Package", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            val perks = listOf(
                                "100% Comprehensive Health, Dental & Vision Coverage",
                                "401(k) Retirement Plan with 6% Employer Match",
                                "$3,000 Annual Remote Office Setup & Technology Stipend",
                                "Quarterly Equity Grants & Performance Bonus"
                            )
                            perks.forEach { perk ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                    Text(text = perk, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        4 -> {
                            // Step 4: Application Flow & ATS
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldStatus)
                                    Column {
                                        Text("AI ATS Scoring Enabled", fontWeight = FontWeight.Bold, color = EmeraldText)
                                        Text("Candidates will be automatically ranked based on semantic relevancy to this job description.", style = MaterialTheme.typography.bodySmall.copy(color = EmeraldText))
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = "What major high-scale system have you designed or led?",
                                onValueChange = {},
                                label = { Text("Screening Question 1") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = "Briefly describe your experience with distributed event streaming.",
                                onValueChange = {},
                                label = { Text("Screening Question 2") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        5 -> {
                            // Step 5: Review & Publish
                            Text("Live Candidate Preview", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(text = title.ifBlank { "Senior Lead Architect" }, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "$companyName • $location • ${workplaceType.label}", color = Outline)
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                        Text(
                                            text = "$${minSalary / 1000}k - $${maxSalary / 1000}k",
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(text = description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Nav Buttons
            if (!publishedSuccess) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = { step -= 1 },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                    }

                    Button(
                        onClick = {
                            if (step < 5) {
                                if (step == 1 && title.isBlank()) {
                                    title = "Senior Platform Architect"
                                }
                                step += 1
                            } else {
                                // Final Publish
                                onJobCreated(
                                    title.ifBlank { "Senior Platform Architect" },
                                    companyName,
                                    location,
                                    workplaceType,
                                    employmentType,
                                    minSalary,
                                    maxSalary,
                                    category,
                                    skillInput.split(",").map { it.trim() },
                                    description,
                                    listOf("Lead technical roadmap", "Partner with executive teams"),
                                    listOf("6+ years high-scale experience", "Strong system design background"),
                                    listOf("Medical, Dental & Vision", "401k 6% Match", "Remote Stipend")
                                )
                                publishedSuccess = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryContainer,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(if (step > 1) 1.5f else 1f).testTag("wizard_next_publish_button")
                    ) {
                        Text(if (step == 5) "Publish Job Posting" else "Next Step", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
