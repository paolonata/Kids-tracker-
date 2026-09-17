package com.kidstracker.ui.schermate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidstracker.domain.Bambino
import com.kidstracker.domain.Categoria
import com.kidstracker.domain.Giornata
import com.kidstracker.domain.Giudizio
import com.kidstracker.domain.Presenza
import com.kidstracker.domain.Salute
import com.kidstracker.domain.Voto
import com.kidstracker.ui.Formati
import com.kidstracker.ui.componenti.BarraAvanzamento
import com.kidstracker.ui.componenti.BottoneContornato
import com.kidstracker.ui.componenti.BottoneSticker
import com.kidstracker.ui.componenti.Faccina
import com.kidstracker.ui.componenti.IconaCalendario
import com.kidstracker.ui.componenti.IconaImpostazioni
import com.kidstracker.ui.componenti.PillolaScelta
import com.kidstracker.ui.componenti.SchedaSticker
import com.kidstracker.ui.componenti.SelettoreBambino
import com.kidstracker.ui.componenti.SelettoreFaccine
import com.kidstracker.ui.componenti.IntestazionePrugna
import com.kidstracker.ui.componenti.sticker
import com.kidstracker.ui.tema.Azzurrino
import com.kidstracker.ui.tema.Crema
import com.kidstracker.ui.tema.CremaFaccina
import com.kidstracker.ui.tema.InchiostroFaccina
import com.kidstracker.ui.tema.Inchiostro
import com.kidstracker.ui.tema.InkSecondario
import com.kidstracker.ui.tema.InkTenue
import com.kidstracker.ui.tema.MicroEtichetta
import com.kidstracker.ui.tema.MicroEtichettaStretta
import com.kidstracker.ui.tema.Rosa
import com.kidstracker.ui.tema.coloreBambino
import com.kidstracker.ui.tema.coloreGiudizio
import com.kidstracker.ui.tema.inchiostroSuGiudizio
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * La schermata di inserimento. Ogni tocco salva subito: non c'è un bottone
 * "salva" da ricordarsi la sera quando si è stanchi.
 */
