# Spring Boot conectado a PostgreSQL

Los controllers consultan la base **odapas_report** mediante JDBC. Se conservan rutas y campos JSON para que Angular siga funcionando.

```text
Angular → ControladorReportes / ControladorZonas → repositorios JDBC → v_reportes / v_zonas → tablas PostgreSQL
```

## Iniciar con tu base instalada

Desde `C:\Users\LENOVO\Desktop\aguapp`:

```powershell
powershell -NoProfile -File .\api-odapas-reporte\iniciar-api.ps1
```

Introduce **la contraseña de postgres que usas en pgAdmin**. No se guarda en archivos. También puedes hacer doble clic en `iniciar-api.cmd`.

El arranque apunta a `127.0.0.1:5432/odapas_report`, con usuario `postgres`, y levanta la API en `http://localhost:8080`. Verifica las vistas al iniciar y muestra `Conexión PostgreSQL verificada. Base: odapas_report`.

Si ya hay otra API en 8080, detén esa ejecución con Ctrl+C en su terminal antes de iniciar esta. Para otro servidor/usuario/puerto de API:

```powershell
.\api-odapas-reporte\iniciar-api.ps1 -Url 'jdbc:postgresql://localhost:5432/odapas_report' -Usuario postgres -PuertoApi 8081
```

Angular usa 8080 por defecto; si cambias ese puerto tendrás que ajustar su URL. La configuración también acepta DB_URL, DB_USER y DB_PASSWORD desde el entorno cuando se inicia con Maven directamente. El launcher usa su parámetro -Url/-Usuario y pide la contraseña si DB_PASSWORD no está definida.

No vuelvas a ejecutar el instalador de la DB. `spring.sql.init.mode=never` evita volver a cargar estructura o seeds. No se introdujo generación de tablas JPA ni se modificaron los datos de tu servidor principal.

## Comprobar

- http://localhost:8080/api/zonas
- http://localhost:8080/api/zonas/ZN
- http://localhost:8080/api/reportes
- http://localhost:8080/api/reportes?tipo=fuga
- http://localhost:8080/api/reportes?zona=ZN
- http://localhost:8080/api/reportes/R-000123

Con el dataset original se obtienen tres zonas y tres reportes. Los conteos cambiarán si agregas datos. GPS y prioridad ausentes permanecen null; las fechas se presentan en horario de México. Las consultas usan parámetros, no concatenan valores recibidos del usuario en el SQL.

## Pruebas

Desde `api-odapas-reporte`, `mvnw.cmd test` ejecuta ocho pruebas web sin necesitar PostgreSQL. Las trece pruebas de integración se omiten salvo que configures explícitamente ODAPAS_TEST_DB_URL.

Para integración usa una **base demo de pruebas separada** con las 24 tablas y el seed original. Las pruebas cambian temporalmente nombre de colonia, descripción de zona y estado de un trabajador y revierten sus transacciones. No apuntarlas a tus datos de trabajo.

```powershell
$env:ODAPAS_TEST_DB_URL = 'jdbc:postgresql://localhost:5432/odapas_report_pruebas'
$env:ODAPAS_TEST_DB_USER = 'postgres'
# Configurar ODAPAS_TEST_DB_PASSWORD en la sesión local sin publicarla.
.\mvnw.cmd test
```

El patrón `Pruebas*.java` está declarado en Surefire para que Maven descubra los nombres de clase en español. Con conexión de pruebas configurada, el total es **21 tests**: 13 de integración PostgreSQL y 8 web. Los reportes de la ejecución verificada están en `evidencias/conexion-postgresql/`.

Se probó sobre PostgreSQL real de validación en 127.0.0.1:55439. La conexión a tu servidor principal usa su propia contraseña, que solo tú introduces al arrancar el script. No se reutiliza ni se cambia la contraseña de pgAdmin.

## Alcance

Se implementó lectura real de zonas y reportes. Registro, autenticación de usuarios y envío de nuevos reportes desde Angular aún requieren endpoints de escritura; esta conexión no los implementa. La UI de registro existente continúa siendo una simulación.

Referencia: [soporte SQL/JDBC de Spring Boot](https://docs.spring.io/spring-boot/reference/data/sql.html).
