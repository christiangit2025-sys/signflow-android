package br.com.fiap.signflow.ui.navigation

object Rotas {
    const val LOGIN = "login"
    const val ADMIN_INICIO = "admin"
    const val SIGNATARIOS = "signatarios"
    const val SIGNATARIO_FORM = "signatario_form?id={id}"
    const val NOVO_DOCUMENTO = "novo?origem={origem}"
    const val DETALHE = "detalhe/{id}"
    const val VISUALIZAR = "visualizar/{id}?final={final}"
    const val SIGNATARIO_INICIO = "caixa"
    const val ASSINAR = "assinar/{id}"

    fun signatarioForm(id: Long? = null) = "signatario_form?id=${id ?: -1L}"
    fun novoDocumento(origem: Long? = null) = "novo?origem=${origem ?: -1L}"
    fun detalhe(id: Long) = "detalhe/$id"
    fun visualizar(id: Long, final: Boolean = false) = "visualizar/$id?final=$final"
    fun assinar(id: Long) = "assinar/$id"
}
