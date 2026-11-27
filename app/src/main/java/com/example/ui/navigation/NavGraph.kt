package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.FinTrackApp
import com.example.ui.screens.import_sms.SharedSmsHolder
import com.example.ui.screens.scan_sms.ScanSmsDataHolder
import com.example.ui.screens.scan_sms.ScannedSmsItem
import com.example.ui.screens.scan_sms.SharedScanViewModel
import com.example.util.SmsAccountMatcher
import com.example.util.SmsHashUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    app: FinTrackApp,
    initialSharedSmsText: String? = null,
    navigateToScanSms: Boolean = false,
    navigateToWidgetScan: Boolean = false,
    navController: NavHostController = rememberNavController(),
    onThemeChanged: (String) -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val sharedScanViewModel: SharedScanViewModel = viewModel()
    var isFullscreenMode by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        if (currentRoute != Screen.Transactions.route && currentRoute != Screen.Reports.route) {
            isFullscreenMode = false
        }
    }

    LaunchedEffect(initialSharedSmsText) {
        if (!initialSharedSmsText.isNullOrBlank()) {
            val allAccounts = try { app.appModule.accountRepository.getAllAccountsList() } catch (e: Exception) { emptyList() }
            val allSenders = try { app.appModule.accountSmsSenderRepository.getAllSendersList() } catch (e: Exception) { emptyList() }
            val patterns = try { app.appModule.smsPatternRepository.getActivePatternsList() } catch (e: Exception) { emptyList() }
            val sharedSender = SharedSmsHolder.sharedSender ?: ""
            val (parsed, senderAccount) = SmsAccountMatcher.parseSmsWithSenderPriority(
                body = initialSharedSmsText,
                smsSender = sharedSender,
                allAccounts = allAccounts,
                allSenders = allSenders,
                patterns = patterns
            )
            if (parsed.amount != null && parsed.amount > 0 && parsed.transactionType != null) {
                val effectiveParsed = parsed.copy(
                    bankName = if (parsed.bankName.isNullOrBlank()) senderAccount?.name else parsed.bankName,
                    accountIdentifier = if (parsed.accountIdentifier.isNullOrBlank()) (senderAccount?.cardNumber?.takeLast(4) ?: "") else parsed.accountIdentifier
                )
                val smsDate = parsed.date ?: System.currentTimeMillis()
                val hash = SmsHashUtil.calculateHash(
                    sender = sharedSender,
                    amount = parsed.amount,
                    date = smsDate,
                    text = initialSharedSmsText,
                    transactionType = parsed.transactionType,
                    accountIdentifier = effectiveParsed.accountIdentifier
                )
                val item = ScannedSmsItem(
                    id = hash,
                    parsedSms = effectiveParsed,
                    rawText = initialSharedSmsText,
                    smsHash = hash,
                    date = smsDate,
                    isRecognized = true,
                    isRegistered = false,
                    sender = sharedSender
                )
                sharedScanViewModel.setPendingScanItem(item)
                ScanSmsDataHolder.selectedItem = item
                navController.navigate(Screen.PreConfirm.route)
            } else {
                navController.navigate(Screen.ImportSms.createRoute(initialSharedSmsText))
            }
        }
    }

    LaunchedEffect(navigateToScanSms) {
        if (navigateToScanSms) {
            navController.navigate(Screen.ScanSms.route)
        }
    }

    LaunchedEffect(navigateToWidgetScan) {
        if (navigateToWidgetScan) {
            navController.navigate(Screen.WidgetScan.route) {
                launchSingleTop = true
            }
        }
    }

    val showBottomBar = (currentRoute in bottomNavRoutes) && !isFullscreenMode

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigationBar(
                    currentRoute = currentRoute,
                    navController = navController
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            addCoreRoutes(
                app = app,
                navController = navController,
                onToggleFullscreen = { isFullscreenMode = it }
            )
            addSmsRoutes(
                app = app,
                navController = navController,
                sharedScanViewModel = sharedScanViewModel
            )
            addManagementRoutes(
                app = app,
                navController = navController,
                onThemeChanged = onThemeChanged
            )
            addPlanningRoutes(
                app = app,
                navController = navController
            )
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