@Composable
fun SchermataGiorno(
    data: LocalDate,
    bambini: List<Bambino>,
    bambinoCorrente: Bambino?,
    giornate: List<Giornata>,
    onSeleziona: (Long) -> Unit,
    onVoto: (Long, Categoria, Voto) -> Unit,
    onPresenza: (Long, Presenza) -> Unit,
    onSalute: (Long, Salute) -> Unit,
    onNota: (Long, String) -> Unit,
    onApriCalendario: () -> Unit,
    onOggi: () -> Unit,
    onImpostazioni: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bambino = bambinoCorrente ?: return
    val giornata = giornate.firstOrNull { it.bambinoId == bambino.id }
        ?: Giornata(bambinoId = bambino.id, data = data)
    val colore = coloreBambino(bambino.coloreIndex)
    val eOggi = data == LocalDate.now()

    Column(modifier = modifier) {
        IntestazionePrugna(
            titolo = Formati.titolo(data),
            sottotitolo = Formati.sottotitolo(data),
            grande = eOggi,
            azioni = {
                BottoneContornato(
                    onClick = if (eOggi) onApriCalendario else onOggi,
                    descrizione = if (eOggi) "Apri il calendario" else "Torna a oggi"
                ) { tinta -> IconaCalendario(tinta, dimensione = 20.dp) }
                BottoneContornato(onImpostazioni, "Apri le impostazioni") { tinta ->
                    IconaImpostazioni(tinta)
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                // Con la finestra a edge-to-edge il sistema non ridimensiona
                // più la finestra da solo alla comparsa della tastiera:
                // senza questo, il campo nota resta coperto sotto di lei.
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SelettoreBambino(
                bambini = bambini,
                selezionatoId = bambino.id,
                onSeleziona = onSeleziona,
                etichettaExtra = { altro ->
                    val sua = giornate.firstOrNull { it.bambinoId == altro.id }
                    "${sua?.segnate ?: 0}/${Giornata.TOTALE_SEGNABILI}"
                }
            )

            BarraAvanzamento(
                fatti = giornata.segnate,
                totale = Giornata.TOTALE_SEGNABILI,
                colore = colore
            )

            giornata.indiceGiornata?.let { indice ->
                val tinta = inchiostroSuGiudizio(giornata.giudizio)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sticker(coloreGiudizio(giornata.giudizio))
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Faccina(
                        voto = when (giornata.giudizio) {
                            Giudizio.BUONO -> Voto.SI
                            Giudizio.COSI_COSI -> Voto.COSI_COSI
                            else -> Voto.NO
                        },
                        dimensione = 58.dp,
                        riempimento = CremaFaccina,
                        tratto = InchiostroFaccina
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when (giornata.giudizio) {
                                Giudizio.BUONO -> "Bella giornata"
                                Giudizio.COSI_COSI -> "Giornata così così"
                                else -> "Giornata difficile"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = tinta
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Indice $indice% · ${giornata.segnate} cose su ${Giornata.TOTALE_SEGNABILI} segnate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tinta
                        )
                    }
                }
            }

            SchedaSticker {
                EtichettaSezione("Presenza")
                Spacer(Modifier.height(11.dp))
                // Quattro opzioni su una riga sola diventavano illeggibili:
                // due per riga lasciano respirare le etichette lunghe.
                Presenza.entries.chunked(2).forEachIndexed { riga, coppia ->
                    if (riga > 0) Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        coppia.forEach { presenza ->
                            PillolaScelta(
                                testo = presenza.etichetta,
                                selezionata = giornata.presenza == presenza,
                                onClick = { onPresenza(bambino.id, presenza) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            val giornataChiusa = giornata.presenza == Presenza.ASSENTE || giornata.festiva
            if (giornataChiusa) {
                SchedaSticker(sfondo = Crema) {
                    Text(
                        if (giornata.festiva) {
                            "Giornata segnata come festiva: non conta nelle analisi."
                        } else {
                            "Giornata segnata come assenza: le faccine non servono."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSecondario
                    )
                }
            } else {
                SchedaSticker(sfondo = Azzurrino) {
                    Text("Pappa e nanna", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(9.dp))
                    IntestazioneColonne()
                    Categoria.nannaEPappa.forEach { categoria ->
                        Spacer(Modifier.height(8.dp))
                        SelettoreFaccine(
                            etichetta = categoria.etichetta,
                            valore = giornata.voti[categoria],
                            onCambia = { voto -> onVoto(bambino.id, categoria, voto) }
                        )
                    }
                }

                SchedaSticker(sfondo = Rosa) {
                    Text("Entrata e uscita", style = MaterialTheme.typography.headlineSmall)
                    Categoria.porta.forEach { categoria ->
                        Spacer(Modifier.height(10.dp))
                        SelettoreFaccine(
                            etichetta = categoria.etichetta,
                            valore = giornata.voti[categoria],
                            onCambia = { voto -> onVoto(bambino.id, categoria, voto) }
                        )
                    }
                }
            }

            SchedaSticker {
                Text("Come stava", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Salute.entries.forEach { salute ->
                        PillolaScelta(
                            testo = salute.etichetta,
                            selezionata = giornata.salute == salute,
                            onClick = { onSalute(bambino.id, salute) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(15.dp))
                EtichettaSezione("Nota")
                Spacer(Modifier.height(7.dp))
                CampoNota(
                    chiave = "${bambino.id}-$data",
                    valoreSalvato = giornata.nota,
                    onCambia = { onNota(bambino.id, it) }
                )
            }

            BottoneSticker(
                testo = if (eOggi) "Fatto" else "Fatto, torna al calendario",
                onClick = onApriCalendario,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Le faccine si salvano da sole, una per una.",
                style = MaterialTheme.typography.labelMedium,
                color = InkTenue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun EtichettaSezione(testo: String) {
    Text(
        testo.uppercase(),
        style = MicroEtichetta,
        color = InkTenue
    )
}

/** I tre titoli sopra le colonne delle faccine: il colore non basta mai da solo. */
@Composable
private fun IntestazioneColonne() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        listOf("SÌ", "COSÌ COSÌ", "NO").forEachIndexed { indice, testo ->
            if (indice > 0) Spacer(Modifier.width(9.dp))
            Text(
                testo,
                style = MicroEtichettaStretta,
                color = InkSecondario,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(52.dp)
            )
        }
    }
}

@Composable
private fun CampoNota(
    chiave: String,
    valoreSalvato: String,
    onCambia: (String) -> Unit
) {
    var scritto by rememberSaveable(chiave) { mutableStateOf<String?>(null) }
    val valore = scritto ?: valoreSalvato

    // imePadding() da solo fa spazio alla tastiera, ma non scorre fin qui:
    // se il campo è già oltre il bordo basso quando la tastiera si apre,
    // senza questo resterebbe comunque fuori vista finché non si scorre a mano.
    val richiestaVista = remember { BringIntoViewRequester() }
    val ambito = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .sticker(Crema, 15.dp, ombra = false)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .bringIntoViewRequester(richiestaVista),
        contentAlignment = Alignment.CenterStart
    ) {
        if (valore.isEmpty()) {
            Text(
                "Es. si è svegliato presto",
                style = MaterialTheme.typography.bodyLarge,
                color = InkTenue
            )
        }
        BasicTextField(
            value = valore,
            onValueChange = {
                scritto = it
                onCambia(it)
            },
            textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = Inchiostro)),
            cursorBrush = SolidColor(Inchiostro),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusEvent {
                    if (it.isFocused) ambito.launch { richiestaVista.bringIntoView() }
                }
        )
    }
}
