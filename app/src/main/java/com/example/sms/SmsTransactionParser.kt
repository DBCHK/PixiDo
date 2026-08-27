package com.example.sms

import java.security.MessageDigest

/**
 * Parses Indian bank / UPI / card SMS into a structured transaction.
 *
 * Handles SBI, HDFC, ICICI, Axis, Kotak, Yes, IDFC, Paytm, PhonePe, GPay, etc.
 * Amount prefers the debit/credit figure over available-balance figures.
 */
object SmsTransactionParser {

    data class ParsedTransaction(
        val amount: Double,
        val bankName: String,
        val isExpense: Boolean,
        val merchantOrInfo: String = "",
        val confidence: Float = 1f,
        val refId: String = "",
        val accountLast4: String = "",
        val category: String = "Other",
        val channel: String = CHANNEL_OTHER,
        val isWallet: Boolean = false
    )

    const val CHANNEL_UPI = "UPI"
    const val CHANNEL_CARD = "CARD"
    const val CHANNEL_ATM = "ATM"
    const val CHANNEL_IMPS = "IMPS"
    const val CHANNEL_NEFT = "NEFT"
    const val CHANNEL_RTGS = "RTGS"
    const val CHANNEL_OTHER = "OTHER"

    /** Known bank / wallet labels mapped from SMS sender address fragments. */
    private val senderBankMap = listOf(
        "HDFCBK" to "HDFC Bank",
        "HDFC" to "HDFC Bank",
        "SBIINB" to "SBI",
        "SBICRD" to "SBI",
        "SBIUPI" to "SBI",
        "ATMSBI" to "SBI",
        "SBI" to "SBI",
        "ICICIB" to "ICICI Bank",
        "ICICI" to "ICICI Bank",
        "AXISBN" to "Axis Bank",
        "AXISBK" to "Axis Bank",
        "AXISB" to "Axis Bank",
        "AXIS" to "Axis Bank",
        "KOTAKB" to "Kotak Mahindra",
        "KOTAK" to "Kotak Mahindra",
        "YESBNK" to "Yes Bank",
        "YESB" to "Yes Bank",
        "IDFCFB" to "IDFC First",
        "IDFC" to "IDFC First",
        "INDUSB" to "IndusInd Bank",
        "INDBNK" to "Indian Bank",
        "CANBNK" to "Canara Bank",
        "CNRBNK" to "Canara Bank",
        "BOIIND" to "Bank of India",
        "MAHABK" to "Bank of Maharashtra",
        "PNBSMS" to "PNB",
        "PUNB" to "PNB",
        "UBIIN" to "Union Bank",
        "FEDBNK" to "Federal Bank",
        "RBLBNK" to "RBL Bank",
        "SCBANK" to "Standard Chartered",
        "HSBCIN" to "HSBC",
        "CITIBK" to "Citi Bank",
        "DBSBNK" to "DBS Bank",
        "AIRBNK" to "Airtel Payments Bank",
        "PAYTMB" to "Paytm Payments Bank",
        "PAYTM" to "Paytm",
        "PHONEPE" to "PhonePe",
        "PHONPE" to "PhonePe",
        "GPAY" to "Google Pay",
        "GOOGL" to "Google Pay",
        "AMAZON" to "Amazon Pay",
        "MOBIKW" to "MobiKwik",
        "FREECH" to "Freecharge",
        "BHIM" to "BHIM UPI",
        "CRED" to "CRED",
        "NACH" to "NACH",
        "UPI" to "UPI"
    )

    private val walletBanks = setOf(
        "Paytm", "PhonePe", "Google Pay", "Amazon Pay", "MobiKwik",
        "Freecharge", "BHIM UPI", "CRED", "Paytm Payments Bank"
    )

