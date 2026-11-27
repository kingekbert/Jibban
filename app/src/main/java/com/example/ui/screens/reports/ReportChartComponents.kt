package com.example.ui.screens.reports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CategorySummary
import com.example.ui.components.AppSurfaceCard
import com.example.ui.components.getCategoryIcon
import com.example.ui.components.rememberAnimatedAmount
import com.example.ui.components.rememberAnimatedProgress
import com.example.ui.components.subtleCardBorder
import com.example.ui.theme.expenseColor
import com.example.ui.theme.incomeColor
import com.example.util.AmountFormatter
import com.example.util.toPersianDigits
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun ReportSummaryCard(
    totalIncome: Long,
    totalExpense: Long,
    netBalance: Long
) {
    val incomeTint = MaterialTheme.incomeColor
    val expenseTint = MaterialTheme.expenseColor
    val animatedIncome = rememberAnimatedAmount(targetAmount = totalIncome, durationMillis = 520)
    val animatedExpense = rememberAnimatedAmount(targetAmount = totalExpense, durationMillis = 520)
    val animatedNet = rememberAnimatedAmount(targetAmount = netBalance, durationMillis = 520)

    AppSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "درآمد کل",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AmountFormatter.format(animatedIncome),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.ExtraBold,
                        color = incomeTint
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "هزینه کل",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AmountFormatter.format(animatedExpense),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.ExtraBold,
                        color = expenseTint
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تراز دوره",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = AmountFormatter.format(animatedNet),
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (netBalance >= 0) incomeTint else expenseTint
                )
            }
        }
    }
}

