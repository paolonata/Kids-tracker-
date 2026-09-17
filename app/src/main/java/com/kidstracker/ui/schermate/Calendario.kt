package com.kidstracker.ui.schermate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidstracker.domain.Bambino
import com.kidstracker.domain.Categoria
import com.kidstracker.domain.Giornata
import com.kidstracker.domain.Giudizio
import com.kidstracker.domain.Statistiche
import com.kidstracker.domain.Voto
import com.kidstracker.ui.Formati
import com.kidstracker.ui.componenti.BottoneContornato
import com.kidstracker.ui.componenti.Faccina
import com.kidstracker.ui.componenti.IconaFreccia
import com.kidstracker.ui.componenti.IntestazionePrugna
import com.kidstracker.ui.componenti.SelettoreBambino
import com.kidstracker.ui.componenti.bordoTratteggiato
import com.kidstracker.ui.componenti.sticker
import com.kidstracker.ui.tema.Azzurrino
import com.kidstracker.ui.tema.Crema
import com.kidstracker.ui.tema.Inchiostro
import com.kidstracker.ui.tema.InchiostroFaccina
import com.kidstracker.ui.tema.InkSecondario
import com.kidstracker.ui.tema.InkTenue
import com.kidstracker.ui.tema.InkTerziario
import com.kidstracker.ui.tema.Menta
import com.kidstracker.ui.tema.Rosa
import com.kidstracker.ui.tema.Superficie
import com.kidstracker.ui.tema.TratteggioTenue
import com.kidstracker.ui.tema.coloreGiudizio
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun SchermataCalendario(
    mese: YearMonth,
    bambini: List<Bambino>,
    bambinoCorrente: Bambino?,
    giornate: List<Giornata>,
    onSeleziona: (Long) -> Unit,
    onMeseIndietro: () -> Unit,
    onMeseAvanti: () -> Unit,
    onApriGiorno: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val bambino = bambinoCorrente ?: return
    val sue = giornate.filter { it.bambinoId == bambino.id }
    val perGiorno = sue.associateBy { it.data }
    val segnate = sue.count { !it.vuota }

    Column(modifier = modifier) {
        IntestazionePrugna(
            titolo = Formati.mese(mese.year, mese.monthValue),
            sottotitolo = "${mese.year} · $segnate giornate segnate",
            azioni = {
                BottoneContornato(onMeseIndietro, "Mese precedente") { tinta ->
                    IconaFreccia(tinta)
                }
                BottoneContornato(
                    onClick = onMeseAvanti,
                    descrizione = "Mese successivo",
                    attivo = mese.isBefore(YearMonth.now())
                ) { tinta -> IconaFreccia(tinta, versoDestra = true) }
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

            GrigliaMese(
                mese = mese,
                perGiorno = perGiorno,
                onApriGiorno = onApriGiorno
            )

            Legenda()

            RiepilogoMese(sue)

            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun GrigliaMese(
    mese: YearMonth,
    perGiorno: Map<LocalDate, Giornata>,
    onApriGiorno: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val primo = mese.atDay(1)
    val scarto = (primo.dayOfWeek.value + 6) % 7
    val quanti = mese.lengthOfMonth()
    val celle = ((scarto + quanti + 6) / 7) * 7
    val oggi = LocalDate.now()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DayOfWeek.values().forEach { giorno ->
                Text(
                    Formati.inizialeGiornoSettimana(giorno),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (giorno.value >= 6) InkTenue else InkSecondario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(7.dp))

        var indice = 0
        while (indice < celle) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(7) { colonna ->
                    val numero = indice + colonna - scarto + 1
                    val dentro = numero in 1..quanti
                    val giorno = if (dentro) mese.atDay(numero) else null
                    CellaGiorno(
                        giorno = giorno,
                        giornata = giorno?.let { perGiorno[it] },
                        futuro = giorno != null && giorno.isAfter(oggi),
                        onClick = { giorno?.let(onApriGiorno) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (indice + 7 < celle) Spacer(Modifier.height(6.dp))
            indice += 7
        }
    }
}

@Composable
private fun CellaGiorno(
    giorno: LocalDate?,
    giornata: Giornata?,
    futuro: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val giudizio = giornata?.giudizio ?: Giudizio.NON_REGISTRATO
    val registrata = giornata != null && !giornata.vuota

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                when {
                    giorno == null -> Modifier
                    giudizio == Giudizio.ASSENTE -> Modifier.bordoTratteggiato(15.dp, InkTenue)
                    registrata -> Modifier.sticker(coloreGiudizio(giudizio), 15.dp, ombra = false)
                    futuro -> Modifier
                    else -> Modifier.bordoTratteggiato(15.dp, TratteggioTenue)
                }
            )
            .then(
                if (giorno != null && !futuro) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = "Apri ${Formati.dataCorta(giorno)}",
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (giorno == null) return@Box

        if (registrata && giudizio != Giudizio.ASSENTE) {
            Faccina(
                voto = when (giudizio) {
                    Giudizio.BUONO -> Voto.SI
                    Giudizio.COSI_COSI -> Voto.COSI_COSI
                    else -> Voto.NO
                },
                dimensione = 26.dp,
                conCerchio = false,
                tratto = InchiostroFaccina
            )
        } else {
            Text(
                giorno.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (futuro) TratteggioTenue else InkTenue
            )
        }

        if (registrata) {
            Text(
                giorno.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Inchiostro,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 5.dp, top = 3.dp)
            )
        }

        if (giornata != null && giornata.stavaPocoBene) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 4.dp)
                    .size(9.dp)
                    .sticker(Crema, 5.dp, ombra = false)
            )
        }
    }
}

@Composable
private fun Legenda() {
    val voci = listOf(
        "Buona" to coloreGiudizio(Giudizio.BUONO),
        "Così così" to coloreGiudizio(Giudizio.COSI_COSI),
        "Difficile" to coloreGiudizio(Giudizio.DIFFICILE),
        "Assente" to Crema
    )
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            voci.forEach { (nome, colore) ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .sticker(Superficie, 15.dp, ombra = false)
                        .padding(horizontal = 7.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .sticker(colore, 4.dp, ombra = false)
                    )
                    Text(
                        nome,
                        style = MaterialTheme.typography.labelSmall,
                        color = InkTerziario
                    )
                }
            }
        }
        Text(
            "Il cerchietto in alto a destra segna i giorni in cui stava poco bene.",
            style = MaterialTheme.typography.labelMedium,
            color = InkTenue
        )
    }
}

