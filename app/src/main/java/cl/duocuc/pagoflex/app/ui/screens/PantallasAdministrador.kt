package cl.duocuc.pagoflex.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.app.model.*
import cl.duocuc.pagoflex.app.ui.components.*
import cl.duocuc.pagoflex.app.ui.theme.*
import cl.duocuc.pagoflex.app.viewmodel.ResumenCobranza
import kotlinx.coroutines.launch

@Composable
fun PantallaInicioAdministrador(
    admin: Usuario,
    foto: ImageBitmap?,
    menu: @Composable () -> Unit,
    pestana: Int,
    tituloPestana: String,
    reportes: List<Reporte>,
    compromisos: List<Compromiso>,
    resumen: ResumenCobranza,
    sePuedeAnular: (Compromiso) -> Boolean,
    onAnular: suspend (Compromiso, motivo: String?, comentario: String) -> String?,
    onVerReporte: (Long) -> Unit,
    onRegistrar: () -> Unit
) {
    val saludo = "Hola, ${admin.nombre.substringBefore(' ')} · Administrador"
    Scaffold(topBar = { BarraSuperior(admin.nombre, saludo, tituloPestana, foto, menu) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (pestana) {
                0 -> ListaReportes(reportes, onVerReporte)
                1 -> ListaCompromisos(compromisos, sePuedeAnular, onAnular, onRegistrar)
                else -> PanelResumen(resumen)
            }
        }
    }
}

@Composable
private fun RowScope.Kpi(titulo: String, valor: Int, color: Color) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(valor.toString(), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = color)
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun ListaReportes(reportes: List<Reporte>, onVerReporte: (Long) -> Unit) {
    var filtro by rememberSaveable { mutableStateOf<String?>(null) }
    val lista = remember(reportes, filtro) { reportes.filter { filtro == null || it.estado.name == filtro }.sortedByDescending { it.atrasado } }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(Modifier.padding(horizontal = Medidas.espacioMedio, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Kpi("Abiertos", reportes.count { it.estado == EstadoReporte.ABIERTO }, MaterialTheme.colorScheme.primary)
                Kpi("En gestión", reportes.count { it.estado == EstadoReporte.EN_GESTION }, Gris)
                Kpi("Atrasados", reportes.count { it.atrasado }, MaterialTheme.colorScheme.error)
            }
            FiltroChips("Todos", EstadoReporte.entries.map { it.name to it.texto }, filtro) { filtro = it }
            TextoAyuda("\"Atrasado\" = más de 2 días hábiles en estado Abierto.")
        }
        if (lista.isEmpty()) item { ListaVacia("No hay reportes con este filtro.") }
        itemsIndexed(lista, key = { _, r -> r.id }) { i, r ->
            FilaTarjeta(
                "${r.numero} · ${r.nombreUsuario}",
                listOf("${r.empresa} · ${r.folio}", "${r.motivo} · ${ReglasDeNegocio.fechaHora(r.fecha)}", "Responsable: ${r.nombreAdmin ?: "sin asignar"}"),
                primera = i == 0, ultima = i == lista.lastIndex, onClick = { onVerReporte(r.id) }
            ) {
                EtiquetaReporte(r.estado)
                if (r.atrasado) Etiqueta("Atrasado", MaterialTheme.colorScheme.error)
            }
        }
        item { Spacer(Modifier.height(Medidas.espacioMedio)) }
    }
}

private fun coincide(c: Compromiso, buscado: String): Boolean {
    val q = buscado.trim()
    val rut = ReglasDeNegocio.limpiarRut(q)
    return q.isEmpty() || c.nombreUsuario.contains(q, ignoreCase = true) || c.folio.contains(q, ignoreCase = true) ||
        (rut.any { it.isDigit() } && ReglasDeNegocio.limpiarRut(c.rutUsuario).contains(rut))
}

