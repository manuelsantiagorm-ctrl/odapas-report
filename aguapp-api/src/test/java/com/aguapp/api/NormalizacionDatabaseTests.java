package com.aguapp.api;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class NormalizacionDatabaseTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Test void controllerLeeCambiosPersistidosYRelacion() throws Exception {
        jdbc.update("UPDATE colonia SET nombre=? WHERE id=?", "Centro actualizado", "c_centro");
        mvc.perform(get("/api/colonias/c_centro")).andExpect(jsonPath("$.nombre").value("Centro actualizado"));
        mvc.perform(get("/api/reportes/r1")).andExpect(jsonPath("$.colonia").value("Centro actualizado"));
    }
    @Test void verificacionDerivada() throws Exception {
        jdbc.update("DELETE FROM verificacion_reporte WHERE reporte_id='r1'");
        mvc.perform(get("/api/reportes/r1")).andExpect(status().isOk()).andExpect(jsonPath("$.verificado").value(false));
    }
    @Test void sinGpsNiColonia() throws Exception {
        jdbc.update("INSERT INTO reporte(id,tipo_codigo,usuario_id) VALUES ('r-sin-gps','fuga','usr-demo')");
        mvc.perform(get("/api/reportes/r-sin-gps")).andExpect(status().isOk())
                .andExpect(jsonPath("$.lat").isEmpty()).andExpect(jsonPath("$.lng").isEmpty())
                .andExpect(jsonPath("$.colonia").isEmpty());
    }
    @Test void rechazaFk() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE reporte SET colonia_id='no-existe' WHERE id='r1'"));
    }
    @Test void rechazaGpsFueraRango() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE reporte SET latitud=91 WHERE id='r1'"));
    }
    @Test void rechazaGpsIncompleto() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE reporte SET longitud=NULL WHERE id='r1'"));
    }
    @Test void rechazaEmailDuplicado() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("UPDATE usuario SET email='ciudadano@example.test' WHERE id='usr-verificador'"));
    }
    @Test void compatibilidadMayusculas() throws Exception {
        mvc.perform(get("/api/reportes/R1")).andExpect(status().isOk());
        mvc.perform(get("/api/reportes").param("tipo","FUGA")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/colonias/C_CENTRO")).andExpect(status().isOk());
    }
    @Test void ausencias() throws Exception {
        mvc.perform(get("/api/colonias/inexistente")).andExpect(status().isNotFound());
        mvc.perform(get("/api/reportes").param("tipo","otro")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
}
