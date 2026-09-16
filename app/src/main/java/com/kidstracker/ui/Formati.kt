package com.kidstracker.ui

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Formati {

    private val italiano: Locale = Locale.forLanguageTag("it-IT")

    private val giornoEMese = DateTimeFormatter.ofPattern("d MMMM", italiano)
    private val giornoMeseCorto = DateTimeFormatter.ofPattern("d MMM", italiano)
    private val soloMese = DateTimeFormatter.ofPattern("LLLL", italiano)
    private val settimanaEGiorno = DateTimeFormatter.ofPattern("EEEE d", italiano)

    fun dataLunga(data: LocalDate): String = maiuscola(data.format(giornoEMese))

    fun dataCorta(data: LocalDate): String = data.format(giornoMeseCorto)

    fun giornoConSettimana(data: LocalDate): String = maiuscola(data.format(settimanaEGiorno))

    fun mese(anno: Int, mese: Int): String =
        maiuscola(LocalDate.of(anno, mese, 1).format(soloMese))

    fun titolo(data: LocalDate): String = when (data) {
        LocalDate.now() -> "Oggi"
        LocalDate.now().minusDays(1) -> "Ieri"
        else -> giornoConSettimana(data)
    }

    fun sottotitolo(data: LocalDate): String = when (data) {
        LocalDate.now(), LocalDate.now().minusDays(1) -> dataLunga(data).lowercase(italiano)
        else -> maiuscola(data.format(soloMese)).lowercase(italiano) + " " + data.year
    }

    fun giornoSettimanaCorto(giorno: DayOfWeek): String =
        giorno.getDisplayName(TextStyle.SHORT, italiano).lowercase(italiano).take(3)

    fun inizialeGiornoSettimana(giorno: DayOfWeek): String =
        giorno.getDisplayName(TextStyle.NARROW, italiano).uppercase(italiano).take(1)

    private fun maiuscola(testo: String): String =
        testo.replaceFirstChar { it.uppercase(italiano) }
}
