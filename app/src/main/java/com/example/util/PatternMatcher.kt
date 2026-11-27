package com.example.util

import com.example.data.local.entity.ExtractedField
import com.example.data.local.entity.FieldType
import com.example.data.local.entity.SmsPatternEntity
import com.example.data.local.entity.TransactionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.ZoneId

object PatternMatcher {

    private val gson = Gson()
    private val fieldListType = object : TypeToken<List<ExtractedField>>() {}.type

    const val MAX_FUTURE_DRIFT_MS: Long = 24L * 60L * 60L * 1000L
    const val MIN_VALID_SMS_TIMESTAMP_MS: Long = 1546300800000L // 2019-01-01T00:00:00Z
    private const val MIN_VALID_JALALI_YEAR = 1390
    private const val ONE_DAY_MS: Long = 24L * 60L * 60L * 1000L
    private const val THIRTY_DAYS_MS: Long = 30L * ONE_DAY_MS

    fun match(smsText: String, patterns: List<SmsPatternEntity>): ParsedSms? {
        if (smsText.isBlank() || patterns.isEmpty()) return null

        val currentTokens = PatternExtractor.extractParts(smsText)
        if (currentTokens.isEmpty()) return null

        for (pattern in patterns) {
            if (!pattern.isActive) continue

            val rawSavedFields: List<ExtractedField> = try {
                gson.fromJson(pattern.extractedFields, fieldListType)
            } catch (e: Exception) {
                emptyList()
            }
            if (rawSavedFields.isEmpty()) continue

            val savedFields = normalizeSavedFields(rawSavedFields)

            // 1. Structure check: token count must match closely (at most difference of 1)
            if (Math.abs(savedFields.size - currentTokens.size) > 1) continue

            var bankName: String? = pattern.bankName
            var amount: Long? = null
            var txType: TransactionType? = null
            var accountIdent: String? = null
            var dateStr: String? = null
            var yearStr: String? = null
            var monthStr: String? = null
            var dayStr: String? = null
            var timeStr: String? = null
            var hourStr: String? = null
            var minStr: String? = null
            var description: String? = null
            var balance: Long? = null
            var hasMatchingFailure = false

            // 2. Read each fieldType and map corresponding token
            for (i in savedFields.indices) {
                val savedField = savedFields[i]
                val fieldType = savedField.fieldType ?: FieldType.IGNORE
                if (fieldType == FieldType.IGNORE) continue

                // Find corresponding token in currentTokens at position
                val currentToken = currentTokens.getOrNull(i)
                    ?: currentTokens.getOrNull(i - 1)
                    ?: currentTokens.getOrNull(i + 1)

                if (currentToken == null) {
                    hasMatchingFailure = true
                    break
                }

                when (fieldType) {
                    FieldType.BANK_NAME -> {
                        bankName = currentToken.text
                    }
                    FieldType.ACCOUNT_IDENTIFIER -> {
                        accountIdent = convertToEnglish(currentToken.text).replace(Regex("""[^\d\.]"""), "")
                    }
                    FieldType.AMOUNT -> {
                        val cleanedNum = convertToEnglish(currentToken.text)
                            .replace(",", "")
                            .replace("،", "")
                            .replace(".", "")
                            .replace(Regex("""[^\d]"""), "")
                        val parsedAmt = cleanedNum.toLongOrNull()
                        if (parsedAmt != null && parsedAmt > 0L) {
                            amount = parsedAmt
                        } else {
                            // Expected amount here but token is not a number! Structure mismatch
                            hasMatchingFailure = true
                            break
                        }
                    }
                    FieldType.TRANSACTION_TYPE -> {
                        val token = currentToken.text
                        if (token.contains("+") || token.contains("واریز") || token.contains("حقوق")) {
                            txType = TransactionType.INCOME
                        } else if (token.contains("-") || token.contains("برداشت") || token.contains("خرید") || token.contains("قبض") || token.contains("انتقال")) {
                            txType = TransactionType.EXPENSE
                        }
                    }
                    FieldType.SIGN -> {
                        val token = currentToken.text
                        if (token.contains("+") || token.contains("واریز")) {
                            txType = TransactionType.INCOME
                        } else if (token.contains("-") || token.contains("برداشت")) {
                            txType = TransactionType.EXPENSE
                        }
                    }
                    FieldType.DATE_YEAR -> {
                        val digits = convertToEnglish(currentToken.text).filter { it.isDigit() }
                        val rawYear = digits.toIntOrNull()
                        val currentJalaliYear = JalaliDate.today().year
                        val normalizedYear = when {
                            rawYear == null -> null
                            digits.length == 2 -> if (rawYear >= 90) 1300 + rawYear else 1400 + rawYear
                            digits.length == 4 -> rawYear
                            else -> null
                        }
                        if (normalizedYear != null && normalizedYear in MIN_VALID_JALALI_YEAR..(currentJalaliYear + 1)) {
                            yearStr = normalizedYear.toString()
                        } else {
                            hasMatchingFailure = true
                            break
                        }
                    }
                    FieldType.DATE_MONTH -> {
                        val digits = convertToEnglish(currentToken.text).filter { it.isDigit() }
                        val parsedMonth = digits.toIntOrNull()
                        if (parsedMonth != null && parsedMonth in 1..12) {
                            monthStr = parsedMonth.toString()
                        } else {
                            hasMatchingFailure = true
                            break
                        }
                    }
                    FieldType.DATE_DAY -> {
                        val digits = convertToEnglish(currentToken.text).filter { it.isDigit() }
                        val parsedDay = digits.toIntOrNull()
                        if (parsedDay != null && parsedDay in 1..31) {
                            dayStr = parsedDay.toString()
                        } else {
                            hasMatchingFailure = true
                            break
                        }
                    }
                    FieldType.TIME_HOUR -> {
                        val digits = convertToEnglish(currentToken.text).filter { it.isDigit() }
                        val parsedHour = digits.toIntOrNull()
                        if (parsedHour != null && parsedHour in 0..23) {
                            hourStr = parsedHour.toString()
                        } else {
                            hasMatchingFailure = true
                            break
                        }
                    }
                    FieldType.TIME_MINUTE -> {
                        val digits = convertToEnglish(currentToken.text).filter { it.isDigit() }
                        val parsedMin = digits.toIntOrNull()
                        if (parsedMin != null && parsedMin in 0..59) {
                            minStr = parsedMin.toString()
                        } else {
                            hasMatchingFailure = true
                            break
                        }
                    }
                    FieldType.DESCRIPTION -> {
                        description = if (description == null) currentToken.text else "$description ${currentToken.text}"
                    }
                    FieldType.BALANCE -> {
                        val cleanedNum = convertToEnglish(currentToken.text)
                            .replace(",", "")
                            .replace("،", "")
                            .replace(".", "")
                            .replace(Regex("""[^\d]"""), "")
                        val parsedBal = cleanedNum.toLongOrNull()
                        if (parsedBal != null) {
                            balance = parsedBal
                        }
                    }
                    FieldType.IGNORE -> {}
                }
            }

            if (hasMatchingFailure) continue

            val hasDateFields = !dateStr.isNullOrBlank() || !timeStr.isNullOrBlank() ||
                !yearStr.isNullOrBlank() || !monthStr.isNullOrBlank() || !dayStr.isNullOrBlank() ||
                !hourStr.isNullOrBlank() || !minStr.isNullOrBlank()

            // Strict requirements: amount > 0, transaction type non-null, valid date
            if (amount != null && amount > 0L && txType != null && hasDateFields) {
                val nowMs = System.currentTimeMillis()
                val maxValidTs = nowMs + MAX_FUTURE_DRIFT_MS
                val patternTimestamp = parseDateTimeToTimestamp(
                    dateStr = dateStr,
                    timeStr = timeStr,
                    hourStr = hourStr,
                    minStr = minStr,
                    yearStr = yearStr,
                    monthStr = monthStr,
                    dayStr = dayStr
                )
                val textTimestamp = extractDateTimeFromText(smsText, nowMs)
                val patternHasFullDate = !dateStr.isNullOrBlank() ||
                    (!monthStr.isNullOrBlank() && !dayStr.isNullOrBlank())
                val patternTsValid = patternTimestamp != null &&
                    patternTimestamp in MIN_VALID_SMS_TIMESTAMP_MS..maxValidTs

                val finalTimestamp = when {
                    !patternTsValid -> textTimestamp
                    textTimestamp != null && !patternHasFullDate -> textTimestamp
                    textTimestamp != null && kotlin.math.abs(patternTimestamp!! - textTimestamp) > ONE_DAY_MS -> textTimestamp
                    textTimestamp != null && hourStr == null && minStr == null && timeStr.isNullOrBlank() -> textTimestamp
                    else -> patternTimestamp
                }

                if (finalTimestamp != null && finalTimestamp in MIN_VALID_SMS_TIMESTAMP_MS..maxValidTs) {
                    val textBalance = extractBalanceFromText(smsText)
                    val finalBalance = when {
                        balance == null -> textBalance
                        // Guard against token-shift bug where balance accidentally grabbed the transaction amount
                        textBalance != null && balance == amount && textBalance != amount -> textBalance
                        else -> balance
                    }
                    return ParsedSms(
                        bankName = bankName ?: pattern.bankName ?: "بانک",
                        transactionType = txType,
                        amount = amount,
                        date = finalTimestamp,
                        accountIdentifier = accountIdent ?: pattern.accountIdentifier.ifBlank { null },
                        rawDescription = description ?: (if (txType == TransactionType.INCOME) "واریز" else "برداشت"),
                        balance = finalBalance
                    )
                }
            }
        }

        return null
    }

