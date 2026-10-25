package cl.duocuc.pagoflex.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.app.R
import cl.duocuc.pagoflex.app.model.EstadoCompromiso
import cl.duocuc.pagoflex.app.model.EstadoReporte
import cl.duocuc.pagoflex.app.model.ReglasDeNegocio
import cl.duocuc.pagoflex.app.ui.theme.*
import kotlinx.coroutines.launch

data class OpcionMenu(val texto: String, val icono: Int, val contador: Int = 0, val alElegir: () -> Unit)

@Composable
fun Icono(id: Int, descripcion: String? = null, tamano: Dp = 22.dp, tint: Color = LocalContentColor.current) {
    Icon(painterResource(id), contentDescription = descripcion, modifier = Modifier.size(tamano), tint = tint)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraTitulo(titulo: String, onVolver: (() -> Unit)? = null, menu: (@Composable () -> Unit)? = null) {
    TopAppBar(
        title = { Text(titulo, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            if (onVolver != null) IconButton(onClick = onVolver) { Icono(R.drawable.ic_volver, stringResource(R.string.cd_volver)) }
        },
        actions = {
            if (menu != null) Box(Modifier.padding(end = Medidas.espacioChico)) { menu() }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun Avatar(nombre: String, foto: ImageBitmap?, tamano: Dp = 42.dp) {
    val forma = Modifier.size(tamano).clip(CircleShape)
    if (foto != null) {
        Image(foto, stringResource(R.string.cd_foto_perfil), forma, contentScale = ContentScale.Crop)
    } else {
        Box(forma.background(Color(0xFFE3E2DE)), contentAlignment = Alignment.Center) {
            Text(ReglasDeNegocio.iniciales(nombre).take(1), color = Color(0xFF45443F), fontWeight = FontWeight.SemiBold, fontSize = (tamano.value * 0.4f).sp)
        }
    }
}

@Composable
fun Insignia(cantidad: Int, modifier: Modifier = Modifier) {
    Text(
        cantidad.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.background(MaterialTheme.colorScheme.primary, CircleShape).padding(horizontal = 7.dp, vertical = 1.dp)
    )
}

@Composable
fun BotonMenu(nombre: String, correo: String, foto: ImageBitmap?, opciones: List<OpcionMenu>, actual: String, onCerrarSesion: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    var abierto by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf(false) }
    if (confirmar) DialogoCerrarSesion(onSi = { confirmar = false; onCerrarSesion() }, onNo = { confirmar = false })
    Box {
        Surface(
            onClick = { abierto = true }, shape = RoundedCornerShape(12.dp), color = if (abierto) colores.primary else colores.surface,
            border = BorderStroke(1.dp, if (abierto) colores.primary else colores.outlineVariant), modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icono(R.drawable.ic_menu, stringResource(R.string.cd_menu), tint = if (abierto) colores.onPrimary else colores.onSurface)
            }
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }, shape = RoundedCornerShape(16.dp), containerColor = colores.surface) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(nombre, foto, 40.dp)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(nombre, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(correo, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant, maxLines = 1)
                }
            }
            HorizontalDivider()
            opciones.forEach { opcion ->
                val esActual = opcion.texto == actual
                val color = if (esActual) colores.primary else colores.onSurface
                DropdownMenuItem(
                    text = { Text(opcion.texto, color = color, fontWeight = if (esActual) FontWeight.SemiBold else FontWeight.Normal) },
                    leadingIcon = { Icono(opcion.icono, tint = color) },
                    trailingIcon = { if (opcion.contador > 0) Insignia(opcion.contador) },
                    onClick = { abierto = false; opcion.alElegir() }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Cerrar sesión", color = colores.error, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icono(R.drawable.ic_salir, tint = colores.error) },
                onClick = { abierto = false; confirmar = true }
            )
        }
    }
}

@Composable
fun BarraSuperior(nombre: String, saludo: String, titulo: String, foto: ImageBitmap?, menu: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = Medidas.espacioChico),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(nombre, foto, 40.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(saludo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        menu()
    }
}

@Composable
fun Tarjeta(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, contenido: @Composable ColumnScope.() -> Unit) {
    val forma = RoundedCornerShape(Medidas.radio)
    Column(
        modifier.fillMaxWidth().clip(forma).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = contenido
    )
}

@Composable
fun TarjetaDestacada(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Primario).padding(20.dp), content = contenido)
}

