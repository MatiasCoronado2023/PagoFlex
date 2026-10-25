package cl.duocuc.pagoflex.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.app.R
import cl.duocuc.pagoflex.app.model.*
import cl.duocuc.pagoflex.app.ui.components.*
import cl.duocuc.pagoflex.app.ui.theme.*
import cl.duocuc.pagoflex.app.viewmodel.ResumenDelMes
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

private val MESES = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
    "agosto", "septiembre", "octubre", "noviembre", "diciembre")

private fun Compromiso.descripcion() = concepto + (cuota?.let { " · cuota $it" } ?: "")

private fun ordenar(lista: List<Compromiso>): List<Compromiso> =
    lista.sortedWith(compareBy<Compromiso> { if (it.estado == EstadoCompromiso.VENCIDO) 0 else 1 }.thenBy { it.vencimiento })

private fun porcentajeDe(i: IndicadorCumplimiento): String? = (i as? IndicadorCumplimiento.Calculado)?.let { ReglasDeNegocio.porcentaje(it.porcentaje) }

@Composable
private fun FilaCompromiso(c: Compromiso, primera: Boolean, ultima: Boolean, onClick: () -> Unit) {
    FilaTarjeta(
        c.empresa, listOf(c.descripcion()), ReglasDeNegocio.iniciales(c.empresa), ReglasDeNegocio.monto(c.total),
        primera = primera, ultima = ultima, onClick = onClick,
        debajo = {
            if (c.numeroCuota != null && c.totalCuotas != null && c.totalCuotas > 1) {
                BarraCuotas(c.numeroCuota, c.totalCuotas, coloresDe(c.empresa).second)
            }
            Etiqueta(ReglasDeNegocio.textoVencimiento(c), colorVencimiento(c))
        }
    )
}

@Composable
private fun colorVencimiento(c: Compromiso): Color = when {
    c.estado == EstadoCompromiso.VENCIDO -> Vencido
    c.estado == EstadoCompromiso.PENDIENTE && ChronoUnit.DAYS.between(ReglasDeNegocio.HOY, c.vencimiento) <= 10 -> Primario
    else -> Gris
}

private fun LazyListScope.grupoCompromisos(titulo: String, lista: List<Compromiso>, onVerCompromiso: (Long) -> Unit) {
    if (lista.isEmpty()) return
    item { EncabezadoGrupo(titulo, ReglasDeNegocio.monto(lista.sumOf { it.total })) }
    itemsIndexed(lista, key = { _, c -> c.id }) { i, c ->
        FilaCompromiso(c, primera = i == 0, ultima = i == lista.lastIndex) { onVerCompromiso(c.id) }
    }
}

