package com.example.data

/**
 * Payment network printed on a credit / debit card.
 * Stored as [AccountEntity.cardNetwork]; inferred from the account name when blank.
 */
enum class CardNetwork {
    VISA,
    MASTERCARD,
    RUPAY,
    OTHER;

    val displayName: String
        get() = when (this) {
            VISA -> "Visa"
            MASTERCARD -> "Mastercard"
            RUPAY -> "RuPay"
            OTHER -> "Other"
        }

    companion object {
        fun fromStorage(raw: String?): CardNetwork {
            if (raw.isNullOrBlank()) return OTHER
            return runCatching { valueOf(raw.trim().uppercase()) }.getOrDefault(OTHER)
        }

        fun infer(name: String, notes: String = ""): CardNetwork {
            val n = "$name $notes".lowercase()
            return when {
                "rupay" in n || "ru pay" in n || "ru-pay" in n -> RUPAY
                "visa" in n -> VISA
                "master" in n || n.contains("mc ") || n.endsWith(" mc") -> MASTERCARD
                else -> OTHER
            }
        }

        fun extractLast4(vararg sources: String): String {
            val combined = sources.joinToString(" ")
            return Regex("(?<!\\d)(\\d{4})(?!\\d)")
                .findAll(combined)
                .lastOrNull()
                ?.groupValues
                ?.getOrNull(1)
                .orEmpty()
        }

        fun formatExpiry(month: Int, year: Int): String {
            if (month !in 1..12 || year == 0) return ""
            val yy = if (year < 100) year else year % 100
            return "%02d/%02d".format(month, yy)
        }

        fun parseExpiry(raw: String): Pair<Int, Int> {
            val digits = raw.filter { it.isDigit() }
            if (digits.length < 3) return 0 to 0
            val month = digits.take(2).toIntOrNull() ?: return 0 to 0
            val year = digits.drop(2).take(2).toIntOrNull() ?: return 0 to 0
            if (month !in 1..12) return 0 to 0
            return month to year
        }
    }
}

fun AccountEntity.resolvedNetwork(): CardNetwork {
    val stored = CardNetwork.fromStorage(cardNetwork)
    if (stored != CardNetwork.OTHER) return stored
    return CardNetwork.infer(name, notes)
}

fun AccountEntity.resolvedLast4(): String {
    val stored = lastFour.filter { it.isDigit() }.takeLast(4)
    if (stored.length == 4) return stored
    return CardNetwork.extractLast4(name, notes)
}

fun AccountEntity.resolvedExpiry(): String =
    CardNetwork.formatExpiry(expiryMonth, expiryYear)

fun AccountEntity.resolvedHolder(fallback: String = ""): String {
    val named = cardholderName.trim()
    if (named.isNotEmpty()) return named
    val fromFallback = fallback.trim()
    if (fromFallback.isNotEmpty()) return fromFallback
    return name
}

fun AccountEntity.maskedPan(): String {
    val last4 = resolvedLast4()
    return if (last4.length == 4) "••••  $last4" else "••••  ••••"
}