@Composable
fun TextoAyuda(texto: String, modifier: Modifier = Modifier.padding(horizontal = Medidas.espacioMedio, vertical = 4.dp)) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Composable
fun resaltado(texto: String, buscado: String): AnnotatedString {
    val colores = MaterialTheme.colorScheme
    val q = buscado.trim()
    val inicio = if (q.isEmpty()) -1 else texto.indexOf(q, ignoreCase = true)
    return buildAnnotatedString {
        append(texto)
        if (inicio >= 0) addStyle(SpanStyle(background = colores.primaryContainer, color = colores.primary, fontWeight = FontWeight.SemiBold), inicio, inicio + q.length)
    }
}

@Composable
fun FilaTarjeta(
    titulo: String,
    lineas: List<String>,
    iniciales: String? = null,
    monto: String? = null,
    buscado: String = "",
    primera: Boolean = true,
    ultima: Boolean = true,
    onClick: (() -> Unit)? = null,
    debajo: @Composable () -> Unit = {},
    etiquetas: @Composable () -> Unit = {}
) {
    val radioArriba = if (primera) Medidas.radio else 0.dp
    val radioAbajo = if (ultima) Medidas.radio else 0.dp
    Column(
        Modifier.padding(horizontal = Medidas.espacioMedio).fillMaxWidth()
            .clip(RoundedCornerShape(radioArriba, radioArriba, radioAbajo, radioAbajo))
            .background(MaterialTheme.colorScheme.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (!primera) HorizontalDivider(Modifier.padding(horizontal = Medidas.espacioMedio), color = LineaSuave)
        Row(Modifier.padding(horizontal = Medidas.espacioMedio, vertical = 14.dp)) {
            if (iniciales != null) CuadroIniciales(iniciales, nombre = titulo)
            Column(Modifier.weight(1f).padding(horizontal = if (iniciales != null) 12.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(resaltado(titulo, buscado), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                lineas.forEach {
                    Text(resaltado(it, buscado), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                debajo()
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (monto != null) Text(monto, fontWeight = FontWeight.SemiBold)
                etiquetas()
            }
        }
    }
}

@Composable
fun CuadroIniciales(iniciales: String, tamano: Dp = 40.dp, nombre: String = iniciales) {
    val (fondo, texto) = coloresDe(nombre)
    Box(Modifier.size(tamano).background(fondo, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Text(iniciales, color = texto, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun BarraCuotas(numero: Int, total: Int, color: Color) {
    var llena by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { llena = true }
    val avance by animateFloatAsState(if (llena) numero.toFloat() / total else 0f, tween(900), label = "barraCuotas")
    Box(Modifier.padding(top = 4.dp, bottom = 2.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(LineaSuave)) {
        Box(Modifier.fillMaxWidth(avance).height(4.dp).background(color))
    }
}

@Composable
fun TextoError(texto: String?) {
    AnimatedVisibility(visible = texto != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Text(texto ?: "", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun CheckAnimado(tamano: Dp = 56.dp) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val escala by animateFloatAsState(
        if (visible) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "check"
    )
    Box(Modifier.size(tamano).scale(escala).background(Pagado, CircleShape), contentAlignment = Alignment.Center) {
        Icono(R.drawable.ic_check, tint = Color.White, tamano = tamano * 0.55f)
    }
}

@Composable
fun EncabezadoGrupo(texto: String, derecha: String? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = Medidas.espacioChico)) {
        Text(texto, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (derecha != null) Text(derecha, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FiltroMenu(textoTodos: String, opciones: List<Pair<String, String>>, seleccionado: String?, onElegir: (String?) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val textoActual = opciones.firstOrNull { it.first == seleccionado }?.second ?: textoTodos
    Box {
        OutlinedButton(
            onClick = { abierto = true }, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = Tinta),
            modifier = Modifier.height(40.dp)
        ) {
            Text(textoActual, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(" ▾", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }, containerColor = MaterialTheme.colorScheme.surface) {
            DropdownMenuItem(text = { Text(textoTodos) }, onClick = { abierto = false; onElegir(null) })
            opciones.forEach { (valor, texto) ->
                DropdownMenuItem(
                    text = { Text(texto, fontWeight = if (valor == seleccionado) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = { abierto = false; onElegir(valor) }
                )
            }
        }
    }
}

@Composable
fun FiltroChips(textoTodos: String, opciones: List<Pair<String, String>>, seleccionado: String?, onElegir: (String?) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Medidas.espacioMedio),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(selected = seleccionado == null, onClick = { onElegir(null) }, label = { Text(textoTodos) })
        opciones.forEach { (valor, texto) -> FilterChip(selected = valor == seleccionado, onClick = { onElegir(valor) }, label = { Text(texto) }) }
    }
}

@Composable
fun CampoTexto(
    valor: String,
    onValorCambia: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    unaLinea: Boolean = true,
    oculto: Boolean = false,
    icono: Int? = null,
    iconoDerecha: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = valor, onValueChange = onValorCambia, modifier = modifier.fillMaxWidth(), label = { Text(etiqueta) },
        leadingIcon = if (icono != null) { { Icono(icono, tamano = 20.dp) } } else null,
        trailingIcon = when {
            iconoDerecha != null -> iconoDerecha
            error != null -> { { IconoError() } }
            else -> null
        },
        visualTransformation = if (oculto) PasswordVisualTransformation() else VisualTransformation.None,
        isError = error != null, supportingText = { TextoError(error) }, singleLine = unaLinea,
        shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado)
    )
}

@Composable
fun IconoError() {
    Icono(R.drawable.ic_error, "Dato no válido", tamano = 20.dp, tint = MaterialTheme.colorScheme.error)
}

@Composable
fun CampoContrasena(valor: String, onValorCambia: (String) -> Unit, etiqueta: String, visible: Boolean, onAlternarVisible: () -> Unit, error: String? = null) {
    CampoTexto(valor, onValorCambia, etiqueta, error = error, tipoTeclado = KeyboardType.Password, oculto = !visible, icono = R.drawable.ic_candado) {
        IconButton(onClick = onAlternarVisible) {
            Icono(
                if (visible) R.drawable.ic_ojo_tachado else R.drawable.ic_ojo,
                stringResource(if (visible) R.string.cd_ocultar_contrasena else R.string.cd_mostrar_contrasena), tamano = 20.dp
            )
        }
    }
}

@Composable
fun CampoRut(valor: String, onValorCambia: (String) -> Unit, etiqueta: String, error: String? = null) {
    OutlinedTextField(
        value = TextFieldValue(valor, TextRange(valor.length)),
        onValueChange = { onValorCambia(ReglasDeNegocio.formatearRut(it.text)) },
        modifier = Modifier.fillMaxWidth(), label = { Text(etiqueta) }, placeholder = { Text("12.345.678-5") },
        leadingIcon = { Icono(R.drawable.ic_rut, tamano = 20.dp) },
        trailingIcon = if (error != null) { { IconoError() } } else null,
        isError = error != null, supportingText = { Text(error ?: "Escribe solo números y K; los puntos y el guion se ponen solos") },
        singleLine = true, shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Characters)
    )
}

@Composable
fun CampoBusqueda(valor: String, onValorCambia: (String) -> Unit, sugerencia: String) {
    val colores = MaterialTheme.colorScheme
    OutlinedTextField(
        value = valor, onValueChange = onValorCambia,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Medidas.espacioMedio, vertical = 4.dp),
        placeholder = { Text(sugerencia) },
        leadingIcon = { Icono(R.drawable.ic_buscar, tint = if (valor.isEmpty()) colores.onSurfaceVariant else colores.primary) },
        trailingIcon = {
            if (valor.isNotEmpty()) IconButton(onClick = { onValorCambia("") }) {
                Icono(R.drawable.ic_cerrar, stringResource(R.string.cd_limpiar_busqueda), tamano = 18.dp)
            }
        },
        singleLine = true, shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun BotonPrincipal(texto: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(Medidas.alturaBoton),
        colors = ButtonDefaults.buttonColors(containerColor = Tinta, contentColor = Color.White)
    ) {
        Text(texto, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BotonSecundario(texto: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(Medidas.alturaBoton),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = Tinta)
    ) {
        Text(texto, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Etiqueta(texto: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(texto, color = color, style = MaterialTheme.typography.bodySmall, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
fun TextoEstado(estado: EstadoCompromiso) = Etiqueta(
    estado.texto,
    when (estado) {
        EstadoCompromiso.VENCIDO -> Vencido
        EstadoCompromiso.PAGADO -> Pagado
        EstadoCompromiso.EN_REVISION, EstadoCompromiso.ANULADO -> Gris
        EstadoCompromiso.PENDIENTE -> MaterialTheme.colorScheme.primary
    }
)

@Composable
fun EtiquetaReporte(estado: EstadoReporte) = Etiqueta(
    estado.texto,
    when (estado) {
        EstadoReporte.ABIERTO -> MaterialTheme.colorScheme.primary
        EstadoReporte.EN_GESTION -> Gris
        EstadoReporte.RESUELTO -> Pagado
    }
)

@Composable
fun FilaDato(etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            valor, fontWeight = if (destacado) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
    }
}

@Composable
fun OpcionesRadio(opciones: List<String>, seleccionada: String?, onElegir: (String) -> Unit) {
    Column {
        opciones.forEach { op ->
            Row(
                Modifier.fillMaxWidth().height(44.dp).selectable(selected = op == seleccionada, onClick = { onElegir(op) }, role = Role.RadioButton),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = op == seleccionada, onClick = null)
                Text(op, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun ListaVacia(texto: String) {
    Text(texto, Modifier.fillMaxWidth().padding(Medidas.espacioGrande), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun TituloSeccion(texto: String, accion: String? = null, onAccion: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (accion != null) TextButton(onClick = onAccion) { Text(accion, fontWeight = FontWeight.Medium) }
    }
}

@Composable
fun DialogoPagoConfirmado(titulo: String, mensaje: String, textoBoton: String, onAceptar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onAceptar,
        icon = { CheckAnimado() },
        title = { Text(titulo, fontWeight = FontWeight.SemiBold) },
        text = { Text(mensaje) },
        confirmButton = {
            Button(onClick = onAceptar, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Tinta)) { Text(textoBoton) }
        }
    )
}

@Composable
fun DialogoAviso(titulo: String, mensaje: String, textoBoton: String = "Aceptar", onAceptar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onAceptar,
        title = { Text(titulo, fontWeight = FontWeight.SemiBold) },
        text = { Text(mensaje) },
        confirmButton = {
            Button(onClick = onAceptar, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Tinta)) { Text(textoBoton) }
        }
    )
}

@Composable
fun DialogoConfirmar(titulo: String, mensaje: String, textoSi: String, onSi: () -> Unit, onNo: () -> Unit, contenidoExtra: @Composable () -> Unit = {}) {
    AlertDialog(
        onDismissRequest = onNo,
        title = { Text(titulo, fontWeight = FontWeight.SemiBold) },
        text = { Column { Text(mensaje); contenidoExtra() } },
        confirmButton = {
            Button(onClick = onSi, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Tinta)) { Text(textoSi) }
        },
        dismissButton = { TextButton(onClick = onNo) { Text("Cancelar") } }
    )
}

@Composable
fun DialogoCerrarSesion(onSi: () -> Unit, onNo: () -> Unit) =
    DialogoConfirmar("¿Cerrar sesión?", "Para volver a entrar tendrás que escribir tu contraseña.", "Sí, cerrar sesión", onSi, onNo)

@Composable
fun DialogoMotivo(
    titulo: String,
    motivos: List<String>,
    textoConfirmar: String,
    onConfirmar: suspend (motivo: String?, comentario: String) -> String?,
    onExito: () -> Unit,
    onCancelar: () -> Unit,
    peligro: Boolean = false,
    encabezado: @Composable () -> Unit = {}
) {
    var motivo by remember { mutableStateOf<String?>(null) }
    var comentario by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                encabezado()
                OpcionesRadio(motivos, motivo) { motivo = it; error = null }
                CampoTexto(comentario, { comentario = it }, if (motivo == "Otro") "Comentario (obligatorio)" else "Comentario (opcional)", unaLinea = false, icono = R.drawable.ic_reportes)
                TextoError(error)
            }
        },
        confirmButton = {
            TextButton(onClick = { scope.launch { error = onConfirmar(motivo, comentario); if (error == null) onExito() } }) {
                Text(textoConfirmar, fontWeight = FontWeight.SemiBold, color = if (peligro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
