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
import androidx.compose.foundation.layout.height
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
import com.kidstracker.ui.tema.Giallo
import com.kidstracker.ui.tema.Inchiostro
import com.kidstracker.ui.tema.InchiostroChiaro
import com.kidstracker.ui.tema.inchiostroSu
import com.kidstracker.ui.tema.InkSecondario
import com.kidstracker.ui.tema.InkTerziario
import com.kidstracker.ui.tema.Misure
import com.kidstracker.ui.tema.Ombra
import com.kidstracker.ui.tema.Prugna
import com.kidstracker.ui.tema.PrugnaChiara
import com.kidstracker.ui.tema.PrugnaSpenta
import com.kidstracker.ui.tema.Superficie
import com.kidstracker.ui.tema.Tratteggio

/** L'ombra piena spostata: è il dettaglio che fa sembrare gli oggetti ritagliati. */
@Composable
fun Modifier.ombraPiena(
    raggio: Dp,
    colore: Color = Ombra,
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
@Composable
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

@Composable
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
    sfondo: Color = Superficie,
    raggio: Dp = Misure.raggioScheda,
    ombra: Boolean = true,
    padding: Dp = 10.dp,
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
 * Scheda con la testata colorata a tutta larghezza e il corpo neutro:
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
    val bordo = Inchiostro
    val spessoreBordo = Misure.bordo
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sticker(Superficie)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tintaTestata)
                .drawBehind {
                    drawRect(
                        color = bordo,
                        topLeft = Offset(0f, size.height - spessoreBordo.toPx()),
                        size = Size(size.width, spessoreBordo.toPx())
                    )
                }
                .padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 9.dp)
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
        Column(modifier = Modifier.padding(10.dp), content = contenuto)
    }
}

@Composable
fun BottoneSticker(
    testo: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sfondo: Color = Giallo,
    contenutoColore: Color? = null,
    abilitato: Boolean = true
) {
    val tinta = contenutoColore ?: inchiostroSu(sfondo)
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .sticker(if (abilitato) sfondo else Superficie, 12.dp, ombra = abilitato)
            .clickable(enabled = abilitato, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            testo,
            style = MaterialTheme.typography.headlineSmall,
            color = if (abilitato) tinta else InkSecondario,
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
    coloreSelezione: Color = Inchiostro,
    coloreTestoSelezionato: Color? = null
) {
    val tintaScelta = coloreTestoSelezionato ?: inchiostroSu(coloreSelezione)
    Box(
        modifier = modifier
            .heightIn(min = 46.dp)
            .sticker(
                sfondo = if (selezionata) coloreSelezione else Superficie,
                raggio = 10.dp,
                ombra = false
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            testo,
            style = MaterialTheme.typography.labelLarge,
            color = if (selezionata) tintaScelta else Inchiostro,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * La fascia in cima a ogni schermata. Titolo e sottotitolo stanno in colonna;
 * sotto, se presente, la riga delle linguette bambino (foto, nome, stato della
 * giornata) — è lei che chiude la fascia, non più i coriandoli.
 */
@Composable
fun IntestazionePrugna(
    titolo: String,
    sottotitolo: String? = null,
    modifier: Modifier = Modifier,
    grande: Boolean = false,
    azioni: (@Composable RowScope.() -> Unit)? = null,
    linguette: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Prugna, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .statusBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)
    ) {
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
                    color = InchiostroChiaro
                )
                if (sottotitolo != null) {
                    Text(
                        sottotitolo,
                        style = MaterialTheme.typography.titleSmall,
                        color = PrugnaChiara,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
            if (azioni != null) {
                Spacer(Modifier.size(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = azioni
                )
            }
        }
        if (linguette != null) {
            Spacer(Modifier.height(12.dp))
            linguette()
        }
    }
}

/** Bottone tondo col solo contorno chiaro, per le azioni dentro la fascia. */
@Composable
fun BottoneContornato(
    onClick: () -> Unit,
    descrizione: String,
    modifier: Modifier = Modifier,
    attivo: Boolean = true,
    contenuto: @Composable (Color) -> Unit
) {
    val colore = if (attivo) InchiostroChiaro else PrugnaSpenta
    Box(
        modifier = modifier
            .size(34.dp)
            .border(Misure.bordo, colore, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .clickable(enabled = attivo, role = Role.Button, onClickLabel = descrizione, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        contenuto(colore)
    }
}
