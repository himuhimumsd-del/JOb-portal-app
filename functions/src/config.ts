import { defineSecret } from "firebase-functions/params";

/**
 * Secret definitions for Google Cloud Secret Manager.
 * In development or local emulator, fallback to environment variables.
 */
export const cashfreeAppId = defineSecret("CASHFREE_APP_ID");
export const cashfreeSecretKey = defineSecret("CASHFREE_SECRET_KEY");

export function getCashfreeConfig() {
  let appId = process.env.CASHFREE_APP_ID || "";
  let secretKey = process.env.CASHFREE_SECRET_KEY || "";

  if (!appId) {
    try {
      appId = cashfreeAppId.value();
    } catch {
      appId = "";
    }
  }

  if (!secretKey) {
    try {
      secretKey = cashfreeSecretKey.value();
    } catch {
      secretKey = "";
    }
  }

  return {
    appId: appId.trim(),
    secretKey: secretKey.trim(),
    // Sandbox only - Production is strictly disabled
    baseUrl: "https://sandbox.cashfree.com/pg",
    apiVersion: "2023-08-01",
    isSandbox: true,
  };
}

export function isLocalOrEmulatorEnvironment(): boolean {
  return Boolean(
    process.env.FUNCTIONS_EMULATOR === "true" ||
    process.env.FIRESTORE_EMULATOR_HOST ||
    process.env.NODE_ENV === "test"
  );
}

export const CONSTANTS = {
  PREMIUM_MEMBERSHIP_AMOUNT_RUPEES: 10.0,
  PREMIUM_MEMBERSHIP_AMOUNT_PAISE: 1000,
  CURRENCY_INR: "INR",
  REFERRAL_REWARD_AMOUNT_RUPEES: 5,
  REFERRAL_REWARD_AMOUNT_PAISE: 500,
  REDEMPTION_MIN_REFERRALS: 10,
  REDEMPTION_MIN_BALANCE_RUPEES: 50,
};