@Composable
private fun ListaCompromisos(
    compromisos: List<Compromiso>,
    sePuedeAnular: (Compromiso) -> Boolean,
    onAnular: suspend (Compromiso, String?, String) -> String?,
    onRegistrar: () -> Unit
) {
    var filtro by rememberSaveable { mutableStateOf<String?>(null) }
    var buscado by rememberSaveable { mutableStateOf("") }
    var porAnular by remember { mutableStateOf<Compromiso?>(null) }
    var anulado by remember { mutableStateOf<String?>(null) }
    val lista = remember(compromisos, filtro, buscado) { compromisos.filter { (filtro == null || it.estado.name == filtro) && coincide(it, buscado) } }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            CampoBusqueda(buscado, { buscado = it }, "Buscar por RUT, nombre o folio")
            FiltroChips("Todos", EstadoCompromiso.entries.map { it.name to it.texto }, filtro) { filtro = it }
            Row(Modifier.fillMaxWidth().padding(start = Medidas.espacioMedio, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (buscado.isBlank()) "${lista.size} compromisos" else "${lista.size} resultado${if (lista.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRegistrar) { Text("+ Registrar compromiso", fontWeight = FontWeight.SemiBold) }
            }
        }
        if (lista.isEmpty()) {
            item {
                ListaVacia(if (buscado.isBlank()) "No hay compromisos con este filtro." else "Sin resultados para \"${buscado.trim()}\". Revisa el RUT, nombre o folio.")
            }
        }
        itemsIndexed(lista, key = { _, c -> c.id }) { i, c ->
            FilaTarjeta(
                c.nombreUsuario,
                listOf("RUT ${ReglasDeNegocio.formatearRut(c.rutUsuario)} · ${c.folio}",
                    "${c.empresa} · ${c.concepto}" + (c.cuota?.let { " · cuota $it" } ?: ""), "Vence ${ReglasDeNegocio.fecha(c.vencimiento)}"),
                ReglasDeNegocio.iniciales(c.nombreUsuario), ReglasDeNegocio.monto(c.total), buscado, primera = i == 0, ultima = i == lista.lastIndex
            ) {
                TextoEstado(c.estado)
                if (sePuedeAnular(c)) {
                    Text(
                        "Anular", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                            .clickable { porAnular = c }.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        item { Spacer(Modifier.height(Medidas.espacioMedio)) }
    }

    porAnular?.let { c ->
        DialogoMotivo(
            "¿Seguro que quieres anular ${c.folio}?", MOTIVOS_ANULACION, "Sí, anular", { motivo, comentario -> onAnular(c, motivo, comentario) },
            onExito = { porAnular = null; anulado = c.folio }, onCancelar = { porAnular = null }, peligro = true
        ) {
            Text("${c.nombreUsuario} · ${c.empresa} · ${ReglasDeNegocio.monto(c.total)}")
            Text("Esta acción no se puede deshacer. Elige el motivo:", style = MaterialTheme.typography.bodySmall)
        }
    }
    anulado?.let { DialogoAviso("Compromiso anulado", "El compromiso $it quedó anulado. La persona lo verá como \"Anulado\".") { anulado = null } }
}

@Composable
private fun PanelResumen(r: ResumenCobranza) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Medidas.espacioMedio),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TarjetaDestacada(Modifier.padding(top = Medidas.espacioChico)) {
            Text("Tasa de cobranza del mes", color = Color.White.copy(alpha = 0.9f))
            Text(r.tasa?.let { ReglasDeNegocio.porcentaje(it) } ?: "—", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            Text("Recaudado (con recargos): ${ReglasDeNegocio.monto(r.recaudado)}", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kpi("Pagados", r.pagados, Pagado)
            Kpi("Pendientes", r.pendientes, MaterialTheme.colorScheme.primary)
            Kpi("Vencidos", r.vencidos, Vencido)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kpi("En revisión", r.enRevision, Gris)
            Kpi("Anulados", r.anulados, Gris)
        }
        TextoAyuda("Tasa = pagados ÷ compromisos que vencen este mes (sin anulados) × 100.", Modifier)
    }
}

@Composable
fun PantallaRegistrarCompromiso(
    empresas: List<Empresa>,
    onRegistrar: suspend (rut: String, idEmpresa: Long?, concepto: String, monto: String, fecha: String,
                          cuota: String, total: String, nombre: String, correo: String, telefono: String) -> Map<String, String>,
    onRegistrado: () -> Unit,
    onVolver: () -> Unit
) {
    var rut by rememberSaveable { mutableStateOf("") }
    var empresa by remember { mutableStateOf<Empresa?>(null) }
    var abrirEmpresas by remember { mutableStateOf(false) }
    var concepto by rememberSaveable { mutableStateOf("") }
    var monto by rememberSaveable { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf("") }
    var cuota by rememberSaveable { mutableStateOf("") }
    var total by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var personaNueva by rememberSaveable { mutableStateOf(false) }
    var errores by remember { mutableStateOf(mapOf<String, String>()) }
    var registrado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (registrado) DialogoAviso("Compromiso registrado con éxito", "Quedó como Pendiente y la persona ya lo ve en su app.", "Aceptar", onRegistrado)

    Scaffold(topBar = { BarraTitulo("Registrar compromiso", onVolver) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(Medidas.espacioMedio)) {
            CampoRut(rut, { rut = it; personaNueva = false }, "RUT de la persona", errores["rut"])
            if (personaNueva) {
                Tarjeta(Modifier.padding(bottom = Medidas.espacioChico)) {
                    Text("Persona nueva: completa sus datos", fontWeight = FontWeight.SemiBold)
                    TextoError(errores["persona"])
                    CampoTexto(nombre, { nombre = it }, "Nombre completo", error = errores["nombre"])
                    CampoTexto(correo, { correo = it }, "Correo", error = errores["correo"], tipoTeclado = KeyboardType.Email)
                    CampoTexto(telefono, { telefono = it }, "Teléfono", error = errores["telefono"], tipoTeclado = KeyboardType.Phone)
                }
            }
            Box(Modifier.padding(bottom = Medidas.espacioChico)) {
                OutlinedButton(onClick = { abrirEmpresas = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(Medidas.alturaBoton)) {
                    Text(empresa?.let { "${it.nombre} (recargo ${ReglasDeNegocio.monto(it.recargo)})" } ?: "Elegir empresa")
                }
                DropdownMenu(expanded = abrirEmpresas, onDismissRequest = { abrirEmpresas = false }) {
                    empresas.forEach { e -> DropdownMenuItem(text = { Text(e.nombre) }, onClick = { empresa = e; abrirEmpresas = false }) }
                }
            }
            TextoError(errores["empresa"])
            CampoTexto(concepto, { concepto = it }, "Concepto", error = errores["concepto"])
            CampoTexto(monto, { monto = it }, "Monto en pesos", error = errores["monto"], tipoTeclado = KeyboardType.Number)
            CampoTexto(fecha, { fecha = it }, "Vencimiento (DD-MM-AAAA)", error = errores["fecha"])
            Row(horizontalArrangement = Arrangement.spacedBy(Medidas.espacioChico)) {
                CampoTexto(cuota, { cuota = it }, "N° cuota", Modifier.weight(1f), errores["cuota"], KeyboardType.Number)
                CampoTexto(total, { total = it }, "Total cuotas", Modifier.weight(1f), tipoTeclado = KeyboardType.Number)
            }
            TextoAyuda("Las cuotas son opcionales. El compromiso se crea como Pendiente.", Modifier)
            TextoError(errores["general"])
            Spacer(Modifier.height(Medidas.espacioMedio))
            BotonPrincipal("Registrar") {
                scope.launch {
                    errores = onRegistrar(rut, empresa?.id, concepto, monto, fecha, cuota, total, nombre, correo, telefono)
                    if (errores.isEmpty()) registrado = true
                    if (listOf("persona", "nombre", "correo", "telefono").any { it in errores }) personaNueva = true
                }
            }
        }
    }
}

@Composable
fun PantallaGestionarReporte(
    r: Reporte,
    compromiso: Compromiso?,
    onTomar: suspend () -> Boolean,
    onResolver: suspend (respuesta: String, anular: Boolean, motivo: String?) -> String?,
    onVolver: () -> Unit
) {
    var respuesta by rememberSaveable { mutableStateOf("") }
    var anular by rememberSaveable { mutableStateOf(false) }
    var motivo by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmarResolver by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<Pair<String, String>?>(null) }
    val scope = rememberCoroutineScope()
    val yaAnulado = compromiso?.estado == EstadoCompromiso.ANULADO
    val efecto = when {
        yaAnulado -> "El compromiso ya fue anulado por la empresa."
        anular -> "El compromiso quedará Anulado."
        else -> "El compromiso vuelve a quedar por pagar (Pendiente o Vencido según su fecha)."
    }

    if (confirmarResolver) {
        DialogoConfirmar("¿Resolver el reporte?", "La persona verá tu respuesta. $efecto", "Sí, resolver", onSi = {
            confirmarResolver = false
            scope.launch {
                error = onResolver(respuesta, anular, motivo)
                if (error == null) aviso = "Reporte resuelto" to "La persona ya puede ver tu respuesta en \"Mis reportes\"."
            }
        }, onNo = { confirmarResolver = false })
    }
    aviso?.let { (titulo, mensaje) -> DialogoAviso(titulo, mensaje) { aviso = null } }

    Scaffold(topBar = { BarraTitulo("Reporte ${r.numero}", onVolver) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Medidas.espacioMedio),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.nombreUsuario, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    EtiquetaReporte(r.estado)
                }
                FilaDato("Fecha", ReglasDeNegocio.fechaHora(r.fecha))
                FilaDato("Motivo", r.motivo)
                r.comentario?.let { Text("“$it”", modifier = Modifier.padding(top = 4.dp)) }
            }
            compromiso?.let { c ->
                Tarjeta {
                    Text("Compromiso", fontWeight = FontWeight.SemiBold)
                    FilaDato("Folio", c.folio)
                    FilaDato("Empresa", c.empresa)
                    FilaDato("Concepto", c.concepto + (c.cuota?.let { " · $it" } ?: ""))
                    FilaDato("Vencimiento", ReglasDeNegocio.fecha(c.vencimiento))
                    FilaDato("Monto", ReglasDeNegocio.monto(c.monto))
                    FilaDato("Estado", c.estado.texto)
                }
            }
            when (r.estado) {
                EstadoReporte.ABIERTO -> BotonPrincipal("Tomar reporte") {
                    scope.launch {
                        aviso = if (onTomar()) "Reporte tomado" to "Quedaste como responsable. Ahora escribe la respuesta para la persona."
                                else "No se pudo tomar" to "Otro administrador ya tomó este reporte."
                    }
                }
                EstadoReporte.EN_GESTION -> Tarjeta {
                    Text("Responsable: ${r.nombreAdmin ?: "—"}", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                    CampoTexto(respuesta, { respuesta = it; error = null }, "Respuesta para la persona", unaLinea = false)
                    if (!yaAnulado) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = anular, onCheckedChange = { anular = it })
                            Text("Anular el compromiso (coordinado con la empresa)")
                        }
                        if (anular) OpcionesRadio(MOTIVOS_ANULACION, motivo) { motivo = it }
                    }
                    TextoAyuda(efecto, Modifier)
                    TextoError(error)
                    Spacer(Modifier.height(Medidas.espacioChico))
                    BotonPrincipal("Resolver") {
                        error = when {
                            respuesta.isBlank() -> "Escribe una respuesta para la persona"
                            anular && motivo == null -> "Elige el motivo de la anulación"
                            else -> null
                        }
                        if (error == null) confirmarResolver = true
                    }
                }
                EstadoReporte.RESUELTO -> Tarjeta {
                    FilaDato("Resuelto por", r.nombreAdmin ?: "—")
                    r.fechaResolucion?.let { FilaDato("Fecha de resolución", ReglasDeNegocio.fechaHora(it)) }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Respuesta", fontWeight = FontWeight.SemiBold)
                    Text(r.respuesta ?: "—")
                }
            }
            Spacer(Modifier.height(Medidas.espacioMedio))
        }
    }
}
