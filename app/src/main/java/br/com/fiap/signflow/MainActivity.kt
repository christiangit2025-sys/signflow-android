package br.com.fiap.signflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.fiap.signflow.ui.navigation.AppNavHost
import br.com.fiap.signflow.ui.theme.SignFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SignFlowTheme {
                AppNavHost()
            }
        }
    }
}
