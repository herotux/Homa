package com.goodwy.smsmessenger.helpers

/** Offline registry for conservative Iranian bank identification. */
object IranianBankRegistry {
    enum class BankId {
        MELLI, MELLAT, TEJARAT, SADERAT, SEPAH, REFAH, MASKAN, KESHAVARZI,
        SANAT_MADAN, POST, TOSEE_SADERAT, TOSEE_TAAVON, TOSEE, PARSIAN,
        PASARGAD, KARAFARIN, SAMAN, BLUBANK, EGHTESAD_NOVIN, SARMAYEH, SINA,
        MEHR_IRAN, SHAHR, GARDESHGARI, DEY, IRAN_ZAMIN, RESALAT, MELAL,
        KHAVAR_MIANEH, IRAN_VENEZUELA, CENTRAL_BANK,
    }

    data class BankInfo(
        val id: BankId,
        val persianName: String,
        val englishName: String,
        val ibanCode: String?,
        val cardPrefixes: List<String>,
        val legacyIbanCodes: List<String> = emptyList(),
        val aliases: List<String> = emptyList(),
        val logoResourceName: String? = null,
    )

    private val banks = listOf(
        BankInfo(BankId.MELLI, "بانک ملی ایران", "Melli Bank", "017", listOf("603799", "636214"), listOf("062"), listOf("ملی", "بانک ملی", "بانک ملی ایران"), "bank_melli"),
        BankInfo(BankId.MELLAT, "بانک ملت", "Mellat Bank", "012", listOf("610433", "991975"), aliases = listOf("ملت", "بانک ملت"), logoResourceName = "bank_mellat"),
        BankInfo(BankId.TEJARAT, "بانک تجارت", "Tejarat Bank", "018", listOf("627353", "585983"), aliases = listOf("تجارت", "بانک تجارت"), logoResourceName = "bank_tejarat"),
        BankInfo(BankId.SADERAT, "بانک صادرات ایران", "Saderat Bank", "019", listOf("603769"), aliases = listOf("صادرات", "بانک صادرات", "بانک صادرات ایران"), logoResourceName = "bank_saderat"),
        BankInfo(BankId.SEPAH, "بانک سپه", "Sepah Bank", "015", listOf("589210", "627381", "639599", "636949", "639370"), aliases = listOf("سپه", "بانک سپه", "انصار", "قوامین", "حکمت ایرانیان", "مهر اقتصاد"), logoResourceName = "bank_sepah"),
        BankInfo(BankId.REFAH, "بانک رفاه کارگران", "Refah Bank", "013", listOf("589463"), aliases = listOf("رفاه", "رفاه کارگران", "بانک رفاه"), logoResourceName = "bank_refah"),
        BankInfo(BankId.MASKAN, "بانک مسکن", "Maskan Bank", "014", listOf("628023"), aliases = listOf("مسکن", "بانک مسکن"), logoResourceName = "bank_maskan"),
        BankInfo(BankId.KESHAVARZI, "بانک کشاورزی", "Keshavarzi Bank", "016", listOf("603770", "639217"), aliases = listOf("کشاورزی", "بانک کشاورزی"), logoResourceName = "bank_keshavarzi"),
        BankInfo(BankId.SANAT_MADAN, "بانک صنعت و معدن", "Sanat-o-Madan Bank", "011", listOf("627961"), aliases = listOf("صنعت و معدن", "بانک صنعت و معدن"), logoResourceName = "bank_sanat_madan"),
        BankInfo(BankId.POST, "پست بانک ایران", "Post Bank", "021", listOf("627760"), aliases = listOf("پست بانک", "پست بانک ایران"), logoResourceName = "bank_post"),
        BankInfo(BankId.TOSEE_SADERAT, "بانک توسعه صادرات ایران", "Export Development Bank", "020", listOf("627648", "207177"), aliases = listOf("توسعه صادرات", "بانک توسعه صادرات"), logoResourceName = "bank_tosee_saderat"),
        BankInfo(BankId.TOSEE_TAAVON, "بانک توسعه تعاون", "Tosee Taavon Bank", "022", listOf("502908"), aliases = listOf("توسعه تعاون", "بانک توسعه تعاون"), logoResourceName = "bank_tosee_taavon"),
        BankInfo(BankId.TOSEE, "موسسه اعتباری توسعه", "Tosee Credit Institution", "051", listOf("628157"), aliases = listOf("موسسه اعتباری توسعه")),
        BankInfo(BankId.PARSIAN, "بانک پارسیان", "Parsian Bank", "054", listOf("622106", "639194", "627884"), aliases = listOf("پارسیان", "بانک پارسیان"), logoResourceName = "bank_parsian"),
        BankInfo(BankId.PASARGAD, "بانک پاسارگاد", "Pasargad Bank", "057", listOf("502229", "639347"), aliases = listOf("پاسارگاد", "بانک پاسارگاد"), logoResourceName = "bank_pasargad"),
        BankInfo(BankId.KARAFARIN, "بانک کارآفرین", "Karafarin Bank", "053", listOf("627488", "502910"), aliases = listOf("کارآفرین", "بانک کارآفرین"), logoResourceName = "bank_karafarin"),
        BankInfo(BankId.SAMAN, "بانک سامان", "Saman Bank", "056", listOf("621986"), aliases = listOf("سامان", "بانک سامان"), logoResourceName = "bank_saman"),
        BankInfo(BankId.BLUBANK, "بلوبانک", "BluBank", "056", listOf("62198618", "62198619"), aliases = listOf("بلو", "بلوبانک")),
        BankInfo(BankId.EGHTESAD_NOVIN, "بانک اقتصاد نوین", "Eghtesad Novin Bank", "055", listOf("627412"), aliases = listOf("اقتصاد نوین", "بانک اقتصاد نوین"), logoResourceName = "bank_eghtesad_novin"),
        BankInfo(BankId.SARMAYEH, "بانک سرمایه", "Sarmayeh Bank", "058", listOf("639607"), aliases = listOf("سرمایه", "بانک سرمایه"), logoResourceName = "bank_sarmayeh"),
        BankInfo(BankId.SINA, "بانک سینا", "Sina Bank", "059", listOf("639346"), aliases = listOf("سینا", "بانک سینا"), logoResourceName = "bank_sina"),
        BankInfo(BankId.MEHR_IRAN, "بانک قرض الحسنه مهر ایران", "Mehr Iran Bank", "060", listOf("606373"), aliases = listOf("مهر ایران", "بانک مهر ایران", "بانک قرض الحسنه مهر ایران", "مهر"), logoResourceName = "bank_mehr_iran"),
        BankInfo(BankId.SHAHR, "بانک شهر", "Shahr Bank", "061", listOf("502806", "504706"), aliases = listOf("شهر", "بانک شهر"), logoResourceName = "bank_shahr"),
        BankInfo(BankId.GARDESHGARI, "بانک گردشگری", "Gardeshgari Bank", "064", listOf("505416", "505426"), aliases = listOf("گردشگری", "بانک گردشگری"), logoResourceName = "bank_gardeshgari"),
        BankInfo(BankId.DEY, "بانک دی", "Dey Bank", "066", listOf("502938"), aliases = listOf("دی", "بانک دی"), logoResourceName = "bank_dey"),
        BankInfo(BankId.IRAN_ZAMIN, "بانک ایران زمین", "Iran Zamin Bank", "069", listOf("505785"), aliases = listOf("ایران زمین", "بانک ایران زمین"), logoResourceName = "bank_iran_zamin"),
        BankInfo(BankId.RESALAT, "بانک قرض الحسنه رسالت", "Resalat Bank", "070", listOf("504172"), aliases = listOf("رسالت", "بانک رسالت", "بانک قرض الحسنه رسالت"), logoResourceName = "bank_resalat"),
        BankInfo(BankId.MELAL, "موسسه اعتباری ملل", "Melal Credit Institution", "075", listOf("606256"), aliases = listOf("ملل", "موسسه اعتباری ملل"), logoResourceName = "bank_melal"),
        BankInfo(BankId.KHAVAR_MIANEH, "بانک خاورمیانه", "Middle East Bank", "078", listOf("585947"), aliases = listOf("خاورمیانه", "بانک خاورمیانه"), logoResourceName = "bank_khavar_mianeh"),
        BankInfo(BankId.IRAN_VENEZUELA, "بانک ایران و ونزوئلا", "Iran-Venezuela Bank", "095", emptyList(), aliases = listOf("ایران و ونزوئلا", "بانک ایران و ونزوئلا"), logoResourceName = "bank_iran_venezuela"),
        BankInfo(BankId.CENTRAL_BANK, "بانک مرکزی جمهوری اسلامی ایران", "Central Bank of Iran", "010", listOf("636795"), aliases = listOf("بانک مرکزی")),
    )

