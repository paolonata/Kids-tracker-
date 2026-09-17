package com.kidstracker.ui.schermate

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.kidstracker.data.Backup
import com.kidstracker.data.Foto
import com.kidstracker.ui.componenti.AvatarBambino
import com.kidstracker.ui.componenti.BottoneSticker
import com.kidstracker.ui.componenti.IconaCestino
import com.kidstracker.ui.componenti.IconaPiu
import com.kidstracker.ui.componenti.IntestazionePrugna
import com.kidstracker.ui.componenti.SchedaSticker
import com.kidstracker.ui.componenti.sticker
import com.kidstracker.ui.tema.Crema
import com.kidstracker.ui.tema.Inchiostro
import com.kidstracker.ui.tema.InchiostroFaccina
import com.kidstracker.ui.tema.Superficie
import com.kidstracker.ui.tema.InkTenue
import com.kidstracker.ui.tema.Menta
import com.kidstracker.ui.tema.InkTerziario
import com.kidstracker.ui.tema.coloreBambino
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SchermataOnboarding(
    onConferma: (List<String>, List<String?>) -> Unit,
    onRipristina: suspend (Backup.Importazione) -> Int,
    modifier: Modifier = Modifier
) {
    val nomi = remember { mutableStateListOf("", "") }
    // La foto diventa subito un file vero (nome qui dentro), non resta una Uri
    // in attesa: il permesso di lettura che dà il selettore di sistema non è
    // garantito durare fino a quando si preme "Cominciamo".
    val foto = remember { mutableStateListOf<String?>(null, null) }
    val validi = nomi.count { it.isNotBlank() }
    var inAttesaDiFoto by remember { mutableStateOf<Int?>(null) }
    val contesto = LocalContext.current
    val ambito = rememberCoroutineScope()
    var erroreFoto by remember { mutableStateOf<String?>(null) }

    val scegliFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val indice = inAttesaDiFoto
        inAttesaDiFoto = null
        if (uri == null || indice == null || indice !in foto.indices) return@rememberLauncherForActivityResult
        ambito.launch {
            val precedente = foto[indice]
            val nome = withContext(Dispatchers.IO) {
                Foto.importaTemporanea(contesto, indice, uri)
            }
            if (nome != null) {
                foto[indice] = nome
                erroreFoto = null
                withContext(Dispatchers.IO) { Foto.scartaTemporanea(contesto, precedente) }
            } else {
                erroreFoto = "Non sono riuscito a leggere quella foto. Riprova, o scegline un'altra."
            }
        }
    }
    var esito by remember { mutableStateOf<String?>(null) }

    val apriBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        ambito.launch {
            esito = try {
                val testo = withContext(Dispatchers.IO) {
                    contesto.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() }
                } ?: error("file vuoto")
                val quante = onRipristina(Backup.importaJson(testo))
                "Ripristinate $quante giornate"
            } catch (errore: Exception) {
                "Non sono riuscito a leggere il backup: " +
                    (errore.message ?: "formato non riconosciuto")
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        IntestazionePrugna(
            titolo = "Ciao",
            sottotitolo = "chi seguiamo, in questa avventura?",
            grande = true
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SchedaSticker {
                Text("I nomi", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tocca la faccina per mettere una foto. Nomi e foto restano su " +
                        "questo telefono e non escono da qui: li puoi cambiare quando vuoi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTerziario
                )
                Spacer(Modifier.height(14.dp))

                nomi.forEachIndexed { indice, nome ->
                    if (indice > 0) Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = "Scegli la foto del bambino ${indice + 1}"
                            ) {
                                inAttesaDiFoto = indice
                                scegliFoto.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        ) {
                            AvatarBambino(
                                foto = foto.getOrNull(indice),
                                dimensione = 46.dp,
                                riempimento = coloreBambino(indice),
                                tratto = InchiostroFaccina
                            )
                        }
                        CampoNome(
                            valore = nome,
                            posizione = indice + 1,
                            onCambia = { nomi[indice] = it },
                            modifier = Modifier.weight(1f)
                        )
                        if (nomi.size > 1) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .sticker(Superficie, 15.dp, ombra = false)
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = "Togli questo nome"
                                    ) {
                                        nomi.removeAt(indice)
                                        if (indice in foto.indices) {
                                            val tolta = foto.removeAt(indice)
                                            ambito.launch(Dispatchers.IO) {
                                                Foto.scartaTemporanea(contesto, tolta)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                IconaCestino(InkTenue)
                            }
                        }
                    }
                }

                erroreFoto?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = InkTerziario)
                }

                if (nomi.size < 4) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .sticker(Crema, 15.dp, ombra = false)
                            .clickable(role = Role.Button) {
                                nomi.add("")
                                foto.add(null)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconaPiu(Inchiostro, dimensione = 18.dp)
                        Text("Aggiungi un bambino", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            BottoneSticker(
                testo = "Cominciamo",
                onClick = { onConferma(nomi.toList(), foto.toList()) },
                abilitato = validi > 0,
                modifier = Modifier.fillMaxWidth()
            )

            SchedaSticker(sfondo = Menta) {
                Text("Hai già un backup?", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Se stai reinstallando l'app, ricarica qui il file che avevi salvato: " +
                        "nomi e storico tornano com'erano.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkTerziario
                )
                Spacer(Modifier.height(13.dp))
                BottoneSticker(
                    testo = "Ricarica un backup",
                    onClick = {
                        apriBackup.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    sfondo = Superficie,
                    modifier = Modifier.fillMaxWidth()
                )
                esito?.let {
                    Spacer(Modifier.height(11.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = InkTerziario)
                }
            }

            Text(
                "Nessun account, nessuna connessione: i dati restano nel telefono.",
                style = MaterialTheme.typography.labelMedium,
                color = InkTenue,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun CampoNome(
    valore: String,
    posizione: Int,
    onCambia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .sticker(Crema, 15.dp, ombra = false)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (valore.isEmpty()) {
            Text(
                "Nome del bambino $posizione",
                style = MaterialTheme.typography.bodyLarge,
                color = InkTenue
            )
        }
        BasicTextField(
            value = valore,
            onValueChange = onCambia,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = Inchiostro)),
            cursorBrush = SolidColor(Inchiostro),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
