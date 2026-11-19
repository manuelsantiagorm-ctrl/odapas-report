package mx.odapas.reporte;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas automaticas de los dos controladores.
 * Se ejecutan con: mvnw.cmd test
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "ODAPAS_TEST_DB_URL", matches = ".+")
class PruebasApiOdapas {
    @Autowired org.springframework.web.context.WebApplicationContext contexto;
    @org.junit.jupiter.api.BeforeEach void autenticarAdministradorDePrueba() {
        String token="integracion-"+java.util.UUID.randomUUID();
        Integer admin=jdbc.queryForObject("SELECT id FROM public.usuario WHERE public.odapas_es_admin(id) ORDER BY id LIMIT 1",Integer.class);
        jdbc.update("INSERT INTO public.sesion_usuario(usuario_id,token_hash) VALUES(?,?)",admin,mx.odapas.reporte.privacidad.Claves.huella(token));
        simuladorPeticiones=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(contexto)
            .defaultRequest(get("/").header("Authorization","Bearer "+token)).build();
    }

    @DynamicPropertySource
    static void conexionDePruebas(DynamicPropertyRegistry propiedades) {
        propiedades.add("spring.datasource.url", () -> System.getenv("ODAPAS_TEST_DB_URL"));
        propiedades.add("spring.datasource.username", () -> System.getenv().getOrDefault("ODAPAS_TEST_DB_USER", "postgres"));
        propiedades.add("spring.datasource.password", () -> System.getenv().getOrDefault("ODAPAS_TEST_DB_PASSWORD", ""));
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MockMvc simuladorPeticiones;

    @Test
    void listaTresZonas() throws Exception {
        simuladorPeticiones.perform(get("/api/zonas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", org.hamcrest.Matchers.hasItems("ZN", "ZC", "ZS")));
    }

    @Test
    void obtieneZonaPorCodigo() throws Exception {
        simuladorPeticiones.perform(get("/api/zonas/ZN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Zona Norte"));
    }

    @Test
    void filtraReportesPorTipo() throws Exception {
        simuladorPeticiones.perform(get("/api/reportes").param("tipo", "fuga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipo", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("fuga"))))
                .andExpect(jsonPath("$[*].folio", org.hamcrest.Matchers.hasItem("R-000123")));
    }

    @Test
    void obtieneReportePorFolio() throws Exception {
        simuladorPeticiones.perform(get("/api/reportes/R-000124"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zona").value("ZC"));
    }

    @Test
    void reporteInexistenteResponde404() throws Exception {
        simuladorPeticiones.perform(get("/api/reportes/R-999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void filtrosCombinadosYMayusculas() throws Exception {
        // Mayúsculas/minúsculas se normalizan y los dos filtros se aplican juntos.
        simuladorPeticiones.perform(get("/api/reportes").param("tipo", "FUGA").param("zona", "zn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].folio", org.hamcrest.Matchers.hasItem("R-000123")))
                .andExpect(jsonPath("$[*].tipo", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("fuga"))))
                .andExpect(jsonPath("$[*].zona", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ZN"))));
        simuladorPeticiones.perform(get("/api/reportes").param("tipo", "fuga").param("zona", "ZC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].folio", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("R-000123"))));
    }

    @Test
    void parametrosNoSeInterpretanComoSql() throws Exception {
        simuladorPeticiones.perform(get("/api/reportes").param("tipo", "fuga' OR '1'='1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void gpsYprioridadAusentesSeConservanComoNull() throws Exception {
        simuladorPeticiones.perform(get("/api/reportes/R-000125"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.latitud").isEmpty())
                .andExpect(jsonPath("$.longitud").isEmpty()).andExpect(jsonPath("$.prioridad").isEmpty());
    }

    @Test
    void convierteArregloPostgresYCalculaPersonal() throws Exception {
        simuladorPeticiones.perform(get("/api/zonas/ZN"))
                .andExpect(jsonPath("$.colonias.length()").value(2))
                .andExpect(jsonPath("$.trabajadoresDisponibles").value(2));
        simuladorPeticiones.perform(get("/api/zonas/ZS"))
                .andExpect(jsonPath("$.trabajadoresDisponibles").value(0));
    }

    @Test
    void cambiosEnPostgresAparecenEnLosControllers() throws Exception {
        jdbc.update("UPDATE zona SET descripcion=? WHERE codigo=?", "Descripción de prueba JDBC", "ZN");
        simuladorPeticiones.perform(get("/api/zonas/ZN"))
                .andExpect(jsonPath("$.descripcion").value("Descripción de prueba JDBC"));
        jdbc.update("UPDATE colonia SET nombre=? WHERE nombre=?", "Colonia actualizada por prueba", "Las Flores");
        simuladorPeticiones.perform(get("/api/reportes/R-000123"))
                .andExpect(jsonPath("$.colonia").value("Colonia actualizada por prueba"));
    }

    @Test
    void suspenderTrabajadorActualizaElConteoDeZona() throws Exception {
        jdbc.update("UPDATE usuario SET estado='suspendido' WHERE id=(SELECT usuario_id FROM trabajador WHERE numero_empleado='REP-001')");
        simuladorPeticiones.perform(get("/api/zonas/ZN"))
                .andExpect(jsonPath("$.trabajadoresDisponibles").value(1));
    }

    @Test
    void fechasSePresentanEnHorarioDeMexico() throws Exception {
        simuladorPeticiones.perform(get("/api/reportes/R-000123"))
                .andExpect(jsonPath("$.fechaCreacion").value("2026-10-07T09:15:00-06:00"));
    }

    @Test
    void zonaInexistenteResponde404() throws Exception {
        simuladorPeticiones.perform(get("/api/zonas/NO-EXISTE")).andExpect(status().isNotFound());
    }
}
