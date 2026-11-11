package br.com.fiap.signflow.ui.signatarios

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.signflow.core.Formatters
import br.com.fiap.signflow.ui.appContainer
import br.com.fiap.signflow.ui.components.Avatar
import br.com.fiap.signflow.ui.components.BarraSuperior
import br.com.fiap.signflow.ui.components.CartaoErro
import br.com.fiap.signflow.ui.components.EstadoVazio
import br.com.fiap.signflow.ui.theme.corDoSignatario

@Composable
fun SignatariosScreen(aoVoltar: () -> Unit, aoNovo: () -> Unit, aoEditar: (Long) -> Unit) {
    val container = appContainer()
    val vm: SignatariosViewModel = viewModel { SignatariosViewModel(container.signatarioRepository, container.session) }
    val lista by vm.signatarios.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { BarraSuperior("Signatários", "Quem pode assinar seus documentos", aoVoltar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = aoNovo, icon = { Icon(Icons.Default.PersonAdd, null) }, text = { Text("Cadastrar") })
        }
    ) { padding ->
        val itens = lista
        when {
            itens == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            itens.isEmpty() -> EstadoVazio(
                Icons.Default.GroupAdd, "Nenhum signatário",
                "Cadastre as pessoas que vão assinar seus documentos.",
                Modifier.padding(padding)
            )
            else -> LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 96.dp)) {
                itemsIndexed(itens, key = { _, u -> u.id }) { i, u ->
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clickable { aoEditar(u.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(u.nome, corDoSignatario(i))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(u.nome, style = MaterialTheme.typography.titleMedium)
                                Text(u.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Formatters.telefone(u.telefone), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SignatarioFormScreen(id: Long?, aoVoltar: () -> Unit) {
    val container = appContainer()
    val vm: SignatarioFormViewModel = viewModel(key = "form-$id") {
        SignatarioFormViewModel(container.signatarioRepository, container.session, id)
    }
    val s by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(s.salvo) { if (s.salvo) aoVoltar() }

    Scaffold(topBar = { BarraSuperior(if (s.editando) "Editar signatário" else "Novo signatário", aoVoltar = aoVoltar) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Campo(s.nome, vm::nome, "Nome completo", Icons.Default.Person, s.erroNome)
            Campo(s.email, vm::email, "E-mail (login)", Icons.Default.Email, s.erroEmail, KeyboardType.Email)
            Campo(s.telefone, vm::telefone, "Telefone", Icons.Default.Phone, s.erroTelefone, KeyboardType.Phone)
            Campo(
                s.senha, vm::senha,
                if (s.editando) "Nova senha (deixe em branco para manter)" else "Senha",
                Icons.Default.Lock, s.erroSenha, KeyboardType.Password, senha = true
            )
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "O e-mail e a senha serão as credenciais de acesso do signatário. A senha é armazenada criptografada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            s.erroGeral?.let { Spacer(Modifier.height(8.dp)); CartaoErro(it) }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = vm::salvar,
                enabled = !s.salvando,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (s.salvando) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(if (s.editando) "Salvar alterações" else "Cadastrar signatário")
            }
        }
    }
}

@Composable
private fun Campo(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    icone: ImageVector,
    erro: String?,
    teclado: KeyboardType = KeyboardType.Text,
    senha: Boolean = false
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        leadingIcon = { Icon(icone, null) },
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (senha) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
    )
}
