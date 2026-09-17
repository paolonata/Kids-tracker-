package com.kidstracker.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kidstracker.notifiche.Promemoria
import com.kidstracker.ui.componenti.BarraNavigazione
import com.kidstracker.ui.componenti.Sezione
import com.kidstracker.ui.schermate.SchermataAndamento
import com.kidstracker.ui.schermate.SchermataCalendario
import com.kidstracker.ui.schermate.SchermataGiorno
import com.kidstracker.ui.schermate.SchermataImpostazioni
import com.kidstracker.ui.schermate.SchermataOnboarding
import com.kidstracker.ui.schermate.SchermataScoperte
import com.kidstracker.ui.tema.Crema
import com.kidstracker.ui.tema.InkSecondario

private const val ROTTA_IMPOSTAZIONI = "impostazioni"

@Composable
fun AppKidsTracker(vm: KidsViewModel) {
    val stato by vm.statoAvvio.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(Crema)) {
        when (stato) {
            StatoAvvio.Caricamento -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Un attimo…",
                    style = MaterialTheme.typography.headlineSmall,
                    color = InkSecondario
                )
            }

            StatoAvvio.ServeOnboarding -> SchermataOnboarding(
                onConferma = { nomi -> vm.creaBambini(nomi) },
                onRipristina = { importazione ->
                    vm.applicaImportazione(importazione, sostituisci = true)
                }
            )

            StatoAvvio.Pronta -> ContenutoPrincipale(vm)
        }
    }
}

@Composable
private fun ContenutoPrincipale(vm: KidsViewModel) {
    val nav = rememberNavController()
    val contesto = LocalContext.current

    val bambini by vm.bambini.collectAsStateWithLifecycle()
    val bambinoCorrente by vm.bambinoCorrente.collectAsStateWithLifecycle()
    val data by vm.data.collectAsStateWithLifecycle()
    val mese by vm.mese.collectAsStateWithLifecycle()
    val periodo by vm.periodo.collectAsStateWithLifecycle()
    val giornateDelGiorno by vm.giornateDelGiorno.collectAsStateWithLifecycle()
    val giornateDelMese by vm.giornateDelMese.collectAsStateWithLifecycle()
    val storico by vm.storico.collectAsStateWithLifecycle()

    val voceCorrente by nav.currentBackStackEntryAsState()
    val rottaCorrente = voceCorrente?.destination?.route

    var sezione by remember { mutableStateOf(Sezione.OGGI) }
    var promemoriaAttivo by remember { mutableStateOf(vm.preferenze.promemoriaAttivo) }
    var oraPromemoria by remember { mutableIntStateOf(vm.preferenze.oraPromemoria) }

    val chiediNotifiche = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* se dice di no, semplicemente non arriva il promemoria */ }

    LaunchedEffect(promemoriaAttivo) {
        if (promemoriaAttivo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            chiediNotifiche.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun vaiA(destinazione: Sezione) {
        sezione = destinazione
        nav.navigate(destinazione.rotta) {
            popUpTo(Sezione.OGGI.rotta) { inclusive = destinazione == Sezione.OGGI }
            launchSingleTop = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Sezione.OGGI.rotta,
            modifier = Modifier.weight(1f)
        ) {
            composable(Sezione.OGGI.rotta) {
                SchermataGiorno(
                    data = data,
                    bambini = bambini,
                    bambinoCorrente = bambinoCorrente,
                    giornate = giornateDelGiorno,
                    onSeleziona = vm::seleziona,
                    onVoto = vm::impostaVoto,
                    onPresenza = vm::impostaPresenza,
                    onSalute = vm::impostaSalute,
                    onNota = vm::impostaNota,
                    onApriCalendario = { vaiA(Sezione.CALENDARIO) },
                    onOggi = { vm.tornaAOggi() },
                    onImpostazioni = { nav.navigate(ROTTA_IMPOSTAZIONI) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(Sezione.CALENDARIO.rotta) {
                SchermataCalendario(
                    mese = mese,
                    bambini = bambini,
                    bambinoCorrente = bambinoCorrente,
                    giornate = giornateDelMese,
                    onSeleziona = vm::seleziona,
                    onMeseIndietro = vm::meseIndietro,
                    onMeseAvanti = vm::meseAvanti,
                    onApriGiorno = { giorno ->
                        vm.apriGiorno(giorno)
                        vaiA(Sezione.OGGI)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(Sezione.ANDAMENTO.rotta) {
                SchermataAndamento(
                    periodo = periodo,
                    bambini = bambini,
                    bambinoCorrente = bambinoCorrente,
                    storico = storico,
                    onSeleziona = vm::seleziona,
                    onPeriodo = vm::impostaPeriodo,
                    onImpostazioni = { nav.navigate(ROTTA_IMPOSTAZIONI) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(Sezione.SCOPERTE.rotta) {
                SchermataScoperte(
                    bambini = bambini,
                    bambinoCorrente = bambinoCorrente,
                    storico = storico,
                    onImpostazioni = { nav.navigate(ROTTA_IMPOSTAZIONI) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(ROTTA_IMPOSTAZIONI) {
                SchermataImpostazioni(
                    bambini = bambini,
                    promemoriaAttivo = promemoriaAttivo,
                    oraPromemoria = oraPromemoria,
                    temaCorrente = vm.tema,
                    onTema = vm::impostaTema,
                    onRinomina = vm::rinomina,
                    onFoto = vm::scegliFoto,
                    onRimuoviFoto = vm::rimuoviFoto,
                    onPromemoria = { attivo ->
                        promemoriaAttivo = attivo
                        vm.preferenze.promemoriaAttivo = attivo
                        Promemoria.riprogramma(contesto, vm.preferenze)
                    },
                    onOra = { minuti ->
                        val normalizzati = ((minuti % (24 * 60)) + 24 * 60) % (24 * 60)
                        oraPromemoria = normalizzati
                        vm.preferenze.oraPromemoria = normalizzati
                        Promemoria.riprogramma(contesto, vm.preferenze)
                    },
                    onEsporta = { bambini to vm.tutteLeGiornate() },
                    onBackupJson = { vm.backupJson() },
                    onImporta = { importazione, sostituisci ->
                        vm.applicaImportazione(importazione, sostituisci)
                    },
                    onCancellaTutto = { vm.cancellaTutteLeGiornate() },
                    onIndietro = { nav.popBackStack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (rottaCorrente != ROTTA_IMPOSTAZIONI) {
            BarraNavigazione(corrente = sezione, onNaviga = { vaiA(it) })
        }
    }
}
