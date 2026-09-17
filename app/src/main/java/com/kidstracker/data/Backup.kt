package com.kidstracker.data

import com.kidstracker.domain.Bambino
import com.kidstracker.domain.Categoria
import com.kidstracker.domain.Giornata
import com.kidstracker.domain.Giudizio
import com.kidstracker.domain.Statistiche
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

    /**
     * 2: i bambini portano con sé la foto in base64 e la presenza può valere FESTIVO.
     * I file della versione 1 restano leggibili: i campi nuovi sono semplicemente assenti.
     */
    const val VERSIONE = 2

    data class GiornataImportata(val nomeBambino: String, val giornata: Giornata)

    data class Importazione(
        val nomiBambini: List<String>,
        val giornate: List<GiornataImportata>,
        /** Foto in base64 per nome del bambino, vuota se il backup non ne aveva. */
        val fotoPerNome: Map<String, String> = emptyMap()
    )

    /**
     * [fotoBase64] riceve il nome del file della foto e restituisce il contenuto
     * codificato: sta al chiamante leggerlo dal disco, qui non si tocca il filesystem.
     */
    fun esportaJson(
        bambini: List<Bambino>,
        giornate: List<Giornata>,
        fotoBase64: (Bambino) -> String? = { null }
    ): String {
        val perId = bambini.associateBy { it.id }
        val radice = JSONObject()
        radice.put("app", "kids-tracker")
        radice.put("versione", VERSIONE)
        radice.put("esportatoIl", LocalDate.now().toString())

        val arrayBambini = JSONArray()
        bambini.forEach { b ->
            val oggetto = JSONObject()
                .put("nome", b.nome)
                .put("coloreIndex", b.coloreIndex)
            fotoBase64(b)?.takeIf { it.isNotBlank() }?.let { oggetto.put("foto", it) }
            arrayBambini.put(oggetto)
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
        val foto = mutableMapOf<String, String>()
        val arrayBambini = radice.optJSONArray("bambini")
        if (arrayBambini != null) {
            for (i in 0 until arrayBambini.length()) {
                val bambino = arrayBambini.getJSONObject(i)
                val nome = bambino.optString("nome").trim()
                if (nome.isEmpty()) continue
                nomi += nome
                bambino.optString("foto", "").takeIf { it.isNotBlank() }?.let { foto[nome] = it }
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
        return Importazione(nomiCompleti, giornate, foto)
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


    // ---- foglio di calcolo -----------------------------------------------------------

    private val NOMI_GIORNI = listOf(
        "lunedì", "martedì", "mercoledì", "giovedì", "venerdì", "sabato", "domenica"
    )

    private fun etichetta(voto: Voto?): String = voto?.etichetta ?: ""

    /**
     * Tre fogli: uno leggibile, uno con gli stessi dati in numeri (per farci
     * i grafici in Excel) e un riepilogo per bambino.
     */
    fun fogliExcel(bambini: List<Bambino>, giornate: List<Giornata>): List<Excel.Foglio> {
        val perId = bambini.associateBy { it.id }
        val ordinate = giornate.sortedWith(compareBy({ it.data }, { it.bambinoId }))

        val leggibile = Excel.Foglio(
            nome = "Giornate",
            intestazioni = listOf(
                "bambino", "data", "giorno", "presenza", "nanna", "primo", "secondo",
                "dolce", "entrata", "uscita", "salute", "indice pappa", "indice giornata", "nota"
            ),
            righe = ordinate.map { g ->
                listOf(
                    Excel.testo(perId[g.bambinoId]?.nome),
                    Excel.testo(g.data.toString()),
                    Excel.testo(NOMI_GIORNI[g.data.dayOfWeek.value - 1]),
                    Excel.testo(g.presenza.etichetta),
                    Excel.testo(etichetta(g.voti[Categoria.NANNA])),
                    Excel.testo(etichetta(g.voti[Categoria.PRIMO])),
                    Excel.testo(etichetta(g.voti[Categoria.SECONDO])),
                    Excel.testo(etichetta(g.voti[Categoria.DOLCE])),
                    Excel.testo(etichetta(g.voti[Categoria.ENTRATA])),
                    Excel.testo(etichetta(g.voti[Categoria.USCITA])),
                    Excel.testo(g.salute.etichetta),
                    Excel.numero(g.indicePappa),
                    Excel.numero(g.indiceGiornata),
                    Excel.testo(g.nota)
                )
            }
        )

        val punteggi = Excel.Foglio(
            nome = "Punteggi",
            intestazioni = listOf("bambino", "data", "giorno") +
                Categoria.tutte.map { it.etichetta.lowercase() } +
                listOf("indice pappa", "indice giornata", "stava poco bene"),
            righe = ordinate.map { g ->
                listOf(
                    Excel.testo(perId[g.bambinoId]?.nome),
                    Excel.testo(g.data.toString()),
                    Excel.numero(g.data.dayOfWeek.value)
                ) + Categoria.tutte.map { Excel.numero(g.voti[it]?.punti) } + listOf(
                    Excel.numero(g.indicePappa),
                    Excel.numero(g.indiceGiornata),
                    Excel.numero(if (g.stavaPocoBene) 1 else 0)
                )
            }
        )

        val riepilogo = Excel.Foglio(
            nome = "Riepilogo",
            intestazioni = listOf(
                "bambino", "giornate segnate", "indice giornata medio", "indice pappa medio",
                "giornate buone", "così così", "difficili", "assenze", "festivi", "striscia record"
            ),
            righe = bambini.map { bambino ->
                val tutte = ordinate.filter { it.bambinoId == bambino.id && !it.vuota }
                // I festivi sono giornate chiuse: si contano a parte e restano
                // fuori da medie e conteggi, come in tutto il resto dell'app.
                val sue = Statistiche.analizzabili(tutte)
                val indici = sue.mapNotNull { it.indiceGiornata }
                val pappe = sue.mapNotNull { it.indicePappa }
                listOf(
                    Excel.testo(bambino.nome),
                    Excel.numero(sue.size),
                    Excel.numero(indici.takeIf { it.isNotEmpty() }?.average()?.arrotonda()),
                    Excel.numero(pappe.takeIf { it.isNotEmpty() }?.average()?.arrotonda()),
                    Excel.numero(sue.count { it.giudizio == Giudizio.BUONO }),
                    Excel.numero(sue.count { it.giudizio == Giudizio.COSI_COSI }),
                    Excel.numero(sue.count { it.giudizio == Giudizio.DIFFICILE }),
                    Excel.numero(sue.count { it.presenza == Presenza.ASSENTE }),
                    Excel.numero(tutte.count { it.festiva }),
                    Excel.numero(Statistiche.strisciaRecord(sue))
                )
            }
        )

        return listOf(leggibile, punteggi, riepilogo)
    }

    private fun Double.arrotonda(): Double = Math.round(this * 10.0) / 10.0

    private fun campoCsv(valore: String): String =
        if (valore.contains(';') || valore.contains('"') || valore.contains('\n')) {
            '"' + valore.replace("\"", "\"\"") + '"'
        } else {
            valore
        }
}
