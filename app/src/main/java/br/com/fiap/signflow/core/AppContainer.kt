package br.com.fiap.signflow.core

import android.content.Context
import br.com.fiap.signflow.data.files.PdfStamper
import br.com.fiap.signflow.data.files.PdfStorage
import br.com.fiap.signflow.data.local.AppDatabase
import br.com.fiap.signflow.data.repository.RoomAuthRepository
import br.com.fiap.signflow.data.repository.RoomDocumentoRepository
import br.com.fiap.signflow.data.repository.RoomSignatarioRepository
import br.com.fiap.signflow.domain.repository.AuthRepository
import br.com.fiap.signflow.domain.repository.DocumentoRepository
import br.com.fiap.signflow.domain.repository.SignatarioRepository

/**
 * Injeção de dependências manual. Para migrar para a API REST (Sprint 2),
 * basta trocar as implementações Room* aqui.
 */
class AppContainer(context: Context) {
    private val database = AppDatabase.criar(context)

    val pdfStorage = PdfStorage(context)
    val session = SessionManager(context)

    val authRepository: AuthRepository = RoomAuthRepository(database.usuarioDao())
    val signatarioRepository: SignatarioRepository = RoomSignatarioRepository(database.usuarioDao())
    val documentoRepository: DocumentoRepository =
        RoomDocumentoRepository(database, pdfStorage, PdfStamper(pdfStorage))
}