@Composable
private fun DatoHero(titulo: String, valor: String, modifier: Modifier) {
    Column(modifier) {
        Text(titulo, color = Color(0xFFFFE8D9), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        Text(valor, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2)
    }
}

@Composable
private fun ListaConMenu(titulo: String, menu: @Composable () -> Unit, contenido: LazyListScope.() -> Unit) {
    Scaffold(topBar = { BarraTitulo(titulo, menu = menu) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            contenido()
            item { Spacer(Modifier.height(Medidas.espacioMedio)) }
        }
    }
}

@Composable
fun PantallaInicioUsuario(
    usuario: Usuario,
    foto: ImageBitmap?,
    menu: @Composable () -> Unit,
    vigentes: List<Compromiso>,
    resumen: ResumenDelMes,
    actualizado: String,
    indicador: IndicadorCumplimiento,
    onVerCompromiso: (Long) -> Unit,
    onPagar: () -> Unit,
    onHistorial: () -> Unit
) {
    val primeros = remember(vigentes) { ordenar(vigentes).take(3) }
    val mes = MESES[ReglasDeNegocio.HOY.monthValue - 1]
    val titulo = when (resumen.cantidadDelMes) {
        0 -> "No tienes pagos pendientes en $mes"
        1 -> "Tienes 1 pago en $mes"
        else -> "Tienes ${resumen.cantidadDelMes} pagos en $mes"
    }
    val atrasos = when (resumen.cantidadVencidos) {
        0 -> "Sin atrasos"
        1 -> "1 pago vencido"
        else -> "${resumen.cantidadVencidos} pagos vencidos"
    }
    var mostrarTotal by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mostrarTotal = true }
    val totalAnimado by animateIntAsState(if (mostrarTotal) resumen.totalPorPagar else 0, tween(900), label = "totalPorPagar")
    Scaffold(topBar = { BarraSuperior(usuario.nombre, "Hola, ${usuario.nombre.substringBefore(' ')} · Usuario", titulo, foto, menu) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                TarjetaDestacada(Modifier.padding(horizontal = Medidas.espacioMedio, vertical = Medidas.espacioChico)) {
                    Text("Por pagar en $mes", color = Color(0xFFFFE8D9), style = MaterialTheme.typography.bodyMedium)
                    Text(ReglasDeNegocio.monto(totalAnimado), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider(Modifier.padding(top = 12.dp, bottom = 10.dp), color = Color.White.copy(alpha = 0.28f))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DatoHero("Próximo pago", resumen.proximo?.let { "${ReglasDeNegocio.fecha(it.vencimiento).take(5)} · ${it.empresa}" } ?: "Ninguno", Modifier.weight(1f))
                        DatoHero(atrasos, porcentajeDe(indicador)?.let { "$it pagado a tiempo" } ?: "Aún sin historial", Modifier.weight(1f))
                    }
                }
            }
            item {
                Row(Modifier.padding(horizontal = Medidas.espacioMedio, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { BotonPrincipal("Pagar", onPagar) }
                    Box(Modifier.weight(1f)) { BotonSecundario("Ver historial", onHistorial) }
                }
                TituloSeccion("Próximos pagos", if (vigentes.size > primeros.size) "Ver todos" else null, onPagar)
            }
            if (primeros.isEmpty()) item { ListaVacia("No tienes cuentas por pagar.") }
            itemsIndexed(primeros, key = { _, c -> c.id }) { i, c ->
                FilaCompromiso(c, primera = i == 0, ultima = i == primeros.lastIndex) { onVerCompromiso(c.id) }
            }
            item { TextoAyuda("Datos actualizados: $actualizado", Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) }
        }
    }
}

