package com.kidstracker

import com.kidstracker.domain.Categoria
import com.kidstracker.domain.Giornata
import com.kidstracker.domain.Giudizio
import com.kidstracker.domain.Presenza
import com.kidstracker.domain.Salute
import com.kidstracker.domain.Statistiche
import com.kidstracker.domain.Voto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatisticheTest {

    private val partenza: LocalDate = LocalDate.of(2026, 9, 1)

    private fun giornata(
        giorno: Int,
        vararg voti: Pair<Categoria, Voto>,
        presenza: Presenza = Presenza.SCUOLA,
        salute: Salute = Salute.BENE
    ) = Giornata(
        bambinoId = 1L,
        data = partenza.plusDays((giorno - 1).toLong()),
        presenza = presenza,
        voti = voti.toMap(),
        salute = salute
    )

    private fun tutte(voto: Voto) = Categoria.tutte.map { it to voto }.toTypedArray()

    @Test
    fun `indice giornata usa solo le categorie segnate`() {
        val parziale = giornata(1, Categoria.PRIMO to Voto.SI, Categoria.SECONDO to Voto.NO)
        // (2 + 0) su un massimo di 4 = 50%
        assertEquals(50, parziale.indiceGiornata)
    }

    @Test
    fun `indice pappa ignora nanna entrata e uscita`() {
        val g = giornata(
            1,
            Categoria.NANNA to Voto.NO,
            Categoria.PRIMO to Voto.SI,
            Categoria.SECONDO to Voto.SI,
            Categoria.DOLCE to Voto.SI
        )
        assertEquals(100, g.indicePappa)
    }

    @Test
    fun `una giornata di assenza non ha indice`() {
        val assente = giornata(1, presenza = Presenza.ASSENTE)
        assertNull(assente.indiceGiornata)
        assertEquals(Giudizio.ASSENTE, assente.giudizio)
    }

    @Test
    fun `il giudizio segue le soglie`() {
        assertEquals(Giudizio.BUONO, giornata(1, *tutte(Voto.SI)).giudizio)
        assertEquals(Giudizio.COSI_COSI, giornata(1, *tutte(Voto.COSI_COSI)).giudizio)
        assertEquals(Giudizio.DIFFICILE, giornata(1, *tutte(Voto.NO)).giudizio)
    }

    @Test
    fun `la media mobile ignora i giorni senza dati invece di contarli zero`() {
        val giornate = listOf(
            giornata(1, *tutte(Voto.SI)),   // 100
            giornata(3, *tutte(Voto.NO))    // 0, il giorno 2 non esiste
        )
        val serie = Statistiche.mediaMobile(giornate, partenza, partenza.plusDays(2), finestra = 7)

        assertEquals(3, serie.size)
        assertEquals(100.0, serie[0].valore!!, 0.001)
        assertEquals(100.0, serie[1].valore!!, 0.001) // il giorno vuoto non abbassa nulla
        assertEquals(50.0, serie[2].valore!!, 0.001)
    }

    @Test
    fun `la media mobile e nulla finche non c'e nessun dato`() {
        val serie = Statistiche.mediaMobile(emptyList(), partenza, partenza.plusDays(1))
        assertTrue(serie.all { it.valore == null })
    }

    @Test
    fun `il confronto salute separa i giorni in forma da quelli storti`() {
        val giornate = listOf(
            giornata(1, *Categoria.pasti.map { it to Voto.SI }.toTypedArray()),
            giornata(2, *Categoria.pasti.map { it to Voto.SI }.toTypedArray()),
            giornata(3, *Categoria.pasti.map { it to Voto.SI }.toTypedArray()),
            giornata(4, *Categoria.pasti.map { it to Voto.NO }.toTypedArray(), salute = Salute.FEBBRE),
            giornata(5, *Categoria.pasti.map { it to Voto.NO }.toTypedArray(), salute = Salute.POCO_BENE),
            giornata(6, *Categoria.pasti.map { it to Voto.NO }.toTypedArray(), salute = Salute.POCO_BENE)
        )
        val confronto = Statistiche.confrontoSalute(giornate)

        assertEquals(3, confronto.giorniInForma)
        assertEquals(3, confronto.giorniPocoBene)
        assertEquals(100.0, confronto.mediaInForma!!, 0.001)
        assertEquals(0.0, confronto.mediaPocoBene!!, 0.001)
        assertEquals(100.0, confronto.differenza!!, 0.001)
        assertTrue(confronto.affidabile)
    }

    @Test
    fun `il confronto salute non e affidabile con pochi giorni`() {
        val giornate = listOf(
            giornata(1, *Categoria.pasti.map { it to Voto.SI }.toTypedArray()),
            giornata(2, *Categoria.pasti.map { it to Voto.NO }.toTypedArray(), salute = Salute.POCO_BENE)
        )
        assertTrue(!Statistiche.confrontoSalute(giornate).affidabile)
    }

    @Test
    fun `la striscia si ferma al primo rosso e l'assenza non la spezza`() {
        val giornate = listOf(
            giornata(1, *tutte(Voto.SI)),
            giornata(2, *tutte(Voto.NO)),            // rosso: taglia qui
            giornata(3, *tutte(Voto.SI)),
            giornata(4, presenza = Presenza.ASSENTE), // salta, non conta
            giornata(5, *tutte(Voto.COSI_COSI))
        )
        assertEquals(2, Statistiche.strisciaCorrente(giornate))
        assertEquals(2, Statistiche.strisciaRecord(giornate))
    }

    @Test
    fun `la variazione confronta le due finestre`() {
        val fine = partenza.plusDays(27)
        // Prime due settimane tutte NO, ultime due tutte SI.
        val giornate = (0..27).map { indice ->
            Giornata(
                bambinoId = 1L,
                data = partenza.plusDays(indice.toLong()),
                voti = mapOf(Categoria.PRIMO to if (indice < 14) Voto.NO else Voto.SI)
            )
        }
        val variazione = Statistiche.variazioneCategoria(giornate, Categoria.PRIMO, fine, finestra = 14)

        assertEquals(100.0, variazione.media!!, 0.001)
        assertEquals(100.0, variazione.delta!!, 0.001)
    }

    @Test
    fun `il giorno piu difficile guarda la quota di entrate storte`() {
        // Cinque lunedì, quattro con entrata NO; cinque martedì tutti buoni.
        val giornate = (0..4).flatMap { settimana ->
            val lunedi = LocalDate.of(2026, 9, 7).plusWeeks(settimana.toLong())
            listOf(
                Giornata(
                    bambinoId = 1L,
                    data = lunedi,
                    voti = mapOf(Categoria.ENTRATA to if (settimana < 4) Voto.NO else Voto.SI)
                ),
                Giornata(
                    bambinoId = 1L,
                    data = lunedi.plusDays(1),
                    voti = mapOf(Categoria.ENTRATA to Voto.SI)
                )
            )
        }

        val peggiore = Statistiche.giornoPiuDifficile(giornate)
        assertEquals(java.time.DayOfWeek.MONDAY, peggiore!!.giorno)
        assertEquals(4, peggiore.difficili)
        assertEquals(5, peggiore.totale)
    }

    @Test
    fun `le differenze fra gemelli sono ordinate per distanza`() {
        val primo = (1..5).map {
            giornata(it, Categoria.NANNA to Voto.SI, Categoria.PRIMO to Voto.COSI_COSI)
        }
        val secondo = (1..5).map {
            giornata(it, Categoria.NANNA to Voto.NO, Categoria.PRIMO to Voto.COSI_COSI)
                .copy(bambinoId = 2L)
        }

        val differenze = Statistiche.differenzeGemelli(primo, secondo)
        assertEquals(Categoria.NANNA, differenze.first().categoria)
        assertEquals(100.0, differenze.first().delta, 0.001)
        assertEquals(0.0, differenze.first { it.categoria == Categoria.PRIMO }.delta, 0.001)
    }

    @Test
    fun `una giornata mai toccata non conta come iniziata`() {
        // "A scuola" è il valore di partenza, non una scelta fatta.
        assertEquals(0, giornata(1).segnate)
        assertTrue(giornata(1).vuota)
    }

    @Test
    fun `quante cose sono state segnate`() {
        assertEquals(3, giornata(1, Categoria.PRIMO to Voto.SI, Categoria.NANNA to Voto.NO).segnate)
        assertEquals(
            Giornata.TOTALE_SEGNABILI,
            giornata(1, presenza = Presenza.ASSENTE).segnate
        )
    }
}
