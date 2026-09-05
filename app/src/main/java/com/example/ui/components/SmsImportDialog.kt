package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AccountEntity
import com.example.data.Currencies
import com.example.data.PendingSmsTransactionEntity
import com.example.sms.SmsAccountMatcher
import com.example.sms.SmsTransactionParser

/**
 * In-app notification panel for a detected debit/credit SMS.
 *
 * Close (X and Close button) dismisses it.
 * "Choose account" opens a picker; picking an account assigns the amount.
 * By default the matching bank / last-used / primary account is pre-selected.
 */
@Composable
fun SmsImportDialog(
    item: PendingSmsTransactionEntity,
    accounts: List<AccountEntity>,
    currencyCode: String = "INR",
    remainingCount: Int = 0,
    lastAccountId: Int? = null,
    onAccept: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var showAccountPicker by remember(item.id) { mutableStateOf(false) }
    var selectedAccount by remember(item.id, accounts, lastAccountId, item.smsBody, item.smsSender) {
        val parsedSms = SmsTransactionParser.parse(item.smsBody, item.smsSender)
        val last4 = parsedSms?.accountLast4?.ifBlank {
            SmsTransactionParser.extractAccountLast4(item.smsBody)
        }.orEmpty()
        mutableStateOf(
            SmsAccountMatcher.defaultAccount(
                accounts = accounts,
                bankName = item.bankName,
                lastAccountId = lastAccountId,
                accountLast4 = last4,
                preferCreditCard = parsedSms?.isCreditCard
            )
        )
    }

    val amountLabel = Currencies.format(item.amount, currencyCode)
    val kindLabel = if (item.isExpense) "Amount deducted" else "Amount credited"
    val kindColor = if (item.isExpense) {
        Color(0xFFFF3B30)
    } else {
        Color(0xFF34C759)
    }
    val parsed = remember(item.id, item.smsBody, item.smsSender) {
        SmsTransactionParser.parse(item.smsBody, item.smsSender)
    }
    val defaultName = selectedAccount?.name
    val channelLabel = when (parsed?.channel) {
        SmsTransactionParser.CHANNEL_UPI -> "UPI"
        SmsTransactionParser.CHANNEL_CARD -> "Card"
        SmsTransactionParser.CHANNEL_ATM -> "ATM"
        SmsTransactionParser.CHANNEL_IMPS -> "IMPS"
        SmsTransactionParser.CHANNEL_NEFT -> "NEFT"
        SmsTransactionParser.CHANNEL_RTGS -> "RTGS"
        else -> null
    }

    PixiGlass(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("sms_import_dialog"),
        shape = RoundedCornerShape(24.dp),
        role = PixiGlassRole.Sheet,
        elevation = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Transaction detected",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = kindLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = kindColor
                    )
                }
                PixiCircleIconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("sms_import_close"),
                    size = 44.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close notification",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item.isExpense) {
                        Icons.AutoMirrored.Outlined.TrendingDown
                    } else {
                        Icons.AutoMirrored.Outlined.TrendingUp
                    },
                    contentDescription = null,
                    tint = kindColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = amountLabel,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("sms_import_amount")
                )
            }

            val subtitle = buildString {
                append(item.bankName)
                channelLabel?.let { append(" · $it") }
                val merchant = item.merchantOrInfo.ifBlank { parsed?.merchantOrInfo.orEmpty() }
                if (merchant.isNotBlank()) {
                    append(" · ")
                    append(merchant)
                }
                parsed?.category?.takeIf { it != "Other" }?.let { append(" · $it") }
            }
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                onClick = { showAccountPicker = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sms_choose_account"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val accountColor = selectedAccount?.let {
                        runCatching { Color(android.graphics.Color.parseColor(it.colorHex)) }
                            .getOrDefault(MaterialTheme.colorScheme.primary)
                    } ?: MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accountColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (defaultName != null) {
                                "By default: $defaultName"
                            } else {
                                "Choose which account"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                selectedAccount?.isCreditCard == true && item.isExpense ->
                                    "Added to this card’s bill · bank balances stay the same"
                                selectedAccount?.isCreditCard == true ->
                                    "Reduces this card’s outstanding"
                                item.isExpense ->
                                    "Amount will be deducted from this account"
                                else ->
                                    "Amount will be credited to this account"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Choose account",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PixiPrimaryButton(
                text = "Choose account",
                onClick = { showAccountPicker = true },
                modifier = Modifier.testTag("sms_choose_account_btn")
            )

            Spacer(modifier = Modifier.height(8.dp))

            PixiSecondaryButton(
                text = if (defaultName != null) "Add to $defaultName" else "Add to Budget",
                onClick = { onAccept(selectedAccount?.id) },
                modifier = Modifier.testTag("sms_import_accept")
            )

            Spacer(modifier = Modifier.height(8.dp))

            PixiOutlineButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.testTag("sms_import_skip")
            )

            if (remainingCount > 0) {
                Text(
                    text = "+$remainingCount more pending",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                )
            }
        }
    }

    if (showAccountPicker) {
        AccountSelectionDialog(
            accounts = accounts,
            selectedAccountId = selectedAccount?.id,
            onAccountSelected = { account ->
                selectedAccount = account
                showAccountPicker = false
                onAccept(account.id)
            },
            onDismiss = { showAccountPicker = false }
        )
    }
}
