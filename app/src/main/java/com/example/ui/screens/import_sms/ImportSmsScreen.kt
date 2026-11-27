package com.example.ui.screens.import_sms

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.components.AmountInput
import com.example.ui.components.CategoryPicker
import com.example.ui.components.FinTrackTimePickerDialog
import com.example.ui.components.JalaliDatePickerDialog
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.util.BankLogoBadge
import com.example.util.DateFormatter
import com.example.util.toPersianDigits
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSmsScreen(
    viewModel: ImportSmsViewModel,
    onNavigateBack: () -> Unit,
    initialText: String? = null,
    onNavigateToPatternLearner: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isSmsExpanded by remember { mutableStateOf(false) }
    var showAllCategories by remember { mutableStateOf(false) }

    LaunchedEffect(initialText) {
        val raw = if (!initialText.isNullOrBlank() && initialText != "{initialText}") {
            try {
                java.net.URLDecoder.decode(initialText, "UTF-8")
            } catch (_: Exception) {
                initialText
            }
        } else {
            SharedSmsHolder.sharedText
        }

        if (!raw.isNullOrBlank()) {
            viewModel.setInitialSmsText(raw, autoParse = true)
            SharedSmsHolder.sharedText = null
        }
    }

    val (currentHour, currentMinute) = remember(state.dateTimestamp) {
        DateFormatter.extractHourAndMinute(state.dateTimestamp)
    }

    LaunchedEffect(state.isSavedSuccess) {
        if (state.isSavedSuccess) {
            Toast.makeText(context, "تراکنش با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearErrorMessage()
        }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialTimestamp = state.dateTimestamp,
            onDismiss = { showDatePicker = false },
            onDateSelected = { timestamp ->
                val newTimestamp = DateFormatter.combineDateAndTime(timestamp, currentHour, currentMinute)
                viewModel.setDateTimestamp(newTimestamp)
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        FinTrackTimePickerDialog(
            initialHour = currentHour,
            initialMinute = currentMinute,
            onTimeSelected = { h, m ->
                val newTimestamp = DateFormatter.combineDateAndTime(state.dateTimestamp, h, m)
                viewModel.setDateTimestamp(newTimestamp)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    val activeColor = if (state.transactionType == TransactionType.INCOME) IncomeColor else ExpenseColor

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ورود دستی پیامک بانکی", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = state.isAnalyzed) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.imePadding()
                ) {
                    Button(
                        onClick = { viewModel.saveTransaction() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(52.dp)
                            .testTag("import_sms_save_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تأیید و ثبت تراکنش",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // SMS Input Card (Collapsible once analyzed)
            if (state.isAnalyzed && !isSmsExpanded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "پیامک تحلیل شد",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { isSmsExpanded = true }) {
                            Text("تغییر متن", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "متن پیامک بانک",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (state.isAnalyzed) {
                                TextButton(onClick = { isSmsExpanded = false }) {
                                    Text("بستن", fontSize = 12.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.smsText,
                            onValueChange = { viewModel.onSmsTextChange(it) },
                            placeholder = { Text("متن پیامک را اینجا جای‌گذاری کنید...") },
                            minLines = 4,
                            maxLines = 7,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_sms_text_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                                    if (pasted.isNotBlank()) {
                                        viewModel.setInitialSmsText(pasted, autoParse = true)
                                        isSmsExpanded = false
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("متنی در کلیپ‌بورد یافت نشد")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("import_sms_paste_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("جای‌گذاری", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    viewModel.parseSms()
                                    isSmsExpanded = false
                                },
                                enabled = state.smsText.isNotBlank(),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("import_sms_analyze_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تحلیل پیامک", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Analysis Result Banner + Form
            if (state.isAnalyzed) {
                if (state.isRecognized) {
                    val pRes = state.parsedResult
                    val bank = pRes?.bankName ?: "بانک ناشناخته"
                    val typeText = if (state.transactionType == TransactionType.INCOME) "واریز / درآمد" else "برداشت / هزینه"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = IncomeColor.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = IncomeColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "پیامک شناسایی شد: $bank — $typeText",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeColor
                                )
                                Text(
                                    text = "اطلاعات استخراج‌شده را بررسی و تأیید کنید",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = ExpenseColor.copy(alpha = 0.12f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ExpenseColor,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "الگوی این پیامک شناسایی نشد",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseColor
                                    )
                                    Text(
                                        text = "می‌توانید اطلاعات را دستی ثبت کنید یا یک‌بار الگوی آن را به جیب‌بان یاد دهید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (onNavigateToPatternLearner != null && state.smsText.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { onNavigateToPatternLearner(state.smsText) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("import_sms_learn_pattern_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تعریف الگوی این پیامک", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Streamlined Transaction Form Card (aligned with PreConfirmScreen)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Segmented Expense / Income Pill Toggle
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val isExpense = state.transactionType == TransactionType.EXPENSE
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (isExpense) ExpenseColor else Color.Transparent)
                                        .clickable { viewModel.setTransactionType(TransactionType.EXPENSE) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "هزینه (برداشت)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (!isExpense) IncomeColor else Color.Transparent)
                                        .clickable { viewModel.setTransactionType(TransactionType.INCOME) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "درآمد (واریز)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Amount Input
                        AmountInput(
                            amount = state.amount,
                            onAmountChange = { viewModel.setAmount(it) },
                            color = activeColor,
                            autoFocus = false,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Account Selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "حساب",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            AccountSelectorChips(
                                accounts = state.accounts,
                                selectedAccountId = state.selectedAccountId,
                                onAccountSelected = { viewModel.setSelectedAccount(it) }
                            )
                        }

                        // Compact Category Picker (Top 8 + Expandable)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "دسته‌بندی",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (state.categories.size > 8) {
                                    TextButton(
                                        onClick = { showAllCategories = !showAllCategories },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.testTag("import_sms_toggle_all_categories")
                                    ) {
                                        Text(
                                            text = if (showAllCategories) "نمایش کمتر" else "همه دسته‌ها (${state.categories.size.toPersianDigits()})",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                            CategoryPicker(
                                categories = state.categories,
                                selectedCategoryId = state.selectedCategoryId,
                                onCategorySelected = { viewModel.setSelectedCategory(it.id) },
                                maxVisibleCount = if (showAllCategories) null else 8,
                                prioritizeSelected = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Date and Time Selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = DateFormatter.formatMedium(state.dateTimestamp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = DateFormatter.formatTime(currentHour, currentMinute),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Note / Description
                        OutlinedTextField(
                            value = state.note,
                            onValueChange = { viewModel.setNote(it) },
                            placeholder = { Text("توضیحات اختیاری...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun AccountSelectorChips(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (accounts.isEmpty()) {
        Text("هیچ حسابی تعریف نشده است", color = MaterialTheme.colorScheme.error)
        return
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(accounts, key = { it.id }) { acc ->
            val isSelected = acc.id == selectedAccountId
            val accColor = Color(acc.color)

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) accColor.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accColor else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onAccountSelected(acc.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BankLogoBadge(
                    bankId = acc.logoResName,
                    accountName = acc.name,
                    cardNumber = acc.cardNumber,
                    size = 20.dp,
                    shapeRadius = 6.dp,
                    fallbackColor = accColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = acc.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
