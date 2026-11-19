# Modelo actualizado: ciudadanos y reparadores

> **Modelo anterior:** estos SQL/DBML corresponden al esquema probado de 32 tablas. La definición actual está en [ODAPAS Report](../docs/ODAPAS_REPORT_ESPECIFICACION.md) y requiere un nuevo modelo con zonas, direcciones, invitaciones y notificaciones. El registro público de reparadores queda descartado: su alta será administrativa.

La entrega actual es `aguapp-api`: 32 tablas en 3FN y 43 relaciones.

## Generar el diagrama

1. Abrir https://dbdiagram.io/ y crear un diagrama.
2. Abrir `aguapp_modelo_relacional.dbml`, copiar **todo** su contenido y reemplazar el ejemplo del editor.
3. El código define tipos, PK, FK, UNIQUE, CHECK, índices y cuatro grupos de tablas. Ajustar la posición visual y exportar PNG/PDF para la entrega.

DBML es código del diagrama. Para ejecutar PostgreSQL usar los archivos SQL, no el DBML.

## Archivos SQL

- `aguapp_normalizada_demo.sql`: estructura completa + catálogos + usuarios laborales ficticios + reportes + orden/reparación demo. Ejecutar en una **base vacía** de PostgreSQL 16 con pgAdmin. Incluye BEGIN/COMMIT.
- `aguapp_normalizada.sql`: estructura completa + catálogos, sin cuentas ni órdenes ficticias. También requiere una base vacía.
- Spring Boot: usar Flyway, que aplica V1/V2/V4 en modo normal y V1/V2/V3/V4/V5 en perfil demo desde el primer arranque. Si la base anterior fue creada por Flyway en demo hasta V3, V4/V5 hacen la actualización sin reiniciar datos. No ejecutar el SQL combinado sobre esa base.

Las V1/V2/V3 ya aplicadas no se editaron. No mezclar instalación manual con Flyway sin preparar un baseline.

## Dos registros distintos con una cuenta común

| Registro | Datos propios | Perfil |
|---|---|---|
| Ciudadano | colonia residencial, referencia de domicilio, aceptación de términos | ciudadano y rol ciudadano |
| Reparador | organismo, número de empleado, especialidades, aprobación del alta | empleado + reparador + especialidades; rol reparador solo después de aprobación |

Nombre, correo, hash de contraseña y teléfono pertenecen a usuario. El correo es único para todos los perfiles. Una persona puede ser trabajador y ciudadano con la misma cuenta. Supervisor y administrador son roles concedidos de forma controlada.

Se propone alta de reparadores por administrador como opción por defecto; también puede implementarse solicitud personal pendiente. Los formularios/servicios de esos registros **todavía no están implementados**. Las tablas y estados soportan ambos flujos.

## Seguimiento

Reporte → orden de trabajo → asignaciones a uno o varios reparadores → intervenciones/reparaciones → evidencias y materiales. Historial conserva estado y actor de cada evento. Verificación del reporte, resultado de reparación y cierre de orden son hechos separados.

Consultar `../docs/NORMALIZACION.md`, `../docs/PRUEBAS_DB.md` y `../docs/RESULTADOS_PRUEBAS.md` para justificación y pruebas. El organismo seed es ficticio; no representa información de ODAPAS.

## Regenerar DBML desde el modelo real

```powershell
psql -U postgres -d aguapp -v ON_ERROR_STOP=1 -At -f database/exportar_modelo.sql -o database/modelo_metadatos.json
node database/generar-diagrama.cjs database/modelo_metadatos.json database/aguapp_modelo_relacional.dbml
```

Estos metadatos contienen estructura, no datos de cuentas. El DBML entregado se generó desde PostgreSQL con migraciones V1 a V5 verificadas.
