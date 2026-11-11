package br.com.fiap.signflow.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.signflow.core.SessionManager
import br.com.fiap.signflow.domain.model.Usuario
import br.com.fiap.signflow.domain.repository.AuthRepository
import br.com.fiap.signflow.domain.rules.DocumentoRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val senha: String = "",
    val carregando: Boolean = true,
    val erro: String? = null,
    val logado: Usuario? = null
)

class LoginViewModel(
    private val auth: AuthRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            auth.garantirDadosIniciais()
            // restaura a sessão anterior, se houver
            val salvo = session.idSalvo?.let { auth.buscarUsuario(it) }
            if (salvo != null) session.iniciar(salvo) else session.encerrar()
            _state.update { it.copy(carregando = false, logado = salvo) }
        }
    }

    fun aoMudarEmail(valor: String) = _state.update { it.copy(email = valor.trim(), erro = null) }
    fun aoMudarSenha(valor: String) = _state.update { it.copy(senha = valor, erro = null) }

    fun preencher(email: String, senha: String) = _state.update { it.copy(email = email, senha = senha, erro = null) }

    fun entrar() {
        val atual = _state.value
        when {
            !DocumentoRules.emailValido(atual.email) -> _state.update { it.copy(erro = "Informe um e-mail válido.") }
            atual.senha.isEmpty() -> _state.update { it.copy(erro = "Informe a senha.") }
            else -> viewModelScope.launch {
                _state.update { it.copy(carregando = true, erro = null) }
                auth.autenticar(atual.email, atual.senha)
                    .onSuccess { usuario ->
                        session.iniciar(usuario)
                        _state.update { it.copy(carregando = false, logado = usuario, senha = "") }
                    }
                    .onFailure { e -> _state.update { it.copy(carregando = false, erro = e.message) } }
            }
        }
    }

    fun navegacaoConcluida() = _state.update { it.copy(logado = null) }
}
