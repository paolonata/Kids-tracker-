package com.kidstracker.domain

import java.time.LocalDate

/** Le tre faccine che manda la scuola. */
enum class Voto(val punti: Int, val etichetta: String) {
    NO(0, "no"),
    COSI_COSI(1, "così così"),
    SI(2, "sì")
}

enum class Presenza(val etichetta: String) {
    SCUOLA("A scuola"),
    ASSENTE("Assente"),
    USCITO_PRIMA("Uscito prima"),

    /**
     * Ponte, gita, chiusura: la giornata è chiusa ma non dice niente su come
     * sta andando il bambino, quindi resta fuori da tutte le statistiche.
     */
    FESTIVO("Festivo")
}

enum class Salute(val etichetta: String) {
    BENE("Sta bene"),
    POCO_BENE("Poco bene"),
    FEBBRE("Febbre")
}

/** Le sei cose che segniamo ogni giorno. */
enum class Categoria(val etichetta: String) {
    NANNA("Nanna"),
    PRIMO("Primo"),
    SECONDO("Secondo"),
    DOLCE("Dolce"),
    ENTRATA("Entrata"),
    USCITA("Uscita");

    companion object {
        val pasti: List<Categoria> = listOf(PRIMO, SECONDO, DOLCE)
        val nannaEPappa: List<Categoria> = listOf(NANNA, PRIMO, SECONDO, DOLCE)
        val porta: List<Categoria> = listOf(ENTRATA, USCITA)
        val tutte: List<Categoria> = listOf(NANNA, PRIMO, SECONDO, DOLCE, ENTRATA, USCITA)

        fun daNome(nome: String?): Categoria? = entries.firstOrNull { it.name == nome }
    }
}

/** Come è andata la giornata nel complesso: è quello che colora calendario e grafici. */
enum class Giudizio { BUONO, COSI_COSI, DIFFICILE, ASSENTE, FESTIVO, NON_REGISTRATO }

data class Bambino(
    val id: Long,
    val nome: String,
    val coloreIndex: Int,
    /** Nome del file della foto, se ne è stata scelta una. */
    val foto: String? = null
)

data class Giornata(
    val bambinoId: Long,
    val data: LocalDate,
    val presenza: Presenza = Presenza.SCUOLA,
    val voti: Map<Categoria, Voto> = emptyMap(),
    val salute: Salute = Salute.BENE,
    val nota: String = ""
) {

    /** Vuota davvero: niente da salvare e niente da mostrare nel calendario. */
    val vuota: Boolean
        get() = presenza == Presenza.SCUOLA && voti.isEmpty() && salute == Salute.BENE && nota.isBlank()

    /** Un giorno festivo è segnato, ma non racconta nulla: fuori da medie e confronti. */
    val festiva: Boolean
        get() = presenza == Presenza.FESTIVO

    /**
     * Le giornate che possono entrare in una statistica: segnate e non festive.
     * È il filtro che tutte le funzioni di [Statistiche] applicano per prime.
     */
    val contaNelleAnalisi: Boolean
        get() = !vuota && !festiva

    val completa: Boolean
        get() = presenza != Presenza.SCUOLA || voti.size == Categoria.tutte.size

    /**
     * Quante delle sette cose (presenza + sei faccine) sono state segnate.
     * Una giornata mai toccata vale zero: "a scuola" è il valore di partenza,
     * non una scelta, e contarlo farebbe apparire 1 su 7 senza aver fatto nulla.
     */
    val segnate: Int
        get() = when {
            vuota -> 0
            presenza != Presenza.SCUOLA -> TOTALE_SEGNABILI
            else -> 1 + voti.size
        }

    private fun indice(categorie: List<Categoria>): Int? {
        if (presenza == Presenza.ASSENTE || festiva) return null
        val presenti = categorie.mapNotNull { voti[it] }
        if (presenti.isEmpty()) return null
        return (presenti.sumOf { it.punti } * 100) / (presenti.size * 2)
    }

    /** Solo primo, secondo e dolce. */
    val indicePappa: Int?
        get() = indice(Categoria.pasti)

    /** Tutte e sei le categorie, pesate uguale. */
    val indiceGiornata: Int?
        get() = indice(Categoria.tutte)

    val haRossi: Boolean
        get() = presenza != Presenza.ASSENTE && !festiva && voti.values.any { it == Voto.NO }

    val stavaPocoBene: Boolean
        get() = salute != Salute.BENE && !festiva

    val giudizio: Giudizio
        get() {
            if (festiva) return Giudizio.FESTIVO
            if (presenza == Presenza.ASSENTE) return Giudizio.ASSENTE
            val indice = indiceGiornata ?: return Giudizio.NON_REGISTRATO
            return when {
                indice >= 67 -> Giudizio.BUONO
                indice >= 34 -> Giudizio.COSI_COSI
                else -> Giudizio.DIFFICILE
            }
        }

    companion object {
        const val TOTALE_SEGNABILI = 7
    }
}
