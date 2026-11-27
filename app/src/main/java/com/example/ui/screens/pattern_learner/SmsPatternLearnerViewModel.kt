package com.example.ui.screens.pattern_learner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ExtractedField
import com.example.data.local.entity.FieldType
import com.example.data.local.entity.SmsPatternEntity
import com.example.data.repository.AccountRepository
import com.example.data.repository.SmsPatternRepository
import com.example.util.PatternExtractor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmsPatternLearnerUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val selectedAccountId: Long? = null,
    val accountIdentifier: String = "",
    val sampleSms: String = "",
    val bankName: String = "",
    val extractedFields: List<ExtractedField> = emptyList(),
    val existingPatterns: List<SmsPatternEntity> = emptyList(),
    val patternSearchQuery: String = "",
    val selectedBankFilter: String? = null,
    val filterOnlySelectedAccount: Boolean = false,
    val editingPatternId: Long? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
) {
    val selectedAccount: AccountEntity?
        get() = accounts.find { it.id == selectedAccountId }

    val availableBankFilters: List<String>
        get() = SmsPatternLearnerViewModel.extractAvailableBanks(existingPatterns)

    val filteredPatterns: List<SmsPatternEntity>
        get() = SmsPatternLearnerViewModel.filterPatterns(
            patterns = existingPatterns,
            query = patternSearchQuery,
            selectedBankFilter = selectedBankFilter,
            filterOnlySelectedAccount = filterOnlySelectedAccount,
            selectedAccount = selectedAccount,
            allAccounts = accounts
        )

    val hasActivePatternFilters: Boolean
        get() = patternSearchQuery.isNotBlank() || selectedBankFilter != null || filterOnlySelectedAccount
}

