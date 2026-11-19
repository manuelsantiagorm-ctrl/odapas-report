# ODAPAS Report — Documentación funcional y reglas de negocio

**Versión:** 1.0 · **Fecha:** 7 de octubre de 2026

**Propósito:** definir el funcionamiento de la aplicación antes de rediseñar su base de datos normalizada.

Esta es una propuesta para el proyecto académico. No describe procesos internos confirmados ni una aplicación oficial de ODAPAS. El nombre de producto acordado para el proyecto es **ODAPAS Report**.

## 1. Decisión principal

El ciudadano se registra por su cuenta con datos personales y dirección. El trabajador **no se registra públicamente**: un administrador lo da de alta, le asigna una zona de atención y habilita su acceso laboral. El trabajador recibe alertas de incidentes de su zona y atiende las órdenes que le asigna el administrador.

Esta alternativa permite comprobar quién pertenece a la empresa, impedir que un ciudadano obtenga permisos laborales y mantener un responsable identificable por reparación. El administrador conoce la zona, disponibilidad y especialidad del personal antes de asignar un trabajo.

Se conserva una sola cuenta de acceso por persona, con correo único y contraseña segura. Los datos de ciudadano y de trabajador se guardan en perfiles distintos. Tener un perfil ciudadano no concede permisos de trabajador. Si un trabajador también desea reportar como ciudadano, se añade el perfil ciudadano a su cuenta, sin duplicar sus credenciales.

**Tres perfiles principales de la primera versión:** ciudadano, trabajador reparador y administrador. Puede delegarse posteriormente la operación diaria a un rol supervisor; no es obligatorio para iniciar el proyecto.

## 2. Objetivo, alcance y límites

### Objetivo general

Facilitar el registro, distribución por zona, atención y seguimiento de reportes sobre fugas, falta de agua y baja presión, con trazabilidad desde la solicitud ciudadana hasta la revisión y cierre de la reparación.

### Incluido en la primera versión

- Registro ciudadano, acceso, recuperación de contraseña y actualización del perfil.
- Dirección del ciudadano y dirección propia de cada incidente.
- Alta, activación, desactivación y administración del personal por un administrador.
- Catálogo de zonas, colonias que cubre cada zona y asignación de trabajadores a una zona.
- Registro de reportes con tipo, descripción, dirección, GPS opcional y fotografías opcionales.
- Alertas al personal de la zona, asignación de responsables y seguimiento de órdenes.
- Diagnóstico, intervenciones, evidencias y materiales utilizados en la reparación.
- Historial de cambios, notificaciones al ciudadano y revisión/cierre por el administrador.
- Filtros, mapa, estadísticas operativas y consultas para pruebas de controllers Spring Boot.

### Fuera del alcance inicial

Cobro de recibos de agua, contratación del servicio, nómina, compras, inventario completo de almacén, control de vehículos, rastreo permanente del trabajador y expedientes jurídicos. El registro no exige CURP, RFC ni número de contrato para poder reportar. Si la empresa requiere otro dato, se incorporará como un requisito nuevo y justificado.

## 3. Usuarios y permisos

| Acción | Ciudadano | Trabajador | Administrador |
|---|---|---|---|
| Registrarse públicamente | Sí, como ciudadano | No | No |
| Actualizar sus datos de contacto y contraseña | Sí | Sí | Sí |
| Crear y consultar sus reportes | Sí | Si tiene perfil ciudadano | Puede registrar un reporte en atención al ciudadano, dejando autor y actor registrados |
| Consultar incidentes pendientes de su zona | Solo los propios; mapa general sin datos personales | Sí, con información operativa limitada | Sí, todas las zonas del organismo |
| Consultar órdenes asignadas | Estado público de sus reportes | Sí, las propias | Sí, todas |
| Dar de alta trabajadores o cambiar su zona | No | No | Sí |
| Cambiar prioridad o validar reportes | No | No | Sí |
| Asignar, cancelar o reasignar órdenes | No | No | Sí |
| Registrar diagnóstico, trabajos y evidencias | No | Solo en asignaciones autorizadas | Consulta, revisión y corrección auditada |
| Marcar una reparación como realizada | No | Sí, con resultado y evidencia requerida | Sí, mediante actuación registrada |
| Cerrar definitivamente una orden | No | No | Sí, después de revisión |
| Cambiar roles o desactivar cuentas laborales | No | No | Sí |
| Consultar contacto del ciudadano | Sus propios datos | Cuando lo necesita para un trabajo asignado | Sí, para atención del caso |

Los permisos se comprueban en el backend en cada petición, no solo ocultando botones. Una persona de otra zona no accede a reportes privados por conocer su ID. Correo, hash de contraseña, teléfono personal y domicilio de residencia no se exponen en el mapa público.

