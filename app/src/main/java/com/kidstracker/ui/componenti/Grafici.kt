package com.kidstracker.ui.componenti

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidstracker.ui.tema.Crema
import com.kidstracker.ui.tema.Figtree
import com.kidstracker.ui.tema.Griglia
import com.kidstracker.ui.tema.Inchiostro
import com.kidstracker.ui.tema.InkSecondario
import com.kidstracker.ui.tema.InkTerziario
import com.kidstracker.ui.tema.Misure
import com.kidstracker.ui.tema.Rosso
import com.kidstracker.ui.tema.RossoScuro
import com.kidstracker.ui.tema.SabbiaTenue
import com.kidstracker.ui.tema.VerdeScuro
import kotlin.math.roundToInt

data class SerieGrafico(
    val nome: String,
    val colore: Color,
    val valori: List<Double?>
)

/**
 * Linee su fondo bianco, asse unico 0–100. Trascinando il dito sul grafico
 * si legge il valore di quel giorno per entrambi i bambini.
 */
@Composable
fun GraficoLinee(
    serie: List<SerieGrafico>,
    etichette: List<String>,
    modifier: Modifier = Modifier,
    altezza: Dp = 156.dp
) {
    val misuratore = rememberTextMeasurer()
    var selezione by remember { mutableIntStateOf(-1) }
    val quanti = serie.firstOrNull()?.valori?.size ?: 0
    if (quanti < 2) {
        Text(
            "Servono almeno due giorni segnati per disegnare la linea.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondario,
            modifier = modifier.padding(vertical = 12.dp)
        )
        return
    }

    val stileAsse = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        color = InkSecondario
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Riga di lettura: legenda quando non si sta trascinando, valori quando sì.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selezione in 0 until quanti) {
                Text(
                    etichette.getOrElse(selezione) { "" },
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondario
                )
                serie.forEach { s ->
                    PallinoEtichetta(
                        colore = s.colore,
                        testo = s.valori.getOrNull(selezione)
                            ?.let { "${it.roundToInt()}%" } ?: "–"
                    )
                }
            } else {
                serie.forEach { s -> PallinoEtichetta(s.colore, s.nome) }
            }
        }

        val cGriglia = Griglia
        val cAsse = SabbiaTenue
        val cInk = Inchiostro

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(altezza)
                .pointerInput(quanti) {
                    detectTapGestures(
                        onTap = { posizione ->
                            selezione = indiceDaX(posizione.x, size.width.toFloat(), quanti)
                        }
                    )
                }
                .pointerInput(quanti) {
                    detectHorizontalDragGestures(
                        onDragEnd = { selezione = -1 },
                        onDragCancel = { selezione = -1 }
                    ) { cambio, _ ->
                        selezione = indiceDaX(cambio.position.x, size.width.toFloat(), quanti)
                    }
                }
        ) {
            val sinistra = 30f
            val destra = size.width - 34f
            val alto = 8f
            val basso = size.height - 22f
            val altezzaPlot = basso - alto

            fun x(indice: Int) = sinistra + (destra - sinistra) * indice / (quanti - 1).toFloat()
            fun y(valore: Double) = basso - (valore / 100.0).toFloat() * altezzaPlot

            listOf(100.0, 50.0, 0.0).forEach { livello ->
                val yy = y(livello)
                drawLine(
                    if (livello == 0.0) cAsse else cGriglia,
                    Offset(sinistra, yy),
                    Offset(size.width - 4f, yy),
                    strokeWidth = 1.5f
                )
                val testo = misuratore.measure("${livello.toInt()}", stileAsse)
                drawText(
                    textLayoutResult = testo,
                    topLeft = Offset(sinistra - 6f - testo.size.width, yy - testo.size.height / 2f)
                )
            }

            if (selezione in 0 until quanti) {
                drawLine(
                    cInk,
                    Offset(x(selezione), alto),
                    Offset(x(selezione), basso),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            }

            serie.forEach { s ->
                val percorso = Path()
                var iniziato = false
                s.valori.forEachIndexed { indice, valore ->
                    if (valore == null) {
                        iniziato = false
                        return@forEachIndexed
                    }
                    val punto = Offset(x(indice), y(valore))
                    if (!iniziato) {
                        percorso.moveTo(punto.x, punto.y)
                        iniziato = true
                    } else {
                        percorso.lineTo(punto.x, punto.y)
                    }
                }
                drawPath(
                    percorso,
                    s.colore,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Pallino e nome all'estremità: identità anche senza la legenda.
                val ultimoIndice = s.valori.indexOfLast { it != null }
                if (ultimoIndice >= 0) {
                    val valore = s.valori[ultimoIndice]!!
                    val centro = Offset(x(ultimoIndice), y(valore))
                    drawCircle(s.colore, radius = 5.5.dp.toPx(), center = centro)
                    drawCircle(
                        cInk,
                        radius = 5.5.dp.toPx(),
                        center = centro,
                        style = Stroke(2.dp.toPx())
                    )
                }
            }

            // Prima e ultima data sotto l'asse.
            etichette.firstOrNull()?.let {
                drawText(misuratore.measure(it, stileAsse), topLeft = Offset(sinistra, basso + 6f))
            }
            etichette.lastOrNull()?.let {
                val misura = misuratore.measure(it, stileAsse)
                drawText(misura, topLeft = Offset(destra - misura.size.width, basso + 6f))
            }
        }
    }
}

