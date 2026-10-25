package cl.duocuc.pagoflex.app.data

data class UsuarioDePrueba(val id: Long, val rut: String, val nombre: String, val correo: String, val telefono: String?, val idRol: Long)

object UsuariosDePrueba {
    const val CLAVE_DEMO = "pago123"

    val usuarios = listOf(
        UsuarioDePrueba(1, "15.834.207-3", "Camila Andrea Rojas Peña", "crojas@correo.ejemplo", "+56 9 0000 0101", 2),
        UsuarioDePrueba(2, "12.476.318-5", "Luis Alberto Muñoz Tapia", "lmunoz@correo.ejemplo", "+56 9 0000 0102", 2),
        UsuarioDePrueba(3, "6.893.541-5", "Rosa Elena Contreras Díaz", "rcontreras@correo.ejemplo", "+56 9 0000 0103", 2),
        UsuarioDePrueba(4, "21.045.873-5", "Matías Ignacio Soto Vergara", "msoto@correo.ejemplo", "+56 9 0000 0104", 2),
        UsuarioDePrueba(5, "17.562.904-1", "Fernanda Paz Godoy Lagos", "fgodoy@correo.ejemplo", "+56 9 0000 0105", 2),
        UsuarioDePrueba(6, "10.318.265-4", "Héctor Manuel Pizarro Campos", "hpizarro@correo.ejemplo", "+56 9 0000 0106", 2),
        UsuarioDePrueba(7, "19.287.430-0", "Javiera Constanza Reyes Molina", "jreyes@correo.ejemplo", "+56 9 0000 0107", 2),
        UsuarioDePrueba(8, "18.704.396-4", "Benjamín Esteban Araya Fuentes", "baraya@correo.ejemplo", "+56 9 0000 0108", 2),
        UsuarioDePrueba(9, "16.248.317-K", "Andrés Felipe Lagos Meza", "alagos@pagoflex.ejemplo", null, 1),
        UsuarioDePrueba(10, "18.352.091-1", "Daniela Sofía Ortiz Bravo", "dortiz@pagoflex.ejemplo", null, 1),
        UsuarioDePrueba(12, "17.781.435-0", "Valentina Ignacia Paredes Silva", "vparedes@pagoflex.ejemplo", null, 1),
        UsuarioDePrueba(13, "19.564.028-9", "Tomás Alejandro Vidal Rojas", "tvidal@pagoflex.ejemplo", null, 1)
    )
}
