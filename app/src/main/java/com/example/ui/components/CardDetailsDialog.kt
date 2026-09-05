package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AccountEntity
import com.example.data.CardNetwork
import com.example.data.Currencies
import com.example.data.maskedPan
import com.example.data.resolvedExpiry
import com.example.data.resolvedHolder
import com.example.data.resolvedNetwork

@Composable
fun CardDetailsDialog(
    account: AccountEntity,
    currencyCode: String,
    holderFallback: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val network = account.resolvedNetwork().let {
        if (it == CardNetwork.OTHER && account.isCreditCard) CardNetwork.VISA else it
    }
    val holder = account.resolvedHolder(holderFallback)
    val last4 = account.maskedPan()
    val expiry = account.resolvedExpiry()
    val nameColor = walletCardOnColor(network, account.id)
    val muted = nameColor.copy(alpha = 0.78f)
    val art = walletCardArtRes(network, account.id)
    val utilized = account.creditUtilized
    val limit = account.creditLimit
    val available = account.creditAvailable

    Dialog(onDismissRequest = onDismiss) {
        PixiCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_details_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Card details",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PixiCloseButton(onClick = onDismiss)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(188.dp)
                        .shadow(10.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(art),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = holder,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = nameColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            )
                            CardNetworkLogo(
                                network = network,
                                modifier = Modifier
                                    .width(if (network == CardNetwork.MASTERCARD) 52.dp else 60.dp)
                                    .height(if (network == CardNetwork.MASTERCARD) 30.dp else 20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = last4,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = muted,
                                letterSpacing = 1.8.sp
                            )
                            if (expiry.isNotBlank()) {
                                Text(
                                    text = "Valid $expiry",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = muted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                DetailRow("Account", account.name)
                DetailRow("Network", network.displayName)
                if (last4.isNotBlank()) DetailRow("Number", last4)
                if (expiry.isNotBlank()) DetailRow("Expiry", expiry)
                if (account.isCreditCard) {
                    DetailRow("Used", Currencies.format(utilized, currencyCode))
                    if (limit > 0) {
                        DetailRow("Limit", Currencies.format(limit, currencyCode))
                        DetailRow("Available", Currencies.format(available, currencyCode))
                    }
                } else {
                    DetailRow("Balance", Currencies.format(account.balance, currencyCode))
                }

                Spacer(modifier = Modifier.height(20.dp))
                PixiPrimaryButton(
                    text = "Edit",
                    onClick = onEdit,
                    modifier = Modifier.testTag("card_details_edit_btn")
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
