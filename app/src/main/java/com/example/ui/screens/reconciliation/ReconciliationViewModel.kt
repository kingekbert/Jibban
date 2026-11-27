package com.example.ui.screens.reconciliation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountBalanceAdjustmentEntity
import com.example.data.local.entity.AccountBalanceSnapshotEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionType
import com.example.data.repository.AccountRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ReconciliationRepository
import com.example.ui.screens.scan_sms.ScannedSmsItem
import com.example.util.ReconciliationCause
import com.example.util.ReconciliationHistoryItem
import com.example.widget.WidgetDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReconciliationUiState(
    val isLoading: Boolean = true,
    val account: AccountEntity? = null,
    val latestSnapshot: AccountBalanceSnapshotEntity? = null,
    val appBalance: Long = 0L,
    val bankBalance: Long = 0L,
    val diff: Long = 0L,
    val isSynced: Boolean = true,
    val cause: ReconciliationCause? = null,
    val history: List<ReconciliationHistoryItem> = emptyList(),
    val adjustments: List<AccountBalanceAdjustmentEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val unregisteredSmsCount: Int = 0,
    val userMessage: String? = null,
    val isSuccessMessage: Boolean = true
)

class ReconciliationViewModel(
    val accountId: Long,
    private val accountRepository: AccountRepository,
    private val reconciliationRepository: ReconciliationRepository,
    private val categoryRepository: CategoryRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReconciliationUiState())
    val uiState: StateFlow<ReconciliationUiState> = _uiState.asStateFlow()

    init {
        loadDiagnosis()
    }

    fun loadDiagnosis() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                runCatching { reconciliationRepository.cleanupInvalidFutureSnapshots(accountId) }
                val account = accountRepository.getAccountById(accountId).first()
                val categories = try { categoryRepository.allCategories.first() } catch (e: Exception) { emptyList() }
                val widgetCandidates = try { WidgetDataStore.getItems(appContext) } catch (e: Exception) { emptyList() }

                // Inspect recent SMS inbox for this account to detect unregistered SMS & sync latest bank balance
                val smsInspection = runCatching {
                    com.example.util.BackgroundSmsScanner.inspectAccountSms(
                        context = appContext,
                        accountId = accountId,
                        days = 30
                    )
                }.getOrNull()

                val unregisteredCount = smsInspection?.unregisteredCount ?: 0

                // Only update latest snapshot from bank SMS when all recognized SMS of this account are registered
                if (unregisteredCount == 0 &&
                    smsInspection?.latestBalance != null &&
                    smsInspection.latestSmsHash.isNotBlank()
                ) {
                    runCatching {
                        reconciliationRepository.recordSnapshot(
                            accountId = accountId,
                            smsDate = smsInspection.latestSmsDate,
                            balance = smsInspection.latestBalance,
                            smsHash = smsInspection.latestSmsHash
                        )
                    }
                }

                val mergedCandidates = (widgetCandidates + (smsInspection?.unregisteredItems ?: emptyList()))
                    .distinctBy { it.smsHash }

                val diag = reconciliationRepository.getReconciliationDiagnosis(accountId, mergedCandidates)
                if (diag != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            account = diag.account,
                            latestSnapshot = diag.latestSnapshot,
                            appBalance = diag.appBalance,
                            bankBalance = diag.bankBalance,
                            diff = diag.diff,
                            isSynced = diag.isSynced,
                            cause = diag.cause,
                            history = diag.history,
                            adjustments = diag.adjustments,
                            categories = categories,
                            unregisteredSmsCount = unregisteredCount
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            account = account,
                            categories = categories,
                            unregisteredSmsCount = unregisteredCount
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "خطا در بارگذاری اطلاعات تطبیق مانده: ${e.localizedMessage}",
                        isSuccessMessage = false
                    )
                }
            }
        }
    }

    fun fixCandidate(item: ScannedSmsItem) {
        viewModelScope.launch {
            try {
                val ok = reconciliationRepository.fixUnregisteredTransactions(accountId, listOf(item))
                if (ok) {
                    WidgetDataStore.removeItem(appContext, item.smsHash)
                    _uiState.update {
                        it.copy(
                            userMessage = "تراکنش با موفقیت ثبت شد و مانده مجدداً محاسبه شد.",
                            isSuccessMessage = true
                        )
                    }
                    loadDiagnosis()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "خطا در ثبت تراکنش: ${e.localizedMessage}", isSuccessMessage = false)
                }
            }
        }
    }

    fun fixAllCandidates(items: List<ScannedSmsItem>) {
        viewModelScope.launch {
            try {
                val ok = reconciliationRepository.fixUnregisteredTransactions(accountId, items)
                if (ok) {
                    items.forEach { WidgetDataStore.removeItem(appContext, it.smsHash) }
                    _uiState.update {
                        it.copy(
                            userMessage = "${items.size} تراکنش کاندید ثبت شد و مانده همخوان گردید.",
                            isSuccessMessage = true
                        )
                    }
                    loadDiagnosis()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "خطا در ثبت تراکنش‌ها: ${e.localizedMessage}", isSuccessMessage = false)
                }
            }
        }
    }

    fun fixBankFee(feeAmount: Long, date: Long) {
        viewModelScope.launch {
            try {
                val ok = reconciliationRepository.fixBankFee(accountId, feeAmount, date)
                if (ok) {
                    _uiState.update {
                        it.copy(
                            userMessage = "کارمزد بانکی ثبت شد و اختلاف مانده برطرف گردید.",
                            isSuccessMessage = true
                        )
                    }
                    loadDiagnosis()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "خطا در ثبت کارمزد: ${e.localizedMessage}", isSuccessMessage = false)
                }
            }
        }
    }

    fun fixInitialBalance(diff: Long) {
        viewModelScope.launch {
            try {
                val ok = reconciliationRepository.fixInitialBalance(accountId, diff)
                if (ok) {
                    _uiState.update {
                        it.copy(
                            userMessage = "مانده اولیه حساب اصلاح شد و مانده همخوان گردید.",
                            isSuccessMessage = true
                        )
                    }
                    loadDiagnosis()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "خطا در اصلاح مانده اولیه: ${e.localizedMessage}", isSuccessMessage = false)
                }
            }
        }
    }

    fun fixManualAdjustment(
        amount: Long,
        type: TransactionType,
        categoryId: Long,
        note: String,
        date: Long
    ) {
        viewModelScope.launch {
            try {
                val ok = reconciliationRepository.fixManualAdjustment(accountId, amount, type, categoryId, note, date)
                if (ok) {
                    _uiState.update {
                        it.copy(
                            userMessage = "تراکنش اصلاحی دستی ثبت شد و مانده به‌روزرسانی گردید.",
                            isSuccessMessage = true
                        )
                    }
                    loadDiagnosis()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "خطا در ثبت تراکنش اصلاحی: ${e.localizedMessage}", isSuccessMessage = false)
                }
            }
        }
    }

    fun reconcileIsolated(
        signedDelta: Long,
        date: Long,
        note: String = "تراز مانده حساب (بدون اثر بر گزارش‌ها)"
    ) {
        viewModelScope.launch {
            try {
                val ok = reconciliationRepository.reconcileBalanceIsolated(
                    accountId = accountId,
                    signedDelta = signedDelta,
                    date = date,
                    note = note
                )
                if (ok) {
                    _uiState.update {
                        it.copy(
                            userMessage = "مانده حساب بدون ثبت هزینه یا درآمد کاذب تراز شد.",
                            isSuccessMessage = true
                        )
                    }
                    loadDiagnosis()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "خطا در تراز مانده حساب: ${e.localizedMessage}", isSuccessMessage = false)
                }
            }
        }
    }

    fun reconcileToTargetBalance(
        targetBankBalance: Long,
        date: Long,
        note: String = "تراز مستقیم با مانده بانک"
    ) {
        viewModelScope.launch {
            try {
                val ok = reconciliationRepository.reconcileToTargetBankBalance(
                    accountId = accountId,
                    targetBankBalance = targetBankBalance,
                    date = date,
                    note = note
                )
                if (ok) {
                    _uiState.update {
                        it.copy(
                            userMessage = "موجودی حساب بدون تغییر در آمار هزینه‌ها و درآمدها با بانک همخوان شد.",
                            isSuccessMessage = true
                        )
                    }
                    loadDiagnosis()
                } else {
                    _uiState.update {
                        it.copy(
                            userMessage = "موجودی حساب هم‌اکنون با مبلغ واردشده برابر است.",
                            isSuccessMessage = true
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "خطا در تراز موجودی: ${e.localizedMessage}", isSuccessMessage = false)
                }
            }
        }
    }

    fun deleteBalanceAdjustment(adjustmentId: Long, showMessage: Boolean = true) {
        viewModelScope.launch {
            try {
                reconciliationRepository.deleteBalanceAdjustment(adjustmentId)
                if (showMessage) {
                    _uiState.update {
                        it.copy(
                            userMessage = "تعدیل تراز مانده حذف و موجودی مجدداً محاسبه شد.",
                            isSuccessMessage = true
                        )
                    }
                }
                loadDiagnosis()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "خطا در حذف تعدیل: ${e.localizedMessage}", isSuccessMessage = false)
                }
            }
        }
    }

    fun restoreBalanceAdjustment(adjustment: AccountBalanceAdjustmentEntity) {
        viewModelScope.launch {
            try {
                reconciliationRepository.restoreBalanceAdjustment(adjustment)
                _uiState.update {
                    it.copy(
                        userMessage = "تعدیل تراز مانده بازگردانی شد و موجودی مجدداً محاسبه شد.",
                        isSuccessMessage = true
                    )
                }
                loadDiagnosis()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "خطا در بازگردانی تعدیل: ${e.localizedMessage}", isSuccessMessage = false)
                }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    class Factory(
        private val accountId: Long,
        private val accountRepository: AccountRepository,
        private val reconciliationRepository: ReconciliationRepository,
        private val categoryRepository: CategoryRepository,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReconciliationViewModel(
                accountId,
                accountRepository,
                reconciliationRepository,
                categoryRepository,
                appContext
            ) as T
        }
    }
}
