package com.example.sms

/**
 * Parses Indian bank / UPI transaction SMS into amount, bank, and debit/credit.
 *
 * Handles common formats from SBI, HDFC, ICICI, Axis, Kotak, Yes, IDFC, Paytm, PhonePe, GPay, etc.
 */
object SmsTransactionParser {

    data class ParsedTransaction(
        val amount: Double,
        val bankName: String,
        val isExpense: Boolean,
        val merchantOrInfo: String = "",
        val confidence: Float = 1f
    )

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
        "GPAY" to "Google Pay",
        "GOOGL" to "Google Pay",
        "AMAZON" to "Amazon Pay",
        "MOBIKW" to "MobiKwik",
        "FREECH" to "Freecharge",
        "BHIM" to "BHIM UPI",
        "NACH" to "NACH",
        "UPI" to "UPI"
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

    /** Amount with ₹ / Rs / INR — prefer the transaction amount, not balance. */
    private val amountPatterns = listOf(
        // Rs.500.00 / Rs 1,500.00 / INR 250 / ₹799.00
        Regex("""(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""₹\s*([\d,]+(?:\.\d{1,2})?)"""),
        // 500.00 Rs / 1,200 INR
        Regex("""([\d,]+(?:\.\d{1,2})?)\s*(?:Rs\.?|INR|₹)""", RegexOption.IGNORE_CASE)
    )

    private val debitKeywords = listOf(
        "debited", "debit", "spent", "paid", "purchase", "withdrawn", "withdrawal",
        "sent", "transferred to", "payment of", "charged", "pos ", "txn of"
    )

    private val creditKeywords = listOf(
        "credited", "credit", "received", "deposited", "refund", "cashback",
        "salary", "neft cr", "imps cr", "upi cr"
    )

    private val transactionHints = listOf(
        "debited", "credited", "spent", "withdrawn", "upi", "neft", "imps",
        "rtgs", "a/c", "acct", "account", "avl bal", "available bal", "txn",
        "transaction", "vpa", "ref no", "ref:"
    )

    fun looksLikeTransactionSms(body: String, sender: String = ""): Boolean {
        val text = body.lowercase()
        val hasMoney = text.contains("rs") || text.contains("inr") || text.contains("₹") ||
            text.contains("rupee")
        if (!hasMoney) return false
        val hasHint = transactionHints.any { text.contains(it) }
        val knownSender = resolveBankFromSender(sender) != null
        return hasHint || knownSender
    }

    fun parse(body: String, sender: String = ""): ParsedTransaction? {
        if (body.isBlank()) return null
        if (!looksLikeTransactionSms(body, sender)) return null

        val amount = extractTransactionAmount(body) ?: return null
        if (amount <= 0.0 || amount > 50_000_000.0) return null

        val isExpense = resolveIsExpense(body) ?: return null
        val bankName = resolveBankName(body, sender)
        val info = extractMerchantOrInfo(body)

        return ParsedTransaction(
            amount = amount,
            bankName = bankName,
            isExpense = isExpense,
            merchantOrInfo = info,
            confidence = if (bankName != "Bank") 1f else 0.75f
        )
    }

    /**
     * Prefer amount near debit/credit wording; skip typical balance phrases.
     */
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

        // Drop amounts that sit right after balance keywords (available bal, avl bal, bal:)
        val balanceHint = Regex(
            """(?:avl(?:ailable)?\s*bal(?:ance)?|available\s*balance|bal(?:ance)?\s*[:=]|closing\s*bal)""",
            RegexOption.IGNORE_CASE
        )
        val nonBalance = candidates.filter { (index, _) ->
            val windowStart = (index - 28).coerceAtLeast(0)
            val prefix = normalized.substring(windowStart, index)
            !balanceHint.containsMatchIn(prefix)
        }

        val pool = nonBalance.ifEmpty { candidates }

        // Prefer amount closest to a debit/credit verb
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
        val text = body.lowercase()
        val debitScore = debitKeywords.count { text.contains(it) }
        val creditScore = creditKeywords.count { text.contains(it) }
        return when {
            debitScore > creditScore -> true
            creditScore > debitScore -> false
            // "debited from" vs "credited to" already covered; ambiguous → skip
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
        val upper = sender.uppercase().replace("-", "").replace(" ", "")
        // Sender often like VM-HDFCBK, AD-SBIINB, AX-ICICIB
        for ((key, name) in senderBankMap) {
            if (upper.contains(key)) return name
        }
        return null
    }

    fun extractMerchantOrInfo(body: String): String {
        val patterns = listOf(
            Regex("""(?:to|at|towards|info[:\s]+|VPA\s+)([A-Za-z0-9@._\-\s]{3,40})""", RegexOption.IGNORE_CASE),
            Regex("""UPI[/\-\s]+([A-Za-z0-9@._\-]{3,40})""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val m = p.find(body) ?: continue
            val raw = m.groupValues.getOrNull(1)?.trim().orEmpty()
            if (raw.isBlank()) continue
            // Avoid capturing balance fragments
            if (raw.lowercase().contains("bal") || raw.lowercase().startsWith("rs")) continue
            return raw.take(48).trim()
        }
        return ""
    }

    /** Stable fingerprint so the same SMS is never prompted twice. */
    fun smsHash(body: String, sender: String, timestampMillis: Long = 0L): String {
        val core = "${sender.trim().uppercase()}|${body.trim()}|$timestampMillis"
        return core.hashCode().toUInt().toString(16) + "_" + body.length
    }
}
