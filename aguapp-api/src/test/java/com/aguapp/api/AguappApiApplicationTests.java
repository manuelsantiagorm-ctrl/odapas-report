package com.aguapp.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AguappApiApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void listaColonias() throws Exception {
        mvc.perform(get("/api/colonias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void coloniaPorId() throws Exception {
        mvc.perform(get("/api/colonias/c_centro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Centro"));
    }

    @Test
    void listaReportesFiltrados() throws Exception {
        mvc.perform(get("/api/reportes").param("tipo", "fuga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void reporteInexistenteDa404() throws Exception {
        mvc.perform(get("/api/reportes/xyz"))
                .andExpect(status().isNotFound());
    }
}
