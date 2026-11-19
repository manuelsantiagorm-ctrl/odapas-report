# Carga masiva verificada

Se ejecutó 05_carga_2000.sql en una base nueva `odapas_report_carga_2000` de la instancia aislada PostgreSQL 16, puerto 55439, preparada con odapas_report_completa.sql.

- 24 tablas recibieron al menos 2,000 filas adicionales cada una.
- 20 tablas recibieron exactamente 2,000 filas adicionales.
- usuario_rol: 4,000 filas nuevas.
- historial_reporte: 4,000 filas nuevas.
- historial_orden: 8,000 filas nuevas.
- notificacion: 18,000 filas nuevas.
- Total: **74,000 filas adicionales**.
- Se confirmaron la transacción y las restricciones diferidas sin errores.
- No se desactivaron triggers, claves foráneas ni exclusiones.
- No se modificó la base del usuario en el puerto 5432.

El resumen real por tabla está en carga-2000-resultados.txt. La preparación de la base de prueba está en carga-2000-instalacion.txt.
