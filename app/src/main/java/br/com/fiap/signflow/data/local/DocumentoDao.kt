package br.com.fiap.signflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentoDao {

    // ---------- Listas ----------

    @Query(
        """
        SELECT d.id, d.titulo, d.status, d.versao, d.criadoEm,
               (SELECT COUNT(*) FROM documento_signatarios s WHERE s.documentoId = d.id) AS totalSignatarios,
               (SELECT COUNT(*) FROM documento_signatarios s WHERE s.documentoId = d.id AND s.status = 'ASSINADO') AS totalAssinados,
               NULL AS meuStatus
        FROM documentos d
        WHERE d.adminId = :adminId
        ORDER BY d.criadoEm DESC
        """
    )
    fun observarDoAdmin(adminId: Long): Flow<List<DocumentoResumoRow>>

    /** O signatário só enxerga documentos que foram enviados para ele. */
    @Query(
        """
        SELECT d.id, d.titulo, d.status, d.versao, d.criadoEm,
               (SELECT COUNT(*) FROM documento_signatarios s WHERE s.documentoId = d.id) AS totalSignatarios,
               (SELECT COUNT(*) FROM documento_signatarios s WHERE s.documentoId = d.id AND s.status = 'ASSINADO') AS totalAssinados,
               me.status AS meuStatus
        FROM documentos d
        INNER JOIN documento_signatarios me ON me.documentoId = d.id AND me.usuarioId = :usuarioId
        WHERE d.enviadoEm IS NOT NULL
        ORDER BY d.criadoEm DESC
        """
    )
    fun observarDoSignatario(usuarioId: Long): Flow<List<DocumentoResumoRow>>

    // ---------- Detalhe ----------

    @Query("SELECT * FROM documentos WHERE id = :id")
    fun observarDocumento(id: Long): Flow<DocumentoEntity?>

    @Query("SELECT * FROM documentos WHERE id = :id")
    suspend fun buscarDocumento(id: Long): DocumentoEntity?

    @Query("SELECT * FROM documentos WHERE protocolo = :protocolo LIMIT 1")
    suspend fun buscarPorProtocolo(protocolo: String): DocumentoEntity?

    @Query(
        """
        SELECT s.usuarioId, u.nome, u.email, s.papel, s.status, s.assinadoEm, s.caminhoAssinatura, s.ordem
        FROM documento_signatarios s
        INNER JOIN usuarios u ON u.id = s.usuarioId
        WHERE s.documentoId = :documentoId
        ORDER BY s.ordem
        """
    )
    fun observarSignatarios(documentoId: Long): Flow<List<SignatarioDocumentoRow>>

    @Query("SELECT * FROM documento_signatarios WHERE documentoId = :documentoId")
    suspend fun listarVinculos(documentoId: Long): List<DocumentoSignatarioEntity>

    @Query("SELECT * FROM campos_assinatura WHERE documentoId = :documentoId ORDER BY pagina, y")
    fun observarCampos(documentoId: Long): Flow<List<CampoAssinaturaEntity>>

    @Query("SELECT * FROM campos_assinatura WHERE documentoId = :documentoId ORDER BY pagina, y")
    suspend fun listarCampos(documentoId: Long): List<CampoAssinaturaEntity>

    @Query("SELECT * FROM eventos WHERE documentoId = :documentoId ORDER BY dataHora, id")
    fun observarEventos(documentoId: Long): Flow<List<EventoEntity>>

    @Query("SELECT * FROM eventos WHERE documentoId = :documentoId ORDER BY dataHora, id")
    suspend fun listarEventos(documentoId: Long): List<EventoEntity>

    @Query("SELECT COUNT(*) FROM eventos WHERE documentoId = :documentoId AND usuarioId = :usuarioId AND tipo = :tipo")
    suspend fun contarEventos(documentoId: Long, usuarioId: Long, tipo: String): Int

    @Query("SELECT COUNT(*) FROM documento_signatarios WHERE documentoId = :documentoId AND usuarioId = :usuarioId")
    suspend fun contarVinculo(documentoId: Long, usuarioId: Long): Int

    // ---------- Escrita ----------

    @Insert
    suspend fun inserirDocumento(documento: DocumentoEntity): Long

    @Update
    suspend fun atualizarDocumento(documento: DocumentoEntity)

    @Insert
    suspend fun inserirSignatarios(signatarios: List<DocumentoSignatarioEntity>)

    @Insert
    suspend fun inserirCampos(campos: List<CampoAssinaturaEntity>)

    @Insert
    suspend fun inserirEvento(evento: EventoEntity)

    @Insert
    suspend fun inserirEventos(eventos: List<EventoEntity>)

    @Query(
        """
        UPDATE documento_signatarios
        SET status = 'ASSINADO', assinadoEm = :quando, caminhoAssinatura = :caminhoAssinatura
        WHERE documentoId = :documentoId AND usuarioId = :usuarioId AND status = 'PENDENTE'
        """
    )
    suspend fun marcarAssinado(documentoId: Long, usuarioId: Long, caminhoAssinatura: String, quando: Long): Int
}
