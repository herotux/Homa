package com.goodwy.smsmessenger.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IranianBankRegistryTest {
    @Test fun cardPrefixLookupUsesLongestPrefix() {
        assertEquals(IranianBankRegistry.BankId.BLUBANK, IranianBankRegistry.findByCard("6219861900000000")?.id)
        assertEquals(IranianBankRegistry.BankId.BLUBANK, IranianBankRegistry.findByCard("6219861800000000")?.id)
        assertEquals(IranianBankRegistry.BankId.SAMAN, IranianBankRegistry.findByCard("6219860099999999")?.id)
    }
    @Test fun cardLookupNormalizesPersianDigits() = assertEquals(IranianBankRegistry.BankId.MELLI, IranianBankRegistry.findByCard("۶۰۳۷۹۹۰۰۰۰۰۰۰۰۰۶")?.id)
    @Test fun cardLuhnValidationWorks() {
        assertTrue(IranianBankRegistry.isValidCardNumber("6037990000000006"))
        assertFalse(IranianBankRegistry.isValidCardNumber("6037990000000007"))
    }
    @Test fun ibanLookupAndValidationUseBankCode() {
        val iban = "IR700120000000000000000000"
        assertEquals(IranianBankRegistry.BankId.MELLAT, IranianBankRegistry.findByIban(iban)?.id)
        assertTrue(IranianBankRegistry.isValidIban(iban))
        assertNull(IranianBankRegistry.findByIban("123"))
    }
    @Test fun knownNonBankSendersAreNotBanks() {
        assertTrue(IranianBankRegistry.isKnownNonBankSender("v.refah"))
        assertTrue(IranianBankRegistry.isKnownNonBankSender("V.MASKAN"))
        assertNull(IranianBankRegistry.findBySmsSender("V.REFAH"))
    }
    @Test fun verifiedSmsSenderLookupWorks() {
        assertEquals(IranianBankRegistry.BankId.MELLAT, IranianBankRegistry.findBySmsSender("200033")?.id)
        assertEquals(IranianBankRegistry.BankId.MELLAT, IranianBankRegistry.findBySmsSender("BankMellat")?.id)
        assertEquals(IranianBankRegistry.BankId.MELLI, IranianBankRegistry.findBySmsSender("۳۰۰۰۱۷")?.id)
        assertEquals(IranianBankRegistry.BankId.SAMAN, IranianBankRegistry.findBySmsSender("20000")?.id)
    }
    @Test fun realSenderProfilesResolveToCorrectBanks() {
        assertEquals(IranianBankRegistry.BankId.RESALAT, IranianBankSenderProfiles.find("+989820004747")?.id)
        assertEquals(IranianBankRegistry.BankId.RESALAT, IranianBankSenderProfiles.find("ResalatBank")?.id)
        assertEquals(IranianBankRegistry.BankId.SHAHR, IranianBankSenderProfiles.find("+989820003501")?.id)
        assertEquals(IranianBankRegistry.BankId.SHAHR, IranianBankSenderProfiles.find("+9820003502")?.id)
        assertEquals(IranianBankRegistry.BankId.MELLI, IranianBankSenderProfiles.find("+9830009417")?.id)
        assertEquals(IranianBankRegistry.BankId.MELLI, IranianBankSenderProfiles.find("+98100041415001")?.id)
        assertEquals(IranianBankRegistry.BankId.MELLI, IranianBankSenderProfiles.find("+98700717")?.id)
        assertEquals(IranianBankRegistry.BankId.MELLI, IranianBankSenderProfiles.find("Bank Melli")?.id)
        assertEquals(IranianBankRegistry.BankId.MELLI, IranianBankSenderProfiles.find("Bank-Melli")?.id)
        assertEquals(IranianBankRegistry.BankId.MASKAN, IranianBankSenderProfiles.find("Maskan Bank")?.id)
        assertEquals(IranianBankRegistry.BankId.SEPAH, IranianBankSenderProfiles.find("30001557")?.id)
        assertEquals(IranianBankRegistry.BankId.SEPAH, IranianBankSenderProfiles.find("671557")?.id)
        assertEquals(IranianBankRegistry.BankId.SEPAH, IranianBankSenderProfiles.find("BankSepah")?.id)
        assertEquals(IranianBankRegistry.BankId.SEPAH, IranianBankSenderProfiles.find("SEPAH BANK")?.id)
        assertEquals(IranianBankRegistry.BankId.SEPAH, IranianBankSenderProfiles.find("SEPAH-BANK")?.id)
        assertEquals(IranianBankRegistry.BankId.TEJARAT, IranianBankSenderProfiles.find("TejaratBank")?.id)
        assertEquals(IranianBankRegistry.BankId.TEJARAT, IranianBankSenderProfiles.find("Tejarat Bank")?.id)
    }
    @Test fun banksWithBundledLogosExposeTheirDrawableName() {
        assertEquals("bank_melli", IranianBankRegistry.findById(IranianBankRegistry.BankId.MELLI)?.logoResourceName)
        assertEquals("bank_mehr_iran", IranianBankRegistry.findById(IranianBankRegistry.BankId.MEHR_IRAN)?.logoResourceName)
        assertEquals("bank_sepah", IranianBankRegistry.findById(IranianBankRegistry.BankId.SEPAH)?.logoResourceName)
        assertNotNull(IranianBankRegistry.findById(IranianBankRegistry.BankId.MELLAT)?.logoResourceName)
    }

    @Test fun bankSmsDetectorUsesVerifiedSenderOnly() {
        val senders = listOf(
            "200033" to IranianBankRegistry.BankId.MELLAT,
            "BankMellat" to IranianBankRegistry.BankId.MELLAT,
            "۳۰۰۰۱۷" to IranianBankRegistry.BankId.MELLI,
            "+98700717" to IranianBankRegistry.BankId.MELLI,
            "SEPAH BANK" to IranianBankRegistry.BankId.SEPAH,
            "30001557" to IranianBankRegistry.BankId.SEPAH,
            "TejaratBank" to IranianBankRegistry.BankId.TEJARAT,
            "Tejarat Bank" to IranianBankRegistry.BankId.TEJARAT,
            "Maskan Bank" to IranianBankRegistry.BankId.MASKAN,
            "+989820004747" to IranianBankRegistry.BankId.RESALAT,
            "+989820003501" to IranianBankRegistry.BankId.SHAHR
        )
        senders.forEach { (sender, expected) ->
            val detection = BankSmsDetector.detect(sender, "هر متن دلخواه و نامرتبط")
            assertEquals(expected, detection?.bank?.id)
            assertEquals(BankSmsDetector.Confidence.HIGH, detection?.confidence)
            assertEquals(BankSmsDetector.Reason.VERIFIED_SENDER, detection?.reason)
        }
    }

    @Test fun bankSmsDetectorRejectsUnknownSenderEvenWhenBodyNamesBank() {
        val bodies = listOf(
            "بانک سپه\nپرداخت گروهي\nمبلغ:6,000,000",
            "بانك ملي ايران\nانتقالي:400,000-\nمانده:28,089",
            "بانک ملت\nمبلغ:6000000\nمانده:6035959",
            "بانک شهر\nمشتری گرامی\nسامانه های بانک شهر"
        )
        bodies.forEach { body -> assertNull(BankSmsDetector.detect("unknown", body)) }
    }

    @Test fun bankSmsDetectorRejectsUnknownSenderEvenWhenItLooksLikeBankSender() {
        assertNull(BankSmsDetector.detect("IR-MELLAT", "تراکنش انجام شد"))
        assertNull(BankSmsDetector.detect("1000", "بانک ملت مبلغ 6000000 واریز شد"))
    }

    @Test fun bankSmsDetectorRejectsCardOnlyAndIbanOnlyBodyWithoutVerifiedSender() {
        assertNull(BankSmsDetector.detect("1000", "کارت ۶۰۳۷۹۹۰۰۰۰۰۰۰۰۰۷"))
        assertNull(BankSmsDetector.detect("1000", "شماره شبا IR700120000000000000000000 برای واریز وجه"))
        assertNull(BankSmsDetector.detect("1000", "بانک ملی\nمبلغ از کارت ۶۰۳۷-۹۹۰۰-۰۰۰۰-۰۰۰۶ کسر شد"))
    }

    @Test fun bankSmsDetectorRejectsPromotionalAndMerchantBodiesWithoutVerifiedSender() {
        val promotional = "هموطن گرامی برای مشارکت در پویش بنای مهربانی و حمایت از جامعه هدف سازمان بهزیستی، بانک مهر ایران"
        val merchant = "مانده حساب شما مبلغ 10.380.000 ریال بوده و شماره کارت فروشگاه جهت واریز خدمتتون ارسال میشود 5892101454789153"
        assertNull(BankSmsDetector.detect("1000", promotional))
        assertNull(BankSmsDetector.detect("1000", merchant))
    }
}
