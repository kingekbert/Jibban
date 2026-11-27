package com.example.ui.screens.dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AccountCard
import com.example.ui.components.AppSurfaceCard
import com.example.ui.components.BalanceCard
import com.example.ui.components.EmptyState
import com.example.ui.components.TransactionCard
import com.example.ui.components.rememberAnimatedAmount
import com.example.ui.components.subtleCardBorder
import com.example.ui.theme.expenseColor
import com.example.ui.theme.goldAccent
import com.example.ui.theme.incomeColor
import com.example.ui.theme.transferColor
import com.example.util.AmountFormatter
import com.example.util.DateFormatter

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddTransaction: (String) -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToScanSms: () -> Unit = {},
    onNavigateToImportSms: () -> Unit = {},
    onNavigateToTransfer: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAccountDetails: (Long) -> Unit = {},
    onNavigateToEditTransaction: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val summary by viewModel.uiState.collectAsStateWithLifecycle()
    val recentTransactions = remember(summary.recentTransactions) {
        summary.recentTransactions.take(5)
    }
    val todayJalaliLabel = remember {
        DateFormatter.formatLong(System.currentTimeMillis())
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Header with today's Jalali date
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "جیب‌بان",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = todayJalaliLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = subtleCardBorder()
                ) {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "تنظیمات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }

        // Hero Balance Card
        item {
            BalanceCard(totalBalance = summary.totalBalance)
        }

        // Today Summary Cards (semantic theme colors + hairline borders)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TodaySummaryCard(
                    title = "درآمد امروز",
                    amount = summary.todayIncome,
                    color = MaterialTheme.incomeColor,
                    isIncome = true,
                    modifier = Modifier.weight(1f)
                )
                TodaySummaryCard(
                    title = "هزینه امروز",
                    amount = summary.todayExpense,
                    color = MaterialTheme.expenseColor,
                    isIncome = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Access (5 Direct Actions with cohesive brand palette)
        item {
            AppSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "دسترسی سریع",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickAccessItem(
                            title = "ثبت هزینه",
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            color = MaterialTheme.expenseColor,
                            onClick = { onNavigateToAddTransaction("EXPENSE") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_quick_expense")
                        )
                        QuickAccessItem(
                            title = "ثبت درآمد",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            color = MaterialTheme.incomeColor,
                            onClick = { onNavigateToAddTransaction("INCOME") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_quick_income")
                        )
                        QuickAccessItem(
                            title = "انتقال",
                            icon = Icons.Default.SwapHoriz,
                            color = MaterialTheme.transferColor,
                            onClick = onNavigateToTransfer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_quick_transfer")
                        )
                        QuickAccessItem(
                            title = "اسکن پیامک",
                            icon = Icons.Default.Sms,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToScanSms,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_quick_scan_sms")
                        )
                        QuickAccessItem(
                            title = "ورود پیامک",
                            icon = Icons.Default.ContentPaste,
                            color = MaterialTheme.goldAccent,
                            onClick = onNavigateToImportSms,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_quick_import_sms")
                        )
                    }
                }
            }
        }

        // Accounts Header & Horizontal List
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حساب‌های من",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToAccounts) {
                        Text(
                            text = "مشاهده همه",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (summary.accounts.isEmpty()) {
                    AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "هنوز حسابی تعریف نشده است",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(summary.accounts, key = { it.account.id }) { acc ->
                            AccountCard(
                                accountWithBalance = acc,
                                onClick = { onNavigateToAccountDetails(acc.account.id) }
                            )
                        }
                    }
                }
            }
        }

        // Recent Transactions Header & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تراکنش‌های اخیر",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToTransactions) {
                    Text(
                        text = "مشاهده همه",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item(key = "empty_recent_transactions") {
                EmptyState(
                    title = "هنوز تراکنشی ثبت نشده است",
                    subtitle = "اولین هزینه یا درآمد خود را ثبت کنید.",
                    buttonText = "ثبت اولین تراکنش",
                    onButtonClick = { onNavigateToAddTransaction("EXPENSE") }
                )
            }
        } else {
            items(recentTransactions, key = { it.transaction.id }) { tx ->
                TransactionCard(
                    transactionWithDetails = tx,
                    onClick = { onNavigateToEditTransaction(tx.transaction.id) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(220),
                        fadeOutSpec = tween(180),
                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                )
            }
        }
    }
}

@Composable
fun QuickAccessItem(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "quickAccessScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                lineHeight = 14.sp
            ),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TodaySummaryCard(
    title: String,
    amount: Long,
    color: Color,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val animatedAmount = rememberAnimatedAmount(targetAmount = amount, durationMillis = 480)
                Text(
                    text = AmountFormatter.format(animatedAmount),
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
