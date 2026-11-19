package com.aguapp.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class ReparadoresDatabaseTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @Test void perfilesSeparadosSinDuplicarCredenciales() {
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM ciudadano WHERE usuario_id='usr-demo'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ciudadano WHERE usuario_id='usr-reparador'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE id='usr-reparador'", Integer.class));
    }
    @Test void listaSoloReparadoresAutorizadosYNoExponeCredenciales() throws Exception {
        mvc.perform(get("/api/reparadores")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].password_hash").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist());
        mvc.perform(get("/api/reparadores/usr-pendiente")).andExpect(status().isNotFound());
    }
    @Test void cuentaInactivaORevocacionDeRolExcluyeReparador() throws Exception {
        jdbc.update("UPDATE usuario SET activo=FALSE WHERE id='usr-reparador'");
        mvc.perform(get("/api/reparadores/usr-reparador")).andExpect(status().isNotFound());
        jdbc.update("DELETE FROM usuario_rol WHERE usuario_id='usr-ayudante' AND rol_codigo='reparador'");
        mvc.perform(get("/api/reparadores")).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void ciudadanoNoPuedeFigurarComoReparadorAsignado() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO asignacion_orden(id,orden_id,reparador_id,asignado_por) VALUES ('asig-invalida','ot1','usr-demo','usr-verificador')"));
    }
    @Test void numeroDeEmpleadoUnicoDentroDelOrganismo() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE empleado SET numero_empleado='REP-001' WHERE usuario_id='usr-ayudante'"));
    }
    @Test void aprobacionRequiereResponsableYFecha() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE empleado SET estado_alta='aprobado' WHERE usuario_id='usr-pendiente'"));
    }
    @Test void ordenMuestraEstadoDeUltimoEventoYReparadoresAsignados() throws Exception {
        mvc.perform(get("/api/ordenes-trabajo/ot1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("reparada")).andExpect(jsonPath("$.reporteId").value("r1"));
        mvc.perform(get("/api/ordenes-trabajo").param("estado","REPARADA").param("reparadorId","usr-reparador"))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/ordenes-trabajo").param("reparadorId","usr-demo"))
                .andExpect(jsonPath("$.length()").value(0));
    }
    @Test void variasPersonasEnUnaOrdenNoDuplicanLaOrden() throws Exception {
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM asignacion_orden WHERE orden_id='ot1'", Integer.class));
        mvc.perform(get("/api/ordenes-trabajo")).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void cierreSeDerivaDelNuevoEvento() throws Exception {
        jdbc.update("INSERT INTO historial_orden(id,orden_id,secuencia,estado_codigo,realizado_por) VALUES ('ev-cierre','ot1',5,'cerrada','usr-verificador')");
        mvc.perform(get("/api/ordenes-trabajo/ot1")).andExpect(jsonPath("$.estado").value("cerrada"));
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM historial_orden WHERE orden_id='ot1'", Integer.class));
    }
    @Test void secuenciaDeHistorialNoSeDuplica() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO historial_orden(id,orden_id,secuencia,estado_codigo,realizado_por) VALUES ('ev-duplicado','ot1',4,'cerrada','usr-verificador')"));
    }
    @Test void finDeReparacionNoPuedePrecederAlInicio() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE reparacion SET terminado_en=iniciado_en - INTERVAL '1' HOUR WHERE id='rep-1'"));
    }
    @Test void noSeCierraReparacionSinResultado() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE reparacion SET resultado_codigo=NULL WHERE id='rep-1'"));
    }
    @Test void consumoRequiereCantidadPositiva() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE consumo_material SET cantidad=0 WHERE reparacion_id='rep-1'"));
    }
    @Test void materialSeTrazaHastaReporteYReparador() {
        assertEquals("r1", jdbc.queryForObject("SELECT ot.reporte_id FROM consumo_material c JOIN reparacion r ON r.id=c.reparacion_id JOIN asignacion_orden a ON a.id=r.asignacion_id JOIN orden_trabajo ot ON ot.id=a.orden_id WHERE c.reparacion_id='rep-1'", String.class));
    }
    @Test void evidenciaDeReparacionSeGuardaSeparadaDelReporte() {
        jdbc.update("INSERT INTO evidencia_reparacion(id,reparacion_id,fase,archivo_url,nombre_original,mime_type,bytes) VALUES ('foto-rep','rep-1','despues','https://example.test/foto.jpg','foto.jpg','image/jpeg',100)");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM evidencia_reparacion WHERE reparacion_id='rep-1'", Integer.class));
    }
    @Test void faseDeEvidenciaDebeSerValida() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO evidencia_reparacion(id,reparacion_id,fase,archivo_url,nombre_original,mime_type,bytes) VALUES ('foto-invalida','rep-1','invalida','url','foto.jpg','image/jpeg',100)"));
    }
    @Test void ordenInexistenteDa404() throws Exception {
        mvc.perform(get("/api/ordenes-trabajo/no-existe")).andExpect(status().isNotFound());
    }
}
