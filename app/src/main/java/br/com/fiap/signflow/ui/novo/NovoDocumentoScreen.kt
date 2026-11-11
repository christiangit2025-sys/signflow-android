package br.com.fiap.signflow.ui.novo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.signflow.ui.appContainer
import br.com.fiap.signflow.ui.components.BarraSuperior
import br.com.fiap.signflow.ui.components.rememberPdf

@Composable
fun NovoDocumentoScreen(
    origemId: Long?,
    aoSair: () -> Unit,
    aoCadastrarSignatario: () -> Unit,
    aoAcompanhar: (Long) -> Unit
) {
    val container = appContainer()
    val vm: NovoDocumentoViewModel = viewModel(key = "novo-$origemId") {
        NovoDocumentoViewModel(
            container.documentoRepository,
            container.signatarioRepository,
            container.pdfStorage,
            container.session,
            origemId
        )
    }
    val s by vm.state.collectAsStateWithLifecycle()
    val pdf = rememberPdf(s.caminhoPdf)
    var confirmarSaida by remember { mutableStateOf(false) }

    fun tentarSair() {
        if (s.etapa == Etapa.RESULTADO || !s.temAlteracoes) aoSair() else confirmarSaida = true
    }

    BackHandler {
        if (!vm.voltar()) tentarSair()
    }

    if (confirmarSaida) {
        AlertDialog(
            onDismissRequest = { confirmarSaida = false },
            title = { Text("Descartar documento?") },
            text = { Text("O documento ainda não foi enviado. As informações preenchidas serão perdidas.") },
            confirmButton = { TextButton(onClick = { confirmarSaida = false; aoSair() }) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { confirmarSaida = false }) { Text("Continuar editando") } }
        )
    }

    Scaffold(
        topBar = {
            Column {
                BarraSuperior(
                    titulo = if (s.versao > 1) "Nova versão (v${s.versao})" else "Novo documento",
                    subtitulo = if (s.etapa == Etapa.RESULTADO) null else "Etapa ${s.etapa.ordinal + 1} de 4 · ${s.etapa.titulo}",
                    aoVoltar = { if (!vm.voltar()) tentarSair() }
                )
                if (s.etapa != Etapa.RESULTADO) Indicador(s.etapa, vm::irPara)
            }
        },
        bottomBar = {
            if (s.etapa != Etapa.RESULTADO) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (s.etapa != Etapa.DADOS) {
                            OutlinedButton(onClick = { vm.voltar() }, enabled = !s.enviando) { Text("Voltar") }
                        }
                        Spacer(Modifier.weight(1f))
                        if (s.etapa == Etapa.REVISAO) {
                            val falhou = s.resultado is ResultadoEnvio.Falha
                            Button(onClick = vm::enviar, enabled = !s.enviando, shape = RoundedCornerShape(14.dp)) {
                                if (s.enviando) {
                                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Enviando…")
                                } else {
                                    Icon(if (falhou) Icons.Default.Refresh else Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (falhou) "Tentar novamente" else "Confirmar envio")
                                }
                            }
                        } else {
                            Button(onClick = vm::avancar, enabled = !s.importando, shape = RoundedCornerShape(14.dp)) {
                                Text("Continuar")
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (s.carregandoVersao) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else when (s.etapa) {
                Etapa.DADOS -> EtapaDados(s, pdf, vm)
                Etapa.SIGNATARIOS -> EtapaSignatarios(s, vm, aoCadastrarSignatario)
                Etapa.CAMPOS -> EtapaCampos(s, pdf, vm)
                Etapa.REVISAO -> EtapaRevisao(s, pdf, vm)
                Etapa.RESULTADO -> EtapaResultado(s, aoAcompanhar = aoAcompanhar, aoInicio = aoSair)
            }
        }
    }
}

@Composable
private fun Indicador(atual: Etapa, aoTocar: (Etapa) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Etapa.entries.filter { it != Etapa.RESULTADO }.forEach { e ->
                val feita = e.ordinal < atual.ordinal
                val ativa = e == atual
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(
                                when {
                                    ativa -> MaterialTheme.colorScheme.primary
                                    feita -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                RoundedCornerShape(50)
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { aoTocar(e) },
                        enabled = feita,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text(
                            e.titulo.substringBefore(" "),
                            fontSize = 11.sp,
                            fontWeight = if (ativa) FontWeight.Bold else FontWeight.Normal,
                            color = if (ativa || feita) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}
