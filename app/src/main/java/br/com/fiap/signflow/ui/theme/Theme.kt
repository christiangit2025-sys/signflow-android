package br.com.fiap.signflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Identidade inspirada na LetsSign: roxo/índigo com destaque ciano
val Roxo = Color(0xFF5B2BD1)
val RoxoEscuro = Color(0xFF1B1464)
val RoxoClaro = Color(0xFFEDE7FD)
val Ciano = Color(0xFF22D3EE)
val Fundo = Color(0xFFF6F4FD)
val Verde = Color(0xFF16A34A)
val Ambar = Color(0xFFD97706)
val Azul = Color(0xFF2563EB)

val GradienteMarca = Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF3B1FA8), RoxoEscuro))

/** Cores para diferenciar os campos de cada signatário no posicionamento. */
val CoresSignatarios = listOf(
    Color(0xFF7C3AED), Color(0xFF0891B2), Color(0xFFDB2777),
    Color(0xFF059669), Color(0xFFEA580C), Color(0xFF4F46E5)
)

fun corDoSignatario(indice: Int) = CoresSignatarios[indice.mod(CoresSignatarios.size)]

private val Esquema = lightColorScheme(
    primary = Roxo,
    onPrimary = Color.White,
    primaryContainer = RoxoClaro,
    onPrimaryContainer = RoxoEscuro,
    secondary = Color(0xFF0E7490),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF7FD),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = Ciano,
    background = Fundo,
    surface = Color.White,
    surfaceVariant = Color(0xFFF0ECFA),
    onSurfaceVariant = Color(0xFF55506B),
    outline = Color(0xFFC9C2E0),
    error = Color(0xFFDC2626)
)

private val Tipografia = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
)

@Composable
fun SignFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, typography = Tipografia, content = content)
}
