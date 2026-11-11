package br.com.fiap.signflow.ui.novo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.fiap.signflow.core.SessionManager
import br.com.fiap.signflow.data.files.ImportacaoPdf
import br.com.fiap.signflow.data.files.PdfStorage
import br.com.fiap.signflow.domain.model.CampoAssinatura
import br.com.fiap.signflow.domain.model.Papel
import br.com.fiap.signflow.domain.model.RascunhoDocumento
import br.com.fiap.signflow.domain.model.SignatarioSelecionado
import br.com.fiap.signflow.domain.model.Usuario
import br.com.fiap.signflow.domain.repository.DocumentoRepository
import br.com.fiap.signflow.domain.repository.SignatarioRepository
import br.com.fiap.signflow.domain.rules.DocumentoRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class Etapa(val titulo: String) {
    DADOS("Documento"),
    SIGNATARIOS("Signatários"),
    CAMPOS("Campos de assinatura"),
    REVISAO("Revisão e envio"),
    RESULTADO("Resultado")
}

sealed interface ResultadoEnvio {
    data class Sucesso(val documentoId: Long, val aposNovaTentativa: Boolean) : ResultadoEnvio
    data class Falha(val mensagem: String) : ResultadoEnvio
}

const val MENSAGEM_PADRAO =
    "Olá! Você recebeu um documento para assinatura eletrônica. Revise o conteúdo com atenção e assine pelo app SignFlow."

data class NovoDocumentoState(
    val etapa: Etapa = Etapa.DADOS,
    /** Chave de idempotência gerada uma vez por rascunho. */
    val protocolo: String = UUID.randomUUID().toString(),
    val titulo: String = "",
    val caminhoPdf: String? = null,
    val nomeArquivo: String? = null,
    val totalPaginas: Int = 0,
    val importando: Boolean = false,
    val erroArquivo: String? = null,
    val disponiveis: List<Usuario> = emptyList(),
    val selecionados: List<SignatarioSelecionado> = emptyList(),
    val campos: List<CampoAssinatura> = emptyList(),
    val paginaAtual: Int = 0,
    val signatarioAtivoId: Long? = null,
    val campoSelecionadoId: String? = null,
    val mensagem: String = MENSAGEM_PADRAO,
    val versao: Int = 1,
    val documentoOrigemId: Long? = null,
    val erros: List<String> = emptyList(),
    val enviando: Boolean = false,
    val tentativas: Int = 0,
    val simularFalha: Boolean = false,
    val resultado: ResultadoEnvio? = null,
    val carregandoVersao: Boolean = false
) {
    val indiceDoSignatario: Map<Long, Int> get() = selecionados.mapIndexed { i, s -> s.usuario.id to i }.toMap()
    val naoSelecionados: List<Usuario> get() = disponiveis.filter { d -> selecionados.none { it.usuario.id == d.id } }
    fun camposDe(usuarioId: Long) = campos.filter { it.usuarioId == usuarioId }
    val temAlteracoes: Boolean get() = titulo.isNotBlank() || caminhoPdf != null || selecionados.isNotEmpty()
}

