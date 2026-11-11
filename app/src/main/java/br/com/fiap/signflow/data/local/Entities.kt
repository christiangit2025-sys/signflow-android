package br.com.fiap.signflow.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios", indices = [Index(value = ["email"], unique = true)])
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val email: String,
    val telefone: String,
    val senhaHash: String,
    val senhaSalt: String,
    /** ADMIN ou SIGNATARIO */
    val perfil: String,
    /** Admin que cadastrou o signatário (isolamento de dados entre admins). */
    val criadoPor: Long? = null,
    val criadoEm: Long = System.currentTimeMillis()
)

/**
 * O PDF fica no armazenamento interno do app (filesDir/pdfs) e o banco guarda
 * nome, caminho, data, status e hash — opção 1 do enunciado.
 */
@Entity(
    tableName = "documentos",
    indices = [Index(value = ["protocolo"], unique = true), Index("adminId")]
)
data class DocumentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Chave de idempotência do envio: um reenvio com o mesmo protocolo não duplica nada. */
    val protocolo: String,
    val titulo: String,
    val nomeArquivo: String,
    val caminhoPdf: String,
    val caminhoPdfFinal: String? = null,
    val hashOriginal: String,
    val totalPaginas: Int,
    val mensagem: String,
    /** ENVIADO, PARCIAL ou CONCLUIDO */
    val status: String,
    val versao: Int = 1,
    val documentoOrigemId: Long? = null,
    val adminId: Long,
    val criadoEm: Long,
    val enviadoEm: Long? = null,
    val concluidoEm: Long? = null
)

@Entity(
    tableName = "documento_signatarios",
    foreignKeys = [
        ForeignKey(
            entity = DocumentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"]
        )
    ],
    indices = [Index(value = ["documentoId", "usuarioId"], unique = true), Index("usuarioId")]
)
data class DocumentoSignatarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentoId: Long,
    val usuarioId: Long,
    /** CONTRATANTE, CONTRATADO ou TESTEMUNHA */
    val papel: String,
    val ordem: Int,
    /** PENDENTE ou ASSINADO */
    val status: String,
    val assinadoEm: Long? = null,
    val caminhoAssinatura: String? = null
)

@Entity(
    tableName = "campos_assinatura",
    foreignKeys = [
        ForeignKey(
            entity = DocumentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentoId")]
)
data class CampoAssinaturaEntity(
    @PrimaryKey val id: String,
    val documentoId: Long,
    val usuarioId: Long,
    /** Página (base 0) e posição/tamanho relativos à página: valores entre 0 e 1. */
    val pagina: Int,
    val x: Float,
    val y: Float,
    val largura: Float,
    val altura: Float
)

@Entity(
    tableName = "eventos",
    foreignKeys = [
        ForeignKey(
            entity = DocumentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentoId")]
)
data class EventoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentoId: Long,
    val tipo: String,
    val descricao: String,
    val usuarioId: Long? = null,
    val dataHora: Long
)

/** Resultado de consulta: documento + contadores para as listas. */
data class DocumentoResumoRow(
    val id: Long,
    val titulo: String,
    val status: String,
    val versao: Int,
    val criadoEm: Long,
    val totalSignatarios: Int,
    val totalAssinados: Int,
    val meuStatus: String?
)

/** Resultado de consulta: signatário do documento com dados do usuário. */
data class SignatarioDocumentoRow(
    val usuarioId: Long,
    val nome: String,
    val email: String,
    val papel: String,
    val status: String,
    val assinadoEm: Long?,
    val caminhoAssinatura: String?,
    val ordem: Int
)