    private val BALANCE_LABEL_REGEX = Regex(
        """(?:(?:مانده|موجودی)(?!\s*قبلی)(?:\s*(?:حساب|فعلی|کل|قابل\s*برداشت|جدید|نهایی|پس\s*از\s*تراکنش))?|باقی\s*مانده|باقیمانده)\s*[:=\-ـ]?\s*(?:\r?\n\s*)?([0-9][0-9,،.]*)(?![/:])\s*(تومان|تومن|ریال|ريال|Rial|IRR)?""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Extracts the bank account remaining balance (in Rials) directly from Persian bank SMS text.
     * Supports labels such as "مانده:", "موجودی فعلی:", "مانده قابل برداشت:", multiline balances,
     * Persian/Arabic/English digits, thousand separators, and automatic Toman-to-Rial conversion.
     */
    fun extractBalanceFromText(smsText: String): Long? {
        if (smsText.isBlank()) return null
        val normalized = convertToEnglish(smsText)
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('\u200C', ' ')

        val matches = BALANCE_LABEL_REGEX.findAll(normalized).toList()
        for (match in matches.asReversed()) {
            val rawNum = match.groupValues[1]
                .replace(",", "")
                .replace("،", "")
                .replace(".", "")
            val parsed = rawNum.toLongOrNull() ?: continue
            if (parsed < 0L || parsed > 100_000_000_000_000L) continue
            val unit = match.groupValues.getOrNull(2).orEmpty().trim()
            val isToman = unit.contains("تومان") || unit.contains("تومن")
            return if (isToman) parsed * 10L else parsed
        }
        return null
    }

    private val FULL_JALALI_DATE_TIME_REGEX = Regex(
        """(?<![\d/])(1[34]\d{2})[/\-](\d{1,2})[/\-](\d{1,2})(?:\s*[-_,،]?\s*(?:ساعت\s*)?(\d{1,2}):(\d{2})(?::\d{2})?)?(?!\d)"""
    )
    private val SHORT_JALALI_DATE_TIME_REGEX = Regex(
        """(?<![\d/])(\d{2})/(\d{1,2})/(\d{1,2})(?:\s*[-_,،]?\s*(?:ساعت\s*)?(\d{1,2}):(\d{2})(?::\d{2})?)?(?!\d)"""
    )
    private val MONTH_DAY_TIME_REGEX = Regex(
        """(?<![\d/\-])(\d{1,2})/(\d{1,2})\s*[-_,،\s]\s*(?:ساعت\s*)?(\d{1,2}):(\d{2})(?::\d{2})?(?!\d)"""
    )
    private val COMPACT_MMDD_TIME_REGEX = Regex(
        """(?<![\d/\-])(0[1-9]|1[0-2])([0-2]\d|3[01])[_\-](\d{1,2}):(\d{2})(?::\d{2})?(?!\d)"""
    )
    private val STANDALONE_TIME_REGEX = Regex(
        """(?<!\d)(\d{1,2}):(\d{2})(?::\d{2})?(?!\d)"""
    )

    /**
     * Directly extracts a validated Jalali date/time timestamp from raw Persian bank SMS text.
     * Rejects impossible calendar dates and future timestamps beyond [MAX_FUTURE_DRIFT_MS].
     */
    fun extractDateTimeFromText(
        smsText: String,
        nowMs: Long = System.currentTimeMillis()
    ): Long? {
        if (smsText.isBlank()) return null
        val normalized = convertToEnglish(smsText)
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('\u200C', ' ')

        val currentJalali = JalaliDate.fromTimestamp(nowMs)
        val maxValidTs = nowMs + MAX_FUTURE_DRIFT_MS

        fun findFallbackTime(): Pair<Int, Int> {
            val tm = STANDALONE_TIME_REGEX.find(normalized) ?: return 0 to 0
            val h = tm.groupValues[1].toIntOrNull() ?: return 0 to 0
            val m = tm.groupValues[2].toIntOrNull() ?: return 0 to 0
            return if (h in 0..23 && m in 0..59) h to m else 0 to 0
        }

        fun buildValidatedTimestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long? {
            if (year !in MIN_VALID_JALALI_YEAR..(currentJalali.year + 1)) return null
            if (month !in 1..12) return null
            val maxDay = JalaliDate.getJalaliMonthLength(year, month)
            if (day !in 1..maxDay) return null
            if (hour !in 0..23 || minute !in 0..59) return null
            return try {
                val (gY, gM, gD) = JalaliDate.jalaliToGregorian(year, month, day)
                val ts = LocalDateTime.of(gY, gM, gD, hour, minute)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                if (ts in MIN_VALID_SMS_TIMESTAMP_MS..maxValidTs) ts else null
            } catch (e: Exception) {
                null
            }
        }

        // 1. Explicit 4-digit Jalali date: YYYY/M/D[-H:MM]
        for (match in FULL_JALALI_DATE_TIME_REGEX.findAll(normalized)) {
            val y = match.groupValues[1].toIntOrNull() ?: continue
            val m = match.groupValues[2].toIntOrNull() ?: continue
            val d = match.groupValues[3].toIntOrNull() ?: continue
            val (h, min) = if (match.groupValues[4].isNotBlank() && match.groupValues[5].isNotBlank()) {
                (match.groupValues[4].toIntOrNull() ?: 0) to (match.groupValues[5].toIntOrNull() ?: 0)
            } else {
                findFallbackTime()
            }
            val ts = buildValidatedTimestamp(y, m, d, h, min)
            if (ts != null) return ts
        }

        // 2. Explicit 2-digit Jalali date: YY/M/D[-H:MM]
        for (match in SHORT_JALALI_DATE_TIME_REGEX.findAll(normalized)) {
            val yy = match.groupValues[1].toIntOrNull() ?: continue
            val y = if (yy >= 90) 1300 + yy else 1400 + yy
            val m = match.groupValues[2].toIntOrNull() ?: continue
            val d = match.groupValues[3].toIntOrNull() ?: continue
            val (h, min) = if (match.groupValues[4].isNotBlank() && match.groupValues[5].isNotBlank()) {
                (match.groupValues[4].toIntOrNull() ?: 0) to (match.groupValues[5].toIntOrNull() ?: 0)
            } else {
                findFallbackTime()
            }
            val ts = buildValidatedTimestamp(y, m, d, h, min)
            if (ts != null) return ts
        }

        // 3. Month/Day + Time: M/D-H:MM (year inferred, rolled back 1 year across Nowruz)
        for (match in MONTH_DAY_TIME_REGEX.findAll(normalized)) {
            val m = match.groupValues[1].toIntOrNull() ?: continue
            val d = match.groupValues[2].toIntOrNull() ?: continue
            val h = match.groupValues[3].toIntOrNull() ?: continue
            val min = match.groupValues[4].toIntOrNull() ?: continue
            val tsCurrent = buildValidatedTimestamp(currentJalali.year, m, d, h, min)
            if (tsCurrent != null) return tsCurrent
            val tsPrev = buildValidatedTimestamp(currentJalali.year - 1, m, d, h, min)
            if (tsPrev != null) return tsPrev
        }

        // 4. Compact MMDD-HH:MM or MMDD_HH:MM (e.g. 0706-21:05)
        for (match in COMPACT_MMDD_TIME_REGEX.findAll(normalized)) {
            val m = match.groupValues[1].toIntOrNull() ?: continue
            val d = match.groupValues[2].toIntOrNull() ?: continue
            val h = match.groupValues[3].toIntOrNull() ?: continue
            val min = match.groupValues[4].toIntOrNull() ?: continue
            val tsCurrent = buildValidatedTimestamp(currentJalali.year, m, d, h, min)
            if (tsCurrent != null) return tsCurrent
            val tsPrev = buildValidatedTimestamp(currentJalali.year - 1, m, d, h, min)
            if (tsPrev != null) return tsPrev
        }

        return null
    }

    /**
     * Resolves a trustworthy SMS timestamp by cross-checking [parsedDate], the explicit date in
     * [smsText], and the carrier [receivedAt] timestamp, while strictly rejecting future dates.
     */
    fun resolveReliableSmsTimestamp(
        parsedDate: Long?,
        smsText: String,
        receivedAt: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        val maxValidTs = nowMs + MAX_FUTURE_DRIFT_MS
        val hasValidReceivedAt = receivedAt in MIN_VALID_SMS_TIMESTAMP_MS..maxValidTs
        val safeReceivedAt = if (hasValidReceivedAt) receivedAt else nowMs
        val textDate = extractDateTimeFromText(smsText, nowMs)
        val validParsed = parsedDate?.takeIf { it in MIN_VALID_SMS_TIMESTAMP_MS..maxValidTs }

        val candidate = when {
            textDate != null && validParsed != null -> {
                if (kotlin.math.abs(validParsed - textDate) > ONE_DAY_MS) textDate else validParsed
            }
            validParsed != null -> validParsed
            textDate != null -> textDate
            else -> null
        }

        if (candidate != null) {
            if (textDate == null && hasValidReceivedAt &&
                kotlin.math.abs(candidate - safeReceivedAt) > THIRTY_DAYS_MS
            ) {
                return safeReceivedAt
            }
            return candidate
        }
        return safeReceivedAt
    }

    private fun isIgnoredOrUnset(field: ExtractedField): Boolean =
        field.fieldType == null || field.fieldType == FieldType.IGNORE

    private fun isTimeHourCandidate(field: ExtractedField): Boolean {
        if (field.fieldType == FieldType.TIME_HOUR) return true
        if (!isIgnoredOrUnset(field)) return false
        val num = convertToEnglish(field.text.trim()).toIntOrNull() ?: return false
        return num in 0..23
    }

    private fun isTimeMinuteCandidate(field: ExtractedField): Boolean {
        if (field.fieldType == FieldType.TIME_MINUTE) return true
        if (!isIgnoredOrUnset(field)) return false
        val digits = convertToEnglish(field.text.trim())
        if (digits.length != 2) return false
        val num = digits.toIntOrNull() ?: return false
        return num in 0..59
    }

    fun normalizeSavedFields(savedFields: List<ExtractedField>): List<ExtractedField> {
        val result = mutableListOf<ExtractedField>()
        var i = 0
        while (i < savedFields.size) {
            val field = savedFields[i]
            val text = field.text.trim()

            // 1. Full combined MMDD-HH:MM or MMDD_HH:MM (e.g. 0706-21:05)
            val fullMatch = Regex("""^([\d۰-۹٠-٩]{2})([\d۰-۹٠-٩]{2})[_\-]([\d۰-۹٠-٩]{2}):([\d۰-۹٠-٩]{2})$""").find(text)
            if (fullMatch != null) {
                val m = fullMatch.groupValues[1]
                val d = fullMatch.groupValues[2]
                val h = fullMatch.groupValues[3]
                val min = fullMatch.groupValues[4]
                val mInt = convertToEnglish(m).toIntOrNull() ?: -1
                val dInt = convertToEnglish(d).toIntOrNull() ?: -1
                val hInt = convertToEnglish(h).toIntOrNull() ?: -1
                val minInt = convertToEnglish(min).toIntOrNull() ?: -1
                if (mInt in 1..12 && dInt in 1..31 && hInt in 0..23 && minInt in 0..59) {
                    result.add(ExtractedField(m, field.lineIndex, FieldType.DATE_MONTH))
                    result.add(ExtractedField(d, field.lineIndex, FieldType.DATE_DAY))
                    result.add(ExtractedField(h, field.lineIndex, FieldType.TIME_HOUR))
                    result.add(ExtractedField(min, field.lineIndex, FieldType.TIME_MINUTE))
                    i++
                    continue
                }
            }

            // 2. Partial bug where previous PatternExtractor split around colon:
            // Field i was "0706-21" and field i+1 was "05"
            val partialBugMatch = Regex("""^([\d۰-۹٠-٩]{2})([\d۰-۹٠-٩]{2})[_\-]([\d۰-۹٠-٩]{2})$""").find(text)
            if (partialBugMatch != null && i + 1 < savedFields.size) {
                val nextField = savedFields[i + 1]
                val nextText = nextField.text.trim()
                if (nextText.matches(Regex("""^[\d۰-۹٠-٩]{2}$"""))) {
                    val m = partialBugMatch.groupValues[1]
                    val d = partialBugMatch.groupValues[2]
                    val h = partialBugMatch.groupValues[3]
                    val min = nextText
                    val mInt = convertToEnglish(m).toIntOrNull() ?: -1
                    val dInt = convertToEnglish(d).toIntOrNull() ?: -1
                    val hInt = convertToEnglish(h).toIntOrNull() ?: -1
                    val minInt = convertToEnglish(min).toIntOrNull() ?: -1
                    if (mInt in 1..12 && dInt in 1..31 && hInt in 0..23 && minInt in 0..59) {
                        result.add(ExtractedField(m, field.lineIndex, FieldType.DATE_MONTH))
                        result.add(ExtractedField(d, field.lineIndex, FieldType.DATE_DAY))
                        result.add(ExtractedField(h, field.lineIndex, FieldType.TIME_HOUR))
                        result.add(ExtractedField(min, nextField.lineIndex, FieldType.TIME_MINUTE))
                        i += 2
                        continue
                    }
                }
            }

            // 3. Standalone MMDD (e.g. "0706")
            val mmDdMatch = Regex("""^([\d۰-۹٠-٩]{2})([\d۰-۹٠-٩]{2})$""").find(text)
            if (mmDdMatch != null && (field.fieldType == FieldType.DATE_MONTH || field.fieldType == FieldType.DATE_DAY || field.fieldType == FieldType.IGNORE)) {
                val m = mmDdMatch.groupValues[1]
                val d = mmDdMatch.groupValues[2]
                val mInt = convertToEnglish(m).toIntOrNull() ?: -1
                val dInt = convertToEnglish(d).toIntOrNull() ?: -1
                if (mInt in 1..12 && dInt in 1..31 && (text.startsWith("0") || text.startsWith("۰") || text.startsWith("٠"))) {
                    result.add(ExtractedField(m, field.lineIndex, FieldType.DATE_MONTH))
                    result.add(ExtractedField(d, field.lineIndex, FieldType.DATE_DAY))
                    i++
                    continue
                }
            }

            // 4. Legacy single-digit date/time bug:
            // Previously `1405/9/3-12:37` was split into [TIME_HOUR, TIME_MINUTE, DATE_YEAR, DATE_MONTH, DATE_DAY, IGNORE("-")]
            // and `1405/9/2-3:51` was split into [DATE_YEAR, DATE_MONTH, DATE_DAY, IGNORE("-"), TIME_HOUR, TIME_MINUTE].
            // Normalize both to the canonical [DATE_YEAR, DATE_MONTH, DATE_DAY, TIME_HOUR, TIME_MINUTE] order.
            if (i + 5 < savedFields.size) {
                val f0 = savedFields[i]
                val f1 = savedFields[i + 1]
                val f2 = savedFields[i + 2]
                val f3 = savedFields[i + 3]
                val f4 = savedFields[i + 4]
                val f5 = savedFields[i + 5]
                val sameLine = f0.lineIndex == f1.lineIndex &&
                    f1.lineIndex == f2.lineIndex &&
                    f2.lineIndex == f3.lineIndex &&
                    f3.lineIndex == f4.lineIndex &&
                    f4.lineIndex == f5.lineIndex

                if (sameLine &&
                    isTimeHourCandidate(f0) &&
                    isTimeMinuteCandidate(f1) &&
                    f2.fieldType == FieldType.DATE_YEAR &&
                    f3.fieldType == FieldType.DATE_MONTH &&
                    f4.fieldType == FieldType.DATE_DAY &&
                    isIgnoredOrUnset(f5) &&
                    (f5.text.trim() == "-" || f5.text.trim() == "_")
                ) {
                    result.add(f2)
                    result.add(f3)
                    result.add(f4)
                    result.add(f0)
                    result.add(f1)
                    i += 6
                    continue
                }

                if (sameLine &&
                    f0.fieldType == FieldType.DATE_YEAR &&
                    f1.fieldType == FieldType.DATE_MONTH &&
                    f2.fieldType == FieldType.DATE_DAY &&
                    isIgnoredOrUnset(f3) &&
                    (f3.text.trim() == "-" || f3.text.trim() == "_") &&
                    isTimeHourCandidate(f4) &&
                    isTimeMinuteCandidate(f5)
                ) {
                    result.add(f0)
                    result.add(f1)
                    result.add(f2)
                    result.add(f4)
                    result.add(f5)
                    i += 6
                    continue
                }
            }

            // 5. Legacy single-digit month/day without year:
            // [TIME_HOUR, TIME_MINUTE, DATE_MONTH, DATE_DAY, IGNORE("-")] or
            // [DATE_MONTH, DATE_DAY, IGNORE("-"), TIME_HOUR, TIME_MINUTE]
            if (i + 4 < savedFields.size) {
                val f0 = savedFields[i]
                val f1 = savedFields[i + 1]
                val f2 = savedFields[i + 2]
                val f3 = savedFields[i + 3]
                val f4 = savedFields[i + 4]
                val sameLine = f0.lineIndex == f1.lineIndex &&
                    f1.lineIndex == f2.lineIndex &&
                    f2.lineIndex == f3.lineIndex &&
                    f3.lineIndex == f4.lineIndex

                if (sameLine &&
                    isTimeHourCandidate(f0) &&
                    isTimeMinuteCandidate(f1) &&
                    f2.fieldType == FieldType.DATE_MONTH &&
                    f3.fieldType == FieldType.DATE_DAY &&
                    isIgnoredOrUnset(f4) &&
                    (f4.text.trim() == "-" || f4.text.trim() == "_")
                ) {
                    result.add(f2)
                    result.add(f3)
                    result.add(f0)
                    result.add(f1)
                    i += 5
                    continue
                }

                if (sameLine &&
                    f0.fieldType == FieldType.DATE_MONTH &&
                    f1.fieldType == FieldType.DATE_DAY &&
                    isIgnoredOrUnset(f2) &&
                    (f2.text.trim() == "-" || f2.text.trim() == "_") &&
                    isTimeHourCandidate(f3) &&
                    isTimeMinuteCandidate(f4)
                ) {
                    result.add(f0)
                    result.add(f1)
                    result.add(f3)
                    result.add(f4)
                    i += 5
                    continue
                }
            }

            result.add(field)
            i++
        }
        return result
    }

    private fun convertToEnglish(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(persianDigits[i], ('0' + i)).replace(arabicDigits[i], ('0' + i))
        }
        return result
    }

    fun parseDateTimeToTimestamp(
        dateStr: String?,
        timeStr: String?,
        hourStr: String? = null,
        minStr: String? = null,
        yearStr: String? = null,
        monthStr: String? = null,
        dayStr: String? = null
    ): Long? {
        if (dateStr.isNullOrBlank() && timeStr.isNullOrBlank() && hourStr.isNullOrBlank() && minStr.isNullOrBlank()
            && yearStr.isNullOrBlank() && monthStr.isNullOrBlank() && dayStr.isNullOrBlank()) {
            return null
        }

        try {
            var hour = hourStr?.toIntOrNull() ?: 0
            var min = minStr?.toIntOrNull() ?: 0
            val currentYear = JalaliDate.today().year
            var jYear = currentYear
            var jMonth = 1
            var jDay = 1

            // If explicit year, month, or day provided
            if (!yearStr.isNullOrBlank()) {
                var y = yearStr.toIntOrNull() ?: currentYear
                if (y < 100) y += 1400
                jYear = y
            }
            if (!monthStr.isNullOrBlank()) {
                jMonth = monthStr.toIntOrNull() ?: 1
            }
            if (!dayStr.isNullOrBlank()) {
                jDay = dayStr.toIntOrNull() ?: 1
            }

            val combinedStr = "${dateStr.orEmpty()} ${timeStr.orEmpty()}".trim()
            val eng = convertToEnglish(combinedStr)

            // Extract time (HH:MM or HH:MM:SS) if not already explicitly provided
            if (hourStr == null || minStr == null) {
                val timeMatch = Regex("""(\d{1,2}):(\d{2})(?::(\d{2}))?""").find(eng)
                if (timeMatch != null) {
                    if (hourStr == null) hour = timeMatch.groupValues[1].toIntOrNull() ?: 0
                    if (minStr == null) min = timeMatch.groupValues[2].toIntOrNull() ?: 0
                }
            }

            // If separate year/month/day were not provided, parse from dateStr/combinedStr
            if (yearStr.isNullOrBlank() && monthStr.isNullOrBlank() && dayStr.isNullOrBlank()) {
                // 1. Full 4-digit year: YYYY/MM/DD or YYYY-MM-DD
                val fullDateMatch = Regex("""(\d{4})[/\-](\d{1,2})[/\-](\d{1,2})""").find(eng)
                if (fullDateMatch != null) {
                    jYear = fullDateMatch.groupValues[1].toInt()
                    jMonth = fullDateMatch.groupValues[2].toInt()
                    jDay = fullDateMatch.groupValues[3].toInt()
                } else {
                    // 2. 2-digit year: YY/MM/DD or YY-MM-DD
                    val shortYearMatch = Regex("""(\d{2})[/\-](\d{2})[/\-](\d{2})""").find(eng)
                    if (shortYearMatch != null) {
                        var yy = shortYearMatch.groupValues[1].toInt()
                        if (yy < 100) yy += 1400
                        jYear = yy
                        jMonth = shortYearMatch.groupValues[2].toInt()
                        jDay = shortYearMatch.groupValues[3].toInt()
                    } else {
                        // 3. Month and Day: MM/DD or MM-DD
                        val monthDayMatch = Regex("""(\d{1,2})[/\-](\d{1,2})""").find(eng)
                        if (monthDayMatch != null) {
                            jMonth = monthDayMatch.groupValues[1].toInt()
                            jDay = monthDayMatch.groupValues[2].toInt()
                        } else {
                            // 4. Raw MMDD (e.g. 0525-17:28 or 0706-21:05)
                            val rawMmDdMatch = Regex("""(\d{2})(\d{2})(?:[_\-]\d{1,2}:\d{2})?""").find(eng)
                            if (rawMmDdMatch != null) {
                                val mCandidate = rawMmDdMatch.groupValues[1].toInt()
                                val dCandidate = rawMmDdMatch.groupValues[2].toInt()
                                if (mCandidate in 1..12 && dCandidate in 1..31) {
                                    jMonth = mCandidate
                                    jDay = dCandidate
                                }
                            }
                        }
                    }
                }
            }

            val clampedMonth = jMonth.coerceIn(1, 12)
            val maxDay = JalaliDate.getJalaliMonthLength(jYear, clampedMonth)
            val (gY, gM, gD) = JalaliDate.jalaliToGregorian(jYear, clampedMonth, jDay.coerceIn(1, maxDay))
            return LocalDateTime.of(gY, gM, gD, hour.coerceIn(0, 23), min.coerceIn(0, 59))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            return null
        }
    }
}
