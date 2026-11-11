package br.com.fiap.signflow.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.signflow.domain.model.CampoAssinatura
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Modificador que posiciona um campo (coordenadas relativas) dentro da página exibida. */
fun Modifier.posicaoDoCampo(escopo: BoxWithConstraintsScope, campo: CampoAssinatura): Modifier = with(escopo) {
    this@posicaoDoCampo
        .offset(x = maxWidth * campo.x, y = maxHeight * campo.y)
        .size(width = maxWidth * campo.largura, height = maxHeight * campo.altura)
}

/** Campo somente leitura: mostra o nome do signatário ou a assinatura já feita. */
@Composable
fun BoxWithConstraintsScope.CampoEstatico(
    campo: CampoAssinatura,
    rotulo: String,
    cor: Color,
    assinatura: ImageBitmap? = null,
    destaque: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .posicaoDoCampo(this, campo)
            .then(modifier)
            .background(cor.copy(alpha = if (assinatura != null) 0.06f else if (destaque) 0.28f else 0.16f), RoundedCornerShape(4.dp))
            .border(if (destaque) 2.dp else 1.dp, cor, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (assinatura != null) {
            Image(
                bitmap = assinatura,
                contentDescription = "Assinatura",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(2.dp)
            )
        } else {
            Text(
                text = rotulo,
                color = cor,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

/** Carrega a imagem PNG de uma assinatura gravada no armazenamento interno. */
@Composable
fun rememberImagemAssinatura(caminho: String?): ImageBitmap? {
    val imagem by produceState<ImageBitmap?>(null, caminho) {
        value = caminho?.let {
            withContext(Dispatchers.IO) {
                android.graphics.BitmapFactory.decodeFile(it)?.asImageBitmap()
            }
        }
    }
    return imagem
}
