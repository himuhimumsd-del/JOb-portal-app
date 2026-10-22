const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_candidate";
const BOB_UID = "bob_candidate";
const CHARLIE_EMPLOYER = "charlie_employer";
const EVE_ATTACKER = "eve_attacker";
const ADMIN_UID = "admin_user";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();

  // Seed company and membership using admin / privileged context
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("companies").doc("acme_corp").set({
      id: "acme_corp",
      name: "Acme Corp",
      createdBy: CHARLIE_EMPLOYER,
      isVerified: true,
    });

    await db.collection("companies").doc("acme_corp").collection("members").doc(CHARLIE_EMPLOYER).set({
      companyId: "acme_corp",
      userId: CHARLIE_EMPLOYER,
      role: "owner",
    });

    await db.collection("taxonomy_categories").doc("tech").set({
      id: "tech",
      name: "Tech & Dev",
      icon: "code",
    });

    await db.collection("jobs").doc("job_active").set({
      id: "job_active",
      title: "Senior Engineer",
      companyId: "acme_corp",
      company: "Acme Corp",
      creatorId: CHARLIE_EMPLOYER,
      status: "ACTIVE",
      applicantsCount: 0,
      isVerified: true,
    });

    await db.collection("jobs").doc("job_draft").set({
      id: "job_draft",
      title: "Confidential Architect",
      companyId: "acme_corp",
      company: "Acme Corp",
      creatorId: CHARLIE_EMPLOYER,
      status: "DRAFT",
      applicantsCount: 0,
      isVerified: false,
    });

    await db.collection("users").doc(ALICE_UID).set({
      id: ALICE_UID,
      fullName: "Alice Vance",
      email: "alice@test.com",
      role: "JOB_SEEKER",
      isAdmin: false,
      isVerified: false,
    });

    await db.collection("applications").doc("job_active_alice_candidate").set({
      id: "job_active_alice_candidate",
      jobId: "job_active",
      companyId: "acme_corp",
      candidateId: ALICE_UID,
      status: "APPLIED",
    });

    await db.collection("interviews").doc("int_alice").set({
      id: "int_alice",
      applicationId: "job_active_alice_candidate",
      jobId: "job_active",
      companyId: "acme_corp",
      candidateId: ALICE_UID,
      candidateName: "Alice Vance",
      candidateRole: "Senior Engineer",
      company: "Acme Corp",
      date: "Tomorrow",
      time: "2:00 PM",
      duration: "45 mins",
      interviewerName: "Charlie",
      meetingUrl: "https://meet.google.com/test-meet",
      status: "Confirmed",
      scoreTechnical: 0,
      scoreCulture: 0,
      feedbackNotes: "",
    });

    await db.collection("interviews").doc("int_alice").collection("scorecards").doc("eval_1").set({
      score: 5,
      notes: "Internal hiring committee evaluation",
    });
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("Public taxonomy categories reading succeeds for unauthenticated clients", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthedDb.collection("taxonomy_categories").doc("tech").get());
});

test("Creating taxonomy category fails for regular users, succeeds for admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("taxonomy_categories").doc("design").set({
    id: "design",
    name: "Design",
    icon: "palette",
  }));

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { admin: true }).firestore();
  await assertSucceeds(adminDb.collection("taxonomy_categories").doc("design").set({
    id: "design",
    name: "Design",
    icon: "palette",
  }));
});

test("Active job is readable by unauthenticated users", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthedDb.collection("jobs").doc("job_active").get());
});

test("Draft job is NOT readable by unauthorized candidate", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("jobs").doc("job_draft").get());
});

test("Draft job is readable by authorized company member", async () => {
  const charlieDb = testEnv.authenticatedContext(CHARLIE_EMPLOYER).firestore();
  await assertSucceeds(charlieDb.collection("jobs").doc("job_draft").get());
});

test("Unauthorized job publishing: non-member cannot post job for company", async () => {
  const eveDb = testEnv.authenticatedContext(EVE_ATTACKER).firestore();
  await assertFails(eveDb.collection("jobs").doc("job_spoofed").set({
    id: "job_spoofed",
    title: "Fake Job",
    companyId: "acme_corp",
    company: "Acme Corp",
    creatorId: EVE_ATTACKER,
    status: "ACTIVE",
    applicantsCount: 0,
    isVerified: false,
  }));
});

test("Authorized company member can post new job", async () => {
  const charlieDb = testEnv.authenticatedContext(CHARLIE_EMPLOYER).firestore();
  await assertSucceeds(charlieDb.collection("jobs").doc("job_new").set({
    id: "job_new",
    title: "Staff Product Designer",
    companyId: "acme_corp",
    company: "Acme Corp",
    creatorId: CHARLIE_EMPLOYER,
    status: "ACTIVE",
    applicantsCount: 0,
    isVerified: false,
  }));
});

