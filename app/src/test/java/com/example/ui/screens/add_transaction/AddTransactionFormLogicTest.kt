package com.example.ui.screens.add_transaction

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FinTrackDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import com.example.data.local.entity.TransactionType
import com.example.data.repository.AccountRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.GroupExpenseRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.TransactionRepository
import com.example.domain.usecase.AddTransactionUseCase
import com.example.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AddTransactionFormLogicTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var database: FinTrackDatabase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var groupExpenseRepository: GroupExpenseRepository
    private lateinit var addTransactionUseCase: AddTransactionUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(com.example.util.Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()

        database = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        settingsRepository = SettingsRepository(Preferences(context))
        accountRepository = AccountRepository(database.accountDao())
        categoryRepository = CategoryRepository(database.categoryDao())
        transactionRepository = TransactionRepository(database.transactionDao(), database)
        groupExpenseRepository = GroupExpenseRepository(database)
        addTransactionUseCase = AddTransactionUseCase(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            groupExpenseRepository = groupExpenseRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    // --- Pure Logic Unit Tests ---

    @Test
    fun `resolveDefaultAccount selects last used account when valid and present`() {
        val acc1 = AccountEntity(id = 10L, name = "ملت", type = AccountType.BANK, initialBalance = 0L, color = 1, icon = "bank")
        val acc2 = AccountEntity(id = 20L, name = "سامان", type = AccountType.BANK, initialBalance = 0L, color = 2, icon = "bank")

        val (selected, isLastUsed) = AddTransactionViewModel.resolveDefaultAccount(
            accounts = listOf(acc1, acc2),
            lastUsedAccountId = 20L
        )

        assertEquals(20L, selected?.id)
        assertTrue(isLastUsed)
    }

    @Test
    fun `resolveDefaultAccount falls back to first account when last used was deleted or invalid`() {
        val acc1 = AccountEntity(id = 10L, name = "ملت", type = AccountType.BANK, initialBalance = 0L, color = 1, icon = "bank")
        val acc2 = AccountEntity(id = 20L, name = "سامان", type = AccountType.BANK, initialBalance = 0L, color = 2, icon = "bank")

        val (selected, isLastUsed) = AddTransactionViewModel.resolveDefaultAccount(
            accounts = listOf(acc1, acc2),
            lastUsedAccountId = 999L
        )

        assertEquals(10L, selected?.id)
        assertFalse(isLastUsed)
    }

    @Test
    fun `resolveDefaultAccount returns null when account list is empty`() {
        val (selected, isLastUsed) = AddTransactionViewModel.resolveDefaultAccount(
            accounts = emptyList(),
            lastUsedAccountId = 10L
        )

        assertNull(selected)
        assertFalse(isLastUsed)
    }

    @Test
    fun `resolveDefaultDestinationAccount prefers last destination and avoids source collision`() {
        val acc1 = AccountEntity(id = 1L, name = "ملت", type = AccountType.BANK, initialBalance = 0L, color = 1, icon = "bank")
        val acc2 = AccountEntity(id = 2L, name = "سامان", type = AccountType.BANK, initialBalance = 0L, color = 2, icon = "bank")
        val acc3 = AccountEntity(id = 3L, name = "بلو", type = AccountType.BANK, initialBalance = 0L, color = 3, icon = "bank")

        val preferred = AddTransactionViewModel.resolveDefaultDestinationAccount(
            accounts = listOf(acc1, acc2, acc3),
            sourceAccount = acc1,
            lastUsedToAccountId = 3L
        )
        assertEquals(3L, preferred?.id)

        val collisionFallback = AddTransactionViewModel.resolveDefaultDestinationAccount(
            accounts = listOf(acc1, acc2, acc3),
            sourceAccount = acc3,
            lastUsedToAccountId = 3L
        )
        assertEquals(1L, collisionFallback?.id)

        val singleAccount = AddTransactionViewModel.resolveDefaultDestinationAccount(
            accounts = listOf(acc1),
            sourceAccount = acc1,
            lastUsedToAccountId = 1L
        )
        assertNull(singleAccount)
    }

    @Test
    fun `orderAccountsForQuickSelect places selected default account first without losing others`() {
        val acc1 = AccountEntity(id = 1L, name = "ملت", type = AccountType.BANK, initialBalance = 0L, color = 1, icon = "bank")
        val acc2 = AccountEntity(id = 2L, name = "سامان", type = AccountType.BANK, initialBalance = 0L, color = 2, icon = "bank")
        val acc3 = AccountEntity(id = 3L, name = "بلو", type = AccountType.BANK, initialBalance = 0L, color = 3, icon = "bank")

        val ordered = AddTransactionViewModel.orderAccountsForQuickSelect(
            accounts = listOf(acc1, acc2, acc3),
            selectedAccountId = 3L
        )

        assertEquals(listOf(3L, 1L, 2L), ordered.map { it.id })
    }

    @Test
    fun `resolveDefaultCategory respects type-specific last category and falls back cleanly`() {
        val food = CategoryEntity(id = 1L, name = "خوراک", type = CategoryType.EXPENSE, color = 1, icon = "food")
        val bills = CategoryEntity(id = 2L, name = "قبوض", type = CategoryType.EXPENSE, color = 2, icon = "bill")
        val salary = CategoryEntity(id = 3L, name = "حقوق", type = CategoryType.INCOME, color = 3, icon = "salary")
        val both = CategoryEntity(id = 4L, name = "عمومی", type = CategoryType.BOTH, color = 4, icon = "other")

        val expenseFiltered = AddTransactionViewModel.filterCategoriesByType(listOf(food, bills, salary, both), TransactionType.EXPENSE)
        assertEquals(listOf(1L, 2L, 4L), expenseFiltered.map { it.id })

        val chosenExpense = AddTransactionViewModel.resolveDefaultCategory(
            categories = expenseFiltered,
            type = TransactionType.EXPENSE,
            lastExpenseCategoryId = 2L,
            lastIncomeCategoryId = 3L,
            lastCategoryId = 1L
        )
        assertEquals(2L, chosenExpense?.id)

        val chosenTransfer = AddTransactionViewModel.resolveDefaultCategory(
            categories = emptyList(),
            type = TransactionType.TRANSFER,
            lastExpenseCategoryId = 2L,
            lastIncomeCategoryId = 3L,
            lastCategoryId = 1L
        )
        assertNull(chosenTransfer)
    }

    // --- End-to-End ViewModel & Persistence Tests ---

    @Test
    fun `AddTransactionViewModel preselects last used account and persists updated selection on save`() = runBlocking {
        withTimeout(5_000L) {
            val acc1Id = database.accountDao().insert(
                AccountEntity(name = "نقدی", type = AccountType.CASH, initialBalance = 1_000_000L, color = 0xFF4CAF50.toInt(), icon = "wallet")
            )
            val acc2Id = database.accountDao().insert(
                AccountEntity(name = "کارت ملت", type = AccountType.BANK, initialBalance = 5_000_000L, color = 0xFF1E88E5.toInt(), icon = "bank")
            )
            val foodCatId = database.categoryDao().insert(
                CategoryEntity(name = "خوراک", type = CategoryType.EXPENSE, color = 0xFFE53935.toInt(), icon = "restaurant")
            )

            // Simulate user's previous transaction used acc2Id
            settingsRepository.lastAccountId = acc2Id

            val viewModel = AddTransactionViewModel(
                initialTypeString = "EXPENSE",
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                settingsRepository = settingsRepository,
                addTransactionUseCase = addTransactionUseCase,
                groupExpenseRepository = groupExpenseRepository
            )

            val loadedState = viewModel.uiState.first { it.accounts.size == 2 && it.selectedAccount != null }
            assertEquals(acc2Id, loadedState.selectedAccount?.id)
            assertTrue(loadedState.isLastUsedAccountDefault)
            assertEquals(foodCatId, loadedState.selectedCategory?.id)

            // Switch to acc1 and save a new transaction
            val acc1 = loadedState.accounts.first { it.id == acc1Id }
            viewModel.setSelectedAccount(acc1)
            viewModel.setAmount(250_000L)
            viewModel.saveTransaction()

            val savedState = viewModel.uiState.first { it.isSuccess }
            assertTrue(savedState.isSuccess)
            assertEquals(acc1Id, settingsRepository.lastAccountId)

            // Next form session automatically defaults to acc1Id
            val nextViewModel = AddTransactionViewModel(
                initialTypeString = "EXPENSE",
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                settingsRepository = settingsRepository,
                addTransactionUseCase = addTransactionUseCase,
                groupExpenseRepository = groupExpenseRepository
            )
            val nextState = nextViewModel.uiState.first { it.accounts.size == 2 && it.selectedAccount != null }
            assertEquals(acc1Id, nextState.selectedAccount?.id)
            assertTrue(nextState.isLastUsedAccountDefault)
        }
    }

    @Test
    fun `AddTransactionViewModel avoids source-destination collision and supports transfer swap`() = runBlocking {
        withTimeout(5_000L) {
            val acc1Id = database.accountDao().insert(
                AccountEntity(name = "کارت اول", type = AccountType.BANK, initialBalance = 2_000_000L, color = 0xFF1E88E5.toInt(), icon = "bank")
            )
            val acc2Id = database.accountDao().insert(
                AccountEntity(name = "کارت دوم", type = AccountType.BANK, initialBalance = 3_000_000L, color = 0xFF43A047.toInt(), icon = "bank")
            )
            settingsRepository.lastAccountId = 9999L // deleted/missing account

            val viewModel = AddTransactionViewModel(
                initialTypeString = "TRANSFER",
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                settingsRepository = settingsRepository,
                addTransactionUseCase = addTransactionUseCase,
                groupExpenseRepository = groupExpenseRepository
            )

            val initial = viewModel.uiState.first { it.accounts.size == 2 && it.selectedAccount != null }
            assertEquals(acc1Id, initial.selectedAccount?.id)
            assertEquals(acc2Id, initial.selectedToAccount?.id)
            assertFalse(initial.isLastUsedAccountDefault)

            // Selecting acc2 as source must auto-shift destination away from acc2
            val acc2 = initial.accounts.first { it.id == acc2Id }
            viewModel.setSelectedAccount(acc2)
            val afterCollisionFix = viewModel.uiState.value
            assertEquals(acc2Id, afterCollisionFix.selectedAccount?.id)
            assertEquals(acc1Id, afterCollisionFix.selectedToAccount?.id)

            // Swap reverses source and destination in one tap
            viewModel.swapTransferAccounts()
            val afterSwap = viewModel.uiState.value
            assertNotNull(afterSwap.selectedAccount)
            assertEquals(acc1Id, afterSwap.selectedAccount?.id)
            assertEquals(acc2Id, afterSwap.selectedToAccount?.id)
        }
    }
}
