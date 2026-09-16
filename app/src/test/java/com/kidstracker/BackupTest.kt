package com.kidstracker

import com.kidstracker.data.Backup
import com.kidstracker.domain.Bambino
import com.kidstracker.domain.Categoria
import com.kidstracker.domain.Giornata
import com.kidstracker.domain.Presenza
import com.kidstracker.domain.Salute
import com.kidstracker.domain.Voto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupTest {

    private val bambini = listOf(
        Bambino(id = 1L, nome = "Aurora", coloreIndex = 0),
        Bambino(id = 2L, nome = "Tommaso", coloreIndex = 1)
    )

    private val giornate = listOf(
        Giornata(
            bambinoId = 1L,
            data = LocalDate.of(2026, 9, 14),
            presenza = Presenza.SCUOLA,
            voti = mapOf(
                Categoria.NANNA to Voto.COSI_COSI,
                Categoria.PRIMO to Voto.NO,
                Categoria.DOLCE to Voto.SI
            ),
            salute = Salute.POCO_BENE,
            nota = "Ha pianto all'ingresso; nota con ; e \"virgolette\""
        ),
        Giornata(
            bambinoId = 2L,
            data = LocalDate.of(2026, 9, 15),
            presenza = Presenza.ASSENTE,
            salute = Salute.FEBBRE
        )
    )

    @Test
    fun `il json esportato si rilegge senza perdere nulla`() {
        val testo = Backup.esportaJson(bambini, giornate)
        val riletto = Backup.importaJson(testo)

        assertEquals(listOf("Aurora", "Tommaso"), riletto.nomiBambini)
        assertEquals(2, riletto.giornate.size)

        val primo = riletto.giornate.first { it.nomeBambino == "Aurora" }.giornata
        assertEquals(LocalDate.of(2026, 9, 14), primo.data)
        assertEquals(Presenza.SCUOLA, primo.presenza)
        assertEquals(Salute.POCO_BENE, primo.salute)
        assertEquals(Voto.COSI_COSI, primo.voti[Categoria.NANNA])
        assertEquals(Voto.NO, primo.voti[Categoria.PRIMO])
        assertEquals(Voto.SI, primo.voti[Categoria.DOLCE])
        assertEquals(null, primo.voti[Categoria.SECONDO])
        assertEquals(giornate[0].nota, primo.nota)

        val secondo = riletto.giornate.first { it.nomeBambino == "Tommaso" }.giornata
        assertEquals(Presenza.ASSENTE, secondo.presenza)
        assertTrue(secondo.voti.isEmpty())
    }

    @Test
    fun `un json senza la sezione bambini prende i nomi dalle giornate`() {
        val testo = """
            {"app":"kids-tracker","versione":1,
             "giornate":[{"bambino":"Aurora","data":"2026-09-14","voti":{"PRIMO":"SI"}}]}
        """.trimIndent()

        val riletto = Backup.importaJson(testo)
        assertEquals(listOf("Aurora"), riletto.nomiBambini)
        assertEquals(1, riletto.giornate.size)
    }

    @Test
    fun `le giornate senza data vengono saltate invece di far fallire tutto`() {
        val testo = """
            {"giornate":[
              {"bambino":"Aurora","data":"non-una-data","voti":{"PRIMO":"SI"}},
              {"bambino":"Aurora","data":"2026-09-14","voti":{"PRIMO":"SI"}}
            ]}
        """.trimIndent()

        assertEquals(1, Backup.importaJson(testo).giornate.size)
    }

    @Test
    fun `il csv protegge il punto e virgola dentro le note`() {
        val righe = Backup.esportaCsv(bambini, giornate).lines()

        assertTrue(righe[0].startsWith("bambino;data;giorno_settimana;presenza"))
        val rigaAurora = righe.first { it.startsWith("Aurora;") }
        assertTrue(rigaAurora.contains("\"Ha pianto all'ingresso; nota con ; e \"\"virgolette\"\"\""))
        // primo=NO(0), secondo=assente, dolce=SI(2) -> 50%
        assertTrue(rigaAurora.contains(";50;"))
    }
}
