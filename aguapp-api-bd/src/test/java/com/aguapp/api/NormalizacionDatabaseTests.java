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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NormalizacionDatabaseTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test
    void controllerLeeCambiosPersistidosYRelacionDeColonia() throws Exception {
        jdbc.update("UPDATE colonia SET nombre = ? WHERE id = ?", "Centro actualizado", "c_centro");
        mvc.perform(get("/api/colonias/c_centro"))
                .andExpect(jsonPath("$.nombre").value("Centro actualizado"));
        mvc.perform(get("/api/reportes/r1"))
                .andExpect(jsonPath("$.colonia").value("Centro actualizado"));
    }

    @Test
    void verificadoSeDerivaDeLaRelacion() throws Exception {
        jdbc.update("DELETE FROM verificacion_reporte WHERE reporte_id = ?", "r1");
        mvc.perform(get("/api/reportes/r1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.verificado").value(false));
    }

    @Test
    void admiteReporteSinGpsNiColonia() throws Exception {
        jdbc.update("INSERT INTO reporte(id,tipo_codigo,usuario_id) VALUES (?,?,?)",
                "r-sin-gps", "fuga", "usr-demo");
        mvc.perform(get("/api/reportes/r-sin-gps"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lat").isEmpty())
                .andExpect(jsonPath("$.lng").isEmpty()).andExpect(jsonPath("$.colonia").isEmpty());
    }

    @Test
    void coloniaInexistenteNoSePuedeReferenciar() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE reporte SET colonia_id = ? WHERE id = ?", "no-existe", "r1"));
    }

    @Test
    void coordenadasDebenEstarEnRango() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE reporte SET latitud = 91 WHERE id = 'r1'"));
    }

    @Test
    void coordenadasDebenEstarCompletas() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE reporte SET longitud = NULL WHERE id = 'r1'"));
    }

    @Test
    void emailNormalizadoEsUnico() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO usuario(id,nombre,email,password_hash,colonia_id) VALUES (?,?,?,?,?)",
                "duplicado", "Prueba", "ciudadano@example.test", "!BLOQUEADA", "c_centro"));
    }

    @Test
    void aceptaBusquedaSinDistinguirMayusculas() throws Exception {
        mvc.perform(get("/api/reportes/R1")).andExpect(status().isOk());
        mvc.perform(get("/api/reportes").param("tipo", "FUGA"))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/colonias/C_CENTRO")).andExpect(status().isOk());
    }

    @Test
    void coloniaInexistenteDa404YTipoDesconocidoListaVacia() throws Exception {
        mvc.perform(get("/api/colonias/inexistente")).andExpect(status().isNotFound());
        mvc.perform(get("/api/reportes").param("tipo", "otro"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
}
