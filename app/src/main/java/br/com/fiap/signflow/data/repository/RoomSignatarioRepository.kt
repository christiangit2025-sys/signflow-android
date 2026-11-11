package br.com.fiap.signflow.data.repository

import br.com.fiap.signflow.core.PasswordHasher
import br.com.fiap.signflow.data.local.UsuarioDao
import br.com.fiap.signflow.data.local.UsuarioEntity
import br.com.fiap.signflow.data.local.toDomain
import br.com.fiap.signflow.domain.model.Perfil
import br.com.fiap.signflow.domain.model.Usuario
import br.com.fiap.signflow.domain.repository.SignatarioRepository
import br.com.fiap.signflow.domain.rules.DocumentoRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomSignatarioRepository(private val dao: UsuarioDao) : SignatarioRepository {

    override fun observar(adminId: Long): Flow<List<Usuario>> =
        dao.observarSignatarios(adminId).map { lista -> lista.map { it.toDomain() } }

    override suspend fun buscar(id: Long): Usuario? = dao.buscarPorId(id)?.toDomain()

    override suspend fun salvar(
        adminId: Long,
        id: Long?,
        nome: String,
        email: String,
        telefone: String,
        senha: String?
    ): Result<Long> = withContext(Dispatchers.Default) {
        val emailLimpo = email.trim().lowercase()
        val telefoneLimpo = telefone.filter { it.isDigit() }

        // validação também na camada de dados (defesa em profundidade)
        when {
            nome.trim().length < 3 -> return@withContext Result.failure(IllegalArgumentException("Informe o nome completo."))
            !DocumentoRules.emailValido(emailLimpo) -> return@withContext Result.failure(IllegalArgumentException("E-mail inválido."))
            !DocumentoRules.telefoneValido(telefoneLimpo) -> return@withContext Result.failure(IllegalArgumentException("Telefone inválido."))
        }

        val existente = dao.buscarPorEmail(emailLimpo)
        if (existente != null && existente.id != id) {
            return@withContext Result.failure(IllegalArgumentException("Já existe um usuário com este e-mail."))
        }

        if (id == null) {
            if (senha == null || !DocumentoRules.senhaValida(senha)) {
                return@withContext Result.failure(IllegalArgumentException("A senha precisa ter pelo menos 6 caracteres."))
            }
            val hash = PasswordHasher.gerar(senha)
            val novoId = dao.inserir(
                UsuarioEntity(
                    nome = nome.trim(), email = emailLimpo, telefone = telefoneLimpo,
                    senhaHash = hash.hash, senhaSalt = hash.salt,
                    perfil = Perfil.SIGNATARIO.name, criadoPor = adminId
                )
            )
            Result.success(novoId)
        } else {
            val atual = dao.buscarPorId(id)
            if (atual == null || atual.criadoPor != adminId) {
                return@withContext Result.failure(IllegalStateException("Signatário não encontrado."))
            }
            var atualizado = atual.copy(nome = nome.trim(), email = emailLimpo, telefone = telefoneLimpo)
            if (!senha.isNullOrEmpty()) {
                if (!DocumentoRules.senhaValida(senha)) {
                    return@withContext Result.failure(IllegalArgumentException("A senha precisa ter pelo menos 6 caracteres."))
                }
                val hash = PasswordHasher.gerar(senha)
                atualizado = atualizado.copy(senhaHash = hash.hash, senhaSalt = hash.salt)
            }
            dao.atualizar(atualizado)
            Result.success(id)
        }
    }
}