El primer administrador se provisiona durante la instalación por un medio controlado. No existe un formulario público que permita elegir el rol administrador o trabajador.

## 4. Registro y experiencia del ciudadano

### 4.1 Datos de registro

| Dato | Obligatorio | Regla |
|---|---|---|
| Nombre completo | Sí | Texto no vacío; no exigir un formato que excluya nombres válidos |
| Correo electrónico | Sí | Normalizado, único y confirmado por enlace |
| Contraseña y confirmación | Sí | Contraseña segura; confirmación solo valida entrada y no se almacena |
| Teléfono de contacto | Sí | Capturar número y validar formato; no asumir que email basta para atención en campo |
| Municipio/localidad de cobertura | Sí | Elegir del catálogo del organismo; puede venir preseleccionado si solo se atiende uno |
| Colonia | Sí | Pertenece al municipio/localidad elegido |
| Calle | Sí | Texto no vacío |
| Número exterior | Sí | Permitir «S/N» cuando no existe número |
| Número interior | No | Solo si aplica |
| Código postal | Sí | Cinco dígitos; no usarlo como única regla para determinar zona |
| Referencias del domicilio | No | Ayudan a localizar el lugar |
| Aceptación de términos/aviso del proyecto | Sí | Guardar versión aceptada y fecha |

El registro crea la cuenta y el perfil ciudadano. Se verifica el correo antes de habilitar el envío de reportes; una cuenta sin verificar puede corregir datos y solicitar otro enlace. Los mecanismos contra registros automatizados se añaden al backend sin obligar a capturar documentos personales innecesarios.

### 4.2 Panel ciudadano

El ciudadano ve sus reportes, folios, estados, última actualización y notificaciones. Puede crear un reporte, consultar el detalle, añadir información mientras el caso está abierto y confirmar si el problema continúa después de una reparación.

Cambiar la dirección del perfil afecta los nuevos reportes que usen ese domicilio; **no modifica la dirección de reportes anteriores**.

### 4.3 Dirección al reportar

El formulario ofrece «Reportar en mi domicilio», precargando su dirección, y «Reportar en otra ubicación», para una fuga en otra calle o colonia dentro de cobertura. Antes de enviar se confirma la ubicación del incidente.

El incidente tiene su propia dirección histórica. La dirección residencial del ciudadano no se usa automáticamente como ubicación del problema si este reporta en otro sitio. El GPS es opcional: si se deniega, se puede reportar con dirección completa y referencias. No se inventan coordenadas usando el centro de una colonia.

## 5. Alta y experiencia del trabajador de ODAPAS

### 5.1 Alta exclusivamente por administrador

Desde «Personal → Nuevo trabajador», el administrador captura:

- Nombre completo, correo y teléfono de contacto.
- Número de empleado, único dentro del organismo.
- Organismo al que pertenece y perfil de trabajador reparador.
- Especialidades: fugas, red de distribución, presión/suministro, según el catálogo.
- **Zona de atención obligatoria**, seleccionada del catálogo activo.
- Cuadrilla, si trabaja como integrante de un equipo.
- Disponibilidad operativa inicial y fecha de inicio de la adscripción a la zona.

El administrador valida la identidad laboral. La app registra quién creó la cuenta y cuándo. El trabajador no elige su rol, zona ni número de empleado desde un registro público.

### 5.2 Activación

1. El administrador guarda el alta laboral con la zona asignada.
2. Se genera una invitación personal de un solo uso, con vencimiento configurable; se propone 24 horas como valor inicial.
3. El trabajador recibe el enlace de activación por el canal de correo configurado, define su contraseña y confirma su identidad de acceso.
4. El sistema habilita la cuenta y su rol laboral; registra fecha de activación.
5. Se muestra su zona y se ofrece habilitar avisos del navegador para recibir alertas.

El administrador no necesita conocer la contraseña del trabajador. Una invitación vencida o utilizada no se reutiliza: el administrador puede generar otra. El token se guarda mediante su hash, sin texto plano.

Si el correo ya existe, no se crea una cuenta duplicada: el administrador revisa la identidad y añade el perfil laboral a la cuenta existente. Una cuenta ciudadana no puede convertir ese proceso en un ascenso automático de permisos.

### 5.3 Estados de la cuenta y disponibilidad

**Estado de acceso:** invitado, activo o suspendido. **Disponibilidad operativa:** disponible o no disponible. Son conceptos distintos: un trabajador activo puede estar temporalmente no disponible y conservar acceso a su historial.

Un trabajador invitado/suspendido no puede recibir nuevas asignaciones ni acceder al panel laboral. Solo personal activo, disponible y con adscripción vigente recibe alertas operativas nuevas. El administrador sigue recibiendo alertas de incidencias que no tienen personal disponible.

