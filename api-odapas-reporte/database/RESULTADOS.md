# Verificación de PostgreSQL — 14 de octubre de 2026

Se verificó el modelo escolar corregido en PostgreSQL 16.11, en una instancia de prueba aislada en 127.0.0.1:55439. No se alteró la instalación habitual del usuario en el puerto 5432.

| Comprobación | Resultado |
|---|---|
| Crear una base vacía y ejecutar el SQL completo | Correcto; transacción confirmada |
| Tablas de negocio | 24 |
| Tipos enumerados | 9 |
| Vistas de consulta | 3: v_zonas, v_reportes, v_ordenes |
| Persona, Contacto y Municipios | Tablas independientes creadas |
| Materiales | No existen tablas de materiales, unidades o consumos |
| Fotografías JSONB | Objeto obligatorio con URL, nombre y tipo de imagen |
| Datos de prueba | 7 cuentas, 5 trabajadores, 3 zonas, 3 reportes, 1 orden y 2 asignaciones |
| Alertas territoriales | Norte: administrador y sus 2 trabajadores; Sur sin trabajadores: administrador |
| Casos de rechazo esperados | 29, todos correctos |
| Recorrido positivo | Inicio → reparación resuelta → foto JSON → cierre por administrador → aviso al ciudadano |
| Fixtures de pruebas | Revertidos con ROLLBACK |

Los rechazos comprueban claves, correo único y normalizado, municipio, código postal, GPS, dirección histórica, JSON inválido, alta laboral no administrativa, número de empleado, solapamientos, clasificación territorial incorrecta, tokens vencidos al crearse, duplicidad de orden, reporte no validado, trabajador de otra zona, ciudadano como trabajador, responsables duplicados, revisión no administrativa, modificación de historial, transiciones inválidas, fechas de reparación y referencias ambiguas en notificaciones.

Los scripts de prueba no realizan autenticación HTTP ni envían push/correos externos. La API todavía usa listas fijas. La base del servidor principal 5432 requiere la contraseña del usuario instalador: esa instalación se realiza desde pgAdmin autenticado o `instalar.ps1`, que solicita la contraseña localmente sin guardarla.

Para reproducir: crear base vacía, ejecutar `odapas_report_completa.sql` y después `04_pruebas.sql`. Los archivos `evidencias/instalacion-postgresql.txt` y `evidencias/pruebas-postgresql.txt` conservan la salida real de la validación.
