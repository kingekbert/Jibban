package com.example.ui.screens.add_transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import com.example.data.local.entity.GroupExpenseShareMode
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.repository.AccountRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.GroupExpenseRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.usecase.AddTransactionUseCase
import com.example.util.AmountFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

data class GroupShareInput(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Long = 0L
)

data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: Long = 0L,
    val selectedAccount: AccountEntity? = null,
    val selectedToAccount: AccountEntity? = null,
    val isLastUsedAccountDefault: Boolean = false,
    val selectedCategory: CategoryEntity? = null,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val selectedHour: Int = java.time.LocalTime.now().hour,
    val selectedMinute: Int = java.time.LocalTime.now().minute,
    val note: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    // Group Expense (Dong)
    val isGroupExpenseEnabled: Boolean = false,
    val groupShareMode: GroupExpenseShareMode = GroupExpenseShareMode.EQUAL,
    val groupShares: List<GroupShareInput> = emptyList(),
    val recentPersonNames: List<String> = emptyList()
)

class AddTransactionViewModel(
    initialTypeString: String,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val groupExpenseRepository: GroupExpenseRepository,
    initialGroupExpense: Boolean = false
) : ViewModel() {

    private val initialType = when (initialTypeString) {
        "INCOME" -> TransactionType.INCOME
        "TRANSFER" -> TransactionType.TRANSFER
        else -> TransactionType.EXPENSE
    }

    private val _uiState = MutableStateFlow(
        AddTransactionUiState(
            type = initialType,
            isGroupExpenseEnabled = initialGroupExpense && initialType == TransactionType.EXPENSE
        )
    )
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()
    private val isSavingGuard = AtomicBoolean(false)
    private var allCategoriesCache: List<CategoryEntity> = emptyList()
    private var hasUserSelectedAccount = false

    init {
        observeFormData()
    }

    private fun observeFormData() {
        viewModelScope.launch {
            combine(
                accountRepository.allAccounts,
                categoryRepository.allCategories,
                groupExpenseRepository.recentPersonNames
            ) { accounts, categories, recentNames ->
                Triple(accounts, categories, recentNames)
            }.collect { (accounts, categories, recentNames) ->
                allCategoriesCache = categories
                val lastAccId = settingsRepository.lastAccountId
                val lastToAccId = settingsRepository.lastToAccountId
                val lastCatId = settingsRepository.lastCategoryId
                val lastExpenseCatId = settingsRepository.lastExpenseCategoryId
                val lastIncomeCatId = settingsRepository.lastIncomeCategoryId

                _uiState.update { current ->
                    val existingSelectedAcc = current.selectedAccount?.let { prev ->
                        accounts.find { it.id == prev.id }
                    }
                    val (resolvedAccount, isLastDefault) = if (hasUserSelectedAccount && existingSelectedAcc != null) {
                        existingSelectedAcc to (existingSelectedAcc.id == lastAccId && lastAccId > 0L)
                    } else {
                        resolveDefaultAccount(accounts, lastAccId)
                    }

                    val existingToAcc = current.selectedToAccount?.let { prev ->
                        accounts.find { it.id == prev.id && it.id != resolvedAccount?.id }
                    }
                    val resolvedToAccount = existingToAcc ?: resolveDefaultDestinationAccount(
                        accounts = accounts,
                        sourceAccount = resolvedAccount,
                        lastUsedToAccountId = lastToAccId
                    )

                    val filteredCategories = filterCategoriesByType(categories, current.type)
                    val resolvedCategory = resolveDefaultCategory(
                        categories = filteredCategories,
                        type = current.type,
                        lastExpenseCategoryId = lastExpenseCatId,
                        lastIncomeCategoryId = lastIncomeCatId,
                        lastCategoryId = lastCatId,
                        currentSelectedId = current.selectedCategory?.id
                    )

                    current.copy(
                        accounts = accounts,
                        categories = filteredCategories,
                        selectedAccount = resolvedAccount,
                        selectedToAccount = resolvedToAccount,
                        isLastUsedAccountDefault = isLastDefault,
                        selectedCategory = resolvedCategory,
                        recentPersonNames = recentNames
                    )
                }
            }
        }
    }

    fun setTransactionType(type: TransactionType) {
        val filtered = filterCategoriesByType(allCategoriesCache, type)
        val lastCatId = settingsRepository.lastCategoryId
        val lastExpenseCatId = settingsRepository.lastExpenseCategoryId
        val lastIncomeCatId = settingsRepository.lastIncomeCategoryId

        _uiState.update { current ->
            val selectedCat = resolveDefaultCategory(
                categories = filtered,
                type = type,
                lastExpenseCategoryId = lastExpenseCatId,
                lastIncomeCategoryId = lastIncomeCatId,
                lastCategoryId = lastCatId,
                currentSelectedId = null
            ) ?: filtered.find { it.id == current.selectedCategory?.id }

            val resolvedToAccount = if (type == TransactionType.TRANSFER &&
                (current.selectedToAccount == null || current.selectedToAccount.id == current.selectedAccount?.id)
            ) {
                resolveDefaultDestinationAccount(
                    accounts = current.accounts,
                    sourceAccount = current.selectedAccount,
                    lastUsedToAccountId = settingsRepository.lastToAccountId
                )
            } else {
                current.selectedToAccount
            }

            current.copy(
                type = type,
                categories = filtered,
                selectedCategory = selectedCat,
                selectedToAccount = resolvedToAccount,
                // Disable group expense if switching away from EXPENSE
                isGroupExpenseEnabled = if (type == TransactionType.EXPENSE) current.isGroupExpenseEnabled else false,
                errorMessage = null
            )
        }
    }

    fun setAmount(amount: Long) {
        _uiState.update { current ->
            val updatedShares = if (current.isGroupExpenseEnabled && current.groupShareMode == GroupExpenseShareMode.EQUAL && current.groupShares.isNotEmpty()) {
                recalculateEqualShares(current.groupShares, amount)
            } else current.groupShares

            current.copy(
                amount = amount,
                groupShares = updatedShares,
                errorMessage = null
            )
        }
    }

    fun setSelectedAccount(account: AccountEntity) {
        hasUserSelectedAccount = true
        val lastAccId = settingsRepository.lastAccountId
        _uiState.update { current ->
            val adjustedToAccount = if (current.selectedToAccount?.id == account.id) {
                resolveDefaultDestinationAccount(
                    accounts = current.accounts,
                    sourceAccount = account,
                    lastUsedToAccountId = settingsRepository.lastToAccountId
                )
            } else {
                current.selectedToAccount
            }
            current.copy(
                selectedAccount = account,
                selectedToAccount = adjustedToAccount,
                isLastUsedAccountDefault = (account.id == lastAccId && lastAccId > 0L),
                errorMessage = null
            )
        }
    }

    fun setSelectedToAccount(account: AccountEntity) {
        _uiState.update { it.copy(selectedToAccount = account, errorMessage = null) }
    }

    fun swapTransferAccounts() {
        hasUserSelectedAccount = true
        val lastAccId = settingsRepository.lastAccountId
        _uiState.update { current ->
            val from = current.selectedAccount
            val to = current.selectedToAccount
            if (from == null || to == null) {
                current
            } else {
                current.copy(
                    selectedAccount = to,
                    selectedToAccount = from,
                    isLastUsedAccountDefault = (to.id == lastAccId && lastAccId > 0L),
                    errorMessage = null
                )
            }
        }
    }

    fun setSelectedCategory(category: CategoryEntity) {
        _uiState.update { it.copy(selectedCategory = category, errorMessage = null) }
    }

    fun setDateTimestamp(timestamp: Long) {
        val hour = _uiState.value.selectedHour
        val minute = _uiState.value.selectedMinute
        val combined = com.example.util.DateFormatter.combineDateAndTime(timestamp, hour, minute)
        _uiState.update { it.copy(dateTimestamp = combined) }
    }

    fun setTime(hour: Int, minute: Int) {
        val combined = com.example.util.DateFormatter.combineDateAndTime(_uiState.value.dateTimestamp, hour, minute)
        _uiState.update {
            it.copy(
                selectedHour = hour,
                selectedMinute = minute,
                dateTimestamp = combined
            )
        }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    // --- Group Expense (Dong) Functions ---
    fun setGroupExpenseEnabled(enabled: Boolean) {
        _uiState.update { current ->
            val updatedShares = if (enabled && current.groupShareMode == GroupExpenseShareMode.EQUAL && current.groupShares.isNotEmpty() && current.amount > 0) {
                recalculateEqualShares(current.groupShares, current.amount)
            } else current.groupShares

            current.copy(
                isGroupExpenseEnabled = enabled,
                groupShares = updatedShares
            )
        }
    }

    fun setGroupShareMode(mode: GroupExpenseShareMode) {
        _uiState.update { current ->
            val updatedShares = if (mode == GroupExpenseShareMode.EQUAL && current.groupShares.isNotEmpty() && current.amount > 0) {
                recalculateEqualShares(current.groupShares, current.amount)
            } else current.groupShares

            current.copy(
                groupShareMode = mode,
                groupShares = updatedShares,
                errorMessage = null
            )
        }
    }

    fun addGroupPerson(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        _uiState.update { current ->
            if (current.groupShares.any { it.name.equals(trimmed, ignoreCase = true) }) {
                return@update current
            }
            val newList = current.groupShares + GroupShareInput(name = trimmed)
            val updated = if (current.groupShareMode == GroupExpenseShareMode.EQUAL && current.amount > 0) {
                recalculateEqualShares(newList, current.amount)
            } else newList

            current.copy(groupShares = updated, errorMessage = null)
        }
    }

    fun removeGroupPerson(id: String) {
        _uiState.update { current ->
            val newList = current.groupShares.filterNot { it.id == id }
            val updated = if (current.groupShareMode == GroupExpenseShareMode.EQUAL && current.amount > 0 && newList.isNotEmpty()) {
                recalculateEqualShares(newList, current.amount)
            } else newList

            current.copy(groupShares = updated, errorMessage = null)
        }
    }

    fun updateGroupPersonAmount(id: String, amount: Long) {
        _uiState.update { current ->
            val newList = current.groupShares.map {
                if (it.id == id) it.copy(amount = amount) else it
            }
            current.copy(groupShares = newList, errorMessage = null)
        }
    }

    private fun recalculateEqualShares(shares: List<GroupShareInput>, totalAmount: Long): List<GroupShareInput> {
        if (shares.isEmpty()) return shares
        // User themselves is part of the consumers, so total people = friends + 1 (user)
        val count = shares.size + 1
        val baseShare = totalAmount / count
        val remainder = totalAmount % count

        return shares.mapIndexed { index, item ->
            // First friend receives remainder to strictly ensure sum equals exact Rial
            val amount = if (index == 0) baseShare + remainder else baseShare
            item.copy(amount = amount)
        }
    }

    fun saveTransaction() {
        val state = _uiState.value
        if (state.isLoading || state.isSuccess) {
            return
        }
        if (state.amount <= 0L) {
            _uiState.update { it.copy(errorMessage = "لطفاً مبلغ معتبری وارد کنید") }
            return
        }
        if (state.selectedAccount == null) {
            _uiState.update { it.copy(errorMessage = "لطفاً حساب مبدا را انتخاب کنید") }
            return
        }
        if (state.type == TransactionType.TRANSFER && state.selectedToAccount == null) {
            _uiState.update { it.copy(errorMessage = "لطفاً حساب مقصد را انتخاب کنید") }
            return
        }
        if (state.type == TransactionType.TRANSFER && state.selectedAccount.id == state.selectedToAccount?.id) {
            _uiState.update { it.copy(errorMessage = "حساب مبدا و مقصد نمی‌توانند یکسان باشند") }
            return
        }
        if (state.type != TransactionType.TRANSFER && state.selectedCategory == null) {
            _uiState.update { it.copy(errorMessage = "لطفاً دسته‌بندی را انتخاب کنید") }
            return
        }

        // Validate Group Expense
        val isGroupExpense = state.type == TransactionType.EXPENSE && state.isGroupExpenseEnabled
        if (isGroupExpense) {
            if (state.groupShares.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "لطفاً حداقل نام یک دوست را برای هزینه مشترک وارد کنید") }
                return
            }
            if (state.groupShares.any { it.name.isBlank() }) {
                _uiState.update { it.copy(errorMessage = "نام تمام افراد در هزینه مشترک باید مشخص باشد") }
                return
            }
            if (state.groupShareMode == GroupExpenseShareMode.CUSTOM) {
                if (state.groupShares.any { it.amount < 0L }) {
                    _uiState.update { it.copy(errorMessage = "مبلغ سهم افراد نمی‌تواند منفی باشد") }
                    return
                }
                val sumFriendsShares = state.groupShares.sumOf { it.amount }
                if (sumFriendsShares > state.amount) {
                    val diff = sumFriendsShares - state.amount
                    val diffText = AmountFormatter.formatAmountWithCurrency(diff)
                    _uiState.update { it.copy(errorMessage = "مجموع سهم دوستان بیشتر از کل مبلغ هزینه است ($diffText مازاد)") }
                    return
                }
            }
        }

        // Synchronous guard prevents duplicate inserts from rapid double-clicks
        if (!isSavingGuard.compareAndSet(false, true)) {
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val finalTimestamp = com.example.util.DateFormatter.combineDateAndTime(
                    state.dateTimestamp,
                    state.selectedHour,
                    state.selectedMinute
                )

                val transaction = TransactionEntity(
                    type = state.type,
                    amount = state.amount,
                    accountId = state.selectedAccount.id,
                    toAccountId = if (state.type == TransactionType.TRANSFER) state.selectedToAccount?.id else null,
                    categoryId = if (state.type != TransactionType.TRANSFER) state.selectedCategory?.id else null,
                    date = finalTimestamp,
                    note = state.note.ifBlank { null }
                )

                val saveResult: Result<Long> = if (isGroupExpense) {
                    val finalShares = if (state.groupShareMode == GroupExpenseShareMode.EQUAL) {
                        recalculateEqualShares(state.groupShares, state.amount)
                    } else {
                        state.groupShares
                    }
                    addTransactionUseCase.addGroupExpenseWithShares(
                        expenseTransaction = transaction,
                        shareMode = state.groupShareMode,
                        shares = finalShares.map { it.name to it.amount },
                        repositoryOverride = groupExpenseRepository
                    ).map { it.transactionId }
                } else {
                    addTransactionUseCase(transaction)
                }

                saveResult.onSuccess {
                    settingsRepository.lastAccountId = state.selectedAccount.id
                    if (state.type == TransactionType.TRANSFER && state.selectedToAccount != null) {
                        settingsRepository.lastToAccountId = state.selectedToAccount.id
                    }
                    state.selectedCategory?.let { cat ->
                        settingsRepository.lastCategoryId = cat.id
                        if (state.type == TransactionType.EXPENSE) {
                            settingsRepository.lastExpenseCategoryId = cat.id
                        } else if (state.type == TransactionType.INCOME) {
                            settingsRepository.lastIncomeCategoryId = cat.id
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, errorMessage = null) }
                }.onFailure { ex ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = ex.message ?: "خطا در ثبت تراکنش"
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = e.message ?: "خطا در ثبت تراکنش"
                    )
                }
            } finally {
                isSavingGuard.set(false)
            }
        }
    }

    class Factory(
        private val initialTypeString: String,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val settingsRepository: SettingsRepository,
        private val addTransactionUseCase: AddTransactionUseCase,
        private val groupExpenseRepository: GroupExpenseRepository,
        private val initialGroupExpense: Boolean = false
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddTransactionViewModel(
                initialTypeString = initialTypeString,
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                settingsRepository = settingsRepository,
                addTransactionUseCase = addTransactionUseCase,
                groupExpenseRepository = groupExpenseRepository,
                initialGroupExpense = initialGroupExpense
            ) as T
        }
    }

    companion object {
        /**
         * Resolves the smart default account for manual transaction entry:
         * prefers the user's last-used account if it still exists in [accounts],
         * otherwise gracefully falls back to the first available account.
         */
        fun resolveDefaultAccount(
            accounts: List<AccountEntity>,
            lastUsedAccountId: Long
        ): Pair<AccountEntity?, Boolean> {
            if (accounts.isEmpty()) return null to false
            if (lastUsedAccountId > 0L) {
                val matched = accounts.find { it.id == lastUsedAccountId }
                if (matched != null) {
                    return matched to true
                }
            }
            return accounts.first() to false
        }

        /**
         * Resolves the destination account for transfers, preferring [lastUsedToAccountId]
         * when valid and distinct from [sourceAccount], otherwise the first other account.
         */
        fun resolveDefaultDestinationAccount(
            accounts: List<AccountEntity>,
            sourceAccount: AccountEntity?,
            lastUsedToAccountId: Long = 0L
        ): AccountEntity? {
            val candidates = accounts.filter { it.id != sourceAccount?.id }
            if (candidates.isEmpty()) return null
            if (lastUsedToAccountId > 0L) {
                candidates.find { it.id == lastUsedToAccountId }?.let { return it }
            }
            return candidates.first()
        }

        /**
         * Orders accounts for the horizontal quick-select strip so the active/default account
         * is immediately visible at the start without horizontal scrolling.
         */
        fun orderAccountsForQuickSelect(
            accounts: List<AccountEntity>,
            selectedAccountId: Long?
        ): List<AccountEntity> {
            if (selectedAccountId == null || accounts.size <= 1) return accounts
            val selected = accounts.find { it.id == selectedAccountId } ?: return accounts
            return listOf(selected) + accounts.filterNot { it.id == selectedAccountId }
        }

        fun filterCategoriesByType(
            categories: List<CategoryEntity>,
            type: TransactionType
        ): List<CategoryEntity> {
            return when (type) {
                TransactionType.EXPENSE -> categories.filter { it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH }
                TransactionType.INCOME -> categories.filter { it.type == CategoryType.INCOME || it.type == CategoryType.BOTH }
                TransactionType.TRANSFER -> emptyList()
            }
        }

        fun resolveDefaultCategory(
            categories: List<CategoryEntity>,
            type: TransactionType,
            lastExpenseCategoryId: Long,
            lastIncomeCategoryId: Long,
            lastCategoryId: Long,
            currentSelectedId: Long? = null
        ): CategoryEntity? {
            if (type == TransactionType.TRANSFER || categories.isEmpty()) return null
            if (currentSelectedId != null) {
                categories.find { it.id == currentSelectedId }?.let { return it }
            }
            val preferredCatId = when (type) {
                TransactionType.EXPENSE -> if (lastExpenseCategoryId > 0L) lastExpenseCategoryId else lastCategoryId
                TransactionType.INCOME -> if (lastIncomeCategoryId > 0L) lastIncomeCategoryId else lastCategoryId
                TransactionType.TRANSFER -> 0L
            }
            return categories.find { it.id == preferredCatId } ?: categories.firstOrNull()
        }
    }
}