No se elimina una cuenta con intervenciones realizadas: se desactiva y se preserva el historial. Cambiar especialidad, cuadrilla o zona no reescribe actuaciones anteriores.

### 5.4 Panel del trabajador

El panel muestra «Mi zona», incidentes nuevos de esa zona, «Mis órdenes», alertas, calendario de trabajos y su disponibilidad. Distingue claramente una alerta territorial de un trabajo asignado.

En su orden puede consultar dirección y referencias del incidente, ubicación en mapa si existe GPS, tipo, prioridad confirmada, descripción, evidencias ciudadanas, otros integrantes asignados y contacto autorizado cuando resulte necesario.

El trabajador confirma recepción de la orden, registra inicio, diagnóstico, avances, impedimentos, materiales y evidencias. Si no puede acudir, registra el impedimento y solicita gestión al administrador; no transfiere por su cuenta el trabajo a otra persona.

## 6. Zonas de atención

### 6.1 Definición y configuración

Una zona es una división operativa del territorio atendido por el organismo, por ejemplo «Zona Norte» o «Zona Centro». Tiene código único dentro del organismo, nombre, descripción y estado activo/inactivo. Los nombres del ejemplo no definen límites reales de ODAPAS.

El administrador configura qué colonias cubre cada zona. Para la primera versión, cada colonia de cobertura pertenece a **una sola zona activa**, y una zona puede incluir varias colonias. Una zona puede tener muchos trabajadores.

Cada trabajador tiene **una zona principal activa a la vez**. El sistema conserva fechas de inicio y fin de cada adscripción. Si después se necesitan zonas de respaldo o varias zonas simultáneas, se ampliará explícitamente la regla; no se guardarán listas de zonas separadas por comas.

La primera versión asigna zonas por colonia, lo que permite una implementación verificable. Si una colonia está dividida entre varias zonas en la operación real, se deben definir sectores/calles o polígonos antes de activar esa cobertura; no resolver ambiguamente con el código postal ni prometer que una colonia partida cabe en este criterio.

### 6.2 Determinación de la zona del reporte

1. El ciudadano confirma municipio/localidad y colonia del incidente.
2. El backend obtiene la zona activa correspondiente desde la cobertura configurada.
3. El reporte conserva la zona que tenía cuando se registró y el motivo de cualquier cambio posterior.
4. El ciudadano no envía un ID de trabajador ni impone una zona para conseguir atención de otra área.

Si el GPS y la dirección parecen incompatibles, el reporte se marca para revisión; no se utiliza una ubicación dudosa como criterio automático para mover personal.

### 6.3 Excepciones

| Situación | Comportamiento |
|---|---|
| Colonia sin zona configurada | Se acepta el reporte con marca «Pendiente de clasificación», se alerta al administrador y no se descarta |
| Zona sin trabajadores disponibles | Se conserva el reporte; alerta al administrador para reasignar recursos |
| Ubicación fuera de cobertura | Se informa al ciudadano y se registra el motivo de no atención; no se genera una orden a una zona incorrecta |
| Zona inactiva con incidentes pendientes | El administrador debe transferir la cobertura/casos antes de desactivarla; no dejar trabajo sin responsable |
| Cambio de zona del trabajador | Termina la adscripción anterior, inicia otra y recibe nuevas alertas de la nueva zona; sus órdenes previas requieren revisión explícita del administrador |
| Corrección de zona de un reporte | Se registra actor, motivo y fecha; se avisa al área destino y se revisan las asignaciones existentes |

La responsabilidad de una orden asignada no desaparece automáticamente porque un trabajador cambie de zona. Se conserva hasta que el administrador la finalice/reasigne con motivo. El trabajador podrá consultar esos trabajos previamente asignados mientras conserve la autorización correspondiente.

Las notificaciones anteriores permanecen en el historial del destinatario. Abrirlas debe volver a comprobar permisos actuales: una alerta antigua no concede acceso indefinido al detalle privado de otra zona. Las asignaciones previas aún autorizadas son la excepción explícita, no todos los reportes territoriales antiguos.

## 7. Reportes ciudadanos

### 7.1 Datos del incidente

Folio único, autor, tipo de incidente, descripción obligatoria, dirección confirmada, zona determinada por el backend, coordenadas opcionales, fotografías opcionales y fecha/hora de creación. Las horas se guardan como instantes y se presentan en la zona horaria de operación, inicialmente America/Mexico_City.

Tipos iniciales: fuga, falta/corte de agua y baja presión. El ciudadano puede describir gravedad observada; el administrador confirma prioridad baja, media, alta o urgente. No se declara prioridad urgente definitiva solo porque el ciudadano la eligió.

