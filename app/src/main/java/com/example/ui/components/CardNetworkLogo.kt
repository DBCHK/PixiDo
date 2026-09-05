package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.CardNetwork

/**
 * Visa / Mastercard / RuPay marks from the wallet art pack.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun CardNetworkLogo(
    network: CardNetwork,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    tint: androidx.compose.ui.graphics.Color? = null
) {
    val tag = when (network) {
        CardNetwork.VISA -> "logo_visa"
        CardNetwork.MASTERCARD -> "logo_mastercard"
        CardNetwork.RUPAY -> "logo_rupay"
        CardNetwork.OTHER -> "logo_card_generic"
    }
    val res = networkLogoRes(network)
    Box(
        modifier = modifier.testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        if (res != 0) {
            Image(
                painter = painterResource(res),
                contentDescription = network.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@DrawableRes
fun networkLogoRes(network: CardNetwork): Int = when (network) {
    CardNetwork.VISA -> R.drawable.logo_visa
    CardNetwork.MASTERCARD -> R.drawable.logo_mastercard
    CardNetwork.RUPAY -> R.drawable.logo_rupay
    CardNetwork.OTHER -> 0
}

@DrawableRes
fun walletCardArtRes(network: CardNetwork, accountId: Int): Int = when (network) {
    CardNetwork.VISA -> R.drawable.card_black
    CardNetwork.MASTERCARD -> R.drawable.card_orange
    CardNetwork.RUPAY -> R.drawable.card_blue
    CardNetwork.OTHER -> if (accountId % 2 == 0) R.drawable.card_peach else R.drawable.card_black
}

fun walletCardOnColor(network: CardNetwork, accountId: Int): androidx.compose.ui.graphics.Color {
    val darkFace = when (network) {
        CardNetwork.VISA -> true
        CardNetwork.RUPAY -> true
        CardNetwork.MASTERCARD -> false
        CardNetwork.OTHER -> accountId % 2 != 0
    }
    return if (darkFace) androidx.compose.ui.graphics.Color.White
    else androidx.compose.ui.graphics.Color(0xFF2A1450)
}
