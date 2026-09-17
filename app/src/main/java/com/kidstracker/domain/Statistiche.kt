package com.kidstracker.domain

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tutta la matematica dell'app sta qui: funzioni pure, senza Android,
 * così si possono verificare con gli unit test.
 */
object Statistiche {

    data class PuntoSerie(val data: LocalDate, val valore: Double?)

    /**
     * Il filtro che precede ogni calcolo: via le giornate mai toccate e via
     * quelle festive. Un ponte o una gita non dicono niente su come mangia o
     * dorme un bambino, e lasciarli dentro sposterebbe le medie senza motivo.
     */
    fun analizzabili(giornate: List<Giornata>): List<Giornata> =
        giornate.filter { it.contaNelleAnalisi }

    /**
     * Media mobile all'indietro: ogni punto è la media dei giorni registrati
     * nella finestra che finisce quel giorno. I giorni senza dati non contano
     * (non valgono zero), così un'assenza non finge un crollo.
     */
    fun mediaMobile(
        giornate: List<Giornata>,
        da: LocalDate,
        a: LocalDate,
        finestra: Int = 7
    ): List<PuntoSerie> {
        val perData = analizzabili(giornate).associate { it.data to it.indiceGiornata }
        val risultato = mutableListOf<PuntoSerie>()
        var giorno = da
        while (!giorno.isAfter(a)) {
            val valori = (0 until finestra).mapNotNull { indietro ->
                perData[giorno.minusDays(indietro.toLong())]
            }
            risultato += PuntoSerie(
                giorno,
                if (valori.isEmpty()) null else valori.average()
            )
            giorno = giorno.plusDays(1)
        }
        return risultato
    }

    /** Indice medio per giorno della settimana, solo sui giorni davvero registrati. */
    fun mediaPerGiornoSettimana(giornate: List<Giornata>): Map<DayOfWeek, Double> =
        analizzabili(giornate)
            .mapNotNull { g -> g.indiceGiornata?.let { g.data.dayOfWeek to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, valori) -> valori.average() }

    data class ConfrontoSalute(
        val mediaInForma: Double?,
        val mediaPocoBene: Double?,
        val giorniInForma: Int,
        val giorniPocoBene: Int
    ) {
        /** Quanti punti percentuali si perdono quando sta poco bene. */
        val differenza: Double?
            get() = if (mediaInForma != null && mediaPocoBene != null) mediaInForma - mediaPocoBene else null

        /** Abbastanza giornate da entrambe le parti per dire qualcosa. */
        val affidabile: Boolean
            get() = giorniInForma >= 3 && giorniPocoBene >= 3
    }

    /** Confronta l'indice pappa nei giorni in forma contro quelli in cui stava poco bene. */
    fun confrontoSalute(giornate: List<Giornata>): ConfrontoSalute {
        val conPappa = analizzabili(giornate).filter { it.indicePappa != null }
        val inForma = conPappa.filter { !it.stavaPocoBene }.mapNotNull { it.indicePappa }
        val pocoBene = conPappa.filter { it.stavaPocoBene }.mapNotNull { it.indicePappa }
        return ConfrontoSalute(
            mediaInForma = inForma.takeIf { it.isNotEmpty() }?.average(),
            mediaPocoBene = pocoBene.takeIf { it.isNotEmpty() }?.average(),
            giorniInForma = inForma.size,
            giorniPocoBene = pocoBene.size
        )
    }

    /**
     * Giorni consecutivi senza nessuna faccina rossa, contati all'indietro
     * dall'ultima giornata registrata. Le assenze non spezzano la striscia
     * ma non la allungano.
     */
    fun strisciaCorrente(giornate: List<Giornata>): Int {
        val ordinate = giornate.filter { !it.vuota }.sortedByDescending { it.data }
        var conta = 0
        for (g in ordinate) {
            // Assenze e festivi si saltano: non spezzano la striscia né la allungano.
            if (g.presenza == Presenza.ASSENTE || g.festiva) continue
            if (g.haRossi) break
            if (g.indiceGiornata == null) continue
            conta++
        }
        return conta
    }

