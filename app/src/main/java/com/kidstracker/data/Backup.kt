package com.kidstracker.data

import com.kidstracker.domain.Bambino
import com.kidstracker.domain.Categoria
import com.kidstracker.domain.Giornata
import com.kidstracker.domain.Presenza
import com.kidstracker.domain.Salute
import com.kidstracker.domain.Voto
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Esportazione e importazione dei dati. Il JSON è il formato completo
 * (si reimporta senza perdite), il CSV serve per aprirlo in un foglio di calcolo.
 */
object Backup {

    const val VERSIONE = 1

    data class GiornataImportata(val nomeBambino: String, val giornata: Giornata)

    data class Importazione(
        val nomiBambini: List<String>,
        val giornate: List<GiornataImportata>
    )

    fun esportaJson(bambini: List<Bambino>, giornate: List<Giornata>): String {
        val perId = bambini.associateBy { it.id }
        val radice = JSONObject()
        radice.put("app", "kids-tracker")
        radice.put("versione", VERSIONE)
        radice.put("esportatoIl", LocalDate.now().toString())

        val arrayBambini = JSONArray()
        bambini.forEach { b ->
            arrayBambini.put(
                JSONObject()
                    .put("nome", b.nome)
                    .put("coloreIndex", b.coloreIndex)
            )
        }
        radice.put("bambini", arrayBambini)

        val arrayGiornate = JSONArray()
        giornate.sortedWith(compareBy({ it.data }, { it.bambinoId })).forEach { g ->
            val voti = JSONObject()
            Categoria.tutte.forEach { categoria ->
                g.voti[categoria]?.let { voti.put(categoria.name, it.name) }
            }
            arrayGiornate.put(
                JSONObject()
                    .put("bambino", perId[g.bambinoId]?.nome ?: "?")
                    .put("data", g.data.toString())
                    .put("presenza", g.presenza.name)
                    .put("voti", voti)
                    .put("salute", g.salute.name)
                    .put("nota", g.nota)
            )
        }
        radice.put("giornate", arrayGiornate)
        return radice.toString(2)
    }

    fun importaJson(testo: String): Importazione {
        val radice = JSONObject(testo)

        val nomi = mutableListOf<String>()
        val arrayBambini = radice.optJSONArray("bambini")
        if (arrayBambini != null) {
            for (i in 0 until arrayBambini.length()) {
                val nome = arrayBambini.getJSONObject(i).optString("nome").trim()
                if (nome.isNotEmpty()) nomi += nome
            }
        }

        val giornate = mutableListOf<GiornataImportata>()
        val arrayGiornate = radice.optJSONArray("giornate") ?: JSONArray()
        for (i in 0 until arrayGiornate.length()) {
            val oggetto = arrayGiornate.getJSONObject(i)
            val nomeBambino = oggetto.optString("bambino").trim()
            val data = runCatching { LocalDate.parse(oggetto.getString("data")) }.getOrNull() ?: continue
            if (nomeBambino.isEmpty()) continue

            val voti = mutableMapOf<Categoria, Voto>()
            oggetto.optJSONObject("voti")?.let { oggettoVoti ->
                Categoria.tutte.forEach { categoria ->
                    val valore = oggettoVoti.optString(categoria.name, "")
                    Voto.entries.firstOrNull { it.name == valore }?.let { voti[categoria] = it }
                }
            }

            giornate += GiornataImportata(
                nomeBambino = nomeBambino,
                giornata = Giornata(
                    bambinoId = 0L,
                    data = data,
                    presenza = Presenza.entries.firstOrNull { it.name == oggetto.optString("presenza") }
                        ?: Presenza.SCUOLA,
                    voti = voti,
                    salute = Salute.entries.firstOrNull { it.name == oggetto.optString("salute") }
                        ?: Salute.BENE,
                    nota = oggetto.optString("nota", "")
                )
            )
        }

        val nomiCompleti = (nomi + giornate.map { it.nomeBambino }).distinct()
        return Importazione(nomiCompleti, giornate)
    }

    fun esportaCsv(bambini: List<Bambino>, giornate: List<Giornata>): String {
        val perId = bambini.associateBy { it.id }
        val righe = StringBuilder()
        righe.append(
            "bambino;data;giorno_settimana;presenza;nanna;primo;secondo;dolce;entrata;uscita;" +
                "salute;indice_pappa;indice_giornata;nota\n"
        )
        giornate.sortedWith(compareBy({ it.data }, { it.bambinoId })).forEach { g ->
            val campi = listOf(
                perId[g.bambinoId]?.nome ?: "?",
                g.data.toString(),
                g.data.dayOfWeek.value.toString(),
                g.presenza.name,
                g.voti[Categoria.NANNA]?.name ?: "",
                g.voti[Categoria.PRIMO]?.name ?: "",
                g.voti[Categoria.SECONDO]?.name ?: "",
                g.voti[Categoria.DOLCE]?.name ?: "",
                g.voti[Categoria.ENTRATA]?.name ?: "",
                g.voti[Categoria.USCITA]?.name ?: "",
                g.salute.name,
                g.indicePappa?.toString() ?: "",
                g.indiceGiornata?.toString() ?: "",
                g.nota
            )
            righe.append(campi.joinToString(";") { campoCsv(it) }).append('\n')
        }
        return righe.toString()
    }

    private fun campoCsv(valore: String): String =
        if (valore.contains(';') || valore.contains('"') || valore.contains('\n')) {
            '"' + valore.replace("\"", "\"\"") + '"'
        } else {
            valore
        }
}
