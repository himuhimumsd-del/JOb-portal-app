package com.example.jobportal.data.model

enum class UserRole(val label: String, val subtitle: String) {
    JOB_SEEKER("Job Seeker", "Talent Discovery & Applications"),
    EMPLOYER("Employer Suite", "Talent Pipeline & Recruiting"),
    ADMIN("Admin Console", "Platform Operations & Governance")
}

enum class WorkplaceType(val label: String) {
    REMOTE("Remote"),
    HYBRID("Hybrid"),
    ON_SITE("On-site")
}

enum class EmploymentType(val label: String) {
    FULL_TIME("Full-time"),
    CONTRACT("Contract"),
    PART_TIME("Part-time"),
    INTERNSHIP("Internship")
}

enum class JobStatus(val label: String) {
    ACTIVE("Active"),
    UNDER_REVIEW("Under Review"),
    DRAFT("Draft"),
    CLOSED("Closed")
}

enum class ApplicationStage(val label: String) {
    APPLIED("Applied"),
    IN_REVIEW("In Review"),
    SHORTLISTED("Shortlisted"),
    INTERVIEW_SCHEDULED("Interview Scheduled"),
    OFFER("Offer"),
    REJECTED("Not Selected")
}

data class Job(
    val id: String,
    val title: String,
    val company: String,
    val location: String,
    val workplaceType: WorkplaceType,
    val employmentType: EmploymentType,
    val salaryRange: String,
    val minSalary: Int,
    val category: String,
    val tags: List<String>,
    val description: String,
    val responsibilities: List<String>,
    val requirements: List<String>,
    val benefits: List<String>,
    val isVerified: Boolean = true,
    val isActivelyHiring: Boolean = true,
    val postedTime: String,
    val status: JobStatus = JobStatus.ACTIVE,
    val applicantsCount: Int = 0
)

data class Application(
    val id: String,
    val jobId: String,
    val jobTitle: String,
    val company: String,
    val appliedDate: String,
    val status: ApplicationStage,
    val resumeVersion: String,
    val interviewDate: String? = null,
    val interviewTime: String? = null,
    val interviewMeetingUrl: String? = null,
    val prepNotes: String? = null
)

data class Applicant(
    val id: String,
    val jobId: String,
    val jobTitle: String,
    val candidateName: String,
    val headline: String,
    val email: String,
    val phone: String,
    val location: String,
    val atsScore: Int,
    val experienceYears: Int,
    val stage: ApplicationStage,
    val appliedTime: String,
    val resumeFileName: String,
    val keySkills: List<String>
)

data class InterviewItem(
    val id: String,
    val candidateName: String,
    val candidateRole: String,
    val company: String,
    val date: String,
    val time: String,
    val duration: String = "45 mins",
    val interviewerName: String,
    val platform: String = "Google Meet",
    val meetingUrl: String = "https://meet.google.com/tnx-exec-hire",
    val status: String = "Confirmed",
    val scoreTechnical: Int = 0,
    val scoreCulture: Int = 0,
    val feedbackNotes: String = ""
)

data class ResumeItem(
    val id: String,
    val fileName: String,
    val uploadDate: String,
    val isDefault: Boolean,
    val atsScore: Int
)

data class JobSeekerProfile(
    val fullName: String,
    val title: String,
    val email: String,
    val phone: String,
    val location: String,
    val bio: String,
    val completionPercent: Int = 92,
    val skills: List<String>,
    val resumes: List<ResumeItem>
)

data class EmployerProfile(
    val companyName: String,
    val tagline: String,
    val industry: String,
    val website: String,
    val location: String,
    val isVerified: Boolean = true,
    val employeeCount: String,
    val openRolesCount: Int,
    val bio: String,
    val perks: List<String>
)

data class JobAlert(
    val id: String,
    val keyword: String,
    val category: String,
    val minSalary: String,
    val frequency: String,
    val isActive: Boolean = true
)

data class CategoryItem(
    val id: String,
    val name: String,
    val jobCount: String,
    val icon: String
)

data class ReportedItem(
    val id: String,
    val type: String,
    val title: String,
    val reportedBy: String,
    val reason: String,
    val timestamp: String,
    val status: String = "Pending Audit"
)

data class AdminVitals(
    val jobSeekersCount: String = "124,580",
    val verifiedOrgsCount: String = "4,820",
    val activeJobsCount: String = "18,940",
    val pendingAuditsCount: Int = 38,
    val systemUptime: String = "99.98%"
)

data class PlatformUser(
    val id: String,
    val name: String,
    val email: String,
    val roleType: String,
    val status: String,
    val isVerified: Boolean
)

enum class ReferralStatus(val label: String) {
    REGISTERED("Registered"),
    PREMIUM_PENDING("Premium Pending"),
    REWARD_CREDITED("Reward Credited"),
    INELIGIBLE("Ineligible")
}

