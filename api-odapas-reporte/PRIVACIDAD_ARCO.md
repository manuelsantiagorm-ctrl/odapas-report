# Derechos ARCO y cuenta de usuario
Responsable: Jose Manuel Santiago Osorio · admin@odapas.demo.
Implementación escolar de ODAPAS Report; no es un servicio oficial de ODAPAS.

## Iniciar y probar
1. Reinicia la API con: powershell -NoProfile -File .\api-odapas-reporte\iniciar-api.ps1
2. Introduce tu contraseña de PostgreSQL. No se modifica ni guarda.
3. En otra terminal ejecuta npm start y abre http://localhost:4200.
4. Registra una cuenta NUEVA. Las antiguas cuentas simuladas del navegador y las semillas con contraseña bloqueada no sirven para login.
5. Abre Mi cuenta y privacidad: consulta, rectifica y descarga tus datos, cambia contraseña, registra solicitudes ARCO o solicita tu baja.
6. Al cambiar contraseña se cierran las sesiones. Al darse de baja, la cuenta queda suspendida y tampoco permite nuevos inicios de sesión.
7. Conserva el folio de cancelación para seguimiento por correo tras la baja; el responsable debe verificar identidad antes de revelar información.

## Base de datos
El arranque aplica automáticamente la ampliación incremental src/main/resources/db/06_privacidad.sql.
NO reinstales 01_esquema.sql ni la base completa. No se eliminan reportes existentes.
Se añaden SEIS tablas: documento_legal, aceptacion_legal, sesion_usuario,
preferencia_privacidad, solicitud_arco y evento_privacidad.
ciudadano.direccion_actual_id admite NULL para desvincular un domicilio al cancelar.
Los documentos guardan versión y texto. Cambiar un texto sin cambiar su versión produce un error de arranque para preservar evidencia de aceptación.
Las aceptaciones separan términos, aviso y versión; las comunicaciones opcionales son independientes y están desactivadas por defecto.

## Administrador
Registra tu propia cuenta con admin@odapas.demo. Después ejecuta MANUALMENTE en pgAdmin,
conectado a odapas_report, database/07_administrador_privacidad.sql.
El script solo otorga administrador a esa cuenta existente y activa; no cambia contraseñas.
Cierra sesión y vuelve a entrar. Abre Mi cuenta → Administrar ARCO.
No ejecutes la asignación de administrador para cuentas ciudadanas de prueba.

Los expedientes pueden pasar a en_revision, atendida o rechazada con respuesta motivada.
Una resolución final no se sobrescribe.
Acceso: entrega o confirma la disponibilidad de los datos. Rectificación: realiza/verifica la corrección antes de resolver; el usuario puede corregir su perfil y el administrador tiene los CRUD protegidos.
Oposición: al atender se desactivan comunicaciones opcionales; cualquier otra finalidad requiere revisión y acciones adicionales.
Cancelación: exige confirmar revisión de conservación, suspende la cuenta, revoca sesiones,
elimina el hash de contraseña, anonimiza nombre/correo/teléfono y desvincula el domicilio.
El domicilio desvinculado se elimina solo si no está usado por otro ciudadano o por un reporte.
Reportes, direcciones históricas, fotos, texto de solicitudes y respaldos pueden conservar datos
identificables: revisarlos y documentar el motivo y seguimiento es una tarea adicional.
Atendida significa resolución emitida, NO supresión automática de todas las copias.
No se promete un plazo legal universal de conservación en el prototipo.

## Seguridad
PBKDF2-HMAC-SHA256 con sal aleatoria y 210000 iteraciones para contraseñas.
Tokens aleatorios de 256 bits; únicamente SHA-256 se guarda en la DB; caducan en ocho horas.
La sesión del navegador usa sessionStorage y se elimina la antigua sesión simulada de localStorage.
Los endpoints de cuenta obtienen el titular de la sesión, nunca de un usuario_id enviado por el cliente.
Editar perfil y cambiar contraseña o darse de baja requieren contraseña actual.
La descarga excluye contraseñas, hashes y tokens. Incluye perfil, direcciones vinculadas,
reportes propios, fotos (metadatos), historial de reportes, solicitudes, aceptaciones,
notificaciones y eventos de privacidad.
Los CRUD /api/personas, /contactos, /municipios, /colonias y /tipos-reporte requieren administrador.
GET /api/reportes requiere sesión: cada ciudadano consulta sus reportes; administrador consulta todos.
La atención operativa de trabajadores por zona necesita una ruta específica posterior.
Los catálogos /api/publico/colonias, /api/publico/documentos y /api/zonas siguen públicos.
El frontend utiliza /api mediante proxy.conf.json hacia localhost:8080.
Para publicación se requiere un proxy equivalente y HTTPS.

## Postman
Importa coleccion-postman/ODAPAS_ARCO.postman_collection.json y usa Runner en el orden original.
Se guarda automáticamente coloniaId, versión y token. Se crea un correo ficticio único.
Comprueba perfil, edición, exportación, cuatro solicitudes, preferencias, protección de datos,
baja y rechazo de acceso posterior. La baja va al final.
La colección de los cinco CRUD ahora requiere administrador. En Variables completa adminEmail
y adminPassword con TU cuenta administradora y ejecuta 00 Iniciar sesion administrador primero.
No compartas una exportación con credenciales guardadas. Una cuenta ciudadana recibe 403.

## Alcance y pendientes reales
El aviso y términos son borradores educativos personalizados. Falta configurar domicilio real
del responsable y una política de conservación/bloqueo adecuada al régimen que corresponda.
No acreditan cumplimiento legal integral. Un organismo público real puede tener obligaciones distintas.
No se implementaron envío de correo, recuperación automática ni verificación de correo.
Cambiar correo elimina su verificación previa. El formulario de nuevos reportes del frontend
sigue siendo la simulación anterior; este módulo no implementa su alta real ni su verificación.
Antes de uso público: HTTPS, límites de intentos, verificación de correo, revisión de datos históricos,
políticas de respaldo/conservación y revisión jurídica. No hay promociones ni rastreo publicitario.

Referencias:
https://datos-personales.scjn.gob.mx/derechos-arco
https://www.diputados.gob.mx/LeyesBiblio/pdf/LFPDPPP.pdf

