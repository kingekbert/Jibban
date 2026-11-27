package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandGoldLight
import com.example.ui.theme.DarkGreenDeep
import com.example.ui.theme.DarkGreenGradientEnd
import com.example.ui.theme.DarkGreenPrimary
import com.example.util.AmountFormatter

@Composable
fun BalanceCard(
    totalBalance: Long,
    isBalanceHidden: Boolean? = null,
    onToggleBalanceHidden: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var localHidden by rememberSaveable { mutableStateOf(true) }
    val hiddenState = isBalanceHidden ?: localHidden
    val toggleHidden: () -> Unit = onToggleBalanceHidden ?: { localHidden = !localHidden }

    val gradientBrush = Brush.linearGradient(
        colors = listOf(DarkGreenDeep, DarkGreenPrimary, DarkGreenGradientEnd),
        start = Offset(0f, 0f),
        end = Offset(900f, 450f)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BrandGoldLight.copy(alpha = 0.24f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
        ) {
            // Subtle bespoke geometric sheen & delicate gold-tinted arcs
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BrandGoldLight.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.12f, h * 0.15f),
                        radius = h * 1.15f
                    ),
                    radius = h * 1.15f,
                    center = Offset(w * 0.12f, h * 0.15f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = h * 0.82f,
                    center = Offset(w * 0.06f, h * 1.05f),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawCircle(
                    color = BrandGoldLight.copy(alpha = 0.09f),
                    radius = h * 1.12f,
                    center = Offset(w * 0.06f, h * 1.05f),
                    style = Stroke(width = 1.dp.toPx())
                )
                // Fine diagonal highlight line
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(w * 0.55f, 0f),
                    end = Offset(w * 0.92f, h),
                    strokeWidth = 1.dp.toPx()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                // Header row: Brand wallet badge + title on start, visibility toggle pill on end
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(BrandGoldLight.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = BrandGoldLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "موجودی کل حساب‌ها",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.13f),
                        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.18f)),
                        modifier = Modifier
                            .clip(CircleShape)
                            .testTag("balance_visibility_toggle")
                            .clickable(
                                role = Role.Button,
                                onClick = toggleHidden
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (hiddenState) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (hiddenState) "نمایش موجودی" else "مخفی کردن موجودی",
                                tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (hiddenState) "نمایش" else "مخفی",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedContent(
                    targetState = hiddenState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "balanceAnim"
                ) { hidden ->
                    if (hidden) {
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "••••••••",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ریال",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = BrandGoldLight.copy(alpha = 0.92f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    } else {
                        val animatedBalance = rememberAnimatedAmount(
                            targetAmount = totalBalance,
                            durationMillis = 520,
                            animateOnFirstAppear = true
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = AmountFormatter.format(animatedBalance, includeCurrency = false),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ریال",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = BrandGoldLight.copy(alpha = 0.92f),
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
