package com.example.ui.screens.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtType
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.repository.AccountRepository
import com.example.data.repository.DebtRepository
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DebtsUiState(
    val selectedTab: DebtType = DebtType.OWED_TO_ME,
    val debts: List<DebtEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val totalOwedToMe: Long = 0L,
    val totalIOwe: Long = 0L,
    val showAddDialog: Boolean = false,
    val isEditing: Boolean = false,
    val personName: String = "",
    val amount: Long = 0L,
    val note: String = "",
    val dueDate: Long? = null,
    val dialogType: DebtType = DebtType.OWED_TO_ME,
    val userMessage: String? = null,
    val isLoading: Boolean = false
)

private data class DebtDialogFormState(
    val showDialog: Boolean = false,
    val editingDebt: DebtEntity? = null,
    val personName: String = "",
    val amount: Long = 0L,
    val note: String = "",
    val dueDate: Long? = null,
    val dialogType: DebtType = DebtType.OWED_TO_ME
)

class DebtsViewModel(
    private val debtRepository: DebtRepository,
    private val accountRepository: AccountRepository? = null,
    private val transactionRepository: TransactionRepository? = null
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(DebtType.OWED_TO_ME)
    private val _formState = MutableStateFlow(DebtDialogFormState())
    private val _userMessage = MutableStateFlow<String?>(null)

    private val accountsFlow = accountRepository?.allAccounts ?: flowOf(emptyList())

    private val _headerFlow = combine(
        _selectedTab,
        _formState,
        _userMessage,
        accountsFlow
    ) { tab, form, msg, accounts ->
        Pair(Pair(tab, form), Pair(msg, accounts))
    }

    val uiState: StateFlow<DebtsUiState> = combine(
        _headerFlow,
        debtRepository.allDebts,
        debtRepository.totalOwedToMe,
        debtRepository.totalIOwe
    ) { header, allDebts, totalOwed, totalOwe ->
        val (tab, form) = header.first
        val (msg, accounts) = header.second
        val filtered = allDebts.filter { it.type == tab }
        DebtsUiState(
            selectedTab = tab,
            debts = filtered,
            accounts = accounts,
            totalOwedToMe = totalOwed,
            totalIOwe = totalOwe,
            showAddDialog = form.showDialog,
            isEditing = form.editingDebt != null,
            personName = form.personName,
            amount = form.amount,
            note = form.note,
            dueDate = form.dueDate,
            dialogType = form.dialogType,
            userMessage = msg,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebtsUiState(isLoading = true)
    )

    fun setSelectedTab(tab: DebtType) {
        _selectedTab.value = tab
    }

    fun openAddDialog(defaultType: DebtType = _selectedTab.value) {
        _formState.value = DebtDialogFormState(
            showDialog = true,
            editingDebt = null,
            personName = "",
            amount = 0L,
            note = "",
            dueDate = null,
            dialogType = defaultType
        )
    }

    fun openEditDialog(debt: DebtEntity) {
        _formState.value = DebtDialogFormState(
            showDialog = true,
            editingDebt = debt,
            personName = debt.personName,
            amount = debt.amount,
            note = debt.note.orEmpty(),
            dueDate = debt.dueDate,
            dialogType = debt.type
        )
    }

    fun closeAddDialog() {
        _formState.update { it.copy(showDialog = false, editingDebt = null) }
    }

    fun setDialogType(type: DebtType) {
        _formState.update { it.copy(dialogType = type) }
    }

    fun setPersonName(name: String) {
        _formState.update { it.copy(personName = name) }
    }

    fun setAmount(amount: Long) {
        _formState.update { it.copy(amount = amount) }
    }

    fun setNote(note: String) {
        _formState.update { it.copy(note = note) }
    }

    fun setDueDate(timestamp: Long?) {
        _formState.update { it.copy(dueDate = timestamp) }
    }

    fun saveDebt() {
        val form = _formState.value
        val name = form.personName.trim()
        val amt = form.amount
        if (name.isBlank() || amt <= 0L) return

        viewModelScope.launch {
            val existing = form.editingDebt
            if (existing != null) {
                debtRepository.updateDebt(
                    existing.copy(
                        personName = name,
                        type = form.dialogType,
                        amount = amt,
                        dueDate = form.dueDate,
                        note = form.note.ifBlank { null }
                    )
                )
                _userMessage.value = "اطلاعات با موفقیت ویرایش شد"
            } else {
                val debt = DebtEntity(
                    personName = name,
                    type = form.dialogType,
                    amount = amt,
                    dueDate = form.dueDate,
                    note = form.note.ifBlank { null }
                )
                debtRepository.insertDebt(debt)
            }
            _formState.update { it.copy(showDialog = false, editingDebt = null) }
        }
    }

    fun toggleSettled(debt: DebtEntity) {
        viewModelScope.launch {
            debtRepository.markAsSettled(debt, !debt.isSettled)
        }
    }

    fun settleDebt(debt: DebtEntity, accountId: Long?) {
        viewModelScope.launch {
            debtRepository.markAsSettled(debt, true)
            if (accountId != null && transactionRepository != null) {
                val txType = if (debt.type == DebtType.OWED_TO_ME) {
                    TransactionType.INCOME
                } else {
                    TransactionType.EXPENSE
                }
                val notePrefix = if (debt.type == DebtType.OWED_TO_ME) {
                    "تسویه طلب از ${debt.personName}"
                } else {
                    "تسویه بدهی به ${debt.personName}"
                }
                val fullNote = if (!debt.note.isNullOrBlank()) "$notePrefix (${debt.note})" else notePrefix
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        type = txType,
                        amount = debt.amount,
                        accountId = accountId,
                        categoryId = null,
                        date = System.currentTimeMillis(),
                        note = fullNote
                    )
                )
                _userMessage.value = "تسویه انجام شد و تراکنش در حساب ثبت گردید"
            } else {
                _userMessage.value = "به عنوان تسویه‌شده علامت‌گذاری شد"
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            debtRepository.deleteDebt(debt)
        }
    }

    fun restoreDebt(debt: DebtEntity) {
        viewModelScope.launch {
            debtRepository.insertDebt(debt.copy(id = 0L))
        }
    }

    class Factory(
        private val debtRepository: DebtRepository,
        private val accountRepository: AccountRepository? = null,
        private val transactionRepository: TransactionRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DebtsViewModel(debtRepository, accountRepository, transactionRepository) as T
        }
    }
}