class SmsPatternLearnerViewModel(
    private val smsPatternRepository: SmsPatternRepository,
    private val accountRepository: AccountRepository,
    initialAccountId: Long? = null,
    initialSampleText: String? = null,
    initialPatternId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SmsPatternLearnerUiState(
            selectedAccountId = initialAccountId,
            sampleSms = initialSampleText ?: ""
        )
    )
    val uiState: StateFlow<SmsPatternLearnerUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        if (initialPatternId != null && initialPatternId > 0L) {
            viewModelScope.launch {
                val pattern = smsPatternRepository.getById(initialPatternId).firstOrNull()
                if (pattern != null) {
                    editPattern(pattern)
                }
            }
        } else if (!initialSampleText.isNullOrBlank()) {
            analyzeSms()
        }

        viewModelScope.launch {
            accountRepository.allAccounts.collect { accList ->
                _uiState.update { state ->
                    val selAcc = accList.find { it.id == state.selectedAccountId }
                    val autoIdent = if (state.accountIdentifier.isBlank()) (selAcc?.shabaNumber ?: selAcc?.cardNumber ?: "") else state.accountIdentifier
                    val autoBank = if (state.bankName.isBlank()) (selAcc?.name ?: "") else state.bankName
                    state.copy(
                        accounts = accList,
                        accountIdentifier = autoIdent,
                        bankName = autoBank
                    )
                }
            }
        }

        viewModelScope.launch {
            smsPatternRepository.allPatterns.collect { patterns ->
                _uiState.update { state ->
                    val validBankFilter = state.selectedBankFilter?.takeIf { activeBank ->
                        extractAvailableBanks(patterns).any {
                            normalizeSearchText(it) == normalizeSearchText(activeBank)
                        }
                    }
                    state.copy(
                        existingPatterns = patterns,
                        selectedBankFilter = validBankFilter
                    )
                }
            }
        }
    }

    fun selectAccount(accountId: Long) {
        val acc = _uiState.value.accounts.find { it.id == accountId }
        val ident = acc?.shabaNumber?.takeIf { it.isNotBlank() } ?: acc?.cardNumber ?: ""
        val bank = acc?.name ?: ""
        _uiState.update {
            it.copy(
                selectedAccountId = accountId,
                accountIdentifier = if (it.accountIdentifier.isBlank()) ident else it.accountIdentifier,
                bankName = if (it.bankName.isBlank()) bank else it.bankName
            )
        }
    }

    fun setSampleSms(sms: String) {
        _uiState.update { it.copy(sampleSms = sms, errorMessage = null) }
    }

    fun setAccountIdentifier(identifier: String) {
        _uiState.update { it.copy(accountIdentifier = identifier) }
    }

    fun setBankName(name: String) {
        _uiState.update { it.copy(bankName = name) }
    }

    fun setPatternSearchQuery(query: String) {
        _uiState.update { it.copy(patternSearchQuery = query) }
    }

    fun setBankFilter(bankName: String?) {
        _uiState.update { current ->
            val nextBank = if (bankName != null &&
                current.selectedBankFilter != null &&
                normalizeSearchText(current.selectedBankFilter) == normalizeSearchText(bankName)
            ) {
                null
            } else {
                bankName
            }
            current.copy(selectedBankFilter = nextBank)
        }
    }

    fun setFilterOnlySelectedAccount(onlySelected: Boolean) {
        _uiState.update { it.copy(filterOnlySelectedAccount = onlySelected) }
    }

    fun clearPatternSearchAndFilters() {
        _uiState.update {
            it.copy(
                patternSearchQuery = "",
                selectedBankFilter = null,
                filterOnlySelectedAccount = false
            )
        }
    }

    fun analyzeSms() {
        val text = _uiState.value.sampleSms.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(errorMessage = "لطفاً متن پیامک نمونه را وارد کنید.") }
            return
        }

        val parts = PatternExtractor.extractParts(text)
        if (parts.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "امکان استخراج بخش‌های پیامک وجود ندارد.") }
            return
        }

        _uiState.update {
            it.copy(
                extractedFields = parts,
                errorMessage = null
            )
        }
    }

    fun updateFieldType(index: Int, fieldType: FieldType) {
        val current = _uiState.value.extractedFields.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(fieldType = fieldType)
            _uiState.update { it.copy(extractedFields = current) }
        }
    }

    fun editPattern(pattern: SmsPatternEntity) {
        val typeToken = object : TypeToken<List<ExtractedField>>() {}.type
        var parsedFields: List<ExtractedField> = try {
            gson.fromJson(pattern.extractedFields, typeToken) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        // If JSON fields empty or invalid, re-extract from sample text
        if (parsedFields.isEmpty() && pattern.sampleSms.isNotBlank()) {
            parsedFields = PatternExtractor.extractParts(pattern.sampleSms)
        }

        _uiState.update {
            it.copy(
                editingPatternId = pattern.id,
                sampleSms = pattern.sampleSms,
                accountIdentifier = pattern.accountIdentifier,
                bankName = pattern.bankName ?: "",
                extractedFields = parsedFields,
                errorMessage = null
            )
        }
    }

    fun cancelEdit() {
        _uiState.update {
            it.copy(
                editingPatternId = null,
                sampleSms = "",
                extractedFields = emptyList(),
                errorMessage = null
            )
        }
    }

    fun savePattern() {
        val state = _uiState.value
        if (state.sampleSms.isBlank()) {
            _uiState.update { it.copy(errorMessage = "متن پیامک نمونه نباید خالی باشد.") }
            return
        }

        if (state.extractedFields.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "ابتدا روی دکمه «تحلیل» بزنید.") }
            return
        }

        val hasAmount = state.extractedFields.any { it.fieldType == FieldType.AMOUNT }
        if (!hasAmount) {
            _uiState.update { it.copy(errorMessage = "لطفاً بخش مربوط به «مبلغ تراکنش» را مشخص کنید.") }
            return
        }

        val jsonFields = gson.toJson(state.extractedFields)
        val entity = SmsPatternEntity(
            id = state.editingPatternId ?: 0L,
            accountIdentifier = state.accountIdentifier.trim(),
            sampleSms = state.sampleSms.trim(),
            extractedFields = jsonFields,
            bankName = state.bankName.trim().ifBlank { null },
            isActive = true
        )

        viewModelScope.launch {
            try {
                if (state.editingPatternId != null) {
                    smsPatternRepository.updatePattern(entity)
                } else {
                    smsPatternRepository.insertPattern(entity)
                }
                _uiState.update {
                    it.copy(
                        isSuccess = true,
                        editingPatternId = null,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "خطا در ذخیره الگو: ${e.localizedMessage}") }
            }
        }
    }

    fun deletePattern(id: Long) {
        viewModelScope.launch {
            smsPatternRepository.deleteById(id)
            if (_uiState.value.editingPatternId == id) {
                cancelEdit()
            }
        }
    }

    fun restorePattern(pattern: SmsPatternEntity) {
        viewModelScope.launch {
            smsPatternRepository.insertPattern(pattern.copy(id = 0L))
        }
    }

    class Factory(
        private val smsPatternRepository: SmsPatternRepository,
        private val accountRepository: AccountRepository,
        private val accountId: Long? = null,
        private val initialSampleText: String? = null,
        private val initialPatternId: Long? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SmsPatternLearnerViewModel(smsPatternRepository, accountRepository, accountId, initialSampleText, initialPatternId) as T
        }
    }

    companion object {
        private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

        /**
         * Normalizes Persian/Arabic letter variants (ی/ي, ک/ك), digits (Persian/Arabic -> ASCII),
         * and whitespace for reliable search and filtering across SMS patterns.
         */
        fun normalizeSearchText(input: String): String {
            if (input.isBlank()) return ""
            val sb = StringBuilder(input.length)
            for (ch in input.trim().lowercase()) {
                val pIdx = persianDigits.indexOf(ch)
                val aIdx = if (pIdx == -1) arabicDigits.indexOf(ch) else -1
                when {
                    pIdx != -1 -> sb.append(('0'.code + pIdx).toChar())
                    aIdx != -1 -> sb.append(('0'.code + aIdx).toChar())
                    ch == 'ي' || ch == 'ى' -> sb.append('ی')
                    ch == 'ك' -> sb.append('ک')
                    ch == '\u200C' || ch == '\u200F' || ch == '\u200E' -> sb.append(' ')
                    else -> sb.append(ch)
                }
            }
            return sb.toString().replace(Regex("\\s+"), " ").trim()
        }

        private fun digitsOnly(input: String?): String {
            if (input.isNullOrBlank()) return ""
            return normalizeSearchText(input).filter { it.isDigit() }
        }

        fun extractAvailableBanks(patterns: List<SmsPatternEntity>): List<String> {
            val seenNormalized = LinkedHashSet<String>()
            val result = mutableListOf<String>()
            for (pattern in patterns) {
                val bank = pattern.bankName?.trim().orEmpty()
                if (bank.isBlank()) continue
                val norm = normalizeSearchText(bank)
                if (seenNormalized.add(norm)) {
                    result.add(bank)
                }
            }
            return result
        }

        fun matchesAccount(pattern: SmsPatternEntity, account: AccountEntity): Boolean {
            val patternIdentDigits = digitsOnly(pattern.accountIdentifier)
            val cardDigits = digitsOnly(account.cardNumber)
            val shabaDigits = digitsOnly(account.shabaNumber)

            if (patternIdentDigits.isNotEmpty()) {
                if (cardDigits.isNotEmpty() && (
                        patternIdentDigits == cardDigits ||
                            cardDigits.endsWith(patternIdentDigits) ||
                            patternIdentDigits.endsWith(cardDigits.takeLast(4))
                        )
                ) {
                    return true
                }
                if (shabaDigits.isNotEmpty() && (
                        patternIdentDigits == shabaDigits ||
                            shabaDigits.contains(patternIdentDigits) ||
                            patternIdentDigits.contains(shabaDigits.takeLast(6))
                        )
                ) {
                    return true
                }
            }

            val normAccName = normalizeSearchText(account.name)
            val normBank = normalizeSearchText(pattern.bankName.orEmpty())
            val normIdent = normalizeSearchText(pattern.accountIdentifier)
            if (normAccName.isNotEmpty()) {
                if (normBank.isNotEmpty() && (normAccName.contains(normBank) || normBank.contains(normAccName))) {
                    return true
                }
                if (normIdent.isNotEmpty() && normIdent == normAccName) {
                    return true
                }
            }
            return false
        }

        fun filterPatterns(
            patterns: List<SmsPatternEntity>,
            query: String,
            selectedBankFilter: String? = null,
            filterOnlySelectedAccount: Boolean = false,
            selectedAccount: AccountEntity? = null,
            allAccounts: List<AccountEntity> = emptyList()
        ): List<SmsPatternEntity> {
            val normQuery = normalizeSearchText(query)
            val queryTokens = if (normQuery.isBlank()) emptyList() else normQuery.split(" ").filter { it.isNotBlank() }
            val normBankFilter = selectedBankFilter?.let { normalizeSearchText(it) }?.takeIf { it.isNotBlank() }

            return patterns.filter { pattern ->
                if (filterOnlySelectedAccount && selectedAccount != null && !matchesAccount(pattern, selectedAccount)) {
                    return@filter false
                }

                if (normBankFilter != null) {
                    val patternBank = normalizeSearchText(pattern.bankName.orEmpty())
                    if (patternBank != normBankFilter) {
                        return@filter false
                    }
                }

                if (queryTokens.isEmpty()) {
                    true
                } else {
                    val linkedAccountNames = allAccounts
                        .filter { matchesAccount(pattern, it) }
                        .joinToString(" ") { normalizeSearchText(it.name) }

                    val searchableCorpus = buildString {
                        append(normalizeSearchText(pattern.bankName.orEmpty()))
                        append(' ')
                        append(normalizeSearchText(pattern.accountIdentifier))
                        append(' ')
                        append(normalizeSearchText(pattern.sampleSms))
                        if (linkedAccountNames.isNotBlank()) {
                            append(' ')
                            append(linkedAccountNames)
                        }
                    }

                    queryTokens.all { token -> searchableCorpus.contains(token) }
                }
            }
        }
    }
}
