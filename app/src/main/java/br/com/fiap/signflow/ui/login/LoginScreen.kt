package br.com.fiap.signflow.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.fiap.signflow.domain.model.Usuario
import br.com.fiap.signflow.ui.appContainer
import br.com.fiap.signflow.ui.components.CartaoErro
import br.com.fiap.signflow.ui.theme.Ciano
import br.com.fiap.signflow.ui.theme.GradienteMarca

@Composable
fun LoginScreen(aoEntrar: (Usuario) -> Unit) {
    val container = appContainer()
    val vm: LoginViewModel = viewModel { LoginViewModel(container.authRepository, container.session) }
    val state by vm.state.collectAsStateWithLifecycle()
    var senhaVisivel by remember { mutableStateOf(false) }

    LaunchedEffect(state.logado) {
        state.logado?.let {
            aoEntrar(it)
            vm.navegacaoConcluida()
        }
    }

    Box(Modifier.fillMaxSize().background(GradienteMarca)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("SignFlow", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                Text(" ✓", color = Ciano, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("by LetsSign · é simples assinar", color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(8.dp))
            Text(
                "Prepare, envie e acompanhe documentos para assinatura direto do celular.",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(28.dp))

            Card(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Entrar", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Use o e-mail e a senha cadastrados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = vm::aoMudarEmail,
                        label = { Text("E-mail") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.senha,
                        onValueChange = vm::aoMudarSenha,
                        label = { Text("Senha") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                                Icon(
                                    if (senhaVisivel) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (senhaVisivel) "Ocultar senha" else "Mostrar senha"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { vm.entrar() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.erro?.let {
                        Spacer(Modifier.height(12.dp))
                        CartaoErro(it)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = vm::entrar,
                        enabled = !state.carregando,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (state.carregando) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Entrar", fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Acesso rápido para demonstração", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { vm.preencher("admin@letssign.com", "admin123") },
                    label = { Text("Admin", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null, tint = Color.White) }
                )
                AssistChip(
                    onClick = { vm.preencher("ana@email.com", "123456") },
                    label = { Text("Signatária (Ana)", color = Color.White) },
                    leadingIcon = { Icon(Icons.Default.Draw, null, tint = Color.White) }
                )
            }
        }
    }
}
