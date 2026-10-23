package com.example.jobportal.ui

import android.app.Activity
import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobportal.BuildConfig
import com.example.jobportal.data.model.*
import com.example.jobportal.data.payment.CashfreeCheckoutManager
import com.example.jobportal.data.payment.CashfreeCheckoutResult
import com.example.jobportal.data.repository.AuthRepository
import com.example.jobportal.data.repository.AuthUiState
import com.example.jobportal.data.repository.AuthUser
import com.example.jobportal.data.repository.JobPortalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    val repository: JobPortalRepository = JobPortalRepository(),
    val authRepository: AuthRepository? = try { AuthRepository() } catch (e: Throwable) { null },
    val cashfreeCheckoutManager: CashfreeCheckoutManager = CashfreeCheckoutManager(),
    private val isDebugBuild: Boolean = run {
        try {
            BuildConfig.DEBUG
        } catch (_: Throwable) {
            true
        }
    }
) : ViewModel() {

    val currentRole = repository.currentRole
    val jobs = repository.jobs
    val savedJobIds = repository.savedJobIds
    val applications = repository.applications
    val applicants = repository.applicants
    val interviews = repository.interviews
    val categories = repository.categories
    val jobAlerts = repository.jobAlerts
    val reportedItems = repository.reportedItems
    val adminVitals = repository.adminVitals
    val users = repository.users
    val seekerProfile = repository.seekerProfile
    val employerProfile = repository.employerProfile
    val referralProfile = repository.referralProfile
    val referrals = repository.referrals
    val ledgerEntries = repository.ledgerEntries
    val withdrawalRequests = repository.withdrawalRequests
    val premiumPayments = repository.premiumPayments

    // Data Source & Cloud States
    val isDemoMode: StateFlow<Boolean> = repository.isDemoMode
    val cloudConnectionState: StateFlow<com.example.jobportal.data.repository.CloudConnectionState> = repository.connectionState
    val isDataLoading: StateFlow<Boolean> = repository.isLoading
    val dataSourceErrorMessage: StateFlow<String?> = repository.errorMessage



    // Authentication State
    val authUser: StateFlow<AuthUser?> = authRepository?.currentUser ?: MutableStateFlow<AuthUser?>(null).asStateFlow()
    val authUiState: StateFlow<AuthUiState> = authRepository?.authUiState ?: MutableStateFlow<AuthUiState>(AuthUiState.Unauthenticated).asStateFlow()

    private val _showAuthDialog = MutableStateFlow(false)
    val showAuthDialog: StateFlow<Boolean> = _showAuthDialog.asStateFlow()

    // Referral & Premium Dialog / Screen States
    private val _showPremiumAccessDialog = MutableStateFlow(false)
    val showPremiumAccessDialog: StateFlow<Boolean> = _showPremiumAccessDialog.asStateFlow()

    private val _checkoutUiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val checkoutUiState: StateFlow<CheckoutUiState> = _checkoutUiState.asStateFlow()

    private val _showReferralDashboard = MutableStateFlow(false)
    val showReferralDashboard: StateFlow<Boolean> = _showReferralDashboard.asStateFlow()

    private val _showEnterReferralCodeDialog = MutableStateFlow(false)
    val showEnterReferralCodeDialog: StateFlow<Boolean> = _showEnterReferralCodeDialog.asStateFlow()

    private val _incomingReferralCode = MutableStateFlow<String?>(null)
    val incomingReferralCode: StateFlow<String?> = _incomingReferralCode.asStateFlow()

    private val _showRedemptionRequestDialog = MutableStateFlow(false)
    val showRedemptionRequestDialog: StateFlow<Boolean> = _showRedemptionRequestDialog.asStateFlow()

    private val _showRedemptionHistoryDialog = MutableStateFlow(false)
    val showRedemptionHistoryDialog: StateFlow<Boolean> = _showRedemptionHistoryDialog.asStateFlow()

    private val _activeTab = MutableStateFlow("home")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _selectedJob = MutableStateFlow<Job?>(null)
    val selectedJob: StateFlow<Job?> = _selectedJob.asStateFlow()

    private val _applyingJob = MutableStateFlow<Job?>(null)
    val applyingJob: StateFlow<Job?> = _applyingJob.asStateFlow()

    private val _selectedApplication = MutableStateFlow<Application?>(null)
    val selectedApplication: StateFlow<Application?> = _selectedApplication.asStateFlow()

    private val _selectedApplicant = MutableStateFlow<Applicant?>(null)
    val selectedApplicant: StateFlow<Applicant?> = _selectedApplicant.asStateFlow()

    private val _showCreateJobWizard = MutableStateFlow(false)
    val showCreateJobWizard: StateFlow<Boolean> = _showCreateJobWizard.asStateFlow()

    private val _showScheduleInterview = MutableStateFlow<Applicant?>(null)
    val showScheduleInterview: StateFlow<Applicant?> = _showScheduleInterview.asStateFlow()

    private val _showRoleSwitcherSheet = MutableStateFlow(false)
    val showRoleSwitcherSheet: StateFlow<Boolean> = _showRoleSwitcherSheet.asStateFlow()

    private val _showJobAlertModal = MutableStateFlow(false)
    val showJobAlertModal: StateFlow<Boolean> = _showJobAlertModal.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedWorkplaceFilter = MutableStateFlow<WorkplaceType?>(null)
    val selectedWorkplaceFilter: StateFlow<WorkplaceType?> = _selectedWorkplaceFilter.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun switchRole(role: UserRole) {
        // Enforce role security: Prevent unprivileged elevation to Admin in non-debug/release builds
        if (!isDebugBuild && role == UserRole.ADMIN) {
            _snackbarMessage.value = "Admin governance requires verified IAM authorization in production."
            return
        }
        repository.setRole(role)
        _activeTab.value = when (role) {
            UserRole.JOB_SEEKER -> "home"
            UserRole.EMPLOYER -> "dashboard"
            UserRole.ADMIN -> "dashboard"
        }
        _showRoleSwitcherSheet.value = false
    }

    fun openAuthDialog() {
        _showAuthDialog.value = true
    }

    fun closeAuthDialog() {
        _showAuthDialog.value = false
        authRepository?.clearError()
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            val result = authRepository?.signInWithGoogle(activity)
            if (result?.isSuccess == true) {
                val user = result.getOrNull()
                _snackbarMessage.value = "Welcome, ${user?.displayName ?: "Candidate"}! 🎉"
                _showAuthDialog.value = false
                // Pop up referral screen upon login so user can immediately invite or apply codes
                if (!incomingReferralCode.value.isNullOrBlank()) {
                    _showEnterReferralCodeDialog.value = true
                } else {
                    _showReferralDashboard.value = true
                }
            } else if (result?.isFailure == true) {
                val exception = result.exceptionOrNull()
                if (exception !is GetCredentialCancellationException) {
                    _snackbarMessage.value = exception?.localizedMessage ?: "Sign-in could not be completed. Please try again."
                }
            }
        }
    }

    fun attemptAutoSignIn(context: Context) {
        viewModelScope.launch {
            val success = authRepository?.attemptAutoSignIn(context) == true
            if (success) {
                val user = authRepository?.currentUser?.value
                if (user != null) {
                    _snackbarMessage.value = "Welcome back, ${user.displayName}! 👋"
                }
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            authRepository?.signOut(context)
            _snackbarMessage.value = "You have signed out."
        }
    }

    fun connectToLiveFirestore(databaseId: String = JobPortalRepository.DEFAULT_FIRESTORE_DATABASE_ID) {
        try {
            repository.switchToFirestore(databaseId)
            _snackbarMessage.value = "Connected to live jobs ✨"
        } catch (e: Throwable) {
            _snackbarMessage.value = "Could not connect to live cloud. Please check your internet connection."
        }
    }

    fun retryLiveConnection() {
        val targetDb = when (val state = repository.connectionState.value) {
            is com.example.jobportal.data.repository.CloudConnectionState.Error -> state.databaseId
            is com.example.jobportal.data.repository.CloudConnectionState.Connected -> state.databaseId
            is com.example.jobportal.data.repository.CloudConnectionState.Connecting -> state.databaseId
            else -> JobPortalRepository.DEFAULT_FIRESTORE_DATABASE_ID
        }
        connectToLiveFirestore(targetDb)
    }

    fun enableCloudMode(databaseId: String) {
        connectToLiveFirestore(databaseId)
    }

    fun enableDemoMode() {
        repository.switchToDemo()
        _snackbarMessage.value = "Switched to Demo Mode"
    }

    fun setTab(tab: String) {
        _activeTab.value = tab
    }

    fun openJobDetails(job: Job) {
        _selectedJob.value = job
    }

    fun closeJobDetails() {
        _selectedJob.value = null
    }

    fun startApplyFlow(job: Job) {
        _applyingJob.value = job
    }

    fun closeApplyFlow() {
        _applyingJob.value = null
    }

    fun submitApplication(jobId: String, resumeVersion: String) {
        val success = repository.applyToJob(jobId, resumeVersion)
        if (success) {
            _snackbarMessage.value = "Application successfully submitted!"
            _applyingJob.value = null
            _selectedJob.value = null
        } else {
            _snackbarMessage.value = repository.errorMessage.value ?: "Could not submit application"
        }
    }

    fun toggleSave(jobId: String) {
        repository.toggleSaveJob(jobId)
    }

    fun openApplicationDetails(app: Application) {
        _selectedApplication.value = app
    }

    fun closeApplicationDetails() {
        _selectedApplication.value = null
    }

    fun openApplicantDetails(applicant: Applicant) {
        _selectedApplicant.value = applicant
    }

    fun closeApplicantDetails() {
        _selectedApplicant.value = null
    }

    fun openCreateJobWizard() {
        _showCreateJobWizard.value = true
    }

    fun closeCreateJobWizard() {
        _showCreateJobWizard.value = false
    }

    fun openScheduleInterview(applicant: Applicant) {
        _showScheduleInterview.value = applicant
    }

    fun closeScheduleInterview() {
        _showScheduleInterview.value = null
    }

    fun openRoleSwitcher() {
        _showRoleSwitcherSheet.value = true
    }

    fun closeRoleSwitcher() {
        _showRoleSwitcherSheet.value = false
    }

    fun openJobAlertModal() {
        _showJobAlertModal.value = true
    }

    fun closeJobAlertModal() {
        _showJobAlertModal.value = false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun selectWorkplaceFilter(workplace: WorkplaceType?) {
        _selectedWorkplaceFilter.value = workplace
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // Referral & Premium Dialog Actions
    fun openPremiumAccess() {
        _showPremiumAccessDialog.value = true
    }

    fun closePremiumAccess() {
        _showPremiumAccessDialog.value = false
    }

    fun openReferralDashboard() {
        _showReferralDashboard.value = true
    }

    fun closeReferralDashboard() {
        _showReferralDashboard.value = false
    }

    fun openEnterReferralCode(prefillCode: String? = null) {
        if (!prefillCode.isNullOrBlank()) {
            _incomingReferralCode.value = prefillCode.trim().uppercase()
        }
        _showEnterReferralCodeDialog.value = true
    }

    fun handleIncomingReferralCode(code: String) {
        val clean = code.trim().uppercase()
        if (clean.isNotBlank()) {
            _incomingReferralCode.value = clean
            _showEnterReferralCodeDialog.value = true
            _snackbarMessage.value = "Referral invite detected: $clean 🎁"
        }
    }

    fun closeEnterReferralCode() {
        _showEnterReferralCodeDialog.value = false
        _incomingReferralCode.value = null
    }

    fun openRedemptionRequest() {
        _showRedemptionRequestDialog.value = true
    }

    fun closeRedemptionRequest() {
        _showRedemptionRequestDialog.value = false
    }

    fun openRedemptionHistory() {
        _showRedemptionHistoryDialog.value = true
    }

    fun closeRedemptionHistory() {
        _showRedemptionHistoryDialog.value = false
    }

    fun applyReferralCode(code: String): Boolean {
        val success = repository.applyReferralAttribution(code)
        if (success) {
            _snackbarMessage.value = "Referral code applied! You earned ₹5 bonus 🎉"
        } else {
            _snackbarMessage.value = repository.errorMessage.value ?: "Could not apply referral code. Please check the code and try again."
        }
        return success
    }

    fun initiatePremiumPayment(): PremiumPaymentRecord {
        return repository.initiatePremiumPayment()
    }

    fun verifyPremiumPayment(paymentId: String, providerRef: String): Boolean {
        val success = repository.processVerifiedPayment(paymentId, providerRef)
        if (success) {
            _snackbarMessage.value = "Premium activated! 🌟 Enjoy direct recruiter reach and priority screening."
        } else {
            _snackbarMessage.value = repository.errorMessage.value ?: "Payment could not be verified. Please try again."
        }
        return success
    }

    fun submitRedemptionRequest(amount: Int, payoutAccount: String): Boolean {
        val success = repository.requestWithdrawal(amount, payoutAccount)
        if (success) {
            _snackbarMessage.value = "Withdrawal request for ₹$amount submitted successfully! 👍"
        } else {
            _snackbarMessage.value = repository.errorMessage.value ?: "Withdrawal request could not be submitted. Please try again."
        }
        return success
    }

    fun processWithdrawalDecision(requestId: String, approve: Boolean, adminNotes: String? = null): Boolean {
        val success = repository.processWithdrawalDecision(requestId, approve, adminNotes ?: "")
        if (success) {
            _snackbarMessage.value = if (approve) "Withdrawal approved" else "Withdrawal rejected & reserved funds restored"
        } else {
            _snackbarMessage.value = repository.errorMessage.value ?: "Failed to process withdrawal decision"
        }
        return success
    }

    fun resetCheckoutState() {
        _checkoutUiState.value = CheckoutUiState.Idle
    }

    fun startCashfreeCheckout(activity: Activity) {
        viewModelScope.launch {
            _checkoutUiState.value = CheckoutUiState.CreatingOrder
            val result = repository.createCashfreePremiumOrder()
            result.onSuccess { session ->
                _checkoutUiState.value = CheckoutUiState.LaunchCheckout(session.orderId, session.paymentSessionId)
                cashfreeCheckoutManager.startCheckout(
                    activity = activity,
                    orderId = session.orderId,
                    paymentSessionId = session.paymentSessionId
                ) { checkoutResult ->
                    when (checkoutResult) {
                        is CashfreeCheckoutResult.Success -> {
                            // On SDK callback, invoke authoritative backend verification using returned orderId
                            verifyCashfreePaymentWithServer(checkoutResult.orderId)
                        }
                        is CashfreeCheckoutResult.Cancelled -> {
                            _checkoutUiState.value = CheckoutUiState.PaymentCancelled(checkoutResult.orderId, checkoutResult.message)
                            _snackbarMessage.value = "Cashfree payment cancelled"
                        }
                        is CashfreeCheckoutResult.Failure -> {
                            _checkoutUiState.value = CheckoutUiState.PaymentFailed(checkoutResult.orderId, checkoutResult.message)
                            _snackbarMessage.value = "Cashfree payment failed: ${checkoutResult.message}"
                        }
                    }
                }
            }.onFailure { e ->
                val errorMsg = e.localizedMessage ?: "Failed to create Cashfree order on server"
                _checkoutUiState.value = CheckoutUiState.PaymentFailed("", errorMsg)
                _snackbarMessage.value = errorMsg
            }
        }
    }

    fun verifyCashfreePaymentWithServer(orderId: String, providerRef: String? = null) {
        viewModelScope.launch {
            _checkoutUiState.value = CheckoutUiState.VerifyingPayment
            val verification = repository.verifyCashfreePayment(orderId, providerRef)
            when (verification.status) {
                "SUCCESS" -> {
                    if (verification.isPremium) {
                        _checkoutUiState.value = CheckoutUiState.PaymentSuccess(orderId)
                        _snackbarMessage.value = "Premium membership activated! Order: $orderId"
                    } else {
                        _checkoutUiState.value = CheckoutUiState.PaymentPending(orderId, "Payment received. Finalizing premium access...")
                    }
                }
                "PENDING" -> {
                    _checkoutUiState.value = CheckoutUiState.PaymentPending(orderId, verification.message ?: "Payment is pending gateway confirmation.")
                }
                else -> {
                    _checkoutUiState.value = CheckoutUiState.PaymentFailed(orderId, verification.message ?: "Payment verification failed.")
                }
            }
        }
    }
}

sealed interface CheckoutUiState {
    object Idle : CheckoutUiState
    object CreatingOrder : CheckoutUiState
    data class LaunchCheckout(val orderId: String, val paymentSessionId: String) : CheckoutUiState
    object VerifyingPayment : CheckoutUiState
    data class PaymentSuccess(val orderId: String) : CheckoutUiState
    data class PaymentPending(val orderId: String, val message: String) : CheckoutUiState
    data class PaymentCancelled(val orderId: String, val message: String) : CheckoutUiState
    data class PaymentFailed(val orderId: String, val message: String) : CheckoutUiState
}
