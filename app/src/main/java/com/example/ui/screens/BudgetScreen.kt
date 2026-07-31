package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.BudgetItemEntity
import com.example.data.Currencies
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.BudgetSettingsDialog
import com.example.ui.components.PixiBadge
import com.example.ui.components.PixiCard
import com.example.ui.components.PixiCardShapeSm
import com.example.ui.components.PixiEmptyState
import com.example.ui.components.PixiScreenHeader
import com.example.ui.components.PixiSectionLabel

@Composable
fun BudgetScreen(
    budgetItems: List<BudgetItemEntity>,
    accounts: List<AccountEntity>,
    currencyCode: String,
    monthlyAllowance: Double,
    onDeleteBudgetItem: (Int) -> Unit,
    onOpenAddBudget: () -> Unit,
    onAddAccount: (String, AccountType, Double, Double, String) -> Unit,
    onDeleteAccount: (Int) -> Unit,
    onSetCurrency: (String) -> Unit,
    onSetMonthlyLimit: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val sound = LocalSoundEngine.current
    var showAddAccount by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val symbol = Currencies.symbolOf(currencyCode)
    val totalExpenses = budgetItems.filter { it.isExpense }.sumOf { it.amount }
    val totalIncome = budgetItems.filter { !it.isExpense }.sumOf { it.amount }
    val netWorth = accounts.sumOf { it.balance }

    val hasLimit = monthlyAllowance > 0
    val remainingAllowance = if (hasLimit) (monthlyAllowance - totalExpenses).coerceAtLeast(0.0) else 0.0
    val spentPercentage = if (hasLimit) {
        (totalExpenses / monthlyAllowance).toFloat().coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(targetValue = spentPercentage, label = "spentProgress")

    val (vibeStatus, vibeColor) = when {
        !hasLimit -> "Set a limit" to MaterialTheme.colorScheme.primary
        spentPercentage < 0.6f -> "On track" to Color(0xFF34D399)
        spentPercentage < 0.85f -> "Watch spend" to Color(0xFFFBBF24)
        else -> "Over budget" to Color(0xFFFF7A8A)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp)
        ) {
            item {
                PixiScreenHeader(
                    title = "Budget",
                    subtitle = "Accounts · limits · cashflow",
                    trailing = {
                        Box(
                            modifier = Modifier
                                .clip(PixiCardShapeSm)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    sound.play(Sfx.SETTINGS_CHANGE)
                                    showSettings = true
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("budget_settings_chip")
                        ) {
                            Text(
                                text = "$currencyCode · $symbol",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                PixiCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_summary_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (hasLimit) "Remaining this month" else "Net across accounts",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (hasLimit) {
                                        Currencies.format(remainingAllowance, currencyCode)
                                    } else {
                                        Currencies.format(netWorth, currencyCode)
                                    },
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasLimit) {
                                        "of ${Currencies.format(monthlyAllowance, currencyCode)} monthly limit"
                                    } else {
                                        "Tap currency to set monthly limit"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            PixiBadge(
                                text = vibeStatus,
                                containerColor = vibeColor.copy(alpha = 0.15f),
                                contentColor = vibeColor
                            )
                        }

                        if (hasLimit) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(PixiCardShapeSm),
                                color = vibeColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CashflowPill(
                                label = "Income",
                                value = "+${Currencies.format(totalIncome, currencyCode)}",
                                icon = Icons.Filled.ArrowDownward,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            CashflowPill(
                                label = "Spent",
                                value = "-${Currencies.format(totalExpenses, currencyCode)}",
                                icon = Icons.Filled.ArrowUpward,
                                tint = Color(0xFFFF7A8A),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                PixiSectionLabel(
                    text = "Accounts",
                    action = "+ Add",
                    onAction = {
                        sound.play(Sfx.DIALOG_OPEN)
                        showAddAccount = true
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (accounts.isEmpty()) {
                item {
                    EmptyAccountsCard(onAdd = { showAddAccount = true })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 18.dp)
                    ) {
                        items(accounts, key = { it.id }) { account ->
                            AccountCard(
                                account = account,
                                currencyCode = currencyCode,
                                onDelete = { onDeleteAccount(account.id) }
                            )
                        }
                    }
                }
            }

            item {
                PixiSectionLabel(text = "Transactions")
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (budgetItems.isEmpty()) {
                item {
                    PixiEmptyState(
                        title = "No transactions yet",
                        subtitle = "Tap the yellow + to log your first expense or income in $symbol"
                    )
                }
            } else {
                items(budgetItems, key = { it.id }) { item ->
                    BudgetItemRow(
                        item = item,
                        currencyCode = currencyCode,
                        accountName = accounts.find { it.id == item.accountId }?.name,
                        onDelete = { onDeleteBudgetItem(item.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    if (showAddAccount) {
        AddAccountDialog(
            currencyCode = currencyCode,
            onDismiss = { showAddAccount = false },
            onAdd = { name, type, balance, limit, color ->
                onAddAccount(name, type, balance, limit, color)
                showAddAccount = false
            }
        )
    }

    if (showSettings) {
        BudgetSettingsDialog(
            currencyCode = currencyCode,
            monthlyLimit = monthlyAllowance,
            onDismiss = { showSettings = false },
            onSave = { code, limit ->
                onSetCurrency(code)
                onSetMonthlyLimit(limit)
                showSettings = false
            }
        )
    }
}

@Composable
private fun CashflowPill(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(PixiCardShapeSm)
            .background(tint.copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

@Composable
fun AccountCard(
    account: AccountEntity,
    currencyCode: String,
    onDelete: () -> Unit
) {
    val type = runCatching { AccountType.valueOf(account.type) }.getOrDefault(AccountType.BANK)
    val icon = when (type) {
        AccountType.BANK -> Icons.Filled.AccountBalance
        AccountType.CASH -> Icons.Filled.Payments
        AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
        AccountType.SAVINGS -> Icons.Filled.Savings
        AccountType.WALLET -> Icons.Filled.Payments
    }
    val accent = runCatching { Color(android.graphics.Color.parseColor(account.colorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)

    val usagePct = if (account.creditLimit > 0) {
        (account.monthlyUsage / account.creditLimit).toFloat().coerceIn(0f, 1f)
    } else 0f

    PixiCard(
        modifier = Modifier
            .width(220.dp)
            .testTag("account_card_${account.id}"),
        borderColor = accent.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = type.name, tint = accent, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Delete account",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = account.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = Currencies.format(account.balance, currencyCode),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            if (account.creditLimit > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { usagePct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(PixiCardShapeSm),
                    color = if (usagePct > 0.85f) Color(0xFFFF7A8A) else accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Used ${Currencies.format(account.monthlyUsage, currencyCode)} / ${Currencies.format(account.creditLimit, currencyCode)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BudgetItemRow(
    item: BudgetItemEntity,
    currencyCode: String,
    accountName: String?,
    onDelete: () -> Unit
) {
    PixiCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isExpense) Color(0xFFFF7A8A).copy(alpha = 0.15f)
                            else Color(0xFF34D399).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isExpense) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = if (item.isExpense) Color(0xFFFF7A8A) else Color(0xFF34D399),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = buildString {
                            append(item.category)
                            if (!accountName.isNullOrBlank()) append(" · $accountName")
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (item.isExpense) "-" else "+"}${Currencies.format(item.amount, currencyCode)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (item.isExpense) Color(0xFFFF7A8A) else Color(0xFF34D399)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAccountsCard(onAdd: () -> Unit) {
    PixiCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_accounts_card"),
        onClick = onAdd,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add your first account",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Bank · Cash · Credit card · Savings · Wallet",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
