package com.example.jobportal.data.model.firestore

import com.example.jobportal.data.model.*

/**
 * Typed Firestore Data Transfer Objects (DTOs) with serialization and deserialization
 * adhering to the TalentNexus security blueprint.
 */

data class CategoryEntity(
    val id: String = "",
    val name: String = "",
    val jobCount: String = "0 jobs",
    val icon: String = "work",
    val createdAt: String = ""
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "jobCount" to jobCount,
        "icon" to icon,
        "createdAt" to createdAt
    )

    fun toDomainModel(): CategoryItem = CategoryItem(
        id = id,
        name = name,
        jobCount = jobCount,
        icon = icon
    )

    companion object {
        fun fromDomainModel(item: CategoryItem): CategoryEntity = CategoryEntity(
            id = item.id,
            name = item.name,
            jobCount = item.jobCount,
            icon = item.icon
        )

        fun fromDocument(id: String, map: Map<String, Any?>): CategoryEntity = CategoryEntity(
            id = id,
            name = map["name"] as? String ?: "",
            jobCount = map["jobCount"] as? String ?: "0 jobs",
            icon = map["icon"] as? String ?: "work",
            createdAt = map["createdAt"] as? String ?: ""
        )
    }
}

data class CompanyEntity(
    val id: String = "",
    val name: String = "",
    val tagline: String = "",
    val industry: String = "",
    val website: String = "",
    val location: String = "",
    val isVerified: Boolean = false,
    val employeeCount: String = "10-50 employees",
    val openRolesCount: Int = 0,
    val bio: String = "",
    val perks: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: String = ""
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "tagline" to tagline,
        "industry" to industry,
        "website" to website,
        "location" to location,
        "isVerified" to isVerified,
        "employeeCount" to employeeCount,
        "openRolesCount" to openRolesCount,
        "bio" to bio,
        "perks" to perks,
        "createdBy" to createdBy,
        "createdAt" to createdAt
    )

    fun toDomainModel(): EmployerProfile = EmployerProfile(
        companyName = name,
        tagline = tagline,
        industry = industry,
        website = website,
        location = location,
        isVerified = isVerified,
        employeeCount = employeeCount,
        openRolesCount = openRolesCount,
        bio = bio,
        perks = perks
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromDocument(id: String, map: Map<String, Any?>): CompanyEntity = CompanyEntity(
            id = id,
            name = map["name"] as? String ?: "",
            tagline = map["tagline"] as? String ?: "",
            industry = map["industry"] as? String ?: "",
            website = map["website"] as? String ?: "",
            location = map["location"] as? String ?: "",
            isVerified = map["isVerified"] as? Boolean ?: false,
            employeeCount = map["employeeCount"] as? String ?: "10-50 employees",
            openRolesCount = (map["openRolesCount"] as? Number)?.toInt() ?: 0,
            bio = map["bio"] as? String ?: "",
            perks = (map["perks"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            createdBy = map["createdBy"] as? String ?: "",
            createdAt = map["createdAt"] as? String ?: ""
        )
    }
}

data class CompanyMembershipEntity(
    val id: String = "",
    val companyId: String = "",
    val userId: String = "",
    val role: String = "member", // "owner", "admin", "recruiter", "member"
    val joinedAt: String = ""
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "companyId" to companyId,
        "userId" to userId,
        "role" to role,
        "joinedAt" to joinedAt
    )

    companion object {
        fun fromDocument(id: String, map: Map<String, Any?>): CompanyMembershipEntity = CompanyMembershipEntity(
            id = id,
            companyId = map["companyId"] as? String ?: "",
            userId = map["userId"] as? String ?: "",
            role = map["role"] as? String ?: "member",
            joinedAt = map["joinedAt"] as? String ?: ""
        )
    }
}

