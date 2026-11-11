package br.com.fiap.signflow.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.signflow.core.Formatters
import br.com.fiap.signflow.domain.model.DocumentoResumo
import br.com.fiap.signflow.domain.model.StatusDocumento
import br.com.fiap.signflow.ui.appContainer
import br.com.fiap.signflow.ui.components.EstadoVazio
import br.com.fiap.signflow.ui.components.EtiquetaStatus
import br.com.fiap.signflow.ui.components.corDoStatus
import br.com.fiap.signflow.ui.theme.GradienteMarca

@Composable
fun AdminHomeScreen(
    aoNovoDocumento: () -> Unit,
    aoAbrirDocumento: (Long) -> Unit,
    aoSignatarios: () -> Unit,
    aoSair: () -> Unit
) {
    val container = appContainer()
    val vm: AdminHomeViewModel = viewModel { AdminHomeViewModel(container.documentoRepository, container.session) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = aoNovoDocumento,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Novo documento") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 88.dp)
        ) {
            item {
                Cabecalho(
                    nome = state.nome,
                    contagem = state.contagem,
                    topo = padding.calculateTopPadding(),
                    aoSignatarios = aoSignatarios,
                    aoSair = { vm.sair(); aoSair() }
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filtros = listOf<StatusDocumento?>(null) + StatusDocumento.entries
                    items(filtros) { f ->
                        FilterChip(
                            selected = state.filtro == f,
                            onClick = { vm.filtrar(f) },
                            label = { Text("${f?.rotulo ?: "Todos"} (${state.contagem[f] ?: 0})") }
                        )
                    }
                }
            }
            if (!state.carregando && state.documentos.isEmpty()) {
                item {
                    EstadoVazio(
                        icone = Icons.Default.Description,
                        titulo = if (state.filtro == null) "Nenhum documento ainda" else "Nada por aqui",
                        descricao = if (state.filtro == null) {
                            "Toque em \"Novo documento\" para enviar um PDF para assinatura."
                        } else {
                            "Não há documentos com o status \"${state.filtro?.rotulo}\"."
                        }
                    )
                }
            }
            items(state.documentos, key = { it.id }) { doc ->
                CartaoDocumento(doc, onClick = { aoAbrirDocumento(doc.id) })
            }
        }
    }
}

@Composable
private fun Cabecalho(
    nome: String,
    contagem: Map<StatusDocumento?, Int>,
    topo: androidx.compose.ui.unit.Dp,
    aoSignatarios: () -> Unit,
    aoSair: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(GradienteMarca, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .padding(top = topo + 8.dp, start = 20.dp, end = 8.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Olá, ${nome.substringBefore(" ")}", color = Color.White.copy(alpha = 0.85f))
                Text("Seus documentos", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = aoSignatarios) {
                Icon(Icons.Default.Group, contentDescription = "Signatários", tint = Color.White)
            }
            IconButton(onClick = aoSair) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair", tint = Color.White)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.padding(end = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusDocumento.entries.forEach { s ->
                Column(
                    Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text("${contagem[s] ?: 0}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (s == StatusDocumento.PARCIAL) "Parciais" else s.rotulo + "s",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun CartaoDocumento(doc: DocumentoResumo, onClick: () -> Unit, extra: @Composable () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(doc.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "v${doc.versao} · ${Formatters.data(doc.criadoEm)} · ${doc.totalAssinados}/${doc.totalSignatarios} assinaturas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (doc.totalSignatarios == 0) 0f else doc.totalAssinados.toFloat() / doc.totalSignatarios },
                    color = corDoStatus(doc.status),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(5.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EtiquetaStatus(doc.status)
                    Spacer(Modifier.width(6.dp))
                    extra()
                }
            }
        }
    }
}
