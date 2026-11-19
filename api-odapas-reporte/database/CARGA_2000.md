# Carga escolar de 2,000 registros por tabla

Archivo: **05_carga_2000.sql**. Interpreta la solicitud como **dos mil**, no dos millones.

## Ejecutar en pgAdmin

1. Abrir Query Tool en la base que tenga el esquema corregido y los datos demo originales, por ejemplo odapas_report.
2. Abrir 05_carga_2000.sql y ejecutar **todo el archivo** con F5, sin seleccionar fragmentos.
3. Revisar los avisos de progreso en Messages y el resumen de filas agregadas.

No necesita volver a ejecutar el instalador ni desactivar triggers. Se recomienda una base de pruebas separada si se desea conservar pequeño el dataset del proyecto.

## Cantidades

Se agregan 2,000 registros a cada una de las 20 tablas principales restantes y cantidades mayores a las siguientes, porque son relaciones o eventos necesarios:

| Tabla | Filas nuevas con el seed original |
|---|---:|
| usuario_rol | 4,000: cada persona tiene perfiles de ciudadano y trabajador |
| historial_reporte | 4,000: recibido y validado por reporte |
| historial_orden | 8,000: asignada, en proceso, reparación realizada y cerrada |
| notificacion | 18,000: avisos territoriales, asignación, avances y cierre |
| Cada una de las otras 20 tablas | 2,000 |
| Total | 74,000 |

Si existen más administradores activos, se generan más notificaciones. El resumen muestra el incremento real de cada tabla. No sería correcto forzar exactamente 2,000 historiales desactivando reglas: 2,000 reportes nuevos y su validación requieren al menos 4,000 eventos.

Se incluyen 2,000 tipos de reporte artificiales solo para probar volumen en ese catálogo. Los reportes generados usan los tres tipos originales para mantener compatibilidad con Angular. Cada trabajador de prueba también tiene perfil ciudadano; ambos roles se guardan en filas independientes.

## Comportamiento

- Conserva datos anteriores; agrega filas identificadas por un lote aleatorio en nombres, correos, códigos y folios.
- Una transacción: si falla una relación o validación, no se confirma ninguna fila del lote. Las secuencias pueden dejar huecos si se revierte una ejecución; eso es normal.
- Repetirlo agrega otro lote, no reemplaza el anterior.
- Se mantienen activas las FK, validaciones, exclusiones y reglas administrativas.
- Las órdenes se completan y cierran correctamente, con una persona responsable y una reparación resuelta.
- Las fotos son JSON de metadatos ficticios; no se generan 4,000 archivos físicos.
- Las cuentas llevan un marcador bloqueado, no contraseñas válidas. Los tokens son ejemplos históricos de prueba, no credenciales utilizables.
- Las alertas son filas internas de la DB; no se mandan mensajes externos.

Después, consulta:

```sql
SELECT COUNT(*) FROM persona;
SELECT COUNT(*) FROM reporte;
SELECT COUNT(*) FROM reparacion;
SELECT COUNT(*) FROM notificacion;
SELECT * FROM v_reportes ORDER BY id DESC LIMIT 20;
```

La API reflejará los nuevos registros si apunta a esa base. Los tests de integración que esperan exactamente tres reportes/tres zonas deben seguir ejecutándose en otra base con el seed pequeño. Esta carga comprueba consistencia y volumen en PostgreSQL; no equivale por sí sola a una prueba de rendimiento de Angular ni de usuarios concurrentes.
