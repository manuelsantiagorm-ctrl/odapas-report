# Resultados verificados — 7 de octubre de 2026

Entorno: Spring Boot 3.3.5, Java Temurin 21 (compilación release 17), Maven 3.9.9, PostgreSQL 16.11 y H2 2.2.224.

| Verificación | Resultado |
|---|---|
| Migraciones V1/V2/V3/V4/V5 en H2 mediante Flyway | Correctas; 32 tablas |
| MockMvc con H2: originales + normalización + reparadores | 30 ejecutados, 0 fallos, 0 errores |
| Actualización de PostgreSQL demo existente V3 → V5 con Flyway | Correcta; conserva reportes y colonias y migra residencia al perfil ciudadano |
| MockMvc con PostgreSQL real | 30 ejecutados, 0 fallos, 0 errores |
| SQL manual combinado en otra base PostgreSQL vacía | Correcto, dentro de una transacción |
| SQL de estructura/catálogos sin datos demo | Correcto, dentro de una transacción en otra base vacía |
| Diagrama generado del catálogo PostgreSQL | 32 tablas, 43 relaciones; PK, FK, UNIQUE, CHECK e índices |
| Validación SQL original y de personal/reparaciones | 36 rechazos esperados (16 + 20); relaciones, historial y trazabilidad correctos |

La comprobación en PostgreSQL detectó que el driver no permite `getObject(columna, Double.class)` sobre NUMERIC. Se corrigió leyendo BigDecimal y convirtiendo con null preservado. Después de corregir, se repitieron satisfactoriamente ambos motores.

Reportes reales en `evidencias/pruebas-db/`: h2-controllers.txt, h2-normalizacion.txt, h2-reparadores.txt, postgresql-controllers.txt, postgresql-normalizacion.txt, postgresql-reparadores.txt, postgresql-restricciones.txt, postgresql-reparadores-restricciones.txt, postgresql-instalacion-manual.txt y postgresql-estructura-sin-demo.txt. Los fixtures se revierten con rollback. Se utilizó una instancia temporal en 127.0.0.1:55439, separada de las bases del usuario, y se detuvo al finalizar.

## Reproducir

Para H2: `mvnw.cmd test` con JDK 17+ disponible.

Para PostgreSQL, crear una **base demo vacía** `aguapp_controller_test` y ajustar el usuario/puerto a su entorno. En PowerShell:

```powershell
.\mvnw.cmd test '-Dspring.datasource.url=jdbc:postgresql://localhost:5432/aguapp_controller_test' '-Dspring.datasource.username=postgres' '-Dspring.datasource.password=<tu contraseña>'
psql -U postgres -d aguapp_controller_test -v ON_ERROR_STOP=1 -f docs/validar-postgresql.sql
psql -U postgres -d aguapp_controller_test -v ON_ERROR_STOP=1 -f docs/validar-reparadores.sql
```

Estos tests cargan el seed de demo automáticamente. No apuntar a una base con datos reales ni datos adicionales: las pruebas originales esperan los conteos de demostración. El SQL manual combinado es otra opción de instalación para una base vacía; no ejecutarlo antes de estas migraciones Flyway.

No se generaron capturas de navegador/Postman ni se probó integración HTTP con Angular. El frontend sigue simulado; los reportes guardados corresponden a integración Spring Boot/DB con MockMvc y a validación SQL real.
