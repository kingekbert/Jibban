package com.example.ui.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.FinTrackApp
import com.example.ui.screens.import_sms.ImportSmsScreen
import com.example.ui.screens.import_sms.ImportSmsViewModel
import com.example.ui.screens.pattern_learner.SmsPatternLearnerScreen
import com.example.ui.screens.pattern_learner.SmsPatternLearnerViewModel
import com.example.ui.screens.scan_sms.PreConfirmScreen
import com.example.ui.screens.scan_sms.PreConfirmViewModel
import com.example.ui.screens.scan_sms.ScanSmsScreen
import com.example.ui.screens.scan_sms.ScanSmsViewModel
import com.example.ui.screens.scan_sms.SharedScanViewModel
import com.example.ui.screens.scan_sms.WidgetScanScreen

internal fun NavGraphBuilder.addSmsRoutes(
    app: FinTrackApp,
    navController: NavHostController,
    sharedScanViewModel: SharedScanViewModel
) {
    composable(
        route = Screen.ImportSms.route,
        arguments = listOf(
            navArgument("initialText") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val initialText = backStackEntry.arguments?.getString("initialText")
        val viewModel: ImportSmsViewModel = viewModel(
            factory = ImportSmsViewModel.Factory(
                accountRepository = app.appModule.accountRepository,
                categoryRepository = app.appModule.categoryRepository,
                smsPatternRepository = app.appModule.smsPatternRepository,
                addTransactionUseCase = app.appModule.addTransactionUseCase,
                accountSmsSenderRepository = app.appModule.accountSmsSenderRepository
            )
        )
        ImportSmsScreen(
            viewModel = viewModel,
            initialText = initialText,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToPatternLearner = { text ->
                navController.navigate(Screen.PatternLearner.createRoute(null, text))
            }
        )
    }

    composable(Screen.ScanSms.route) {
        val viewModel: ScanSmsViewModel = viewModel(
            factory = ScanSmsViewModel.Factory(
                transactionRepository = app.appModule.transactionRepository,
                accountRepository = app.appModule.accountRepository,
                smsPatternRepository = app.appModule.smsPatternRepository,
                accountSmsSenderRepository = app.appModule.accountSmsSenderRepository,
                reconciliationRepository = app.appModule.reconciliationRepository
            )
        )
        ScanSmsScreen(
            viewModel = viewModel,
            sharedScanViewModel = sharedScanViewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToPreConfirm = {
                navController.navigate(Screen.PreConfirm.route)
            },
            onNavigateToPatternLearnerWithText = { text ->
                navController.navigate(Screen.PatternLearner.createRoute(null, text))
            },
            onNavigateToReconciliation = { accId ->
                navController.navigate(Screen.Reconciliation.createRoute(accId))
            }
        )
    }

    composable(Screen.WidgetScan.route) {
        val viewModel: ScanSmsViewModel = viewModel(
            factory = ScanSmsViewModel.Factory(
                transactionRepository = app.appModule.transactionRepository,
                accountRepository = app.appModule.accountRepository,
                smsPatternRepository = app.appModule.smsPatternRepository,
                accountSmsSenderRepository = app.appModule.accountSmsSenderRepository
            )
        )
        WidgetScanScreen(
            viewModel = viewModel,
            sharedScanViewModel = sharedScanViewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToPreConfirm = {
                navController.navigate(Screen.PreConfirm.route)
            },
            onNavigateToFullScan = {
                navController.navigate(Screen.ScanSms.route)
            }
        )
    }

    composable(Screen.PreConfirm.route) {
        val viewModel: PreConfirmViewModel = viewModel(
            factory = PreConfirmViewModel.Factory(
                accountRepository = app.appModule.accountRepository,
                categoryRepository = app.appModule.categoryRepository,
                addTransactionUseCase = app.appModule.addTransactionUseCase,
                groupExpenseRepository = app.appModule.groupExpenseRepository,
                accountSmsSenderRepository = app.appModule.accountSmsSenderRepository,
                reconciliationRepository = app.appModule.reconciliationRepository
            )
        )

        PreConfirmScreen(
            viewModel = viewModel,
            sharedScanViewModel = sharedScanViewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToReconciliation = { accId ->
                navController.navigate(Screen.Reconciliation.createRoute(accId))
            }
        )
    }

    composable(
        route = Screen.PatternLearner.route,
        arguments = listOf(
            navArgument("accountId") {
                type = NavType.LongType
                defaultValue = -1L
            },
            navArgument("initialText") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("patternId") {
                type = NavType.LongType
                defaultValue = -1L
            }
        )
    ) { backStackEntry ->
        val argId = backStackEntry.arguments?.getLong("accountId") ?: -1L
        val accountId = if (argId > 0) argId else null
        val initialText = backStackEntry.arguments?.getString("initialText")
        val argPatternId = backStackEntry.arguments?.getLong("patternId") ?: -1L
        val patternId = if (argPatternId > 0) argPatternId else null
        val viewModel: SmsPatternLearnerViewModel = viewModel(
            factory = SmsPatternLearnerViewModel.Factory(
                smsPatternRepository = app.appModule.smsPatternRepository,
                accountRepository = app.appModule.accountRepository,
                accountId = accountId,
                initialSampleText = initialText,
                initialPatternId = patternId
            )
        )
        SmsPatternLearnerScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
