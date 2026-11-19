# PostgreSQL — ODAPAS Report corregido

Esta entrega usa el último modelo escolar acordado: **24 tablas**, Persona y Contacto separados, Municipios, fotos con metadatos JSONB y **ninguna tabla de materiales**.

## Instalar desde pgAdmin

1. Conectar a su servidor PostgreSQL 16 con su usuario habitual.
2. Abrir Query Tool sobre la base de mantenimiento `postgres` y ejecutar `00_crear_base.sql`. Crear la base requiere permisos de creación y no se hace dentro de BEGIN/COMMIT.
3. Actualizar la lista de bases y abrir Query Tool sobre **odapas_report**.
4. Abrir y ejecutar **odapas_report_completa.sql**. Contiene tablas, reglas, vistas y datos ficticios en una sola transacción. Requiere una base vacía. No ejecutar además 01/02/03, porque son los mismos componentes.
5. Ejecutar **04_pruebas.sql** en esa base. Comprueba restricciones y un recorrido completo; revierte sus fixtures al terminar.

Si ya existe una base llamada odapas_report con tablas, crear otra base vacía. Estos scripts no ejecutan DROP ni reemplazan información. No ejecutar sobre las bases antiguas aguapp o las migraciones de los otros proyectos.

## Instalación desde PowerShell

Desde la carpeta `api-odapas-reporte`:

```powershell
powershell -NoProfile -File .\database\instalar.ps1
```

El instalador pide la contraseña localmente, sin mostrarla ni guardarla en archivos. Usa PostgreSQL 16 en 127.0.0.1:5432, usuario postgres y base odapas_report. Si utiliza otro servidor, puerto o usuario:

```powershell
powershell -NoProfile -File .\database\instalar.ps1 -Servidor localhost -Puerto 5432 -Usuario tu_usuario -Base odapas_report
```

Para instalar solo estructura/catálogos vacíos, agregar `-SinDatosPrueba`. Se incluye `odapas_report_estructura.sql` como alternativa manual sin seed.

## Archivos

| Archivo | Uso |
|---|---|
| 00_crear_base.sql | Crear odapas_report en el servidor autenticado |
| odapas_report_completa.sql | Instalación completa y atómica con datos demo |
| odapas_report_estructura.sql | Tablas, reglas y vistas sin datos demo |
| 01_esquema.sql | Tablas, tipos, claves, índices y restricciones |
| 02_reglas_y_vistas.sql | Reglas operativas, alertas internas y vistas |
| 03_datos_prueba.sql | Dataset ficticio para revisar el modelo |
| 04_pruebas.sql | 29 rechazos esperados y recorrido de reparación/cierre con rollback |
| 05_carga_2000.sql | Carga opcional de al menos 2,000 filas adicionales por tabla; ver CARGA_2000.md |
| instalar.ps1 | Instalador local con contraseña privada |

## Qué contiene

- Personas y contactos independientes de la cuenta: no se repiten nombre, correo ni teléfono en Usuario.
- Perfil ciudadano con domicilio actual y perfil trabajador creado por administrador.
- Municipio, colonia y dirección. El reporte referencia la ubicación histórica del incidente.
- Zonas, cobertura histórica de colonias y adscripción histórica de trabajadores.
- Reportes, revisión, clasificación de zona, órdenes, asignaciones y reparaciones.
- Fotografías: una fila por foto, con JSONB de metadatos; no almacenar arreglos de fotos ni archivos binarios en ese campo.
- Notificaciones internas persistentes. No se enviaron correos, SMS ni mensajes externos.

Se añadieron `zona.codigo` y `tipo_reporte.codigo` como identificadores únicos para conservar los códigos ZN/ZC/ZS y fuga/falta_agua/baja_presion de la API actual. Esos atributos no duplican nombres en reportes. Se usan TIMESTAMPTZ para instantes y DATE para fechas programadas.

## Fotos JSON

```json
{"url":"/fotos/fuga-001.jpg","nombre":"fuga-001.jpg","tipo":"image/jpeg"}
```

PostgreSQL valida que sea un objeto, que existan esas tres propiedades y sean texto no vacío, y que el tipo sea una imagen. El archivo se guarda fuera de la DB. Los JSON demo son ejemplos; no corresponden a imágenes reales adjuntas.

La estructura de entidades se mantiene separada y normalizada. JSON se utiliza expresamente por la corrección solicitada; los metadatos internos del objeto son una decisión de representación, no una prueba adicional de 3FN.

## Reglas comprobadas

- Sin solapamientos de cobertura para una colonia ni de adscripción para un trabajador, mediante exclusiones de intervalos.
- Una sola orden activa por reporte y un solo responsable principal vigente, mediante índices y validación transaccional.
- La orden necesita reporte validado, zona correspondiente y administrador creador.
- Asignación a trabajador activo y adscrito a esa zona; el ciudadano no puede figurar como trabajador.
- Clasificación automática del reporte por la colonia del incidente; sin cobertura se conserva pendiente de clasificación y se alerta al administrador.
- Alertas internas al administrador y trabajadores activos de la zona. Una alerta no asigna el trabajo.
- Historiales con secuencias y fechas coherentes; no se editan o borran eventos.
- El administrador cierra tras una reparación resuelta; el cierre finaliza asignaciones y genera un aviso al ciudadano.
- Dirección ya utilizada en un reporte inmutable; cambios residenciales no reescriben incidentes.

`btree_gist` viene con PostgreSQL y este script habilita esa extensión en la base. El usuario instalador debe poder habilitarla. Se emplean [restricciones PostgreSQL](https://www.postgresql.org/docs/16/ddl-constraints.html), [exclusiones con btree_gist](https://www.postgresql.org/docs/16/btree-gist.html) y [validación JSON](https://www.postgresql.org/docs/16/functions-json.html).

## Consultas de revisión

```sql
SELECT * FROM v_zonas ORDER BY codigo;
SELECT * FROM v_reportes ORDER BY folio;
SELECT * FROM v_ordenes ORDER BY folio;
SELECT tipo, destinatario_id, reporte_id, orden_id, leido_en FROM notificacion ORDER BY id;
SELECT foto_json->>'url' AS url, foto_json->>'tipo' AS tipo FROM fotografia_reporte;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';
```

Dataset: 7 personas/cuentas, 1 administrador, 1 ciudadano, 5 trabajadores, 3 zonas, 5 colonias, 3 reportes, 1 orden y 2 asignaciones. Zona Norte tiene 2 trabajadores y Centro 3. Zona Sur no tiene trabajadores, pero su reporte alerta al administrador. Las cuentas demo contienen un marcador bloqueado, no contraseñas utilizables.

## Spring Boot

Los controllers ya consultan `v_zonas` y `v_reportes` mediante repositorios JDBC. Consulta [CONEXION_POSTGRESQL.md](../CONEXION_POSTGRESQL.md) para iniciar la API con la contraseña de tu servidor sin volver a instalar la base.

La conexión predeterminada usa `jdbc:postgresql://127.0.0.1:5432/odapas_report` y variables de entorno para las credenciales. Las reglas de actor guardan coherencia en los datos, pero no reemplazan autenticación HTTP: los futuros endpoints de escritura deberán obtener el usuario de la sesión.
