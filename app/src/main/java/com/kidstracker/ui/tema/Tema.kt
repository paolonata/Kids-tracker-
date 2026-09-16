package com.kidstracker.ui.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidstracker.R
import com.kidstracker.domain.Giudizio
import com.kidstracker.domain.Voto

// ---- colori --------------------------------------------------------------------------
// La palette è quella del prototipo. I due colori dei bambini sono stati verificati
// per restare distinguibili anche con daltonismo (ΔE 15.9) e per non confondersi
// con il rosso/giallo/verde delle faccine.

val Crema = Color(0xFFFFF6E9)
val Inchiostro = Color(0xFF231428)
val Prugna = Color(0xFF3B1F49)
val PrugnaChiara = Color(0xFFD7B9E8)
val PrugnaSpenta = Color(0xFF7A5E88)

val InkSecondario = Color(0xFF6B5A73)
val InkTerziario = Color(0xFF4A3B52)
val InkTenue = Color(0xFF8B7A93)
val Tratteggio = Color(0xFFC7BCCD)
val SabbiaTenue = Color(0xFFC9BBAA)

val Verde = Color(0xFF17A05E)
val Giallo = Color(0xFFF5B324)
val Rosso = Color(0xFFE5503C)
val VerdeScuro = Color(0xFF0F7A46)
val RossoScuro = Color(0xFFC33A28)

val Azzurrino = Color(0xFFDCEBFF)
val Rosa = Color(0xFFFFE1F0)
val Menta = Color(0xFFD8F5E5)
val Lilla = Color(0xFFEDE0FF)
val Sabbia = Color(0xFFFFEBC7)
val RossoTenue = Color(0xFFFFE0DB)

private val ColoriBambini = listOf(
    Color(0xFF1B6FE3), // blu
    Color(0xFFC42A86), // magenta
    Color(0xFF7A3BC4), // viola
    Color(0xFF0E7C86)  // ottanio
)

val Coriandoli = listOf(
    Color(0xFF1B6FE3),
    Color(0xFFC42A86),
    Color(0xFF17A05E),
    Color(0xFFF5B324),
    Color(0xFFE5503C),
    Color(0xFF8B45D6)
)

fun coloreBambino(indice: Int): Color = ColoriBambini[indice.mod(ColoriBambini.size)]

fun coloreVoto(voto: Voto): Color = when (voto) {
    Voto.SI -> Verde
    Voto.COSI_COSI -> Giallo
    Voto.NO -> Rosso
}

fun coloreGiudizio(giudizio: Giudizio): Color = when (giudizio) {
    Giudizio.BUONO -> Verde
    Giudizio.COSI_COSI -> Giallo
    Giudizio.DIFFICILE -> Rosso
    Giudizio.ASSENTE -> Crema
    Giudizio.NON_REGISTRATO -> Color.Transparent
}

/** Che colore deve avere il testo sopra la fascia colorata del giudizio. */
fun inchiostroSuGiudizio(giudizio: Giudizio): Color = when (giudizio) {
    Giudizio.BUONO, Giudizio.DIFFICILE -> Crema
    else -> Inchiostro
}

fun tintaGiudizio(giudizio: Giudizio): Color = when (giudizio) {
    Giudizio.BUONO -> Menta
    Giudizio.COSI_COSI -> Sabbia
    Giudizio.DIFFICILE -> RossoTenue
    else -> Crema
}

// ---- tipografia ----------------------------------------------------------------------

private fun fredoka(peso: Int) = Font(
    resId = R.font.fredoka,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso))
)

private fun figtree(peso: Int) = Font(
    resId = R.font.figtree,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso))
)

/** Tonda e rotonda: è la voce dell'app, per titoli e numeri grandi. */
val Fredoka = FontFamily(fredoka(400), fredoka(500), fredoka(600))

/** Per tutto il resto: etichette, note, testo corrente. */
val Figtree = FontFamily(figtree(400), figtree(600), figtree(700), figtree(800))

val TipografiaKids = Typography(
    displayLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 42.sp, lineHeight = 46.sp),
    displayMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 38.sp),
    displaySmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 27.sp),
    headlineSmall = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, lineHeight = 17.sp),
    labelMedium = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, lineHeight = 15.sp),
    labelSmall = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, lineHeight = 13.sp)
)

// ---- misure ricorrenti ---------------------------------------------------------------

object Misure {
    val bordo = 2.dp
    val raggioScheda = 22.dp
    val raggioPiccolo = 16.dp
    val ombraX = 3.dp
    val ombraY = 4.dp
    val toccoMinimo = 48.dp
}

/**
 * L'app è volutamente solo chiara: il fondo crema è parte dell'identità,
 * e le faccine colorate su scuro perderebbero contrasto.
 */
@Composable
fun KidsTema(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Inchiostro,
            onPrimary = Crema,
            secondary = Prugna,
            onSecondary = Crema,
            background = Crema,
            onBackground = Inchiostro,
            surface = Color.White,
            onSurface = Inchiostro,
            surfaceVariant = Crema,
            onSurfaceVariant = InkSecondario,
            error = Rosso,
            onError = Color.White
        ),
        typography = TipografiaKids,
        content = content
    )
}
