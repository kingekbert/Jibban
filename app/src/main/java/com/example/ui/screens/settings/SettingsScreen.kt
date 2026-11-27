package com.example.ui.screens.settings

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.BiometricAuthStatus
import com.example.util.BiometricHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToScanSms: () -> Unit = {},
    onNavigateToPatternLearner: ((Long?) -> Unit) = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAccountSelectionDialog by remember { mutableStateOf(false) }
    var showRestoreOptionsDialog by remember { mutableStateOf(false) }
    var showPasteJsonDialog by remember { mutableStateOf(false) }
    var jsonInputText by remember { mutableStateOf("") }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showPlainWarningDialog by remember { mutableStateOf(false) }
    var showPlainExportOptionsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Document Creator Launcher for Exporting Encrypted Backup (.ftb)
    val createFtbDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { destinationUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bytes = viewModel.generateEncryptedBackup()
                    context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        outputStream.write(bytes)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "فایل پشتیبان امن (.ftb) ذخیره شد", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "خطا در ذخیره فایل پشتیبان: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Document Creator Launcher for Exporting Plain Backup (.json)
    val createJsonDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { destinationUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val jsonContent = viewModel.generatePlainBackupJson()
                    context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "فایل پشتیبان JSON در حافظه ذخیره شد", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "خطا در ذخیره فایل پشتیبان: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val handleSelectedBackupUri: (android.net.Uri?) -> Unit = { uri ->
        uri?.let { sourceUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bytes = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                        inputStream.readBytes()
                    }
                    withContext(Dispatchers.Main) {
                        if (bytes != null && bytes.isNotEmpty()) {
                            viewModel.onBytesSelectedForRestore(bytes)
                        } else {
                            Toast.makeText(context, "فایل انتخاب‌شده خالی است", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "خطا در خواندن فایل: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Primary SAF Document Picker Launcher for Importing Backup (.ftb or .json)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = handleSelectedBackupUri
    )

    // Fallback GetContent Launcher for devices with custom file pickers
    val getContentFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = handleSelectedBackupUri
    )

    // Last-resort explicit chooser launcher
    val chooserFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            handleSelectedBackupUri(result.data?.data)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SMS Patterns Card
            item {
                Card(
                    onClick = { showAccountSelectionDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pattern,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "الگوهای پیامک بانکی",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تعریف و یادگیری الگوی پیامک برای حساب‌ها",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Scan SMS Card
            item {
                Card(
                    onClick = onNavigateToScanSms,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "اسکن پیامک‌های بانکی",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "شناسایی خودکار پیامک‌های ۳۰ روز اخیر",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Security & App Lock Card
            item {
                val hostActivity = remember(context) { context.findFragmentActivity() }
                SecuritySettingsCard(
                    isLockEnabled = state.isBiometricEnabled,
                    lockGraceMs = state.lockGraceMs,
                    isSecureScreenEnabled = state.isSecureScreenEnabled,
                    onToggleLock = { targetEnabled ->
                        val status = BiometricHelper.checkBiometricStatus(context)
                        if (targetEnabled) {
                            if (status != BiometricAuthStatus.AVAILABLE) {
                                viewModel.showSecurityError(
                                    "ابتدا در تنظیمات دستگاه، رمز صفحه یا اثر انگشت فعال کنید."
                                )
                            } else if (hostActivity != null) {
                                BiometricHelper.authenticate(
                                    activity = hostActivity,
                                    title = "فعال‌سازی قفل جیب‌بان",
                                    subtitle = "برای فعال‌سازی قفل، هویت خود را تأیید کنید",
                                    onSuccess = { viewModel.setBiometricEnabled(true) },
                                    onError = { err -> viewModel.showSecurityError(err) }
                                )
                            } else {
                                viewModel.setBiometricEnabled(true)
                            }
                        } else {
                            if (status == BiometricAuthStatus.AVAILABLE && hostActivity != null) {
                                BiometricHelper.authenticate(
                                    activity = hostActivity,
                                    title = "غیرفعال‌سازی قفل جیب‌بان",
                                    subtitle = "برای غیرفعال‌سازی قفل، هویت خود را تأیید کنید",
                                    onSuccess = { viewModel.setBiometricEnabled(false) },
                                    onError = { err -> viewModel.showSecurityError(err) }
                                )
                            } else {
                                viewModel.setBiometricEnabled(false)
                            }
                        }
                    },
                    onSelectGraceMs = { viewModel.setLockGraceMs(it) },
                    onToggleSecureScreen = { viewModel.setSecureScreenEnabled(it) }
                )
            }

            // Theme Mode Card
            item {
                ThemeSelectionCard(
                    selectedThemeMode = state.themeMode,
                    onThemeSelected = { viewModel.setThemeMode(it) }
                )
            }

            // Backup & Restore Card
            item {
                BackupRestoreCard(
                    lastBackupFormatted = state.lastBackupTimeFormatted,
                    isLoading = state.isLoading,
                    onExportBackup = { showExportOptionsDialog = true },
                    onImportBackup = { showRestoreOptionsDialog = true }
                )
            }

            // About Application Card
            item {
                AboutAppCard()
            }
        }
    }

    if (showAccountSelectionDialog) {
        PatternAccountSelectionDialog(
            accounts = accounts,
            onSelectAccount = { selectedAccountId ->
                showAccountSelectionDialog = false
                onNavigateToPatternLearner(selectedAccountId)
            },
            onDismiss = { showAccountSelectionDialog = false }
        )
    }

    if (showRestoreOptionsDialog) {
        RestoreOptionsDialog(
            onPickFile = {
                showRestoreOptionsDialog = false
                try {
                    openDocumentLauncher.launch(arrayOf("*/*"))
                } catch (e: android.content.ActivityNotFoundException) {
                    try {
                        getContentFallbackLauncher.launch("*/*")
                    } catch (e2: android.content.ActivityNotFoundException) {
                        try {
                            val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                                type = "*/*"
                            }
                            chooserFallbackLauncher.launch(
                                android.content.Intent.createChooser(fallbackIntent, "انتخاب فایل پشتیبان")
                            )
                        } catch (e3: android.content.ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                "برنامه مدیریت فایل یافت نشد. می‌توانید متن فایل را مستقیماً وارد کنید.",
                                Toast.LENGTH_LONG
                            ).show()
                            showPasteJsonDialog = true
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "خطا در باز کردن انتخابگر فایل: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onPasteText = {
                showRestoreOptionsDialog = false
                jsonInputText = ""
                showPasteJsonDialog = true
            },
            onDismiss = { showRestoreOptionsDialog = false }
        )
    }

    if (showPasteJsonDialog) {
        PasteBackupTextDialog(
            jsonInputText = jsonInputText,
            onInputChange = { jsonInputText = it },
            onConfirm = {
                val text = jsonInputText.trim()
                if (text.isNotBlank()) {
                    showPasteJsonDialog = false
                    viewModel.onTextSelectedForRestore(text)
                } else {
                    Toast.makeText(context, "لطفاً متن فایل پشتیبان را وارد کنید", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showPasteJsonDialog = false }
        )
    }

    if (showExportOptionsDialog) {
        ExportBackupOptionsDialog(
            onSaveEncryptedFile = {
                showExportOptionsDialog = false
                try {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    createFtbDocumentLauncher.launch("jeebban_backup_$timestamp.ftb")
                } catch (e: Exception) {
                    Toast.makeText(context, "خطا در باز کردن ذخیره‌ساز: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onShareEncryptedBackup = {
                showExportOptionsDialog = false
                coroutineScope.launch {
                    try {
                        val encrypted = viewModel.generateEncryptedBackup()
                        val b64 = com.example.data.backup.BackupCrypto.bytesToBase64(encrypted)
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, b64)
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, "اشتراک‌گذاری بک‌آپ امن (.ftb)")
                        context.startActivity(shareIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطا در اشتراک‌گذاری: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onSelectPortablePlain = {
                showExportOptionsDialog = false
                showPlainWarningDialog = true
            },
            onDismiss = { showExportOptionsDialog = false }
        )
    }

    if (showPlainWarningDialog) {
        PlainBackupSecurityWarningDialog(
            onConfirm = {
                showPlainWarningDialog = false
                showPlainExportOptionsDialog = true
            },
            onDismiss = { showPlainWarningDialog = false }
        )
    }

    if (showPlainExportOptionsDialog) {
        PlainBackupExportActionsDialog(
            onSaveJsonFile = {
                showPlainExportOptionsDialog = false
                try {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val defaultFileName = "jeebban_backup_$timestamp.json"
                    createJsonDocumentLauncher.launch(defaultFileName)
                } catch (e: Exception) {
                    Toast.makeText(context, "برنامه ذخیره فایل در دسترس نیست: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            onCopyJsonToClipboard = {
                showPlainExportOptionsDialog = false
                coroutineScope.launch {
                    try {
                        val json = viewModel.generatePlainBackupJson()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("JeebBan Portable Backup", json)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "متن پشتیبان با موفقیت در کلیپ‌بورد کپی شد", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطا در کپی: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onShareJsonText = {
                showPlainExportOptionsDialog = false
                coroutineScope.launch {
                    try {
                        val json = viewModel.generatePlainBackupJson()
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, json)
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, "اشتراک‌گذاری نسخه پشتیبان JSON")
                        context.startActivity(shareIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطا در اشتراک‌گذاری: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showPlainExportOptionsDialog = false }
        )
    }

    if (state.showRestoreDialog) {
        RestoreConfirmWarningDialog(
            onConfirm = { viewModel.confirmRestore() },
            onDismiss = { viewModel.dismissRestoreDialog() }
        )
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