Las fotos del reporte documentan lo observado por el ciudadano. Las fotos de reparación pertenecen a la intervención del trabajador; no se mezclan como si fueran la misma evidencia.

### 7.2 Validación y duplicados

El administrador revisa que la ubicación, tipo y descripción permitan actuar. Puede validar, pedir aclaraciones, rechazar con motivo o vincular un reporte duplicado a un incidente principal. Un reporte duplicado conserva su autor, folio y evidencia; no se elimina.

Distintos ciudadanos que reportan la misma fuga no deben generar automáticamente múltiples cuadrillas sobre el mismo incidente. Sus reportes se vinculan al principal y sus autores reciben las actualizaciones correspondientes. La detección por cercanía es una sugerencia; la vinculación definitiva queda registrada como decisión del administrador.

No se rechaza un reporte únicamente por no tener foto o GPS. El ciudadano puede solicitar cancelación de un reporte pendiente; una orden ya en proceso requiere decisión del administrador con motivo.

### 7.3 Estado visible

La app distingue la clasificación del reporte de la atención técnica. La validación del reporte puede estar pendiente, validada, rechazada, cancelada o vinculada como duplicado. El avance de atención se obtiene de la orden correspondiente.

El ciudadano ve una presentación comprensible: recibido, en revisión, asignado, en atención, pendiente de recursos, reparación realizada, cerrado o no procedente, con su motivo cuando corresponda. Esta presentación se calcula de los hechos registrados; no se mantendrán varias columnas de estado independientes que puedan contradecirse.

## 8. Alertas y notificaciones

### 8.1 Regla para reportes nuevos

Al confirmar la creación del reporte, el sistema registra un evento y determina sus destinatarios: trabajadores activos/disponibles con adscripción vigente a la zona del incidente y administrador del organismo. El personal de otra zona no recibe esa alerta territorial.

Si la zona está pendiente o no hay personal elegible, siempre se alerta al administrador. Un reporte recién recibido puede necesitar validación: la notificación indica esa condición y no autoriza a iniciar una reparación.

**Recibir una alerta no equivale a ser responsable del trabajo.** La orden necesita una asignación explícita del administrador. Esta decisión evita que todos los trabajadores acudan a la misma fuga o que nadie asuma responsabilidad.

### 8.2 Eventos y destinatarios

| Evento | Destinatarios |
|---|---|
| Reporte registrado | Administrador y trabajadores elegibles de la zona; confirmación al ciudadano |
| Reporte sin zona o sin personal disponible | Administrador |
| Orden asignada/reasignada | Trabajadores asignados; información de avance al ciudadano |
| Trabajo iniciado | Ciudadano y administrador |
| Impedimento o material faltante | Administrador; ciudadano recibe estado de espera sin datos laborales innecesarios |
| Reparación realizada | Administrador para revisar y ciudadano para informar resultado provisional |
| Orden cerrada | Ciudadano y personal asignado |
| Ciudadano indica que continúa el problema | Administrador; después, personal que reciba la nueva asignación |
| Invitación laboral creada/reemitida | Persona invitada |

### 8.3 Canales y entrega

La notificación se guarda en la bandeja de la app aunque nadie tenga el panel abierto. Con el panel conectado, se muestran aviso y contador actualizado. Se propone Web Push para avisos de trabajadores con la app cerrada, siempre que el dispositivo sea compatible y el usuario habilite notificaciones. Si no lo habilita, las alertas siguen disponibles al entrar al panel; no se promete un aviso externo que el dispositivo no puede entregar.

La notificación interna es obligatoria. El aviso externo es un canal complementario y no sustituye la bandeja persistente. El mensaje externo muestra folio/tipo y solicita abrir la app; no expone domicilio completo ni teléfono en la pantalla bloqueada.

No almacenar una sola lista de destinatarios dentro de una notificación: cada entrega registra destinatario, canal, estado y fechas. Se distingue pendiente/enviada/fallida de leída/no leída. Leer una alerta no significa aceptar una orden ni completar una reparación.

### 8.4 Fiabilidad

El guardado del reporte y del evento de aviso se realiza en una misma transacción. La entrega externa se procesa después mediante una cola o patrón outbox; una falla de envío no borra el reporte. Los reintentos son configurables y no generan varias copias del mismo aviso para el mismo evento/destinatario/canal.

Las pantallas recargan desde la fuente persistente después de una reconexión. La respuesta de un proveedor de avisos no se considera prueba de lectura. Las pruebas comprobarán encolado, destinatarios, reintentos y duplicados; no asumirán que todos los dispositivos reciben push instantáneamente.

## 9. Órdenes, asignación y reparación

### 9.1 Creación y asignación