@Composable
private fun RiepilogoMese(giornate: List<Giornata>) {
    val buone = giornate.count { it.giudizio == Giudizio.BUONO }
    val segnate = giornate.count { !it.vuota }
    val pappa = Statistiche.mediaCategoria(giornate, Categoria.PRIMO)
        ?.let { primo ->
            listOfNotNull(
                primo,
                Statistiche.mediaCategoria(giornate, Categoria.SECONDO),
                Statistiche.mediaCategoria(giornate, Categoria.DOLCE)
            ).average()
        }
    val striscia = Statistiche.strisciaCorrente(giornate)

    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Riquadro("$buone", "giornate buone su $segnate", Menta, Modifier.weight(1f))
        Riquadro(Statistiche.percentuale(pappa), "indice pappa del mese", Azzurrino, Modifier.weight(1f))
        Riquadro("$striscia", "giorni buoni di fila", Rosa, Modifier.weight(1f))
    }
}

@Composable
private fun Riquadro(
    valore: String,
    etichetta: String,
    tinta: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .sticker(tinta, 19.dp)
            .padding(horizontal = 11.dp, vertical = 13.dp)
    ) {
        Text(valore, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(6.dp))
        Text(etichetta, style = MaterialTheme.typography.labelMedium, color = InkTerziario)
    }
}
