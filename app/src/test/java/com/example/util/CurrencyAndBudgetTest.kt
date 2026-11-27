package com.example.util

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import com.example.ui.screens.reports.ReportsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyAndBudgetTest {

    @Test
    fun `AmountFormatter formats strictly in Rial without tenfold scaling or duplicate suffix`() {
        assertEquals("۰ ریال", AmountFormatter.format(0L))
        assertEquals("۲۰,۰۰۰,۰۰۰ ریال", AmountFormatter.format(20_000_000L))
        assertEquals("۱۲۵,۴۳۷ ریال", AmountFormatter.format(125_437L))
        assertEquals("۲۰,۰۰۰,۰۰۰", AmountFormatter.format(20_000_000L, includeCurrency = false))
        assertEquals("۲۰,۰۰۰,۰۰۰ ریال", AmountFormatter.formatRial(20_000_000L))
        assertEquals("۲۰,۰۰۰,۰۰۰ ریال", AmountFormatter.formatAmountWithCurrency(20_000_000L))
        assertEquals("- ۵۰۰,۰۰۰ ریال", AmountFormatter.formatWithSign(500_000L, isExpense = true))
        assertEquals("+ ۵۰۰,۰۰۰ ریال", AmountFormatter.formatWithSign(500_000L, isExpense = false))
        assertFalse(AmountFormatter.format(1_000_000L).contains("تومان"))
        assertEquals(1, Regex("ریال").findAll(AmountFormatter.format(1_000_000L)).count())
    }

    @Test
    fun `sanitizeAmountDigits normalizes Persian, Arabic, and English digits without scaling`() {
        assertEquals("20000000", AmountFormatter.sanitizeAmountDigits("۲۰,۰۰۰,۰۰۰"))
        assertEquals("20000000", AmountFormatter.sanitizeAmountDigits("٢٠٬٠٠٠٬٠٠٠ ریال"))
        assertEquals("125437", AmountFormatter.sanitizeAmountDigits("00125,437"))
        assertEquals("0", AmountFormatter.sanitizeAmountDigits("000"))
        assertEquals("", AmountFormatter.sanitizeAmountDigits("   ریال  "))
        assertEquals(
            "123456789012345",
            AmountFormatter.sanitizeAmountDigits("12345678901234567890")
        )
    }

    @Test
    fun `parseAmount preserves exact Rial amounts including non-multiples of ten`() {
        assertEquals(0L, AmountFormatter.parseAmount(""))
        assertEquals(0L, AmountFormatter.parseAmount("۰"))
        assertEquals(20_000_000L, AmountFormatter.parseAmount("۲۰,۰۰۰,۰۰۰ ریال"))
        assertEquals(125_437L, AmountFormatter.parseAmount("۱۲۵,۴۳۷"))
        assertEquals(125_437L, AmountFormatter.parseAmount("١٢٥٬٤٣٧"))
        assertEquals(
            125_437L,
            AmountFormatter.parseAmount(AmountFormatter.format(125_437L))
        )
    }

    @Test
    fun `calculateCategoryBudgetProgress compares Rial budgets directly against Rial expenses`() {
        val foodCategory = CategoryEntity(
            id = 1L,
            name = "خوراک",
            type = CategoryType.EXPENSE,
            color = 0xFFE53935.toInt(),
            icon = "restaurant",
            monthlyBudget = 20_000_000L // 20,000,000 Rials
        )
        val transportCategory = CategoryEntity(
            id = 2L,
            name = "حمل‌ونقل",
            type = CategoryType.EXPENSE,
            color = 0xFF1E88E5.toInt(),
            icon = "directions_car",
            monthlyBudget = 5_000_005L // non-multiple of 10 Rials
        )
        val noBudgetCategory = CategoryEntity(
            id = 3L,
            name = "متفرقه",
            type = CategoryType.EXPENSE,
            color = 0xFF757575.toInt(),
            icon = "more_horiz",
            monthlyBudget = 0L
        )
        val incomeCategory = CategoryEntity(
            id = 4L,
            name = "حقوق",
            type = CategoryType.INCOME,
            color = 0xFF43A047.toInt(),
            icon = "payments",
            monthlyBudget = 0L
        )

        val expenses = mapOf(
            1L to 15_000_000L, // 75% of 20,000,000 Rials
            2L to 6_000_005L,  // 120% of 5,000,005 Rials (over budget)
            3L to 2_500_000L
        )

        val results = ReportsViewModel.calculateCategoryBudgetProgress(
            categories = listOf(foodCategory, transportCategory, noBudgetCategory, incomeCategory),
            categoryExpenses = expenses
        )

        assertEquals(2, results.size)

        val foodProgress = results.first { it.category.id == 1L }
        assertEquals(20_000_000L, foodProgress.budget)
        assertEquals(15_000_000L, foodProgress.spentThisMonth)
        assertEquals(5_000_000L, foodProgress.remaining)
        assertEquals(75f, foodProgress.percentage, 0.01f)
        assertFalse(foodProgress.isOverBudget)

        val transportProgress = results.first { it.category.id == 2L }
        assertEquals(5_000_005L, transportProgress.budget)
        assertEquals(6_000_005L, transportProgress.spentThisMonth)
        assertEquals(0L, transportProgress.remaining)
        assertEquals(120f, transportProgress.percentage, 0.01f)
        assertTrue(transportProgress.isOverBudget)
    }

    @Test
    fun `calculateCategoryBudgetProgress handles exact boundary spent equal to budget`() {
        val billsCategory = CategoryEntity(
            id = 10L,
            name = "قبوض",
            type = CategoryType.EXPENSE,
            color = 0xFF8E24AA.toInt(),
            icon = "receipt",
            monthlyBudget = 3_500_000L
        )

        val results = ReportsViewModel.calculateCategoryBudgetProgress(
            categories = listOf(billsCategory),
            categoryExpenses = mapOf(10L to 3_500_000L)
        )

        assertEquals(1, results.size)
        val progress = results.first()
        assertEquals(100f, progress.percentage, 0.01f)
        assertEquals(0L, progress.remaining)
        assertFalse(progress.isOverBudget)
    }
}