@Composable
fun PantallaPagar(vigentes: List<Compromiso>, menu: @Composable () -> Unit, onVerCompromiso: (Long) -> Unit) {
    val lista = remember(vigentes) { ordenar(vigentes) }
    val mes = MESES[ReglasDeNegocio.HOY.monthValue - 1]
    val vencidos = lista.filter { it.estado == EstadoCompromiso.VENCIDO }
    val delMes = lista.filter { it.estado != EstadoCompromiso.VENCIDO && it.vencimiento.year == ReglasDeNegocio.HOY.year && it.vencimiento.month == ReglasDeNegocio.HOY.month }
    val masAdelante = lista.filter { it !in vencidos && it !in delMes }
    ListaConMenu("Pagar", menu) {
        item { TextoAyuda("Toca un compromiso para ver el detalle y pagarlo. Si tienes varios con la misma empresa, se paga primero el más antiguo.", Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
        if (lista.isEmpty()) item { ListaVacia("No tienes cuentas por pagar.") }
        grupoCompromisos("Vencidos", vencidos, onVerCompromiso)
        grupoCompromisos("Vencen en $mes", delMes, onVerCompromiso)
        grupoCompromisos("Más adelante", masAdelante, onVerCompromiso)
    }
}

@Composable
fun PantallaDetalleCompromiso(
    c: Compromiso,
    comprobante: Pago?,
    reporteActivo: Reporte?,
    motivoNoPagable: String?,
    onPagar: suspend (medio: String) -> Pago?,
    onReportar: suspend (motivo: String?, comentario: String) -> String?,
    onRecordar: () -> Boolean,
    onVerComprobante: (idCompromiso: Long) -> Unit,
    onVolver: () -> Unit
) {
    var confirmar by remember { mutableStateOf(false) }
    var reportar by remember { mutableStateOf(false) }
    var pagoHecho by remember { mutableStateOf<Pago?>(null) }
    var aviso by remember { mutableStateOf<Pair<String, String>?>(null) }
    var procesando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val enviarRecordatorio = {
        aviso = if (onRecordar()) "Recordatorio enviado" to "Revisa la notificación de PagoFlex en tu teléfono."
        else "No se pudo enviar" to "Activa las notificaciones de PagoFlex en los ajustes del teléfono."
    }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            enviarRecordatorio()
        } else {
            aviso = "Notificaciones desactivadas" to "Sin este permiso no podemos recordarte tus pagos. Puedes activarlo después en los ajustes del teléfono."
        }
    }

    Scaffold(topBar = { BarraTitulo("Detalle del compromiso", onVolver) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Medidas.espacioMedio),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CuadroIniciales(ReglasDeNegocio.iniciales(c.empresa), 46.dp, c.empresa)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(c.empresa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(c.descripcion(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextoEstado(c.estado)
                }
                Text(ReglasDeNegocio.monto(c.total), fontSize = 30.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                TextoAyuda("Total a pagar", Modifier)
            }
            Tarjeta {
                FilaDato("Folio", c.folio)
                FilaDato("Concepto", c.concepto)
                FilaDato("Cuota", c.cuota ?: "No aplica")
                FilaDato("Vencimiento", ReglasDeNegocio.fecha(c.vencimiento))
                c.motivoAnulacion?.let { FilaDato("Motivo de anulación", it) }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                FilaDato("Monto original", ReglasDeNegocio.monto(c.monto))
                if (c.estado == EstadoCompromiso.VENCIDO) {
                    FilaDato("Días de atraso", c.diasAtraso.toString())
                    FilaDato("Recargo por atraso (una vez)", "+ ${ReglasDeNegocio.monto(c.recargo)}")
                }
                FilaDato("Total a pagar", ReglasDeNegocio.monto(c.total), destacado = true)
            }
            reporteActivo?.let {
                Tarjeta {
                    Text("Reporte ${it.numero} · ${it.estado.texto}", fontWeight = FontWeight.SemiBold)
                    Text("Mientras revisamos tu reporte, este compromiso no se puede pagar.", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (c.estado == EstadoCompromiso.PAGADO && comprobante != null) BotonSecundario("Ver comprobante") { onVerComprobante(c.id) }
            if (ReglasDeNegocio.esPagable(c)) {
                BotonPrincipal("Pagar ${ReglasDeNegocio.monto(c.total)}") {
                    if (motivoNoPagable != null) aviso = "Todavía no se puede pagar" to motivoNoPagable else confirmar = true
                }
                BotonSecundario("Reportar un problema") { reportar = true }
                BotonSecundario("Recordarme este pago") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else enviarRecordatorio()
                }
            }
            Spacer(Modifier.height(Medidas.espacioMedio))
        }
    }

    if (confirmar) {
        var medio by remember { mutableStateOf(MEDIOS_PAGO.first()) }
        DialogoConfirmar(
            "¿Estás seguro de realizar el pago?", "Vas a pagar ${ReglasDeNegocio.monto(c.total)} a ${c.empresa} (${c.descripcion()}).", "Sí, pagar",
            onSi = {
                confirmar = false
                procesando = true
                scope.launch {
                    val pago = onPagar(medio)
                    procesando = false
                    if (pago != null) pagoHecho = pago
                    else aviso = "El pago no se completó" to "No se pudo registrar el pago. No se realizó ningún cobro; inténtalo de nuevo."
                }
            },
            onNo = { confirmar = false }
        ) {
            Text("Medio de pago (simulado):", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = Medidas.espacioChico))
            OpcionesRadio(MEDIOS_PAGO, medio) { medio = it }
            Text("No se piden datos de tarjetas ni cuentas.", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (reportar) {
        DialogoMotivo(
            "Reportar un problema", MOTIVOS_REPORTE, "Enviar", onReportar,
            onExito = { reportar = false; aviso = "Reporte enviado" to "Te responderemos pronto. Mientras tanto el compromiso queda en revisión." },
            onCancelar = { reportar = false }
        )
    }
    if (procesando) {
        AlertDialog(
            onDismissRequest = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                    Text("Procesando el pago…", modifier = Modifier.padding(start = 14.dp))
                }
            },
            confirmButton = {}
        )
    }
    pagoHecho?.let { p ->
        DialogoPagoConfirmado(
            if (p.medio == "Transferencia") "Transferencia realizada" else "Pago realizado",
            "Pagaste ${ReglasDeNegocio.monto(p.total)} a ${p.empresa}.\nComprobante N° ${p.numeroComprobante}.", "Ver comprobante"
        ) { pagoHecho = null; onVerComprobante(c.id) }
    }
    aviso?.let { (titulo, mensaje) -> DialogoAviso(titulo, mensaje, "Entendido") { aviso = null } }
}

@Composable
private fun CortePunteado() {
    Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(2.dp)) {
            drawLine(
                Color(0xFFD6D5D0), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
        }
        Box(Modifier.align(Alignment.CenterStart).offset(x = (-10).dp).size(20.dp).background(Fondo, CircleShape))
        Box(Modifier.align(Alignment.CenterEnd).offset(x = 10.dp).size(20.dp).background(Fondo, CircleShape))
    }
}

@Composable
fun PantallaComprobante(p: Pago, onVolver: () -> Unit) {
    Scaffold(topBar = { BarraTitulo("Comprobante", onVolver) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Medidas.radio)).background(MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PagoFlex", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.padding(top = 10.dp)) { CheckAnimado(48.dp) }
                    Text("Pago confirmado", color = Pagado, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
                    Text(ReglasDeNegocio.monto(p.total), fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                    TextoAyuda(ReglasDeNegocio.fechaHora(p.fechaHora), Modifier)
                }
                CortePunteado()
                Column(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
                    FilaDato("N° comprobante", p.numeroComprobante)
                    FilaDato("Pagado por", p.pagador)
                    FilaDato("RUT", ReglasDeNegocio.ocultarRut(p.rutPagador))
                    FilaDato("Empresa", p.empresa)
                    FilaDato("Folio", p.folio)
                    FilaDato("Concepto", p.concepto + (p.cuota?.let { " · cuota $it" } ?: ""))
                    FilaDato("Vencimiento", ReglasDeNegocio.fecha(p.vencimiento))
                    FilaDato("Medio de pago", p.medio)
                    FilaDato("Canal", p.canal)
                }
                HorizontalDivider(Modifier.padding(horizontal = 22.dp), color = LineaSuave)
                Column(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
                    FilaDato("Monto original", ReglasDeNegocio.monto(p.montoOriginal))
                    FilaDato("Recargo", ReglasDeNegocio.monto(p.recargo))
                    FilaDato("Total pagado", ReglasDeNegocio.monto(p.total), destacado = true)
                }
            }
            TextoAyuda("Este comprobante quedó guardado en tu historial y no se puede modificar.", Modifier)
            BotonPrincipal("Listo", onVolver)
            Spacer(Modifier.height(Medidas.espacioMedio))
        }
    }
}

private fun nombreMes(anioMes: String): String =
    MESES[anioMes.takeLast(2).toInt() - 1].replaceFirstChar { it.uppercase() } + " " + anioMes.take(4)

@Composable
fun PantallaHistorial(
    pagos: List<Pago>,
    compromisos: List<Compromiso>,
    menu: @Composable () -> Unit,
    onVerComprobante: (idCompromiso: Long) -> Unit
) {
    var empresa by remember { mutableStateOf<String?>(null) }
    var periodo by remember { mutableStateOf<String?>(null) }
    val anulados = remember(compromisos) { compromisos.filter { it.estado == EstadoCompromiso.ANULADO } }
    val empresas = remember(pagos, anulados) { (pagos.map { it.empresa } + anulados.map { it.empresa }).distinct().sorted() }
    val periodos = remember(pagos) { pagos.map { it.fechaHora.take(7) }.distinct().sortedDescending().take(12) }
    val pagosFiltrados = pagos.filter { (empresa == null || it.empresa == empresa) && it.fechaHora.startsWith(periodo ?: "") }
    val pagosPorMes = pagosFiltrados.sortedByDescending { it.fechaHora }.groupBy { it.fechaHora.take(7) }
    val anuladosFiltrados = if (periodo == null) anulados.filter { empresa == null || it.empresa == empresa } else emptyList()

    ListaConMenu("Historial de pagos", menu) {
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Medidas.espacioMedio, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroMenu("Todas las empresas", empresas.map { it to it }, empresa) { empresa = it }
                FiltroMenu("Todos los meses", periodos.map { it to nombreMes(it) }, periodo) { periodo = it }
            }
        }
        if (pagosFiltrados.isEmpty() && anuladosFiltrados.isEmpty()) item { ListaVacia("No hay movimientos con este filtro.") }
        pagosPorMes.forEach { (anioMes, delMes) ->
            item(key = "mes$anioMes") { EncabezadoGrupo(nombreMes(anioMes), ReglasDeNegocio.monto(delMes.sumOf { it.total })) }
            itemsIndexed(delMes, key = { _, p -> "p${p.id}" }) { i, p ->
                FilaTarjeta(
                    p.empresa, listOf(p.concepto + (p.cuota?.let { " · cuota $it" } ?: ""), "${ReglasDeNegocio.fechaHora(p.fechaHora)} · ${p.canal}"),
                    ReglasDeNegocio.iniciales(p.empresa), ReglasDeNegocio.monto(p.total), primera = i == 0, ultima = i == delMes.lastIndex,
                    onClick = { onVerComprobante(p.idCompromiso) },
                    debajo = {
                        if (p.recargo > 0) {
                            Text("Incluye recargo de ${ReglasDeNegocio.monto(p.recargo)} por atraso", style = MaterialTheme.typography.bodySmall, color = Primario)
                        }
                    }
                ) { TextoEstado(EstadoCompromiso.PAGADO) }
            }
        }
        if (anuladosFiltrados.isNotEmpty()) {
            item { EncabezadoGrupo("Anulados por la empresa") }
            itemsIndexed(anuladosFiltrados, key = { _, c -> "a${c.id}" }) { i, c ->
                FilaTarjeta(
                    c.empresa, listOf(c.descripcion(), "${c.folio} · ${c.motivoAnulacion ?: "Anulado por la empresa"}"), ReglasDeNegocio.iniciales(c.empresa),
                    primera = i == 0, ultima = i == anuladosFiltrados.lastIndex
                ) { TextoEstado(EstadoCompromiso.ANULADO) }
            }
        }
    }
}

