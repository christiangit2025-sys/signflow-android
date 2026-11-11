package br.com.fiap.signflow.domain.repository

import br.com.fiap.signflow.domain.model.DocumentoDetalhe
import br.com.fiap.signflow.domain.model.DocumentoResumo
import br.com.fiap.signflow.domain.model.RascunhoDocumento
import br.com.fiap.signflow.domain.model.StatusDocumento
import br.com.fiap.signflow.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/*
 * Contratos da camada de dados. Na Sprint 1 a implementação é local (Room + arquivos);
 * na Sprint 2 basta criar implementações que chamam a API REST, sem mexer nas telas.
 */

interface AuthRepository {
    suspend fun autenticar(email: String, senha: String): Result<Usuario>
    suspend fun buscarUsuario(id: Long): Usuario?
    suspend fun garantirDadosIniciais()
}

interface SignatarioRepository {
    fun observar(adminId: Long): Flow<List<Usuario>>
    suspend fun buscar(id: Long): Usuario?

    /** Cria (id nulo) ou edita um signatário. Na edição, senha vazia mantém a atual. */
    suspend fun salvar(
        adminId: Long,
        id: Long?,
        nome: String,
        email: String,
        telefone: String,
        senha: String?
    ): Result<Long>
}

interface DocumentoRepository {
    fun observarDoAdmin(adminId: Long): Flow<List<DocumentoResumo>>
    fun observarDoSignatario(usuarioId: Long): Flow<List<DocumentoResumo>>
    fun observarDetalhe(documentoId: Long): Flow<DocumentoDetalhe?>

    /** Dados de um usuário só são acessíveis por ele (admin dono ou signatário convidado). */
    suspend fun podeAcessar(documentoId: Long, usuario: Usuario): Boolean

    /** Idempotente pelo protocolo do rascunho: tentar de novo nunca duplica as solicitações. */
    suspend fun enviar(adminId: Long, rascunho: RascunhoDocumento): Result<Long>

    suspend fun registrarVisualizacao(documentoId: Long, usuarioId: Long)

    suspend fun assinar(documentoId: Long, usuarioId: Long, caminhoAssinatura: String): Result<StatusDocumento>

    /** Monta um novo rascunho (nova versão e novo fluxo) a partir de um documento já enviado. */
    suspend fun prepararNovaVersao(documentoId: Long, adminId: Long): RascunhoDocumento?
}
