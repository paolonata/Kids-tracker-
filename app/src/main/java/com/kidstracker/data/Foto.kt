package com.kidstracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.File

/**
 * Le foto dei bambini.
 *
 * Non finiscono nel database: vengono copiate nella cartella privata dell'app,
 * ridotte a 512px e salvate in JPEG. Nella tabella resta solo il nome del file.
 * Il nome porta dentro l'orario di importazione, così quando si cambia foto
 * cambia anche il nome e nessuna cache mostra ancora quella vecchia.
 */
object Foto {

    private const val CARTELLA = "foto"
    private const val LATO_MASSIMO = 512
    private const val QUALITA = 85

    fun cartella(contesto: Context): File =
        File(contesto.filesDir, CARTELLA).apply { mkdirs() }

    fun file(contesto: Context, nome: String): File = File(cartella(contesto), nome)

    /**
     * Copia la foto scelta dentro l'app e restituisce il nome del file,
     * oppure null se l'immagine non è leggibile.
     */
    fun importa(contesto: Context, bambinoId: Long, origine: Uri): String? {
        val bitmap = leggiRidotta(contesto, origine) ?: return null
        val nome = "bambino_${bambinoId}_${System.currentTimeMillis()}.jpg"
        return try {
            file(contesto, nome).outputStream().use { flusso ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITA, flusso)
            }
            nome
        } catch (errore: Exception) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    /** Salva dei byte JPEG già pronti (serve al ripristino da backup). */
    fun salvaByte(contesto: Context, bambinoId: Long, byte: ByteArray): String? = try {
        val nome = "bambino_${bambinoId}_${System.currentTimeMillis()}.jpg"
        file(contesto, nome).writeBytes(byte)
        nome
    } catch (errore: Exception) {
        null
    }

    /** Anteprima di una foto appena scelta, prima che esista il bambino a cui darla. */
    fun anteprima(contesto: Context, origine: Uri): Bitmap? = leggiRidotta(contesto, origine)

    fun carica(contesto: Context, nome: String): Bitmap? = try {
        val f = file(contesto, nome)
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
    } catch (errore: Exception) {
        null
    }

    fun elimina(contesto: Context, nome: String?) {
        if (nome.isNullOrBlank()) return
        runCatching { file(contesto, nome).delete() }
    }

    fun base64(contesto: Context, nome: String?): String? {
        if (nome.isNullOrBlank()) return null
        val f = file(contesto, nome)
        if (!f.exists()) return null
        return runCatching {
            Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
        }.getOrNull()
    }

    fun daBase64(testo: String?): ByteArray? {
        if (testo.isNullOrBlank()) return null
        return runCatching { Base64.decode(testo, Base64.NO_WRAP) }.getOrNull()
    }

    /**
     * Decodifica l'immagine scelta scalandola durante la lettura, così una foto
     * da 12 megapixel non viene mai caricata per intero in memoria, e la
     * raddrizza secondo l'orientamento EXIF (le foto di ritratto arrivano
     * quasi sempre ruotate).
     */
    private fun leggiRidotta(contesto: Context, origine: Uri): Bitmap? {
        val misura = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contesto.contentResolver.openInputStream(origine)?.use {
            BitmapFactory.decodeStream(it, null, misura)
        } ?: return null

        if (misura.outWidth <= 0 || misura.outHeight <= 0) return null

        val opzioni = BitmapFactory.Options().apply {
            inSampleSize = campionamento(misura.outWidth, misura.outHeight)
        }
        val grezza = contesto.contentResolver.openInputStream(origine)?.use {
            BitmapFactory.decodeStream(it, null, opzioni)
        } ?: return null

        val gradi = orientamento(contesto, origine)
        val raddrizzata = if (gradi == 0f) grezza else ruota(grezza, gradi)
        return riduci(raddrizzata)
    }

    private fun campionamento(larghezza: Int, altezza: Int): Int {
        var passo = 1
        var lato = maxOf(larghezza, altezza)
        while (lato / 2 >= LATO_MASSIMO) {
            lato /= 2
            passo *= 2
        }
        return passo
    }

    private fun orientamento(contesto: Context, origine: Uri): Float = runCatching {
        contesto.contentResolver.openInputStream(origine)?.use { flusso ->
            when (
                ExifInterface(flusso).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
    }.getOrDefault(0f)

    private fun ruota(bitmap: Bitmap, gradi: Float): Bitmap {
        val matrice = Matrix().apply { postRotate(gradi) }
        val ruotata = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrice, true)
        if (ruotata != bitmap) bitmap.recycle()
        return ruotata
    }

    private fun riduci(bitmap: Bitmap): Bitmap {
        val lato = maxOf(bitmap.width, bitmap.height)
        if (lato <= LATO_MASSIMO) return bitmap
        val fattore = LATO_MASSIMO.toFloat() / lato
        val ridotta = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * fattore).toInt().coerceAtLeast(1),
            (bitmap.height * fattore).toInt().coerceAtLeast(1),
            true
        )
        if (ridotta != bitmap) bitmap.recycle()
        return ridotta
    }
}
