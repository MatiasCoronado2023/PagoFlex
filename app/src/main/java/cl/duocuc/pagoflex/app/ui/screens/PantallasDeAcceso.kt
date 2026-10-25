package cl.duocuc.pagoflex.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.app.R
import cl.duocuc.pagoflex.app.ui.components.*
import cl.duocuc.pagoflex.app.ui.theme.Primario
import cl.duocuc.pagoflex.app.ui.theme.Medidas
import kotlinx.coroutines.launch

private val CUENTAS_PRUEBA = listOf("crojas@correo.ejemplo" to "usuario", "alagos@pagoflex.ejemplo" to "administrador")

@Composable
fun PantallaIniciarSesion(
    correoRecordado: String,
    onIngresar: suspend (correo: String, clave: String) -> String?,
    onIrARegistro: () -> Unit
) {
    var correo by rememberSaveable { mutableStateOf(correoRecordado) }
    var clave by rememberSaveable { mutableStateOf("") }
    var verClave by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()).imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .background(Primario).statusBarsPadding().padding(vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(painterResource(R.drawable.ic_logo), stringResource(R.string.cd_logo), Modifier.size(72.dp))
                Text("PagoFlex", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                Text("Revisa y paga tus cuentas", color = Color.White.copy(alpha = 0.9f))
            }
            Column(Modifier.padding(Medidas.espacioGrande), verticalArrangement = Arrangement.spacedBy(Medidas.espacioChico)) {
                Text("Inicia sesión", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                CampoTexto(correo, { correo = it; error = null }, "Correo", tipoTeclado = KeyboardType.Email, icono = R.drawable.ic_correo)
                CampoContrasena(clave, { clave = it; error = null }, "Contraseña", verClave, { verClave = !verClave }, error = error)
                BotonPrincipal("Ingresar", onClick = { scope.launch { error = onIngresar(correo, clave) } })
                TextButton(onClick = onIrARegistro, modifier = Modifier.fillMaxWidth()) { Text("Crear una cuenta", fontWeight = FontWeight.Medium) }
                Tarjeta {
                    Text("Cuentas de prueba (contraseña pago123)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text("Toca una cuenta para completar los datos.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CUENTAS_PRUEBA.forEach { (cuenta, rol) ->
                        Text(
                            "$cuenta · $rol", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth().clickable { correo = cuenta; clave = "pago123"; error = null }.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PantallaCrearCuenta(
    onRegistrar: suspend (nombre: String, rut: String, correo: String, telefono: String,
                          clave: String, clave2: String, acepta: Boolean) -> Map<String, String>,
    onCuentaCreada: () -> Unit,
    onVolver: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var rut by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var clave2 by rememberSaveable { mutableStateOf("") }
    var verClave by rememberSaveable { mutableStateOf(false) }
    var acepta by rememberSaveable { mutableStateOf(false) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var creada by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (creada) {
        DialogoAviso("Cuenta creada", "Ya puedes revisar y pagar tus cuentas en PagoFlex.", "Continuar", onCuentaCreada)
    }

    Scaffold(topBar = { BarraTitulo("Crear cuenta", onVolver) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(Medidas.espacioMedio)
        ) {
            CampoTexto(nombre, { nombre = it }, "Nombre completo", error = errores["nombre"], icono = R.drawable.ic_perfil)
            CampoRut(rut, { rut = it }, "RUT", errores["rut"])
            CampoTexto(correo, { correo = it }, "Correo", error = errores["correo"], tipoTeclado = KeyboardType.Email, icono = R.drawable.ic_correo)
            CampoTexto(telefono, { telefono = it }, "Teléfono (opcional)", error = errores["telefono"], tipoTeclado = KeyboardType.Phone, icono = R.drawable.ic_telefono)
            CampoContrasena(clave, { clave = it }, "Contraseña (mínimo 6)", verClave, { verClave = !verClave }, error = errores["clave"])
            CampoContrasena(clave2, { clave2 = it }, "Repite la contraseña", verClave, { verClave = !verClave }, error = errores["clave2"])

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = acepta, onCheckedChange = { acepta = it })
                Text("Acepto el uso de mis datos solo para gestionar mis pagos (Ley 19.628).")
            }
            TextoError(errores["datos"])
            TextoError(errores["general"])

            Spacer(Modifier.height(Medidas.espacioMedio))
            BotonPrincipal("Crear cuenta", onClick = {
                scope.launch {
                    errores = onRegistrar(nombre, rut, correo, telefono, clave, clave2, acepta)
                    if (errores.isEmpty()) creada = true
                }
            })
            Text(
                "Si una empresa ya te registró con tu RUT, aquí activas esa misma cuenta.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Medidas.espacioChico)
            )
        }
    }
}
