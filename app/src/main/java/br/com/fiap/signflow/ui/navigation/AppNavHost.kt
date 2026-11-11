package br.com.fiap.signflow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.fiap.signflow.domain.model.Perfil
import br.com.fiap.signflow.domain.model.StatusSignatario
import br.com.fiap.signflow.ui.admin.AdminHomeScreen
import br.com.fiap.signflow.ui.assinar.AssinarScreen
import br.com.fiap.signflow.ui.detalhe.DetalheScreen
import br.com.fiap.signflow.ui.login.LoginScreen
import br.com.fiap.signflow.ui.novo.NovoDocumentoScreen
import br.com.fiap.signflow.ui.signatario.CaixaEntradaScreen
import br.com.fiap.signflow.ui.signatarios.SignatarioFormScreen
import br.com.fiap.signflow.ui.signatarios.SignatariosScreen
import br.com.fiap.signflow.ui.visualizar.VisualizarScreen

private fun NavHostController.irParaLogin() = navigate(Rotas.LOGIN) {
    popUpTo(graph.id) { inclusive = true }
}

private fun NavHostController.voltar() {
    if (!popBackStack()) irParaLogin()
}

private val argId = navArgument("id") { type = NavType.LongType }

@Composable
fun AppNavHost(nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = Rotas.LOGIN) {

        composable(Rotas.LOGIN) {
            LoginScreen(aoEntrar = { usuario ->
                val destino = if (usuario.perfil == Perfil.ADMIN) Rotas.ADMIN_INICIO else Rotas.SIGNATARIO_INICIO
                nav.navigate(destino) { popUpTo(Rotas.LOGIN) { inclusive = true } }
            })
        }

        // ---------------- Admin ----------------

        composable(Rotas.ADMIN_INICIO) {
            AdminHomeScreen(
                aoNovoDocumento = { nav.navigate(Rotas.novoDocumento()) },
                aoAbrirDocumento = { nav.navigate(Rotas.detalhe(it)) },
                aoSignatarios = { nav.navigate(Rotas.SIGNATARIOS) },
                aoSair = { nav.irParaLogin() }
            )
        }

        composable(Rotas.SIGNATARIOS) {
            SignatariosScreen(
                aoVoltar = { nav.voltar() },
                aoNovo = { nav.navigate(Rotas.signatarioForm()) },
                aoEditar = { nav.navigate(Rotas.signatarioForm(it)) }
            )
        }

        composable(
            Rotas.SIGNATARIO_FORM,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) { entry ->
            val id = entry.arguments?.getLong("id")?.takeIf { it > 0 }
            SignatarioFormScreen(id = id, aoVoltar = { nav.voltar() })
        }

        composable(
            Rotas.NOVO_DOCUMENTO,
            arguments = listOf(navArgument("origem") { type = NavType.LongType; defaultValue = -1L })
        ) { entry ->
            val origem = entry.arguments?.getLong("origem")?.takeIf { it > 0 }
            NovoDocumentoScreen(
                origemId = origem,
                aoSair = { nav.voltar() },
                aoCadastrarSignatario = { nav.navigate(Rotas.signatarioForm()) },
                aoAcompanhar = { id ->
                    nav.navigate(Rotas.detalhe(id)) {
                        popUpTo(Rotas.ADMIN_INICIO)
                    }
                }
            )
        }

        // ---------------- Compartilhadas ----------------

        composable(Rotas.DETALHE, arguments = listOf(argId)) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            DetalheScreen(
                documentoId = id,
                aoVoltar = { nav.voltar() },
                aoVisualizar = { final -> nav.navigate(Rotas.visualizar(id, final)) },
                aoNovaVersao = { nav.navigate(Rotas.novoDocumento(id)) },
                aoAbrirDocumento = { outro -> nav.navigate(Rotas.detalhe(outro)) }
            )
        }

        composable(
            Rotas.VISUALIZAR,
            arguments = listOf(argId, navArgument("final") { type = NavType.BoolType; defaultValue = false })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            val final = entry.arguments?.getBoolean("final") ?: false
            VisualizarScreen(documentoId = id, final = final, aoVoltar = { nav.voltar() })
        }

        // ---------------- Signatário ----------------

        composable(Rotas.SIGNATARIO_INICIO) {
            CaixaEntradaScreen(
                aoAbrir = { doc ->
                    if (doc.meuStatus == StatusSignatario.PENDENTE) nav.navigate(Rotas.assinar(doc.id))
                    else nav.navigate(Rotas.detalhe(doc.id))
                },
                aoSair = { nav.irParaLogin() }
            )
        }

        composable(Rotas.ASSINAR, arguments = listOf(argId)) { entry ->
            val id = entry.arguments?.getLong("id") ?: return@composable
            AssinarScreen(
                documentoId = id,
                aoVoltar = { nav.voltar() },
                aoAcompanhar = {
                    nav.navigate(Rotas.detalhe(id)) { popUpTo(Rotas.SIGNATARIO_INICIO) }
                }
            )
        }
    }
}
