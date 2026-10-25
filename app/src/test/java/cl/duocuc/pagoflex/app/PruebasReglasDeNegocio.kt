package cl.duocuc.pagoflex.app

import cl.duocuc.pagoflex.app.data.UsuariosDePrueba
import cl.duocuc.pagoflex.app.model.Compromiso
import cl.duocuc.pagoflex.app.model.EstadoCompromiso
import cl.duocuc.pagoflex.app.model.IndicadorCumplimiento
import cl.duocuc.pagoflex.app.model.Pago
import cl.duocuc.pagoflex.app.model.ReglasDeNegocio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class PruebasReglasDeNegocio {
    private val empresas = mapOf(
        1L to ("Altos del Maipo" to 8500), 2L to ("Colegio Bosque Nativo" to 5000), 3L to ("Crédito Andino" to 12000),
        4L to ("Horizonte Sur" to 3000), 5L to ("Manos Abiertas" to 0), 6L to ("Club Los Halcones" to 2000),
        7L to ("Instituto Cordillera" to 6000), 8L to ("Plaza Oriente" to 7500)
    )

    private val filas = listOf("src", "app/src").map { File("$it/main/assets/datos_iniciales.sql") }
        .first { it.exists() }.readLines()

    private fun leer(rx: String) = filas.mapNotNull { Regex(rx).find(it)?.groupValues }

    private val compromisos: List<Compromiso> by lazy {
        leer("""^\((\d+), '(CP-[^']+)', (\d+), (\d+), '((?:[^']|'')*)', (NULL|\d+), (NULL|\d+), (\d+), '([^']+)', '([^']+)', """).map { g ->
            val e = empresas.getValue(g[4].toLong())
            Compromiso(
                g[1].toLong(), g[2], g[3].toLong(), "persona", "15.834.207-3", g[4].toLong(), e.first, e.second,
                g[5].replace("''", "'"), g[6].toIntOrNull(), g[7].toIntOrNull(), g[8].toInt(), LocalDate.parse(g[9]), g[10], null
            )
        }
    }

    private val pagos: List<Pago> by lazy {
        leer("""^\((?:\d+, )?'(CPR-[^']+)', (\d+), '([^']+)', (\d+), (\d+), (\d+), '([^']+)', '([^']+)'\)""").map { g ->
            Pago(0, g[1], g[2].toLong(), g[3], g[4].toInt(), g[5].toInt(), g[6].toInt(), g[7], g[8],
                "", "", null, LocalDate.of(2026, 1, 1), "", "", "")
        }
    }

    private fun de(idUsuario: Long) = compromisos.filter { it.idUsuario == idUsuario }
    private fun pagosDe(idUsuario: Long): List<Pago> {
        val ids = de(idUsuario).map { it.id }.toSet()
        return pagos.filter { it.idCompromiso in ids }
    }
    private fun c(id: Long) = compromisos.first { it.id == id }

    @Test fun rutsDelAnexoSonValidos() = UsuariosDePrueba.usuarios.forEach { assertTrue(it.rut, ReglasDeNegocio.rutValido(it.rut)) }

    @Test fun rutConDigitoMaloSeRechaza() = assertFalse(ReglasDeNegocio.rutValido("15.834.207-4"))

    @Test fun rutSeMuestraOculto() = assertEquals("15.***.***-3", ReglasDeNegocio.ocultarRut("15.834.207-3"))

    @Test fun rutSeFormateaMientrasSeEscribe() {
        assertEquals("158-3", ReglasDeNegocio.formatearRut("1583"))
        assertEquals("15.834-2", ReglasDeNegocio.formatearRut("158342"))
        assertEquals("15.834.207-3", ReglasDeNegocio.formatearRut("158342073"))
    }

    @Test fun rutConKSeFormateaYSoloAceptaKAlFinal() {
        assertEquals("16.248.317-K", ReglasDeNegocio.formatearRut("16248317k"))
        assertEquals("16.248.317-K", ReglasDeNegocio.formatearRut("16.248.317-K"))
        assertEquals("1", ReglasDeNegocio.formatearRut("K1"))
    }

    @Test fun montoConPuntoDeMiles() = assertEquals("$ 520.000", ReglasDeNegocio.monto(520000))

    @Test fun fechaImposibleSeRechaza() = assertNull(ReglasDeNegocio.leerFecha("31-02-2026"))

    @Test fun laContrasenaNoSeGuardaTalCual() {
        val h = ReglasDeNegocio.hash("ana@duoc.cl", "pago123")
        assertFalse(h.contains("pago123"))
        assertEquals(h, ReglasDeNegocio.hash("ANA@duoc.cl", "pago123"))
        assertEquals(64, h.length)
    }

    @Test fun compromisoVencidoSumaRecargoUnaVez() {
        val cuotaClub = c(102)
        assertEquals(EstadoCompromiso.VENCIDO, cuotaClub.estado)
        assertEquals(1, cuotaClub.diasAtraso)
        assertEquals(17000, cuotaClub.total)
    }

    @Test fun textoDeVencimientoEnLasListas() {
        assertEquals("Vencido hace 1 día", ReglasDeNegocio.textoVencimiento(c(102)))
        assertEquals("Vence en 9 días · 15-10", ReglasDeNegocio.textoVencimiento(c(103)))
        assertEquals("8 de 24", c(105).cuota)
    }

    @Test fun venceALas2359DelDiaIndicado() {
        val arriendo = c(101)
        assertEquals(EstadoCompromiso.PENDIENTE, arriendo.estado)
        assertEquals(520000, arriendo.total)
    }

    @Test fun resumenDelMesDeCamilaEs633400() {
        val porPagar = de(1).filter { ReglasDeNegocio.esPagable(it) && it.vencimiento.monthValue == 10 && it.vencimiento.year == 2026 }
        assertEquals("$ 633.400", ReglasDeNegocio.monto(porPagar.sumOf { it.total }))
    }

    @Test fun noSePagaLaCuota15AntesQueLa14() {
        val mensaje = ReglasDeNegocio.validarPago(c(107), de(2))
        assertNotNull(mensaje)
        assertTrue(mensaje!!.contains("cuota 14 de 36"))
        assertNull(ReglasDeNegocio.validarPago(c(106), de(2)))
    }

    @Test fun entreEmpresasDistintasNoHayOrden() = assertNull(ReglasDeNegocio.validarPago(c(101), de(1)))

    @Test fun noSePagaLoAnuladoNiLoEnRevisionNiLoPagado() {
        assertNotNull(ReglasDeNegocio.validarPago(c(111), compromisos))
        assertNotNull(ReglasDeNegocio.validarPago(c(109), compromisos))
        assertNotNull(ReglasDeNegocio.validarPago(c(104), compromisos))
    }

    @Test fun indicadorDeCamilaEs917AlDia() {
        val i = ReglasDeNegocio.indicador(de(1), pagosDe(1))
        assertTrue(i is IndicadorCumplimiento.Calculado)
        i as IndicadorCumplimiento.Calculado
        assertEquals(33, i.aTiempo)
        assertEquals(36, i.considerados)
        assertEquals("91,7 %", ReglasDeNegocio.porcentaje(i.porcentaje))
        assertEquals("Al día", i.categoria)
    }

    @Test fun conMenosDeTresCompromisosEsHistorialInsuficiente() =
        assertTrue(ReglasDeNegocio.indicador(de(8), pagosDe(8)) is IndicadorCumplimiento.HistorialInsuficiente)

    @Test fun pagadoDespuesDelVencimientoNoCuentaATiempo() {
        val pagadoTarde = c(102).copy(estadoGuardado = "PAGADO")
        val pago = Pago(1, "X", 102, "2026-10-06 10:00", 15000, 2000, 17000, "m", "App",
            "", "", null, LocalDate.of(2026, 10, 5), "", "", "")
        assertFalse(ReglasDeNegocio.fueATiempo(pagadoTarde, listOf(pago)))
    }

    @Test fun camilaGanaReconocimientosPorPagarATiempo() {
        val logros = ReglasDeNegocio.logrosGanados(de(1), pagosDe(1))
        assertTrue("Primer pago a tiempo" in logros)
        assertTrue("Cinco pagos a tiempo" in logros)
    }

    @Test fun pagarATiempoDesdeLaAppDaUnReconocimientoNuevo() {
        assertFalse("Pago a tiempo desde la app" in ReglasDeNegocio.logrosGanados(de(1), pagosDe(1)))
        val pagado = de(1).map { if (it.id == 101L) it.copy(estadoGuardado = "PAGADO") else it }
        val pago = Pago(999, "CPR-000999", 101, "2026-10-06 10:00", 520000, 0, 520000, "Tarjeta de débito", "App",
            "", "", null, LocalDate.of(2026, 10, 10), "", "", "")
        assertTrue("Pago a tiempo desde la app" in ReglasDeNegocio.logrosGanados(pagado, pagosDe(1) + pago))
    }

    @Test fun sinPagosNoHayReconocimientos() =
        assertTrue(ReglasDeNegocio.logrosGanados(de(8).map { it.copy(estadoGuardado = "PENDIENTE") }, emptyList()).isEmpty())

    @Test fun reporteAbiertoHace4DiasHabilesEstaAtrasado() = assertEquals(4, ReglasDeNegocio.diasHabilesDesde("2026-09-30 12:00"))

    @Test fun losFinesDeSemanaNoCuentan() = assertEquals(2, ReglasDeNegocio.diasHabilesDesde("2026-10-03 09:30"))
}
