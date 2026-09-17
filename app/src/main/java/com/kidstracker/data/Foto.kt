package com.kidstracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

/**
 * Le foto dei bambini.
 *
 * Non finiscono nel database: vengono copiate nella cartella privata dell'app,
 * ridotte a 512px e salvate in JPEG. Nella tabella resta solo il nome del file.
 * Il nome porta dentro l'orario di importazione, così quando si cambia foto
 * cambia anche il nome e nessuna cache mostra ancora quella vecchia.
 *
 * importa/importaTemporanea prendono i byte già letti, non una Uri: su
 * alcuni telefoni il permesso di lettura che il selettore di sistema dà su
 * quella Uri scade anche solo nel tempo di passare da un dispatcher a un
 * altro. leggiByte va chiamata subito, appena la Uri arriva, prima di
 * qualsiasi lavoro differito in una coroutine — vedi il suo commento.
 */
object Foto {

    private const val CARTELLA = "foto"
    private const val LATO_MASSIMO = 512
    private const val QUALITA = 85

    fun cartella(contesto: Context): File =
        File(contesto.filesDir, CARTELLA).apply { mkdirs() }

    fun file(contesto: Context, nome: String): File = File(cartella(contesto), nome)

    /**
     * Legge subito, sul posto, i byte della foto scelta dal selettore di
     * sistema. Va chiamata appena si riceve la Uri, prima di lanciare
     * qualunque coroutine: il permesso di lettura che il selettore concede
     * su quella Uri non è garantito sopravvivere nemmeno a un solo cambio
     * di dispatcher, e su alcuni telefoni scade proprio in quel momento
     * (openInputStream torna null, non un'eccezione). Una volta che i byte
     * sono in mano, la Uri originale non serve più: tutto il resto del
     * lavoro (decodifica, ridimensionamento, scrittura) può avvenire con
     * comodo su un altro thread.
     */
    fun leggiByte(contesto: Context, origine: Uri): Result<ByteArray> = try {
        val byte = contesto.contentResolver.openInputStream(origine)?.use { it.readBytes() }
        if (byte != null) {
            Result.success(byte)
        } else {
            Result.failure(
                IOException("Il sistema non mi lascia aprire questa foto (openInputStream nullo)")
            )
        }
    } catch (errore: Exception) {
        Result.failure(errore)
    }

    /** Copia la foto scelta dentro l'app e restituisce il nome del file. */
    fun importa(contesto: Context, bambinoId: Long, byte: ByteArray): Result<String> {
        val bitmap = decodificaRidotta(byte).getOrElse { return Result.failure(it) }
        return scriviJpeg(contesto, "bambino_${bambinoId}_${System.currentTimeMillis()}.jpg", bitmap)
    }

    /**
     * Copia la foto scelta in un file temporaneo, per l'onboarding: le schede
     * non hanno ancora un id quando si sceglie la foto.
     */
    fun importaTemporanea(contesto: Context, indice: Int, byte: ByteArray): Result<String> {
        val bitmap = decodificaRidotta(byte).getOrElse { return Result.failure(it) }
        return scriviJpeg(contesto, "tmp_onboarding_${indice}_${System.currentTimeMillis()}.jpg", bitmap)
    }

    /**
     * Il file temporaneo dell'onboarding diventa quello vero: un semplice
     * spostamento sul filesystem, senza toccare più la Uri originale.
     */
    fun confermaTemporanea(contesto: Context, temporanea: String, bambinoId: Long): String? {
        val origine = file(contesto, temporanea)
        if (!origine.exists()) return null
        val nome = "bambino_${bambinoId}_${System.currentTimeMillis()}.jpg"
        return try {
            if (origine.renameTo(file(contesto, nome))) nome else null
        } catch (errore: Exception) {
            null
        }
    }

    /** Toglie il temporaneo se l'onboarding lo sostituisce o non lo usa mai. */
    fun scartaTemporanea(contesto: Context, temporanea: String?) = elimina(contesto, temporanea)

    private fun scriviJpeg(contesto: Context, nome: String, bitmap: Bitmap): Result<String> = try {
        val destinazione = file(contesto, nome)
        destinazione.outputStream().use { flusso ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITA, flusso)
        }
        // Uno storage pieno o un I/O interrotto può lasciare un file da 0 byte
        // senza sollevare un'eccezione: qui lo trattiamo come un fallimento,
        // non come una foto che poi risulta semplicemente illeggibile.
        if (destinazione.length() > 0) {
            Result.success(nome)
        } else {
            destinazione.delete()
            Result.failure(IOException("Il file scritto è vuoto: spazio esaurito o scrittura interrotta"))
        }
    } catch (errore: Exception) {
        Result.failure(errore)
    } finally {
        bitmap.recycle()
    }

    /** Salva dei byte JPEG già pronti (serve al ripristino da backup). */
    fun salvaByte(contesto: Context, bambinoId: Long, byte: ByteArray): String? = try {
        val nome = "bambino_${bambinoId}_${System.currentTimeMillis()}.jpg"
        file(contesto, nome).writeBytes(byte)
        nome
    } catch (errore: Exception) {
        null
    }

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
     * Decodifica l'immagine già letta scalandola, così una foto da 12
     * megapixel non viene mai caricata per intero in memoria, e la
     * raddrizza secondo l'orientamento EXIF (le foto di ritratto arrivano
     * quasi sempre ruotate). Lavora sui byte, non su una Uri: a questo
     * punto la foto è già al sicuro dentro il processo.
     */
    private fun decodificaRidotta(byte: ByteArray): Result<Bitmap> {
        return try {
            val misura = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(byte, 0, byte.size, misura)

            if (misura.outWidth <= 0 || misura.outHeight <= 0) {
                return Result.failure(
                    IOException("Formato immagine non riconosciuto (${misura.outWidth}×${misura.outHeight})")
                )
            }

            val opzioni = BitmapFactory.Options().apply {
                inSampleSize = campionamento(misura.outWidth, misura.outHeight)
            }
            val grezza = BitmapFactory.decodeByteArray(byte, 0, byte.size, opzioni)
                ?: return Result.failure(IOException("BitmapFactory non è riuscito a decodificare l'immagine"))

            val gradi = orientamento(byte)
            val raddrizzata = if (gradi == 0f) grezza else ruota(grezza, gradi)
            Result.success(riduci(raddrizzata))
        } catch (errore: Exception) {
            Result.failure(errore)
        }
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

    private fun orientamento(byte: ByteArray): Float = runCatching {
        ByteArrayInputStream(byte).use { flusso ->
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
        }
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
