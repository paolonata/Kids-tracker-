package com.kidstracker.ui.schermate

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidstracker.data.Backup
import com.kidstracker.data.Excel
import com.kidstracker.domain.Bambino
import com.kidstracker.domain.Giornata
import com.kidstracker.ui.componenti.AvatarBambino
import com.kidstracker.ui.componenti.BottoneContornato
import com.kidstracker.ui.componenti.BottoneSticker
import com.kidstracker.ui.componenti.IconaFreccia
import com.kidstracker.ui.componenti.IconaOrologio
import com.kidstracker.ui.componenti.IntestazionePrugna
import com.kidstracker.ui.componenti.PillolaScelta
import com.kidstracker.ui.componenti.SchedaSticker
import com.kidstracker.ui.componenti.sticker
import com.kidstracker.ui.tema.Crema
import com.kidstracker.ui.tema.Lilla
import com.kidstracker.ui.tema.Menta
import com.kidstracker.ui.tema.TemaScelto
import com.kidstracker.ui.tema.Inchiostro
import com.kidstracker.ui.tema.InchiostroFaccina
import com.kidstracker.ui.tema.Superficie
import com.kidstracker.ui.tema.InkTenue
import com.kidstracker.ui.tema.InkTerziario
import com.kidstracker.ui.tema.Rosso
import com.kidstracker.ui.tema.Sabbia
import com.kidstracker.ui.tema.coloreBambino
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SchermataImpostazioni(
    bambini: List<Bambino>,
    promemoriaAttivo: Boolean,
    oraPromemoria: Int,
    temaCorrente: TemaScelto,
    onTema: (TemaScelto) -> Unit,
    onRinomina: (Bambino, String) -> Unit,
    onFoto: suspend (Bambino, Uri) -> Boolean,
    onRimuoviFoto: (Bambino) -> Unit,
    onPromemoria: (Boolean) -> Unit,
    onOra: (Int) -> Unit,
    onEsporta: suspend () -> Pair<List<Bambino>, List<Giornata>>,
    onBackupJson: suspend () -> String,
    onImporta: suspend (Backup.Importazione, Boolean) -> Int,
    onCancellaTutto: () -> Unit,
    onIndietro: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contesto = LocalContext.current
    val ambito = rememberCoroutineScope()
    var messaggio by remember { mutableStateOf<String?>(null) }
    var erroreFoto by remember { mutableStateOf<String?>(null) }
    var chiedeConferma by remember { mutableStateOf(false) }

    // Un solo selettore di foto per tutti i bambini: si ricorda chi l'ha aperto.
    // rememberSaveable, non remember: il selettore di sistema è pesante, e su
    // molti telefoni Android chiude il processo dell'app per liberare memoria
    // mentre è aperto. Con "remember" questo valore si perdeva silenziosamente
    // ogni volta che succedeva, e al ritorno nessun bambino risultava in
    // attesa: la foto veniva scelta ma non si vedeva né dava errore.
    var inAttesaDiFoto by rememberSaveable { mutableStateOf<Long?>(null) }
    val scegliFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val bambino = bambini.firstOrNull { it.id == inAttesaDiFoto }
        inAttesaDiFoto = null
        if (uri == null || bambino == null) return@rememberLauncherForActivityResult
        ambito.launch {
            val riuscita = onFoto(bambino, uri)
            erroreFoto = if (riuscita) {
                null
            } else {
                "Non sono riuscito a leggere quella foto. Riprova, o scegline un'altra."
            }
        }
    }

    val salvaJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        ambito.launch {
            val testo = onBackupJson()
            messaggio = scrivi(contesto, uri, testo, "Backup salvato, foto comprese")
        }
    }

    val salvaCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        ambito.launch {
            val (elenco, giornate) = onEsporta()
            val testo = Backup.esportaCsv(elenco, giornate)
            messaggio = scrivi(contesto, uri, testo, "${giornate.size} righe nel CSV")
        }
    }

    val salvaExcel = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        ambito.launch {
            val (elenco, giornate) = onEsporta()
            messaggio = try {
                withContext(Dispatchers.IO) {
                    contesto.contentResolver.openOutputStream(uri)?.use { flusso ->
                        Excel.scrivi(flusso, Backup.fogliExcel(elenco, giornate))
                    } ?: error("non scrivibile")
                }
                "${giornate.size} giornate nel foglio Excel"
            } catch (errore: Exception) {
                "Non sono riuscito a salvare: ${errore.message ?: "errore sconosciuto"}"
            }
        }
    }

    val apriJson = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        ambito.launch {
            messaggio = try {
                val testo = withContext(Dispatchers.IO) {
                    contesto.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } ?: error("file vuoto")
                val importazione = Backup.importaJson(testo)
                val quante = onImporta(importazione, false)
                "$quante giornate importate"
            } catch (errore: Exception) {
                "Non sono riuscito a leggere il file: ${errore.message ?: "formato non riconosciuto"}"
            }
        }
    }

    Column(modifier = modifier) {
        IntestazionePrugna(
            titolo = "Impostazioni",
            sottotitolo = "nomi, promemoria e backup",
            azioni = {
                BottoneContornato(onIndietro, "Torna indietro") { tinta -> IconaFreccia(tinta) }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SchedaSticker {
                Text("I bambini", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tocca la faccina per mettere la loro foto: resta su questo telefono " +
                        "e finisce nel backup insieme allo storico.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTerziario
                )
                bambini.forEach { bambino ->
                    Spacer(Modifier.height(12.dp))
                    var nome by remember(bambino.id) { mutableStateOf(bambino.nome) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = "Scegli la foto di ${bambino.nome}"
                            ) {
                                inAttesaDiFoto = bambino.id
                                scegliFoto.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        ) {
                            AvatarBambino(
                                foto = bambino.foto,
                                dimensione = 46.dp,
                                riempimento = coloreBambino(bambino.coloreIndex),
                                tratto = InchiostroFaccina
                            )
                        }
                        CampoNome(
                            valore = nome,
                            posizione = bambino.coloreIndex + 1,
                            onCambia = {
                                nome = it
                                onRinomina(bambino, it)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        BottoneSticker(
                            testo = if (bambino.foto == null) "Scegli una foto" else "Cambia foto",
                            onClick = {
                                inAttesaDiFoto = bambino.id
                                scegliFoto.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            sfondo = Superficie,
                            modifier = Modifier.weight(1f)
                        )
                        if (bambino.foto != null) {
                            BottoneSticker(
                                testo = "Togli",
                                onClick = { onRimuoviFoto(bambino) },
                                sfondo = Crema,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                erroreFoto?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = InkTerziario)
                }
            }

            SchedaSticker(sfondo = Lilla) {
                Text("Aspetto", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Il tema scuro usa gli stessi colori delle faccine: cambia lo sfondo, " +
                        "non il significato.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTerziario
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TemaScelto.entries.forEach { scelta ->
                        PillolaScelta(
                            testo = scelta.etichetta,
                            selezionata = scelta == temaCorrente,
                            onClick = { onTema(scelta) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            SchedaSticker(sfondo = Sabbia) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    IconaOrologio(Inchiostro, dimensione = 20.dp)
                    Text("Promemoria", style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Una notifica al giorno per ricordarti di segnare le faccine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTerziario
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    PillolaScelta(
                        testo = "Attivo",
                        selezionata = promemoriaAttivo,
                        onClick = { onPromemoria(true) },
                        modifier = Modifier.weight(1f)
                    )
                    PillolaScelta(
                        testo = "Spento",
                        selezionata = !promemoriaAttivo,
                        onClick = { onPromemoria(false) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (promemoriaAttivo) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PassoOra("−15 min", "Anticipa di un quarto d'ora") {
                            onOra(oraPromemoria - 15)
                        }
                        Text(
                            orario(oraPromemoria),
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        PassoOra("+15 min", "Posticipa di un quarto d'ora") {
                            onOra(oraPromemoria + 15)
                        }
                    }
                }
            }

            SchedaSticker(sfondo = Menta) {
                Text("Backup", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Salva il backup e potrai disinstallare l'app, reinstallarla e " +
                        "ritrovare tutto lo storico: al primo avvio c'è il tasto per ricaricarlo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTerziario
                )
                Spacer(Modifier.height(13.dp))
                BottoneSticker(
                    testo = "Salva il backup",
                    onClick = { salvaJson.launch(nomeFile("json")) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(9.dp))
                BottoneSticker(
                    testo = "Ricarica un backup",
                    onClick = { apriJson.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    sfondo = Superficie,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Per guardarli altrove", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Il file Excel ha tre fogli: le giornate in chiaro, gli stessi dati in " +
                        "numeri per i grafici, e un riepilogo per bambino.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTerziario
                )
                Spacer(Modifier.height(11.dp))
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    BottoneSticker(
                        testo = "Esporta in Excel",
                        onClick = { salvaExcel.launch(nomeFile("xlsx")) },
                        sfondo = Superficie,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BottoneSticker(
                        testo = "Esporta in CSV",
                        onClick = { salvaCsv.launch(nomeFile("csv")) },
                        sfondo = Superficie,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                messaggio?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = InkTerziario)
                }
            }

            SchedaSticker(sfondo = Crema) {
                Text("Ricominciare da capo", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Cancella tutte le giornate segnate. I nomi restano. " +
                        "Non si torna indietro: prima esporta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTerziario
                )
                Spacer(Modifier.height(13.dp))
                if (chiedeConferma) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        BottoneSticker(
                            testo = "Sì, cancella",
                            onClick = {
                                onCancellaTutto()
                                chiedeConferma = false
                                messaggio = "Giornate cancellate"
                            },
                            sfondo = Rosso,
                            contenutoColore = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        BottoneSticker(
                            testo = "No",
                            onClick = { chiedeConferma = false },
                            sfondo = Superficie,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    BottoneSticker(
                        testo = "Cancella tutte le giornate",
                        onClick = { chiedeConferma = true },
                        sfondo = Superficie,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text(
                "Kids Tracker · i dati non escono da questo telefono",
                style = MaterialTheme.typography.labelMedium,
                color = InkTenue,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun PassoOra(testo: String, descrizione: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 92.dp, height = 46.dp)
            .sticker(Superficie, 15.dp, ombra = false)
            .clickable(role = Role.Button, onClickLabel = descrizione, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(testo, style = MaterialTheme.typography.labelLarge)
    }
}

private fun orario(minuti: Int): String {
    val normalizzati = ((minuti % (24 * 60)) + 24 * 60) % (24 * 60)
    val ore = normalizzati / 60
    val resto = normalizzati % 60
    return "%02d:%02d".format(ore, resto)
}

private fun nomeFile(estensione: String): String =
    "kids-tracker-${LocalDate.now()}.$estensione"

private suspend fun scrivi(
    contesto: Context,
    uri: Uri,
    testo: String,
    conferma: String
): String = try {
    withContext(Dispatchers.IO) {
        contesto.contentResolver.openOutputStream(uri)?.use { flusso ->
            flusso.write(testo.toByteArray())
        } ?: error("non scrivibile")
    }
    conferma
} catch (errore: Exception) {
    "Non sono riuscito a salvare: ${errore.message ?: "errore sconosciuto"}"
}