data class ReferralItem(
    val id: String,
    val referrerUid: String,
    val referredUid: String,
    val referredName: String,
    val referralCode: String,
    val status: ReferralStatus,
    val rewardAmount: Int = 0,
    val qualifyingPaymentRef: String? = null,
    val createdAt: String,
    val rewardedAt: String? = null
)

enum class LedgerType(val label: String) {
    REFERRAL_REWARD("Referral Reward"),
    WITHDRAWAL_REQUEST_RESERVED("Withdrawal Reserved"),
    WITHDRAWAL_APPROVED_PAYOUT("Withdrawal Payout"),
    WITHDRAWAL_REJECTED_RESTORED("Withdrawal Restored"),
    PREMIUM_PAYMENT("Premium Subscription"),
    REVERSAL_CHARGEBACK("Reward Reversal")
}

data class LedgerEntry(
    val id: String,
    val userId: String,
    val type: LedgerType,
    val amount: Int,
    val referenceId: String,
    val notes: String,
    val timestamp: String
)

enum class WithdrawalStatus(val label: String) {
    REQUESTED("Pending Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    COMPLETED("Paid")
}

data class WithdrawalRequest(
    val id: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val amount: Int,
    val payoutMethod: String = "UPI",
    val payoutAccountDetails: String,
    val status: WithdrawalStatus = WithdrawalStatus.REQUESTED,
    val adminNotes: String? = null,
    val requestedAt: String,
    val processedAt: String? = null,
    val processedBy: String? = null
)

enum class PaymentStatus(val label: String) {
    INITIATED("Initiated"),
    PENDING_VERIFICATION("Pending Verification"),
    VERIFIED("Verified"),
    FAILED("Failed"),
    REFUNDED("Refunded")
}

data class PremiumMembership(
    val userId: String,
    val isPremium: Boolean = false,
    val amountPaise: Int = 1000, // ₹10
    val currency: String = "INR",
    val paymentRef: String? = null,
    val purchasedAt: String? = null
)

data class ReferralAttribution(
    val referredUid: String,
    val referrerUid: String,
    val referralCode: String,
    val attributedAt: String
)

data class ReferralReward(
    val id: String,
    val referrerUid: String,
    val referredUid: String,
    val amountPaise: Int = 500, // ₹5
    val status: ReferralStatus = ReferralStatus.REWARD_CREDITED,
    val qualifyingPaymentRef: String? = null,
    val createdAt: String,
    val rewardedAt: String? = null
)

data class PremiumPaymentRecord(
    val id: String,
    val userId: String,
    val userEmail: String,
    val amountPaise: Int = 1000, // ₹10 = 1000 paise
    val amount: Int = 10, // ₹10 (rupees convenience)
    val currency: String = "INR",
    val status: PaymentStatus = PaymentStatus.INITIATED,
    val provider: String = "MOCK_GATEWAY",
    val providerTransactionRef: String? = null,
    val verifiedAt: String? = null,
    val createdAt: String
)

data class ReferralProfile(
    val referralCode: String,
    val shareableLink: String,
    val totalInvitedCount: Int = 0,
    val registeredCount: Int = 0,
    val pendingPaymentCount: Int = 0,
    val successfulReferralsCount: Int = 0,
    val totalRewardsEarnedPaise: Int = 0,
    val availableBalancePaise: Int = 0,
    val reservedBalancePaise: Int = 0,
    val totalRewardsEarned: Int = 0, // ₹ rupees convenience
    val availableBalance: Int = 0, // ₹ rupees convenience
    val reservedBalance: Int = 0, // ₹ rupees convenience
    val isEligibleForWithdrawal: Boolean = false,
    val isPremium: Boolean = false
) {
    // Both conditions required by business rule:
    // 1. At least 10 qualifying successful referrals completed
    // 2. Available referral reward balance is at least ₹50 (5000 paise)
    val meetsReferralsRequirement: Boolean
        get() = successfulReferralsCount >= 10

    val meetsBalanceRequirement: Boolean
        get() = availableBalance >= 50 || availableBalancePaise >= 5000

    val meetsBothRedemptionRequirements: Boolean
        get() = meetsReferralsRequirement && meetsBalanceRequirement

    fun getUnmetRequirementDescription(): String? {
        return when {
            !meetsReferralsRequirement && !meetsBalanceRequirement ->
                "Requires at least 10 successful referrals (currently $successfulReferralsCount) AND ₹50 available balance (currently ₹${availableBalance.coerceAtLeast(availableBalancePaise / 100)})."
            !meetsReferralsRequirement ->
                "Requires at least 10 successful referrals (currently $successfulReferralsCount / 10 completed)."
            !meetsBalanceRequirement ->
                "Requires at least ₹50 available referral balance (currently ₹${availableBalance.coerceAtLeast(availableBalancePaise / 100)})."
            else -> null
        }
    }
}

