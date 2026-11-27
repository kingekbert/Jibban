package com.example.ui.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.FinTrackApp
import com.example.ui.screens.accounts.AccountDetailScreen
import com.example.ui.screens.accounts.AccountDetailViewModel
import com.example.ui.screens.accounts.AccountsScreen
import com.example.ui.screens.accounts.AccountsViewModel
import com.example.ui.screens.accounts.AddAccountScreen
import com.example.ui.screens.accounts.AddAccountViewModel
import com.example.ui.screens.categories.AddCategoryScreen
import com.example.ui.screens.categories.AddCategoryViewModel
import com.example.ui.screens.categories.CategoriesScreen
import com.example.ui.screens.categories.CategoriesViewModel
import com.example.ui.screens.reconciliation.ReconciliationScreen
import com.example.ui.screens.reconciliation.ReconciliationViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel

internal fun NavGraphBuilder.addManagementRoutes(
    app: FinTrackApp,
    navController: NavHostController,
    onThemeChanged: (String) -> Unit
) {
    composable(Screen.Accounts.route) {
        val accountsViewModel: AccountsViewModel = viewModel(
            factory = AccountsViewModel.Factory(
                accountRepository = app.appModule.accountRepository,
                smsPatternRepository = app.appModule.smsPatternRepository
            )
        )
        AccountsScreen(
            viewModel = accountsViewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddAccount = {
                navController.navigate(Screen.AddAccount.createRoute(null))
            },
            onNavigateToEditAccount = { accountId ->
                navController.navigate(Screen.AddAccount.createRoute(accountId))
            },
            onNavigateToAccountDetails = { accountId ->
                navController.navigate(Screen.AccountDetails.createRoute(accountId))
            },
            onNavigateToPatternLearner = { accountId ->
                navController.navigate(Screen.PatternLearner.createRoute(accountId))
            }
        )
    }

    composable(
        route = Screen.AddAccount.route,
        arguments = listOf(
            navArgument("accountId") {
                type = NavType.LongType
                defaultValue = -1L
            }
        )
    ) { backStackEntry ->
        val argId = backStackEntry.arguments?.getLong("accountId") ?: -1L
        val accountId = if (argId > 0) argId else null
        val viewModel: AddAccountViewModel = viewModel(
            factory = AddAccountViewModel.Factory(
                accountId,
                app.appModule.accountRepository,
                app.appModule.accountSmsSenderRepository
            )
        )
        AddAccountScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToPatternLearner = { accId ->
                navController.navigate(Screen.PatternLearner.createRoute(accId))
            }
        )
    }

    composable(
        route = Screen.AccountDetails.route,
        arguments = listOf(
            navArgument("accountId") {
                type = NavType.LongType
            }
        )
    ) { backStackEntry ->
        val accountId = backStackEntry.arguments?.getLong("accountId") ?: return@composable
        val viewModel: AccountDetailViewModel = viewModel(
            factory = AccountDetailViewModel.Factory(
                accountId = accountId,
                accountRepository = app.appModule.accountRepository,
                transactionRepository = app.appModule.transactionRepository,
                smsPatternRepository = app.appModule.smsPatternRepository,
                reconciliationRepository = app.appModule.reconciliationRepository
            )
        )
        AccountDetailScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToEditPattern = { accId, patId ->
                navController.navigate(Screen.PatternLearner.createRoute(accountId = accId, patternId = patId))
            },
            onNavigateToAddPattern = { accId ->
                navController.navigate(Screen.PatternLearner.createRoute(accountId = accId))
            },
            onNavigateToReconciliation = { accId ->
                navController.navigate(Screen.Reconciliation.createRoute(accId))
            }
        )
    }

    composable(
        route = Screen.Reconciliation.route,
        arguments = listOf(
            navArgument("accountId") {
                type = NavType.LongType
            }
        )
    ) { backStackEntry ->
        val accountId = backStackEntry.arguments?.getLong("accountId") ?: return@composable
        val viewModel: ReconciliationViewModel = viewModel(
            factory = ReconciliationViewModel.Factory(
                accountId = accountId,
                accountRepository = app.appModule.accountRepository,
                reconciliationRepository = app.appModule.reconciliationRepository,
                categoryRepository = app.appModule.categoryRepository,
                appContext = app
            )
        )
        ReconciliationScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(Screen.Categories.route) {
        val viewModel: CategoriesViewModel = viewModel(
            factory = CategoriesViewModel.Factory(app.appModule.categoryRepository)
        )
        CategoriesScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddCategory = { type: String ->
                navController.navigate(Screen.AddCategory.createRoute(null, type))
            },
            onNavigateToEditCategory = { categoryId: Long, type: String ->
                navController.navigate(Screen.AddCategory.createRoute(categoryId, type))
            }
        )
    }

    composable(
        route = Screen.AddCategory.route,
        arguments = listOf(
            navArgument("categoryId") {
                type = NavType.LongType
                defaultValue = -1L
            },
            navArgument("type") {
                type = NavType.StringType
                defaultValue = "EXPENSE"
            }
        )
    ) { backStackEntry ->
        val argId = backStackEntry.arguments?.getLong("categoryId") ?: -1L
        val categoryId = if (argId > 0) argId else null
        val type = backStackEntry.arguments?.getString("type") ?: "EXPENSE"
        val viewModel: AddCategoryViewModel = viewModel(
            factory = AddCategoryViewModel.Factory(categoryId, type, app.appModule.categoryRepository)
        )
        AddCategoryScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(Screen.Settings.route) {
        val viewModel: SettingsViewModel = viewModel(
            factory = SettingsViewModel.Factory(
                settingsRepository = app.appModule.settingsRepository,
                appLockRepository = app.appModule.appLockRepository,
                backupManager = app.appModule.backupManager,
                accountRepository = app.appModule.accountRepository,
                onThemeChanged = onThemeChanged
            )
        )
        SettingsScreen(
            viewModel = viewModel,
            onNavigateToScanSms = {
                navController.navigate(Screen.ScanSms.route)
            },
            onNavigateToPatternLearner = { accountId ->
                navController.navigate(Screen.PatternLearner.createRoute(accountId))
            }
        )
    }
}
