package com.kidstracker.data

import android.content.Context

/** Poche impostazioni, senza bisogno di un database. */
class Preferenze(context: Context) {

    private val prefs = context.getSharedPreferences(NOME, Context.MODE_PRIVATE)

    var promemoriaAttivo: Boolean
        get() = prefs.getBoolean(CHIAVE_PROMEMORIA, true)
        set(valore) = prefs.edit().putBoolean(CHIAVE_PROMEMORIA, valore).apply()

    /** Minuti dalla mezzanotte. Default: 17:30, quando si torna da scuola. */
    var oraPromemoria: Int
        get() = prefs.getInt(CHIAVE_ORA, 17 * 60 + 30)
        set(valore) = prefs.edit().putInt(CHIAVE_ORA, valore.coerceIn(0, 24 * 60 - 1)).apply()

    val ore: Int get() = oraPromemoria / 60
    val minuti: Int get() = oraPromemoria % 60

    companion object {
        const val NOME = "preferenze"
        private const val CHIAVE_PROMEMORIA = "promemoria_attivo"
        private const val CHIAVE_ORA = "promemoria_ora"
    }
}
