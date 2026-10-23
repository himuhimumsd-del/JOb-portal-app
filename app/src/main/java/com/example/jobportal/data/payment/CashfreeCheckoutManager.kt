package com.example.jobportal.data.payment

import android.app.Activity
import android.util.Log
import com.cashfree.pg.api.CFPaymentGatewayService
import com.cashfree.pg.core.api.CFSession
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.utils.CFErrorResponse
import com.cashfree.pg.ui.api.CFDropCheckoutPayment

sealed interface CashfreeCheckoutResult {
    data class Success(val orderId: String) : CashfreeCheckoutResult
    data class Failure(val orderId: String, val message: String, val code: String? = null) : CashfreeCheckoutResult
    data class Cancelled(val orderId: String, val message: String) : CashfreeCheckoutResult
}

class CashfreeCheckoutManager {
    private val tag = "CashfreeCheckoutManager"

    fun startCheckout(
        activity: Activity,
        orderId: String,
        paymentSessionId: String,
        onResult: (CashfreeCheckoutResult) -> Unit
    ) {
        try {
            val session = CFSession.CFSessionBuilder()
                .setEnvironment(CFSession.Environment.SANDBOX)
                .setOrderId(orderId)
                .setPaymentSessionID(paymentSessionId)
                .build()

            val payment = CFDropCheckoutPayment.CFDropCheckoutPaymentBuilder()
                .setSession(session)
                .build()

            val gatewayService = CFPaymentGatewayService.getInstance()
            gatewayService.setCheckoutCallback(object : CFCheckoutResponseCallback {
                override fun onPaymentVerify(orderID: String) {
                    Log.d(tag, "Cashfree onPaymentVerify callback received for orderId: $orderID")
                    onResult(CashfreeCheckoutResult.Success(orderID))
                }

                override fun onPaymentFailure(cfErrorResponse: CFErrorResponse, orderID: String) {
                    Log.w(tag, "Cashfree onPaymentFailure callback for orderId: $orderID. Error: ${cfErrorResponse.message}")
                    val msg = cfErrorResponse.message ?: "Payment was not completed"
                    if (msg.contains("cancel", ignoreCase = true) || msg.contains("back", ignoreCase = true)) {
                        onResult(CashfreeCheckoutResult.Cancelled(orderID, msg))
                    } else {
                        onResult(CashfreeCheckoutResult.Failure(orderID, msg, cfErrorResponse.code))
                    }
                }
            })

            gatewayService.doPayment(activity, payment)
        } catch (e: Exception) {
            Log.e(tag, "Failed to launch Cashfree checkout", e)
            onResult(CashfreeCheckoutResult.Failure(orderId, e.localizedMessage ?: "Failed to launch checkout"))
        }
    }
}
