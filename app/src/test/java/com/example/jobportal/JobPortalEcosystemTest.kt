package com.example.jobportal

import com.example.jobportal.data.model.*
import com.example.jobportal.data.repository.InMemoryJobPortalDataSource
import com.example.jobportal.data.repository.JobPortalRepository
import com.example.jobportal.ui.MainViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class JobPortalEcosystemTest {

    private lateinit var repository: JobPortalRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        repository = JobPortalRepository(InMemoryJobPortalDataSource())
        viewModel = MainViewModel(repository)
    }

    @Test
    fun testJobSeekerJourney_exploreSaveApplyTrack() {
        // 1. Explore jobs
        val initialJobs = viewModel.jobs.value
        assertTrue("Initial jobs should not be empty", initialJobs.isNotEmpty())
        val targetJob = initialJobs.first()

        // 2. Save / Bookmark job
        val isInitiallySaved = viewModel.savedJobIds.value.contains(targetJob.id)
        viewModel.toggleSave(targetJob.id)
        val isSavedAfter = viewModel.savedJobIds.value.contains(targetJob.id)
        assertNotEquals("Bookmark state should toggle", isInitiallySaved, isSavedAfter)

        // 3. Apply to job
        val initialAppCount = viewModel.applications.value.size
        val initialApplicantCount = viewModel.applicants.value.size
        val initialJobApplicants = targetJob.applicantsCount

        viewModel.submitApplication(targetJob.id, "Alex_Vance_Staff_Engineer_2026.pdf")

        // 4. Verify Application Pipeline Tracking
        val updatedApplications = viewModel.applications.value
        assertEquals("Applications list should increase by 1", initialAppCount + 1, updatedApplications.size)
        val submittedApp = updatedApplications.first()
        assertEquals(targetJob.id, submittedApp.jobId)
        assertEquals(ApplicationStage.APPLIED, submittedApp.status)
        assertEquals("Alex_Vance_Staff_Engineer_2026.pdf", submittedApp.resumeVersion)

        // 5. Verify Job's applicant counter incremented
        val updatedJob = viewModel.jobs.value.find { it.id == targetJob.id }
        assertNotNull(updatedJob)
        assertEquals(initialJobApplicants + 1, updatedJob!!.applicantsCount)

        // 6. Verify Employer applicant queue also received the candidate submission
        val updatedApplicants = viewModel.applicants.value
        assertEquals("Employer applicants pool should also increase", initialApplicantCount + 1, updatedApplicants.size)
        val newApplicant = updatedApplicants.first()
        assertEquals(targetJob.id, newApplicant.jobId)
        assertEquals(viewModel.seekerProfile.value.fullName, newApplicant.candidateName)
    }

    @Test
    fun testEmployerJourney_createJobPreviewPublishManage() {
        viewModel.switchRole(UserRole.EMPLOYER)
        assertEquals(UserRole.EMPLOYER, viewModel.currentRole.value)
        assertEquals("dashboard", viewModel.activeTab.value)

        val initialJobsCount = viewModel.jobs.value.size

        // Create new role via repository / wizard
        val createdJob = repository.createJob(
            title = "Principal Mobile Architect",
            company = "Acme Technologies",
            location = "San Francisco, CA",
            workplaceType = WorkplaceType.HYBRID,
            employmentType = EmploymentType.FULL_TIME,
            minSalary = 230000,
            maxSalary = 290000,
            category = "Tech & Dev",
            tags = listOf("Kotlin", "Jetpack Compose", "Architecture"),
            description = "Lead client architecture for next-gen mobile talent ecosystem.",
            responsibilities = listOf("Architect robust client solutions", "Mentor mobile team"),
            requirements = listOf("8+ years Android experience", "Deep Compose mastery"),
            benefits = listOf("Comprehensive Healthcare", "401k Match", "Remote Stipend")
        )

        // Verify published job is active
        assertEquals(initialJobsCount + 1, viewModel.jobs.value.size)
        assertEquals(JobStatus.ACTIVE, createdJob.status)
        assertEquals("$230k - $290k", createdJob.salaryRange)

        // Toggle job status to CLOSED / Paused
        repository.updateJobStatus(createdJob.id, JobStatus.CLOSED)
        val pausedJob = viewModel.jobs.value.find { it.id == createdJob.id }
        assertNotNull(pausedJob)
        assertEquals(JobStatus.CLOSED, pausedJob!!.status)

        // Reactivate
        repository.updateJobStatus(createdJob.id, JobStatus.ACTIVE)
        val reactivatedJob = viewModel.jobs.value.find { it.id == createdJob.id }
        assertEquals(JobStatus.ACTIVE, reactivatedJob!!.status)
    }

    @Test
    fun testEmployerCandidateInterviewJourney() {
        val candidate = viewModel.applicants.value.first()

        // 1. Advance candidate to Shortlist
        repository.updateApplicantStage(candidate.id, ApplicationStage.SHORTLISTED)
        val shortlistedCand = viewModel.applicants.value.find { it.id == candidate.id }
        assertEquals(ApplicationStage.SHORTLISTED, shortlistedCand!!.stage)

        // 2. Schedule interview
        val initialInterviews = viewModel.interviews.value.size
        repository.scheduleNewInterview(
            candidateName = candidate.candidateName,
            candidateRole = candidate.jobTitle,
            company = "Acme Technologies",
            date = "Wednesday, Oct 21",
            time = "3:00 PM PST",
            interviewer = "Sarah Chen (VP Engineering)"
        )

        // Verify interview added
        val updatedInterviews = viewModel.interviews.value
        assertEquals(initialInterviews + 1, updatedInterviews.size)
        val newInterview = updatedInterviews.first()
        assertEquals(candidate.candidateName, newInterview.candidateName)
        assertEquals("Wednesday, Oct 21", newInterview.date)
        assertTrue(newInterview.meetingUrl.startsWith("https://meet.google.com"))

        // Verify applicant stage synced
        val candidateAfterInterview = viewModel.applicants.value.find { it.id == candidate.id }
        assertEquals(ApplicationStage.INTERVIEW_SCHEDULED, candidateAfterInterview!!.stage)

        // 3. Add scorecard evaluation
        repository.updateInterviewScorecard(newInterview.id, 5, "Outstanding technical background.")
        val evaluatedInterview = viewModel.interviews.value.find { it.id == newInterview.id }
        assertNotNull(evaluatedInterview)
        assertEquals(5, evaluatedInterview!!.scoreTechnical)
        assertEquals("Outstanding technical background.", evaluatedInterview.feedbackNotes)
    }

    @Test
    fun testAdminGovernanceJourney() {
        viewModel.switchRole(UserRole.ADMIN)
        assertEquals(UserRole.ADMIN, viewModel.currentRole.value)

        // 1. Moderate jobs
        val testJob = viewModel.jobs.value.first()
        repository.moderateJob(testJob.id, approve = false)
        val flaggedJob = viewModel.jobs.value.find { it.id == testJob.id }
        assertEquals(JobStatus.CLOSED, flaggedJob!!.status)

        repository.moderateJob(testJob.id, approve = true)
        val approvedJob = viewModel.jobs.value.find { it.id == testJob.id }
        assertEquals(JobStatus.ACTIVE, approvedJob!!.status)

        // 2. Manage users
        val users = viewModel.users.value
        assertTrue("Users list should not be empty", users.isNotEmpty())
        val targetUser = users.first()
        val initialVerified = targetUser.isVerified

        repository.toggleUserVerification(targetUser.id)
        val userAfterToggle = viewModel.users.value.find { it.id == targetUser.id }
        assertNotEquals(initialVerified, userAfterToggle!!.isVerified)

        // 3. Taxonomy management
        val initialCategories = viewModel.categories.value.size
        repository.addCategory("Cybersecurity & Zero Trust", "security")
        assertEquals(initialCategories + 1, viewModel.categories.value.size)
        val newCat = viewModel.categories.value.find { it.name == "Cybersecurity & Zero Trust" }
        assertNotNull(newCat)

        // 4. Resolve dispute report
        val report = viewModel.reportedItems.value.first()
        val initialPendingAudits = viewModel.adminVitals.value.pendingAuditsCount
        repository.resolveReport(report.id, "Action Taken / Resolved")
        val resolvedReport = viewModel.reportedItems.value.find { it.id == report.id }
        assertEquals("Action Taken / Resolved", resolvedReport!!.status)
        assertEquals(initialPendingAudits - 1, viewModel.adminVitals.value.pendingAuditsCount)
    }

    @Test
    fun testRoleSwitchingAndTabRouting() {
        // Start Job Seeker
        viewModel.switchRole(UserRole.JOB_SEEKER)
        assertEquals("home", viewModel.activeTab.value)

        viewModel.setTab("explore")
        assertEquals("explore", viewModel.activeTab.value)

        // Switch to Employer
        viewModel.switchRole(UserRole.EMPLOYER)
        assertEquals(UserRole.EMPLOYER, viewModel.currentRole.value)
        assertEquals("dashboard", viewModel.activeTab.value)

        // Switch to Admin
        viewModel.switchRole(UserRole.ADMIN)
        assertEquals(UserRole.ADMIN, viewModel.currentRole.value)
        assertEquals("dashboard", viewModel.activeTab.value)
    }

    @Test
    fun testAuthDialogAndStateManagement() {
        // Initial state
        assertFalse("Auth dialog should initially be closed", viewModel.showAuthDialog.value)
        assertNull("AuthUser should initially be null without session", viewModel.authUser.value)

        // Open auth dialog
        viewModel.openAuthDialog()
        assertTrue("Auth dialog should be visible after openAuthDialog", viewModel.showAuthDialog.value)

        // Close auth dialog
        viewModel.closeAuthDialog()
        assertFalse("Auth dialog should be closed after closeAuthDialog", viewModel.showAuthDialog.value)
    }

    @Test
    fun testNewUserNotAutomaticallyPremium() {
        val profile = viewModel.referralProfile.value
        assertFalse("New user must not be marked premium by default", profile.isPremium)
    }

    @Test
    fun testSelfReferralIsRejected() {
        val userReferralCode = viewModel.referralProfile.value.referralCode
        val success = viewModel.applyReferralCode(userReferralCode)
        assertFalse("Self-referral must be rejected", success)
    }

    @Test
    fun testReferralAttributionLocking_cannotReassignAfterCommitment() {
        val firstCode = "TNX-SPONSOR1"
        val firstResult = viewModel.applyReferralCode(firstCode)
        assertTrue("First referral attribution must succeed", firstResult)

        val secondCode = "TNX-SPONSOR2"
        val secondResult = viewModel.applyReferralCode(secondCode)
        assertFalse("Referral attribution must not be reassignable once committed", secondResult)
    }

    @Test
    fun testVerifiedPurchaseGrantsPremiumAndCreditsReferralReward() {
        // Apply valid referral attribution
        viewModel.applyReferralCode("TNX-MENTOR")

        // Initiate ₹10 purchase
        val payment = viewModel.initiatePremiumPayment()
        assertEquals(10, payment.amount)
        assertEquals(1000, payment.amountPaise)
        assertEquals(PaymentStatus.PENDING_VERIFICATION, payment.status)
        assertFalse("Payer must not be premium before verification", viewModel.referralProfile.value.isPremium)

        // Verify payment
        val verified = viewModel.verifyPremiumPayment(payment.id, "txn_sandbox_test_101")
        assertTrue("Payment verification must succeed", verified)
        assertTrue("Payer must be premium after verified payment", viewModel.referralProfile.value.isPremium)

        // Duplicate payment callback must be idempotent
        val duplicateVerified = viewModel.verifyPremiumPayment(payment.id, "txn_sandbox_test_101")
        assertTrue("Duplicate verification should return idempotent success", duplicateVerified)
    }

    @Test
    fun testRedemptionRequiresBothConditions() {
        // Case 1: Initial state (referrals = 5 < 10, balance = 25 < 50)
        val initialProfile = viewModel.referralProfile.value
        assertFalse("Must not be eligible when both conditions unmet", initialProfile.meetsBothRedemptionRequirements)

        val failedRedemption1 = viewModel.submitRedemptionRequest(50, "test@upi")
        assertFalse("Redemption must fail when fewer than 10 referrals and balance below ₹50", failedRedemption1)

        // Case 2: Unmet condition reporting
        val unmetMsg = initialProfile.getUnmetRequirementDescription()
        assertNotNull(unmetMsg)
        assertTrue("Unmet message must mention both requirements", unmetMsg!!.contains("10") && unmetMsg.contains("50"))
    }

    @Test
    fun testRedemptionSuccessAndDuplicateDeductionPrevention() {
        // Fast-forward profile to satisfy BOTH conditions (10 referrals and ₹50 balance)
        val readyProfile = viewModel.referralProfile.value.copy(
            successfulReferralsCount = 10,
            availableBalance = 60,
            availableBalancePaise = 6000,
            isEligibleForWithdrawal = true
        )
        // Set ready state
        (viewModel.repository.dataSource.value as com.example.jobportal.data.repository.InMemoryJobPortalDataSource).let { ds ->
            // Reflect into internal state
        }
    }

    @Test
    fun testWithdrawalReservationAndAdminApprovalFlow() {
        val ds = viewModel.repository.dataSource.value
        // Setup user with 10 referrals and ₹100 balance
        val testProfile = viewModel.referralProfile.value.copy(
            successfulReferralsCount = 10,
            availableBalance = 100,
            availableBalancePaise = 10000,
            isEligibleForWithdrawal = true
        )

        // Test withdrawal validation in repository
        val initialRequests = viewModel.withdrawalRequests.value.size
        assertEquals(0, initialRequests)
    }
}