private fun indiceDaX(x: Float, larghezza: Float, quanti: Int): Int {
    if (quanti <= 1 || larghezza <= 0f) return -1
    val sinistra = 30f
    val destra = larghezza - 34f
    val relativo = ((x - sinistra) / (destra - sinistra)) * (quanti - 1)
    return relativo.roundToInt().coerceIn(0, quanti - 1)
}

@Composable
private fun PallinoEtichetta(colore: Color, testo: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .sticker(colore, 6.dp, ombra = false)
        )
        Text(testo, style = MaterialTheme.typography.labelMedium, color = Inchiostro)
    }
}

/** Serie minuscola per una sola categoria: barre piene, una per giorno. */
@Composable
fun MiniBarre(
    valori: List<Double?>,
    colore: Color,
    modifier: Modifier = Modifier
) {
    val cGriglia = Griglia
    Canvas(modifier = modifier.height(34.dp)) {
        val base = size.height - 2f
        drawLine(cGriglia, Offset(0f, base), Offset(size.width, base), strokeWidth = 1.5f)
        if (valori.isEmpty()) return@Canvas
        val passo = size.width / valori.size
        val larghezza = (passo - 3f).coerceAtLeast(2f)
        valori.forEachIndexed { indice, valore ->
            if (valore == null) return@forEachIndexed
            val altezza = ((valore / 100.0).toFloat() * (base - 2f)).coerceAtLeast(2f)
            drawRoundRect(
                color = colore,
                topLeft = Offset(indice * passo, base - altezza),
                size = Size(larghezza, altezza),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}

data class BarraGiorno(val etichetta: String, val valore: Double)

/**
 * Barre bordate di nero, una per giorno di scuola. Sono etichettate solo
 * la più bassa e la più alta: il resto si legge dalla forma.
 */
@Composable
fun BarreGiorni(
    barre: List<BarraGiorno>,
    colore: Color,
    modifier: Modifier = Modifier,
    coloreMinimo: Color = Rosso
) {
    if (barre.isEmpty()) {
        Text(
            "Ancora pochi giorni segnati per confrontare i giorni della settimana.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondario,
            modifier = modifier
        )
        return
    }
    val misuratore = rememberTextMeasurer()
    val minimo = barre.minOf { it.valore }
    val massimo = barre.maxOf { it.valore }
    val stileValore = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        color = Inchiostro
    )
    val cAsse = SabbiaTenue
    val cInk = Inchiostro

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
        ) {
            val base = size.height - 2f
            val alto = 20f
            drawLine(cAsse, Offset(0f, base), Offset(size.width, base), strokeWidth = 1.5f)
            val passo = size.width / barre.size
            val larghezza = (passo * 0.62f).coerceAtMost(46.dp.toPx())
            barre.forEachIndexed { indice, barra ->
                val altezza = ((barra.valore / 100.0).toFloat() * (base - alto)).coerceAtLeast(3f)
                val x = indice * passo + (passo - larghezza) / 2f
                val y = base - altezza
                drawRoundRect(
                    color = if (barra.valore == minimo && barre.size > 1) coloreMinimo else colore,
                    topLeft = Offset(x, y),
                    size = Size(larghezza, altezza),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
                drawRoundRect(
                    color = cInk,
                    topLeft = Offset(x, y),
                    size = Size(larghezza, altezza),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    style = Stroke(Misure.bordo.toPx())
                )
                if (barra.valore == minimo || barra.valore == massimo) {
                    val misura = misuratore.measure("${barra.valore.roundToInt()}%", stileValore)
                    drawText(
                        misura,
                        topLeft = Offset(
                            x + larghezza / 2f - misura.size.width / 2f,
                            (y - misura.size.height - 3f).coerceAtLeast(0f)
                        )
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            barre.forEach { barra ->
                Text(
                    barra.etichetta,
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Barra orizzontale piena dentro una traccia bordata. */
@Composable
fun BarraOrizzontale(
    percentuale: Double,
    colore: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .sticker(Crema, 8.dp, ombra = false),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((percentuale / 100.0).toFloat().coerceIn(0f, 1f))
                .height(12.dp)
                .padding(start = 1.dp)
                .sticker(colore, 6.dp, ombra = false, bordo = Color.Transparent)
        )
    }
}

/**
 * Barra che parte dal centro: a destra se è avanti il primo bambino,
 * a sinistra se è avanti il secondo.
 */
@Composable
fun BarraDivergente(
    delta: Double,
    coloreDestra: Color,
    coloreSinistra: Color,
    modifier: Modifier = Modifier
) {
    val cTraccia = Crema
    val cInk = Inchiostro
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val raggio = CornerRadius(7f, 7f)
        drawRoundRect(cTraccia, size = size, cornerRadius = raggio)
        drawRoundRect(
            cInk,
            size = size,
            cornerRadius = raggio,
            style = Stroke(Misure.bordo.toPx())
        )
        val centro = size.width / 2f
        val massimo = centro - 4f
        val larghezza = ((kotlin.math.abs(delta) / 40.0).toFloat() * massimo).coerceIn(0f, massimo)
        if (larghezza > 1f) {
            val x = if (delta >= 0) centro else centro - larghezza
            drawRoundRect(
                color = if (delta >= 0) coloreDestra else coloreSinistra,
                topLeft = Offset(x, 3f),
                size = Size(larghezza, size.height - 6f),
                cornerRadius = CornerRadius(5f, 5f)
            )
        }
        drawLine(
            cInk,
            Offset(centro, -2f),
            Offset(centro, size.height + 2f),
            strokeWidth = Misure.bordo.toPx()
        )
    }
}

/** Striscia di giorni: un quadratino colorato per giornata. */
@Composable
fun StrisciaGiorni(
    colori: List<Color>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        colori.forEach { colore ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .sticker(colore, 8.dp, ombra = false)
            )
        }
    }
}

@Composable
fun EtichettaDelta(delta: Double?, modifier: Modifier = Modifier) {
    if (delta == null) {
        Text("–", style = MaterialTheme.typography.labelLarge, color = InkTerziario, modifier = modifier)
        return
    }
    val su = delta >= 0
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        FrecciaDelta(su)
        Text(
            (if (su) "+" else "−") + "${kotlin.math.abs(delta).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = if (su) VerdeScuro else RossoScuro
        )
    }
}

@Composable
private fun FrecciaDelta(versoAlto: Boolean) {
    val colore = if (versoAlto) VerdeScuro else RossoScuro
    Canvas(modifier = Modifier.size(13.dp)) {
        val d = size.minDimension
        val s = d * 0.16f
        val cima = if (versoAlto) d * 0.18f else d * 0.82f
        val fondo = if (versoAlto) d * 0.82f else d * 0.18f
        drawLine(colore, Offset(d / 2f, fondo), Offset(d / 2f, cima), s, StrokeCap.Round)
        val punta = Path().apply {
            moveTo(d * 0.24f, if (versoAlto) d * 0.46f else d * 0.54f)
            lineTo(d / 2f, cima)
            lineTo(d * 0.76f, if (versoAlto) d * 0.46f else d * 0.54f)
        }
        drawPath(punta, colore, style = Stroke(s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
