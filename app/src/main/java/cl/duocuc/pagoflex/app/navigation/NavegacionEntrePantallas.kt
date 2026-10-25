package cl.duocuc.pagoflex.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duocuc.pagoflex.app.R
import cl.duocuc.pagoflex.app.model.EstadoReporte
import cl.duocuc.pagoflex.app.model.Usuario
import cl.duocuc.pagoflex.app.ui.components.*
import cl.duocuc.pagoflex.app.ui.screens.*
import cl.duocuc.pagoflex.app.viewmodel.*
import kotlinx.coroutines.delay

private val PESTANAS_ADMIN = listOf("Reportes", "Compromisos", "Resumen de cobranza")

private fun inicioDe(usuario: Usuario?): String = when {
    usuario == null -> Ruta.Login.ruta
    usuario.esAdministrador -> Ruta.InicioAdmin.ruta
    else -> Ruta.InicioUsuario.ruta
}

private fun NavHostController.irAlInicio(usuario: Usuario?) = navigate(inicioDe(usuario)) { popUpTo(0) { inclusive = true } }

private fun NavHostController.irAPestana(ruta: Ruta, raiz: Ruta) {
    if (ruta == raiz) popBackStack(raiz.ruta, inclusive = false)
    else navigate(ruta.ruta) { popUpTo(raiz.ruta); launchSingleTop = true }
}

