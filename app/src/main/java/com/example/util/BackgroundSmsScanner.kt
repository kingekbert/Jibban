package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.example.FinTrackApp
import com.example.data.local.entity.AccountEntity
import com.example.ui.screens.scan_sms.ScannedSmsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AccountSmsInspectionResult(
    val unregisteredCount: Int = 0,
    val latestBalance: Long? = null,
    val latestSmsDate: Long = 0L,
    val latestSmsHash: String = "",
    val unregisteredItems: List<ScannedSmsItem> = emptyList()
)

object BackgroundSmsScanner {

    private val OTP_KEYWORDS = listOf(
        "رمز پویا", "رمز یکبار", "کد تایید", "کد فعالسازی", "کد فعال‌سازی", "رمز اینترنتی",
        "رمز دوم", "کد ورود", "کد احراز", "کد صحت", "کد ثبت نام", "کد تائید"
    )

    fun isOtpSms(text: String): Boolean {
        return OTP_KEYWORDS.any { text.contains(it, ignoreCase = true) }
    }

    fun resolveAccountForSms(
        senderAccount: AccountEntity?,
        accountIdent: String,
        bankName: String,
        allAccounts: List<AccountEntity>
    ): AccountEntity? {
        if (senderAccount != null) return senderAccount

        val cleanIdent = accountIdent.trim()
        if (cleanIdent.isNotBlank()) {
            val byIdent = allAccounts.find { acc ->
                val card = acc.cardNumber?.replace(Regex("""[^\d]"""), "")
                val shaba = acc.shabaNumber?.replace(Regex("""[^\dA-Za-z]"""), "")
                (card != null && (card.contains(cleanIdent) || (card.length >= 4 && cleanIdent.contains(card.takeLast(4))))) ||
                    (shaba != null && shaba.contains(cleanIdent, ignoreCase = true)) ||
                    acc.name.contains(cleanIdent, ignoreCase = true) ||
                    cleanIdent.contains(acc.name, ignoreCase = true)
            }
            if (byIdent != null) return byIdent
        }

        val cleanBank = bankName.trim()
        if (cleanBank.isNotBlank()) {
            val byName = allAccounts.find { acc ->
                acc.name.contains(cleanBank, ignoreCase = true) ||
                    cleanBank.contains(acc.name, ignoreCase = true)
            }
            if (byName != null) return byName

            val detectedBankId = BankLogos.detectBank(null, cleanBank)?.id
            if (detectedBankId != null) {
                val byLogo = allAccounts.find { acc ->
                    val accBankId = acc.logoResName ?: BankLogos.detectBank(acc.cardNumber, acc.name)?.id
                    accBankId == detectedBankId
                }
                if (byLogo != null) return byLogo
            }
        }
        return null
    }

    suspend fun scanRecentSms(
        context: Context,
        days: Int = 3
    ): List<ScannedSmsItem> = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return@withContext emptyList()

        val app = context.applicationContext as? FinTrackApp ?: return@withContext emptyList()
        val appModule = app.appModule

        val existingTransactions = appModule.transactionRepository.getAllTransactionsWithDetailsList()
        val allAccounts = appModule.accountRepository.getAllAccountsList()
        val allSenders = runCatching {
            appModule.accountSmsSenderRepository.getAllSendersList()
        }.getOrDefault(emptyList())
        val patterns = runCatching {
            appModule.smsPatternRepository.getActivePatternsList()
        }.getOrDefault(emptyList())

        if (patterns.isEmpty()) return@withContext emptyList()

        val accountMap = allAccounts.associateBy { it.id }
        val scannedResult = mutableListOf<ScannedSmsItem>()
        val consumedManualTxIds = mutableSetOf<Long>()
        val seenHashes = mutableSetOf<String>()
        val ignoredHashes = IgnoredSmsStore.getIgnoredHashes(context)

        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")
        val startTime = System.currentTimeMillis() - (days.toLong() * 24L * 60L * 60L * 1000L)

        val (selection, selectionArgs) = SmsAccountMatcher.buildSmsQuerySelection(
            startTime, allAccounts, allSenders
        )

        val cursor = runCatching {
            context.contentResolver.query(uri, projection, selection, selectionArgs, "date DESC")
        }.getOrNull()

