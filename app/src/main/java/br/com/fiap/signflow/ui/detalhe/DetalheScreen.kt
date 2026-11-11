package br.com.fiap.signflow.ui.detalhe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.signflow.core.Formatters
import br.com.fiap.signflow.domain.model.DocumentoDetalhe
import br.com.fiap.signflow.domain.model.StatusDocumento
import br.com.fiap.signflow.domain.model.StatusSignatario
import br.com.fiap.signflow.domain.model.TipoEvento
import br.com.fiap.signflow.ui.appContainer
import br.com.fiap.signflow.ui.components.Avatar
import br.com.fiap.signflow.ui.components.BarraSuperior
import br.com.fiap.signflow.ui.components.EstadoVazio
import br.com.fiap.signflow.ui.components.EtiquetaStatus
import br.com.fiap.signflow.ui.components.Secao
import br.com.fiap.signflow.ui.components.corDoStatus
import br.com.fiap.signflow.ui.theme.Verde
import br.com.fiap.signflow.ui.theme.corDoSignatario

@Composable
fun DetalheScreen(
    documentoId: Long,
    aoVoltar: () -> Unit,
    aoVisualizar: (final: Boolean) -> Unit,
    aoNovaVersao: () -> Unit,
    aoAbrirDocumento: (Long) -> Unit
) {
    val container = appContainer()
    val vm: DetalheViewModel = viewModel(key = "detalhe-$documentoId") {
        DetalheViewModel(container.documentoRepository, container.session, documentoId)
    }
    val permitido by vm.permitido.collectAsStateWithLifecycle()
    val doc by vm.detalhe.collectAsStateWithLifecycle()

    Scaffold(topBar = { BarraSuperior("Acompanhamento", aoVoltar = aoVoltar) }) { padding ->
        val d = doc
        when {
            permitido == false -> EstadoVazio(
                Icons.Default.Lock, "Acesso negado",
                "Este documento não foi enviado para você.", Modifier.padding(padding)
            )
            permitido == null || d == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> Conteudo(checkNotNull(d), vm.ehAdmin, Modifier.padding(padding), aoVisualizar, aoNovaVersao, aoAbrirDocumento)
        }
    }
}

@Composable
private fun Conteudo(
    d: DocumentoDetalhe,
    ehAdmin: Boolean,
    modifier: Modifier,
    aoVisualizar: (Boolean) -> Unit,
    aoNovaVersao: () -> Unit,
    aoAbrirDocumento: (Long) -> Unit
) {
    val context = LocalContext.current
    val assinados = d.signatarios.count { it.status == StatusSignatario.ASSINADO }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // ---------- situação geral ----------
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EtiquetaStatus(d.status)
                    Spacer(Modifier.weight(1f))
                    Text("v${d.versao}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                Text(d.titulo, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${d.nomeArquivo} · ${d.totalPaginas} pág. · enviado por ${d.adminNome}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Text("$assinados de ${d.signatarios.size} assinaturas", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (d.signatarios.isEmpty()) 0f else assinados.toFloat() / d.signatarios.size },
                    color = corDoStatus(d.status),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(Modifier.height(12.dp))
                Datas(d)
            }
        }

        Spacer(Modifier.height(12.dp))
        if (d.status == StatusDocumento.CONCLUIDO && d.caminhoPdfFinal != null) {
            Button(onClick = { aoVisualizar(true) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.TaskAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("Abrir documento final assinado")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { compartilharPdf(context, d.caminhoPdfFinal, d.titulo) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Compartilhar PDF assinado")
            }
        } else {
            OutlinedButton(onClick = { aoVisualizar(false) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Visibility, null)
                Spacer(Modifier.width(8.dp))
                Text("Visualizar documento e campos")
            }
        }
        if (ehAdmin) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = aoNovaVersao, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.ContentCopy, null)
                Spacer(Modifier.width(8.dp))
                Text("Criar nova versão (novo fluxo)")
            }
            Text(
                "O documento enviado não pode ser alterado. Mudanças geram uma nova versão com um novo fluxo de assinaturas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            d.documentoOrigemId?.let { origem ->
                androidx.compose.material3.TextButton(onClick = { aoAbrirDocumento(origem) }) {
                    Text("Ver versão anterior")
                }
            }
        }

        // ---------- signatários ----------
        Spacer(Modifier.height(20.dp))
        Secao("Signatários") {
            d.signatarios.forEachIndexed { i, s ->
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(s.nome, corDoSignatario(i))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.nome, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${s.papel.rotulo} · ${s.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        s.assinadoEm?.let {
                            Text("Assinou em ${Formatters.dataHora(it)}", style = MaterialTheme.typography.bodySmall, color = Verde)
                        }
                    }
                    EtiquetaStatus(s.status)
                }
                if (i < d.signatarios.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }

        if (d.mensagem.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Secao("Mensagem do convite") {
                Text(
                    d.mensagem,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                )
            }
        }

        // ---------- histórico ----------
        Spacer(Modifier.height(20.dp))
        Secao("Histórico") {
            d.eventos.forEachIndexed { i, e ->
                Row {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(12.dp).background(
                                when (e.tipo) {
                                    TipoEvento.ASSINADO, TipoEvento.CONCLUIDO -> Verde
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                CircleShape
                            )
                        )
                        if (i < d.eventos.lastIndex) {
                            Box(Modifier.width(2.dp).height(40.dp).background(MaterialTheme.colorScheme.outline))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.padding(bottom = 10.dp)) {
                        Text(e.tipo.rotulo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Text(e.descricao, style = MaterialTheme.typography.bodySmall)
                        Text(
                            Formatters.dataHoraSegundos(e.dataHora),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ---------- integridade ----------
        Spacer(Modifier.height(20.dp))
        Secao("Integridade") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text("Protocolo: ${d.protocolo}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text("SHA-256 do arquivo original:", style = MaterialTheme.typography.bodySmall)
                Text(d.hashOriginal, fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 13.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Datas(d: DocumentoDetalhe) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LinhaData("Criado", d.criadoEm)
        d.enviadoEm?.let { LinhaData("Enviado", it) }
        d.concluidoEm?.let { LinhaData("Concluído", it) }
    }
}

@Composable
private fun LinhaData(rotulo: String, quando: Long) {
    Row {
        Text("$rotulo:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(Formatters.dataHora(quando), style = MaterialTheme.typography.bodySmall)
    }
}
