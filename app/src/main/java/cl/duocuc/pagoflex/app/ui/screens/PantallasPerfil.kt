package cl.duocuc.pagoflex.app.ui.screens

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duocuc.pagoflex.app.R
import cl.duocuc.pagoflex.app.model.ReglasDeNegocio
import cl.duocuc.pagoflex.app.model.Usuario
import cl.duocuc.pagoflex.app.ui.components.*
import cl.duocuc.pagoflex.app.ui.theme.Medidas
import cl.duocuc.pagoflex.app.ui.theme.Pagado
import kotlinx.coroutines.launch

@Composable
private fun GrupoTitulo(texto: String) {
    Text(
        texto.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun FilaDatoPerfil(icono: Int, etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icono(icono, tamano = 20.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.padding(start = 12.dp)) {
            TextoAyuda(etiqueta, Modifier)
            Text(valor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FilaAccion(icono: Int, titulo: String, color: Color, detalle: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icono(icono, tamano = 20.dp, tint = color)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(titulo, fontWeight = FontWeight.SemiBold, color = color)
            if (detalle != null) TextoAyuda(detalle, Modifier)
        }
        if (detalle != null) Icono(R.drawable.ic_flecha, tamano = 18.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPerfil(
    usuario: Usuario,
    foto: ImageBitmap?,
    menu: @Composable () -> Unit,
    onTomarFoto: (Bitmap) -> Unit,
    onElegirFoto: (Uri) -> Unit,
    onQuitarFoto: () -> Unit,
    onCambiarClave: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var hojaFoto by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    val estadoHoja = rememberModalBottomSheetState()
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { foto -> if (foto != null) onTomarFoto(foto) }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onElegirFoto(uri) }

    if (confirmarSalida) DialogoCerrarSesion(onSi = { confirmarSalida = false; onCerrarSesion() }, onNo = { confirmarSalida = false })
    aviso?.let { DialogoAviso("No se pudo abrir la cámara", it, "Entendido") { aviso = null } }

    Scaffold(topBar = { BarraTitulo("Mi perfil", menu = menu) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Medidas.espacioMedio),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(Modifier.padding(top = 8.dp).clickable { hojaFoto = true }) {
                Avatar(usuario.nombre, foto, 96.dp)
                Surface(
                    shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.background),
                    modifier = Modifier.size(34.dp).align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icono(R.drawable.ic_camara, stringResource(R.string.cd_cambiar_foto), tamano = 17.dp, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Text(usuario.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            TextoAyuda("${if (usuario.esAdministrador) "Administrador" else "Usuario"} · RUT ${ReglasDeNegocio.ocultarRut(usuario.rut)}", Modifier)
            TextButton(onClick = { hojaFoto = true }) { Text("Cambiar foto de perfil", fontWeight = FontWeight.SemiBold) }

            Tarjeta {
                GrupoTitulo("Mis datos")
                FilaDatoPerfil(R.drawable.ic_correo, "Correo", usuario.correo)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FilaDatoPerfil(R.drawable.ic_telefono, "Teléfono", usuario.telefono ?: "No registrado")
            }
            Spacer(Modifier.height(8.dp))
            Tarjeta {
                GrupoTitulo("Seguridad")
                FilaAccion(R.drawable.ic_candado, "Cambiar contraseña", MaterialTheme.colorScheme.primary, "Mínimo 6 caracteres", onCambiarClave)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FilaAccion(R.drawable.ic_salir, "Cerrar sesión", MaterialTheme.colorScheme.error) { confirmarSalida = true }
            }
            Spacer(Modifier.height(Medidas.espacioMedio))
        }
    }

    if (hojaFoto) {
        ModalBottomSheet(onDismissRequest = { hojaFoto = false }, sheetState = estadoHoja) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text("Foto de perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                FilaAccion(R.drawable.ic_camara, "Tomar una foto", MaterialTheme.colorScheme.primary, onClick = {
                    hojaFoto = false
                    try {
                        camara.launch(null)
                    } catch (e: ActivityNotFoundException) {
                        aviso = "Este teléfono no tiene una app de cámara disponible. Elige una foto de la galería."
                    }
                })
                FilaAccion(R.drawable.ic_galeria, "Elegir de la galería", MaterialTheme.colorScheme.primary, onClick = {
                    hojaFoto = false
                    galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                })
                if (foto != null) FilaAccion(R.drawable.ic_basura, "Quitar foto", MaterialTheme.colorScheme.error) { hojaFoto = false; onQuitarFoto() }
                TextButton(
                    onClick = { hojaFoto = false },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                ) { Text("Cancelar", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) }
            }
        }
    }
}

@Composable
private fun Requisito(texto: String, cumplido: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icono(R.drawable.ic_check, tamano = 16.dp, tint = if (cumplido) Pagado else MaterialTheme.colorScheme.outline)
        Text(
            texto, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp),
            color = if (cumplido) Pagado else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (cumplido) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun PantallaCambiarClave(
    onGuardar: suspend (actual: String, nueva: String, repetida: String) -> Map<String, String>,
    onListo: () -> Unit,
    onVolver: () -> Unit
) {
    var actual by rememberSaveable { mutableStateOf("") }
    var nueva by rememberSaveable { mutableStateOf("") }
    var repetida by rememberSaveable { mutableStateOf("") }
    var ver by rememberSaveable { mutableStateOf(false) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var guardada by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (guardada) DialogoAviso("Contraseña actualizada", "La próxima vez que inicies sesión usa tu nueva contraseña.", "Entendido", onListo)

    Scaffold(topBar = { BarraTitulo("Cambiar contraseña", onVolver) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(Medidas.espacioMedio),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextoAyuda("Por tu seguridad, primero escribe tu contraseña actual.", Modifier)
            CampoContrasena(actual, { actual = it; errores = errores - "actual" }, "Contraseña actual", ver, { ver = !ver }, error = errores["actual"])
            CampoContrasena(nueva, { nueva = it; errores = errores - "nueva" }, "Nueva contraseña", ver, { ver = !ver }, error = errores["nueva"])
            CampoContrasena(repetida, { repetida = it; errores = errores - "repetida" }, "Repite la nueva contraseña", ver, { ver = !ver }, error = errores["repetida"])
            Tarjeta {
                Requisito("Mínimo 6 caracteres", nueva.length >= 6)
                Requisito("Distinta a la actual", nueva.isNotEmpty() && nueva != actual)
                Requisito("Ambas contraseñas coinciden", repetida.isNotEmpty() && repetida == nueva)
            }
            TextoError(errores["general"])
            Spacer(Modifier.height(Medidas.espacioMedio))
            BotonPrincipal("Guardar contraseña") {
                scope.launch {
                    errores = onGuardar(actual, nueva, repetida)
                    if (errores.isEmpty()) guardada = true
                }
            }
        }
    }
}
