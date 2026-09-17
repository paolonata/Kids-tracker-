package com.kidstracker.ui.componenti

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kidstracker.domain.Bambino
import com.kidstracker.ui.tema.Crema
import com.kidstracker.ui.tema.Inchiostro
import com.kidstracker.ui.tema.InchiostroChiaro
import com.kidstracker.ui.tema.InchiostroFaccina
import com.kidstracker.ui.tema.InkSecondario
import com.kidstracker.ui.tema.InkTenue
import com.kidstracker.ui.tema.Misure
import com.kidstracker.ui.tema.Superficie
import com.kidstracker.ui.tema.coloreBambino

enum class Sezione(val rotta: String, val etichetta: String) {
    OGGI("oggi", "Oggi"),
    CALENDARIO("calendario", "Calendario"),
    ANDAMENTO("andamento", "Andamento"),
    SCOPERTE("scoperte", "Scoperte")
}

@Composable
fun BarraNavigazione(
    corrente: Sezione,
    onNaviga: (Sezione) -> Unit,
    modifier: Modifier = Modifier
) {
    val bordo = Inchiostro
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Superficie)
            .drawBehind {
                drawRect(
                    color = bordo,
                    topLeft = Offset.Zero,
                    size = Size(size.width, Misure.bordo.toPx())
                )
            }
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Sezione.entries.forEach { sezione ->
            val attiva = sezione == corrente
            val colore = if (attiva) Crema else InkSecondario
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (attiva) Inchiostro else Color.Transparent)
                    .clickable(role = Role.Tab) { onNaviga(sezione) }
                    .semantics { selected = attiva }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (sezione) {
                    Sezione.OGGI -> IconaSole(colore)
                    Sezione.CALENDARIO -> IconaCalendario(colore)
                    Sezione.ANDAMENTO -> IconaGrafico(colore)
                    Sezione.SCOPERTE -> IconaLampadina(colore)
                }
                Text(
                    sezione.etichetta,
                    style = MaterialTheme.typography.labelMedium,
                    color = colore
                )
            }
        }
    }
}

/**
 * Le due linguette per passare da un bambino all'altro.
 * Quella attiva prende il colore del bambino. A sinistra del nome c'è la foto
 * del bambino, o la faccina in negativo se non ne è stata scelta una.
 */
@Composable
fun SelettoreBambino(
    bambini: List<Bambino>,
    selezionatoId: Long?,
    onSeleziona: (Long) -> Unit,
    modifier: Modifier = Modifier,
    compatto: Boolean = false,
    etichettaExtra: (Bambino) -> String? = { null }
) {
    if (bambini.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compatto) 8.dp else 10.dp)
    ) {
        bambini.forEach { bambino ->
            val attivo = bambino.id == selezionatoId
            val colore = coloreBambino(bambino.coloreIndex)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = if (compatto) 44.dp else 56.dp)
                    .sticker(
                        sfondo = if (attivo) colore else Superficie,
                        raggio = if (compatto) 15.dp else 19.dp,
                        ombra = attivo && !compatto
                    )
                    .clickable(role = Role.Tab) { onSeleziona(bambino.id) }
                    .semantics { selected = attivo }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AvatarBambino(
                    foto = bambino.foto,
                    dimensione = if (compatto) 24.dp else 34.dp,
                    riempimento = if (attivo) Crema else colore,
                    tratto = if (attivo) InchiostroFaccina else Superficie,
                    bordo = if (attivo) Crema else Inchiostro
                )
                Text(
                    bambino.nome,
                    style = if (compatto) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    color = if (attivo) Crema else InkTenue,
                    modifier = Modifier.weight(1f, fill = false)
                )
                etichettaExtra(bambino)?.let { extra ->
                    Text(
                        extra,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (attivo) Crema else InkTenue
                    )
                }
            }
        }
    }
}

/**
 * Le linguette bambino della fascia in cima: foto tonda da 46dp, nome e lo
 * stato della giornata sotto. È la fascia stessa a fare da selettore, quindi
 * non serve più nessun selettore dentro il corpo di Oggi e Calendario.
 */
@Composable
fun LinguetteBambino(
    bambini: List<Bambino>,
    selezionatoId: Long?,
    onSeleziona: (Long) -> Unit,
    stato: (Bambino) -> String,
    modifier: Modifier = Modifier
) {
    if (bambini.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        bambini.forEach { bambino ->
            val attivo = bambino.id == selezionatoId
            val colore = coloreBambino(bambino.coloreIndex)
            val fondo = if (attivo) colore else InchiostroChiaro.copy(alpha = 0.07f)
            val bordo = if (attivo) InchiostroChiaro else InchiostroChiaro.copy(alpha = 0.26f)
            val testo = if (attivo) InchiostroChiaro else InchiostroChiaro.copy(alpha = 0.66f)
            val forma = RoundedCornerShape(Misure.raggioScheda)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Misure.toccoMinimo)
                    .background(fondo, forma)
                    .border(Misure.bordo, bordo, forma)
                    .clip(forma)
                    .clickable(role = Role.Tab) { onSeleziona(bambino.id) }
                    .semantics { selected = attivo }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AvatarBambino(
                    foto = bambino.foto,
                    dimensione = 46.dp,
                    riempimento = colore,
                    tratto = InchiostroFaccina,
                    bordo = if (attivo) InchiostroChiaro else InchiostroChiaro.copy(alpha = 0.4f)
                )
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        bambino.nome,
                        style = MaterialTheme.typography.headlineSmall,
                        color = testo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stato(bambino),
                        style = MaterialTheme.typography.labelMedium,
                        color = testo.copy(alpha = testo.alpha * 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** La barretta a segmenti che dice quanto manca a finire la giornata. */
@Composable
fun BarraAvanzamento(
    fatti: Int,
    totale: Int,
    colore: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(totale) { indice ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .sticker(
                            sfondo = if (indice < fatti) colore else Crema,
                            raggio = 5.dp,
                            ombra = false
                        )
                )
            }
        }
        Text(
            "$fatti su $totale",
            style = MaterialTheme.typography.labelMedium,
            color = InkSecondario
        )
    }
}
