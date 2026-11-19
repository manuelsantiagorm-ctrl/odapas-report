# Verificación del módulo de cuenta y ARCO
Fecha: 2026-10-21.

- PostgreSQL aislado: puerto 55439, base odapas_report_pruebas.
- 37 pruebas automáticas aprobadas: 11 ARCO/cuenta, 13 integración anterior, 5 CRUD web y 8 controladores web.
- 12 comprobaciones HTTP reales en la API temporal 18080 (http-resultados.json).
- Verificado en navegador: login real, apertura de Mi cuenta, rectificación con contraseña actual y persistencia, solicitud ARCO con folio y estado.
- Frontend Angular: compilación de producción exitosa. Avisos de tamaño del bundle (981.85 kB frente a aviso de 500 kB) y formato CommonJS de Leaflet; no impiden compilar.
- Migración incremental arrancó correctamente y se pudo repetir sin duplicar documentos.
- Las cuentas usadas en pruebas son ficticias; no se modificó odapas_report del servidor principal 5432.
- La aplicación real aplica la ampliación al reiniciar Spring Boot; véase PRIVACIDAD_ARCO.md.

