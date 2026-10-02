package com.goodwy.smsmessenger.features.bankcards

import android.content.Context
import android.graphics.Color
import com.goodwy.smsmessenger.helpers.IranianBankRegistry
import java.util.Locale

/** Isolated persistence/domain boundary for the standalone bank-card UI. */
class BankCardsRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("bank_cards", Context.MODE_PRIVATE)

    fun getCards(): List<BankCard> = synchronized(prefs) {
        (0 until prefs.getInt(KEY_COUNT, 0)).mapNotNull { i ->
            val card = prefs.getString("card_$i", null)?.let(::normalizeCard) ?: return@mapNotNull null
            if (card.length != 16) return@mapNotNull null
            BankCard(
                prefs.getLong("id_$i", i.toLong() + 1),
                prefs.getString("bank_$i", "") ?: "",
                card,
                prefs.getString("holder_$i", "") ?: "",
                normalizeIban(prefs.getString("iban_$i", "") ?: ""),
                prefs.getLong("created_$i", 0L),
                prefs.getLong("updated_$i", 0L),
                detect(card)
            )
        }
    }

    fun save(existing: BankCard?, cardNumber: String, holderName: String, iban: String): Result<Unit> = runCatching {
        val card = normalizeCard(cardNumber)
        val bank = IranianBankRegistry.findByCard(card) ?: error("Unknown bank")
        check(IranianBankRegistry.isValidCardNumber(card)) { "Invalid card" }
        val normalizedIban = normalizeIban(iban)
        check(normalizedIban.isBlank() || IranianBankRegistry.isValidIban(normalizedIban)) { "Invalid IBAN" }
        val now = System.currentTimeMillis()
        val item = BankCard(
            existing?.id ?: nextId(),
            bank.id.name,
            card,
            holderName.trim(),
            normalizedIban,
            existing?.createdAt ?: now,
            now,
            detect(card)
        )
        val list = getCards().toMutableList()
        val index = existing?.let { list.indexOfFirst { c -> c.id == it.id } } ?: -1
        if (index >= 0) list[index] = item else list.add(item)
        writeAll(list)
    }

    fun delete(card: BankCard) = runCatching { writeAll(getCards().filterNot { it.id == card.id }) }
    fun reorder(cards: List<BankCard>) = runCatching { writeAll(cards) }

    fun detect(cardNumber: String): BankVisual? {
        val bank = IranianBankRegistry.findByCard(normalizeCard(cardNumber)) ?: return null
        val color = when (bank.id.name) {
            "MELLAT" -> Color.rgb(165, 30, 45)
            "MELLI" -> Color.rgb(20, 75, 135)
            "TEJARAT" -> Color.rgb(0, 112, 175)
            "SADERAT" -> Color.rgb(0, 92, 155)
            "SEPAH" -> Color.rgb(205, 160, 35)
            "PASARGAD" -> Color.rgb(32, 65, 105)
            "PARSIAN" -> Color.rgb(20, 115, 110)
            "SAMAN" -> Color.rgb(20, 110, 145)
            "SHAHR" -> Color.rgb(80, 55, 125)
            else -> Color.rgb(70, 80, 95)
        }
        return BankVisual(bank.id.name, bank.persianName, bank.englishName, bank.logoResourceName, color)
    }

    fun normalizeCard(value: String) = normalizeDigits(value).filter(Char::isDigit)
    fun formatCard(value: String) = normalizeCard(value).chunked(4).joinToString("   ")
    fun normalizeIban(value: String) = normalizeDigits(value).replace(" ", "").replace("-", "").uppercase(Locale.US)
    fun formatIban(value: String) = normalizeIban(value).chunked(4).joinToString(" ")
    fun validCard(value: String) = runCatching { IranianBankRegistry.isValidCardNumber(normalizeCard(value)) }.getOrDefault(false)
    fun validIban(value: String) = value.isBlank() || runCatching { IranianBankRegistry.isValidIban(normalizeIban(value)) }.getOrDefault(false)

    private fun nextId() = (getCards().maxOfOrNull { it.id } ?: 0L) + 1L

    private fun writeAll(cards: List<BankCard>) {
        val editor = prefs.edit().clear()
        editor.putInt(KEY_COUNT, cards.size)
        cards.forEachIndexed { i, c ->
            editor.putLong("id_$i", c.id)
            editor.putString("bank_$i", c.bankId)
            editor.putString("card_$i", c.cardNumber)
            editor.putString("holder_$i", c.holderName)
            editor.putString("iban_$i", c.iban)
            editor.putLong("created_$i", c.createdAt)
            editor.putLong("updated_$i", c.updatedAt)
        }
        check(editor.commit()) { "Card storage commit failed" }
    }

    private fun normalizeDigits(value: String) = buildString(value.length) {
        value.forEach { c ->
            append(
                when (c) {
                    in '۰'..'۹' -> ('0'.code + c.code - '۰'.code).toChar()
                    in '٠'..'٩' -> ('0'.code + c.code - '٠'.code).toChar()
                    else -> c
                }
            )
        }
    }

    companion object {
        private const val KEY_COUNT = "count"
    }
}
