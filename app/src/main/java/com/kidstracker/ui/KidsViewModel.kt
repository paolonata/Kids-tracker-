package com.kidstracker.ui

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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class Periodo(val giorni: Int, val etichetta: String) {
    SETTIMANA(7, "7 giorni"),
    MESE(30, "30 giorni"),
    TRIMESTRE(90, "3 mesi")
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
    val preferenze: Preferenze
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
    private val _periodo = MutableStateFlow(Periodo.MESE)
    private val _statoAvvio = MutableStateFlow<StatoAvvio>(StatoAvvio.Caricamento)

    val data: StateFlow<LocalDate> = _data.asStateFlow()
    val mese: StateFlow<YearMonth> = _mese.asStateFlow()
    val periodo: StateFlow<Periodo> = _periodo.asStateFlow()
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
        if (presenza == Presenza.ASSENTE) {
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

    fun creaBambini(nomi: List<String>, alTermine: () -> Unit = {}) {
        val puliti = nomi.map { it.trim() }.filter { it.isNotEmpty() }
        if (puliti.isEmpty()) return
        viewModelScope.launch {
            repo.creaBambini(puliti)
            _statoAvvio.value = StatoAvvio.Pronta
            alTermine()
        }
    }

    fun rinomina(bambino: Bambino, nome: String) {
        if (nome.isBlank()) return
        viewModelScope.launch { repo.rinomina(bambino, nome) }
    }

    fun cancellaTutteLeGiornate(alTermine: () -> Unit = {}) {
        viewModelScope.launch {
            repo.cancellaGiornate()
            alTermine()
        }
    }

    suspend fun tutteLeGiornate(): List<Giornata> = repo.tutteLeGiornate()

    suspend fun applicaImportazione(
        nomi: List<String>,
        giornate: List<Backup.GiornataImportata>,
        sostituisci: Boolean
    ): Int {
        val esistenti = repo.bambini.first()
        val perNome = esistenti.associateBy { it.nome.lowercase() }.toMutableMap()

        val mancanti = nomi.filter { !perNome.containsKey(it.lowercase()) }
        if (mancanti.isNotEmpty()) {
            repo.creaBambini(mancanti)
            repo.bambini.first().forEach { perNome[it.nome.lowercase()] = it }
        }

        val convertite = giornate.mapNotNull { importata ->
            val bambino = perNome[importata.nomeBambino.lowercase()] ?: return@mapNotNull null
            importata.giornata.copy(bambinoId = bambino.id)
        }
        repo.importa(convertite, sostituisci)
        _statoAvvio.value = StatoAvvio.Pronta
        return convertite.size
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KidsTrackerApp
                KidsViewModel(app.contenitore.repository, app.contenitore.preferenze)
            }
        }
    }
}
