package com.fidelpdf.app.engine

/** Corrections prudentes des confusions classiques de l'OCR (O/0, l/1…) dans les nombres et dates. */
object TextCleanup {

    private fun isSeparator(c: Char) = c == '.' || c == ',' || c == '/' || c == '-' || c == ':' || c == '%' || c == ' '

    fun fixWord(w: String): String {
        if (w.length < 2) return w
        var digits = 0
        var confusable = 0
        for (c in w) {
            when {
                c.isDigit() -> digits++
                c == 'O' || c == 'o' || c == 'l' || c == 'I' || c == '|' || c == 'S' -> confusable++
                isSeparator(c) -> {}
                else -> return w
            }
        }
        if (digits < 2 || confusable == 0 || confusable > digits) return w
        val sb = StringBuilder(w.length)
        for (c in w) {
            sb.append(
                when (c) {
                    'O', 'o' -> '0'
                    'l', 'I', '|' -> '1'
                    'S' -> '5'
                    else -> c
                }
            )
        }
        return sb.toString()
    }
}