    private val bodyBankPatterns = listOf(
        Regex("""\bHDFC\s*Bank\b""", RegexOption.IGNORE_CASE) to "HDFC Bank",
        Regex("""\bICICI\s*Bank\b""", RegexOption.IGNORE_CASE) to "ICICI Bank",
        Regex("""\bAxis\s*Bank\b""", RegexOption.IGNORE_CASE) to "Axis Bank",
        Regex("""\bKotak\b""", RegexOption.IGNORE_CASE) to "Kotak Mahindra",
        Regex("""\bYes\s*Bank\b""", RegexOption.IGNORE_CASE) to "Yes Bank",
        Regex("""\bIDFC\b""", RegexOption.IGNORE_CASE) to "IDFC First",
        Regex("""\bState\s+Bank\s+of\s+India\b""", RegexOption.IGNORE_CASE) to "SBI",
        Regex("""\bSBI\b""", RegexOption.IGNORE_CASE) to "SBI",
        Regex("""\bIndusInd\b""", RegexOption.IGNORE_CASE) to "IndusInd Bank",
        Regex("""\bCanara\s*Bank\b""", RegexOption.IGNORE_CASE) to "Canara Bank",
        Regex("""\bBank\s+of\s+India\b""", RegexOption.IGNORE_CASE) to "Bank of India",
        Regex("""\bPNB\b""", RegexOption.IGNORE_CASE) to "PNB",
        Regex("""\bFederal\s*Bank\b""", RegexOption.IGNORE_CASE) to "Federal Bank",
        Regex("""\bRBL\b""", RegexOption.IGNORE_CASE) to "RBL Bank",
        Regex("""\bPaytm\b""", RegexOption.IGNORE_CASE) to "Paytm",
        Regex("""\bPhonePe\b""", RegexOption.IGNORE_CASE) to "PhonePe",
        Regex("""\bGoogle\s*Pay\b|\bGPay\b""", RegexOption.IGNORE_CASE) to "Google Pay"
    )

    private val amountPatterns = listOf(
        Regex("""(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""₹\s*([\d,]+(?:\.\d{1,2})?)"""),
        Regex("""([\d,]+(?:\.\d{1,2})?)\s*(?:Rs\.?|INR|₹)""", RegexOption.IGNORE_CASE)
    )

    private val debitPhrases = listOf(
        "debited", "has been spent", "spent on", "spent at", "paid to", "paid rs",
        "paid inr", "paid ₹", "you paid", "you've paid", "you have paid",
        "purchase of", "withdrawn", "withdrawal", "sent to", "transferred to",
        "payment of", "charged", "debit alert", "dr alert", "pos purchase"
    )

    private val creditPhrases = listOf(
        "credited", "has been received", "received from", "received rs",
        "deposited", "refund", "cashback of", "salary credited",
        "neft cr", "imps cr", "upi cr", "cr alert", "you received"
    )

    private val skipPhrases = listOf(
        "otp", "one time password", "do not share", "dont share", "don't share",
        "declined", "failed", "failure", "unsuccessful", "not successful",
        "could not", "can't be", "cannot be", "not processed", "rejected",
        "insufficient", "kyc", "click here", "download the app", "offer:",
        "win rs", "get cashback on", "is due on", "overdue", "reminder:",
        "will be debited", "requested to debit", "mini statement", "ministatement",
        "last 5 txn", "your statement"
    )

    private val expenseCategoryHints = listOf(
        listOf("swiggy", "zomato", "dominos", "starbucks", "cafe", "restaurant", "dunzo") to "Food & Drink",
        listOf("uber", "ola ", "rapido", "irctc", "redbus", "metro", "fastag", "petrol", "fuel") to "Transport",
        listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa") to "Shopping",
        listOf("netflix", "spotify", "youtube", "hotstar", "prime video", "apple.com/bill") to "Subscriptions",
        listOf("airtel", "jio", "vi ", "bsnl", "electricity", "gas bill", "water bill", "broadband") to "Bills",
        listOf("pharmacy", "1mg", "pharmeasy", "apollo", "hospital", "clinic") to "Health",
        listOf("bookmyshow", "pvr", "inox") to "Entertainment",
        listOf("atm", "cash wdl", "cash withdrawal") to "Other"
    )

    private val incomeCategoryHints = listOf(
        listOf("salary") to "Salary",
        listOf("refund") to "Refund",
        listOf("interest", "dividend", "mutual fund") to "Investments",
        listOf("cashback") to "Refund"
    )

    fun isWalletBank(bankName: String): Boolean = walletBanks.contains(bankName)

    fun looksLikeTransactionSms(body: String, sender: String = ""): Boolean {
        if (body.isBlank()) return false
        val text = normalizeBody(body)
        if (skipPhrases.any { text.contains(it) }) return false

        val loanish = text.contains("loan") || text.contains("emi") || text.contains("lending")
        if (loanish) {
            val settled = debitPhrases.any { text.contains(it) } || creditPhrases.any { text.contains(it) }
            val reminder = text.contains("due") || text.contains("overdue") || text.contains("reminder")
            if (!settled || reminder && !text.contains("debited") && !text.contains("credited")) {
                return false
            }
        }

        val hasMoney = text.contains("rs") || text.contains("inr") || text.contains("₹") ||
            text.contains("rupee")
        if (!hasMoney) return false
        val hasVerb = debitPhrases.any { text.contains(it) } ||
            creditPhrases.any { text.contains(it) } ||
            text.contains("upi") ||
            text.contains("neft") ||
            text.contains("imps")
        val knownSender = resolveBankFromSender(sender) != null
        return hasVerb || knownSender
    }

