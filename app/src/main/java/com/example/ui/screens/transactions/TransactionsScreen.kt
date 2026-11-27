package com.example.ui.screens.transactions

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import com.example.ui.components.JalaliDatePickerBottomSheet
import com.example.util.JalaliDate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.local.relation.TransactionWithDetails
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CardDefaults
import com.example.ui.components.EmptyState
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.TransactionCard
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.TransferColor
import com.example.util.AmountFormatter
import com.example.util.CsvExporter
import com.example.util.DateFormatter
import com.example.util.toPersianDigits
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onNavigateToEditTransaction: (Long) -> Unit,
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToScanSms: (() -> Unit)? = null,
    onToggleFullscreen: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var showSortBottomSheet by remember { mutableStateOf(false) }
    var showDatePickerBottomSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = state.isFullscreen) {
        viewModel.toggleFullscreen()
    }

    LaunchedEffect(state.isFullscreen) {
        onToggleFullscreen(state.isFullscreen)
    }

    DisposableEffect(Unit) {
        onDispose {
            onToggleFullscreen(false)
        }
    }

    if (showDatePickerBottomSheet) {
        JalaliDatePickerBottomSheet(
            initialTimestamp = state.selectedDayTimestamp ?: System.currentTimeMillis(),
            onDateSelected = { timestamp ->
                viewModel.setDayFilter(timestamp)
                showDatePickerBottomSheet = false
            },
            onDismiss = { showDatePickerBottomSheet = false }
        )
    }

    if (showFilterBottomSheet) {
        FilterBottomSheet(
            period = state.selectedPeriod,
            customStart = state.customStartTimestamp,
            customEnd = state.customEndTimestamp,
            accountId = state.selectedAccountId,
            categoryIds = state.selectedCategoryIds,
            transactionType = state.selectedType,
            accounts = state.accounts,
            categories = state.categories,
            onlyBookmarked = state.onlyBookmarked,
            allowTransfer = true,
            onDismiss = { showFilterBottomSheet = false },
            onApply = { period, customStart, customEnd, accountId, categoryIds, type, onlyBookmarked ->
                viewModel.applyFilters(period, customStart, customEnd, accountId, categoryIds, type, onlyBookmarked)
                showFilterBottomSheet = false
            },
            onReset = {
                viewModel.resetFilters()
                showFilterBottomSheet = false
            }
        )
    }

    if (showSortBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortBottomSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "مرتب‌سازی تراکنش‌ها",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TransactionSortOrder.entries.forEach { order ->
                    val isSelected = state.selectedSortOrder == order
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                            .clickable {
                                viewModel.setSortOrder(order)
                                showSortBottomSheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = order.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("حذف تراکنش", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف این تراکنش اطمینان دارید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val tx = transactionToDelete
                        if (tx != null) {
                            viewModel.deleteTransaction(tx)
                            transactionToDelete = null

                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "تراکنش حذف شد",
                                    actionLabel = "بازگردانی",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoDelete()
                                }
                            }
                        }
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (!state.isFullscreen) {
                TopAppBar(
                    title = {
                        if (state.isSearchActive) {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("جستجو در توضیحات و دسته...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            Text("تراکنش‌ها", fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.setSearchActive(!state.isSearchActive) },
                            modifier = Modifier.testTag("transactions_search_button")
                        ) {
                            Icon(
                                imageVector = if (state.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (state.isSearchActive) "بستن جستجو" else "جستجو"
                            )
                        }
                        if (!state.isSearchActive) {
                            BadgedBox(
                                badge = {
                                    if (state.selectedDayTimestamp != null) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(8.dp)
                                        )
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = { showDatePickerBottomSheet = true },
                                    modifier = Modifier.testTag("transactions_calendar_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = "تقویم روزانه",
                                        tint = if (state.selectedDayTimestamp != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            BadgedBox(
                                badge = {
                                    if (state.activeFilterCount > 0) {
                                        Badge {
                                            Text(state.activeFilterCount.toString().toPersianDigits())
                                        }
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = { showFilterBottomSheet = true },
                                    modifier = Modifier.testTag("transactions_filter_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "فیلترها"
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showSortBottomSheet = true },
                                modifier = Modifier.testTag("transactions_sort_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "مرتب‌سازی",
                                    tint = if (state.selectedSortOrder != TransactionSortOrder.DATE_DESC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { showOverflowMenu = true },
                                    modifier = Modifier.testTag("transactions_overflow_button")
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
                                        text = { Text("خروجی اکسل (CSV)") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.FileDownload,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            val csv = viewModel.exportTransactionsCsv()
                                            CsvExporter.shareCsv(context, csv, "jeebban_transactions.csv")
                                        },
                                        modifier = Modifier.testTag("transactions_export_csv_item")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("حالت تمام‌صفحه") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Fullscreen,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.toggleFullscreen()
                                        },
                                        modifier = Modifier.testTag("transactions_fullscreen_item")
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!state.isFullscreen) {
                FloatingActionButton(
                    onClick = onNavigateToAddTransaction,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "ثبت تراکنش"
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (state.isFullscreen) PaddingValues(0.dp) else innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Active Filter Chips
                if (state.selectedDayTimestamp != null || state.onlyBookmarked) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.selectedDayTimestamp != null) {
                            val dayFormatted = JalaliDate.fromTimestamp(state.selectedDayTimestamp!!).format(includeDayName = true)
                            InputChip(
                                selected = true,
                                onClick = { viewModel.clearDayFilter() },
                                label = {
                                    Text(
                                        text = "فیلتر روز: $dayFormatted",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف فیلتر روز",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        if (state.onlyBookmarked) {
                            InputChip(
                                selected = true,
                                onClick = { viewModel.setOnlyBookmarked(false) },
                                label = {
                                    Text(
                                        text = "فقط نشانه‌دارها",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Bookmark,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف فیلتر نشانه‌دار",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    selectedLabelColor = Color(0xFFB45309),
                                    selectedTrailingIconColor = Color(0xFFB45309)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Grouped Transaction List
                if (state.groupedTransactions.isEmpty() && !state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.hasActiveSearchOrFilters) {
                            EmptyState(
                                icon = Icons.Default.SearchOff,
                                title = "تراکنشی با این فیلترها پیدا نشد",
                                subtitle = "با عبارت جستجو یا فیلترهای فعال فعلی موردی مطابقت ندارد. می‌توانید فیلترها را پاک کنید تا همه تراکنش‌ها نمایش داده شوند.",
                                buttonText = "پاک کردن فیلترها",
                                buttonIcon = Icons.Default.FilterAltOff,
                                onButtonClick = { viewModel.clearSearchAndFilters() },
                                testTag = "transactions_filtered_empty_state"
                            )
                        } else {
                            EmptyState(
                                icon = Icons.Default.ReceiptLong,
                                title = "هنوز تراکنشی ثبت نشده است",
                                subtitle = "برای شروع مدیریت مالی، اولین هزینه یا درآمد خود را ثبت کنید یا پیامک‌های بانکی را برای ثبت خودکار اسکن نمایید.",
                                buttonText = "ثبت تراکنش جدید",
                                buttonIcon = Icons.Default.Add,
                                onButtonClick = onNavigateToAddTransaction,
                                secondaryButtonText = if (onNavigateToScanSms != null) "اسکن پیامک‌های بانکی" else null,
                                secondaryButtonIcon = Icons.Default.Sms,
                                onSecondaryButtonClick = onNavigateToScanSms,
                                testTag = "transactions_empty_state"
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = if (state.isFullscreen) PaddingValues(horizontal = 8.dp, vertical = (4.dp / state.zoomLevel).coerceIn(1.dp, 12.dp)) else PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(if (state.isFullscreen) (4.dp / state.zoomLevel).coerceIn(1.dp, 12.dp) else 12.dp)
                    ) {
                        state.groupedTransactions.forEach { (dateHeader, transactions) ->
                            item(key = dateHeader) {
                                val (dayIncome, dayExpense) = remember(transactions) {
                                    var income = 0L
                                    var expense = 0L
                                    transactions.forEach { item ->
                                        when (item.transaction.type) {
                                            TransactionType.INCOME -> income += item.transaction.amount
                                            TransactionType.EXPENSE -> expense += item.transaction.amount
                                            TransactionType.TRANSFER -> Unit
                                        }
                                    }
                                    income to expense
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(
                                            fadeInSpec = tween(200),
                                            fadeOutSpec = tween(160),
                                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                        .padding(
                                            top = if (state.isFullscreen) (4.dp / state.zoomLevel).coerceIn(1.dp, 8.dp) else 8.dp,
                                            bottom = if (state.isFullscreen) (2.dp / state.zoomLevel).coerceIn(1.dp, 4.dp) else 4.dp
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateHeader,
                                        style = if (state.isFullscreen) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                                        fontSize = if (state.isFullscreen) (13.sp * state.zoomLevel) else TextUnit.Unspecified,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (dayIncome > 0L) {
                                            Text(
                                                text = "+ " + AmountFormatter.format(dayIncome, includeCurrency = false),
                                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                                color = IncomeColor,
                                                fontSize = if (state.isFullscreen) (10.sp * state.zoomLevel) else 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (dayExpense > 0L) {
                                            Text(
                                                text = "- " + AmountFormatter.format(dayExpense, includeCurrency = false),
                                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                                color = ExpenseColor,
                                                fontSize = if (state.isFullscreen) (10.sp * state.zoomLevel) else 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (dayIncome > 0L || dayExpense > 0L) {
                                            Text(
                                                text = "ریال",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = if (state.isFullscreen) (9.sp * state.zoomLevel) else 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            items(transactions, key = { it.transaction.id }) { item ->
                                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                // باز شدن ویرایش
                                                onNavigateToEditTransaction(item.transaction.id)
                                                false // بازگشت کارت به موقعیت اولیه هنگام باز شدن صفحه ویرایش
                                            }
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                // دیالوگ تأیید حذف
                                                transactionToDelete = item.transaction
                                                false // بازگشت کارت به موقعیت اولیه برای نمایش دیالوگ تأیید
                                            }
                                            SwipeToDismissBoxValue.Settled -> false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(220),
                                        fadeOutSpec = tween(180),
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ),
                                    enableDismissFromStartToEnd = true,
                                    enableDismissFromEndToStart = true,
                                    backgroundContent = {
                                        val direction = when {
                                            dismissState.dismissDirection != SwipeToDismissBoxValue.Settled -> dismissState.dismissDirection
                                            dismissState.targetValue != SwipeToDismissBoxValue.Settled -> dismissState.targetValue
                                            else -> SwipeToDismissBoxValue.Settled
                                        }

                                        val isDelete = direction == SwipeToDismissBoxValue.StartToEnd
                                        val isEdit = direction == SwipeToDismissBoxValue.EndToStart

                                        val bgColor = when {
                                            isDelete -> MaterialTheme.colorScheme.errorContainer
                                            isEdit -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                        val contentColor = when {
                                            isDelete -> MaterialTheme.colorScheme.onErrorContainer
                                            isEdit -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> Color.Transparent
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(if (state.isFullscreen) 10.dp else 16.dp))
                                                .background(bgColor)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = when {
                                                isDelete -> AbsoluteAlignment.CenterLeft
                                                isEdit -> AbsoluteAlignment.CenterRight
                                                else -> Alignment.Center
                                            }
                                        ) {
                                            if (isDelete) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "حذف",
                                                        modifier = Modifier.size(24.dp),
                                                        tint = contentColor
                                                    )
                                                    Text(
                                                        text = "حذف",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = contentColor
                                                    )
                                                }
                                            } else if (isEdit) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "ویرایش",
                                                        modifier = Modifier.size(24.dp),
                                                        tint = contentColor
                                                    )
                                                    Text(
                                                        text = "ویرایش",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = contentColor
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    content = {
                                        if (state.isFullscreen) {
                                            CompactTransactionCard(
                                                transactionWithDetails = item,
                                                zoomLevel = state.zoomLevel,
                                                onClick = { onNavigateToEditTransaction(item.transaction.id) },
                                                onToggleBookmark = { viewModel.toggleBookmark(item.transaction) }
                                            )
                                        } else {
                                            TransactionCard(
                                                transactionWithDetails = item,
                                                onClick = { onNavigateToEditTransaction(item.transaction.id) },
                                                onToggleBookmark = { viewModel.toggleBookmark(item.transaction) },
                                                showDate = false
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (state.isFullscreen) {
                Surface(
                    modifier = Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.zoomIn() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "بزرگ‌نمایی",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.zoomOut() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "کوچک‌نمایی",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleFullscreen() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "خروج از تمام‌صفحه",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTransactionCard(
    transactionWithDetails: TransactionWithDetails,
    zoomLevel: Float = 1.0f,
    onClick: () -> Unit = {},
    onToggleBookmark: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tx = transactionWithDetails.transaction
    val (amountColor, amountPrefix, defaultIcon) = when (tx.type) {
        TransactionType.EXPENSE -> Triple(ExpenseColor, "-", transactionWithDetails.categoryIcon ?: "more_horiz")
        TransactionType.INCOME -> Triple(IncomeColor, "+", transactionWithDetails.categoryIcon ?: "payments")
        TransactionType.TRANSFER -> Triple(TransferColor, "", "swap_horiz")
    }

    val iconColor = transactionWithDetails.categoryColor?.let { Color(it) } ?: amountColor

    val titleText = when (tx.type) {
        TransactionType.EXPENSE, TransactionType.INCOME -> transactionWithDetails.categoryName ?: "بدون دسته‌بندی"
        TransactionType.TRANSFER -> "انتقال بین حساب‌ها"
    }

    val subtitleText = when (tx.type) {
        TransactionType.EXPENSE, TransactionType.INCOME -> transactionWithDetails.accountName ?: ""
        TransactionType.TRANSFER -> "${transactionWithDetails.accountName ?: ""} ← ${transactionWithDetails.toAccountName ?: ""}"
    }

    val cardPadding = (8.dp / zoomLevel).coerceIn(2.dp, 14.dp)
    val iconBoxSize = (32.dp * zoomLevel).coerceIn(18.dp, 56.dp)
    val innerIconSize = (18.dp * zoomLevel).coerceIn(12.dp, 32.dp)
    val titleFontSize = (13.sp * zoomLevel)
    val amountFontSize = (14.sp * zoomLevel)
    val subFontSize = (11.sp * zoomLevel)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(iconBoxSize)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (tx.type == TransactionType.TRANSFER) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "انتقال",
                        tint = TransferColor,
                        modifier = Modifier.size(innerIconSize)
                    )
                } else {
                    Icon(
                        imageVector = getCategoryIcon(defaultIcon),
                        contentDescription = titleText,
                        tint = iconColor,
                        modifier = Modifier.size(innerIconSize)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (tx.note.isNullOrBlank()) subtitleText else "$subtitleText (${tx.note})",
                    fontSize = subFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$amountPrefix ${AmountFormatter.format(tx.amount)}",
                    fontSize = amountFontSize,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                if (onToggleBookmark != null) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size((24 * zoomLevel).coerceIn(22f, 36f).dp)
                    ) {
                        Icon(
                            imageVector = if (tx.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (tx.isBookmarked) "حذف نشان" else "نشانه‌گذاری",
                            tint = if (tx.isBookmarked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size((15 * zoomLevel).coerceIn(15f, 22f).dp)
                        )
                    }
                } else if (tx.isBookmarked) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = "نشانه‌دار",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size((14 * zoomLevel).coerceIn(14f, 20f).dp)
                    )
                }
            }
        }
    }
}
