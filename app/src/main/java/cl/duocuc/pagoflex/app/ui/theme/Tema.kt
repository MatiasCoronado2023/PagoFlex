package cl.duocuc.pagoflex.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Primario = Color(0xFFB84A06)
val Tinta = Color(0xFF1A1A1A)
val Fondo = Color(0xFFF5F5F3)
val LineaSuave = Color(0xFFEEEDE9)
val Vencido = Color(0xFFB3261E)
val Pagado = Color(0xFF2E7D32)
val Gris = Color(0xFF66655F)

private val COLORES_INICIALES = listOf(
    Color(0xFFF1E9D8) to Color(0xFF6B4E16),
    Color(0xFFE1ECF8) to Color(0xFF1D4E89),
    Color(0xFFF6E3EA) to Color(0xFF8A2E52),
    Color(0xFFDDF1EC) to Color(0xFF0F5A4A),
    Color(0xFFECE8F8) to Color(0xFF4B3A8C),
    Color(0xFFE4E9EE) to Color(0xFF3C4C5C),
    Color(0xFFECEFD9) to Color(0xFF545A1A),
    Color(0xFFE5F0DA) to Color(0xFF3B6216)
)

fun coloresDe(nombre: String): Pair<Color, Color> = COLORES_INICIALES[Math.floorMod(nombre.hashCode(), COLORES_INICIALES.size)]

private val Esquema = lightColorScheme(
    primary = Primario, onPrimary = Color.White,
    primaryContainer = Color(0xFFFBE9DE), onPrimaryContainer = Color(0xFF5A2200),
    secondary = Tinta, onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEEDE9), onSecondaryContainer = Tinta,
    background = Fondo, onBackground = Tinta,
    surface = Color.White, onSurface = Tinta,
    surfaceVariant = LineaSuave, onSurfaceVariant = Gris,
    outline = Color(0xFF8C8B85), outlineVariant = Color(0xFFE6E5E1),
    error = Vencido, onError = Color.White,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFAFAF9),
    surfaceContainer = Color.White, surfaceContainerHigh = Color.White, surfaceContainerHighest = LineaSuave
)

object Medidas {
    val espacioChico = 8.dp
    val espacioMedio = 16.dp
    val espacioGrande = 24.dp
    val alturaBoton = 50.dp
    val radio = 16.dp
}

@Composable
fun TemaPagoFlex(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Esquema,
        shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp)),
        content = content
    )
}
