package se.zenmode.core

/**
 * Enkel och robust jämförelse av telefonnummer som klarar olika format,
 * t.ex. "+46 70-123 45 67" och "070 123 45 67".
 */
object PhoneMatch {
    private const val SIGNIFICANT_DIGITS = 9

    fun digits(number: String): String = number.filter { it.isDigit() }

    fun same(a: String, b: String): Boolean {
        val da = digits(a)
        val db = digits(b)
        if (da.isEmpty() || db.isEmpty()) return false
        // Korta nummer (t.ex. kortnummer) måste matcha exakt.
        if (da.length < 6 || db.length < 6) return da == db
        val n = minOf(SIGNIFICANT_DIGITS, da.length, db.length)
        return da.takeLast(n) == db.takeLast(n)
    }
}
