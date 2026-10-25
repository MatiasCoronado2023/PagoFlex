package cl.duocuc.pagoflex.app.model

import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object ReglasDeNegocio {
    val HOY: LocalDate = LocalDate.of(2026, 10, 6)

    const val RUT_INVALIDO = "RUT no válido (revisa el dígito verificador)"

    private val formatoFecha = DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val formatoFechaHoraBd = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
    private val formatoCorreo = Regex("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")

    fun ahora(): String = LocalDateTime.of(HOY, LocalTime.now()).format(formatoFechaHoraBd)

    fun estado(c: Compromiso): EstadoCompromiso = when (c.estadoGuardado) {
        "PAGADO" -> EstadoCompromiso.PAGADO
        "ANULADO" -> EstadoCompromiso.ANULADO
        "EN_REVISION" -> EstadoCompromiso.EN_REVISION
        else -> if (HOY.isAfter(c.vencimiento)) EstadoCompromiso.VENCIDO else EstadoCompromiso.PENDIENTE
    }

    fun diasAtraso(c: Compromiso): Int =
        if (estado(c) == EstadoCompromiso.VENCIDO) ChronoUnit.DAYS.between(c.vencimiento, HOY).toInt() else 0

    fun recargo(c: Compromiso): Int = if (estado(c) == EstadoCompromiso.VENCIDO) c.recargoEmpresa else 0

    fun esPagable(c: Compromiso): Boolean = c.estado == EstadoCompromiso.PENDIENTE || c.estado == EstadoCompromiso.VENCIDO

    fun validarPago(c: Compromiso, delUsuario: List<Compromiso>): String? {
        when (c.estado) {
            EstadoCompromiso.PAGADO -> return "Este compromiso ya está pagado."
            EstadoCompromiso.ANULADO -> return "Este compromiso fue anulado: no tienes que pagarlo."
            EstadoCompromiso.EN_REVISION -> return "Estamos revisando tu reporte. Podrás pagar cuando esté resuelto."
            else -> {}
        }
        val anterior = delUsuario
            .filter { it.id != c.id && it.idEmpresa == c.idEmpresa && esPagable(it) && it.vencimiento.isBefore(c.vencimiento) }
            .minByOrNull { it.vencimiento }
        return anterior?.let { "Primero paga la cuota ${it.cuota ?: it.folio} de ${it.empresa}, que vence antes." }
    }

    fun textoVencimiento(c: Compromiso): String {
        val dias = ChronoUnit.DAYS.between(HOY, c.vencimiento).toInt()
        val diaMes = c.vencimiento.format(DateTimeFormatter.ofPattern("dd-MM"))
        return when (c.estado) {
            EstadoCompromiso.VENCIDO -> if (c.diasAtraso == 1) "Vencido hace 1 día" else "Vencido hace ${c.diasAtraso} días"
            EstadoCompromiso.PENDIENTE -> when (dias) {
                0 -> "Vence hoy"
                1 -> "Vence mañana · $diaMes"
                else -> "Vence en $dias días · $diaMes"
            }
            else -> c.estado.texto
        }
    }

    fun fueATiempo(c: Compromiso, pagos: List<Pago>): Boolean {
        val pago = pagos.firstOrNull { it.idCompromiso == c.id } ?: return false
        return !LocalDate.parse(pago.fechaHora.take(10)).isAfter(c.vencimiento)
    }

    fun esElegible(c: Compromiso): Boolean =
        !c.vencimiento.isBefore(HOY.minusMonths(12)) && c.vencimiento.isBefore(HOY) &&
            c.estado != EstadoCompromiso.ANULADO && c.estado != EstadoCompromiso.EN_REVISION

    fun indicador(compromisos: List<Compromiso>, pagos: List<Pago>): IndicadorCumplimiento {
        val elegibles = compromisos.filter { esElegible(it) }
        if (elegibles.size < 3) return IndicadorCumplimiento.HistorialInsuficiente
        val aTiempo = elegibles.count { fueATiempo(it, pagos) }
        val porcentaje = Math.round(aTiempo * 1000.0 / elegibles.size) / 10.0
        val categoria = when {
            porcentaje >= 95.0 -> "Ejemplar"
            porcentaje >= 80.0 -> "Al día"
            else -> "En camino"
        }
        return IndicadorCumplimiento.Calculado(aTiempo, elegibles.size, porcentaje, categoria)
    }

    fun logrosGanados(compromisos: List<Compromiso>, pagos: List<Pago>): List<String> {
        val ganados = mutableListOf<String>()
        val pagadosATiempo = compromisos.filter { it.estado == EstadoCompromiso.PAGADO && fueATiempo(it, pagos) }
        if (pagadosATiempo.isNotEmpty()) ganados += "Primer pago a tiempo"
        if (pagadosATiempo.any { c -> pagos.any { it.idCompromiso == c.id && it.canal == "App" } }) ganados += "Pago a tiempo desde la app"
        if (pagadosATiempo.size >= 5) ganados += "Cinco pagos a tiempo"

        val porMes = compromisos
            .filter { it.estado != EstadoCompromiso.ANULADO && it.vencimiento.isBefore(HOY.withDayOfMonth(1)) }
            .groupBy { it.vencimiento.withDayOfMonth(1) }
        val mesesAlDia = porMes.filter { (_, delMes) -> delMes.all { fueATiempo(it, pagos) } }.keys
        if (mesesAlDia.isNotEmpty()) ganados += "Mes al día"
        if (mesesAlDia.any { it.plusMonths(1) in mesesAlDia && it.plusMonths(2) in mesesAlDia }) ganados += "Tres meses seguidos al día"
        return ganados
    }

    private val feriados = listOf(
        "2026-01-01", "2026-04-03", "2026-04-04", "2026-05-01", "2026-05-21", "2026-06-21",
        "2026-06-29", "2026-07-16", "2026-08-15", "2026-09-18", "2026-09-19", "2026-10-12",
        "2026-10-31", "2026-11-01", "2026-12-08", "2026-12-25"
    ).map { LocalDate.parse(it) }

    fun diasHabilesDesde(fechaBd: String): Int {
        var dia = LocalDate.parse(fechaBd.take(10)).plusDays(1)
        var habiles = 0
        while (!dia.isAfter(HOY)) {
            val finDeSemana = dia.dayOfWeek == DayOfWeek.SATURDAY || dia.dayOfWeek == DayOfWeek.SUNDAY
            if (!finDeSemana && dia !in feriados) habiles++
            dia = dia.plusDays(1)
        }
        return habiles
    }

    fun limpiarRut(rut: String): String = rut.replace(".", "").replace("-", "").trim().uppercase()

    fun rutValido(rut: String): Boolean {
        val limpio = limpiarRut(rut)
        val cuerpo = limpio.dropLast(1)
        if (limpio.length < 2 || !cuerpo.all { it.isDigit() }) return false
        var suma = 0
        var factor = 2
        for (digito in cuerpo.reversed()) {
            suma += (digito - '0') * factor
            factor = if (factor == 7) 2 else factor + 1
        }
        val dv = when (val resto = 11 - (suma % 11)) {
            11 -> '0'
            10 -> 'K'
            else -> '0' + resto
        }
        return limpio.last() == dv
    }

    fun formatearRut(texto: String): String {
        val limpio = texto.uppercase().filter { it.isDigit() || it == 'K' }
        val valido = limpio.filterIndexed { i, letra -> letra != 'K' || i == limpio.lastIndex }.take(9)
        if (valido.length < 2) return valido
        val cuerpo = valido.dropLast(1).reversed().chunked(3).joinToString(".").reversed()
        return "$cuerpo-${valido.last()}"
    }

    fun ocultarRut(rut: String): String {
        val formateado = formatearRut(rut)
        return formateado.substringBefore('.') + ".***.***-" + formateado.last()
    }

    fun iniciales(nombre: String): String =
        nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

    fun correoValido(correo: String): Boolean = formatoCorreo.matches(correo.trim())

    fun validarPersona(nombre: String, correo: String, telefono: String, telefonoObligatorio: Boolean): MutableMap<String, String> {
        val errores = mutableMapOf<String, String>()
        if (nombre.trim().length < 3) errores["nombre"] = "Ingresa el nombre completo"
        if (!correoValido(correo)) errores["correo"] = "Ingresa un correo válido"
        val telefonoMalo = telefono.count { it.isDigit() } < 8
        if ((telefonoObligatorio || telefono.isNotBlank()) && telefonoMalo) errores["telefono"] = "Ingresa un teléfono válido"
        return errores
    }

    fun hash(correo: String, clave: String): String =
        MessageDigest.getInstance("SHA-256").digest("${correo.trim().lowercase()}:$clave".toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun monto(valor: Number): String = "$ " + String.format(Locale.ROOT, "%,d", valor.toLong()).replace(',', '.')

    fun fecha(d: LocalDate): String = d.format(formatoFecha)

    fun fechaHora(bd: String): String = fecha(LocalDate.parse(bd.take(10))) + bd.drop(10)

    fun leerFecha(texto: String): LocalDate? = runCatching { LocalDate.parse(texto.trim(), formatoFecha) }.getOrNull()

    fun porcentaje(valor: Double): String = String.format(Locale.ROOT, "%.1f", valor).replace('.', ',') + " %"
}
