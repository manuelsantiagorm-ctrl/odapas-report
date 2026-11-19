package mx.odapas.reporte;

import mx.odapas.reporte.controlador.*;
import mx.odapas.reporte.repositorio.RepositorioCatalogos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;

@WebMvcTest({ControladorPersonas.class, ControladorMunicipios.class, ControladorColonias.class,
    ControladorTiposReporte.class, ControladorContactos.class})
class PruebasCincoCrudWeb {
    @org.springframework.boot.test.mock.mockito.MockBean mx.odapas.reporte.privacidad.ServicioCuenta cuentas;
    @org.junit.jupiter.api.BeforeEach void sesionAdministrador() {
        org.mockito.Mockito.when(cuentas.autenticar(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new mx.odapas.reporte.privacidad.ServicioCuenta.Identidad(1,true));
    }
    @Autowired MockMvc mvc;
    @MockBean RepositorioCatalogos repo;
    private static final Map<String,String> TABLAS = Map.of("personas","persona","municipios","municipios",
        "colonias","colonia","tipos-reporte","tipo_reporte","contactos","contacto");
    @ParameterizedTest
    @ValueSource(strings={"personas","municipios","colonias","tipos-reporte","contactos"})
    void contratoCrudCompleto(String ruta) throws Exception {
        String tabla = TABLAS.get(ruta);
        Map<String,Object> fila = Map.of("id",42,"nombre","Prueba");
        when(repo.listar(tabla)).thenReturn(List.of(fila));
        when(repo.obtener(tabla,42)).thenReturn(fila);
        when(repo.crear(eq(tabla),anyMap())).thenReturn(fila);
        when(repo.actualizar(eq(tabla),eq(42),anyMap())).thenReturn(fila);
        mvc.perform(post("/api/"+ruta).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Prueba\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(42));
        mvc.perform(get("/api/"+ruta)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(42));
        mvc.perform(get("/api/"+ruta+"/42")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(42));
        mvc.perform(put("/api/"+ruta+"/42").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cambio\"}"))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/"+ruta+"/42")).andExpect(status().isNoContent());
        verify(repo).crear(tabla,Map.of("nombre","Prueba"));
        verify(repo).actualizar(tabla,42,Map.of("nombre","Cambio"));
        verify(repo).eliminar(tabla,42);
    }
}

