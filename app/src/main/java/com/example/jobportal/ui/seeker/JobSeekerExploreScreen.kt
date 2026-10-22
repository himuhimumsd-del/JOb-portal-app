package com.example.jobportal.ui.seeker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobportal.data.model.Job
import com.example.jobportal.data.model.WorkplaceType
import com.example.jobportal.ui.components.JobCardItem
import com.example.jobportal.ui.theme.Primary
import com.example.jobportal.ui.theme.Secondary

@Composable
fun JobSeekerExploreScreen(
    jobs: List<Job>,
    savedJobIds: Set<String>,
    searchQuery: String,
    selectedCategory: String?,
    selectedWorkplace: WorkplaceType?,
    categories: List<com.example.jobportal.data.model.CategoryItem> = emptyList(),
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onWorkplaceSelect: (WorkplaceType?) -> Unit,
    onJobClick: (Job) -> Unit,
    onToggleSave: (Job) -> Unit,
    onApplyClick: (Job) -> Unit
) {
    val filteredJobs = remember(jobs, searchQuery, selectedCategory, selectedWorkplace) {
        jobs.filter { job ->
            val matchesQuery = searchQuery.isBlank() ||
                job.title.contains(searchQuery, ignoreCase = true) ||
                job.company.contains(searchQuery, ignoreCase = true) ||
                job.description.contains(searchQuery, ignoreCase = true) ||
                job.tags.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesCategory = selectedCategory == null || job.category.equals(selectedCategory, ignoreCase = true)
            val matchesWorkplace = selectedWorkplace == null || job.workplaceType == selectedWorkplace

            matchesQuery && matchesCategory && matchesWorkplace
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("explore_jobs_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search Input Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input"),
                placeholder = { Text("Search by title, skills, or company...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        }

        // Workplace Type Filter Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Workplace Type",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedWorkplace == null,
                            onClick = { onWorkplaceSelect(null) },
                            label = { Text("All Workplaces") },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                    items(WorkplaceType.values()) { wp ->
                        FilterChip(
                            selected = selectedWorkplace == wp,
                            onClick = { onWorkplaceSelect(if (selectedWorkplace == wp) null else wp) },
                            label = { Text(wp.label) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        // Category Filter Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Industry Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                val categoryNames = if (categories.isNotEmpty()) categories.map { it.name } else listOf("Tech & Dev", "Product & UX", "Data & AI", "Sales & Growth")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onCategorySelect(null) },
                            label = { Text("All Fields") },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                    items(categoryNames) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onCategorySelect(if (selectedCategory == cat) null else cat) },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        // Search Results Count Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Matching Openings (${filteredJobs.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (searchQuery.isNotEmpty() || selectedCategory != null || selectedWorkplace != null) {
                    TextButton(onClick = {
                        onSearchQueryChange("")
                        onCategorySelect(null)
                        onWorkplaceSelect(null)
                    }) {
                        Text("Reset Filters", style = MaterialTheme.typography.labelSmall.copy(color = Primary))
                    }
                }
            }
        }

        // Empty state or jobs list
        if (filteredJobs.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WorkOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No jobs matched your filters",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Try adjusting your search keywords, workplace type, or industry category.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredJobs) { job ->
                JobCardItem(
                    job = job,
                    isSaved = savedJobIds.contains(job.id),
                    onJobClick = onJobClick,
                    onToggleSave = onToggleSave,
                    onApplyClick = onApplyClick
                )
            }
        }
    }
}
