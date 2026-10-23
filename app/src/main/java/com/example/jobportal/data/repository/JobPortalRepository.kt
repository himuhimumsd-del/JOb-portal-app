package com.example.jobportal.data.repository

import com.example.jobportal.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface CloudConnectionState {
    object Uninitialized : CloudConnectionState
    data class Connecting(val databaseId: String) : CloudConnectionState
    data class Connected(val databaseId: String) : CloudConnectionState
    data class Error(val databaseId: String, val message: String, val cause: Throwable? = null) : CloudConnectionState
    object DemoMode : CloudConnectionState
}

/**
 * Facade Repository for TalentNexus Job Portal.
 * Coordinates between InMemoryJobPortalDataSource (demo / test mode) and FirestoreJobPortalDataSource (cloud mode).
 * Provides immediate synchronous state updates for UI and tests, combined with reactive observation
 * for cloud Firestore snapshot listeners.
 */
class JobPortalRepository(
    initialDataSource: JobPortalDataSource = createDefaultDataSource()
) {
    companion object {
        const val DEFAULT_FIRESTORE_DATABASE_ID = "ai-studio-android-jobporta-b95bcbd3-ac1e-47e0-8460-98c89a740c01"

        fun createDefaultDataSource(): JobPortalDataSource {
            return try {
                com.google.firebase.FirebaseApp.getInstance()
                FirestoreJobPortalDataSource(DEFAULT_FIRESTORE_DATABASE_ID)
            } catch (e: Throwable) {
                // When running outside Android or prior to Firebase initialization,
                // start in a clean disconnected state awaiting explicit connection
                DisconnectedJobPortalDataSource(
                    databaseId = DEFAULT_FIRESTORE_DATABASE_ID,
                    initialErrorMessage = "Awaiting live Firestore connection..."
                )
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    private val _isDemoMode = MutableStateFlow(initialDataSource is InMemoryJobPortalDataSource)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _connectionState = MutableStateFlow<CloudConnectionState>(
        when (initialDataSource) {
            is InMemoryJobPortalDataSource -> CloudConnectionState.DemoMode
            is FirestoreJobPortalDataSource -> CloudConnectionState.Connected(initialDataSource.databaseId)
            is DisconnectedJobPortalDataSource -> CloudConnectionState.Error(
                initialDataSource.databaseId,
                initialDataSource.initialErrorMessage ?: "Live database disconnected"
            )
            else -> CloudConnectionState.Uninitialized
        }
    )
    val connectionState: StateFlow<CloudConnectionState> = _connectionState.asStateFlow()

    private val _dataSource = MutableStateFlow(initialDataSource)
    val dataSource: StateFlow<JobPortalDataSource> = _dataSource.asStateFlow()

    val currentRole = MutableStateFlow(initialDataSource.currentRole.value)
    val jobs = MutableStateFlow(initialDataSource.jobs.value)
    val savedJobIds = MutableStateFlow(initialDataSource.savedJobIds.value)
    val applications = MutableStateFlow(initialDataSource.applications.value)
    val applicants = MutableStateFlow(initialDataSource.applicants.value)
    val interviews = MutableStateFlow(initialDataSource.interviews.value)
    val categories = MutableStateFlow(initialDataSource.categories.value)
    val jobAlerts = MutableStateFlow(initialDataSource.jobAlerts.value)
    val reportedItems = MutableStateFlow(initialDataSource.reportedItems.value)
    val users = MutableStateFlow(initialDataSource.users.value)
    val adminVitals = MutableStateFlow(initialDataSource.adminVitals.value)
    val seekerProfile = MutableStateFlow(initialDataSource.seekerProfile.value)
    val employerProfile = MutableStateFlow(initialDataSource.employerProfile.value)
    val referralProfile = MutableStateFlow(initialDataSource.referralProfile.value)
    val referrals = MutableStateFlow(initialDataSource.referrals.value)
    val ledgerEntries = MutableStateFlow(initialDataSource.ledgerEntries.value)
    val withdrawalRequests = MutableStateFlow(initialDataSource.withdrawalRequests.value)
    val premiumPayments = MutableStateFlow(initialDataSource.premiumPayments.value)
    val isLoading = MutableStateFlow(initialDataSource.isLoading.value)
    val errorMessage = MutableStateFlow(initialDataSource.errorMessage.value)

    private var observerJob: kotlinx.coroutines.Job? = null

    init {
        observeCurrentDataSource()
    }

    private fun syncStateFromCurrentDataSource() {
        val ds = _dataSource.value
        currentRole.value = ds.currentRole.value
        jobs.value = ds.jobs.value
        savedJobIds.value = ds.savedJobIds.value
        applications.value = ds.applications.value
        applicants.value = ds.applicants.value
        interviews.value = ds.interviews.value
        categories.value = ds.categories.value
        jobAlerts.value = ds.jobAlerts.value
        reportedItems.value = ds.reportedItems.value
        users.value = ds.users.value
        adminVitals.value = ds.adminVitals.value
        seekerProfile.value = ds.seekerProfile.value
        employerProfile.value = ds.employerProfile.value
        referralProfile.value = ds.referralProfile.value
        referrals.value = ds.referrals.value
        ledgerEntries.value = ds.ledgerEntries.value
        withdrawalRequests.value = ds.withdrawalRequests.value
        premiumPayments.value = ds.premiumPayments.value
        isLoading.value = ds.isLoading.value
        errorMessage.value = ds.errorMessage.value
    }

    private fun observeCurrentDataSource() {
        observerJob?.cancel()
        observerJob = scope.launch {
            val ds = _dataSource.value
            launch { ds.currentRole.collect { currentRole.value = it } }
            launch { ds.jobs.collect { jobs.value = it } }
            launch { ds.savedJobIds.collect { savedJobIds.value = it } }
            launch { ds.applications.collect { applications.value = it } }
            launch { ds.applicants.collect { applicants.value = it } }
            launch { ds.interviews.collect { interviews.value = it } }
            launch { ds.categories.collect { categories.value = it } }
            launch { ds.jobAlerts.collect { jobAlerts.value = it } }
            launch { ds.reportedItems.collect { reportedItems.value = it } }
            launch { ds.users.collect { users.value = it } }
            launch { ds.adminVitals.collect { adminVitals.value = it } }
            launch { ds.seekerProfile.collect { seekerProfile.value = it } }
            launch { ds.employerProfile.collect { employerProfile.value = it } }
            launch { ds.referralProfile.collect { referralProfile.value = it } }
            launch { ds.referrals.collect { referrals.value = it } }
            launch { ds.ledgerEntries.collect { ledgerEntries.value = it } }
            launch { ds.withdrawalRequests.collect { withdrawalRequests.value = it } }
            launch { ds.premiumPayments.collect { premiumPayments.value = it } }
            launch { ds.isLoading.collect { isLoading.value = it } }
            launch { ds.errorMessage.collect { errorMessage.value = it } }
        }
    }

    fun switchToDataSource(newDataSource: JobPortalDataSource) {
        _dataSource.value = newDataSource
        _isDemoMode.value = newDataSource is InMemoryJobPortalDataSource
        syncStateFromCurrentDataSource()
        observeCurrentDataSource()
    }

    fun switchToFirestore(databaseId: String) {
        require(databaseId.isNotBlank() && databaseId != "(default)") {
            "Cannot switch to Firestore: named databaseId must be specified and non-default. Received: '$databaseId'"
        }
        _connectionState.value = CloudConnectionState.Connecting(databaseId)
        try {
            val oldDs = _dataSource.value
            val firestoreDs = FirestoreJobPortalDataSource(databaseId = databaseId)
            oldDs.cleanup()
            switchToDataSource(firestoreDs)
            _connectionState.value = CloudConnectionState.Connected(databaseId)
        } catch (e: Throwable) {
            try {
                android.util.Log.e("JobPortalRepository", "Failed to connect to named Firestore database '$databaseId': ${e.message}", e)
            } catch (_: Throwable) {
                System.err.println("[JobPortalRepository] Failed to connect to named Firestore database '$databaseId': ${e.message}")
            }
            _connectionState.value = CloudConnectionState.Error(
                databaseId = databaseId,
                message = "Unable to connect to live cloud jobs. Please check your internet connection.",
                cause = e
            )
            val errorDs = DisconnectedJobPortalDataSource(
                databaseId = databaseId,
                initialErrorMessage = "Live cloud connection is currently unavailable. Please check your internet connection and try again."
            )
            _dataSource.value.cleanup()
            switchToDataSource(errorDs)
        }
    }

    fun switchToDemo() {
        _dataSource.value.cleanup()
        _connectionState.value = CloudConnectionState.DemoMode
        switchToDataSource(InMemoryJobPortalDataSource())
    }

    fun setRole(role: UserRole) {
        _dataSource.value.setRole(role)
        currentRole.value = _dataSource.value.currentRole.value
    }

    fun toggleSaveJob(jobId: String) {
        _dataSource.value.toggleSaveJob(jobId)
        savedJobIds.value = _dataSource.value.savedJobIds.value
    }

    fun applyToJob(jobId: String, resumeVersion: String): Boolean {
        val res = _dataSource.value.applyToJob(jobId, resumeVersion)
        applications.value = _dataSource.value.applications.value
        applicants.value = _dataSource.value.applicants.value
        jobs.value = _dataSource.value.jobs.value
        errorMessage.value = _dataSource.value.errorMessage.value
        return res
    }

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
    ): Job {
        val created = _dataSource.value.createJob(
            title, company, location, workplaceType, employmentType,
            minSalary, maxSalary, category, tags, description,
            responsibilities, requirements, benefits
        )
        jobs.value = _dataSource.value.jobs.value
        errorMessage.value = _dataSource.value.errorMessage.value
        return created
    }

    fun updateJobStatus(jobId: String, newStatus: JobStatus) {
        _dataSource.value.updateJobStatus(jobId, newStatus)
        jobs.value = _dataSource.value.jobs.value
        errorMessage.value = _dataSource.value.errorMessage.value
    }

    fun updateApplicantStage(applicantId: String, stage: ApplicationStage) {
        _dataSource.value.updateApplicantStage(applicantId, stage)
        applicants.value = _dataSource.value.applicants.value
        applications.value = _dataSource.value.applications.value
        errorMessage.value = _dataSource.value.errorMessage.value
    }

    fun scheduleNewInterview(
        candidateName: String,
        candidateRole: String,
        company: String,
        date: String,
        time: String,
        interviewer: String
    ) {
        _dataSource.value.scheduleNewInterview(candidateName, candidateRole, company, date, time, interviewer)
        interviews.value = _dataSource.value.interviews.value
        applications.value = _dataSource.value.applications.value
        applicants.value = _dataSource.value.applicants.value
        errorMessage.value = _dataSource.value.errorMessage.value
    }

    fun updateInterviewScorecard(interviewId: String, score: Int, notes: String) {
        _dataSource.value.updateInterviewScorecard(interviewId, score, notes)
        interviews.value = _dataSource.value.interviews.value
        errorMessage.value = _dataSource.value.errorMessage.value
    }

    fun rescheduleInterview(interviewId: String, newDate: String, newTime: String) {
        _dataSource.value.rescheduleInterview(interviewId, newDate, newTime)
        interviews.value = _dataSource.value.interviews.value
        applications.value = _dataSource.value.applications.value
        errorMessage.value = _dataSource.value.errorMessage.value
    }

    fun cancelInterview(interviewId: String) {
        _dataSource.value.cancelInterview(interviewId)
        interviews.value = _dataSource.value.interviews.value
        applications.value = _dataSource.value.applications.value
        errorMessage.value = _dataSource.value.errorMessage.value
    }

    fun addJobAlert(keyword: String, category: String, minSalary: String, frequency: String) {
        _dataSource.value.addJobAlert(keyword, category, minSalary, frequency)
        jobAlerts.value = _dataSource.value.jobAlerts.value
    }

    fun toggleJobAlert(id: String) {
        _dataSource.value.toggleJobAlert(id)
        jobAlerts.value = _dataSource.value.jobAlerts.value
    }

    fun moderateJob(jobId: String, approve: Boolean) {
        _dataSource.value.moderateJob(jobId, approve)
        jobs.value = _dataSource.value.jobs.value
    }

    fun resolveReport(reportId: String, status: String) {
        _dataSource.value.resolveReport(reportId, status)
        reportedItems.value = _dataSource.value.reportedItems.value
    }

    fun toggleUserVerification(userId: String) {
        _dataSource.value.toggleUserVerification(userId)
        users.value = _dataSource.value.users.value
    }

    fun toggleUserSuspension(userId: String) {
        _dataSource.value.toggleUserSuspension(userId)
        users.value = _dataSource.value.users.value
    }

    fun addCategory(name: String, icon: String) {
        _dataSource.value.addCategory(name, icon)
        categories.value = _dataSource.value.categories.value
    }

    fun updateEmployerProfile(profile: EmployerProfile) {
        _dataSource.value.updateEmployerProfile(profile)
        employerProfile.value = _dataSource.value.employerProfile.value
    }

    fun updateSeekerProfile(profile: JobSeekerProfile) {
        _dataSource.value.updateSeekerProfile(profile)
        seekerProfile.value = _dataSource.value.seekerProfile.value
    }

    // Referral & Premium Operations
    fun applyReferralAttribution(referralCode: String): Boolean {
        val result = _dataSource.value.applyReferralAttribution(referralCode)
        referralProfile.value = _dataSource.value.referralProfile.value
        errorMessage.value = _dataSource.value.errorMessage.value
        return result
    }

    fun initiatePremiumPayment(): PremiumPaymentRecord {
        val record = _dataSource.value.initiatePremiumPayment()
        premiumPayments.value = _dataSource.value.premiumPayments.value
        return record
    }

    fun processVerifiedPayment(paymentId: String, providerRef: String): Boolean {
        val result = _dataSource.value.processVerifiedPayment(paymentId, providerRef)
        premiumPayments.value = _dataSource.value.premiumPayments.value
        referralProfile.value = _dataSource.value.referralProfile.value
        referrals.value = _dataSource.value.referrals.value
        ledgerEntries.value = _dataSource.value.ledgerEntries.value
        errorMessage.value = _dataSource.value.errorMessage.value
        return result
    }

    suspend fun createCashfreePremiumOrder(): Result<CashfreeOrderSession> {
        val result = _dataSource.value.createCashfreePremiumOrder()
        errorMessage.value = _dataSource.value.errorMessage.value
        return result
    }

    suspend fun verifyCashfreePayment(orderId: String, providerRef: String? = null): PaymentVerificationResult {
        val result = _dataSource.value.verifyCashfreePayment(orderId, providerRef)
        referralProfile.value = _dataSource.value.referralProfile.value
        referrals.value = _dataSource.value.referrals.value
        ledgerEntries.value = _dataSource.value.ledgerEntries.value
        errorMessage.value = _dataSource.value.errorMessage.value
        return result
    }

    fun requestWithdrawal(amount: Int, payoutAccountDetails: String): Boolean {
        val result = _dataSource.value.requestWithdrawal(amount, payoutAccountDetails)
        referralProfile.value = _dataSource.value.referralProfile.value
        withdrawalRequests.value = _dataSource.value.withdrawalRequests.value
        ledgerEntries.value = _dataSource.value.ledgerEntries.value
        errorMessage.value = _dataSource.value.errorMessage.value
        return result
    }

    fun processWithdrawalDecision(requestId: String, approve: Boolean, adminNotes: String): Boolean {
        val result = _dataSource.value.processWithdrawalDecision(requestId, approve, adminNotes)
        withdrawalRequests.value = _dataSource.value.withdrawalRequests.value
        referralProfile.value = _dataSource.value.referralProfile.value
        ledgerEntries.value = _dataSource.value.ledgerEntries.value
        errorMessage.value = _dataSource.value.errorMessage.value
        return result
    }
}
