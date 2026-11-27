package com.example.ui.screens.cheques

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ChequeEntity
import com.example.data.local.entity.ChequeStatus
import com.example.data.local.entity.ChequeType
import androidx.compose.material.icons.filled.WarningAmber
import com.example.ui.components.EmptyState
import com.example.ui.components.StatusPill
import com.example.ui.components.subtleCardBorder
import com.example.ui.theme.expenseColor
import com.example.ui.theme.incomeColor
import com.example.util.AmountFormatter
import com.example.util.DateFormatter
import com.example.util.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChequesScreen(
    viewModel: ChequesViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Long) -> Unit = {},
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var chequeToClear by remember { mutableStateOf<ChequeEntity?>(null) }
    var chequeToDelete by remember { mutableStateOf<ChequeEntity?>(null) }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text("دفترچه چک‌های صیادی", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "بازگشت"
                                )
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "ثبت چک جدید")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val expenseTint = MaterialTheme.expenseColor
            val incomeTint = MaterialTheme.incomeColor

            // Header Stats Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pending Payable
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = subtleCardBorder(0.55f),
                    colors = CardDefaults.cardColors(containerColor = expenseTint.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "پرداختی در انتظار",
                            style = MaterialTheme.typography.bodySmall,
                            color = expenseTint
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AmountFormatter.format(state.pendingPayableTotal),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = expenseTint
                        )
                    }
                }

                // Pending Receivable
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = subtleCardBorder(0.55f),
                    colors = CardDefaults.cardColors(containerColor = incomeTint.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "دریافتی در انتظار",
                            style = MaterialTheme.typography.bodySmall,
                            color = incomeTint
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = AmountFormatter.format(state.pendingReceivableTotal),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = incomeTint
                        )
                    }
                }
            }

            // Filter Chips (replaces nested TabRow)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ChequeFilterTab.values().toList(), key = { it.name }) { tab ->
                    FilterChip(
                        selected = state.activeTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (state.activeTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Cheques List
            if (state.filteredCheques.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "هیچ چکی ثبت نشده است",
                        icon = Icons.Default.Receipt,
                        buttonText = "ثبت چک جدید",
                        onButtonClick = onNavigateToAdd
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.filteredCheques, key = { it.id }) { cheque ->
                        ChequeItemCard(
                            cheque = cheque,
                            onClearClicked = { chequeToClear = cheque },
                            onEdit = { onNavigateToEdit(cheque.id) },
                            onStatusChange = { newStatus -> viewModel.updateStatus(cheque, newStatus) },
                            onDelete = { chequeToDelete = cheque }
                        )
                    }
                }
            }
        }
    }

    // Clear Cheque Dialog
    chequeToClear?.let { cheque ->
        ClearChequeDialog(
            cheque = cheque,
            accounts = state.accounts,
            onConfirm = { accountId ->
                viewModel.clearChequeWithAccount(cheque, accountId)
                chequeToClear = null
            },
            onDismiss = { chequeToClear = null }
        )
    }

    // Delete Confirmation Dialog
    chequeToDelete?.let { cheque ->
        AlertDialog(
            onDismissRequest = { chequeToDelete = null },
            title = { Text("حذف چک", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف چک صیادی شماره «${cheque.sayadNumber}» مطمئن هستید؟".toPersianDigits()) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.delete(cheque)
                        chequeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.expenseColor)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { chequeToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun ChequeItemCard(
    cheque: ChequeEntity,
    onClearClicked: () -> Unit,
    onEdit: () -> Unit = {},
    onStatusChange: (ChequeStatus) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPayable = cheque.type == ChequeType.PAYABLE
    val expenseTint = MaterialTheme.expenseColor
    val incomeTint = MaterialTheme.incomeColor
    val typeColor = if (isPayable) expenseTint else incomeTint
    val now = System.currentTimeMillis()
    val isPastDue = cheque.status == ChequeStatus.PENDING && cheque.dueDate < now
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = subtleCardBorder(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPastDue) expenseTint.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Sayad Number Badge + Type Badge + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isPayable) "پرداختی" else "دریافتی",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "صیاد: ${cheque.sayadNumber}".toPersianDigits(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "گزینه‌ها")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("ویرایش چک") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تنظیم به در انتظار") },
                            onClick = {
                                onStatusChange(ChequeStatus.PENDING)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تنظیم به برگشت‌خورده") },
                            onClick = {
                                onStatusChange(ChequeStatus.BOUNCED)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تنظیم به باطل‌شده") },
                            onClick = {
                                onStatusChange(ChequeStatus.CANCELLED)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف چک", color = expenseTint) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = expenseTint) },
                            onClick = {
                                onDelete()
                                showMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Party Name + Bank Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isPayable) "در وجه: ${cheque.partyName}" else "صادرکننده: ${cheque.partyName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "بانک ${cheque.bankName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount + Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "مبلغ چک: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AmountFormatter.format(cheque.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (isPastDue) expenseTint else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "سررسید: ${DateFormatter.formatJalali(cheque.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPastDue) expenseTint else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isPastDue) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Badge & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusText = when (cheque.status) {
                    ChequeStatus.PENDING -> "در انتظار سررسید"
                    ChequeStatus.CLEARED -> "پاس‌شده / وصول‌شده"
                    ChequeStatus.BOUNCED -> "برگشت‌خورده"
                    ChequeStatus.CANCELLED -> "باطل‌شده"
                }
                val statusColor = when (cheque.status) {
                    ChequeStatus.PENDING -> MaterialTheme.colorScheme.primary
                    ChequeStatus.CLEARED -> incomeTint
                    ChequeStatus.BOUNCED -> expenseTint
                    ChequeStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                if (cheque.status == ChequeStatus.BOUNCED) {
                    StatusPill(
                        text = statusText,
                        icon = Icons.Default.WarningAmber,
                        tint = expenseTint
                    )
                } else {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (cheque.status == ChequeStatus.PENDING) {
                    Button(
                        onClick = onClearClicked,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = incomeTint)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPayable) "پاس شد" else "وصول شد", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearChequeDialog(
    cheque: ChequeEntity,
    accounts: List<AccountEntity>,
    onConfirm: (accountId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    val isPayable = cheque.type == ChequeType.PAYABLE
    val title = if (isPayable) "ثبت پاس شدن چک پرداختی" else "ثبت وصول چک دریافتی"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isPayable)
                        "مبلغ ${AmountFormatter.format(cheque.amount)} از حساب انتخابی کسر و به عنوان هزینه ثبت خواهد شد."
                    else
                        "مبلغ ${AmountFormatter.format(cheque.amount)} به حساب انتخابی واریز و به عنوان درآمد ثبت خواهد شد.",
                    style = MaterialTheme.typography.bodySmall
                )

                ExposedDropdownMenuBox(
                    expanded = accountDropdownExpanded,
                    onExpandedChange = { accountDropdownExpanded = it }
                ) {
                    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "انتخاب حساب...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("حساب بانکی مربوطه") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccountId = account.id
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedAccountId) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.incomeColor)
            ) {
                Text("تأیید و تسویه")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
