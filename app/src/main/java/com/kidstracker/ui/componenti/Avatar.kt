package com.kidstracker.ui.componenti

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.kidstracker.data.Foto
import com.kidstracker.domain.Voto
import com.kidstracker.ui.tema.Inchiostro
import com.kidstracker.ui.tema.Misure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Le foto sono due e piccole: tenerle in memoria evita di rileggerle dal disco
 * a ogni ricomposizione. La chiave è il nome del file, che cambia a ogni nuova
 * foto, quindi non c'è modo di mostrarne una vecchia.
 */
private val memoria = mutableMapOf<String, Bitmap>()

/**
 * La faccia del bambino: la sua foto se ne ha scelta una, altrimenti la faccina
 * colorata di prima. Stessa dimensione e stesso posto nelle due varianti, così
 * il resto del layout non si accorge della differenza.
 */
@Composable
fun AvatarBambino(
    foto: String?,
    dimensione: Dp,
    riempimento: Color,
    tratto: Color,
    modifier: Modifier = Modifier,
    bordo: Color = Inchiostro
) {
    val contesto = LocalContext.current
    val immagine by produceState<Bitmap?>(initialValue = foto?.let { memoria[it] }, foto) {
        val nome = foto
        if (nome.isNullOrBlank()) {
            value = null
            return@produceState
        }
        memoria[nome]?.let {
            value = it
            return@produceState
        }
        val caricata = withContext(Dispatchers.IO) { Foto.carica(contesto, nome) }
        if (caricata != null) memoria[nome] = caricata
        value = caricata
    }

    Ritratto(immagine, dimensione, riempimento, tratto, bordo, modifier)
}

/**
 * L'avatar di un bambino che non esiste ancora: durante l'onboarding la foto è
 * solo un [Uri] scelto dalla galleria, e il file vero si scrive più tardi,
 * quando la scheda ha un id.
 */
@Composable
fun AvatarDaUri(
    origine: Uri?,
    dimensione: Dp,
    riempimento: Color,
    tratto: Color,
    modifier: Modifier = Modifier,
    bordo: Color = Inchiostro
) {
    val contesto = LocalContext.current
    val immagine by produceState<Bitmap?>(initialValue = null, origine) {
        val scelta = origine
        value = if (scelta == null) {
            null
        } else {
            withContext(Dispatchers.IO) { Foto.anteprima(contesto, scelta) }
        }
    }

    Ritratto(immagine, dimensione, riempimento, tratto, bordo, modifier)
}

/** La foto tonda e bordata, o la faccina di riserva se la foto non c'è. */
@Composable
private fun Ritratto(
    bitmap: Bitmap?,
    dimensione: Dp,
    riempimento: Color,
    tratto: Color,
    bordo: Color,
    modifier: Modifier
) {
    if (bitmap == null) {
        Faccina(
            voto = Voto.SI,
            dimensione = dimensione,
            riempimento = riempimento,
            tratto = tratto,
            modifier = modifier
        )
        return
    }

    val spessore = Misure.bordo
    Box(
        modifier = modifier.size(dimensione),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(dimensione)
                .clip(CircleShape)
                // Il bordo va disegnato sopra l'immagine: un border() sotto il
                // clip finirebbe ritagliato via a metà.
                .drawWithContent {
                    drawContent()
                    val larghezza = spessore.toPx()
                    drawCircle(
                        color = bordo,
                        radius = (size.minDimension - larghezza) / 2f,
                        style = Stroke(larghezza)
                    )
                }
        )
    }
}
