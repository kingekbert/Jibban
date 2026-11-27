package com.example.ui.screens.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.GroupedListRow
import com.example.ui.components.GroupedRowDivider
import com.example.ui.components.GroupedSectionCard
import com.example.ui.theme.goldAccent
import com.example.ui.theme.transferColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToAccounts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToGroupExpenses: () -> Unit = {},
    onNavigateToScanSms: () -> Unit = {},
    onNavigateToImportSms: () -> Unit = {},
    onNavigateToPatternLearner: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emerald = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.goldAccent
    val blue = MaterialTheme.transferColor
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بیشتر", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            GroupedSectionCard(title = "مدیریت مالی") {
                GroupedListRow(
                    title = "حساب‌ها",
                    subtitle = "مدیریت حساب‌های بانکی و کیف پول",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = emerald,
                    onClick = onNavigateToAccounts,
                    modifier = Modifier.testTag("more_accounts_item")
                )
                GroupedRowDivider()
                GroupedListRow(
                    title = "دسته‌بندی‌ها",
                    subtitle = "دسته‌های هزینه، درآمد و سقف بودجه",
                    icon = Icons.Default.Category,
                    iconTint = gold,
                    onClick = onNavigateToCategories,
                    modifier = Modifier.testTag("more_categories_item")
                )
                GroupedRowDivider()
                GroupedListRow(
                    title = "بدهی و طلب",
                    subtitle = "ثبت و پیگیری بدهی‌ها و طلب‌ها",
                    icon = Icons.Default.Handshake,
                    iconTint = blue,
                    onClick = onNavigateToDebts,
                    modifier = Modifier.testTag("more_debts_item")
                )
                GroupedRowDivider()
                GroupedListRow(
                    title = "هزینه‌های مشترک (دنگ)",
                    subtitle = "محاسبه دنگ سفر و دورهمی و تسویه",
                    icon = Icons.Default.Groups,
                    iconTint = emerald,
                    onClick = onNavigateToGroupExpenses,
                    modifier = Modifier.testTag("more_group_expenses_item")
                )
            }

            GroupedSectionCard(title = "پیامک‌های بانکی") {
                GroupedListRow(
                    title = "اسکن پیامک‌های بانکی",
                    subtitle = "شناسایی و ثبت تراکنش‌ها از پیامک‌های دریافتی",
                    icon = Icons.Default.Sms,
                    iconTint = emerald,
                    onClick = onNavigateToScanSms,
                    modifier = Modifier.testTag("more_scan_sms_item")
                )
                GroupedRowDivider()
                GroupedListRow(
                    title = "ورود دستی پیامک",
                    subtitle = "ثبت تراکنش از متن کپی‌شده پیامک",
                    icon = Icons.Default.ContentPaste,
                    iconTint = gold,
                    onClick = onNavigateToImportSms,
                    modifier = Modifier.testTag("more_import_sms_item")
                )
                GroupedRowDivider()
                GroupedListRow(
                    title = "الگوهای پیامک بانکی",
                    subtitle = "آموزش و مدیریت الگوی پیامک بانک‌ها",
                    icon = Icons.Default.AutoFixHigh,
                    iconTint = blue,
                    onClick = onNavigateToPatternLearner,
                    modifier = Modifier.testTag("more_pattern_learner_item")
                )
            }

            GroupedSectionCard(title = "برنامه") {
                GroupedListRow(
                    title = "تنظیمات",
                    subtitle = "پشتیبان‌گیری، امنیت، پوسته و اسکن خودکار",
                    icon = Icons.Default.Settings,
                    iconTint = neutral,
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("more_settings_item")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