    private val byId = banks.associateBy { it.id }

    // Verified sender identifiers from the supplied real SMS samples plus the existing conservative registry.
    private val smsSenders = mapOf(
        "1000700" to BankId.TOSEE_TAAVON, "100070007" to BankId.TOSEE_TAAVON,
        "1000900" to BankId.PASARGAD, "50009000" to BankId.PASARGAD,
        "20000" to BankId.SAMAN, "200020" to BankId.SEPAH, "200021" to BankId.SEPAH, "200022" to BankId.SEPAH,
        "200033" to BankId.MELLAT, "200030" to BankId.MELLAT, "2000333" to BankId.MELLAT,
        "200038" to BankId.SINA, "200050" to BankId.EGHTESAD_NOVIN, "200060" to BankId.SADERAT,
        "200070" to BankId.TEJARAT, "20007010" to BankId.TEJARAT, "2000911" to BankId.KESHAVARZI, "200093" to BankId.KESHAVARZI,
        "300054" to BankId.PARSIAN, "50001099" to BankId.PARSIAN, "300055" to BankId.PARSIAN, "30007" to BankId.PARSIAN,
        "300066" to BankId.REFAH, "300044" to BankId.REFAH, "100088" to BankId.SARMAYEH, "300014" to BankId.MASKAN, "300017" to BankId.MELLI,
        "BANKMELLAT" to BankId.MELLAT, "MELLATBANK" to BankId.MELLAT,
        "BANKMELLI" to BankId.MELLI, "BANKMELLIIRAN" to BankId.MELLI,
        "BANKSAMAN" to BankId.SAMAN, "BANKTEJARAT" to BankId.TEJARAT, "TEJARATBANK" to BankId.TEJARAT,
        "BANKSADERAT" to BankId.SADERAT, "BANKPASARGAD" to BankId.PASARGAD, "BANKSEPAH" to BankId.SEPAH,
        "TTBANK" to BankId.TOSEE_TAAVON,
        // Exact senders observed in the supplied samples.
        "+98700717" to BankId.MELLI,
        "+989999987641" to BankId.BLUBANK,
        "+983000852809" to BankId.MEHR_IRAN,
        "+983000852803" to BankId.MEHR_IRAN,
        "+983000852801" to BankId.MEHR_IRAN,
        "بانک سپه" to BankId.SEPAH,
        "بانک ملی" to BankId.MELLI,
        "BANKMELLI" to BankId.MELLI,
        "BANKMELLIIRAN" to BankId.MELLI,
        "BANKMELLI" to BankId.MELLI,
        "TEJARATBANK" to BankId.TEJARAT,
        "BANKTEJARAT" to BankId.TEJARAT,
        "TTBANK" to BankId.TOSEE_TAAVON,
    )

