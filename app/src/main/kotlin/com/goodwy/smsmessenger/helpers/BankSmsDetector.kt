package com.goodwy.smsmessenger.helpers

/** Conservative offline detector for Iranian bank SMS messages. */
object BankSmsDetector {
    enum class Confidence { HIGH, MEDIUM }
    enum class Reason { VERIFIED_SENDER, IBAN, CARD_NUMBER, TRANSACTION_CONTEXT, EXPLICIT_BANK_NAME }

    data class Detection(
        val bank: IranianBankRegistry.BankInfo,
        val confidence: Confidence,
        val reason: Reason,
        val score: Int,
        val reasons: List<Reason>,
    )

    /**
     * Bank identity is intentionally derived only from the SMS sender.
     * Body text, card numbers, IBANs and bank names must never identify a bank.
     */
    fun detect(sender: String, body: String): Detection? {
        IranianBankSenderProfiles.find(sender)?.let {
            return Detection(
                bank = it,
                confidence = Confidence.HIGH,
                reason = Reason.VERIFIED_SENDER,
                score = 100,
                reasons = listOf(Reason.VERIFIED_SENDER),
            )
        }

        IranianBankRegistry.findBySmsSender(sender)?.let {
            return Detection(
                bank = it,
                confidence = Confidence.HIGH,
                reason = Reason.VERIFIED_SENDER,
                score = 100,
                reasons = listOf(Reason.VERIFIED_SENDER),
            )
        }

        // Keep the body parameter for API compatibility, but deliberately do not inspect it.
        return null
    }
}
