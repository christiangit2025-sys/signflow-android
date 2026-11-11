package br.com.fiap.signflow.ui.novo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.fiap.signflow.core.Formatters
import br.com.fiap.signflow.domain.model.Papel
import br.com.fiap.signflow.domain.model.Usuario
import br.com.fiap.signflow.ui.components.Avatar
import br.com.fiap.signflow.ui.components.CampoEstatico
import br.com.fiap.signflow.ui.components.CartaoErro
import br.com.fiap.signflow.ui.components.EstadoPdf
import br.com.fiap.signflow.ui.components.PaginaPdf
import br.com.fiap.signflow.ui.components.Secao
import br.com.fiap.signflow.ui.components.posicaoDoCampo
import br.com.fiap.signflow.ui.theme.Verde
import br.com.fiap.signflow.ui.theme.corDoSignatario

@Composable
private fun ListaDeErros(erros: List<String>) {
    if (erros.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 12.dp)) {
        erros.forEach { CartaoErro(it) }
    }
}

// =====================================================================
// Etapa 1 — título e arquivo
// =====================================================================

@Composable
fun EtapaDados(s: NovoDocumentoState, pdf: EstadoPdf, vm: NovoDocumentoViewModel) {
    val seletor = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importarPdf(uri)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ListaDeErros(s.erros)
        Secao("Título do documento") {
            OutlinedTextField(
                value = s.titulo,
                onValueChange = vm::titulo,
                placeholder = { Text("Ex.: Contrato de prestação de serviços") },
                singleLine = true,
                supportingText = { Text("${s.titulo.length}/120") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))
        Secao("Arquivo PDF") {
            if (s.versao > 1) {
                Text(
                    "Nova versão: o PDF da versão anterior foi copiado. Você pode trocar o arquivo se precisar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !s.importando) { seletor.launch(arrayOf("application/pdf")) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (s.importando) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Icon(
                            if (s.caminhoPdf != null) Icons.Default.PictureAsPdf else Icons.Default.UploadFile,
                            null, tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        when {
                            s.importando -> Text("Validando arquivo…", style = MaterialTheme.typography.titleMedium)
                            s.caminhoPdf != null -> {
                                Text(s.nomeArquivo.orEmpty(), style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                Text(
                                    "${s.totalPaginas} página(s) · toque para trocar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {
                                Text("Selecionar PDF do celular", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Até 20 MB. Arquivos protegidos por senha não são aceitos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            s.erroArquivo?.let {
                Spacer(Modifier.height(10.dp))
                CartaoErro(it)
            }
        }

        if (s.caminhoPdf != null && pdf is EstadoPdf.Pronto) {
            Spacer(Modifier.height(16.dp))
            Secao("Pré-visualização") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PaginaPdf(pdf.renderer, 0, Modifier.widthIn(max = 220.dp))
                }
            }
        }
    }
}

// =====================================================================
// Etapa 2 — seleção e classificação dos signatários
// =====================================================================

@Composable
fun EtapaSignatarios(s: NovoDocumentoState, vm: NovoDocumentoViewModel, aoCadastrar: () -> Unit) {
    var substituindo by remember { mutableStateOf<Long?>(null) }

    substituindo?.let { antigoId ->
        AlertDialog(
            onDismissRequest = { substituindo = null },
            title = { Text("Substituir por…") },
            text = {
                if (s.naoSelecionados.isEmpty()) {
                    Text("Não há outros signatários disponíveis. Cadastre um novo na lista de signatários.")
                } else Column {
                    s.naoSelecionados.forEach { u ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                vm.substituirSignatario(antigoId, u)
                                substituindo = null
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(u.nome, MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(u.nome, style = MaterialTheme.typography.titleMedium)
                                Text(u.email, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { substituindo = null }) { Text("Fechar") } }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ListaDeErros(s.erros)
        Secao("Quem vai assinar (${s.selecionados.size})") {
            if (s.selecionados.isEmpty()) {
                Text(
                    "Adicione pelo menos um signatário da lista abaixo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            s.selecionados.forEachIndexed { i, sel ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, corDoSignatario(i).copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(sel.usuario.nome, corDoSignatario(i))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${i + 1}. ${sel.usuario.nome}", style = MaterialTheme.typography.titleMedium)
                                Text(sel.usuario.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { vm.moverSignatario(sel.usuario.id, true) }, enabled = i > 0) {
                                Icon(Icons.Default.ArrowUpward, "Subir")
                            }
                            IconButton(onClick = { vm.moverSignatario(sel.usuario.id, false) }, enabled = i < s.selecionados.lastIndex) {
                                Icon(Icons.Default.ArrowDownward, "Descer")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SeletorPapel(sel.papel) { vm.definirPapel(sel.usuario.id, it) }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { substituindo = sel.usuario.id }) {
                                Icon(Icons.Default.SwapHoriz, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Substituir")
                            }
                            TextButton(onClick = { vm.removerSignatario(sel.usuario.id) }) {
                                Icon(Icons.Default.Close, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(4.dp))
                                Text("Remover", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Secao("Signatários cadastrados") {
            if (s.disponiveis.isEmpty()) {
                Text("Nenhum signatário cadastrado ainda.", style = MaterialTheme.typography.bodyMedium)
            }
            s.naoSelecionados.forEach { u -> LinhaDisponivel(u) { vm.adicionarSignatario(u) } }
            if (s.disponiveis.isNotEmpty() && s.naoSelecionados.isEmpty()) {
                Text(
                    "Todos os signatários cadastrados já foram adicionados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = aoCadastrar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PersonAdd, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cadastrar novo signatário")
            }
        }
    }
}

@Composable
private fun LinhaDisponivel(u: Usuario, aoAdicionar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = aoAdicionar).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(u.nome, MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(u.nome, style = MaterialTheme.typography.bodyLarge)
            Text("${u.email} · ${Formatters.telefone(u.telefone)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(onClick = aoAdicionar) {
            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Adicionar")
        }
    }
}

@Composable
private fun SeletorPapel(papel: Papel, aoEscolher: (Papel) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = true,
            onClick = { aberto = true },
            label = { Text(papel.rotulo) },
            trailingIcon = { Text("▾") }
        )
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            Papel.entries.forEach { p ->
                DropdownMenuItem(text = { Text(p.rotulo) }, onClick = { aoEscolher(p); aberto = false })
            }
        }
    }
}

// =====================================================================
// Etapa 3 — posicionamento dos campos de assinatura
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EtapaCampos(s: NovoDocumentoState, pdf: EstadoPdf, vm: NovoDocumentoViewModel) {
    val indices = s.indiceDoSignatario
    val nomes = s.selecionados.associate { it.usuario.id to it.usuario.nome }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ListaDeErros(s.erros)

        Text("Para quem é o campo?", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            s.selecionados.forEachIndexed { i, sel ->
                val cor = corDoSignatario(i)
                val qtd = s.camposDe(sel.usuario.id).size
                FilterChip(
                    selected = s.signatarioAtivoId == sel.usuario.id,
                    onClick = { vm.ativarSignatario(sel.usuario.id) },
                    label = { Text("${sel.usuario.nome.substringBefore(" ")} ($qtd)") },
                    leadingIcon = {
                        if (qtd == 0) Icon(Icons.Default.Warning, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        else Box(Modifier.size(12.dp).background(cor, CircleShape))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cor.copy(alpha = 0.18f),
                        selectedLabelColor = cor
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.irParaPagina(s.paginaAtual - 1) }, enabled = s.paginaAtual > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Página anterior")
            }
            Text(
                "Página ${s.paginaAtual + 1} de ${s.totalPaginas}",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { vm.irParaPagina(s.paginaAtual + 1) }, enabled = s.paginaAtual < s.totalPaginas - 1) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Próxima página")
            }
        }

        when (pdf) {
            is EstadoPdf.Carregando -> Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is EstadoPdf.Erro -> CartaoErro(pdf.mensagem)
            is EstadoPdf.Pronto -> {
                val proporcao = pdf.renderer.proporcoes.getOrElse(s.paginaAtual) { 0.707f }
                PaginaPdf(
                    pdf.renderer, s.paginaAtual,
                    Modifier.pointerInput(Unit) { detectTapGestures { vm.selecionarCampo(null) } }
                ) {
                    val larguraPx = constraints.maxWidth.toFloat()
                    val alturaPx = constraints.maxHeight.toFloat()
                    s.campos.filter { it.pagina == s.paginaAtual }.forEach { campo -> key(campo.id) {
                        val cor = corDoSignatario(indices[campo.usuarioId] ?: 0)
                        val selecionado = s.campoSelecionadoId == campo.id
                        val vmAtual = vm
                        Box(
                            Modifier
                                .posicaoDoCampo(this, campo)
                                .background(cor.copy(alpha = if (selecionado) 0.32f else 0.18f), RoundedCornerShape(4.dp))
                                .border(if (selecionado) 2.5.dp else 1.5.dp, cor, RoundedCornerShape(4.dp))
                                .pointerInput(campo.id, larguraPx, alturaPx) {
                                    detectDragGestures(onDragStart = { vmAtual.selecionarCampo(campo.id) }) { change, arraste ->
                                        change.consume()
                                        vmAtual.moverCampo(campo.id, arraste.x / larguraPx, arraste.y / alturaPx)
                                    }
                                }
                                .pointerInput(campo.id) { detectTapGestures { vmAtual.selecionarCampo(campo.id) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
                                Icon(Icons.Default.Draw, null, tint = cor, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    nomes[campo.usuarioId]?.substringBefore(" ") ?: "?",
                                    color = cor, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1
                                )
                            }
                        }
                    } }
                }

                Spacer(Modifier.height(10.dp))
                val selecionado = s.campos.firstOrNull { it.id == s.campoSelecionadoId }
                if (selecionado != null) {
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Campo de ${nomes[selecionado.usuarioId]?.substringBefore(" ")}",
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                        IconButton(onClick = { vm.redimensionarCampo(selecionado.id, 0.85f) }) { Icon(Icons.Default.ZoomOut, "Diminuir") }
                        IconButton(onClick = { vm.redimensionarCampo(selecionado.id, 1.15f) }) { Icon(Icons.Default.ZoomIn, "Aumentar") }
                        IconButton(onClick = { vm.excluirCampo(selecionado.id) }) {
                            Icon(Icons.Default.Delete, "Excluir campo", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    val ativo = s.selecionados.firstOrNull { it.usuario.id == s.signatarioAtivoId }
                    Button(
                        onClick = { vm.adicionarCampo(proporcao) },
                        enabled = ativo != null,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Adicionar campo para ${ativo?.usuario?.nome?.substringBefore(" ") ?: "…"} nesta página")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Arraste o campo até a posição desejada. Toque nele para redimensionar ou excluir. " +
                            "A posição é salva em relação à página, então aparece igual em qualquer tela.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// =====================================================================
// Etapa 4 — revisão
// =====================================================================

@Composable
fun EtapaRevisao(s: NovoDocumentoState, pdf: EstadoPdf, vm: NovoDocumentoViewModel) {
    val indices = s.indiceDoSignatario
    val nomes = s.selecionados.associate { it.usuario.id to it.usuario.nome }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ListaDeErros(s.erros)
        (s.resultado as? ResultadoEnvio.Falha)?.let {
            CartaoErro(it.mensagem)
            Spacer(Modifier.height(12.dp))
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                Text(s.titulo, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${s.nomeArquivo} · ${s.totalPaginas} página(s) · versão ${s.versao}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Protocolo ${Formatters.protocoloCurto(s.protocolo)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Secao("Signatários e campos") {
            s.selecionados.forEachIndexed { i, sel ->
                val campos = s.camposDe(sel.usuario.id)
                val paginas = campos.map { it.pagina + 1 }.distinct().sorted().joinToString(", ")
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(sel.usuario.nome, corDoSignatario(i))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${i + 1}. ${sel.usuario.nome}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${sel.papel.rotulo} · ${campos.size} campo(s) · pág. $paginas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            TextButton(onClick = { vm.irPara(Etapa.SIGNATARIOS) }) { Text("Alterar signatários") }
        }

        Spacer(Modifier.height(8.dp))
        Secao("Mensagem do convite") {
            OutlinedTextField(
                value = s.mensagem,
                onValueChange = vm::mensagem,
                minLines = 3,
                supportingText = { Text("${s.mensagem.length}/500") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))
        Secao("Documento final") {
            if (pdf is EstadoPdf.Pronto) {
                (0 until s.totalPaginas).forEach { p ->
                    Text("Página ${p + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    PaginaPdf(pdf.renderer, p) {
                        s.campos.filter { it.pagina == p }.forEach { campo ->
                            CampoEstatico(
                                campo,
                                nomes[campo.usuarioId]?.substringBefore(" ") ?: "?",
                                corDoSignatario(indices[campo.usuarioId] ?: 0)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                TextButton(onClick = { vm.irPara(Etapa.CAMPOS) }) { Text("Ajustar campos") }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Simular instabilidade de rede", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Modo demonstração: a confirmação do envio se perde, para testar a nova tentativa sem duplicar solicitações.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = s.simularFalha, onCheckedChange = vm::simularFalha)
        }
        Spacer(Modifier.height(16.dp))
    }
}

// =====================================================================
// Resultado
// =====================================================================

@Composable
fun EtapaResultado(s: NovoDocumentoState, aoAcompanhar: (Long) -> Unit, aoInicio: () -> Unit) {
    val sucesso = s.resultado as? ResultadoEnvio.Sucesso ?: return
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(96.dp).background(Verde.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CheckCircle, null, tint = Verde, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Documento enviado!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "\"${s.titulo}\" foi enviado para ${s.selecionados.size} signatário(s). " +
                "Eles já podem ver o documento pendente no app.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text("Protocolo ${Formatters.protocoloCurto(s.protocolo)}", fontWeight = FontWeight.SemiBold)
        if (sucesso.aposNovaTentativa) {
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Verde.copy(alpha = 0.1f))) {
                Text(
                    "Envio confirmado na tentativa nº ${s.tentativas}. Nenhuma solicitação foi duplicada.",
                    modifier = Modifier.padding(12.dp),
                    color = Verde,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = { aoAcompanhar(sucesso.documentoId) }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text("Acompanhar documento")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = aoInicio, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text("Voltar ao início")
        }
    }
}