Una vez validado el caso, el administrador genera una orden con folio único, reporte principal, zona responsable, prioridad, descripción y fecha programada si se conoce. La creación y el primer evento se registran juntos.

El administrador selecciona personal de la zona, activo, disponible y con especialidad adecuada. Puede asignar una persona o una cuadrilla. Si asigna una cuadrilla, se guardan los integrantes que efectivamente quedaron asignados; cambiar después la cuadrilla no modifica esa lista histórica.

Se designa un **responsable principal** y, si corresponde, colaboradores. Todos tienen asignación individual trazable. Los colaboradores pueden documentar su intervención; el responsable consolida el resultado técnico. El administrador puede autorizar apoyo de otra zona con motivo, sin cambiar de forma silenciosa la adscripción habitual del trabajador.

Como regla inicial, un incidente principal tiene **una sola orden activa a la vez**. Puede conservar varias órdenes históricas por reincidencia o trabajos posteriores. El servicio impedirá duplicar órdenes activas bajo concurrencia; la regla deberá reflejarse en el futuro modelo y sus pruebas.

### 9.2 Trabajo en campo

1. El trabajador recibe su asignación y confirma recepción.
2. Registra inicio y diagnóstico del problema real encontrado.
3. Registra acciones realizadas e impedimentos, si los hay.
4. Añade fotografías antes/durante/después y materiales con cantidades/unidades.
5. Termina su intervención con resultado resuelto, parcial o no resuelto.
6. El responsable solicita revisión cuando considera terminada la reparación.

Para solicitar cierre se exige descripción del trabajo, resultado y evidencia posterior. Si no es posible obtener foto, se requiere justificación registrada y revisión explícita del administrador; no se simula un archivo para cumplir la regla.

Materiales representan consumo de la intervención. El sistema no asegura existencia en almacén mientras no se implemente un módulo de inventario.

### 9.3 Estados de orden y transiciones

| Estado actual | Transición permitida | Actor y condición |
|---|---|---|
| Recibida | Asignada / cancelada | Administrador; asignación válida o motivo de cancelación |
| Asignada | En proceso / cancelada | Responsable inicia; administrador cancela con motivo |
| En proceso | Pendiente de recursos / reparación realizada / cancelada | Responsable documenta impedimento o resultado; administrador puede cancelar con motivo |
| Pendiente de recursos | En proceso / cancelada | Responsable reanuda al resolver impedimento; administrador cancela |
| Reparación realizada | Cerrada / en proceso | Administrador revisa: acepta o devuelve con observaciones |
| Cerrada / cancelada | Sin edición directa de trabajos finalizados | Una reincidencia genera nuevo reporte/orden vinculada, previa revisión |

Reasignar personal es un cambio de asignación que conserva el historial; no constituye por sí solo un nuevo estado técnico. La orden conserva todos los eventos con actor, instante y motivo. No se confía en el dispositivo para imponer actor, prioridad o estado final.

### 9.4 Revisión y cierre

El administrador comprueba diagnóstico, trabajos, resultado, evidencias y consistencia de la atención. Puede cerrar o devolver el trabajo con observaciones. La marca «reparación realizada» informa al ciudadano, pero no equivale todavía a «cerrado».

El ciudadano puede indicar que la falla continúa. No reabre unilateralmente la orden: crea una observación/solicitud de revisión vinculada y el administrador decide si requiere nueva intervención o una nueva orden. No se exige confirmación ciudadana para todo cierre, porque la falta de respuesta no debe dejar indefinidamente abierto el trabajo.

## 10. Pantallas de la aplicación

| Área | Pantallas y funciones |
|---|---|
| Acceso | Registro ciudadano, login, verificación de correo, recuperación, activación laboral por invitación |
| Ciudadano | Perfil/dirección, nuevo reporte, mis reportes, detalle/seguimiento, notificaciones, observaciones posteriores |
| Trabajador | Mi zona, incidentes territoriales, mis órdenes, detalle, recepción/inicio, diagnóstico/avances, fotos/materiales, impedimentos, resultado, disponibilidad, notificaciones |
| Administrador | Tablero, reportes por zona/tipo/prioridad, revisión/duplicados, órdenes/asignaciones, personal/invitaciones, zonas/cobertura, cuadrillas/especialidades, revisión/cierre, catálogos, auditoría |

El mapa muestra ubicación del incidente cuando existen coordenadas. Sin GPS se muestra dirección y estado en una lista; no se coloca un pin falso. Los indicadores se calculan de los reportes y órdenes, sustituyendo los mensajes fijos del prototipo actual.

## 11. Reglas transversales

