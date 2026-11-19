# ODAPAS Report — Proyecto completo (backend + frontend)

Sistema para que la ciudadanía reporte **fugas, falta de agua y baja presión**, y que ODAPAS los
revise, asigne a una cuadrilla, repare y cierre, con aviso al ciudadano en cada paso.

| Parte | Tecnología | Carpeta en tu PC |
|---|---|---|
| Frontend | Angular 17 + Leaflet (mapas) | `aguapp\src` |
| Backend (API REST) | Spring Boot 3.3.5, Java 21, JDBC | `aguapp\api-odapas-reporte` |
| Base de datos | PostgreSQL 16 (`odapas_report`) | `api-odapas-reporte\database` |

---

## 1. Cómo encenderlo (2 terminales)

**Terminal 1 — API** (pide la contraseña de PostgreSQL):
```powershell
cd C:\Users\LENOVO\Desktop\aguapp\api-odapas-reporte
.\iniciar-api.ps1
```
Al arrancar, la API agrega sola lo que le falta a la base (folios automáticos, bitácora y nuevos tipos de aviso).
**No borra ni cambia tus datos.**

**Terminal 2 — Frontend:**
```powershell
cd C:\Users\LENOVO\Desktop\aguapp
npx ng serve
```
Abre **http://localhost:4200**. Para la práctica en red sigue usando `npm run start:red`.

---

## 2. Los tres tipos de usuario

### Ciudadano
1. **Crear cuenta** (nombre, teléfono, correo, contraseña, domicilio y aceptación del aviso de privacidad).
2. **Verificar correo** — es obligatorio para reportar (lo exige la base de datos). Como el proyecto todavía no
   envía correos reales, la pantalla muestra el **enlace de demostración** que llegaría por correo: dale clic.
3. **Reportar**: tipo de problema, descripción, ubicación (su domicilio u otra dirección), punto en el mapa o GPS,
   y hasta 3 fotos.
4. **Seguimiento**: el avance se dibuja como una tubería (Recibido → Revisado → Cuadrilla → Reparación → Atendido),
   con historial, fotos del antes y el después y avisos.
5. **Mi cuenta**: editar datos, cambiar contraseña, descargar sus datos, derechos ARCO y darse de baja.

### Administrador
- **Panel**: lo pendiente (por revisar, sin cuadrilla, por cerrar), gráfica de 14 días, tipos, estados y zonas.
- **Reportes**: bandeja con búsqueda, filtros, paginación y vista en mapa. Al abrir uno: validar / marcar en revisión /
  no procede, **crear orden de trabajo** (prioridad, fecha, responsable y apoyo de la zona), y después **cerrar**,
  **devolver** o **cancelar** la orden, o **cambiar el responsable**.
- **Órdenes**: todas las órdenes con filtros.
- **Personal**: dar de alta trabajadores (se genera una **invitación**), cambiarlos de zona, suspender o reactivar.
- **Zonas**: crear y editar zonas y decidir qué colonias cubre cada una.
- **Privacidad**: responder solicitudes ARCO. **Bitácora**: registro de todo lo que pasa (no se puede borrar).

### Trabajador (personal de campo)
1. Abre su **invitación** y crea su contraseña.
2. Ve sus órdenes pendientes y los incidentes de su zona.
3. En cada orden: **confirma que la recibió** → **registra el diagnóstico** → **sube fotos del antes y del después**
   → **marca el trabajo como terminado**. El administrador lo revisa y cierra.

### Usuarios de demostración
Ejecuta una vez `database/09_usuarios_demo.sql` en pgAdmin (con la API ya encendida al menos una vez).

| Rol | Correo | Contraseña | Zona |
|---|---|---|---|
| Administrador | admin@example.test | Admin2026Demo | — |
| Ciudadano | ciudadano@example.test | Ciudadano2026 | Las Flores (Zona Norte) |
| Trabajador | ana@example.test, pedro@example.test | Trabajador2026 | Zona Norte |
| Trabajador | laura@example.test, jose@example.test, maria@example.test | Trabajador2026 | Zona Centro |

### ¿Cómo hago a alguien administrador?
1. Registra la cuenta desde la app (con el correo que quieras).
2. En pgAdmin ejecuta `database/07_administrador_privacidad.sql` (cambia el correo dentro del archivo si no es
   `admin@odapas.demo`).
3. Cierra sesión y vuelve a entrar.

---

## 3. Seguridad que tiene

| Medida | Dónde |
|---|---|
| Contraseñas con PBKDF2 (nunca en texto plano), mínimo 10 caracteres con letras y números | `privacidad/Claves.java` |
| Sesión con token aleatorio; en la base solo se guarda su huella SHA-256 | `ServicioCuenta.java` |
| Permisos por rol: `/api/admin/**` solo administrador, `/api/trabajador/**` solo trabajador | `configuracion/SeguridadCuenta.java` |
| Cada ciudadano solo ve **sus** reportes y fotos; el trabajador solo las órdenes **asignadas** a él | `ServicioReportes.java`, `ServicioArchivos.java` |
| Bloqueo de 15 min después de 5 contraseñas incorrectas (responde 429) | `seguridad/LimitadorIntentos.java` |
| Enlaces de verificación/recuperación/invitación de **un solo uso** y con caducidad (48 h / 30 min / 24 h) | `servicio/ServicioEnlaces.java` |
| "Olvidé mi contraseña" no revela si un correo existe | `ServicioAcceso.java` |
| Fotos: se revisa el contenido real del archivo (JPG/PNG/WEBP), máximo 5 MB, nombre aleatorio | `servicio/ServicioArchivos.java` |
| Encabezados de seguridad (nosniff, no iframes, sin caché en la API) | `seguridad/EncabezadosSeguridad.java` |
| Consultas SQL con parámetros `?` (evita inyección SQL) | todos los servicios |
| Errores sin detalles internos (sin stacktrace) | `controlador/ErroresCrud.java`, `application.properties` |
| Bitácora inalterable de operaciones y accesos fallidos | `db/08_operacion.sql`, `servicio/Bitacora.java` |
| Mapa público sin folios ni datos personales | `ControladorPublico.java` |

