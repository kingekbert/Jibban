package com.example.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.components.EmptyState
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.JalaliDatePickerBottomSheet
import com.example.util.CsvExporter
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
    var editingCategoryBudget by remember { mutableStateOf<CategoryEntity?>(null) }

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
                            onClick = { showDatePickerBottomSheet = true }
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
                            onClick = { showFilterBottomSheet = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "فیلترها"
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val csv = viewModel.exportReportCsv()
                            CsvExporter.shareCsv(context, csv, "jeebban_report.csv")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "خروجی اکسل (CSV)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleFullscreen() }
                    ) {
                        Icon(
                            imageVector = if (state.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (state.isFullscreen) "خروج از تمام‌صفحه" else "تمام‌صفحه"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Day Filter Chip
            if (state.selectedDayTimestamp != null) {
                val dayFormatted = JalaliDate.fromTimestamp(state.selectedDayTimestamp!!).format(includeDayName = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    // 3. نمودار دونات تفکیک دسته‌ها
                    item {
                        val isExpense = state.selectedType != TransactionType.INCOME
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
