package com.example.jobportal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jobportal.data.model.UserRole
import com.example.jobportal.ui.theme.CarbonDock

data class DockNavItem(
    val id: String,
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
)

@Composable
fun FloatingBottomDock(
    currentRole: UserRole,
    activeTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = when (currentRole) {
        UserRole.JOB_SEEKER -> listOf(
            DockNavItem("home", "Home", Icons.Default.Home, Icons.Outlined.Home),
            DockNavItem("explore", "Explore", Icons.Default.TravelExplore, Icons.Outlined.TravelExplore),
            DockNavItem("applications", "Applied", Icons.Default.Work, Icons.Outlined.WorkOutline),
            DockNavItem("saved", "Saved", Icons.Default.Bookmark, Icons.Outlined.BookmarkBorder),
            DockNavItem("profile", "Profile", Icons.Default.Person, Icons.Outlined.Person)
        )
        UserRole.EMPLOYER -> listOf(
            DockNavItem("dashboard", "Overview", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
            DockNavItem("manage_jobs", "Jobs", Icons.Default.Cases, Icons.Outlined.Cases),
            DockNavItem("applicants", "Talent", Icons.Default.Group, Icons.Outlined.Group),
            DockNavItem("interviews", "Interviews", Icons.Default.CalendarMonth, Icons.Outlined.CalendarToday),
            DockNavItem("company", "Company", Icons.Default.Apartment, Icons.Outlined.Apartment)
        )
        UserRole.ADMIN -> listOf(
            DockNavItem("dashboard", "Vitals", Icons.Default.Shield, Icons.Outlined.Shield),
            DockNavItem("moderation", "Moderation", Icons.Default.FactCheck, Icons.Outlined.FactCheck),
            DockNavItem("users", "Users", Icons.Default.ManageAccounts, Icons.Outlined.ManageAccounts),
            DockNavItem("taxonomy", "Taxonomy", Icons.Default.Category, Icons.Outlined.Category),
            DockNavItem("reports", "Disputes", Icons.Default.Flag, Icons.Outlined.Flag)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = Color(0x660F172A),
                    ambientColor = Color(0x330F172A)
                ),
            shape = CircleShape,
            color = CarbonDock.copy(alpha = 0.96f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = item.id == activeTab
                    val interactionSource = remember { MutableInteractionSource() }

                    val bgColor = if (isSelected) Color.White else Color.Transparent
                    val contentColor = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onTabSelected(item.id)
                            }
                            .testTag("dock_tab_${item.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.iconSelected else item.iconUnselected,
                                contentDescription = item.label,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
