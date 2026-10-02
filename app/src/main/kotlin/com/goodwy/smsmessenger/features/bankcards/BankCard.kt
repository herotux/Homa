package com.goodwy.smsmessenger.features.bankcards

data class BankCard(
    val id: Long,
    val bankId: String,
    val cardNumber: String,
    val holderName: String,
    val iban: String,
    val createdAt: Long,
    val updatedAt: Long,
    val visual: BankVisual?
)

data class BankVisual(
    val id: String,
    val persianName: String,
    val englishName: String,
    val logoResourceName: String?,
    val color: Int
)