---

## 4. Todas las APIs

**Públicas** (sin sesión): `GET /api/publico/documentos`, `/colonias?q=`, `/tipos-reporte`, `/mapa`, `/resumen`, `GET /api/zonas`

**Acceso**: `POST /api/auth/registro`, `/login`, `/logout`, `/verificar`, `/reenviar-verificacion`, `/recuperar`,
`/restablecer`, `/activar`

**Ciudadano**: `GET|POST /api/reportes`, `GET /api/reportes/{folio}`, `GET /api/reportes/{folio}/detalle`,
`POST /api/reportes/{folio}/fotos`, `GET /api/archivos/{nombre}`

**Avisos** (cualquier usuario): `GET /api/notificaciones`, `GET /api/notificaciones/contador`,
`PUT /api/notificaciones/{id}/leida`, `PUT /api/notificaciones/leer-todas`

**Mi cuenta**: `GET|PUT /api/cuenta`, `PUT /api/cuenta/password`, `GET /api/cuenta/exportar`, `GET|POST /api/cuenta/arco`,
`PUT /api/cuenta/preferencias`, `POST /api/cuenta/baja`, `GET|POST /api/cuenta/aceptaciones`

**Trabajador**: `GET /api/trabajador/resumen`, `GET /api/trabajador/ordenes`, `GET /api/trabajador/ordenes/{folio}`,
`POST .../{folio}/recibir`, `.../iniciar`, `.../fotos?fase=antes|despues`, `.../terminar`

**Administrador**: `GET /api/admin/estadisticas`, `GET /api/admin/reportes`, `PUT /api/admin/reportes/{folio}/revision`,
`POST /api/admin/reportes/{folio}/clasificar`, `GET /api/admin/reportes/{folio}/elegibles`, `GET|POST /api/admin/ordenes`,
`PUT /api/admin/ordenes/{folio}/estado`, `PUT /api/admin/ordenes/{folio}/responsable`, `GET|POST /api/admin/trabajadores`,
`POST /api/admin/trabajadores/{id}/invitacion`, `PUT /api/admin/trabajadores/{id}/zona`, `PUT /api/admin/trabajadores/{id}/estado`,
`GET|POST /api/admin/zonas`, `PUT /api/admin/zonas/{id}`, `GET /api/admin/colonias?q=`, `PUT /api/admin/colonias/{id}/zona`,
`GET /api/admin/bitacora`, `GET|PUT /api/admin/arco`, y los 5 CRUD: `/api/personas`, `/api/contactos`, `/api/municipios`,
`/api/colonias`, `/api/tipos-reporte`.

---

## 5. Cómo probarlo

| Prueba | Comando / archivo | Resultado esperado |
|---|---|---|
| JUnit (45 pruebas) | `.\ejecutar-pruebas.cmd` | `Tests run: 45, Failures: 0` |
| Postman (40 peticiones, flujo completo) | Importa `coleccion-postman/ODAPAS_Completa.postman_collection.json`, pon `adminEmail` y `adminPassword` en las **Variables** de la colección y usa **Run collection** | 41 comprobaciones en verde |
| Flujo de punta a punta en Python | `python pruebas-e2e\flujo_completo.py` (con `ADMIN_EMAIL` y `ADMIN_PASSWORD`) | `TODO CORRECTO: 31 comprobaciones` |

En Postman, las dos peticiones de fotos necesitan que elijas una imagen en *Body → form-data → fotos*.

---

## 6. Archivos importantes nuevos

```
api-odapas-reporte/src/main/java/mx/odapas/reporte/
├── controlador/ ControladorAdministracion, ControladorTrabajador, ControladorNotificaciones,
│                ControladorArchivos, ControladorPublico, ControladorAutenticacion, ControladorReportes
├── servicio/    ServicioAdministracion, ServicioTrabajador, ServicioReportes, ServicioAcceso,
│                ServicioEnlaces, ServicioArchivos, ServicioNotificaciones, Bitacora, Datos
├── seguridad/   Peticion, LimitadorIntentos, EncabezadosSeguridad
└── configuracion/ MigracionOperacion (+ resources/db/08_operacion.sql)

aguapp/src/app/
├── nucleo/      sesion.service, interceptor-sesion, guardias, modelos, etiquetas, api
├── compartido/  mapa, tubería de seguimiento, foto protegida, buscador de colonias, selector de fotos, avisos
└── paginas/     publico/ (inicio, entrar, registro, verificar, recuperar, restablecer, activar, legal)
                 ciudadano/ (mis reportes, nuevo reporte, detalle, avisos, cuenta)
                 trabajador/ (panel, orden de trabajo)
                 admin/ (panel, reportes, reporte, órdenes, personal, zonas, privacidad, bitácora)
```
Las fotos se guardan en `api-odapas-reporte\archivos` (puedes cambiarlo con la variable `ODAPAS_ARCHIVOS`).

### Para producción real (pendiente)
Configurar un servidor de correo (SMTP) y poner `ODAPAS_MOSTRAR_ENLACES=false` para que los enlaces ya no se
muestren en pantalla; usar HTTPS.
