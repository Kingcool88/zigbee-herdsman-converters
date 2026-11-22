package se.zenmode.core

import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnlockSmsTest {
    @Test
    fun requestRoundTrip() {
        val msg = UnlockSms.request("Anna", "012345", 5)
        assertEquals("012345", UnlockSms.parseRequestCode(msg))
        assertTrue(msg.contains("Anna"))
    }

    @Test
    fun nonRequestIsNull() {
        assertNull(UnlockSms.parseRequestCode("Hej! 012345"))
    }

    @Test
    fun approvalViaAppOrPlainReply() {
        assertTrue(UnlockSms.isApprovalOf(UnlockSms.approval("012345"), "012345"))
        assertTrue(UnlockSms.isApprovalOf("012345", "012345"))
        assertTrue(UnlockSms.isApprovalOf("Ok, koden är 012345 :)", "012345"))
    }

    @Test
    fun approvalRejectsWrongOrEmbeddedCode() {
        assertFalse(UnlockSms.isApprovalOf("012346", "012345"))
        assertFalse(UnlockSms.isApprovalOf("90123456", "012345"))
        assertFalse(UnlockSms.isApprovalOf(UnlockSms.request("x", "012345", 5), "012345"))
        assertFalse(UnlockSms.isApprovalOf(UnlockSms.denial("012345"), "012345"))
    }

    @Test
    fun denial() {
        assertTrue(UnlockSms.isDenialOf(UnlockSms.denial("012345"), "012345"))
        assertFalse(UnlockSms.isDenialOf(UnlockSms.denial("012345"), "999999"))
    }

    @Test
    fun codeIsSixDigits() {
        val r = Random(1)
        repeat(100) { assertTrue(Regex("""\d{6}""").matches(UnlockSms.generateCode(r))) }
    }
}
