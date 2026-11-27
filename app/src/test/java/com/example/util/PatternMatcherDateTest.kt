package com.example.util

import com.example.data.local.entity.ExtractedField
import com.example.data.local.entity.FieldType
import com.example.data.local.entity.SmsPatternEntity
import com.example.data.local.entity.TransactionType
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PatternMatcherDateTest {

    private val gson = Gson()

    private val smsSingleDigitHour = """
        420970020082269001
        149,910,000-
        1405/9/2-3:51
        مانده:758,874,872
    """.trimIndent()

    private val smsDoubleDigitHour = """
        420970020082269001
        2,000,000-
        1405/9/3-12:37
        مانده:756,874,872
    """.trimIndent()

    @Test
    fun extractParts_producesIdenticalTokenLayoutForSingleAndDoubleDigitHours() {
        val parts1 = PatternExtractor.extractParts(smsSingleDigitHour).map { it.text }
        val parts2 = PatternExtractor.extractParts(smsDoubleDigitHour).map { it.text }

        assertEquals(
            listOf("420970020082269001", "149,910,000", "-", "1405", "9", "2", "3", "51", "مانده", "758,874,872"),
            parts1
        )
        assertEquals(
            listOf("420970020082269001", "2,000,000", "-", "1405", "9", "3", "12", "37", "مانده", "756,874,872"),
            parts2
        )
    }

    @Test
    fun match_newPattern_parsesBothUserSmsCorrectly() {
        val fields = listOf(
            ExtractedField("420970020082269001", 0, FieldType.ACCOUNT_IDENTIFIER),
            ExtractedField("2,000,000", 1, FieldType.AMOUNT),
            ExtractedField("-", 1, FieldType.SIGN),
            ExtractedField("1405", 2, FieldType.DATE_YEAR),
            ExtractedField("9", 2, FieldType.DATE_MONTH),
            ExtractedField("3", 2, FieldType.DATE_DAY),
            ExtractedField("12", 2, FieldType.TIME_HOUR),
            ExtractedField("37", 2, FieldType.TIME_MINUTE),
            ExtractedField("مانده", 3, FieldType.IGNORE),
            ExtractedField("756,874,872", 3, FieldType.BALANCE)
        )
        val pattern = SmsPatternEntity(
            id = 1L,
            accountIdentifier = "420970020082269001",
            sampleSms = smsDoubleDigitHour,
            extractedFields = gson.toJson(fields),
            bankName = "مهر ایران"
        )

        val parsed1 = PatternMatcher.match(smsSingleDigitHour, listOf(pattern))
        assertNotNull(parsed1)
        assertEquals(149_910_000L, parsed1!!.amount)
        assertEquals(TransactionType.EXPENSE, parsed1.transactionType)
        assertEquals(758_874_872L, parsed1.balance)
        val date1 = JalaliDate.fromTimestamp(parsed1.date!!)
        assertEquals(JalaliDate(1405, 9, 2), date1)
        assertEquals(Pair(3, 51), DateFormatter.extractHourAndMinute(parsed1.date!!))

        val parsed2 = PatternMatcher.match(smsDoubleDigitHour, listOf(pattern))
        assertNotNull(parsed2)
        assertEquals(2_000_000L, parsed2!!.amount)
        assertEquals(TransactionType.EXPENSE, parsed2.transactionType)
        assertEquals(756_874_872L, parsed2.balance)
        val date2 = JalaliDate.fromTimestamp(parsed2.date!!)
        assertEquals(JalaliDate(1405, 9, 3), date2)
        assertEquals(Pair(12, 37), DateFormatter.extractHourAndMinute(parsed2.date!!))
    }

    @Test
    fun match_legacyBuggySavedPatternWithTimeBeforeDate_stillParsesBothSmsAccurately() {
        // Exact legacy saved field sequence produced before the fix when learning from `1405/9/3-12:37`
        val legacyFields = listOf(
            ExtractedField("420970020082269001", 0, FieldType.ACCOUNT_IDENTIFIER),
            ExtractedField("2,000,000", 1, FieldType.AMOUNT),
            ExtractedField("-", 1, FieldType.SIGN),
            ExtractedField("12", 2, FieldType.TIME_HOUR),
            ExtractedField("37", 2, FieldType.TIME_MINUTE),
            ExtractedField("1405", 2, FieldType.DATE_YEAR),
            ExtractedField("9", 2, FieldType.DATE_MONTH),
            ExtractedField("3", 2, FieldType.DATE_DAY),
            ExtractedField("-", 2, FieldType.IGNORE),
            ExtractedField("مانده", 3, FieldType.IGNORE),
            ExtractedField("756,874,872", 3, FieldType.BALANCE)
        )
        val legacyPattern = SmsPatternEntity(
            id = 2L,
            accountIdentifier = "420970020082269001",
            sampleSms = smsDoubleDigitHour,
            extractedFields = gson.toJson(legacyFields),
            bankName = "مهر ایران"
        )

        val parsed1 = PatternMatcher.match(smsSingleDigitHour, listOf(legacyPattern))
        assertNotNull(parsed1)
        assertEquals(JalaliDate(1405, 9, 2), JalaliDate.fromTimestamp(parsed1!!.date!!))
        assertEquals(Pair(3, 51), DateFormatter.extractHourAndMinute(parsed1.date!!))

        val parsed2 = PatternMatcher.match(smsDoubleDigitHour, listOf(legacyPattern))
        assertNotNull(parsed2)
        assertEquals(JalaliDate(1405, 9, 3), JalaliDate.fromTimestamp(parsed2!!.date!!))
        assertEquals(Pair(12, 37), DateFormatter.extractHourAndMinute(parsed2.date!!))
    }

    @Test
    fun match_legacySavedPatternWithHyphenSeparatorBetweenDateAndTime_stillParsesBothSmsAccurately() {
        val legacyFields = listOf(
            ExtractedField("420970020082269001", 0, FieldType.ACCOUNT_IDENTIFIER),
            ExtractedField("149,910,000", 1, FieldType.AMOUNT),
            ExtractedField("-", 1, FieldType.SIGN),
            ExtractedField("1405", 2, FieldType.DATE_YEAR),
            ExtractedField("9", 2, FieldType.DATE_MONTH),
            ExtractedField("2", 2, FieldType.DATE_DAY),
            ExtractedField("-", 2, FieldType.IGNORE),
            ExtractedField("3", 2, FieldType.TIME_HOUR),
            ExtractedField("51", 2, FieldType.TIME_MINUTE),
            ExtractedField("مانده", 3, FieldType.IGNORE),
            ExtractedField("758,874,872", 3, FieldType.BALANCE)
        )
        val legacyPattern = SmsPatternEntity(
            id = 3L,
            accountIdentifier = "420970020082269001",
            sampleSms = smsSingleDigitHour,
            extractedFields = gson.toJson(legacyFields),
            bankName = "مهر ایران"
        )

        val parsed1 = PatternMatcher.match(smsSingleDigitHour, listOf(legacyPattern))
        assertNotNull(parsed1)
        assertEquals(JalaliDate(1405, 9, 2), JalaliDate.fromTimestamp(parsed1!!.date!!))
        assertEquals(Pair(3, 51), DateFormatter.extractHourAndMinute(parsed1.date!!))

        val parsed2 = PatternMatcher.match(smsDoubleDigitHour, listOf(legacyPattern))
        assertNotNull(parsed2)
        assertEquals(JalaliDate(1405, 9, 3), JalaliDate.fromTimestamp(parsed2!!.date!!))
        assertEquals(Pair(12, 37), DateFormatter.extractHourAndMinute(parsed2.date!!))
    }

    @Test
    fun match_persianDigitsSingleDigitDateAndHour_parsesCorrectly() {
        val persianSms = """
            420970020082269001
            ۱۴۹,۹۱۰,۰۰۰-
            ۱۴۰۵/۹/۲-۳:۵۱
            مانده:۷۵۸,۸۷۴,۸۷۲
        """.trimIndent()

        val fields = listOf(
            ExtractedField("420970020082269001", 0, FieldType.ACCOUNT_IDENTIFIER),
            ExtractedField("2,000,000", 1, FieldType.AMOUNT),
            ExtractedField("-", 1, FieldType.SIGN),
            ExtractedField("1405", 2, FieldType.DATE_YEAR),
            ExtractedField("9", 2, FieldType.DATE_MONTH),
            ExtractedField("3", 2, FieldType.DATE_DAY),
            ExtractedField("12", 2, FieldType.TIME_HOUR),
            ExtractedField("37", 2, FieldType.TIME_MINUTE),
            ExtractedField("مانده", 3, FieldType.IGNORE),
            ExtractedField("756,874,872", 3, FieldType.BALANCE)
        )
        val pattern = SmsPatternEntity(
            id = 4L,
            accountIdentifier = "420970020082269001",
            sampleSms = smsDoubleDigitHour,
            extractedFields = gson.toJson(fields),
            bankName = "مهر ایران"
        )

        val parsed = PatternMatcher.match(persianSms, listOf(pattern))
        assertNotNull(parsed)
        assertEquals(JalaliDate(1405, 9, 2), JalaliDate.fromTimestamp(parsed!!.date!!))
        assertEquals(Pair(3, 51), DateFormatter.extractHourAndMinute(parsed.date!!))
    }

    @Test
    fun match_invalidDateTokens_rejectedInsteadOfProducingBogusDate() {
        val malformedFields = listOf(
            ExtractedField("420970020082269001", 0, FieldType.ACCOUNT_IDENTIFIER),
            ExtractedField("149,910,000", 1, FieldType.AMOUNT),
            ExtractedField("-", 1, FieldType.SIGN),
            ExtractedField("x", 2, FieldType.TIME_HOUR),
            ExtractedField("y", 2, FieldType.TIME_MINUTE),
            ExtractedField("z", 2, FieldType.DATE_YEAR),
            ExtractedField("w", 2, FieldType.DATE_MONTH),
            ExtractedField("q", 2, FieldType.DATE_DAY),
            ExtractedField("مانده", 3, FieldType.IGNORE),
            ExtractedField("758,874,872", 3, FieldType.BALANCE)
        )
        val pattern = SmsPatternEntity(
            id = 5L,
            accountIdentifier = "420970020082269001",
            sampleSms = smsSingleDigitHour,
            extractedFields = gson.toJson(malformedFields),
            bankName = "مهر ایران"
        )

        // Token at index 3 is "1405" which is invalid as TIME_HOUR -> must be rejected cleanly
        val parsed = PatternMatcher.match(smsSingleDigitHour, listOf(pattern))
        assertNull(parsed)
    }

    @Test
    fun match_userReportedAbanSingleDigitHourSms_parsesAccurateDatesAndBalances() {
        val smsAban21 = """
            420970020082269001
            1,000,000-
            1405/8/21-8:14
            مانده:1,075,709,472
        """.trimIndent()

        val smsAban8 = """
            420970020082269001
            325,000-
            1405/8/8-7:37
            مانده:609,934,422
        """.trimIndent()

        // Legacy saved pattern where time tokens preceded date AND were left as IGNORE
        val legacyUntaggedTimeFields = listOf(
            ExtractedField("420970020082269001", 0, FieldType.ACCOUNT_IDENTIFIER),
            ExtractedField("2,000,000", 1, FieldType.AMOUNT),
            ExtractedField("-", 1, FieldType.SIGN),
            ExtractedField("12", 2, FieldType.IGNORE),
            ExtractedField("37", 2, FieldType.IGNORE),
            ExtractedField("1405", 2, FieldType.DATE_YEAR),
            ExtractedField("9", 2, FieldType.DATE_MONTH),
            ExtractedField("3", 2, FieldType.DATE_DAY),
            ExtractedField("-", 2, FieldType.IGNORE),
            ExtractedField("مانده", 3, FieldType.IGNORE),
            ExtractedField("756,874,872", 3, FieldType.BALANCE)
        )
        val pattern = SmsPatternEntity(
            id = 6L,
            accountIdentifier = "420970020082269001",
            sampleSms = smsDoubleDigitHour,
            extractedFields = gson.toJson(legacyUntaggedTimeFields),
            bankName = "مهر ایران"
        )

        val parsed21 = PatternMatcher.match(smsAban21, listOf(pattern))
        assertNotNull(parsed21)
        assertEquals(1_000_000L, parsed21!!.amount)
        assertEquals(1_075_709_472L, parsed21.balance)
        assertEquals(JalaliDate(1405, 8, 21), JalaliDate.fromTimestamp(parsed21.date!!))
        assertEquals(Pair(8, 14), DateFormatter.extractHourAndMinute(parsed21.date!!))

        val parsed8 = PatternMatcher.match(smsAban8, listOf(pattern))
        assertNotNull(parsed8)
        assertEquals(325_000L, parsed8!!.amount)
        assertEquals(609_934_422L, parsed8.balance)
        assertEquals(JalaliDate(1405, 8, 8), JalaliDate.fromTimestamp(parsed8.date!!))
        assertEquals(Pair(7, 37), DateFormatter.extractHourAndMinute(parsed8.date!!))
    }

    @Test
    fun resolveReliableSmsTimestamp_rejectsBogusFutureDates1421And1408AndRecoversTrueSmsDate() {
        val smsAban21 = """
            420970020082269001
            1,000,000-
            1405/8/21-8:14
            مانده:1,075,709,472
        """.trimIndent()
        val smsAban8 = """
            420970020082269001
            325,000-
            1405/8/8-7:37
            مانده:609,934,422
        """.trimIndent()

        val bogus1421 = JalaliDate(1421, 1, 8).toStartOfDayTimestamp()
        val bogus1408 = JalaliDate(1408, 1, 7).toStartOfDayTimestamp()
        val received21 = JalaliDate(1405, 8, 21).toStartOfDayTimestamp() + (8 * 60 + 15) * 60_000L
        val received8 = JalaliDate(1405, 8, 8).toStartOfDayTimestamp() + (7 * 60 + 38) * 60_000L

        val resolved21 = PatternMatcher.resolveReliableSmsTimestamp(
            parsedDate = bogus1421,
            smsText = smsAban21,
            receivedAt = received21
        )
        assertEquals(JalaliDate(1405, 8, 21), JalaliDate.fromTimestamp(resolved21))
        assertEquals(Pair(8, 14), DateFormatter.extractHourAndMinute(resolved21))

        val resolved8 = PatternMatcher.resolveReliableSmsTimestamp(
            parsedDate = bogus1408,
            smsText = smsAban8,
            receivedAt = received8
        )
        assertEquals(JalaliDate(1405, 8, 8), JalaliDate.fromTimestamp(resolved8))
        assertEquals(Pair(7, 37), DateFormatter.extractHourAndMinute(resolved8))

        // Direct text extraction must reject impossible future dates like 1421/01/08
        assertNull(PatternMatcher.extractDateTimeFromText("تاریخ: 1421/01/08-08:14"))
    }
}
