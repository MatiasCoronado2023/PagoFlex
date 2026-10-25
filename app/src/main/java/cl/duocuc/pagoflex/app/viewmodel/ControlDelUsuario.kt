package cl.duocuc.pagoflex.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.pagoflex.app.data.BaseDeDatos
import cl.duocuc.pagoflex.app.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ResumenDelMes(val totalPorPagar: Int, val cantidadDelMes: Int, val cantidadVencidos: Int, val proximo: Compromiso?)

data class DatosDelUsuario(
    val compromisos: List<Compromiso> = emptyList(), val pagos: List<Pago> = emptyList(),
    val reportes: List<Reporte> = emptyList(), val reconocimientos: List<LogroObtenido> = emptyList(),
    val actualizado: String = ""
)

class ControlDelUsuario(application: Application) : AndroidViewModel(application) {
    private val db = BaseDeDatos.obtener(application)
    private var usuario: Usuario? = null

    private val _datos = MutableStateFlow(DatosDelUsuario())
    val datos: StateFlow<DatosDelUsuario> = _datos.asStateFlow()

    fun cargar(u: Usuario) {
        if (usuario?.id != u.id) _datos.value = DatosDelUsuario()
        usuario = u
        viewModelScope.launch { recargar() }
    }

    private suspend fun recargar() {
        val u = usuario ?: return
        _datos.value = withContext(Dispatchers.IO) {
            val compromisos = db.compromisosDeUsuario(u.id)
            val pagos = db.pagosDeUsuario(u.id)
            ReglasDeNegocio.logrosGanados(compromisos, pagos).forEach { db.otorgarReconocimiento(u.id, it) }
            DatosDelUsuario(compromisos, pagos, db.reportesDeUsuario(u.id), db.reconocimientosDeUsuario(u.id), ReglasDeNegocio.fechaHora(ReglasDeNegocio.ahora()))
        }
    }

    fun vigentes(d: DatosDelUsuario): List<Compromiso> =
        d.compromisos.filter { it.estado != EstadoCompromiso.PAGADO && it.estado != EstadoCompromiso.ANULADO }

    fun resumen(d: DatosDelUsuario): ResumenDelMes {
        val porPagarDelMes = d.compromisos.filter {
            ReglasDeNegocio.esPagable(it) && it.vencimiento.year == ReglasDeNegocio.HOY.year && it.vencimiento.month == ReglasDeNegocio.HOY.month
        }
        return ResumenDelMes(
            totalPorPagar = porPagarDelMes.sumOf { it.total },
            cantidadDelMes = porPagarDelMes.size,
            cantidadVencidos = d.compromisos.count { it.estado == EstadoCompromiso.VENCIDO },
            proximo = d.compromisos.filter { it.estado == EstadoCompromiso.PENDIENTE }.minByOrNull { it.vencimiento }
        )
    }

    fun indicador(d: DatosDelUsuario): IndicadorCumplimiento = ReglasDeNegocio.indicador(d.compromisos, d.pagos)

    fun validarPago(c: Compromiso): String? = ReglasDeNegocio.validarPago(c, _datos.value.compromisos)

    fun comprobanteDe(idCompromiso: Long): Pago? = _datos.value.pagos.firstOrNull { it.idCompromiso == idCompromiso }

    fun reporteActivo(idCompromiso: Long): Reporte? =
        _datos.value.reportes.firstOrNull { it.idCompromiso == idCompromiso && it.estado != EstadoReporte.RESUELTO }

    suspend fun pagar(c: Compromiso, medio: String): Pago? {
        if (validarPago(c) != null) return null
        val pago = withContext(Dispatchers.IO) { db.registrarPago(c.id, medio) }
        recargar()
        return pago
    }

    suspend fun reportar(c: Compromiso, motivo: String?, comentario: String): String? {
        if (motivo == null) return "Elige un motivo"
        if (motivo == "Otro" && comentario.isBlank()) return "Cuéntanos qué pasó (el comentario es obligatorio si eliges \"Otro\")"
        reporteActivo(c.id)?.let { return "Ya tienes el reporte ${it.numero} en curso para este compromiso" }
        if (!ReglasDeNegocio.esPagable(c)) return "Este compromiso no se puede reportar"
        val id = withContext(Dispatchers.IO) { db.crearReporte(c.id, motivo, comentario) }
        recargar()
        return if (id == -1L) "No se pudo crear el reporte. Inténtalo de nuevo." else null
    }
}
