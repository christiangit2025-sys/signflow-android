package br.com.fiap.signflow.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val CorTinta = Color(0xFF1B1464)

/**
 * Captura a assinatura desenhada com o dedo e devolve um PNG com fundo transparente,
 * recortado ao redor do traço.
 */
@Composable
fun PainelAssinaturaDialog(
    nome: String,
    aoCancelar: () -> Unit,
    aoConfirmar: (Bitmap) -> Unit
) {
    val tracos = remember { mutableStateListOf<List<Offset>>() }
    var tracoAtual by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var tamanho by remember { mutableStateOf(IntSize.Zero) }

    Dialog(onDismissRequest = aoCancelar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Assine no quadro abaixo", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Assinando como $nome",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(2.4f)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .onSizeChanged { tamanho = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { tracoAtual = listOf(it) },
                                onDrag = { mudanca, _ -> tracoAtual = tracoAtual + mudanca.position },
                                onDragEnd = {
                                    if (tracoAtual.size > 1) tracos.add(tracoAtual)
                                    tracoAtual = emptyList()
                                },
                                onDragCancel = { tracoAtual = emptyList() }
                            )
                        }
                        .pointerInput(Unit) {
                            // um toque vira um ponto (útil para pingos de "i" e "j")
                            detectTapGestures { p -> tracos.add(listOf(p, p + Offset(1f, 1f))) }
                        }
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        // linha guia
                        val yGuia = size.height * 0.78f
                        drawLine(Color(0xFFD9D3EE), Offset(24f, yGuia), Offset(size.width - 24f, yGuia), strokeWidth = 2f)
                        (tracos + listOf(tracoAtual)).forEach { pontos ->
                            if (pontos.size > 1) {
                                val path = Path().apply {
                                    moveTo(pontos.first().x, pontos.first().y)
                                    pontos.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(path, CorTinta, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }
                    }
                    if (tracos.isEmpty() && tracoAtual.isEmpty()) {
                        Text(
                            "Desenhe sua assinatura aqui",
                            color = Color(0xFFB0A8CC),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { tracos.clear() }, enabled = tracos.isNotEmpty()) { Text("Limpar") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = aoCancelar) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { aoConfirmar(gerarBitmap(tracos.toList(), tamanho)) },
                        enabled = tracos.isNotEmpty() && tamanho != IntSize.Zero
                    ) { Text("Usar assinatura") }
                }
            }
        }
    }
}

/** Desenha os traços num Bitmap transparente e recorta o espaço vazio em volta. */
private fun gerarBitmap(tracos: List<List<Offset>>, tamanho: IntSize): Bitmap {
    val todos = tracos.flatten()
    val margem = 12f
    val minX = (todos.minOf { it.x } - margem).coerceAtLeast(0f)
    val minY = (todos.minOf { it.y } - margem).coerceAtLeast(0f)
    val maxX = (todos.maxOf { it.x } + margem).coerceAtMost(tamanho.width.toFloat())
    val maxY = (todos.maxOf { it.y } + margem).coerceAtMost(tamanho.height.toFloat())
    val largura = (maxX - minX).toInt().coerceAtLeast(10)
    val altura = (maxY - minY).toInt().coerceAtLeast(10)

    val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val tinta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(0x1B, 0x14, 0x64)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    tracos.forEach { pontos ->
        val path = android.graphics.Path()
        path.moveTo(pontos.first().x - minX, pontos.first().y - minY)
        pontos.drop(1).forEach { path.lineTo(it.x - minX, it.y - minY) }
        canvas.drawPath(path, tinta)
    }
    return bitmap
}
