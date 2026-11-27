package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.goldAccent

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
        .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = if (compact) 20.dp else 32.dp)

    val primaryColor = MaterialTheme.colorScheme.primary
    val containerTint = MaterialTheme.colorScheme.primaryContainer
    val goldColor = MaterialTheme.goldAccent
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = containerModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Bespoke multi-layered geometric illustration matching JeebBan's emerald + gold aesthetic
        val artSize = if (compact) 80.dp else 108.dp
        Box(
            modifier = Modifier.size(artSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension * 0.44f

                // Outer dashed editorial ring
                drawCircle(
                    color = outlineColor.copy(alpha = 0.7f),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )

                // Soft offset emerald backdrop shape
                val cardW = size.width * 0.56f
                val cardH = size.height * 0.56f
                drawRoundRect(
                    color = containerTint.copy(alpha = 0.52f),
                    topLeft = Offset(cx - cardW / 2f, cy - cardH / 2f),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                )

                // Delicate gold accent dot top-start & emerald dot bottom-end
                drawCircle(
                    color = goldColor.copy(alpha = 0.85f),
                    radius = if (compact) 3.5.dp.toPx() else 4.5.dp.toPx(),
                    center = Offset(cx + r * 0.68f, cy - r * 0.62f)
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.35f),
                    radius = if (compact) 2.5.dp.toPx() else 3.5.dp.toPx(),
                    center = Offset(cx - r * 0.74f, cy + r * 0.56f)
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = primaryColor,
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
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
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
