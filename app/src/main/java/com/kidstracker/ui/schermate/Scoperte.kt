package com.kidstracker.ui.schermate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kidstracker.domain.Bambino
import com.kidstracker.domain.Giornata
import com.kidstracker.domain.Giudizio
import com.kidstracker.domain.Statistiche
import com.kidstracker.ui.componenti.BarraDivergente
import com.kidstracker.ui.componenti.BottoneContornato
import com.kidstracker.ui.componenti.IconaImpostazioni
import com.kidstracker.ui.componenti.BarraOrizzontale
import com.kidstracker.ui.componenti.IntestazionePrugna
import com.kidstracker.ui.componenti.SchedaSticker
import com.kidstracker.ui.componenti.SelettoreBambino
import com.kidstracker.ui.componenti.StrisciaGiorni
import com.kidstracker.ui.componenti.sticker
import com.kidstracker.ui.tema.Azzurrino
import com.kidstracker.ui.tema.BluTenue
import com.kidstracker.ui.tema.InchiostroChiaro
import com.kidstracker.ui.tema.InkTerziario
import com.kidstracker.ui.tema.Lilla
import com.kidstracker.ui.tema.Menta
import com.kidstracker.ui.tema.Rosa
import com.kidstracker.ui.tema.Superficie
import com.kidstracker.ui.tema.Verde
import com.kidstracker.ui.tema.coloreBambino
import com.kidstracker.ui.tema.coloreGiudizio
import com.kidstracker.ui.Formati
import kotlin.math.abs

