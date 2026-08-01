package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AccountEntity
import com.example.data.Currencies
import com.example.data.TransactionType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddBudgetDialog(
    currencyCode: String = "USD",
    accounts: List<AccountEntity> = emptyList(),
    onDismiss: () -> Unit,
    onAddBudgetItem: (
        title: String,
        amount: Double,
        isExpense: Boolean,
        category: String,
        note: String,
        accountId: Int?,
        transactionType: TransactionType
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var txnType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var category by remember { mutableStateOf("Food & Drink") }
    var note by remember { mutableStateOf("") }
    var selectedAccountId by remember {
        mutableStateOf(accounts.firstOrNull { it.isPrimary }?.id ?: accounts.firstOrNull()?.id)
    }

    val symbol = Currencies.symbolOf(currencyCode)

    val expenseCategories = listOf(
        "Food & Drink", "Subscriptions", "Transport", "Shopping",
        "Bills", "Health", "Entertainment", "Savings", "Other"
    )
    val incomeCategories = listOf(
        "Salary", "Freelance", "Gifts", "Investments", "Refund", "Other"
    )
    val lentCategories = listOf(
        "Friend", "Family", "Colleague", "Business", "Other"
    )
    val borrowCategories = listOf(
        "Friend", "Family", "Loan", "Business", "Other"
    )

    // TRANSFER is handled by TransferDialog on the Budget screen
    val loggableTypes = listOf(
        TransactionType.EXPENSE,
        TransactionType.INCOME,
        TransactionType.LENT,
        TransactionType.BORROW
    )

    val currentCats = when (txnType) {
        TransactionType.EXPENSE -> expenseCategories
        TransactionType.INCOME -> incomeCategories
        TransactionType.LENT -> lentCategories
        TransactionType.BORROW -> borrowCategories
        TransactionType.TRANSFER -> expenseCategories
    }

    val dialogTitle = when (txnType) {
        TransactionType.EXPENSE -> "Log Expense"
        TransactionType.INCOME -> "Log Income"
        TransactionType.LENT -> "Log Money Lent"
        TransactionType.BORROW -> "Log Money Borrowed"
        TransactionType.TRANSFER -> "Transfer"
    }

    val hint = when (txnType) {
        TransactionType.EXPENSE -> "Counts toward your monthly budget"
        TransactionType.INCOME -> "Added to income · not a budget spend"
        TransactionType.LENT -> "You lent this out · not a budget expense"
        TransactionType.BORROW -> "You borrowed this · not counted as income"
        TransactionType.TRANSFER -> "Use Transfer on Accounts for bank → card payments"
    }

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
                .testTag("add_budget_dialog")
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
                        text = dialogTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PixiCloseButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_add_budget")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = hint,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Expense / Income / Lent / Borrow segmented control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .clip(PixiPillShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    loggableTypes.forEach { type ->
                        val selected = txnType == type
                        Box(
                            modifier = Modifier
                                .clip(PixiPillShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    txnType = type
                                    category = when (type) {
                                        TransactionType.EXPENSE -> expenseCategories[0]
                                        TransactionType.INCOME -> incomeCategories[0]
                                        TransactionType.LENT -> lentCategories[0]
                                        TransactionType.BORROW -> borrowCategories[0]
                                        TransactionType.TRANSFER -> expenseCategories[0]
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($symbol)") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_budget_amount"),
                    singleLine = true,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = {
                        Text(
                            when (txnType) {
                                TransactionType.LENT -> "Who did you lend to?"
                                TransactionType.BORROW -> "Who did you borrow from?"
                                else -> "Title / merchant"
                            }
                        )
                    },
                    placeholder = {
                        Text(
                            when (txnType) {
                                TransactionType.LENT -> "e.g. Alex"
                                TransactionType.BORROW -> "e.g. Mom, bank loan"
                                else -> "What was this for?"
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_budget_title"),
                    singleLine = true,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                if (accounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Account",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { acc ->
                            PixiChip(
                                label = acc.name,
                                selected = acc.id == selectedAccountId,
                                onClick = { selectedAccountId = acc.id }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = when (txnType) {
                        TransactionType.LENT, TransactionType.BORROW -> "With"
                        else -> "Category"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentCats.forEach { cat ->
                        PixiChip(
                            label = cat,
                            selected = cat == category,
                            onClick = { category = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                PixiPrimaryButton(
                    text = "Save Transaction",
                    onClick = {
                        val parsedAmount = amountStr.toDoubleOrNull() ?: 0.0
                        if (parsedAmount > 0) {
                            onAddBudgetItem(
                                title,
                                parsedAmount,
                                txnType.decreasesAsset,
                                category,
                                note,
                                selectedAccountId,
                                txnType
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.testTag("submit_add_budget_btn")
                )
            }
        }
    }
}
