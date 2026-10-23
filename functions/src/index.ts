import * as admin from "firebase-admin";
import { getFirestore } from "firebase-admin/firestore";
import { onCall, onRequest, HttpsError } from "firebase-functions/v2/https";
import {
  cashfreeAppId,
  cashfreeSecretKey,
  getCashfreeConfig,
  CONSTANTS,
} from "./config";
import {
  createCashfreeOrder,
  getCashfreeOrder,
  getCashfreeOrderPayments,
  verifyCashfreeWebhookSignature,
} from "./cashfreeService";
import { processSuccessfulPayment } from "./paymentProcessor";

if (!admin.apps.length) {
  admin.initializeApp();
}

const NAMED_DB_ID = "ai-studio-android-jobporta-b95bcbd3-ac1e-47e0-8460-98c89a740c01";

function getDatabase(): admin.firestore.Firestore {
  try {
    return process.env.FIRESTORE_EMULATOR_HOST ? admin.firestore() : getFirestore(NAMED_DB_ID);
  } catch {
    return admin.firestore();
  }
}

const db = getDatabase();

/**
 * 1. createCashfreePremiumOrder
 * Initiates an authoritative ₹10.00 sandbox order with Cashfree.
 * Requires Firebase Authentication. Amount is strictly locked to ₹10.00 on the server.
 * Requires bound Secret Manager secrets (CASHFREE_APP_ID, CASHFREE_SECRET_KEY).
 */
export const createCashfreePremiumOrder = onCall(
  {
    secrets: [cashfreeAppId, cashfreeSecretKey],
    invoker: "public",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "Authentication required: Must be signed in to create a premium membership order."
      );
    }

    const uid = request.auth.uid;
    const email = request.auth.token.email || "support@talentnexus.app";
    const nowIso = new Date().toISOString();

    // 1. Check Secret Manager credentials first - fail closed with clear error if unconfigured
    const config = getCashfreeConfig();
    if (!config.appId || !config.secretKey) {
      throw new HttpsError(
        "failed-precondition",
        "Cashfree payment gateway credentials are not configured on the server. Please configure CASHFREE_APP_ID and CASHFREE_SECRET_KEY secrets in Google Cloud Secret Manager."
      );
    }

    // Generate unique order ID adhering to Cashfree alphanumeric requirements (<= 50 chars)
    const sanitizedUidSuffix = uid.replace(/[^a-zA-Z0-9]/g, "").slice(0, 6) || "user";
    const orderId = `order_tnx_${sanitizedUidSuffix}_${Date.now()}`;

    // 2. Persist initial pending payment record in Firestore
    const paymentRecord = {
      id: orderId,
      userId: uid,
      userEmail: email,
      amount: CONSTANTS.PREMIUM_MEMBERSHIP_AMOUNT_RUPEES,
      amountPaise: CONSTANTS.PREMIUM_MEMBERSHIP_AMOUNT_PAISE,
      currency: CONSTANTS.CURRENCY_INR,
      status: "INITIATED",
      provider: "CASHFREE",
      providerTransactionRef: null,
      paymentSessionId: null,
      orderStatus: "ACTIVE",
      createdAt: nowIso,
    };

    await db.collection("premium_payments").doc(orderId).set(paymentRecord);

    try {
      // 3. Call official Cashfree Sandbox Create Order API
      const cfResponse = await createCashfreeOrder(orderId, uid, email);
      if (!cfResponse.payment_session_id) {
        throw new Error("Cashfree did not return a valid payment_session_id.");
      }
      const paymentSessionId = cfResponse.payment_session_id;
      const returnedOrderId = cfResponse.order_id || orderId;

      // 4. Update payment record with session ID and pending status
      await db.collection("premium_payments").doc(orderId).update({
        paymentSessionId,
        status: "PENDING_VERIFICATION",
      });

      return {
        orderId: returnedOrderId,
        paymentSessionId,
        amount: CONSTANTS.PREMIUM_MEMBERSHIP_AMOUNT_RUPEES,
        currency: CONSTANTS.CURRENCY_INR,
        environment: "SANDBOX",
      };
    } catch (err: unknown) {
      if (err instanceof HttpsError) {
        throw err;
      }
      const errorMessage = err instanceof Error ? err.message : "Failed to create Cashfree order";
      await db.collection("premium_payments").doc(orderId).update({
        status: "FAILED",
        failureReason: errorMessage,
      });
      throw new HttpsError("internal", `Cashfree Order Error: ${errorMessage}`);
    }
  }
);

