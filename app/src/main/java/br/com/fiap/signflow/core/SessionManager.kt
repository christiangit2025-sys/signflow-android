package br.com.fiap.signflow.core

import android.content.Context
import br.com.fiap.signflow.domain.model.Perfil
import br.com.fiap.signflow.domain.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda o usuário logado. Apenas dados de identificação são persistidos — nunca a senha.
 * A sessão é restaurada de forma síncrona para sobreviver à recriação do processo.
 */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("sessao", Context.MODE_PRIVATE)

    private val _usuario = MutableStateFlow(restaurar())
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    val usuarioAtual: Usuario get() = requireNotNull(_usuario.value) { "Nenhum usuário logado" }

    val idSalvo: Long? get() = _usuario.value?.id

    fun iniciar(usuario: Usuario) {
        _usuario.value = usuario
        prefs.edit()
            .putLong("id", usuario.id)
            .putString("nome", usuario.nome)
            .putString("email", usuario.email)
            .putString("telefone", usuario.telefone)
            .putString("perfil", usuario.perfil.name)
            .apply()
    }

    fun encerrar() {
        _usuario.value = null
        prefs.edit().clear().apply()
    }

    private fun restaurar(): Usuario? {
        val id = prefs.getLong("id", -1L)
        if (id <= 0) return null
        return runCatching {
            Usuario(
                id = id,
                nome = prefs.getString("nome", null)!!,
                email = prefs.getString("email", null)!!,
                telefone = prefs.getString("telefone", "")!!,
                perfil = Perfil.valueOf(prefs.getString("perfil", null)!!)
            )
        }.getOrNull()
    }
}
