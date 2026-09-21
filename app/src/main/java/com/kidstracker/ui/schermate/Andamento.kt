package com.kidstracker.ui.schermate

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidstracker.domain.Bambino
import com.kidstracker.domain.Categoria
import com.kidstracker.domain.Giornata
import com.kidstracker.domain.Statistiche
import com.kidstracker.ui.Formati
import com.kidstracker.ui.Periodo
import com.kidstracker.ui.componenti.BarraGiorno
import com.kidstracker.ui.componenti.BottoneContornato
import com.kidstracker.ui.componenti.IconaFreccia
import com.kidstracker.ui.componenti.IconaImpostazioni
import com.kidstracker.ui.componenti.BarreGiorni
import com.kidstracker.ui.componenti.EtichettaDelta
import com.kidstracker.ui.componenti.GraficoLinee
import com.kidstracker.ui.componenti.IntestazionePrugna
import com.kidstracker.ui.componenti.MiniBarre
import com.kidstracker.ui.componenti.PillolaScelta
import com.kidstracker.ui.componenti.SchedaConTestata
import com.kidstracker.ui.componenti.SelettoreBambino
import com.kidstracker.ui.componenti.SerieGrafico
import com.kidstracker.ui.tema.Azzurrino
import com.kidstracker.ui.tema.Giallo
import com.kidstracker.ui.tema.InkTerziario
import com.kidstracker.ui.tema.Lilla
import com.kidstracker.ui.tema.Menta
import com.kidstracker.ui.tema.coloreBambino
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun SchermataAndamento(
    periodo: Periodo,
    meseAndamento: YearMonth?,
    bambini: List<Bambino>,
    bambinoCorrente: Bambino?,
    storico: List<Giornata>,
    onSeleziona: (Long) -> Unit,
    onPeriodo: (Periodo) -> Unit,
    onMostraMese: () -> Unit,
    onMeseIndietro: () -> Unit,
    onMeseAvanti: () -> Unit,
    onImpostazioni: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bambino = bambinoCorrente ?: return

    // Due modi di scegliere la finestra da guardare: un numero di giorni
    // all'indietro da oggi, oppure un mese intero di calendario.
    val fine: LocalDate
    val inizio: LocalDate
    if (meseAndamento != null) {
        fine = minOf(meseAndamento.atEndOfMonth(), LocalDate.now())
        inizio = meseAndamento.atDay(1)
    } else {
        fine = LocalDate.now()
        inizio = fine.minusDays((periodo.giorni - 1).toLong())
    }

    val serie = bambini.map { b ->
        val sue = storico.filter { it.bambinoId == b.id }
        SerieGrafico(
            nome = b.nome,
            colore = coloreBambino(b.coloreIndex),
            valori = Statistiche.mediaMobile(sue, inizio, fine).map { it.valore }
        )
    }
    val etichette = generateSequence(inizio) { it.plusDays(1) }
        .takeWhile { !it.isAfter(fine) }
        .map { Formati.dataCorta(it) }
        .toList()

    val sueGiornate = storico.filter { it.bambinoId == bambino.id }

    Column(modifier = modifier) {
        IntestazionePrugna(
            titolo = meseAndamento?.let { Formati.mese(it.year, it.monthValue) } ?: "Andamento",
            sottotitolo = meseAndamento?.let { "${it.year}" } ?: "come stanno cambiando le cose",
            azioni = {
                if (meseAndamento != null) {
                    BottoneContornato(onMeseIndietro, "Mese precedente") { tinta ->
                        IconaFreccia(tinta)
                    }
                    BottoneContornato(
                        onClick = onMeseAvanti,
                        descrizione = "Mese successivo",
                        attivo = meseAndamento.isBefore(YearMonth.now())
                    ) { tinta -> IconaFreccia(tinta, versoDestra = true) }
                }
                BottoneContornato(onImpostazioni, "Apri le impostazioni") { tinta ->
                    IconaImpostazioni(tinta)
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (meseAndamento == null) {
                // Sono sei pillole: su un telefono stretto non ci stanno tutte
                // fianco a fianco, quindi la riga scorre invece di schiacciarle.
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Periodo.entries.forEach { scelta ->
                        PillolaScelta(
                            testo = scelta.etichetta,
                            selezionata = scelta == periodo,
                            onClick = { onPeriodo(scelta) },
                            coloreSelezione = Giallo
                        )
                    }
                    PillolaScelta(
                        testo = "Un mese",
                        selezionata = false,
                        onClick = onMostraMese,
                        coloreSelezione = Giallo
                    )
                }
            } else {
                PillolaScelta(
                    testo = "Torna agli ultimi giorni",
                    selezionata = false,
                    onClick = { onPeriodo(periodo) },
                    coloreSelezione = Giallo
                )
            }

            SchedaConTestata(
                titolo = "Indice giornata",
                sottotitolo = "media mobile a 7 giorni · 0–100%",
                tintaTestata = Azzurrino
            ) {
                GraficoLinee(serie = serie, etichette = etichette)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Trascina il dito sul grafico per leggere un giorno preciso.",
                    style = MaterialTheme.typography.labelMedium,
                    color = InkTerziario
                )
            }

            SchedaConTestata(
                titolo = "Categoria per categoria",
                sottotitolo = "ultimi 14 giorni di scuola · variazione sul periodo prima",
                tintaTestata = Lilla,
                extraTestata = {
                    Spacer(Modifier.height(11.dp))
                    SelettoreBambino(
                        bambini = bambini,
                        selezionatoId = bambino.id,
                        onSeleziona = onSeleziona,
                        compatto = true
                    )
                }
            ) {
                // Sabato e domenica non sono giorni di scuola: contarli qui
                // lasciava una barra vuota ogni settimana, per ogni categoria.
                // Si risale finché non si hanno 14 giorni feriali, non 14
                // giorni di calendario: la stessa finestra per ogni riga.
                val ultimiGiorniScuola = generateSequence(LocalDate.now()) { it.minusDays(1) }
                    .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
                    .take(14)
                    .toList()
                    .asReversed()

                // Solo l'ordine con cui compaiono le righe qui: Categoria.tutte
                // resta quella dell'export e delle statistiche, invariata.
                ORDINE_CATEGORIA_PER_CATEGORIA.forEach { categoria ->
                    val variazione = Statistiche.variazioneCategoria(sueGiornate, categoria, LocalDate.now())
                    val ultimi = ultimiGiorniScuola.map { giorno ->
                        sueGiornate.firstOrNull { it.data == giorno }
                            ?.voti?.get(categoria)
                            ?.let { it.punti * 50.0 }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            categoria.etichetta,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.width(64.dp)
                        )
                        MiniBarre(
                            valori = ultimi,
                            modifier = Modifier.weight(1f)
                        )
                        EtichettaDelta(variazione.delta, modifier = Modifier.width(62.dp))
                    }
                }
            }

            SchedaConTestata(
                titolo = "Giorno della settimana",
                sottotitolo = "indice medio di ${bambino.nome} · tutto lo storico",
                tintaTestata = Menta
            ) {
                val medie = Statistiche.mediaPerGiornoSettimana(sueGiornate)
                val barre = DayOfWeek.values()
                    .filter { it.value <= 5 }
                    .mapNotNull { giorno ->
                        medie[giorno]?.let {
                            BarraGiorno(Formati.giornoSettimanaCorto(giorno), it)
                        }
                    }
                BarreGiorni(barre = barre)
                Spacer(Modifier.height(8.dp))
                Text(
                    frasePerGiorno(barre, bambino.nome),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTerziario
                )
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

/** L'ordine con cui compaiono le righe nella carta "Categoria per categoria". */
private val ORDINE_CATEGORIA_PER_CATEGORIA = listOf(
    Categoria.ENTRATA,
    Categoria.COLAZIONE,
    Categoria.ACQUA,
    Categoria.PRIMO,
    Categoria.SECONDO,
    Categoria.DOLCE,
    Categoria.NANNA,
    Categoria.USCITA
)

private fun frasePerGiorno(barre: List<BarraGiorno>, nome: String): String {
    if (barre.size < 3) {
        return "Con qualche giornata in più qui comparirà il confronto fra i giorni della settimana."
    }
    val peggiore = barre.minBy { it.valore }
    val migliore = barre.maxBy { it.valore }
    val differenza = (migliore.valore - peggiore.valore).toInt()
    if (differenza < 8) {
        return "I giorni della settimana di $nome si somigliano tutti: meno di dieci punti fra il migliore e il peggiore."
    }
    return "Il giorno più in salita per $nome è ${peggiore.etichetta}: " +
        "$differenza punti sotto ${migliore.etichetta}."
}