test("Job creation cannot self-verify without admin privileges", async () => {
  const charlieDb = testEnv.authenticatedContext(CHARLIE_EMPLOYER).firestore();
  await assertFails(charlieDb.collection("jobs").doc("job_unverified_escalation").set({
    id: "job_unverified_escalation",
    title: "Senior Lead",
    companyId: "acme_corp",
    company: "Acme Corp",
    creatorId: CHARLIE_EMPLOYER,
    status: "ACTIVE",
    applicantsCount: 0,
    isVerified: true, // Should fail because not admin
  }));
});

test("Candidate cannot read another candidate's private profile", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Candidate can read their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Privilege escalation: User cannot grant themselves admin flag", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({
    isAdmin: true,
  }));
});

test("Cross-candidate application isolation: Bob cannot view Alice's application", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("applications").doc("job_active_alice_candidate").get());
});

test("Company member can view candidate application for their job", async () => {
  const charlieDb = testEnv.authenticatedContext(CHARLIE_EMPLOYER).firestore();
  await assertSucceeds(charlieDb.collection("applications").doc("job_active_alice_candidate").get());
});

test("Candidate can submit valid application for active job with deterministic ID", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("applications").doc("job_active_bob_candidate").set({
    id: "job_active_bob_candidate",
    jobId: "job_active",
    companyId: "acme_corp",
    candidateId: BOB_UID,
    status: "APPLIED",
    resumeVersion: "Bob_Resume_2026.pdf",
    appliedDate: "Just now",
  }));
});

test("Candidate cannot submit application for draft job", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("applications").doc("job_draft_bob_candidate").set({
    id: "job_draft_bob_candidate",
    jobId: "job_draft",
    companyId: "acme_corp",
    candidateId: BOB_UID,
    status: "APPLIED",
  }));
});

test("Candidate cannot submit application with non-deterministic ID", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("applications").doc("random_uuid_id").set({
    id: "random_uuid_id",
    jobId: "job_active",
    companyId: "acme_corp",
    candidateId: BOB_UID,
    status: "APPLIED",
  }));
});

test("Candidate cannot submit application on behalf of another user", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("applications").doc("job_active_alice_candidate").set({
    id: "job_active_alice_candidate",
    jobId: "job_active",
    companyId: "acme_corp",
    candidateId: ALICE_UID,
    status: "APPLIED",
  }));
});

test("Candidate cannot create application with elevated stage (HIRED)", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("applications").doc("job_active_bob_candidate").set({
    id: "job_active_bob_candidate",
    jobId: "job_active",
    companyId: "acme_corp",
    candidateId: BOB_UID,
    status: "HIRED",
  }));
});

test("Candidate cannot self-assign ATS score on application submission", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("applications").doc("job_active_bob_candidate").set({
    id: "job_active_bob_candidate",
    jobId: "job_active",
    companyId: "acme_corp",
    candidateId: BOB_UID,
    status: "APPLIED",
    atsScore: 99,
  }));
});

test("Candidate cannot alter their own application stage", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("applications").doc("job_active_alice_candidate").update({
    status: "SHORTLISTED",
  }));
});

test("Authorized employer can advance candidate stage to SHORTLISTED", async () => {
  const charlieDb = testEnv.authenticatedContext(CHARLIE_EMPLOYER).firestore();
  await assertSucceeds(charlieDb.collection("applications").doc("job_active_alice_candidate").update({
    status: "SHORTLISTED",
  }));
});

test("Unauthorized third party cannot modify application stage", async () => {
  const eveDb = testEnv.authenticatedContext(EVE_ATTACKER).firestore();
  await assertFails(eveDb.collection("applications").doc("job_active_alice_candidate").update({
    status: "REJECTED",
  }));
});

test("Candidate cannot alter authoritative job applicant counter directly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("jobs").doc("job_active").update({
    applicantsCount: 999,
  }));
});

test("Candidate can read their own scheduled interview details", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("interviews").doc("int_alice").get());
});

test("Candidate cannot read another candidate's scheduled interview", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("interviews").doc("int_alice").get());
});

test("Candidate cannot create an interview session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("interviews").doc("int_unauthorized").set({
    id: "int_unauthorized",
    applicationId: "job_active_alice_candidate",
    jobId: "job_active",
    companyId: "acme_corp",
    candidateId: ALICE_UID,
    meetingUrl: "https://meet.google.com/hacked",
  }));
});

