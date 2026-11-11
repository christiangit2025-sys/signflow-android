package br.com.fiap.signflow.ui.signatario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.signflow.core.SessionManager
import br.com.fiap.signflow.domain.model.DocumentoResumo
import br.com.fiap.signflow.domain.model.StatusSignatario
import br.com.fiap.signflow.domain.repository.DocumentoRepository
import br.com.fiap.signflow.ui.admin.CartaoDocumento
import br.com.fiap.signflow.ui.appContainer
import br.com.fiap.signflow.ui.components.Etiqueta
import br.com.fiap.signflow.ui.components.EstadoVazio
import br.com.fiap.signflow.ui.components.corDoStatus
import br.com.fiap.signflow.ui.theme.GradienteMarca
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CaixaEntradaViewModel(repo: DocumentoRepository, private val session: SessionManager) : ViewModel() {
    val nome = session.usuarioAtual.nome
    val documentos: StateFlow<List<DocumentoResumo>?> = repo.observarDoSignatario(session.usuarioAtual.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun sair() = session.encerrar()
}

@Composable
fun CaixaEntradaScreen(aoAbrir: (DocumentoResumo) -> Unit, aoSair: () -> Unit) {
    val container = appContainer()
    val vm: CaixaEntradaViewModel = viewModel { CaixaEntradaViewModel(container.documentoRepository, container.session) }
    val docs by vm.documentos.collectAsStateWithLifecycle()
    var aba by rememberSaveable { mutableIntStateOf(0) }

    val pendentes = docs.orEmpty().filter { it.meuStatus == StatusSignatario.PENDENTE }
    val assinados = docs.orEmpty().filter { it.meuStatus == StatusSignatario.ASSINADO }
    val lista = if (aba == 0) pendentes else assinados

    Scaffold { padding ->
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(GradienteMarca, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .padding(top = padding.calculateTopPadding() + 8.dp, start = 20.dp, end = 8.dp, bottom = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Olá, ${vm.nome.substringBefore(" ")}", color = Color.White.copy(alpha = 0.85f))
                        Text("Documentos para você", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { vm.sair(); aoSair() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Sair", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (pendentes.isEmpty()) "Você está em dia: nenhuma assinatura pendente."
                    else "Você tem ${pendentes.size} documento(s) aguardando sua assinatura.",
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            TabRow(selectedTabIndex = aba, containerColor = Color.Transparent) {
                Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("Pendentes (${pendentes.size})") })
                Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("Assinados (${assinados.size})") })
            }
            if (docs != null && lista.isEmpty()) {
                EstadoVazio(
                    if (aba == 0) Icons.Default.Inbox else Icons.Default.TaskAlt,
                    if (aba == 0) "Nada pendente" else "Nenhum documento assinado",
                    if (aba == 0) "Quando alguém enviar um documento para você, ele aparece aqui."
                    else "Os documentos que você assinar ficam guardados aqui."
                )
            }
            LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = padding.calculateBottomPadding() + 16.dp)) {
                items(lista, key = { it.id }) { doc ->
                    CartaoDocumento(doc, onClick = { aoAbrir(doc) }) {
                        doc.meuStatus?.let { Etiqueta("Você: ${it.rotulo}", corDoStatus(it)) }
                    }
                }
            }
        }
    }
}
