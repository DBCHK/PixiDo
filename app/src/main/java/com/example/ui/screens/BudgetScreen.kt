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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.BudgetItemEntity
import com.example.data.Currencies
import com.example.data.TransactionType
import com.example.ui.components.AccountFormDialog
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.BudgetSettingsDialog
import com.example.ui.components.LivingCreditGradientBox
import com.example.ui.components.PixiBadge
import com.example.ui.components.PixiCard
import com.example.ui.components.PixiCardShape
import com.example.ui.components.PixiCardShapeSm
import com.example.ui.components.PixiDoodle3D
import com.example.ui.components.PixiEmptyState
import com.example.ui.components.PixiIslandContentInset
import com.example.ui.components.PixiScreenHeader
import com.example.ui.components.PixiSectionLabel
import com.example.ui.components.TransferDialog
import com.example.ui.components.creditGradientMutedOnColor
import com.example.ui.components.creditGradientOnColor
import com.example.ui.theme.rememberPixiDimens

@Composable
fun BudgetScreen(
    budgetItems: List<BudgetItemEntity>,
    accounts: List<AccountEntity>,
    currencyCode: String,
    monthlyAllowance: Double,
    onDeleteBudgetItem: (Int) -> Unit,
    onOpenAddBudget: () -> Unit,
    onAddAccount: (String, AccountType, Double, Double, String) -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (Int) -> Unit,
    onTransfer: (fromId: Int, toId: Int, amount: Double, note: String) -> Unit,
    onSetCurrency: (String) -> Unit,
    onSetMonthlyLimit: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val sound = LocalSoundEngine.current
    val d = rememberPixiDimens()
    var showAddAccount by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }

    val symbol = Currencies.symbolOf(currencyCode)

    // Only EXPENSE counts toward monthly budget — not lent, not income, not credit limits
    val totalExpenses = budgetItems
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }
    val totalIncome = budgetItems
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amount }
    val totalLent = budgetItems
        .filter { it.type == TransactionType.LENT }
        .sumOf { it.amount }
    val totalBorrowed = budgetItems
        .filter { it.type == TransactionType.BORROW }
        .sumOf { it.amount }

    // Credit cards: limit is NEVER part of overall budget / net worth
    val creditCards = accounts.filter { it.isCreditCard }
    val assetAccounts = accounts.filter { !it.isCreditCard }
    val assetTotal = assetAccounts.sumOf { it.balance }
    val creditUtilizedTotal = creditCards.sumOf { it.creditUtilized }
    val creditLimitTotal = creditCards.sumOf { it.creditLimit.coerceAtLeast(0.0) }
    // Net worth = assets − credit debt (limits ignored entirely)
    val netWorth = assetTotal - creditUtilizedTotal

    val hasLimit = monthlyAllowance > 0
    val remainingAllowance = if (hasLimit) (monthlyAllowance - totalExpenses).coerceAtLeast(0.0) else 0.0
    val spentPercentage = if (hasLimit) {
        (totalExpenses / monthlyAllowance).toFloat().coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(targetValue = spentPercentage, label = "spentProgress")

    val creditUtilPct = if (creditLimitTotal > 0) {
        (creditUtilizedTotal / creditLimitTotal).toFloat().coerceIn(0f, 1f)
    } else 0f
    val animatedCredit by animateFloatAsState(targetValue = creditUtilPct, label = "creditUtil")

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
                .padding(horizontal = d.screenHorizontal),
            contentPadding = PaddingValues(
                bottom = d.screenVertical + 8.dp + PixiIslandContentInset,
                top = d.screenVertical
            )
        ) {
            item {
                PixiScreenHeader(
                    title = "Budget",
                    subtitle = "This month",
                    trailing = {
                        Box(
                            modifier = Modifier
                                .clip(PixiCardShapeSm)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    sound.play(Sfx.SETTINGS_CHANGE)
                                    showSettings = true
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("budget_settings_chip")
                        ) {
                            Text(
                                text = "$currencyCode · $symbol",
                                fontSize = d.caption,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(d.sectionGap))
            }

            // ── Monthly budget summary (expenses only; credit limits excluded) ──
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasLimit) "Remaining this month" else "Net worth",
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
                                    fontSize = if (d.isCompact) 28.sp else 34.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (hasLimit) {
                                        "of ${Currencies.format(monthlyAllowance, currencyCode)} monthly limit · expenses only"
                                    } else {
                                        "Assets − card debt · limits not included"
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
                                    .height(6.dp)
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
                            Spacer(modifier = Modifier.width(10.dp))
                            CashflowPill(
                                label = "Spent",
                                value = "-${Currencies.format(totalExpenses, currencyCode)}",
                                icon = Icons.Filled.ArrowUpward,
                                tint = Color(0xFFFF7A8A),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (totalLent > 0 || totalBorrowed > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                CashflowPill(
                                    label = "Lent out",
                                    value = Currencies.format(totalLent, currencyCode),
                                    icon = Icons.AutoMirrored.Filled.CallMade,
                                    tint = Color(0xFF67D4E8),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                CashflowPill(
                                    label = "Borrowed",
                                    value = Currencies.format(totalBorrowed, currencyCode),
                                    icon = Icons.AutoMirrored.Filled.CallReceived,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Always show net worth strip when a monthly limit is set
                        if (hasLimit) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Net worth ${Currencies.format(netWorth, currencyCode)} · card limits excluded",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Credit cards: living gradient summary (Apple Card–inspired) ──
            if (creditCards.isNotEmpty()) {
                item {
                    val onInk = creditGradientOnColor(creditUtilPct)
                    val onMuted = creditGradientMutedOnColor(creditUtilPct)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 10.dp,
                                shape = PixiCardShape,
                                ambientColor = Color(0xFF9EE8C0).copy(alpha = 0.25f),
                                spotColor = Color(0xFFFF9A7A).copy(alpha = 0.2f)
                            )
                            .clip(PixiCardShape)
                            .testTag("credit_utilized_card")
                    ) {
                        LivingCreditGradientBox(
                            utilization = creditUtilPct,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (d.isCompact) 168.dp else 186.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.35f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.CreditCard,
                                                contentDescription = null,
                                                tint = onInk,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Credit cards",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = onInk
                                            )
                                            Text(
                                                text = "${creditCards.size} card${if (creditCards.size == 1) "" else "s"}",
                                                fontSize = 11.sp,
                                                color = onMuted
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.White.copy(alpha = 0.4f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (creditLimitTotal > 0) {
                                                "${(creditUtilPct * 100).toInt()}% used"
                                            } else {
                                                "No limit"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = onInk
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = "Total credit utilized",
                                    fontSize = 12.sp,
                                    color = onMuted
                                )
                                Text(
                                    text = Currencies.format(creditUtilizedTotal, currencyCode),
                                    fontSize = if (d.isCompact) 26.sp else 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = onInk
                                )
                                if (creditLimitTotal > 0) {
                                    Text(
                                        text = "${Currencies.format(creditLimitTotal, currencyCode)} limit · ${
                                            Currencies.format(
                                                (creditLimitTotal - creditUtilizedTotal).coerceAtLeast(0.0),
                                                currencyCode
                                            )
                                        } available",
                                        fontSize = 12.sp,
                                        color = onMuted
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { animatedCredit },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(PixiCardShapeSm),
                                        color = Color.White.copy(alpha = 0.75f),
                                        trackColor = Color.Black.copy(alpha = 0.12f)
                                    )
                                } else {
                                    Text(
                                        text = "Tap a card to set its credit limit",
                                        fontSize = 12.sp,
                                        color = onMuted
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PixiSectionLabel(
                        text = "Accounts",
                        action = "+ Add",
                        onAction = {
                            sound.play(Sfx.DIALOG_OPEN)
                            showAddAccount = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (accounts.size >= 2) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(PixiCardShapeSm)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    sound.play(Sfx.DIALOG_OPEN)
                                    showTransfer = true
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("transfer_accounts_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Transfer",
                                fontSize = d.caption,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
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
                                onEdit = {
                                    sound.play(Sfx.DIALOG_OPEN)
                                    editingAccount = account
                                },
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
                        subtitle = "Tap the yellow + to log expense, income, lent, or borrow in $symbol",
                        doodleRes = null
                    )
                }
            } else {
                items(budgetItems, key = { it.id }) { item ->
                    val fromName = accounts.find { it.id == item.accountId }?.name
                    val toName = accounts.find { it.id == item.relatedAccountId }?.name
                    BudgetItemRow(
                        item = item,
                        currencyCode = currencyCode,
                        accountName = when (item.type) {
                            TransactionType.TRANSFER ->
                                listOfNotNull(fromName, toName).joinToString(" → ")
                                    .ifBlank { fromName }
                            else -> fromName
                        },
                        onDelete = { onDeleteBudgetItem(item.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    if (showTransfer) {
        TransferDialog(
            currencyCode = currencyCode,
            accounts = accounts,
            onDismiss = { showTransfer = false },
            onTransfer = { from, to, amount, note ->
                sound.play(Sfx.TRANSFER)
                onTransfer(from, to, amount, note)
                showTransfer = false
            }
        )
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

    editingAccount?.let { account ->
        AccountFormDialog(
            currencyCode = currencyCode,
            existing = account,
            onDismiss = { editingAccount = null },
            onSave = { name, type, balance, limit, color ->
                val isCard = type == AccountType.CREDIT_CARD
                val owed = balance.coerceAtLeast(0.0)
                onEditAccount(
                    account.copy(
                        name = name,
                        type = type.name,
                        balance = owed,
                        creditLimit = if (isCard) limit.coerceAtLeast(0.0) else 0.0,
                        monthlyUsage = if (isCard) owed else account.monthlyUsage,
                        colorHex = color
                    )
                )
                editingAccount = null
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
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(
                value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Shared size so bank + credit account cards align in the horizontal list. */
private val AccountCardWidth = 232.dp
private val AccountCardHeight = 160.dp
private val AccountCardShape = RoundedCornerShape(26.dp)

@Composable
fun AccountCard(
    account: AccountEntity,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val type = account.accountType
    val icon = when (type) {
        AccountType.BANK -> Icons.Filled.AccountBalance
        AccountType.CASH -> Icons.Filled.Payments
        AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
        AccountType.SAVINGS -> Icons.Filled.Savings
        AccountType.WALLET -> Icons.Filled.Payments
    }
    val accent = runCatching { Color(android.graphics.Color.parseColor(account.colorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)

    val isCard = account.isCreditCard
    val utilized = account.creditUtilized
    val usagePct = if (isCard && account.creditLimit > 0) {
        (utilized / account.creditLimit).toFloat().coerceIn(0f, 1f)
    } else 0f

    if (isCard) {
        CreditCardFace(
            account = account,
            currencyCode = currencyCode,
            utilization = usagePct,
            onEdit = onEdit,
            onDelete = onDelete
        )
    } else {
        // Same outer size as credit cards for consistent row height
        Box(
            modifier = Modifier
                .width(AccountCardWidth)
                .height(AccountCardHeight)
                .shadow(
                    elevation = 4.dp,
                    shape = AccountCardShape,
                    ambientColor = accent.copy(alpha = 0.12f),
                    spotColor = accent.copy(alpha = 0.1f)
                )
                .clip(AccountCardShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onEdit)
                .testTag("account_card_${account.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
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
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit account",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
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
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = account.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = Currencies.format(account.balance, currencyCode),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
                Text(
                    text = "Tap to edit",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Credit face: soft radial bloom from center + vignette that eases as utilization rises.
 */
@Composable
private fun CreditCardFace(
    account: AccountEntity,
    currencyCode: String,
    utilization: Float,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val onInk = creditGradientOnColor(utilization)
    val onMuted = creditGradientMutedOnColor(utilization)
    val utilized = account.creditUtilized

    Box(
        modifier = Modifier
            .width(AccountCardWidth)
            .height(AccountCardHeight)
            .shadow(
                elevation = 8.dp,
                shape = AccountCardShape,
                ambientColor = Color(0xFF9EE8C0).copy(alpha = 0.22f),
                spotColor = Color(0xFFFF9A7A).copy(alpha = 0.18f)
            )
            .clip(AccountCardShape)
            .clickable(onClick = onEdit)
            .testTag("account_card_${account.id}")
    ) {
        LivingCreditGradientBox(
            utilization = utilization,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = account.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = onInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit card",
                                tint = onInk.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = "Delete card",
                                tint = onInk.copy(alpha = 0.55f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Credit card",
                    fontSize = 11.sp,
                    color = onMuted
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Utilized",
                    fontSize = 11.sp,
                    color = onMuted
                )
                Text(
                    text = Currencies.format(utilized, currencyCode),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = onInk
                )
                if (account.creditLimit > 0) {
                    Text(
                        text = "${Currencies.format(account.creditAvailable, currencyCode)} available · " +
                            "${(utilization * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Tap to set credit limit",
                        fontSize = 11.sp,
                        color = onMuted
                    )
                }
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
    val type = item.type
    val (tint, icon, sign) = when (type) {
        TransactionType.EXPENSE -> Triple(Color(0xFFFF7A8A), Icons.Filled.ArrowUpward, "-")
        TransactionType.INCOME -> Triple(Color(0xFF34D399), Icons.Filled.ArrowDownward, "+")
        TransactionType.LENT -> Triple(Color(0xFF67D4E8), Icons.AutoMirrored.Filled.CallMade, "−")
        TransactionType.BORROW -> Triple(Color(0xFFFBBF24), Icons.AutoMirrored.Filled.CallReceived, "+")
        TransactionType.TRANSFER -> Triple(Color(0xFF9B7AE8), Icons.Filled.SwapHoriz, "↔")
    }

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
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = type.displayName,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(type.displayName)
                            append(" · ")
                            append(item.category)
                            if (!accountName.isNullOrBlank()) append(" · $accountName")
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$sign${Currencies.format(item.amount, currencyCode)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PixiDoodle3D(
                resId = R.drawable.doodle_account,
                size = 120.dp,
                modifier = Modifier.padding(bottom = 8.dp),
                orbitSeconds = 7
            )
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
