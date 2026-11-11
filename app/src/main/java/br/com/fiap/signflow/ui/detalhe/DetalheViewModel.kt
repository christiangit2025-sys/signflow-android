package br.com.fiap.signflow.ui.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.signflow.core.SessionManager
import br.com.fiap.signflow.domain.model.DocumentoDetalhe
import br.com.fiap.signflow.domain.model.Perfil
import br.com.fiap.signflow.domain.repository.DocumentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Compartilhado pelas telas de acompanhamento e de visualização do PDF. */
class DetalheViewModel(
    repo: DocumentoRepository,
    session: SessionManager,
    documentoId: Long
) : ViewModel() {

    val usuario = session.usuarioAtual
    val ehAdmin = usuario.perfil == Perfil.ADMIN

    /** null = verificando; false = sem permissão para ver este documento. */
    private val _permitido = MutableStateFlow<Boolean?>(null)
    val permitido: StateFlow<Boolean?> = _permitido.asStateFlow()

    val detalhe: StateFlow<DocumentoDetalhe?> = repo.observarDetalhe(documentoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { _permitido.value = repo.podeAcessar(documentoId, usuario) }
    }
}
