package br.com.fiap.signflow.domain.rules

import br.com.fiap.signflow.domain.model.CampoAssinatura
import br.com.fiap.signflow.domain.model.StatusDocumento
import br.com.fiap.signflow.domain.model.StatusSignatario

/**
 * Regras de negócio puras (sem Android), testadas em src/test.
 */
object DocumentoRules {

    const val TITULO_MIN = 3

    /** O documento só é concluído quando TODOS os signatários assinaram. */
    fun calcularStatus(statusSignatarios: List<StatusSignatario>): StatusDocumento {
        val assinados = statusSignatarios.count { it == StatusSignatario.ASSINADO }
        return when {
            statusSignatarios.isNotEmpty() && assinados == statusSignatarios.size -> StatusDocumento.CONCLUIDO
            assinados > 0 -> StatusDocumento.PARCIAL
            else -> StatusDocumento.ENVIADO
        }
    }

    /** Mantém o campo inteiro dentro dos limites da página. */
    fun limitar(campo: CampoAssinatura): CampoAssinatura {
        val largura = campo.largura.coerceIn(0.05f, 1f)
        val altura = campo.altura.coerceIn(0.02f, 1f)
        return campo.copy(
            largura = largura,
            altura = altura,
            x = campo.x.coerceIn(0f, 1f - largura),
            y = campo.y.coerceIn(0f, 1f - altura)
        )
    }

    fun dentroDaPagina(campo: CampoAssinatura): Boolean =
        campo.x >= 0f && campo.y >= 0f &&
            campo.x + campo.largura <= 1.0001f &&
            campo.y + campo.altura <= 1.0001f

    /**
     * Valida um rascunho antes do envio. Retorna a lista de problemas (vazia = válido).
     */
    fun validarEnvio(
        titulo: String,
        temPdf: Boolean,
        totalPaginas: Int,
        signatarioIds: List<Long>,
        campos: List<CampoAssinatura>
    ): List<String> {
        val erros = mutableListOf<String>()
        if (titulo.trim().length < TITULO_MIN) erros += "Informe um título com pelo menos $TITULO_MIN caracteres."
        if (!temPdf) erros += "Selecione um arquivo PDF válido."
        if (signatarioIds.isEmpty()) erros += "Adicione pelo menos um signatário."
        if (signatarioIds.size != signatarioIds.toSet().size) erros += "Há signatários repetidos."

        val semCampo = signatarioIds.filter { id -> campos.none { it.usuarioId == id } }
        if (semCampo.isNotEmpty()) erros += "Todo signatário precisa de pelo menos um campo de assinatura."

        if (campos.any { it.usuarioId !in signatarioIds }) erros += "Há campos sem participante associado."
        if (campos.any { !dentroDaPagina(it) }) erros += "Há campos fora dos limites da página."
        if (campos.any { it.pagina !in 0 until totalPaginas }) erros += "Há campos em páginas inexistentes."
        return erros
    }

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun emailValido(email: String) = EMAIL_REGEX.matches(email.trim())

    /** Aceita telefone com 10 ou 11 dígitos (fixo ou celular com DDD). */
    fun telefoneValido(telefone: String) = telefone.filter { it.isDigit() }.length in 10..11

    fun senhaValida(senha: String) = senha.length >= 6
}