@Composable
fun NavegacionEntrePantallas() {
    val controlSesion: ControlDeSesion = viewModel()
    val controlUsuario: ControlDelUsuario = viewModel()
    val controlAdmin: ControlDelAdministrador = viewModel()
    val listo by controlSesion.listo.collectAsStateWithLifecycle()
    val usuario by controlSesion.usuarioActual.collectAsStateWithLifecycle()
    val foto by controlSesion.foto.collectAsStateWithLifecycle()
    val correoRecordado by controlSesion.correoRecordado.collectAsStateWithLifecycle()

    var splashVisto by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1200); splashVisto = true }
    if (!listo || !splashVisto) {
        PantallaBienvenida()
        return
    }
    LaunchedEffect(usuario) { usuario?.let { u -> if (u.esAdministrador) controlAdmin.cargar() else controlUsuario.cargar(u) } }

    val nav = rememberNavController()
    var pestanaAdmin by rememberSaveable { mutableIntStateOf(0) }
    val datosUsuario by controlUsuario.datos.collectAsStateWithLifecycle()
    val datosAdmin by controlAdmin.datos.collectAsStateWithLifecycle()

    val irUsuario = { ruta: Ruta -> nav.irAPestana(ruta, Ruta.InicioUsuario) }
    val irAdmin = { pestana: Int -> pestanaAdmin = pestana; nav.irAPestana(Ruta.InicioAdmin, Ruta.InicioAdmin) }
    val cerrarSesion = { controlSesion.cerrarSesion(); pestanaAdmin = 0; nav.irAlInicio(null) }

    val menuUsuario = listOf(
        OpcionMenu("Inicio", R.drawable.ic_inicio) { irUsuario(Ruta.InicioUsuario) },
        OpcionMenu("Pagar", R.drawable.ic_pagar) { irUsuario(Ruta.Pagar) },
        OpcionMenu("Historial de pagos", R.drawable.ic_historial) { irUsuario(Ruta.Historial) },
        OpcionMenu("Mis reportes", R.drawable.ic_reportes, datosUsuario.reportes.count { it.estado != EstadoReporte.RESUELTO }) {
            irUsuario(Ruta.MisReportes)
        },
        OpcionMenu("Mis logros", R.drawable.ic_logros, datosUsuario.reconocimientos.size) { irUsuario(Ruta.Logros) },
        OpcionMenu("Mi perfil", R.drawable.ic_perfil) { irUsuario(Ruta.Perfil) }
    )
    val menuAdmin = listOf(
        OpcionMenu("Reportes", R.drawable.ic_reportes, datosAdmin.reportes.count { it.atrasado }) { irAdmin(0) },
        OpcionMenu("Compromisos", R.drawable.ic_compromisos) { irAdmin(1) },
        OpcionMenu("Resumen de cobranza", R.drawable.ic_resumen) { irAdmin(2) },
        OpcionMenu("Registrar compromiso", R.drawable.ic_compromisos) { nav.navigate(Ruta.RegistrarCompromiso.ruta) },
        OpcionMenu("Mi perfil", R.drawable.ic_perfil) { nav.irAPestana(Ruta.Perfil, Ruta.InicioAdmin) }
    )

    val botonMenu: @Composable (String) -> Unit = { actual ->
        usuario?.let { u ->
            BotonMenu(u.nombre, u.correo, foto, if (u.esAdministrador) menuAdmin else menuUsuario, actual, cerrarSesion)
        }
    }

    fun NavGraphBuilder.pantalla(ruta: Ruta, esAdmin: Boolean?, contenido: @Composable (u: Usuario, id: Long) -> Unit) {
        val argumentos = if ("{id}" in ruta.ruta) listOf(navArgument("id") { type = NavType.LongType }) else emptyList()
        composable(ruta.ruta, argumentos) { entrada ->
            val u = usuario
            if (u == null) {
                LaunchedEffect(Unit) { nav.irAlInicio(null) }
                return@composable
            }
            if (esAdmin == null || u.esAdministrador == esAdmin) contenido(u, entrada.arguments?.getLong("id") ?: 0L)
        }
    }

    NavHost(
        navController = nav,
        startDestination = Ruta.Login.ruta,
        enterTransition = { slideInHorizontally(tween(300)) { ancho -> ancho } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(300)) { ancho -> -ancho / 3 } + fadeOut(tween(300)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { ancho -> -ancho / 3 } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { ancho -> ancho } + fadeOut(tween(300)) }
    ) {
        composable(Ruta.Login.ruta) {
            PantallaIniciarSesion(
                correoRecordado,
                onIngresar = { correo, clave ->
                    controlSesion.iniciarSesion(correo, clave).also { if (it == null) nav.irAlInicio(controlSesion.usuarioActual.value) }
                },
                onIrARegistro = { nav.navigate(Ruta.Registro.ruta) }
            )
        }
        composable(Ruta.Registro.ruta) {
            PantallaCrearCuenta(
                onRegistrar = { nombre, rut, correo, telefono, clave, clave2, acepta ->
                    controlSesion.registrar(nombre, rut, correo, telefono, clave, clave2, acepta)
                },
                onCuentaCreada = { nav.irAlInicio(controlSesion.usuarioActual.value) },
                onVolver = { nav.navigateUp() }
            )
        }

        pantalla(Ruta.InicioUsuario, false) { u, _ ->
            PantallaInicioUsuario(
                u, foto, { botonMenu("Inicio") }, controlUsuario.vigentes(datosUsuario), controlUsuario.resumen(datosUsuario), datosUsuario.actualizado,
                controlUsuario.indicador(datosUsuario),
                onVerCompromiso = { nav.navigate(Ruta.Detalle.crear(it)) },
                onPagar = { irUsuario(Ruta.Pagar) },
                onHistorial = { irUsuario(Ruta.Historial) }
            )
        }
        pantalla(Ruta.Pagar, false) { _, _ ->
            PantallaPagar(controlUsuario.vigentes(datosUsuario), { botonMenu("Pagar") }) { nav.navigate(Ruta.Detalle.crear(it)) }
        }
        pantalla(Ruta.Historial, false) { _, _ ->
            PantallaHistorial(datosUsuario.pagos, datosUsuario.compromisos, { botonMenu("Historial de pagos") }) { nav.navigate(Ruta.Comprobante.crear(it)) }
        }
        pantalla(Ruta.MisReportes, false) { _, _ -> PantallaMisReportes(datosUsuario.reportes) { botonMenu("Mis reportes") } }
        pantalla(Ruta.Detalle, false) { _, id ->
            val c = datosUsuario.compromisos.firstOrNull { it.id == id } ?: return@pantalla
            PantallaDetalleCompromiso(
                c, controlUsuario.comprobanteDe(c.id), controlUsuario.reporteActivo(c.id), controlUsuario.validarPago(c),
                onPagar = { medio -> controlUsuario.pagar(c, medio) },
                onReportar = { motivo, comentario -> controlUsuario.reportar(c, motivo, comentario) },
                onVerComprobante = { nav.navigate(Ruta.Comprobante.crear(it)) { popUpTo(Ruta.InicioUsuario.ruta) } },
                onVolver = { nav.navigateUp() }
            )
        }
        pantalla(Ruta.Comprobante, false) { _, id ->
            datosUsuario.pagos.firstOrNull { it.idCompromiso == id }?.let { PantallaComprobante(it) { nav.navigateUp() } }
        }
        pantalla(Ruta.Logros, false) { _, _ ->
            PantallaLogros(controlUsuario.indicador(datosUsuario), datosUsuario.reconocimientos) { botonMenu("Mis logros") }
        }

        pantalla(Ruta.Perfil, null) { u, _ ->
            PantallaPerfil(
                u, foto, { botonMenu("Mi perfil") },
                onTomarFoto = { controlSesion.cambiarFoto(it) },
                onElegirFoto = { controlSesion.cambiarFoto(it) },
                onQuitarFoto = { controlSesion.quitarFoto() },
                onCambiarClave = { nav.navigate(Ruta.CambiarClave.ruta) },
                onCerrarSesion = cerrarSesion
            )
        }
        pantalla(Ruta.CambiarClave, null) { _, _ ->
            PantallaCambiarClave({ actual, nueva, repetida -> controlSesion.cambiarClave(actual, nueva, repetida) }, { nav.navigateUp() }) {
                nav.navigateUp()
            }
        }

        pantalla(Ruta.InicioAdmin, true) { admin, _ ->
            PantallaInicioAdministrador(
                admin, foto, { botonMenu(PESTANAS_ADMIN[pestanaAdmin]) }, pestanaAdmin, PESTANAS_ADMIN[pestanaAdmin],
                datosAdmin.reportes, datosAdmin.compromisos, controlAdmin.resumen(datosAdmin), controlAdmin::sePuedeAnular,
                onAnular = { c, motivo, comentario -> controlAdmin.anular(admin, c, motivo, comentario) },
                onVerReporte = { nav.navigate(Ruta.GestionReporte.crear(it)) },
                onRegistrar = { nav.navigate(Ruta.RegistrarCompromiso.ruta) }
            )
        }
        pantalla(Ruta.RegistrarCompromiso, true) { admin, _ ->
            PantallaRegistrarCompromiso(
                datosAdmin.empresas,
                { rut, idEmpresa, concepto, monto, fecha, cuota, total, nombre, correo, telefono ->
                    controlAdmin.registrarCompromiso(admin, rut, idEmpresa, concepto, monto, fecha, cuota, total, nombre, correo, telefono)
                },
                onRegistrado = { nav.navigateUp() },
                onVolver = { nav.navigateUp() }
            )
        }
        pantalla(Ruta.GestionReporte, true) { admin, id ->
            val r = datosAdmin.reportes.firstOrNull { it.id == id } ?: return@pantalla
            PantallaGestionarReporte(
                r, datosAdmin.compromisos.firstOrNull { it.id == r.idCompromiso },
                onTomar = { controlAdmin.tomar(admin, r) },
                onResolver = { respuesta, anular, motivo -> controlAdmin.resolver(admin, r, respuesta, anular, motivo) },
                onVolver = { nav.navigateUp() }
            )
        }
    }
}
