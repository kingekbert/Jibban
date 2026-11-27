package com.example.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import com.example.ui.components.AppSurfaceCard
import com.example.ui.components.PersianAmountVisualTransformation
import com.example.ui.components.StatusPill
import com.example.ui.components.getCategoryIcon
import com.example.ui.components.rememberAnimatedProgress
import com.example.ui.theme.expenseColor
import com.example.ui.theme.incomeColor
import com.example.ui.theme.warningColor
import com.example.util.AmountFormatter
import com.example.util.NumberToWords
import com.example.util.toPersianDigits
import java.util.Locale

@Composable
fun FinancialInsightCard(
    insight: FinancialInsight?,
    modifier: Modifier = Modifier
) {
    if (insight == null || (insight.currentMonthExpense == 0L && insight.lastMonthExpense == 0L)) return

    val incomeTint = MaterialTheme.incomeColor
    val expenseTint = MaterialTheme.expenseColor

    AppSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "خلاصه این ماه",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "مقایسه هزینه‌ها با ماه قبل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (insight.expenseDiffPercent != null) {
                val isIncreased = insight.isExpenseIncreased
                val diffAbs = kotlin.math.abs(insight.expenseDiffPercent)
                val diffFormatted = String.format(Locale.US, "%.1f", diffAbs).toPersianDigits()
                val badgeColor = if (isIncreased) expenseTint else incomeTint
                val icon = if (isIncreased) Icons.Default.TrendingUp else Icons.Default.TrendingDown
                val statusText = if (isIncreased) {
                    "نسبت به ماه قبل $diffFormatted٪ بیشتر خرج کرده‌اید."
                } else {
                    "نسبت به ماه قبل $diffFormatted٪ کمتر خرج کرده‌اید."
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.11f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "میانگین روزانه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AmountFormatter.format(insight.dailyAverageExpenseThisMonth),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (insight.topSpendingCategoryName != null && insight.topSpendingCategoryPercent > 0) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "بیشترین خرج ماه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${insight.topSpendingCategoryName} (${String.format(Locale.US, "%.0f", insight.topSpendingCategoryPercent).toPersianDigits()}٪)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBudgetSection(
    budgetList: List<CategoryBudgetProgress>,
    allCategories: List<CategoryEntity>,
    onSetBudgetClick: (CategoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val expenseTint = MaterialTheme.expenseColor
    val incomeTint = MaterialTheme.incomeColor
    val warnTint = MaterialTheme.warningColor

    AppSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "سقف بودجه ماهانه",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "کنترل سقف هزینه هر دسته در ماه جاری",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (budgetList.isEmpty()) {
                Text(
                    text = "هنوز سقف بودجه‌ای تنظیم نشده است. برای هر دسته می‌توانید سقف ماهانه تعیین کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                budgetList.forEach { item ->
                    val progressFraction = (item.percentage / 100f).coerceIn(0f, 1f)
                    val progressColor = when {
                        item.percentage >= 100f -> expenseTint
                        item.percentage >= 75f -> warnTint
                        else -> incomeTint
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { onSetBudgetClick(item.category) }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(item.category.color).copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(item.category.icon),
                                        contentDescription = null,
                                        tint = Color(item.category.color),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.category.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "${AmountFormatter.format(item.spentThisMonth)} از ${AmountFormatter.format(item.budget)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val animatedBudgetProgress = rememberAnimatedProgress(progressFraction)
                        LinearProgressIndicator(
                            progress = { animatedBudgetProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.isOverBudget) {
                                StatusPill(
                                    text = "عبور از سقف (${String.format(Locale.US, "%.0f", item.percentage).toPersianDigits()}٪)",
                                    icon = Icons.Default.WarningAmber,
                                    tint = expenseTint
                                )
                            } else {
                                Text(
                                    text = "باقیمانده: ${AmountFormatter.format(item.remaining)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = incomeTint,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.0f", item.percentage).toPersianDigits()}٪ مصرف شده",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            val expenseCats = remember(allCategories) {
                allCategories.filter { it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH }
            }
            if (expenseCats.isNotEmpty()) {
                var showCatPickerForBudget by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showCatPickerForBudget = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تنظیم یا تغییر سقف بودجه")
                }

                if (showCatPickerForBudget) {
                    AlertDialog(
                        onDismissRequest = { showCatPickerForBudget = false },
                        title = { Text("انتخاب دسته برای سقف بودجه", fontWeight = FontWeight.Bold) },
                        text = {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(expenseCats, key = { it.id }) { cat ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                showCatPickerForBudget = false
                                                onSetBudgetClick(cat)
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(cat.color).copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getCategoryIcon(cat.icon),
                                                contentDescription = null,
                                                tint = Color(cat.color),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = cat.name, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (cat.monthlyBudget > 0) {
                                            Text(
                                                text = AmountFormatter.format(cat.monthlyBudget),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showCatPickerForBudget = false }) { Text("بستن") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetEditDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var budgetText by remember(category.id, category.monthlyBudget) {
        mutableStateOf(if (category.monthlyBudget > 0L) category.monthlyBudget.toString() else "")
    }
    val parsedBudget = AmountFormatter.parseAmount(budgetText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سقف بودجه: ${category.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "سقف ماهانه این دسته را به ${AmountFormatter.BASE_CURRENCY_LABEL} وارد کنید (برای حذف، خالی بگذارید):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = AmountFormatter.sanitizeAmountDigits(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_edit_input"),
                    label = { Text("سقف ماهانه (${AmountFormatter.BASE_CURRENCY_LABEL})") },
                    placeholder = { Text("مثال: ۲۰,۰۰۰,۰۰۰") },
                    suffix = { Text(AmountFormatter.BASE_CURRENCY_LABEL) },
                    supportingText = if (parsedBudget > 0L) {
                        {
                            Text(
                                text = NumberToWords.convert(parsedBudget, suffix = AmountFormatter.BASE_CURRENCY_LABEL),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else null,
                    visualTransformation = remember { PersianAmountVisualTransformation() },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rial = AmountFormatter.parseAmount(budgetText)
                    onSave(rial)
                    onDismiss()
                },
                modifier = Modifier.testTag("budget_edit_save_button")
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
