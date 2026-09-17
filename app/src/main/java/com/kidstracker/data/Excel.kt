package com.kidstracker.data

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Scrittore minimo di file .xlsx.
 *
 * Un file Excel è uno zip con dentro qualche XML: scrivendoli a mano l'app
 * esporta un foglio vero, apribile in Excel, Numbers o Fogli Google, senza
 * portarsi dietro una libreria da diversi megabyte.
 *
 * I numeri restano numeri, così si possono sommare e mettere in grafico
 * senza doverli riconvertire.
 */
object Excel {

    sealed interface Cella {
        data class Testo(val valore: String) : Cella
        data class Numero(val valore: Double) : Cella
        data object Vuota : Cella
    }

    data class Foglio(
        val nome: String,
        val intestazioni: List<String>,
        val righe: List<List<Cella>>
    )

    fun testo(valore: String?): Cella =
        if (valore.isNullOrEmpty()) Cella.Vuota else Cella.Testo(valore)

    fun numero(valore: Number?): Cella =
        if (valore == null) Cella.Vuota else Cella.Numero(valore.toDouble())

    fun scrivi(destinazione: OutputStream, fogli: List<Foglio>) {
        require(fogli.isNotEmpty()) { "serve almeno un foglio" }
        ZipOutputStream(destinazione).use { zip ->
            zip.voce("[Content_Types].xml", contentTypes(fogli.size))
            zip.voce("_rels/.rels", relsRadice())
            zip.voce("xl/workbook.xml", workbook(fogli))
            zip.voce("xl/_rels/workbook.xml.rels", relsWorkbook(fogli.size))
            zip.voce("xl/styles.xml", stili())
            fogli.forEachIndexed { indice, foglio ->
                zip.voce("xl/worksheets/sheet${indice + 1}.xml", foglioXml(foglio))
            }
        }
    }

    private fun ZipOutputStream.voce(nome: String, contenuto: String) {
        putNextEntry(ZipEntry(nome))
        write(contenuto.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val NS_PKG = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val TIPO_DOC = "application/vnd.openxmlformats-officedocument.spreadsheetml"

    private fun contentTypes(quantiFogli: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="$TIPO_DOC.sheet.main+xml"/>""")
        repeat(quantiFogli) { indice ->
            append("""<Override PartName="/xl/worksheets/sheet${indice + 1}.xml" ContentType="$TIPO_DOC.worksheet+xml"/>""")
        }
        append("""<Override PartName="/xl/styles.xml" ContentType="$TIPO_DOC.styles+xml"/>""")
        append("</Types>")
    }

    private fun relsRadice(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="$NS_PKG">""" +
            """<Relationship Id="rId1" Type="$NS_REL/officeDocument" Target="xl/workbook.xml"/>""" +
            "</Relationships>"

    private fun workbook(fogli: List<Foglio>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="$NS" xmlns:r="$NS_REL"><sheets>""")
        fogli.forEachIndexed { indice, foglio ->
            append(
                """<sheet name="${escape(nomeFoglio(foglio.nome))}" """ +
                    """sheetId="${indice + 1}" r:id="rId${indice + 1}"/>"""
            )
        }
        append("</sheets></workbook>")
    }

    private fun relsWorkbook(quantiFogli: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="$NS_PKG">""")
        repeat(quantiFogli) { indice ->
            append(
                """<Relationship Id="rId${indice + 1}" Type="$NS_REL/worksheet" """ +
                    """Target="worksheets/sheet${indice + 1}.xml"/>"""
            )
        }
        append(
            """<Relationship Id="rId${quantiFogli + 1}" Type="$NS_REL/styles" Target="styles.xml"/>"""
        )
        append("</Relationships>")
    }

    /** Due soli stili: normale e grassetto per la riga di intestazione. */
    private fun stili(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<styleSheet xmlns="$NS">""" +
            """<fonts count="2">""" +
            """<font><sz val="11"/><name val="Calibri"/></font>""" +
            """<font><b/><sz val="11"/><name val="Calibri"/></font>""" +
            "</fonts>" +
            """<fills count="2"><fill><patternFill patternType="none"/></fill>""" +
            """<fill><patternFill patternType="gray125"/></fill></fills>""" +
            """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="2">""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
            """<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
            "</cellXfs>" +
            """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
            "</styleSheet>"

    private fun foglioXml(foglio: Foglio): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="$NS"><sheetData>""")

        if (foglio.intestazioni.isNotEmpty()) {
            append("""<row r="1">""")
            foglio.intestazioni.forEachIndexed { colonna, titolo ->
                append(cellaXml(colonna, 1, Cella.Testo(titolo), grassetto = true))
            }
            append("</row>")
        }

        val scarto = if (foglio.intestazioni.isEmpty()) 0 else 1
        foglio.righe.forEachIndexed { indice, riga ->
            val numeroRiga = indice + 1 + scarto
            append("""<row r="$numeroRiga">""")
            riga.forEachIndexed { colonna, cella ->
                append(cellaXml(colonna, numeroRiga, cella, grassetto = false))
            }
            append("</row>")
        }

        append("</sheetData></worksheet>")
    }

    private fun cellaXml(colonna: Int, riga: Int, cella: Cella, grassetto: Boolean): String {
        val riferimento = "${lettereColonna(colonna)}$riga"
        val stile = if (grassetto) " s=\"1\"" else ""
        return when (cella) {
            is Cella.Vuota -> ""
            is Cella.Numero -> """<c r="$riferimento"$stile><v>${numeroXml(cella.valore)}</v></c>"""
            is Cella.Testo ->
                """<c r="$riferimento"$stile t="inlineStr"><is><t xml:space="preserve">""" +
                    escape(cella.valore) + "</t></is></c>"
        }
    }

    private fun numeroXml(valore: Double): String =
        if (valore == valore.toLong().toDouble()) valore.toLong().toString() else valore.toString()

    /** 0 -> A, 25 -> Z, 26 -> AA. */
    internal fun lettereColonna(indice: Int): String {
        var n = indice
        val lettere = StringBuilder()
        while (true) {
            lettere.insert(0, ('A' + n % 26))
            n = n / 26 - 1
            if (n < 0) break
        }
        return lettere.toString()
    }

    private val VIETATI_NEI_NOMI = charArrayOf('\\', '/', '?', '*', '[', ']', ':')

    /**
     * Excel rifiuta certi caratteri nei nomi dei fogli e non ne accetta più di 31.
     * Li sostituiamo con uno spazio invece di cancellarli, altrimenti
     * "Per/bambino" diventerebbe "Perbambino".
     */
    internal fun nomeFoglio(nome: String): String {
        val pulito = nome
            .map { if (it in VIETATI_NEI_NOMI) ' ' else it }
            .joinToString("")
            .replace(Regex("\\s+"), " ")
            .trim()
        return pulito.take(31).trim().ifEmpty { "Foglio" }
    }

    internal fun escape(testo: String): String = buildString(testo.length) {
        testo.forEach { carattere ->
            when {
                carattere == '&' -> append("&amp;")
                carattere == '<' -> append("&lt;")
                carattere == '>' -> append("&gt;")
                carattere == '"' -> append("&quot;")
                carattere == '\'' -> append("&apos;")
                // I caratteri di controllo non sono validi in XML: meglio toglierli
                // che produrre un file che Excel rifiuta di aprire.
                carattere.code < 0x20 && carattere != '\t' && carattere != '\n' && carattere != '\r' -> Unit
                else -> append(carattere)
            }
        }
    }
}