    fun parse(body: String, sender: String = ""): ParsedTransaction? {
        if (body.isBlank()) return null
        if (!looksLikeTransactionSms(body, sender)) return null

        val amount = extractTransactionAmount(body) ?: return null
        if (amount <= 0.0 || amount > 50_000_000.0) return null

        val isExpense = resolveIsExpense(body) ?: return null
        val bankName = resolveBankName(body, sender)
        val info = extractMerchantOrInfo(body)
        val refId = extractRefId(body)
        val last4 = extractAccountLast4(body)
        val channel = resolveChannel(body)
        val category = resolveCategory(body, info, isExpense)
        val wallet = isWalletBank(bankName)
        val confidence = when {
            bankName != "Bank" && (refId.isNotBlank() || info.isNotBlank()) -> 1f
            bankName != "Bank" -> 0.9f
            else -> 0.7f
        }

        return ParsedTransaction(
            amount = amount,
            bankName = bankName,
            isExpense = isExpense,
            merchantOrInfo = info,
            confidence = confidence,
            refId = refId,
            accountLast4 = last4,
            category = category,
            channel = channel,
            isWallet = wallet
        )
    }

    fun extractTransactionAmount(body: String): Double? {
        val normalized = body.replace('\u00A0', ' ')
        val candidates = mutableListOf<Pair<Int, Double>>()

        for (pattern in amountPatterns) {
            pattern.findAll(normalized).forEach { match ->
                val raw = match.groupValues.drop(1).firstOrNull { it.isNotBlank() } ?: return@forEach
                val value = raw.replace(",", "").toDoubleOrNull() ?: return@forEach
                candidates += match.range.first to value
            }
        }
        if (candidates.isEmpty()) return null

        val balanceHint = Regex(
            """(?:avl(?:ailable)?\s*bal(?:ance)?|available\s*balance|bal(?:ance)?\s*[:=]|closing\s*bal|total\s*bal)""",
            RegexOption.IGNORE_CASE
        )
        val nonBalance = candidates.filter { (index, _) ->
            val windowStart = (index - 28).coerceAtLeast(0)
            val prefix = normalized.substring(windowStart, index)
            !balanceHint.containsMatchIn(prefix)
        }

        val pool = nonBalance.ifEmpty { candidates }
        val verb = Regex(
            """debited|credited|spent|paid|withdrawn|received|deposited|purchase|sent""",
            RegexOption.IGNORE_CASE
        )
        val withVerb = pool.minByOrNull { (index, _) ->
            val nearby = normalized.substring(
                (index - 40).coerceAtLeast(0),
                (index + 40).coerceAtMost(normalized.length)
            )
            if (verb.containsMatchIn(nearby)) 0 else 1
        }
        return withVerb?.second ?: pool.first().second
    }

    fun resolveIsExpense(body: String): Boolean? {
        val text = normalizeBody(body)
        val debitScore = debitPhrases.count { text.contains(it) }
        val creditScore = creditPhrases.count { text.contains(it) }
        return when {
            debitScore > creditScore -> true
            creditScore > debitScore -> false
            else -> null
        }
    }

    fun resolveBankName(body: String, sender: String): String {
        resolveBankFromSender(sender)?.let { return it }
        for ((pattern, name) in bodyBankPatterns) {
            if (pattern.containsMatchIn(body)) return name
        }
        return "Bank"
    }

    fun resolveBankFromSender(sender: String): String? {
        if (sender.isBlank()) return null
        val upper = normalizeSender(sender)
        for ((key, name) in senderBankMap) {
            if (upper.contains(key)) return name
        }
        return null
    }

