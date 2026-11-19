# Spring Boot y PostgreSQL — 15 de octubre de 2026

- Spring Boot 3.3.5, compilación Java release 17 con JDK Temurin 21.
- PostgreSQL 16 real de validación, base odapas_report, puerto 55439.
- **21 pruebas ejecutadas: 0 fallos, 0 errores, 0 omitidas** al configurar ODAPAS_TEST_DB_URL.
- 13 pruebas de integración: consultas, filtros combinados, mayúsculas, parámetros SQL, ausencias/404, GPS null, arrays PostgreSQL, conteo de trabajadores, fecha de México y cambios persistidos visibles en el JSON.
- 8 pruebas web: contrato de respuesta, delegación al repositorio, filtros, 404, null y CORS.
- Las modificaciones de integración usan transacciones con rollback; no tocaron la base del usuario en 5432.
- API iniciada en 18080: HTTP devolvió 3 zonas, 3 reportes, 1 fuga en Norte y GPS ausente preservado como null. Salida en http-real.json.
- Se corrigió un carácter `4` aislado en ControladorZonas que impedía compilar.
- Se configuró Surefire para descubrir los nombres españoles `Pruebas*.java`; una compilación exitosa sin descubrir tests no cuenta como validación.

La configuración final apunta a 127.0.0.1:5432/odapas_report. El usuario introduce su contraseña al ejecutar iniciar-api.ps1, sin guardarla en archivos. No se ejecutó el arranque sobre su servidor principal, porque no se dispone de esa contraseña. Los ensayos HTTP utilizaron solamente la instancia de validación.

Los reportes integracion-postgresql.txt y controladores-web.txt conservan los resultados de la ejecución completa. Con `mvnw.cmd test` sin configurar la URL de pruebas, se ejecutan los 8 tests web y se omiten los 13 de PostgreSQL; no se intenta conectar a la base principal.
