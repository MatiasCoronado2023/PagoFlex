package cl.duocuc.pagoflex.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.pagoflex.app.data.BaseDeDatos
import cl.duocuc.pagoflex.app.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ResumenCobranza(
    val pagados: Int, val pendientes: Int, val vencidos: Int, val enRevision: Int, val anulados: Int,
    val recaudado: Long, val tasa: Double?
)

data class DatosDelAdministrador(
    val compromisos: List<Compromiso> = emptyList(), val reportes: List<Reporte> = emptyList(),
    val pagos: List<Pago> = emptyList(), val empresas: List<Empresa> = emptyList()
)

class ControlDelAdministrador(application: Application) : AndroidViewModel(application) {
    private val db = BaseDeDatos.obtener(application)

    private val _datos = MutableStateFlow(DatosDelAdministrador())
    val datos: StateFlow<DatosDelAdministrador> = _datos.asStateFlow()

    fun cargar() {
        viewModelScope.launch { recargar() }
    }

    private suspend fun recargar() {
        _datos.value = withContext(Dispatchers.IO) {
            DatosDelAdministrador(db.listarCompromisos(), db.listarReportes(), db.listarPagos(), db.listarEmpresas())
        }
    }

    fun resumen(d: DatosDelAdministrador): ResumenCobranza {
        val delMes = d.compromisos.filter { it.vencimiento.year == ReglasDeNegocio.HOY.year && it.vencimiento.month == ReglasDeNegocio.HOY.month }
        fun cantidad(estado: EstadoCompromiso) = delMes.count { it.estado == estado }
        val pagados = cantidad(EstadoCompromiso.PAGADO)
        val anulados = cantidad(EstadoCompromiso.ANULADO)
        val base = delMes.size - anulados
        val idsDelMes = delMes.map { it.id }
        return ResumenCobranza(
            pagados, cantidad(EstadoCompromiso.PENDIENTE), cantidad(EstadoCompromiso.VENCIDO), cantidad(EstadoCompromiso.EN_REVISION), anulados,
            recaudado = d.pagos.filter { it.idCompromiso in idsDelMes }.sumOf { it.total.toLong() },
            tasa = if (base > 0) pagados * 100.0 / base else null
        )
    }

    suspend fun registrarCompromiso(
        admin: Usuario, rut: String, idEmpresa: Long?, concepto: String, montoTexto: String,
        fechaTexto: String, cuotaTexto: String, totalTexto: String,
        nombreNuevo: String, correoNuevo: String, telefonoNuevo: String
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val rutValido = ReglasDeNegocio.rutValido(rut)
        val persona = if (rutValido) db.buscarUsuarioPorRut(rut) else null
        val esNueva = rutValido && persona == null
        val sinDatos = nombreNuevo.isBlank() && correoNuevo.isBlank() && telefonoNuevo.isBlank()
        val errores = if (esNueva && !sinDatos) ReglasDeNegocio.validarPersona(nombreNuevo, correoNuevo, telefonoNuevo, telefonoObligatorio = true)
                      else mutableMapOf()
        if (!rutValido) errores["rut"] = ReglasDeNegocio.RUT_INVALIDO
        else if (persona?.esAdministrador == true) errores["rut"] = "Ese RUT es de un administrador"
        if (esNueva && sinDatos) errores["persona"] = "Esta persona no está en PagoFlex. Completa sus datos para registrarla."
        if (esNueva && "correo" !in errores && !sinDatos && db.existeCorreo(correoNuevo)) errores["correo"] = "Ese correo ya está registrado"
        if (idEmpresa == null) errores["empresa"] = "Elige la empresa"
        if (concepto.isBlank()) errores["concepto"] = "El concepto es obligatorio"
        val monto = montoTexto.replace(".", "").trim().toIntOrNull()
        if (monto == null || monto <= 0) errores["monto"] = "El monto debe ser un número entero mayor que 0"
        val fecha = ReglasDeNegocio.leerFecha(fechaTexto)
        when {
            fecha == null -> errores["fecha"] = "Usa el formato DD-MM-AAAA"
            fecha.isBefore(ReglasDeNegocio.HOY) -> errores["fecha"] = "La fecha no puede ser anterior a hoy (${ReglasDeNegocio.fecha(ReglasDeNegocio.HOY)})"
        }
        val cuota = cuotaTexto.trim().toIntOrNull()
        val total = totalTexto.trim().toIntOrNull()
        if (cuotaTexto.isNotBlank() || totalTexto.isNotBlank()) {
            when {
                cuota == null || total == null || cuota <= 0 || total <= 0 -> errores["cuota"] = "Escribe la cuota y el total de cuotas"
                cuota > total -> errores["cuota"] = "La cuota no puede ser mayor que el total de cuotas"
            }
        }
        if (errores.isEmpty() && persona != null && db.existeCompromiso(persona.id, idEmpresa!!, concepto, cuota)) {
            errores["concepto"] = "Ese compromiso ya está registrado para esta persona y empresa"
        }
        if (errores.isNotEmpty()) return@withContext errores

        val idPersona = persona?.id ?: db.registrarUsuario(nombreNuevo, rut, correoNuevo, telefonoNuevo, null)
        if (idPersona == -1L) return@withContext mapOf("general" to "No se pudo registrar a la persona")
        val id = db.insertarCompromiso(idPersona, idEmpresa!!, concepto, cuota, total, monto!!, fecha!!, admin.id)
        recargar()
        if (id == -1L) mapOf("general" to "No se pudo registrar el compromiso") else emptyMap()
    }

    fun sePuedeAnular(c: Compromiso): Boolean =
        c.estado == EstadoCompromiso.PENDIENTE || c.estado == EstadoCompromiso.VENCIDO || c.estado == EstadoCompromiso.EN_REVISION

    suspend fun anular(admin: Usuario, c: Compromiso, motivo: String?, comentario: String): String? {
        if (!sePuedeAnular(c)) return "Solo se pueden anular compromisos pendientes, vencidos o en revisión"
        if (motivo == null) return "Elige un motivo"
        if (motivo == "Otro" && comentario.isBlank()) return "El comentario es obligatorio si eliges \"Otro\""
        val texto = if (comentario.isBlank()) motivo else "$motivo: ${comentario.trim()}"
        val ok = withContext(Dispatchers.IO) { db.anularCompromiso(c.id, texto, admin.id) }
        recargar()
        return if (ok) null else "No se pudo anular el compromiso"
    }

    suspend fun tomar(admin: Usuario, r: Reporte): Boolean {
        val ok = withContext(Dispatchers.IO) { db.tomarReporte(r.id, admin.id) }
        recargar()
        return ok
    }

    suspend fun resolver(admin: Usuario, r: Reporte, respuesta: String, anular: Boolean, motivo: String?): String? {
        if (respuesta.isBlank()) return "Escribe una respuesta para la persona"
        if (anular && motivo == null) return "Elige el motivo de la anulación"
        val ok = withContext(Dispatchers.IO) { db.resolverReporte(r.id, admin.id, respuesta, anular, motivo) }
        recargar()
        return if (ok) null else "Solo quien tomó el reporte puede resolverlo"
    }
}