    fun extractMerchantOrInfo(body: String): String {
        val patterns = listOf(
            Regex("""(?:to\s+VPA|VPA\s+)([A-Za-z0-9@._\-]{3,40})""", RegexOption.IGNORE_CASE),
            Regex("""(?:paid to|to|at|towards|info[:\s]+)\s*([A-Za-z][A-Za-z0-9@._\-\s]{2,40})""", RegexOption.IGNORE_CASE),
            Regex("""UPI[/\-\s]+([A-Za-z0-9@._\-]{3,40})""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val m = p.find(body) ?: continue
            val cleaned = cleanMerchant(m.groupValues.getOrNull(1).orEmpty())
            if (cleaned.isNotBlank()) return cleaned
        }
        return ""
    }

    fun extractRefId(body: String): String {
        val patterns = listOf(
            Regex(
                """(?:UPI\s*(?:Ref(?:erence)?(?:\s*no\.?)?)|RRN|UTR|Txn(?:n)?(?:\s*(?:id|ref))?|Ref(?:erence)?(?:\s*no\.?)?)[:\s#\-]*([0-9]{9,18})""",
                RegexOption.IGNORE_CASE
            ),
            Regex("""\b([0-9]{12,12})\b""")
        )
        for (p in patterns) {
            val m = p.find(body) ?: continue
            val id = m.groupValues.getOrNull(1)?.trim().orEmpty()
            if (id.length in 9..18) return id
        }
        return ""
    }

    fun extractAccountLast4(body: String): String {
        val patterns = listOf(
            Regex("""(?:a/?c|acct|account|card)\s*(?:no\.?|ending)?\s*(?:\*{2,}|\bXX)?(\d{4})\b""", RegexOption.IGNORE_CASE),
            Regex("""(?:ending|xx|\*{2,})(\d{4})\b""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val m = p.find(body) ?: continue
            val last4 = m.groupValues.getOrNull(1).orEmpty()
            if (last4.length == 4) return last4
        }
        return ""
    }

    fun resolveChannel(body: String): String {
        val text = normalizeBody(body)
        return when {
            text.contains("upi") || text.contains("vpa") -> CHANNEL_UPI
            text.contains("imps") -> CHANNEL_IMPS
            text.contains("neft") -> CHANNEL_NEFT
            text.contains("rtgs") -> CHANNEL_RTGS
            text.contains("atm") -> CHANNEL_ATM
            text.contains("credit card") || text.contains("debit card") ||
                text.contains(" card ") || text.contains("pos ") -> CHANNEL_CARD
            else -> CHANNEL_OTHER
        }
    }

    fun resolveCategory(body: String, merchant: String, isExpense: Boolean): String {
        val hay = normalizeBody("$body $merchant")
        val table = if (isExpense) expenseCategoryHints else incomeCategoryHints
        for ((needles, category) in table) {
            if (needles.any { hay.contains(it) }) return category
        }
        return "Other"
    }

    /** Same SMS regardless of inbox vs broadcast timestamp. */
    fun contentHash(body: String, sender: String): String {
        val core = "${normalizeSender(sender)}|${normalizeBody(body)}"
        return sha256Prefix(core)
    }

    /** @deprecated Use [contentHash]; kept so older call sites compile if any remain. */
    fun smsHash(body: String, sender: String, timestampMillis: Long = 0L): String {
        if (timestampMillis == 0L) return contentHash(body, sender)
        return contentHash(body, sender)
    }

    fun normalizeBody(body: String): String =
        body.replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()

    fun normalizeSender(sender: String): String {
        var s = sender.uppercase().replace(Regex("[^A-Z0-9]"), "")
        if (s.length > 4 && s.take(2).all { it.isLetter() }) {
            val rest = s.substring(2)
            if (senderBankMap.any { rest.contains(it.first) }) s = rest
        }
        return s
    }

    fun merchantTokens(merchant: String): Set<String> =
        merchant.lowercase()
            .split(Regex("[^a-z0-9@]+"))
            .filter { it.length >= 3 && it !in setOf("upi", "vpa", "okaxis", "okhdfc", "paytm") }
            .toSet()

    private fun cleanMerchant(raw: String): String {
        var s = raw.trim()
        s = s.replace(Regex("""(?i)\s*(avl|available|bal(?:ance)?|ref(?:erence)?|upi ref|on \d).*"""), "")
        s = s.trim(' ', '.', ',', ';', '-', ':')
        val lower = s.lowercase()
        if (s.length < 3) return ""
        if (lower.contains("bal") || lower.startsWith("rs") || lower.startsWith("inr")) return ""
        if (lower.startsWith("a/c") || lower.startsWith("acct") || lower.startsWith("account")) return ""
        return s.take(48)
    }

    private fun sha256Prefix(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.take(10).joinToString("") { "%02x".format(it) }
    }
}
