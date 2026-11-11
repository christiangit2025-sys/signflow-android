package br.com.fiap.signflow.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val local: Locale = Locale.forLanguageTag("pt-BR")

    fun dataHora(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", local).format(Date(millis))

    fun dataHoraSegundos(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", local).format(Date(millis))

    fun data(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", local).format(Date(millis))

    /** Formata enquanto digita: (11) 98765-4321 */
    fun telefone(digitos: String): String {
        val d = digitos.filter { it.isDigit() }.take(11)
        return when {
            d.length <= 2 -> d
            d.length <= 6 -> "(${d.take(2)}) ${d.drop(2)}"
            d.length <= 10 -> "(${d.take(2)}) ${d.substring(2, 6)}-${d.drop(6)}"
            else -> "(${d.take(2)}) ${d.substring(2, 7)}-${d.drop(7)}"
        }
    }

    fun protocoloCurto(protocolo: String) = protocolo.take(8).uppercase(local)
}
