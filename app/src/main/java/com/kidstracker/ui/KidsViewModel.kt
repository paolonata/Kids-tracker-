package com.kidstracker.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kidstracker.KidsTrackerApp
import com.kidstracker.data.Backup
import com.kidstracker.data.Foto
import com.kidstracker.data.KidsRepository
import com.kidstracker.data.Preferenze
import com.kidstracker.domain.Bambino
import com.kidstracker.domain.Categoria
import com.kidstracker.domain.Giornata
import com.kidstracker.domain.Presenza
import com.kidstracker.domain.Salute
import com.kidstracker.domain.Voto
import com.kidstracker.ui.tema.TemaScelto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

enum class Periodo(val giorni: Int, val etichetta: String) {
    SETTIMANA(7, "7 giorni"),
    DUE_SETTIMANE(14, "14 giorni"),
    MESE(30, "30 giorni"),
    DUE_MESI(60, "60 giorni"),
    TRE_MESI(90, "90 giorni")
}

/** Stato iniziale: serve l'onboarding o si può entrare? */
sealed interface StatoAvvio {
    data object Caricamento : StatoAvvio
    data object ServeOnboarding : StatoAvvio
    data object Pronta : StatoAvvio
}

@OptIn(ExperimentalCoroutinesApi::class)
class KidsViewModel(
    private val repo: KidsRepository,
    val preferenze: Preferenze,
    private val contesto: Context
) : ViewModel() {

    private val oggi: LocalDate get() = LocalDate.now()

    /** Il tema scelto vive qui perché serve prima ancora di comporre le schermate. */
    var tema by mutableStateOf(TemaScelto.daNome(preferenze.temaScelto))
        private set

    fun impostaTema(nuovo: TemaScelto) {
        tema = nuovo
        preferenze.temaScelto = nuovo.name
    }

    val bambini: StateFlow<List<Bambino>> =
        repo.bambini.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _idSelezionato = MutableStateFlow<Long?>(null)
    private val _data = MutableStateFlow(LocalDate.now())
    private val _mese = MutableStateFlow(YearMonth.now())
    private val _periodo = MutableStateFlow(Periodo.DUE_SETTIMANE)
    // null = si guarda un numero di giorni all'indietro (_periodo). Un mese
    // scelto qui prende il sopravvento: sono due modi diversi di guardare
    // lo stesso Andamento, non due schermate.
    private val _meseAndamento = MutableStateFlow<YearMonth?>(null)
    private val _statoAvvio = MutableStateFlow<StatoAvvio>(StatoAvvio.Caricamento)

    val data: StateFlow<LocalDate> = _data.asStateFlow()
    val mese: StateFlow<YearMonth> = _mese.asStateFlow()
    val periodo: StateFlow<Periodo> = _periodo.asStateFlow()
    val meseAndamento: StateFlow<YearMonth?> = _meseAndamento.asStateFlow()
    val statoAvvio: StateFlow<StatoAvvio> = _statoAvvio.asStateFlow()

    /** Se non è stato scelto nessuno, vale il primo della lista. */
    val bambinoCorrente: StateFlow<Bambino?> =
        combine(bambini, _idSelezionato) { lista, id ->
            lista.firstOrNull { it.id == id } ?: lista.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Le giornate del giorno in modifica, per tutti i bambini. */
    val giornateDelGiorno: StateFlow<List<Giornata>> =
        _data.flatMapLatest { repo.giornoDiTutti(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val giornateDelMese: StateFlow<List<Giornata>> =
        _mese.flatMapLatest { mese -> repo.giornate(mese.atDay(1), mese.atEndOfMonth()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Sei mesi di storico: basta per tutti i grafici e le scoperte. */
    val storico: StateFlow<List<Giornata>> =
        repo.giornate(LocalDate.now().minusDays(400), LocalDate.now().plusDays(1))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _statoAvvio.value =
                if (repo.quantiBambini() == 0) StatoAvvio.ServeOnboarding else StatoAvvio.Pronta
        }
    }

    // ---- navigazione interna ----------------------------------------------------------

    fun seleziona(bambinoId: Long) {
        _idSelezionato.value = bambinoId
    }

    fun apriGiorno(giorno: LocalDate) {
        _data.value = giorno
    }

    fun tornaAOggi() {
        _data.value = oggi
        _mese.value = YearMonth.now()
    }

    fun meseIndietro() {
        _mese.value = _mese.value.minusMonths(1)
    }

    fun meseAvanti() {
        if (_mese.value.isBefore(YearMonth.now())) _mese.value = _mese.value.plusMonths(1)
    }

    fun impostaPeriodo(nuovo: Periodo) {
        _periodo.value = nuovo
        _meseAndamento.value = null
    }

    /** Passa alla vista per mese in Andamento, di norma su quello corrente. */
    fun mostraMeseAndamento() {
        _meseAndamento.value = _meseAndamento.value ?: YearMonth.now()
    }

    fun meseAndamentoIndietro() {
        _meseAndamento.value = (_meseAndamento.value ?: YearMonth.now()).minusMonths(1)
    }

    fun meseAndamentoAvanti() {
        val corrente = _meseAndamento.value ?: return
        if (corrente.isBefore(YearMonth.now())) _meseAndamento.value = corrente.plusMonths(1)
    }

    // ---- modifiche: si salva a ogni tocco, senza bottone salva ------------------------

    private fun modifica(bambinoId: Long, blocco: (Giornata) -> Giornata) {
        val giorno = _data.value
        viewModelScope.launch {
            val attuale = repo.giornata(bambinoId, giorno).first()
                ?: Giornata(bambinoId = bambinoId, data = giorno)
            repo.salva(blocco(attuale))
        }
    }

    fun impostaVoto(bambinoId: Long, categoria: Categoria, voto: Voto) = modifica(bambinoId) {
        it.copy(voti = it.voti + (categoria to voto))
    }

    fun impostaPresenza(bambinoId: Long, presenza: Presenza) = modifica(bambinoId) {
        // Assente e festivo sono giornate chiuse: le faccine rimaste da prima
        // resterebbero invisibili ma continuerebbero a pesare sui conti.
        if (presenza == Presenza.ASSENTE || presenza == Presenza.FESTIVO) {
            it.copy(presenza = presenza, voti = emptyMap())
        } else {
            it.copy(presenza = presenza)
        }
    }

    fun impostaSalute(bambinoId: Long, salute: Salute) = modifica(bambinoId) {
        it.copy(salute = salute)
    }

    fun impostaNota(bambinoId: Long, nota: String) = modifica(bambinoId) {
        it.copy(nota = nota)
    }

    // ---- onboarding e impostazioni ----------------------------------------------------

    /**
     * [fotoTemporanee] è parallela a [nomi] e può contenere dei buchi: le
     * schede senza nome vengono scartate insieme alla loro foto, così
     * l'accoppiamento con gli id restituiti resta corretto. Ogni voce è già
     * un file scritto dall'onboarding (vedi [Foto.importaTemporanea]): qui
     * basta spostarlo sul nome definitivo, senza più toccare Uri di sistema.
     */
    fun creaBambini(
        nomi: List<String>,
        fotoTemporanee: List<String?> = emptyList(),
        alTermine: () -> Unit = {}
    ) {
        val schede = nomi.mapIndexed { indice, nome -> nome.trim() to fotoTemporanee.getOrNull(indice) }
            .filter { (nome, _) -> nome.isNotEmpty() }
        if (schede.isEmpty()) return
        viewModelScope.launch {
            val id = repo.creaBambini(schede.map { it.first })
            schede.forEachIndexed { indice, (_, temporanea) ->
                val bambinoId = id.getOrNull(indice) ?: return@forEachIndexed
                if (temporanea == null) return@forEachIndexed
                val nome = withContext(Dispatchers.IO) {
                    Foto.confermaTemporanea(contesto, temporanea, bambinoId)
                } ?: return@forEachIndexed
                repo.impostaFoto(bambinoId, nome)
            }
            _statoAvvio.value = StatoAvvio.Pronta
            alTermine()
        }
    }

    fun rinomina(bambino: Bambino, nome: String) {
        if (nome.isBlank()) return
        viewModelScope.launch { repo.rinomina(bambino, nome) }
    }

    /**
     * Importa la foto scelta dalla galleria per il bambino con questo id.
     *
     * Prende i byte già letti, non una Uri: leggerli richiede di chiamare
     * [Foto.leggiByte] appena la Uri arriva dal selettore, prima ancora di
     * entrare in questa funzione — vedi il suo commento sul perché.
     *
     * Prende l'id, non l'oggetto [Bambino]: il selettore di sistema può far
     * ricreare l'app (la chiude per liberare memoria mentre è aperto), e nel
     * primo istante dopo la ricreazione la lista in [bambini] può ancora
     * essere quella vuota di partenza, prima che Room le mandi lo stato vero.
     * Cercare qui, con una lettura fresca dal database, invece che nella
     * lista già in mano a Compose, evita che una foto scelta bene si perda
     * in silenzio proprio in quella finestra.
     *
     * Restituisce un [Result]: la schermata può così mostrare il motivo vero
     * di un fallimento (formato non decodificabile, storage piena...)
     * invece di un "non ci sono riuscito" che non dice nulla.
     */
    suspend fun scegliFoto(bambinoId: Long, byte: ByteArray): Result<Unit> {
        val bambino = repo.bambini.first().firstOrNull { it.id == bambinoId }
            ?: return Result.failure(IllegalStateException("Bambino non trovato (id=$bambinoId)"))
        val nuova = withContext(Dispatchers.IO) {
            Foto.importa(contesto, bambino.id, byte)
        }.getOrElse { return Result.failure(it) }
        repo.impostaFoto(bambino.id, nuova)
        withContext(Dispatchers.IO) { Foto.elimina(contesto, bambino.foto) }
        return Result.success(Unit)
    }

    fun rimuoviFoto(bambino: Bambino) {
        viewModelScope.launch {
            repo.impostaFoto(bambino.id, null)
            withContext(Dispatchers.IO) { Foto.elimina(contesto, bambino.foto) }
        }
    }

    /** Il JSON di backup si porta dietro anche le foto, codificate in base64. */
    suspend fun backupJson(): String {
        val elenco = repo.bambini.first()
        val giornate = repo.tutteLeGiornate()
        return withContext(Dispatchers.IO) {
            Backup.esportaJson(elenco, giornate) { Foto.base64(contesto, it.foto) }
        }
    }

    fun cancellaTutteLeGiornate(alTermine: () -> Unit = {}) {
        viewModelScope.launch {
            repo.cancellaGiornate()
            alTermine()
        }
    }

    suspend fun tutteLeGiornate(): List<Giornata> = repo.tutteLeGiornate()

    suspend fun applicaImportazione(
        importazione: Backup.Importazione,
        sostituisci: Boolean
    ): Int {
        val esistenti = repo.bambini.first()
        val perNome = esistenti.associateBy { it.nome.lowercase() }.toMutableMap()

        val mancanti = importazione.nomiBambini.filter { !perNome.containsKey(it.lowercase()) }
        if (mancanti.isNotEmpty()) {
            repo.creaBambini(mancanti)
            repo.bambini.first().forEach { perNome[it.nome.lowercase()] = it }
        }

        ripristinaFoto(importazione.fotoPerNome, perNome)

        val convertite = importazione.giornate.mapNotNull { importata ->
            val bambino = perNome[importata.nomeBambino.lowercase()] ?: return@mapNotNull null
            importata.giornata.copy(bambinoId = bambino.id)
        }
        repo.importa(convertite, sostituisci)
        _statoAvvio.value = StatoAvvio.Pronta
        return convertite.size
    }

    /** Le foto del backup sostituiscono quelle sul telefono solo dove ce ne sono. */
    private suspend fun ripristinaFoto(
        codificate: Map<String, String>,
        perNome: Map<String, Bambino>
    ) {
        codificate.forEach { (nome, base64) ->
            val bambino = perNome[nome.lowercase()] ?: return@forEach
            val byte = Foto.daBase64(base64) ?: return@forEach
            val salvata = withContext(Dispatchers.IO) {
                Foto.salvaByte(contesto, bambino.id, byte)
            } ?: return@forEach
            repo.impostaFoto(bambino.id, salvata)
            withContext(Dispatchers.IO) { Foto.elimina(contesto, bambino.foto) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KidsTrackerApp
                KidsViewModel(
                    app.contenitore.repository,
                    app.contenitore.preferenze,
                    app.applicationContext
                )
            }
        }
    }
}
