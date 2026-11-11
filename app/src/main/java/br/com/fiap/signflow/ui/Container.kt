package br.com.fiap.signflow.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import br.com.fiap.signflow.SignFlowApp
import br.com.fiap.signflow.core.AppContainer

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as SignFlowApp).container