@Composable
fun PantallaMisReportes(reportes: List<Reporte>, menu: @Composable () -> Unit) {
    ListaConMenu("Mis reportes", menu) {
        item { TextoAyuda("Para reportar un problema, abre el compromiso desde Inicio o Pagar y toca \"Reportar un problema\".") }
        if (reportes.isEmpty()) item { ListaVacia("No has reportado problemas.") }
        items(reportes, key = { it.id }) { r ->
            Tarjeta(Modifier.padding(horizontal = Medidas.espacioMedio, vertical = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.numero, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    EtiquetaReporte(r.estado)
                }
                TextoAyuda("${r.empresa} · ${r.folio}\n${ReglasDeNegocio.fechaHora(r.fecha)} · ${r.motivo}", Modifier)
                r.comentario?.let { Text("“$it”", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
                r.respuesta?.let {
                    Column(
                        Modifier.padding(top = 8.dp).fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)).padding(10.dp)
                    ) {
                        Text("Respuesta de PagoFlex", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun PantallaLogros(indicador: IndicadorCumplimiento, obtenidos: List<LogroObtenido>, menu: @Composable () -> Unit) {
    ListaConMenu("Mis logros", menu) {
        item {
            TarjetaDestacada(Modifier.padding(horizontal = Medidas.espacioMedio, vertical = Medidas.espacioChico)) {
                Text("Cumplimiento (últimos 12 meses)", color = Color.White.copy(alpha = 0.9f))
                Text(porcentajeDe(indicador) ?: "Historial insuficiente", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                if (indicador is IndicadorCumplimiento.Calculado) {
                    Text("${indicador.categoria} · ${indicador.aTiempo} de ${indicador.considerados} pagos a tiempo", color = Color.White)
                }
            }
            TextoAyuda("Los logros se ganan pagando a tiempo. No valen dinero y no se pierden aunque después te atrases.")
            TituloSeccion("Logros (${obtenidos.size} de ${LOGROS.size})")
        }
        items(LOGROS, key = { it.nombre }) { logro ->
            val ganado = obtenidos.firstOrNull { it.nombre == logro.nombre }
            val colores = MaterialTheme.colorScheme
            Tarjeta(Modifier.padding(horizontal = Medidas.espacioMedio, vertical = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).background(if (ganado != null) colores.primaryContainer else colores.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icono(R.drawable.ic_logros, tint = if (ganado != null) colores.primary else Gris) }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(logro.nombre, fontWeight = FontWeight.SemiBold, color = if (ganado != null) colores.onSurface else colores.onSurfaceVariant)
                        Text(logro.descripcion, style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                        Text(
                            ganado?.let { "Obtenido el ${ReglasDeNegocio.fechaHora(it.fecha).take(10)}" } ?: "Aún no obtenido",
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                            color = if (ganado != null) Pagado else colores.outline
                        )
                    }
                }
            }
        }
    }
}
