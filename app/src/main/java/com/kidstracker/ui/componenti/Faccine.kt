package com.kidstracker.ui.componenti

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kidstracker.domain.Voto
import com.kidstracker.ui.tema.CremaFaccina
import com.kidstracker.ui.tema.Inchiostro
import com.kidstracker.ui.tema.InchiostroFaccina
import com.kidstracker.ui.tema.Superficie
import com.kidstracker.ui.tema.Misure
import com.kidstracker.ui.tema.Tratteggio
import com.kidstracker.ui.tema.coloreVoto

/**
 * La faccina: cerchio pieno, due occhi e una bocca che cambia forma.
 * La forma della bocca porta il significato anche senza il colore.
 */
@Composable
fun Faccina(
    voto: Voto?,
    modifier: Modifier = Modifier,
    dimensione: Dp = 34.dp,
    riempimento: Color? = null,
    tratto: Color = Inchiostro,
    conCerchio: Boolean = true
) {
    Canvas(modifier = modifier.size(dimensione)) {
        val d = size.minDimension
        val centro = Offset(d / 2f, d / 2f)
        val spessoreCerchio = d * 0.07f

        if (conCerchio) {
            if (riempimento != null) {
                drawCircle(riempimento, radius = d * 0.45f, center = centro)
            }
            drawCircle(
                tratto,
                radius = d * 0.45f - spessoreCerchio / 2f,
                center = centro,
                style = Stroke(width = spessoreCerchio)
            )
        }

        val raggioOcchio = d * 0.058f
        drawCircle(tratto, raggioOcchio, Offset(d * 0.35f, d * 0.41f))
        drawCircle(tratto, raggioOcchio, Offset(d * 0.65f, d * 0.41f))

        val bocca = Path()
        when (voto) {
            Voto.SI -> {
                bocca.moveTo(d * 0.31f, d * 0.60f)
                bocca.quadraticBezierTo(d * 0.5f, d * 0.80f, d * 0.69f, d * 0.60f)
            }
            Voto.COSI_COSI -> {
                bocca.moveTo(d * 0.34f, d * 0.655f)
                bocca.lineTo(d * 0.66f, d * 0.655f)
            }
            Voto.NO -> {
                bocca.moveTo(d * 0.31f, d * 0.72f)
                bocca.quadraticBezierTo(d * 0.5f, d * 0.52f, d * 0.69f, d * 0.72f)
            }
            null -> {
                bocca.moveTo(d * 0.37f, d * 0.655f)
                bocca.lineTo(d * 0.63f, d * 0.655f)
            }
        }
        drawPath(
            bocca,
            color = tratto,
            style = Stroke(width = d * 0.082f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Le tre faccine di una riga. Non selezionate sono slot tratteggiati vuoti;
 * quella scelta è un adesivo attaccato, storto di tre gradi.
 */
@Composable
fun SelettoreFaccine(
    etichetta: String,
    valore: Voto?,
    onCambia: (Voto) -> Unit,
    modifier: Modifier = Modifier,
    lato: Dp = 40.dp
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            etichetta,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            listOf(Voto.SI, Voto.COSI_COSI, Voto.NO).forEach { voto ->
                val selezionato = valore == voto
                Box(
                    modifier = Modifier
                        .size(lato)
                        .then(
                            if (selezionato) {
                                Modifier
                                    .rotate(-3f)
                                    .ombraPiena(17.dp, dx = 2.dp, dy = 3.dp)
                                    // Fisso, non il fondo pagina: la faccina scelta
                                    // resta su carta chiara anche col tema scuro.
                                    .background(CremaFaccina, RoundedCornerShape(17.dp))
                                    .border(Misure.bordo, Inchiostro, RoundedCornerShape(17.dp))
                            } else {
                                Modifier.bordoTratteggiato(17.dp, Tratteggio)
                            }
                        )
                        .clip(RoundedCornerShape(17.dp))
                        .clickable(
                            role = Role.RadioButton,
                            onClickLabel = "$etichetta: ${voto.etichetta}"
                        ) { onCambia(voto) }
                        .semantics { selected = selezionato },
                    contentAlignment = Alignment.Center
                ) {
                    Faccina(
                        voto = voto,
                        dimensione = lato * 0.66f,
                        riempimento = if (selezionato) coloreVoto(voto) else Superficie,
                        tratto = if (selezionato) InchiostroFaccina else Tratteggio,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                }
            }
        }
    }
}
