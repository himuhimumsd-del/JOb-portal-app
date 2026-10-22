import * as admin from "firebase-admin";
import { onCall, onRequest, HttpsError } from "firebase-functions/v2/https";
import { cashfreeAppId, cashfreeSecretKey, getCashfreeConfig, CONSTANTS } from "./config";
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

const db = admin.firestore();

/**
 * 1. createCashfreePremiumOrder
 * Initiates an authoritative ₹10.00 sandbox order with Cashfree.
 * Requires Firebase Authentication. Amount is strictly locked to ₹10.00 on the server.
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

    // Generate unique order ID adhering to Cashfree alphanumeric requirements (<= 50 chars)
    const sanitizedUidSuffix = uid.replace(/[^a-zA-Z0-9]/g, "").slice(0, 6) || "user";
    const orderId = `order_tnx_${sanitizedUidSuffix}_${Date.now()}`;

    // 1. Persist initial pending payment record in Firestore
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
      // 2. Call official Cashfree Sandbox Create Order API
      const cfResponse = await createCashfreeOrder(orderId, uid, email);

      if (!cfResponse.payment_session_id) {
        throw new Error("Cashfree did not return a valid payment_session_id.");
      }

      // 3. Update payment record with session ID and pending status
      await db.collection("premium_payments").doc(orderId).update({
        paymentSessionId: cfResponse.payment_session_id,
        status: "PENDING_VERIFICATION",
      });

      return {
        orderId: cfResponse.order_id,
        paymentSessionId: cfResponse.payment_session_id,
        amount: CONSTANTS.PREMIUM_MEMBERSHIP_AMOUNT_RUPEES,
        currency: CONSTANTS.CURRENCY_INR,
        environment: "SANDBOX",
      };
    } catch (err: unknown) {
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

    // 1. Query Cashfree Sandbox Server-to-Server
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

    if (!isPaid) {
      const activePending = cfPayments.some((p) => p.payment_status === "PENDING");
      return {
        status: activePending ? "PENDING" : "FAILED",
        orderId,
        isPremium: false,
        message: activePending
          ? "Payment is processing with bank/UPI."
          : `Order is not paid (Cashfree order_status: ${cfOrder.order_status}).`,
      };
    }

    // 4. Process authoritative entitlement and referral credit atomically
    const transactionRef =
      successfulPayment?.cf_payment_id || cfOrder.cf_order_id || `cf_order_${orderId}`;

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
 */
export const cashfreePaymentWebhook = onRequest(
  {
    secrets: [cashfreeSecretKey],
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
