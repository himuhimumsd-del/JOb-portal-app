import * as admin from "firebase-admin";
import { CONSTANTS } from "./config";

export interface ProcessPaymentResult {
  success: boolean;
  orderId: string;
  userId: string;
  isPremium: boolean;
  alreadyVerified: boolean;
  rewardCredited: boolean;
  referrerUid?: string;
  message?: string;
}

/**
 * Shared, authoritative server-side payment processor.
 * Atomically verifies payment, grants premium status to the payer,
 * and qualifies the referrer for exactly ₹5 reward (if eligible) once.
 * Fully idempotent against duplicate webhook deliveries and simultaneous retries.
 *
 * CRITICAL FIRESTORE RULE: All transaction.get() reads MUST occur before any transaction writes.
 */
export async function processSuccessfulPayment(
  db: admin.firestore.Firestore,
  orderId: string,
  providerTransactionRef: string,
  source: "CLIENT_CALLABLE_VERIFY" | "GATEWAY_WEBHOOK" | "ADMIN_RECONCILIATION"
): Promise<ProcessPaymentResult> {
  const paymentRef = db.collection("premium_payments").doc(orderId);

  return await db.runTransaction(async (transaction) => {
    // ==========================================
    // PHASE 1: ALL READ OPERATIONS FIRST
    // ==========================================
    const paymentDoc = await transaction.get(paymentRef);

    if (!paymentDoc.exists) {
      throw new Error(`Payment record '${orderId}' not found in database.`);
    }

    const paymentData = paymentDoc.data()!;

    // Idempotency Check: If already verified, do not duplicate actions
    if (paymentData.status === "VERIFIED") {
      return {
        success: true,
        orderId,
        userId: paymentData.userId,
        isPremium: true,
        alreadyVerified: true,
        rewardCredited: false,
        message: "Payment already verified previously.",
      };
    }

    const userId: string = paymentData.userId;
    const userRef = db.collection("users").doc(userId);
    const userDoc = await transaction.get(userRef);

    // Prepare referral reads
    let referrerUid: string | undefined = undefined;
    let referralCode = "";
    let refRef: admin.firestore.DocumentReference | null = null;
    let refDoc: admin.firestore.DocumentSnapshot | null = null;
    let referrerRef: admin.firestore.DocumentReference | null = null;
    let referrerDoc: admin.firestore.DocumentSnapshot | null = null;

    if (userDoc.exists) {
      const userData = userDoc.data()!;
      referrerUid = userData.referredByUid;
      referralCode = userData.referredByCode || "";

      // Anti-self-referral: Only read referrer docs if payer did not refer themselves
      if (referrerUid && referrerUid !== userId) {
        const refId = `ref_${referrerUid}_${userId}`;
        refRef = db.collection("referrals").doc(refId);
        refDoc = await transaction.get(refRef);

        referrerRef = db.collection("users").doc(referrerUid);
        referrerDoc = await transaction.get(referrerRef);
      }
    }

    // ==========================================
    // PHASE 2: ALL WRITE OPERATIONS SECOND
    // ==========================================
    const nowIso = new Date().toISOString();

    // 1. Mark payment record verified
    transaction.update(paymentRef, {
      status: "VERIFIED",
      orderStatus: "PAID",
      provider: "CASHFREE",
      providerTransactionRef: providerTransactionRef,
      verifiedAt: nowIso,
      verifiedVia: source,
      amount: CONSTANTS.PREMIUM_MEMBERSHIP_AMOUNT_RUPEES,
      amountPaise: CONSTANTS.PREMIUM_MEMBERSHIP_AMOUNT_PAISE,
      currency: CONSTANTS.CURRENCY_INR,
    });

    // 2. Grant premium membership to payer
    if (userDoc.exists) {
      transaction.update(userRef, {
        isPremium: true,
        premiumPurchasedAt: nowIso,
      });
    }

    // 3. Process referral reward if eligible and not already rewarded
    let rewardCredited = false;

    if (refRef && refDoc && referrerRef && referrerDoc && referrerDoc.exists) {
      const isAlreadyRewarded =
        refDoc.exists && refDoc.data()?.status === "REWARD_CREDITED";

      if (!isAlreadyRewarded) {
        const userData = userDoc.data()!;

        // Update referral document
        const refData = {
          id: refRef.id,
          referrerUid: referrerUid,
          referredUid: userId,
          referredName: userData.fullName || "Referred Member",
          referralCode: referralCode,
          status: "REWARD_CREDITED",
          rewardAmount: CONSTANTS.REFERRAL_REWARD_AMOUNT_RUPEES,
          qualifyingPaymentRef: orderId,
          createdAt: refDoc.exists ? refDoc.data()?.createdAt || nowIso : nowIso,
          rewardedAt: nowIso,
        };
        transaction.set(refRef, refData, { merge: true });

        // Credit referrer balance
        const refUserData = referrerDoc.data()!;
        const curBal = Number(refUserData.availableReferralBalance || 0);
        const curEarned = Number(refUserData.totalEarnedRewards || 0);
        const curSuccess = Number(refUserData.successfulReferralsCount || 0);

        transaction.update(referrerRef, {
          availableReferralBalance: curBal + CONSTANTS.REFERRAL_REWARD_AMOUNT_RUPEES,
          totalEarnedRewards: curEarned + CONSTANTS.REFERRAL_REWARD_AMOUNT_RUPEES,
          successfulReferralsCount: curSuccess + 1,
        });

        // Write immutable ledger entry
        const ledgerRef = db
          .collection("referral_ledger")
          .doc(`ledger_reward_${orderId}`);
        transaction.set(ledgerRef, {
          id: `ledger_reward_${orderId}`,
          userId: referrerUid,
          type: "REFERRAL_REWARD",
          amount: CONSTANTS.REFERRAL_REWARD_AMOUNT_RUPEES,
          referenceId: orderId,
          notes: `Referral reward: ₹${CONSTANTS.REFERRAL_REWARD_AMOUNT_RUPEES} for verified Cashfree payment ${providerTransactionRef}`,
          timestamp: nowIso,
        });

        rewardCredited = true;
      }
    }

    return {
      success: true,
      orderId,
      userId,
      isPremium: true,
      alreadyVerified: false,
      rewardCredited,
      referrerUid,
    };
  });
}
