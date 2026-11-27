package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VazirmatnFontFamily
import com.example.util.AmountFormatter
import com.example.util.NumberToWords
import com.example.util.toEnglishDigits

class PersianAmountVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rawDigits = text.text.toEnglishDigits().filter { it.isDigit() }
        if (rawDigits.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val amount = rawDigits.toLongOrNull() ?: 0L
        val formatted = AmountFormatter.format(amount, includeCurrency = false)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, rawDigits.length)
                if (clamped == 0) return 0
                var digitCount = 0
                for (i in formatted.indices) {
                    if (formatted[i].isDigit() || formatted[i] in '۰'..'۹') {
                        digitCount++
                    }
                    if (digitCount == clamped) {
                        return i + 1
                    }
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                if (clamped == 0) return 0
                var digitCount = 0
                for (i in 0 until clamped) {
                    if (formatted[i].isDigit() || formatted[i] in '۰'..'۹') {
                        digitCount++
                    }
                }
                return digitCount
            }
        }

        return TransformedText(
            AnnotatedString(formatted),
            offsetMapping
        )
    }
}

@Composable
fun AmountInput(
    amount: Long,
    onAmountChange: (Long) -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
    autoFocus: Boolean = false,
    modifier: Modifier = Modifier,
    currencyLabel: String = "ریال"
) {
    var rawText by remember(amount) {
        mutableStateOf(if (amount == 0L) "" else amount.toString())
    }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
        label = "amount_border"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) color.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface,
        label = "amount_bg"
    )

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Hero typographic amount entry surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable {
                    try {
                        focusRequester.requestFocus()
                    } catch (_: Exception) {}
                },
            shape = RoundedCornerShape(20.dp),
            color = containerColor,
            border = BorderStroke(if (isFocused) 1.5.dp else 1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rawText.isEmpty()) {
                            Text(
                                text = "۰",
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                        }
                        BasicTextField(
                            value = rawText,
                            onValueChange = { newValue ->
                                val cleanDigits = newValue.toEnglishDigits().filter { it.isDigit() }
                                rawText = cleanDigits
                                val parsed = cleanDigits.toLongOrNull() ?: 0L
                                onAmountChange(parsed)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { isFocused = it.isFocused },
                            textStyle = TextStyle(
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = color,
                                fontFeatureSettings = "tnum"
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(color),
                            visualTransformation = remember { PersianAmountVisualTransformation() },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = color.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = currencyLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Live amount in Persian words
                AnimatedVisibility(
                    visible = amount != 0L,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val wordsText = NumberToWords.convert(amount = amount, suffix = currencyLabel)
                    Text(
                        text = wordsText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Clean quick-append +000 and clear pill row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val newAmount = if (amount == 0L) 1000L else amount * 1000L
                    rawText = newAmount.toString()
                    onAmountChange(newAmount)
                },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                border = subtleCardBorder(0.6f)
            ) {
                Text(
                    text = "+۰۰۰",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            if (amount > 0L) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        rawText = ""
                        onAmountChange(0L)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "پاک کردن مبلغ",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "پاک کردن",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
