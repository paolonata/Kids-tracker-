package com.kidstracker.ui.componenti

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kidstracker.ui.tema.Coriandoli
import com.kidstracker.ui.tema.Crema
import com.kidstracker.ui.tema.Giallo
import com.kidstracker.ui.tema.Inchiostro
import com.kidstracker.ui.tema.InkSecondario
import com.kidstracker.ui.tema.InkTerziario
import com.kidstracker.ui.tema.Misure
import com.kidstracker.ui.tema.Prugna
import com.kidstracker.ui.tema.PrugnaChiara
import com.kidstracker.ui.tema.PrugnaSpenta
import com.kidstracker.ui.tema.Tratteggio

/** L'ombra piena spostata: è il dettaglio che fa sembrare gli oggetti ritagliati. */
fun Modifier.ombraPiena(
    raggio: Dp,
    colore: Color = Inchiostro,
    dx: Dp = Misure.ombraX,
    dy: Dp = Misure.ombraY
): Modifier = this.drawBehind {
    drawRoundRect(
        color = colore,
        topLeft = Offset(dx.toPx(), dy.toPx()),
        size = size,
        cornerRadius = CornerRadius(raggio.toPx(), raggio.toPx())
    )
}

/** Il contorno tratteggiato di uno slot vuoto dell'album. */
fun Modifier.bordoTratteggiato(
    raggio: Dp,
    colore: Color = Tratteggio,
    spessore: Dp = Misure.bordo
): Modifier = this.drawBehind {
    val s = spessore.toPx()
    drawRoundRect(
        color = colore,
        topLeft = Offset(s / 2f, s / 2f),
        size = Size(size.width - s, size.height - s),
        cornerRadius = CornerRadius(raggio.toPx(), raggio.toPx()),
        style = Stroke(
            width = s,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(s * 3f, s * 2.2f))
        )
    )
}

fun Modifier.sticker(
    sfondo: Color,
    raggio: Dp = Misure.raggioScheda,
    ombra: Boolean = true,
    bordo: Color = Inchiostro
): Modifier {
    val forma = RoundedCornerShape(raggio)
    return this
        .then(if (ombra) Modifier.ombraPiena(raggio) else Modifier)
        .background(sfondo, forma)
        .border(Misure.bordo, bordo, forma)
        .clip(forma)
}

@Composable
fun SchedaSticker(
    modifier: Modifier = Modifier,
    sfondo: Color = Color.White,
    raggio: Dp = Misure.raggioScheda,
    ombra: Boolean = true,
    padding: Dp = 15.dp,
    contenuto: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sticker(sfondo, raggio, ombra)
            .padding(padding),
        content = contenuto
    )
}

/**
 * Scheda con la testata colorata a tutta larghezza e il corpo bianco:
 * il colore sta nel titolo, l'area dei grafici resta pulita.
 */
@Composable
fun SchedaConTestata(
    titolo: String,
    sottotitolo: String?,
    tintaTestata: Color,
    modifier: Modifier = Modifier,
    extraTestata: (@Composable ColumnScope.() -> Unit)? = null,
    contenuto: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sticker(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tintaTestata)
                .drawBehind {
                    drawRect(
                        color = Inchiostro,
                        topLeft = Offset(0f, size.height - Misure.bordo.toPx()),
                        size = Size(size.width, Misure.bordo.toPx())
                    )
                }
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 13.dp)
        ) {
            Text(titolo, style = MaterialTheme.typography.headlineSmall)
            if (sottotitolo != null) {
                Text(
                    sottotitolo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTerziario,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            extraTestata?.invoke(this)
        }
        Column(modifier = Modifier.padding(14.dp), content = contenuto)
    }
}

@Composable
fun BottoneSticker(
    testo: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sfondo: Color = Giallo,
    contenutoColore: Color = Inchiostro,
    abilitato: Boolean = true
) {
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .sticker(if (abilitato) sfondo else Color.White, 20.dp, ombra = abilitato)
            .clickable(enabled = abilitato, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            testo,
            style = MaterialTheme.typography.headlineSmall,
            color = if (abilitato) contenutoColore else InkSecondario,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PillolaScelta(
    testo: String,
    selezionata: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    coloreSelezione: Color = Inchiostro
) {
    Box(
        modifier = modifier
            .heightIn(min = 46.dp)
            .sticker(
                sfondo = if (selezionata) coloreSelezione else Color.White,
                raggio = 15.dp,
                ombra = false
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            testo,
            style = MaterialTheme.typography.labelLarge,
            color = if (selezionata) Crema else Inchiostro,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FilaCoriandoli(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Coriandoli.forEach { colore ->
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(colore, RoundedCornerShape(50))
            )
        }
    }
}

/** La fascia prugna in cima a ogni schermata. */
@Composable
fun IntestazionePrugna(
    titolo: String,
    sottotitolo: String? = null,
    modifier: Modifier = Modifier,
    grande: Boolean = false,
    rigaSopra: (@Composable RowScope.() -> Unit)? = null,
    azione: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Prugna, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 30.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (rigaSopra != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    content = rigaSopra
                )
                Spacer(Modifier.size(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        titolo,
                        style = if (grande) {
                            MaterialTheme.typography.displayLarge
                        } else {
                            MaterialTheme.typography.displayMedium
                        },
                        color = Crema
                    )
                    if (sottotitolo != null) {
                        Text(
                            sottotitolo,
                            style = MaterialTheme.typography.titleSmall,
                            color = PrugnaChiara,
                            modifier = Modifier.padding(top = 7.dp)
                        )
                    }
                }
                azione?.invoke()
            }
        }
        FilaCoriandoli(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 2.dp, bottom = 1.dp)
        )
    }
}

/** Bottone tondo con il solo contorno chiaro, per le azioni dentro la fascia prugna. */
@Composable
fun BottoneContornato(
    onClick: () -> Unit,
    descrizione: String,
    modifier: Modifier = Modifier,
    attivo: Boolean = true,
    contenuto: @Composable (Color) -> Unit
) {
    val colore = if (attivo) Crema else PrugnaSpenta
    Box(
        modifier = modifier
            .size(44.dp)
            .border(Misure.bordo, colore, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .clickable(enabled = attivo, role = Role.Button, onClickLabel = descrizione, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        contenuto(colore)
    }
}
