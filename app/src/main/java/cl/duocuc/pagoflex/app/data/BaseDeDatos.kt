package cl.duocuc.pagoflex.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.duocuc.pagoflex.app.model.*
import java.time.LocalDate

class BaseDeDatos private constructor(private val contexto: Context) :
    SQLiteOpenHelper(contexto, "pagoflex.db", null, 2) {
    companion object {
        private var instancia: BaseDeDatos? = null

        @Synchronized
        fun obtener(context: Context): BaseDeDatos {
            if (instancia == null) instancia = BaseDeDatos(context.applicationContext)
            return instancia!!
        }

        private val TABLAS = listOf(
            """CREATE TABLE roles (id_rol INTEGER PRIMARY KEY AUTOINCREMENT, nombre_rol TEXT NOT NULL UNIQUE)""",
            """CREATE TABLE usuarios (id_usuario INTEGER PRIMARY KEY AUTOINCREMENT, rut TEXT NOT NULL UNIQUE,
                nombre TEXT NOT NULL, correo TEXT NOT NULL UNIQUE, telefono TEXT, clave_hash TEXT NOT NULL,
                id_rol INTEGER NOT NULL REFERENCES roles(id_rol))""",
            """CREATE TABLE empresas (id_empresa INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL UNIQUE,
                recargo INTEGER NOT NULL DEFAULT 0 CHECK (recargo >= 0))""",
            """CREATE TABLE compromisos (id_compromiso INTEGER PRIMARY KEY AUTOINCREMENT, folio TEXT NOT NULL UNIQUE,
                id_usuario INTEGER NOT NULL REFERENCES usuarios(id_usuario),
                id_empresa INTEGER NOT NULL REFERENCES empresas(id_empresa),
                concepto TEXT NOT NULL, numero_cuota INTEGER, total_cuotas INTEGER,
                monto INTEGER NOT NULL CHECK (monto > 0), fecha_vencimiento TEXT NOT NULL,
                estado TEXT NOT NULL DEFAULT 'PENDIENTE', motivo_anulacion TEXT,
                id_admin_registro INTEGER REFERENCES usuarios(id_usuario), fecha_registro TEXT,
                id_admin_anulacion INTEGER REFERENCES usuarios(id_usuario), fecha_anulacion TEXT)""",
            """CREATE TABLE pagos (id_pago INTEGER PRIMARY KEY AUTOINCREMENT, numero_comprobante TEXT NOT NULL UNIQUE,
                id_compromiso INTEGER NOT NULL UNIQUE REFERENCES compromisos(id_compromiso), fecha_hora TEXT NOT NULL,
                monto_original INTEGER NOT NULL, recargo INTEGER NOT NULL, total INTEGER NOT NULL,
                medio TEXT NOT NULL, canal TEXT NOT NULL)""",
            """CREATE TABLE reportes (id_reporte INTEGER PRIMARY KEY AUTOINCREMENT, numero TEXT NOT NULL UNIQUE,
                id_compromiso INTEGER NOT NULL REFERENCES compromisos(id_compromiso), motivo TEXT NOT NULL,
                comentario TEXT, fecha TEXT NOT NULL, estado TEXT NOT NULL DEFAULT 'ABIERTO',
                id_admin INTEGER REFERENCES usuarios(id_usuario), fecha_toma TEXT, respuesta TEXT, fecha_resolucion TEXT)""",
            """CREATE TABLE reconocimientos (id_reconocimiento INTEGER PRIMARY KEY AUTOINCREMENT,
                id_usuario INTEGER NOT NULL REFERENCES usuarios(id_usuario), nombre TEXT NOT NULL, fecha TEXT NOT NULL,
                UNIQUE (id_usuario, nombre))"""
        )

        private const val SQL_COMPROMISOS =
            """SELECT c.*, u.nombre AS nombre_usuario, u.rut AS rut_usuario, e.nombre AS nombre_empresa,
            e.recargo AS recargo_empresa FROM compromisos c
            INNER JOIN usuarios u ON c.id_usuario = u.id_usuario INNER JOIN empresas e ON c.id_empresa = e.id_empresa"""

        private const val SQL_PAGOS =
            """SELECT p.*, c.folio, c.concepto, c.numero_cuota, c.total_cuotas, c.fecha_vencimiento,
            e.nombre AS nombre_empresa, u.nombre AS nombre_usuario, u.rut AS rut_usuario FROM pagos p
            INNER JOIN compromisos c ON p.id_compromiso = c.id_compromiso
            INNER JOIN empresas e ON c.id_empresa = e.id_empresa INNER JOIN usuarios u ON c.id_usuario = u.id_usuario"""

        private const val SQL_REPORTES =
            """SELECT r.*, c.folio, c.concepto, e.nombre AS nombre_empresa, u.nombre AS nombre_usuario,
            a.nombre AS nombre_admin FROM reportes r
            INNER JOIN compromisos c ON r.id_compromiso = c.id_compromiso
            INNER JOIN empresas e ON c.id_empresa = e.id_empresa INNER JOIN usuarios u ON c.id_usuario = u.id_usuario
            LEFT JOIN usuarios a ON r.id_admin = a.id_usuario"""
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        TABLAS.forEach { db.execSQL(it) }
        db.execSQL("INSERT INTO roles (id_rol, nombre_rol) VALUES (1, 'Administrador'), (2, 'Usuario')")
        UsuariosDePrueba.usuarios.forEach { u ->
            db.insert("usuarios", null, valores("id_usuario" to u.id, "rut" to u.rut, "nombre" to u.nombre,
                "correo" to u.correo, "telefono" to u.telefono, "clave_hash" to ReglasDeNegocio.hash(u.correo, UsuariosDePrueba.CLAVE_DEMO),
                "id_rol" to u.idRol))
        }
        contexto.assets.open("datos_iniciales.sql").bufferedReader().use { it.readText() }
            .split(Regex(";\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }.forEach { db.execSQL(it) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        listOf("reconocimientos", "reportes", "pagos", "compromisos", "empresas", "usuarios", "roles")
            .forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }

    fun login(correo: String, clave: String): Usuario? =
        consultarUsuarios("WHERE correo = ? AND clave_hash = ?", correo.trim().lowercase(), ReglasDeNegocio.hash(correo, clave)).firstOrNull()

    fun existeCorreo(correo: String) = contar("SELECT COUNT(*) FROM usuarios WHERE correo = ?", correo.trim().lowercase()) > 0

    fun existeRut(rut: String) = contar("SELECT COUNT(*) FROM usuarios WHERE rut = ?", ReglasDeNegocio.formatearRut(rut)) > 0

    fun cuentaActiva(rut: String) =
        contar("SELECT COUNT(*) FROM usuarios WHERE rut = ? AND clave_hash <> ''", ReglasDeNegocio.formatearRut(rut)) > 0

    fun correoDeOtro(correo: String, rut: String) =
        contar("SELECT COUNT(*) FROM usuarios WHERE correo = ? AND rut <> ?", correo.trim().lowercase(), ReglasDeNegocio.formatearRut(rut)) > 0

    fun registrarUsuario(nombre: String, rut: String, correo: String, telefono: String?, clave: String?): Long =
        writableDatabase.insert("usuarios", null, valores(
            "rut" to ReglasDeNegocio.formatearRut(rut), "nombre" to nombre.trim(), "correo" to correo.trim().lowercase(),
            "telefono" to telefono?.trim()?.ifBlank { null }, "clave_hash" to (clave?.let { ReglasDeNegocio.hash(correo, it) } ?: ""),
            "id_rol" to RolUsuario.USUARIO))

    fun activarCuenta(nombre: String, rut: String, correo: String, telefono: String?, clave: String): Boolean =
        writableDatabase.update("usuarios", valores("nombre" to nombre.trim(), "correo" to correo.trim().lowercase(),
            "telefono" to telefono?.trim()?.ifBlank { null }, "clave_hash" to ReglasDeNegocio.hash(correo, clave)),
            "rut = ? AND clave_hash = ''", arrayOf(ReglasDeNegocio.formatearRut(rut))) == 1

    fun cambiarClave(idUsuario: Long, correo: String, actual: String, nueva: String): Boolean =
        writableDatabase.update("usuarios", valores("clave_hash" to ReglasDeNegocio.hash(correo, nueva)),
            "id_usuario = ? AND clave_hash = ?", arrayOf(idUsuario.toString(), ReglasDeNegocio.hash(correo, actual))) == 1

    fun buscarUsuario(id: Long): Usuario? = consultarUsuarios("WHERE id_usuario = ?", id).firstOrNull()

    fun buscarUsuarioPorRut(rut: String): Usuario? = consultarUsuarios("WHERE rut = ?", ReglasDeNegocio.formatearRut(rut)).firstOrNull()

    fun listarEmpresas(): List<Empresa> = consultar("SELECT * FROM empresas ORDER BY nombre") {
        Empresa(it.long("id_empresa"), it.texto("nombre"), it.entero("recargo"))
    }

    fun compromisosDeUsuario(idUsuario: Long) =
        consultarCompromisos("WHERE c.id_usuario = ? ORDER BY c.fecha_vencimiento", idUsuario)

    fun listarCompromisos() = consultarCompromisos("ORDER BY c.fecha_vencimiento DESC")

    fun buscarCompromiso(id: Long): Compromiso? = consultarCompromisos("WHERE c.id_compromiso = ?", id).firstOrNull()

    fun existeCompromiso(idUsuario: Long, idEmpresa: Long, concepto: String, cuota: Int?): Boolean =
        contar("""SELECT COUNT(*) FROM compromisos WHERE id_usuario = ? AND id_empresa = ?
            AND LOWER(concepto) = LOWER(?) AND IFNULL(numero_cuota, 0) = ?""", idUsuario, idEmpresa, concepto.trim(), cuota ?: 0) > 0

    fun insertarCompromiso(
        idUsuario: Long, idEmpresa: Long, concepto: String, numeroCuota: Int?, totalCuotas: Int?,
        monto: Int, vencimiento: LocalDate, idAdmin: Long
    ): Long {
        val id = siguienteId("compromisos", "id_compromiso")
        val ok = transaccion {
            writableDatabase.insert("compromisos", null, valores("id_compromiso" to id, "folio" to "CP-%d-%04d".format(ReglasDeNegocio.HOY.year, id),
                "id_usuario" to idUsuario, "id_empresa" to idEmpresa, "concepto" to concepto.trim(),
                "numero_cuota" to numeroCuota, "total_cuotas" to totalCuotas, "monto" to monto,
                "fecha_vencimiento" to vencimiento.toString(), "estado" to "PENDIENTE",
                "id_admin_registro" to idAdmin, "fecha_registro" to ReglasDeNegocio.ahora())) != -1L
        }
        return if (ok) id else -1L
    }

    fun anularCompromiso(id: Long, motivo: String, idAdmin: Long): Boolean =
        writableDatabase.update("compromisos", anulacion(motivo, idAdmin),
            "id_compromiso = ? AND estado IN ('PENDIENTE', 'EN_REVISION')", arrayOf(id.toString())) == 1

    fun registrarPago(idCompromiso: Long, medio: String): Pago? {
        val c = buscarCompromiso(idCompromiso) ?: return null
        if (!ReglasDeNegocio.esPagable(c)) return null
        val id = siguienteId("pagos", "id_pago")
        val ok = transaccion {
            val pagoGuardado = writableDatabase.insert("pagos", null, valores("id_pago" to id, "numero_comprobante" to "CPR-%06d".format(id),
                "id_compromiso" to idCompromiso, "fecha_hora" to ReglasDeNegocio.ahora(), "monto_original" to c.monto,
                "recargo" to c.recargo, "total" to c.total, "medio" to medio, "canal" to "App")) != -1L
            pagoGuardado && cambiarEstado(idCompromiso, "PENDIENTE", "PAGADO")
        }
        return if (ok) consultarPagos("WHERE p.id_compromiso = ?", idCompromiso).firstOrNull() else null
    }

    fun pagosDeUsuario(idUsuario: Long) = consultarPagos("WHERE c.id_usuario = ? ORDER BY p.fecha_hora DESC", idUsuario)

    fun listarPagos() = consultarPagos("ORDER BY p.fecha_hora DESC")

    fun reportesDeUsuario(idUsuario: Long) = consultarReportes("WHERE c.id_usuario = ? ORDER BY r.fecha DESC", idUsuario)

    fun listarReportes() = consultarReportes("ORDER BY r.fecha")

    fun crearReporte(idCompromiso: Long, motivo: String, comentario: String?): Long {
        if (consultarReportes("WHERE r.id_compromiso = ? AND r.estado <> 'RESUELTO'", idCompromiso).isNotEmpty()) return -1L
        val id = siguienteId("reportes", "id_reporte")
        val ok = transaccion {
            val reporteGuardado = writableDatabase.insert("reportes", null, valores("id_reporte" to id, "numero" to "REP-%04d".format(id),
                "id_compromiso" to idCompromiso, "motivo" to motivo, "comentario" to comentario?.trim()?.ifBlank { null },
                "fecha" to ReglasDeNegocio.ahora(), "estado" to "ABIERTO")) != -1L
            reporteGuardado && cambiarEstado(idCompromiso, "PENDIENTE", "EN_REVISION")
        }
        return if (ok) id else -1L
    }

    fun tomarReporte(idReporte: Long, idAdmin: Long): Boolean =
        writableDatabase.update("reportes", valores("estado" to "EN_GESTION", "id_admin" to idAdmin, "fecha_toma" to ReglasDeNegocio.ahora()),
            "id_reporte = ? AND estado = 'ABIERTO'", arrayOf(idReporte.toString())) == 1

    fun resolverReporte(idReporte: Long, idAdmin: Long, respuesta: String, anular: Boolean, motivoAnulacion: String?): Boolean {
        val reporte = consultarReportes("WHERE r.id_reporte = ?", idReporte).firstOrNull() ?: return false
        return transaccion {
            val resuelto = writableDatabase.update("reportes",
                valores("estado" to "RESUELTO", "respuesta" to respuesta.trim(), "fecha_resolucion" to ReglasDeNegocio.ahora()),
                "id_reporte = ? AND estado = 'EN_GESTION' AND id_admin = ?", arrayOf(idReporte.toString(), idAdmin.toString())) == 1
            val compromiso = writableDatabase.update("compromisos",
                if (anular) anulacion(motivoAnulacion, idAdmin) else valores("estado" to "PENDIENTE"),
                "id_compromiso = ? AND estado = 'EN_REVISION'", arrayOf(reporte.idCompromiso.toString())) == 1
            resuelto && (compromiso || buscarCompromiso(reporte.idCompromiso)?.estadoGuardado == "ANULADO")
        }
    }

    fun reconocimientosDeUsuario(idUsuario: Long): List<LogroObtenido> =
        consultar("SELECT nombre, fecha FROM reconocimientos WHERE id_usuario = ? ORDER BY fecha", idUsuario) {
            LogroObtenido(it.texto("nombre"), it.texto("fecha"))
        }

    fun otorgarReconocimiento(idUsuario: Long, nombre: String) {
        writableDatabase.insertWithOnConflict("reconocimientos", null,
            valores("id_usuario" to idUsuario, "nombre" to nombre, "fecha" to ReglasDeNegocio.ahora()), SQLiteDatabase.CONFLICT_IGNORE)
    }

    private fun valores(vararg pares: Pair<String, Any?>) = ContentValues().apply {
        pares.forEach { (columna, valor) ->
            when (valor) {
                null -> putNull(columna)
                is Int -> put(columna, valor)
                is Long -> put(columna, valor)
                else -> put(columna, valor.toString())
            }
        }
    }

    private fun anulacion(motivo: String?, idAdmin: Long) = valores("estado" to "ANULADO", "motivo_anulacion" to motivo,
        "id_admin_anulacion" to idAdmin, "fecha_anulacion" to ReglasDeNegocio.ahora())

    private fun cambiarEstado(idCompromiso: Long, desde: String, hacia: String) =
        writableDatabase.update("compromisos", valores("estado" to hacia), "id_compromiso = ? AND estado = ?",
            arrayOf(idCompromiso.toString(), desde)) == 1

    private fun siguienteId(tabla: String, columna: String) = contar("SELECT IFNULL(MAX($columna), 0) + 1 FROM $tabla").toLong()

    private fun transaccion(bloque: () -> Boolean): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val ok = bloque()
            if (ok) db.setTransactionSuccessful()
            return ok
        } finally {
            db.endTransaction()
        }
    }

    private fun contar(sql: String, vararg args: Any): Int =
        readableDatabase.rawQuery(sql, args.map { it.toString() }.toTypedArray()).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    private fun <T> consultar(sql: String, vararg args: Any, fila: (Cursor) -> T): List<T> =
        readableDatabase.rawQuery(sql, args.map { it.toString() }.toTypedArray()).use { c ->
            buildList { while (c.moveToNext()) add(fila(c)) }
        }

    private fun consultarUsuarios(filtro: String, vararg args: Any) = consultar("SELECT * FROM usuarios $filtro", *args) {
        Usuario(it.long("id_usuario"), it.texto("rut"), it.texto("nombre"), it.texto("correo"),
            it.textoONull("telefono"), it.long("id_rol"))
    }

    private fun consultarCompromisos(filtro: String, vararg args: Any) = consultar("$SQL_COMPROMISOS $filtro", *args) {
        Compromiso(it.long("id_compromiso"), it.texto("folio"), it.long("id_usuario"), it.texto("nombre_usuario"),
            it.texto("rut_usuario"), it.long("id_empresa"), it.texto("nombre_empresa"), it.entero("recargo_empresa"),
            it.texto("concepto"), it.enteroONull("numero_cuota"), it.enteroONull("total_cuotas"), it.entero("monto"),
            LocalDate.parse(it.texto("fecha_vencimiento")), it.texto("estado"), it.textoONull("motivo_anulacion"))
    }

    private fun consultarPagos(filtro: String, vararg args: Any) = consultar("$SQL_PAGOS $filtro", *args) {
        val n = it.enteroONull("numero_cuota")
        val t = it.enteroONull("total_cuotas")
        Pago(it.long("id_pago"), it.texto("numero_comprobante"), it.long("id_compromiso"), it.texto("fecha_hora"),
            it.entero("monto_original"), it.entero("recargo"), it.entero("total"), it.texto("medio"), it.texto("canal"),
            it.texto("folio"), it.texto("concepto"), if (n != null && t != null) "$n de $t" else null,
            LocalDate.parse(it.texto("fecha_vencimiento")), it.texto("nombre_empresa"), it.texto("nombre_usuario"),
            it.texto("rut_usuario"))
    }

    private fun consultarReportes(filtro: String, vararg args: Any) = consultar("$SQL_REPORTES $filtro", *args) {
        Reporte(it.long("id_reporte"), it.texto("numero"), it.long("id_compromiso"), it.texto("folio"), it.texto("concepto"),
            it.texto("nombre_empresa"), it.texto("nombre_usuario"), it.texto("motivo"), it.textoONull("comentario"),
            it.texto("fecha"), EstadoReporte.valueOf(it.texto("estado")), it.textoONull("nombre_admin"),
            it.textoONull("respuesta"), it.textoONull("fecha_resolucion"))
    }

    private fun Cursor.texto(col: String): String = getString(getColumnIndexOrThrow(col))
    private fun Cursor.textoONull(col: String): String? = getColumnIndexOrThrow(col).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.long(col: String): Long = getLong(getColumnIndexOrThrow(col))
    private fun Cursor.entero(col: String): Int = getInt(getColumnIndexOrThrow(col))
    private fun Cursor.enteroONull(col: String): Int? = getColumnIndexOrThrow(col).let { if (isNull(it)) null else getInt(it) }
}
