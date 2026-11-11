package br.com.fiap.signflow.data.repository

import android.util.Log
import androidx.room.withTransaction
import br.com.fiap.signflow.data.files.PdfStamper
import br.com.fiap.signflow.data.files.PdfStorage
import br.com.fiap.signflow.data.local.AppDatabase
import br.com.fiap.signflow.data.local.DocumentoEntity
import br.com.fiap.signflow.data.local.DocumentoSignatarioEntity
import br.com.fiap.signflow.data.local.EventoEntity
import br.com.fiap.signflow.data.local.toDomain
import br.com.fiap.signflow.data.local.toEntity
import br.com.fiap.signflow.domain.model.DocumentoDetalhe
import br.com.fiap.signflow.domain.model.DocumentoResumo
import br.com.fiap.signflow.domain.model.Papel
import br.com.fiap.signflow.domain.model.Perfil
import br.com.fiap.signflow.domain.model.RascunhoDocumento
import br.com.fiap.signflow.domain.model.SignatarioSelecionado
import br.com.fiap.signflow.domain.model.StatusDocumento
import br.com.fiap.signflow.domain.model.StatusSignatario
import br.com.fiap.signflow.domain.model.TipoEvento
import br.com.fiap.signflow.domain.model.Usuario
import br.com.fiap.signflow.domain.repository.DocumentoRepository
import br.com.fiap.signflow.domain.rules.DocumentoRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class RoomDocumentoRepository(
    private val db: AppDatabase,
    private val storage: PdfStorage,
    private val stamper: PdfStamper
) : DocumentoRepository {

    private val dao = db.documentoDao()
    private val usuarioDao = db.usuarioDao()

    override fun observarDoAdmin(adminId: Long): Flow<List<DocumentoResumo>> =
        dao.observarDoAdmin(adminId).map { lista -> lista.map { it.toDomain() } }

    override fun observarDoSignatario(usuarioId: Long): Flow<List<DocumentoResumo>> =
        dao.observarDoSignatario(usuarioId).map { lista -> lista.map { it.toDomain() } }

    override fun observarDetalhe(documentoId: Long): Flow<DocumentoDetalhe?> = combine(
        dao.observarDocumento(documentoId),
        dao.observarSignatarios(documentoId),
        dao.observarCampos(documentoId),
        dao.observarEventos(documentoId)
    ) { doc, signatarios, campos, eventos ->
        if (doc == null) return@combine null
        DocumentoDetalhe(
            id = doc.id,
            protocolo = doc.protocolo,
            titulo = doc.titulo,
            nomeArquivo = doc.nomeArquivo,
            caminhoPdf = doc.caminhoPdf,
            caminhoPdfFinal = doc.caminhoPdfFinal,
            hashOriginal = doc.hashOriginal,
            totalPaginas = doc.totalPaginas,
            mensagem = doc.mensagem,
            status = StatusDocumento.valueOf(doc.status),
            versao = doc.versao,
            documentoOrigemId = doc.documentoOrigemId,
            adminId = doc.adminId,
            adminNome = usuarioDao.buscarPorId(doc.adminId)?.nome ?: "Administrador",
            criadoEm = doc.criadoEm,
            enviadoEm = doc.enviadoEm,
            concluidoEm = doc.concluidoEm,
            signatarios = signatarios.map { it.toDomain() },
            campos = campos.map { it.toDomain() },
            eventos = eventos.map { it.toDomain() }
        )
    }

    override suspend fun podeAcessar(documentoId: Long, usuario: Usuario): Boolean {
        val doc = dao.buscarDocumento(documentoId) ?: return false
        return when (usuario.perfil) {
            Perfil.ADMIN -> doc.adminId == usuario.id
            Perfil.SIGNATARIO -> doc.enviadoEm != null && dao.contarVinculo(documentoId, usuario.id) > 0
        }
    }

    override suspend fun enviar(adminId: Long, rascunho: RascunhoDocumento): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            // Idempotência: se este protocolo já foi registrado, devolve o mesmo documento.
            dao.buscarPorProtocolo(rascunho.protocolo)?.let { return@runCatching it.id }

            val erros = DocumentoRules.validarEnvio(
                titulo = rascunho.titulo,
                temPdf = rascunho.caminhoPdf.isNotBlank(),
                totalPaginas = rascunho.totalPaginas,
                signatarioIds = rascunho.signatarios.map { it.usuario.id },
                campos = rascunho.campos
            )
            require(erros.isEmpty()) { erros.first() }

            val hash = storage.sha256(rascunho.caminhoPdf)
            val agora = System.currentTimeMillis()
            val admin = usuarioDao.buscarPorId(adminId)?.nome ?: "Administrador"

            db.withTransaction {
                val id = dao.inserirDocumento(
                    DocumentoEntity(
                        protocolo = rascunho.protocolo,
                        titulo = rascunho.titulo.trim(),
                        nomeArquivo = rascunho.nomeArquivo,
                        caminhoPdf = rascunho.caminhoPdf,
                        hashOriginal = hash,
                        totalPaginas = rascunho.totalPaginas,
                        mensagem = rascunho.mensagem.trim(),
                        status = StatusDocumento.ENVIADO.name,
                        versao = rascunho.versao,
                        documentoOrigemId = rascunho.documentoOrigemId,
                        adminId = adminId,
                        criadoEm = agora,
                        enviadoEm = agora
                    )
                )
                dao.inserirSignatarios(
                    rascunho.signatarios.mapIndexed { i, s ->
                        DocumentoSignatarioEntity(
                            documentoId = id,
                            usuarioId = s.usuario.id,
                            papel = s.papel.name,
                            ordem = i,
                            status = StatusSignatario.PENDENTE.name
                        )
                    }
                )
                dao.inserirCampos(rascunho.campos.map { it.toEntity(id, UUID.randomUUID().toString()) })

                val eventos = mutableListOf<EventoEntity>()
                if (rascunho.documentoOrigemId != null) {
                    eventos += EventoEntity(
                        documentoId = id, tipo = TipoEvento.NOVA_VERSAO.name, usuarioId = adminId, dataHora = agora,
                        descricao = "Versão ${rascunho.versao} criada a partir de um documento anterior"
                    )
                }
                eventos += EventoEntity(
                    documentoId = id, tipo = TipoEvento.CRIADO.name, usuarioId = adminId, dataHora = agora,
                    descricao = "Documento criado por $admin"
                )
                eventos += EventoEntity(
                    documentoId = id, tipo = TipoEvento.ENVIADO.name, usuarioId = adminId, dataHora = agora,
                    descricao = "Enviado para ${rascunho.signatarios.size} signatário(s)"
                )
                rascunho.signatarios.forEach { s ->
                    eventos += EventoEntity(
                        documentoId = id, tipo = TipoEvento.CONVITE.name, usuarioId = s.usuario.id, dataHora = agora,
                        descricao = "Convite enviado para ${s.usuario.nome} (${s.papel.rotulo})"
                    )
                }
                dao.inserirEventos(eventos)
                id
            }
        }
    }

    override suspend fun registrarVisualizacao(documentoId: Long, usuarioId: Long): Unit = withContext(Dispatchers.IO) {
        val usuario = usuarioDao.buscarPorId(usuarioId) ?: return@withContext
        if (dao.contarEventos(documentoId, usuarioId, TipoEvento.VISUALIZADO.name) == 0) {
            dao.inserirEvento(
                EventoEntity(
                    documentoId = documentoId, tipo = TipoEvento.VISUALIZADO.name, usuarioId = usuarioId,
                    dataHora = System.currentTimeMillis(), descricao = "${usuario.nome} abriu o documento"
                )
            )
        }
        Unit
    }

    override suspend fun assinar(
        documentoId: Long,
        usuarioId: Long,
        caminhoAssinatura: String
    ): Result<StatusDocumento> = withContext(Dispatchers.IO) {
        runCatching {
            val usuario = requireNotNull(usuarioDao.buscarPorId(usuarioId)) { "Usuário não encontrado." }
            val agora = System.currentTimeMillis()

            val novoStatus = db.withTransaction {
                val alterados = dao.marcarAssinado(documentoId, usuarioId, caminhoAssinatura, agora)
                check(alterados == 1) { "Este documento já foi assinado por você ou não está disponível." }

                dao.inserirEvento(
                    EventoEntity(
                        documentoId = documentoId, tipo = TipoEvento.ASSINADO.name, usuarioId = usuarioId,
                        dataHora = agora, descricao = "${usuario.nome} assinou o documento"
                    )
                )

                val status = DocumentoRules.calcularStatus(
                    dao.listarVinculos(documentoId).map { StatusSignatario.valueOf(it.status) }
                )
                val doc = requireNotNull(dao.buscarDocumento(documentoId))
                dao.atualizarDocumento(
                    doc.copy(
                        status = status.name,
                        concluidoEm = if (status == StatusDocumento.CONCLUIDO) agora else null
                    )
                )
                if (status == StatusDocumento.CONCLUIDO) {
                    dao.inserirEvento(
                        EventoEntity(
                            documentoId = documentoId, tipo = TipoEvento.CONCLUIDO.name, dataHora = agora + 1,
                            descricao = "Todas as assinaturas foram coletadas"
                        )
                    )
                }
                status
            }

            if (novoStatus == StatusDocumento.CONCLUIDO) gerarDocumentoFinal(documentoId)
            novoStatus
        }
    }

    /** Gera o PDF final carimbado. Uma falha aqui não desfaz as assinaturas já registradas. */
    private suspend fun gerarDocumentoFinal(documentoId: Long) {
        try {
            val doc = dao.buscarDocumento(documentoId) ?: return
            val vinculos = dao.listarVinculos(documentoId)
            val assinaturas = vinculos.mapNotNull { v ->
                val u = usuarioDao.buscarPorId(v.usuarioId) ?: return@mapNotNull null
                PdfStamper.AssinaturaParaCarimbo(
                    usuarioId = u.id, nome = u.nome, email = u.email,
                    papel = Papel.valueOf(v.papel).rotulo,
                    assinadoEm = v.assinadoEm ?: return@mapNotNull null,
                    caminhoImagem = v.caminhoAssinatura ?: return@mapNotNull null
                )
            }
            val campos = dao.listarCampos(documentoId).map {
                PdfStamper.CampoParaCarimbo(it.usuarioId, it.pagina, it.x, it.y, it.largura, it.altura)
            }
            val eventos = dao.listarEventos(documentoId).map { it.dataHora to it.descricao }
            val admin = usuarioDao.buscarPorId(doc.adminId)

            val caminhoFinal = stamper.gerarFinal(
                caminhoOriginal = doc.caminhoPdf,
                assinaturas = assinaturas,
                campos = campos,
                dados = PdfStamper.DadosAutenticidade(
                    titulo = doc.titulo,
                    protocolo = doc.protocolo,
                    versao = doc.versao,
                    hashOriginal = doc.hashOriginal,
                    emitidoPor = admin?.let { "${it.nome} <${it.email}>" } ?: "Administrador",
                    eventos = eventos
                )
            )
            dao.atualizarDocumento(doc.copy(caminhoPdfFinal = caminhoFinal))
        } catch (e: Exception) {
            Log.e("SignFlow", "Falha ao gerar o PDF final", e)
        }
    }

    override suspend fun prepararNovaVersao(documentoId: Long, adminId: Long): RascunhoDocumento? = withContext(Dispatchers.IO) {
        val doc = dao.buscarDocumento(documentoId) ?: return@withContext null
        if (doc.adminId != adminId) return@withContext null

        val vinculos = dao.listarVinculos(documentoId).sortedBy { it.ordem }
        val signatarios = vinculos.mapNotNull { v ->
            usuarioDao.buscarPorId(v.usuarioId)?.let { SignatarioSelecionado(it.toDomain(), Papel.valueOf(v.papel)) }
        }
        RascunhoDocumento(
            protocolo = UUID.randomUUID().toString(),
            titulo = doc.titulo,
            nomeArquivo = doc.nomeArquivo,
            caminhoPdf = storage.duplicar(doc.caminhoPdf),
            totalPaginas = doc.totalPaginas,
            mensagem = doc.mensagem,
            signatarios = signatarios,
            campos = dao.listarCampos(documentoId).map { it.toDomain().copy(id = UUID.randomUUID().toString()) },
            versao = doc.versao + 1,
            documentoOrigemId = doc.id
        )
    }
}
