package com.kidstracker.ui.tema

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.kidstracker.R
import com.kidstracker.domain.Giudizio
import com.kidstracker.domain.Voto

/** Quale tema usare. "Sistema" segue l'interruttore di Android. */
enum class TemaScelto(val etichetta: String) {
    SISTEMA("Sistema"),
    CHIARO("Chiaro"),
    SCURO("Scuro");

    companion object {
        fun daNome(nome: String?): TemaScelto = entries.firstOrNull { it.name == nome } ?: SISTEMA
    }
}

/**
 * Tutti i colori dell'app in un posto solo, una versione per tema.
 *
 * Le faccine (verde, giallo, rosso) sono deliberatamente identiche nei due temi:
 * sono il linguaggio della scuola, non devono cambiare tinta a seconda dell'ora.
 * I colori dei bambini invece cambiano, perché sul fondo scuro il blu e il
 * magenta chiari perderebbero contrasto: entrambe le coppie sono state verificate
 * per restare distinguibili anche con daltonismo.
 */
data class Palette(
    val scuro: Boolean,
    val sfondo: Color,
    val superficie: Color,
    val inchiostro: Color,
    val ombra: Color,
    val testata: Color,
    val testataChiara: Color,
    val testataSpenta: Color,
    val inkSecondario: Color,
    val inkTerziario: Color,
    val inkTenue: Color,
    val tratteggio: Color,
    val tratteggioTenue: Color,
    val griglia: Color,
    val asse: Color,
    val verde: Color,
    val giallo: Color,
    val rosso: Color,
    val verdeTesto: Color,
    val rossoTesto: Color,
    val bluTenue: Color,
    val tintaAzzurra: Color,
    val tintaRosa: Color,
    val tintaMenta: Color,
    val tintaLilla: Color,
    val tintaSabbia: Color,
    val tintaRossa: Color,
    val bambini: List<Color>,
    val coriandoli: List<Color>
)

private val VerdeFaccina = Color(0xFF17A05E)
private val GialloFaccina = Color(0xFFF5B324)
private val RossoFaccina = Color(0xFFE5503C)

val PaletteChiara = Palette(
    scuro = false,
    sfondo = Color(0xFFFFF6E9),
    superficie = Color(0xFFFFFFFF),
    inchiostro = Color(0xFF231428),
    ombra = Color(0xFF231428),
    testata = Color(0xFF3B1F49),
    testataChiara = Color(0xFFD7B9E8),
    testataSpenta = Color(0xFF7A5E88),
    inkSecondario = Color(0xFF6B5A73),
    inkTerziario = Color(0xFF4A3B52),
    inkTenue = Color(0xFF8B7A93),
    tratteggio = Color(0xFFC7BCCD),
    tratteggioTenue = Color(0xFFE3D9C9),
    griglia = Color(0xFFEDE6DA),
    asse = Color(0xFFC9BBAA),
    verde = VerdeFaccina,
    giallo = GialloFaccina,
    rosso = RossoFaccina,
    verdeTesto = Color(0xFF0F7A46),
    rossoTesto = Color(0xFFC33A28),
    bluTenue = Color(0xFF86B6EF),
    tintaAzzurra = Color(0xFFDCEBFF),
    tintaRosa = Color(0xFFFFE1F0),
    tintaMenta = Color(0xFFD8F5E5),
    tintaLilla = Color(0xFFEDE0FF),
    tintaSabbia = Color(0xFFFFEBC7),
    tintaRossa = Color(0xFFFFE0DB),
    bambini = listOf(
        Color(0xFF1B6FE3),
        Color(0xFFC42A86),
        Color(0xFF7A3BC4),
        Color(0xFF0E7C86)
    ),
    coriandoli = listOf(
        Color(0xFF1B6FE3),
        Color(0xFFC42A86),
        Color(0xFF17A05E),
        Color(0xFFF5B324),
        Color(0xFFE5503C),
        Color(0xFF8B45D6)
    )
)

