package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Currencies
import com.example.data.PendingSmsTransactionEntity

/**
 * Prompt shown on app open when a bank / UPI SMS was detected.
 * User can add the amount to Budget or skip.
 */
@Composable
fun SmsImportDialog(
    item: PendingSmsTransactionEntity,
    currencyCode: String = "INR",
    remainingCount: Int = 0,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val symbol = Currencies.symbolOf(currencyCode)
    val amountLabel = Currencies.format(item.amount, currencyCode)
    val kindLabel = if (item.isExpense) "Debit · Expense" else "Credit · Income"
    val kindColor = if (item.isExpense) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        PixiCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sms_import_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Bank SMS detected",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Add this transaction to your Budget?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                PixiSoftCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = amountLabel.ifBlank { "$symbol${"%.2f".format(item.amount)}" },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("sms_import_amount")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.bankName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("sms_import_bank")
                        )
                        if (item.merchantOrInfo.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.merchantOrInfo,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isExpense) {
                                    Icons.AutoMirrored.Outlined.TrendingDown
                                } else {
                                    Icons.AutoMirrored.Outlined.TrendingUp
                                },
                                contentDescription = null,
                                tint = kindColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = kindLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = kindColor
                            )
                        }
                    }
                }

                if (remainingCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "+$remainingCount more waiting",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                PixiPrimaryButton(
                    text = "Add to Budget",
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sms_import_accept")
                )
                Spacer(modifier = Modifier.height(10.dp))
                PixiOutlineButton(
                    text = "Skip",
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sms_import_skip")
                )
            }
        }
    }
}
