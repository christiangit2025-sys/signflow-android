package br.com.fiap.signflow.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

sealed interface ImportacaoPdf {
    data class Sucesso(val caminho: String, val nomeArquivo: String, val totalPaginas: Int) : ImportacaoPdf
    data class Falha(val motivo: String) : ImportacaoPdf
}

/**
 * Armazena os PDFs no sistema de arquivos interno do app (privado, inacessível a outros apps).
 * Estrutura: filesDir/pdfs/originais, filesDir/pdfs/assinaturas, filesDir/pdfs/finais
 */
class PdfStorage(private val context: Context) {

    private val raiz get() = File(context.filesDir, "pdfs")
    val pastaOriginais get() = File(raiz, "originais").apply { mkdirs() }
    val pastaAssinaturas get() = File(raiz, "assinaturas").apply { mkdirs() }
    val pastaFinais get() = File(raiz, "finais").apply { mkdirs() }

    /** Copia o PDF escolhido para o app e valida se ele pode ser processado. */
    suspend fun importar(uri: Uri): ImportacaoPdf = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var nome = "documento.pdf"
        var tamanho = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getString(0)?.let { nome = it }
                if (!c.isNull(1)) tamanho = c.getLong(1)
            }
        }

        if (!nome.endsWith(".pdf", ignoreCase = true) && resolver.getType(uri) != "application/pdf") {
            return@withContext ImportacaoPdf.Falha("O arquivo selecionado não é um PDF.")
        }
        if (tamanho == 0L) return@withContext ImportacaoPdf.Falha("O arquivo está vazio.")
        if (tamanho > TAMANHO_MAXIMO) {
            return@withContext ImportacaoPdf.Falha("O arquivo tem mais de ${TAMANHO_MAXIMO / 1024 / 1024} MB.")
        }

        val destino = File(pastaOriginais, "${UUID.randomUUID()}.pdf")
        try {
            val entrada = resolver.openInputStream(uri)
                ?: return@withContext ImportacaoPdf.Falha("Não foi possível abrir o arquivo.")
            entrada.use { input -> FileOutputStream(destino).use { input.copyTo(it) } }
        } catch (e: Exception) {
            destino.delete()
            return@withContext ImportacaoPdf.Falha("Não foi possível ler o arquivo selecionado.")
        }

        when (val validacao = validar(destino)) {
            is ImportacaoPdf.Falha -> {
                destino.delete()
                validacao
            }
            is ImportacaoPdf.Sucesso -> validacao.copy(nomeArquivo = nome)
        }
    }

    /** Verifica cabeçalho %PDF, abertura, senha e quantidade de páginas. */
    fun validar(arquivo: File): ImportacaoPdf {
        val cabecalho = ByteArray(5)
        val lidos = arquivo.inputStream().use { it.read(cabecalho) }
        if (lidos < 5 || String(cabecalho) != "%PDF-") {
            return ImportacaoPdf.Falha("Arquivo inválido: o conteúdo não é de um PDF.")
        }
        return try {
            ParcelFileDescriptor.open(arquivo, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    if (renderer.pageCount == 0) {
                        ImportacaoPdf.Falha("O PDF não possui páginas.")
                    } else {
                        // tenta renderizar a 1ª página para garantir que o arquivo não está corrompido
                        renderer.openPage(0).use { page ->
                            val teste = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                            page.render(teste, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            teste.recycle()
                        }
                        ImportacaoPdf.Sucesso(arquivo.absolutePath, arquivo.name, renderer.pageCount)
                    }
                }
            }
        } catch (e: SecurityException) {
            ImportacaoPdf.Falha("O PDF está protegido por senha e não pode ser processado.")
        } catch (e: IOException) {
            ImportacaoPdf.Falha("O PDF está corrompido ou não pôde ser processado.")
        } catch (e: Exception) {
            ImportacaoPdf.Falha("O PDF não pôde ser processado.")
        }
    }

    /** Uma nova versão recebe sua própria cópia: o documento já enviado nunca é alterado. */
    fun duplicar(caminho: String): String {
        val destino = File(pastaOriginais, "${UUID.randomUUID()}.pdf")
        File(caminho).copyTo(destino)
        return destino.absolutePath
    }

    fun salvarAssinatura(bitmap: Bitmap): String {
        val arquivo = File(pastaAssinaturas, "${UUID.randomUUID()}.png")
        FileOutputStream(arquivo).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return arquivo.absolutePath
    }

    fun descartar(caminho: String?) {
        if (caminho != null) File(caminho).delete()
    }

    fun sha256(caminho: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        File(caminho).inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val TAMANHO_MAXIMO = 20L * 1024 * 1024
    }
}
