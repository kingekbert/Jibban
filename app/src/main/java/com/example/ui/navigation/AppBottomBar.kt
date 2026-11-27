package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

internal data class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

internal val appBottomNavTabs = listOf(
    BottomNavTab(
        route = Screen.Dashboard.route,
        label = "داشبورد",
        icon = Icons.Default.Home
    ),
    BottomNavTab(
        route = Screen.Transactions.route,
        label = "تراکنش‌ها",
        icon = Icons.Default.ReceiptLong
    ),
    BottomNavTab(
        route = Screen.Reports.route,
        label = "گزارش‌ها",
        icon = Icons.Default.BarChart
    ),
    BottomNavTab(
        route = Screen.Plans.route,
        label = "برنامه‌ها",
        icon = Icons.Default.EventRepeat
    ),
    BottomNavTab(
        route = Screen.More.route,
        label = "بیشتر",
        icon = Icons.Default.GridView
    )
)

internal val bottomNavRoutes: List<String> = appBottomNavTabs.map { it.route }

@Composable
internal fun AppBottomNavigationBar(
    currentRoute: String?,
    navController: NavHostController
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        appBottomNavTabs.forEach { tab ->
            val isSelected = currentRoute == tab.route
            NavigationBarItem(
                modifier = Modifier.testTag("bottom_nav_${tab.route}"),
                selected = isSelected,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = {
                    Text(
                        text = tab.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}
