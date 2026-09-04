package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.R
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.BudgetItemEntity
import com.example.data.Currencies
import com.example.data.TransactionType
import com.example.data.UserProfile
import com.example.ui.components.AccountFormDialog
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.BudgetSettingsDialog
import com.example.ui.components.InkActionPill
import com.example.ui.components.InkAvatar
import com.example.ui.components.InkCardPaletteWallet
import com.example.ui.components.InkHeroScaffold
import com.example.ui.components.InkSheetShape
import com.example.ui.components.InkRoundIconButton
import com.example.ui.components.InkSheetSearchField
import com.example.ui.components.InkStackedCards
import com.example.ui.components.InkStatChip
import com.example.ui.components.LivingCreditGradientBox
import com.example.ui.components.PixiCard
import com.example.ui.components.PixiCardShape
import com.example.ui.components.PixiCardShapeSm
import com.example.ui.components.PixiDoodle3D
import com.example.ui.components.PixiEmptyState
import com.example.ui.components.PixiIslandContentInset
import com.example.ui.components.PixiSectionLabel
import com.example.ui.components.SpendTrendCard
import com.example.ui.components.TransferDialog
import com.example.ui.components.creditGradientMutedOnColor
import com.example.ui.components.creditGradientOnColor
import com.example.ui.theme.WalletMuted
import com.example.ui.theme.WalletOnInk
import com.example.ui.theme.rememberPixiDimens
import java.util.Calendar

private val WalletIncome = Color(0xFF22C55E)

