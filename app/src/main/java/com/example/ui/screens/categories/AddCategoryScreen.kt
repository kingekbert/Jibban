package com.example.ui.screens.categories

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.local.entity.CategoryType
import com.example.ui.components.AppColorPalette
import com.example.ui.components.AppIconsList
import com.example.ui.components.PersianAmountVisualTransformation
import com.example.ui.components.getCategoryIcon
import com.example.util.AmountFormatter
import com.example.util.NumberToWords

val CategoryIconsList = AppIconsList
val CategoryColorPalette = AppColorPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(
    viewModel: AddCategoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val saveContainerColor = Color(state.color)
    val saveContentColor = if (saveContainerColor.luminance() > 0.55f) Color.Black else Color.White

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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("حذف دسته‌بندی", fontWeight = FontWeight.Bold) },
            text = {
                Text("آیا از حذف دسته‌بندی «${state.name}» اطمینان دارید؟ تراکنش‌های مرتبط حذف نخواهند شد.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCategory()
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) "ویرایش دسته‌بندی" else "افزودن دسته‌بندی جدید",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (state.isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف دسته‌بندی",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { viewModel.saveCategory() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_category_button"),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = saveContainerColor,
                            contentColor = saveContentColor
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isEditing) "ذخیره تغییرات" else "ذخیره دسته‌بندی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Name
            Text("نام دسته‌بندی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.setName(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("مثال: رستوران، سوپرمارکت") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category Type
            Text("نوع دسته‌بندی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.type == CategoryType.EXPENSE,
                    onClick = { viewModel.setType(CategoryType.EXPENSE) },
                    label = { Text("هزینه") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = state.type == CategoryType.INCOME,
                    onClick = { viewModel.setType(CategoryType.INCOME) },
                    label = { Text("درآمد") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = state.type == CategoryType.BOTH,
                    onClick = { viewModel.setType(CategoryType.BOTH) },
                    label = { Text("هر دو") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Monthly Budget in Rials (for Expense/Both categories)
            if (state.type != CategoryType.INCOME) {
                Text("سقف بودجه ماهانه (اختیاری)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = if (state.monthlyBudget > 0L) state.monthlyBudget.toString() else "",
                    onValueChange = { str ->
                        val sanitized = AmountFormatter.sanitizeAmountDigits(str)
                        val rial = AmountFormatter.parseAmount(sanitized)
                        viewModel.setMonthlyBudget(rial)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_monthly_budget_input"),
                    label = { Text("سقف مجاز خرج‌کرد ماهانه (${AmountFormatter.BASE_CURRENCY_LABEL})") },
                    placeholder = { Text("مثال: ۲۰,۰۰۰,۰۰۰") },
                    suffix = { Text(AmountFormatter.BASE_CURRENCY_LABEL) },
                    supportingText = {
                        Text(
                            text = if (state.monthlyBudget > 0L) {
                                NumberToWords.convert(state.monthlyBudget, suffix = AmountFormatter.BASE_CURRENCY_LABEL)
                            } else {
                                "واحد پایه ریال است؛ برای بدون سقف، خالی بگذارید یا ۰ وارد کنید."
                            }
                        )
                    },
                    visualTransformation = remember { PersianAmountVisualTransformation() },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Icon Picker
            Text("انتخاب آیکون", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val iconRows = remember { CategoryIconsList.chunked(6) }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                iconRows.forEach { rowIcons ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowIcons.forEach { iconName ->
                            val isSelected = state.icon == iconName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(state.color).copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color(state.color) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setIcon(iconName) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(iconName),
                                    contentDescription = iconName,
                                    tint = if (isSelected) Color(state.color) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        val remainingInRow = 6 - rowIcons.size
                        repeat(remainingInRow) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Color Picker
            Text("رنگ دسته‌بندی (۲۴+ رنگ)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryColorPalette.forEach { color ->
                    val colorArgb = color.toArgb()
                    val isSelected = state.color == colorArgb

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { viewModel.setColor(colorArgb) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "انتخاب‌شده",
                                tint = if (color.luminance() > 0.55f) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
