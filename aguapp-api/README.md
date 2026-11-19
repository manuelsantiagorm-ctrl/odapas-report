# AguApp API — API REST con Spring Boot

**Nueva definición de producto:** [ODAPAS Report — documentación funcional](docs/ODAPAS_REPORT_ESPECIFICACION.md). El esquema y los cuatro controllers descritos abajo son el antecedente implementado; el próximo modelo incorporará zonas, direcciones, invitaciones y alertas. El alta laboral definitiva será exclusivamente administrativa.

API REST de **AguApp** (sistema de reportes de fugas y falta de agua), hecha con **Spring Boot 3**.
Expone **4 controllers** GET: colonias, reportes, reparadores aprobados y órdenes de trabajo. Consultan una base en 3FN de 32 tablas mediante JDBC. Flyway crea el esquema y el perfil demo carga los ejemplos. Incluye perfiles separados de ciudadano/personal, cuadrillas y seguimiento de reparaciones.

La actividad está en [docs/NORMALIZACION.md](docs/NORMALIZACION.md): análisis, diagrama y justificación 3FN. Antes de arrancar, preparar PostgreSQL según [docs/PRUEBAS_DB.md](docs/PRUEBAS_DB.md).

> Materia: Desarrollo Web Profesional · Actividad: Desarrollo de una API REST con Spring Boot
> Alumno: Santiago Osorio José Manuel

---

## Tecnologías

| Herramienta | Versión |
|---|---|
| Java (JDK) | 17 o superior |
| Spring Boot | 3.3.5 (`spring-boot-starter-web`) |
| Persistencia | JDBC, PostgreSQL 16+, Flyway; H2 en pruebas |
| Maven | 3.9.x (incluido con `mvnw`, no hace falta instalarlo) |
| Postman | cualquier versión reciente |

---

## Estructura del proyecto

```
aguapp-api/
├── pom.xml                                  # Dependencias de Maven
├── mvnw / mvnw.cmd                          # Wrapper: descarga Maven si no lo tienes
├── src/main/java/com/aguapp/api/
│   ├── AguappApiApplication.java            # Clase principal (arranca el servidor)
│   ├── controller/
│   │   ├── ColoniaController.java           # Controller 1 → /api/colonias
│   │   └── ReporteController.java           # Controller 2 → /api/reportes
│   └── model/
│       ├── Colonia.java                     # record con los datos de una colonia
│       └── Reporte.java                     # record con los datos de un reporte
├── src/main/resources/application.properties  # Puerto 8080
├── src/test/java/.../AguappApiApplicationTests.java  # Pruebas automáticas
├── postman/AguApp-API.postman_collection.json        # Colección para importar en Postman
└── evidencias/
    ├── navegador/                           # Capturas de las pruebas en el navegador
    └── postman/                             # Capturas de las pruebas en Postman
```

---

## Cómo ejecutar el proyecto

1. Tener instalado **Java 17 o superior**. Comprobar con:
   ```bash
   java -version
   ```
2. Clonar el repositorio y entrar a la carpeta:
   ```bash
   git clone https://github.com/<tu-usuario>/aguapp-api.git
   cd aguapp-api
   ```
3. Levantar la API:
   - **Windows:** `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo`
   - **Linux / macOS:** `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`
   - (Si ya tienes Maven instalado también sirve `mvn spring-boot:run`, o dar *Run* a `AguappApiApplication` desde IntelliJ / VS Code.)
4. Cuando en la consola aparezca `Tomcat started on port 8080`, la API está lista en **http://localhost:8080**.

Para correr las pruebas automáticas: `mvnw.cmd test` (Windows) o `./mvnw test`.

---

## Endpoints

Todos usan el método **GET**.

Nuevos controllers: `/api/reparadores`, `/api/reparadores/{id}`, `/api/ordenes-trabajo` y `/api/ordenes-trabajo/{id}`. Las órdenes admiten filtros `estado` y `reparadorId`. Revisar [docs/PRUEBAS_DB.md](docs/PRUEBAS_DB.md) para ejemplos y resultados. Registro, aprobación y escritura de trabajos todavía requieren servicios con autenticación; estos endpoints son de consulta académica.

### Controller 1 — `ColoniaController` (`/api/colonias`)

| Método | URL | Descripción |
|---|---|---|
| GET | `http://localhost:8080/api/colonias` | Lista todas las colonias |
| GET | `http://localhost:8080/api/colonias/c_centro` | Una colonia por su id (404 si no existe) |

Ejemplo de respuesta de `/api/colonias/c_centro`:
```json
{
  "id" : "c_centro",
  "nombre" : "Centro",
  "estadoServicio" : "normal"
}
```

### Controller 2 — `ReporteController` (`/api/reportes`)

| Método | URL | Descripción |
|---|---|---|
| GET | `http://localhost:8080/api/reportes` | Lista todos los reportes |
| GET | `http://localhost:8080/api/reportes?tipo=fuga` | Filtra por tipo (`fuga`, `baja`, `corte`) |
| GET | `http://localhost:8080/api/reportes/r1` | Un reporte por su id (404 si no existe) |

Ejemplo de respuesta de `/api/reportes/r1`:
```json
{
  "id" : "r1",
  "tipo" : "fuga",
  "colonia" : "Centro",
  "notas" : "Fuga en calle 12",
  "lat" : 19.4327,
  "lng" : -99.1332,
  "fecha" : "2026-10-07",
  "verificado" : true
}
```

---

## Pruebas

### 1. Navegador
Con la API corriendo, abrir en el navegador cada URL de las tablas de arriba. El navegador hace una petición GET y muestra el JSON.

Capturas en [`evidencias/navegador`](evidencias/navegador):

| Endpoint | Captura |
|---|---|
| `/api/colonias` | ![colonias](evidencias/navegador/01-colonias.png) |
| `/api/colonias/c_centro` | ![colonia](evidencias/navegador/02-colonia-por-id.png) |
| `/api/reportes` | ![reportes](evidencias/navegador/03-reportes.png) |
| `/api/reportes?tipo=fuga` | ![filtro](evidencias/navegador/04-reportes-filtro.png) |
| `/api/reportes/r1` | ![reporte](evidencias/navegador/05-reporte-por-id.png) |

### 2. Postman
1. En Postman: **Import** → seleccionar `postman/AguApp-API.postman_collection.json`.
2. La colección trae la variable `baseUrl = http://localhost:8080` y una carpeta por controller.
3. Dar **Send** en cada petición. Cada una incluye tests (pestaña *Test Results*) que comprueban el status 200 y el contenido.
4. También se puede correr toda la colección con **Run collection**.

Capturas en [`evidencias/postman`](evidencias/postman):

| Prueba | Captura |
|---|---|
| GET `/api/colonias` | ![postman colonias](evidencias/postman/01-colonias.png) |
| GET `/api/reportes` | ![postman reportes](evidencias/postman/02-reportes.png) |
| Run collection (todas las pruebas) | ![postman runner](evidencias/postman/03-runner.png) |
