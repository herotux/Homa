package com.goodwy.smsmessenger.features.bankcards

import com.goodwy.smsmessenger.helpers.IranianBankRegistry

data class DetectedFinancialNumber(
    val value: String,
    val bank: IranianBankRegistry.BankInfo?,
    val isCard: Boolean
)

object BankFinancialDetector {
    private val cardPattern = Regex(
        """(?<![\p{L}\p{N}])(?:[0-9۰-۹٠-٩][\s-]?){15}[0-9۰-۹٠-٩](?![\p{L}\p{N}])"""
    )
    private val ibanPattern = Regex(
        """(?<![\p{L}\p{N}])IR[\s-]?[0-9۰-۹٠-٩]{2}(?:[\s-]?[0-9۰-۹٠-٩]){22}(?![\p{L}\p{N}])""",
        RegexOption.IGNORE_CASE
    )

    fun findAll(text: String): List<DetectedFinancialNumber> {
        val results = mutableListOf<DetectedFinancialNumber>()
        val occupied = mutableListOf<IntRange>()

        ibanPattern.findAll(text).forEach { match ->
            val normalized = normalizeIban(match.value)
            if (IranianBankRegistry.isValidIban(normalized)) {
                results += DetectedFinancialNumber(normalized, IranianBankRegistry.findByIban(normalized), false)
                occupied += match.range
            }
        }

        cardPattern.findAll(text).forEach { match ->
            if (occupied.none { it.first <= match.range.last && match.range.first <= it.last }) {
                val normalized = normalizeDigits(match.value).filter(Char::isDigit)
                val bank = IranianBankRegistry.findByCard(normalized)
                if (bank != null && IranianBankRegistry.isValidCardNumber(normalized)) {
                    results += DetectedFinancialNumber(normalized, bank, true)
                }
            }
        }

        return results
    }

    private fun normalizeIban(value: String): String =
        normalizeDigits(value).replace(" ", "").replace("-", "").uppercase()

    private fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { char ->
            append(
                when (char) {
                    in '۰'..'۹' -> ('0'.code + char.code - '۰'.code).toChar()
                    in '٠'..'٩' -> ('0'.code + char.code - '٠'.code).toChar()
                    else -> char
                }
            )
        }
    }
}
