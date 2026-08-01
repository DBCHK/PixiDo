package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AccountEntity
import com.example.data.Currencies

/**
 * Move money between own accounts — e.g. bank → credit card repayment.
 * Does not count toward monthly spending budget.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransferDialog(
    currencyCode: String,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onTransfer: (
        fromAccountId: Int,
        toAccountId: Int,
        amount: Double,
        note: String
    ) -> Unit
) {
    val symbol = Currencies.symbolOf(currencyCode)
    val sources = accounts.filter { !it.isCreditCard }
    val destinations = accounts

    var fromId by remember {
        mutableStateOf(sources.firstOrNull { it.isPrimary }?.id ?: sources.firstOrNull()?.id)
    }
    var toId by remember {
        mutableStateOf(
            accounts.firstOrNull { it.isCreditCard && it.id != fromId }?.id
                ?: accounts.firstOrNull { it.id != fromId }?.id
        )
    }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val fromAccount = accounts.find { it.id == fromId }
    val toAccount = accounts.find { it.id == toId }
    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val canSubmit = fromId != null && toId != null && fromId != toId && amount > 0

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    )

    Dialog(onDismissRequest = onDismiss) {
        PixiCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transfer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transfer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PixiCloseButton(onClick = onDismiss)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Move money between accounts · pay credit cards without counting as spend",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "From",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (sources.isEmpty()) {
                    Text(
                        text = "Add a bank, cash, or savings account first",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sources.forEach { acc ->
                            PixiChip(
                                label = acc.name,
                                selected = acc.id == fromId,
                                onClick = {
                                    fromId = acc.id
                                    if (toId == acc.id) {
                                        toId = destinations.firstOrNull { it.id != acc.id }?.id
                                    }
                                }
                            )
                        }
                    }
                    fromAccount?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Balance ${Currencies.format(it.balance, currencyCode)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "To",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    destinations.filter { it.id != fromId }.forEach { acc ->
                        val label = if (acc.isCreditCard) {
                            "${acc.name} (card)"
                        } else acc.name
                        PixiChip(
                            label = label,
                            selected = acc.id == toId,
                            onClick = { toId = acc.id }
                        )
                    }
                }
                toAccount?.let { acc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (acc.isCreditCard) {
                            "Owed ${Currencies.format(acc.creditUtilized, currencyCode)}" +
                                if (acc.creditLimit > 0) {
                                    " · ${Currencies.format(acc.creditAvailable, currencyCode)} free"
                                } else ""
                        } else {
                            "Balance ${Currencies.format(acc.balance, currencyCode)}"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($symbol)") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_transfer_amount"),
                    singleLine = true,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    placeholder = {
                        Text(
                            if (toAccount?.isCreditCard == true) "Credit card payment"
                            else "e.g. Move to savings"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                if (fromAccount != null && toAccount != null && amount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = buildString {
                            append("${fromAccount.name} → ${toAccount.name}")
                            if (toAccount.isCreditCard) append(" · pays down card debt")
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                PixiPrimaryButton(
                    text = if (toAccount?.isCreditCard == true) "Pay card" else "Transfer",
                    onClick = {
                        val f = fromId
                        val t = toId
                        if (canSubmit && f != null && t != null) {
                            onTransfer(f, t, amount, note.trim())
                            onDismiss()
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier.testTag("submit_transfer_btn")
                )
            }
        }
    }
}