@Composable
fun SchermataScoperte(
    bambini: List<Bambino>,
    bambinoCorrente: Bambino?,
    storico: List<Giornata>,
    onSeleziona: (Long) -> Unit,
    onImpostazioni: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bambino = bambinoCorrente ?: return
    // Le scoperte nascono dai confronti: i festivi non hanno niente da confrontare.
    val sue = storico.filter { it.bambinoId == bambino.id && it.contaNelleAnalisi }
    val segnate = storico.count { it.contaNelleAnalisi }

    Column(modifier = modifier) {
        IntestazionePrugna(
            titolo = "Scoperte",
            sottotitolo = if (segnate == 0) {
                "ancora nessuna giornata segnata"
            } else {
                "su $segnate giornate segnate"
            },
            azioni = {
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
            SelettoreBambino(
                bambini = bambini,
                selezionatoId = bambino.id,
                onSeleziona = onSeleziona
            )

            if (sue.size < 5) {
                SchedaSticker(sfondo = Azzurrino) {
                    Text(
                        "Ci vuole ancora un po'",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Con almeno cinque giornate segnate l'app comincia a confrontare " +
                            "i periodi, i giorni della settimana e l'effetto dei malesseri. " +
                            "Finora ne hai segnate ${sue.size}.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkTerziario
                    )
                }
                return@Column
            }

            SchedaSalute(sue, bambino)
            SchedaEntrata(sue, bambino)
            SchedaStriscia(sue)
            if (bambini.size >= 2) SchedaGemelli(bambini, storico)

            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SchedaSalute(giornate: List<Giornata>, bambino: Bambino, modifier: Modifier = Modifier) {
    val confronto = Statistiche.confrontoSalute(giornate)
    SchedaSticker(sfondo = Azzurrino, modifier = modifier) {
        if (!confronto.affidabile) {
            Text("Malessere e pappa", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Per dire se il malessere pesa sulla pappa servono almeno tre giornate " +
                    "per tipo. Ora ne hai ${confronto.giorniInForma} in forma e " +
                    "${confronto.giorniPocoBene} in cui stava poco bene.",
                style = MaterialTheme.typography.bodyLarge,
                color = InkTerziario
            )
            return@SchedaSticker
        }

        val differenza = confronto.differenza ?: 0.0
        Text(
            when {
                differenza >= 25 -> "Quando sta poco bene, mangia molto meno"
                differenza >= 10 -> "Quando sta poco bene, mangia un po' meno"
                differenza <= -10 -> "Quando sta poco bene mangia anche di più"
                else -> "Il malessere non sembra toccare la pappa"
            },
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Indice pappa medio di ${bambino.nome} nelle giornate in forma, " +
                "contro quelle in cui l'hai segnato poco bene.",
            style = MaterialTheme.typography.bodyLarge,
            color = InkTerziario
        )
        Spacer(Modifier.height(13.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sticker(Superficie, 16.dp, ombra = false)
                .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RigaConfronto(
                "Giorni in forma",
                confronto.mediaInForma,
                confronto.giorniInForma,
                coloreBambino(bambino.coloreIndex)
            )
            RigaConfronto(
                "Giorni in cui stava poco bene",
                confronto.mediaPocoBene,
                confronto.giorniPocoBene,
                BluTenue
            )
        }
    }
}

@Composable
private fun RigaConfronto(nome: String, valore: Double?, giorni: Int, colore: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(nome, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                "${Statistiche.puntiSuDue(valore)}/2",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        Spacer(Modifier.height(6.dp))
        BarraOrizzontale(percentuale = valore ?: 0.0, colore = colore)
        Spacer(Modifier.height(4.dp))
        Text(
            "media su $giorni giornate",
            style = MaterialTheme.typography.labelSmall,
            color = InkTerziario
        )
    }
}

@Composable
private fun SchedaEntrata(giornate: List<Giornata>, bambino: Bambino) {
    val peggiore = Statistiche.giornoPiuDifficile(giornate)
    val perGiorno = Statistiche.entrateDifficiliPerGiorno(giornate)
    SchedaSticker(sfondo = Rosa) {
        if (peggiore == null) {
            // Nessun giorno della settimana ha ancora 3 osservazioni: non
            // vuol dire che l'entrata vada bene, solo che non si può ancora
            // dire QUALE giorno pesa di più. Il conteggio complessivo, non
            // legato al giorno della settimana, dice la verità nel frattempo.
            val complessiva = Statistiche.entrataDifficileComplessiva(giornate)
            if (complessiva == null || complessiva.difficili == 0) {
                Text("L'entrata a scuola", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Finora ${bambino.nome} non ha mai avuto un'entrata difficile. Buon segno.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkTerziario
                )
            } else {
                val quota = complessiva.difficili.toDouble() / complessiva.totale
                Text(
                    if (quota >= 0.5) "L'entrata è difficile più spesso che no" else "Qualche entrata difficile",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${complessiva.difficili} volte su ${complessiva.totale} finora per ${bambino.nome}. " +
                        "Non ci sono ancora abbastanza giornate sullo stesso giorno della settimana " +
                        "per dire quale pesa di più.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkTerziario
                )
            }
        } else {
            val giornoNome = Formati.giornoSettimanaCorto(peggiore.giorno)
            Text(
                "Il $giornoNome l'entrata è la più difficile",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${peggiore.difficili} volte su ${peggiore.totale} ${bambino.nome} " +
                    "è entrato male di $giornoNome.",
                style = MaterialTheme.typography.bodyLarge,
                color = InkTerziario
            )
        }

        if (perGiorno.isNotEmpty()) {
            Spacer(Modifier.height(13.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                perGiorno.forEach { conteggio ->
                    val quota = conteggio.difficili.toDouble() / conteggio.totale
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .sticker(Superficie, 15.dp, ombra = false)
                            .padding(vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .sticker(
                                    when {
                                        quota >= 0.5 -> coloreGiudizio(Giudizio.DIFFICILE)
                                        quota > 0.0 -> coloreGiudizio(Giudizio.COSI_COSI)
                                        else -> coloreGiudizio(Giudizio.BUONO)
                                    },
                                    6.dp,
                                    ombra = false
                                )
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            Formati.giornoSettimanaCorto(conteggio.giorno),
                            style = MaterialTheme.typography.labelSmall,
                            color = InkTerziario
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SchedaStriscia(giornate: List<Giornata>) {
    val corrente = Statistiche.strisciaCorrente(giornate)
    val record = Statistiche.strisciaRecord(giornate)
    val ultime = giornate.sortedBy { it.data }.takeLast(14)

    SchedaSticker(sfondo = Menta) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (corrente == 0) {
                        "La striscia riparte da qui"
                    } else {
                        "$corrente giorni di fila senza rossi"
                    },
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (record <= corrente && corrente > 0) {
                        "È il record dall'inizio della scuola."
                    } else {
                        "Il record dall'inizio della scuola è $record."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkTerziario
                )
            }
            Spacer(Modifier.width(13.dp))
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .sticker(Verde, 31.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$corrente",
                    style = MaterialTheme.typography.displaySmall,
                    color = InchiostroChiaro
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        StrisciaGiorni(colori = ultime.map { coloreGiudizio(it.giudizio) })
        Spacer(Modifier.height(8.dp))
        Text(
            "Ultime ${ultime.size} giornate segnate, da sinistra a destra",
            style = MaterialTheme.typography.labelMedium,
            color = InkTerziario
        )
    }
}

@Composable
private fun SchedaGemelli(bambini: List<Bambino>, storico: List<Giornata>) {
    val primo = bambini[0]
    val secondo = bambini[1]
    val differenze = Statistiche.differenzeGemelli(
        storico.filter { it.bambinoId == primo.id },
        storico.filter { it.bambinoId == secondo.id }
    ).take(3)

    SchedaSticker(sfondo = Lilla) {
        if (differenze.isEmpty()) {
            Text("${primo.nome} e ${secondo.nome}", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Servono almeno tre giornate segnate per entrambi, sulla stessa categoria, " +
                    "per poterli confrontare.",
                style = MaterialTheme.typography.bodyLarge,
                color = InkTerziario
            )
            return@SchedaSticker
        }

        val massima = differenze.first()
        Text(
            if (abs(massima.delta) < 8) {
                "${primo.nome} e ${secondo.nome} vanno di pari passo"
            } else {
                "Dove ${primo.nome} e ${secondo.nome} si dividono"
            },
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Differenza media per categoria, su tutto lo storico.",
            style = MaterialTheme.typography.bodyLarge,
            color = InkTerziario
        )
        Spacer(Modifier.height(13.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .sticker(Superficie, 16.dp, ombra = false)
                .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            differenze.forEach { differenza ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        differenza.categoria.etichetta,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.width(58.dp)
                    )
                    BarraDivergente(
                        delta = differenza.delta,
                        coloreDestra = coloreBambino(primo.coloreIndex),
                        coloreSinistra = coloreBambino(secondo.coloreIndex),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        (if (differenza.delta >= 0) "+" else "−") +
                            Statistiche.puntiSuDue(abs(differenza.delta)),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                listOf(primo, secondo).forEach { chi ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .sticker(coloreBambino(chi.coloreIndex), 6.dp, ombra = false)
                        )
                        Text(
                            "${chi.nome} avanti",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkTerziario
                        )
                    }
                }
            }
        }
    }
}