val PaletteScura = Palette(
    scuro = true,
    sfondo = Color(0xFF181320),
    superficie = Color(0xFF241C2E),
    inchiostro = Color(0xFFF7EFE4),
    ombra = Color(0xFF0B0810),
    testata = Color(0xFF34203F),
    testataChiara = Color(0xFFC9A9DC),
    testataSpenta = Color(0xFF6E5B7C),
    inkSecondario = Color(0xFFBAAAC4),
    inkTerziario = Color(0xFFD5C8DD),
    inkTenue = Color(0xFF9A88A6),
    tratteggio = Color(0xFF5E4F6C),
    tratteggioTenue = Color(0xFF453A52),
    griglia = Color(0xFF3A3145),
    asse = Color(0xFF554963),
    verde = VerdeFaccina,
    giallo = GialloFaccina,
    rosso = RossoFaccina,
    verdeTesto = Color(0xFF45C98A),
    rossoTesto = Color(0xFFFF8E77),
    bluTenue = Color(0xFF2F5F96),
    tintaAzzurra = Color(0xFF1E3350),
    tintaRosa = Color(0xFF46203A),
    tintaMenta = Color(0xFF17402F),
    tintaLilla = Color(0xFF33254E),
    tintaSabbia = Color(0xFF453317),
    tintaRossa = Color(0xFF4A2420),
    bambini = listOf(
        Color(0xFF4D93F0),
        Color(0xFFE45BA6),
        Color(0xFFA77BE8),
        Color(0xFF39AEB8)
    ),
    coriandoli = listOf(
        Color(0xFF4D93F0),
        Color(0xFFE45BA6),
        Color(0xFF2FC17B),
        Color(0xFFF5B324),
        Color(0xFFF07A66),
        Color(0xFFA77BE8)
    )
)

val LocalPalette = staticCompositionLocalOf { PaletteChiara }

// ---- i nomi storici, ora letti dalla palette attiva ----------------------------------

val Crema: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.sfondo
val Superficie: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.superficie
val Inchiostro: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.inchiostro
val Ombra: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ombra
val Prugna: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.testata
val PrugnaChiara: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.testataChiara
val PrugnaSpenta: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.testataSpenta

val InkSecondario: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.inkSecondario
val InkTerziario: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.inkTerziario
val InkTenue: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.inkTenue
val Tratteggio: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tratteggio
val TratteggioTenue: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tratteggioTenue
val Griglia: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.griglia
val SabbiaTenue: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.asse

val Verde: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.verde
val Giallo: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.giallo
val Rosso: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.rosso
val VerdeScuro: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.verdeTesto
val RossoScuro: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.rossoTesto
val BluTenue: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bluTenue

val Azzurrino: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tintaAzzurra
val Rosa: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tintaRosa
val Menta: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tintaMenta
val Lilla: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tintaLilla
val Sabbia: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tintaSabbia
val RossoTenue: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tintaRossa

val Coriandoli: List<Color> @Composable @ReadOnlyComposable get() = LocalPalette.current.coriandoli

@Composable
@ReadOnlyComposable
fun coloreBambino(indice: Int): Color {
    val tinte = LocalPalette.current.bambini
    return tinte[indice.mod(tinte.size)]
}

@Composable
@ReadOnlyComposable
fun coloreVoto(voto: Voto): Color = when (voto) {
    Voto.SI -> Verde
    Voto.COSI_COSI -> Giallo
    Voto.NO -> Rosso
}

@Composable
@ReadOnlyComposable
fun coloreGiudizio(giudizio: Giudizio): Color = when (giudizio) {
    Giudizio.BUONO -> Verde
    Giudizio.COSI_COSI -> Giallo
    Giudizio.DIFFICILE -> Rosso
    Giudizio.ASSENTE -> Crema
    // Il festivo non ha un colore suo: la cella del calendario ci mette il velo.
    Giudizio.FESTIVO -> Color.Transparent
    Giudizio.NON_REGISTRATO -> Color.Transparent
}

