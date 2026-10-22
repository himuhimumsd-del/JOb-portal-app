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
import com.example.jobportal.ui.theme.*

/**
 * Enter Referral Code Dialog.
 * Allows new users to enter a referral code during onboarding or from the dashboard.
 * Enforces validation, prevents self-referral, and locks once committed.
 */
@Composable
fun EnterReferralCodeDialog(
    currentUserReferralCode: String,
    initialCode: String = "",
    onApplyReferralCode: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf(initialCode) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isConfirmed by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("enter_referral_code_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Have a Referral Code?",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Attribute your registration",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_referral_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (isConfirmed) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldStatus)
                            Column {
                                Text(
                                    text = "Attribution Saved!",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = EmeraldText,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Your account is linked to code ${codeInput.trim().uppercase()}. This attribution is permanent.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = EmeraldText, fontSize = 11.sp)
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
                } else {
                    Text(
                        text = "Enter the unique referral code from your inviter. When you complete your first verified ₹10 premium membership, your referrer qualifies for a ₹5 reward.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = {
                            codeInput = it.uppercase()
                            validationError = null
                        },
                        label = { Text("Referral Code (e.g. TNX-ABC123)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Tag, contentDescription = null, tint = Primary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("referral_code_input")
                    )

                    AnimatedVisibility(visible = validationError != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = validationError ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Outline,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Notice: Referral attribution cannot be modified or reassigned after submission.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Outline, fontSize = 11.sp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val clean = codeInput.trim().uppercase()
                            if (clean.isBlank()) {
                                validationError = "Please enter a valid referral code."
                                return@Button
                            }
                            if (clean == currentUserReferralCode.trim().uppercase()) {
                                validationError = "You cannot use your own referral code (self-referral is forbidden)."
                                return@Button
                            }

                            isSubmitting = true
                            validationError = null
                            val success = onApplyReferralCode(clean)
                            isSubmitting = false
                            if (success) {
                                isConfirmed = true
                            } else {
                                validationError = "Invalid or already committed referral code."
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
                            .testTag("submit_referral_code_button"),
                        enabled = !isSubmitting && codeInput.isNotBlank()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Apply Referral Code", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
