# Ejecución y pruebas de la DB normalizada

## Preparar PostgreSQL

Con PostgreSQL 16 instalado, crear una base nueva desde psql o pgAdmin:

```sql
CREATE DATABASE aguapp;
```

Configurar credenciales de un usuario con permisos de crear tablas en esa base. En PowerShell, desde `aguapp-api`:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/aguapp'
$env:DB_USER = 'postgres'
$env:DB_PASSWORD = '<tu contraseña local>'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Se requiere JDK 17+ y Maven (el wrapper puede descargar Maven). No pegar contraseñas reales en archivos del proyecto. Flyway ejecuta V1 (11 tablas), V2 (catálogos) y, con perfil demo, V3 (4 colonias, 2 usuarios bloqueados, 3 reportes, 2 verificaciones y 3 entradas de calendario). No se cargan fotos ni documentos falsos: sus tablas se prueban con fixtures transaccionales. El perfil normal carga solo V1/V2 y no datos ficticios. Usar bases separadas para demo y datos reales; quitar el perfil demo no elimina filas ya cargadas.

Migraciones aplicadas no deben editarse: los cambios posteriores deben tener una nueva versión. No usar `schema.sql`, `data.sql` o generación Hibernate simultáneamente. Si la DB ya contiene tablas de otra versión, crear una base de prueba nueva; no ejecutar DROP sobre datos existentes.

Para revisión manual del SQL en una base vacía, ejecutar V1, V2 y V3 en orden desde pgAdmin/psql. No mezclar esa instalación manual con Flyway sobre la misma base sin plan de baseline.

## Pruebas automatizadas

```powershell
.\mvnw.cmd test
```

Los tests usan H2 en memoria, modo PostgreSQL, las mismas migraciones y MockMvc. No necesitan credenciales de la DB local ni modifican sus datos. Las pruebas nuevas son transaccionales y revierten sus cambios.

Se incluyen las cuatro pruebas GET originales y nueve pruebas adicionales: cambio de nombre en colonia reflejado en ambos controllers, verificación derivada de la relación, reporte sin GPS/colonia, rechazo de FK inexistente, rango GPS, par GPS incompleto, email duplicado, compatibilidad de mayúsculas y respuestas de ausencia/filtro desconocido. H2 sirve como verificación rápida; no sustituye la ejecución del SQL en PostgreSQL.

## Navegador o Postman con perfil demo

Usar la colección existente `postman/AguApp-API.postman_collection.json`.

| Petición GET | Resultado esperado |
|---|---|
| `/api/colonias` | 200; 4 colonias |
| `/api/colonias/c_centro` | 200; Centro / normal |
| `/api/colonias/inexistente` | 404 |
| `/api/reportes` | 200; r1, r2, r3 |
| `/api/reportes?tipo=fuga` | 200; solo r1 |
| `/api/reportes?tipo=FUGA` | 200; solo r1 |
| `/api/reportes?tipo=desconocido` | 200; lista vacía |
| `/api/reportes/r1` | 200; Centro / fuga / verificado true |
| `/api/reportes/r3` | 200; La Paz / corte / verificado false |
| `/api/reportes/inexistente` | 404 |

Para demostrar que se consulta la DB, cambiar temporalmente `colonia.nombre` dentro de una transacción de test: ambos controllers deben reflejarlo. La prueba automatizada realiza esta comprobación y revierte la modificación. Para capturas externas usar una base demo desechable y restaurar la fila después de capturar.

## Consultas para comprobar la entrega

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history ORDER BY installed_rank;

SELECT r.id, t.nombre AS tipo, c.nombre AS colonia, u.nombre AS autor,
       r.creado_en, v.verificado_en
FROM reporte r
JOIN tipo_reporte t ON t.codigo = r.tipo_codigo
JOIN usuario u ON u.id = r.usuario_id
LEFT JOIN colonia c ON c.id = r.colonia_id
LEFT JOIN verificacion_reporte v ON v.reporte_id = r.id
ORDER BY r.id;

SELECT c.nombre, cal.fecha, e.nombre AS estado
FROM calendario_suministro cal
JOIN colonia c ON c.id = cal.colonia_id
JOIN estado_servicio e ON e.codigo = cal.estado_servicio_codigo
ORDER BY cal.fecha;
```

Entregar: justificación 1FN/2FN/3FN, diagrama, DDL, datos demo, resultados de pruebas y capturas reales. No presentar escenarios futuros como endpoints implementados: login, registro, recuperación, carga de fotos, calendario y verificación requieren aún controllers y servicios con autenticación. Los dos controllers actuales solo leen la DB.
