package se.zenmode.core

import java.security.SecureRandom
import java.util.Random

/**
 * SMS-protokollet för partner-upplåsning.
 *
 * 1. Låst mobil skickar:   "ZENMODE-REQ 123456: Anna ber om att låsa upp ... Svara med koden 123456 ..."
 * 2. Partnern godkänner antingen via appen (skickar "ZENMODE-OK 123456")
 *    eller genom att bara svara med koden "123456" från sin vanliga SMS-app.
 * 3. Neka via appen skickar "ZENMODE-NO 123456".
 */
object UnlockSms {
    const val REQ_PREFIX = "ZENMODE-REQ"
    const val OK_PREFIX = "ZENMODE-OK"
    const val NO_PREFIX = "ZENMODE-NO"

    private val requestRegex = Regex("""^\s*ZENMODE-REQ (\d{6})""")
    private val denyRegex = Regex("""^\s*ZENMODE-NO (\d{6})""")

    fun request(ownerName: String, code: String, minutes: Int): String {
        val who = ownerName.ifBlank { "Din vän" }
        return "$REQ_PREFIX $code: $who ber om att låsa upp sin mobil från zen-läget i $minutes min. " +
            "Svara med koden $code för att godkänna."
    }

    fun approval(code: String): String = "$OK_PREFIX $code"

    fun denial(code: String): String = "$NO_PREFIX $code"

    /** Om [body] är en upplåsningsbegäran: returnerar koden. */
    fun parseRequestCode(body: String): String? = requestRegex.find(body)?.groupValues?.get(1)

    fun isRequest(body: String): Boolean = requestRegex.containsMatchIn(body)

    fun isDenialOf(body: String, code: String): Boolean = denyRegex.find(body)?.groupValues?.get(1) == code

    /** Godkännande = svaret innehåller exakt koden (inte som del av ett längre tal) och är inte en begäran/nekande. */
    fun isApprovalOf(body: String, code: String): Boolean {
        if (isRequest(body) || denyRegex.containsMatchIn(body)) return false
        return Regex("""(?<!\d)${Regex.escape(code)}(?!\d)""").containsMatchIn(body)
    }

    fun generateCode(random: Random = SecureRandom()): String = "%06d".format(random.nextInt(1_000_000))
}