@Composable
@ReadOnlyComposable
fun tintaGiudizio(giudizio: Giudizio): Color = when (giudizio) {
    Giudizio.BUONO -> Menta
    Giudizio.COSI_COSI -> Sabbia
    Giudizio.DIFFICILE -> RossoTenue
    else -> Crema
}

/**
 * Il testo sopra la fascia colorata del giudizio. Le faccine non cambiano
 * tinta fra i due temi, quindi non deve cambiare nemmeno questo.
 */
fun inchiostroSuGiudizio(giudizio: Giudizio): Color = when (giudizio) {
    Giudizio.BUONO, Giudizio.DIFFICILE -> Color(0xFFFFF6E9)
    else -> Color(0xFF231428)
}

/** Il tratto delle faccine resta scuro anche col tema scuro: sta sopra tinte piene. */
val InchiostroFaccina = Color(0xFF231428)

/** Il chiaro fisso dentro le faccine, anche col tema scuro. */
val CremaFaccina = Color(0xFFFFF6E9)

// ---- tipografia ----------------------------------------------------------------------

@OptIn(ExperimentalTextApi::class)
private fun fredoka(peso: Int) = Font(
    resId = R.font.fredoka,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso))
)

@OptIn(ExperimentalTextApi::class)
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

/**
 * Le etichettine tutte maiuscole ("PRESENZA", "NOTA", "COSÌ COSÌ"). Le maiuscole
 * attaccate si leggono male: un filo di spaziatura fra le lettere le separa.
 */
val MicroEtichetta = TextStyle(
    fontFamily = Figtree,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.06.em
)

/** Come [MicroEtichetta], ma per le colonne strette sopra le faccine. */
val MicroEtichettaStretta = MicroEtichetta.copy(fontSize = 10.sp, lineHeight = 13.sp)

// ---- misure ricorrenti ---------------------------------------------------------------

object Misure {
    val bordo = 2.dp
    val raggioScheda = 22.dp
    val raggioPiccolo = 16.dp
    val ombraX = 3.dp
    val ombraY = 4.dp
    val toccoMinimo = 48.dp
}

@Composable
fun KidsTema(tema: TemaScelto, content: @Composable () -> Unit) {
    val scuro = when (tema) {
        TemaScelto.SISTEMA -> isSystemInDarkTheme()
        TemaScelto.CHIARO -> false
        TemaScelto.SCURO -> true
    }
    val palette = if (scuro) PaletteScura else PaletteChiara

    val vista = LocalView.current
    if (!vista.isInEditMode) {
        SideEffect {
            val finestra = (vista.context as Activity).window
            WindowCompat.getInsetsController(finestra, vista).apply {
                // La fascia in cima è scura in entrambi i temi: icone chiare.
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = !scuro
            }
        }
    }

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = if (scuro) {
                darkColorScheme(
                    primary = palette.inchiostro,
                    onPrimary = palette.sfondo,
                    secondary = palette.testata,
                    onSecondary = palette.inchiostro,
                    background = palette.sfondo,
                    onBackground = palette.inchiostro,
                    surface = palette.superficie,
                    onSurface = palette.inchiostro,
                    surfaceVariant = palette.sfondo,
                    onSurfaceVariant = palette.inkSecondario,
                    error = palette.rosso,
                    onError = Color.White
                )
            } else {
                lightColorScheme(
                    primary = palette.inchiostro,
                    onPrimary = palette.sfondo,
                    secondary = palette.testata,
                    onSecondary = palette.sfondo,
                    background = palette.sfondo,
                    onBackground = palette.inchiostro,
                    surface = palette.superficie,
                    onSurface = palette.inchiostro,
                    surfaceVariant = palette.sfondo,
                    onSurfaceVariant = palette.inkSecondario,
                    error = palette.rosso,
                    onError = Color.White
                )
            },
            typography = TipografiaKids,
            content = content
        )
    }
}
