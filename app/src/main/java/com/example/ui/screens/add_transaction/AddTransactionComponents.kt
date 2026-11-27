package com.example.ui.screens.add_transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.GroupExpenseShareMode
import com.example.data.local.entity.TransactionType
import com.example.ui.components.PersianAmountVisualTransformation
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.TransferColor
import com.example.util.AmountFormatter
import com.example.util.toPersianDigits

@Composable
internal fun TransactionTypeSegmentedSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        Triple(TransactionType.EXPENSE, "هزینه", ExpenseColor),
        Triple(TransactionType.INCOME, "درآمد", IncomeColor),
        Triple(TransactionType.TRANSFER, "انتقال", TransferColor)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (type, label, accentColor) ->
                val isSelected = selectedType == type
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) accentColor else Color.Transparent,
                    animationSpec = tween(220),
                    label = "pillBg_${type.name}"
                )
                val pillTextColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(220),
                    label = "pillText_${type.name}"
                )
                val pillScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.96f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "pillScale_${type.name}"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .scale(pillScale)
                        .clip(RoundedCornerShape(10.dp))
                        .background(pillBg)
                        .clickable { onTypeSelected(type) }
                        .testTag("tx_type_${type.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = pillTextColor
                    )
                }
            }
        }
    }
}

@Composable
internal fun QuickAccountSelector(
    accounts: List<AccountEntity>,
    selectedAccount: AccountEntity?,
    isLastUsedDefault: Boolean,
    onAccountSelected: (AccountEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val orderedAccounts = remember(accounts, selectedAccount?.id) {
        AddTransactionViewModel.orderAccountsForQuickSelect(accounts, selectedAccount?.id)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "حساب",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (isLastUsedDefault && selectedAccount != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "پیش‌فرض آخرین تراکنش",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (orderedAccounts.isEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "هیچ حسابی یافت نشد",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_account_strip"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(orderedAccounts, key = { it.id }) { account ->
                    val isSelected = account.id == selectedAccount?.id
                    val accountColor = Color(account.color)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onAccountSelected(account) },
                        modifier = Modifier.testTag("account_chip_${account.id}"),
                        label = {
                            Text(
                                text = account.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "انتخاب‌شده",
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accountColor)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

@Composable
internal fun TransferAccountsCard(
    accounts: List<AccountEntity>,
    selectedFromAccount: AccountEntity?,
    selectedToAccount: AccountEntity?,
    onFromAccountSelected: (AccountEntity) -> Unit,
    onToAccountSelected: (AccountEntity) -> Unit,
    onSwapAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "حساب مبدا",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onSwapAccounts,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("swap_transfer_accounts_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "جابه‌جایی حساب مبدا و مقصد",
                        tint = TransferColor
                    )
                }
            }
            AccountDropdown(
                accounts = accounts,
                selectedAccount = selectedFromAccount,
                onAccountSelected = onFromAccountSelected
            )

            Text(
                text = "حساب مقصد",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            AccountDropdown(
                accounts = accounts.filter { it.id != selectedFromAccount?.id },
                selectedAccount = selectedToAccount,
                onAccountSelected = onToAccountSelected
            )
        }
    }
}

@Composable
internal fun GroupExpenseSection(
    state: AddTransactionUiState,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectShareMode: (GroupExpenseShareMode) -> Unit,
    onAddPerson: (String) -> Unit,
    onRemovePerson: (String) -> Unit,
    onUpdatePersonAmount: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var newPersonName by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isGroupExpenseEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleEnabled(!state.isGroupExpenseEnabled) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = if (state.isGroupExpenseEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "هزینه مشترک (دنگ)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.isGroupExpenseEnabled && state.groupShares.isNotEmpty()) {
                                "${state.groupShares.size.toPersianDigits()} نفر اضافه شده"
                            } else {
                                "تقسیم مبلغ بین افراد و ثبت طلب"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = state.isGroupExpenseEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("group_expense_switch")
                )
            }

            AnimatedVisibility(
                visible = state.isGroupExpenseEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TabRow(
                        selectedTabIndex = if (state.groupShareMode == GroupExpenseShareMode.EQUAL) 0 else 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = state.groupShareMode == GroupExpenseShareMode.EQUAL,
                            onClick = { onSelectShareMode(GroupExpenseShareMode.EQUAL) },
                            text = { Text("تقسیم مساوی") }
                        )
                        Tab(
                            selected = state.groupShareMode == GroupExpenseShareMode.CUSTOM,
                            onClick = { onSelectShareMode(GroupExpenseShareMode.CUSTOM) },
                            text = { Text("سهم دلخواه") }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPersonName,
                            onValueChange = { newPersonName = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("نام دوست یا هم‌سفر...") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                if (newPersonName.isNotBlank()) {
                                    onAddPerson(newPersonName)
                                    newPersonName = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("افزودن")
                        }
                    }

                    val availableSuggestions = remember(state.recentPersonNames, state.groupShares) {
                        state.recentPersonNames.filter { name ->
                            state.groupShares.none { it.name.equals(name, ignoreCase = true) }
                        }
                    }
                    if (availableSuggestions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "افراد پیشنهادی اخیر:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(availableSuggestions, key = { it }) { name ->
                                    AssistChip(
                                        onClick = { onAddPerson(name) },
                                        label = { Text(name) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (state.groupShares.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "هنوز فردی به هزینه مشترک اضافه نشده است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.groupShares.forEach { person ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = person.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (state.groupShareMode == GroupExpenseShareMode.EQUAL) {
                                                    Text(
                                                        text = AmountFormatter.formatAmountWithCurrency(person.amount),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        if (state.groupShareMode == GroupExpenseShareMode.CUSTOM) {
                                            OutlinedTextField(
                                                value = if (person.amount > 0L) person.amount.toString() else "",
                                                onValueChange = { input ->
                                                    val sanitized = AmountFormatter.sanitizeAmountDigits(input)
                                                    val num = AmountFormatter.parseAmount(sanitized)
                                                    onUpdatePersonAmount(person.id, num)
                                                },
                                                modifier = Modifier.width(140.dp),
                                                placeholder = { Text("مبلغ (ریال)") },
                                                visualTransformation = remember { PersianAmountVisualTransformation() },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        IconButton(onClick = { onRemovePerson(person.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف فرد",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (state.groupShareMode == GroupExpenseShareMode.CUSTOM) {
                            val sumFriends = state.groupShares.sumOf { it.amount }
                            val myShare = state.amount - sumFriends

                            if (myShare < 0L) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "مازاد سهم دوستان:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = AmountFormatter.formatAmountWithCurrency(kotlin.math.abs(myShare)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "سهم خودم:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = AmountFormatter.formatAmountWithCurrency(myShare),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
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
}

@Composable
fun AccountDropdown(
    accounts: List<AccountEntity>,
    selectedAccount: AccountEntity?,
    onAccountSelected: (AccountEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = selectedAccount?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = selectedAccount?.name ?: "انتخاب حساب",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "انتخاب حساب",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text(acc.name, fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(acc.color))
                        )
                    },
                    onClick = {
                        onAccountSelected(acc)
                        expanded = false
                    }
                )
            }
        }
    }
}
