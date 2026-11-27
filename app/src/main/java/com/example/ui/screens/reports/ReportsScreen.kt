package com.example.ui.screens.reports

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.components.EmptyState
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.JalaliDatePickerBottomSheet
import com.example.ui.screens.transactions.DateFilterPeriod
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.util.CsvExporter
import com.example.util.DateFormatter
import com.example.util.JalaliDate
import com.example.util.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onCategoryClick: ((categoryId: Long, startDate: Long, endDate: Long, accountId: Long?, type: String) -> Unit)? = null,
    onNavigateToAddTransaction: (() -> Unit)? = null,
    onNavigateToScanSms: (() -> Unit)? = null,
    onToggleFullscreen: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var showDatePickerBottomSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var editingCategoryBudget by remember { mutableStateOf<CategoryEntity?>(null) }

    BackHandler(enabled = state.isFullscreen) {
        viewModel.setFullscreen(false)
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
            allowTransfer = false,
            onDismiss = { showFilterBottomSheet = false },
            onApply = { period, customStart, customEnd, accountId, categoryIds, type ->
                viewModel.applyFilters(period, customStart, customEnd, accountId, categoryIds, type)
                showFilterBottomSheet = false
            },
            onReset = {
                viewModel.resetFilters()
                showFilterBottomSheet = false
            }
        )
    }

    Scaffold(
        topBar = {
            if (!state.isFullscreen) {
                TopAppBar(
                    title = { Text("گزارش‌های مالی", fontWeight = FontWeight.Bold) },
                    actions = {
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
                                modifier = Modifier.testTag("reports_calendar_button")
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
                                modifier = Modifier.testTag("reports_filter_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "فیلترها"
                                )
                            }
                        }
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("reports_overflow_button")
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
                                        val csv = viewModel.exportReportCsv()
                                        CsvExporter.shareCsv(context, csv, "jeebban_report.csv")
                                    },
                                    modifier = Modifier.testTag("reports_export_csv_item")
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
                                    modifier = Modifier.testTag("reports_fullscreen_item")
                                )
                            }
                        }
                    }
                )
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
                // Active Filters Chip Bar
                if (state.activeFilterCount > 0) {
                    ReportsActiveFiltersBar(
                        state = state,
                        onClearDay = { viewModel.clearDayFilter() },
                        onClearPeriod = { viewModel.clearPeriodFilter() },
                        onClearAccount = { viewModel.setAccountFilter(null) },
                        onClearCategories = { viewModel.setCategoryFilter(emptySet()) },
                        onClearType = { viewModel.setReportType(null) },
                        onClearAll = { viewModel.resetFilters() }
                    )
                }

                val isCompletelyEmpty = !state.isLoading && state.totalTransactionsCount == 0
                val isFilteredEmpty = !state.isLoading && state.totalTransactionsCount > 0 && state.filteredTransactions.isEmpty() && state.activeFilterCount > 0

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isCompletelyEmpty) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                EmptyState(
                                    icon = Icons.Default.BarChart,
                                    title = "هنوز داده‌ای برای گزارش‌گیری ثبت نشده است",
                                    subtitle = "با ثبت اولین تراکنش یا اسکن پیامک‌های بانکی، نمودارهای هزینه، روند ۶ ماهه و تحلیل مالی شما به‌صورت خودکار ساخته می‌شوند.",
                                    buttonText = if (onNavigateToAddTransaction != null) "ثبت تراکنش جدید" else null,
                                    buttonIcon = Icons.Default.Add,
                                    onButtonClick = onNavigateToAddTransaction,
                                    secondaryButtonText = if (onNavigateToScanSms != null) "اسکن پیامک‌های بانکی" else null,
                                    secondaryButtonIcon = Icons.Default.Sms,
                                    onSecondaryButtonClick = onNavigateToScanSms,
                                    testTag = "reports_empty_state"
                                )
                            }
                        }
                    } else if (isFilteredEmpty) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                EmptyState(
                                    icon = Icons.Default.FilterAltOff,
                                    title = "تراکنشی در این بازه یا فیلتر یافت نشد",
                                    subtitle = "فیلترهای فعال فعلی (بازه زمانی، روز، حساب یا دسته‌بندی) هیچ تراکنشی ندارند. برای مشاهده گزارش‌ها می‌توانید فیلترها را پاک کنید.",
                                    buttonText = "پاک کردن فیلترها",
                                    buttonIcon = Icons.Default.FilterAltOff,
                                    onButtonClick = { viewModel.resetFilters() },
                                    testTag = "reports_filtered_empty_state"
                                )
                            }
                        }
                    } else {
                        // 1. کارت درآمد کل / هزینه کل
                        item {
                            ReportSummaryCard(
                                totalIncome = state.totalIncome,
                                totalExpense = state.totalExpense,
                                netBalance = state.netBalance
                            )
                        }

                        // 2. روند درآمد و هزینه ۶ ماه اخیر
                        item {
                            MonthlyTrendCard(monthlyTrends = state.monthlyTrends)
                        }

                        // 3. نمودار دونات تفکیک دسته‌ها با سوییچ سریع هزینه‌ها/درآمدها
                        item {
                            val isExpense = state.chartType != TransactionType.INCOME
                            val totalForType = if (isExpense) state.totalExpense else state.totalIncome
                            val chartTitle = if (isExpense) "نمودار هزینه‌ها به تفکیک دسته" else "نمودار درآمدها به تفکیک دسته"
                            val emptyText = if (isExpense) "هیچ تراکنش هزینه‌ای در این بازه ثبت نشده است" else "هیچ تراکنش درآمدی در این بازه ثبت نشده است"

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = chartTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        textAlign = TextAlign.Start
                                    )

                                    ReportChartTypeToggle(
                                        isExpenseSelected = isExpense,
                                        onSelectType = { viewModel.setChartType(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                    )

                                    if (state.categorySummaries.isEmpty() || totalForType == 0L) {
                                        EmptyState(
                                            icon = Icons.Default.PieChart,
                                            title = emptyText,
                                            subtitle = if (state.activeFilterCount > 0) {
                                                "می‌توانید فیلترهای فعلی را پاک کنید تا همه داده‌ها نمایش داده شوند."
                                            } else {
                                                "با ثبت تراکنش در این بخش، نمودار تفکیک دسته‌ها نمایش داده می‌شود."
                                            },
                                            buttonText = if (state.activeFilterCount > 0) "پاک کردن فیلترها" else null,
                                            buttonIcon = Icons.Default.FilterAltOff,
                                            onButtonClick = if (state.activeFilterCount > 0) ({ viewModel.resetFilters() }) else null,
                                            compact = true,
                                            testTag = "reports_chart_empty_state"
                                        )
                                    } else {
                                        DonutChart(
                                            categorySummaries = state.categorySummaries,
                                            totalAmount = totalForType,
                                            isExpense = isExpense,
                                            modifier = Modifier.size(200.dp)
                                        )
                                        ChartLegend(categorySummaries = state.categorySummaries)
                                    }
                                }
                            }
                        }

                        // 4. تفکیک دسته‌بندی‌ها (لیست)
                        if (state.categorySummaries.isNotEmpty()) {
                            item {
                                Text(
                                    text = "تفکیک دسته‌بندی‌ها",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            items(state.categorySummaries, key = { it.category.id }) { summary ->
                                CategorySummaryItem(
                                    summary = summary,
                                    onClick = {
                                        onCategoryClick?.invoke(
                                            summary.category.id,
                                            state.currentStartTimestamp,
                                            state.currentEndTimestamp,
                                            state.selectedAccountId,
                                            summary.category.type.name
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // 5. سقف بودجه ماهانه دسته‌ها
                    item {
                        CategoryBudgetSection(
                            budgetList = state.budgetProgressList,
                            allCategories = state.categories,
                            onSetBudgetClick = { editingCategoryBudget = it }
                        )
                    }

                    // 6. هوش مصنوعی و بینش مالی (آخر)
                    if (!isCompletelyEmpty) {
                        item {
                            FinancialInsightCard(insight = state.financialInsight)
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
                    IconButton(
                        onClick = { viewModel.setFullscreen(false) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("reports_exit_fullscreen_button")
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

    editingCategoryBudget?.let { cat ->
        BudgetEditDialog(
            category = cat,
            onDismiss = { editingCategoryBudget = null },
            onSave = { newBudget ->
                viewModel.updateCategoryBudget(cat.id, newBudget)
            }
        )
    }
}

@Composable
private fun ReportChartTypeToggle(
    isExpenseSelected: Boolean,
    onSelectType: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ChartTypeToggleButton(
                label = "هزینه‌ها",
                selected = isExpenseSelected,
                activeColor = ExpenseColor,
                onClick = { onSelectType(TransactionType.EXPENSE) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("reports_chart_toggle_expense")
            )
            ChartTypeToggleButton(
                label = "درآمدها",
                selected = !isExpenseSelected,
                activeColor = IncomeColor,
                onClick = { onSelectType(TransactionType.INCOME) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("reports_chart_toggle_income")
            )
        }
    }
}

@Composable
private fun ChartTypeToggleButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) activeColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReportsActiveFiltersBar(
    state: ReportsUiState,
    onClearDay: () -> Unit,
    onClearPeriod: () -> Unit,
    onClearAccount: () -> Unit,
    onClearCategories: () -> Unit,
    onClearType: () -> Unit,
    onClearAll: () -> Unit
) {
    val selectedAccountName = remember(state.selectedAccountId, state.accounts) {
        state.selectedAccountId?.let { id -> state.accounts.find { it.id == id }?.name }
    }
    val selectedCategoryLabel = remember(state.selectedCategoryIds, state.categories) {
        when {
            state.selectedCategoryIds.isEmpty() -> null
            state.selectedCategoryIds.size == 1 -> {
                val id = state.selectedCategoryIds.first()
                state.categories.find { it.id == id }?.name?.let { "دسته: $it" } ?: "۱ دسته‌بندی"
            }
            else -> "${state.selectedCategoryIds.size.toString().toPersianDigits()} دسته‌بندی"
        }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reports_active_filters_bar"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.selectedDayTimestamp != null) {
            item(key = "filter_day") {
                val dayFormatted = JalaliDate.fromTimestamp(state.selectedDayTimestamp).format(includeDayName = true)
                ActiveFilterChip(
                    label = "روز: $dayFormatted",
                    clearContentDescription = "حذف فیلتر روز",
                    onClick = onClearDay
                )
            }
        }

        if (state.selectedPeriod != DateFilterPeriod.ALL) {
            item(key = "filter_period") {
                val periodText = if (
                    state.selectedPeriod == DateFilterPeriod.CUSTOM &&
                    state.customStartTimestamp != null &&
                    state.customEndTimestamp != null
                ) {
                    val startStr = DateFormatter.formatShort(state.customStartTimestamp)
                    val endStr = DateFormatter.formatShort(state.customEndTimestamp)
                    "بازه: $startStr تا $endStr"
                } else {
                    "بازه: ${state.selectedPeriod.label}"
                }
                ActiveFilterChip(
                    label = periodText,
                    clearContentDescription = "حذف فیلتر بازه زمانی",
                    onClick = onClearPeriod
                )
            }
        }

        if (selectedAccountName != null) {
            item(key = "filter_account") {
                ActiveFilterChip(
                    label = "حساب: $selectedAccountName",
                    clearContentDescription = "حذف فیلتر حساب",
                    onClick = onClearAccount
                )
            }
        }

        if (selectedCategoryLabel != null) {
            item(key = "filter_categories") {
                ActiveFilterChip(
                    label = selectedCategoryLabel,
                    clearContentDescription = "حذف فیلتر دسته‌بندی",
                    onClick = onClearCategories
                )
            }
        }

        if (state.selectedType != null) {
            item(key = "filter_type") {
                val typeLabel = if (state.selectedType == TransactionType.INCOME) "نوع: درآمد" else "نوع: هزینه"
                ActiveFilterChip(
                    label = typeLabel,
                    clearContentDescription = "حذف فیلتر نوع تراکنش",
                    onClick = onClearType
                )
            }
        }

        item(key = "filter_clear_all") {
            InputChip(
                selected = false,
                onClick = onClearAll,
                label = {
                    Text(
                        text = "پاک کردن فیلترها",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FilterAltOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = InputChipDefaults.inputChipColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    labelColor = MaterialTheme.colorScheme.onErrorContainer,
                    leadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                border = null,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("reports_clear_all_filters_chip")
            )
        }
    }
}

@Composable
private fun ActiveFilterChip(
    label: String,
    clearContentDescription: String,
    onClick: () -> Unit
) {
    InputChip(
        selected = true,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = clearContentDescription,
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
