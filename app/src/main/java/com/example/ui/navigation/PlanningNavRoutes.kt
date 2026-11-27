package com.example.ui.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.FinTrackApp
import com.example.ui.screens.cheques.AddChequeScreen
import com.example.ui.screens.cheques.AddChequeViewModel
import com.example.ui.screens.cheques.ChequesScreen
import com.example.ui.screens.cheques.ChequesViewModel
import com.example.ui.screens.debts.DebtsScreen
import com.example.ui.screens.debts.DebtsViewModel
import com.example.ui.screens.group_expenses.GroupExpensesScreen
import com.example.ui.screens.group_expenses.GroupExpensesViewModel
import com.example.ui.screens.plans.PlansScreen
import com.example.ui.screens.recurring.AddRecurringTransactionScreen
import com.example.ui.screens.recurring.AddRecurringTransactionViewModel
import com.example.ui.screens.recurring.RecurringTransactionsScreen
import com.example.ui.screens.recurring.RecurringTransactionsViewModel
import com.example.ui.screens.savings.AddSavingsGoalScreen
import com.example.ui.screens.savings.AddSavingsGoalViewModel
import com.example.ui.screens.savings.SavingsGoalsScreen
import com.example.ui.screens.savings.SavingsGoalsViewModel

internal fun NavGraphBuilder.addPlanningRoutes(
    app: FinTrackApp,
    navController: NavHostController
) {
    composable(Screen.Debts.route) {
        val viewModel: DebtsViewModel = viewModel(
            factory = DebtsViewModel.Factory(
                debtRepository = app.appModule.debtRepository,
                accountRepository = app.appModule.accountRepository,
                transactionRepository = app.appModule.transactionRepository
            )
        )
        DebtsScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(Screen.GroupExpenses.route) {
        val viewModel: GroupExpensesViewModel = viewModel(
            factory = GroupExpensesViewModel.Factory(
                groupExpenseRepository = app.appModule.groupExpenseRepository,
                accountRepository = app.appModule.accountRepository,
                categoryRepository = app.appModule.categoryRepository
            )
        )
        GroupExpensesScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAddGroupExpense = {
                navController.navigate(Screen.AddTransaction.createRoute("EXPENSE", groupExpense = true))
            }
        )
    }

    composable(Screen.Cheques.route) {
        val viewModel: ChequesViewModel = viewModel(
            factory = ChequesViewModel.Factory(
                chequeRepository = app.appModule.chequeRepository,
                accountRepository = app.appModule.accountRepository,
                transactionRepository = app.appModule.transactionRepository
            )
        )
        ChequesScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAdd = { navController.navigate(Screen.AddCheque.createRoute()) },
            onNavigateToEdit = { chequeId -> navController.navigate(Screen.AddCheque.createRoute(chequeId)) }
        )
    }

    composable(
        route = Screen.AddCheque.route,
        arguments = listOf(
            navArgument("chequeId") {
                type = NavType.LongType
                defaultValue = -1L
            }
        )
    ) { backStackEntry ->
        val argId = backStackEntry.arguments?.getLong("chequeId") ?: -1L
        val chequeId = if (argId > 0L) argId else null
        val viewModel: AddChequeViewModel = viewModel(
            factory = AddChequeViewModel.Factory(
                chequeRepository = app.appModule.chequeRepository,
                accountRepository = app.appModule.accountRepository,
                editChequeId = chequeId
            )
        )
        AddChequeScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(Screen.Recurring.route) {
        val viewModel: RecurringTransactionsViewModel = viewModel(
            factory = RecurringTransactionsViewModel.Factory(
                recurringRepository = app.appModule.recurringRepository,
                transactionRepository = app.appModule.transactionRepository
            )
        )
        RecurringTransactionsScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAdd = { navController.navigate(Screen.AddRecurring.createRoute()) },
            onNavigateToEdit = { recurringId -> navController.navigate(Screen.AddRecurring.createRoute(recurringId)) }
        )
    }

    composable(
        route = Screen.AddRecurring.route,
        arguments = listOf(
            navArgument("recurringId") {
                type = NavType.LongType
                defaultValue = -1L
            }
        )
    ) { backStackEntry ->
        val argId = backStackEntry.arguments?.getLong("recurringId") ?: -1L
        val recurringId = if (argId > 0L) argId else null
        val viewModel: AddRecurringTransactionViewModel = viewModel(
            factory = AddRecurringTransactionViewModel.Factory(
                recurringRepository = app.appModule.recurringRepository,
                accountRepository = app.appModule.accountRepository,
                categoryRepository = app.appModule.categoryRepository,
                editRecurringId = recurringId
            )
        )
        AddRecurringTransactionScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(Screen.SavingsGoals.route) {
        val viewModel: SavingsGoalsViewModel = viewModel(
            factory = SavingsGoalsViewModel.Factory(
                savingsGoalRepository = app.appModule.savingsGoalRepository,
                accountRepository = app.appModule.accountRepository,
                transactionRepository = app.appModule.transactionRepository
            )
        )
        SavingsGoalsScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() },
            onNavigateToAdd = { navController.navigate(Screen.AddSavingsGoal.createRoute()) },
            onNavigateToEdit = { goalId -> navController.navigate(Screen.AddSavingsGoal.createRoute(goalId)) }
        )
    }

    composable(
        route = Screen.AddSavingsGoal.route,
        arguments = listOf(
            navArgument("goalId") {
                type = NavType.LongType
                defaultValue = -1L
            }
        )
    ) { backStackEntry ->
        val argId = backStackEntry.arguments?.getLong("goalId") ?: -1L
        val goalId = if (argId > 0L) argId else null
        val viewModel: AddSavingsGoalViewModel = viewModel(
            factory = AddSavingsGoalViewModel.Factory(
                savingsGoalRepository = app.appModule.savingsGoalRepository,
                editGoalId = goalId
            )
        )
        AddSavingsGoalScreen(
            viewModel = viewModel,
            onNavigateBack = { navController.popBackStack() }
        )
    }

    composable(Screen.Plans.route) {
        val recurringViewModel: RecurringTransactionsViewModel = viewModel(
            factory = RecurringTransactionsViewModel.Factory(
                recurringRepository = app.appModule.recurringRepository,
                transactionRepository = app.appModule.transactionRepository
            )
        )
        val chequesViewModel: ChequesViewModel = viewModel(
            factory = ChequesViewModel.Factory(
                chequeRepository = app.appModule.chequeRepository,
                accountRepository = app.appModule.accountRepository,
                transactionRepository = app.appModule.transactionRepository
            )
        )
        val savingsGoalsViewModel: SavingsGoalsViewModel = viewModel(
            factory = SavingsGoalsViewModel.Factory(
                savingsGoalRepository = app.appModule.savingsGoalRepository,
                accountRepository = app.appModule.accountRepository,
                transactionRepository = app.appModule.transactionRepository
            )
        )

        PlansScreen(
            recurringViewModel = recurringViewModel,
            chequesViewModel = chequesViewModel,
            savingsGoalsViewModel = savingsGoalsViewModel,
            onNavigateToAddRecurring = { navController.navigate(Screen.AddRecurring.createRoute()) },
            onNavigateToEditRecurring = { recurringId -> navController.navigate(Screen.AddRecurring.createRoute(recurringId)) },
            onNavigateToAddCheque = { navController.navigate(Screen.AddCheque.createRoute()) },
            onNavigateToEditCheque = { chequeId -> navController.navigate(Screen.AddCheque.createRoute(chequeId)) },
            onNavigateToAddSavingsGoal = { navController.navigate(Screen.AddSavingsGoal.createRoute()) },
            onNavigateToEditSavingsGoal = { goalId -> navController.navigate(Screen.AddSavingsGoal.createRoute(goalId)) }
        )
    }
}
