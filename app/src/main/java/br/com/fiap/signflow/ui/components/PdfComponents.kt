package br.com.fiap.signflow.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import br.com.fiap.signflow.data.files.PdfPageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface EstadoPdf {
    data object Carregando : EstadoPdf
    data class Pronto(val renderer: PdfPageRenderer) : EstadoPdf
    data class Erro(val mensagem: String) : EstadoPdf
}

/**
 * Abre o PDF em segundo plano e fecha o renderer quando a tela sai de composição.
 * Enquanto não houver caminho (dados ainda carregando), o estado fica em [EstadoPdf.Carregando].
 */
@Composable
fun rememberPdf(caminho: String?): EstadoPdf {
    // o estado é recriado de forma síncrona quando o caminho muda (sem "piscar" o estado anterior)
    val estado = remember(caminho) { mutableStateOf<EstadoPdf>(EstadoPdf.Carregando) }
    LaunchedEffect(caminho) {
        if (caminho == null) return@LaunchedEffect
        estado.value = withContext(Dispatchers.IO) {
            try {
                EstadoPdf.Pronto(PdfPageRenderer(File(caminho)))
            } catch (e: Exception) {
                EstadoPdf.Erro("Não foi possível abrir o PDF.")
            }
        }
    }
    val atual = estado.value
    DisposableEffect(atual) {
        onDispose { if (atual is EstadoPdf.Pronto) atual.renderer.close() }
    }
    return atual
}

/**
 * Exibe uma página do PDF mantendo a proporção real dela. O conteúdo em [sobreposicao]
 * recebe as dimensões da página para posicionar campos em coordenadas relativas.
 */
@Composable
fun PaginaPdf(
    renderer: PdfPageRenderer,
    pagina: Int,
    modifier: Modifier = Modifier,
    sobreposicao: @Composable BoxWithConstraintsScope.() -> Unit = {}
) {
    val proporcao = renderer.proporcoes.getOrElse(pagina) { 0.707f }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(proporcao)
            .background(Color.White)
            .border(1.dp, Color(0x22000000))
    ) {
        val larguraPx = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
        val bitmap by produceState<android.graphics.Bitmap?>(null, renderer, pagina, larguraPx) {
            value = withContext(Dispatchers.IO) {
                runCatching { renderer.renderizar(pagina, larguraPx) }.getOrNull()
            }
        }
        val atual = bitmap
        if (atual == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center).size(28.dp))
        } else {
            Image(
                bitmap = atual.asImageBitmap(),
                contentDescription = "Página ${pagina + 1}",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        sobreposicao()
    }
}
