package cl.duocuc.pagoflex.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duocuc.pagoflex.app.data.*
import cl.duocuc.pagoflex.app.model.ReglasDeNegocio
import cl.duocuc.pagoflex.app.model.Usuario
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ControlDeSesion(application: Application) : AndroidViewModel(application) {
    private val db = BaseDeDatos.obtener(application)
    private val sesion = RecordarCorreo(application)
    private val fotos = FotoDePerfil(application)

    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    private val _foto = MutableStateFlow<ImageBitmap?>(null)
    val foto: StateFlow<ImageBitmap?> = _foto.asStateFlow()

    private val _correoRecordado = MutableStateFlow("")
    val correoRecordado: StateFlow<String> = _correoRecordado.asStateFlow()

    private val _listo = MutableStateFlow(false)
    val listo: StateFlow<Boolean> = _listo.asStateFlow()

    init {
        viewModelScope.launch {
            _correoRecordado.value = withContext(Dispatchers.IO) { sesion.leerCorreo() }
            _listo.value = true
        }
    }

    suspend fun iniciarSesion(correo: String, clave: String): String? {
        if (correo.isBlank() || clave.isBlank()) return "Ingresa tu correo y tu contraseña"
        val usuario = withContext(Dispatchers.IO) { db.login(correo, clave) } ?: return "Correo o contraseña incorrectos"
        entrar(usuario)
        return null
    }

    private suspend fun entrar(usuario: Usuario) {
        withContext(Dispatchers.IO) { sesion.guardarCorreo(usuario.correo) }
        _correoRecordado.value = usuario.correo
        _usuarioActual.value = usuario
        _foto.value = withContext(Dispatchers.IO) { fotos.leer(usuario.id)?.asImageBitmap() }
    }

    suspend fun registrar(
        nombre: String, rut: String, correo: String, telefono: String,
        clave: String, clave2: String, aceptaDatos: Boolean
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val errores = ReglasDeNegocio.validarPersona(nombre, correo, telefono, telefonoObligatorio = false)
        val rutValido = ReglasDeNegocio.rutValido(rut)
        val rutExiste = rutValido && db.existeRut(rut)
        if (!rutValido) errores["rut"] = ReglasDeNegocio.RUT_INVALIDO
        else if (rutExiste && db.cuentaActiva(rut)) errores["rut"] = "Ya existe una cuenta con este RUT"
        if ("correo" !in errores && (if (rutValido) db.correoDeOtro(correo, rut) else db.existeCorreo(correo))) {
            errores["correo"] = "Ya existe una cuenta con este correo"
        }
        if (clave.length < 6) errores["clave"] = "Mínimo 6 caracteres"
        if (clave2 != clave) errores["clave2"] = "Las contraseñas no coinciden"
        if (!aceptaDatos) errores["datos"] = "Debes aceptar el uso de tus datos"
        if (errores.isNotEmpty()) return@withContext errores

        val ok = if (rutExiste) db.activarCuenta(nombre, rut, correo, telefono, clave)
                 else db.registrarUsuario(nombre, rut, correo, telefono, clave) != -1L
        val usuario = if (ok) db.login(correo, clave) else null
        if (usuario == null) return@withContext mapOf("general" to "No se pudo crear la cuenta. Inténtalo de nuevo.")
        entrar(usuario)
        emptyMap()
    }

    suspend fun cambiarClave(actual: String, nueva: String, repetida: String): Map<String, String> {
        val usuario = _usuarioActual.value ?: return mapOf("general" to "Vuelve a iniciar sesión")
        val errores = mutableMapOf<String, String>()
        if (actual.isBlank()) errores["actual"] = "Escribe tu contraseña actual"
        when {
            nueva.length < 6 -> errores["nueva"] = "Mínimo 6 caracteres"
            nueva == actual -> errores["nueva"] = "Debe ser distinta a la actual"
        }
        if (repetida != nueva) errores["repetida"] = "Las contraseñas no coinciden"
        if (errores.isNotEmpty()) return errores
        val ok = withContext(Dispatchers.IO) { db.cambiarClave(usuario.id, usuario.correo, actual, nueva) }
        return if (ok) emptyMap() else mapOf("actual" to "La contraseña actual no es correcta")
    }

    fun cambiarFoto(foto: Bitmap) = guardarFoto { idUsuario -> fotos.guardar(idUsuario, foto) }

    fun cambiarFoto(uri: Uri) = guardarFoto { idUsuario -> fotos.guardar(idUsuario, uri) }

    private fun guardarFoto(guardar: (Long) -> Bitmap?) {
        val usuario = _usuarioActual.value ?: return
        viewModelScope.launch {
            val nueva = withContext(Dispatchers.IO) { guardar(usuario.id) }
            if (nueva != null) _foto.value = nueva.asImageBitmap()
        }
    }

    fun quitarFoto() {
        val usuario = _usuarioActual.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { fotos.borrar(usuario.id) }
            _foto.value = null
        }
    }

    fun cerrarSesion() {
        _usuarioActual.value = null
        _foto.value = null
    }
}
