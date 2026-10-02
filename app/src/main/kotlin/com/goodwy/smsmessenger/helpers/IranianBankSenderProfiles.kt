package com.goodwy.smsmessenger.helpers

/** Exact sender profiles observed in real Iranian bank SMS samples. */
object IranianBankSenderProfiles {
    private val normalizedSenders = mapOf(
        "+989820004747" to IranianBankRegistry.BankId.RESALAT,
        "98500014747" to IranianBankRegistry.BankId.RESALAT,
        "RESALATBANK" to IranianBankRegistry.BankId.RESALAT,
        "+989820003501" to IranianBankRegistry.BankId.SHAHR,
        "+9820003501" to IranianBankRegistry.BankId.SHAHR,
        "+9820003502" to IranianBankRegistry.BankId.SHAHR,
        "MASKANBANK" to IranianBankRegistry.BankId.MASKAN,
        "BANKMASKAN" to IranianBankRegistry.BankId.MASKAN,
        "+9830009417" to IranianBankRegistry.BankId.MELLI,
        "+98100041415001" to IranianBankRegistry.BankId.MELLI,
        "+98700717" to IranianBankRegistry.BankId.MELLI,
        "BMIR" to IranianBankRegistry.BankId.MELLI,
        "BMI.IR" to IranianBankRegistry.BankId.MELLI,
        "BANKMELLI" to IranianBankRegistry.BankId.MELLI,
        "BANKMELLIIRAN" to IranianBankRegistry.BankId.MELLI,
        "BANK MELLI" to IranianBankRegistry.BankId.MELLI,
        "بانک ملی" to IranianBankRegistry.BankId.MELLI,
        "BANKSEPAH" to IranianBankRegistry.BankId.SEPAH,
        "SEPAHBANK" to IranianBankRegistry.BankId.SEPAH,
        "SEPAH BANK" to IranianBankRegistry.BankId.SEPAH,
        "30001557" to IranianBankRegistry.BankId.SEPAH,
        "671557" to IranianBankRegistry.BankId.SEPAH,
        "1557" to IranianBankRegistry.BankId.SEPAH,
        "بانکسپه" to IranianBankRegistry.BankId.SEPAH,
        "بانک سپه" to IranianBankRegistry.BankId.SEPAH,
        "TEJARATBANK" to IranianBankRegistry.BankId.TEJARAT,
        "BANKTEJARAT" to IranianBankRegistry.BankId.TEJARAT,
        "TEJARAT BANK" to IranianBankRegistry.BankId.TEJARAT,
        "بانک تجارت" to IranianBankRegistry.BankId.TEJARAT,
        "MELLATBANK" to IranianBankRegistry.BankId.MELLAT,
        "BANKMELLAT" to IranianBankRegistry.BankId.MELLAT,
        "MELLAT BANK" to IranianBankRegistry.BankId.MELLAT,
        "بانک ملت" to IranianBankRegistry.BankId.MELLAT,
        "TTBANK" to IranianBankRegistry.BankId.TOSEE_TAAVON,
        "TOSEE TAAVON" to IranianBankRegistry.BankId.TOSEE_TAAVON,
        "TOSEE TAVON" to IranianBankRegistry.BankId.TOSEE_TAAVON,
        "BANKTOSEE TAAVON" to IranianBankRegistry.BankId.TOSEE_TAAVON,
        "BANKTOSEETAAVON" to IranianBankRegistry.BankId.TOSEE_TAAVON,
        "بانک توسعه تعاون" to IranianBankRegistry.BankId.TOSEE_TAAVON,
        "توسعه تعاون" to IranianBankRegistry.BankId.TOSEE_TAAVON,
    )

        fun find(sender: String): IranianBankRegistry.BankInfo? {
        val normalized = normalize(sender)
        val id = normalizedSenders[normalized]
        return id?.let(IranianBankRegistry::findById)
    }

    /** Returns true when the sender itself looks like a Sepah sender identifier. */
    fun isSepahCandidate(sender: String): Boolean {
        val normalized = normalize(sender)
        return normalizedSenders[normalized] == IranianBankRegistry.BankId.SEPAH ||
            normalized.contains("SEPAH") ||
            normalized.contains("سپه") ||
            normalized == "30001557" ||
            normalized == "671557" ||
            normalized == "1557"
    }

    /** Debug-only representation used by the conversation UI logs. */
    fun debugNormalize(sender: String): String {
        val normalized = normalize(sender)
        val codePoints = sender.take(120).map { it.code.toString() }.joinToString(",")
        return "raw=${sender.take(120)} normalized=$normalized rawLength=${sender.length} normalizedLength=${normalized.length} codePoints=$codePoints"
    }

        private fun normalize(value: String): String = buildString(value.length) {
        normalizeDigits(value.trim()).uppercase().forEachIndexed { index, char ->
            if (char == '+' && index == 0) append(char)
            else if (char.isLetterOrDigit()) append(char)
        }
    }

    private fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { char ->
            append(when (char) {
                in '۰'..'۹' -> ('0'.code + (char - '۰')).toChar()
                in '٠'..'٩' -> ('0'.code + (char - '٠')).toChar()
                else -> char
            })
        }
    }
}
