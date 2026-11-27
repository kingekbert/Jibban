package com.example.ui.screens.savings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SavingsEntryType
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.relation.SavingsGoalWithDetails
import com.example.ui.components.EmptyState
import com.example.ui.components.rememberAnimatedAmount
import com.example.ui.components.rememberAnimatedProgress
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.util.AmountFormatter
import com.example.util.DateFormatter
import com.example.util.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    viewModel: SavingsGoalsViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Long) -> Unit = {},
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeGoalForAction by remember { mutableStateOf<Pair<SavingsGoalWithDetails, GoalActionType>?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("صندوق‌ها و اهداف پس‌انداز", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "بازگشت"
                                )
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "هدف پس‌انداز جدید")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (state.goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "هنوز هدف پس‌اندازی ثبت نکرده‌اید",
                    icon = Icons.Default.Savings,
                    buttonText = "افزودن اولین هدف پس‌انداز",
                    onButtonClick = onNavigateToAdd
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Stats Card (now scrolls naturally with the goals list)
                item(key = "savings_header_stats") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "مجموع پس‌انداز فعلی",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val animatedSaved = rememberAnimatedAmount(targetAmount = state.totalCurrentSaved, durationMillis = 520)
                                    Text(
                                        text = AmountFormatter.format(animatedSaved),
                                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "از کل هدف",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = AmountFormatter.format(state.totalTargetAmount),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val animatedOverallProgress = rememberAnimatedProgress(state.overallProgress)
                            LinearProgressIndicator(
                                progress = { animatedOverallProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(state.overallProgress * 100).toInt()}٪ از کل مسیر طی شده".toPersianDigits(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${state.activeCount} فعال • ${state.completedCount} محقق‌شده".toPersianDigits(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(state.goals, key = { it.goal.id }) { item ->
                    SavingsGoalCard(
                        item = item,
                        onEdit = { onNavigateToEdit(item.goal.id) },
                        onDeposit = { activeGoalForAction = Pair(item, GoalActionType.DEPOSIT) },
                        onWithdraw = { activeGoalForAction = Pair(item, GoalActionType.WITHDRAW) },
                        onDelete = { goalToDelete = item.goal },
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

    // Deposit / Withdraw Dialog
    activeGoalForAction?.let { (item, actionType) ->
        GoalDepositWithdrawDialog(
            goal = item.goal,
            currentAmount = item.currentAmount,
            accounts = state.accounts,
            actionType = actionType,
            onConfirm = { amount, accountId ->
                if (actionType == GoalActionType.DEPOSIT) {
                    viewModel.deposit(item.goal.id, amount, accountId)
                } else {
                    viewModel.withdraw(item.goal.id, amount, accountId)
                }
                activeGoalForAction = null
            },
            onDismiss = { activeGoalForAction = null }
        )
    }

    // Delete Confirmation Dialog
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("حذف هدف پس‌انداز", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف «${goal.title}» مطمئن هستید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(goal)
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseColor)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun SavingsGoalCard(
    item: SavingsGoalWithDetails,
    onEdit: () -> Unit = {},
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goal = item.goal
    val currentAmount = item.currentAmount
    val isCompleted = item.isCompleted
    var showHistory by remember { mutableStateOf(false) }

    val progress = if (goal.targetAmount > 0) {
        (currentAmount.toFloat() / goal.targetAmount).coerceIn(0f, 1f)
    } else 0f
    val remaining = (goal.targetAmount - currentAmount).coerceAtLeast(0L)
    val accentColor = Color(goal.color)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) IncomeColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Icon + Title + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (goal.targetDate != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "موعد هدف: ${DateFormatter.formatJalali(goal.targetDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "پس‌اندازشده:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AmountFormatter.format(currentAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) IncomeColor else MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "هدف:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AmountFormatter.format(goal.targetAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Progress bar
            val animatedGoalProgress = rememberAnimatedProgress(progress)
            LinearProgressIndicator(
                progress = { animatedGoalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isCompleted) IncomeColor else accentColor,
                trackColor = accentColor.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Text / Completed Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = IncomeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تبریک! این هدف محقق‌شده است 🎉",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "مانده تا هدف: ${AmountFormatter.format(remaining)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}٪".toPersianDigits(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) IncomeColor else accentColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: واریز / برداشت
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDeposit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeColor)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("واریز به پس‌انداز", fontWeight = FontWeight.Bold)
                }

                if (currentAmount > 0) {
                    OutlinedButton(
                        onClick = onWithdraw,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("برداشت از پس‌انداز", color = ExpenseColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // History toggle button
            if (item.entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showHistory = !showHistory }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showHistory) "بستن تاریخچه واریز و برداشت" else "مشاهده تاریخچه واریز و برداشت (${item.entries.size})".toPersianDigits(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(visible = showHistory) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item.entries.sortedByDescending { it.date }.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (entry.type == SavingsEntryType.DEPOSIT) "واریز" else "برداشت",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.type == SavingsEntryType.DEPOSIT) IncomeColor else ExpenseColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = DateFormatter.formatJalali(entry.date),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = (if (entry.type == SavingsEntryType.DEPOSIT) "+ " else "- ") + AmountFormatter.format(entry.amount),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.type == SavingsEntryType.DEPOSIT) IncomeColor else ExpenseColor
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
