package com.example.ui.screens.accounts

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountType
import com.example.ui.components.AccountIconDisplay
import com.example.ui.components.AmountInput
import com.example.ui.components.AppColorPalette
import com.example.ui.components.AppIconsList
import com.example.ui.components.BankIconInfo
import com.example.util.BankLogoBadge
import com.example.util.BankLogos
import com.example.util.toPersianDigits
import java.io.ByteArrayOutputStream

val AccountColorPalette = AppColorPalette
val AccountIconsList = AppIconsList

private val GeneralAccountIcons = listOf(
    BankIconInfo("CASH", "نقدی", "CASH"),
    BankIconInfo("wallet", "کیف پول", "کیف‌پول"),
    BankIconInfo("credit_card", "کارت", "کارت"),
    BankIconInfo("account_balance", "بانکی", "بانک"),
    BankIconInfo("store", "فروشگاه", "فروشگاه"),
    BankIconInfo("attach_money", "ارز / پس‌انداز", "پس‌انداز")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAccountScreen(
    viewModel: AddAccountViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPatternLearner: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var cardDetailsExpanded by remember { mutableStateOf(false) }
    var smsSectionExpanded by remember { mutableStateOf(false) }
    var appearanceExpanded by remember { mutableStateOf(false) }
    var initializedExpandStates by remember { mutableStateOf(false) }

    LaunchedEffect(state.isEditing, state.cardNumber, state.shabaNumber, state.cardExpiry, state.smsSenders) {
        if (state.isEditing && !initializedExpandStates) {
            if (state.cardNumber.isNotBlank() || state.shabaNumber.isNotBlank() || state.cardExpiry.isNotBlank()) {
                cardDetailsExpanded = true
            }
            if (state.smsSenders.isNotEmpty()) {
                smsSectionExpanded = true
            }
            initializedExpandStates = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val maxDim = 256
                    val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        if (ratio > 1) {
                            Bitmap.createScaledBitmap(bitmap, maxDim, (maxDim / ratio).toInt(), true)
                        } else {
                            Bitmap.createScaledBitmap(bitmap, (maxDim * ratio).toInt(), maxDim, true)
                        }
                    } else bitmap
                    val outputStream = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                    val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    viewModel.setLogoImage(base64)
                }
            } catch (_: Exception) {
                // Ignore image read failure
            }
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setShowConversationDialog(true, context.applicationContext)
        } else {
            Toast.makeText(context, "برای انتخاب از پیامک‌ها، مجوز خواندن پیامک لازم است", Toast.LENGTH_SHORT).show()
        }
    }

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

    val saveBtnContainer = Color(state.color)
    val saveBtnContent = if (saveBtnContainer.luminance() > 0.55f) Color.Black else Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) "ویرایش حساب" else "افزودن حساب جدید",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.imePadding()
            ) {
                Button(
                    onClick = { viewModel.saveAccount() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp)
                        .testTag("add_account_save_button"),
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = saveBtnContainer,
                        contentColor = saveBtnContent
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isEditing) "ذخیره تغییرات" else "ذخیره حساب",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Account Name
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("نام حساب", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.setName(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input"),
                    placeholder = { Text("مثال: بانک ملی، کیف پول") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 2. Account Type Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("نوع حساب", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.type == AccountType.BANK,
                        onClick = { viewModel.setType(AccountType.BANK) },
                        label = { Text("بانکی", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = state.type == AccountType.CASH,
                        onClick = { viewModel.setType(AccountType.CASH) },
                        label = { Text("نقدی / کیف پول", fontSize = 12.sp) },
                        modifier = Modifier.weight(1.25f)
                    )
                    FilterChip(
                        selected = state.type == AccountType.CREDIT_CARD,
                        onClick = { viewModel.setType(AccountType.CREDIT_CARD) },
                        label = { Text("کارت اعتباری", fontSize = 12.sp) },
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }

            // 3. Initial Balance
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("موجودی اولیه (ریال)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                AmountInput(
                    amount = state.initialBalance,
                    onAmountChange = { viewModel.setInitialBalance(it) }
                )
            }

            // 4. Unified Collapsible Appearance Section (Logo / Icon + Color)
            val isCash = state.type == AccountType.CASH
            val detectedBank = if (state.isNoLogo || isCash) null else (
                BankLogos.getBankById(state.logoResName)
                    ?: BankLogos.detectBank(state.cardNumber, state.name)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { appearanceExpanded = !appearanceExpanded }
                            .padding(14.dp)
                            .testTag("account_appearance_toggle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!isCash && (!state.isNoLogo || state.logoImage != null)) {
                            BankLogoBadge(
                                bankId = if (state.isNoLogo) null else state.logoResName,
                                logoImage = if (state.isNoLogo) null else state.logoImage,
                                accountName = state.name,
                                cardNumber = state.cardNumber,
                                size = 40.dp,
                                fallbackColor = Color(state.color)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(state.color)),
                                contentAlignment = Alignment.Center
                            ) {
                                AccountIconDisplay(
                                    iconName = state.icon,
                                    tint = saveBtnContent,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "ظاهر حساب (لوگو، آیکون و رنگ)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val appearanceSummary = when {
                                state.logoImage != null -> "تصویر سفارشی از گالری"
                                !isCash && !state.isNoLogo && detectedBank != null -> "بانک ${detectedBank.namePersian}"
                                isCash || state.isNoLogo -> "آیکون و رنگ انتخابی"
                                else -> "تشخیص هوشمند لوگو و رنگ"
                            }
                            Text(
                                text = appearanceSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(state.color))
                        )
                        Icon(
                            imageVector = if (appearanceExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (appearanceExpanded) "بستن" else "باز کردن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = appearanceExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Logo Actions (Gallery / No Logo / Auto)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "انتخاب از گالری",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("از گالری", fontSize = 12.sp)
                                }

                                if (!isCash) {
                                    FilterChip(
                                        selected = state.isNoLogo,
                                        onClick = { viewModel.setNoLogo(!state.isNoLogo) },
                                        label = { Text("بدون لوگو", fontSize = 12.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }

                                if (state.logoResName != null || state.logoImage != null || state.isNoLogo) {
                                    TextButton(
                                        onClick = {
                                            viewModel.setNoLogo(false)
                                            viewModel.setLogoResName(null)
                                            viewModel.setLogoImage(null)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "خودکار",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("خودکار", fontSize = 12.sp)
                                    }
                                }
                            }

                            // Bank Logos (only for non-cash accounts)
                            if (!isCash) {
                                Text(
                                    text = "لوگوی بانک",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(BankLogos.banks, key = { it.id }) { bank ->
                                        val isSelected = !state.isNoLogo &&
                                            state.logoImage == null &&
                                            (state.logoResName == bank.id || (state.logoResName == null && detectedBank?.id == bank.id))
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.setLogoResName(bank.id)
                                                viewModel.setColor(bank.color.toInt())
                                            },
                                            label = { Text(bank.namePersian, fontSize = 12.sp) },
                                            leadingIcon = {
                                                BankLogoBadge(
                                                    bankId = bank.id,
                                                    size = 22.dp,
                                                    shapeRadius = 5.dp
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            // General Account Icons (for CASH or when "بدون لوگو" is enabled)
                            if (isCash || state.isNoLogo) {
                                Text(
                                    text = "آیکون حساب",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(GeneralAccountIcons, key = { it.id }) { iconInfo ->
                                        val isSelected = state.icon == iconInfo.id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setIcon(iconInfo.id) },
                                            label = { Text(iconInfo.name, fontSize = 12.sp) },
                                            leadingIcon = {
                                                AccountIconDisplay(
                                                    iconName = iconInfo.id,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            // Color Picker
                            Text(
                                text = "رنگ حساب",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(AccountColorPalette, key = { idx, color -> "${idx}_${color.toArgb()}" }) { idx, color ->
                                    val colorArgb = color.toArgb()
                                    val isSelected = state.color == colorArgb
                                    val checkTint = if (color.luminance() > 0.55f) Color.Black else Color.White

                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .semantics {
                                                contentDescription = "رنگ ${(idx + 1).toPersianDigits()}"
                                                selected = isSelected
                                            }
                                            .clickable(role = Role.RadioButton) { viewModel.setColor(colorArgb) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (isSelected) 3.dp else 0.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "انتخاب‌شده",
                                                    tint = checkTint,
                                                    modifier = Modifier.size(20.dp)
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

            // 5. Bank / Card / SMS Specific Collapsible Sections (hidden for CASH)
            AnimatedVisibility(
                visible = !isCash,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Collapsible Card & Shaba Details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { cardDetailsExpanded = !cardDetailsExpanded }
                                    .padding(14.dp)
                                    .testTag("account_card_details_toggle"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "اطلاعات کارت و شبا (اختیاری)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val filledHint = when {
                                            state.cardNumber.length >= 4 -> "کارت ****${state.cardNumber.takeLast(4).toPersianDigits()}"
                                            state.shabaNumber.isNotBlank() -> "شماره شبا ثبت شده"
                                            else -> "شماره کارت، شبا و تاریخ انقضا"
                                        }
                                        Text(
                                            text = filledHint,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (cardDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (cardDetailsExpanded) "بستن" else "باز کردن",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(
                                visible = cardDetailsExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = state.cardNumber,
                                        onValueChange = {
                                            if (it.length <= 16 && it.all { c -> c.isDigit() }) viewModel.setCardNumber(it)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("شماره کارت (۱۶ رقم)") },
                                        placeholder = { Text("۶۰۳۷۹۹۷۵۱۲۳۴۵۶۷۸") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = state.shabaNumber,
                                            onValueChange = { viewModel.setShabaNumber(it) },
                                            modifier = Modifier.weight(1.5f),
                                            label = { Text("شماره شبا") },
                                            placeholder = { Text("IR000000000000000000000000") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = state.cardExpiry,
                                            onValueChange = { viewModel.setCardExpiry(it) },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("انقضا") },
                                            placeholder = { Text("۰۴/۰۸") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Collapsible SMS Senders & Patterns Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { smsSectionExpanded = !smsSectionExpanded }
                                    .padding(14.dp)
                                    .testTag("account_sms_section_toggle"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sms,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "سرشماره‌های پیامک بانکی",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (state.smsSenders.isNotEmpty()) {
                                                "${state.smsSenders.size.toPersianDigits()} سرشماره متصل است"
                                            } else {
                                                "اختیاری — اتصال مستقیم پیامک بانک به این حساب"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (state.smsSenders.isNotEmpty()) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (smsSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (smsSectionExpanded) "بستن" else "باز کردن",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(
                                visible = smsSectionExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        "با اتصال سرشماره‌های بانک به این حساب، پیامک‌های این سرشماره مستقیماً با بالاترین اولویت به این حساب مرتبط می‌شوند.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val hasPermission = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.READ_SMS
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                    if (hasPermission) {
                                                        viewModel.setShowConversationDialog(true, context.applicationContext)
                                                    } else {
                                                        smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                                    }
                                                } catch (_: Throwable) {
                                                    Toast.makeText(
                                                        context,
                                                        "خطا در درخواست مجوز پیامک. می‌توانید سرشماره را دستی وارد کنید.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1.3f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Chat,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("انتخاب از پیامک‌ها", fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.setShowManualSenderDialog(true) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("افزودن دستی", fontSize = 12.sp)
                                        }
                                    }

                                    if (state.smsSenders.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "سرشماره‌ای ثبت نشده است (کل پیامک‌ها اسکن می‌شود)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    } else {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            state.smsSenders.forEach { sender ->
                                                InputChip(
                                                    selected = true,
                                                    onClick = { },
                                                    label = {
                                                        Text(
                                                            text = sender.toPersianDigits(),
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = 13.sp
                                                        )
                                                    },
                                                    trailingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "حذف $sender",
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clickable { viewModel.removeSender(sender) }
                                                        )
                                                    },
                                                    colors = InputChipDefaults.inputChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    if (state.isEditing && state.accountId != null && onNavigateToPatternLearner != null) {
                                        OutlinedButton(
                                            onClick = { onNavigateToPatternLearner(state.accountId!!) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("account_manage_patterns_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Pattern,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("مدیریت الگوهای پیامک این حساب", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (state.showConversationDialog) {
        ConversationSelectionDialog(
            conversations = state.conversationList,
            isLoading = state.isLoadingConversations,
            onDismiss = { viewModel.setShowConversationDialog(false) },
            onSelect = { conv ->
                viewModel.addSender(conv.address)
                viewModel.setShowConversationDialog(false)
            }
        )
    }

    if (state.showManualSenderDialog) {
        ManualSenderDialog(
            onDismiss = { viewModel.setShowManualSenderDialog(false) },
            onAdd = { sender ->
                viewModel.addSender(sender)
                viewModel.setShowManualSenderDialog(false)
            }
        )
    }
}