@Composable
fun BudgetScreen(
    budgetItems: List<BudgetItemEntity>,
    accounts: List<AccountEntity>,
    currencyCode: String,
    monthlyAllowance: Double,
    profile: UserProfile = UserProfile(),
    onDeleteBudgetItem: (Int) -> Unit,
    onOpenAddBudget: () -> Unit,
    onAddAccount: (String, AccountType, Double, Double, String) -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (Int) -> Unit,
    onTransfer: (fromId: Int, toId: Int, amount: Double, note: String) -> Unit,
    onSetCurrency: (String) -> Unit,
    onSetMonthlyLimit: (Double) -> Unit,
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sound = LocalSoundEngine.current
    val d = rememberPixiDimens()
    var showAddAccount by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var showAccounts by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val symbol = Currencies.symbolOf(currencyCode)
    val monthStart = remember { startOfMonth() }

    val monthItems = remember(budgetItems, monthStart) {
        budgetItems.filter { it.timestamp >= monthStart }
    }
    val totalExpenses = monthItems
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }
    val totalIncome = monthItems
        .filter { it.type == TransactionType.INCOME }
        .sumOf { it.amount }
    val totalLent = monthItems
        .filter { it.type == TransactionType.LENT }
        .sumOf { it.amount }
    val totalBorrowed = monthItems
        .filter { it.type == TransactionType.BORROW }
        .sumOf { it.amount }

    val creditCards = accounts.filter { it.isCreditCard }
    val assetAccounts = accounts.filter { !it.isCreditCard }
    val assetTotal = assetAccounts.sumOf { it.balance }
    val creditUtilizedTotal = creditCards.sumOf { it.creditUtilized }
    val creditLimitTotal = creditCards.sumOf { it.creditLimit.coerceAtLeast(0.0) }
    val netWorth = assetTotal - creditUtilizedTotal

    val hasLimit = monthlyAllowance > 0
    val remainingAllowance = if (hasLimit) (monthlyAllowance - totalExpenses).coerceAtLeast(0.0) else 0.0
    val spentPercentage = if (hasLimit) {
        (totalExpenses / monthlyAllowance).toFloat().coerceIn(0f, 1f)
    } else 0f

    val creditUtilPct = if (creditLimitTotal > 0) {
        (creditUtilizedTotal / creditLimitTotal).toFloat().coerceIn(0f, 1f)
    } else 0f

    val (vibeStatus, vibeColor) = when {
        !hasLimit -> "This month" to WalletMuted
        spentPercentage < 0.6f -> "On track" to Color(0xFF34D399)
        spentPercentage < 0.85f -> "Watch spend" to Color(0xFFFBBF24)
        else -> "Over budget" to Color(0xFFFF7A8A)
    }

    val filteredItems = remember(budgetItems, query) {
        val q = query.trim()
        val base = if (q.isBlank()) budgetItems
        else budgetItems.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true) ||
                it.note.contains(q, ignoreCase = true)
        }
        base.sortedByDescending { it.timestamp }
    }

    val spendAvatars = remember(monthItems) {
        monthItems
            .filter { it.type == TransactionType.EXPENSE }
            .distinctBy { it.category.ifBlank { it.title } }
            .take(5)
    }

    val cardColors = remember(accounts) {
        if (accounts.isEmpty()) InkCardPaletteWallet
        else {
            val parsed = accounts.take(3).map { account ->
                runCatching { Color(android.graphics.Color.parseColor(account.colorHex)) }
                    .getOrDefault(InkCardPaletteWallet.first())
            }
            (parsed + InkCardPaletteWallet).distinct().take(3)
        }
    }
    val spendSplit = Currencies.split(totalExpenses, currencyCode)

    InkHeroScaffold(
        modifier = modifier.testTag("budget_summary_card"),
        topBar = {
            InkAvatar(
                profile = profile,
                onClick = {
                    sound.play(Sfx.PROFILE_OPEN)
                    onOpenProfile()
                },
                testTag = "wallet_avatar"
            )
            InkRoundIconButton(
                onClick = {
                    sound.play(Sfx.SETTINGS_CHANGE)
                    showSettings = true
                },
                modifier = Modifier.testTag("budget_settings_chip"),
                contentDescription = "Budget settings",
                icon = Icons.Outlined.Notifications
            )
        },
        collapsing = {
            Text(
                text = "Total balance",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = WalletMuted
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WalletBalance(
                    amount = netWorth,
                    currencyCode = currencyCode,
                    modifier = Modifier.weight(1f)
                )
                InkStackedCards(
                    colors = cardColors,
                    onClick = {
                        sound.play(Sfx.DIALOG_OPEN)
                        showAccounts = true
                    },
                    modifier = Modifier.testTag("wallet_stacked_cards")
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InkActionPill(
                    label = "Send",
                    leadingIcon = Icons.Filled.NorthEast,
                    onClick = {
                        sound.play(Sfx.DIALOG_OPEN)
                        if (accounts.size >= 2) showTransfer = true
                        else showAccounts = true
                    },
                    modifier = Modifier.testTag("wallet_send_btn")
                )
                InkActionPill(
                    label = "Request",
                    onClick = {
                        sound.play(Sfx.FAB)
                        sound.play(Sfx.DIALOG_OPEN)
                        onOpenAddBudget()
                    },
                    modifier = Modifier.testTag("wallet_request_btn")
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            InkStatChip(
                label = "Spending",
                value = "${spendSplit.symbol}${spendSplit.whole}",
                vibe = vibeStatus,
                vibeColor = vibeColor,
                avatars = spendAvatars.map { txnInitials(it) to txnAccent(it) },
                extraCount = (monthItems.filter { it.type == TransactionType.EXPENSE }.size - spendAvatars.size)
                    .coerceAtLeast(0),
                modifier = Modifier.testTag("wallet_spending_chip")
            )
            if (hasLimit) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${formatCompactMoney(remainingAllowance, currencyCode)} left of ${
                        formatCompactMoney(monthlyAllowance, currencyCode)
                    }",
                    fontSize = 12.sp,
                    color = WalletMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        sheetTitle = "Transactions",
        sheetLeading = {
            IconButton(
                onClick = {
                    sound.play(Sfx.DIALOG_OPEN)
                    showAccounts = true
                },
                modifier = Modifier.testTag("wallet_accounts_menu")
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Accounts")
            }
        },
        sheetTrailing = {
            IconButton(
                onClick = {
                    sound.play(Sfx.TAP_SOFT)
                    showSearch = !showSearch
                    if (!showSearch) query = ""
                },
                modifier = Modifier.testTag("wallet_search_btn")
            ) {
                Icon(
                    if (showSearch) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (showSearch) "Close search" else "Search"
                )
            }
        },
        sheetBanner = {
            AnimatedVisibility(visible = showSearch, enter = fadeIn(), exit = fadeOut()) {
                InkSheetSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search $symbol transactions",
                    modifier = Modifier.testTag("wallet_search_field")
                )
            }
        }
    ) {
        if (query.isBlank()) {
            item(key = "spend_trend") {
                SpendTrendCard(
                    items = budgetItems,
                    currencyCode = currencyCode,
                    monthlyAllowance = monthlyAllowance
                )
            }
        }
        if (filteredItems.isEmpty()) {
            item {
                PixiEmptyState(
                    title = if (budgetItems.isEmpty()) "No transactions yet" else "No matches",
                    subtitle = if (budgetItems.isEmpty()) {
                        "Tap Request or the + button to log spend and income in $symbol"
                    } else {
                        "Nothing matches “${query.trim()}”"
                    },
                    doodleRes = null,
                    actionLabel = if (budgetItems.isEmpty()) "Add a transaction" else null,
                    onAction = if (budgetItems.isEmpty()) onOpenAddBudget else null
                )
            }
        } else {
            items(filteredItems, key = { it.id }) { item ->
                val fromName = accounts.find { it.id == item.accountId }?.name
                val toName = accounts.find { it.id == item.relatedAccountId }?.name
                val accountName = when (item.type) {
                    TransactionType.TRANSFER ->
                        listOfNotNull(fromName, toName).joinToString(" → ")
                            .ifBlank { fromName }
                    else -> fromName
                }
                WalletTxnRow(
                    item = item,
                    currencyCode = currencyCode,
                    accountName = accountName,
                    onDelete = { onDeleteBudgetItem(item.id) }
                )
            }
        }
    }

    if (showAccounts) {
        WalletAccountsSheet(
            accounts = accounts,
            currencyCode = currencyCode,
            creditCards = creditCards,
            creditUtilizedTotal = creditUtilizedTotal,
            creditLimitTotal = creditLimitTotal,
            creditUtilPct = creditUtilPct,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            totalLent = totalLent,
            totalBorrowed = totalBorrowed,
            onDismiss = { showAccounts = false },
            onAddAccount = {
                sound.play(Sfx.DIALOG_OPEN)
                showAddAccount = true
            },
            onTransfer = {
                sound.play(Sfx.DIALOG_OPEN)
                showTransfer = true
            },
            onEdit = { account ->
                sound.play(Sfx.DIALOG_OPEN)
                editingAccount = account
            },
            onDelete = { onDeleteAccount(it) }
        )
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
private fun WalletBalance(
    amount: Double,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val split = Currencies.split(amount, currencyCode)
    val prefix = if (split.negative) "−${split.symbol}" else split.symbol
    Row(
        modifier = modifier.testTag("wallet_total_balance"),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "$prefix${split.whole}",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = WalletOnInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = ".${split.cents}",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = WalletMuted,
            modifier = Modifier.padding(bottom = 6.dp, start = 1.dp)
        )
    }
}

@Composable
fun WalletTxnRow(
    item: BudgetItemEntity,
    currencyCode: String,
    accountName: String?,
    onDelete: () -> Unit
) {
    val accent = txnAccent(item)
    val isIn = item.type == TransactionType.INCOME || item.type == TransactionType.BORROW
    val amountColor = if (isIn) WalletIncome else MaterialTheme.colorScheme.onSurface
    val sign = if (isIn) "" else "-"
    val amountText = "$sign${formatListAmount(item.amount, currencyCode)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("budget_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
            Box(modifier = Modifier.size(48.dp)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = txnInitials(item),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = buildString {
                    append(item.category.ifBlank { item.type.displayName })
                    if (!accountName.isNullOrBlank()) append(" · $accountName")
                }
                Text(
                    text = sub,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = amountText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
                maxLines = 1
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(16.dp)
                )
            }
    }
}

@Composable
private fun WalletAccountsSheet(
    accounts: List<AccountEntity>,
    currencyCode: String,
    creditCards: List<AccountEntity>,
    creditUtilizedTotal: Double,
    creditLimitTotal: Double,
    creditUtilPct: Float,
    totalIncome: Double,
    totalExpenses: Double,
    totalLent: Double,
    totalBorrowed: Double,
    onDismiss: () -> Unit,
    onAddAccount: () -> Unit,
    onTransfer: () -> Unit,
    onEdit: (AccountEntity) -> Unit,
    onDelete: (Int) -> Unit
) {
    val sound = LocalSoundEngine.current
    val animatedCredit by animateFloatAsState(targetValue = creditUtilPct, label = "creditUtil")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
                    .clip(InkSheetShape)
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("wallet_accounts_sheet")
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PixiSectionLabel(
                        text = "Accounts",
                        action = "+ Add",
                        onAction = onAddAccount,
                        modifier = Modifier.weight(1f)
                    )
                    if (accounts.size >= 2) {
                        Row(
                            modifier = Modifier
                                .clip(PixiCardShapeSm)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable(onClick = onTransfer)
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
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
                    Row(modifier = Modifier.fillMaxWidth()) {
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

                if (creditCards.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
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
                                .height(150.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp)
                            ) {
                                Text("Credit cards", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = onInk)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    Currencies.format(creditUtilizedTotal, currencyCode),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = onInk
                                )
                                if (creditLimitTotal > 0) {
                                    Text(
                                        "${(creditUtilPct * 100).toInt()}% of ${
                                            Currencies.format(creditLimitTotal, currencyCode)
                                        } limit",
                                        fontSize = 12.sp,
                                        color = onMuted
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { animatedCredit },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(PixiCardShapeSm),
                                        color = Color.White.copy(alpha = 0.75f),
                                        trackColor = Color.Black.copy(alpha = 0.12f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (accounts.isEmpty()) {
                    EmptyAccountsCard(onAdd = onAddAccount)
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        accounts.forEach { account ->
                            AccountCard(
                                account = account,
                                currencyCode = currencyCode,
                                onEdit = {
                                    sound.play(Sfx.DIALOG_OPEN)
                                    onEdit(account)
                                },
                                onDelete = { onDelete(account.id) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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

                Text(text = "Credit card", fontSize = 11.sp, color = onMuted)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Utilized", fontSize = 11.sp, color = onMuted)
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
                    Text(text = "Tap to set credit limit", fontSize = 11.sp, color = onMuted)
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

private fun startOfMonth(): Long =
    Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

internal fun txnAccent(item: BudgetItemEntity): Color {
    if (item.type == TransactionType.INCOME) return Color(0xFF22C55E)
    if (item.type == TransactionType.LENT) return Color(0xFF67D4E8)
    if (item.type == TransactionType.BORROW) return Color(0xFFFBBF24)
    if (item.type == TransactionType.TRANSFER) return Color(0xFF9B7AE8)
    val key = item.category.lowercase()
    return when {
        "food" in key || "drink" in key -> Color(0xFFFF8A4C)
        "shop" in key -> Color(0xFF60A5FA)
        "transport" in key || "uber" in key -> Color(0xFF38BDF8)
        "bill" in key || "util" in key -> Color(0xFFA78BFA)
        "health" in key -> Color(0xFF34D399)
        "entertain" in key -> Color(0xFFF472B6)
        "salary" in key -> Color(0xFF22C55E)
        else -> {
            val palette = listOf(
                Color(0xFF60A5FA), Color(0xFFFBBF24), Color(0xFF34D399),
                Color(0xFFF472B6), Color(0xFFA78BFA), Color(0xFF67D4E8)
            )
            palette[kotlin.math.abs(item.title.hashCode()) % palette.size]
        }
    }
}

internal fun txnInitials(item: BudgetItemEntity): String {
    val src = item.title.ifBlank { item.category }.trim()
    val parts = src.split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        src.isNotEmpty() -> src.take(1).uppercase()
        else -> "•"
    }
}

internal fun formatListAmount(amount: Double, currencyCode: String): String {
    val split = Currencies.split(amount, currencyCode)
    return if (split.cents == "00") "${split.whole} ${split.symbol}" else "${split.whole}.${split.cents} ${split.symbol}"
}

internal fun formatCompactMoney(amount: Double, currencyCode: String): String {
    val split = Currencies.split(amount, currencyCode)
    val prefix = if (split.negative) "-${split.symbol}" else split.symbol
    return if (split.cents == "00") "$prefix${split.whole}" else "$prefix${split.whole}.${split.cents}"
}
