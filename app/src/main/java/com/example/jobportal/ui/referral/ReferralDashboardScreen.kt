package com.example.jobportal.ui.referral

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobportal.data.model.ReferralItem
import com.example.jobportal.data.model.ReferralProfile
import com.example.jobportal.data.model.ReferralStatus
import com.example.jobportal.ui.theme.*

/**
 * Referral Dashboard Screen.
 * Full-featured executive referral management hub:
 * - Referral code & shareable link with native Android share sheet & clipboard copy
 * - Metric cards for invites, registrations, pending payments, successful conversions, balances
 * - Progress towards dual redemption criteria (10 referrals AND ₹50 balance)
 * - Referral history breakdown with privacy-safe identifiers and status badges
 */
@Composable
fun ReferralDashboardScreen(
    profile: ReferralProfile,
    referrals: List<ReferralItem>,
    onOpenEnterCode: () -> Unit,
    onOpenRedemptionRequest: () -> Unit,
    onOpenRedemptionHistory: () -> Unit,
    onOpenPremiumScreen: () -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    BackHandler {
        onBack()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("referral_dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Navigation Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("back_from_referrals_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Referrals & Rewards",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (profile.isPremium) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AmberContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = null, tint = AmberText, modifier = Modifier.size(16.dp))
                            Text("Premium", style = MaterialTheme.typography.labelSmall.copy(color = AmberText, fontWeight = FontWeight.Bold))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenPremiumScreen,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Get Premium (₹10)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Referral Code & Share Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Your Executive Referral Code",
                                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = profile.referralCode,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Primary
                                )
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("भर्ती-App Referral Code", profile.referralCode)
                                clipboard?.setPrimaryClip(clip)
                                onShowMessage("Referral code copied to clipboard!")
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .testTag("copy_code_button")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = Primary, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Shareable link row
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = profile.shareableLink,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Outline,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("भर्ती-App Referral Link", profile.shareableLink)
                                    clipboard?.setPrimaryClip(clip)
                                    onShowMessage("Referral link copied!")
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Copy Link", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    // Native Android Share Sheet Button
                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Join भर्ती-App Job Portal with my invite code: ${profile.referralCode}\n${profile.shareableLink}\nGet direct recruiter access and priority ATS screening!"
                                )
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share भर्ती-App Referral")
                            context.startActivity(shareIntent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("native_share_referral_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Share Referral Link", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Financial Balances & Redemption Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Available Reward Balance",
                                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = "₹${profile.availableBalance}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldStatus
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Reserved: ₹${profile.reservedBalance}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Outline)
                            )
                            Text(
                                text = "Total Earned: ₹${profile.totalRewardsEarned}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Primary)
                            )
                        }
                    }

                    // Progress towards Dual Redemption Criteria
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Redemption Requirements (Both Required)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            // 1. Successful referrals progress
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "10 Qualifying Referrals",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                    )
                                    Text(
                                        text = "${profile.successfulReferralsCount} / 10",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (profile.meetsReferralsRequirement) EmeraldStatus else Primary
                                        )
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (profile.successfulReferralsCount / 10f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (profile.meetsReferralsRequirement) EmeraldStatus else Primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }

                            // 2. Minimum balance progress
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "₹50 Available Balance",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                    )
                                    Text(
                                        text = "₹${profile.availableBalance} / ₹50",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (profile.meetsBalanceRequirement) EmeraldStatus else Primary
                                        )
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (profile.availableBalance / 50f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (profile.meetsBalanceRequirement) EmeraldStatus else Primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }
                        }
                    }

                    // Action buttons: Request Redemption & View History
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenRedemptionRequest,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (profile.meetsBothRedemptionRequirements) EmeraldStatus else MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = if (profile.meetsBothRedemptionRequirements) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("request_redemption_button")
                        ) {
                            Text(
                                text = if (profile.meetsBothRedemptionRequirements) "Redeem ₹${profile.availableBalance}" else "Check Eligibility",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenRedemptionHistory,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("redemption_history_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Payouts", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Quick Action: Enter Referral Code (if not yet attributed)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Were you invited?",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Enter your inviter's referral code to establish attribution.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenEnterCode,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("enter_code_quick_action")
                    ) {
                        Text("Enter Code", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Referral Funnel Stats Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Referral Pipeline Statistics",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FunnelStatCard(
                        title = "Registered",
                        count = profile.registeredCount.toString(),
                        subtitle = "Installed & signed up",
                        icon = Icons.Default.PersonAdd,
                        modifier = Modifier.weight(1f)
                    )
                    FunnelStatCard(
                        title = "Awaiting Pay",
                        count = profile.pendingPaymentCount.toString(),
                        subtitle = "Pending ₹10 fee",
                        icon = Icons.Default.HourglassTop,
                        modifier = Modifier.weight(1f)
                    )
                    FunnelStatCard(
                        title = "Successful",
                        count = profile.successfulReferralsCount.toString(),
                        subtitle = "₹5 reward credited",
                        icon = Icons.Default.CheckCircle,
                        isHighlighted = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Referral History List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Referral Activity Log",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${referrals.size} entries",
                    style = MaterialTheme.typography.bodySmall.copy(color = Outline)
                )
            }
        }

        if (referrals.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = Outline, modifier = Modifier.size(36.dp))
                        Text(
                            text = "No Referrals Yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Share your link above. When connections register and purchase ₹10 membership, you'll earn ₹5.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                }
            }
        } else {
            items(referrals) { item ->
                val statusBg = when (item.status) {
                    ReferralStatus.REWARD_CREDITED -> EmeraldBg
                    ReferralStatus.PREMIUM_PENDING -> AmberContainer
                    ReferralStatus.REGISTERED -> PrimaryContainer.copy(alpha = 0.15f)
                    ReferralStatus.INELIGIBLE -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
                val statusColor = when (item.status) {
                    ReferralStatus.REWARD_CREDITED -> EmeraldText
                    ReferralStatus.PREMIUM_PENDING -> AmberText
                    ReferralStatus.REGISTERED -> Primary
                    ReferralStatus.INELIGIBLE -> Outline
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(statusBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.referredName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = statusColor)
                                )
                            }

                            Column {
                                Text(
                                    text = item.referredName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Joined: ${item.createdAt}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Outline, fontSize = 10.sp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = item.status.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            if (item.rewardAmount > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "+₹${item.rewardAmount}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldStatus
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FunnelStatCard(
    title: String,
    count: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isHighlighted) EmeraldBg else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlighted) EmeraldStatus else Primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isHighlighted) EmeraldText else MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = Outline, fontSize = 9.sp)
            )
        }
    }
}
