package com.example.jobportal.ui.referral

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jobportal.data.model.PaymentStatus
import com.example.jobportal.data.model.PremiumPaymentRecord
import com.example.jobportal.ui.theme.*

/**
 * Premium Membership Access Screen.
 * Explains ₹10 (1000 paise) executive tier benefits, displays payment gateway state,
 * and handles payment verification with clear error and retry states.
 */
@Composable
fun PremiumAccessScreen(
    isPremium: Boolean,
    onInitiatePayment: () -> PremiumPaymentRecord,
    onVerifyPayment: (String, String) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var paymentStep by remember { mutableStateOf<PaymentStep>(if (isPremium) PaymentStep.AlreadyPremium else PaymentStep.Overview) }
    var currentPaymentRecord by remember { mutableStateOf<PremiumPaymentRecord?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("premium_access_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "भर्ती-App Premium",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Executive Career Tier",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_premium_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                when (paymentStep) {
                    PaymentStep.AlreadyPremium -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = EmeraldBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldStatus,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "Premium Active",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = EmeraldText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "You have unlocked executive privileges, verified badge, and priority screening.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = EmeraldText)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }

                    PaymentStep.Overview -> {
                        // Pricing Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "One-Time Membership Fee",
                                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "₹10",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Primary
                                    )
                                )
                                Text(
                                    text = "1000 paise • Lifetime access • No recurring charges",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Outline, fontSize = 11.sp)
                                )
                            }
                        }

                        // Benefits List
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Exclusive Membership Privileges",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )

                            val benefits = listOf(
                                "Verified Executive Candidate badge on all applications",
                                "Unlimited active job applications and direct recruiter contacts",
                                "Priority ATS resume scoring and live ranking breakdown",
                                "Technical interview briefing kits and interviewer profiles",
                                "Qualify referred connections for instant ₹5 referral rewards"
                            )

                            benefits.forEach { benefit ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = EmeraldStatus,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = benefit,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 18.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Security & Gateway Notice
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Signing in does not grant premium. Verified payment transaction required for entitlement.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        // CTA Button
                        Button(
                            onClick = {
                                isProcessing = true
                                errorMessage = null
                                try {
                                    val record = onInitiatePayment()
                                    currentPaymentRecord = record
                                    paymentStep = PaymentStep.PaymentGateway(record)
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: "Failed to initiate payment."
                                } finally {
                                    isProcessing = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("continue_to_payment_button"),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Payment, contentDescription = null)
                                    Text("Continue to Payment (₹10)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is PaymentStep.PaymentGateway -> {
                        val record = (paymentStep as PaymentStep.PaymentGateway).record

                        // Sandbox / Gateway Simulator Banner
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AmberContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = null,
                                        tint = AmberText
                                    )
                                    Text(
                                        text = "Development Payment Sandbox",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = AmberText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No production payment gateway credentials are configured. Verifying in development environment sandbox.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = AmberText, fontSize = 11.sp)
                                )
                            }
                        }

                        // Payment Details Breakdown
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Transaction Ref:", style = MaterialTheme.typography.bodySmall.copy(color = Outline))
                                    Text(record.id, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Amount Due:", style = MaterialTheme.typography.bodySmall.copy(color = Outline))
                                    Text("₹10.00 (1000 paise)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Primary))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Status:", style = MaterialTheme.typography.bodySmall.copy(color = Outline))
                                    Text(record.status.label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AmberText))
                                }
                            }
                        }

                        // Error Banner
                        AnimatedVisibility(visible = errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // Verification Actions
                        Button(
                            onClick = {
                                isProcessing = true
                                errorMessage = null
                                val mockProviderRef = "txn_sandbox_${System.currentTimeMillis()}"
                                val success = onVerifyPayment(record.id, mockProviderRef)
                                isProcessing = false
                                if (success) {
                                    paymentStep = PaymentStep.Success
                                } else {
                                    errorMessage = "Payment verification failed. Please try again."
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldStatus,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("confirm_sandbox_payment_button"),
                            enabled = !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying with Gateway...")
                            } else {
                                Text("Simulate Verified Payment (₹10)", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                paymentStep = PaymentStep.Overview
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel / Change Payment Method")
                        }
                    }

                    PaymentStep.Success -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = EmeraldBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldStatus,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Payment Verified Successfully!",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldText
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Your ₹10 membership is verified. Premium entitlements are now unlocked on your account.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = EmeraldText,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("premium_success_done_button")
                        ) {
                            Text("Start Exploring Premium", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private sealed interface PaymentStep {
    object Overview : PaymentStep
    object AlreadyPremium : PaymentStep
    data class PaymentGateway(val record: PremiumPaymentRecord) : PaymentStep
    object Success : PaymentStep
}
