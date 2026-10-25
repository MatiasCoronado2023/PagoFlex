package cl.duocuc.pagoflex.app.model

import java.time.LocalDate

object RolUsuario {
    const val ADMINISTRADOR = 1L
    const val USUARIO = 2L
}

data class Usuario(val id: Long, val rut: String, val nombre: String, val correo: String, val telefono: String?, val rolId: Long) {
    val esAdministrador: Boolean get() = rolId == RolUsuario.ADMINISTRADOR
}

data class Empresa(val id: Long, val nombre: String, val recargo: Int)

enum class EstadoCompromiso(val texto: String) {
    PENDIENTE("Pendiente"), VENCIDO("Vencido"), PAGADO("Pagado"), EN_REVISION("En revisión"), ANULADO("Anulado")
}

data class Compromiso(
    val id: Long, val folio: String,
    val idUsuario: Long, val nombreUsuario: String, val rutUsuario: String,
    val idEmpresa: Long, val empresa: String, val recargoEmpresa: Int,
    val concepto: String, val numeroCuota: Int?, val totalCuotas: Int?, val monto: Int,
    val vencimiento: LocalDate, val estadoGuardado: String, val motivoAnulacion: String?
) {
    val cuota: String? get() = if (numeroCuota != null && totalCuotas != null) "$numeroCuota de $totalCuotas" else null
    val estado: EstadoCompromiso get() = ReglasDeNegocio.estado(this)
    val diasAtraso: Int get() = ReglasDeNegocio.diasAtraso(this)
    val recargo: Int get() = ReglasDeNegocio.recargo(this)
    val total: Int get() = monto + recargo
}

data class Pago(
    val id: Long, val numeroComprobante: String, val idCompromiso: Long, val fechaHora: String,
    val montoOriginal: Int, val recargo: Int, val total: Int, val medio: String, val canal: String,
    val folio: String, val concepto: String, val cuota: String?, val vencimiento: LocalDate,
    val empresa: String, val pagador: String, val rutPagador: String
)

enum class EstadoReporte(val texto: String) {
    ABIERTO("Abierto"), EN_GESTION("En gestión"), RESUELTO("Resuelto")
}

data class Reporte(
    val id: Long, val numero: String, val idCompromiso: Long, val folio: String, val concepto: String,
    val empresa: String, val nombreUsuario: String, val motivo: String, val comentario: String?, val fecha: String,
    val estado: EstadoReporte, val nombreAdmin: String?, val respuesta: String?, val fechaResolucion: String?
) {
    val atrasado: Boolean get() = estado == EstadoReporte.ABIERTO && ReglasDeNegocio.diasHabilesDesde(fecha) > 2
}

val MOTIVOS_REPORTE = listOf("Ya pagué por otro medio", "No reconozco este cobro", "El monto no corresponde", "Otro")

val MOTIVOS_ANULACION = listOf("Repactación", "Error de registro", "Pagado directamente a la empresa", "Otro")

val MEDIOS_PAGO = listOf("Tarjeta de débito", "Tarjeta de crédito", "Transferencia")

sealed class IndicadorCumplimiento {
    data object HistorialInsuficiente : IndicadorCumplimiento()
    data class Calculado(val aTiempo: Int, val considerados: Int, val porcentaje: Double, val categoria: String) : IndicadorCumplimiento()
}

data class Logro(val nombre: String, val descripcion: String)

val LOGROS = listOf(
    Logro("Primer pago a tiempo", "Pagaste un compromiso antes de su vencimiento."),
    Logro("Pago a tiempo desde la app", "Pagaste a tiempo usando esta app."),
    Logro("Cinco pagos a tiempo", "Llevas 5 compromisos pagados a tiempo."),
    Logro("Mes al día", "Pagaste a tiempo todo lo que vencía en un mes."),
    Logro("Tres meses seguidos al día", "Tres meses seguidos con todo pagado a tiempo.")
)

data class LogroObtenido(val nombre: String, val fecha: String)
