package br.com.fiap.signflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.signflow.domain.model.StatusDocumento
import br.com.fiap.signflow.domain.model.StatusSignatario
import br.com.fiap.signflow.ui.theme.Ambar
import br.com.fiap.signflow.ui.theme.Azul
import br.com.fiap.signflow.ui.theme.Verde

fun corDoStatus(status: StatusDocumento): Color = when (status) {
    StatusDocumento.ENVIADO -> Azul
    StatusDocumento.PARCIAL -> Ambar
    StatusDocumento.CONCLUIDO -> Verde
}

fun corDoStatus(status: StatusSignatario): Color = when (status) {
    StatusSignatario.PENDENTE -> Ambar
    StatusSignatario.ASSINADO -> Verde
}

@Composable
fun Etiqueta(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(cor.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).background(cor, CircleShape))
        Spacer(Modifier.size(6.dp))
        Text(texto, color = cor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EtiquetaStatus(status: StatusDocumento, modifier: Modifier = Modifier) =
    Etiqueta(status.rotulo, corDoStatus(status), modifier)

@Composable
fun EtiquetaStatus(status: StatusSignatario, modifier: Modifier = Modifier) =
    Etiqueta(status.rotulo, corDoStatus(status), modifier)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    subtitulo: String? = null,
    aoVoltar: (() -> Unit)? = null,
    acoes: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(titulo, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                if (subtitulo != null) {
                    Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        navigationIcon = {
            if (aoVoltar != null) {
                IconButton(onClick = aoVoltar) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        },
        actions = { acoes() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun EstadoVazio(icone: ImageVector, titulo: String, descricao: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            descricao,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CartaoErro(mensagem: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.size(10.dp))
            Text(mensagem, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun Secao(titulo: String, modifier: Modifier = Modifier, conteudo: @Composable () -> Unit) {
    Column(modifier) {
        Text(
            titulo.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(8.dp))
        conteudo()
    }
}

fun iniciais(nome: String): String =
    nome.trim().split(" ").filter { it.isNotBlank() }.let { partes ->
        when {
            partes.isEmpty() -> "?"
            partes.size == 1 -> partes[0].take(2)
            else -> "${partes.first().first()}${partes.last().first()}"
        }
    }.uppercase()

@Composable
fun Avatar(nome: String, cor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).background(cor.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(iniciais(nome), color = cor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
