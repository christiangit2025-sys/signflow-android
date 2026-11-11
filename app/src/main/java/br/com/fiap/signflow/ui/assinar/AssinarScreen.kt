package br.com.fiap.signflow.ui.assinar

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.signflow.core.Formatters
import br.com.fiap.signflow.domain.model.StatusDocumento
import br.com.fiap.signflow.domain.model.StatusSignatario
import br.com.fiap.signflow.ui.appContainer
import br.com.fiap.signflow.ui.components.BarraSuperior
import br.com.fiap.signflow.ui.components.CampoEstatico
import br.com.fiap.signflow.ui.components.CartaoErro
import br.com.fiap.signflow.ui.components.EstadoPdf
import br.com.fiap.signflow.ui.components.EstadoVazio
import br.com.fiap.signflow.ui.components.PaginaPdf
import br.com.fiap.signflow.ui.components.PainelAssinaturaDialog
import br.com.fiap.signflow.ui.components.posicaoDoCampo
import br.com.fiap.signflow.ui.components.rememberImagemAssinatura
import br.com.fiap.signflow.ui.components.rememberPdf
import br.com.fiap.signflow.ui.theme.Roxo
import br.com.fiap.signflow.ui.theme.Verde

@Composable
fun AssinarScreen(documentoId: Long, aoVoltar: () -> Unit, aoAcompanhar: () -> Unit) {
    val container = appContainer()
    val vm: AssinarViewModel = viewModel(key = "assinar-$documentoId") {
        AssinarViewModel(container.documentoRepository, container.pdfStorage, container.session, documentoId)
    }
    val s by vm.state.collectAsStateWithLifecycle()
    val doc by vm.detalhe.collectAsStateWithLifecycle()
    val d = doc
    val pdf = rememberPdf(if (s.permitido == true) d?.caminhoPdf else null)
    var desenhando by remember { mutableStateOf(false) }

    val eu = d?.signatarios?.firstOrNull { it.usuarioId == vm.usuario.id }
    val jaAssinado = eu?.status == StatusSignatario.ASSINADO

    if (desenhando) {
        PainelAssinaturaDialog(
            nome = vm.usuario.nome,
            aoCancelar = { desenhando = false },
            aoConfirmar = { vm.definirAssinatura(it); desenhando = false }
        )
    }

    s.concluido?.let { status ->
        Sucesso(status, aoVoltar, aoAcompanhar)
        return
    }

    Scaffold(
        topBar = { BarraSuperior(d?.titulo ?: "Assinar documento", "Enviado por ${d?.adminNome ?: "…"}", aoVoltar) },
        bottomBar = {
            if (s.permitido == true && d != null && !jaAssinado) {
                Surface(shadowElevation = 8.dp) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                        s.erro?.let { CartaoErro(it); Spacer(Modifier.height(8.dp)) }
                        if (s.assinatura == null) {
                            Button(onClick = { desenhando = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                                Icon(Icons.Default.Draw, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Assinar documento")
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth().clickable { vm.aceite(!s.aceite) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = s.aceite, onCheckedChange = vm::aceite)
                                Text(
                                    "Li o documento e concordo em assiná-lo eletronicamente como ${eu?.papel?.rotulo?.lowercase()}.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { vm.limparAssinatura(); desenhando = true }) { Text("Refazer") }
                                Spacer(Modifier.weight(1f))
                                Button(onClick = vm::salvar, enabled = !s.salvando, shape = RoundedCornerShape(14.dp)) {
                                    if (s.salvando) {
                                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text("Salvar documento assinado")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (s.permitido == false) {
            EstadoVazio(Icons.Default.Lock, "Acesso negado", "Este documento não foi enviado para você.", Modifier.padding(padding))
            return@Scaffold
        }
        if (d == null || pdf is EstadoPdf.Carregando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (pdf is EstadoPdf.Erro) {
            Box(Modifier.padding(padding).padding(16.dp)) { CartaoErro(pdf.mensagem) }
            return@Scaffold
        }
        val renderer = (pdf as EstadoPdf.Pronto).renderer
        val minhaPrevia = remember(s.assinatura) { s.assinatura?.asImageBitmap() }
        val porUsuario = d.signatarios.associateBy { it.usuarioId }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 12.dp, end = 12.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            )
        ) {
            item {
                if (jaAssinado) {
                    Card(colors = CardDefaults.cardColors(containerColor = Verde.copy(alpha = 0.1f)), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Verde)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Você já assinou este documento", style = MaterialTheme.typography.titleMedium)
                                eu?.assinadoEm?.let { Text("Em ${Formatters.dataHora(it)}", style = MaterialTheme.typography.bodySmall) }
                            }
                            TextButton(onClick = aoAcompanhar) { Text("Acompanhar") }
                        }
                    }
                } else if (d.mensagem.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp)) {
                            Icon(Icons.Default.MarkEmailRead, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Mensagem de ${d.adminNome}", style = MaterialTheme.typography.labelLarge)
                                Text(d.mensagem, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (!jaAssinado) {
                    Text(
                        "Seus campos estão destacados. Toque em um deles ou no botão abaixo para assinar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(renderer.totalPaginas) { p ->
                Text(
                    "Página ${p + 1} de ${renderer.totalPaginas}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                PaginaPdf(renderer, p) {
                    d.campos.filter { it.pagina == p }.forEach { campo ->
                        val meu = campo.usuarioId == vm.usuario.id
                        val sig = porUsuario[campo.usuarioId]
                        val gravada = rememberImagemAssinatura(sig?.caminhoAssinatura)
                        if (meu && !jaAssinado) {
                            CampoEstatico(
                                campo = campo,
                                rotulo = "Toque para assinar",
                                cor = Roxo,
                                assinatura = minhaPrevia,
                                destaque = true,
                                modifier = Modifier.clickable { desenhando = true }
                            )
                        } else {
                            CampoEstatico(
                                campo = campo,
                                rotulo = sig?.nome?.substringBefore(" ") ?: "?",
                                cor = if (meu) Roxo else Color.Gray,
                                assinatura = gravada
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun Sucesso(status: StatusDocumento, aoVoltar: () -> Unit, aoAcompanhar: () -> Unit) {
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(96.dp).background(Verde.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, null, tint = Verde, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Assinatura registrada!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                if (status == StatusDocumento.CONCLUIDO) {
                    "Você foi o último a assinar. O documento foi concluído e o PDF final com todas as assinaturas já está disponível."
                } else {
                    "Seu registro foi concluído. O documento segue aguardando as assinaturas dos demais participantes."
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
            Button(onClick = aoAcompanhar, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Ver acompanhamento")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = aoVoltar, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Voltar aos meus documentos")
            }
        }
    }
}