- Cada cuenta tiene correo único global; el número de empleado es único dentro del organismo.
- Roles y zonas se conceden desde operación administrativa, nunca desde valores elegidos en un registro ciudadano.
- Domicilio actual y ubicación histórica del incidente son datos diferentes.
- Los catálogos de zonas, colonias, tipos, prioridades, especialidades y unidades se referencian por claves; no repetir nombres en todas las filas.
- El backend genera folios/IDs y obtiene el actor de la sesión autenticada. El cliente no impone usuario creador, otorgante, verificador o autor del cierre.
- Toda asignación técnica referencia un perfil trabajador válido; las comprobaciones de activo, disponibilidad, zona y especialidad se hacen dentro de la operación autorizada.
- Cada cambio significativo conserva actor, fecha y motivo. Se preservan los historiales al cambiar de domicilio, zona, cuadrilla o rol.
- Desactivar una cuenta exige revisar trabajos pendientes y cerrar/reasignar sus responsabilidades. No eliminar datos históricos de reparaciones.
- Los tiempos de invitación/recuperación, las cantidades de materiales y los pares GPS se validan también en la DB.
- No guardar contraseñas ni tokens de activación/recuperación en texto plano; no devolver hashes en respuestas HTTP.
- Guardar archivos de imagen fuera de las tablas y conservar metadatos, autor y relación con reporte/intervención. Validar tamaño, contenido y tipo real de archivo en backend.
- Reintentar envíos o pulsar dos veces «Enviar» no debe crear incidencias/órdenes/avisos duplicados. Incorporar idempotencia en operaciones críticas.
- La primera versión supone un organismo operativo configurado y puede admitir varios municipios de su cobertura. No crear zonas o municipios inventados a partir del mapa.

## 12. Requisitos de calidad y operación

| Aspecto | Requisito verificable |
|---|---|
| Persistencia | Cuentas, direcciones, reportes, asignaciones y alertas permanecen al reiniciar el servidor |
| Seguridad | Autenticación y autorización por rol, propietario y zona en backend; transporte HTTPS en despliegue |
| Concurrencia | Evitar dos órdenes activas para un mismo incidente y conflictos al asignar responsable/generar secuencias |
| Notificaciones | Reporte y evento se confirman juntos; falla del canal no pierde el evento; reintentos sin duplicados |
| Integridad | PK, FK, UNIQUE y CHECK para identidad, catálogos y dominios; transacciones para reglas entre tablas |
| Rendimiento | Listados paginados, filtros por zona/estado/responsable/fecha e índices correspondientes |
| Usabilidad | Formularios en español, mensajes claros, estados comprensibles y diseño utilizable en teléfono |
| Conectividad | Informar fallo/reconexión; recuperar bandeja y órdenes; no presentar envío fallido como confirmado |
| Auditoría | Registrar altas, activaciones, cambios de zona, asignaciones y cierre sin sobrescribir historia |
| Recuperación | Copias y restauración probadas antes de utilizar datos reales; entorno de pruebas separado |

No se fija todavía un SLA real de reparación: los tiempos objetivo por prioridad y horario deberán configurarse según capacidad de la operación. No presentar una estimación como promesa de atención confirmada.

## 13. Casos de uso

| ID | Caso | Resultado principal |
|---|---|---|
| CU-01 | Registrar ciudadano | Cuenta, perfil, dirección y aceptación; correo pendiente/confirmado según el flujo |
| CU-02 | Dar de alta trabajador | Identidad laboral validada, zona inicial e invitación registrada |
| CU-03 | Activar trabajador | Invitación consumida una sola vez y acceso laboral habilitado |
| CU-04 | Configurar zona | Colonias de cobertura sin asignación activa ambigua |
| CU-05 | Asignar/cambiar zona del trabajador | Adscripción con inicio/fin y revisión de responsabilidades previas |
| CU-06 | Reportar incidente | Folio, ubicación histórica, zona determinada y evento de notificación |
| CU-07 | Recibir alerta territorial | Destinatario elegible obtiene una notificación persistente; aún sin responsabilidad asignada |
| CU-08 | Validar/vincular duplicado | Decisión administrativa con motivo y relación al incidente principal |
| CU-09 | Crear/asignar orden | Responsable principal y colaboradores con adscripciones/autorizaciones comprobadas |
| CU-10 | Registrar intervención | Diagnóstico, fechas, acciones, evidencias y consumo relacionados a una asignación |
| CU-11 | Gestionar impedimento/reasignación | Evento y motivo; responsabilidad transferida de forma explícita |
| CU-12 | Revisar/cerrar | Revisión administrativa, evento de cierre y aviso a ciudadano/personal |
| CU-13 | Reportar reincidencia | Solicitud vinculada y revisión antes de una nueva orden |
| CU-14 | Desactivar trabajador | Acceso revocado, trabajo pendiente revisado e historia preservada |

## 14. Escenario completo de ejemplo

