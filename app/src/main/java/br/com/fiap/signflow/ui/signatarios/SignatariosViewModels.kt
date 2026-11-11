package br.com.fiap.signflow.ui.signatarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.signflow.core.Formatters
import br.com.fiap.signflow.core.SessionManager
import br.com.fiap.signflow.domain.model.Usuario
import br.com.fiap.signflow.domain.repository.SignatarioRepository
import br.com.fiap.signflow.domain.rules.DocumentoRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignatariosViewModel(repo: SignatarioRepository, session: SessionManager) : ViewModel() {
    val signatarios: StateFlow<List<Usuario>?> = repo.observar(session.usuarioAtual.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class SignatarioFormState(
    val editando: Boolean = false,
    val nome: String = "",
    val email: String = "",
    val telefone: String = "",
    val senha: String = "",
    val erroNome: String? = null,
    val erroEmail: String? = null,
    val erroTelefone: String? = null,
    val erroSenha: String? = null,
    val erroGeral: String? = null,
    val salvando: Boolean = false,
    val salvo: Boolean = false
)

class SignatarioFormViewModel(
    private val repo: SignatarioRepository,
    private val session: SessionManager,
    private val id: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(SignatarioFormState(editando = id != null))
    val state: StateFlow<SignatarioFormState> = _state.asStateFlow()

    init {
        if (id != null) viewModelScope.launch {
            repo.buscar(id)?.let { u ->
                _state.update { it.copy(nome = u.nome, email = u.email, telefone = Formatters.telefone(u.telefone)) }
            }
        }
    }

    fun nome(v: String) = _state.update { it.copy(nome = v, erroNome = null, erroGeral = null) }
    fun email(v: String) = _state.update { it.copy(email = v.trim(), erroEmail = null, erroGeral = null) }
    fun telefone(v: String) = _state.update { it.copy(telefone = Formatters.telefone(v), erroTelefone = null, erroGeral = null) }
    fun senha(v: String) = _state.update { it.copy(senha = v, erroSenha = null, erroGeral = null) }

    fun salvar() {
        val s = _state.value
        val validado = s.copy(
            erroNome = if (s.nome.trim().length < 3 || !s.nome.trim().contains(" ")) "Informe nome e sobrenome." else null,
            erroEmail = if (!DocumentoRules.emailValido(s.email)) "E-mail inválido." else null,
            erroTelefone = if (!DocumentoRules.telefoneValido(s.telefone)) "Telefone com DDD (10 ou 11 dígitos)." else null,
            erroSenha = when {
                !s.editando && !DocumentoRules.senhaValida(s.senha) -> "Mínimo de 6 caracteres."
                s.editando && s.senha.isNotEmpty() && !DocumentoRules.senhaValida(s.senha) -> "Mínimo de 6 caracteres."
                else -> null
            }
        )
        _state.value = validado
        if (listOf(validado.erroNome, validado.erroEmail, validado.erroTelefone, validado.erroSenha).any { it != null }) return

        viewModelScope.launch {
            _state.update { it.copy(salvando = true) }
            repo.salvar(session.usuarioAtual.id, id, s.nome, s.email, s.telefone, s.senha.ifEmpty { null })
                .onSuccess { _state.update { it.copy(salvando = false, salvo = true) } }
                .onFailure { e -> _state.update { it.copy(salvando = false, erroGeral = e.message) } }
        }
    }
}