data class JobEntity(
    val id: String = "",
    val title: String = "",
    val company: String = "",
    val companyId: String = "",
    val creatorId: String = "",
    val location: String = "",
    val workplaceType: String = WorkplaceType.REMOTE.name,
    val employmentType: String = EmploymentType.FULL_TIME.name,
    val salaryRange: String = "",
    val minSalary: Int = 0,
    val maxSalary: Int = 0,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val description: String = "",
    val responsibilities: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val isActivelyHiring: Boolean = true,
    val postedTime: String = "Just now",
    val status: String = JobStatus.ACTIVE.name,
    val applicantsCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "company" to company,
        "companyId" to companyId,
        "creatorId" to creatorId,
        "location" to location,
        "workplaceType" to workplaceType,
        "employmentType" to employmentType,
        "salaryRange" to salaryRange,
        "minSalary" to minSalary,
        "maxSalary" to maxSalary,
        "category" to category,
        "tags" to tags,
        "description" to description,
        "responsibilities" to responsibilities,
        "requirements" to requirements,
        "benefits" to benefits,
        "isVerified" to isVerified,
        "isActivelyHiring" to isActivelyHiring,
        "postedTime" to postedTime,
        "status" to status,
        "applicantsCount" to applicantsCount,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    fun toDomainModel(): Job {
        val wp = try { WorkplaceType.valueOf(workplaceType) } catch (e: Exception) { WorkplaceType.REMOTE }
        val emp = try { EmploymentType.valueOf(employmentType) } catch (e: Exception) { EmploymentType.FULL_TIME }
        val st = try { JobStatus.valueOf(status) } catch (e: Exception) { JobStatus.ACTIVE }
        return Job(
            id = id,
            title = title,
            company = company,
            location = location,
            workplaceType = wp,
            employmentType = emp,
            salaryRange = salaryRange,
            minSalary = minSalary,
            category = category,
            tags = tags,
            description = description,
            responsibilities = responsibilities,
            requirements = requirements,
            benefits = benefits,
            isVerified = isVerified,
            isActivelyHiring = isActivelyHiring,
            postedTime = postedTime,
            status = st,
            applicantsCount = applicantsCount
        )
    }

    companion object {
        fun fromDomainModel(job: Job, companyId: String = "org-default", creatorId: String = ""): JobEntity = JobEntity(
            id = job.id,
            title = job.title,
            company = job.company,
            companyId = companyId,
            creatorId = creatorId,
            location = job.location,
            workplaceType = job.workplaceType.name,
            employmentType = job.employmentType.name,
            salaryRange = job.salaryRange,
            minSalary = job.minSalary,
            maxSalary = job.minSalary + 50000,
            category = job.category,
            tags = job.tags,
            description = job.description,
            responsibilities = job.responsibilities,
            requirements = job.requirements,
            benefits = job.benefits,
            isVerified = job.isVerified,
            isActivelyHiring = job.isActivelyHiring,
            postedTime = job.postedTime,
            status = job.status.name,
            applicantsCount = job.applicantsCount
        )

        @Suppress("UNCHECKED_CAST")
        fun fromDocument(id: String, map: Map<String, Any?>): JobEntity = JobEntity(
            id = id,
            title = map["title"] as? String ?: "",
            company = map["company"] as? String ?: "",
            companyId = map["companyId"] as? String ?: "",
            creatorId = map["creatorId"] as? String ?: "",
            location = map["location"] as? String ?: "",
            workplaceType = map["workplaceType"] as? String ?: WorkplaceType.REMOTE.name,
            employmentType = map["employmentType"] as? String ?: EmploymentType.FULL_TIME.name,
            salaryRange = map["salaryRange"] as? String ?: "",
            minSalary = (map["minSalary"] as? Number)?.toInt() ?: 0,
            maxSalary = (map["maxSalary"] as? Number)?.toInt() ?: 0,
            category = map["category"] as? String ?: "",
            tags = (map["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            description = map["description"] as? String ?: "",
            responsibilities = (map["responsibilities"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            requirements = (map["requirements"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            benefits = (map["benefits"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            isVerified = map["isVerified"] as? Boolean ?: false,
            isActivelyHiring = map["isActivelyHiring"] as? Boolean ?: true,
            postedTime = map["postedTime"] as? String ?: "Just now",
            status = map["status"] as? String ?: JobStatus.ACTIVE.name,
            applicantsCount = (map["applicantsCount"] as? Number)?.toInt() ?: 0,
            createdAt = map["createdAt"] as? String ?: "",
            updatedAt = map["updatedAt"] as? String ?: ""
        )
    }
}

data class UserEntity(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val bio: String = "",
    val title: String = "",
    val completionPercent: Int = 50,
    val skills: List<String> = emptyList(),
    val role: String = UserRole.JOB_SEEKER.name,
    val isAdmin: Boolean = false,
    val isVerified: Boolean = false,
    val referralCode: String = "",
    val referredByCode: String = "",
    val referredByUid: String = "",
    val availableReferralBalance: Int = 0,
    val reservedWithdrawalBalance: Int = 0,
    val totalEarnedRewards: Int = 0,
    val successfulReferralsCount: Int = 0,
    val isPremium: Boolean = false,
    val premiumPurchasedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "fullName" to fullName,
        "email" to email,
        "phone" to phone,
        "location" to location,
        "bio" to bio,
        "title" to title,
        "completionPercent" to completionPercent,
        "skills" to skills,
        "role" to role,
        "isAdmin" to isAdmin,
        "isVerified" to isVerified,
        "referralCode" to referralCode,
        "referredByCode" to referredByCode,
        "referredByUid" to referredByUid,
        "availableReferralBalance" to availableReferralBalance,
        "reservedWithdrawalBalance" to reservedWithdrawalBalance,
        "totalEarnedRewards" to totalEarnedRewards,
        "successfulReferralsCount" to successfulReferralsCount,
        "isPremium" to isPremium,
        "premiumPurchasedAt" to premiumPurchasedAt,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    fun toSeekerProfile(resumes: List<ResumeItem> = emptyList()): JobSeekerProfile = JobSeekerProfile(
        fullName = fullName,
        title = title,
        email = email,
        phone = phone,
        location = location,
        bio = bio,
        completionPercent = completionPercent,
        skills = skills,
        resumes = resumes
    )

    fun toPlatformUser(): PlatformUser = PlatformUser(
        id = id,
        name = fullName,
        email = email,
        roleType = if (role == UserRole.EMPLOYER.name) "Employer Org" else "Job Seeker",
        status = if (isVerified) "Verified" else "Active",
        isVerified = isVerified
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromDocument(id: String, map: Map<String, Any?>): UserEntity = UserEntity(
            id = id,
            fullName = map["fullName"] as? String ?: "",
            email = map["email"] as? String ?: "",
            phone = map["phone"] as? String ?: "",
            location = map["location"] as? String ?: "",
            bio = map["bio"] as? String ?: "",
            title = map["title"] as? String ?: "",
            completionPercent = (map["completionPercent"] as? Number)?.toInt() ?: 50,
            skills = (map["skills"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            role = map["role"] as? String ?: UserRole.JOB_SEEKER.name,
            isAdmin = map["isAdmin"] as? Boolean ?: false,
            isVerified = map["isVerified"] as? Boolean ?: false,
            referralCode = map["referralCode"] as? String ?: "",
            referredByCode = map["referredByCode"] as? String ?: "",
            referredByUid = map["referredByUid"] as? String ?: "",
            availableReferralBalance = (map["availableReferralBalance"] as? Number)?.toInt() ?: 0,
            reservedWithdrawalBalance = (map["reservedWithdrawalBalance"] as? Number)?.toInt() ?: 0,
            totalEarnedRewards = (map["totalEarnedRewards"] as? Number)?.toInt() ?: 0,
            successfulReferralsCount = (map["successfulReferralsCount"] as? Number)?.toInt() ?: 0,
            isPremium = map["isPremium"] as? Boolean ?: false,
            premiumPurchasedAt = map["premiumPurchasedAt"] as? String,
            createdAt = map["createdAt"] as? String ?: "",
            updatedAt = map["updatedAt"] as? String ?: ""
        )
    }
}

data class ApplicationEntity(
    val id: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val companyId: String = "",
    val candidateId: String = "",
    val candidateName: String = "",
    val candidateEmail: String = "",
    val candidateHeadline: String = "",
    val candidatePhone: String = "",
    val candidateLocation: String = "",
    val keySkills: List<String> = emptyList(),
    val atsScore: Int = 0,
    val experienceYears: Int = 3,
    val appliedDate: String = "Just now",
    val status: String = ApplicationStage.APPLIED.name,
    val resumeVersion: String = "Default Resume",
    val interviewDate: String? = null,
    val interviewTime: String? = null,
    val interviewMeetingUrl: String? = null,
    val prepNotes: String? = null
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "jobId" to jobId,
        "jobTitle" to jobTitle,
        "company" to company,
        "companyId" to companyId,
        "candidateId" to candidateId,
        "candidateName" to candidateName,
        "candidateEmail" to candidateEmail,
        "candidateHeadline" to candidateHeadline,
        "candidatePhone" to candidatePhone,
        "candidateLocation" to candidateLocation,
        "keySkills" to keySkills,
        "atsScore" to atsScore,
        "experienceYears" to experienceYears,
        "appliedDate" to appliedDate,
        "status" to status,
        "resumeVersion" to resumeVersion,
        "interviewDate" to interviewDate,
        "interviewTime" to interviewTime,
        "interviewMeetingUrl" to interviewMeetingUrl,
        "prepNotes" to prepNotes
    )

    fun toDomainModel(): Application {
        val stage = try { ApplicationStage.valueOf(status) } catch (e: Exception) { ApplicationStage.APPLIED }
        return Application(
            id = id,
            jobId = jobId,
            jobTitle = jobTitle,
            company = company,
            appliedDate = appliedDate,
            status = stage,
            resumeVersion = resumeVersion,
            interviewDate = interviewDate,
            interviewTime = interviewTime,
            interviewMeetingUrl = interviewMeetingUrl,
            prepNotes = prepNotes
        )
    }

    fun toApplicant(): Applicant {
        val stage = try { ApplicationStage.valueOf(status) } catch (e: Exception) { ApplicationStage.APPLIED }
        return Applicant(
            id = id,
            jobId = jobId,
            jobTitle = jobTitle,
            candidateName = candidateName.ifEmpty { "Candidate" },
            headline = candidateHeadline.ifEmpty { "भर्ती-App Candidate" },
            email = candidateEmail,
            phone = candidatePhone,
            location = candidateLocation.ifEmpty { "Remote / US" },
            atsScore = atsScore,
            experienceYears = experienceYears,
            stage = stage,
            appliedTime = appliedDate,
            resumeFileName = resumeVersion,
            keySkills = keySkills.ifEmpty { listOf("Engineering", "Problem Solving") }
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromDocument(id: String, map: Map<String, Any?>): ApplicationEntity = ApplicationEntity(
            id = id,
            jobId = map["jobId"] as? String ?: "",
            jobTitle = map["jobTitle"] as? String ?: "",
            company = map["company"] as? String ?: "",
            companyId = map["companyId"] as? String ?: "",
            candidateId = map["candidateId"] as? String ?: "",
            candidateName = map["candidateName"] as? String ?: "",
            candidateEmail = map["candidateEmail"] as? String ?: "",
            candidateHeadline = map["candidateHeadline"] as? String ?: "",
            candidatePhone = map["candidatePhone"] as? String ?: "",
            candidateLocation = map["candidateLocation"] as? String ?: "",
            keySkills = (map["keySkills"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            atsScore = (map["atsScore"] as? Number)?.toInt() ?: 0,
            experienceYears = (map["experienceYears"] as? Number)?.toInt() ?: 3,
            appliedDate = map["appliedDate"] as? String ?: "Just now",
            status = map["status"] as? String ?: ApplicationStage.APPLIED.name,
            resumeVersion = map["resumeVersion"] as? String ?: "Default Resume",
            interviewDate = map["interviewDate"] as? String,
            interviewTime = map["interviewTime"] as? String,
            interviewMeetingUrl = map["interviewMeetingUrl"] as? String,
            prepNotes = map["prepNotes"] as? String
        )
    }
}

data class InterviewEntity(
    val id: String = "",
    val applicationId: String = "",
    val jobId: String = "",
    val companyId: String = "",
    val candidateId: String = "",
    val candidateName: String = "",
    val candidateRole: String = "",
    val company: String = "",
    val date: String = "",
    val time: String = "",
    val duration: String = "45 mins",
    val interviewerName: String = "",
    val platform: String = "Google Meet",
    val meetingUrl: String = "",
    val status: String = "Confirmed",
    val scoreTechnical: Int = 0,
    val scoreCulture: Int = 0,
    val feedbackNotes: String = ""
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "applicationId" to applicationId,
        "jobId" to jobId,
        "companyId" to companyId,
        "candidateId" to candidateId,
        "candidateName" to candidateName,
        "candidateRole" to candidateRole,
        "company" to company,
        "date" to date,
        "time" to time,
        "duration" to duration,
        "interviewerName" to interviewerName,
        "platform" to platform,
        "meetingUrl" to meetingUrl,
        "status" to status,
        "scoreTechnical" to scoreTechnical,
        "scoreCulture" to scoreCulture,
        "feedbackNotes" to feedbackNotes
    )

    fun toDomainModel(): InterviewItem = InterviewItem(
        id = id,
        candidateName = candidateName,
        candidateRole = candidateRole,
        company = company,
        date = date,
        time = time,
        duration = duration,
        interviewerName = interviewerName,
        platform = platform,
        meetingUrl = meetingUrl,
        status = status,
        scoreTechnical = scoreTechnical,
        scoreCulture = scoreCulture,
        feedbackNotes = feedbackNotes
    )

    companion object {
        fun fromDocument(id: String, map: Map<String, Any?>): InterviewEntity = InterviewEntity(
            id = id,
            applicationId = map["applicationId"] as? String ?: "",
            jobId = map["jobId"] as? String ?: "",
            companyId = map["companyId"] as? String ?: "",
            candidateId = map["candidateId"] as? String ?: "",
            candidateName = map["candidateName"] as? String ?: "",
            candidateRole = map["candidateRole"] as? String ?: "",
            company = map["company"] as? String ?: "",
            date = map["date"] as? String ?: "",
            time = map["time"] as? String ?: "",
            duration = map["duration"] as? String ?: "45 mins",
            interviewerName = map["interviewerName"] as? String ?: "",
            platform = map["platform"] as? String ?: "Google Meet",
            meetingUrl = map["meetingUrl"] as? String ?: "",
            status = map["status"] as? String ?: "Confirmed",
            scoreTechnical = (map["scoreTechnical"] as? Number)?.toInt() ?: 0,
            scoreCulture = (map["scoreCulture"] as? Number)?.toInt() ?: 0,
            feedbackNotes = map["feedbackNotes"] as? String ?: ""
        )
    }
}

data class JobAlertEntity(
    val id: String = "",
    val userId: String = "",
    val keyword: String = "",
    val category: String = "",
    val minSalary: String = "",
    val frequency: String = "",
    val isActive: Boolean = true,
    val createdAt: String = ""
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "keyword" to keyword,
        "category" to category,
        "minSalary" to minSalary,
        "frequency" to frequency,
        "isActive" to isActive,
        "createdAt" to createdAt
    )

    fun toDomainModel(): JobAlert = JobAlert(
        id = id,
        keyword = keyword,
        category = category,
        minSalary = minSalary,
        frequency = frequency,
        isActive = isActive
    )

    companion object {
        fun fromDocument(id: String, map: Map<String, Any?>): JobAlertEntity = JobAlertEntity(
            id = id,
            userId = map["userId"] as? String ?: "",
            keyword = map["keyword"] as? String ?: "",
            category = map["category"] as? String ?: "",
            minSalary = map["minSalary"] as? String ?: "",
            frequency = map["frequency"] as? String ?: "",
            isActive = map["isActive"] as? Boolean ?: true,
            createdAt = map["createdAt"] as? String ?: ""
        )
    }
}

data class ReportEntity(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val reportedByUid: String = "",
    val reportedBy: String = "",
    val reason: String = "",
    val targetId: String = "",
    val timestamp: String = "",
    val status: String = "Pending Audit"
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "type" to type,
        "title" to title,
        "reportedByUid" to reportedByUid,
        "reportedBy" to reportedBy,
        "reason" to reason,
        "targetId" to targetId,
        "timestamp" to timestamp,
        "status" to status
    )

    fun toDomainModel(): ReportedItem = ReportedItem(
        id = id,
        type = type,
        title = title,
        reportedBy = reportedBy,
        reason = reason,
        timestamp = timestamp,
        status = status
    )

    companion object {
        fun fromDocument(id: String, map: Map<String, Any?>): ReportEntity = ReportEntity(
            id = id,
            type = map["type"] as? String ?: "",
            title = map["title"] as? String ?: "",
            reportedByUid = map["reportedByUid"] as? String ?: "",
            reportedBy = map["reportedBy"] as? String ?: "",
            reason = map["reason"] as? String ?: "",
            targetId = map["targetId"] as? String ?: "",
            timestamp = map["timestamp"] as? String ?: "",
            status = map["status"] as? String ?: "Pending Audit"
        )
    }
}

data class ReferralEntity(
    val id: String = "",
    val referrerUid: String = "",
    val referredUid: String = "",
    val referredName: String = "",
    val referralCode: String = "",
    val status: String = ReferralStatus.REGISTERED.name,
    val rewardAmount: Int = 0,
    val qualifyingPaymentRef: String? = null,
    val createdAt: String = "",
    val rewardedAt: String? = null
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "referrerUid" to referrerUid,
        "referredUid" to referredUid,
        "referredName" to referredName,
        "referralCode" to referralCode,
        "status" to status,
        "rewardAmount" to rewardAmount,
        "qualifyingPaymentRef" to qualifyingPaymentRef,
        "createdAt" to createdAt,
        "rewardedAt" to rewardedAt
    )

    fun toDomainModel(): ReferralItem {
        val s = try { ReferralStatus.valueOf(status) } catch (e: Exception) { ReferralStatus.REGISTERED }
        return ReferralItem(
            id = id,
            referrerUid = referrerUid,
            referredUid = referredUid,
            referredName = referredName,
            referralCode = referralCode,
            status = s,
            rewardAmount = rewardAmount,
            qualifyingPaymentRef = qualifyingPaymentRef,
            createdAt = createdAt,
            rewardedAt = rewardedAt
        )
    }

    companion object {
        fun fromDocument(id: String, map: Map<String, Any?>): ReferralEntity = ReferralEntity(
            id = id,
            referrerUid = map["referrerUid"] as? String ?: "",
            referredUid = map["referredUid"] as? String ?: "",
            referredName = map["referredName"] as? String ?: "",
            referralCode = map["referralCode"] as? String ?: "",
            status = map["status"] as? String ?: ReferralStatus.REGISTERED.name,
            rewardAmount = (map["rewardAmount"] as? Number)?.toInt() ?: 0,
            qualifyingPaymentRef = map["qualifyingPaymentRef"] as? String,
            createdAt = map["createdAt"] as? String ?: "",
            rewardedAt = map["rewardedAt"] as? String
        )
    }
}

data class LedgerEntityDoc(
    val id: String = "",
    val userId: String = "",
    val type: String = LedgerType.REFERRAL_REWARD.name,
    val amount: Int = 0,
    val referenceId: String = "",
    val notes: String = "",
    val timestamp: String = ""
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "type" to type,
        "amount" to amount,
        "referenceId" to referenceId,
        "notes" to notes,
        "timestamp" to timestamp
    )

    fun toDomainModel(): LedgerEntry {
        val t = try { LedgerType.valueOf(type) } catch (e: Exception) { LedgerType.REFERRAL_REWARD }
        return LedgerEntry(
            id = id,
            userId = userId,
            type = t,
            amount = amount,
            referenceId = referenceId,
            notes = notes,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDocument(id: String, map: Map<String, Any?>): LedgerEntityDoc = LedgerEntityDoc(
            id = id,
            userId = map["userId"] as? String ?: "",
            type = map["type"] as? String ?: LedgerType.REFERRAL_REWARD.name,
            amount = (map["amount"] as? Number)?.toInt() ?: 0,
            referenceId = map["referenceId"] as? String ?: "",
            notes = map["notes"] as? String ?: "",
            timestamp = map["timestamp"] as? String ?: ""
        )
    }
}

data class WithdrawalEntity(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val amount: Int = 0,
    val payoutMethod: String = "UPI",
    val payoutAccountDetails: String = "",
    val status: String = WithdrawalStatus.REQUESTED.name,
    val adminNotes: String? = null,
    val requestedAt: String = "",
    val processedAt: String? = null,
    val processedBy: String? = null
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "amount" to amount,
        "payoutMethod" to payoutMethod,
        "payoutAccountDetails" to payoutAccountDetails,
        "status" to status,
        "adminNotes" to adminNotes,
        "requestedAt" to requestedAt,
        "processedAt" to processedAt,
        "processedBy" to processedBy
    )

    fun toDomainModel(): WithdrawalRequest {
        val s = try { WithdrawalStatus.valueOf(status) } catch (e: Exception) { WithdrawalStatus.REQUESTED }
        return WithdrawalRequest(
            id = id,
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            amount = amount,
            payoutMethod = payoutMethod,
            payoutAccountDetails = payoutAccountDetails,
            status = s,
            adminNotes = adminNotes,
            requestedAt = requestedAt,
            processedAt = processedAt,
            processedBy = processedBy
        )
    }

    companion object {
        fun fromDocument(id: String, map: Map<String, Any?>): WithdrawalEntity = WithdrawalEntity(
            id = id,
            userId = map["userId"] as? String ?: "",
            userName = map["userName"] as? String ?: "",
            userEmail = map["userEmail"] as? String ?: "",
            amount = (map["amount"] as? Number)?.toInt() ?: 0,
            payoutMethod = map["payoutMethod"] as? String ?: "UPI",
            payoutAccountDetails = map["payoutAccountDetails"] as? String ?: "",
            status = map["status"] as? String ?: WithdrawalStatus.REQUESTED.name,
            adminNotes = map["adminNotes"] as? String,
            requestedAt = map["requestedAt"] as? String ?: "",
            processedAt = map["processedAt"] as? String,
            processedBy = map["processedBy"] as? String
        )
    }
}

data class PaymentEntity(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val amount: Int = 10,
    val currency: String = "INR",
    val status: String = PaymentStatus.INITIATED.name,
    val provider: String = "MOCK_GATEWAY",
    val providerTransactionRef: String? = null,
    val verifiedAt: String? = null,
    val createdAt: String = ""
) {
    fun toDocumentMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "userEmail" to userEmail,
        "amount" to amount,
        "currency" to currency,
        "status" to status,
        "provider" to provider,
        "providerTransactionRef" to providerTransactionRef,
        "verifiedAt" to verifiedAt,
        "createdAt" to createdAt
    )

    fun toDomainModel(): PremiumPaymentRecord {
        val s = try { PaymentStatus.valueOf(status) } catch (e: Exception) { PaymentStatus.INITIATED }
        return PremiumPaymentRecord(
            id = id,
            userId = userId,
            userEmail = userEmail,
            amount = amount,
            currency = currency,
            status = s,
            provider = provider,
            providerTransactionRef = providerTransactionRef,
            verifiedAt = verifiedAt,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDocument(id: String, map: Map<String, Any?>): PaymentEntity = PaymentEntity(
            id = id,
            userId = map["userId"] as? String ?: "",
            userEmail = map["userEmail"] as? String ?: "",
            amount = (map["amount"] as? Number)?.toInt() ?: 10,
            currency = map["currency"] as? String ?: "INR",
            status = map["status"] as? String ?: PaymentStatus.INITIATED.name,
            provider = map["provider"] as? String ?: "MOCK_GATEWAY",
            providerTransactionRef = map["providerTransactionRef"] as? String,
            verifiedAt = map["verifiedAt"] as? String,
            createdAt = map["createdAt"] as? String ?: ""
        )
    }
}
