# Preparación y pruebas

Resultados ejecutados y evidencias en [RESULTADOS_PRUEBAS.md](RESULTADOS_PRUEBAS.md).
Para importar desde pgAdmin en una base vacía, puede usar `database/aguapp_normalizada_demo.sql`: combina estructura, catálogos y datos demo dentro de una transacción. Es una alternativa a Flyway, no un paso previo al arranque automático.

## PostgreSQL

Requisitos: PostgreSQL 16+, JDK 17+, Maven (incluido en el wrapper si puede descargarlo). Crear una base vacía desde pgAdmin o psql:

```sql
CREATE DATABASE aguapp;
```

En PowerShell, desde aguapp-api, configurar las credenciales locales:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/aguapp'
$env:DB_USER = 'postgres'
$env:DB_PASSWORD = '<tu contraseña>'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Flyway aplica V1/V2/V4 al esquema normal. Con perfil demo desde la primera ejecución aplica V1/V2/V3/V4/V5: 32 tablas, 4 colonias, 5 cuentas bloqueadas (un ciudadano, un supervisor y tres perfiles reparador, uno pendiente), 3 reportes, 2 verificaciones, 3 días de calendario, una cuadrilla, dos asignaciones, una reparación y una orden con cuatro eventos. Perfil normal no carga datos ficticios. Usar DB distinta para demo/datos reales. No activar demo después de que una base normal ya alcanzó V4: V3 sería una migración de versión anterior. La actualización de una base demo V3 existente sí aplica V4/V5 sin borrar datos.

No editar migraciones ya aplicadas ni mezclar Flyway con schema.sql, data.sql o generación Hibernate. Para versiones previas usar una DB de prueba nueva; no borrar datos existentes. Para revisión manual puede ejecutar V1/V2/V3 en orden sobre una base vacía, pero no mezclar esa instalación con Flyway sin baseline planificado.

## Tests

```powershell
.\mvnw.cmd test
```

H2 en memoria con modo PostgreSQL ejecuta las mismas migraciones y pruebas MockMvc; no toca su PostgreSQL. Las 30 pruebas comprenden 4 originales, 9 de normalización y 17 de personal/reparaciones. Comprueban registro separado, exclusión de altas pendientes/cuentas inactivas, revocación del rol, FK de reparador, número de empleado único, decisión de alta, estado desde historial, asignación múltiple, cierre, materiales y evidencias. Las nuevas pruebas usan transacciones con rollback.

Para ejecutar la validación específica de PostgreSQL, después de V1/V2/V3 en una base demo:

```powershell
psql -U postgres -d aguapp -v ON_ERROR_STOP=1 -f docs/validar-postgresql.sql
psql -U postgres -d aguapp -v ON_ERROR_STOP=1 -f docs/validar-reparadores.sql
```

Los scripts validan 36 rechazos por restricciones (16 originales y 20 de personal/reparaciones), relaciones y trazabilidad. Sus fixtures se revierten con ROLLBACK. H2 no sustituye esta comprobación en el motor real.

## Pruebas GET en navegador/Postman

Importar postman/AguApp-API.postman_collection.json y activar perfil demo.

| GET | Esperado |
|---|---|
| /api/colonias | 200, 4 colonias |
| /api/colonias/c_centro | 200, Centro/normal |
| /api/colonias/inexistente | 404 |
| /api/reportes | 200, r1/r2/r3 |
| /api/reportes?tipo=fuga | 200, solo r1 |
| /api/reportes?tipo=FUGA | 200, solo r1 |
| /api/reportes?tipo=desconocido | 200, [] |
| /api/reportes/r1 | 200, fuga/Centro, verificado true |
| /api/reportes/r3 | 200, corte/La Paz, verificado false |
| /api/reportes/inexistente | 404 |
| /api/reparadores | 200, dos reparadores aprobados |
| /api/reparadores/usr-reparador | 200, Reparador de prueba |
| /api/reparadores/usr-pendiente | 404, sin acceso al directorio aprobado |
| /api/ordenes-trabajo | 200, una orden ot1 |
| /api/ordenes-trabajo/ot1 | 200, folio OT-2026-0001, estado reparada |
| /api/ordenes-trabajo?estado=reparada | 200, una orden |
| /api/ordenes-trabajo?reparadorId=usr-reparador | 200, una orden asignada |
| /api/ordenes-trabajo?reparadorId=usr-demo | 200, [] |

Los conteos exactos de Postman son para seed demo sin modificaciones. Capturas reales en evidencias/navegador y evidencias/postman; no inventar evidencia.

## Consultas para entrega

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT r.id, t.nombre AS tipo, c.nombre AS colonia, u.nombre AS autor,
       r.creado_en, v.verificado_en
FROM reporte r
JOIN tipo_reporte t ON t.codigo=r.tipo_codigo
JOIN usuario u ON u.id=r.usuario_id
LEFT JOIN colonia c ON c.id=r.colonia_id
LEFT JOIN verificacion_reporte v ON v.reporte_id=r.id
ORDER BY r.id;
SELECT c.nombre, cal.fecha, e.nombre AS estado
FROM calendario_suministro cal JOIN colonia c ON c.id=cal.colonia_id
JOIN estado_servicio e ON e.codigo=cal.estado_servicio_codigo ORDER BY cal.fecha;
```

La entrega incluye justificación 1FN/2FN/3FN, diagrama, DDL, seeds y resultados. Los cuatro controllers implementados son GET de consulta académica. Login, registros diferenciados, aprobación, permisos HTTP, asignación, historial, reparación y carga de fotos requieren servicios autenticados de escritura; tener tablas/roles no significa que esas pantallas o permisos ya estén implementados.
