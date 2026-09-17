package com.kidstracker

import com.kidstracker.data.Backup
import com.kidstracker.data.Excel
import com.kidstracker.domain.Bambino
import com.kidstracker.domain.Categoria
import com.kidstracker.domain.Giornata
import com.kidstracker.domain.Presenza
import com.kidstracker.domain.Salute
import com.kidstracker.domain.Voto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Il .xlsx è scritto a mano, quindi va verificato a mano: che lo zip abbia
 * i pezzi giusti, che ogni XML sia ben formato e che i numeri restino numeri.
 */
class ExcelTest {

    private fun scrivi(fogli: List<Excel.Foglio>): Map<String, String> {
        val uscita = ByteArrayOutputStream()
        Excel.scrivi(uscita, fogli)

        val contenuti = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(uscita.toByteArray())).use { zip ->
            while (true) {
                val voce = zip.nextEntry ?: break
                contenuti[voce.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }
        return contenuti
    }

    private fun benFormato(xml: String): Boolean = runCatching {
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }.isSuccess

    private val foglioProva = Excel.Foglio(
        nome = "Giornate",
        intestazioni = listOf("bambino", "indice"),
        righe = listOf(
            listOf(Excel.testo("Luca"), Excel.numero(50)),
            listOf(Excel.testo("Bianca"), Excel.numero(71.5)),
            listOf(Excel.testo("Senza indice"), Excel.numero(null))
        )
    )

    @Test
    fun `lo zip contiene tutti i pezzi che Excel si aspetta`() {
        val dentro = scrivi(listOf(foglioProva))

        assertTrue(dentro.containsKey("[Content_Types].xml"))
        assertTrue(dentro.containsKey("_rels/.rels"))
        assertTrue(dentro.containsKey("xl/workbook.xml"))
        assertTrue(dentro.containsKey("xl/_rels/workbook.xml.rels"))
        assertTrue(dentro.containsKey("xl/styles.xml"))
        assertTrue(dentro.containsKey("xl/worksheets/sheet1.xml"))
    }

    @Test
    fun `ogni pezzo e xml ben formato`() {
        val dentro = scrivi(listOf(foglioProva, foglioProva.copy(nome = "Secondo")))
        dentro.forEach { (nome, xml) ->
            assertTrue("XML malformato in $nome", benFormato(xml))
        }
        assertTrue(dentro.containsKey("xl/worksheets/sheet2.xml"))
    }

    @Test
    fun `i numeri restano numeri e le celle vuote spariscono`() {
        val foglio = scrivi(listOf(foglioProva))["xl/worksheets/sheet1.xml"]!!

        assertTrue(foglio.contains("<v>50</v>"))       // intero senza decimali inutili
        assertTrue(foglio.contains("<v>71.5</v>"))
        assertTrue(foglio.contains("""<c r="A2" t="inlineStr">"""))
        // La cella senza valore non viene proprio scritta.
        assertTrue(!foglio.contains("""r="B4""""))
    }

    @Test
    fun `i caratteri speciali non rompono il file`() {
        val foglio = Excel.Foglio(
            nome = "Prova",
            intestazioni = listOf("nota"),
            righe = listOf(listOf(Excel.testo("""pane & "burro" <tutto>""")))
        )
        val xml = scrivi(listOf(foglio))["xl/worksheets/sheet1.xml"]!!

        assertTrue(benFormato(xml))
        assertTrue(xml.contains("pane &amp; &quot;burro&quot; &lt;tutto&gt;"))
    }

    @Test
    fun `le colonne oltre la zeta continuano con due lettere`() {
        assertEquals("A", Excel.lettereColonna(0))
        assertEquals("Z", Excel.lettereColonna(25))
        assertEquals("AA", Excel.lettereColonna(26))
        assertEquals("AB", Excel.lettereColonna(27))
        assertEquals("BA", Excel.lettereColonna(52))
    }

    @Test
    fun `i nomi dei fogli vengono ripuliti`() {
        assertEquals("Per bambino", Excel.nomeFoglio("Per/bambino"))
        assertEquals(31, Excel.nomeFoglio("x".repeat(60)).length)
        assertEquals("Foglio", Excel.nomeFoglio("[]:*?"))
    }

    @Test
    fun `l'esportazione produce i tre fogli con i dati giusti`() {
        val bambini = listOf(
            Bambino(id = 1L, nome = "Luca", coloreIndex = 0),
            Bambino(id = 2L, nome = "Bianca", coloreIndex = 1)
        )
        val giornate = listOf(
            Giornata(
                bambinoId = 1L,
                data = LocalDate.of(2026, 9, 14), // lunedì
                voti = mapOf(Categoria.PRIMO to Voto.SI, Categoria.SECONDO to Voto.NO),
                salute = Salute.POCO_BENE,
                nota = "stanco"
            ),
            Giornata(
                bambinoId = 2L,
                data = LocalDate.of(2026, 9, 14),
                presenza = Presenza.ASSENTE
            )
        )

        val fogli = Backup.fogliExcel(bambini, giornate)
        assertEquals(listOf("Giornate", "Punteggi", "Riepilogo"), fogli.map { it.nome })
        assertEquals(2, fogli[0].righe.size)
        assertEquals(2, fogli[2].righe.size) // una riga per bambino

        val leggibile = scrivi(fogli)["xl/worksheets/sheet1.xml"]!!
        assertTrue(benFormato(leggibile))
        assertTrue(leggibile.contains("lunedì"))
        assertTrue(leggibile.contains("Poco bene"))
        assertTrue(leggibile.contains("così così") || leggibile.contains("sì"))

        // Nel foglio dei punteggi il primo (sì) vale 2 e il secondo (no) vale 0.
        val punteggi = scrivi(fogli)["xl/worksheets/sheet2.xml"]!!
        assertTrue(benFormato(punteggi))
        assertTrue(punteggi.contains("<v>2</v>"))
    }
}
