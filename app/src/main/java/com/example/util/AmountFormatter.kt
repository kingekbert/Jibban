package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object AmountFormatter {
    const val BASE_CURRENCY_LABEL = "ریال"
    const val MAX_INPUT_DIGITS = 15

    fun format(amount: Long, includeCurrency: Boolean = true): String {
        val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))
        val formatted = formatter.format(amount).toPersianDigits()
        return if (includeCurrency) {
            "$formatted $BASE_CURRENCY_LABEL"
        } else {
            formatted
        }
    }

    fun formatRial(amount: Long): String = format(amount, includeCurrency = true)

    fun formatAmountWithCurrency(amount: Long): String = format(amount, includeCurrency = true)

    fun formatWithSign(amount: Long, isExpense: Boolean): String {
        val formatted = format(amount, includeCurrency = true)
        return if (isExpense) {
            "- $formatted"
        } else {
            "+ $formatted"
        }
    }

    /**
     * Normalizes user-typed monetary input (English, Persian, or Arabic digits) into clean ASCII digits
     * with no implicit unit scaling (always base unit: Rial) and bounded length to avoid Long overflow.
     */
    fun sanitizeAmountDigits(input: String, maxDigits: Int = MAX_INPUT_DIGITS): String {
        val digits = input.toEnglishDigits().filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val trimmed = digits.trimStart('0').ifEmpty { "0" }
        return trimmed.take(maxDigits.coerceIn(1, 18))
    }

    fun parseAmount(input: String): Long {
        val englishDigits = input.toEnglishDigits().filter { it.isDigit() }.take(18)
        return englishDigits.toLongOrNull() ?: 0L
    }
}

fun String.toEnglishDigits(): String {
    val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val sb = StringBuilder()
    for (ch in this) {
        val pIndex = persianDigits.indexOf(ch)
        if (pIndex != -1) {
            sb.append(englishDigits[pIndex])
        } else {
            val aIndex = arabicDigits.indexOf(ch)
            if (aIndex != -1) {
                sb.append(englishDigits[aIndex])
            } else if (ch in '0'..'9') {
                sb.append(ch)
            }
        }
    }
    return sb.toString()
}
