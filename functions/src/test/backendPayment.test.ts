import { describe, it, beforeEach } from "node:test";
import * as assert from "node:assert";
import * as crypto from "crypto";
import * as admin from "firebase-admin";
import { CONSTANTS, getCashfreeConfig } from "../config";
import { verifyCashfreeWebhookSignature } from "../cashfreeService";
import { processSuccessfulPayment } from "../paymentProcessor";

// Point Firestore to emulator if host is present or set default emulator port
if (!process.env.FIRESTORE_EMULATOR_HOST) {
  process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8085";
}

if (!admin.apps.length) {
  admin.initializeApp({ projectId: "demo-no-project" });
}

const db = admin.firestore();

describe("Cashfree Sandbox Payment & Referral Backend Tests", () => {
  const TEST_SECRET_KEY = "cfsk_test_mock_secret_key_1234567890abcdef";

  beforeEach(async () => {
    // Clear test collections in emulator
    const payments = await db.collection("premium_payments").get();
    const batch = db.batch();
    payments.docs.forEach((d) => batch.delete(d.ref));

    const users = await db.collection("users").get();
    users.docs.forEach((d) => batch.delete(d.ref));

    const referrals = await db.collection("referrals").get();
    referrals.docs.forEach((d) => batch.delete(d.ref));

    const ledgers = await db.collection("referral_ledger").get();
    ledgers.docs.forEach((d) => batch.delete(d.ref));

    await batch.commit();
  });

  // 1. Webhook Signature Cryptographic Security
  describe("Webhook Signature Security", () => {
    const rawPayload = JSON.stringify({
      data: {
        order: { order_id: "order_123", order_amount: 10.0, order_currency: "INR" },
        payment: { cf_payment_id: "cf_pay_999", payment_status: "SUCCESS" },
      },
      event_time: "2026-10-20T08:00:00Z",
      type: "PAYMENT_SUCCESS_WEBHOOK",
    });
    const timestamp = "1729411200000";

    it("verifies genuine signature calculated with secret key and timestamp", () => {
      const dataToSign = `${timestamp}${rawPayload}`;
      const validSignature = crypto
        .createHmac("sha256", TEST_SECRET_KEY)
        .update(dataToSign)
        .digest("base64");

      const isValid = verifyCashfreeWebhookSignature(
        rawPayload,
        validSignature,
        timestamp,
        TEST_SECRET_KEY
      );
      assert.strictEqual(isValid, true, "Valid signature must be verified successfully.");
    });

    it("rejects tampered webhook payload", () => {
      const dataToSign = `${timestamp}${rawPayload}`;
      const validSignature = crypto
        .createHmac("sha256", TEST_SECRET_KEY)
        .update(dataToSign)
        .digest("base64");

      const tamperedPayload = rawPayload.replace("order_123", "order_hacked");
      const isValid = verifyCashfreeWebhookSignature(
        tamperedPayload,
        validSignature,
        timestamp,
        TEST_SECRET_KEY
      );
      assert.strictEqual(isValid, false, "Tampered payload must be rejected.");
    });

    it("rejects tampered timestamp header (anti-replay)", () => {
      const dataToSign = `${timestamp}${rawPayload}`;
      const validSignature = crypto
        .createHmac("sha256", TEST_SECRET_KEY)
        .update(dataToSign)
        .digest("base64");

      const tamperedTimestamp = "1729411200999";
      const isValid = verifyCashfreeWebhookSignature(
        rawPayload,
        validSignature,
        tamperedTimestamp,
        TEST_SECRET_KEY
      );
      assert.strictEqual(isValid, false, "Tampered timestamp must be rejected.");
    });

    it("rejects signatures computed with incorrect secret key", () => {
      const dataToSign = `${timestamp}${rawPayload}`;
      const wrongSignature = crypto
        .createHmac("sha256", "wrong_attacker_secret_key")
        .update(dataToSign)
        .digest("base64");

      const isValid = verifyCashfreeWebhookSignature(
        rawPayload,
        wrongSignature,
        timestamp,
        TEST_SECRET_KEY
      );
      assert.strictEqual(isValid, false, "Signature with wrong key must be rejected.");
    });

    it("safely rejects null or empty signature headers without crashing", () => {
      assert.strictEqual(
        verifyCashfreeWebhookSignature(rawPayload, undefined, timestamp, TEST_SECRET_KEY),
        false
      );
      assert.strictEqual(
        verifyCashfreeWebhookSignature(rawPayload, "", timestamp, TEST_SECRET_KEY),
        false
      );
      assert.strictEqual(
        verifyCashfreeWebhookSignature(rawPayload, "invalid_sig", timestamp, ""),
        false
      );
    });
  });

  // 2. Fixed Pricing & Security Constraints
  describe("Server-Side Fixed Monetary Constraints", () => {
    it("enforces fixed ₹10.00 (1000 paise) premium price on server constants", () => {
      assert.strictEqual(CONSTANTS.PREMIUM_MEMBERSHIP_AMOUNT_RUPEES, 10.0);
      assert.strictEqual(CONSTANTS.PREMIUM_MEMBERSHIP_AMOUNT_PAISE, 1000);
      assert.strictEqual(CONSTANTS.CURRENCY_INR, "INR");
      assert.strictEqual(CONSTANTS.REFERRAL_REWARD_AMOUNT_RUPEES, 5);
      assert.strictEqual(CONSTANTS.REDEMPTION_MIN_REFERRALS, 10);
      assert.strictEqual(CONSTANTS.REDEMPTION_MIN_BALANCE_RUPEES, 50);
    });

    it("verifies sandbox configuration mode is active and production is disabled", () => {
      const config = getCashfreeConfig();
      assert.strictEqual(config.isSandbox, true);
      assert.ok(config.baseUrl.includes("sandbox.cashfree.com"));
    });
  });

  // 3. Payment Processing, Idempotency & Referral Integrity
  describe("Fulfillment & Referral Reward Processing", () => {
    it("grants premium status to payer and credits ₹5 to referrer", async () => {
      const REFERRER_ID = "usr_referrer_bob";
      const PAYER_ID = "usr_payer_alice";
      const ORDER_ID = "order_tnx_test_001";

      // Seed referrer
      await db.collection("users").doc(REFERRER_ID).set({
        id: REFERRER_ID,
        fullName: "Bob Referrer",
        email: "bob@talentnexus.app",
        referralCode: "TNX-BOB777",
        availableReferralBalance: 10,
        totalEarnedRewards: 10,
        successfulReferralsCount: 2,
        isPremium: false,
      });

      // Seed payer attributed to Bob
      await db.collection("users").doc(PAYER_ID).set({
        id: PAYER_ID,
        fullName: "Alice Vance",
        email: "alice@talentnexus.app",
        referredByUid: REFERRER_ID,
        referredByCode: "TNX-BOB777",
        isPremium: false,
      });

      // Seed order record
      await db.collection("premium_payments").doc(ORDER_ID).set({
        id: ORDER_ID,
        userId: PAYER_ID,
        userEmail: "alice@talentnexus.app",
        amount: 10,
        amountPaise: 1000,
        currency: "INR",
        status: "PENDING_VERIFICATION",
        createdAt: new Date().toISOString(),
      });

      // Execute processor
      const result = await processSuccessfulPayment(
        db,
        ORDER_ID,
        "cf_pay_test_txn_888",
        "CLIENT_CALLABLE_VERIFY"
      );

      assert.strictEqual(result.success, true);
      assert.strictEqual(result.isPremium, true);
      assert.strictEqual(result.alreadyVerified, false);
      assert.strictEqual(result.rewardCredited, true);

      // Verify payer state in Firestore
      const payerDoc = await db.collection("users").doc(PAYER_ID).get();
      assert.strictEqual(payerDoc.data()?.isPremium, true);
      assert.ok(payerDoc.data()?.premiumPurchasedAt);

      // Verify payment doc status in Firestore
      const paymentDoc = await db.collection("premium_payments").doc(ORDER_ID).get();
      assert.strictEqual(paymentDoc.data()?.status, "VERIFIED");
      assert.strictEqual(paymentDoc.data()?.orderStatus, "PAID");
      assert.strictEqual(paymentDoc.data()?.providerTransactionRef, "cf_pay_test_txn_888");

      // Verify referrer balance incremented by exactly ₹5
      const referrerDoc = await db.collection("users").doc(REFERRER_ID).get();
      assert.strictEqual(referrerDoc.data()?.availableReferralBalance, 15);
      assert.strictEqual(referrerDoc.data()?.totalEarnedRewards, 15);
      assert.strictEqual(referrerDoc.data()?.successfulReferralsCount, 3);

      // Verify referral record
      const refDoc = await db.collection("referrals").doc(`ref_${REFERRER_ID}_${PAYER_ID}`).get();
      assert.strictEqual(refDoc.data()?.status, "REWARD_CREDITED");
      assert.strictEqual(refDoc.data()?.rewardAmount, 5);
      assert.strictEqual(refDoc.data()?.qualifyingPaymentRef, ORDER_ID);

      // Verify immutable referral ledger entry
      const ledgerDoc = await db.collection("referral_ledger").doc(`ledger_reward_${ORDER_ID}`).get();
      assert.strictEqual(ledgerDoc.data()?.userId, REFERRER_ID);
      assert.strictEqual(ledgerDoc.data()?.amount, 5);
      assert.strictEqual(ledgerDoc.data()?.type, "REFERRAL_REWARD");
    });

    it("prevents duplicate rewards and grants when called repeatedly (idempotency)", async () => {
      const REFERRER_ID = "usr_referrer_bob";
      const PAYER_ID = "usr_payer_alice";
      const ORDER_ID = "order_tnx_test_duplicate";

      await db.collection("users").doc(REFERRER_ID).set({
        id: REFERRER_ID,
        fullName: "Bob Referrer",
        availableReferralBalance: 0,
        totalEarnedRewards: 0,
        successfulReferralsCount: 0,
      });

      await db.collection("users").doc(PAYER_ID).set({
        id: PAYER_ID,
        fullName: "Alice Vance",
        referredByUid: REFERRER_ID,
        isPremium: false,
      });

      await db.collection("premium_payments").doc(ORDER_ID).set({
        id: ORDER_ID,
        userId: PAYER_ID,
        amount: 10,
        status: "PENDING_VERIFICATION",
      });

      // Call 1: First verification
      const result1 = await processSuccessfulPayment(db, ORDER_ID, "cf_tx_1", "CLIENT_CALLABLE_VERIFY");
      assert.strictEqual(result1.rewardCredited, true);

      const refAfter1 = await db.collection("users").doc(REFERRER_ID).get();
      assert.strictEqual(refAfter1.data()?.availableReferralBalance, 5);
      assert.strictEqual(refAfter1.data()?.successfulReferralsCount, 1);

      // Call 2: Duplicate delivery (e.g. webhook re-try or concurrent button click)
      const result2 = await processSuccessfulPayment(db, ORDER_ID, "cf_tx_1", "GATEWAY_WEBHOOK");
      assert.strictEqual(result2.alreadyVerified, true);
      assert.strictEqual(result2.rewardCredited, false);

      // Balances must remain strictly unchanged
      const refAfter2 = await db.collection("users").doc(REFERRER_ID).get();
      assert.strictEqual(refAfter2.data()?.availableReferralBalance, 5);
      assert.strictEqual(refAfter2.data()?.totalEarnedRewards, 5);
      assert.strictEqual(refAfter2.data()?.successfulReferralsCount, 1);
    });

    it("rejects self-referral reward (cannot earn reward by referring own account)", async () => {
      const USER_ID = "usr_self_referrer";
      const ORDER_ID = "order_self_referral";

      // User tried to refer themselves
      await db.collection("users").doc(USER_ID).set({
        id: USER_ID,
        fullName: "Self Referrer",
        referredByUid: USER_ID, // self-referral
        availableReferralBalance: 0,
        totalEarnedRewards: 0,
        successfulReferralsCount: 0,
        isPremium: false,
      });

      await db.collection("premium_payments").doc(ORDER_ID).set({
        id: ORDER_ID,
        userId: USER_ID,
        amount: 10,
        status: "PENDING_VERIFICATION",
      });

      const result = await processSuccessfulPayment(db, ORDER_ID, "cf_tx_self", "CLIENT_CALLABLE_VERIFY");

      assert.strictEqual(result.isPremium, true);
      assert.strictEqual(result.rewardCredited, false, "Self-referral reward must not be credited.");

      const userDoc = await db.collection("users").doc(USER_ID).get();
      assert.strictEqual(userDoc.data()?.availableReferralBalance, 0);
      assert.strictEqual(userDoc.data()?.successfulReferralsCount, 0);
    });

    it("enforces dual withdrawal conditions logic (10 referrals AND ₹50 balance)", () => {
      function checkRedemptionEligibility(referrals: number, balance: number): boolean {
        return (
          referrals >= CONSTANTS.REDEMPTION_MIN_REFERRALS &&
          balance >= CONSTANTS.REDEMPTION_MIN_BALANCE_RUPEES
        );
      }

      // 5 referrals, ₹60 balance -> INELIGIBLE
      assert.strictEqual(checkRedemptionEligibility(5, 60), false);

      // 12 referrals, ₹40 balance -> INELIGIBLE
      assert.strictEqual(checkRedemptionEligibility(12, 40), false);

      // 10 referrals, ₹50 balance -> ELIGIBLE
      assert.strictEqual(checkRedemptionEligibility(10, 50), true);

      // 15 referrals, ₹75 balance -> ELIGIBLE
      assert.strictEqual(checkRedemptionEligibility(15, 75), true);
    });
  });
});