    /** La striscia più lunga mai raggiunta. */
    fun strisciaRecord(giornate: List<Giornata>): Int {
        val ordinate = giornate.filter { !it.vuota }.sortedBy { it.data }
        var migliore = 0
        var corrente = 0
        for (g in ordinate) {
            when {
                g.presenza == Presenza.ASSENTE || g.festiva -> Unit
                g.haRossi -> corrente = 0
                g.indiceGiornata != null -> {
                    corrente++
                    if (corrente > migliore) migliore = corrente
                }
            }
        }
        return migliore
    }

    /** Media di una categoria su una lista di giornate, in percentuale. */
    fun mediaCategoria(giornate: List<Giornata>, categoria: Categoria): Double? {
        val voti = analizzabili(giornate)
            .filter { it.presenza != Presenza.ASSENTE }
            .mapNotNull { it.voti[categoria] }
        if (voti.isEmpty()) return null
        return voti.sumOf { it.punti } * 100.0 / (voti.size * 2)
    }

    data class Variazione(val media: Double?, val delta: Double?)

    /**
     * Media della categoria negli ultimi [finestra] giorni e differenza
     * rispetto ai [finestra] giorni precedenti.
     */
    fun variazioneCategoria(
        giornate: List<Giornata>,
        categoria: Categoria,
        fine: LocalDate,
        finestra: Int = 14
    ): Variazione {
        val inizioRecente = fine.minusDays((finestra - 1).toLong())
        val inizioPrecedente = inizioRecente.minusDays(finestra.toLong())
        val recenti = giornate.filter { !it.data.isBefore(inizioRecente) && !it.data.isAfter(fine) }
        val precedenti = giornate.filter {
            !it.data.isBefore(inizioPrecedente) && it.data.isBefore(inizioRecente)
        }
        val mediaRecente = mediaCategoria(recenti, categoria)
        val mediaPrecedente = mediaCategoria(precedenti, categoria)
        return Variazione(
            media = mediaRecente,
            delta = if (mediaRecente != null && mediaPrecedente != null) mediaRecente - mediaPrecedente else null
        )
    }

    data class DifferenzaGemelli(val categoria: Categoria, val delta: Double)

    /**
     * Differenza per categoria fra due bambini: positiva se è avanti il primo.
     * Ordinata per distanza, le più grosse per prime.
     */
    fun differenzeGemelli(
        primo: List<Giornata>,
        secondo: List<Giornata>,
        minimoGiorni: Int = 3
    ): List<DifferenzaGemelli> =
        Categoria.tutte.mapNotNull { categoria ->
            val a = analizzabili(primo).filter { it.voti.containsKey(categoria) }
            val b = analizzabili(secondo).filter { it.voti.containsKey(categoria) }
            if (a.size < minimoGiorni || b.size < minimoGiorni) return@mapNotNull null
            val mediaA = mediaCategoria(a, categoria) ?: return@mapNotNull null
            val mediaB = mediaCategoria(b, categoria) ?: return@mapNotNull null
            DifferenzaGemelli(categoria, mediaA - mediaB)
        }.sortedByDescending { abs(it.delta) }

    data class ConteggioGiorno(val giorno: DayOfWeek, val difficili: Int, val totale: Int)

    /**
     * Quante volte l'entrata è andata male, per giorno della settimana.
     * Serve a dire "il lunedì è il giorno peggiore" con un numero dietro.
     */
    fun entrateDifficiliPerGiorno(giornate: List<Giornata>): List<ConteggioGiorno> =
        analizzabili(giornate)
            .filter { it.presenza != Presenza.ASSENTE && it.voti.containsKey(Categoria.ENTRATA) }
            .groupBy { it.data.dayOfWeek }
            .map { (giorno, lista) ->
                ConteggioGiorno(
                    giorno = giorno,
                    difficili = lista.count { it.voti[Categoria.ENTRATA] == Voto.NO },
                    totale = lista.size
                )
            }
            .sortedBy { it.giorno.value }

    /** Il giorno della settimana con la quota più alta di entrate difficili. */
    fun giornoPiuDifficile(giornate: List<Giornata>, minimoOsservazioni: Int = 3): ConteggioGiorno? =
        entrateDifficiliPerGiorno(giornate)
            .filter { it.totale >= minimoOsservazioni && it.difficili > 0 }
            .maxByOrNull { it.difficili.toDouble() / it.totale }

    fun percentuale(valore: Double?): String =
        if (valore == null) "–" else "${valore.roundToInt()}%"
}
