package com.example.jobportal.data.repository

import android.util.Log
import com.example.jobportal.data.model.*
import com.example.jobportal.data.model.firestore.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Authenticated Firestore implementation of JobPortalDataSource for TalentNexus.
 * Enforces server-side security rules, custom database ID scoping, and real-time snapshot listeners.
 */
private fun createNamedFirestore(databaseId: String): FirebaseFirestore {
    require(databaseId.isNotBlank() && databaseId != "(default)") {
        "Named Firestore database ID must be explicitly configured and non-default. Received: '$databaseId'"
    }
    val app = com.google.firebase.FirebaseApp.getInstance()
    return FirebaseFirestore.getInstance(app, databaseId)
}

class FirestoreJobPortalDataSource(
    private val firestore: FirebaseFirestore,
    val databaseId: String = "ai-studio-android-jobporta-b95bcbd3-ac1e-47e0-8460-98c89a740c01",
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : JobPortalDataSource {

    constructor(
        databaseId: String
    ) : this(
        createNamedFirestore(databaseId),
        databaseId,
        FirebaseAuth.getInstance()
    )

    constructor(
        databaseId: String,
        auth: FirebaseAuth
    ) : this(
        createNamedFirestore(databaseId),
        databaseId,
        auth
    )

    private val tag = "FirestoreDataSource"

    private val _currentRole = MutableStateFlow(UserRole.JOB_SEEKER)
    override val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    override val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
    override val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    private val _savedJobIds = MutableStateFlow<Set<String>>(emptySet())
    override val savedJobIds: StateFlow<Set<String>> = _savedJobIds.asStateFlow()

    private val _applications = MutableStateFlow<List<Application>>(emptyList())
    override val applications: StateFlow<List<Application>> = _applications.asStateFlow()

    private val _applicants = MutableStateFlow<List<Applicant>>(emptyList())
    override val applicants: StateFlow<List<Applicant>> = _applicants.asStateFlow()

    private val _interviews = MutableStateFlow<List<InterviewItem>>(emptyList())
    override val interviews: StateFlow<List<InterviewItem>> = _interviews.asStateFlow()

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
            fullName = auth.currentUser?.displayName ?: "Candidate",
            title = "Professional",
            email = auth.currentUser?.email ?: "",
            phone = "",
            location = "United States",
            bio = "Member of भर्ती-App verified talent network.",
            completionPercent = 80,
            skills = listOf("Kotlin", "Mobile Architecture", "Systems Engineering"),
            resumes = listOf(
                ResumeItem("res-default", "Primary_Resume.pdf", "Recent", isDefault = true, atsScore = 95)
            )
        )
    )
    override val seekerProfile: StateFlow<JobSeekerProfile> = _seekerProfile.asStateFlow()

    private val _employerProfile = MutableStateFlow(
        EmployerProfile(
            companyName = "Enterprise Partner",
            tagline = "Verified Hiring Organization",
            industry = "Technology",
            website = "https://talentnexus.io",
            location = "San Francisco, CA",
            isVerified = false,
            employeeCount = "50-100 employees",
            openRolesCount = 0,
            bio = "Verified recruiting pipeline on भर्ती-App.",
            perks = listOf("Health, Dental & Vision", "Flexible Work", "Retirement 401(k)")
        )
    )
    override val employerProfile: StateFlow<EmployerProfile> = _employerProfile.asStateFlow()

    // Referral System State
    private val _referralProfile = MutableStateFlow(
        ReferralProfile(
            referralCode = "",
            shareableLink = "",
            totalInvitedCount = 0,
            registeredCount = 0,
            pendingPaymentCount = 0,
            successfulReferralsCount = 0,
            totalRewardsEarned = 0,
            availableBalance = 0,
            reservedBalance = 0,
            isEligibleForWithdrawal = false,
            isPremium = false
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

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var jobsListener: ListenerRegistration? = null
    private var categoriesListener: ListenerRegistration? = null
    private var candidateAppsListener: ListenerRegistration? = null
    private var employerAppsListener: ListenerRegistration? = null
    private var candidateInterviewsListener: ListenerRegistration? = null
    private var employerInterviewsListener: ListenerRegistration? = null
    private var userProfileListener: ListenerRegistration? = null
    private var referralsListener: ListenerRegistration? = null
    private var ledgerListener: ListenerRegistration? = null
    private var withdrawalsListener: ListenerRegistration? = null
    private var paymentsListener: ListenerRegistration? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            _seekerProfile.value = _seekerProfile.value.copy(
                fullName = user.displayName?.ifBlank { "Candidate" } ?: "Candidate",
                email = user.email ?: ""
            )
            configureRoleListeners(_currentRole.value)
            startReferralListeners()
        } else {
            detachUserScopedListeners()
            clearUserScopedState()
        }
    }

    init {
        try {
            auth.addAuthStateListener(authListener)
        } catch (_: Throwable) {}
        startListeners()
    }

    fun startListeners() {
        // Observe public active jobs from the named database
        jobsListener?.remove()
        jobsListener = firestore.collection("jobs")
            .whereEqualTo("status", JobStatus.ACTIVE.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing active jobs in database '$databaseId': ${error.code} - ${error.message}")
                    _errorMessage.value = "Failed to load live jobs from '$databaseId' (${error.code.name}): ${error.localizedMessage ?: "Database error"}"
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val liveJobs = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            JobEntity.fromDocument(doc.id, data).toDomainModel()
                        }
                    }
                    _jobs.value = liveJobs
                    _errorMessage.value = null
                }
            }

        // Observe taxonomy categories
        categoriesListener?.remove()
        categoriesListener = firestore.collection("taxonomy_categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing categories: ${error.message}", error)
                    _errorMessage.value = "Failed to load categories: ${error.localizedMessage}"
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val liveCats = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            CategoryEntity.fromDocument(doc.id, data).toDomainModel()
                        }
                    }
                    _categories.value = liveCats
                    _errorMessage.value = null
                }
            }

        configureRoleListeners(_currentRole.value)
        startReferralListeners()
    }

    private fun configureRoleListeners(role: UserRole) {
        if (role == UserRole.JOB_SEEKER) {
            employerAppsListener?.remove()
            employerAppsListener = null
            employerInterviewsListener?.remove()
            employerInterviewsListener = null
            startCandidateApplicationsListener()
            startCandidateInterviewsListener()
        } else if (role == UserRole.EMPLOYER) {
            candidateAppsListener?.remove()
            candidateAppsListener = null
            candidateInterviewsListener?.remove()
            candidateInterviewsListener = null
            startEmployerApplicationsListener()
            startEmployerInterviewsListener()
        }
    }

    private fun startCandidateApplicationsListener() {
        candidateAppsListener?.remove()
        val uid = auth.currentUser?.uid ?: return
        candidateAppsListener = firestore.collection("applications")
            .whereEqualTo("candidateId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to candidate applications: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val apps = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            ApplicationEntity.fromDocument(doc.id, data).toDomainModel()
                        }
                    }
                    _applications.value = apps
                }
            }
    }

    private fun startCandidateInterviewsListener() {
        candidateInterviewsListener?.remove()
        val uid = auth.currentUser?.uid ?: return
        candidateInterviewsListener = firestore.collection("interviews")
            .whereEqualTo("candidateId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to candidate interviews: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val liveInterviews = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            InterviewEntity.fromDocument(doc.id, data).toDomainModel()
                        }
                    }
                    _interviews.value = liveInterviews
                }
            }
    }

    private fun startEmployerApplicationsListener() {
        employerAppsListener?.remove()
        val companyId = "org-${_employerProfile.value.companyName.lowercase().replace(" ", "-")}"
        employerAppsListener = firestore.collection("applications")
            .whereEqualTo("companyId", companyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to employer applicants: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val applicantsList = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            ApplicationEntity.fromDocument(doc.id, data).toApplicant()
                        }
                    }
                    _applicants.value = applicantsList
                }
            }
    }

    private fun startEmployerInterviewsListener() {
        employerInterviewsListener?.remove()
        val companyId = "org-${_employerProfile.value.companyName.lowercase().replace(" ", "-")}"
        employerInterviewsListener = firestore.collection("interviews")
            .whereEqualTo("companyId", companyId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to employer interviews: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val liveInterviews = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            InterviewEntity.fromDocument(doc.id, data).toDomainModel()
                        }
                    }
                    _interviews.value = liveInterviews
                }
            }
    }

    private fun startReferralListeners() {
        val uid = auth.currentUser?.uid ?: return

        // 1. User profile listener for referral code & balances
        userProfileListener?.remove()
        userProfileListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing user profile: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = UserEntity.fromDocument(snapshot.id, snapshot.data ?: emptyMap())
                    val available = user.availableReferralBalance
                    val successful = user.successfulReferralsCount
                    val isEligible = (available >= 50 && successful >= 10)
                    val code = user.referralCode.ifBlank { "TNX-" + uid.take(6).uppercase() }

                    _referralProfile.value = _referralProfile.value.copy(
                        referralCode = code,
                        shareableLink = "https://talentnexus.app/join?ref=$code",
                        successfulReferralsCount = successful,
                        totalRewardsEarned = user.totalEarnedRewards,
                        availableBalance = available,
                        reservedBalance = user.reservedWithdrawalBalance,
                        isEligibleForWithdrawal = isEligible,
                        isPremium = user.isPremium
                    )
                } else if (snapshot != null && !snapshot.exists()) {
                    val newCode = "TNX-" + UUID.randomUUID().toString().take(6).uppercase()
                    val initialUser = UserEntity(
                        id = uid,
                        fullName = auth.currentUser?.displayName.orEmpty().ifBlank { "Executive Candidate" },
                        email = auth.currentUser?.email.orEmpty(),
                        referralCode = newCode,
                        createdAt = "Just now"
                    )
                    firestore.collection("users").document(uid).set(initialUser.toDocumentMap())
                }
            }

        // 2. Referrals listener
        referralsListener?.remove()
        referralsListener = firestore.collection("referrals")
            .whereEqualTo("referrerUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing referrals: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { ReferralEntity.fromDocument(doc.id, it).toDomainModel() }
                    }
                    _referrals.value = items
                    val registered = items.count { it.status == ReferralStatus.REGISTERED }
                    val pending = items.count { it.status == ReferralStatus.PREMIUM_PENDING }
                    _referralProfile.value = _referralProfile.value.copy(
                        totalInvitedCount = items.size,
                        registeredCount = registered,
                        pendingPaymentCount = pending
                    )
                }
            }

        // 3. Ledger entries listener
        ledgerListener?.remove()
        ledgerListener = firestore.collection("referral_ledger")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { LedgerEntityDoc.fromDocument(doc.id, it).toDomainModel() }
                    }
                    _ledgerEntries.value = items
                }
            }

        // 4. Withdrawal requests listener
        withdrawalsListener?.remove()
        val wdrQuery = if (_currentRole.value == UserRole.ADMIN) {
            firestore.collection("withdrawal_requests")
        } else {
            firestore.collection("withdrawal_requests").whereEqualTo("userId", uid)
        }
        withdrawalsListener = wdrQuery.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { WithdrawalEntity.fromDocument(doc.id, it).toDomainModel() }
                }
                _withdrawalRequests.value = items
            }
        }

        // 5. Premium payments listener
        paymentsListener?.remove()
        paymentsListener = firestore.collection("premium_payments")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { PaymentEntity.fromDocument(doc.id, it).toDomainModel() }
                    }
                    _premiumPayments.value = items
                }
            }
    }

    private fun detachUserScopedListeners() {
        candidateAppsListener?.remove()
        candidateAppsListener = null
        employerAppsListener?.remove()
        employerAppsListener = null
        candidateInterviewsListener?.remove()
        candidateInterviewsListener = null
        employerInterviewsListener?.remove()
        employerInterviewsListener = null
        userProfileListener?.remove()
        userProfileListener = null
        referralsListener?.remove()
        referralsListener = null
        ledgerListener?.remove()
        ledgerListener = null
        withdrawalsListener?.remove()
        withdrawalsListener = null
        paymentsListener?.remove()
        paymentsListener = null
    }

    private fun clearUserScopedState() {
        _applications.value = emptyList()
        _applicants.value = emptyList()
        _interviews.value = emptyList()
        _referrals.value = emptyList()
        _ledgerEntries.value = emptyList()
        _withdrawalRequests.value = emptyList()
        _premiumPayments.value = emptyList()
        _referralProfile.value = ReferralProfile(referralCode = "", shareableLink = "")
        _seekerProfile.value = JobSeekerProfile(
            fullName = "Candidate",
            title = "Professional",
            email = "",
            phone = "",
            location = "Remote",
            bio = "Member of भर्ती-App verified talent network.",
            completionPercent = 0,
            skills = emptyList(),
            resumes = emptyList()
        )
    }

    fun stopListeners() {
        try {
            auth.removeAuthStateListener(authListener)
        } catch (_: Exception) {}
        jobsListener?.remove()
        jobsListener = null
        categoriesListener?.remove()
        categoriesListener = null
        detachUserScopedListeners()
    }

    override fun cleanup() {
        stopListeners()
    }

    override fun setRole(role: UserRole) {
        _currentRole.value = role
        configureRoleListeners(role)
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
        val user = auth.currentUser
        if (user == null) {
            _errorMessage.value = "Authentication required. Please sign in with Google to apply."
            return false
        }

        // Validate active job
        val job = _jobs.value.find { it.id == jobId }
        if (job == null || job.status != JobStatus.ACTIVE) {
            _errorMessage.value = "This position is no longer accepting active applications."
            return false
        }

        val deterministicAppId = "${jobId}_${user.uid}"

        // Local duplicate check
        if (_applications.value.any { it.id == deterministicAppId || it.jobId == jobId }) {
            _errorMessage.value = "You have already applied to this position."
            return false
        }

        val profile = _seekerProfile.value
        val candidateName = profile.fullName.ifBlank { user.displayName.orEmpty() }
        val candidateEmail = profile.email.ifBlank { user.email.orEmpty() }

        if (candidateEmail.isBlank()) {
            _errorMessage.value = "Candidate email is required to submit an application."
            return false
        }

        val companyId = "org-${job.company.lowercase().replace(" ", "-")}"
        val chosenResume = resumeVersion.ifBlank {
            profile.resumes.firstOrNull { it.isDefault }?.fileName ?: "Primary_Resume.pdf"
        }

        val entity = ApplicationEntity(
            id = deterministicAppId,
            jobId = jobId,
            jobTitle = job.title,
            company = job.company,
            companyId = companyId,
            candidateId = user.uid,
            candidateName = candidateName,
            candidateEmail = candidateEmail,
            candidateHeadline = profile.title.ifBlank { "भर्ती-App Candidate" },
            candidatePhone = profile.phone,
            candidateLocation = profile.location.ifBlank { "United States" },
            keySkills = profile.skills.ifEmpty { listOf("Engineering", "Software Architecture") },
            atsScore = 0,
            experienceYears = 3,
            appliedDate = "Just now",
            status = ApplicationStage.APPLIED.name,
            resumeVersion = chosenResume
        )

        // Optimistically reflect submission locally
        val submittedApp = entity.toDomainModel()
        _applications.value = listOf(submittedApp) + _applications.value

        scope.launch {
            try {
                _isLoading.value = true
                val docRef = firestore.collection("applications").document(deterministicAppId)
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    if (snapshot.exists()) {
                        throw IllegalStateException("Duplicate application detected.")
                    }
                    transaction.set(docRef, entity.toDocumentMap())
                }.addOnSuccessListener {
                    Log.d(tag, "Application $deterministicAppId submitted successfully")
                    _errorMessage.value = null
                    _isLoading.value = false
                }.addOnFailureListener { e ->
                    Log.e(tag, "Failed to submit application: ${e.message}", e)
                    _errorMessage.value = if (e is IllegalStateException) {
                        "You have already applied to this position."
                    } else {
                        "Failed to submit application: ${e.localizedMessage}"
                    }
                    _isLoading.value = false
                    // Rollback optimistic update on failure
                    _applications.value = _applications.value.filter { it.id != deterministicAppId }
                }
            } catch (t: Throwable) {
                Log.e(tag, "Error submitting application", t)
                _errorMessage.value = t.localizedMessage
                _isLoading.value = false
                _applications.value = _applications.value.filter { it.id != deterministicAppId }
            }
        }
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
        val currentUser = auth.currentUser
        val creatorId = currentUser?.uid ?: "unauthenticated_caller"
        val jobId = "job-${UUID.randomUUID().toString().take(6)}"
        val companyId = "org-${company.lowercase().replace(" ", "-")}"

        val newJob = Job(
            id = jobId,
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
            isVerified = false, // Never self-verify
            isActivelyHiring = true,
            postedTime = "Just now",
            status = JobStatus.ACTIVE,
            applicantsCount = 0
        )

        val jobEntity = JobEntity.fromDomainModel(newJob, companyId = companyId, creatorId = creatorId)

        scope.launch {
            try {
                _isLoading.value = true
                firestore.collection("jobs")
                    .document(jobId)
                    .set(jobEntity.toDocumentMap())
                    .addOnSuccessListener {
                        Log.d(tag, "Job $jobId written successfully to Firestore")
                        _errorMessage.value = null
                        _isLoading.value = false
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Failed to write job $jobId: ${e.message}", e)
                        _errorMessage.value = "Failed to publish job: ${e.localizedMessage}"
                        _isLoading.value = false
                    }
            } catch (t: Throwable) {
                Log.e(tag, "Unexpected error writing job: ${t.message}", t)
                _errorMessage.value = t.localizedMessage
                _isLoading.value = false
            }
        }

        return newJob
    }

    override fun updateJobStatus(jobId: String, newStatus: JobStatus) {
        scope.launch {
            firestore.collection("jobs").document(jobId)
                .update("status", newStatus.name)
                .addOnFailureListener { e ->
                    Log.e(tag, "Failed to update job status: ${e.message}", e)
                    _errorMessage.value = e.localizedMessage
                }
        }
    }

    override fun updateApplicantStage(applicantId: String, stage: ApplicationStage) {
        val prevApplicants = _applicants.value
        val prevApplications = _applications.value

        _applicants.value = _applicants.value.map {
            if (it.id == applicantId) it.copy(stage = stage) else it
        }
        _applications.value = _applications.value.map { app ->
            if (app.id == applicantId) {
                app.copy(status = stage)
            } else app
        }

        scope.launch {
            firestore.collection("applications").document(applicantId)
                .update("status", stage.name)
                .addOnSuccessListener {
                    Log.d(tag, "Applicant $applicantId stage successfully updated to $stage")
                    _errorMessage.value = null
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Failed to update applicant stage: ${e.message}", e)
                    _errorMessage.value = "Failed to update stage: ${e.localizedMessage}"
                    _applicants.value = prevApplicants
                    _applications.value = prevApplications
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
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _errorMessage.value = "Authentication required to schedule interviews."
            return
        }

        if (date.isBlank() || time.isBlank() || interviewer.isBlank()) {
            _errorMessage.value = "Interview date, time, and interviewer are required."
            return
        }

        val applicant = _applicants.value.find {
            it.candidateName.equals(candidateName, ignoreCase = true) ||
            it.jobTitle.equals(candidateRole, ignoreCase = true)
        }
        val applicationId = applicant?.id ?: "app-${UUID.randomUUID().toString().take(6)}"
        val candidateId = if (applicationId.contains("_")) applicationId.substringAfterLast("_") else "cand-${applicationId}"
        val companyId = "org-${company.lowercase().replace(" ", "-")}"
        val deterministicInterviewId = "int_${applicationId}"

        // Check in-memory duplicate check
        if (_interviews.value.any { it.id == deterministicInterviewId && it.status == "Confirmed" }) {
            _errorMessage.value = "An interview is already confirmed for this application."
            return
        }

        val meetingUrl = "https://meet.google.com/tnx-interview-${UUID.randomUUID().toString().take(4)}"
        val newInt = InterviewItem(
            id = deterministicInterviewId,
            candidateName = candidateName,
            candidateRole = candidateRole,
            company = company,
            date = date,
            time = time,
            duration = "45 mins",
            interviewerName = interviewer,
            meetingUrl = meetingUrl,
            status = "Confirmed"
        )

        // Optimistic local update
        val prevInterviews = _interviews.value
        val prevApplications = _applications.value
        _interviews.value = listOf(newInt) + _interviews.value.filter { it.id != deterministicInterviewId }

        _applications.value = _applications.value.map { app ->
            if (app.id == applicationId || app.jobTitle.equals(candidateRole, ignoreCase = true)) {
                app.copy(
                    status = ApplicationStage.INTERVIEW_SCHEDULED,
                    interviewDate = date,
                    interviewTime = time,
                    interviewMeetingUrl = meetingUrl
                )
            } else app
        }

        val interviewEntity = InterviewEntity(
            id = deterministicInterviewId,
            applicationId = applicationId,
            jobId = applicant?.jobId ?: "",
            companyId = companyId,
            candidateId = candidateId,
            candidateName = candidateName,
            candidateRole = candidateRole,
            company = company,
            date = date,
            time = time,
            duration = "45 mins",
            interviewerName = interviewer,
            platform = "Google Meet",
            meetingUrl = meetingUrl,
            status = "Confirmed",
            scoreTechnical = 0,
            scoreCulture = 0,
            feedbackNotes = ""
        )

        scope.launch {
            try {
                val intRef = firestore.collection("interviews").document(deterministicInterviewId)
                val appRef = firestore.collection("applications").document(applicationId)

                firestore.runTransaction { transaction ->
                    val intSnapshot = transaction.get(intRef)
                    if (intSnapshot.exists() && intSnapshot.getString("status") == "Confirmed") {
                        throw IllegalStateException("Interview already scheduled for this application.")
                    }

                    val appSnapshot = transaction.get(appRef)
                    val authoritativeCandidateId = if (appSnapshot.exists()) {
                        appSnapshot.getString("candidateId") ?: candidateId
                    } else candidateId

                    val authoritativeJobId = if (appSnapshot.exists()) {
                        appSnapshot.getString("jobId") ?: (applicant?.jobId ?: "")
                    } else (applicant?.jobId ?: "")

                    val authoritativeCompanyId = if (appSnapshot.exists()) {
                        appSnapshot.getString("companyId") ?: companyId
                    } else companyId

                    val finalEntity = interviewEntity.copy(
                        candidateId = authoritativeCandidateId,
                        jobId = authoritativeJobId,
                        companyId = authoritativeCompanyId
                    )

                    transaction.set(intRef, finalEntity.toDocumentMap())

                    if (appSnapshot.exists()) {
                        transaction.update(
                            appRef,
                            mapOf(
                                "status" to ApplicationStage.INTERVIEW_SCHEDULED.name,
                                "interviewDate" to date,
                                "interviewTime" to time,
                                "interviewMeetingUrl" to meetingUrl
                            )
                        )
                    }
                }.addOnSuccessListener {
                    Log.d(tag, "Interview $deterministicInterviewId and application atomically scheduled")
                    _errorMessage.value = null
                }.addOnFailureListener { e ->
                    Log.e(tag, "Failed to schedule interview: ${e.message}", e)
                    _errorMessage.value = if (e is IllegalStateException) {
                        e.message
                    } else {
                        "Failed to schedule interview: ${e.localizedMessage}"
                    }
                    _interviews.value = prevInterviews
                    _applications.value = prevApplications
                }
            } catch (t: Throwable) {
                Log.e(tag, "Error in schedule transaction", t)
                _errorMessage.value = t.localizedMessage
                _interviews.value = prevInterviews
                _applications.value = prevApplications
            }
        }
    }

    override fun rescheduleInterview(interviewId: String, newDate: String, newTime: String) {
        if (newDate.isBlank() || newTime.isBlank()) {
            _errorMessage.value = "New interview date and time must be provided."
            return
        }

        val prevInterviews = _interviews.value
        val prevApplications = _applications.value

        _interviews.value = _interviews.value.map {
            if (it.id == interviewId) it.copy(date = newDate, time = newTime, status = "Rescheduled") else it
        }

        _applications.value = _applications.value.map { app ->
            if (interviewId.contains(app.id) || app.interviewMeetingUrl != null) {
                app.copy(interviewDate = newDate, interviewTime = newTime)
            } else app
        }

        scope.launch {
            try {
                val intRef = firestore.collection("interviews").document(interviewId)
                firestore.runTransaction { transaction ->
                    val intSnapshot = transaction.get(intRef)
                    if (!intSnapshot.exists()) {
                        throw NoSuchElementException("Interview not found.")
                    }

                    val appId = intSnapshot.getString("applicationId")
                    transaction.update(
                        intRef,
                        mapOf(
                            "date" to newDate,
                            "time" to newTime,
                            "status" to "Rescheduled"
                        )
                    )

                    if (!appId.isNullOrBlank()) {
                        val appRef = firestore.collection("applications").document(appId)
                        val appDoc = transaction.get(appRef)
                        if (appDoc.exists()) {
                            transaction.update(
                                appRef,
                                mapOf(
                                    "interviewDate" to newDate,
                                    "interviewTime" to newTime
                                )
                            )
                        }
                    }
                }.addOnSuccessListener {
                    Log.d(tag, "Interview $interviewId rescheduled successfully")
                    _errorMessage.value = null
                }.addOnFailureListener { e ->
                    Log.e(tag, "Failed to reschedule interview: ${e.message}", e)
                    _errorMessage.value = "Failed to reschedule: ${e.localizedMessage}"
                    _interviews.value = prevInterviews
                    _applications.value = prevApplications
                }
            } catch (t: Throwable) {
                _errorMessage.value = t.localizedMessage
                _interviews.value = prevInterviews
                _applications.value = prevApplications
            }
        }
    }

    override fun cancelInterview(interviewId: String) {
        val prevInterviews = _interviews.value
        val prevApplications = _applications.value

        _interviews.value = _interviews.value.map {
            if (it.id == interviewId) it.copy(status = "Cancelled") else it
        }

        _applications.value = _applications.value.map { app ->
            if (interviewId.contains(app.id)) {
                app.copy(
                    status = ApplicationStage.IN_REVIEW,
                    interviewDate = null,
                    interviewTime = null,
                    interviewMeetingUrl = null
                )
            } else app
        }

        scope.launch {
            try {
                val intRef = firestore.collection("interviews").document(interviewId)
                firestore.runTransaction { transaction ->
                    val intSnapshot = transaction.get(intRef)
                    if (!intSnapshot.exists()) {
                        throw NoSuchElementException("Interview not found.")
                    }

                    val appId = intSnapshot.getString("applicationId")
                    transaction.update(intRef, "status", "Cancelled")

                    if (!appId.isNullOrBlank()) {
                        val appRef = firestore.collection("applications").document(appId)
                        val appDoc = transaction.get(appRef)
                        if (appDoc.exists()) {
                            transaction.update(
                                appRef,
                                mapOf(
                                    "status" to ApplicationStage.IN_REVIEW.name,
                                    "interviewDate" to null,
                                    "interviewTime" to null,
                                    "interviewMeetingUrl" to null
                                )
                            )
                        }
                    }
                }.addOnSuccessListener {
                    Log.d(tag, "Interview $interviewId cancelled successfully")
                    _errorMessage.value = null
                }.addOnFailureListener { e ->
                    Log.e(tag, "Failed to cancel interview: ${e.message}", e)
                    _errorMessage.value = "Failed to cancel interview: ${e.localizedMessage}"
                    _interviews.value = prevInterviews
                    _applications.value = prevApplications
                }
            } catch (t: Throwable) {
                _errorMessage.value = t.localizedMessage
                _interviews.value = prevInterviews
                _applications.value = prevApplications
            }
        }
    }

    override fun updateInterviewScorecard(interviewId: String, score: Int, notes: String) {
        val prevInterviews = _interviews.value
        _interviews.value = _interviews.value.map {
            if (it.id == interviewId) {
                it.copy(
                    scoreTechnical = score,
                    scoreCulture = score,
                    feedbackNotes = notes
                )
            } else it
        }

        scope.launch {
            try {
                val batch = firestore.batch()
                val intRef = firestore.collection("interviews").document(interviewId)
                batch.update(
                    intRef,
                    mapOf(
                        "scoreTechnical" to score,
                        "scoreCulture" to score,
                        "feedbackNotes" to notes
                    )
                )

                // Persist to private scorecards subcollection for evaluation audit
                val evalRef = intRef.collection("scorecards").document("eval_main")
                batch.set(
                    evalRef,
                    mapOf(
                        "scoreTechnical" to score,
                        "scoreCulture" to score,
                        "notes" to notes,
                        "submittedBy" to (auth.currentUser?.uid ?: "interviewer"),
                        "updatedAt" to "Just now"
                    )
                )

                batch.commit()
                    .addOnSuccessListener {
                        Log.d(tag, "Scorecard for $interviewId saved successfully")
                        _errorMessage.value = null
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Failed to save scorecard: ${e.message}", e)
                        _errorMessage.value = "Failed to save scorecard: ${e.localizedMessage}"
                        _interviews.value = prevInterviews
                    }
            } catch (t: Throwable) {
                Log.e(tag, "Error saving scorecard", t)
                _errorMessage.value = t.localizedMessage
                _interviews.value = prevInterviews
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
        val status = if (approve) JobStatus.ACTIVE else JobStatus.CLOSED
        updateJobStatus(jobId, status)
    }

    override fun resolveReport(reportId: String, status: String) {
        _reportedItems.value = _reportedItems.value.map {
            if (it.id == reportId) it.copy(status = status) else it
        }
    }

    override fun toggleUserVerification(userId: String) {
        _users.value = _users.value.map {
            if (it.id == userId) {
                val newVerified = !it.isVerified
                it.copy(
                    isVerified = newVerified,
                    status = if (newVerified) "Verified" else "Active"
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
        val catId = "cat-${UUID.randomUUID().toString().take(4)}"
        val entity = CategoryEntity(id = catId, name = name, icon = icon, jobCount = "0 jobs")
        scope.launch {
            firestore.collection("taxonomy_categories")
                .document(catId)
                .set(entity.toDocumentMap())
                .addOnFailureListener { e ->
                    Log.e(tag, "Failed to add category $catId: ${e.message}", e)
                    _errorMessage.value = e.localizedMessage
                }
        }
    }

    override fun updateEmployerProfile(profile: EmployerProfile) {
        _employerProfile.value = profile
    }

    override fun updateSeekerProfile(profile: JobSeekerProfile) {
        _seekerProfile.value = profile
    }

    override fun applyReferralAttribution(referralCode: String): Boolean {
        val user = auth.currentUser
        if (user == null) {
            _errorMessage.value = "Authentication required to apply referral code."
            return false
        }
        val cleanCode = referralCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            _errorMessage.value = "Referral code cannot be blank."
            return false
        }
        if (cleanCode == _referralProfile.value.referralCode) {
            _errorMessage.value = "Self-referral is not permitted."
            return false
        }

        scope.launch {
            try {
                _isLoading.value = true
                val userRef = firestore.collection("users").document(user.uid)
                val referrerQuery = firestore.collection("users")
                    .whereEqualTo("referralCode", cleanCode)
                    .limit(1)
                    .get()
                    .await()

                if (referrerQuery.isEmpty) {
                    _errorMessage.value = "Invalid referral code: '$cleanCode' not found."
                    _isLoading.value = false
                    return@launch
                }

                val referrerDoc = referrerQuery.documents.first()
                val referrerUid = referrerDoc.id
                if (referrerUid == user.uid) {
                    _errorMessage.value = "You cannot use your own referral code."
                    _isLoading.value = false
                    return@launch
                }

                val refId = "ref_${referrerUid}_${user.uid}"
                val refDoc = firestore.collection("referrals").document(refId)

                firestore.runTransaction { transaction ->
                    val userSnapshot = transaction.get(userRef)
                    val existingReferredBy = userSnapshot.getString("referredByCode")
                    if (!existingReferredBy.isNullOrBlank()) {
                        throw IllegalStateException("Referral attribution is already established and cannot be changed.")
                    }

                    val refSnapshot = transaction.get(refDoc)
                    if (refSnapshot.exists()) {
                        throw IllegalStateException("You have already been attributed via a referral.")
                    }

                    transaction.update(
                        userRef,
                        mapOf(
                            "referredByCode" to cleanCode,
                            "referredByUid" to referrerUid
                        )
                    )

                    val referralEntity = ReferralEntity(
                        id = refId,
                        referrerUid = referrerUid,
                        referredUid = user.uid,
                        referredName = user.displayName.orEmpty().ifBlank { "Referred Member" },
                        referralCode = cleanCode,
                        status = ReferralStatus.REGISTERED.name,
                        rewardAmount = 0,
                        createdAt = "Just now"
                    )
                    transaction.set(refDoc, referralEntity.toDocumentMap())
                }.addOnSuccessListener {
                    Log.d(tag, "Referral attribution applied successfully")
                    _errorMessage.value = null
                    _isLoading.value = false
                }.addOnFailureListener { e ->
                    Log.e(tag, "Failed to apply referral code: ${e.message}", e)
                    _errorMessage.value = e.localizedMessage
                    _isLoading.value = false
                }
            } catch (t: Throwable) {
                Log.e(tag, "Error applying referral code", t)
                _errorMessage.value = t.localizedMessage
                _isLoading.value = false
            }
        }
        return true
    }

    override fun initiatePremiumPayment(): PremiumPaymentRecord {
        val user = auth.currentUser
        val uid = user?.uid ?: "unauthenticated_caller"
        val paymentId = "pay_${UUID.randomUUID().toString().take(8)}"
        val record = PremiumPaymentRecord(
            id = paymentId,
            userId = uid,
            userEmail = user?.email.orEmpty(),
            amount = 10,
            currency = "INR",
            status = PaymentStatus.PENDING_VERIFICATION,
            provider = "MOCK_GATEWAY",
            createdAt = "Just now"
        )

        scope.launch {
            try {
                val paymentEntity = PaymentEntity(
                    id = paymentId,
                    userId = uid,
                    userEmail = user?.email.orEmpty(),
                    amount = 10,
                    currency = "INR",
                    status = PaymentStatus.PENDING_VERIFICATION.name,
                    provider = "MOCK_GATEWAY",
                    createdAt = "Just now"
                )
                firestore.collection("premium_payments")
                    .document(paymentId)
                    .set(paymentEntity.toDocumentMap())
            } catch (e: Exception) {
                Log.e(tag, "Failed to initiate payment: ${e.message}", e)
            }
        }
        return record
    }

    override fun processVerifiedPayment(paymentId: String, providerRef: String): Boolean {
        val user = auth.currentUser
        if (user == null) {
            _errorMessage.value = "Authentication required for payment verification."
            return false
        }

        scope.launch {
            try {
                _isLoading.value = true
                val paymentRef = firestore.collection("premium_payments").document(paymentId)
                val userRef = firestore.collection("users").document(user.uid)

                firestore.runTransaction { transaction ->
                    val paymentDoc = transaction.get(paymentRef)
                    if (!paymentDoc.exists()) {
                        throw NoSuchElementException("Payment record $paymentId not found.")
                    }
                    val currentStatus = paymentDoc.getString("status")
                    if (currentStatus == PaymentStatus.VERIFIED.name) {
                        return@runTransaction
                    }

                    // 1. Mark payment verified
                    transaction.update(
                        paymentRef,
                        mapOf(
                            "status" to PaymentStatus.VERIFIED.name,
                            "providerTransactionRef" to providerRef,
                            "verifiedAt" to "Just now"
                        )
                    )

                    // 2. Grant premium status to payer
                    transaction.update(
                        userRef,
                        mapOf(
                            "isPremium" to true,
                            "premiumPurchasedAt" to "Just now"
                        )
                    )

                    // 3. Check referral attribution
                    val userDoc = transaction.get(userRef)
                    val referrerUid = userDoc.getString("referredByUid")
                    val referralCode = userDoc.getString("referredByCode")

                    if (!referrerUid.isNullOrBlank() && referrerUid != user.uid) {
                        val refId = "ref_${referrerUid}_${user.uid}"
                        val refDoc = firestore.collection("referrals").document(refId)
                        val refSnapshot = transaction.get(refDoc)

                        val isAlreadyRewarded = refSnapshot.exists() &&
                            refSnapshot.getString("status") == ReferralStatus.REWARD_CREDITED.name

                        if (!isAlreadyRewarded) {
                            transaction.set(
                                refDoc,
                                mapOf(
                                    "id" to refId,
                                    "referrerUid" to referrerUid,
                                    "referredUid" to user.uid,
                                    "referredName" to (user.displayName.orEmpty().ifBlank { "Referred Member" }),
                                    "referralCode" to (referralCode ?: ""),
                                    "status" to ReferralStatus.REWARD_CREDITED.name,
                                    "rewardAmount" to 5,
                                    "qualifyingPaymentRef" to paymentId,
                                    "createdAt" to (refSnapshot.getString("createdAt") ?: "Just now"),
                                    "rewardedAt" to "Just now"
                                )
                            )

                            val referrerRef = firestore.collection("users").document(referrerUid)
                            val referrerDoc = transaction.get(referrerRef)
                            if (referrerDoc.exists()) {
                                val curBal = (referrerDoc.getLong("availableReferralBalance") ?: 0L).toInt()
                                val curEarned = (referrerDoc.getLong("totalEarnedRewards") ?: 0L).toInt()
                                val curSuccessful = (referrerDoc.getLong("successfulReferralsCount") ?: 0L).toInt()

                                transaction.update(
                                    referrerRef,
                                    mapOf(
                                        "availableReferralBalance" to (curBal + 5),
                                        "totalEarnedRewards" to (curEarned + 5),
                                        "successfulReferralsCount" to (curSuccessful + 1)
                                    )
                                )
                            }

                            val ledgerRef = firestore.collection("referral_ledger").document("ledger_reward_${paymentId}")
                            transaction.set(
                                ledgerRef,
                                mapOf(
                                    "id" to "ledger_reward_${paymentId}",
                                    "userId" to referrerUid,
                                    "type" to LedgerType.REFERRAL_REWARD.name,
                                    "amount" to 5,
                                    "referenceId" to paymentId,
                                    "notes" to "Referral reward: ₹5 for verified premium payment $providerRef",
                                    "timestamp" to "Just now"
                                )
                            )
                        }
                    }
                }.addOnSuccessListener {
                    Log.d(tag, "Payment $paymentId successfully verified and rewards processed")
                    _errorMessage.value = null
                    _isLoading.value = false
                }.addOnFailureListener { e ->
                    Log.e(tag, "Failed to verify payment: ${e.message}", e)
                    _errorMessage.value = "Payment verification error: ${e.localizedMessage}"
                    _isLoading.value = false
                }
            } catch (t: Throwable) {
                _errorMessage.value = t.localizedMessage
                _isLoading.value = false
            }
        }
        return true
    }

    override fun requestWithdrawal(amount: Int, payoutAccountDetails: String): Boolean {
        val user = auth.currentUser
        if (user == null) {
            _errorMessage.value = "Authentication required to request withdrawal."
            return false
        }
        if (amount < 50) {
            _errorMessage.value = "Minimum withdrawal amount is ₹50."
            return false
        }
        if (payoutAccountDetails.isBlank()) {
            _errorMessage.value = "Please provide valid UPI ID or bank account details."
            return false
        }

        val reqId = "wdr_${UUID.randomUUID().toString().take(8)}"

        scope.launch {
            try {
                _isLoading.value = true
                val userRef = firestore.collection("users").document(user.uid)
                val wdrRef = firestore.collection("withdrawal_requests").document(reqId)
                val ledgerRef = firestore.collection("referral_ledger").document("ledger_reserve_${reqId}")

                firestore.runTransaction { transaction ->
                    val userDoc = transaction.get(userRef)
                    if (!userDoc.exists()) {
                        throw NoSuchElementException("User account not found.")
                    }

                    val available = (userDoc.getLong("availableReferralBalance") ?: 0L).toInt()
                    val reserved = (userDoc.getLong("reservedWithdrawalBalance") ?: 0L).toInt()
                    val successful = (userDoc.getLong("successfulReferralsCount") ?: 0L).toInt()

                    val requestedRupees = amount

                    if (successful < 10) {
                        throw IllegalStateException("Redemption blocked: Requires at least 10 successful referrals (currently $successful).")
                    }
                    if (available < 50) {
                        throw IllegalStateException("Redemption blocked: Requires at least ₹50 available referral balance (currently ₹$available).")
                    }
                    if (requestedRupees < 50) {
                        throw IllegalStateException("Minimum withdrawal amount is ₹50.")
                    }
                    if (requestedRupees > available) {
                        throw IllegalStateException("Insufficient balance. Available: ₹$available, Requested: ₹$requestedRupees")
                    }

                    // 1. Atomically reserve balance
                    transaction.update(
                        userRef,
                        mapOf(
                            "availableReferralBalance" to (available - requestedRupees),
                            "reservedWithdrawalBalance" to (reserved + requestedRupees)
                        )
                    )

                    // 2. Create withdrawal request
                    val method = if (payoutAccountDetails.contains("@")) "UPI" else "Bank Transfer (NEFT)"
                    val wdrEntity = WithdrawalEntity(
                        id = reqId,
                        userId = user.uid,
                        userName = user.displayName.orEmpty().ifBlank { "भर्ती-App Member" },
                        userEmail = user.email.orEmpty(),
                        amount = amount,
                        payoutMethod = method,
                        payoutAccountDetails = payoutAccountDetails,
                        status = WithdrawalStatus.REQUESTED.name,
                        requestedAt = "Just now"
                    )
                    transaction.set(wdrRef, wdrEntity.toDocumentMap())

                    // 3. Create reservation ledger entry
                    transaction.set(
                        ledgerRef,
                        mapOf(
                            "id" to "ledger_reserve_${reqId}",
                            "userId" to user.uid,
                            "type" to LedgerType.WITHDRAWAL_REQUEST_RESERVED.name,
                            "amount" to -amount,
                            "referenceId" to reqId,
                            "notes" to "Reserved ₹$amount for withdrawal to $payoutAccountDetails",
                            "timestamp" to "Just now"
                        )
                    )
                }.addOnSuccessListener {
                    Log.d(tag, "Withdrawal $reqId requested and ₹$amount reserved atomically")
                    _errorMessage.value = null
                    _isLoading.value = false
                }.addOnFailureListener { e ->
                    Log.e(tag, "Withdrawal request failed: ${e.message}", e)
                    _errorMessage.value = e.localizedMessage
                    _isLoading.value = false
                }
            } catch (t: Throwable) {
                _errorMessage.value = t.localizedMessage
                _isLoading.value = false
            }
        }
        return true
    }

    override fun processWithdrawalDecision(requestId: String, approve: Boolean, adminNotes: String): Boolean {
        val user = auth.currentUser
        if (user == null) {
            _errorMessage.value = "Authentication required for admin processing."
            return false
        }

        scope.launch {
            try {
                _isLoading.value = true
                val wdrRef = firestore.collection("withdrawal_requests").document(requestId)

                firestore.runTransaction { transaction ->
                    val wdrDoc = transaction.get(wdrRef)
                    if (!wdrDoc.exists()) {
                        throw NoSuchElementException("Withdrawal request $requestId not found.")
                    }
                    val currentStatus = wdrDoc.getString("status")
                    if (currentStatus != WithdrawalStatus.REQUESTED.name) {
                        throw IllegalStateException("Withdrawal request has already been processed (Status: $currentStatus).")
                    }

                    val requesterUid = wdrDoc.getString("userId") ?: throw IllegalStateException("Missing requester UID")
                    val amount = (wdrDoc.getLong("amount") ?: 0L).toInt()
                    val userRef = firestore.collection("users").document(requesterUid)
                    val userDoc = transaction.get(userRef)

                    val available = (userDoc.getLong("availableReferralBalance") ?: 0L).toInt()
                    val reserved = (userDoc.getLong("reservedWithdrawalBalance") ?: 0L).toInt()

                    if (approve) {
                        val newReserved = (reserved - amount).coerceAtLeast(0)
                        transaction.update(userRef, "reservedWithdrawalBalance", newReserved)

                        transaction.update(
                            wdrRef,
                            mapOf(
                                "status" to WithdrawalStatus.COMPLETED.name,
                                "adminNotes" to adminNotes.ifBlank { "Approved & manually disbursed" },
                                "processedAt" to "Just now",
                                "processedBy" to user.uid
                            )
                        )

                        val ledgerRef = firestore.collection("referral_ledger").document("ledger_payout_${requestId}")
                        transaction.set(
                            ledgerRef,
                            mapOf(
                                "id" to "ledger_payout_${requestId}",
                                "userId" to requesterUid,
                                "type" to LedgerType.WITHDRAWAL_APPROVED_PAYOUT.name,
                                "amount" to -amount,
                                "referenceId" to requestId,
                                "notes" to "Disbursed payout ₹$amount: $adminNotes",
                                "timestamp" to "Just now"
                            )
                        )
                    } else {
                        val newReserved = (reserved - amount).coerceAtLeast(0)
                        val newAvailable = available + amount

                        transaction.update(
                            userRef,
                            mapOf(
                                "availableReferralBalance" to newAvailable,
                                "reservedWithdrawalBalance" to newReserved
                            )
                        )

                        transaction.update(
                            wdrRef,
                            mapOf(
                                "status" to WithdrawalStatus.REJECTED.name,
                                "adminNotes" to adminNotes.ifBlank { "Rejected by administrator" },
                                "processedAt" to "Just now",
                                "processedBy" to user.uid
                            )
                        )

                        val ledgerRef = firestore.collection("referral_ledger").document("ledger_restore_${requestId}")
                        transaction.set(
                            ledgerRef,
                            mapOf(
                                "id" to "ledger_restore_${requestId}",
                                "userId" to requesterUid,
                                "type" to LedgerType.WITHDRAWAL_REJECTED_RESTORED.name,
                                "amount" to amount,
                                "referenceId" to requestId,
                                "notes" to "Restored ₹$amount to available balance: $adminNotes",
                                "timestamp" to "Just now"
                            )
                        )
                    }
                }.addOnSuccessListener {
                    Log.d(tag, "Withdrawal decision processed successfully (Approve: $approve)")
                    _errorMessage.value = null
                    _isLoading.value = false
                }.addOnFailureListener { e ->
                    Log.e(tag, "Failed to process withdrawal decision: ${e.message}", e)
                    _errorMessage.value = e.localizedMessage
                    _isLoading.value = false
                }
            } catch (t: Throwable) {
                _errorMessage.value = t.localizedMessage
                _isLoading.value = false
            }
        }
        return true
    }
}
