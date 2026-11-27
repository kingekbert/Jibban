package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    title: String = "هنوز تراکنشی ثبت نشده است",
    icon: ImageVector = Icons.Default.ReceiptLong,
    buttonText: String? = "ثبت تراکنش جدید",
    onButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    buttonIcon: ImageVector? = Icons.Default.Add,
    secondaryButtonText: String? = null,
    secondaryButtonIcon: ImageVector? = null,
    onSecondaryButtonClick: (() -> Unit)? = null,
    compact: Boolean = false,
    testTag: String? = null
) {
    val containerModifier = modifier
        .fillMaxWidth()
        .let { if (testTag != null) it.testTag(testTag) else it }
        .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = if (compact) 18.dp else 28.dp)

    Column(
        modifier = containerModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 56.dp else 72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (compact) 28.dp else 36.dp)
            )
        }

        Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))

        Text(
            text = title,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        if (buttonText != null && onButtonClick != null) {
            Spacer(modifier = Modifier.height(if (compact) 14.dp else 18.dp))
            Button(
                onClick = onButtonClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (buttonIcon != null) {
                    Icon(
                        imageVector = buttonIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = buttonText, fontWeight = FontWeight.Bold)
            }
        }

        if (secondaryButtonText != null && onSecondaryButtonClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSecondaryButtonClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (secondaryButtonIcon != null) {
                    Icon(
                        imageVector = secondaryButtonIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = secondaryButtonText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
