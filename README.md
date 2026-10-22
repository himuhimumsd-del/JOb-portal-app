# TalentNexus — Executive Mobile Talent Ecosystem

A modern, high-fidelity Android application built with Kotlin and Jetpack Compose. TalentNexus seamlessly connects ambitious professionals, high-growth employers, and platform administrators within an executive-tier mobile interface.

---

## 📱 Three Core Platform Ecosystems

### 1. Job Seeker Experience
- **Home Feed**: Curated executive roles, real-time search with filter triggers, profile completion tracker, industry category discovery, and featured openings.
- **Explore & Filter**: Full-text keyword search with multi-dimensional filtering by workplace type (Remote, Hybrid, On-site) and industry category (Tech & Dev, Product & UX, Data & AI, Sales & Growth).
- **Job Details & 1-Tap Apply**: Detailed role missions, responsibilities, requirements, and benefits with tailored resume version selection and ATS compatibility scoring.
- **Applications Pipeline Tracker**: Real-time status tracker (Applied, In Review, Shortlisted, Interview Scheduled, Offer) with interview session briefings and Google Meet integration.
- **Saved Bookmarks**: Quick access to saved roles for comparison and instant application.
- **Profile & Resume Management**: Candidate competencies, resume versions, ATS scoring, and automated job alert triggers.

### 2. Employer & Recruiter Suite
- **Talent Hub Dashboard**: Real-time sourcing KPIs (Active Openings, Total Applicants, Shortlisted, Upcoming Interviews) and multi-stage candidate velocity pipeline funnel.
- **Job Listings Management**: Comprehensive job post lifecycle (Active, Under Review, Draft, Closed) with applicant counters and status toggles.
- **5-Step Post Job Wizard**: Step-by-step role creation (Basics, Skills, Compensation & Perks, ATS Screening, Live Candidate Preview & Instant Publish).
- **Applicant Vetting**: AI-ranked candidate pipeline with ATS match scores, key skills review, stage advancement, and interview scheduling.
- **Interviews & Scorecards**: Calendar coordination, Google Meet links, and multi-criteria candidate evaluation scorecards (1-5 stars & hiring notes).
- **Company Branding & Settings**: Verified employer profile, benefits showcase, and recruiter settings.

### 3. Administrator Portal
- **Operations Dashboard**: Platform health telemetrics (99.98% uptime, 124k+ seekers, 4,820 verified orgs), live audit feed, and system governance.
- **Listings Moderation**: Global compliance review queue with Approve or Flag/Suspend actions.
- **User Governance**: Directory of job seekers and verified enterprise entities with KYB standing and status management.
- **Taxonomy & Skills Dictionary**: Standardized industry fields and core competency taxonomy manager.
- **Disputes & KYB Audits**: Policy enforcement on flagged listings, compensation disclosures, and company registrations.

---

## 🛠️ Technology Stack & Architecture

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM with reactive Kotlin StateFlow and Repository Pattern
- **Typography**: Plus Jakarta Sans (Google Fonts bundled locally in `res/font/`)
- **Build System**: Gradle 9.3.1 with Kotlin DSL and Version Catalog (`libs.versions.toml`)
- **Platform**: Android SDK 36 (Minimum SDK 26) with Edge-to-Edge window insets support
