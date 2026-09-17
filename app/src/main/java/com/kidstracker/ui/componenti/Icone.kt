package com.kidstracker.ui.componenti

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Icone disegnate a mano con Canvas invece che importate da una libreria:
 * lo spessore del tratto è lo stesso dei bordi delle schede, così l'insieme
 * resta coerente, e l'app non si porta dietro il pacchetto delle icone Material.
 */

@Composable
private fun IconaCanvas(
    dimensione: Dp,
    modifier: Modifier = Modifier,
    disegno: DrawScope.(Float, Float) -> Unit
) {
    Canvas(modifier = modifier.size(dimensione)) {
        val d = size.minDimension
        disegno(d, d * 0.092f)
    }
}

@Composable
fun IconaSole(colore: Color, modifier: Modifier = Modifier, dimensione: Dp = 21.dp) {
    IconaCanvas(dimensione, modifier) { d, s ->
        drawCircle(colore, radius = d * 0.19f, center = Offset(d / 2f, d / 2f), style = Stroke(s))
        repeat(8) { i ->
            val angolo = Math.PI / 4.0 * i
            val dentro = d * 0.30f
            val fuori = d * 0.44f
            drawLine(
                colore,
                Offset(d / 2f + (cos(angolo) * dentro).toFloat(), d / 2f + (sin(angolo) * dentro).toFloat()),
                Offset(d / 2f + (cos(angolo) * fuori).toFloat(), d / 2f + (sin(angolo) * fuori).toFloat()),
                strokeWidth = s,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun IconaCalendario(colore: Color, modifier: Modifier = Modifier, dimensione: Dp = 21.dp) {
    IconaCanvas(dimensione, modifier) { d, s ->
        drawRoundRect(
            colore,
            topLeft = Offset(d * 0.14f, d * 0.22f),
            size = androidx.compose.ui.geometry.Size(d * 0.72f, d * 0.64f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(d * 0.13f, d * 0.13f),
            style = Stroke(s)
        )
        drawLine(colore, Offset(d * 0.14f, d * 0.42f), Offset(d * 0.86f, d * 0.42f), s)
        drawLine(colore, Offset(d * 0.35f, d * 0.14f), Offset(d * 0.35f, d * 0.28f), s, StrokeCap.Round)
        drawLine(colore, Offset(d * 0.65f, d * 0.14f), Offset(d * 0.65f, d * 0.28f), s, StrokeCap.Round)
    }
}

@Composable
fun IconaGrafico(colore: Color, modifier: Modifier = Modifier, dimensione: Dp = 21.dp) {
    IconaCanvas(dimensione, modifier) { d, s ->
        drawLine(colore, Offset(d * 0.15f, d * 0.85f), Offset(d * 0.85f, d * 0.85f), s, StrokeCap.Round)
        val linea = Path().apply {
            moveTo(d * 0.17f, d * 0.65f)
            lineTo(d * 0.36f, d * 0.44f)
            lineTo(d * 0.51f, d * 0.57f)
            lineTo(d * 0.77f, d * 0.27f)
        }
        drawPath(linea, colore, style = Stroke(s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconaLampadina(colore: Color, modifier: Modifier = Modifier, dimensione: Dp = 21.dp) {
    IconaCanvas(dimensione, modifier) { d, s ->
        drawCircle(colore, radius = d * 0.27f, center = Offset(d / 2f, d * 0.40f), style = Stroke(s))
        drawLine(colore, Offset(d * 0.38f, d * 0.68f), Offset(d * 0.62f, d * 0.68f), s, StrokeCap.Round)
        drawLine(colore, Offset(d * 0.40f, d * 0.79f), Offset(d * 0.60f, d * 0.79f), s, StrokeCap.Round)
        drawLine(colore, Offset(d * 0.44f, d * 0.90f), Offset(d * 0.56f, d * 0.90f), s, StrokeCap.Round)
    }
}

@Composable
fun IconaFreccia(
    colore: Color,
    modifier: Modifier = Modifier,
    dimensione: Dp = 18.dp,
    versoDestra: Boolean = false
) {
    IconaCanvas(dimensione, modifier) { d, s ->
        val punta = if (versoDestra) d * 0.68f else d * 0.32f
        val coda = if (versoDestra) d * 0.34f else d * 0.66f
        val freccia = Path().apply {
            moveTo(coda, d * 0.20f)
            lineTo(punta, d * 0.50f)
            lineTo(coda, d * 0.80f)
        }
        drawPath(freccia, colore, style = Stroke(s * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconaImpostazioni(
    colore: Color,
    modifier: Modifier = Modifier,
    dimensione: Dp = 20.dp,
    sfondo: Color = Color.Transparent
) {
    IconaCanvas(dimensione, modifier) { d, s ->
        listOf(0.26f, 0.5f, 0.74f).forEachIndexed { indice, y ->
            drawLine(colore, Offset(d * 0.13f, d * y), Offset(d * 0.87f, d * y), s, StrokeCap.Round)
            val x = when (indice) {
                0 -> 0.66f
                1 -> 0.36f
                else -> 0.58f
            }
            if (sfondo != Color.Transparent) {
                drawCircle(sfondo, radius = d * 0.11f, center = Offset(d * x, d * y))
            }
            drawCircle(colore, radius = d * 0.14f, center = Offset(d * x, d * y), style = Stroke(s))
        }
    }
}

@Composable
fun IconaOrologio(colore: Color, modifier: Modifier = Modifier, dimensione: Dp = 18.dp) {
    IconaCanvas(dimensione, modifier) { d, s ->
        drawCircle(colore, radius = d * 0.38f, center = Offset(d / 2f, d / 2f), style = Stroke(s))
        drawLine(colore, Offset(d * 0.5f, d * 0.30f), Offset(d * 0.5f, d * 0.52f), s, StrokeCap.Round)
        drawLine(colore, Offset(d * 0.5f, d * 0.52f), Offset(d * 0.66f, d * 0.60f), s, StrokeCap.Round)
    }
}

@Composable
fun IconaPiu(colore: Color, modifier: Modifier = Modifier, dimensione: Dp = 20.dp) {
    IconaCanvas(dimensione, modifier) { d, s ->
        drawLine(colore, Offset(d * 0.5f, d * 0.18f), Offset(d * 0.5f, d * 0.82f), s, StrokeCap.Round)
        drawLine(colore, Offset(d * 0.18f, d * 0.5f), Offset(d * 0.82f, d * 0.5f), s, StrokeCap.Round)
    }
}

@Composable
fun IconaCestino(colore: Color, modifier: Modifier = Modifier, dimensione: Dp = 20.dp) {
    IconaCanvas(dimensione, modifier) { d, s ->
        drawLine(colore, Offset(d * 0.16f, d * 0.28f), Offset(d * 0.84f, d * 0.28f), s, StrokeCap.Round)
        drawLine(colore, Offset(d * 0.38f, d * 0.28f), Offset(d * 0.38f, d * 0.18f), s, StrokeCap.Round)
        drawLine(colore, Offset(d * 0.62f, d * 0.28f), Offset(d * 0.62f, d * 0.18f), s, StrokeCap.Round)
        val cestino = Path().apply {
            moveTo(d * 0.24f, d * 0.32f)
            lineTo(d * 0.30f, d * 0.86f)
            lineTo(d * 0.70f, d * 0.86f)
            lineTo(d * 0.76f, d * 0.32f)
        }
        drawPath(cestino, colore, style = Stroke(s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconaTermometro(colore: Color, modifier: Modifier = Modifier, dimensione: Dp = 16.dp) {
    IconaCanvas(dimensione, modifier) { d, s ->
        drawCircle(colore, radius = d * 0.19f, center = Offset(d * 0.5f, d * 0.76f), style = Stroke(s))
        drawLine(colore, Offset(d * 0.5f, d * 0.16f), Offset(d * 0.5f, d * 0.58f), s, StrokeCap.Round)
    }
}
