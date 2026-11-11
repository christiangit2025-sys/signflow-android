package br.com.fiap.signflow.data.local

import br.com.fiap.signflow.domain.model.CampoAssinatura
import br.com.fiap.signflow.domain.model.DocumentoResumo
import br.com.fiap.signflow.domain.model.Evento
import br.com.fiap.signflow.domain.model.Papel
import br.com.fiap.signflow.domain.model.Perfil
import br.com.fiap.signflow.domain.model.SignatarioNoDocumento
import br.com.fiap.signflow.domain.model.StatusDocumento
import br.com.fiap.signflow.domain.model.StatusSignatario
import br.com.fiap.signflow.domain.model.TipoEvento
import br.com.fiap.signflow.domain.model.Usuario

fun UsuarioEntity.toDomain() = Usuario(id, nome, email, telefone, Perfil.valueOf(perfil))

fun DocumentoResumoRow.toDomain() = DocumentoResumo(
    id = id,
    titulo = titulo,
    status = StatusDocumento.valueOf(status),
    versao = versao,
    criadoEm = criadoEm,
    totalSignatarios = totalSignatarios,
    totalAssinados = totalAssinados,
    meuStatus = meuStatus?.let { StatusSignatario.valueOf(it) }
)

fun SignatarioDocumentoRow.toDomain() = SignatarioNoDocumento(
    usuarioId = usuarioId,
    nome = nome,
    email = email,
    papel = Papel.valueOf(papel),
    status = StatusSignatario.valueOf(status),
    assinadoEm = assinadoEm,
    caminhoAssinatura = caminhoAssinatura
)

fun CampoAssinaturaEntity.toDomain() = CampoAssinatura(id, usuarioId, pagina, x, y, largura, altura)

fun CampoAssinatura.toEntity(documentoId: Long, novoId: String) =
    CampoAssinaturaEntity(novoId, documentoId, usuarioId, pagina, x, y, largura, altura)

fun EventoEntity.toDomain() = Evento(TipoEvento.valueOf(tipo), descricao, dataHora)