Ejemplo ficticio, sin afirmar límites reales de ODAPAS:

1. El administrador crea Zona Norte y le asigna Colonia Las Flores.
2. Da de alta a Ana, empleada REP-014, con especialidad fugas y adscripción vigente a Zona Norte. Ana activa su cuenta.
3. Luis se registra como ciudadano con su dirección en Las Flores, verifica correo y reporta una fuga frente a su domicilio.
4. La app guarda el reporte R-000123 con la dirección confirmada y Zona Norte. Luis recibe confirmación; Ana y el administrador reciben la alerta.
5. El administrador valida la fuga, asigna prioridad alta y crea OT-000045. Designa a Ana responsable; otro trabajador puede figurar como colaborador.
6. Ana confirma recepción, inicia trabajo, registra una unión dañada, las fotos y el material utilizado. Marca resultado resuelto y solicita revisión.
7. Luis ve «Reparación realizada». El administrador revisa y cierra la orden; Luis recibe el aviso de cierre.
8. Si Ana cambia después a Zona Sur, el historial de OT-000045 sigue mostrando que ella atendió el incidente cuando estaba asignada a Zona Norte.

## 15. Criterios de aceptación y pruebas

| ID | Escenario | Esperado |
|---|---|---|
| CA-01 | Ciudadano intenta registrarse con rol administrador/trabajador | No se conceden permisos privilegiados |
| CA-02 | Personal intenta autorregistrarse en un formulario público laboral | No existe ese flujo; alta por administrador |
| CA-03 | Se intenta repetir correo o número de empleado del mismo organismo | Operación rechazada sin crear perfiles parciales |
| CA-04 | Alta laboral sin zona | Se rechaza o permanece incompleta sin habilitar acceso/asignaciones |
| CA-05 | Invitación vencida, usada o inválida | No activa la cuenta; permite gestión administrativa de nueva invitación |
| CA-06 | Registro de fuga en colonia de Zona Norte | Solo personal elegible de Norte y administrador reciben la alerta territorial |
| CA-07 | Reporte en otra ubicación distinta a la residencia | Usa zona del incidente, no zona del domicilio del autor |
| CA-08 | GPS denegado con dirección completa | Reporte aceptado y localizable por dirección |
| CA-09 | Colonia sin zona o zona sin personal disponible | Reporte conservado y administrador alertado |
| CA-10 | Trabajador lee la alerta territorial | Aviso leído; no se autoasigna ni inicia trabajo por esa acción |
| CA-11 | Trabajador modifica una orden ajena/sin autorización | Backend rechaza la operación |
| CA-12 | Administrador asigna persona inactiva/no disponible/de otra zona | Se rechaza; apoyo externo requiere autorización explícita y motivo |
| CA-13 | Dos administradores crean simultáneamente orden activa del mismo incidente | Solo una orden activa queda confirmada |
| CA-14 | Cuadrilla cambia integrantes tras la asignación | La lista histórica de responsables de la orden no cambia |
| CA-15 | Trabajador cambia de zona | Nuevas alertas usan nueva adscripción; órdenes anteriores se revisan explícitamente |
| CA-16 | Resultado técnico sin trabajo realizado/evidencia o justificación | No permite solicitar cierre |
| CA-17 | Trabajador intenta cerrar definitivamente | Se rechaza; administrador revisa y cierra |
| CA-18 | Ciudadano indica falla persistente tras cierre | Solicitud vinculada; sin reescribir cierre anterior |
| CA-19 | Falla envío externo después de guardar reporte | Reporte y aviso interno permanecen; reintento no duplica entrega |
| CA-20 | Reinicio/reconexión | Reportes, historial y bandeja se recuperan desde DB |
| CA-21 | Ciudadano cambia su domicilio | No cambia ubicación de incidentes anteriores |
| CA-22 | Desactivación del trabajador con tareas pendientes | No recibe nuevas tareas; sus tareas se revisan/reasignan y conservan trazabilidad |
| CA-23 | Se confirma duplicado | Se conserva folio/autor; no se abre automáticamente otra orden activa |
| CA-24 | Consulta de mapa/listado público | No expone credenciales, contacto privado ni domicilio residencial del denunciante |

Estos criterios guiarán pruebas de servicios, DB, controllers y UI. Tener un CHECK o una prueba GET no demuestra por sí solo permisos correctos ni entrega de notificaciones.

## 16. Base para el siguiente modelo relacional normalizado

### 16.1 Conceptos que deberán representarse