/**
 * 2. verifyCashfreePremiumPayment
 * Verifies payment status directly against Cashfree server-side APIs.
 * Prevents client-side spoofing, validates order ownership and ₹10.00 amount.
 * Requires bound Secret Manager secrets (CASHFREE_APP_ID, CASHFREE_SECRET_KEY).
 */
export const verifyCashfreePremiumPayment = onCall(
  {
    secrets: [cashfreeAppId, cashfreeSecretKey],
    invoker: "public",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication required to verify payment.");
    }

    const { orderId } = request.data || {};
    if (!orderId || typeof orderId !== "string") {
      throw new HttpsError("invalid-argument", "Valid 'orderId' is required.");
    }

    const callerUid = request.auth.uid;
    const paymentDoc = await db.collection("premium_payments").doc(orderId).get();

    if (!paymentDoc.exists) {
      throw new HttpsError("not-found", `Order '${orderId}' not found in database.`);
    }

    const paymentData = paymentDoc.data()!;

    // Anti-spoofing: Caller must own this order
    if (paymentData.userId !== callerUid) {
      throw new HttpsError("permission-denied", "Unauthorized: You do not own this order.");
    }

    // Idempotent shortcut if already completed
    if (paymentData.status === "VERIFIED") {
      return {
        status: "SUCCESS",
        orderId,
        isPremium: true,
        message: "Payment already verified.",
      };
    }

    // 1. Verify Payment Authenticity Server-to-Server
    const config = getCashfreeConfig();
    if (!config.appId || !config.secretKey) {
      throw new HttpsError(
        "failed-precondition",
        "Cashfree payment gateway credentials are not configured on the server. Server-to-server payment verification unavailable."
      );
    }

    if (paymentData.provider !== "CASHFREE") {
      throw new HttpsError("failed-precondition", `Invalid payment provider: ${paymentData.provider}`);
    }

    const [cfOrder, cfPayments] = await Promise.all([
      getCashfreeOrder(orderId),
      getCashfreeOrderPayments(orderId).catch(() => []),
    ]);

    // 2. Validate Order Attributes
    if (cfOrder.order_amount !== CONSTANTS.PREMIUM_MEMBERSHIP_AMOUNT_RUPEES) {
      throw new HttpsError("failed-precondition", "Discrepancy in order amount.");
    }
    if (cfOrder.order_currency !== CONSTANTS.CURRENCY_INR) {
      throw new HttpsError("failed-precondition", "Discrepancy in order currency.");
    }

    // 3. Check for Successful Payment Transaction
    const successfulPayment = cfPayments.find((p) => p.payment_status === "SUCCESS");
    const isPaid = cfOrder.order_status === "PAID" || !!successfulPayment;
    const transactionRef =
      successfulPayment?.cf_payment_id || cfOrder.cf_order_id || `cf_order_${orderId}`;

    if (!isPaid) {
      const isPending =
        cfOrder.order_status === "ACTIVE" ||
        cfPayments.some((p) => p.payment_status === "PENDING");
      return {
        status: isPending ? "PENDING" : "FAILED",
        orderId,
        isPremium: false,
        message: isPending
          ? "Payment is pending gateway confirmation."
          : "Cashfree payment failed or was declined.",
      };
    }

    // 4. Process authoritative entitlement and referral credit atomically
    const processResult = await processSuccessfulPayment(
      db,
      orderId,
      transactionRef,
      "CLIENT_CALLABLE_VERIFY"
    );

    return {
      status: "SUCCESS",
      orderId,
      isPremium: processResult.isPremium,
      rewardCredited: processResult.rewardCredited,
    };
  }
);

/**
 * 3. cashfreePaymentWebhook
 * HTTPS webhook endpoint for asynchronous Cashfree server notifications.
 * Verifies HMAC-SHA256 signature, validates payload, and executes idempotent fulfillment.
 * Requires bound Secret Manager secrets (CASHFREE_APP_ID, CASHFREE_SECRET_KEY).
 */
