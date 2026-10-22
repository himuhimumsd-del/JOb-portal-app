package com.example.jobportal.data.repository

import com.example.jobportal.data.model.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Data source abstraction for TalentNexus Job Portal.
 * Enables clean separation between in-memory mock demo mode and authenticated Firestore cloud mode.
 */
interface JobPortalDataSource {
    val currentRole: StateFlow<UserRole>
    val jobs: StateFlow<List<Job>>
    val savedJobIds: StateFlow<Set<String>>
    val applications: StateFlow<List<Application>>
    val applicants: StateFlow<List<Applicant>>
    val interviews: StateFlow<List<InterviewItem>>
    val categories: StateFlow<List<CategoryItem>>
    val jobAlerts: StateFlow<List<JobAlert>>
    val reportedItems: StateFlow<List<ReportedItem>>
    val users: StateFlow<List<PlatformUser>>
    val adminVitals: StateFlow<AdminVitals>
    val seekerProfile: StateFlow<JobSeekerProfile>
    val employerProfile: StateFlow<EmployerProfile>
    val referralProfile: StateFlow<ReferralProfile>
    val referrals: StateFlow<List<ReferralItem>>
    val ledgerEntries: StateFlow<List<LedgerEntry>>
    val withdrawalRequests: StateFlow<List<WithdrawalRequest>>
    val premiumPayments: StateFlow<List<PremiumPaymentRecord>>

    val isLoading: StateFlow<Boolean>
    val errorMessage: StateFlow<String?>

    fun setRole(role: UserRole)
    fun toggleSaveJob(jobId: String)
    fun applyToJob(jobId: String, resumeVersion: String): Boolean
    fun createJob(
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
    ): Job

    fun updateJobStatus(jobId: String, newStatus: JobStatus)
    fun updateApplicantStage(applicantId: String, stage: ApplicationStage)
    fun scheduleNewInterview(
        candidateName: String,
        candidateRole: String,
        company: String,
        date: String,
        time: String,
        interviewer: String
    )

    fun updateInterviewScorecard(interviewId: String, score: Int, notes: String)
    fun rescheduleInterview(interviewId: String, newDate: String, newTime: String)
    fun cancelInterview(interviewId: String)
    fun addJobAlert(keyword: String, category: String, minSalary: String, frequency: String)
    fun toggleJobAlert(id: String)
    fun moderateJob(jobId: String, approve: Boolean)
    fun resolveReport(reportId: String, status: String)
    fun toggleUserVerification(userId: String)
    fun toggleUserSuspension(userId: String)
    fun addCategory(name: String, icon: String)
    fun updateEmployerProfile(profile: EmployerProfile)
    fun updateSeekerProfile(profile: JobSeekerProfile)

    // Referral System Operations
    fun applyReferralAttribution(referralCode: String): Boolean
    fun initiatePremiumPayment(): PremiumPaymentRecord
    fun processVerifiedPayment(paymentId: String, providerRef: String): Boolean
    fun requestWithdrawal(amount: Int, payoutAccountDetails: String): Boolean
    fun processWithdrawalDecision(requestId: String, approve: Boolean, adminNotes: String): Boolean

    /**
     * Releases active listeners, coroutines, and resource handles when switching data sources or closing.
     */
    fun cleanup() {}
}