| Área | Conceptos y relaciones necesarios |
|---|---|
| Identidad | Cuenta, perfiles ciudadano/trabajador, roles y permisos concedidos |
| Acceso | Verificación de correo, invitaciones laborales, recuperación, activación/suspensión |
| Dirección | Municipio/localidad, colonia, dirección actual del ciudadano y ubicación histórica del incidente |
| Territorio | Organismo, zona, cobertura de colonias, adscripción histórica de trabajador y clasificación/cambios de zona del reporte |
| Personal | Número de empleado, especialidades, disponibilidad, cuadrillas e integrantes históricos |
| Incidentes | Reporte, tipo, evidencia ciudadana, validación/observaciones, relación de duplicados y reincidencias |
| Atención | Orden, prioridad, responsable principal, colaboradores, recepción, asignaciones y eventos de seguimiento |
| Reparación | Intervenciones, diagnóstico, resultados, evidencias técnicas, materiales/unidades/consumos y revisión administrativa |
| Comunicaciones | Evento de aviso, notificación por destinatario, lectura, intentos de entrega por canal y suscripción push del dispositivo |

Esta lista define necesidades de información, **no obliga a una tabla por palabra**. El próximo diseño identificará dependencias funcionales y resolverá cada relación sin duplicación ni campos multivaluados.

### 16.2 Criterios de normalización

**1FN:** atributos atómicos y filas para fotos, roles, especialidades, integrantes y destinatarios. No guardar arreglos/listas de IDs separados por comas en una celda.

**2FN:** en relaciones de varias claves, los atributos dependen de la clave completa: adscripción, rol concedido, destinatario/entrega y consumo. Datos del usuario, zona y material se separan de esas relaciones.

**3FN:** no copiar nombre de colonia, zona, trabajador, organismo, especialidad o unidad en cada reporte/orden. Referenciar catálogos y perfiles. No duplicar credenciales entre ciudadano y trabajador. No guardar como fuentes independientes estados que ya se derivan de los eventos.

Una dirección/adscripción histórica no es una copia redundante del dato actual: identifica el contexto de un evento que debe sobrevivir a cambios posteriores. La forma concreta de guardar versiones/snapshots se justificará en el modelo, para no mezclar domicilio actual con ubicación del incidente.

### 16.3 Cambios respecto al esquema de 32 tablas anterior

El esquema SQL/DBML anterior es un antecedente ya probado, pero **no satisface todavía esta especificación**: faltan zonas/cobertura, adscripciones, direcciones completas, invitaciones, verificación de correo, notificaciones/entregas y reglas nuevas de responsabilidad principal y única orden activa.

La alternativa anterior de solicitud pública del reparador se descarta por la decisión del usuario. Su sustituto es el alta laboral administrativa y activación por invitación. Los estados de alta del modelo anterior deberán revisarse para reflejar este flujo.

No se debe presentar el diagrama de 32 tablas como el modelo final de ODAPAS Report. La siguiente etapa partirá de este documento para definir el diagrama actualizado, diccionario, PK/FK/UNIQUE/CHECK, migraciones, seeds y pruebas. No editar migraciones aplicadas para simular que estas funciones ya existen.

## 17. Orden de desarrollo propuesto

1. **Modelo relacional:** entidades, claves, cardinalidades, historial, reglas y migraciones según esta especificación.
2. **Identidad y territorio:** acceso, registro ciudadano/dirección, alta laboral/invitaciones, permisos y zonas/cobertura.
3. **Reportes y alertas:** creación, ubicación, clasificación, notificación persistente y entrega de avisos.
4. **Operación:** revisión, duplicados, órdenes, asignación de responsables, cuadrillas y seguimiento.
5. **Reparación y cierre:** evidencias, materiales, resultado, revisión y reincidencia.
6. **Pruebas completas:** integridad, permisos, concurrencia, fallas de entrega y recorridos de los tres perfiles.

## 18. Qué está decidido y qué deberá configurarse

**Decidido por el usuario:** nombre ODAPAS Report, registro ciudadano con dirección, alta de trabajadores por administrador, zona específica por trabajador y alertas cuando se reporta una incidencia en su zona.

**Decisiones de diseño propuestas para avanzar:** una zona principal vigente por trabajador; cobertura inicial por colonia; administrador asigna responsables; alertar no autoasigna; una orden activa por incidente principal; revisión administrativa antes de cierre; notificación interna persistente con push complementario.

**Configuración pendiente, que no impide diseñar la DB:** municipio/organismo real de operación, nombres y límites reales de zonas, colonias cubiertas, plantilla/especialidades, canal de correo/push, vencimiento de invitaciones, horarios y tiempos objetivo por prioridad. No se inventarán estos datos reales en los seeds.

**Estado de esta entrega:** documentación funcional creada. Las nuevas pantallas, permisos, zonas, invitaciones y alertas aquí descritas aún no están implementadas. Se mantienen los artefactos anteriores como referencia; el modelo definitivo y su implementación corresponden a la siguiente etapa.