@Composable
fun DonutChart(
    categorySummaries: List<CategorySummary>,
    totalAmount: Long,
    isExpense: Boolean,
    modifier: Modifier = Modifier
) {
    val baseStrokeWidth = 26.dp
    val selectedStrokeWidth = 32.dp
    val sweepProgress = remember { Animatable(0f) }
    var selectedSliceIndex by remember(categorySummaries, isExpense) { mutableIntStateOf(-1) }
    val animatedTotal = rememberAnimatedAmount(targetAmount = totalAmount, durationMillis = 560)
    val expenseTint = MaterialTheme.expenseColor
    val incomeTint = MaterialTheme.incomeColor
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    LaunchedEffect(categorySummaries, totalAmount, isExpense) {
        selectedSliceIndex = -1
        sweepProgress.snapTo(0f)
        sweepProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing)
        )
    }

    val selectedSummary = categorySummaries.getOrNull(selectedSliceIndex)

    Box(
        modifier = modifier.pointerInput(categorySummaries, totalAmount) {
            detectTapGestures { offset ->
                if (totalAmount <= 0L || categorySummaries.isEmpty()) return@detectTapGestures
                val cx = size.width / 2f
                val cy = size.height / 2f
                val dx = offset.x - cx
                val dy = offset.y - cy
                val dist = hypot(dx, dy)
                val outerRadius = minOf(size.width, size.height) / 2f
                val innerRadius = outerRadius - selectedStrokeWidth.toPx() * 1.4f

                if (dist < innerRadius) {
                    // Tapping the center resets selection to total
                    selectedSliceIndex = -1
                    return@detectTapGestures
                }

                // Compute angle starting from -90 deg (12 o'clock) clockwise in [0, 360)
                var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                if (deg < 0f) deg += 360f

                var acc = 0f
                var tappedIdx = -1
                categorySummaries.forEachIndexed { idx, summary ->
                    val span = (summary.percentage / 100f) * 360f
                    if (deg >= acc && deg <= acc + span) {
                        tappedIdx = idx
                    }
                    acc += span
                }
                selectedSliceIndex = if (tappedIdx == selectedSliceIndex) -1 else tappedIdx
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = baseStrokeWidth.toPx()
            val activeStrokePx = selectedStrokeWidth.toPx()
            var startAngle = -90f
            val progress = sweepProgress.value
            val gapDegrees = if (categorySummaries.size > 1) 2.5f else 0f

            if (totalAmount <= 0L || categorySummaries.isEmpty()) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                )
            } else {
                drawArc(
                    color = trackColor.copy(alpha = 0.5f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                )
                categorySummaries.forEachIndexed { idx, summary ->
                    val rawSweep = (summary.percentage / 100f) * 360f * progress
                    val drawSweep = (rawSweep - gapDegrees).coerceAtLeast(0.8f)
                    if (rawSweep > 0f) {
                        val isSelected = idx == selectedSliceIndex
                        val dimAlpha = if (selectedSliceIndex == -1 || isSelected) 1f else 0.35f
                        drawArc(
                            color = Color(summary.category.color).copy(alpha = dimAlpha),
                            startAngle = startAngle + (gapDegrees / 2f),
                            sweepAngle = drawSweep,
                            useCenter = false,
                            style = Stroke(
                                width = if (isSelected) activeStrokePx else strokePx,
                                cap = StrokeCap.Butt
                            )
                        )
                        startAngle += rawSweep
                    }
                }
            }
        }

        // Center interactive display: shows selected category or total
        AnimatedContent(
            targetState = selectedSummary,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "donutCenter"
        ) { active ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 36.dp)
            ) {
                if (active != null) {
                    Text(
                        text = active.category.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(active.category.color),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = AmountFormatter.format(active.totalAmount, includeCurrency = false),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%.1f٪ از کل".format(active.percentage).toPersianDigits(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = if (isExpense) "مجموع هزینه" else "مجموع درآمد",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AmountFormatter.format(animatedTotal, includeCurrency = false),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isExpense) expenseTint else incomeTint
                    )
                    Text(
                        text = "ریال",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChartLegend(
    categorySummaries: List<CategorySummary>,
    modifier: Modifier = Modifier
) {
    val topItems = categorySummaries.take(6)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val rows = topItems.chunked(2)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { summary ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(summary.category.color))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = summary.category.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "%.1f٪".format(summary.percentage).toPersianDigits(),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MonthlyTrendCard(
    monthlyTrends: List<MonthlyTrend>,
    modifier: Modifier = Modifier
) {
    if (monthlyTrends.isEmpty()) return

    val incomeTint = MaterialTheme.incomeColor
    val expenseTint = MaterialTheme.expenseColor
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val maxAmount = monthlyTrends.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0L
    val barGrowth = remember { Animatable(0f) }
    var selectedMonthIndex by remember(monthlyTrends) { mutableIntStateOf(-1) }

    LaunchedEffect(monthlyTrends) {
        selectedMonthIndex = -1
        barGrowth.snapTo(0f)
        barGrowth.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "روند ۶ ماه اخیر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "برای جزئیات، روی هر ماه بزنید",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(incomeTint)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "درآمد",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(expenseTint)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "هزینه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Interactive tapped-month detail pill
            val selectedTrend = monthlyTrends.getOrNull(selectedMonthIndex)
            AnimatedVisibility(
                visible = selectedTrend != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedTrend?.let { trend ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = subtleCardBorder(0.55f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trend.monthName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "درآمد: ${AmountFormatter.format(trend.income)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = incomeTint,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "هزینه: ${AmountFormatter.format(trend.expense)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                    color = expenseTint,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Bar Chart Area with subtle horizontal dashed gridlines
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .padding(top = 6.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(98.dp)
                ) {
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    listOf(0.05f, 0.5f, 0.98f).forEach { fraction ->
                        val y = size.height * fraction
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 0.9.dp.toPx(),
                            pathEffect = if (fraction < 0.9f) dashEffect else null
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyTrends.forEachIndexed { idx, trend ->
                        val incomeRatio = if (maxAmount > 0L) (trend.income.toFloat() / maxAmount).coerceIn(0.04f, 1f) else 0.04f
                        val expenseRatio = if (maxAmount > 0L) (trend.expense.toFloat() / maxAmount).coerceIn(0.04f, 1f) else 0.04f
                        val hasIncome = trend.income > 0L
                        val hasExpense = trend.expense > 0L
                        val isSelected = idx == selectedMonthIndex
                        val dimFactor = if (selectedMonthIndex == -1 || isSelected) 1f else 0.45f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    selectedMonthIndex = if (isSelected) -1 else idx
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier
                                    .height(96.dp)
                                    .padding(horizontal = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(
                                            if (hasIncome) (incomeRatio * barGrowth.value).coerceAtLeast(0.03f) else 0.03f
                                        )
                                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                        .background(
                                            if (hasIncome) incomeTint.copy(alpha = dimFactor)
                                            else incomeTint.copy(alpha = 0.14f)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(
                                            if (hasExpense) (expenseRatio * barGrowth.value).coerceAtLeast(0.03f) else 0.03f
                                        )
                                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                        .background(
                                            if (hasExpense) expenseTint.copy(alpha = dimFactor)
                                            else expenseTint.copy(alpha = 0.14f)
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = trend.monthName,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                maxLines = 1,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySummaryItem(
    summary: CategorySummary,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryColor = Color(summary.category.color)
    val percentageText = "%.1f".format(summary.percentage).toPersianDigits() + "٪"
    val countText = "${summary.transactionCount}".toPersianDigits() + " تراکنش"

    AppSurfaceCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(summary.category.icon),
                        contentDescription = summary.category.name,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = AmountFormatter.format(summary.totalAmount),
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = percentageText,
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = categoryColor
                    )
                }
            }

            val animatedPercent = rememberAnimatedProgress(summary.percentage / 100f)
            LinearProgressIndicator(
                progress = { animatedPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = categoryColor,
                trackColor = categoryColor.copy(alpha = 0.16f)
            )
        }
    }
}
