package com.example.ui.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.FinTrackApp
import com.example.data.local.entity.TransactionType
import com.example.ui.screens.add_transaction.AddTransactionScreen
import com.example.ui.screens.add_transaction.AddTransactionViewModel
import com.example.ui.screens.add_transaction.EditTransactionScreen
import com.example.ui.screens.add_transaction.EditTransactionViewModel
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.more.MoreScreen
import com.example.ui.screens.reports.CategoryDetailScreen
import com.example.ui.screens.reports.CategoryDetailViewModel
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.reports.ReportsViewModel
import com.example.ui.screens.transactions.TransactionsScreen
import com.example.ui.screens.transactions.TransactionsViewModel
import com.example.ui.screens.transfer.TransferScreen
import com.example.ui.screens.transfer.TransferViewModel

internal fun NavGraphBuilder.addCoreRoutes(
    app: FinTrackApp,
    navController: NavHostController,
    onToggleFullscreen: (Boolean) -> Unit
) {
    composable(Screen.Dashboard.route) {
        val viewModel: DashboardViewModel = viewModel(
            factory = DashboardViewModel.Factory(app.appModule.getDashboardDataUseCase)
        )
        DashboardScreen(
            viewModel = viewModel,
            onNavigateToAddTransaction = { type ->
                navController.navigate(Screen.AddTransaction.createRoute(type))
            },
            onNavigateToTransactions = {
                navController.navigate(Screen.Transactions.route)
            },
            onNavigateToAccounts = {
                navController.navigate(Screen.Accounts.route)
            },
            onNavigateToCategories = {
                navController.navigate(Screen.Categories.route)
            },
            onNavigateToDebts = {
                navController.navigate(Screen.Debts.route)
            },
            onNavigateToReports = {
                navController.navigate(Screen.Reports.route)
            },
            onNavigateToScanSms = {
                navController.navigate(Screen.ScanSms.route)
            },
            onNavigateToImportSms = {
                navController.navigate(Screen.ImportSms.route)
            },
            onNavigateToTransfer = {
                navController.navigate(Screen.Transfer.route)
            },
            onNavigateToRecurring = {
                navController.navigate(Screen.Recurring.route)
            },
            onNavigateToSavingsGoals = {
                navController.navigate(Screen.SavingsGoals.route)
            },
            onNavigateToCheques = {
                navController.navigate(Screen.Cheques.route)
            },
            onNavigateToPlans = {
                navController.navigate(Screen.Plans.route)
            },
            onNavigateToSettings = {
                navController.navigate(Screen.Settings.route)
            }
        )
    }

    composable(
        route = Screen.AddTransaction.route,
        arguments = listOf(
            navArgument("type") {
                type = NavType.StringType
                defaultValue = "EXPENSE"
            }
        )
    ) { backStackEntry ->
        val type = backStackEntry.arguments?.getString("type") ?: "EXPENSE"
        val viewModel: AddTransactionViewModel = viewModel(
            factory = AddTransactionViewModel.Factory(
                initialTypeString = type,
                accountRepository = app.appModule.accountRepository,
                categoryRepository = app.appModule.categoryRepository,
                settingsRepository = app.appModule.settingsRepository,
                addTransactionUseCase = app.appModule.addTransactionUseCase,
                groupExpenseRepository = app.appModule.groupExpenseRepository
            )
        )
        AddTransactionScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(Screen.Transactions.route) {
        val viewModel: TransactionsViewModel = viewModel(
            factory = TransactionsViewModel.Factory(
                transactionRepository = app.appModule.transactionRepository,
                accountRepository = app.appModule.accountRepository,
                categoryRepository = app.appModule.categoryRepository
            )
        )
        TransactionsScreen(
            viewModel = viewModel,
            onNavigateToEditTransaction = { transactionId ->
                navController.navigate(Screen.EditTransaction.createRoute(transactionId))
            },
            onNavigateToAddTransaction = {
                navController.navigate(Screen.AddTransaction.createRoute("EXPENSE"))
            },
            onNavigateToScanSms = {
                navController.navigate(Screen.ScanSms.route)
            },
            onToggleFullscreen = onToggleFullscreen
        )
    }

    composable(
        route = Screen.EditTransaction.route,
        arguments = listOf(
            navArgument("transactionId") {
                type = NavType.LongType
            }
        )
    ) { backStackEntry ->
        val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: return@composable
        val viewModel: EditTransactionViewModel = viewModel(
            factory = EditTransactionViewModel.Factory(
                transactionId = transactionId,
                transactionRepository = app.appModule.transactionRepository,
                accountRepository = app.appModule.accountRepository,
                categoryRepository = app.appModule.categoryRepository
            )
        )
        EditTransactionScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(Screen.Transfer.route) {
        val viewModel: TransferViewModel = viewModel(
            factory = TransferViewModel.Factory(
                accountRepository = app.appModule.accountRepository,
                settingsRepository = app.appModule.settingsRepository,
                addTransactionUseCase = app.appModule.addTransactionUseCase
            )
        )
        TransferScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(Screen.Reports.route) {
        val viewModel: ReportsViewModel = viewModel(
            factory = ReportsViewModel.Factory(
                transactionRepository = app.appModule.transactionRepository,
                accountRepository = app.appModule.accountRepository,
                categoryRepository = app.appModule.categoryRepository
            )
        )
        ReportsScreen(
            viewModel = viewModel,
            onNavigateToAddTransaction = {
                navController.navigate(Screen.AddTransaction.createRoute("EXPENSE"))
            },
            onNavigateToScanSms = {
                navController.navigate(Screen.ScanSms.route)
            },
            onToggleFullscreen = onToggleFullscreen,
            onCategoryClick = { categoryId, start, end, accId, typeStr ->
                navController.navigate(
                    Screen.CategoryDetail.createRoute(
                        categoryId = categoryId,
                        startDate = start,
                        endDate = end,
                        accountId = accId,
                        type = typeStr
                    )
                )
            }
        )
    }

    composable(
        route = Screen.CategoryDetail.route,
        arguments = listOf(
            navArgument("categoryId") { type = NavType.LongType },
            navArgument("startDate") { type = NavType.LongType; defaultValue = 0L },
            navArgument("endDate") { type = NavType.LongType; defaultValue = Long.MAX_VALUE },
            navArgument("accountId") { type = NavType.LongType; defaultValue = -1L },
            navArgument("type") { type = NavType.StringType; defaultValue = "EXPENSE" }
        )
    ) { backStackEntry ->
        val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: return@composable
        val startDate = backStackEntry.arguments?.getLong("startDate") ?: 0L
        val endDate = backStackEntry.arguments?.getLong("endDate") ?: Long.MAX_VALUE
        val rawAccountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
        val accountId = if (rawAccountId > 0) rawAccountId else null
        val typeStr = backStackEntry.arguments?.getString("type") ?: "EXPENSE"
        val reportType = if (typeStr == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE

        val viewModel: CategoryDetailViewModel = viewModel(
            factory = CategoryDetailViewModel.Factory(
                categoryId = categoryId,
                startDate = startDate,
                endDate = endDate,
                accountId = accountId,
                reportType = reportType,
                categoryRepository = app.appModule.categoryRepository,
                transactionRepository = app.appModule.transactionRepository,
                accountRepository = app.appModule.accountRepository
            )
        )
        CategoryDetailScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(Screen.More.route) {
        MoreScreen(
            onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
            onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
            onNavigateToDebts = { navController.navigate(Screen.Debts.route) },
            onNavigateToGroupExpenses = { navController.navigate(Screen.GroupExpenses.route) },
            onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
            onNavigateToSavingsGoals = { navController.navigate(Screen.SavingsGoals.route) },
            onNavigateToCheques = { navController.navigate(Screen.Cheques.route) },
            onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
        )
    }
}
