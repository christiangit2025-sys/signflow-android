package br.com.fiap.signflow.ui.detalhe

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Compartilha o PDF final por meio do FileProvider (o arquivo continua privado ao app). */
fun compartilharPdf(context: Context, caminho: String, titulo: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(caminho))
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, titulo)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar documento assinado"))
}
