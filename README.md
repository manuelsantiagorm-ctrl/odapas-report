# ODAPAS Report

**Cuenta y derechos ARCO implementados:** [guía de uso](api-odapas-reporte/PRIVACIDAD_ARCO.md) y [colección de Postman](api-odapas-reporte/coleccion-postman/ODAPAS_ARCO.postman_collection.json). Incluye registro e inicio de sesión en PostgreSQL, edición de perfil, baja con revocación de sesiones, solicitudes ARCO y documentos legales versionados. La ampliación añade seis tablas al modelo base de 24 al reiniciar la API.

**Base PostgreSQL actual, modelo escolar corregido:** [instalación y uso](api-odapas-reporte/database/README.md), [SQL completo](api-odapas-reporte/database/odapas_report_completa.sql) y [verificación](api-odapas-reporte/database/RESULTADOS.md). Incluye 24 tablas con Persona, Contacto, Municipios y fotos JSONB; no incluye materiales. Los controllers de `api-odapas-reporte` ya consultan esta base por JDBC: [cómo iniciar la API](api-odapas-reporte/CONEXION_POSTGRESQL.md).

La definición funcional actual está en [Documentación completa de ODAPAS Report](aguapp-api/docs/ODAPAS_REPORT_ESPECIFICACION.md), con [resumen y recorrido](aguapp-api/docs/ODAPAS_REPORT_RESUMEN.md).

Decisiones: el ciudadano se registra con dirección; el administrador da de alta trabajadores, los asigna a una zona y gestiona órdenes. Las alertas de incidentes se dirigen al personal elegible de la zona. El próximo modelo relacional se basará en esta documentación.

**Antecedente técnico:** los siguientes archivos corresponden al esquema anterior de 32 tablas. Está probado, pero aún no incluye las zonas, direcciones completas, invitaciones, notificaciones y demás reglas de la nueva especificación. No es el modelo final de ODAPAS Report.

La normalización de la base de datos y su integración con los controllers de Spring Boot están en `aguapp-api`:

Modelo anterior: 32 tablas con perfiles de ciudadanos/personal, roles, reparadores, cuadrillas, asignaciones y seguimiento de reparaciones. [DBML del antecedente](aguapp-api/database/aguapp_modelo_relacional.dbml).

- [Análisis, diagrama y justificación 3FN](aguapp-api/docs/NORMALIZACION.md).
- [SQL completo para importar una base demo vacía](aguapp-api/database/aguapp_normalizada_demo.sql).
- [Preparación y ejecución](aguapp-api/docs/PRUEBAS_DB.md).
- [Resultados de pruebas en H2 y PostgreSQL](aguapp-api/docs/RESULTADOS_PRUEBAS.md).

El frontend consulta reportes a través de los dos controllers actuales de `api-odapas-reporte`, conectados a PostgreSQL. El registro de usuarios y la creación de reportes desde formularios todavía requieren implementar sus endpoints de escritura. Los cuatro controllers de `aguapp-api` son un antecedente anterior, no el backend actual.

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 17.3.17.

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via a platform of your choice. To use this command, you need to first add a package that implements end-to-end testing capabilities.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
