package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.AccountType
import com.example.data.CardNetwork
import com.example.data.Currencies

data class AccountFormData(
    val name: String,
    val type: AccountType,
    val balance: Double,
    val creditLimit: Double,
    val colorHex: String,
    val cardNetwork: String = "",
    val lastFour: String = "",
    val expiryMonth: Int = 0,
    val expiryYear: Int = 0,
    val cardholderName: String = ""
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddAccountDialog(
    currencyCode: String,
    onDismiss: () -> Unit,
    onAdd: (AccountFormData) -> Unit
) {
    AccountFormDialog(
        currencyCode = currencyCode,
        existing = null,
        onDismiss = onDismiss,
        onSave = onAdd
    )
}

/**
 * Create or edit bank / credit / wallet accounts.
 * After an account is added, open this with [existing] to change balances, limits, and details.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountFormDialog(
    currencyCode: String,
    existing: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (AccountFormData) -> Unit
) {
    val isEdit = existing != null
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var type by remember(existing?.id) {
        mutableStateOf(
            existing?.accountType ?: AccountType.BANK
        )
    }
    var balanceStr by remember(existing?.id) {
        mutableStateOf(
            existing?.balance?.takeIf { it != 0.0 }?.let { trimNum(it) }.orEmpty()
        )
    }
    var limitStr by remember(existing?.id) {
        mutableStateOf(
            existing?.creditLimit?.takeIf { it > 0 }?.let { trimNum(it) }.orEmpty()
        )
    }
    var network by remember(existing?.id) {
        mutableStateOf(
            CardNetwork.fromStorage(existing?.cardNetwork).takeIf { it != CardNetwork.OTHER }
                ?: CardNetwork.infer(existing?.name.orEmpty(), existing?.notes.orEmpty()).takeIf {
                    it != CardNetwork.OTHER
                }
                ?: CardNetwork.VISA
        )
    }
    var lastFour by remember(existing?.id) {
        mutableStateOf(existing?.lastFour?.filter { it.isDigit() }?.takeLast(4).orEmpty())
    }
    var holder by remember(existing?.id) {
        mutableStateOf(existing?.cardholderName.orEmpty())
    }
    var expiryStr by remember(existing?.id) {
        mutableStateOf(
            CardNetwork.formatExpiry(
                existing?.expiryMonth ?: 0,
                existing?.expiryYear ?: 0
            )
        )
    }

    // Expanded soft palette for banks, cards, wallets, savings
    val colors = listOf(
        // Lilacs & violets
        "#C4A8F5", "#9B7AE8", "#A78BFA", "#8B5CF6", "#DDD6FE",
        // Pinks & roses
        "#FF6BA8", "#F472B6", "#FB7185", "#F43F5E", "#FDA4AF",
        // Blues & cyans
        "#67D4E8", "#60A5FA", "#38BDF8", "#0EA5E9", "#22D3EE", "#818CF8",
        // Greens & mints
        "#34D399", "#6EE7B7", "#10B981", "#84CC16", "#A3E635",
        // Warm golds & oranges
        "#FBBF24", "#FFE566", "#F59E0B", "#FB923C", "#F97316", "#D4A574",
        // Neutrals
        "#94A3B8", "#64748B", "#1C1C1E", "#E2E8F0"
    )
    var selectedColor by remember(existing?.id) {
        mutableStateOf(
            existing?.colorHex?.takeIf { hex ->
                colors.any { it.equals(hex, ignoreCase = true) }
            } ?: existing?.colorHex?.takeIf { it.isNotBlank() } ?: colors[0]
        )
    }

    val symbol = Currencies.symbolOf(currencyCode)

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
                .testTag(if (isEdit) "edit_account_dialog" else "add_account_dialog")
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
                        text = if (isEdit) "Edit Account" else "Add Account",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PixiCloseButton(onClick = onDismiss)
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account name") },
                    placeholder = { Text("e.g. Chase Checking, Amex") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_account_name"),
                    singleLine = true,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Type",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountType.entries.forEach { t ->
                        val label = t.name.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.uppercase() }
                        PixiChip(
                            label = label,
                            selected = t == type,
                            onClick = { type = t }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = {
                        Text(
                            if (type == AccountType.CREDIT_CARD) {
                                "Amount currently owed ($symbol)"
                            } else {
                                "Balance ($symbol)"
                            }
                        )
                    },
                    placeholder = { Text("0.00") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_account_balance"),
                    singleLine = true,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )
                if (type == AccountType.CREDIT_CARD) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Debt on the card — not your credit limit.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (type == AccountType.CREDIT_CARD) {
                    OutlinedTextField(
                        value = limitStr,
                        onValueChange = { limitStr = it },
                        label = { Text("Credit limit ($symbol)") },
                        placeholder = { Text("e.g. 2000") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_account_limit"),
                        singleLine = true,
                        shape = PixiFieldShape,
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Limit is never added to overall budget or net worth.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (type == AccountType.CREDIT_CARD) {
                    Text(
                        text = "Card network",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            CardNetwork.VISA,
                            CardNetwork.MASTERCARD,
                            CardNetwork.RUPAY
                        ).forEach { net ->
                            val selected = network == net
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(PixiCardShapeSm)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = PixiCardShapeSm
                                    )
                                    .clickable { network = net }
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                                    .testTag("card_network_${net.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CardNetworkLogo(
                                        network = net,
                                        modifier = Modifier
                                            .width(52.dp)
                                            .height(20.dp),
                                        onDark = false
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = net.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = holder,
                        onValueChange = { holder = it },
                        label = { Text("Name on card") },
                        placeholder = { Text("As printed on the card") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_cardholder"),
                        singleLine = true,
                        shape = PixiFieldShape,
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = lastFour,
                            onValueChange = { lastFour = it.filter { ch -> ch.isDigit() }.take(4) },
                            label = { Text("Last 4 digits") },
                            placeholder = { Text("5678") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_card_last4"),
                            singleLine = true,
                            shape = PixiFieldShape,
                            colors = fieldColors
                        )
                        OutlinedTextField(
                            value = expiryStr,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(4)
                                expiryStr = when {
                                    digits.length <= 2 -> digits
                                    else -> digits.take(2) + "/" + digits.drop(2)
                                }
                            },
                            label = { Text("Valid thru") },
                            placeholder = { Text("MM/YY") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_card_expiry"),
                            singleLine = true,
                            shape = PixiFieldShape,
                            colors = fieldColors
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "Color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pick a shade for this account",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val palette = buildList {
                        val existingHex = existing?.colorHex.orEmpty()
                        if (existingHex.isNotBlank() &&
                            colors.none { it.equals(existingHex, ignoreCase = true) }
                        ) {
                            add(existingHex)
                        }
                        addAll(colors)
                    }
                    palette.forEach { hex ->
                        val c = runCatching {
                            androidx.compose.ui.graphics.Color(
                                android.graphics.Color.parseColor(hex)
                            )
                        }.getOrNull() ?: return@forEach
                        val isSel = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(if (isSel) 34.dp else 30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSel) 2.5.dp else 1.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                PixiPrimaryButton(
                    text = if (isEdit) "Save Changes" else "Create Account",
                    onClick = {
                        val isCard = type == AccountType.CREDIT_CARD
                        val (expM, expY) = CardNetwork.parseExpiry(expiryStr)
                        val inferred = CardNetwork.infer(name)
                        onSave(
                            AccountFormData(
                                name = name.ifBlank { type.name.replace('_', ' ') },
                                type = type,
                                balance = balanceStr.toDoubleOrNull() ?: 0.0,
                                creditLimit = limitStr.toDoubleOrNull() ?: 0.0,
                                colorHex = selectedColor,
                                cardNetwork = if (isCard) {
                                    (if (inferred != CardNetwork.OTHER) inferred else network).name
                                } else "",
                                lastFour = if (isCard) lastFour.filter { it.isDigit() }.take(4) else "",
                                expiryMonth = if (isCard) expM else 0,
                                expiryYear = if (isCard) expY else 0,
                                cardholderName = if (isCard) holder.trim() else ""
                            )
                        )
                    },
                    modifier = Modifier.testTag(
                        if (isEdit) "submit_edit_account_btn" else "submit_add_account_btn"
                    )
                )
            }
        }
    }
}

private fun trimNum(v: Double): String {
    return if (v == v.toLong().toDouble()) v.toLong().toString()
    else "%.2f".format(v).trimEnd('0').trimEnd('.')
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetSettingsDialog(
    currencyCode: String,
    monthlyLimit: Double,
    onDismiss: () -> Unit,
    onSave: (currencyCode: String, monthlyLimit: Double) -> Unit
) {
    var selectedCurrency by remember { mutableStateOf(currencyCode) }
    var limitStr by remember {
        mutableStateOf(if (monthlyLimit > 0) monthlyLimit.toString() else "")
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
                .testTag("budget_settings_dialog")
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
                        text = "Budget Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PixiCloseButton(onClick = onDismiss)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Currency",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Currencies.all.forEach { info ->
                        PixiChip(
                            label = "${info.symbol} ${info.code}",
                            selected = info.code == selectedCurrency,
                            onClick = { selectedCurrency = info.code }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Monthly budget limit (empty = none)") },
                    placeholder = { Text("0") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_monthly_limit"),
                    singleLine = true,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(22.dp))

                PixiPrimaryButton(
                    text = "Save Settings",
                    onClick = {
                        onSave(selectedCurrency, limitStr.toDoubleOrNull() ?: 0.0)
                    },
                    modifier = Modifier.testTag("save_budget_settings_btn")
                )
            }
        }
    }
}
