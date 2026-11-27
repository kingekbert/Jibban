package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.screens.transactions.DateFilterPeriod
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.TransferColor
import com.example.util.DateFormatter
import com.example.util.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    period: DateFilterPeriod,
    customStart: Long?,
    customEnd: Long?,
    accountId: Long?,
    categoryIds: Set<Long>,
    transactionType: TransactionType?,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onlyBookmarked: Boolean = false,
    allowTransfer: Boolean = true,
    allowBookmark: Boolean = true,
    onDismiss: () -> Unit,
    onApply: (
        period: DateFilterPeriod,
        customStart: Long?,
        customEnd: Long?,
        accountId: Long?,
        categoryIds: Set<Long>,
        type: TransactionType?,
        onlyBookmarked: Boolean
    ) -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var tempPeriod by remember { mutableStateOf(period) }
    var tempCustomStart by remember { mutableStateOf(customStart) }
    var tempCustomEnd by remember { mutableStateOf(customEnd) }
    var tempAccountId by remember { mutableStateOf(accountId) }
    var tempCategoryIds by remember { mutableStateOf(categoryIds) }
    var tempType by remember { mutableStateOf(transactionType) }
    var tempOnlyBookmarked by remember { mutableStateOf(onlyBookmarked) }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    if (showCustomDatePicker) {
        JalaliDateRangePickerDialog(
            onDismiss = { showCustomDatePicker = false },
            onRangeSelected = { start, end ->
                tempCustomStart = start
                tempCustomEnd = end
                tempPeriod = DateFilterPeriod.CUSTOM
                showCustomDatePicker = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Pinned Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "فیلترها",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن"
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            // Single Scrollable Body (no nested vertical scroll!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // 1. بازه زمانی
                Text(
                    text = "بازه زمانی",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tempPeriod == DateFilterPeriod.ALL,
                        onClick = { tempPeriod = DateFilterPeriod.ALL },
                        label = { Text("همه") }
                    )
                    FilterChip(
                        selected = tempPeriod == DateFilterPeriod.TODAY,
                        onClick = { tempPeriod = DateFilterPeriod.TODAY },
                        label = { Text("امروز") }
                    )
                    FilterChip(
                        selected = tempPeriod == DateFilterPeriod.WEEK,
                        onClick = { tempPeriod = DateFilterPeriod.WEEK },
                        label = { Text("این هفته") }
                    )
                    FilterChip(
                        selected = tempPeriod == DateFilterPeriod.MONTH,
                        onClick = { tempPeriod = DateFilterPeriod.MONTH },
                        label = { Text("این ماه") }
                    )
                    FilterChip(
                        selected = tempPeriod == DateFilterPeriod.CUSTOM,
                        onClick = { showCustomDatePicker = true },
                        label = { Text("بازه دلخواه") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                if (tempPeriod == DateFilterPeriod.CUSTOM && tempCustomStart != null && tempCustomEnd != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "از ${DateFormatter.formatShort(tempCustomStart!!)} تا ${DateFormatter.formatShort(tempCustomEnd!!)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. حساب
                Text(
                    text = "حساب",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tempAccountId == null,
                        onClick = { tempAccountId = null },
                        label = { Text("همه حساب‌ها") }
                    )
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = tempAccountId == acc.id,
                            onClick = { tempAccountId = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. نوع تراکنش
                Text(
                    text = "نوع تراکنش",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tempType == null,
                        onClick = { tempType = null },
                        label = { Text("همه") }
                    )
                    FilterChip(
                        selected = tempType == TransactionType.EXPENSE,
                        onClick = { tempType = TransactionType.EXPENSE },
                        label = { Text("هزینه") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseColor.copy(alpha = 0.2f),
                            selectedLabelColor = ExpenseColor
                        )
                    )
                    FilterChip(
                        selected = tempType == TransactionType.INCOME,
                        onClick = { tempType = TransactionType.INCOME },
                        label = { Text("درآمد") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeColor.copy(alpha = 0.2f),
                            selectedLabelColor = IncomeColor
                        )
                    )
                    if (allowTransfer) {
                        FilterChip(
                            selected = tempType == TransactionType.TRANSFER,
                            onClick = { tempType = TransactionType.TRANSFER },
                            label = { Text("انتقال") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TransferColor.copy(alpha = 0.2f),
                                selectedLabelColor = TransferColor
                            )
                        )
                    }
                }

                if (allowBookmark) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "وضعیت نشانه‌گذاری",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FilterChip(
                        selected = tempOnlyBookmarked,
                        onClick = { tempOnlyBookmarked = !tempOnlyBookmarked },
                        label = { Text("فقط تراکنش‌های نشانه‌دار") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (tempOnlyBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFB45309),
                            selectedLeadingIconColor = Color(0xFFD97706)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. دسته‌بندی‌ها (انتخاب تک‌ضرب و چندتایی با چیپ بدون اسکرول تودرتو)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "دسته‌بندی‌ها",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (tempCategoryIds.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${tempCategoryIds.size.toString().toPersianDigits()} انتخاب‌شده",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (tempCategoryIds.isNotEmpty()) {
                        TextButton(
                            onClick = { tempCategoryIds = emptySet() },
                            modifier = Modifier.testTag("filter_categories_clear_all")
                        ) {
                            Text("نمایش همه", fontSize = 12.sp)
                        }
                    }
                }

                Text(
                    text = if (tempCategoryIds.isEmpty()) {
                        "همه دسته‌ها انتخاب هستند؛ برای فیلتر سریع، دسته‌های دلخواه را لمس کنید."
                    } else {
                        "می‌توانید چند دسته را هم‌زمان انتخاب کنید یا روی «همه» بزنید."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("filter_categories_flow"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = tempCategoryIds.isEmpty(),
                        onClick = { tempCategoryIds = emptySet() },
                        label = { Text("همه دسته‌ها") },
                        modifier = Modifier.testTag("filter_category_all")
                    )

                    categories.forEach { cat ->
                        val isSelected = tempCategoryIds.contains(cat.id)
                        val catColor = Color(cat.color)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                tempCategoryIds = if (tempCategoryIds.isEmpty()) {
                                    // Single tap selects ONLY this category
                                    setOf(cat.id)
                                } else if (isSelected) {
                                    tempCategoryIds - cat.id
                                } else {
                                    val next = tempCategoryIds + cat.id
                                    if (next.size == categories.size) emptySet() else next
                                }
                            },
                            label = {
                                Text(
                                    text = cat.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(catColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat.icon),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.18f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("filter_category_chip_${cat.id}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Pinned Bottom Actions: حذف فیلترها | اعمال فیلتر
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        tempPeriod = DateFilterPeriod.ALL
                        tempCustomStart = null
                        tempCustomEnd = null
                        tempAccountId = null
                        tempCategoryIds = emptySet()
                        tempType = null
                        tempOnlyBookmarked = false
                        onReset()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_reset_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حذف فیلترها")
                }

                Button(
                    onClick = {
                        onApply(
                            tempPeriod,
                            tempCustomStart,
                            tempCustomEnd,
                            tempAccountId,
                            tempCategoryIds,
                            tempType,
                            tempOnlyBookmarked
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_apply_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("اعمال فیلتر")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    period: DateFilterPeriod,
    customStart: Long?,
    customEnd: Long?,
    accountId: Long?,
    categoryIds: Set<Long>,
    transactionType: TransactionType?,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    allowTransfer: Boolean = true,
    onDismiss: () -> Unit,
    onApply: (
        period: DateFilterPeriod,
        customStart: Long?,
        customEnd: Long?,
        accountId: Long?,
        categoryIds: Set<Long>,
        type: TransactionType?
    ) -> Unit,
    onReset: () -> Unit
) {
    FilterBottomSheet(
        period = period,
        customStart = customStart,
        customEnd = customEnd,
        accountId = accountId,
        categoryIds = categoryIds,
        transactionType = transactionType,
        accounts = accounts,
        categories = categories,
        onlyBookmarked = false,
        allowTransfer = allowTransfer,
        allowBookmark = false,
        onDismiss = onDismiss,
        onApply = { p, cs, ce, a, c, t, _ ->
            onApply(p, cs, ce, a, c, t)
        },
        onReset = onReset
    )
}
