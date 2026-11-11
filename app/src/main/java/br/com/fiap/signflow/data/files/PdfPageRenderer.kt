package br.com.fiap.signflow.data.files

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.Closeable
import java.io.File

/**
 * Renderiza páginas de PDF em Bitmap com o PdfRenderer nativo do Android.
 * O PdfRenderer não é thread-safe, por isso os acessos são sincronizados.
 */
class PdfPageRenderer(arquivo: File) : Closeable {

    private val descriptor = ParcelFileDescriptor.open(arquivo, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descriptor)

    val totalPaginas: Int get() = renderer.pageCount

    /** Proporção largura/altura de cada página (usada para posicionar campos em coordenadas relativas). */
    val proporcoes: List<Float> = synchronized(this) {
        (0 until renderer.pageCount).map { i ->
            renderer.openPage(i).use { it.width.toFloat() / it.height.toFloat() }
        }
    }

    fun renderizar(pagina: Int, larguraPx: Int): Bitmap = synchronized(this) {
        renderer.openPage(pagina).use { page ->
            val largura = larguraPx.coerceIn(200, 1600)
            val altura = (largura * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        }
    }

    override fun close() = synchronized(this) {
        renderer.close()
        descriptor.close()
    }
}
