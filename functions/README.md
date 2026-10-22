# TalentNexus Cashfree Sandbox Payment Gateway Backend

This directory contains the Firebase Cloud Functions v2 TypeScript backend for the TalentNexus Executive Career Tier Premium Membership (₹10.00 INR) payment flow and referral reward integration.

## Architecture & Security Principles

1. **Server-Side Price Enforcement**: Order amounts are fixed to ₹10.00 INR (1000 paise) directly on the backend. Client-requested amounts or statuses are never trusted.
2. **Authenticated Operations**: Order creation and verification require verified Firebase Authentication contexts (`request.auth.uid`).
3. **Double Verification**: Payments are verified directly with Cashfree's server-side API before granting premium access or crediting referral rewards.
4. **HMAC-SHA256 Webhook Verification**: Asynchronous webhook events require valid `x-webhook-signature` and `x-webhook-timestamp` headers verified using timing-safe comparisons.
5. **Idempotent Fulfillment**: The shared payment processor (`processSuccessfulPayment`) uses Firestore transactions to prevent double-crediting or duplicate referral ledger entries across retries or webhook duplicates.
6. **Secret Management**: Cashfree App ID and Secret Key are managed via Google Cloud Secret Manager (`defineSecret`) and bound exclusively to functions that require them.

## Cloud Functions

- `createCashfreePremiumOrder` (Callable): Creates an authoritative sandbox order with Cashfree and returns `orderId` and `paymentSessionId`.
- `verifyCashfreePremiumPayment` (Callable): Verifies order status with Cashfree server-to-server and triggers fulfillment.
- `cashfreePaymentWebhook` (HTTPS): Webhook endpoint for Cashfree asynchronous payment event notifications.

## Secret Configuration

To configure sandbox credentials in Google Cloud Secret Manager:

```bash
firebase functions:secrets:set CASHFREE_APP_ID
firebase functions:secrets:set CASHFREE_SECRET_KEY
```

For local emulator testing:
Copy `.env.example` to `.env.local` and provide your test credentials:
```bash
cp .env.example .env.local
```

## Running Tests

Build TypeScript and execute unit & integration tests against the local Firestore emulator:

```bash
npm run build
firebase emulators:exec --only firestore "npm test"
```
