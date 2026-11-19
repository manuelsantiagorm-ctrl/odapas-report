package mx.odapas.reporte;

import mx.odapas.reporte.configuracion.ConfiguracionCors;
import mx.odapas.reporte.controlador.ControladorReportes;
import mx.odapas.reporte.controlador.ControladorZonas;
import mx.odapas.reporte.modelo.Reporte;
import mx.odapas.reporte.modelo.Zona;
import mx.odapas.reporte.repositorio.RepositorioReportes;
import mx.odapas.reporte.repositorio.RepositorioZonas;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ControladorZonas.class, ControladorReportes.class})
@Import(ConfiguracionCors.class)
class PruebasControladoresWeb {
    @org.springframework.boot.test.mock.mockito.MockBean mx.odapas.reporte.privacidad.ServicioCuenta cuentas;
    @org.junit.jupiter.api.BeforeEach void sesionAdministrador() {
        org.mockito.Mockito.when(cuentas.autenticar(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new mx.odapas.reporte.privacidad.ServicioCuenta.Identidad(1,true));
    }
    @Autowired MockMvc mvc;
    @MockBean RepositorioZonas zonas;
    @MockBean RepositorioReportes reportes;
    @MockBean mx.odapas.reporte.servicio.ServicioReportes servicioReportes;

    @Test void devuelveDatosDelRepositorioYConservaContrato() throws Exception {
        when(zonas.listar()).thenReturn(List.of(new Zona("Z-PRUEBA", "Zona dinámica", "Prueba", true, List.of("Colonia prueba"), 7)));
        mvc.perform(get("/api/zonas")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("Z-PRUEBA"))
                .andExpect(jsonPath("$[0].colonias[0]").value("Colonia prueba"))
                .andExpect(jsonPath("$[0].trabajadoresDisponibles").value(7));
    }
    @Test void zonaNoEncontradaDa404() throws Exception {
        when(zonas.obtener("NO")).thenReturn(Optional.empty());
        mvc.perform(get("/api/zonas/NO")).andExpect(status().isNotFound());
    }
    @Test void filtrosLleganAlRepositorio() throws Exception {
        when(reportes.listar("fuga", "ZN")).thenReturn(List.of());
        mvc.perform(get("/api/reportes").param("tipo", "fuga").param("zona", "ZN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        verify(reportes).listar("fuga", "ZN");
    }
    @Test void listarSinFiltrosEsValido() throws Exception {
        when(reportes.listar(null, null)).thenReturn(List.of());
        mvc.perform(get("/api/reportes")).andExpect(status().isOk());
        verify(reportes).listar(null, null);
    }
    @Test void reporteNoEncontradoDa404() throws Exception {
        when(reportes.obtener("NO")).thenReturn(Optional.empty());
        mvc.perform(get("/api/reportes/NO")).andExpect(status().isNotFound());
    }
    @Test void conservaNulosDelContrato() throws Exception {
        when(reportes.obtener("R-PRUEBA")).thenReturn(Optional.of(new Reporte(
                "R-PRUEBA", "fuga", "Prueba", "Colonia", "Calle S/N", null,
                null, null, "recibido", null, "2026-10-15T09:00:00-06:00")));
        mvc.perform(get("/api/reportes/R-PRUEBA")).andExpect(status().isOk())
                .andExpect(jsonPath("$.zona").isEmpty()).andExpect(jsonPath("$.latitud").isEmpty());
    }
    @Test void permiteOrigenAngular() throws Exception {
        when(zonas.listar()).thenReturn(List.of());
        mvc.perform(get("/api/zonas").header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
    @Test void rechazaOrigenNoAutorizado() throws Exception {
        mvc.perform(get("/api/zonas").header("Origin", "https://origen-no-permitido.example"))
                .andExpect(status().isForbidden());
    }
}
