package br.com.fiap.signflow.data.repository

import br.com.fiap.signflow.core.PasswordHasher
import br.com.fiap.signflow.data.local.UsuarioDao
import br.com.fiap.signflow.data.local.UsuarioEntity
import br.com.fiap.signflow.data.local.toDomain
import br.com.fiap.signflow.domain.model.Perfil
import br.com.fiap.signflow.domain.model.Usuario
import br.com.fiap.signflow.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RoomAuthRepository(private val dao: UsuarioDao) : AuthRepository {

    private val semente = Mutex()

    override suspend fun autenticar(email: String, senha: String): Result<Usuario> = withContext(Dispatchers.Default) {
        val usuario = dao.buscarPorEmail(email.trim())
        // mensagem genérica: não revela se o e-mail existe
        if (usuario == null || !PasswordHasher.confere(senha, usuario.senhaHash, usuario.senhaSalt)) {
            Result.failure(IllegalArgumentException("E-mail ou senha incorretos."))
        } else {
            Result.success(usuario.toDomain())
        }
    }

    override suspend fun buscarUsuario(id: Long): Usuario? = dao.buscarPorId(id)?.toDomain()

    /** Cria o admin padrão e signatários de exemplo na primeira execução. */
    override suspend fun garantirDadosIniciais(): Unit = semente.withLock {
        withContext(Dispatchers.Default) { semear() }
    }

    private suspend fun semear() {
        if (dao.contar() > 0) return
        val adminId = dao.inserir(usuario("Administrador LetsSign", "admin@letssign.com", "11999990000", "admin123", Perfil.ADMIN, null))
        dao.inserir(usuario("Ana Souza", "ana@email.com", "11987654321", "123456", Perfil.SIGNATARIO, adminId))
        dao.inserir(usuario("Bruno Lima", "bruno@email.com", "11976543210", "123456", Perfil.SIGNATARIO, adminId))
        dao.inserir(usuario("Carla Mendes", "carla@email.com", "11965432109", "123456", Perfil.SIGNATARIO, adminId))
    }

    private fun usuario(nome: String, email: String, tel: String, senha: String, perfil: Perfil, criadoPor: Long?): UsuarioEntity {
        val hash = PasswordHasher.gerar(senha)
        return UsuarioEntity(
            nome = nome, email = email, telefone = tel,
            senhaHash = hash.hash, senhaSalt = hash.salt,
            perfil = perfil.name, criadoPor = criadoPor
        )
    }
}
