package mx.odapas.reporte;

import mx.odapas.reporte.privacidad.Claves;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Seguridad y reglas de los módulos de operación (administrador, trabajador, reportes nuevos).
 * Cada prueba se revierte al terminar (@Transactional), no deja datos.
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "ODAPAS_TEST_DB_URL", matches = ".+")
class PruebasOperacion {
    @Autowired WebApplicationContext contexto;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;

    @DynamicPropertySource
    static void conexion(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", () -> System.getenv("ODAPAS_TEST_DB_URL"));
        p.add("spring.datasource.username", () -> System.getenv().getOrDefault("ODAPAS_TEST_DB_USER", "postgres"));
        p.add("spring.datasource.password", () -> System.getenv().getOrDefault("ODAPAS_TEST_DB_PASSWORD", ""));
    }

    @BeforeEach void preparar() { mvc = MockMvcBuilders.webAppContextSetup(contexto).build(); }

    /** Crea una sesión para el primer usuario que cumpla la condición y devuelve el encabezado Authorization. */
    private String sesion(String condicion) {
        Integer id = jdbc.queryForObject("SELECT u.id FROM public.usuario u WHERE u.estado='activo' AND " + condicion + " ORDER BY u.id LIMIT 1", Integer.class);
        String token = "prueba-" + UUID.randomUUID();
        jdbc.update("INSERT INTO public.sesion_usuario(usuario_id,token_hash) VALUES(?,?)", id, Claves.huella(token));
        return "Bearer " + token;
    }
    private String admin() { return sesion("public.odapas_es_admin(u.id)"); }
    private String soloCiudadano() {
        return sesion("EXISTS(SELECT 1 FROM public.usuario_rol r WHERE r.usuario_id=u.id AND r.rol='ciudadano') " +
                "AND NOT EXISTS(SELECT 1 FROM public.usuario_rol r WHERE r.usuario_id=u.id AND r.rol IN ('administrador','trabajador'))");
    }

    @Test void panelAdministradorExigeSesionYRol() throws Exception {
        mvc.perform(get("/api/admin/estadisticas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/estadisticas").header("Authorization", soloCiudadano())).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/estadisticas").header("Authorization", admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reportes.total").isNumber());
    }

    @Test void panelTrabajadorSoloParaTrabajadores() throws Exception {
        mvc.perform(get("/api/trabajador/ordenes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/trabajador/ordenes").header("Authorization", soloCiudadano())).andExpect(status().isForbidden());
    }

    @Test void mapaPublicoNoExponeDatosPersonales() throws Exception {
        mvc.perform(get("/api/publico/mapa")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].folio").doesNotExist())
                .andExpect(content().string(not(containsString("telefono"))));
    }

    @Test void bandejaPaginadaYFiltrada() throws Exception {
        mvc.perform(get("/api/admin/reportes").param("tipo", "fuga").param("tamano", "5").header("Authorization", admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.datos.length()", lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$.datos[*].tipo", everyItem(is("fuga"))));
    }

    @Test void noSeCreaOrdenParaReporteSinValidar() throws Exception {
        // R-000124 está en revisión en los datos de prueba
        mvc.perform(post("/api/admin/ordenes").header("Authorization", admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reporteFolio\":\"R-000124\",\"prioridad\":\"alta\",\"responsableId\":5}"))
                .andExpect(status().isConflict());
    }

    @Test void validacionDeDatosResponde400() throws Exception {
        mvc.perform(put("/api/admin/reportes/R-000124/revision").header("Authorization", admin()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"rechazado\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error", containsString("motivo")));
    }

    @Test void rechazaArchivosQueNoSonImagen() throws Exception {
        var falso = new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", "<script>alert(1)</script>".getBytes());
        mvc.perform(multipart("/api/reportes/R-000123/fotos").file(falso).header("Authorization", admin()))
                .andExpect(status().is4xxClientError());
    }

    @Test void tokenDeEnlaceInventadoNoSirve() throws Exception {
        mvc.perform(post("/api/auth/verificar").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"inventado\"}"))
                .andExpect(status().is4xxClientError());
        mvc.perform(post("/api/auth/verificar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + "A".repeat(43) + "\"}"))
                .andExpect(status().isGone());
    }
}