export const cashfreePaymentWebhook = onRequest(
  {
    secrets: [cashfreeAppId, cashfreeSecretKey],
    cors: false,
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("Method Not Allowed");
      return;
    }

    const signature = req.headers["x-webhook-signature"] as string | undefined;
    const timestamp = req.headers["x-webhook-timestamp"] as string | undefined;
    const rawBody = req.rawBody;

    const config = getCashfreeConfig();

    if (!config.secretKey) {
      console.error("[CashfreeWebhook] Secret key not configured in environment.");
      res.status(500).send("Webhook secret key not configured.");
      return;
    }

    if (!rawBody || !signature) {
      console.warn("[CashfreeWebhook] Missing raw body or signature header.");
      res.status(400).send("Missing webhook signature or body.");
      return;
    }

    // 1. Verify Cryptographic Signature
    const isValidSignature = verifyCashfreeWebhookSignature(
      rawBody,
      signature,
      timestamp,
      config.secretKey
    );

    if (!isValidSignature) {
      console.warn("[CashfreeWebhook] Invalid HMAC-SHA256 signature rejected.");
      res.status(400).send("Invalid signature.");
      return;
    }

    // 2. Parse Validated Payload
    let payload: any;
    try {
      payload = JSON.parse(rawBody.toString("utf8"));
    } catch {
      res.status(400).send("Invalid JSON payload.");
      return;
    }

    const eventType = payload.type;
    const orderData = payload.data?.order;
    const paymentData = payload.data?.payment;

    const orderId = orderData?.order_id;
    const cfPaymentId = paymentData?.cf_payment_id || `wh_${Date.now()}`;
    const paymentStatus = paymentData?.payment_status;

    console.log(`[CashfreeWebhook] Received event ${eventType} for order ${orderId}, status: ${paymentStatus}`);

    // We process only successful payment events
    const isSuccessful =
      eventType === "PAYMENT_SUCCESS_WEBHOOK" ||
      paymentStatus === "SUCCESS" ||
      orderData?.order_status === "PAID";

    if (!isSuccessful) {
      res.status(200).json({ status: "IGNORED", reason: `Status '${paymentStatus}' not actionable.` });
      return;
    }

    if (!orderId) {
      res.status(400).send("Missing order_id in webhook payload.");
      return;
    }

    try {
      // 3. Correlate with stored payment record
      const paymentDoc = await db.collection("premium_payments").doc(orderId).get();
      if (!paymentDoc.exists) {
        console.warn(`[CashfreeWebhook] Order '${orderId}' not found in database.`);
        res.status(404).send("Order not found.");
        return;
      }

      // 4. Atomically process fulfillment
      const result = await processSuccessfulPayment(
        db,
        orderId,
        String(cfPaymentId),
        "GATEWAY_WEBHOOK"
      );

      res.status(200).json({
        status: "OK",
        orderId,
        alreadyVerified: result.alreadyVerified,
        rewardCredited: result.rewardCredited,
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Error processing webhook";
      console.error(`[CashfreeWebhook] Error processing order ${orderId}:`, err);
      res.status(500).send(`Internal Error: ${msg}`);
    }
  }
);

/**
 * 4. requestReferralWithdrawal
 * Authoritative, server-side referral withdrawal request submission.
 * Enforces dual eligibility criteria (>= 10 successful referrals AND >= ₹50 available balance).
 * Atomically reserves balance, generates pending withdrawal request, and appends immutable ledger entry.
 */
export const requestReferralWithdrawal = onCall(
  {
    invoker: "public",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError(
        "unauthenticated",
        "Authentication required: Must be signed in to submit a withdrawal request."
      );
    }

    const callerUid = request.auth.uid;
    const { amount, payoutAccountDetails, payoutMethod } = request.data || {};

    // 1. Input validation
    const parsedAmount = Number(amount);
    if (!parsedAmount || isNaN(parsedAmount) || !Number.isInteger(parsedAmount) || parsedAmount <= 0) {
      throw new HttpsError(
        "invalid-argument",
        "A valid whole number integer amount is required."
      );
    }

    if (parsedAmount < CONSTANTS.REDEMPTION_MIN_BALANCE_RUPEES) {
      throw new HttpsError(
        "failed-precondition",
        `Minimum withdrawal amount is ₹${CONSTANTS.REDEMPTION_MIN_BALANCE_RUPEES}.`
      );
    }

    if (!payoutAccountDetails || typeof payoutAccountDetails !== "string" || !payoutAccountDetails.trim()) {
      throw new HttpsError(
        "invalid-argument",
        "Valid payout account details (UPI ID or Bank Account / IFSC) are required."
      );
    }

    const cleanAccountDetails = payoutAccountDetails.trim();
    const resolvedMethod =
      typeof payoutMethod === "string" && payoutMethod.trim()
        ? payoutMethod.trim()
        : cleanAccountDetails.includes("@")
        ? "UPI"
        : "Bank Transfer (NEFT)";

    const sanitizedUidSuffix = callerUid.replace(/[^a-zA-Z0-9]/g, "").slice(0, 6) || "user";
    const requestId = `wdr_${sanitizedUidSuffix}_${Date.now()}`;
    const userRef = db.collection("users").doc(callerUid);
    const wdrRef = db.collection("withdrawal_requests").doc(requestId);
    const ledgerRef = db.collection("referral_ledger").doc(`ledger_reserve_${requestId}`);

    return await db.runTransaction(async (transaction) => {
      // ==========================================
      // PHASE 1: ALL READ OPERATIONS FIRST
      // ==========================================
      const userDoc = await transaction.get(userRef);
      if (!userDoc.exists) {
        throw new HttpsError("not-found", "User account profile not found.");
      }

      const userData = userDoc.data()!;
      const availableBalance = Number(userData.availableReferralBalance || 0);
      const reservedBalance = Number(userData.reservedWithdrawalBalance || 0);
      const successfulReferrals = Number(userData.successfulReferralsCount || 0);

      // Enforce server-side eligibility rules
      if (successfulReferrals < CONSTANTS.REDEMPTION_MIN_REFERRALS) {
        throw new HttpsError(
          "failed-precondition",
          `Redemption blocked: Requires at least ${CONSTANTS.REDEMPTION_MIN_REFERRALS} successful referrals (currently ${successfulReferrals}).`
        );
      }

      if (availableBalance < CONSTANTS.REDEMPTION_MIN_BALANCE_RUPEES) {
        throw new HttpsError(
          "failed-precondition",
          `Redemption blocked: Requires at least ₹${CONSTANTS.REDEMPTION_MIN_BALANCE_RUPEES} available referral balance (currently ₹${availableBalance}).`
        );
      }

      if (parsedAmount > availableBalance) {
        throw new HttpsError(
          "failed-precondition",
          `Insufficient balance. Available: ₹${availableBalance}, Requested: ₹${parsedAmount}.`
        );
      }

      const existingWdr = await transaction.get(wdrRef);
      if (existingWdr.exists) {
        throw new HttpsError("already-exists", `Withdrawal request '${requestId}' already exists.`);
      }

      // ==========================================
      // PHASE 2: ALL WRITE OPERATIONS SECOND
      // ==========================================
      const nowIso = new Date().toISOString();
      const newAvailable = availableBalance - parsedAmount;
      const newReserved = reservedBalance + parsedAmount;

      // 1. Atomically adjust balances
      transaction.update(userRef, {
        availableReferralBalance: newAvailable,
        reservedWithdrawalBalance: newReserved,
      });

      // 2. Persist withdrawal request (status: REQUESTED only)
      transaction.set(wdrRef, {
        id: requestId,
        userId: callerUid,
        userName: userData.fullName || "भर्ती-App Member",
        userEmail: userData.email || request.auth?.token?.email || "",
        amount: parsedAmount,
        payoutMethod: resolvedMethod,
        payoutAccountDetails: cleanAccountDetails,
        status: "REQUESTED",
        requestedAt: nowIso,
      });

      // 3. Append immutable audit ledger record
      transaction.set(ledgerRef, {
        id: `ledger_reserve_${requestId}`,
        userId: callerUid,
        type: "WITHDRAWAL_REQUEST",
        amount: parsedAmount,
        referenceId: requestId,
        notes: `Withdrawal request submitted: ₹${parsedAmount} reserved via ${resolvedMethod}`,
        timestamp: nowIso,
      });

      return {
        success: true,
        requestId,
        amount: parsedAmount,
        payoutMethod: resolvedMethod,
        payoutAccountDetails: cleanAccountDetails,
        status: "REQUESTED",
        remainingAvailableBalance: newAvailable,
        reservedWithdrawalBalance: newReserved,
        message: "Withdrawal request submitted successfully.",
      };
    });
  }
);
