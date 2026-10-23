import { describe, it, beforeEach } from "node:test";
import * as assert from "node:assert";
import * as crypto from "crypto";
import * as admin from "firebase-admin";
import { CONSTANTS, getCashfreeConfig } from "../config";
import { verifyCashfreeWebhookSignature } from "../cashfreeService";
import { processSuccessfulPayment } from "../paymentProcessor";
import {
  createCashfreePremiumOrder,
  requestReferralWithdrawal,
  verifyCashfreePremiumPayment,
  cashfreePaymentWebhook,
} from "../index";

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

    const withdrawals = await db.collection("withdrawal_requests").get();
    withdrawals.docs.forEach((d) => batch.delete(d.ref));

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

    it("fails closed when webhook secret key is missing in configuration", async () => {
      delete process.env.CASHFREE_SECRET_KEY;
      delete process.env.CASHFREE_APP_ID;
      const { EventEmitter } = await import("events");
      let statusCode = 0;
      let sentBody = "";
      const req = {
        method: "POST",
        headers: {},
        rawBody: Buffer.from("{}"),
      } as any;
      const res = Object.assign(new EventEmitter(), {
        status: (code: number) => {
          statusCode = code;
          return {
            send: (body: string) => {
              sentBody = body;
              res.emit("finish");
            },
          };
        },
      }) as any;

      await (cashfreePaymentWebhook as any)(req, res);
      assert.strictEqual(statusCode, 500);
      assert.ok(sentBody.includes("Webhook secret key not configured"));
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

  // 4. Server-Side Referral Withdrawal Callable Tests
  describe("Server-side Referral Withdrawal Callable Tests", () => {
    it("rejects unauthenticated withdrawal requests", async () => {
      await assert.rejects(
        async () => {
          await (requestReferralWithdrawal as any).run({
            auth: null,
            data: { amount: 50, payoutAccountDetails: "test@upi" },
          });
        },
        /Authentication required/
      );
    });

    it("rejects requests when successful referral count is less than 10", async () => {
      const USER_ID = "usr_ineligible_refs";
      await db.collection("users").doc(USER_ID).set({
        id: USER_ID,
        fullName: "Ineligible Candidate",
        availableReferralBalance: 100,
        reservedWithdrawalBalance: 0,
        successfulReferralsCount: 6, // < 10
      });

      await assert.rejects(
        async () => {
          await (requestReferralWithdrawal as any).run({
            auth: { uid: USER_ID },
            data: { amount: 50, payoutAccountDetails: "ineligible@upi" },
          });
        },
        /Requires at least 10 successful referrals/
      );
    });

    it("rejects requests when available balance is less than ₹50", async () => {
      const USER_ID = "usr_ineligible_bal";
      await db.collection("users").doc(USER_ID).set({
        id: USER_ID,
        fullName: "Low Balance User",
        availableReferralBalance: 30, // < 50
        reservedWithdrawalBalance: 0,
        successfulReferralsCount: 15,
      });

      await assert.rejects(
        async () => {
          await (requestReferralWithdrawal as any).run({
            auth: { uid: USER_ID },
            data: { amount: 50, payoutAccountDetails: "lowbal@upi" },
          });
        },
        /Minimum withdrawal amount is ₹50|Requires at least ₹50/
      );
    });

    it("rejects withdrawal requests exceeding available balance", async () => {
      const USER_ID = "usr_excess_amount";
      await db.collection("users").doc(USER_ID).set({
        id: USER_ID,
        fullName: "Excess User",
        availableReferralBalance: 60,
        reservedWithdrawalBalance: 0,
        successfulReferralsCount: 12,
      });

      await assert.rejects(
        async () => {
          await (requestReferralWithdrawal as any).run({
            auth: { uid: USER_ID },
            data: { amount: 100, payoutAccountDetails: "excess@upi" },
          });
        },
        /Insufficient balance/
      );
    });

    it("atomically reserves balance, creates withdrawal request with status REQUESTED, and appends ledger record", async () => {
      const USER_ID = "usr_eligible_success";
      await db.collection("users").doc(USER_ID).set({
        id: USER_ID,
        fullName: "Eligible Winner",
        email: "winner@talentnexus.app",
        availableReferralBalance: 120,
        reservedWithdrawalBalance: 0,
        successfulReferralsCount: 14,
      });

      const response = await (requestReferralWithdrawal as any).run({
        auth: { uid: USER_ID, token: { email: "winner@talentnexus.app" } },
        data: { amount: 70, payoutAccountDetails: "winner@okaxis", payoutMethod: "UPI" },
      });

      assert.strictEqual(response.success, true);
      assert.strictEqual(response.amount, 70);
      assert.strictEqual(response.status, "REQUESTED");
      assert.strictEqual(response.remainingAvailableBalance, 50);
      assert.strictEqual(response.reservedWithdrawalBalance, 70);

      // Verify user balances updated in Firestore
      const userDoc = await db.collection("users").doc(USER_ID).get();
      assert.strictEqual(userDoc.data()?.availableReferralBalance, 50);
      assert.strictEqual(userDoc.data()?.reservedWithdrawalBalance, 70);

      // Verify withdrawal_requests doc created
      const wdrDoc = await db.collection("withdrawal_requests").doc(response.requestId).get();
      assert.strictEqual(wdrDoc.exists, true);
      assert.strictEqual(wdrDoc.data()?.userId, USER_ID);
      assert.strictEqual(wdrDoc.data()?.amount, 70);
      assert.strictEqual(wdrDoc.data()?.status, "REQUESTED");
      assert.strictEqual(wdrDoc.data()?.payoutMethod, "UPI");
      assert.strictEqual(wdrDoc.data()?.payoutAccountDetails, "winner@okaxis");

      // Verify referral_ledger entry created
      const ledgerDoc = await db.collection("referral_ledger").doc(`ledger_reserve_${response.requestId}`).get();
      assert.strictEqual(ledgerDoc.exists, true);
      assert.strictEqual(ledgerDoc.data()?.userId, USER_ID);
      assert.strictEqual(ledgerDoc.data()?.type, "WITHDRAWAL_REQUEST");
      assert.strictEqual(ledgerDoc.data()?.amount, 70);
    });

    it("prevents double-spending on consecutive withdrawal attempts", async () => {
      const USER_ID = "usr_double_spend";
      await db.collection("users").doc(USER_ID).set({
        id: USER_ID,
        fullName: "Double Spender",
        availableReferralBalance: 50,
        reservedWithdrawalBalance: 0,
        successfulReferralsCount: 10,
      });

      // First withdrawal of ₹50 -> Should succeed
      const res1 = await (requestReferralWithdrawal as any).run({
        auth: { uid: USER_ID },
        data: { amount: 50, payoutAccountDetails: "user@upi" },
      });
      assert.strictEqual(res1.success, true);
      assert.strictEqual(res1.remainingAvailableBalance, 0);

      // Immediate second withdrawal of ₹50 -> Must fail because available balance is now 0
      await assert.rejects(
        async () => {
          await (requestReferralWithdrawal as any).run({
            auth: { uid: USER_ID },
            data: { amount: 50, payoutAccountDetails: "user@upi" },
          });
        },
        /Insufficient balance|Requires at least ₹50/
      );
    });
  });

  // 5. Server-side createCashfreePremiumOrder Callable Tests
  describe("Server-side createCashfreePremiumOrder Callable Tests", () => {
    const origFetch = globalThis.fetch;

    beforeEach(() => {
      delete process.env.CASHFREE_APP_ID;
      delete process.env.CASHFREE_SECRET_KEY;
      globalThis.fetch = origFetch;
    });

    it("rejects unauthenticated order creation", async () => {
      await assert.rejects(
        async () => {
          await (createCashfreePremiumOrder as any).run({
            auth: null,
            data: {},
          });
        },
        /Authentication required/
      );
    });

    it("fails closed when Cashfree credentials are missing (missing secrets)", async () => {
      await assert.rejects(
        async () => {
          await (createCashfreePremiumOrder as any).run({
            auth: { uid: "usr_fail_closed", token: { email: "fail@test.com" } },
            data: {},
          });
        },
        /Cashfree payment gateway credentials are not configured on the server/
      );
    });

    it("fails closed when only CASHFREE_APP_ID is present but CASHFREE_SECRET_KEY is missing", async () => {
      process.env.CASHFREE_APP_ID = "mock_app_id_only";
      await assert.rejects(
        async () => {
          await (createCashfreePremiumOrder as any).run({
            auth: { uid: "usr_partial_creds", token: { email: "partial@test.com" } },
            data: {},
          });
        },
        /Cashfree payment gateway credentials are not configured on the server/
      );
    });

    it("creates authenticated order with fixed ₹10.00 amount via Cashfree Sandbox API when secrets are configured", async () => {
      const USER_ID = "usr_order_creator";
      process.env.CASHFREE_APP_ID = "mock_cf_app_id_configured";
      process.env.CASHFREE_SECRET_KEY = "mock_cf_secret_key_configured";

      let calledUrl = "";
      let calledHeaders: any = {};
      let calledBody: any = {};

      globalThis.fetch = async (url: any, init?: any) => {
        calledUrl = String(url);
        calledHeaders = init?.headers || {};
        calledBody = JSON.parse(init?.body || "{}");

        return {
          ok: true,
          status: 200,
          json: async () => ({
            cf_order_id: "cf_mock_order_777",
            order_id: calledBody.order_id,
            order_status: "ACTIVE",
            order_amount: 10.0,
            order_currency: "INR",
            payment_session_id: "session_cf_sandbox_mock_token_999",
          }),
        } as any;
      };

      const response = await (createCashfreePremiumOrder as any).run({
        auth: {
          uid: USER_ID,
          token: { email: "candidate_order@talentnexus.app" },
        },
        data: {},
      });

      assert.ok(response.orderId, "Must return an orderId");
      assert.strictEqual(response.paymentSessionId, "session_cf_sandbox_mock_token_999");
      assert.strictEqual(response.amount, 10.0, "Must be fixed ₹10.00");
      assert.strictEqual(response.currency, "INR", "Must be INR currency");
      assert.strictEqual(response.environment, "SANDBOX", "Must be SANDBOX environment");

      // Verify outbound request targeted Sandbox with fixed ₹10.00 amount
      assert.ok(calledUrl.includes("sandbox.cashfree.com/pg/orders"));
      assert.strictEqual(calledHeaders["x-client-id"], "mock_cf_app_id_configured");
      assert.strictEqual(calledHeaders["x-client-secret"], "mock_cf_secret_key_configured");
      assert.strictEqual(calledBody.order_amount, 10.0);
      assert.strictEqual(calledBody.order_currency, "INR");

      // Verify payment document in Firestore
      const orderDoc = await db.collection("premium_payments").doc(response.orderId).get();
      assert.strictEqual(orderDoc.exists, true, "Payment record must exist in Firestore");
      const orderData = orderDoc.data();
      assert.strictEqual(orderData?.userId, USER_ID);
      assert.strictEqual(orderData?.amount, 10.0);
      assert.strictEqual(orderData?.currency, "INR");
      assert.strictEqual(orderData?.paymentSessionId, "session_cf_sandbox_mock_token_999");
      assert.strictEqual(orderData?.status, "PENDING_VERIFICATION");
    });
  });

  // 6. Server-side verifyCashfreePremiumPayment Callable Tests
  describe("Server-side verifyCashfreePremiumPayment Callable Tests", () => {
    const origFetch = globalThis.fetch;

    beforeEach(() => {
      delete process.env.CASHFREE_APP_ID;
      delete process.env.CASHFREE_SECRET_KEY;
      globalThis.fetch = origFetch;
    });

    it("rejects unauthenticated verification requests", async () => {
      await assert.rejects(
        async () => {
          await (verifyCashfreePremiumPayment as any).run({
            auth: null,
            data: { orderId: "order_mock_test" },
          });
        },
        /Authentication required/
      );
    });

    it("rejects verification if caller does not own the order (anti-spoofing)", async () => {
      const ORDER_ID = "order_spoof_check";
      await db.collection("premium_payments").doc(ORDER_ID).set({
        id: ORDER_ID,
        userId: "legitimate_owner_uid",
        amount: 10,
        currency: "INR",
        provider: "CASHFREE",
        status: "PENDING_VERIFICATION",
      });

      await assert.rejects(
        async () => {
          await (verifyCashfreePremiumPayment as any).run({
            auth: { uid: "attacker_uid" },
            data: { orderId: ORDER_ID },
          });
        },
        /Unauthorized: You do not own this order/
      );
    });

    it("fails closed when Cashfree credentials are missing (missing secrets)", async () => {
      const ORDER_ID = "order_fail_closed_verify";
      await db.collection("premium_payments").doc(ORDER_ID).set({
        id: ORDER_ID,
        userId: "usr_fail_closed",
        amount: 10,
        currency: "INR",
        provider: "CASHFREE",
        status: "PENDING_VERIFICATION",
      });

      await assert.rejects(
        async () => {
          await (verifyCashfreePremiumPayment as any).run({
            auth: { uid: "usr_fail_closed" },
            data: { orderId: ORDER_ID },
          });
        },
        /Cashfree payment gateway credentials are not configured on the server/
      );
    });

    it("verifies PAID order via Cashfree Sandbox server-to-server API and activates premium", async () => {
      const USER_ID = "usr_premium_caller";
      const ORDER_ID = "order_valid_premium";
      process.env.CASHFREE_APP_ID = "mock_cf_app_id_configured";
      process.env.CASHFREE_SECRET_KEY = "mock_cf_secret_key_configured";

      await db.collection("users").doc(USER_ID).set({
        id: USER_ID,
        fullName: "Premium Candidate",
        email: "candidate@talentnexus.app",
        isPremium: false,
      });

      await db.collection("premium_payments").doc(ORDER_ID).set({
        id: ORDER_ID,
        userId: USER_ID,
        userEmail: "candidate@talentnexus.app",
        amount: 10,
        currency: "INR",
        provider: "CASHFREE",
        status: "PENDING_VERIFICATION",
        createdAt: new Date().toISOString(),
      });

      globalThis.fetch = async (url: any) => {
        const urlStr = String(url);
        if (urlStr.endsWith("/payments")) {
          return {
            ok: true,
            status: 200,
            json: async () => [
              {
                cf_payment_id: "cf_pay_live_mock_888",
                payment_status: "SUCCESS",
                payment_amount: 10.0,
                payment_currency: "INR",
              },
            ],
          } as any;
        }

        return {
          ok: true,
          status: 200,
          json: async () => ({
            cf_order_id: "cf_order_live_mock_777",
            order_id: ORDER_ID,
            order_status: "PAID",
            order_amount: 10.0,
            order_currency: "INR",
          }),
        } as any;
      };

      const response = await (verifyCashfreePremiumPayment as any).run({
        auth: { uid: USER_ID },
        data: { orderId: ORDER_ID },
      });

      assert.strictEqual(response.status, "SUCCESS");
      assert.strictEqual(response.isPremium, true);

      // Verify user document has isPremium: true in database
      const userDoc = await db.collection("users").doc(USER_ID).get();
      assert.strictEqual(userDoc.data()?.isPremium, true);

      // Verify payment doc has status: VERIFIED in database
      const payDoc = await db.collection("premium_payments").doc(ORDER_ID).get();
      assert.strictEqual(payDoc.data()?.status, "VERIFIED");
      assert.strictEqual(payDoc.data()?.orderStatus, "PAID");
    });

    it("returns PENDING status without unlocking premium when order is still ACTIVE", async () => {
      const USER_ID = "usr_pending_caller";
      const ORDER_ID = "order_pending_check";
      process.env.CASHFREE_APP_ID = "mock_cf_app_id_configured";
      process.env.CASHFREE_SECRET_KEY = "mock_cf_secret_key_configured";

      await db.collection("users").doc(USER_ID).set({
        id: USER_ID,
        fullName: "Pending Candidate",
        isPremium: false,
      });

      await db.collection("premium_payments").doc(ORDER_ID).set({
        id: ORDER_ID,
        userId: USER_ID,
        amount: 10,
        currency: "INR",
        provider: "CASHFREE",
        status: "PENDING_VERIFICATION",
      });

      globalThis.fetch = async (url: any) => {
        const urlStr = String(url);
        if (urlStr.endsWith("/payments")) {
          return { ok: true, status: 200, json: async () => [] } as any;
        }
        return {
          ok: true,
          status: 200,
          json: async () => ({
            cf_order_id: "cf_order_pending",
            order_id: ORDER_ID,
            order_status: "ACTIVE",
            order_amount: 10.0,
            order_currency: "INR",
          }),
        } as any;
      };

      const response = await (verifyCashfreePremiumPayment as any).run({
        auth: { uid: USER_ID },
        data: { orderId: ORDER_ID },
      });

      assert.strictEqual(response.status, "PENDING");
      assert.strictEqual(response.isPremium, false);

      const userDoc = await db.collection("users").doc(USER_ID).get();
      assert.strictEqual(userDoc.data()?.isPremium, false);
    });

    it("rejects verification when Cashfree reports discrepancy in order amount", async () => {
      const USER_ID = "usr_tamper_caller";
      const ORDER_ID = "order_tampered_amount";
      process.env.CASHFREE_APP_ID = "mock_cf_app_id_configured";
      process.env.CASHFREE_SECRET_KEY = "mock_cf_secret_key_configured";

      await db.collection("premium_payments").doc(ORDER_ID).set({
        id: ORDER_ID,
        userId: USER_ID,
        amount: 10,
        currency: "INR",
        provider: "CASHFREE",
        status: "PENDING_VERIFICATION",
      });

      globalThis.fetch = async () => ({
        ok: true,
        status: 200,
        json: async () => ({
          cf_order_id: "cf_order_tampered",
          order_id: ORDER_ID,
          order_status: "PAID",
          order_amount: 99.0, // Discrepancy! Expected 10.0
          order_currency: "INR",
        }),
      } as any);

      await assert.rejects(
        async () => {
          await (verifyCashfreePremiumPayment as any).run({
            auth: { uid: USER_ID },
            data: { orderId: ORDER_ID },
          });
        },
        /Discrepancy in order amount/
      );
    });
  });
});
