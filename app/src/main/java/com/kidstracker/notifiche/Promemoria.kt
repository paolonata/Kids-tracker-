package com.kidstracker.notifiche

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kidstracker.MainActivity
import com.kidstracker.R
import com.kidstracker.data.Preferenze
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Un promemoria al giorno all'ora scelta. Non serve internet:
 * è WorkManager che sveglia l'app e mostra la notifica.
 */
object Promemoria {

    const val CANALE = "promemoria_giornaliero"
    private const val LAVORO = "promemoria_giornaliero"
    private const val ID_NOTIFICA = 1001

    fun riprogramma(context: Context, preferenze: Preferenze) {
        val gestore = WorkManager.getInstance(context)
        if (!preferenze.promemoriaAttivo) {
            gestore.cancelUniqueWork(LAVORO)
            return
        }

        val richiesta = PeriodicWorkRequestBuilder<LavoroPromemoria>(1, TimeUnit.DAYS)
            .setInitialDelay(ritardoIniziale(preferenze).toMinutes(), TimeUnit.MINUTES)
            .build()

        gestore.enqueueUniquePeriodicWork(
            LAVORO,
            ExistingPeriodicWorkPolicy.UPDATE,
            richiesta
        )
    }

    /** Quanto manca alla prossima occorrenza dell'ora scelta. */
    internal fun ritardoIniziale(
        preferenze: Preferenze,
        adesso: LocalDateTime = LocalDateTime.now()
    ): Duration {
        var bersaglio = adesso
            .withHour(preferenze.ore)
            .withMinute(preferenze.minuti)
            .withSecond(0)
            .withNano(0)
        if (!bersaglio.isAfter(adesso)) bersaglio = bersaglio.plusDays(1)
        return Duration.between(adesso, bersaglio)
    }

    fun mostra(context: Context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val apri = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notifica = Notification.Builder(context, CANALE)
            .setSmallIcon(R.drawable.ic_notifica)
            .setContentTitle("Com'è andata oggi?")
            .setContentText("Due minuti per attaccare le faccine di oggi.")
            .setAutoCancel(true)
            .setContentIntent(apri)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(ID_NOTIFICA, notifica)
    }
}

class LavoroPromemoria(
    context: Context,
    parametri: WorkerParameters
) : CoroutineWorker(context, parametri) {

    override suspend fun doWork(): Result {
        Promemoria.mostra(applicationContext)
        return Result.success()
    }
}
