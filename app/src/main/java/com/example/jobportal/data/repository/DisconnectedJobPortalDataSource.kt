package com.example.jobportal.data.repository

import com.example.jobportal.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Disconnected live placeholder data source.
 * Represents an uninitialized, failed, or offline live Firestore connection state.
 *
 * CRITICAL ARCHITECTURAL GUARANTEE:
 * This data source maintains empty collections and surfaces clear, user-friendly diagnostic
 * error messages. It ensures the application NEVER silently falls back to mock in-memory data
 * when live cloud database connectivity fails or permissions are denied.
 */
class DisconnectedJobPortalDataSource(
    val databaseId: String,
    val initialErrorMessage: String? = null
) : JobPortalDataSource {

    private val _currentRole = MutableStateFlow(UserRole.JOB_SEEKER)
    override val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    override val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _savedJobIds = MutableStateFlow<Set<String>>(emptySet())
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

    private val _users = MutableStateFlow<List<PlatformUser>>(emptyList())
    override val users: StateFlow<List<PlatformUser>> = _users.asStateFlow()

    private val _adminVitals = MutableStateFlow(AdminVitals())
    override val adminVitals: StateFlow<AdminVitals> = _adminVitals.asStateFlow()

    private val _seekerProfile = MutableStateFlow(
        JobSeekerProfile(
            fullName = "Candidate",
            title = "Professional",
            email = "",
            phone = "",
            location = "Remote",
            bio = "Live database '$databaseId' is currently disconnected.",
            completionPercent = 0,
            skills = emptyList(),
            resumes = emptyList()
        )
    )
    override val seekerProfile: StateFlow<JobSeekerProfile> = _seekerProfile.asStateFlow()

    private val _employerProfile = MutableStateFlow(
        EmployerProfile(
            companyName = "Organization",
            tagline = "Hiring Partner",
            industry = "Technology",
            website = "",
            location = "Remote",
            isVerified = false,
            employeeCount = "1-10",
            openRolesCount = 0,
            bio = "Live database '$databaseId' is currently disconnected.",
            perks = emptyList()
        )
    )
    override val employerProfile: StateFlow<EmployerProfile> = _employerProfile.asStateFlow()

    private val _referralProfile = MutableStateFlow(
        ReferralProfile(
            referralCode = "",
            shareableLink = ""
        )
    )
    override val referralProfile: StateFlow<ReferralProfile> = _referralProfile.asStateFlow()

    private val _referrals = MutableStateFlow<List<ReferralItem>>(emptyList())
    override val referrals: StateFlow<List<ReferralItem>> = _referrals.asStateFlow()

    private val _ledgerEntries = MutableStateFlow<List<LedgerEntry>>(emptyList())
    override val ledgerEntries: StateFlow<List<LedgerEntry>> = _ledgerEntries.asStateFlow()

    private val _withdrawalRequests = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    override val withdrawalRequests: StateFlow<List<WithdrawalRequest>> = _withdrawalRequests.asStateFlow()

    private val _premiumPayments = MutableStateFlow<List<PremiumPaymentRecord>>(emptyList())
    override val premiumPayments: StateFlow<List<PremiumPaymentRecord>> = _premiumPayments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(initialErrorMessage)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    override fun setRole(role: UserRole) {
        _currentRole.value = role
    }

    override fun toggleSaveJob(jobId: String) {
        val current = _savedJobIds.value.toMutableSet()
        if (current.contains(jobId)) current.remove(jobId) else current.add(jobId)
        _savedJobIds.value = current
    }

    override fun applyToJob(jobId: String, resumeVersion: String): Boolean {
        _errorMessage.value = "Cannot submit application: Live Firestore database '$databaseId' is disconnected."
        return false
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
        _errorMessage.value = "Cannot post job: Live Firestore database '$databaseId' is disconnected."
        throw IllegalStateException("Live Firestore database '$databaseId' is disconnected. Action rejected.")
    }

    override fun updateJobStatus(jobId: String, newStatus: JobStatus) {
        _errorMessage.value = "Cannot update job: Live Firestore database '$databaseId' is disconnected."
    }

    override fun updateApplicantStage(applicantId: String, stage: ApplicationStage) {
        _errorMessage.value = "Cannot update applicant: Live Firestore database '$databaseId' is disconnected."
    }

    override fun scheduleNewInterview(
        candidateName: String,
        candidateRole: String,
        company: String,
        date: String,
        time: String,
        interviewer: String
    ) {
        _errorMessage.value = "Cannot schedule interview: Live Firestore database '$databaseId' is disconnected."
    }

    override fun updateInterviewScorecard(interviewId: String, score: Int, notes: String) {
        _errorMessage.value = "Cannot update scorecard: Live Firestore database '$databaseId' is disconnected."
    }

    override fun rescheduleInterview(interviewId: String, newDate: String, newTime: String) {
        _errorMessage.value = "Cannot reschedule interview: Live Firestore database '$databaseId' is disconnected."
    }

    override fun cancelInterview(interviewId: String) {
        _errorMessage.value = "Cannot cancel interview: Live Firestore database '$databaseId' is disconnected."
    }

    override fun addJobAlert(keyword: String, category: String, minSalary: String, frequency: String) {
        _errorMessage.value = "Cannot save job alert: Live Firestore database '$databaseId' is disconnected."
    }

    override fun toggleJobAlert(id: String) {}

    override fun moderateJob(jobId: String, approve: Boolean) {
        _errorMessage.value = "Cannot moderate job: Live Firestore database '$databaseId' is disconnected."
    }

    override fun resolveReport(reportId: String, status: String) {
        _errorMessage.value = "Cannot resolve report: Live Firestore database '$databaseId' is disconnected."
    }

    override fun toggleUserVerification(userId: String) {
        _errorMessage.value = "Cannot update user: Live Firestore database '$databaseId' is disconnected."
    }

    override fun toggleUserSuspension(userId: String) {
        _errorMessage.value = "Cannot update user: Live Firestore database '$databaseId' is disconnected."
    }

    override fun addCategory(name: String, icon: String) {
        _errorMessage.value = "Cannot add category: Live Firestore database '$databaseId' is disconnected."
    }

    override fun updateEmployerProfile(profile: EmployerProfile) {
        _employerProfile.value = profile
    }

    override fun updateSeekerProfile(profile: JobSeekerProfile) {
        _seekerProfile.value = profile
    }

    override fun applyReferralAttribution(referralCode: String): Boolean {
        _errorMessage.value = "Cannot apply referral attribution: Live Firestore database '$databaseId' is disconnected."
        return false
    }

    override fun initiatePremiumPayment(): PremiumPaymentRecord {
        _errorMessage.value = "Cannot initiate payment: Live Firestore database '$databaseId' is disconnected."
        throw IllegalStateException("Live Firestore database '$databaseId' is disconnected.")
    }

    override fun processVerifiedPayment(paymentId: String, providerRef: String): Boolean {
        _errorMessage.value = "Cannot verify payment: Live Firestore database '$databaseId' is disconnected."
        return false
    }

    override suspend fun createCashfreePremiumOrder(): Result<CashfreeOrderSession> {
        val err = "Cannot create Cashfree order: Live Firestore database '$databaseId' is disconnected."
        _errorMessage.value = err
        return Result.failure(IllegalStateException(err))
    }

    override suspend fun verifyCashfreePayment(orderId: String, providerRef: String?): PaymentVerificationResult {
        val err = "Cannot verify Cashfree payment: Live Firestore database '$databaseId' is disconnected."
        _errorMessage.value = err
        return PaymentVerificationResult(
            status = "FAILED",
            orderId = orderId,
            message = err
        )
    }

    override fun requestWithdrawal(amount: Int, payoutAccountDetails: String): Boolean {
        _errorMessage.value = "Cannot request withdrawal: Live Firestore database '$databaseId' is disconnected."
        return false
    }

    override fun processWithdrawalDecision(requestId: String, approve: Boolean, adminNotes: String): Boolean {
        _errorMessage.value = "Cannot decide withdrawal: Live Firestore database '$databaseId' is disconnected."
        return false
    }

    override fun cleanup() {
        // No-op for disconnected state
    }
}
