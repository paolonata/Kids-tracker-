package com.kidstracker.data

import com.kidstracker.domain.Bambino
import com.kidstracker.domain.Categoria
import com.kidstracker.domain.Giornata
import com.kidstracker.domain.Presenza
import com.kidstracker.domain.Salute
import com.kidstracker.domain.Voto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class KidsRepository(private val dao: KidsDao) {

    val bambini: Flow<List<Bambino>> =
        dao.osservaBambini().map { lista -> lista.map { it.aDominio() } }

    suspend fun quantiBambini(): Int = dao.quantiBambini()

    suspend fun creaBambini(nomi: List<String>) {
        nomi.forEachIndexed { indice, nome ->
            dao.inserisciBambino(
                BambinoEntity(nome = nome.trim(), coloreIndex = indice, ordine = indice)
            )
        }
    }

    suspend fun rinomina(bambino: Bambino, nome: String) {
        dao.aggiornaBambino(
            BambinoEntity(
                id = bambino.id,
                nome = nome.trim(),
                coloreIndex = bambino.coloreIndex,
                ordine = bambino.coloreIndex
            )
        )
    }

    suspend fun eliminaBambino(id: Long) = dao.eliminaBambino(id)

    fun giornate(da: LocalDate, a: LocalDate): Flow<List<Giornata>> =
        dao.osservaIntervallo(da.toEpochDay(), a.toEpochDay())
            .map { lista -> lista.map { it.aDominio() } }

    fun giornata(bambinoId: Long, data: LocalDate): Flow<Giornata?> =
        dao.osservaGiornata(bambinoId, data.toEpochDay()).map { it?.aDominio() }

    fun giornoDiTutti(data: LocalDate): Flow<List<Giornata>> =
        dao.osservaGiorno(data.toEpochDay()).map { lista -> lista.map { it.aDominio() } }

    suspend fun salva(giornata: Giornata) {
        if (giornata.vuota) {
            dao.elimina(giornata.bambinoId, giornata.data.toEpochDay())
        } else {
            dao.salva(giornata.aEntita())
        }
    }

    suspend fun tutteLeGiornate(): List<Giornata> = dao.tutte().map { it.aDominio() }

    suspend fun importa(giornate: List<Giornata>, sostituisci: Boolean) {
        if (sostituisci) dao.cancellaGiornate()
        dao.salvaTutte(giornate.filterNot { it.vuota }.map { it.aEntita() })
    }

    suspend fun cancellaGiornate() = dao.cancellaGiornate()
}

// ---- mappatura fra tabella e dominio -------------------------------------------------

internal fun BambinoEntity.aDominio() = Bambino(id = id, nome = nome, coloreIndex = coloreIndex)

internal fun GiornataEntity.aDominio(): Giornata {
    val voti = buildMap {
        voto(nanna)?.let { put(Categoria.NANNA, it) }
        voto(primo)?.let { put(Categoria.PRIMO, it) }
        voto(secondo)?.let { put(Categoria.SECONDO, it) }
        voto(dolce)?.let { put(Categoria.DOLCE, it) }
        voto(entrata)?.let { put(Categoria.ENTRATA, it) }
        voto(uscita)?.let { put(Categoria.USCITA, it) }
    }
    return Giornata(
        bambinoId = bambinoId,
        data = LocalDate.ofEpochDay(giorno),
        presenza = Presenza.entries.firstOrNull { it.name == presenza } ?: Presenza.SCUOLA,
        voti = voti,
        salute = Salute.entries.firstOrNull { it.name == salute } ?: Salute.BENE,
        nota = nota
    )
}

internal fun Giornata.aEntita() = GiornataEntity(
    bambinoId = bambinoId,
    giorno = data.toEpochDay(),
    presenza = presenza.name,
    nanna = voti[Categoria.NANNA]?.name,
    primo = voti[Categoria.PRIMO]?.name,
    secondo = voti[Categoria.SECONDO]?.name,
    dolce = voti[Categoria.DOLCE]?.name,
    entrata = voti[Categoria.ENTRATA]?.name,
    uscita = voti[Categoria.USCITA]?.name,
    salute = salute.name,
    nota = nota,
    aggiornatoIl = System.currentTimeMillis()
)

private fun voto(nome: String?): Voto? = Voto.entries.firstOrNull { it.name == nome }