class NovoDocumentoViewModel(
    private val documentos: DocumentoRepository,
    signatarios: SignatarioRepository,
    private val storage: PdfStorage,
    private val session: SessionManager,
    origemId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(NovoDocumentoState())
    val state: StateFlow<NovoDocumentoState> = _state.asStateFlow()

    private val adminId = session.usuarioAtual.id

    init {
        viewModelScope.launch {
            signatarios.observar(adminId).collect { lista -> _state.update { it.copy(disponiveis = lista) } }
        }
        if (origemId != null) carregarVersao(origemId)
    }

    /** Alterações em documento enviado geram nova versão e novo fluxo (o original fica preservado). */
    private fun carregarVersao(origemId: Long) = viewModelScope.launch {
        _state.update { it.copy(carregandoVersao = true) }
        val r = documentos.prepararNovaVersao(origemId, adminId)
        _state.update {
            if (r == null) it.copy(carregandoVersao = false, erroArquivo = "Não foi possível carregar o documento de origem.")
            else it.copy(
                carregandoVersao = false,
                protocolo = r.protocolo,
                titulo = r.titulo,
                caminhoPdf = r.caminhoPdf,
                nomeArquivo = r.nomeArquivo,
                totalPaginas = r.totalPaginas,
                selecionados = r.signatarios,
                campos = r.campos,
                mensagem = r.mensagem.ifBlank { MENSAGEM_PADRAO },
                versao = r.versao,
                documentoOrigemId = r.documentoOrigemId,
                signatarioAtivoId = r.signatarios.firstOrNull()?.usuario?.id
            )
        }
    }

    // ---------------- Etapa 1: dados e arquivo ----------------

    fun titulo(v: String) = _state.update { it.copy(titulo = v.take(120), erros = emptyList()) }

    fun importarPdf(uri: Uri) = viewModelScope.launch {
        _state.update { it.copy(importando = true, erroArquivo = null) }
        when (val r = storage.importar(uri)) {
            is ImportacaoPdf.Sucesso -> {
                storage.descartar(_state.value.caminhoPdf)
                _state.update {
                    it.copy(
                        importando = false,
                        caminhoPdf = r.caminho,
                        nomeArquivo = r.nomeArquivo,
                        totalPaginas = r.totalPaginas,
                        campos = emptyList(), // campos dependem das páginas do arquivo anterior
                        paginaAtual = 0,
                        titulo = it.titulo.ifBlank { r.nomeArquivo.removeSuffix(".pdf").removeSuffix(".PDF") }
                    )
                }
            }
            is ImportacaoPdf.Falha -> _state.update { it.copy(importando = false, erroArquivo = r.motivo) }
        }
    }

    // ---------------- Etapa 2: signatários ----------------

    fun adicionarSignatario(usuario: Usuario) = _state.update { s ->
        if (s.selecionados.any { it.usuario.id == usuario.id }) s
        else s.copy(
            selecionados = s.selecionados + SignatarioSelecionado(usuario, Papel.CONTRATANTE.takeIf { s.selecionados.isEmpty() } ?: Papel.CONTRATADO),
            signatarioAtivoId = s.signatarioAtivoId ?: usuario.id,
            erros = emptyList()
        )
    }

    fun definirPapel(usuarioId: Long, papel: Papel) = _state.update { s ->
        s.copy(selecionados = s.selecionados.map { if (it.usuario.id == usuarioId) it.copy(papel = papel) else it })
    }

    fun removerSignatario(usuarioId: Long) = _state.update { s ->
        val restantes = s.selecionados.filterNot { it.usuario.id == usuarioId }
        s.copy(
            selecionados = restantes,
            campos = s.campos.filterNot { it.usuarioId == usuarioId },
            signatarioAtivoId = if (s.signatarioAtivoId == usuarioId) restantes.firstOrNull()?.usuario?.id else s.signatarioAtivoId
        )
    }

    /** Troca um signatário por outro mantendo papel e campos já posicionados. */
    fun substituirSignatario(antigoId: Long, novo: Usuario) = _state.update { s ->
        if (s.selecionados.any { it.usuario.id == novo.id }) return@update s
        s.copy(
            selecionados = s.selecionados.map { if (it.usuario.id == antigoId) it.copy(usuario = novo) else it },
            campos = s.campos.map { if (it.usuarioId == antigoId) it.copy(usuarioId = novo.id) else it },
            signatarioAtivoId = if (s.signatarioAtivoId == antigoId) novo.id else s.signatarioAtivoId
        )
    }

    fun moverSignatario(usuarioId: Long, paraCima: Boolean) = _state.update { s ->
        val lista = s.selecionados.toMutableList()
        val i = lista.indexOfFirst { it.usuario.id == usuarioId }
        val j = if (paraCima) i - 1 else i + 1
        if (i < 0 || j !in lista.indices) return@update s
        lista[i] = lista[j].also { lista[j] = lista[i] }
        s.copy(selecionados = lista)
    }

    // ---------------- Etapa 3: campos de assinatura ----------------

    fun irParaPagina(p: Int) = _state.update { it.copy(paginaAtual = p.coerceIn(0, (it.totalPaginas - 1).coerceAtLeast(0)), campoSelecionadoId = null) }

    fun ativarSignatario(id: Long) = _state.update { it.copy(signatarioAtivoId = id, campoSelecionadoId = null) }

    /** [proporcaoPagina] = largura/altura da página, para o campo nascer com formato de "linha de assinatura". */
    fun adicionarCampo(proporcaoPagina: Float) = _state.update { s ->
        val dono = s.signatarioAtivoId ?: return@update s
        val largura = 0.34f
        val altura = (largura * proporcaoPagina / 3.2f).coerceIn(0.03f, 0.2f)
        val existentes = s.campos.count { it.pagina == s.paginaAtual }
        val campo = DocumentoRules.limitar(
            CampoAssinatura(
                id = UUID.randomUUID().toString(),
                usuarioId = dono,
                pagina = s.paginaAtual,
                x = 0.5f - largura / 2 + (existentes % 3) * 0.04f,
                y = 0.7f - (existentes % 6) * (altura + 0.02f),
                largura = largura,
                altura = altura
            )
        )
        s.copy(campos = s.campos + campo, campoSelecionadoId = campo.id, erros = emptyList())
    }

    fun selecionarCampo(id: String?) = _state.update { it.copy(campoSelecionadoId = id) }

    fun moverCampo(id: String, dx: Float, dy: Float) = _state.update { s ->
        s.copy(campos = s.campos.map { if (it.id == id) DocumentoRules.limitar(it.copy(x = it.x + dx, y = it.y + dy)) else it })
    }

    fun redimensionarCampo(id: String, fator: Float) = _state.update { s ->
        s.copy(campos = s.campos.map { c ->
            if (c.id != id) c
            else {
                val novaLargura = (c.largura * fator).coerceIn(0.12f, 0.8f)
                val escala = novaLargura / c.largura
                // redimensiona a partir do centro
                DocumentoRules.limitar(
                    c.copy(
                        x = c.x + (c.largura - novaLargura) / 2,
                        y = c.y + (c.altura - c.altura * escala) / 2,
                        largura = novaLargura,
                        altura = c.altura * escala
                    )
                )
            }
        })
    }

    fun excluirCampo(id: String) = _state.update { s ->
        s.copy(campos = s.campos.filterNot { it.id == id }, campoSelecionadoId = null)
    }

    // ---------------- Etapa 4: revisão e envio ----------------

    fun mensagem(v: String) = _state.update { it.copy(mensagem = v.take(500)) }

    fun simularFalha(ativo: Boolean) = _state.update { it.copy(simularFalha = ativo) }

    fun enviar() {
        val s = _state.value
        if (s.enviando) return
        val erros = validar(s)
        if (erros.isNotEmpty()) {
            _state.update { it.copy(erros = erros) }
            return
        }
        val rascunho = RascunhoDocumento(
            protocolo = s.protocolo,
            titulo = s.titulo,
            nomeArquivo = s.nomeArquivo.orEmpty(),
            caminhoPdf = s.caminhoPdf.orEmpty(),
            totalPaginas = s.totalPaginas,
            mensagem = s.mensagem,
            signatarios = s.selecionados,
            campos = s.campos,
            versao = s.versao,
            documentoOrigemId = s.documentoOrigemId
        )
        viewModelScope.launch {
            _state.update { it.copy(enviando = true, erros = emptyList(), resultado = null) }
            val resultado = documentos.enviar(adminId, rascunho)
            val tentativa = s.tentativas + 1

            // Modo demonstração: o registro acontece, mas a "resposta" se perde (como um timeout de rede).
            // A nova tentativa reutiliza o mesmo protocolo e NÃO duplica as solicitações.
            if (s.simularFalha && resultado.isSuccess) {
                _state.update {
                    it.copy(
                        enviando = false, tentativas = tentativa, simularFalha = false,
                        resultado = ResultadoEnvio.Falha(
                            "Tempo de resposta esgotado. Verifique sua conexão e toque em \"Tentar novamente\". " +
                                "Fique tranquilo: as solicitações não serão duplicadas."
                        )
                    )
                }
                return@launch
            }

            resultado
                .onSuccess { id ->
                    _state.update {
                        it.copy(
                            enviando = false, tentativas = tentativa, etapa = Etapa.RESULTADO,
                            resultado = ResultadoEnvio.Sucesso(id, aposNovaTentativa = tentativa > 1)
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            enviando = false, tentativas = tentativa,
                            resultado = ResultadoEnvio.Falha(
                                (e.message ?: "Não foi possível registrar o envio.") + " Revise os dados e tente novamente."
                            )
                        )
                    }
                }
        }
    }

    // ---------------- Navegação entre etapas ----------------

    fun avancar() {
        val s = _state.value
        val erros = when (s.etapa) {
            Etapa.DADOS -> buildList {
                if (s.titulo.trim().length < DocumentoRules.TITULO_MIN) add("Informe um título com pelo menos ${DocumentoRules.TITULO_MIN} caracteres.")
                if (s.caminhoPdf == null) add("Selecione um arquivo PDF.")
            }
            Etapa.SIGNATARIOS -> if (s.selecionados.isEmpty()) listOf("Adicione pelo menos um signatário.") else emptyList()
            Etapa.CAMPOS -> s.selecionados.filter { sel -> s.campos.none { it.usuarioId == sel.usuario.id } }
                .map { "Posicione pelo menos um campo para ${it.usuario.nome}." }
            else -> emptyList()
        }
        if (erros.isNotEmpty()) {
            _state.update { it.copy(erros = erros) }
            return
        }
        val proxima = Etapa.entries.getOrNull(s.etapa.ordinal + 1) ?: return
        _state.update { it.copy(etapa = proxima, erros = emptyList(), campoSelecionadoId = null) }
    }

    /** Retorna false quando já está na primeira etapa (a tela decide se sai). */
    fun voltar(): Boolean {
        val s = _state.value
        if (s.etapa == Etapa.DADOS || s.etapa == Etapa.RESULTADO) return false
        _state.update { it.copy(etapa = Etapa.entries[s.etapa.ordinal - 1], erros = emptyList(), resultado = null) }
        return true
    }

    fun irPara(etapa: Etapa) {
        if (etapa.ordinal < _state.value.etapa.ordinal && _state.value.etapa != Etapa.RESULTADO) {
            _state.update { it.copy(etapa = etapa, erros = emptyList(), resultado = null) }
        }
    }

    private fun validar(s: NovoDocumentoState) = DocumentoRules.validarEnvio(
        titulo = s.titulo,
        temPdf = s.caminhoPdf != null,
        totalPaginas = s.totalPaginas,
        signatarioIds = s.selecionados.map { it.usuario.id },
        campos = s.campos
    )

    /** Rascunho abandonado: apaga a cópia do PDF que não chegou a ser enviada. */
    override fun onCleared() {
        val s = _state.value
        if (s.resultado !is ResultadoEnvio.Sucesso && s.tentativas == 0) storage.descartar(s.caminhoPdf)
    }
}
