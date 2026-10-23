package com.example.jobportal.data.repository

import com.example.jobportal.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * In-memory implementation of JobPortalDataSource used for local unit tests and offline/demo mode.
 */
class InMemoryJobPortalDataSource : JobPortalDataSource {

    private val _currentRole = MutableStateFlow(UserRole.JOB_SEEKER)
    override val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    override val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _savedJobIds = MutableStateFlow<Set<String>>(setOf("job-1", "job-3"))
    override val savedJobIds: StateFlow<Set<String>> = _savedJobIds.asStateFlow()

    private val _applications = MutableStateFlow<List<Application>>(emptyList())
    override val applications: StateFlow<List<Application>> = _applications.asStateFlow()

    private val _applicants = MutableStateFlow<List<Applicant>>(emptyList())
    override val applicants: StateFlow<List<Applicant>> = _applicants.asStateFlow()

    private val _interviews = MutableStateFlow<List<InterviewItem>>(emptyList())
    override val interviews: StateFlow<List<InterviewItem>> = _interviews.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
    override val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    private val _jobAlerts = MutableStateFlow<List<JobAlert>>(emptyList())
    override val jobAlerts: StateFlow<List<JobAlert>> = _jobAlerts.asStateFlow()

    private val _reportedItems = MutableStateFlow<List<ReportedItem>>(emptyList())
    override val reportedItems: StateFlow<List<ReportedItem>> = _reportedItems.asStateFlow()

    private val _users = MutableStateFlow<List<PlatformUser>>(
        listOf(
            PlatformUser("usr-1", "Alex Vance", "alex.vance@techhorizon.io", "Job Seeker", "Active", true),
            PlatformUser("usr-2", "Acme Technologies", "recruiting@acme.tech", "Employer Org", "Verified", true),
            PlatformUser("usr-3", "Dr. Elena Rostova", "elena.rostova@ai-horizon.org", "Job Seeker", "Active", true),
            PlatformUser("usr-4", "MetaCrypt Apex Ltd", "contact@metacrypt.io", "Employer Org", "Pending KYB", false),
            PlatformUser("usr-5", "Marcus Thorne", "marcus.thorne@vector-core.io", "Job Seeker", "Active", true)
        )
    )
    override val users: StateFlow<List<PlatformUser>> = _users.asStateFlow()

    private val _adminVitals = MutableStateFlow(AdminVitals())
    override val adminVitals: StateFlow<AdminVitals> = _adminVitals.asStateFlow()

    private val _seekerProfile = MutableStateFlow(
        JobSeekerProfile(
            fullName = "Alex Vance, MSCS",
            title = "Staff Product Architect & Systems Engineer",
            email = "alex.vance@techhorizon.io",
            phone = "+1 (415) 890-4421",
            location = "San Francisco, CA (Open to Remote)",
            bio = "Ex-Stripe, Staff Engineer with 9+ years scaling distributed real-time platforms, design systems, and developer experience.",
            completionPercent = 92,
            skills = listOf("Kotlin", "Jetpack Compose", "Distributed Systems", "TypeScript", "System Architecture", "GraphQL", "Kubernetes"),
            resumes = listOf(
                ResumeItem("res-1", "Alex_Vance_Staff_Engineer_2026.pdf", "Updated 2d ago", isDefault = true, atsScore = 96),
                ResumeItem("res-2", "Alex_Vance_Product_Architect_v2.pdf", "Updated 2w ago", isDefault = false, atsScore = 89)
            )
        )
    )
    override val seekerProfile: StateFlow<JobSeekerProfile> = _seekerProfile.asStateFlow()

