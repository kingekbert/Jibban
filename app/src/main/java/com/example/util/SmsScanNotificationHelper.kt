package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.entity.TransactionType
import com.example.ui.screens.scan_sms.ScannedSmsItem

object SmsScanNotificationHelper {
    private const val CHANNEL_ID = "jeebban_sms_scan_channel"
    private const val CHANNEL_NAME = "تراکنش‌های پیامکی جدید"
    private const val CHANNEL_DESC = "اعلان شناسایی تراکنش‌های بانکی جدید در پیامک‌ها برای ثبت سریع"
    private const val NOTIFICATION_ID = 4102

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showNewTransactionsNotification(context: Context, newItems: List<ScannedSmsItem>) {
        if (newItems.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "scan_sms")
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            pendingIntentFlags
        )

        val count = newItems.size
        val title = if (count == 1) {
            "تراکنش پیامکی جدید شناسایی شد"
        } else {
            "${count.toPersianDigits()} تراکنش پیامکی جدید شناسایی شد"
        }

        val latest = newItems.first()
        val typeLabel = if (latest.parsedSms.transactionType == TransactionType.INCOME) "واریز" else "برداشت"
        val amtText = AmountFormatter.formatRial(latest.parsedSms.amount ?: 0L)
        val bankName = latest.parsedSms.bankName?.takeIf { it.isNotBlank() } ?: "بانک"

        val contentText = if (count == 1) {
            "$typeLabel $amtText ($bankName) — برای ثبت لمس کنید"
        } else {
            "آخرین مورد: $typeLabel $amtText ($bankName). برای بررسی و ثبت لمس کنید."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_wallet)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