    private val knownNonBankSenders = setOf("V.REFAH", "V.MASKAN")

    fun allBanks(): List<BankInfo> = banks
    fun findById(id: BankId): BankInfo? = byId[id]

    fun findByCard(cardNumber: String): BankInfo? {
        val normalized = normalizeDigits(cardNumber).filter(Char::isDigit)
        if (normalized.length < 6) return null
        return banks.asSequence().flatMap { bank -> bank.cardPrefixes.asSequence().map { it to bank } }
            .filter { (prefix, _) -> normalized.startsWith(prefix) }
            .maxByOrNull { (prefix, _) -> prefix.length }?.second
    }

    fun isValidCardNumber(cardNumber: String): Boolean {
        val digits = normalizeDigits(cardNumber).filter(Char::isDigit)
        if (digits.length != 16 || digits.all { it == '0' }) return false
        var sum = 0
        for (i in digits.indices) {
            var value = digits[i] - '0'
            if (i % 2 == 0) { value *= 2; if (value > 9) value -= 9 }
            sum += value
        }
        return sum % 10 == 0
    }

    fun findByIban(iban: String): BankInfo? {
        val normalized = normalizeIban(iban) ?: return null
        val code = normalized.substring(4, 7)
        return banks.firstOrNull { it.ibanCode == code || code in it.legacyIbanCodes }
    }

    fun isValidIban(iban: String): Boolean {
        val normalized = normalizeIban(iban) ?: return false
        val rearranged = normalized.substring(4) + normalized.substring(0, 4)
        var remainder = 0
        for (char in rearranged) {
            val value = if (char.isDigit()) char - '0' else char - 'A' + 10
            remainder = if (char.isDigit()) (remainder * 10 + value) % 97 else (remainder * 100 + value) % 97
        }
        return remainder == 1
    }

    fun findBySmsSender(sender: String): BankInfo? {
        val normalized = normalizeSender(sender)
        if (normalized.isEmpty() || normalized in knownNonBankSenders) return null
        return smsSenders[normalized]?.let { byId[it] }
    }

    fun isKnownNonBankSender(sender: String): Boolean = normalizeSender(sender) in knownNonBankSenders

    private fun normalizeIban(value: String): String? {
        val normalized = normalizeDigits(value).uppercase().replace(" ", "").replace("-", "")
        return normalized.takeIf { it.matches(Regex("IR\\d{24}")) }
    }

    private fun normalizeSender(value: String): String = normalizeDigits(value.trim()).uppercase().replace(" ", "").replace("-", "")

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
