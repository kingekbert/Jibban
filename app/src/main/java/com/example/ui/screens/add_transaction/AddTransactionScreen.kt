package com.example.ui.screens.add_transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TransactionType
import com.example.ui.components.AmountInput
import com.example.ui.components.CategoryPicker
import com.example.ui.components.FinTrackTimePickerDialog
import com.example.ui.components.JalaliDatePickerDialog
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.TransferColor
import com.example.util.DateFormatter
import com.example.util.toPersianDigits

private const val QUICK_CATEGORY_VISIBLE_LIMIT = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAllCategories by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val targetActiveColor = when (state.type) {
        TransactionType.EXPENSE -> ExpenseColor
        TransactionType.INCOME -> IncomeColor
        TransactionType.TRANSFER -> TransferColor
    }
    val activeColor by animateColorAsState(
        targetValue = targetActiveColor,
        animationSpec = tween(240),
        label = "addTxActiveColor"
    )

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialTimestamp = state.dateTimestamp,
            onDateSelected = { timestamp ->
                viewModel.setDateTimestamp(timestamp)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        FinTrackTimePickerDialog(
            initialHour = state.selectedHour,
            initialMinute = state.selectedMinute,
            onTimeSelected = { hour, minute ->
                viewModel.setTime(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateBack()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ثبت تراکنش جدید", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    if (state.type == TransactionType.EXPENSE) {
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("add_transaction_more_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "گزینه‌های بیشتر"
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (state.isGroupExpenseEnabled) "لغو هزینه مشترک (دنگ)"
                                            else "ثبت به عنوان هزینه مشترک (دنگ)"
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = if (state.isGroupExpenseEnabled) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    },
                                    trailingIcon = if (state.isGroupExpenseEnabled) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "فعال",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.setGroupExpenseEnabled(!state.isGroupExpenseEnabled)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                tonalElevation = 3.dp,
                shadowElevation = 6.dp
            ) {
                Button(
                    onClick = { viewModel.saveTransaction() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(52.dp)
                        .testTag("save_transaction_button"),
                    enabled = !state.isLoading && !state.isSuccess,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "ثبت")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ثبت تراکنش",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Compact Transaction Type Selector
            TransactionTypeSegmentedSelector(
                selectedType = state.type,
                onTypeSelected = {
                    showAllCategories = false
                    viewModel.setTransactionType(it)
                }
            )

            // 2. Prominent Amount Input (auto-focused for instant entry)
            AmountInput(
                amount = state.amount,
                onAmountChange = { viewModel.setAmount(it) },
                color = activeColor,
                autoFocus = true,
                modifier = Modifier.testTag("transaction_amount_input")
            )

            // 3. Smart Account Selector (immediately visible right under amount)
            if (state.type == TransactionType.TRANSFER) {
                TransferAccountsCard(
                    accounts = state.accounts,
                    selectedFromAccount = state.selectedAccount,
                    selectedToAccount = state.selectedToAccount,
                    onFromAccountSelected = { viewModel.setSelectedAccount(it) },
                    onToAccountSelected = { viewModel.setSelectedToAccount(it) },
                    onSwapAccounts = { viewModel.swapTransferAccounts() }
                )
            } else {
                QuickAccountSelector(
                    accounts = state.accounts,
                    selectedAccount = state.selectedAccount,
                    isLastUsedDefault = state.isLastUsedAccountDefault,
                    onAccountSelected = { viewModel.setSelectedAccount(it) }
                )
            }

            // 4. Compact Category Picker (top 8 visible + expandable toggle)
            if (state.type != TransactionType.TRANSFER) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "دسته‌بندی",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.categories.size > QUICK_CATEGORY_VISIBLE_LIMIT) {
                            TextButton(
                                onClick = { showAllCategories = !showAllCategories },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.testTag("toggle_all_categories_button")
                            ) {
                                Text(
                                    text = if (showAllCategories) {
                                        "نمایش کمتر"
                                    } else {
                                        "همه دسته‌ها (${state.categories.size.toPersianDigits()})"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Icon(
                                    imageVector = if (showAllCategories) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    CategoryPicker(
                        categories = state.categories,
                        selectedCategoryId = state.selectedCategory?.id,
                        onCategorySelected = { viewModel.setSelectedCategory(it) },
                        maxVisibleCount = if (showAllCategories) null else QUICK_CATEGORY_VISIBLE_LIMIT,
                        prioritizeSelected = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // 5. Compact Optional Details (Date/Time + Note + Collapsible Group Expense)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .weight(1.3f)
                        .clickable { showDatePicker = true }
                        .testTag("transaction_date_picker"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "تاریخ",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateFormatter.formatMedium(state.dateTimestamp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePicker = true }
                        .testTag("transaction_time_picker"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "ساعت",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = DateFormatter.formatTime(state.selectedHour, state.selectedMinute),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = { viewModel.setNote(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_note_input"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                placeholder = { Text("توضیحات اختیاری (مثال: ناهار با دوستان)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            AnimatedVisibility(
                visible = state.type == TransactionType.EXPENSE && state.isGroupExpenseEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                GroupExpenseSection(
                    state = state,
                    onToggleEnabled = { viewModel.setGroupExpenseEnabled(it) },
                    onSelectShareMode = { viewModel.setGroupShareMode(it) },
                    onAddPerson = { viewModel.addGroupPerson(it) },
                    onRemovePerson = { viewModel.removeGroupPerson(it) },
                    onUpdatePersonAmount = { id, amount -> viewModel.updateGroupPersonAmount(id, amount) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
