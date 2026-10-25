# PagoFlex

Aplicación móvil Android que reúne en un solo lugar los compromisos de pago de una persona con distintas empresas (arriendos, créditos, cuotas sociales, colegiaturas), le permite pagarlos de forma simulada y reportar problemas, y entrega a un administrador las herramientas para gestionar reportes y compromisos.

Proyecto de la asignatura **DSY1105 – Desarrollo de Aplicaciones Móviles** (Duoc UC), basado en el caso PagoFlex.

## Integrantes

- Matias Coronado
- [Nombre integrante 2]

## Funcionalidades implementadas

**Acceso**
- Inicio de sesión y creación de cuenta con validación por campo (RUT con dígito verificador, correo, teléfono, contraseña).
- Recuerda el último correo usado (SharedPreferences).

**Usuario**
- Resumen del mes: total por pagar, próximo pago, atrasos e indicador de cumplimiento.
- Compromisos agrupados por vencimiento, con recargo por atraso y orden de pago obligatorio por empresa.
- Pago simulado con confirmación y comprobante.
- Historial de pagos agrupado por mes, con filtros por empresa y mes.
- Reportes de problemas y logros por pagar a tiempo.
- Perfil con foto desde la cámara o la galería y cambio de contraseña.

**Administrador**
- Gestión de reportes (tomar, responder y resolver) con indicador de reportes atrasados.
- Búsqueda de compromisos por RUT, nombre o folio, registro de compromisos nuevos y anulación con motivo.
- Resumen de cobranza del mes.

**Requisitos de la evaluación**

| Requisito | Implementación |
|---|---|
| Diseño visual | Material 3 con tema propio |
| Formularios validados | Mensajes de error por campo, validados desde la lógica |
| Navegación | Navigation Compose con control de acceso por rol |
| Gestión de estado | ViewModel + StateFlow (arquitectura MVVM) |
| Almacenamiento local | SQLite, SharedPreferences y archivo de imagen |
| Recursos nativos | Cámara y galería de fotos |
| Animaciones | Montos, barra de cuotas, mensajes de error y confirmación de pago |

## Tecnologías

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · ViewModel + StateFlow · Coroutines · SQLite · SharedPreferences · JUnit

## Estructura

```
app/src/main/java/cl/duocuc/pagoflex/app/
├── data/        Base de datos SQLite, preferencias y foto de perfil
├── model/       Clases de datos y reglas de negocio
├── viewmodel/   Lógica y estado de cada rol (MVVM)
├── navigation/  Rutas y navegación entre pantallas
└── ui/          Tema, componentes y pantallas
```

## Pasos para ejecutar

1. Instalar IntelliJ IDEA o Android Studio con el plugin de Android y el Android SDK.
2. Clonar el repositorio: `git clone https://github.com/MatiasCoronado2023/PagoFlex.git`
3. Abrir la carpeta del proyecto (**File → Open**) y esperar la sincronización de Gradle.
4. Crear o elegir un emulador (por ejemplo Medium Phone, API 37) o conectar un teléfono con depuración USB.
5. Presionar **Run**.

Las pruebas unitarias se ejecutan con clic derecho en `app/src/test` → **Run Tests**.

## Cuentas de prueba

Contraseña: `pago123`

| Rol | Correo |
|---|---|
| Usuario | crojas@correo.ejemplo |
| Usuario | lmunoz@correo.ejemplo |
| Administrador | alagos@pagoflex.ejemplo |
