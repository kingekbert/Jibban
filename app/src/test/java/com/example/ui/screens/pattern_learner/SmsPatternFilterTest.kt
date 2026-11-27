package com.example.ui.screens.pattern_learner

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.SmsPatternEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsPatternFilterTest {

    private val mellatAccount = AccountEntity(
        id = 1L,
        name = "کارت ملت شخصی",
        type = AccountType.BANK,
        initialBalance = 0L,
        color = 1,
        icon = "bank",
        cardNumber = "6104337912345678",
        shabaNumber = "IR120120000000001234567890"
    )

    private val samanAccount = AccountEntity(
        id = 2L,
        name = "بلوبانک سامان",
        type = AccountType.BANK,
        initialBalance = 0L,
        color = 2,
        icon = "bank",
        cardNumber = "6219861998765432"
    )

    private val patterns = listOf(
        SmsPatternEntity(
            id = 10L,
            accountIdentifier = "5678",
            sampleSms = "بانک ملت\nبرداشت 2,000,000 ریال\nمانده: 10,000,000",
            extractedFields = "[]",
            bankName = "ملت"
        ),
        SmsPatternEntity(
            id = 11L,
            accountIdentifier = "98765432",
            sampleSms = "بلوبانک\nواریز ۵۰۰,۰۰۰ ریال به حساب شما",
            extractedFields = "[]",
            bankName = "سامان"
        ),
        SmsPatternEntity(
            id = 12L,
            accountIdentifier = "420970020082269001",
            sampleSms = "420970020082269001\n149,910,000-\n1405/9/2-3:51\nمانده:758,874,872",
            extractedFields = "[]",
            bankName = "تجارت"
        ),
        SmsPatternEntity(
            id = 13L,
            accountIdentifier = "",
            sampleSms = "بانك ملي ايران برداشت از كارت",
            extractedFields = "[]",
            bankName = null
        )
    )

    @Test
    fun `normalizeSearchText unifies Persian and Arabic letters and digits`() {
        assertEquals("کارت ملی 6104", SmsPatternLearnerViewModel.normalizeSearchText("كارت ملي ۶۱۰۴"))
        assertEquals("بانک 5678", SmsPatternLearnerViewModel.normalizeSearchText("  بانك   ٥٦٧٨  "))
        assertEquals("", SmsPatternLearnerViewModel.normalizeSearchText("   "))
    }

    @Test
    fun `extractAvailableBanks deduplicates normalized bank names and ignores null or blank`() {
        val withDuplicates = patterns + SmsPatternEntity(
            id = 14L,
            accountIdentifier = "1111",
            sampleSms = "تست",
            extractedFields = "[]",
            bankName = " ملت "
        )

        val banks = SmsPatternLearnerViewModel.extractAvailableBanks(withDuplicates)
        assertEquals(listOf("ملت", "سامان", "تجارت"), banks)
    }

    @Test
    fun `filterPatterns returns all items when query and filters are empty`() {
        val result = SmsPatternLearnerViewModel.filterPatterns(
            patterns = patterns,
            query = ""
        )
        assertEquals(patterns.map { it.id }, result.map { it.id })
    }

    @Test
    fun `filterPatterns matches bank name, Persian digits, account identifier, and Arabic letter variants`() {
        val byBank = SmsPatternLearnerViewModel.filterPatterns(patterns, query = "تجارت")
        assertEquals(listOf(12L), byBank.map { it.id })

        val byPersianDigits = SmsPatternLearnerViewModel.filterPatterns(patterns, query = "۵۶۷۸")
        assertEquals(listOf(10L), byPersianDigits.map { it.id })

        val bySmsTextNormalized = SmsPatternLearnerViewModel.filterPatterns(patterns, query = "کارت ملی")
        assertEquals(listOf(13L), bySmsTextNormalized.map { it.id })

        val byMultiToken = SmsPatternLearnerViewModel.filterPatterns(patterns, query = "سامان ۵۰۰,۰۰۰")
        assertEquals(listOf(11L), byMultiToken.map { it.id })
    }

    @Test
    fun `filterPatterns matches linked account names and selected account filter`() {
        val byLinkedAccountName = SmsPatternLearnerViewModel.filterPatterns(
            patterns = patterns,
            query = "شخصی",
            allAccounts = listOf(mellatAccount, samanAccount)
        )
        assertEquals(listOf(10L), byLinkedAccountName.map { it.id })

        val onlySaman = SmsPatternLearnerViewModel.filterPatterns(
            patterns = patterns,
            query = "",
            filterOnlySelectedAccount = true,
            selectedAccount = samanAccount,
            allAccounts = listOf(mellatAccount, samanAccount)
        )
        assertEquals(listOf(11L), onlySaman.map { it.id })
    }

    @Test
    fun `filterPatterns combines bank chip filter with search query and returns empty on mismatch`() {
        val bankOnly = SmsPatternLearnerViewModel.filterPatterns(
            patterns = patterns,
            query = "",
            selectedBankFilter = "ملت"
        )
        assertEquals(listOf(10L), bankOnly.map { it.id })

        val mismatch = SmsPatternLearnerViewModel.filterPatterns(
            patterns = patterns,
            query = "بلوبانک",
            selectedBankFilter = "ملت"
        )
        assertTrue(mismatch.isEmpty())
    }

    @Test
    fun `matchesAccount detects card suffix match and rejects unrelated accounts`() {
        assertTrue(SmsPatternLearnerViewModel.matchesAccount(patterns[0], mellatAccount))
        assertFalse(SmsPatternLearnerViewModel.matchesAccount(patterns[0], samanAccount))
        assertTrue(SmsPatternLearnerViewModel.matchesAccount(patterns[1], samanAccount))
    }
}
