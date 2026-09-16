package com.kidstracker.ui.componenti

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.kidstracker.domain.Bambino
import com.kidstracker.domain.Voto
import com.kidstracker.ui.tema.Crema
import com.kidstracker.ui.tema.Inchiostro
import com.kidstracker.ui.tema.InkSecondario
import com.kidstracker.ui.tema.InkTenue
import com.kidstracker.ui.tema.Misure
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .drawBehind {
                drawRect(
                    color = Inchiostro,
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
 * Quella attiva prende il colore del bambino, con la faccina in negativo.
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
                        sfondo = if (attivo) colore else Color.White,
                        raggio = if (compatto) 15.dp else 19.dp,
                        ombra = attivo && !compatto
                    )
                    .clickable(role = Role.Tab) { onSeleziona(bambino.id) }
                    .semantics { selected = attivo }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Faccina(
                    voto = Voto.SI,
                    dimensione = if (compatto) 20.dp else 26.dp,
                    riempimento = if (attivo) Crema else colore,
                    tratto = if (attivo) Inchiostro else Color.White
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
