package br.com.fiap.signflow.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.signflow.core.SessionManager
import br.com.fiap.signflow.domain.model.DocumentoResumo
import br.com.fiap.signflow.domain.model.StatusDocumento
import br.com.fiap.signflow.domain.repository.DocumentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AdminHomeUiState(
    val nome: String = "",
    val documentos: List<DocumentoResumo> = emptyList(),
    val filtro: StatusDocumento? = null,
    val contagem: Map<StatusDocumento?, Int> = emptyMap(),
    val carregando: Boolean = true
)

class AdminHomeViewModel(
    repo: DocumentoRepository,
    private val session: SessionManager
) : ViewModel() {

    private val usuario = session.usuarioAtual
    private val filtro = MutableStateFlow<StatusDocumento?>(null)

    val state: StateFlow<AdminHomeUiState> = combine(repo.observarDoAdmin(usuario.id), filtro) { docs, f ->
        AdminHomeUiState(
            nome = usuario.nome,
            documentos = if (f == null) docs else docs.filter { it.status == f },
            filtro = f,
            contagem = buildMap<StatusDocumento?, Int> {
                put(null, docs.size)
                StatusDocumento.entries.forEach { s -> put(s, docs.count { it.status == s }) }
            },
            carregando = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminHomeUiState(nome = usuario.nome))

    fun filtrar(status: StatusDocumento?) {
        filtro.value = status
    }

    fun sair() = session.encerrar()
}