test("Candidate cannot update interview schedule or scorecard", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("interviews").doc("int_alice").update({
    scoreTechnical: 5,
    scoreCulture: 5,
    feedbackNotes: "Hired myself",
  }));
});

test("Candidate cannot read internal scorecards subcollection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("interviews").doc("int_alice").collection("scorecards").doc("eval_1").get());
});

test("Authorized company member can schedule interview session", async () => {
  const charlieDb = testEnv.authenticatedContext(CHARLIE_EMPLOYER).firestore();
  await assertSucceeds(charlieDb.collection("interviews").doc("int_new").set({
    id: "int_new",
    applicationId: "job_active_alice_candidate",
    jobId: "job_active",
    companyId: "acme_corp",
    candidateId: ALICE_UID,
    candidateName: "Alice Vance",
    candidateRole: "Senior Engineer",
    company: "Acme Corp",
    date: "Friday",
    time: "11:00 AM",
    duration: "45 mins",
    interviewerName: "Charlie",
    meetingUrl: "https://meet.google.com/test-meet-new",
    status: "Confirmed",
  }));
});

test("Non-company-member cannot schedule interview for another company", async () => {
  const eveDb = testEnv.authenticatedContext(EVE_ATTACKER).firestore();
  await assertFails(eveDb.collection("interviews").doc("int_eve").set({
    id: "int_eve",
    applicationId: "job_active_alice_candidate",
    jobId: "job_active",
    companyId: "acme_corp",
    candidateId: ALICE_UID,
    meetingUrl: "https://meet.google.com/eve",
  }));
});

test("Authorized company member can update scorecard ratings and notes", async () => {
  const charlieDb = testEnv.authenticatedContext(CHARLIE_EMPLOYER).firestore();
  await assertSucceeds(charlieDb.collection("interviews").doc("int_alice").update({
    scoreTechnical: 5,
    scoreCulture: 4,
    feedbackNotes: "Strong systems architecture depth, excellent culture add.",
  }));
});

test("Authorized company member can reschedule or cancel interview", async () => {
  const charlieDb = testEnv.authenticatedContext(CHARLIE_EMPLOYER).firestore();
  await assertSucceeds(charlieDb.collection("interviews").doc("int_alice").update({
    date: "Next Monday",
    time: "3:00 PM",
    status: "Rescheduled",
  }));
});

test("Authorized company member can read and write internal scorecards subcollection", async () => {
  const charlieDb = testEnv.authenticatedContext(CHARLIE_EMPLOYER).firestore();
  await assertSucceeds(charlieDb.collection("interviews").doc("int_alice").collection("scorecards").doc("eval_1").get());
  await assertSucceeds(charlieDb.collection("interviews").doc("int_alice").collection("scorecards").doc("eval_2").set({
    score: 5,
    notes: "Approved for offer",
  }));
});

test("Privilege escalation: User cannot grant themselves premium status directly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({
    isPremium: true,
  }));
});

test("Privilege escalation: User cannot arbitrarily increment referral balance", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({
    availableReferralBalance: 10000,
  }));
});

test("Referral security: User cannot refer themselves", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("referrals").doc("ref_self").set({
    referredUid: ALICE_UID,
    referrerUid: ALICE_UID,
    referralCode: "TNX-ALICE",
    status: "REGISTERED",
    rewardAmount: 0,
  }));
});

test("Referral security: Candidate can register valid referral attribution", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("referrals").doc("ref_alice_bob").set({
    referredUid: BOB_UID,
    referrerUid: ALICE_UID,
    referralCode: "TNX-ALICE",
    status: "REGISTERED",
    rewardAmount: 0,
  }));
});

test("Referral privacy: Bob cannot read Alice's private referral record", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("referrals").doc("ref_alice_private").get());
});

test("Referral security: User cannot self-award referral reward or alter status to REWARD_CREDITED", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("referrals").doc("ref_alice_bob").update({
    status: "REWARD_CREDITED",
    rewardAmount: 5,
  }));
});

test("Withdrawal security: User can submit pending withdrawal request", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("withdrawal_requests").doc("wdr_alice_1").set({
    userId: ALICE_UID,
    amount: 50,
    status: "REQUESTED",
    payoutAccountDetails: "alice@upi",
  }));
});

test("Withdrawal security: User cannot approve their own withdrawal request", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("withdrawal_requests").doc("wdr_alice_1").update({
    status: "COMPLETED",
  }));
});

test("Payment security: User can initiate payment record but cannot mark it VERIFIED directly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("premium_payments").doc("pay_alice_1").set({
    userId: ALICE_UID,
    amount: 10,
    status: "INITIATED",
  }));
  await assertFails(aliceDb.collection("premium_payments").doc("pay_alice_1").update({
    status: "VERIFIED",
  }));
});

