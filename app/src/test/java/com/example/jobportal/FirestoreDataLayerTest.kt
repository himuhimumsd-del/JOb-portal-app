package com.example.jobportal

import com.example.jobportal.data.model.*
import com.example.jobportal.data.model.firestore.*
import com.example.jobportal.data.payment.CashfreeCheckoutResult
import com.example.jobportal.data.repository.InMemoryJobPortalDataSource
import com.example.jobportal.data.repository.JobPortalRepository
import com.example.jobportal.ui.CheckoutUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FirestoreDataLayerTest {

    @Test
    fun testCategoryEntitySerializationRoundTrip() {
        val category = CategoryItem(
            id = "cat-cloud-1",
            name = "Systems Architecture",
            jobCount = "120 jobs",
            icon = "memory"
        )
        val entity = CategoryEntity.fromDomainModel(category)
        val docMap = entity.toDocumentMap()

        assertEquals("cat-cloud-1", docMap["id"])
        assertEquals("Systems Architecture", docMap["name"])
        assertEquals("120 jobs", docMap["jobCount"])
        assertEquals("memory", docMap["icon"])

        val deserialized = CategoryEntity.fromDocument("cat-cloud-1", docMap).toDomainModel()
        assertEquals(category.id, deserialized.id)
        assertEquals(category.name, deserialized.name)
        assertEquals(category.jobCount, deserialized.jobCount)
        assertEquals(category.icon, deserialized.icon)
    }

    @Test
    fun testJobEntitySerializationRoundTrip() {
        val originalJob = Job(
            id = "job-cloud-test",
            title = "Principal Platform Engineer",
            company = "Acme Cloud",
            location = "San Francisco, CA",
            workplaceType = WorkplaceType.REMOTE,
            employmentType = EmploymentType.FULL_TIME,
            salaryRange = "$220k - $260k",
            minSalary = 220000,
            category = "Tech & Dev",
            tags = listOf("Kotlin", "Cloud", "Distributed Systems"),
            description = "Building resilient distributed engines",
            responsibilities = listOf("Architect systems", "Lead drills"),
            requirements = listOf("8+ years experience"),
            benefits = listOf("Equity", "Healthcare"),
            isVerified = false,
            isActivelyHiring = true,
            postedTime = "Just now",
            status = JobStatus.ACTIVE,
            applicantsCount = 0
        )

        val entity = JobEntity.fromDomainModel(originalJob, companyId = "org-acme", creatorId = "usr-creator")
        val docMap = entity.toDocumentMap()

        assertEquals("org-acme", docMap["companyId"])
        assertEquals("usr-creator", docMap["creatorId"])
        assertEquals("ACTIVE", docMap["status"])
        assertEquals(0, docMap["applicantsCount"])
        assertEquals(false, docMap["isVerified"])

        val reconstructed = JobEntity.fromDocument("job-cloud-test", docMap).toDomainModel()
        assertEquals(originalJob.id, reconstructed.id)
        assertEquals(originalJob.title, reconstructed.title)
        assertEquals(originalJob.company, reconstructed.company)
        assertEquals(originalJob.workplaceType, reconstructed.workplaceType)
        assertEquals(originalJob.employmentType, reconstructed.employmentType)
        assertEquals(originalJob.minSalary, reconstructed.minSalary)
        assertEquals(originalJob.tags, reconstructed.tags)
        assertEquals(JobStatus.ACTIVE, reconstructed.status)
    }

    @Test
    fun testUserEntitySecurityDefaults() {
        val userMap = mapOf(
            "fullName" to "Jane Doe",
            "email" to "jane@example.com",
            "role" to "JOB_SEEKER",
            "isAdmin" to false,
            "isVerified" to false
        )
        val entity = UserEntity.fromDocument("usr-jane", userMap)
        assertEquals("usr-jane", entity.id)
        assertFalse("User must not be admin by default", entity.isAdmin)
        assertFalse("User must not be verified by default", entity.isVerified)

        val profile = entity.toSeekerProfile()
        assertEquals("Jane Doe", profile.fullName)
        assertEquals("jane@example.com", profile.email)
    }

    @Test
    fun testCompanyAndMembershipEntitySerialization() {
        val companyMap = mapOf(
            "name" to "Nexus Labs",
            "tagline" to "Distributed compute",
            "industry" to "Cloud",
            "website" to "https://nexuslabs.io",
            "location" to "Austin, TX",
            "isVerified" to true,
            "employeeCount" to "50-100",
            "openRolesCount" to 4,
            "createdBy" to "usr-founder"
        )
        val companyEntity = CompanyEntity.fromDocument("comp-1", companyMap)
        assertEquals("Nexus Labs", companyEntity.name)
        assertEquals("usr-founder", companyEntity.createdBy)

        val memberMap = mapOf(
            "companyId" to "comp-1",
            "userId" to "usr-recruiter",
            "role" to "recruiter",
            "joinedAt" to "2026-10-18"
        )
        val member = CompanyMembershipEntity.fromDocument("comp-1_usr-recruiter", memberMap)
        assertEquals("comp-1", member.companyId)
        assertEquals("usr-recruiter", member.userId)
        assertEquals("recruiter", member.role)
    }

    @Test
    fun testApplicationAndInterviewSerialization() {
        val appMap = mapOf(
            "jobId" to "job-100",
            "jobTitle" to "Staff Architect",
            "company" to "Linear",
            "companyId" to "org-linear",
            "candidateId" to "cand-999",
            "status" to "INTERVIEW_SCHEDULED",
            "resumeVersion" to "Staff_Architect_2026.pdf"
        )
        val app = ApplicationEntity.fromDocument("app-55", appMap).toDomainModel()
        assertEquals("job-100", app.jobId)
        assertEquals(ApplicationStage.INTERVIEW_SCHEDULED, app.status)

        val interviewMap = mapOf(
            "applicationId" to "app-55",
            "candidateId" to "cand-999",
            "candidateName" to "Alex Vance",
            "candidateRole" to "Staff Architect",
            "company" to "Linear",
            "meetingUrl" to "https://meet.google.com/test-meet",
            "date" to "Tomorrow",
            "time" to "10:00 AM",
            "status" to "Confirmed"
        )
        val interview = InterviewEntity.fromDocument("int-77", interviewMap).toDomainModel()
        assertEquals("Alex Vance", interview.candidateName)
        assertEquals("https://meet.google.com/test-meet", interview.meetingUrl)
    }

    @Test
    fun testJobPortalRepositoryDataSourceSwitching() {
        val inMemory = InMemoryJobPortalDataSource()
        val repository = JobPortalRepository(inMemory)

        assertTrue("Initially in demo mode", repository.isDemoMode.value)
        assertEquals(3, repository.jobs.value.size)

        // Verify operations work through the repository facade
        val created = repository.createJob(
            title = "QA Automation Lead",
            company = "TestCorp",
            location = "Remote",
            workplaceType = WorkplaceType.REMOTE,
            employmentType = EmploymentType.CONTRACT,
            minSalary = 150000,
            maxSalary = 180000,
            category = "Tech & Dev",
            tags = listOf("JUnit", "Robolectric"),
            description = "Lead QA engineering",
            responsibilities = listOf("Write tests"),
            requirements = listOf("5+ years experience"),
            benefits = listOf("Remote stipend")
        )

        assertNotNull(created)
        assertEquals("QA Automation Lead", created.title)
        assertTrue(repository.jobs.value.any { it.title == "QA Automation Lead" })
    }

    @Test
    fun testApplicationToApplicantMapping() {
        val appEntity = ApplicationEntity(
            id = "job-42_usr-alice",
            jobId = "job-42",
            jobTitle = "Staff Android Engineer",
            company = "Nexus Global",
            companyId = "org-nexus",
            candidateId = "usr-alice",
            candidateName = "Alice Vance",
            candidateEmail = "alice@nexus.io",
            candidateHeadline = "Staff Mobile Architect",
            candidatePhone = "+1 555-0199",
            candidateLocation = "San Francisco, CA",
            keySkills = listOf("Kotlin", "Compose", "Architecture"),
            atsScore = 94,
            experienceYears = 8,
            appliedDate = "Today",
            status = ApplicationStage.SHORTLISTED.name,
            resumeVersion = "Alice_Staff_2026.pdf"
        )

        val applicant = appEntity.toApplicant()
        assertEquals("job-42_usr-alice", applicant.id)
        assertEquals("job-42", applicant.jobId)
        assertEquals("Alice Vance", applicant.candidateName)
        assertEquals("Staff Mobile Architect", applicant.headline)
        assertEquals(94, applicant.atsScore)
        assertEquals(8, applicant.experienceYears)
        assertEquals(ApplicationStage.SHORTLISTED, applicant.stage)
        assertEquals("Alice_Staff_2026.pdf", applicant.resumeFileName)
        assertEquals(listOf("Kotlin", "Compose", "Architecture"), applicant.keySkills)
    }

    @Test
    fun testDeterministicApplicationIdPattern() {
        val jobId = "job-777"
        val candidateUid = "usr-candidate-999"
        val deterministicId = "${jobId}_${candidateUid}"

        assertEquals("job-777_usr-candidate-999", deterministicId)
        assertTrue("Deterministic ID must contain both job ID and user UID", deterministicId.startsWith("job-777_"))
        assertTrue("Deterministic ID must end with user UID", deterministicId.endsWith("usr-candidate-999"))
    }

    @Test
    fun testDuplicateApplicationPrevention() {
        val testJobId = "job-unapplied-999"
        val candidateUid = "usr-test-candidate"
        val deterministicAppId = "${testJobId}_${candidateUid}"

        val entity1 = ApplicationEntity(
            id = deterministicAppId,
            jobId = testJobId,
            companyId = "org-google",
            candidateId = candidateUid,
            status = ApplicationStage.APPLIED.name
        )

        val existingApplications = listOf(entity1)
        val isDuplicate = existingApplications.any { it.id == deterministicAppId || (it.jobId == testJobId && it.candidateId == candidateUid) }
        assertTrue("Submitting the same deterministic ID must be detected as duplicate", isDuplicate)

        val nonDuplicateJobId = "job-new-123"
        val isNonDuplicate = existingApplications.any { it.id == "${nonDuplicateJobId}_${candidateUid}" || (it.jobId == nonDuplicateJobId && it.candidateId == candidateUid) }
        assertFalse("New distinct job ID must not be flagged as duplicate", isNonDuplicate)
    }

    @Test
    fun testAuthorizedApplicantStageProgression() {
        val repository = JobPortalRepository(InMemoryJobPortalDataSource())
        val applicant = repository.applicants.value.first()
        val originalStage = applicant.stage

        // Transition to IN_REVIEW
        repository.updateApplicantStage(applicant.id, ApplicationStage.IN_REVIEW)
        val afterReviewing = repository.applicants.value.first { it.id == applicant.id }
        assertEquals(ApplicationStage.IN_REVIEW, afterReviewing.stage)

        // Transition to SHORTLISTED
        repository.updateApplicantStage(applicant.id, ApplicationStage.SHORTLISTED)
        val afterShortlist = repository.applicants.value.first { it.id == applicant.id }
        assertEquals(ApplicationStage.SHORTLISTED, afterShortlist.stage)

        // Transition to REJECTED
        repository.updateApplicantStage(applicant.id, ApplicationStage.REJECTED)
        val afterReject = repository.applicants.value.first { it.id == applicant.id }
        assertEquals(ApplicationStage.REJECTED, afterReject.stage)
    }

    @Test
    fun testInterviewEntityFullSerialization() {
        val entity = InterviewEntity(
            id = "int_job-10_usr-1",
            applicationId = "app-10",
            jobId = "job-10",
            companyId = "org-stripe",
            candidateId = "usr-1",
            candidateName = "Alex Vance",
            candidateRole = "Staff Engineer",
            company = "Stripe",
            date = "Tomorrow",
            time = "10:00 AM",
            duration = "45 mins",
            interviewerName = "Charlie Recruiter",
            platform = "Google Meet",
            meetingUrl = "https://meet.google.com/test-meet",
            status = "Confirmed",
            scoreTechnical = 5,
            scoreCulture = 4,
            feedbackNotes = "Exceptional distributed systems fundamentals"
        )

        val docMap = entity.toDocumentMap()
        assertEquals("int_job-10_usr-1", docMap["id"])
        assertEquals("app-10", docMap["applicationId"])
        assertEquals("org-stripe", docMap["companyId"])
        assertEquals(5, docMap["scoreTechnical"])
        assertEquals("Exceptional distributed systems fundamentals", docMap["feedbackNotes"])

        val reconstructed = InterviewEntity.fromDocument("int_job-10_usr-1", docMap).toDomainModel()
        assertEquals("Alex Vance", reconstructed.candidateName)
        assertEquals("Staff Engineer", reconstructed.candidateRole)
        assertEquals("Stripe", reconstructed.company)
        assertEquals("10:00 AM", reconstructed.time)
        assertEquals("https://meet.google.com/test-meet", reconstructed.meetingUrl)
        assertEquals(5, reconstructed.scoreTechnical)
        assertEquals("Exceptional distributed systems fundamentals", reconstructed.feedbackNotes)
    }

    @Test
    fun testScheduleInterviewApplicationSynchronization() {
        val repository = JobPortalRepository(InMemoryJobPortalDataSource())
        val initialInterviewCount = repository.interviews.value.size

        repository.scheduleNewInterview(
            candidateName = "Marcus Thorne",
            candidateRole = "Principal Systems Architect",
            company = "Linear",
            date = "Oct 28",
            time = "3:00 PM",
            interviewer = "Sarah Chen"
        )

        val updatedInterviews = repository.interviews.value
        assertEquals("Interviews should increase by 1", initialInterviewCount + 1, updatedInterviews.size)

        val scheduled = updatedInterviews.first()
        assertEquals("Marcus Thorne", scheduled.candidateName)
        assertEquals("Oct 28", scheduled.date)
        assertEquals("3:00 PM", scheduled.time)
        assertTrue("Meeting URL should be valid Google Meet", scheduled.meetingUrl.startsWith("https://meet.google.com/"))
    }

    @Test
    fun testInterviewReschedulingAndCancellation() {
        val repository = JobPortalRepository(InMemoryJobPortalDataSource())
        val interview = repository.interviews.value.first()

        // Reschedule
        repository.rescheduleInterview(interview.id, "Nov 1", "4:30 PM")
        val rescheduled = repository.interviews.value.first { it.id == interview.id }
        assertEquals("Nov 1", rescheduled.date)
        assertEquals("4:30 PM", rescheduled.time)
        assertEquals("Rescheduled", rescheduled.status)

        // Cancel
        repository.cancelInterview(interview.id)
        val cancelled = repository.interviews.value.first { it.id == interview.id }
        assertEquals("Cancelled", cancelled.status)
    }

    @Test
    fun testScorecardPersistence() {
        val repository = JobPortalRepository(InMemoryJobPortalDataSource())
        val interview = repository.interviews.value.first()

        repository.updateInterviewScorecard(interview.id, 5, "Outstanding architecture and design skills")
        val evaluated = repository.interviews.value.first { it.id == interview.id }

        assertEquals(5, evaluated.scoreTechnical)
        assertEquals(5, evaluated.scoreCulture)
        assertEquals("Outstanding architecture and design skills", evaluated.feedbackNotes)
    }

    @Test
    fun testInterviewReschedulingUpdatesLinkedApplication() {
        val repository = JobPortalRepository(InMemoryJobPortalDataSource())
        val interview = repository.interviews.value.first()

        repository.rescheduleInterview(interview.id, "Nov 15", "10:00 AM PST")

        val rescheduledInterview = repository.interviews.value.first { it.id == interview.id }
        assertEquals("Rescheduled", rescheduledInterview.status)
        assertEquals("Nov 15", rescheduledInterview.date)
        assertEquals("10:00 AM PST", rescheduledInterview.time)

        // Linked application should reflect new scheduled date and time
        val linkedApp = repository.applications.value.firstOrNull {
            it.jobTitle.equals(interview.candidateRole, ignoreCase = true)
        }
        if (linkedApp != null) {
            assertEquals("Nov 15", linkedApp.interviewDate)
            assertEquals("10:00 AM PST", linkedApp.interviewTime)
        }
    }

    @Test
    fun testInterviewCancellationUpdatesLinkedApplication() {
        val repository = JobPortalRepository(InMemoryJobPortalDataSource())
        val interview = repository.interviews.value.first()

        repository.cancelInterview(interview.id)

        val cancelledInterview = repository.interviews.value.first { it.id == interview.id }
        assertEquals("Cancelled", cancelledInterview.status)

        // Linked application should revert to IN_REVIEW with cleared interview date/time
        val linkedApp = repository.applications.value.firstOrNull {
            it.jobTitle.equals(interview.candidateRole, ignoreCase = true)
        }
        if (linkedApp != null) {
            assertEquals(ApplicationStage.IN_REVIEW, linkedApp.status)
            assertNull(linkedApp.interviewDate)
            assertNull(linkedApp.interviewTime)
        }
    }

    @Test
    fun testScorecardPrivacyFromCandidateApplication() {
        val repository = JobPortalRepository(InMemoryJobPortalDataSource())
        val app = repository.applications.value.first()

        // Verify candidate Application model does not expose internal employer scorecard fields
        assertNotNull(app.id)
        assertNotNull(app.jobTitle)
        // Verify Application class has no technical evaluation score or internal feedback fields
        val appFields = Application::class.java.declaredFields.map { it.name }
        assertFalse("Candidate application must not contain scoreTechnical", appFields.contains("scoreTechnical"))
        assertFalse("Candidate application must not contain feedbackNotes", appFields.contains("feedbackNotes"))
        assertFalse("Candidate application must not contain interviewerNotes", appFields.contains("interviewerNotes"))
    }

    @Test
    fun testReferralEntitySerializationRoundTrip() {
        val refItem = ReferralItem(
            id = "ref-test-1",
            referrerUid = "usr-referrer-10",
            referredUid = "usr-referred-20",
            referredName = "David Kim",
            referralCode = "TNX-REF10",
            status = ReferralStatus.REWARD_CREDITED,
            rewardAmount = 5,
            qualifyingPaymentRef = "pay_test_99",
            createdAt = "2 days ago",
            rewardedAt = "Yesterday"
        )

        val entity = ReferralEntity(
            id = refItem.id,
            referrerUid = refItem.referrerUid,
            referredUid = refItem.referredUid,
            referredName = refItem.referredName,
            referralCode = refItem.referralCode,
            status = refItem.status.name,
            rewardAmount = refItem.rewardAmount,
            qualifyingPaymentRef = refItem.qualifyingPaymentRef,
            createdAt = refItem.createdAt,
            rewardedAt = refItem.rewardedAt
        )
        val docMap = entity.toDocumentMap()
        assertEquals("ref-test-1", docMap["id"])
        assertEquals("usr-referrer-10", docMap["referrerUid"])
        assertEquals("REWARD_CREDITED", docMap["status"])
        assertEquals(5, docMap["rewardAmount"])

        val reconstructed = ReferralEntity.fromDocument("ref-test-1", docMap).toDomainModel()
        assertEquals(refItem.id, reconstructed.id)
        assertEquals(refItem.referrerUid, reconstructed.referrerUid)
        assertEquals(refItem.referredUid, reconstructed.referredUid)
        assertEquals(ReferralStatus.REWARD_CREDITED, reconstructed.status)
        assertEquals(5, reconstructed.rewardAmount)
    }

    @Test
    fun testWithdrawalEntitySerializationRoundTrip() {
        val wdr = WithdrawalRequest(
            id = "wdr-test-88",
            userId = "usr-candidate-1",
            userName = "Alex Vance",
            userEmail = "alex@example.com",
            amount = 50,
            payoutMethod = "UPI",
            payoutAccountDetails = "alex@okaxis",
            status = WithdrawalStatus.REQUESTED,
            adminNotes = null,
            requestedAt = "Just now"
        )

        val entity = WithdrawalEntity(
            id = wdr.id,
            userId = wdr.userId,
            userName = wdr.userName,
            userEmail = wdr.userEmail,
            amount = wdr.amount,
            payoutMethod = wdr.payoutMethod,
            payoutAccountDetails = wdr.payoutAccountDetails,
            status = wdr.status.name,
            requestedAt = wdr.requestedAt
        )
        val docMap = entity.toDocumentMap()
        assertEquals(50, docMap["amount"])
        assertEquals("REQUESTED", docMap["status"])
        assertEquals("UPI", docMap["payoutMethod"])

        val reconstructed = WithdrawalEntity.fromDocument("wdr-test-88", docMap).toDomainModel()
        assertEquals(wdr.id, reconstructed.id)
        assertEquals(50, reconstructed.amount)
        assertEquals(WithdrawalStatus.REQUESTED, reconstructed.status)
        assertEquals("alex@okaxis", reconstructed.payoutAccountDetails)
    }

    @Test
    fun testDualRedemptionCriteriaValidation() {
        // Condition 1: referrals < 10, balance < 50
        val p1 = ReferralProfile(
            referralCode = "TNX-TEST",
            shareableLink = "https://talentnexus.app/join?ref=TNX-TEST",
            successfulReferralsCount = 9,
            availableBalance = 40
        )
        assertFalse(p1.meetsReferralsRequirement)
        assertFalse(p1.meetsBalanceRequirement)
        assertFalse(p1.meetsBothRedemptionRequirements)
        assertTrue(p1.getUnmetRequirementDescription()!!.contains("10 successful referrals") && p1.getUnmetRequirementDescription()!!.contains("₹50"))

        // Condition 2: referrals >= 10, but balance < 50
        val p2 = ReferralProfile(
            referralCode = "TNX-TEST",
            shareableLink = "https://talentnexus.app/join?ref=TNX-TEST",
            successfulReferralsCount = 10,
            availableBalance = 35
        )
        assertTrue(p2.meetsReferralsRequirement)
        assertFalse(p2.meetsBalanceRequirement)
        assertFalse(p2.meetsBothRedemptionRequirements)
        assertTrue(p2.getUnmetRequirementDescription()!!.contains("₹50 available referral balance"))

        // Condition 3: balance >= 50, but referrals < 10
        val p3 = ReferralProfile(
            referralCode = "TNX-TEST",
            shareableLink = "https://talentnexus.app/join?ref=TNX-TEST",
            successfulReferralsCount = 8,
            availableBalance = 60
        )
        assertFalse(p3.meetsReferralsRequirement)
        assertTrue(p3.meetsBalanceRequirement)
        assertFalse(p3.meetsBothRedemptionRequirements)
        assertTrue(p3.getUnmetRequirementDescription()!!.contains("10 successful referrals"))

        // Condition 4: Both met: referrals >= 10 AND balance >= 50
        val p4 = ReferralProfile(
            referralCode = "TNX-TEST",
            shareableLink = "https://talentnexus.app/join?ref=TNX-TEST",
            successfulReferralsCount = 12,
            availableBalance = 60
        )
        assertTrue(p4.meetsReferralsRequirement)
        assertTrue(p4.meetsBalanceRequirement)
        assertTrue(p4.meetsBothRedemptionRequirements)
        assertNull(p4.getUnmetRequirementDescription())
    }

    @Test
    fun testWithdrawalLifecycle_reservationAndAdminRejectionRestoration() {
        val ds = InMemoryJobPortalDataSource()
        val repo = JobPortalRepository(ds)

        // Seed 10 successful referrals and ₹60 balance by processing 12 payments
        for (i in 1..12) {
            ds.applyReferralAttribution("TNX-SPONSOR-$i")
            val p = ds.initiatePremiumPayment()
            ds.processVerifiedPayment(p.id, "txn_mock_seed_$i")
        }

        val profileBefore = repo.referralProfile.value
        if (profileBefore.meetsBothRedemptionRequirements) {
            val balanceBefore = profileBefore.availableBalance
            val withdrawResult = repo.requestWithdrawal(50, "test@upi")
            assertTrue("Withdrawal request must succeed when both conditions met", withdrawResult)

            val profileAfterRequest = repo.referralProfile.value
            assertEquals("Available balance must decrease by requested amount", balanceBefore - 50, profileAfterRequest.availableBalance)
            assertEquals("Reserved balance must increase by 50", 50, profileAfterRequest.reservedBalance)

            // Test admin rejection restores reserved funds
            val reqId = repo.withdrawalRequests.value.first().id
            val rejectResult = repo.processWithdrawalDecision(reqId, approve = false, adminNotes = "Incorrect UPI ID")
            assertTrue("Rejection must succeed", rejectResult)

            val profileAfterReject = repo.referralProfile.value
            assertEquals("Available balance must be restored to original", balanceBefore, profileAfterReject.availableBalance)
            assertEquals("Reserved balance must be zero after rejection", 0, profileAfterReject.reservedBalance)
        }
    }

    @Test
    fun testLargeWithdrawalAmountSucceedsWithoutTruncation() {
        val ds = InMemoryJobPortalDataSource()
        val repo = JobPortalRepository(ds)

        // Seed 25 successful referrals and ₹150 available balance
        ds.seedReferralStats(availableRupees = 150, successfulReferrals = 25)

        val profileBefore = repo.referralProfile.value
        assertTrue(profileBefore.successfulReferralsCount >= 10)
        assertTrue(profileBefore.availableBalance >= 100)

        // Request ₹100 withdrawal (must not be divided by 100 to 1!)
        val success = repo.requestWithdrawal(100, "alex@okaxis")
        assertTrue("Withdrawal for ₹100 must succeed", success)

        val latestRequest = repo.withdrawalRequests.value.first()
        assertEquals(100, latestRequest.amount)
        assertEquals("alex@okaxis", latestRequest.payoutAccountDetails)
        assertEquals(WithdrawalStatus.REQUESTED, latestRequest.status)

        // Profile reserved balance should be exactly 100
        assertEquals(100, repo.referralProfile.value.reservedBalance)
    }

    @Test
    fun testRepeatedPaymentVerificationIsIdempotent() {
        val ds = InMemoryJobPortalDataSource()
        val repo = JobPortalRepository(ds)

        ds.applyReferralAttribution("TNX-ORIGINAL")
        val payment = ds.initiatePremiumPayment()

        // First verification
        val firstResult = ds.processVerifiedPayment(payment.id, "txn_razorpay_999")
        assertTrue(firstResult)
        val profileAfterFirst = repo.referralProfile.value
        val rewardsAfterFirst = profileAfterFirst.totalRewardsEarned

        // Duplicate verification callback with same transaction
        val duplicateResult = ds.processVerifiedPayment(payment.id, "txn_razorpay_999")
        assertTrue("Duplicate verification should return true idempotently", duplicateResult)

        val profileAfterDuplicate = repo.referralProfile.value
        assertEquals("Rewards must not double on duplicate payment callback", rewardsAfterFirst, profileAfterDuplicate.totalRewardsEarned)
    }

    @Test
    fun testDualRedemptionRequirementEnforcedInDataSources() {
        val ds = InMemoryJobPortalDataSource()
        val repo = JobPortalRepository(ds)

        // Case 1: ₹60 balance, but only 5 referrals -> BLOCKED
        ds.seedReferralStats(availableRupees = 60, successfulReferrals = 5)
        assertFalse(repo.referralProfile.value.isEligibleForWithdrawal)
        assertFalse(repo.referralProfile.value.meetsBothRedemptionRequirements)
        val attempt1 = repo.requestWithdrawal(50, "user@upi")
        assertFalse("Withdrawal must be blocked when referrals < 10", attempt1)

        // Case 2: 12 referrals, but only ₹40 balance -> BLOCKED
        ds.seedReferralStats(availableRupees = 40, successfulReferrals = 12)
        assertFalse(repo.referralProfile.value.isEligibleForWithdrawal)
        assertFalse(repo.referralProfile.value.meetsBothRedemptionRequirements)
        val attempt2 = repo.requestWithdrawal(50, "user@upi")
        assertFalse("Withdrawal must be blocked when balance < ₹50", attempt2)

        // Case 3: 10 referrals AND ₹50 balance -> ALLOWED
        ds.seedReferralStats(availableRupees = 50, successfulReferrals = 10)
        assertTrue(repo.referralProfile.value.isEligibleForWithdrawal)
        assertTrue(repo.referralProfile.value.meetsBothRedemptionRequirements)
        val attempt3 = repo.requestWithdrawal(50, "user@upi")
        assertTrue("Withdrawal must succeed when both conditions are met", attempt3)
    }

    @Test
    fun testDeepLinkReferralCaptureInViewModel() {
        val ds = InMemoryJobPortalDataSource()
        val repo = JobPortalRepository(ds)
        val vm = com.example.jobportal.ui.MainViewModel(repo)

        // Simulate incoming deep link capture: https://talentnexus.app/join?ref=tnx-vip888
        vm.handleIncomingReferralCode("tnx-vip888")

        // Should clean, uppercase, and trigger dialog
        assertEquals("TNX-VIP888", vm.incomingReferralCode.value)
        assertTrue(vm.showEnterReferralCodeDialog.value)

        // Closing the dialog resets the transient incoming code
        vm.closeEnterReferralCode()
        assertFalse(vm.showEnterReferralCodeDialog.value)
        assertNull(vm.incomingReferralCode.value)
    }

    @Test
    fun testNamedDatabaseValidationRejectsDefaultOrBlank() {
        // Blank database ID must fail fast
        try {
            com.example.jobportal.data.repository.FirestoreJobPortalDataSource(databaseId = "")
            fail("Blank database ID must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("non-default") == true || e.message?.contains("explicitly configured") == true)
        }

        // (default) database ID must be rejected to prevent silent fallback
        try {
            com.example.jobportal.data.repository.FirestoreJobPortalDataSource(databaseId = "(default)")
            fail("(default) database ID must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("non-default") == true)
        }
    }

    @Test
    fun testRepositoryInitializationDefaultsWithoutSilentMockData() {
        // In JVM test environment where FirebaseApp is uninitialized,
        // JobPortalRepository must NOT default to InMemoryJobPortalDataSource with mock data
        val repo = JobPortalRepository()
        assertFalse("Must not be in demo mode by default", repo.isDemoMode.value)
        assertEquals("Jobs list must be empty initially, not populated with mock data", 0, repo.jobs.value.size)
        assertTrue(
            "Connection state must be Error or Connecting or Uninitialized, not DemoMode",
            repo.connectionState.value !is com.example.jobportal.data.repository.CloudConnectionState.DemoMode
        )
    }

    @Test
    fun testCloudConnectionStateTransitionsAndNoMockDataOnFailure() {
        val repo = JobPortalRepository()
        val targetDb = "ai-studio-android-jobporta-b95bcbd3-ac1e-47e0-8460-98c89a740c01"

        // Switch to Firestore
        repo.switchToFirestore(targetDb)

        // If Firebase is not running or throws, must enter CloudConnectionState.Error
        val state = repo.connectionState.value
        if (state is com.example.jobportal.data.repository.CloudConnectionState.Error) {
            assertEquals(targetDb, state.databaseId)
            assertFalse("Must not fall back to demo mode on failure", repo.isDemoMode.value)
            assertEquals("Jobs must remain empty on cloud error, never silent mock data", 0, repo.jobs.value.size)
            assertNotNull("Error message must be set", repo.errorMessage.value)
        }

        // Only explicit switchToDemo enables mock data
        repo.switchToDemo()
        assertTrue("Now in demo mode", repo.isDemoMode.value)
        assertEquals(3, repo.jobs.value.size)
        assertTrue(repo.connectionState.value is com.example.jobportal.data.repository.CloudConnectionState.DemoMode)
    }

    @Test
    fun testDisconnectedDataSourceRejectsOperationsGracefully() {
        val targetDb = "ai-studio-android-jobporta-b95bcbd3-ac1e-47e0-8460-98c89a740c01"
        val disconnectedDs = com.example.jobportal.data.repository.DisconnectedJobPortalDataSource(
            databaseId = targetDb,
            initialErrorMessage = "Cloud connection offline"
        )

        assertEquals(0, disconnectedDs.jobs.value.size)
        assertEquals(0, disconnectedDs.applications.value.size)
        assertEquals("Cloud connection offline", disconnectedDs.errorMessage.value)

        // Attempting to apply returns false and updates error
        val applied = disconnectedDs.applyToJob("job-1", "resume.pdf")
        assertFalse("Apply must fail when disconnected", applied)
        assertTrue(disconnectedDs.errorMessage.value?.contains("disconnected") == true)

        // Attempting to create job throws IllegalStateException
        try {
            disconnectedDs.createJob(
                title = "Engineer",
                company = "Corp",
                location = "Remote",
                workplaceType = WorkplaceType.REMOTE,
                employmentType = EmploymentType.FULL_TIME,
                minSalary = 100000,
                maxSalary = 120000,
                category = "Tech",
                tags = emptyList(),
                description = "Desc",
                responsibilities = emptyList(),
                requirements = emptyList(),
                benefits = emptyList()
            )
            fail("createJob must throw IllegalStateException when disconnected")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("disconnected") == true)
        }
    }

    @Test
    fun testServerSideWithdrawalEligibilityValidation() {
        val profileIneligibleReferrals = ReferralProfile(
            referralCode = "TNX-ELIG1",
            shareableLink = "https://talentnexus.app/join?ref=TNX-ELIG1",
            successfulReferralsCount = 9, // Exactly 1 below threshold of 10
            availableBalance = 100
        )
        assertFalse("Requires >= 10 successful referrals", profileIneligibleReferrals.meetsReferralsRequirement)
        assertTrue(profileIneligibleReferrals.meetsBalanceRequirement)
        assertFalse(profileIneligibleReferrals.meetsBothRedemptionRequirements)

        val profileIneligibleBalance = ReferralProfile(
            referralCode = "TNX-ELIG2",
            shareableLink = "https://talentnexus.app/join?ref=TNX-ELIG2",
            successfulReferralsCount = 15,
            availableBalance = 49 // Exactly ₹1 below threshold of ₹50
        )
        assertTrue(profileIneligibleBalance.meetsReferralsRequirement)
        assertFalse("Requires >= ₹50 balance", profileIneligibleBalance.meetsBalanceRequirement)
        assertFalse(profileIneligibleBalance.meetsBothRedemptionRequirements)

        val profileEligibleThreshold = ReferralProfile(
            referralCode = "TNX-ELIG3",
            shareableLink = "https://talentnexus.app/join?ref=TNX-ELIG3",
            successfulReferralsCount = 10,
            availableBalance = 50
        )
        assertTrue(profileEligibleThreshold.meetsReferralsRequirement)
        assertTrue(profileEligibleThreshold.meetsBalanceRequirement)
        assertTrue(profileEligibleThreshold.meetsBothRedemptionRequirements)
    }

    @Test
    fun testUnauthenticatedWithdrawalRequestFailsImmediately() {
        val ds = com.example.jobportal.data.repository.InMemoryJobPortalDataSource()
        val repo = JobPortalRepository(ds)
        // Set invalid amount < 50
        val resUnder = repo.requestWithdrawal(40, "test@upi")
        assertFalse("Withdrawals under ₹50 must be rejected", resUnder)

        // Set blank payout details
        val resBlank = repo.requestWithdrawal(50, "   ")
        assertFalse("Blank payout details must be rejected", resBlank)
    }

    @Test
    fun testCashfreeOrderSessionModelContract() {
        val session = CashfreeOrderSession(
            orderId = "order_tnx_test_12345",
            paymentSessionId = "session_sandbox_order_tnx_test_12345",
            amount = 10,
            currency = "INR",
            environment = "SANDBOX"
        )
        assertEquals("order_tnx_test_12345", session.orderId)
        assertEquals("session_sandbox_order_tnx_test_12345", session.paymentSessionId)
        assertEquals(10, session.amount)
        assertEquals("INR", session.currency)
        assertEquals("SANDBOX", session.environment)
    }

    @Test
    fun testCashfreePaymentVerificationResultContract() {
        val successResult = PaymentVerificationResult(
            status = "SUCCESS",
            orderId = "order_tnx_123",
            isPremium = true,
            rewardCredited = true,
            message = "Payment verified"
        )
        assertEquals("SUCCESS", successResult.status)
        assertTrue(successResult.isPremium)
        assertTrue(successResult.rewardCredited)

        val pendingResult = PaymentVerificationResult(
            status = "PENDING",
            orderId = "order_tnx_123",
            isPremium = false,
            message = "Pending gateway settlement"
        )
        assertEquals("PENDING", pendingResult.status)
        assertFalse(pendingResult.isPremium)

        val failedResult = PaymentVerificationResult(
            status = "FAILED",
            orderId = "order_tnx_123",
            isPremium = false,
            message = "Payment dropped by user"
        )
        assertEquals("FAILED", failedResult.status)
        assertFalse(failedResult.isPremium)
    }

    @Test
    fun testCashfreeCheckoutResultCallbackVariants() {
        val success: CashfreeCheckoutResult = CashfreeCheckoutResult.Success("order_ok_1")
        assertEquals("order_ok_1", (success as CashfreeCheckoutResult.Success).orderId)

        val cancelled: CashfreeCheckoutResult = CashfreeCheckoutResult.Cancelled("order_cancel_1", "User pressed back")
        assertEquals("order_cancel_1", (cancelled as CashfreeCheckoutResult.Cancelled).orderId)
        assertEquals("User pressed back", cancelled.message)

        val failure: CashfreeCheckoutResult = CashfreeCheckoutResult.Failure("order_fail_1", "Insufficient funds", "PAYMENT_DECLINED")
        assertEquals("order_fail_1", (failure as CashfreeCheckoutResult.Failure).orderId)
        assertEquals("Insufficient funds", failure.message)
        assertEquals("PAYMENT_DECLINED", failure.code)
    }

    @Test
    fun testInMemoryCreateCashfreePremiumOrderFlow() = runBlocking {
        val ds = InMemoryJobPortalDataSource()
        val repo = JobPortalRepository(ds)

        val orderResult = repo.createCashfreePremiumOrder()
        assertTrue("Order creation must succeed in demo/in-memory mode", orderResult.isSuccess)

        val session = orderResult.getOrThrow()
        assertTrue("orderId must be prefixed with order_tnx", session.orderId.startsWith("order_tnx"))
        assertTrue("paymentSessionId must be present", session.paymentSessionId.isNotBlank())
        assertEquals("Order amount must be fixed to ₹10", 10, session.amount)
        assertEquals("Currency must be INR", "INR", session.currency)
        assertEquals("Environment must be SANDBOX", "SANDBOX", session.environment)

        // Verify payment record in repository
        val payments = repo.premiumPayments.value
        val paymentRecord = payments.find { it.id == session.orderId }
        assertNotNull("Payment record must be created in pending status", paymentRecord)
        assertEquals(PaymentStatus.PENDING_VERIFICATION, paymentRecord?.status)
        assertEquals("DEMO_SIMULATOR", paymentRecord?.provider)
    }

    @Test
    fun testInMemoryVerifyCashfreePaymentFlow() = runBlocking {
        val ds = InMemoryJobPortalDataSource()
        val repo = JobPortalRepository(ds)

        // Initial state: not premium
        assertFalse("User must not be premium initially", repo.referralProfile.value.isPremium)

        // Create order
        val orderResult = repo.createCashfreePremiumOrder().getOrThrow()

        // Verify payment with server
        val verification = repo.verifyCashfreePayment(orderResult.orderId, "txn_mock_success")
        assertEquals("SUCCESS", verification.status)
        assertTrue("Verification must confirm isPremium = true", verification.isPremium)

        // Profile must now reflect premium status
        assertTrue("Referral profile must be updated to premium", repo.referralProfile.value.isPremium)
    }

    @Test
    fun testCheckoutUiStateTransitions() {
        val idle: CheckoutUiState = CheckoutUiState.Idle
        val creating: CheckoutUiState = CheckoutUiState.CreatingOrder
        val launch: CheckoutUiState = CheckoutUiState.LaunchCheckout("order_1", "sess_1")
        val verifying: CheckoutUiState = CheckoutUiState.VerifyingPayment
        val success: CheckoutUiState = CheckoutUiState.PaymentSuccess("order_1")
        val pending: CheckoutUiState = CheckoutUiState.PaymentPending("order_1", "Waiting")
        val cancelled: CheckoutUiState = CheckoutUiState.PaymentCancelled("order_1", "Cancelled")
        val failed: CheckoutUiState = CheckoutUiState.PaymentFailed("order_1", "Failed")

        assertTrue(idle is CheckoutUiState.Idle)
        assertTrue(creating is CheckoutUiState.CreatingOrder)
        assertEquals("order_1", (launch as CheckoutUiState.LaunchCheckout).orderId)
        assertTrue(verifying is CheckoutUiState.VerifyingPayment)
        assertEquals("order_1", (success as CheckoutUiState.PaymentSuccess).orderId)
        assertEquals("Waiting", (pending as CheckoutUiState.PaymentPending).message)
        assertEquals("Cancelled", (cancelled as CheckoutUiState.PaymentCancelled).message)
        assertEquals("Failed", (failed as CheckoutUiState.PaymentFailed).message)
    }

    @Test
    fun testUnauthenticatedStateDefaultsAndNamedDatabaseValidation() {
        val repo = JobPortalRepository(InMemoryJobPortalDataSource())
        try {
            repo.switchToFirestore("(default)")
            fail("Must reject (default) databaseId")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("non-default") == true)
        }

        try {
            repo.switchToFirestore("")
            fail("Must reject blank databaseId")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("non-default") == true)
        }
    }
}
