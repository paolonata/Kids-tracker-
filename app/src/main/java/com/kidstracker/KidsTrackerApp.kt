package com.kidstracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import com.kidstracker.data.KidsDatabase
import com.kidstracker.data.KidsRepository
import com.kidstracker.data.Preferenze
import com.kidstracker.notifiche.Promemoria

/** Un po' di iniezione delle dipendenze fatta a mano: per un'app così basta e avanza. */
class Contenitore(context: Context) {

    private val database: KidsDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            KidsDatabase::class.java,
            KidsDatabase.NOME
        ).addMigrations(KidsDatabase.DA_1_A_2).build()
    }

    val repository: KidsRepository by lazy { KidsRepository(database.dao()) }
    val preferenze: Preferenze by lazy { Preferenze(context.applicationContext) }
}

class KidsTrackerApp : Application() {

    lateinit var contenitore: Contenitore
        private set

    override fun onCreate() {
        super.onCreate()
        contenitore = Contenitore(this)
        creaCanaleNotifiche()
        Promemoria.riprogramma(this, contenitore.preferenze)
    }

    private fun creaCanaleNotifiche() {
        val canale = NotificationChannel(
            Promemoria.CANALE,
            getString(R.string.canale_promemoria),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Un colpetto sulla spalla per segnare la giornata."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(canale)
    }
}