    private val _employerProfile = MutableStateFlow(
        EmployerProfile(
            companyName = "Acme Technologies",
            tagline = "Building autonomous intelligence infrastructure for the modern enterprise",
            industry = "Enterprise Cloud & AI Systems",
            website = "https://acme.tech",
            location = "San Francisco, CA • New York • London",
            isVerified = true,
            employeeCount = "250-500 employees",
            openRolesCount = 6,
            bio = "Acme is an enterprise infrastructure company backed by Tier-1 venture partners, powering millions of autonomous workflows worldwide.",
            perks = listOf("Comprehensive Medical, Dental & Vision", "401(k) 6% Match", "$3,000 Remote Work Stipend", "Unlimited PTO & Sabbatical", "Quarterly Equity Grants")
        )
    )
    override val employerProfile: StateFlow<EmployerProfile> = _employerProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        initializeData()
    }

    private fun initializeData() {
        _jobs.value = listOf(
            Job(
                id = "job-1",
                title = "Staff Product Designer",
                company = "Linear",
                location = "San Francisco, CA",
                workplaceType = WorkplaceType.REMOTE,
                employmentType = EmploymentType.FULL_TIME,
                salaryRange = "$180k - $220k",
                minSalary = 180000,
                category = "Product & UX",
                tags = listOf("Design Systems", "Figma", "Micro-interactions", "High Growth"),
                description = "Linear is looking for a Staff Product Designer to craft world-class software experiences that help high-performance product teams build software with speed and elegance.",
                responsibilities = listOf(
                    "Lead end-to-end design across core platform modules and mobile workflows",
                    "Evolve Linear's signature minimalist design system and animation language",
                    "Partner directly with engineering founders and executive leadership"
                ),
                requirements = listOf(
                    "7+ years of product design experience in SaaS or developer tools",
                    "Exceptional craft in typography, spatial layouts, and rapid prototyping",
                    "Strong visual portfolio highlighting shipped, consumer-grade desktop or mobile apps"
                ),
                benefits = listOf(
                    "Generous equity package",
                    "100% covered health, dental, and vision insurance",
                    "$4,000 annual home office & wellness budget",
                    "Bi-annual international team retreats"
                ),
                isVerified = true,
                isActivelyHiring = true,
                postedTime = "2h ago",
                status = JobStatus.ACTIVE,
                applicantsCount = 42
            ),
            Job(
                id = "job-2",
                title = "Principal Distributed Systems Engineer",
                company = "Stripe",
                location = "Seattle, WA",
                workplaceType = WorkplaceType.HYBRID,
                employmentType = EmploymentType.FULL_TIME,
                salaryRange = "$225k - $275k",
                minSalary = 225000,
                category = "Tech & Dev",
                tags = listOf("Go", "Kubernetes", "High Throughput", "Financial Infra"),
                description = "Stripe powers economic infrastructure for the internet. Join the Core Ledger team to architect resilient, zero-downtime consensus engines handling tens of billions of transactions annually.",
                responsibilities = listOf(
                    "Architect multi-region distributed ledgers with sub-5ms p99 latency",
                    "Guide architectural standards across 40+ engineering squads",
                    "Drive chaos engineering and fault-tolerance drill scenarios"
                ),
                requirements = listOf(
                    "8+ years engineering mission-critical backend systems in Go, Rust, or Java",
                    "Deep knowledge of Paxos/Raft consensus and database storage engines",
                    "Proven track record delivering ultra-reliable systems at planetary scale"
                ),
                benefits = listOf(
                    "Top 1% market compensation & liquidity",
                    "Flexible hybrid working rhythm",
                    "Comprehensive family healthcare",
                    "Annual learning & travel stipend"
                ),
                isVerified = true,
                isActivelyHiring = true,
                postedTime = "5h ago",
                status = JobStatus.ACTIVE,
                applicantsCount = 18
            ),
            Job(
                id = "job-3",
                title = "Lead AI Research Scientist (LLM Alignment)",
                company = "Anthropic",
                location = "San Francisco, CA",
                workplaceType = WorkplaceType.ON_SITE,
                employmentType = EmploymentType.FULL_TIME,
                salaryRange = "$260k - $340k",
                minSalary = 260000,
                category = "Data & AI",
                tags = listOf("PyTorch", "Alignment", "RLHF", "Frontier Models"),
                description = "Anthropic is an AI safety and research company. We're seeking a Lead AI Scientist to pioneer scalable oversight algorithms and mechanistic interpretability techniques for frontier models.",
                responsibilities = listOf(
                    "Design and execute novel reinforcement learning from human & AI feedback protocols",
                    "Analyze transformer attention dynamics and feature activations in high-parameter models",
                    "Publish breakthrough research at leading peer-reviewed venues"
                ),
                requirements = listOf(
                    "Ph.D. in Computer Science, Machine Learning, or related quantitative field",
                    "Strong publication track record at NeurIPS, ICML, or ICLR",
                    "Extensive practical experience training LLMs on massive GPU compute clusters"
                ),
                benefits = listOf(
                    "Substantial equity incentive grants",
                    "Unlimited access to dedicated supercomputing clusters",
                    "Full executive healthcare coverage",
                    "Daily chef-curated meals"
                ),
                isVerified = true,
                isActivelyHiring = true,
                postedTime = "1d ago",
                status = JobStatus.ACTIVE,
                applicantsCount = 67
            )
        )

        _categories.value = listOf(
            CategoryItem("cat-1", "Tech & Dev", "4,210 jobs", "code"),
            CategoryItem("cat-2", "Product & UX", "1,890 jobs", "design_services"),
            CategoryItem("cat-3", "Data & AI", "3,140 jobs", "psychology"),
            CategoryItem("cat-4", "Marketing & Growth", "980 jobs", "campaign"),
            CategoryItem("cat-5", "Finance & Ops", "760 jobs", "payments"),
            CategoryItem("cat-6", "Customer Success", "540 jobs", "support_agent")
        )

        _applications.value = listOf(
            Application(
                id = "app-101",
                jobId = "job-1",
                jobTitle = "Staff Product Designer",
                company = "Linear",
                appliedDate = "3 days ago",
                status = ApplicationStage.INTERVIEW_SCHEDULED,
                resumeVersion = "Alex_Vance_Staff_Engineer_2026.pdf",
                interviewDate = "Thursday, Oct 22",
                interviewTime = "2:30 PM PST",
                interviewMeetingUrl = "https://meet.google.com/lin-dsgn-vance",
                prepNotes = "Review design system tokens and cross-platform keyboard shortcuts workflow."
            ),
            Application(
                id = "app-102",
                jobId = "job-2",
                jobTitle = "Principal Distributed Systems Engineer",
                company = "Stripe",
                appliedDate = "1 week ago",
                status = ApplicationStage.IN_REVIEW,
                resumeVersion = "Alex_Vance_Staff_Engineer_2026.pdf"
            )
        )

        _applicants.value = listOf(
            Applicant(
                id = "cand-1",
                jobId = "job-3",
                jobTitle = "Lead AI Research Scientist (LLM Alignment)",
                candidateName = "Dr. Elena Rostova",
                headline = "Senior ML Scientist @ DeepMind • PhD Stanford",
                email = "elena.rostova@ai-horizon.org",
                phone = "+1 (650) 449-0129",
                location = "Palo Alto, CA",
                atsScore = 98,
                experienceYears = 7,
                stage = ApplicationStage.INTERVIEW_SCHEDULED,
                appliedTime = "1d ago",
                resumeFileName = "Elena_Rostova_CV_2026.pdf",
                keySkills = listOf("PyTorch", "RLHF", "Mechanistic Interpretability", "Distributed Training")
            ),
            Applicant(
                id = "cand-2",
                jobId = "job-1",
                jobTitle = "Staff Product Designer",
                candidateName = "Marcus Thorne",
                headline = "Principal Design Architect • ex-Airbnb",
                email = "marcus.thorne@vector-core.io",
                phone = "+1 (415) 302-8812",
                location = "San Francisco, CA",
                atsScore = 91,
                experienceYears = 9,
                stage = ApplicationStage.SHORTLISTED,
                appliedTime = "2d ago",
                resumeFileName = "Marcus_Thorne_Portfolio.pdf",
                keySkills = listOf("Figma Tokens", "Design Tokens", "React Native", "Prototyping")
            )
        )

        _interviews.value = listOf(
            InterviewItem(
                id = "int-1",
                candidateName = "Dr. Elena Rostova",
                candidateRole = "Lead AI Research Scientist",
                company = "Acme Technologies",
                date = "Today",
                time = "2:30 PM",
                duration = "60 mins",
                interviewerName = "Sarah Chen (VP AI Research)",
                meetingUrl = "https://meet.google.com/acme-talent-rostova",
                status = "Confirmed",
                scoreTechnical = 5,
                scoreCulture = 5,
                feedbackNotes = "Exceptional track record in alignment architectures. Strongly recommend offer."
            ),
            InterviewItem(
                id = "int-2",
                candidateName = "Alex Vance",
                candidateRole = "Staff Product Designer",
                company = "Linear",
                date = "Thursday, Oct 22",
                time = "2:30 PM PST",
                duration = "45 mins",
                interviewerName = "Karri Saarinen (Head of Design)",
                meetingUrl = "https://meet.google.com/lin-dsgn-vance",
                status = "Confirmed"
            )
        )

        _jobAlerts.value = listOf(
            JobAlert("alt-1", "Staff Engineer", "Tech & Dev", "$180k+", "Daily", true),
            JobAlert("alt-2", "Product Architect", "Product & UX", "$170k+", "Weekly", true)
        )

        _reportedItems.value = listOf(
            ReportedItem(
                id = "rep-1",
                type = "Job Listing",
                title = "Suspicious compensation disclosure on 'Senior Lead'",
                reportedBy = "seeker_flag_992",
                reason = "Inaccurate salary range advertised ($300k posted vs $120k actual contract)",
                timestamp = "3h ago",
                status = "Pending Audit"
            ),
            ReportedItem(
                id = "rep-2",
                type = "Employer Org",
                title = "Unverified entity registration 'MetaCrypt Apex'",
                reportedBy = "automated_fraud_sentinel",
                reason = "Failed automated KYB business registry cross-check",
                timestamp = "1d ago",
                status = "Under Review"
            )
        )
    }

    override fun setRole(role: UserRole) {
        _currentRole.value = role
    }

    override fun toggleSaveJob(jobId: String) {
        val current = _savedJobIds.value.toMutableSet()
        if (current.contains(jobId)) {
            current.remove(jobId)
        } else {
            current.add(jobId)
        }
        _savedJobIds.value = current
    }

    override fun applyToJob(jobId: String, resumeVersion: String): Boolean {
        val job = _jobs.value.find { it.id == jobId } ?: return false
        val newApp = Application(
            id = "app-${UUID.randomUUID().toString().take(6)}",
            jobId = jobId,
            jobTitle = job.title,
            company = job.company,
            appliedDate = "Just now",
            status = ApplicationStage.APPLIED,
            resumeVersion = resumeVersion
        )
        _applications.value = listOf(newApp) + _applications.value
        _jobs.value = _jobs.value.map {
            if (it.id == jobId) it.copy(applicantsCount = it.applicantsCount + 1) else it
        }

        val ats = _seekerProfile.value.resumes.find { it.fileName == resumeVersion }?.atsScore ?: 94
        val newApplicant = Applicant(
            id = "cand-${UUID.randomUUID().toString().take(6)}",
            jobId = jobId,
            jobTitle = job.title,
            candidateName = _seekerProfile.value.fullName,
            headline = _seekerProfile.value.title,
            email = _seekerProfile.value.email,
            phone = _seekerProfile.value.phone,
            location = _seekerProfile.value.location,
            atsScore = ats,
            experienceYears = 8,
            stage = ApplicationStage.APPLIED,
            appliedTime = "Just now",
            resumeFileName = resumeVersion,
            keySkills = _seekerProfile.value.skills
        )
        _applicants.value = listOf(newApplicant) + _applicants.value
        return true
    }

    override fun createJob(
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
    ): Job {
        val newJob = Job(
            id = "job-${UUID.randomUUID().toString().take(6)}",
            title = title,
            company = company,
            location = location,
            workplaceType = workplaceType,
            employmentType = employmentType,
            salaryRange = "$${minSalary / 1000}k - $${maxSalary / 1000}k",
            minSalary = minSalary,
            category = category,
            tags = tags,
            description = description,
            responsibilities = responsibilities,
            requirements = requirements,
            benefits = benefits,
            isVerified = true,
            isActivelyHiring = true,
            postedTime = "Just now",
            status = JobStatus.ACTIVE,
            applicantsCount = 0
        )
        _jobs.value = listOf(newJob) + _jobs.value
        _employerProfile.value = _employerProfile.value.copy(
            openRolesCount = _employerProfile.value.openRolesCount + 1
        )
        return newJob
    }

    override fun updateJobStatus(jobId: String, newStatus: JobStatus) {
        _jobs.value = _jobs.value.map {
            if (it.id == jobId) it.copy(status = newStatus) else it
        }
    }

    override fun updateApplicantStage(applicantId: String, stage: ApplicationStage) {
        val applicant = _applicants.value.find { it.id == applicantId }
        _applicants.value = _applicants.value.map {
            if (it.id == applicantId) it.copy(stage = stage) else it
        }
        if (applicant != null) {
            _applications.value = _applications.value.map { app ->
                if (app.jobId == applicant.jobId || app.jobTitle.equals(applicant.jobTitle, ignoreCase = true)) {
                    app.copy(status = stage)
                } else app
            }
        }
    }

    override fun scheduleNewInterview(
        candidateName: String,
        candidateRole: String,
        company: String,
        date: String,
        time: String,
        interviewer: String
    ) {
        val meetingUrl = "https://meet.google.com/tnx-interview-${UUID.randomUUID().toString().take(4)}"
        val newInt = InterviewItem(
            id = "int-${UUID.randomUUID().toString().take(6)}",
            candidateName = candidateName,
            candidateRole = candidateRole,
            company = company,
            date = date,
            time = time,
            interviewerName = interviewer,
            meetingUrl = meetingUrl
        )
        _interviews.value = listOf(newInt) + _interviews.value

        _applications.value = _applications.value.map { app ->
            if (app.jobTitle.equals(candidateRole, ignoreCase = true) || app.company.equals(company, ignoreCase = true)) {
                app.copy(
                    status = ApplicationStage.INTERVIEW_SCHEDULED,
                    interviewDate = date,
                    interviewTime = time,
                    interviewMeetingUrl = meetingUrl
                )
            } else app
        }

        _applicants.value = _applicants.value.map { cand ->
            if (cand.candidateName.equals(candidateName, ignoreCase = true) && cand.jobTitle.equals(candidateRole, ignoreCase = true)) {
                cand.copy(stage = ApplicationStage.INTERVIEW_SCHEDULED)
            } else cand
        }
    }

    override fun updateInterviewScorecard(interviewId: String, score: Int, notes: String) {
        _interviews.value = _interviews.value.map {
            if (it.id == interviewId) {
                it.copy(
                    scoreTechnical = score,
                    scoreCulture = score,
                    feedbackNotes = notes
                )
            } else it
        }
    }

    override fun rescheduleInterview(interviewId: String, newDate: String, newTime: String) {
        val target = _interviews.value.find { it.id == interviewId }
        _interviews.value = _interviews.value.map {
            if (it.id == interviewId) {
                it.copy(date = newDate, time = newTime, status = "Rescheduled")
            } else it
        }

        if (target != null) {
            _applications.value = _applications.value.map { app ->
                if (interviewId.contains(app.id) || app.jobTitle.equals(target.candidateRole, ignoreCase = true)) {
                    app.copy(interviewDate = newDate, interviewTime = newTime)
                } else app
            }
        }
    }

    override fun cancelInterview(interviewId: String) {
        val target = _interviews.value.find { it.id == interviewId }
        _interviews.value = _interviews.value.map {
            if (it.id == interviewId) {
                it.copy(status = "Cancelled")
            } else it
        }

        if (target != null) {
            _applications.value = _applications.value.map { app ->
                if (interviewId.contains(app.id) || app.jobTitle.equals(target.candidateRole, ignoreCase = true)) {
                    app.copy(
                        status = ApplicationStage.IN_REVIEW,
                        interviewDate = null,
                        interviewTime = null,
                        interviewMeetingUrl = null
                    )
                } else app
            }
        }
    }

    override fun addJobAlert(keyword: String, category: String, minSalary: String, frequency: String) {
        val alert = JobAlert(
            id = "alt-${UUID.randomUUID().toString().take(6)}",
            keyword = keyword,
            category = category,
            minSalary = minSalary,
            frequency = frequency,
            isActive = true
        )
        _jobAlerts.value = listOf(alert) + _jobAlerts.value
    }

    override fun toggleJobAlert(id: String) {
        _jobAlerts.value = _jobAlerts.value.map {
            if (it.id == id) it.copy(isActive = !it.isActive) else it
        }
    }

    override fun moderateJob(jobId: String, approve: Boolean) {
        _jobs.value = _jobs.value.map {
            if (it.id == jobId) {
                it.copy(status = if (approve) JobStatus.ACTIVE else JobStatus.CLOSED)
            } else it
        }
    }

    override fun resolveReport(reportId: String, status: String) {
        _reportedItems.value = _reportedItems.value.map {
            if (it.id == reportId) it.copy(status = status) else it
        }
        _adminVitals.value = _adminVitals.value.copy(
            pendingAuditsCount = (_adminVitals.value.pendingAuditsCount - 1).coerceAtLeast(0)
        )
    }

    override fun toggleUserVerification(userId: String) {
        _users.value = _users.value.map {
            if (it.id == userId) {
                val newVerified = !it.isVerified
                it.copy(
                    isVerified = newVerified,
                    status = if (newVerified) (if (it.roleType == "Job Seeker") "Active" else "Verified") else "Unverified"
                )
            } else it
        }
    }

    override fun toggleUserSuspension(userId: String) {
        _users.value = _users.value.map {
            if (it.id == userId) {
                val isSuspended = it.status == "Suspended"
                it.copy(status = if (isSuspended) "Active" else "Suspended")
            } else it
        }
    }

    override fun addCategory(name: String, icon: String) {
        val newCat = CategoryItem("cat-${UUID.randomUUID().toString().take(4)}", name, "0 jobs", icon)
        _categories.value = _categories.value + newCat
    }

    override fun updateEmployerProfile(profile: EmployerProfile) {
        _employerProfile.value = profile
    }

    override fun updateSeekerProfile(profile: JobSeekerProfile) {
        _seekerProfile.value = profile
    }

    // Referral System Implementation
    private val _referralProfile = MutableStateFlow(
        ReferralProfile(
            referralCode = "TNX-ALEX99",
            shareableLink = "https://talentnexus.app/join?ref=TNX-ALEX99",
            totalInvitedCount = 8,
            registeredCount = 2,
            pendingPaymentCount = 1,
            successfulReferralsCount = 5,
            totalRewardsEarned = 25,
            availableBalance = 25,
            reservedBalance = 0,
            isEligibleForWithdrawal = false, // 25 < 50 and 5 < 10
            isPremium = false
        )
    )
    override val referralProfile: StateFlow<ReferralProfile> = _referralProfile.asStateFlow()

    private val _referrals = MutableStateFlow<List<ReferralItem>>(
        listOf(
            ReferralItem(
                id = "ref-1",
                referrerUid = "usr-candidate-1",
                referredUid = "usr-demo-sarah",
                referredName = "Sarah J.",
                referralCode = "TNX-ALEX99",
                status = ReferralStatus.REWARD_CREDITED,
                rewardAmount = 5,
                qualifyingPaymentRef = "pay_mock_101",
                createdAt = "3 days ago",
                rewardedAt = "2 days ago"
            ),
            ReferralItem(
                id = "ref-2",
                referrerUid = "usr-candidate-1",
                referredUid = "usr-demo-david",
                referredName = "David K.",
                referralCode = "TNX-ALEX99",
                status = ReferralStatus.PREMIUM_PENDING,
                rewardAmount = 0,
                qualifyingPaymentRef = null,
                createdAt = "Yesterday"
            ),
            ReferralItem(
                id = "ref-3",
                referrerUid = "usr-candidate-1",
                referredUid = "usr-demo-elena",
                referredName = "Elena R.",
                referralCode = "TNX-ALEX99",
                status = ReferralStatus.REGISTERED,
                rewardAmount = 0,
                qualifyingPaymentRef = null,
                createdAt = "5 hours ago"
            )
        )
    )
    override val referrals: StateFlow<List<ReferralItem>> = _referrals.asStateFlow()

    private val _ledgerEntries = MutableStateFlow<List<LedgerEntry>>(
        listOf(
            LedgerEntry(
                id = "ledger_reward_ref_1",
                userId = "usr-candidate-1",
                type = LedgerType.REFERRAL_REWARD,
                amount = 5,
                referenceId = "ref-1",
                notes = "Referral bonus for verified payment from Sarah J.",
                timestamp = "2 days ago"
            )
        )
    )
    override val ledgerEntries: StateFlow<List<LedgerEntry>> = _ledgerEntries.asStateFlow()

    private val _withdrawalRequests = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    override val withdrawalRequests: StateFlow<List<WithdrawalRequest>> = _withdrawalRequests.asStateFlow()

    private val _premiumPayments = MutableStateFlow<List<PremiumPaymentRecord>>(emptyList())
    override val premiumPayments: StateFlow<List<PremiumPaymentRecord>> = _premiumPayments.asStateFlow()

    private var attributedReferralCode: String? = null

    override fun applyReferralAttribution(referralCode: String): Boolean {
        val cleanCode = referralCode.trim().uppercase()
        if (cleanCode == _referralProfile.value.referralCode) {
            _errorMessage.value = "Self-referral is not allowed."
            return false
        }
        if (attributedReferralCode != null) {
            _errorMessage.value = "Referral attribution is already established and cannot be modified."
            return false
        }
        attributedReferralCode = cleanCode
        _errorMessage.value = null
        return true
    }

    override fun initiatePremiumPayment(): PremiumPaymentRecord {
        val paymentId = "pay_${UUID.randomUUID().toString().take(8)}"
        val record = PremiumPaymentRecord(
            id = paymentId,
            userId = "usr-candidate-1",
            userEmail = _seekerProfile.value.email,
            amount = 10,
            currency = "INR",
            status = PaymentStatus.PENDING_VERIFICATION,
            provider = "MOCK_GATEWAY",
            createdAt = "Just now"
        )
        _premiumPayments.value = listOf(record) + _premiumPayments.value
        return record
    }

    override fun processVerifiedPayment(paymentId: String, providerRef: String): Boolean {
        val payment = _premiumPayments.value.find { it.id == paymentId }
        if (payment == null) {
            _errorMessage.value = "Payment record not found."
            return false
        }
        if (payment.status == PaymentStatus.VERIFIED) {
            // Idempotent: already verified
            return true
        }

        // 1. Mark payment verified
        val verifiedPayment = payment.copy(
            status = PaymentStatus.VERIFIED,
            providerTransactionRef = providerRef,
            verifiedAt = "Just now"
        )
        _premiumPayments.value = _premiumPayments.value.map { if (it.id == paymentId) verifiedPayment else it }

        // 2. Grant premium entitlement
        _referralProfile.value = _referralProfile.value.copy(isPremium = true)

        // 3. Check referral attribution & credit ₹5 to referrer if attributed and not already rewarded
        val code = attributedReferralCode
        if (code != null) {
            val existingRef = _referrals.value.find { it.referralCode == code && it.status == ReferralStatus.REWARD_CREDITED }
            if (existingRef == null) {
                // Credit reward
                val rewardAmount = 5
                val newBal = _referralProfile.value.availableBalance + rewardAmount
                val newEarned = _referralProfile.value.totalRewardsEarned + rewardAmount
                val newSuccessful = _referralProfile.value.successfulReferralsCount + 1
                val eligible = (newSuccessful >= 10 && newBal >= 50)

                _referralProfile.value = _referralProfile.value.copy(
                    availableBalance = newBal,
                    availableBalancePaise = newBal * 100,
                    totalRewardsEarned = newEarned,
                    totalRewardsEarnedPaise = newEarned * 100,
                    successfulReferralsCount = newSuccessful,
                    isEligibleForWithdrawal = eligible
                )

                // Add ledger entry
                val ledger = LedgerEntry(
                    id = "ledger_reward_${paymentId}",
                    userId = "referrer_${code}",
                    type = LedgerType.REFERRAL_REWARD,
                    amount = rewardAmount,
                    referenceId = paymentId,
                    notes = "Referral reward: ₹5 for verified payment $providerRef",
                    timestamp = "Just now"
                )
                _ledgerEntries.value = listOf(ledger) + _ledgerEntries.value

                // Update referral status
                val newRef = ReferralItem(
                    id = "ref_${paymentId}",
                    referrerUid = "referrer_${code}",
                    referredUid = "usr-candidate-1",
                    referredName = _seekerProfile.value.fullName,
                    referralCode = code,
                    status = ReferralStatus.REWARD_CREDITED,
                    rewardAmount = rewardAmount,
                    qualifyingPaymentRef = paymentId,
                    createdAt = "Today",
                    rewardedAt = "Just now"
                )
                _referrals.value = listOf(newRef) + _referrals.value
            }
        }
        return true
    }

    override suspend fun createCashfreePremiumOrder(): Result<CashfreeOrderSession> {
        val orderId = "order_tnx_demo_${System.currentTimeMillis()}"
        val sessionId = "session_demo_${orderId}"
        val record = PremiumPaymentRecord(
            id = orderId,
            userId = "usr-candidate-1",
            userEmail = _seekerProfile.value.email,
            amount = 10,
            currency = "INR",
            status = PaymentStatus.PENDING_VERIFICATION,
            provider = "DEMO_SIMULATOR",
            paymentSessionId = sessionId,
            createdAt = "Just now"
        )
        _premiumPayments.value = listOf(record) + _premiumPayments.value
        return Result.success(
            CashfreeOrderSession(
                orderId = orderId,
                paymentSessionId = sessionId,
                amount = 10,
                currency = "INR",
                environment = "SANDBOX"
            )
        )
    }

    override suspend fun verifyCashfreePayment(orderId: String, providerRef: String?): PaymentVerificationResult {
        val success = processVerifiedPayment(orderId, providerRef ?: "demo_sim_${System.currentTimeMillis()}")
        return if (success) {
            PaymentVerificationResult(
                status = "SUCCESS",
                orderId = orderId,
                isPremium = true,
                rewardCredited = true,
                message = "Demo simulation mode: verified in-memory only (not a live Cashfree payment)"
            )
        } else {
            PaymentVerificationResult(
                status = "FAILED",
                orderId = orderId,
                isPremium = false,
                message = _errorMessage.value ?: "Verification failed in demo mode"
            )
        }
    }

    override fun requestWithdrawal(amount: Int, payoutAccountDetails: String): Boolean {
        val profile = _referralProfile.value
        val requestedRupees = amount

        // Enforce BOTH conditions per business specification
        if (profile.successfulReferralsCount < 10) {
            _errorMessage.value = "Redemption blocked: Requires at least 10 successful referrals (currently ${profile.successfulReferralsCount})."
            return false
        }
        if (profile.availableBalance < 50 && profile.availableBalancePaise < 5000) {
            _errorMessage.value = "Redemption blocked: Requires at least ₹50 available referral balance (currently ₹${profile.availableBalance})."
            return false
        }
        if (requestedRupees < 50) {
            _errorMessage.value = "Minimum withdrawal amount is ₹50."
            return false
        }
        if (requestedRupees > profile.availableBalance) {
            _errorMessage.value = "Insufficient available balance. Requested ₹$requestedRupees, available ₹${profile.availableBalance}."
            return false
        }
        if (payoutAccountDetails.isBlank()) {
            _errorMessage.value = "Please enter valid UPI ID or bank account details."
            return false
        }

        // Atomically reserve funds
        val newAvailable = profile.availableBalance - requestedRupees
        val newReserved = profile.reservedBalance + requestedRupees
        val eligible = (profile.successfulReferralsCount >= 10 && newAvailable >= 50)

        _referralProfile.value = profile.copy(
            availableBalance = newAvailable,
            availableBalancePaise = newAvailable * 100,
            reservedBalance = newReserved,
            reservedBalancePaise = newReserved * 100,
            isEligibleForWithdrawal = eligible
        )

        val reqId = "wdr_${UUID.randomUUID().toString().take(8)}"
        val request = WithdrawalRequest(
            id = reqId,
            userId = "usr-candidate-1",
            userName = _seekerProfile.value.fullName,
            userEmail = _seekerProfile.value.email,
            amount = requestedRupees,
            payoutMethod = if (payoutAccountDetails.contains("@")) "UPI" else "Bank Transfer (NEFT)",
            payoutAccountDetails = payoutAccountDetails,
            status = WithdrawalStatus.REQUESTED,
            requestedAt = "Just now"
        )
        _withdrawalRequests.value = listOf(request) + _withdrawalRequests.value

        val ledger = LedgerEntry(
            id = "ledger_reserve_${reqId}",
            userId = "usr-candidate-1",
            type = LedgerType.WITHDRAWAL_REQUEST_RESERVED,
            amount = -requestedRupees,
            referenceId = reqId,
            notes = "Reserved ₹$requestedRupees for withdrawal to $payoutAccountDetails",
            timestamp = "Just now"
        )
        _ledgerEntries.value = listOf(ledger) + _ledgerEntries.value
        return true
    }

    override fun processWithdrawalDecision(requestId: String, approve: Boolean, adminNotes: String): Boolean {
        val req = _withdrawalRequests.value.find { it.id == requestId }
        if (req == null || req.status != WithdrawalStatus.REQUESTED) {
            _errorMessage.value = "Withdrawal request not found or already processed."
            return false
        }

        val profile = _referralProfile.value
        if (approve) {
            // Deduct from reserved balance permanently
            val newReserved = (profile.reservedBalance - req.amount).coerceAtLeast(0)
            _referralProfile.value = profile.copy(
                reservedBalance = newReserved,
                reservedBalancePaise = newReserved * 100
            )

            _withdrawalRequests.value = _withdrawalRequests.value.map {
                if (it.id == requestId) it.copy(
                    status = WithdrawalStatus.COMPLETED,
                    adminNotes = adminNotes.ifBlank { "Approved and processed manually" },
                    processedAt = "Just now",
                    processedBy = "ADMIN_CONSOLE"
                ) else it
            }

            val ledger = LedgerEntry(
                id = "ledger_payout_${requestId}",
                userId = req.userId,
                type = LedgerType.WITHDRAWAL_APPROVED_PAYOUT,
                amount = -req.amount,
                referenceId = requestId,
                notes = "Payout completed: ₹${req.amount} via ${req.payoutMethod}",
                timestamp = "Just now"
            )
            _ledgerEntries.value = listOf(ledger) + _ledgerEntries.value
        } else {
            // Restore reserved funds back to available balance
            val newAvailable = profile.availableBalance + req.amount
            val newReserved = (profile.reservedBalance - req.amount).coerceAtLeast(0)
            val eligible = (profile.successfulReferralsCount >= 10 && newAvailable >= 50)

            _referralProfile.value = profile.copy(
                availableBalance = newAvailable,
                availableBalancePaise = newAvailable * 100,
                reservedBalance = newReserved,
                reservedBalancePaise = newReserved * 100,
                isEligibleForWithdrawal = eligible
            )

            _withdrawalRequests.value = _withdrawalRequests.value.map {
                if (it.id == requestId) it.copy(
                    status = WithdrawalStatus.REJECTED,
                    adminNotes = adminNotes.ifBlank { "Rejected by administrator" },
                    processedAt = "Just now",
                    processedBy = "ADMIN_CONSOLE"
                ) else it
            }

            val ledger = LedgerEntry(
                id = "ledger_restore_${requestId}",
                userId = req.userId,
                type = LedgerType.WITHDRAWAL_REJECTED_RESTORED,
                amount = req.amount,
                referenceId = requestId,
                notes = "Restored ₹${req.amount} to available balance: $adminNotes",
                timestamp = "Just now"
            )
            _ledgerEntries.value = listOf(ledger) + _ledgerEntries.value
        }
        return true
    }

    fun seedReferralStats(availableRupees: Int, successfulReferrals: Int) {
        val eligible = (successfulReferrals >= 10 && availableRupees >= 50)
        _referralProfile.value = _referralProfile.value.copy(
            availableBalance = availableRupees,
            availableBalancePaise = availableRupees * 100,
            successfulReferralsCount = successfulReferrals,
            isEligibleForWithdrawal = eligible
        )
    }
}
