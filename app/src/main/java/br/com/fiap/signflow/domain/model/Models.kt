package br.com.fiap.signflow.domain.model

data class Usuario(
    val id: Long,
    val nome: String,
    val email: String,
    val telefone: String,
    val perfil: Perfil
)

/** Campo de assinatura com posição e tamanho RELATIVOS à página (0..1), independentes da tela. */
data class CampoAssinatura(
    val id: String,
    val usuarioId: Long,
    val pagina: Int,
    val x: Float,
    val y: Float,
    val largura: Float,
    val altura: Float
)

data class SignatarioNoDocumento(
    val usuarioId: Long,
    val nome: String,
    val email: String,
    val papel: Papel,
    val status: StatusSignatario,
    val assinadoEm: Long?,
    val caminhoAssinatura: String?
)

data class Evento(
    val tipo: TipoEvento,
    val descricao: String,
    val dataHora: Long
)

data class DocumentoResumo(
    val id: Long,
    val titulo: String,
    val status: StatusDocumento,
    val versao: Int,
    val criadoEm: Long,
    val totalSignatarios: Int,
    val totalAssinados: Int,
    /** Para a visão do signatário: situação dele neste documento. */
    val meuStatus: StatusSignatario? = null
)

data class DocumentoDetalhe(
    val id: Long,
    val protocolo: String,
    val titulo: String,
    val nomeArquivo: String,
    val caminhoPdf: String,
    val caminhoPdfFinal: String?,
    val hashOriginal: String,
    val totalPaginas: Int,
    val mensagem: String,
    val status: StatusDocumento,
    val versao: Int,
    val documentoOrigemId: Long?,
    val adminId: Long,
    val adminNome: String,
    val criadoEm: Long,
    val enviadoEm: Long?,
    val concluidoEm: Long?,
    val signatarios: List<SignatarioNoDocumento>,
    val campos: List<CampoAssinatura>,
    val eventos: List<Evento>
)

/** Dados de um documento ainda em preparação (antes do envio). */
data class RascunhoDocumento(
    /** Gerado uma única vez por rascunho: garante que um reenvio não duplique as solicitações. */
    val protocolo: String,
    val titulo: String,
    val nomeArquivo: String,
    val caminhoPdf: String,
    val totalPaginas: Int,
    val mensagem: String,
    val signatarios: List<SignatarioSelecionado>,
    val campos: List<CampoAssinatura>,
    val versao: Int = 1,
    val documentoOrigemId: Long? = null
)

data class SignatarioSelecionado(
    val usuario: Usuario,
    val papel: Papel
)
