package com.example

import com.example.R
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.CardNetwork
import com.example.data.maskedPan
import com.example.data.resolvedLast4
import com.example.data.resolvedNetwork
import com.example.ui.components.networkLogoRes
import com.example.ui.components.walletCardArtRes
import org.junit.Assert.assertEquals
import org.junit.Test

class CardNetworkTest {

    @Test
    fun infersVisaMastercardRupayFromName() {
        assertEquals(CardNetwork.VISA, CardNetwork.infer("HDFC Visa"))
        assertEquals(CardNetwork.MASTERCARD, CardNetwork.infer("Axis Mastercard"))
        assertEquals(CardNetwork.RUPAY, CardNetwork.infer("SBI RuPay"))
        assertEquals(CardNetwork.OTHER, CardNetwork.infer("Everyday"))
    }

    @Test
    fun extractsLast4AndFormatsExpiry() {
        assertEquals("5678", CardNetwork.extractLast4("Visa 5678", "note"))
        assertEquals("05/29", CardNetwork.formatExpiry(5, 29))
        assertEquals("05/29", CardNetwork.formatExpiry(5, 2029))
        assertEquals(5 to 29, CardNetwork.parseExpiry("05/29"))
        assertEquals(5 to 29, CardNetwork.parseExpiry("0529"))
    }

    @Test
    fun accountResolvesStoredThenInferred() {
        val visa = AccountEntity(
            id = 1,
            name = "Travel",
            type = AccountType.CREDIT_CARD.name,
            cardNetwork = CardNetwork.VISA.name,
            lastFour = "5678",
            cardholderName = "Alex"
        )
        assertEquals(CardNetwork.VISA, visa.resolvedNetwork())
        assertEquals("5678", visa.resolvedLast4())
        assertEquals("••••  5678", visa.maskedPan())

        val inferred = AccountEntity(
            id = 2,
            name = "RuPay Select 8809",
            type = AccountType.CREDIT_CARD.name
        )
        assertEquals(CardNetwork.RUPAY, inferred.resolvedNetwork())
        assertEquals("8809", inferred.resolvedLast4())
    }

    @Test
    fun walletPackAssetsMapToNetworks() {
        assertEquals(R.drawable.card_black, walletCardArtRes(CardNetwork.VISA, 1))
        assertEquals(R.drawable.card_orange, walletCardArtRes(CardNetwork.MASTERCARD, 1))
        assertEquals(R.drawable.card_blue, walletCardArtRes(CardNetwork.RUPAY, 1))
        assertEquals(R.drawable.card_peach, walletCardArtRes(CardNetwork.OTHER, 2))
        assertEquals(R.drawable.card_black, walletCardArtRes(CardNetwork.OTHER, 1))
        assertEquals(R.drawable.logo_visa, networkLogoRes(CardNetwork.VISA))
        assertEquals(R.drawable.logo_mastercard, networkLogoRes(CardNetwork.MASTERCARD))
        assertEquals(R.drawable.logo_rupay, networkLogoRes(CardNetwork.RUPAY))
        assertEquals(0, networkLogoRes(CardNetwork.OTHER))
    }
}
