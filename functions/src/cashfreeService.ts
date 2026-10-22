import * as crypto from "crypto";
import { getCashfreeConfig, CONSTANTS } from "./config";

export interface CashfreeOrderResponse {
  cf_order_id?: string;
  order_id: string;
  order_status: "ACTIVE" | "PAID" | "EXPIRED" | "TERMINATED";
  order_amount: number;
  order_currency: string;
  payment_session_id?: string;
  order_note?: string;
  customer_details?: {
    customer_id: string;
    customer_name?: string;
    customer_email?: string;
    customer_phone?: string;
  };
}

export interface CashfreePaymentItem {
  cf_payment_id: string;
  payment_status: "SUCCESS" | "FAILED" | "PENDING" | "USER_DROPPED" | "CANCELLED";
  payment_amount: number;
  payment_currency: string;
  payment_completion_time?: string;
  payment_message?: string;
}

/**
 * Creates an order in Cashfree Sandbox.
 * Note: The order amount is strictly fixed to ₹10.00 INR on the server.
 */
export async function createCashfreeOrder(
  orderId: string,
  customerId: string,
  customerEmail: string,
  customerPhone?: string
): Promise<CashfreeOrderResponse> {
  const config = getCashfreeConfig();

  if (!config.appId || !config.secretKey) {
    throw new Error(
      "Cashfree credentials missing. Set CASHFREE_APP_ID and CASHFREE_SECRET_KEY secrets."
    );
  }

  const payload = {
    order_id: orderId,
    order_amount: CONSTANTS.PREMIUM_MEMBERSHIP_AMOUNT_RUPEES,
    order_currency: CONSTANTS.CURRENCY_INR,
    customer_details: {
      customer_id: customerId,
      customer_email: customerEmail || "support@talentnexus.app",
      customer_phone: customerPhone || "9999999999",
    },
    order_meta: {
      return_url: `https://talentnexus.app/payment/return?order_id={order_id}`,
    },
    order_note: "TalentNexus Executive Career Tier Premium Membership",
  };

  const response = await fetch(`${config.baseUrl}/orders`, {
    method: "POST",
    headers: {
      "x-client-id": config.appId,
      "x-client-secret": config.secretKey,
      "x-api-version": config.apiVersion,
      "Content-Type": "application/json",
      "Accept": "application/json",
    },
    body: JSON.stringify(payload),
  });

  const responseBody = await response.json();

  if (!response.ok) {
    const errorMsg =
      (responseBody as { message?: string })?.message ||
      `Cashfree Create Order failed with HTTP ${response.status}`;
    throw new Error(errorMsg);
  }

  return responseBody as CashfreeOrderResponse;
}

/**
 * Retrieves the authoritative order state directly from Cashfree Sandbox.
 */
export async function getCashfreeOrder(orderId: string): Promise<CashfreeOrderResponse> {
  const config = getCashfreeConfig();

  if (!config.appId || !config.secretKey) {
    throw new Error("Cashfree credentials missing.");
  }

  const response = await fetch(`${config.baseUrl}/orders/${encodeURIComponent(orderId)}`, {
    method: "GET",
    headers: {
      "x-client-id": config.appId,
      "x-client-secret": config.secretKey,
      "x-api-version": config.apiVersion,
      "Accept": "application/json",
    },
  });

  const responseBody = await response.json();

  if (!response.ok) {
    const errorMsg =
      (responseBody as { message?: string })?.message ||
      `Cashfree Get Order failed with HTTP ${response.status}`;
    throw new Error(errorMsg);
  }

  return responseBody as CashfreeOrderResponse;
}

/**
 * Retrieves payment transactions for an order from Cashfree Sandbox.
 */
export async function getCashfreeOrderPayments(
  orderId: string
): Promise<CashfreePaymentItem[]> {
  const config = getCashfreeConfig();

  if (!config.appId || !config.secretKey) {
    throw new Error("Cashfree credentials missing.");
  }

  const response = await fetch(
    `${config.baseUrl}/orders/${encodeURIComponent(orderId)}/payments`,
    {
      method: "GET",
      headers: {
        "x-client-id": config.appId,
        "x-client-secret": config.secretKey,
        "x-api-version": config.apiVersion,
        "Accept": "application/json",
      },
    }
  );

  const responseBody = await response.json();

  if (!response.ok) {
    const errorMsg =
      (responseBody as { message?: string })?.message ||
      `Cashfree Get Payments failed with HTTP ${response.status}`;
    throw new Error(errorMsg);
  }

  return Array.isArray(responseBody) ? (responseBody as CashfreePaymentItem[]) : [];
}

/**
 * Verifies Cashfree webhook signature using HMAC-SHA256 and constant-time comparison.
 * Cashfree signs (timestamp + rawBody) with the secret key.
 */
export function verifyCashfreeWebhookSignature(
  rawBody: string | Buffer,
  signatureHeader: string | undefined,
  timestampHeader: string | undefined,
  secretKey: string
): boolean {
  if (!signatureHeader || !secretKey) {
    return false;
  }

  try {
    const bodyStr = Buffer.isBuffer(rawBody) ? rawBody.toString("utf8") : rawBody;
    // According to Cashfree official documentation:
    // data to sign = timestamp + rawBody
    const dataToSign = timestampHeader ? `${timestampHeader}${bodyStr}` : bodyStr;

    const computedSignature = crypto
      .createHmac("sha256", secretKey)
      .update(dataToSign)
      .digest("base64");

    const expectedBuffer = Buffer.from(computedSignature, "utf8");
    const receivedBuffer = Buffer.from(signatureHeader, "utf8");

    if (expectedBuffer.length !== receivedBuffer.length) {
      // Fallback check without timestamp in case payload was signed directly
      const fallbackSignature = crypto
        .createHmac("sha256", secretKey)
        .update(bodyStr)
        .digest("base64");
      const fallbackBuffer = Buffer.from(fallbackSignature, "utf8");
      if (fallbackBuffer.length === receivedBuffer.length) {
        return crypto.timingSafeEqual(fallbackBuffer, receivedBuffer);
      }
      return false;
    }

    return crypto.timingSafeEqual(expectedBuffer, receivedBuffer);
  } catch {
    return false;
  }
}
