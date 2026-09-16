package com.kidstracker.ui.schermate

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.kidstracker.domain.Voto
import com.kidstracker.ui.componenti.BottoneSticker
import com.kidstracker.ui.componenti.Faccina
import com.kidstracker.ui.componenti.IconaCestino
import com.kidstracker.ui.componenti.IconaPiu
import com.kidstracker.ui.componenti.IntestazionePrugna
import com.kidstracker.ui.componenti.SchedaSticker
import com.kidstracker.ui.componenti.sticker
import com.kidstracker.ui.tema.Crema
import com.kidstracker.ui.tema.Inchiostro
import com.kidstracker.ui.tema.InkTenue
import com.kidstracker.ui.tema.InkTerziario
import com.kidstracker.ui.tema.coloreBambino

@Composable
fun SchermataOnboarding(
    onConferma: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val nomi = remember { mutableStateListOf("", "") }
    val validi = nomi.count { it.isNotBlank() }

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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SchedaSticker(modifier = Modifier.offset(y = (-20).dp)) {
                Text("I nomi", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Servono solo per distinguere le schede: restano su questo telefono " +
                        "e non escono da qui. Li puoi cambiare quando vuoi.",
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
                        Faccina(
                            voto = Voto.SI,
                            dimensione = 34.dp,
                            riempimento = coloreBambino(indice),
                            tratto = Inchiostro
                        )
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
                                    .sticker(Color.White, 15.dp, ombra = false)
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = "Togli questo nome"
                                    ) { nomi.removeAt(indice) },
                                contentAlignment = Alignment.Center
                            ) {
                                IconaCestino(InkTenue)
                            }
                        }
                    }
                }

                if (nomi.size < 4) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .sticker(Crema, 15.dp, ombra = false)
                            .clickable(role = Role.Button) { nomi.add("") }
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
                onClick = { onConferma(nomi.toList()) },
                abilitato = validi > 0,
                modifier = Modifier.fillMaxWidth()
            )

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
