package br.com.fiap.signflow.ui.visualizar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.signflow.ui.appContainer
import br.com.fiap.signflow.ui.components.BarraSuperior
import br.com.fiap.signflow.ui.components.CampoEstatico
import br.com.fiap.signflow.ui.components.CartaoErro
import br.com.fiap.signflow.ui.components.EstadoPdf
import br.com.fiap.signflow.ui.components.EstadoVazio
import br.com.fiap.signflow.ui.components.PaginaPdf
import br.com.fiap.signflow.ui.components.rememberImagemAssinatura
import br.com.fiap.signflow.ui.components.rememberPdf
import br.com.fiap.signflow.ui.detalhe.DetalheViewModel
import br.com.fiap.signflow.ui.detalhe.compartilharPdf
import br.com.fiap.signflow.ui.theme.corDoSignatario

/**
 * Visualiza o PDF. No modo "final" mostra o arquivo carimbado gerado pelo PDFBox;
 * caso contrário mostra o original com os campos (e assinaturas já feitas) sobrepostos.
 */
@Composable
fun VisualizarScreen(documentoId: Long, final: Boolean, aoVoltar: () -> Unit) {
    val container = appContainer()
    val context = LocalContext.current
    val vm: DetalheViewModel = viewModel(key = "visualizar-$documentoId") {
        DetalheViewModel(container.documentoRepository, container.session, documentoId)
    }
    val permitido by vm.permitido.collectAsStateWithLifecycle()
    val doc by vm.detalhe.collectAsStateWithLifecycle()
    val d = doc
    val caminho = if (final) d?.caminhoPdfFinal else d?.caminhoPdf
    val pdf = rememberPdf(if (permitido == true) caminho else null)

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = d?.titulo ?: "Documento",
                subtitulo = if (final) "Documento final assinado" else "Original com campos de assinatura",
                aoVoltar = aoVoltar,
                acoes = {
                    val arquivoFinal = d?.caminhoPdfFinal
                    if (final && d != null && arquivoFinal != null) {
                        IconButton(onClick = { compartilharPdf(context, arquivoFinal, d.titulo) }) {
                            Icon(Icons.Default.Share, "Compartilhar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (permitido == false) {
            EstadoVazio(Icons.Default.Lock, "Acesso negado", "Você não tem acesso a este documento.", Modifier.padding(padding))
        } else if (d == null || pdf is EstadoPdf.Carregando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else when (pdf) {
            is EstadoPdf.Carregando -> Unit
            is EstadoPdf.Erro -> Box(Modifier.padding(padding).padding(16.dp)) { CartaoErro(pdf.mensagem) }
            is EstadoPdf.Pronto -> {
                val indices = d.signatarios.mapIndexed { i, s -> s.usuarioId to i }.toMap()
                val porUsuario = d.signatarios.associateBy { it.usuarioId }
                LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 24.dp)) {
                    items(pdf.renderer.totalPaginas) { p ->
                        Text(
                            "Página ${p + 1} de ${pdf.renderer.totalPaginas}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        PaginaPdf(pdf.renderer, p) {
                            if (!final) {
                                d.campos.filter { it.pagina == p }.forEach { campo ->
                                    val sig = porUsuario[campo.usuarioId]
                                    val imagem = rememberImagemAssinatura(sig?.caminhoAssinatura)
                                    CampoEstatico(
                                        campo = campo,
                                        rotulo = sig?.nome?.substringBefore(" ") ?: "?",
                                        cor = corDoSignatario(indices[campo.usuarioId] ?: 0),
                                        assinatura = imagem
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
