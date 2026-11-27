package com.example.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MoreSectionHeader(title = "مدیریت مالی")

            MoreMenuItem(
                title = "حساب‌ها",
                subtitle = "مدیریت و مشاهده حساب‌های بانکی و کیف پول",
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToAccounts,
                testTag = "more_accounts_item"
            )

            MoreMenuItem(
                title = "دسته‌بندی‌ها",
                subtitle = "مدیریت دسته‌های هزینه و درآمد و بودجه‌بندی",
                icon = Icons.Default.Category,
                iconColor = MaterialTheme.colorScheme.secondary,
                onClick = onNavigateToCategories,
                testTag = "more_categories_item"
            )

            MoreMenuItem(
                title = "بدهی و طلب",
                subtitle = "مدیریت و پیگیری بدهی‌ها و طلب‌ها با دیگران",
                icon = Icons.Default.Handshake,
                iconColor = MaterialTheme.colorScheme.tertiary,
                onClick = onNavigateToDebts,
                testTag = "more_debts_item"
            )

            MoreMenuItem(
                title = "هزینه‌های مشترک (دنگ)",
                subtitle = "پیگیری دنگ‌های سفر و دورهمی، تسویه و اشتراک‌گذاری",
                icon = Icons.Default.Groups,
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToGroupExpenses,
                testTag = "more_group_expenses_item"
            )

            Spacer(modifier = Modifier.height(6.dp))
            MoreSectionHeader(title = "پیامک‌های بانکی")

            MoreMenuItem(
                title = "اسکن پیامک‌های بانکی",
                subtitle = "شناسایی و ثبت خودکار تراکنش‌ها از پیامک‌های دریافتی",
                icon = Icons.Default.Sms,
                iconColor = Color(0xFF00ACC1),
                onClick = onNavigateToScanSms,
                testTag = "more_scan_sms_item"
            )

            MoreMenuItem(
                title = "ورود دستی پیامک",
                subtitle = "تحلیل و ثبت سریع تراکنش از متن کپی‌شده پیامک",
                icon = Icons.Default.ContentPaste,
                iconColor = Color(0xFFE91E63),
                onClick = onNavigateToImportSms,
                testTag = "more_import_sms_item"
            )

            MoreMenuItem(
                title = "الگوهای پیامک بانکی",
                subtitle = "آموزش، جستجو و مدیریت الگوی پیامک بانک‌ها",
                icon = Icons.Default.AutoFixHigh,
                iconColor = Color(0xFF7B1FA2),
                onClick = onNavigateToPatternLearner,
                testTag = "more_pattern_learner_item"
            )

            Spacer(modifier = Modifier.height(6.dp))
            MoreSectionHeader(title = "تنظیمات برنامه")

            MoreMenuItem(
                title = "تنظیمات",
                subtitle = "پشتیبان‌گیری، امنیت، پوسته و تنظیمات برنامه",
                icon = Icons.Default.Settings,
                iconColor = MaterialTheme.colorScheme.outline,
                onClick = onNavigateToSettings,
                testTag = "more_settings_item"
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MoreSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun MoreMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
