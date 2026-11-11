package br.com.fiap.signflow.ui.assinar

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.signflow.core.SessionManager
import br.com.fiap.signflow.data.files.PdfStorage
import br.com.fiap.signflow.domain.model.DocumentoDetalhe
import br.com.fiap.signflow.domain.model.StatusDocumento
import br.com.fiap.signflow.domain.repository.DocumentoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AssinarUiState(
    val permitido: Boolean? = null,
    val assinatura: Bitmap? = null,
    val aceite: Boolean = false,
    val salvando: Boolean = false,
    val erro: String? = null,
    val concluido: StatusDocumento? = null
)

class AssinarViewModel(
    private val repo: DocumentoRepository,
    private val storage: PdfStorage,
    session: SessionManager,
    private val documentoId: Long
) : ViewModel() {

    val usuario = session.usuarioAtual

    private val _state = MutableStateFlow(AssinarUiState())
    val state: StateFlow<AssinarUiState> = _state.asStateFlow()

    val detalhe: StateFlow<DocumentoDetalhe?> = repo.observarDetalhe(documentoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            val ok = repo.podeAcessar(documentoId, usuario)
            _state.update { it.copy(permitido = ok) }
            if (ok) repo.registrarVisualizacao(documentoId, usuario.id)
        }
    }

    fun definirAssinatura(bitmap: Bitmap) = _state.update { it.copy(assinatura = bitmap, erro = null) }
    fun limparAssinatura() = _state.update { it.copy(assinatura = null, aceite = false) }
    fun aceite(v: Boolean) = _state.update { it.copy(aceite = v, erro = null) }

    fun salvar() {
        val s = _state.value
        val bitmap = s.assinatura ?: return _state.update { it.copy(erro = "Desenhe sua assinatura primeiro.") }
        if (!s.aceite) return _state.update { it.copy(erro = "Confirme que leu e concorda com o documento.") }
        if (s.salvando) return

        viewModelScope.launch {
            _state.update { it.copy(salvando = true, erro = null) }
            val caminho = withContext(Dispatchers.IO) { storage.salvarAssinatura(bitmap) }
            repo.assinar(documentoId, usuario.id, caminho)
                .onSuccess { status -> _state.update { it.copy(salvando = false, concluido = status) } }
                .onFailure { e ->
                    storage.descartar(caminho)
                    _state.update { it.copy(salvando = false, erro = e.message ?: "Não foi possível salvar a assinatura.") }
                }
        }
    }
}
