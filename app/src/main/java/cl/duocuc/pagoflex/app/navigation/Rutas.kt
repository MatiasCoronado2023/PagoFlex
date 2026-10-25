package cl.duocuc.pagoflex.app.navigation

sealed class Ruta(val ruta: String) {
    data object Login : Ruta("login")
    data object Registro : Ruta("registro")

    data object InicioUsuario : Ruta("usuario")
    data object Pagar : Ruta("pagar")
    data object Historial : Ruta("historial")
    data object MisReportes : Ruta("mis_reportes")
    data object Logros : Ruta("logros")
    data object Comprobante : Ruta("comprobante/{id}") {
        fun crear(idCompromiso: Long) = "comprobante/$idCompromiso"
    }
    data object Detalle : Ruta("detalle/{id}") {
        fun crear(id: Long) = "detalle/$id"
    }

    data object Perfil : Ruta("perfil")
    data object CambiarClave : Ruta("cambiar_clave")

    data object InicioAdmin : Ruta("admin")
    data object RegistrarCompromiso : Ruta("registrar_compromiso")
    data object GestionReporte : Ruta("reporte/{id}") {
        fun crear(id: Long) = "reporte/$id"
    }
}
