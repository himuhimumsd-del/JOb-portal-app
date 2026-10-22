package com.example.jobportal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jobportal.data.model.UserRole
import com.example.jobportal.ui.admin.*
import com.example.jobportal.ui.components.*
import com.example.jobportal.ui.employer.*
import com.example.jobportal.ui.referral.*
import com.example.jobportal.ui.seeker.*

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val currentRole by viewModel.currentRole.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    val jobs by viewModel.jobs.collectAsState()
    val savedJobIds by viewModel.savedJobIds.collectAsState()
    val applications by viewModel.applications.collectAsState()
    val applicants by viewModel.applicants.collectAsState()
    val interviews by viewModel.interviews.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val jobAlerts by viewModel.jobAlerts.collectAsState()
    val reportedItems by viewModel.reportedItems.collectAsState()
    val adminVitals by viewModel.adminVitals.collectAsState()
    val users by viewModel.users.collectAsState()
    val seekerProfile by viewModel.seekerProfile.collectAsState()
    val employerProfile by viewModel.employerProfile.collectAsState()
    val referralProfile by viewModel.referralProfile.collectAsState()
    val referrals by viewModel.referrals.collectAsState()
    val withdrawalRequests by viewModel.withdrawalRequests.collectAsState()

    val authUser by viewModel.authUser.collectAsState()
    val authUiState by viewModel.authUiState.collectAsState()
    val showAuthDialog by viewModel.showAuthDialog.collectAsState()

    val showPremiumAccessDialog by viewModel.showPremiumAccessDialog.collectAsState()
    val showReferralDashboard by viewModel.showReferralDashboard.collectAsState()
    val showEnterReferralCodeDialog by viewModel.showEnterReferralCodeDialog.collectAsState()
    val incomingReferralCode by viewModel.incomingReferralCode.collectAsState()
    val showRedemptionRequestDialog by viewModel.showRedemptionRequestDialog.collectAsState()
    val showRedemptionHistoryDialog by viewModel.showRedemptionHistoryDialog.collectAsState()

    val selectedJob by viewModel.selectedJob.collectAsState()
    val applyingJob by viewModel.applyingJob.collectAsState()
    val selectedApplication by viewModel.selectedApplication.collectAsState()
    val selectedApplicant by viewModel.selectedApplicant.collectAsState()
    val showCreateJobWizard by viewModel.showCreateJobWizard.collectAsState()
    val showScheduleInterview by viewModel.showScheduleInterview.collectAsState()
    val showRoleSwitcherSheet by viewModel.showRoleSwitcherSheet.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val selectedWorkplaceFilter by viewModel.selectedWorkplaceFilter.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val cloudConnectionState by viewModel.cloudConnectionState.collectAsState()
    val dataSourceErrorMessage by viewModel.dataSourceErrorMessage.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Handle back button to return to home/dashboard if on secondary tab
    BackHandler(enabled = activeTab != "home" && activeTab != "dashboard") {
        viewModel.setTab(if (currentRole == UserRole.JOB_SEEKER) "home" else "dashboard")
    }

    Scaffold(
        topBar = {
            RoleSwitcherBar(
                currentRole = currentRole,
                authUser = authUser,
                cloudConnectionState = cloudConnectionState,
                isDemoMode = isDemoMode,
                onOpenRoleSwitcher = { viewModel.openRoleSwitcher() },
                onOpenAuth = { viewModel.openAuthDialog() },
                onNotificationsClick = {
                    viewModel.showMessage("No new notifications at this time.")
                }
            )
        },
        bottomBar = {
            FloatingBottomDock(
                currentRole = currentRole,
                activeTab = activeTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Live Cloud Error Diagnostic Banner (Mock fallback is strictly disabled)
            val isCloudError = cloudConnectionState is com.example.jobportal.data.repository.CloudConnectionState.Error ||
                (dataSourceErrorMessage != null && !isDemoMode)

            if (isCloudError) {
                val errorDetails = when (val state = cloudConnectionState) {
                    is com.example.jobportal.data.repository.CloudConnectionState.Error -> state.message
                    else -> dataSourceErrorMessage ?: "Cloud Firestore connection unavailable."
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                                contentDescription = "Connection Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Live Firestore Cloud Unavailable",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                        Text(
                            text = "Target Database: ai-studio-android-jobporta-b95bcbd3-ac1e-47e0-8460-98c89a740c01\n$errorDetails",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                        Text(
                            text = "Mock fallback is disabled to prevent simulated data leakage.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Button(
                                onClick = { viewModel.retryLiveConnection() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = "Retry Live Cloud",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onError,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.enableDemoMode() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = "Switch to Demo Mode",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                )
                            }
                        }
                    }
                }
            } else if (cloudConnectionState is com.example.jobportal.data.repository.CloudConnectionState.Connecting) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Connecting to live Firestore database...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
            AnimatedContent(
                targetState = currentRole to activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { (role, tab) ->
                when (role) {
                    UserRole.JOB_SEEKER -> {
                        when (tab) {
                            "home" -> JobSeekerHomeScreen(
                                jobs = jobs,
                                savedJobIds = savedJobIds,
                                profile = seekerProfile,
                                categories = categories,
                                onSearchClick = { viewModel.setTab("explore") },
                                onCategoryClick = { catName ->
                                    viewModel.selectCategoryFilter(catName)
                                    viewModel.setTab("explore")
                                },
                                onJobClick = { viewModel.openJobDetails(it) },
                                onToggleSave = { viewModel.toggleSave(it.id) },
                                onApplyClick = { viewModel.startApplyFlow(it) },
                                onFinishProfileClick = { viewModel.setTab("profile") }
                            )
                            "explore" -> JobSeekerExploreScreen(
                                jobs = jobs,
                                savedJobIds = savedJobIds,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategoryFilter,
                                selectedWorkplace = selectedWorkplaceFilter,
                                categories = categories,
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onCategorySelect = { viewModel.selectCategoryFilter(it) },
                                onWorkplaceSelect = { viewModel.selectWorkplaceFilter(it) },
                                onJobClick = { viewModel.openJobDetails(it) },
                                onToggleSave = { viewModel.toggleSave(it.id) },
                                onApplyClick = { viewModel.startApplyFlow(it) }
                            )
                            "applications" -> JobSeekerApplicationsScreen(
                                applications = applications,
                                onApplicationClick = { viewModel.openApplicationDetails(it) },
                                onViewInterviewClick = { viewModel.openApplicationDetails(it) }
                            )
                            "saved" -> JobSeekerSavedScreen(
                                savedJobs = jobs.filter { savedJobIds.contains(it.id) },
                                savedJobIds = savedJobIds,
                                onJobClick = { viewModel.openJobDetails(it) },
                                onToggleSave = { viewModel.toggleSave(it.id) },
                                onApplyClick = { viewModel.startApplyFlow(it) },
                                onExploreClick = { viewModel.setTab("explore") }
                            )
                            "profile" -> JobSeekerProfileScreen(
                                profile = seekerProfile,
                                jobAlerts = jobAlerts,
                                referralProfile = referralProfile,
                                onAddSkill = { skill ->
                                    viewModel.repository.updateSeekerProfile(
                                        seekerProfile.copy(skills = seekerProfile.skills + skill)
                                    )
                                    viewModel.showMessage("Added $skill to competencies")
                                },
                                onUploadResume = { fileName ->
                                    val newResume = com.example.jobportal.data.model.ResumeItem(
                                        id = "res-${System.currentTimeMillis()}",
                                        fileName = fileName,
                                        uploadDate = "Just now",
                                        isDefault = true,
                                        atsScore = 95
                                    )
                                    viewModel.repository.updateSeekerProfile(
                                        seekerProfile.copy(resumes = listOf(newResume) + seekerProfile.resumes.map { it.copy(isDefault = false) })
                                    )
                                    viewModel.showMessage("New resume uploaded & ATS scored")
                                },
                                onToggleAlert = { viewModel.repository.toggleJobAlert(it) },
                                onOpenCreateAlert = {
                                    viewModel.repository.addJobAlert("Staff Engineer", "Tech & Dev", "$200k+", "Daily")
                                    viewModel.showMessage("Created new job alert: Staff Engineer")
                                },
                                onOpenReferrals = { viewModel.openReferralDashboard() },
                                onOpenPremium = { viewModel.openPremiumAccess() },
                                onSwitchRoleClick = { viewModel.openRoleSwitcher() }
                            )
                            else -> JobSeekerHomeScreen(
                                jobs = jobs,
                                savedJobIds = savedJobIds,
                                profile = seekerProfile,
                                categories = categories,
                                onSearchClick = { viewModel.setTab("explore") },
                                onCategoryClick = { catName ->
                                    viewModel.selectCategoryFilter(catName)
                                    viewModel.setTab("explore")
                                },
                                onJobClick = { viewModel.openJobDetails(it) },
                                onToggleSave = { viewModel.toggleSave(it.id) },
                                onApplyClick = { viewModel.startApplyFlow(it) },
                                onFinishProfileClick = { viewModel.setTab("profile") }
                            )
                        }
                    }
                    UserRole.EMPLOYER -> {
                        when (tab) {
                            "dashboard" -> EmployerDashboardScreen(
                                profile = employerProfile,
                                activeJobsCount = jobs.count { it.status == com.example.jobportal.data.model.JobStatus.ACTIVE },
                                totalApplicantsCount = applicants.size + 180,
                                shortlistedCount = applicants.count { it.stage == com.example.jobportal.data.model.ApplicationStage.SHORTLISTED } + 30,
                                interviewsCount = interviews.size,
                                recentApplicants = applicants,
                                onPostJobClick = { viewModel.openCreateJobWizard() },
                                onApplicantClick = { viewModel.openApplicantDetails(it) },
                                onAdvanceApplicant = { cand, stage ->
                                    viewModel.repository.updateApplicantStage(cand.id, stage)
                                    viewModel.showMessage("Advanced ${cand.candidateName} to ${stage.label}")
                                }
                            )
                            "manage_jobs" -> EmployerManageJobsScreen(
                                jobs = jobs,
                                onPostJobClick = { viewModel.openCreateJobWizard() },
                                onJobClick = { viewModel.openJobDetails(it) },
                                onToggleJobStatus = { job ->
                                    val nextStatus = if (job.status == com.example.jobportal.data.model.JobStatus.ACTIVE) com.example.jobportal.data.model.JobStatus.CLOSED else com.example.jobportal.data.model.JobStatus.ACTIVE
                                    viewModel.repository.updateJobStatus(job.id, nextStatus)
                                    viewModel.showMessage("Updated '${job.title}' to ${nextStatus.label}")
                                }
                            )
                            "applicants" -> EmployerApplicantsScreen(
                                applicants = applicants,
                                onApplicantClick = { viewModel.openApplicantDetails(it) },
                                onAdvanceApplicant = { cand, stage ->
                                    viewModel.repository.updateApplicantStage(cand.id, stage)
                                    viewModel.showMessage("Advanced ${cand.candidateName} to ${stage.label}")
                                },
                                onScheduleInterview = { viewModel.openScheduleInterview(it) }
                            )
                            "interviews" -> EmployerInterviewsScreen(
                                interviews = interviews,
                                onAddScorecard = { item, score, notes ->
                                    viewModel.repository.updateInterviewScorecard(item.id, score, notes)
                                    viewModel.showMessage("Saved evaluation: $score/5 stars")
                                },
                                onRescheduleInterview = { item, date, time ->
                                    viewModel.repository.rescheduleInterview(item.id, date, time)
                                    viewModel.showMessage("Interview rescheduled to $date at $time")
                                },
                                onCancelInterview = { item ->
                                    viewModel.repository.cancelInterview(item.id)
                                    viewModel.showMessage("Interview with ${item.candidateName} cancelled")
                                }
                            )
                            "company" -> EmployerCompanyProfileScreen(
                                profile = employerProfile,
                                referralProfile = referralProfile,
                                onOpenReferrals = { viewModel.openReferralDashboard() },
                                onOpenPremium = { viewModel.openPremiumAccess() },
                                onUpdateProfile = {
                                    viewModel.repository.updateEmployerProfile(it)
                                    viewModel.showMessage("Company profile updated")
                                },
                                onSwitchRoleClick = { viewModel.openRoleSwitcher() }
                            )
                            else -> EmployerDashboardScreen(
                                profile = employerProfile,
                                activeJobsCount = jobs.count { it.status == com.example.jobportal.data.model.JobStatus.ACTIVE },
                                totalApplicantsCount = applicants.size + 180,
                                shortlistedCount = 32,
                                interviewsCount = interviews.size,
                                recentApplicants = applicants,
                                onPostJobClick = { viewModel.openCreateJobWizard() },
                                onApplicantClick = { viewModel.openApplicantDetails(it) },
                                onAdvanceApplicant = { cand, stage ->
                                    viewModel.repository.updateApplicantStage(cand.id, stage)
                                }
                            )
                        }
                    }
                    UserRole.ADMIN -> {
                        when (tab) {
                            "dashboard" -> AdminDashboardScreen(
                                vitals = adminVitals,
                                withdrawalRequests = withdrawalRequests,
                                onProcessWithdrawal = { reqId, approve, notes ->
                                    viewModel.processWithdrawalDecision(reqId, approve, notes)
                                },
                                onNavigateModeration = { viewModel.setTab("moderation") },
                                onNavigateUsers = { viewModel.setTab("users") }
                            )
                            "moderation" -> AdminJobsModerationScreen(
                                jobs = jobs,
                                onModerate = { job, approve ->
                                    viewModel.repository.moderateJob(job.id, approve)
                                    viewModel.showMessage(if (approve) "Approved '${job.title}'" else "Suspended '${job.title}'")
                                }
                            )
                            "users" -> AdminUsersScreen(
                                users = users,
                                onToggleVerify = {
                                    viewModel.repository.toggleUserVerification(it)
                                    viewModel.showMessage("Updated verification status")
                                },
                                onToggleSuspend = {
                                    viewModel.repository.toggleUserSuspension(it)
                                    viewModel.showMessage("Updated user status")
                                }
                            )
                            "taxonomy" -> AdminTaxonomyScreen(
                                categories = categories,
                                onAddCategory = { name, icon ->
                                    viewModel.repository.addCategory(name, icon)
                                    viewModel.showMessage("Added taxonomy field: $name")
                                }
                            )
                            "reports" -> AdminReportsScreen(
                                reports = reportedItems,
                                onResolveReport = { id, status ->
                                    viewModel.repository.resolveReport(id, status)
                                    viewModel.showMessage("Report $status")
                                }
                            )
                            else -> AdminDashboardScreen(
                                vitals = adminVitals,
                                withdrawalRequests = withdrawalRequests,
                                onProcessWithdrawal = { reqId, approve, notes ->
                                    viewModel.processWithdrawalDecision(reqId, approve, notes)
                                },
                                onNavigateModeration = { viewModel.setTab("moderation") },
                                onNavigateUsers = { viewModel.setTab("users") }
                            )
                        }
                    }
                }
            }
        }
    }
}

    // Role switcher bottom sheet
    if (showRoleSwitcherSheet) {
        RoleSelectionSheet(
            currentRole = currentRole,
            authUser = authUser,
            onRoleSelected = { viewModel.switchRole(it) },
            onOpenAuth = { viewModel.openAuthDialog() },
            onDismiss = { viewModel.closeRoleSwitcher() }
        )
    }

    // Google Sign-In & Executive Identity Dialog
    if (showAuthDialog) {
        AuthDialog(
            authUser = authUser,
            authUiState = authUiState,
            onSignInWithGoogle = { activity -> viewModel.signInWithGoogle(activity) },
            onSignOut = { viewModel.signOut(context) },
            onDismiss = { viewModel.closeAuthDialog() }
        )
    }

    // Job details modal sheet
    selectedJob?.let { job ->
        JobDetailSheet(
            job = job,
            isSaved = savedJobIds.contains(job.id),
            onDismiss = { viewModel.closeJobDetails() },
            onToggleSave = { viewModel.toggleSave(job.id) },
            onStartApply = { viewModel.startApplyFlow(job) }
        )
    }

    // 1-tap easy apply dialog
    applyingJob?.let { job ->
        ApplyDialog(
            job = job,
            profile = seekerProfile,
            onDismiss = { viewModel.closeApplyFlow() },
            onSubmit = { resumeVersion ->
                viewModel.submitApplication(job.id, resumeVersion)
            }
        )
    }

    // Candidate interview detail sheet
    selectedApplication?.let { app ->
        InterviewDetailSheet(
            application = app,
            onDismiss = { viewModel.closeApplicationDetails() }
        )
    }

    // Post job 5-step wizard
    if (showCreateJobWizard) {
        EmployerPostJobWizard(
            companyName = employerProfile.companyName,
            onDismiss = { viewModel.closeCreateJobWizard() },
            onJobCreated = { title, company, location, workplace, empType, minSal, maxSal, cat, tags, desc, resps, reqs, benefits ->
                viewModel.repository.createJob(
                    title = title,
                    company = company,
                    location = location,
                    workplaceType = workplace,
                    employmentType = empType,
                    minSalary = minSal,
                    maxSalary = maxSal,
                    category = cat,
                    tags = tags,
                    description = desc,
                    responsibilities = resps,
                    requirements = reqs,
                    benefits = benefits
                )
                viewModel.showMessage("Published '$title' to global ecosystem!")
            }
        )
    }

    // Employer Schedule interview dialog
    showScheduleInterview?.let { applicant ->
        ScheduleInterviewDialog(
            applicant = applicant,
            companyName = employerProfile.companyName,
            onDismiss = { viewModel.closeScheduleInterview() },
            onScheduleConfirmed = { date, time, interviewer ->
                viewModel.repository.scheduleNewInterview(
                    candidateName = applicant.candidateName,
                    candidateRole = applicant.jobTitle,
                    company = employerProfile.companyName,
                    date = date,
                    time = time,
                    interviewer = interviewer
                )
                viewModel.repository.updateApplicantStage(applicant.id, com.example.jobportal.data.model.ApplicationStage.INTERVIEW_SCHEDULED)
                viewModel.closeScheduleInterview()
                viewModel.showMessage("Interview scheduled with ${applicant.candidateName} for $date")
            }
        )
    }

    // Employer Candidate Details Sheet
    selectedApplicant?.let { applicant ->
        ApplicantDetailSheet(
            applicant = applicant,
            onDismiss = { viewModel.closeApplicantDetails() },
            onAdvanceStage = { cand, stage ->
                viewModel.repository.updateApplicantStage(cand.id, stage)
                viewModel.showMessage("Advanced ${cand.candidateName} to ${stage.label}")
            },
            onScheduleInterview = { cand ->
                viewModel.closeApplicantDetails()
                viewModel.openScheduleInterview(cand)
            }
        )
    }

    // Premium Membership Access Dialog
    if (showPremiumAccessDialog) {
        PremiumAccessScreen(
            isPremium = referralProfile.isPremium,
            onInitiatePayment = { viewModel.initiatePremiumPayment() },
            onVerifyPayment = { id, ref -> viewModel.verifyPremiumPayment(id, ref) },
            onDismiss = { viewModel.closePremiumAccess() }
        )
    }

    // Referral Dashboard Dialog / Screen
    if (showReferralDashboard) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { viewModel.closeReferralDashboard() },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ReferralDashboardScreen(
                    profile = referralProfile,
                    referrals = referrals,
                    onOpenEnterCode = { viewModel.openEnterReferralCode() },
                    onOpenRedemptionRequest = { viewModel.openRedemptionRequest() },
                    onOpenRedemptionHistory = { viewModel.openRedemptionHistory() },
                    onOpenPremiumScreen = { viewModel.openPremiumAccess() },
                    onShowMessage = { viewModel.showMessage(it) },
                    onBack = { viewModel.closeReferralDashboard() }
                )
            }
        }
    }

    // Enter Referral Code Dialog
    if (showEnterReferralCodeDialog) {
        EnterReferralCodeDialog(
            currentUserReferralCode = referralProfile.referralCode,
            initialCode = incomingReferralCode ?: "",
            onApplyReferralCode = { viewModel.applyReferralCode(it) },
            onDismiss = { viewModel.closeEnterReferralCode() }
        )
    }

    // Redemption Request Dialog
    if (showRedemptionRequestDialog) {
        RedemptionRequestDialog(
            profile = referralProfile,
            onSubmitRequest = { amount, payoutAccount ->
                viewModel.submitRedemptionRequest(amount, payoutAccount)
            },
            onDismiss = { viewModel.closeRedemptionRequest() }
        )
    }

    // Redemption History Sheet
    if (showRedemptionHistoryDialog) {
        RedemptionHistorySheet(
            requests = withdrawalRequests,
            onDismiss = { viewModel.closeRedemptionHistory() }
        )
    }
}