        cursor?.use { c ->
            val addressIndex = c.getColumnIndex("address")
            val bodyIndex = c.getColumnIndex("body")
            val dateIndex = c.getColumnIndex("date")

            while (c.moveToNext()) {
                val sender = if (addressIndex != -1) c.getString(addressIndex) ?: "" else ""
                val body = if (bodyIndex != -1) c.getString(bodyIndex) ?: "" else ""
                val receivedDate = if (dateIndex != -1) c.getLong(dateIndex) else System.currentTimeMillis()

                if (body.isBlank() || isOtpSms(body)) continue

                val (parsed, senderAccount) = SmsAccountMatcher.parseSmsWithSenderPriority(
                    body = body,
                    smsSender = sender,
                    allAccounts = allAccounts,
                    allSenders = allSenders,
                    patterns = patterns
                )

                val amt = parsed.amount
                val smsDate = parsed.date ?: receivedDate
                val txType = parsed.transactionType
                if (amt == null || amt <= 0L || txType == null) continue

                val accountIdent = parsed.accountIdentifier?.trim() ?: ""
                val bankName = parsed.bankName?.trim() ?: ""
                val effectiveAccountIdent = if (accountIdent.isNotBlank()) accountIdent else (senderAccount?.cardNumber?.takeLast(4) ?: "")
                val effectiveBankName = if (bankName.isNotBlank()) bankName else (senderAccount?.name ?: "")
                val extractedBalance = parsed.balance ?: PatternMatcher.extractBalanceFromText(body)

                val effectiveParsed = parsed.copy(
                    bankName = if (parsed.bankName.isNullOrBlank()) senderAccount?.name else parsed.bankName,
                    accountIdentifier = if (parsed.accountIdentifier.isNullOrBlank()) (senderAccount?.cardNumber?.takeLast(4) ?: "") else parsed.accountIdentifier,
                    balance = extractedBalance
                )

                val hash = SmsHashUtil.calculateHash(
                    sender = sender,
                    amount = amt,
                    date = smsDate,
                    text = body,
                    transactionType = txType,
                    accountIdentifier = effectiveAccountIdent
                )

                if (hash in ignoredHashes || !seenHashes.add(hash)) continue

                val matchedAccount = resolveAccountForSms(
                    senderAccount = senderAccount,
                    accountIdent = effectiveAccountIdent,
                    bankName = effectiveBankName,
                    allAccounts = allAccounts
                )

                val legacyHashes = setOf(
                    SmsHashUtil.calculateLegacyV1Hash(sender, amt, smsDate, body),
                    SmsHashUtil.calculateLegacySimpleHash(amt, smsDate, effectiveAccountIdent)
                )

                val isRegistered = SmsDeduplicator.isSmsRegistered(
                    candidate = SmsDeduplicator.SmsCandidate(
                        transactionType = txType,
                        amount = amt,
                        date = smsDate,
                        primaryHash = hash,
                        legacyHashes = legacyHashes,
                        matchedAccountId = matchedAccount?.id,
                        accountIdent = effectiveAccountIdent,
                        bankName = effectiveBankName
                    ),
                    existingTransactions = existingTransactions,
                    accountsById = accountMap,
                    consumedManualTxIds = consumedManualTxIds
                )

                scannedResult.add(
                    ScannedSmsItem(
                        id = hash,
                        parsedSms = effectiveParsed,
                        rawText = body,
                        smsHash = hash,
                        date = smsDate,
                        isRecognized = true,
                        isRegistered = isRegistered,
                        sender = sender,
                        balanceDiff = null,
                        matchedAccountId = matchedAccount?.id,
                        receivedAt = receivedDate
                    )
                )
            }
        }

        val transfers = TransferDetector.findTransfers(scannedResult, allAccounts, existingTransactions)
        val registeredTransferHashes = transfers.filter { it.isRegistered }
            .flatMap { listOf(it.withdrawalItem.smsHash, it.depositItem.smsHash) }
            .toSet()

        if (registeredTransferHashes.isNotEmpty()) {
            scannedResult.map { item ->
                if (registeredTransferHashes.contains(item.smsHash)) item.copy(isRegistered = true) else item
            }
        } else {
            scannedResult
        }
    }

    suspend fun scanForUnregisteredSms(
        context: Context,
        days: Int = 3
    ): List<ScannedSmsItem> {
        return scanRecentSms(context, days).filter { !it.isRegistered }
    }

    suspend fun inspectAccountSms(
        context: Context,
        accountId: Long,
        days: Int = 30
    ): AccountSmsInspectionResult {
        val allItems = scanRecentSms(context, days)
        val accountItems = allItems.filter { it.matchedAccountId == accountId }
        val unregistered = accountItems.filter { !it.isRegistered }
        val latestWithBalance = accountItems
            .filter { (it.parsedSms.balance ?: PatternMatcher.extractBalanceFromText(it.rawText)) != null }
            .maxWithOrNull(
                compareBy<ScannedSmsItem> { if (it.receivedAt > 0L) it.receivedAt else it.date }
                    .thenBy { it.date }
            )
        val latestBal = latestWithBalance?.let {
            it.parsedSms.balance ?: PatternMatcher.extractBalanceFromText(it.rawText)
        }
        return AccountSmsInspectionResult(
            unregisteredCount = unregistered.size,
            latestBalance = latestBal,
            latestSmsDate = latestWithBalance?.date ?: 0L,
            latestSmsHash = latestWithBalance?.smsHash ?: "",
            unregisteredItems = unregistered
        )
    }
}
