package com.example.ui.screens.pattern_learner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ExtractedField
import com.example.data.local.entity.FieldType
import com.example.data.local.entity.SmsPatternEntity
import com.example.ui.components.EmptyState
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.util.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmsPatternLearnerScreen(
    viewModel: SmsPatternLearnerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var patternToDelete by remember { mutableStateOf<SmsPatternEntity?>(null) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateBack()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("یادگیری الگوی پیامک", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Edit Banner
            if (state.editingPatternId != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "در حال ویرایش الگوی ذخیره‌شده",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.cancelEdit() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("انصراف", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Account selection
            item {
                Text("حساب مرتبط", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.accounts, key = { it.id }) { acc ->
                        val isSelected = state.selectedAccountId == acc.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectAccount(acc.id) },
                            label = { Text(acc.name) }
                        )
                    }
                }
            }

            // Bank Name & Identifier
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.bankName,
                        onValueChange = { viewModel.setBankName(it) },
                        label = { Text("نام بانک") },
                        placeholder = { Text("مثلاً تجارت، ملی") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = state.accountIdentifier,
                        onValueChange = { viewModel.setAccountIdentifier(it) },
                        label = { Text("شناسه / شماره حساب") },
                        placeholder = { Text("مثلاً شماره حساب یا شبا") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Sample SMS Input
            item {
                Text("متن پیامک نمونه بانک", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.sampleSms,
                    onValueChange = { viewModel.setSampleSms(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    placeholder = { Text("متن کامل یکی از پیامک‌های واریز یا برداشت این بانک را اینجا جای‌گذاری کنید...") },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.analyzeSms() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تحلیل و تفکیک اجزای پیامک", fontWeight = FontWeight.Bold)
                }
            }

            // Extracted tokens list
            if (state.extractedFields.isNotEmpty()) {
                item {
                    Text(
                        "تطبیق بخش‌های پیامک",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "برای بخش‌هایی مثل مبلغ، نام بانک، و نوع تراکنش برچسب مربوطه را انتخاب کنید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(
                    count = state.extractedFields.size,
                    key = { index -> "${index}_${state.extractedFields[index].text}" }
                ) { index ->
                    val field = state.extractedFields[index]
                    ExtractedFieldRow(
                        field = field,
                        onTypeSelected = { selectedType ->
                            viewModel.updateFieldType(index, selectedType)
                        }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.editingPatternId != null) {
                            OutlinedButton(
                                onClick = { viewModel.cancelEdit() },
                                modifier = Modifier
                                    .weight(0.35f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("انصراف")
                            }
                        }
                        Button(
                            onClick = { viewModel.savePattern() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.editingPatternId != null) "به‌روزرسانی الگوی هوشمند" else "ذخیره الگوی هوشمند",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Existing Saved Patterns (Searchable & Filterable)
            item {
                val filteredCount = state.filteredPatterns.size
                val totalCount = state.existingPatterns.size

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الگوهای فعال ذخیره‌شده",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (totalCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = if (state.hasActivePatternFilters) {
                                    "${filteredCount.toPersianDigits()} از ${totalCount.toPersianDigits()} الگو"
                                } else {
                                    "${totalCount.toPersianDigits()} الگو"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (state.existingPatterns.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        EmptyState(
                            icon = Icons.Default.AutoAwesome,
                            title = "هنوز الگوی پیامکی ذخیره نشده است",
                            subtitle = "متن یک پیامک بانکی را در کادر بالا وارد کنید و دکمه «تحلیل و تفکیک اجزای پیامک» را بزنید تا اولین الگوی هوشمند شما ساخته شود.",
                            buttonText = null,
                            compact = true,
                            testTag = "patterns_empty_state"
                        )
                    }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.patternSearchQuery,
                            onValueChange = { viewModel.setPatternSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pattern_search_input"),
                            placeholder = { Text("جستجو بر اساس بانک، شماره حساب یا متن پیامک...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "جستجو در الگوها",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = if (state.patternSearchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { viewModel.setPatternSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "پاک کردن جستجو"
                                        )
                                    }
                                }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        val hasFilterOptions = state.selectedAccount != null || state.availableBankFilters.isNotEmpty()
                        if (hasFilterOptions) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pattern_filter_chips"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item(key = "filter_all") {
                                    FilterChip(
                                        selected = !state.filterOnlySelectedAccount && state.selectedBankFilter == null,
                                        onClick = {
                                            viewModel.setFilterOnlySelectedAccount(false)
                                            viewModel.setBankFilter(null)
                                        },
                                        label = { Text("همه (${state.existingPatterns.size.toPersianDigits()})") },
                                        modifier = Modifier.testTag("pattern_filter_all")
                                    )
                                }

                                state.selectedAccount?.let { selectedAcc ->
                                    item(key = "filter_selected_account") {
                                        FilterChip(
                                            selected = state.filterOnlySelectedAccount,
                                            onClick = {
                                                viewModel.setFilterOnlySelectedAccount(!state.filterOnlySelectedAccount)
                                            },
                                            label = { Text("حساب: ${selectedAcc.name}") },
                                            modifier = Modifier.testTag("pattern_filter_selected_account")
                                        )
                                    }
                                }

                                items(state.availableBankFilters, key = { "bank_$it" }) { bank ->
                                    val isBankSelected = state.selectedBankFilter != null &&
                                        SmsPatternLearnerViewModel.normalizeSearchText(state.selectedBankFilter!!) ==
                                        SmsPatternLearnerViewModel.normalizeSearchText(bank)
                                    FilterChip(
                                        selected = isBankSelected,
                                        onClick = { viewModel.setBankFilter(bank) },
                                        label = { Text(bank) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.filteredPatterns.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            EmptyState(
                                icon = Icons.Default.SearchOff,
                                title = "الگویی با این جستجو یا فیلتر پیدا نشد",
                                subtitle = "عبارت جستجو را تغییر دهید یا فیلترهای فعال را پاک کنید تا همه الگوهای ذخیره‌شده نمایش داده شوند.",
                                buttonText = "پاک کردن جستجو و فیلتر",
                                buttonIcon = Icons.Default.FilterAltOff,
                                onButtonClick = { viewModel.clearPatternSearchAndFilters() },
                                compact = true,
                                testTag = "patterns_filtered_empty_state"
                            )
                        }
                    }
                } else {
                    items(state.filteredPatterns, key = { it.id }) { pattern ->
                        val isCurrentlyEditing = state.editingPatternId == pattern.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.editPattern(pattern)
                                    coroutineScope.launch { listState.animateScrollToItem(0) }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentlyEditing)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = if (isCurrentlyEditing)
                                androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            pattern.bankName ?: "بانک نامشخص",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (isCurrentlyEditing) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "در حال ویرایش",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    if (pattern.accountIdentifier.isNotBlank()) {
                                        Text(
                                            "شناسه: ${pattern.accountIdentifier}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        pattern.sampleSms.take(60) + if (pattern.sampleSms.length > 60) "..." else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            viewModel.editPattern(pattern)
                                            coroutineScope.launch { listState.animateScrollToItem(0) }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "ویرایش",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { patternToDelete = pattern }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    patternToDelete?.let { pattern ->
        val patternTitle = pattern.bankName?.takeIf { it.isNotBlank() } ?: "بانک نامشخص"
        AlertDialog(
            onDismissRequest = { patternToDelete = null },
            title = { Text("حذف الگوی پیامک", fontWeight = FontWeight.Bold) },
            text = {
                Text("آیا از حذف الگوی پیامک «$patternTitle» مطمئن هستید؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val deletedPattern = pattern
                        viewModel.deletePattern(deletedPattern.id)
                        patternToDelete = null
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = "الگوی «$patternTitle» حذف شد",
                                actionLabel = "بازگردانی",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restorePattern(deletedPattern)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseColor)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { patternToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun ExtractedFieldRow(
    field: ExtractedField,
    onTypeSelected: (FieldType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentType = field.fieldType ?: FieldType.IGNORE

    val chipBgColor = when (currentType) {
        FieldType.AMOUNT -> IncomeColor.copy(alpha = 0.2f)
        FieldType.TRANSACTION_TYPE, FieldType.SIGN -> MaterialTheme.colorScheme.primaryContainer
        FieldType.BANK_NAME -> MaterialTheme.colorScheme.secondaryContainer
        FieldType.ACCOUNT_IDENTIFIER -> MaterialTheme.colorScheme.tertiaryContainer
        FieldType.DATE_YEAR, FieldType.DATE_MONTH, FieldType.DATE_DAY, FieldType.TIME_HOUR, FieldType.TIME_MINUTE -> MaterialTheme.colorScheme.surfaceVariant
        FieldType.BALANCE -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
        FieldType.IGNORE -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = chipBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "خط ${(field.lineIndex + 1).toPersianDigits()}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = field.text,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = currentType.persianLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (currentType != FieldType.IGNORE) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    FieldType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    type.persianLabel,
                                    fontWeight = if (type == currentType) FontWeight.Bold else FontWeight.Normal,
                                    color = if (type == currentType) MaterialTheme.colorScheme.primary else Color.Unspecified
                                )
                            },
                            onClick = {
                                onTypeSelected(type)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
