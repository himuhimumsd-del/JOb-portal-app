package com.example.jobportal.ui.referral

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jobportal.data.model.ReferralProfile
import com.example.jobportal.ui.theme.*

/**
 * Redemption Request Dialog.
 * Enforces dual conditions (10 referrals AND ₹50 balance), collects payout details,
 * and records requests for administrative review.
 */
@Composable
fun RedemptionRequestDialog(
    profile: ReferralProfile,
    onSubmitRequest: (Int, String) -> Boolean,
    onDismiss: () -> Unit
) {
    val meetsReferrals = profile.meetsReferralsRequirement
    val meetsBalance = profile.meetsBalanceRequirement
    val isEligible = profile.meetsBothRedemptionRequirements

    var amountInput by remember { mutableStateOf(profile.availableBalance.coerceAtLeast(50).toString()) }
    var payoutDetails by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("redemption_request_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Request Payout",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Redeem Referral Earnings",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_redemption_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (isSuccess) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldStatus)
                                Text(
                                    text = "Redemption Submitted!",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = EmeraldText,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                text = "Your withdrawal request of ₹$amountInput has been received and reserved. It will be reviewed by platform administration.",
                                style = MaterialTheme.typography.bodySmall.copy(color = EmeraldText)
                            )
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
                } else {
                    // Eligibility Status Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isEligible) EmeraldBg else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Dual Redemption Criteria",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEligible) EmeraldText else MaterialTheme.colorScheme.onSurface
                                )
                            )

                            // Criterion 1: 10 referrals
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (meetsReferrals) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (meetsReferrals) EmeraldStatus else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "At least 10 successful referrals (${profile.successfulReferralsCount} / 10)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (meetsReferrals) EmeraldText else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            // Criterion 2: ₹50 balance
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (meetsBalance) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (meetsBalance) EmeraldStatus else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "At least ₹50 available balance (Available: ₹${profile.availableBalance})",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (meetsBalance) EmeraldText else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    if (!isEligible) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = profile.getUnmetRequirementDescription() ?: "Redemption requirements not met.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Back to Dashboard", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Form fields
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Redemption Amount (₹)") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.CurrencyRupee, contentDescription = null, tint = Primary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("redemption_amount_input")
                        )

                        OutlinedTextField(
                            value = payoutDetails,
                            onValueChange = {
                                payoutDetails = it
                                errorMessage = null
                            },
                            label = { Text("Payout Account (UPI ID or Bank IFSC/Acc)") },
                            placeholder = { Text("e.g. name@okhdfcbank") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = Primary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payout_account_input")
                        )

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

                        Text(
                            text = "Payout Note: Withdrawals are recorded as requests for review. Payouts require administrative processing before disbursement.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Outline,
                                fontSize = 11.sp
                            )
                        )

                        Button(
                            onClick = {
                                val amount = amountInput.toIntOrNull() ?: 0
                                if (amount < 50) {
                                    errorMessage = "Minimum withdrawal is ₹50."
                                    return@Button
                                }
                                if (amount > profile.availableBalance) {
                                    errorMessage = "Amount exceeds available balance (₹${profile.availableBalance})."
                                    return@Button
                                }
                                if (payoutDetails.isBlank()) {
                                    errorMessage = "Please enter valid UPI ID or bank account details."
                                    return@Button
                                }

                                isSubmitting = true
                                errorMessage = null
                                val success = onSubmitRequest(amount, payoutDetails.trim())
                                isSubmitting = false
                                if (success) {
                                    isSuccess = true
                                } else {
                                    errorMessage = "Failed to submit withdrawal request."
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_redemption_button"),
                            enabled = !isSubmitting && payoutDetails.isNotBlank()
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Submit Redemption Request", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
